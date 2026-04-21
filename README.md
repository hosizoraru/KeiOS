# KeiOS

[中文版本 (CN)](README_CN.md)

Build docs:
- [Build Guide (EN)](README_BUILD.md)
- [构建指南 (CN)](README_CN_BUILD.md)

## Current Distribution

- GitHub distribution is source-first during daily rapid iteration.
- Early adopters can clone this repository and build locally with the guides above.
- Packaged release cadence resumes after the current iteration window stabilizes.
- `Benchmark-CI` workflow (`.github/workflows/benchmark-ci.yml`) supports manual benchmark APK builds and uploads downloadable artifacts.

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
