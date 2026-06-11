# 功能说明:CmRuleEngine 商业授权与审计查询(产品集成)

把 **CmRuleEngine(规则引擎)** 作为产品接入平台:按 CmRuleEngine 的商业版本逻辑
(社区版 / 专业版 / 企业版 / 旗舰版,见 CmRuleEngine 仓库 `rule-engine-server/src/capabilities.js`
的 `CAPABILITY_KEYS`/`PRESETS`/`resolve`)签发 License,
能力键级合同微调,内置本产品维度的**审计查询**。沿用平台一贯设计:RBAC、审计、Ed25519 签名、订阅驱动签发。

## 集成模型(谁负责什么)

| 侧 | 职责 |
|---|---|
| **CODEMAN(本平台)** | 产品目录登记、四档套餐、签发/续期/变更/吊销、CRL、审计查询、授权 UI |
| **CmRuleEngine(客户端)** | rule-engine-server 经环境变量 `RE_LICENSE`(.lic 路径)/`RE_LICENSE_PUBKEY`(公钥)验签 → `resolve(claims.edition, claims.features)` → 裁剪引擎能力 |

**契约**:`claims.productCode = CMRULE`;`claims.edition ∈ COMMUNITY/PROFESSIONAL/ENTERPRISE/ULTIMATE`;
`claims.features` = 签发时固化的「**档位预设 ∪ 合同微调**」能力表,键名与 CmRuleEngine `CAPABILITY_KEYS`(19 键)同名。
显式键优先于 CmRuleEngine 内置预设 —— 两侧版本漂移不影响已签发授权(防漂移设计)。

## 四档版本(V20 种子套餐,订阅自动签发)

| 套餐 | edition | 价格基准 | 席位 | 相对全开关闭的能力 |
|---|---|---|---|---|
| `CMRULE_COMMUNITY` 社区版 | COMMUNITY | 0 | 1 | 模板库、决策表、规则流、EDGE 规则链、版本管理、编排基础、审计查询、影响分析、审计导出、高级编排、国产 DB 适配、HA 集群、AI 生成、分布式事务、信创 —— 15 键 |
| `CMRULE_PRO` 专业版 | PROFESSIONAL | 16800 | 5 | 影响分析、审计导出、高级编排、国产 DB 适配、HA 集群、AI 生成、分布式事务、信创 —— 8 键 |
| `CMRULE_ENT` 企业版 | ENTERPRISE | 49800 | 20 | AI 生成、分布式事务、信创 —— 3 键 |
| `CMRULE_ULT` 旗舰版 | ULTIMATE | 158000 | 999 | 无(全功能) |

产品目录(产品与版本页):`CMRULE` 产品,模块 = 能力域(基础设计/低代码零代码/决策建模/规则流/版本管理/服务编排/审计/平台增强/智能,9 个),
功能点 = CmRuleEngine 能力键(与 License features 同名,授权与计费同粒度)。

## 授权 UI(控制台「CmRuleEngine 授权」,权限 `cmrule`)

- **档位 × 能力矩阵**:仅列四档有差异的 15 个能力键(其余 4 键——基础规则链/零代码向导/体检/节点搜索——四档全含),与签发表单同源。
- **签发向导**:选档位(四档单选)→ 能力开关按档位预设初始化 → 合同级微调(如专业版客户单独购买影响分析)→
  仅把**相对预设的改动**作为 `overrides` 提交,审计详情一目了然;Ed25519 签名产出 .lic。
- **列表**:仅本产品 License;下载 .lic / 续期 / 吊销复用通用端点与权限。
- **审计查询**:动作 / 关键字(License 编号、客户、操作人)/ 时间段过滤 + 分页 + CSV 导出。

## 审计查询(`cmrule:audit`)

范围 = `CMRULE_*` 动作(如 `CMRULE_LICENSE_ISSUE`,detail 含档位与微调内容)
**+ 涉及本产品 License 编号的通用 License 事件**(`LICENSE_ISSUE/RENEW/MODIFY/REVOKE/EXPIRED`)——
即本产品授权的全生命周期轨迹。API:

```
GET /api/cmrule/audit?action=&keyword=&from=&to=&page=&size=    # 分页 {total, rows}
GET /api/cmrule/audit/export.csv?action=&keyword=&from=&to=     # CSV
```

## 后端 API(`com.codeman.platform.cmrule`)

| 端点 | 权限 | 说明 |
|---|---|---|
| `GET /api/cmrule/editions` | `cmrule:view` | 四档预设 + 全键能力解析表(与客户端 resolve 同语义) |
| `GET /api/cmrule/licenses` | `cmrule:view` | 仅 productCode=CMRULE 的 License |
| `POST /api/cmrule/licenses/issue` | `cmrule:issue` | 档位+能力微调签发;**校验档位与能力键白名单**,features=预设∪微调 |
| `GET /api/cmrule/audit[/export.csv]` | `cmrule:audit` | 审计查询(见上) |

续期/变更/吊销/下载/验签复用通用 `/api/licenses/**`(原权限不变)。

## 数据与权限(V20 迁移)

- 产品目录种子:CMRULE 产品 + 9 模块 + 19 功能点(能力键)+ v1.0 版本矩阵 + 四档套餐
  (plan 的 `product_code`/`edition` 两列已由 V19 引入,订阅自动签发据此落 License 的 productCode 与 edition)。
- 权限点:`cmrule`(菜单)/`cmrule:view`/`cmrule:issue`/`cmrule:audit`
  (SUPER_ADMIN 自动补齐 FULL;VIEWER 新装自动获 MENU+view)。

## 客户端集成件与演示(`examples/cmrule-integration/`)

- `cmrule-license.mjs`:零依赖适配器(浏览器/Node≥20)——解析 .lic → WebCrypto Ed25519 验签 →
  产品/状态/有效期校验 → `{edition, overrides}`;含已签名 CRL 的 `isRevoked`。
  (rule-engine-server 自带验签——`RE_LICENSE`/`RE_LICENSE_PUBKEY`;本适配器供浏览器/Node 宿主自行验签。)
- `verify-demo.mjs`:命令行验签 + 能力解析演示。
- `issue-and-verify.sh`:登录 → 矩阵 → 签发 → 下载 → 平台验签 → 本地验签 →
  rule-engine-server 配置提示 → 审计查询 全链路脚本。

## 验证

- 后端 `CmruleIntegrationTest` 7 用例:档位矩阵(四档 19 键)/ 签发-验签契约(微调固化进 features)/
  旗舰版 19 键全开 / 非法档位与能力键 400 / 列表只含本产品 / 审计全轨迹(动作、关键字、时间窗、CSV)/
  CMRULE 套餐订阅自动签发。
- 前端 e2e `cmrule.spec.ts`:登录 → 矩阵 → 专业版+versionDiff 微调签发 → 列表 → 审计过滤。
- `mvn test` 全绿;`npm run build` 通过;`generate-schema.sh`/`build-docs.mjs` 已重新生成。
