# 功能 · CmReport 报表平台授权集成

> CODEMAN 平台为 **CmReport 报表平台**(独立产品线)签发其产品格式的 License,
> 复用平台的租户/套餐/订阅/计费/审计闭环。日期:2026-06-11。

## 1. 集成设计(双信任根、单平台)

| 维度 | CODEMAN 自身 | CmReport 产品线 |
|---|---|---|
| 签名算法 | Ed25519 / SM2(可切换) | **RSA-2048 SHA256withRSA**(产品内置验签) |
| 密钥 | `./var/keys/ed25519.json` 或 KMS/env | `./var/keys/cmreport-rsa.json` 或 `CMREPORT_RSA_PRIVATE_KEY/PUBLIC_KEY` |
| 令牌格式 | claims(schemaVersion 2)| `base64url(payload).base64url(sig)`,payload 为产品 `License` 字段 |
| 版本语义 | BASIC/PROFESSIONAL/ENTERPRISE/FLAGSHIP | **community/lite/pro/enterprise/ultimate**(产品按版本推导能力基线) |
| 记录存储 | `license` 表(`product_code=CODEMAN`) | 同一张表(`product_code=CMREPORT`),续期/吊销/历史/下载共用 |

- **产品分流**:`LicenseService.resign()` 对 `product_code=CMREPORT` 的行改走
  `CmReportLicenseSigner`(RSA + 产品 payload),其余保持原 claims 格式;
  续期/套餐变更/到期重签均自动按产品路由,不会把 CmReport 授权改写成 CODEMAN 格式。
- **payload 字段**(与产品 `com.codeman.report.license.License` 一一对应):
  `licenseId / customer / edition / issuedAt(ms) / expiresAt(ms) / capabilities / addons / limits / fingerprint`。
  `edition` 是版本基线(能力集由产品 `EditionCapabilities` 推导);`addons` 在基线之上叠加(如 `ai.pack`)。
- **能力矩阵副本**:`CmReportEditions`(平台侧)与产品 `EditionCapabilities` 保持同步,
  用于 UI 展示与附加包校验;产品才是能力语义的权威。

## 2. 端点(权限点复用 license:*)

| 端点 | 说明 |
|---|---|
| `GET /api/cmreport/editions` | 版本矩阵(edition → 累计能力集)+ 附加包/限额键 |
| `POST /api/cmreport/licenses/issue` | 按版本签发:`{tenantCode, customer, edition, addons[], limits{}, fingerprint?, notBefore, notAfter}` |
| `GET /api/cmreport/licenses` | CmReport 产品线授权列表 |
| `GET /api/cmreport/public-key` | RSA 验签公钥(X509 Base64,产品配置用)+ kid |
| `GET /api/cmreport/licenses/{id}/verify` | 运营侧自检(RSA 验签 + payload 回显) |
| `GET /api/licenses/{id}/download` 等 | 下载 .lic / 续期 / 吊销 / 历史 —— 复用通用端点 |

控制台页面:**「CmReport 授权」**(`/cmreport`,导航复用 license 权限点):
按版本签发(能力集预览/附加包/限额/指纹)、列表、下载 .lic、自检、续期、吊销、验签公钥复制(含 yml 片段)。

## 3. 套餐与订阅自动签发

迁移 `V19__cmreport_catalog.sql` 注入产品 `CMREPORT`、能力模块与 **5 档套餐**:

| 套餐 | 版本(features.cmEdition) | 限额示例 |
|---|---|---|
| `CMREPORT-COMMUNITY` | community | 并发 10 |
| `CMREPORT-LITE` | lite | 并发 30 / 1 实例 |
| `CMREPORT-PRO` | pro | 并发 100 / 2 实例 |
| `CMREPORT-ENT` | enterprise | 并发 500 / 4 节点 |
| `CMREPORT-ULT` | ultimate(含 ai.pack) | 并发 2000 / 16 节点 |

订阅(`POST /api/subscriptions`,套餐码 `CMREPORT-*`)即自动签发对应版本的产品格式 License,
计费/出账/审计与 CODEMAN 套餐一致;套餐变更按新版本重签(版本 +1),退订吊销。

## 4. 产品侧(CmReport)配置

```yaml
# CmReport 的 application.yml
cmreport:
  license:
    enabled: true            # 强制门控(默认 false=过渡期放行)
    public-key: <GET /api/cmreport/public-key 的 publicKeyBase64>
```
1. 控制台「CmReport 授权 → 验签公钥」一键复制 yml 片段;
2. 签发后下载 `.lic`,产品侧 `POST /report/server/license/activate` 导入(或配置文件预置);
3. 产品 `/report/server/license/status` 可查看版本/能力清单/到期/限额与本机指纹。

## 5. 安全与限制

- RSA 私钥与 Ed25519 私钥**相互独立**,轮换互不影响;生产用 `CMREPORT_RSA_PRIVATE_KEY/PUBLIC_KEY` 经 KMS 注入(私钥不落盘)。
- CmReport 当前为**离线令牌**:平台侧吊销仅作运营记录与续发拦截,已发离线令牌在产品侧到期前仍有效
  (产品在线激活/心跳通道为产品路线图 L.2.2,接通后可实时吊销)。
- `signature` 列由 V19 扩至 512(RSA-2048 签名 base64url ≈344 字符)。

## 6. 验证

- `mvn test` 全绿;新增 `CmReportLicenseIntegrationTest` 5 用例:
  版本矩阵、按版本签发(payload 字段/附加包/限额/指纹,以产品同款 RSA 验签流程核验)、
  续期重签仍为产品格式、`CMREPORT-ENT` 订阅自动签发路由、未知版本/附加包 4xx 拒绝。
- 迁移计数 18 → **19**(`MysqlMigrationTest`/`MysqlRealMigrationTest` 同步更新);
  `deploy/sql/schema-*.sql` 已由 `generate-schema.sh` 重生成。
