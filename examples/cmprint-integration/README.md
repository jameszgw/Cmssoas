# CmPrint × CODEMAN 商业授权集成

CmPrint(Web 打印模板设计器)的商业授权与审计查询接入 CODEMAN 平台的参考实现。
平台侧功能(产品目录、三档套餐、授权 UI、审计查询)已内置;本目录提供**客户端集成件**与**全链路演示脚本**。

## 契约(两侧约定,勿单边改动)

| 项 | 约定 |
|---|---|
| 产品编码 | `claims.productCode = "CMPRINT"` |
| 版本档位 | `claims.edition ∈ COMMUNITY / PROFESSIONAL / ENTERPRISE`(对应 CmPrint `resolveEdition` 的 community/professional/enterprise,传入前转小写) |
| 能力表 | `claims.features` = 签发时固化的「档位预设 ∪ 合同微调」,键名与 CmPrint `CAPABILITY_KEYS` 完全同名(37 键;见 cmprint 仓库 `src/core/capabilities.js`) |
| 消费方式 | `resolveEdition(claims.edition.toLowerCase(), claims.features)` → `<cmprint-designer :capabilities>`;显式键优先于 CmPrint 内置预设,**两侧版本漂移不影响已签发授权** |
| 版本范围 | `claims.appVersionRange`(默认 `>=0.5.0 <1.0.0`),按 CmPrint package.json 的 semver 校验 |
| .lic 格式 | `base64url(claims JSON) + "." + base64url(Ed25519 签名)`;公钥 `GET /api/licenses/public-key`(匿名分发:`GET /pub/license/public-keys`) |
| 吊销 | 在线:平台验签/心跳即时拒绝;离线:定期拉取已签名 CRL(`GET /pub/crl`)用 `isRevoked` 检查 |

## 文件

- `cmprint-license.mjs` — 零依赖适配器(浏览器 / Node ≥ 20):解析 .lic → WebCrypto Ed25519 验签 →
  产品/状态/有效期校验 → 映射为 `{ edition, overrides }`;另含已签名 CRL 校验 `isRevoked`。
- `verify-demo.mjs` — 命令行演示:`node verify-demo.mjs <license.lic> <公钥Base64>`,
  打印 claims 摘要与 resolveEdition 同语义的全键能力解析结果。
- `issue-and-verify.sh` — 全链路脚本:登录 → 档位矩阵 → 签发(可带能力微调)→ 下载 .lic →
  平台验签 → 本地验签 → 审计查询。

## 快速开始

```bash
# 平台已启动(默认 admin/8888)
BASE_URL=http://localhost:8080 bash issue-and-verify.sh PROFESSIONAL "客户名称" '{"directPrint":true}'
```

前端集成(Vue 示例):

```js
import { resolveEdition } from 'cmprint'
import { loadCmprintLicense } from './cmprint-license.mjs'

const licText = await (await fetch('/license/cmprint.lic')).text()          // 部署侧分发 .lic
const pub = (await (await fetch(`${PLATFORM}/pub/license/public-keys`)).json())[0].publicKeyBase64
const { edition, overrides } = await loadCmprintLicense(licText, pub)        // 验签失败/过期/吊销会抛错
const capabilities = resolveEdition(edition, overrides)
// <cmprint-designer :capabilities="capabilities" />
```

服务端集成(CmPrint 企业版服务端渲染,Java SDK):

```java
LicenseClaims claims = new LicenseVerifier(publicKeyBase64).verify(licText);
if (!"CMPRINT".equals(claims.raw().get("productCode")) || !claims.isCurrentlyValid())
    throw new IllegalStateException("无有效 CmPrint License");
if (!claims.hasFeature("exportPdf")) { /* 拒绝 PDF 渲染请求 */ }
```

## 三档套餐(平台 V19 种子,订阅自动签发)

| 套餐 | edition | 关闭能力 |
|---|---|---|
| CMPRINT_COMMUNITY 社区版 | COMMUNITY | 高级导出(PDF/Word/Excel/图片/分享)、直连打印、套打/校准、水印、子模板、云模板库、换肤(13 键) |
| CMPRINT_PRO 专业版 | PROFESSIONAL | 直连打印、云模板库(2 键) |
| CMPRINT_ENT 企业版 | ENTERPRISE | 无(全功能) |

## 审计查询

控制台「CmPrint 授权」页内置;API:`GET /api/cmprint/audit?action=&keyword=&from=&to=&page=&size=`
(权限 `cmprint:audit`)。范围 = `CMPRINT_*` 动作 + 涉及本产品 License 编号的通用 License 事件
(签发/续期/变更/吊销/到期),支持 CSV 导出(`/api/cmprint/audit/export.csv`)。

## 安全注意

- 公钥应随客户端**内置分发**(或固定指纹校验),不要在运行时无校验地信任远端返回;
- `sigAlg=SM2` 的 License 本适配器不支持本地验签(WebCrypto 无 SM2),请走平台在线验签或 Java SDK;
- 离线部署建议定期(如每日)拉取已签名 CRL 并用 `isRevoked` 检查,宽限策略由集成方自定。
