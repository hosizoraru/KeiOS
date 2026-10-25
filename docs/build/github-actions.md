# GitHub Actions Guide

<!-- markdownlint-disable MD013 -->

[Documentation Index](../../readme/INDEX.md) | [Build Guide](../../readme/BUILD.md) | [Chinese Build Guide](../../readme/BUILD_CN.md)

## Workflows And Outputs

| Workflow | Automatic events | Jobs | APK artifact |
| --- | --- | --- | --- |
| [CI / Debug APK](../../.github/workflows/ci-debug-apk.yml) | Push to `master`; non-draft PRs targeting `master` | Debug APK and unit tests run in parallel | `KeiOS_<versionName>_debug` |
| [CI / Benchmark APK](../../.github/workflows/ci-benchmark-apk.yml) | Push to `master` | R8-enabled Benchmark APK | `KeiOS_<versionName>_benchmark` |

The APK inside each artifact has the same base name and an `.apk` extension. Both
workflows use Ubuntu 26.04, Java 21, the tracked Gradle Wrapper, and Android SDK
components checked by the [setup action](../../.github/actions/setup-android-gradle-build/action.yml).

Documentation, agent instructions, and local QA/performance helpers are excluded
from automatic triggers. Debug CI still runs when `scripts/qa/test_report.sh`
changes because the test job executes it. Any app or module source change is
included; new modules do not need to be added to a path allowlist.

Debug APK assembly and unit tests are independent jobs. An uploaded Debug APK
does not establish that the test job passed. That job runs build-logic checks,
`testDebugUnitTest` across Android modules, and Roborazzi screenshot verification.
Benchmark builds skip `lintVitalBenchmark` by default; manual runs can enable it.
Neither workflow publishes a stable release or installs an app on a device.

## Choose A Package

| Build | Application ID | CI signing | Use |
| --- | --- | --- | --- |
| Debug | `os.kei.debug` | Public debug key | Development preview alongside Stable |
| Benchmark | `os.kei` | Public debug key | R8/resource-shrinking and profile-path checks |
| Stable Release | `os.kei` | Private Release key | Published stable updates |

The [public CI key](../../app/signing/README.md) is intentionally tracked. It is
separate from the private Release key. CI Benchmark cannot update a Stable
installation signed by that Release key. Local Benchmark builds can use the
Release key when the private signing properties are configured; see the
[build guide](../../readme/BUILD.md).

A successful Benchmark build verifies packaging, not a measured performance
improvement. Baseline Profile collection, installation, visual acceptance, and
runtime measurements have their own verification steps.

## Run A Manual Build

