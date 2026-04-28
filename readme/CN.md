# KeiOS

[English Version](../README.md)

构建文档：
- [文档索引](INDEX.md)
- [Build Guide (EN)](BUILD.md)
- [构建指南 (CN)](BUILD_CN.md)
- [Todo List (EN)](TODO.md)
- [待办清单 (CN)](TODO_CN.md)

## 当前分发方式

- 稳定版安装包已发布到 [GitHub Releases](https://github.com/hosizoraru/KeiOS/releases)。
- 当前公开正式版本为 [KeiOS v1.1.0](https://github.com/hosizoraru/KeiOS/releases/tag/v1.1.0)。
- 正式版基线：`os.kei`、`arm64-v8a`、`Android 15+`（`minSdk 35`）。
- 对于源码用户、贡献者和尝鲜验证场景，依旧推荐 clone 仓库后按构建指南本地构建。
- `CI / Benchmark APK` 工作流（`.github/workflows/ci-benchmark-apk.yml`）依旧支持手动构建 Benchmark APK，并上传可下载 Artifact。

## 项目概览

- 这是一个聚焦系统工具、GitHub 跟踪、BA 内容页的 Android 应用。
- UI 技术栈：Compose + Miuix KMP + Lifecycle ViewModel Compose + 液态玻璃风格组件。
- 运行基线：`minSdk=35`、`targetSdk=37`、Java/Kotlin 工具链统一 Java 21。

## 运行配置入口（最近变更）

- `设置 > 视觉与交互`
  主题模式、过渡动画、ActionBar 分层样式、液态底栏、卡片按压反馈、主页图标 HDR 高光、非 Home 页面背景图。
- `设置 > 通知与兼容`
  超级岛通知样式开关、HyperOS 兼容绕过开关。
- `设置 > 复制与文本选择`
  轻量条目复制模式、完整系统选区模式。
- `设置 > 缓存`
  GitHub / MCP / OS / BA 等模块的缓存诊断摘要。
- `BA 页面 > BA 配置`
  AP 设置、媒体设置、媒体自适应旋转、自定义媒体保存位置。
- `GitHub 页面 > 检查与下载 / 跟踪编辑`
  刷新间隔、预发策略、下载器路由、最新发布下载行为。
