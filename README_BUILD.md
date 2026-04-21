# KeiOS Build Guide (EN)

[Main README](README.md)

## Current Install Channel

- This build guide is the primary install path during daily rapid iteration.
- GitHub distribution is source-first, and early adopters install via clone + local build.
- Use the commands in `Common Local Commands` to generate a debug APK.

## Local Build Notes

This repo keeps machine-specific paths and secrets out of VCS on purpose.

### Build Baseline

- Gradle daemon + Java compile + Kotlin JVM target are all aligned to Java 21.
- Cross-platform daemon toolchain metadata is tracked in `gradle/gradle-daemon-jvm.properties` (JetBrains Java 21).
- Android config baseline: `compileSdk=37`, `targetSdk=37`, `minSdk=35`.
- Do not commit local JDK paths or tokens to tracked files.

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
miuix.version=0.9.0-d9dc35b5-SNAPSHOT
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
./gradlew :app:recordRoborazziDebug --tests "com.example.keios.ui.page.main.widget.AppDesignSystemScreenshotTest"

# verify current rendering against baselines
./gradlew :app:verifyRoborazziDebug --tests "com.example.keios.ui.page.main.widget.AppDesignSystemScreenshotTest"
```

Current baseline scope:

- `AppCardHeader`
- `AppOverviewCard`
- unified list-body layout / supporting block rhythm

## GitHub Actions: CI / Debug APK

Workflow: `.github/workflows/ci-debug-apk.yml`

- Trigger: `push` with any commit message containing `Build-CI`.
- Job output: debug APK artifact uploaded to GitHub Actions.
- APK file name format: `KeiOS-debug-YYYYMMDD-HHMMSS-<shortSha>.apk` (UTC).
- Artifact name format: `KeiOS-debug-YYYYMMDD-HHMMSS-<shortSha>-run<run_number>`.

Example commit message:

```text
chore: tune guide cache Build-CI
```

## GitHub Actions: CI / Benchmark APK (Manual)

Workflow: `.github/workflows/ci-benchmark-apk.yml`

- Trigger: manual run via `workflow_dispatch`.
- Optional input: `commit` (commit SHA / branch / tag).
- Default behavior: build latest commit on selected branch when `commit` is empty.
- Build task: `./gradlew :app:assembleBenchmark --stacktrace`.
- Job output: benchmark APK artifact uploaded to GitHub Actions.
- APK file name format: `KeiOS-benchmark-YYYYMMDD-HHMMSS-<shortSha>.apk` (UTC).
- Artifact name format: `KeiOS-benchmark-YYYYMMDD-HHMMSS-<shortSha>-run<run_number>`.

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
./gradlew :app:testDebugUnitTest --tests "com.example.keios.feature.github.data.remote.GitHubStrategyLiveBenchmarkTest"
```

One-off CLI example (without editing local properties):

```bash
./gradlew :app:testDebugUnitTest \
  --tests "com.example.keios.feature.github.data.remote.GitHubStrategyLiveBenchmarkTest" \
  -Dkeios.github.liveBenchmark=true \
  -Dkeios.github.api.token=ghp_xxx \
  -Dkeios.github.liveTargets=topjohnwu/Magisk,neovim/neovim
```

### What This Test Verifies

- Both strategies execute and produce benchmark results.
- Target list is non-empty.
- Warm samples are served from strategy cache.

Because this is a live network benchmark, failures can still come from GitHub API/network/rate-limit conditions.