Open either workflow in the repository's
[Actions page](https://github.com/hosizoraru/KeiOS/actions), choose **Run workflow**,
and select the branch containing the workflow definition.

| Input | Workflows | Default | Effect |
| --- | --- | --- | --- |
| `commit` | Both | Empty | SHA, branch, or tag to check out; empty uses the selected branch's event SHA |
| `publish-build-scan` | Both | `false` | Opt in to publishing Gradle Build Scan data |
| `run-lint-vital` | Benchmark | `false` | Include `lintVitalBenchmark` |

The source commit must be available on GitHub. `--ref` selects the workflow
definition; `commit` selects the source checkout, including local composite
actions from that checkout. Older commits can therefore use older setup and
artifact metadata behavior. Check the actual source SHA in the job summary.

Examples using an authenticated GitHub CLI:

```bash
# Build the event SHA on master with the Debug workflow.
gh workflow run ci-debug-apk.yml --repo hosizoraru/KeiOS --ref master

# Build a tag using the Benchmark workflow, including its lint task.
gh workflow run ci-benchmark-apk.yml --repo hosizoraru/KeiOS --ref master \
  -f commit=v1.19.0 -f run-lint-vital=true
```

Concurrency groups distinguish events, PR numbers, and manual targets or branch
refs. A newer run cancels an older run in the same group. Different fork PRs
with the same branch name, and manual builds targeting different commits, do not
share a group.

## Download And Verify

Use the run's **Artifacts** section, its job-summary link, or the
[Debug](https://nightly.link/hosizoraru/KeiOS/workflows/ci-debug-apk/master) /
[Benchmark](https://nightly.link/hosizoraru/KeiOS/workflows/ci-benchmark-apk/master)
nightly.link pages. APK artifacts are retained for 14 days; an expired artifact
requires a new build.

For a particular run, GitHub CLI can download and extract the selected artifact:

```bash
gh run download RUN_ID --repo hosizoraru/KeiOS \
  --name ARTIFACT_NAME --dir ci-download
```

Replace `RUN_ID` with the numeric ID in the run's `/actions/runs/<id>` URL (not
the `D#`/`B#` display counter), and `ARTIFACT_NAME` with the exact name shown in
that run. Check the summary's full source commit, application ID, variant, and
version, and the signing step's certificate fingerprint in the logs, before
treating an APK as the requested build.

The summary separates two checksums:

- **APK SHA-256** identifies the extracted APK bytes. Compare this with
  `sha256sum path/to/KeiOS_...apk` on Linux, or
  `shasum -a 256 path/to/KeiOS_...apk` on macOS.
- **Artifact archive SHA-256** identifies the uploaded ZIP artifact. It is not
  the APK checksum and should not be compared with the extracted APK.

The signing step verifies the APK against the expected public CI certificate;
the expected fingerprint is documented in the public key's README. A checksum
identifies bytes, while signing identifies the installation/update certificate.

## Tests, Reports, And Cancellation

- The Gradle test step is the pass/fail gate. Summaries and uploaded reports
  supplement it and do not turn a failed test run into a pass.
- Failed test jobs upload HTML reports and screenshot differences for 7 days.
- Completed test jobs upload small timing XML files and a summary for 14 days,
  including successful jobs.
- Test summaries and timing uploads run after success or failure. They stop when
  the run is cancelled.
- When a build is cancelled by a newer commit, inspect the replacement run
  rather than treating the cancelled run as accepted.

## Maintain The Workflows

External actions are pinned to full commit SHAs with release versions in
comments. When updating one, resolve the release tag in its upstream repository,
peel annotated tags to a commit, and check the action's input/output and runner
requirements. The app's SDK/JDK/dependency versions remain owned by the version
catalog and Gradle files.

Workflow tokens have only `contents: read` and `packages: read`, and checkout
does not persist credentials. Only automatic Debug APK pushes on `master` write
the shared Gradle cache; PRs, manual runs, tests, and Benchmark jobs read it.
Build Scans stay opt-in.

Run `actionlint` from the repository root. The tracked
[configuration](../../.github/actionlint.yaml) adds the supported Ubuntu 26.04
label for linter versions that do not recognize it yet. Changes to composite
action scripts also need shell/Python checks and behavior fixtures. Relevant
APK metadata cases include missing/multiple outputs, traversal or symlink
escapes, multiline fields, and literal shell metacharacters.

Also run the existing workflow contract tests:

```bash
./gradlew :app:testDebugUnitTest \
  --tests os.kei.ci.GitHubWorkflowContractTest --stacktrace
```

These check module coverage, ordered path exclusions and re-inclusions, pinned
actions, build tasks, and signing checks. Keep the tests aligned with the
workflow behavior when changing its configuration.

An ordinary push across changed workflow/build files is the cloud validation
step. Check both workflow conclusions and every job against the pushed SHA;
then confirm the uploaded APK's metadata and checksums. A local lint pass,
an earlier run, or an APK upload alone does not establish those results.

## References

- [Workflow syntax](https://docs.github.com/en/actions/reference/workflows-and-actions/workflow-syntax)
- [Concurrency](https://docs.github.com/en/actions/how-tos/write-workflows/choose-when-workflows-run/control-workflow-concurrency)
- [Expressions and cancellation](https://docs.github.com/en/actions/reference/workflows-and-actions/expressions)
- [Secure use](https://docs.github.com/en/actions/reference/security/secure-use)
- [Downloading artifacts](https://docs.github.com/en/actions/how-tos/manage-workflow-runs/download-workflow-artifacts)
- [Gradle cache behavior](https://github.com/gradle/actions/blob/v6.4.0/docs/setup-gradle.md)
- [Android app signing](https://developer.android.com/studio/publish/app-signing)
