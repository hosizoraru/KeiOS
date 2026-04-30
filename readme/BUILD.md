# KeiOS Build Guide (EN)

[Main README](../README.md) · [Documentation Index](INDEX.md)

## Install Channels

- Stable installs should use [GitHub Releases](https://github.com/hosizoraru/KeiOS/releases).
- The latest public release is [KeiOS v1.3.0](https://github.com/hosizoraru/KeiOS/releases/tag/v1.3.0).
- This build guide covers local source builds, debug packages, and contributor workflows.
- Use the commands in `Common Local Commands` to generate a debug APK for development or preview validation.

## Local Build Notes

This repo keeps machine-specific paths and secrets out of VCS on purpose.

### Build Baseline

- Gradle daemon + Java compile + Kotlin JVM target are all aligned to Java 21.
- Cross-platform daemon toolchain metadata is tracked in `gradle/gradle-daemon-jvm.properties` (JetBrains Java 21).
- Android config baseline: `compileSdk=37`, `targetSdk=37`, `minSdk=35`.
- Keep local JDK paths and tokens in untracked local config files.

### Versioning

- Release builds use the latest merged semver tag as the base version.
- Debug and benchmark builds use the next patch version plus commit count and short SHA, for example `1.2.5+12.gabcdef0`.
- CI APK file names include variant, ABI, versionName, versionCode, short SHA, run number, and attempt number.

### Required Local Secrets (for dependency resolution)

`settings.gradle.kts` resolves Miuix artifacts from GitHub Packages and expects credentials from local properties/env.
Set these in `~/.gradle/gradle.properties`:

```properties
gpr.user=<your_github_username_or_actor>
gpr.key=<your_github_token_with_packages_read_scope>
```

Fallback env vars are also supported by Gradle config:

- `GITHUB_ACTOR`
- `GITHUB_TOKEN`

### Optional Local Overrides

Use `~/.gradle/gradle.properties` (preferred) or `local.properties` for local-only tuning:

```properties
# Only if JDK auto-resolution fails on your machine
org.gradle.java.home=/path/to/your/jdk

# Optional: pin another Miuix version locally
miuix.version=0.9.0-81ad71b1-SNAPSHOT
```

JDK fallback examples:

- macOS Android Studio JBR: `/Applications/Android Studio.app/Contents/jbr/Contents/Home`
- Windows Android Studio JBR: `C:\\Program Files\\Android\\Android Studio\\jbr`

### Common Local Commands

```bash
# compile check
./gradlew :app:compileDebugKotlin

# full debug apk build
./gradlew :app:assembleDebug

# unit tests
./gradlew :app:testDebugUnitTest
```

### Screenshot Baseline

Shared UI primitives use `Roborazzi` screenshot baselines under `app/src/test/screenshots/design-system`.

```bash
# record / refresh baselines
./gradlew :app:recordRoborazziDebug --tests "os.kei.ui.page.main.widget.AppDesignSystemScreenshotTest"

# verify current rendering against baselines
./gradlew :app:verifyRoborazziDebug --tests "os.kei.ui.page.main.widget.AppDesignSystemScreenshotTest"
```

Current baseline scope:

- `AppCardHeader`
- `AppOverviewCard`
- unified list-body layout / supporting block rhythm

## GitHub Actions: CI / Debug APK

Workflow: `.github/workflows/ci-debug-apk.yml`

- Trigger: `push` on `master`; Markdown/readme-only changes are ignored.
- Manual trigger: `workflow_dispatch` with optional `commit` (commit SHA / branch / tag).
- Job output: debug APK artifact uploaded to GitHub Actions.
- Intended use: quick preview builds for development validation.
- Signing: the shared CI debug keystore in `app/signing/` is used only for debug/benchmark artifacts.
- Retention: 14 days.
- nightly.link: `https://nightly.link/hosizoraru/KeiOS/workflows/ci-debug-apk/master`
- APK file name format: `keios-android-debug-apk-arm64-v8a-v<versionName>-<versionCode>-<shortSha>-run-<run_number>-attempt-<attempt>.apk`.
- Artifact name format: `keios-android-debug-apk-arm64-v8a-v<versionName>-<versionCode>-<shortSha>-run-<run_number>-attempt-<attempt>`.

## GitHub Actions: CI / Benchmark APK

Workflow: `.github/workflows/ci-benchmark-apk.yml`

- Trigger: `push` on `master`; Markdown/readme-only changes are ignored.
- Manual trigger: `workflow_dispatch` with optional `commit` (commit SHA / branch / tag).
- Default behavior: build latest commit on selected branch when `commit` is empty.
- Build task: `./gradlew :app:assembleBenchmark --stacktrace`.
- Job output: benchmark APK artifact uploaded to GitHub Actions.
- Intended use: benchmark / preview verification outside the stable release channel.
- Signing: the shared CI debug keystore in `app/signing/` is used only for debug/benchmark artifacts.
- Retention: 14 days.
- nightly.link: `https://nightly.link/hosizoraru/KeiOS/workflows/ci-benchmark-apk/master`
- APK file name format: `keios-android-benchmark-apk-arm64-v8a-v<versionName>-<versionCode>-<shortSha>-run-<run_number>-attempt-<attempt>.apk`.
- Artifact name format: `keios-android-benchmark-apk-arm64-v8a-v<versionName>-<versionCode>-<shortSha>-run-<run_number>-attempt-<attempt>`.

## GitHub Live Benchmark Test

`GitHubStrategyLiveBenchmarkTest` is an opt-in network test that compares Atom vs API strategy behavior against live repositories.

### Enable Gate (default is disabled)

The test runs only when `keios.github.liveBenchmark=true`.
Lookup order:

1. JVM system properties
2. Environment variables
3. `~/.gradle/gradle.properties`

### Local Keys

```properties
keios.github.liveBenchmark=true
keios.github.api.token=ghp_xxx
keios.github.liveTargets=topjohnwu/Magisk,neovim/neovim,shadowsocks/shadowsocks-android
keios.github.forceGuest=false
```

Notes:

- `keios.github.liveTargets` is optional (built-in defaults are used if omitted).
- `keios.github.forceGuest=true` forces guest mode even if token exists.
- `gpr.key` is accepted as fallback token.

### Run

```bash
./gradlew :app:testDebugUnitTest --tests "os.kei.feature.github.data.remote.GitHubStrategyLiveBenchmarkTest"
```

One-off CLI example (without editing local properties):

```bash
./gradlew :app:testDebugUnitTest \
  --tests "os.kei.feature.github.data.remote.GitHubStrategyLiveBenchmarkTest" \
  -Dkeios.github.liveBenchmark=true \
  -Dkeios.github.api.token=ghp_xxx \
  -Dkeios.github.liveTargets=topjohnwu/Magisk,neovim/neovim
```

### What This Test Verifies

- Both strategies execute and produce benchmark results.
- Target list is non-empty.
- Warm samples are served from strategy cache.

Because this is a live network benchmark, failures can still come from GitHub API/network/rate-limit conditions.
