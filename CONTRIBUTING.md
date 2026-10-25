# Contributing to KeiOS

Thanks for your interest in contributing to KeiOS. This project is a personal open source Android
app focused on system utilities, GitHub tracking, and Blue Archive content pages.

## Project Stack

KeiOS is built primarily with:

- Kotlin
- Jetpack Compose
- Gradle
- Android minSdk 35
- Miuix and custom liquid glass style UI components

For local builds, read the [English](readme/BUILD.md) or [Chinese](readme/BUILD_CN.md)
build guide. The [CI Workflow Guide](docs/build/github-actions.md) explains automated builds,
manual runs, test reports, artifact verification, and signing differences.

## Issues

Please use issues to report bugs, request features, or discuss larger changes before implementation.

When reporting a bug, include:

- What happened and what you expected.
- Clear reproduction steps.
- Device model, Android version, KeiOS version, and install source.
- Logs, screenshots, or recordings when available.

For feature requests, describe the user problem, the workflow it improves, and any alternatives you
considered.

## Pull Requests

Small, focused pull requests are easiest to review and merge. Please keep changes scoped to one
concern whenever possible.

For large features, UI redesigns, architecture changes, storage changes, or behavior that affects
multiple pages, please open an issue first so the approach can be discussed.

Before opening a PR:

- Keep the diff focused and reviewable.
- Explain what changed and why.
- Include screenshots or recordings for UI changes.
- Describe how you tested the change.
- For performance-related changes, state whether testing used debug, release, or benchmark builds,
  and mention any meaningful differences.
- Avoid committing secrets, local machine paths, generated private files, or device-specific debug
  data.

## Testing

This repository may not have complete automated coverage for every feature. Use the most relevant
local validation for your change, such as building the app, running available tests, checking the
affected screen on a device or emulator, and reviewing logs.

Choose checks that can catch the changed behavior:

| Change | Evidence to include |
| --- | --- |
| Documentation | Valid local links, current source/configuration claims, and Markdown checks |
| Logic or state | Focused behavior tests and compilation of affected consumers |
| UI or localized copy | Relevant screenshot/semantics/resource checks and affected device/window states |
| Dependencies, R8, or packaging | Relevant Release/Benchmark build and packaged-artifact checks |
| Workflows or composite actions | `actionlint`, [workflow contract tests](docs/build/github-actions.md#maintain-the-workflows), changed-script checks, behavior fixtures, and the resulting cloud run |
| Performance | Comparable Release/Benchmark measurements with device and capture conditions |

Debug CI runs build-logic checks, Android module unit tests, and screenshot verification
in a separate job from APK assembly. Report their outcomes separately. Keep source/build,
installation, observed UI behavior, and performance claims tied to their own evidence.

Generated Baseline Profiles, screenshot goldens, the Gradle Wrapper, and the public CI debug
key are tracked project inputs. Build caches, private keys, tokens, local paths, and raw
device captures stay local. Check `.gitignore` and the staged diff rather than treating every
generated-looking file as disposable.

## Documentation

Update documentation when the user-facing behavior, build process, setup flow, or contribution
workflow changes. If your change affects local builds, check whether `readme/BUILD.md` or
`readme/BUILD_CN.md` should be updated.

Keep the [main README](README.md) and [Chinese overview](readme/CN.md) aligned on release
channels, package names, and supported platforms. Put contributor procedures in the build/CI
guides and link them from the [documentation index](readme/INDEX.md). Local one-run logs and
handoffs belong in ignored `.planning/`; keep reusable conclusions in `docs/` with their inputs
and evidence limits. Published release notes describe that release, so later CI maintenance
should update contributor documentation.
