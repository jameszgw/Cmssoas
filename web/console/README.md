# CODEMAN 控制台（正式工程）

软件授权运营平台前端，由原型转为正式工程。

## 技术栈
Vite 5 · Vue 3 · TypeScript · Vue Router 4 · Pinia 2 · vue-i18n 9 · Element Plus 2 · Axios

## 已落地能力
| 能力 | 实现 |
|------|------|
| **换肤** | `stores/theme.ts` + `styles/app.css` 三套主题（科技蓝/暗夜/商务金），暗夜联动 Element Plus 官方 dark css-vars，EP 组件随主题变色 |
| **国际化** | `i18n/` 中英双语，`stores/locale.ts` 持久化，联动 Element Plus 组件语言 |
| **2K/4K 自适应** | `composables/useScreenScale.ts` 按视口设置根字号 `--scale`，全站 rem 等比放大；容器随视口铺满 |
| **字体区分** | `--font-help`（帮助文案）/`--font-data`（业务数据，等宽+tabular-nums），`.data` 类统一用于编号/版本/金额/日期 |
| **大气运营平台** | 顶部导航布局 + 渐变 Hero + KPI + 趋势/环形/条形图 + 表格 + 待办 + 快捷操作 |
| **开通即发邮件** | `OnboardTenantDialog.vue` 表单 + 实时邮件预览；`api/tenant.ts` 调 `POST /api/tenants`（后端执行建库/初始化并发送开通邮件） |

## 目录
```
src/
├── api/            request(axios) · tenant · mock
├── components/     ThemeSwitcher · LangSwitcher · OnboardTenantDialog · charts/
├── composables/    useScreenScale (2K/4K)
├── i18n/           locales/zh-CN · en-US
├── layouts/        DefaultLayout (顶部导航)
├── router/         路由
├── stores/         theme · locale (Pinia)
├── styles/         app.css (设计系统/主题/EP 变量映射)
├── types/          领域类型
└── views/          Overview · Tenants · Licensing/Products/Plans/Audit
```

## 运行
```bash
npm install
npm run dev       # http://localhost:5173
npm run build     # 类型检查 + 产物
npm run preview
```

## 对接后端
`api/tenant.ts` 中 `USE_MOCK = true` 为演示数据；后端就绪后置为 `false`，
`vite.config.ts` 已将 `/api` 代理到 `http://localhost:8080`。
开通接口约定：`POST /api/tenants` → `{ code, emailSent, email }`，后端负责
建库 / 初始化 / 创建超管 / **发送开通邮件**。
