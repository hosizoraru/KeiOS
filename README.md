# KeiOS

[中文版本 (CN)](readme/CN.md)

Docs:
- [Documentation Index](readme/INDEX.md)
- [Build Guide (EN)](readme/BUILD.md)
- [构建指南 (CN)](readme/BUILD_CN.md)
- [Todo List (EN)](readme/TODO.md)
- [待办清单 (CN)](readme/TODO_CN.md)

## Current Distribution

- Stable release APKs are available on [GitHub Releases](https://github.com/hosizoraru/KeiOS/releases).
- The latest public release is [KeiOS v1.1.0](https://github.com/hosizoraru/KeiOS/releases/tag/v1.1.0).
- Release package baseline: `os.kei`, `arm64-v8a`, `Android 15+` (`minSdk 35`).
- Local clone + build remains the preferred path for source users, contributors, and rapid preview validation.
- `CI / Benchmark APK` workflow (`.github/workflows/ci-benchmark-apk.yml`) still supports manual benchmark APK builds and uploads downloadable artifacts.

## Project Snapshot

- Android app focused on system utility + GitHub tracking + BA content pages.
- UI stack: Compose + Miuix KMP + Lifecycle ViewModel Compose + liquid glass style components.
- Runtime baseline: `minSdk=35`, `targetSdk=37`, Java/Kotlin toolchain on Java 21.

## Runtime Settings Map (Recent)

- `Settings > Visual & Interaction`
  Theme mode, transition animations, ActionBar layered style, liquid bottom bar, card press feedback, home icon HDR highlight, non-home page custom background image.
- `Settings > Notification & Compatibility`
  Super Island style toggle and HyperOS compatibility bypass toggle.
- `Settings > Copy & Text Selection`
  Lightweight row copy mode and expanded system text-selection mode.
- `Settings > Cache`
  Cache diagnostics summary for GitHub / MCP / OS / BA modules.
- `BA Page > BA Config`
  AP settings, media settings, adaptive media rotation, custom media save location.
- `GitHub Page > Check & Download / Track Sheet`
  Refresh interval, prerelease strategy, downloader routing, latest-release download behavior.
