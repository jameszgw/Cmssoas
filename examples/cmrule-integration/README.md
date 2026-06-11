# CmRuleEngine × CODEMAN 商业授权集成

CmRuleEngine(规则引擎)的商业授权与审计查询接入 CODEMAN 平台的参考实现。
平台侧功能(产品目录、四档套餐、授权 UI、审计查询)已内置;本目录提供**客户端集成件**与**全链路演示脚本**。

## 契约(两侧约定,勿单边改动;对侧 = CmRuleEngine 仓库 `rule-engine-server/src/capabilities.js`)

| 项 | 约定 |
|---|---|
| 产品编码 | `claims.productCode = "CMRULE"` |
| 版本档位 | `claims.edition ∈ COMMUNITY / PROFESSIONAL / ENTERPRISE / ULTIMATE`(与 capabilities.js 的 PRESETS 键同名) |
| 能力表 | `claims.features` = 签发时固化的「档位预设 ∪ 合同微调」,键名与 CmRuleEngine `CAPABILITY_KEYS` 完全同名(19 键) |
| 消费方式 | rule-engine-server 通过环境变量 `RE_LICENSE`(.lic 路径)与 `RE_LICENSE_PUBKEY`(公钥 Base64)直接验签,再 `resolve(claims.edition, claims.features)` 得全键能力门禁;显式键优先于内置预设,**两侧版本漂移不影响已签发授权** |
| 版本范围 | `claims.appVersionRange`(默认 `>=1.0.0 <2.0.0`),按 CmRuleEngine package.json 的 semver 校验 |
| .lic 格式 | `base64url(claims JSON) + "." + base64url(Ed25519 签名)`;公钥 `GET /api/licenses/public-key`(匿名分发:`GET /pub/license/public-keys`) |
| 吊销 | 在线:平台验签/心跳即时拒绝;离线:定期拉取已签名 CRL(`GET /pub/crl`)用 `isRevoked` 检查 |

## 文件

- `cmrule-license.mjs` — 零依赖适配器(浏览器 / Node ≥ 20):解析 .lic → WebCrypto Ed25519 验签 →
  产品/状态/有效期校验 → 映射为 `{ edition, overrides }`;另含已签名 CRL 校验 `isRevoked`。
  (rule-engine-server 自带验签,本适配器供浏览器/Node 宿主自行验签与能力解析。)
- `verify-demo.mjs` — 命令行演示:`node verify-demo.mjs <license.lic> <公钥Base64>`,
  打印 claims 摘要与 resolve 同语义的全键能力解析结果。
- `issue-and-verify.sh` — 全链路脚本:登录 → 档位矩阵 → 签发(可带能力微调)→ 下载 .lic 与公钥 →
  平台验签 → 本地验签 → 提示配置 rule-engine-server(RE_LICENSE/RE_LICENSE_PUBKEY)→ 审计查询。

## 快速开始

```bash
# 平台已启动(默认 admin/8888)
BASE_URL=http://localhost:8080 bash issue-and-verify.sh PROFESSIONAL "客户名称" '{"versionDiff":true}'
```

CmRuleEngine 部署侧(rule-engine-server 直接消费):

```bash
export RE_LICENSE=/etc/cmrule/LIC-2026-0001.lic       # 平台签发下载的 .lic
export RE_LICENSE_PUBKEY="MCowBQYDK2VwAyEA..."        # GET /api/licenses/public-key 的 publicKeyBase64
node rule-engine-server/src/index.js                   # 启动时验签并按 resolve(edition, features) 裁剪能力
```

浏览器 / Node 宿主自行验签(本适配器):

```js
import { resolve } from 'cmrule/capabilities'          // rule-engine-server 同款解析
import { loadCmruleLicense } from './cmrule-license.mjs'

const licText = await (await fetch('/license/cmrule.lic')).text()             // 部署侧分发 .lic
const pub = (await (await fetch(`${PLATFORM}/pub/license/public-keys`)).json())[0].publicKeyBase64
const { edition, overrides } = await loadCmruleLicense(licText, pub)          // 验签失败/过期/吊销会抛错
const capabilities = resolve(edition, overrides)
// capabilities.decisionTable / capabilities.aiRuleGen … 即能力门禁
```

## 四档套餐(平台 V20 种子,订阅自动签发)

| 套餐 | edition | 关闭能力 |
|---|---|---|
| CMRULE_COMMUNITY 社区版 | COMMUNITY | 模板库、决策表、规则流、EDGE 规则链、版本管理、编排基础、审计查询、影响分析、审计导出、高级编排、国产 DB、HA 集群、AI 生成、分布式事务、信创(15 键) |
| CMRULE_PRO 专业版 | PROFESSIONAL | 影响分析、审计导出、高级编排、国产 DB、HA 集群、AI 生成、分布式事务、信创(8 键) |
| CMRULE_ENT 企业版 | ENTERPRISE | AI 生成、分布式事务、信创(3 键) |
| CMRULE_ULT 旗舰版 | ULTIMATE | 无(全功能) |

## 审计查询

控制台「CmRuleEngine 授权」页内置;API:`GET /api/cmrule/audit?action=&keyword=&from=&to=&page=&size=`
(权限 `cmrule:audit`)。范围 = `CMRULE_*` 动作 + 涉及本产品 License 编号的通用 License 事件
(签发/续期/变更/吊销/到期),支持 CSV 导出(`/api/cmrule/audit/export.csv`)。

## 安全注意

- 公钥应随客户端**内置分发**(或固定指纹校验),不要在运行时无校验地信任远端返回;
- `sigAlg=SM2` 的 License 本适配器不支持本地验签(WebCrypto 无 SM2),请走平台在线验签或 Java SDK;
- 离线部署建议定期(如每日)拉取已签名 CRL 并用 `isRevoked` 检查,宽限策略由集成方自定。
