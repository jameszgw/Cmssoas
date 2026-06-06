# 运营平台前端高保真原型

精致、大气的软件授权运营平台界面原型（顶部导航的"官网风"布局，非传统左右栏）。

## 已实现的设计要点

| 要求 | 实现 |
|------|------|
| **帮助文案 vs 业务数据 字体区分** | 两套字体变量：`--font-help`（人文无衬线，说明/标签）与 `--font-data`（等宽 + `tabular-nums`，编号/版本/金额/日期）。`.data` 类统一应用于业务数据，并微调字色，一眼可辨；总览页含"排版规范"演示卡 |
| **2K / 4K 自适应** | `html` 根字号 = `15px × var(--scale)`，全站 `rem`；高分屏提高 `--scale`（2K≈1.15，4K≈1.7）整体等比放大；容器 `max-width:1880px` 居中 |
| **换肤** | CSS 变量主题，`html[data-theme]` 切换：`tech`（科技蓝）/`midnight`（暗夜）/`gold`（商务金），含 hero/topbar 渐变、阴影、状态色 |
| **国际化** | `data-i18n` + 字典，中英切换（`?lang=en`），含邮件模板文案 |
| **大气运营平台** | 顶部导航 + 渐变 Hero + KPI 卡 + 趋势面积图/环形图(SVG) + 租户表 + 运营待办；玻璃拟态卡片与柔和阴影 |
| **开通即发邮件** | "开通租户"模态左侧表单 + 右侧**开通邀请邮件实时预览**（含一次性激活链接、有效期、MFA 说明） |

## 预览参数（URL）
`?theme=tech|midnight|gold` · `?lang=zh|en` · `?scale=1.15` · `?modal=open`

## 生成截图
```bash
npm i playwright@1.56.1
PLAYWRIGHT_BROWSERS_PATH=/opt/pw-browsers node shoot.mjs   # 输出到 shots/
```
直接用浏览器打开 `index.html` 亦可交互预览。

> 本原型用于评审视觉与交互；下一步将转为正式工程：Vite + Vue3 + TS + Pinia + vue-i18n + Element Plus，并接入后端"开通即发邮件"接口。
