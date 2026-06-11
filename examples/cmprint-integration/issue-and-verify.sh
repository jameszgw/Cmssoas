#!/usr/bin/env bash
# CmPrint 商业授权全链路脚本:登录 → 按档位签发(可带能力微调)→ 下载 .lic → 平台验签 → 审计查询。
# 用法:
#   BASE_URL=http://localhost:8080 ADMIN_USER=admin ADMIN_PASS=8888 \
#   bash issue-and-verify.sh [edition] [customer] ['{"directPrint":true}']
# 依赖:curl + python3(JSON 解析)。
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
ADMIN_USER="${ADMIN_USER:-admin}"
ADMIN_PASS="${ADMIN_PASS:-8888}"
EDITION="${1:-PROFESSIONAL}"
CUSTOMER="${2:-CmPrint 集成演示客户}"
OVERRIDES="${3:-{\}}"

jsonget() { python3 -c "import sys,json;d=json.load(sys.stdin);print(d$1)"; }

echo "== 1) 登录 $BASE_URL =="
TOKEN=$(curl -sf "$BASE_URL/api/auth/login" -H 'Content-Type: application/json' \
  -d "{\"username\":\"$ADMIN_USER\",\"password\":\"$ADMIN_PASS\"}" | jsonget "['token']")
AUTH=(-H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json')

echo "== 2) 档位矩阵(/api/cmprint/editions)=="
curl -sf "${AUTH[@]}" "$BASE_URL/api/cmprint/editions" | python3 -c '
import sys, json
v = json.load(sys.stdin)
print("  产品 %s · 默认版本范围 %s · 能力键 %d 个" % (v["productCode"], v["defaultVersionRange"], len(v["capabilityKeys"])))
for e in v["editions"]:
    off = [k for k, b in e["capabilities"].items() if not b]
    print("  %-13s 关闭 %2d 键 %s" % (e["edition"], len(off), off if off else "(全功能)"))'

echo "== 3) 签发 $EDITION(微调:$OVERRIDES)=="
TODAY=$(date +%F)
NEXT=$(date -d "+1 year" +%F 2>/dev/null || date -v+1y +%F)
LIC_ID=$(curl -sf "${AUTH[@]}" "$BASE_URL/api/cmprint/licenses/issue" -d "{
  \"tenantCode\":\"T-CMP-DEMO\",\"customer\":\"$CUSTOMER\",\"edition\":\"$EDITION\",
  \"overrides\":$OVERRIDES,\"notBefore\":\"$TODAY\",\"notAfter\":\"$NEXT\"}" | jsonget "['licenseId']")
echo "  已签发 $LIC_ID"

echo "== 4) 下载 .lic 与公钥 =="
curl -sf "${AUTH[@]}" -o "$LIC_ID.lic" "$BASE_URL/api/licenses/$LIC_ID/download"
# 公钥也可匿名取自 /pub/license/public-keys(JWKS 风格,离线分发用)
PUB=$(curl -sf "${AUTH[@]}" "$BASE_URL/api/licenses/public-key" | jsonget "['publicKeyBase64']")
echo "  $LIC_ID.lic ($(wc -c < "$LIC_ID.lic") bytes) · 公钥 ${PUB:0:24}…"

echo "== 5) 平台验签(模拟 SDK)=="
curl -sf "${AUTH[@]}" "$BASE_URL/api/licenses/verify" \
  -d "{\"lic\":\"$(cat "$LIC_ID.lic")\"}" | python3 -c '
import sys, json
v = json.load(sys.stdin)
assert v["valid"], v["reason"]
c = v["claims"]
print("  ✅ 有效 · %s %s · features=%s" % (c["productCode"], c["edition"], json.dumps(c["features"], ensure_ascii=False)))'

echo "== 6) 本地验签 + 能力解析(Node WebCrypto,可选)=="
if command -v node >/dev/null && node -e 'process.exit(+!(parseInt(process.versions.node)>=20))'; then
  node "$(dirname "$0")/verify-demo.mjs" "$LIC_ID.lic" "$PUB"
else
  echo "  (跳过:需 Node ≥ 20)"
fi

echo "== 7) 审计查询(/api/cmprint/audit)=="
curl -sf "${AUTH[@]}" "$BASE_URL/api/cmprint/audit?keyword=$LIC_ID" | python3 -c '
import sys, json
v = json.load(sys.stdin)
print("  共 %d 条" % v["total"])
for r in v["rows"]:
    print("  %s %-22s %-8s %s" % (r["createdAt"][:19], r["action"], r["actor"], r["detail"]))'

echo "完成。集成端:resolveEdition(claims.edition.toLowerCase(), claims.features) → <cmprint-designer :capabilities>"
