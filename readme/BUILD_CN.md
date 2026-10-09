# KeiOS 构建指南 (CN)

<!-- markdownlint-disable MD013 -->

[主 README](CN.md) · [文档索引](INDEX.md)

## 安装方式

- 稳定安装建议直接使用 [GitHub Releases](https://github.com/hosizoraru/KeiOS/releases)。
- 公开稳定版通过 [最新稳定版](https://github.com/hosizoraru/KeiOS/releases/latest) 获取。
- v1.17.0 包含动态回忆大厅、学生 3D 工具、搜索修复与按包名识别的预发行版追踪。
  六段 Baseline Profile 已在 HyperOS Phone 完成，并在 Pad 鉴赏适配后使用 HyperOS Pad 重新完整采集；正式签名 APK 可从 [v1.17.0](https://github.com/hosizoraru/KeiOS/releases/tag/v1.17.0) 下载。
- 本构建指南覆盖源码本地构建、Debug 包生成和贡献者开发流程。
- 使用 `常用本地命令` 中的命令即可产出 Debug、Benchmark 与 Release APK。

## 本地构建说明（Local Build Notes）

本项目有意将机器相关路径与密钥排除在版本控制之外。

### 构建基线

- Gradle daemon、Java 编译、Kotlin JVM 目标统一为 Java 21。
- 跨平台 daemon toolchain 配置已在 `gradle/gradle-daemon-jvm.properties` 中跟踪（JetBrains Java 21）。
- Android 构建基线：`compileSdk=37`、`targetSdk=37`、`minSdk=35`。API 37 有次版本，本项目固定
  `compileSdkMinor = 0`，即针对 **37.0**（SDK 扩展级别 22）编译，而不是本地 SDK 恰好装了哪个 37.x。
- Gradle Wrapper：`9.8.0`；Kotlin 插件：`2.4.21-RC`；Android Gradle Plugin：`9.4.1`；
  Compose 运行库：`1.12.1`；Ktor：`3.6.0`。
- Release APK 读取 `app/src/release/generated/baselineProfiles/` 中已生成的 Baseline Profiles。
  Benchmark 构建会接入同一份 profile 目录，用于预发行性能验证。
- 本地 JDK 路径与 Token 保留在未跟踪的本机配置文件中。

### 共享构建逻辑

根 settings 通过 `includeBuild` 接入独立的 `build-logic`。约定插件统一维护 Android SDK/JDK
默认值、Compose 编译器设置、MIUIX 版本覆盖与 Android 产物替换、单元测试 JVM exports，
以及 Release/Diagnostic 变体配对。各模块显式应用对应插件，命名空间、依赖、签名和特殊
构建类型仍由模块维护。独立构建导入主项目的版本目录，AGP/Kotlin 版本只需在一处更新。

Git 版本元数据辅助代码和 Profile 采集完整性校验任务也位于该目录。可使用
`./gradlew -p build-logic check` 在没有设备时验证插件及其测试。插件职责和配置缓存检查见
[构建逻辑说明](../build-logic/README.md)。这个命令不会执行 Baseline Profile 采集。

### 版本号规则

- CI 会在 Gradle 外根据当前 HEAD 已合入的最新 semver tag 和当前发布目标注入版本元数据。
- 本地构建可在 `~/.gradle/gradle.properties` 或 `local.properties` 覆盖
  `keios.version.name`、`keios.nextVersion.name`、`keios.version.anchorTag` 与 `keios.git.*`。
- Release 构建使用最新已合入 semver tag 与当前发布目标之间更新的版本，例如 `1.17.0`。
- Debug / Benchmark 构建使用下一 patch 版本，并追加 commit 数和短 SHA，例如 `1.17.1+12.gabcdef0`。
- 缺少 CI 注入 metadata 的本地构建会直接读取 git metadata，以最新已合入 tag 作为 commit 数锚点，并在发布目标更新时使用发布目标作为 release base。
- 包名链路保持精简：Debug 安装为 `os.kei.debug`；Benchmark 与 Release 安装为 `os.kei`。
- 当前 CI artifact 名称保持简洁：`KeiOS_<versionName>`，APK 文件名为 `KeiOS_<versionName>.apk`。
- workflow run name 会用 `D#<run_number>` 表示 Debug、`B#<run_number>` 表示 Benchmark；job summary
  会输出完整 commit SHA、run number、versionName、versionCode、application ID 与 artifact digest。

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
miuix.version=0.9.4-5c91d5e5-SNAPSHOT
```

Miuix 迭代很快。遇到疑似 Miuix 的问题时，先确认当前 pin 是不是最新快照再动手改，很多问题上游
已经修好了：

```bash
scripts/deps/miuix_snapshot_check.sh
```

脚本会输出生效的 pin 及其来源文件、最新已发布快照，以及两者之间的库侧提交。`--diff PullToRefresh`
打印单个组件的 patch，`--update` 直接把 pin 移到最新。退出码：0 为最新，1 为落后。

其余依赖有一个同类脚本，它直接读 catalog，而不是靠写死的清单：

```bash
scripts/deps/catalog_freshness.sh
```

它按本项目一贯的升级规则判断——取最新的 RC 或正式版，新版本线的 rc 压过旧的正式版，同一条线上
正式版压过 rc——并且拒绝把两种情况误报成可升级：pin 已经比仓库发布的更新，以及 pin 根本不是一个
可排序的正式版本号。`--all` 列出全部判定，`--update` 移动落后的 pin 并指出还在引用旧版本号的文档，
`--self-test` 不联网地自检比较器。退出码：0 为最新，1 为落后。

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

# 汇总上一次测试运行的结果（覆盖全部模块）
scripts/qa/test_report.sh
```

`test_report.sh` 读的是 Gradle 已经写好的 XML，因此可以反复运行且不需要重新构建。它按模块打印
总数；一旦有失败，它按原因聚类而不是逐条罗列——系统性问题往往表现为几百条日志里埋着同一个原因。
`--slowest 10` 排出最慢的测试类，`--all` 展示全部原因，`--module app` 只看一个模块。退出码：
0 全绿，1 有失败。

### 验证用 AVD

```bash
scripts/dev/avd_phone.sh     # Android 17 手机（KeiOS_API37_Validation）
scripts/dev/avd_tablet.sh    # Android 17 平板（KeiOS_Pad_API37_Validation）
```

两者都会在 AVD 未启动时先拉起，构建 `:app:assembleDebug` 与 `:app:assembleBenchmarkRelease`，装上去，
然后把设备上的包读回来，拿 sha256 与刚构建的 APK 比对。关键在这次回读：它是去**验证**模拟器上跑的
是不是当前这棵树，而不是声称如此。版本号答不了这个问题——它带的是 git 描述，同一棵树的两次构建版本号
完全相同。

`--no-build` 跳过 Gradle 只做校验，是问这个问题最省的方式；带构建时 APK 每次都会重新打包，所以安装
不会被跳过。`--launch` 装完拉起 Debug 应用，`--only debug|bench` 只处理其一，`--headless` 无窗口启动，
`--wipe` 冷启动并清空该 AVD，`--reinstall` 用于签名不一致时先卸载（连数据一起）。两者都包装
`scripts/dev/avd_up.sh`，选项和退出码都在那里：0 校验通过，1 不一致，2 参数错误，3 缺少前置，
4 启动超时。

脚本只会操作名字以 `KeiOS` 开头的 AVD，`--avd` 和 `AVD_PHONE` / `AVD_TABLET` 覆盖同样受这条约束。
本机的 AVD 列表是多个项目共用的，两个项目同时驱动一台模拟器，结果就是彼此的包都装到了对方那里。

**BenchRelease 会覆盖 release 应用。** `benchmarkRelease` 是 `initWith(release)`，而 release 没有声明
`applicationIdSuffix`，所以它的包名就是 `os.kei`——不像 `debug`（`.debug`）和 `releaseDiagnostic`
（`.diag`）。在验证用 AVD 上这正是本意；在随身手机上不是，因此非模拟器目标默认拒绝，必须显式传
`--allow-physical`。

### v1.17.0 发布门禁

发布准备阶段可以同步文案、版本目标，并建立未推送的本地预备 tag。采集和产物验证通过后，
再让 tag 指向最终源码并发布；被其他任务占用的 AVD 不参与这些检查。

先完成不依赖设备的检查：

```bash
./gradlew :build-logic:check testDebugUnitTest -Proborazzi.test.verify=true --continue --stacktrace
git diff --check
```

选定 AVD 空闲后，显式绑定设备采集：

```bash
ANDROID_SERIAL=<空闲-avd-serial> ./gradlew :app:generateReleaseBaselineProfile
```

权限授权是采集前提。安装两个独立采集包后，采集器会先授予目标系统支持的全部已声明运行时权限、
对应 AppOps 及 HyperOS 的应用列表访问权限，并回读核对。必要授权失败时立即停止；授权仅作用于
`os.kei.profilecapture`，后续采集保留授权，正式版、调试版和诊断版的权限配置不变。
采集期间临时开启勿扰并恢复原模式；开始前记录 `zen_mode` 和窗口设置，以便异常中断后恢复。

核对六段旅程、有效且新鲜的导出及两份生成源文件。先提交已验收的采集结果和最终运行时修复，
再运行比较已提交引用的新鲜度门禁：

```bash
scripts/qa/baseline_profile_freshness.sh
./gradlew :app:lintVitalRelease :app:assembleRelease :app:assembleBenchmark
```

本次发布重点复查：

- 搜索：连续输入、中间插入、文字选择、中文输入法组词保留光标和编辑状态；键盘切换时弹层模糊连续。
- GitHub：仅预发行仓库开启选项后可扫描与追踪；API、Atom、刷新与重启后，主应用和插件按各自包名判断版本。
- 学生图鉴：有资源时 MP4 与 Spine 入口共存；沉浸和重置视角不中断动画；BGM 静音及后台行为正确。
  3D 模型与服装对应、视角、进度、速度、循环、描边、背景保存和控件对比度正确。
- 冷启动深浅色背景及入口状态正确，关闭过渡动画仍正常；玻璃材质和组件动效保留。
- 新鲜度门禁对已提交源码和依赖报告 fresh，未提交运行时变更另行核对；生成文件非空且保留采集计划的路径。
  规则数量只记录当次采集，不作为固定发布门禁。
- 正式 APK 的签名、1.17.0 / versionCode 11700999、R8/lint，以及 assets/dexopt/baseline.prof 和 baseline.profm 完成核对。
- 让未推送的本地 v1.17.0 tag 指向最终验收提交，复核 tag 目标与 APK 元数据；不强制替换已发布的 tag。
- 使用 [Release Notes v1.17.0](RELEASE_V1.17.0.md)，检查通过后移除发布准备提示，发布签名 APK 和校验文件。

### 截图基线

共享 UI 基础组件已接入 `Roborazzi`，基线图位于 `app/src/test/screenshots/design-system`。

```bash
# 录制 / 刷新截图基线
./gradlew :app:recordRoborazziDebug --tests "os.kei.ui.page.main.widget.AppDesignSystemScreenshotTest"

# 校验当前渲染结果是否与基线一致
./gradlew :app:verifyRoborazziDebug --tests "os.kei.ui.page.main.widget.AppDesignSystemScreenshotTest"
```

当前基线覆盖范围：

- `AppCardHeader`
- `AppOverviewCard`
- 统一后的列表正文骨架与说明块节奏

## GitHub Actions：CI / Debug APK

工作流路径：`.github/workflows/ci-debug-apk.yml`

- Runner：APK 与测试 job 均使用 `ubuntu-26.04`。显式指定 LTS 版本，避免
  `ubuntu-latest` 自动迁移操作系统；Ubuntu 26.04 镜像本身仍会更新。Java 21、Gradle Wrapper
  和所需 Android SDK 组件由构建步骤显式配置。
- 触发方式：`master` 分支 `push` 与非 draft `pull_request`；仅 Markdown/readme 变更会跳过。
- 手动触发：`workflow_dispatch`，可选 `commit`（commit SHA / branch / tag）。
- 构建产物：自动构建并上传 Debug APK 到 GitHub Actions。
- 使用场景：开发过程中的快速预览与验证。
- 签名：仅 Debug / Benchmark artifact 使用 `app/signing/` 内的共享 CI debug keystore。
- 保留期：14 天。
- nightly.link：`https://nightly.link/hosizoraru/KeiOS/workflows/ci-debug-apk/master`
- APK 文件名格式：`KeiOS_<versionName>.apk`。
- Artifact 名称格式：`KeiOS_<versionName>`。

## GitHub Actions：CI / Benchmark APK

工作流路径：`.github/workflows/ci-benchmark-apk.yml`

- Runner：`ubuntu-26.04`，与 Debug 和测试 job 使用相同的 Java、Gradle、Android 配置。
- 触发方式：`master` 分支 `push`；仅 Markdown/readme 变更会跳过。
- 手动触发：`workflow_dispatch`，可选 `commit`（commit SHA / branch / tag）。
- 默认行为：`commit` 为空时构建所选分支的最新提交。
- 构建任务：`./gradlew :app:assembleBenchmark --stacktrace`。
- 构建产物：自动上传 Benchmark APK 到 GitHub Actions Artifact。
- 使用场景：以接近 Release 的 R8、资源收缩和 Baseline Profile 链路做预发行性能验证。
- 签名：CI Benchmark artifact 使用 `app/signing/` 内的共享 debug keystore；本地签名构建可复用 release 签名。
- 保留期：14 天。
- nightly.link：`https://nightly.link/hosizoraru/KeiOS/workflows/ci-benchmark-apk/master`
- APK 文件名格式：`KeiOS_<versionName>.apk`。
- Artifact 名称格式：`KeiOS_<versionName>`。

## GitHub 实时基准测试（GitHub Live Benchmark Test）

`GitHubStrategyLiveBenchmarkTest` 是一个按需启用的联网测试，用于对比 Atom 与 API 两种策略在真实仓库上的行为。它覆盖
release 读取、策略缓存 warm 样本、从 release APK 扫描包名、订阅项目检查，以及按包名反扫仓库。

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
./gradlew :feature-github:testDebugUnitTest --tests "os.kei.feature.github.data.remote.GitHubStrategyLiveBenchmarkTest"
```

一次性命令示例（不改本地配置文件）：

```bash
KEIOS_GITHUB_API_TOKEN=ghp_xxx ./gradlew :feature-github:testDebugUnitTest \
  --tests "os.kei.feature.github.data.remote.GitHubStrategyLiveBenchmarkTest" \
  -Dkeios.github.liveBenchmark=true \
  -Dkeios.github.liveTargets=topjohnwu/Magisk,neovim/neovim
```

token 通过环境变量传入，而不是 `-D`：`-D` 的值会被写进 Gradle 的配置缓存，所以
`keios.github.api.token` 不会转发给测试进程。

### 此测试验证内容

- 两种策略都执行并产出基准结果。
- 目标仓库列表非空。
- warm 阶段样本来自策略缓存。
- 包名扫描样本能从 release APK 的 AndroidManifest 数据中读取 package ID。
- 仓库反扫样本能按包名 / 应用名搜索候选，并通过 APK 包名扫描验证匹配结果。

该测试依赖实时网络，请考虑 GitHub API 限流、网络波动等外部因素导致的偶发失败。
