# CODEMAN Session Handoff (English)

> For the next session to resume quickly. Mirrors `docs/HANDOFF.md` (中文). Captures current state, conventions, key decisions, gotcha-fixes, and candidate next steps.

## 0. TL;DR
- **Project**: CODEMAN — Spring Boot code protection + License authentication + multi-tenant operations platform (backend + Vue3 console + client SDK + code-protection demo).
- **Dev branch**: `claude/hopeful-ride-u8txs8` (current session; previously `claude/loving-einstein-Pp4cA`; work only on the dev branch, **never** touch `main`). GitHub repo slug stays `jameszgw/cmssoas` (**not renamed**; README badges keep this slug).
- **Latest commit**: `0c4ad69` (handoff); last feature `63830a4` (online code hardening). CI fully green (run #56, 11 jobs success; tag job skipped is expected).
- **Stack/versions**: Spring Boot **3.5.0**, Java **21** (Java 8 NOT supported), Maven; Vue3+TS+Element Plus+vite; groupId/base package **com.codeman**; version **1.0.1**; license **GPLv3**.
- **Databases**: H2 (dev default, `MODE=PostgreSQL`) / PostgreSQL (prod, `profile=prod`) / MySQL 5.7·8.0 (`profile=mysql`, `db/mysql` dialect scripts, validated by CI real-DB matrix).
- **Initial account**: `admin / 8888` (forced change on first login); default new-user password `Codeman@123`.
- **Build**: backend `cd server/license-platform && mvn test` (full); frontend `cd web/console && npm run build`.

## 1. Naming/branding (rename fully done)
- Java packages/coords `com.codeman.*`; groupId `com.codeman`; three modules version `1.0.1`.
- Brand `CODEMAN`; DB name `codeman`; mail domain `@codeman.com`; JWT default secret `codeman-dev-...`; frontend localStorage keys `codeman.*` (token/theme/locale/portal/help); npm package `codeman-console`.
- **Only exception**: GitHub repo URL stays `jameszgw/cmssoas` (README badges kept); working dir path stays `/home/user/Cmssoas` (cannot change).
- Zero `cmssoas/CMSSOAS/Cmssoas` left (LICENSE is the GPLv3 text, untouched).

## 2. Delivered modules (`com.codeman.platform.*`)
overview / tenant (onboard/activate/MFA) / license (issue·renew·modify·revoke·**signed CRL**·auto-expire; Ed25519+SM2) / online (SDK channel) / catalog (product·plan·subscription) / **customer (master data + Customer 360)** / billing (**payment collect (manual confirm)** + **e-invoice**) / **contract (self-built e-sign, hash evidence)** / **notice (notice + consent)** / **cs (AI support, OpenAI-compatible, graceful degrade)** / **portal (tenant self-service)** / **harden (online code hardening)** / rbac / mail (outbox) / alert / common.
- Console menus (permission codes): overview, tenant, license, online, catalog, plan, **customer**, billing, **contract**, **notice**, **cs**, **harden**, **tenant:portal**, audit, role:view, user:view.
- Public pages (no ops auth): `/activate/:token`, `/portal` + `/portal/home`.
- Public backend endpoints (`/pub/**`, bypass JwtAuthFilter): `/pub/notices/active`, `/pub/consents`, `/pub/payments/notify/{channel}`, `/pub/portal/login|overview`, `/pub/crl`, `/pub/license/public-keys`.

## 3. Migrations
- PG/H2: `server/.../db/migration/V1..V20`. MySQL dialect: `server/.../db/mysql/V1..V20` (transform: `IDENTITY→AUTO_INCREMENT`, `TIMESTAMP→DATETIME`, boolean default `1/0`, per-table `ENGINE=InnoDB DEFAULT CHARSET=utf8mb4`).
- **When adding a migration**: ① add `Vn` under `db/migration`; ② add same-numbered MySQL dialect under `db/mysql`; ③ run `bash deploy/sql/generate-schema.sh` to regenerate `deploy/sql/schema-*.sql`; ④ bump the migration-count assertions in `MysqlMigrationTest`/`MysqlRealMigrationTest` (currently **20**).
- Recent: V15 customer, V16 tenant_portal, V17 tax_invoice, V18 harden.

## 4. Provider abstraction pattern (core idiom: vendor-agnostic + degradable)
- AI support: `ChatProvider`+`OpenAiCompatProvider` (pure JDK HttpClient, SSE); without `app.ai.*` it degrades to keyword KB (`knowledge/faq.md`). Set `AI_BASE_URL/AI_API_KEY/AI_MODEL` to use GLM-4-Flash/Qwen/DeepSeek/Ollama.
- Payment: `PaymentProvider`+`MockPaymentProvider` (default manual confirm; `app.pay.provider`).
- E-invoice: `EInvoiceProvider`+`MockEInvoiceProvider` (`app.einvoice.provider`).
- Hardening: `HardenProvider` (OBFUSCATE=ProGuard in-process; ENCRYPT_BIND=class encrypt + License; FATJAR_ENCRYPT=class encrypt + passphrase). Reserved for Allatori/ClassFinal/Xjar.

## 5. Online code hardening (most recent big feature)
- Coexists with build-time hardening (`examples/protected-app`, `mvn -Pharden`); per-tenant `HardenConfig` (BUILD/ONLINE/BOTH) + per-task override; BUILD mode rejects online jobs.
- Flow: `POST /api/harden/jobs` (multipart upload) → async pipeline (selected techniques in order) → `GET /api/harden/jobs/{id}/download`. Config `app.harden.work-dir` (compose mounts named volume `harden`), `spring.servlet.multipart`.
- Injects pure-JDK `com.codeman.platform.harden.runtime.HardenLauncher/HardenClassLoader` (AES-256-GCM decrypt-on-load; key = SHA-256: LICENSE_BIND from bound License `.lic` text, PASSPHRASE from passphrase).
- Run output: `java -Dharden.license=<.lic> -jar x.jar` (bound) / `java -Dharden.key=<pass> -jar x.jar` (passphrase).
- **Gotcha fixes**: proguard-base brings log4j-core which clashes with Spring's log4j-to-slf4j → added `src/main/resources/log4j2.component.properties` forcing `Log4jContextFactory`; ProGuard library jars limited to `java.base/java.logging` (avoid huge java.desktop hang); `HardenService.run` catches `Throwable` (prevent Error silently hanging a job).

## 5b. CmPrint commercial licensing & audit query (most recent big feature)
- **Contract**: claims.productCode=CMPRINT; edition ∈ COMMUNITY/PROFESSIONAL/ENTERPRISE; claims.features = "edition preset ∪ contract overrides" (keys = CmPrint CAPABILITY_KEYS, 37 keys); client calls `resolveEdition(edition.toLowerCase(), features)` → `<cmprint-designer :capabilities>`. Explicit keys are frozen at issue time → no drift between repos.
- Backend `com.codeman.platform.cmprint`: `CmprintEditions` (constants mirror cmprint capabilities.js — **keep both sides in sync**), endpoints editions/licenses/issue (edition + capability-key whitelist validation) / audit (Specification filter: CMPRINT_* actions + generic license events matched by this product's license ids; action/keyword/date range + paging + CSV).
- **Subscription auto-issue fix**: plan gained product_code/edition; SubscriptionService no longer hardcodes "CODEMAN"/plan-code (legacy CODEMAN plans backfilled edition=code, behavior unchanged). LicenseView gained productCode.
- Frontend `views/CmPrint.vue` (perm=cmprint): edition × capability matrix (only the 13 differing keys), issue wizard (switches init from edition preset; only diffs vs preset submitted as overrides), license list (renew/revoke/download reuse generic endpoints), audit query.
- Client kit `examples/cmprint-integration/`: cmprint-license.mjs (zero-dep WebCrypto Ed25519 verifier; public key = X.509 SPKI base64), verify-demo.mjs, issue-and-verify.sh (full chain verified live; tampered .lic rejected).
- **Pitfalls**: Java `Map.copyOf` does not preserve order (edition-order assertion failed once) — use unmodifiable LinkedHashMap; `/api/**` requires JWT even without @RequirePerm (only `/pub/**` is anonymous).

## 5c. Template asset management (latest feature)
- `com.codeman.platform.tpl`: print_template (live content / draft separated) + version trail + per-tenant gallery keys; approval flow DRAFT→PENDING(read-only)→APPROVED; rollback pulls history into the draft and goes through approval again; every action audited as TPL_* (included in the CmPrint audit query scope).
- Public gallery `/pub/cmprint/gallery/{key}/api/templates...` matches the cmprint cloud-api.js contract verbatim ({success,message?,data}; items id/name/template/tags/version/author/useCount); designer uploads land as PENDING; owners may only delete not-yet-live uploads.
- **Contract consistency check (CI-enforced)**: `scripts/check-cmprint-contract.mjs` compares CmprintEditions.java ↔ examples/cmprint-integration/cmprint-contract.json (cmprint repo runs test/cmprint-contract.test.js against its docs/cmprint-contract.json; the two JSON files must be byte-identical, sha256[:16]=a1ef84c1629dea43).
- **Pitfalls**: Spring Data does not scan repository interfaces nested inside a plain interface (NoSuchBeanDefinition) — one top-level file per repo; browser 403 is usually the CORS allowlist (backend only permits :5173), not auth.

## 6. Tests & CI
- Backend `mvn test`: 37 tests (one `MysqlRealMigrationTest` runs only in CI real-DB, skipped locally). Key: `*IntegrationTest` (rbac/features: notice·payment·tax·customer·portal·licenseLifecycle·harden·**cmprint**·**tplAsset**), `MysqlMigrationTest` (H2 MySQL mode), `KnowledgeBaseTest`, `Sm2SignatureServiceTest`, `TotpTest`, `TenantSchemaServiceTest`.
- CI `.github/workflows/ci.yml`: backend / sdk / sign-smoke(ed25519,sm2) / harden(ProGuard) / frontend / e2e(Playwright, 9 specs incl. cmprint/templates) + contract check (check-cmprint-contract) / **mysql matrix [8.0, 5.7] real-DB migration** / ci-summary / release(tag only).
- **Real-run technique in sandbox** (no persistent `&`): use the Bash tool with `run_in_background:true` to run `java -jar`; wait with `until grep "Tomcat started on port 8080" log`; inspect CI via `mcp__github__actions_*` + `mcp__github__get_job_logs` (when list output is too large, save to file and parse with python).

## 7. Known limits / candidate TODOs (not done)
- Hardening: default ProGuard keep-rules are conservative (generic jars); strong obfuscation needs user keep-rules; full fat-jar (BOOT-INF/lib) encryption not done (currently encrypts business classes by package prefix). Xjar/Allatori/ClassFinal adapters **reserved, unimplemented**.
- Payment/e-sign/e-invoice: default mock/manual/sandbox; real channels (WeChat Pay/Alipay/Stripe, Aisino/Baiwang, e-sign vendors) adapters unimplemented (abstractions in place).
- Other candidates: usage metering/billing, revenue reconciliation reports, human-agent support console, real KMS/HSM, HA/backup-restore/blue-green, PostgreSQL real-DB CI (currently only MySQL matrix).
- Ops: `tag v*` push blocked by env (HTTP 403) — user must tag via GitHub UI; no PR created (not requested).

## 8. Constraints (throughout)
- Private keys never plaintext (inject via env/Nacos/KMS); AI support never sends secrets/PII.
- Develop & push only on the current dev branch (`claude/hopeful-ride-u8txs8`); `git push -u origin <branch>`, retry on network failure with 2/4/8/16s backoff; **no PR, no tag** unless explicitly asked.
- Docs: `docs/` (features/design) + `deploy/` (deploy/ops/help); keep README/DEPLOY/deploy indexes in sync; code & migrations carry Chinese comments.

## 9. Key paths
- Backend: `server/license-platform` (`src/main/java/com/codeman/platform/<module>`, `src/main/resources/{application*.yml,db/migration,db/mysql,knowledge,log4j2.component.properties}`).
- Frontend: `web/console/src/{views,api,layouts/DefaultLayout.vue,router/index.ts,i18n/locales/{zh-CN,en-US}.ts,components}`; screenshots `web/console/shots/`.
- SDK/demo: `sdk/license-sdk`, `examples/protected-app` (ProGuard `proguard.pro` + `-Pharden`).
- Docs: `docs/功能-*.md`, `docs/智能客服-大模型选型与部署.md`, `docs/02-代码保护方案.md`; `deploy/README.md`, `deploy/sql/*`, `deploy/docs/*`; `.env.example`, `.env.prod.example`, `docker-compose.yml`, `docker-compose.mysql.yml`.
