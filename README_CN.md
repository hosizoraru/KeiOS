# KeiOS

[English Version](README.md)

## 项目概览

- 这是一个聚焦系统工具、GitHub 跟踪、BA 内容页的 Android 应用。
- UI 技术栈：Compose + Miuix KMP + 液态玻璃风格组件。
- 运行基线：`minSdk=35`、`targetSdk=37`、Java/Kotlin 工具链统一 Java 21。

## 本地构建说明（Local Build Notes）

本项目有意将机器相关路径与密钥排除在版本控制之外。

### 构建基线

- Gradle daemon、Java 编译、Kotlin JVM 目标统一为 Java 21。
- 跨平台 daemon toolchain 配置已在 `gradle/gradle-daemon-jvm.properties` 中跟踪（JetBrains Java 21）。
- Android 构建基线：`compileSdk=37`、`targetSdk=37`、`minSdk=35`。
- 不要把本地 JDK 路径或 Token 写入受版本控制的文件。

### 必需的本地凭据（依赖解析）

`settings.gradle.kts` 通过 GitHub Packages 拉取 Miuix 依赖，需要本地凭据。请在 `~/.gradle/gradle.properties` 中配置：

```properties
gpr.user=<你的_github_用户名_或_actor>
gpr.key=<具备_packages_read_权限的_github_token>
```

Gradle 配置也支持环境变量兜底：

- `GITHUB_ACTOR`
- `GITHUB_TOKEN`

### 可选本地覆盖项

推荐通过 `~/.gradle/gradle.properties`（优先）或 `local.properties` 做本机覆盖：

```properties
# 仅在你的机器无法自动解析 JDK 时再设置
org.gradle.java.home=/path/to/your/jdk

# 可选：本地覆盖 Miuix 版本
miuix.version=0.9.0-d9dc35b5-SNAPSHOT
```

JDK 兜底示例路径：

- macOS Android Studio JBR：`/Applications/Android Studio.app/Contents/jbr/Contents/Home`
- Windows Android Studio JBR：`C:\\Program Files\\Android\\Android Studio\\jbr`

### 常用本地命令

```bash
# Kotlin 编译检查
./gradlew :app:compileDebugKotlin

# 构建 Debug APK
./gradlew :app:assembleDebug

# 运行单元测试
./gradlew :app:testDebugUnitTest
```

### 截图基线

共享 UI 基础组件已接入 `Roborazzi`，基线图位于 `app/src/test/screenshots/design-system`。

```bash
# 录制 / 刷新截图基线
./gradlew :app:recordRoborazziDebug --tests "com.example.keios.ui.page.main.widget.AppDesignSystemScreenshotTest"

# 校验当前渲染结果是否与基线一致
./gradlew :app:verifyRoborazziDebug --tests "com.example.keios.ui.page.main.widget.AppDesignSystemScreenshotTest"
```

当前基线覆盖范围：

- `AppCardHeader`
- `AppOverviewCard`
- 统一后的列表正文骨架与说明块节奏

## 运行配置入口（最近变更）

- `设置 > 视觉与交互`  
  主题模式、ActionBar 分层样式、液态底栏、卡片按压反馈、主页图标 HDR 高光。
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

## GitHub Actions：Build-CI 自动构建调试包

工作流路径：`.github/workflows/build-debug-on-message.yml`

- 触发方式：`push` 事件中任一 commit message 包含 `Build-CI`。
- 构建产物：自动构建并上传 Debug APK 到 GitHub Actions。
- APK 文件名格式：`KeiOS-debug-YYYYMMDD-HHMMSS-<shortSha>.apk`（UTC 时间）。
- Artifact 名称格式：`keiOS-debug-apk-<run_number>`。

示例提交信息：

```text
chore: tune guide cache Build-CI
```

## GitHub 实时基准测试（GitHub Live Benchmark Test）

`GitHubStrategyLiveBenchmarkTest` 是一个**按需启用**的联网测试，用于对比 Atom 与 API 两种策略在真实仓库上的行为。

### 启用开关（默认关闭）

仅当 `keios.github.liveBenchmark=true` 时执行。读取优先级如下：

1. JVM 系统属性
2. 环境变量
3. `~/.gradle/gradle.properties`

### 本地参数

```properties
keios.github.liveBenchmark=true
keios.github.api.token=ghp_xxx
keios.github.liveTargets=topjohnwu/Magisk,neovim/neovim,shadowsocks/shadowsocks-android
keios.github.forceGuest=false
```

说明：

- `keios.github.liveTargets` 可省略，省略时使用内置默认仓库。
- `keios.github.forceGuest=true` 会在有 token 的情况下仍强制走游客模式。
- `gpr.key` 也可作为 `keios.github.api.token` 的兜底值。

### 运行方式

```bash
./gradlew :app:testDebugUnitTest --tests "com.example.keios.feature.github.data.remote.GitHubStrategyLiveBenchmarkTest"
```

一次性命令示例（不改本地配置文件）：

```bash
./gradlew :app:testDebugUnitTest \
  --tests "com.example.keios.feature.github.data.remote.GitHubStrategyLiveBenchmarkTest" \
  -Dkeios.github.liveBenchmark=true \
  -Dkeios.github.api.token=ghp_xxx \
  -Dkeios.github.liveTargets=topjohnwu/Magisk,neovim/neovim
```

### 此测试验证内容

- 两种策略都执行并产出基准结果。
- 目标仓库列表非空。
- warm 阶段样本来自策略缓存。

该测试依赖实时网络，请考虑 GitHub API 限流、网络波动等外部因素导致的偶发失败。
