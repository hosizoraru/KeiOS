# KeiOS

[中文版本 (CN)](readme/CN.md)

<p align="center">
  <a href="https://github.com/hosizoraru/KeiOS/releases"><img alt="Latest release" src="https://img.shields.io/github/v/release/hosizoraru/KeiOS?include_prereleases&sort=semver&display_name=tag&style=flat-square"></a>
  <a href="LICENSE"><img alt="License" src="https://img.shields.io/github/license/hosizoraru/KeiOS?style=flat-square"></a>
  <a href="https://github.com/hosizoraru/KeiOS/stargazers"><img alt="GitHub stars" src="https://img.shields.io/github/stars/hosizoraru/KeiOS?style=flat-square"></a>
  <a href="https://github.com/hosizoraru/KeiOS/network/members"><img alt="GitHub forks" src="https://img.shields.io/github/forks/hosizoraru/KeiOS?style=flat-square"></a>
  <a href="https://github.com/hosizoraru/KeiOS/issues"><img alt="GitHub issues" src="https://img.shields.io/github/issues/hosizoraru/KeiOS?style=flat-square"></a>
  <a href="https://github.com/hosizoraru/KeiOS/commits/master"><img alt="Last commit" src="https://img.shields.io/github/last-commit/hosizoraru/KeiOS/master?style=flat-square"></a>
  <img alt="Release downloads" src="https://img.shields.io/github/downloads/hosizoraru/KeiOS/total?style=flat-square">
</p>

<p align="center">
  <a href="https://github.com/hosizoraru/KeiOS/actions/workflows/ci-debug-apk.yml"><img alt="Debug APK CI" src="https://github.com/hosizoraru/KeiOS/actions/workflows/ci-debug-apk.yml/badge.svg?branch=master"></a>
  <a href="https://github.com/hosizoraru/KeiOS/actions/workflows/ci-benchmark-apk.yml"><img alt="Benchmark APK CI" src="https://github.com/hosizoraru/KeiOS/actions/workflows/ci-benchmark-apk.yml/badge.svg?branch=master"></a>
  <img alt="minSdk" src="https://img.shields.io/badge/minSdk-35-3DDC84?style=flat-square&logo=android&logoColor=white">
  <img alt="targetSdk" src="https://img.shields.io/badge/targetSdk-37-3DDC84?style=flat-square&logo=android&logoColor=white">
  <img alt="Kotlin" src="https://img.shields.io/badge/Kotlin-2.3.20-7F52FF?style=flat-square&logo=kotlin&logoColor=white">
  <img alt="Jetpack Compose" src="https://img.shields.io/badge/Jetpack%20Compose-1.10.6-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white">
</p>

KeiOS is an Android utility console for system inspection, local MCP service control, GitHub Releases and Actions artifact workflows, and Blue Archive helper tools. It combines a Compose + Miuix interface with liquid-glass style chrome, dense status cards, import/export tools, localized MCP skills, notifications, and cache diagnostics.

## Project Signals

| Item | Value |
| --- | --- |
| Stable package | `os.kei` |
| Supported ABI | `arm64-v8a` |
| Android baseline | Android 15+ (`minSdk 35`) |
| Target SDK | Android 17 / API 37 |
| UI stack | Jetpack Compose, Miuix, liquid-glass chrome |
| Runtime stack | Kotlin, Java 21, Shizuku, Media3, MMKV, Ktor, OkHttp |
| Languages | Simplified Chinese, English, Japanese |

## Quick Links

- [Latest Stable Release](https://github.com/hosizoraru/KeiOS/releases/latest)
- [All Releases](https://github.com/hosizoraru/KeiOS/releases)
- [Debug APK CI artifact](https://nightly.link/hosizoraru/KeiOS/workflows/ci-debug-apk/master)
- [Benchmark APK CI artifact](https://nightly.link/hosizoraru/KeiOS/workflows/ci-benchmark-apk/master)
- [Feature Overview](readme/FEATURES.md)
- [Build Guide](readme/BUILD.md)

## Main Features

- Home dashboard with compact MCP, GitHub, BA, Shizuku, and share-import status.
- OS tools for system tables, Android/Java/Linux properties, activity shortcuts, Shizuku shell cards, and card import/export.
- Local MCP server controls with config copy, runtime logs, foreground service support, Claw onboarding, localized SKILL.md output, and tools for Home, OS, GitHub, and BA snapshots.
- GitHub tracking for Releases and Actions artifacts, with prerelease strategies, nightly.link or token-backed Actions lookup, branch/workflow/run/artifact selection, share-import links, and installed-app linkage.
- BA office helpers for AP, cafe visit, arena refresh reminders, server-aware timing, media settings, Super Island notifications, and student-guide entry points.
- Student Guide catalog with search, sorting, media cache, voice-language labels, BGM favorites, gallery viewing, media export, and import/export for favorites.
- Settings for theme, motion, liquid-glass components, bottom-bar effect policy, background images, app language, permissions, cache diagnostics, logs, and notification compatibility.

Read the full feature tour:
- [Feature Overview (EN)](readme/FEATURES.md)
- [功能完整介绍 (CN)](readme/FEATURES_CN.md)

## Current Distribution

- Stable APKs are published through [GitHub Releases](https://github.com/hosizoraru/KeiOS/releases).
- Current stable tag: [v1.3.2](https://github.com/hosizoraru/KeiOS/releases/tag/v1.3.2).
- Release package baseline: `os.kei`, `arm64-v8a`, Android 15+ (`minSdk 35`).
- Runtime and build baseline: `targetSdk=37`, Java 21, Kotlin/Gradle project toolchain.
- App language resources currently cover Simplified Chinese, English, and Japanese.

## Documentation

- [Documentation Index](readme/INDEX.md)
- [Build Guide (EN)](readme/BUILD.md)
- [构建指南 (CN)](readme/BUILD_CN.md)
- [Todo List (EN)](readme/TODO.md)
- [待办清单 (CN)](readme/TODO_CN.md)

## Star History

[![Star History Chart](https://api.star-history.com/svg?repos=hosizoraru/KeiOS&type=Date)](https://www.star-history.com/#hosizoraru/KeiOS&Date)
