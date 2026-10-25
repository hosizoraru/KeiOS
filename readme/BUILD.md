# KeiOS Build Guide (EN)

<!-- markdownlint-disable MD013 -->

[Main README](../README.md) · [Documentation Index](INDEX.md) · [CI Workflow Guide](../docs/build/github-actions.md)

## Install Channels

- Stable installs should use [GitHub Releases](https://github.com/hosizoraru/KeiOS/releases).
- The public stable channel resolves through [Latest Stable Release](https://github.com/hosizoraru/KeiOS/releases/latest).
- [v1.19.0](https://github.com/hosizoraru/KeiOS/releases/tag/v1.19.0) is published, with improvements
  to BGM playback, launcher icons, imports, and unfinished edits. See its
  [bilingual notes](RELEASE_V1.19.0.md) and [release gate](#v1190-release-gate).
  Use the stable-channel link above for future public updates.
- This build guide covers local source builds, debug packages, and contributor workflows.
- Use the commands in `Common Local Commands` to generate debug, benchmark, and release APKs.

## Local Build Notes

This repo keeps machine-specific paths and secrets out of VCS on purpose.

### Build Baseline

- Gradle daemon + Java compile + Kotlin JVM target are all aligned to Java 21.
- Cross-platform daemon toolchain metadata is tracked in `gradle/gradle-daemon-jvm.properties` (JetBrains Java 21).
- Android config baseline: `compileSdk=37`, `targetSdk=37`, `minSdk=35`. API 37 ships minor
  revisions, and this app pins `compileSdkMinor = 0` — it compiles against **37.0** (SDK extension
  22), not whichever 37.x the local SDK happens to have installed.
- Gradle Wrapper: `9.8.2`; Kotlin plugin: `2.4.21`; Android Gradle Plugin: `9.5.0-rc01`;
  Compose runtime: `1.12.1`; Ktor: `3.6.0`.
- Release APKs read generated Baseline Profiles from `app/src/release/generated/baselineProfiles/`.
  The benchmark build wires the same profile directory so pre-release performance checks exercise
  the release profile path.
- Keep local JDK paths and tokens in untracked local config files.

### Shared Build Logic

The root settings include an independent `build-logic` build. Its convention plugins own shared
Android SDK/JDK defaults, Compose compiler settings, MIUIX override/substitution rules, unit test JVM
exports, and release/diagnostic variant pairing. Each module applies the appropriate convention
explicitly; module namespaces, dependencies, signing and special build types stay in that module.
The included build imports the main version catalog, so AGP/Kotlin versions have one source.

Git version helpers and the profile-capture verification task also live there. Validate them without
a device using `./gradlew -p build-logic check`. See [Build Logic](../build-logic/README.md) for plugin
ownership and configuration-cache checks. This command does not generate Baseline Profiles.

### Versioning

- CI injects version metadata through Gradle properties generated from the latest merged semver tag
  and the current release target.
- Local builds can override `keios.version.name`, `keios.nextVersion.name`,
  `keios.version.anchorTag`, and `keios.git.*` in `~/.gradle/gradle.properties` or `local.properties`.
- Release builds use the newer value between the latest merged semver tag and the current release
  target, currently `1.19.0` (`versionCode 11900999`).
- Debug and benchmark builds use the next patch version plus commit count and short SHA, for example
  `1.19.1+12.gabcdef0`.
- Local builds resolve git metadata directly when CI metadata is absent, using the latest merged tag
  as the commit-count anchor and the current release target as the release base when it is newer.
- Package chains stay compact: debug installs as `os.kei.debug`; benchmark and release install as `os.kei`.
- Current CI artifact names include the build variant: `KeiOS_<versionName>_<variant>` with an APK
  file named `KeiOS_<versionName>_<variant>.apk`.
- Workflow run names include `D#<run_number>` for debug and `B#<run_number>` for benchmark, and job
  summaries print the full commit SHA, run number, versionName, versionCode, application ID, and
  APK SHA-256, and uploaded artifact archive SHA-256.

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
miuix.version=0.9.4-685dceb7-SNAPSHOT
```

For the optional HONOR order-feedback client, set `keios.oem.honor.appId` to the
real platform application ID. An empty value keeps its enable flag false and
prevents transmission. Scene/business values remain explicit integration inputs;
this does not enable every notification as a YOYO card. See the
[OEM implementation and verification notes](../docs/planning/oem-adaptation-implementation-2026-10-23.md).

Miuix iterates fast. When something Miuix-shaped misbehaves, check whether the pin is current
before writing a workaround — the fix is often already upstream:

```bash
scripts/deps/miuix_snapshot_check.sh
```

It reports the effective pin and which file supplies it, the newest published snapshot, and the
library commits in between. `--diff PullToRefresh` prints the patch for one component, `--update`
moves the pin. Exit codes: 0 up to date, 1 behind.

GitHub Packages can order hash-named snapshots lexically in Maven metadata. Use publication times
(as the script does), rather than the Maven `<latest>` field, to identify the newest snapshot.

Every other pin has a sibling script, which reads the catalog itself rather than a hardcoded list:

```bash
scripts/deps/catalog_freshness.sh
```

It applies the rule this project bumps by — newest release candidate or stable, so a newer line's
rc beats an older final but a final beats its own rc — and refuses to call two things upgrades that
are not: a pin already ahead of what the repository publishes, and a pin that is not a plain
release at all. `--all` lists everything it considered, `--update` moves the behind pins and then
names any doc still quoting an old version, and `--self-test` checks the comparator without
touching the network. Exit codes: 0 current, 1 behind.

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

# what the last test run actually produced, across every module
scripts/qa/test_report.sh
```

`test_report.sh` reads the XML Gradle already wrote, so it is free to re-run and needs no rebuild.
It prints per-module totals and, when something failed, groups the failures by cause instead of
listing them one by one — a systemic breakage arrives as hundreds of lines with one cause buried in
each. `--slowest 10` ranks the slowest classes, `--all` shows every cause, `--module app` narrows
it. Exit codes: 0 green, 1 failures.

### Validation AVDs

```bash
scripts/dev/avd_phone.sh     # Android 17 Phone  (KeiOS_API37_Validation)
scripts/dev/avd_tablet.sh    # Android 17 Tablet (KeiOS_Pad_API37_Validation)
```

Each boots its AVD if it is not already up, builds `:app:assembleDebug` and
`:app:assembleBenchmarkRelease`, installs both, and then reads the packages back off the device and
compares their sha256 against the APKs it just built. The point is the read-back: it answers "is
what is running on that emulator what is in this tree" rather than asserting it. A version name
cannot answer that — it carries a git description, so two builds of the same tree share one.

`--no-build` skips Gradle and just verifies, which is the cheap way to ask the question; with a
build the APK is repackaged every time, so the install never gets skipped. `--launch` starts the
debug app, `--only debug|bench` narrows it, `--headless` boots with no window, `--wipe` cold boots
and erases that AVD, `--reinstall` replaces a package signed with a different key and its data.
Both wrap `scripts/dev/avd_up.sh`, which carries the options and the exit codes: 0 verified,
1 mismatch, 2 bad usage, 3 missing prerequisite, 4 boot timeout.

Only AVDs named `KeiOS*` are touched, `--avd` and the `AVD_PHONE` / `AVD_TABLET` overrides
included. This machine's AVD list is shared with other projects, and driving one emulator from two
of them leaves each with the other's packages on it.

**BenchRelease installs over the release app.** `benchmarkRelease` does `initWith(release)` and
release declares no `applicationIdSuffix`, so it is `os.kei` — unlike `debug` (`.debug`) and
`releaseDiagnostic` (`.diag`). On a validation AVD that is the intent; on a phone you carry it is
not, so a non-emulator target is refused unless `--allow-physical` is passed.

### Data-preserving Baseline Profile collection

Bind one device and run the explicit collection task:

```bash
ANDROID_SERIAL=<selected-target> ./gradlew :app:generateReleaseBaselineProfile
```

Collection uses `nonMinifiedRelease` as `os.kei.profilecapture` and a separate
self-instrumenting producer `os.kei.baselineprofile.capture`. Both are disposable;
release, debug, diagnostic and the older `os.kei.baselineprofile` installation
remain separate. Verify those identities and existing device/package state before
a physical run. Never substitute `benchmarkRelease` for this collector.

After installation, the producer authorizes all declared runtime permissions available
on the target, their associated AppOps, and HyperOS's installed-app query gate before
any journey. It verifies readback and fails immediately if a required grant cannot be
established. Grants apply only to the disposable collector and remain available for
later runs when the collector installation is retained; existing release/debug/diagnostic permissions
are preserved. Priority DND is enabled during journeys and the previous mode is restored afterward.
Snapshot `zen_mode` along with window settings for recovery if instrumentation is interrupted.

The six journeys have a maximum of 16 replays per device. To collect a Phone/Tablet
matrix in one task, bind both with `ANDROID_SERIAL=<phone-serial>,<tablet-serial>`;
other connected devices are excluded. The producer requires six passing tests and
fresh nonempty outputs for every selected device before copying profiles into release sources,
so a connected-runner transport error reporting zero tests cannot erase accepted
profiles. Check actual test results and generated outputs, then build release and
verify both `assets/dexopt/baseline.prof` and `baseline.profm`. See
[the collection plan](../docs/planning/baseline-profile-coverage.md) for coverage and
device restoration. Collection success is separate from a measured speedup.

Audit the complete fresh merge before packaging:

```bash
python3 scripts/qa/baseline_profile_rules.py \
  --capture-dir baselineprofile/build/outputs/connected_android_test_additional_output/nonMinifiedRelease \
  --capture-start-marker baselineprofile/build/intermediates/profile-capture-start.txt
```

This checks all six exports per device, exact signature/flag merging, startup
membership, duplicate and capture-only rules, and the retained Liquid Sheet paths.

### v1.19.0 Release Gate

Release preparation updates notes and version targets. Commit, tag, push, and publication each
require explicit authorization; this checklist does not authorize them.
An occupied AVD must not be used for these checks. Reuse accepted collection results only while
their runtime and dependency inputs remain valid; version/copy edits still require final artifact
checks. Earlier 1.18.0 validation APKs do not establish 1.19.0 artifact acceptance.

Run the device-independent checks first:

```bash
./gradlew :build-logic:check testDebugUnitTest -Proborazzi.test.verify=true --continue --stacktrace
git diff --check
```

If collection inputs changed or accepted capture evidence is missing, collect on an available
AVD with an explicit serial:

```bash
ANDROID_SERIAL=<available-avd-serial> ./gradlew :app:generateReleaseBaselineProfile
```

Verify all six journeys, fresh nonempty exports, and both generated source files. The
[coverage record](../docs/planning/baseline-profile-coverage.md) records the accepted API 37
HyperOS Phone collection. Once authorized commits include accepted outputs and final runtime
fixes, apply the committed-ref freshness gate; account separately for uncommitted runtime changes:

```bash
scripts/qa/baseline_profile_freshness.sh
./gradlew :app:lintVitalRelease :app:assembleRelease :app:assembleBenchmark
```

Recommended focused checks for this release:

- Media: artwork matches student identity; native notification reentry retains an unfavorited
  track, playlist, and paused position. Check Next, Stop, background return, and audio-focus behavior.
- Imports: one readable document, cancellation/retry, unchanged domain validation, and temporary
  source access for background cropping.
- Drafts/window policy: restore after data loads, reject stale edit targets, retain failed-save
  input, clear on cancellation, and reevaluate orientation after resize. Distinguish automated
  coverage from actual OEM process-kill timing or physical foldable acceptance.
- Reminders/icons: dismissible AP/Cafe thresholds and silent AP refreshes; verified icon switching,
  failure feedback, variant badges, and update recovery without losing launcher access.
- OEM clients: preserve explicit business bindings and ordinary fallback. Protocol tests do not
  establish native card recognition, admission, or production credential acceptance.
- About: the v1.19.0 card and search keywords agree in Simplified Chinese, English, and Japanese.
  Review its changed screenshot before updating the golden.
- The freshness gate reports fresh for committed sources and dependency versions; account separately
  for uncommitted runtime edits. Verify all six fresh capture exports and the exact merge with
  `scripts/qa/baseline_profile_rules.py`. Counts are capture evidence, not fixed release gates.
- Verify release signing, version 1.19.0 / versionCode 11900999, R8/lint, and packaged
  assets/dexopt/baseline.prof plus baseline.profm against final source. Inspect the final APK's
  version and Git metadata; do not relabel an earlier artifact.
- If explicitly asked to create the annotated local v1.19.0 tag, use the final accepted commit.
  Rebuild after finalizing it so git-derived APK metadata uses the correct anchor.
  Never force-replace a published tag.
- Use [Release Notes v1.19.0](RELEASE_V1.19.0.md). Push/tag publication and GitHub Release publication
  remain separate authorized actions. Publish the signed APK and checksum only when requested,
  then update preparation wording and public links after verifying the published release.

### v1.18.0 Release Gate

Archived: [v1.18.0 notes](RELEASE_V1.18.0.md) retain the published release's scope.
Use the [v1.19.0 gate](#v1190-release-gate) for current preparation.

### v1.17.0 Release Gate

The archived [v1.17.0 release notes](RELEASE_V1.17.0.md) retain that release's scope.
Use the [v1.19.0 gate](#v1190-release-gate) for the current release target.

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

See the [CI Workflow Guide](../docs/build/github-actions.md) for manual-run examples,
artifact downloads, checksum verification, and the difference between APK and test job results.

- Runner: `ubuntu-26.04` for APK and test jobs. The explicit LTS label avoids automatic
  `ubuntu-latest` OS migrations. Image updates still occur within Ubuntu 26.04; Java 21,
  the Gradle Wrapper, and required Android SDK components are configured explicitly.
- Trigger: `push` and non-draft `pull_request` on `master`; Markdown/readme-only changes are
  ignored. Changes to `scripts/qa/test_report.sh` trigger CI because the test job executes it.
- Manual trigger: `workflow_dispatch` with optional `commit` (commit SHA / branch / tag).
- Manual inputs use the typed `inputs` context. `publish-build-scan` is opt-in and defaults to false.
- Concurrency is scoped by event and PR number, manual target, or branch. Separate fork PRs and
  manual builds of different commits do not cancel each other.
- Job output: debug APK artifact uploaded to GitHub Actions.
- Intended use: quick preview builds for development validation.
- Signing: the shared CI debug keystore in `app/signing/` is used only for debug/benchmark artifacts.
- Retention: 14 days.
- nightly.link: `https://nightly.link/hosizoraru/KeiOS/workflows/ci-debug-apk/master`
- APK file name format: `KeiOS_<versionName>_debug.apk`.
- Artifact name format: `KeiOS_<versionName>_debug`.

## GitHub Actions: CI / Benchmark APK

Workflow: `.github/workflows/ci-benchmark-apk.yml`

- Runner: `ubuntu-26.04`, using the same Java/Gradle/Android setup as debug and test jobs.
- Trigger: `push` on `master`; Markdown/readme-only changes are ignored.
- Manual trigger: `workflow_dispatch` with optional `commit` (commit SHA / branch / tag).
- Default behavior: build latest commit on selected branch when `commit` is empty.
- Build task: `./gradlew :app:assembleBenchmark --stacktrace -x lintVitalBenchmark` by default.
  Enable the manual `run-lint-vital` input to include that lint task.
- Job output: benchmark APK artifact uploaded to GitHub Actions.
- Intended use: release-like preview verification for R8, resource shrinking, and Baseline Profile performance.
- Signing: CI uses the shared debug keystore for benchmark artifacts; local signed builds can reuse release signing.
- Retention: 14 days.
- nightly.link: `https://nightly.link/hosizoraru/KeiOS/workflows/ci-benchmark-apk/master`
- APK file name format: `KeiOS_<versionName>_benchmark.apk`.
- Artifact name format: `KeiOS_<versionName>_benchmark`.

### Shared workflow rules

- External actions are pinned to full commit SHAs with their release versions in comments.
- Tokens retain `contents: read` and `packages: read`; checkout does not persist credentials.
- Only automatic debug APK builds pushed to `master` write the shared Gradle cache.
  Pull requests, manual builds, unit tests, and benchmark jobs use read-only caches.
- APK metadata must identify one existing APK within its output directory. Multiline metadata
  values are rejected before writing step outputs.
- Summaries report the APK file SHA-256 separately from the uploaded artifact archive digest and
  link to the uploaded artifact. Metadata reaches shell scripts through environment variables.
- Test summaries and timing artifacts run after success or failure, and stop on cancellation.
- Run `actionlint` from the repository root to validate workflows. `.github/actionlint.yaml`
  supplies the officially supported Ubuntu 26.04 label until the linter recognizes it natively.
  Composite action scripts also need shell/Python and behavior checks when changed.

References: [workflow syntax](https://docs.github.com/en/actions/reference/workflows-and-actions/workflow-syntax),
[concurrency](https://docs.github.com/en/actions/how-tos/write-workflows/choose-when-workflows-run/control-workflow-concurrency),
[secure use](https://docs.github.com/en/actions/reference/security/secure-use),
and [Gradle cache behavior](https://github.com/gradle/actions/blob/v6.4.0/docs/setup-gradle.md).

## GitHub Live Benchmark Test

`GitHubStrategyLiveBenchmarkTest` is an opt-in network test that compares Atom vs API strategy
behavior against live repositories. It covers release reads, strategy cache warm samples,
package-name scanning from release APKs, direct subscription inspection, and reverse repository
scans from package names.

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
./gradlew :feature-github:testDebugUnitTest --tests "os.kei.feature.github.data.remote.GitHubStrategyLiveBenchmarkTest"
```

One-off CLI example (without editing local properties):

```bash
KEIOS_GITHUB_API_TOKEN=ghp_xxx ./gradlew :feature-github:testDebugUnitTest \
  --tests "os.kei.feature.github.data.remote.GitHubStrategyLiveBenchmarkTest" \
  -Dkeios.github.liveBenchmark=true \
  -Dkeios.github.liveTargets=topjohnwu/Magisk,neovim/neovim
```

The token goes through the environment, not `-D`: a `-D` value is stored in Gradle's configuration
cache, so `keios.github.api.token` is not forwarded to the test JVM.

### What This Test Verifies

- Both strategies execute and produce benchmark results.
- Target list is non-empty.
- Warm samples are served from strategy cache.
- Package-name scan samples can read package IDs from AndroidManifest data inside release APK
  assets.
- Repository-scan samples can search candidates by package/app label and verify matches through APK
  package-name scans.

Because this is a live network benchmark, failures can still come from GitHub API/network/rate-limit conditions.
