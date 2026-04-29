# KeiOS Feature Overview

[中文版本 (CN)](FEATURES_CN.md)

KeiOS is built as a daily Android utility console. The app brings together system inspection, MCP service management, GitHub release tracking, Blue Archive office reminders, and a student-guide media browser in one phone-first interface.

## Home

Home is the status hub. It summarizes MCP runtime state, GitHub update/cache status, BA AP values, cafe AP, AP headroom, Shizuku status, and the currently visible page/card layout. Users can adjust bottom-page visibility and Home summary cards from the top action area.

## OS

The OS page focuses on device and system inspection:

- TopInfo and key-value sections for System, Secure, Global, Android properties, Java properties, and Linux environment.
- Search across OS parameters and activity entries.
- Configurable activity shortcut cards, including a Google System Service sample card.
- Import/export flows for activity cards and shell cards with preview and merge handling.
- Shizuku-powered shell runner with command history, formatted output, timeout controls, dangerous-command confirmation, and save-to-card support.
- Cached system snapshots for faster return visits.

## MCP

The MCP page manages a local KeiOS MCP server:

- Start/stop controls, local-only or LAN-oriented connection settings, port/path/token display, and config copy.
- MCP tool overview and logs.
- Claw Skill quick setup prompt for registering the KeiOS MCP skill.
- Foreground keep-alive service and test notifications.
- HyperOS Super Island template support and AOSP Live Update fallback settings through the notification compatibility controls.

## GitHub

The GitHub page tracks APK releases from GitHub projects:

- Stable and prerelease update checks for tracked repositories.
- GitHub API strategy configuration with optional token support.
- Release asset reading, APK download routing, and latest-release download actions.
- Tracked-item editing with app package linkage and installed-app matching.
- Share-import flow for repository, release, tag, and direct APK links.
- Refresh notifications, local cache summaries, and self-track shortcut for KeiOS.

## BA Office

The BA page acts as a Blue Archive office dashboard:

- AP and cafe AP tracking with server-aware timing.
- AP threshold notifications, cafe visit reminders, and arena refresh reminders.
- Friend code copy and office overview cards.
- Server, cafe level, AP threshold, media rotation, and custom media save-location settings.
- Calendar and pool cards with student-guide entry points.

## Student Guide

The student guide expands the BA workflow into catalog and media browsing:

- Catalog tabs for student and related entries, with search, sort, sync status, and local caching.
- Student detail pages with profile, strategy/simulation sections, gallery media, voice/audio/video content, and source sharing.
- Gift preference parsing with image and attitude markers.
- BGM favorites library with playback queue, batch cache, retry, import/export, and jump back into the student guide.
- Media cache controls and export flows, including archive-style saves for expression/media packs.

## Settings And Compatibility

Settings collect the runtime controls in one place:

- Theme mode, transition animations, predictive back, preloading, and Home HDR highlight.
- Liquid-glass ActionBar, liquid bottom bar, bottom-bar full-effect policy during scrolling, glass Switch style, and card press feedback.
- Custom non-Home background image and opacity controls.
- Notification permission, battery optimization, OEM autostart, app-list access, and Shizuku status.
- Super Island notification style, HyperOS compatibility bypass, and restore-delay tuning.
- Copy/text-selection mode, cache diagnostics, debug logs, exportable log ZIPs, and clear-cache actions.

## Platform Baseline

- Package: `os.kei`.
- ABI: `arm64-v8a`.
- Android baseline: Android 15+ (`minSdk 35`), `targetSdk=37`.
- UI stack: Jetpack Compose, Miuix KMP, Lifecycle ViewModel Compose, custom liquid-glass chrome, MMKV-backed preferences.
- Build baseline: Java 21 and Gradle project tooling.
