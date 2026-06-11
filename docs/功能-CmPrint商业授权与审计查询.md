# 功能说明:CmPrint 商业授权与审计查询(产品集成)

把 **CmPrint(Web 打印模板设计器)** 作为产品接入平台:按 CmPrint 的商业版本逻辑
(社区版 / 专业版 / 企业版,见 cmprint 仓库 `docs/commercial-editions.zh-CN.md` 与
`src/core/capabilities.js` 的 `EDITIONS`/`resolveEdition`)签发 License,
能力键级合同微调,内置本产品维度的**审计查询**。沿用平台一贯设计:RBAC、审计、Ed25519 签名、订阅驱动签发。

## 集成模型(谁负责什么)

| 侧 | 职责 |
|---|---|
| **CODEMAN(本平台)** | 产品目录登记、三档套餐、签发/续期/变更/吊销、CRL、审计查询、授权 UI |
| **CmPrint(客户端)** | 验签 .lic → `resolveEdition(claims.edition.toLowerCase(), claims.features)` → `<cmprint-designer :capabilities>` 裁剪 UI/功能 |

**契约**:`claims.productCode = CMPRINT`;`claims.edition ∈ COMMUNITY/PROFESSIONAL/ENTERPRISE`;
`claims.features` = 签发时固化的「**档位预设 ∪ 合同微调**」能力表,键名与 CmPrint `CAPABILITY_KEYS`(37 键)同名。
显式键优先于 CmPrint 内置预设 —— 两侧版本漂移不影响已签发授权(防漂移设计)。

## 三档版本(V19 种子套餐,订阅自动签发)

| 套餐 | edition | 价格基准 | 席位 | 相对全开关闭的能力 |
|---|---|---|---|---|
| `CMPRINT_COMMUNITY` 社区版 | COMMUNITY | 0 | 1 | 高级导出(PDF 图片/文本、Word、Excel/CSV、图片、分享)、直连打印、套打底图、对位校准、水印、子模板、云模板库、换肤 —— 13 键 |
| `CMPRINT_PRO` 专业版 | PROFESSIONAL | 12800 | 5 | 直连打印、云模板库 —— 2 键 |
| `CMPRINT_ENT` 企业版 | ENTERPRISE | 39800 | 20 | 无(全功能) |

产品目录(产品与版本页):`CMPRINT` 产品,模块 = 能力域(设计器/数据/预览打印/导出/模板),
功能点 = CmPrint 能力键(与 License features 同名,授权与计费同粒度)。

## 授权 UI(控制台「CmPrint 授权」,权限 `cmprint`)

- **档位 × 能力矩阵**:仅列三档有差异的 13 个能力键(其余 24 键三档全含),与签发表单同源。
- **签发向导**:选档位 → 能力开关按档位预设初始化 → 合同级微调(如专业版客户单独购买直连打印)→
  仅把**相对预设的改动**作为 `overrides` 提交,审计详情一目了然;Ed25519 签名产出 .lic。
- **列表**:仅本产品 License;下载 .lic / 续期 / 吊销复用通用端点与权限。
- **审计查询**:动作 / 关键字(License 编号、客户、操作人)/ 时间段过滤 + 分页 + CSV 导出。

## 审计查询(`cmprint:audit`)

范围 = `CMPRINT_*` 动作(如 `CMPRINT_LICENSE_ISSUE`,detail 含档位与微调内容)
**+ 涉及本产品 License 编号的通用 License 事件**(`LICENSE_ISSUE/RENEW/MODIFY/REVOKE/EXPIRED`)——
即本产品授权的全生命周期轨迹。API:

```
GET /api/cmprint/audit?action=&keyword=&from=&to=&page=&size=    # 分页 {total, rows}
GET /api/cmprint/audit/export.csv?action=&keyword=&from=&to=     # CSV
```

## 后端 API(`com.codeman.platform.cmprint`)

| 端点 | 权限 | 说明 |
|---|---|---|
| `GET /api/cmprint/editions` | `cmprint:view` | 三档预设 + 全键能力解析表(与前端 resolveEdition 同语义) |
| `GET /api/cmprint/licenses` | `cmprint:view` | 仅 productCode=CMPRINT 的 License |
| `POST /api/cmprint/licenses/issue` | `cmprint:issue` | 档位+能力微调签发;**校验档位与能力键白名单**,features=预设∪微调 |
| `GET /api/cmprint/audit[/export.csv]` | `cmprint:audit` | 审计查询(见上) |

续期/变更/吊销/下载/验签复用通用 `/api/licenses/**`(原权限不变)。

## 数据与权限(V19 迁移)

- `plan` 表新增 `product_code`(默认 CODEMAN)与 `edition` —— **订阅自动签发**据此落
  License 的 productCode 与 edition(修复此前写死 CODEMAN/套餐码;存量 CODEMAN 套餐行为不变)。
- 产品目录种子:CMPRINT 产品 + 5 模块 + 17 功能点(能力键)+ v0.5 版本矩阵 + 三档套餐。
- 权限点:`cmprint`(菜单)/`cmprint:view`/`cmprint:issue`/`cmprint:audit`
  (SUPER_ADMIN 自动补齐 FULL;VIEWER 新装自动获 MENU+view)。

## 客户端集成件与演示(`examples/cmprint-integration/`)

- `cmprint-license.mjs`:零依赖适配器(浏览器/Node≥20)——解析 .lic → WebCrypto Ed25519 验签 →
  产品/状态/有效期校验 → `{edition, overrides}`;含已签名 CRL 的 `isRevoked`。
- `verify-demo.mjs`:命令行验签 + 能力解析演示。
- `issue-and-verify.sh`:登录 → 矩阵 → 签发 → 下载 → 平台验签 → 本地验签 → 审计查询 全链路脚本。

服务端渲染(CmPrint 企业版)用 Java SDK 门禁:`claims.hasFeature("exportPdf")` 等。

## 验证

- 后端 `CmprintIntegrationTest` 6 用例:档位矩阵 / 签发-验签契约(微调固化进 features)/
  非法档位与能力键 400 / 列表只含本产品 / 审计全轨迹(动作、关键字、时间窗、CSV)/ CMPRINT 套餐订阅自动签发。
- 前端 e2e `cmprint.spec.ts`:登录 → 矩阵 → 专业版+directPrint 微调签发 → 列表 → 审计过滤(全套 8/8)。
- 真机演示脚本跑通(签发 LIC-2026-0010,本地 WebCrypto 验签通过,篡改 .lic 被拒)。
- 截图:`web/console/shots/cmp-01..03`。
