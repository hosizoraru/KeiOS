# KeiOS Feature Overview

<!-- markdownlint-disable MD013 -->

[中文版本 (CN)](FEATURES_CN.md)

KeiOS is built as a daily Android utility console. The app brings together system inspection, MCP
service management, GitHub Releases / Actions tracking, subscription-project tracking, GitHub Star
import,
repository discovery, Blue Archive office reminders, JSON data migration, local issue feedback, and
a Student Guide media browser in one phone-first interface.

## Home

Home is the status hub. It uses compact status pills for Shizuku, MCP, GitHub, and BA state, then
keeps focused MCP, GitHub, and BA cards below the hero. Users can adjust bottom-page visibility and
Home summary-card visibility from the top action area.

## OS

The OS page focuses on device and system inspection:

- TopInfo and key-value sections for System, Secure, Global, Android properties, Java properties, and Linux environment.
- TopInfo highlights readable device, build, CPU, runtime, memory, storage, locale, developer-state,
  and verified-boot summaries before the raw property tables.
- Search across OS parameters and activity entries.
- Configurable activity shortcut cards, including built-in AOSP/Google system entries for hidden
  system settings.
- Searchable, grouped activity-card and shell-card sheets with separate import/export flows,
  preview, and merge handling.
- Background shortcut action execution for refresh and page-entry flows.
- Shizuku-powered shell runner with command history, formatted output, timeout controls, dangerous-command confirmation, and save-to-card support.
- Cached system snapshots for faster return visits.
- v2 liquid floating dock controls for add, refresh, search, and page actions.

## MCP

The MCP page manages a local KeiOS MCP server:

- Start/stop controls, local-only or LAN-oriented connection settings, port/path/token display, and config copy.
- Productized tool sections for entrypoint, workflow, and advanced tools, with search and grouped
  cards for users who want a smaller starting surface.
- Claw Skill quick setup prompt, localized SKILL.md generation, workflow blueprints, and tool-level
  help resources.
- 51 MCP tools for Home overview, OS cards, system TopInfo, GitHub tracking/share import/discovery,
  Star List import, package scans, reverse repository scans, direct subscription inspection, and
  Blue Archive account, daily-plan, Craft Chamber, and cache operations.
- Typed catalog metadata, JSON schemas, tool annotations, structured outputs, and resource/prompt
  registries for MCP clients that support the newer protocol surface.
- Adaptive runtime-log and session monitoring to keep long-running MCP service sessions lighter.
- Foreground keep-alive service, test notifications, and semantic icon bitmap support for
  notification builders.
- HyperOS Super Island template support and AOSP Live Update fallback settings through the notification compatibility controls.

## GitHub

The GitHub page tracks APK updates from GitHub projects and subscription projects:

- Stable and prerelease update checks for tracked repositories, plus remote-version checks for
  direct APK links, JSON feeds, companion JSON files, versioned directories, and APK directory
  indexes.
- GitHub API strategy configuration with optional token support shared by Releases and Actions.
- Release asset reading, APK download routing, app-managed install routing, and latest-release
  download actions.
- Dedicated release-history pages with compact and expanded cards, release notes, filtered APK
  assets, page/tag navigation, tag-only filtering, and direct installation of an older version.
- Dedicated F-Droid version-history pages that load compact package metadata first, add file details
  progressively, explain anti-features, and expose checksums and signer data for trust checks.
- Older builds in both histories open APK info and install through the app-managed installer, and every
  asset share or download follows the share-to-installer and download settings.
- Release selection that reads projects which restarted their numbering (confirming with
  `releases/latest` only when the list looks like it), retires pre-releases superseded by a shipped
  stable or left unfed for 14 days, ignores releases with no downloadable file, reports uncertain
  comparisons, and explains a surprising choice on the card.
- Refresh diagnostics that name the network phase of a slow item (queued, DNS, connecting, server
  wait, download) with request count, bytes, and connection reuse, in history, exports, and MCP.
- GitHub Actions browser for branches, workflows, runs, and artifacts, with nightly.link public lookup and token-backed GitHub API lookup.
- Actions recommended-run update checks, app-icon notifications, debug notification testing, and
  notification deep links into the tracked project's Actions sheet.
- Branch recommendation that considers the default branch, recent activity, successful runs, and artifact availability.
- Artifact ranking that highlights Android packages, build types, universal packages, recency, and previous download history.
- Tracked-item editing with app package linkage, installed-app matching, package-name scanning from
  latest stable release APKs, reverse repository scanning from package name plus app label, source
  mode switching, subscription import/export compatibility, and unsaved-change confirmation.
- Deep repository profiles, health scoring, archived/fork signals, release-note parsing,
  release-note translation, precise APK version modes, and runtime cache freshness checks.
- Subscription-project cards with remote health, remote stable/prerelease releases, Scene-style
  index release notes, installed-app labels, release-note actions, and install actions when the
  tracked package is missing.
- Share-import flow for repository, release, tag, and direct APK links with transparent window
  handling, notification-first/sheet-first routing, external installer handoff, and app-managed
  Shizuku delivery.
- Managed install surfaces with remote/local APK comparison, manifest inspection, ABI/SDK/package
  hints, versionName/versionCode display, install confirmation notifications, and Shizuku
  PackageInstaller session handling.
- Star List import from authenticated stars, public user stars, and public Star List URLs, with list
  discovery, search, multi-select filters, Android/APK quality classification, release APK
  verification, and import confirmation.
- Strategy diagnostics that compare Atom and API behavior for release checks, package-name scans,
  and reverse repository scans.
- Remembered sort/order/filter preferences, filtered-list refresh from the floating dock, full
  refresh from shortcuts, refresh notifications, local cache summaries, tracked item
  focus/auto-scroll, and self-track shortcut for KeiOS.
- Independent Actions update intervals with Follow global / 1h / 2h / 3h / longer options.

## Import, Feedback, And Migration

- JSON import activity for KeiOS data migration with preview, metric tiles, result navigation, and
  multi-schema routing for OS card transfer data.
- Local GitHub issue assistant with structured log levels, issue markdown generation, and
  telemetry-free diagnostics.
- Shared import/export services for OS cards, shell cards, GitHub tracks, and student-guide data.

## BA Office

The BA page acts as a Blue Archive office dashboard:

- AP and cafe AP tracking with server-aware timing.
- AP threshold notifications, cafe visit reminders, arena refresh reminders, and BA-specific Super
  Island presentation using progress and countdown templates.
- Shortcut-triggered AP and cafe AP Super Island notifications.
- Six Craft Chamber timers per account cover three Generate slots and three Fusion slots, with grade,
  count, total-duration editing, precise completion reminders, and compact running summaries.
- Configurable daily templates can update AP remainder, headpat/invite counts, and craft plans for
  one account or every account through Quick Settings tiles, launcher shortcuts, and MCP.
- Server-specific nickname and friend-code ID cards, plus friend code copy and office overview
  cards.
- Server, cafe level, AP threshold, media rotation, and custom media save-location settings.
- A merged calendar-and-pool page with server-aware category navigation, compact time layouts,
  notification settings, and student-guide entry points. Wide windows show calendar and pool lanes
  together, while phones switch categories inside one route.

## Student Guide

The student guide expands the BA workflow into catalog and media browsing:

- Catalog tabs for students and related entries, with search, sort, compact localized labels, sync
  status, local caching, implemented-student filters, and school filters for NPC/satellite entries.
- Student and related-entry detail pages with profile, strategy/simulation sections, NPC/satellite
  label grouping, gallery media, localized voice-language labels, audio/video content, and source
  sharing.
- Gift preference parsing with image and attitude markers.
- MP4 Memorial Lobbies remain available. Interactive Spine lobbies add actions, zoom, pan, view reset,
  linked BGM, mute, and controls that can be hidden without interrupting the animation.
- Student 3D models where resource mappings are available, with model/animation selection,
  camera controls, seeking, speed, looping, outlines, and a saved background palette.
  Spine assets and model files use bounded caches to reuse downloads.
- Wide tablet model tools use a scene-backed Liquid Glass sidebar with an inline palette and keyboard
  avoidance. Opening tools preserves the model viewport; immersion retains camera gestures and playback.
- Interactive lobbies and 3D models default to System WebView. Independent compatibility presentation
  options are available in Settings → Interface for devices with display problems, effective on re-entry.
- BGM favorites library with playback queue, liquid bottom dock, mini player, batch cache, retry,
  removal Undo, import/export, native media notifications, and jump back into the student guide.
- Media cache controls and export flows, including archive-style saves for expression/media packs.
- NPC and satellite entries adapt older GameKee pages with broader gallery parsing, related-role
  classification, leaner information rows, and compatibility for pages with fewer implemented-student
  fields.

## Settings And Compatibility

Settings collect the runtime controls in one place:

- Theme mode, transition animations, predictive back, search focus behavior, preloading, app
  language shortcut, and Home HDR highlight.
- v2 liquid-glass ActionBar, title cards, search fields, floating docks, bottom bar, bottom-bar
  full-effect policy during scrolling, and scoped card press feedback.
- Long liquid sheets compose lazily, offscreen glass skips draw work, and drag reads run in layout;
  the existing material, animation, blur, and interaction effects stay intact.
- Adaptive main navigation keeps a bottom bar on phones, moves to top navigation at regular tablet
  widths, and uses a remembered drag-to-close sidebar on ultra-wide windows.
- Settings, About, MCP, OS cards and Shell, BA Office, Student Guide, the merged calendar-and-pool
  route, GitHub tracking and history, and Play use independent two-lane layouts where width allows.
- Icon design selector with Android Designs as the default set and Apple Designs as the refreshed
  alternate set.
- Custom secondary-page background image and opacity controls, including cards, chrome, and modal
  presentation scenes.
- Notification permission, battery optimization, OEM autostart, app-list access, and Shizuku status.
- Super Island notification style, automatic closing time, HyperOS compatibility bypass,
  and restore-delay tuning. GitHub refresh progress keeps its task-specific closing behavior.
- Copy/text-selection mode, cache diagnostics, debug logs, exportable log ZIPs, and clear-cache actions.
- Local GitHub issue feedback and structured log-level controls.
- Debug component lab and liquid catalog for checking shared chrome, buttons, dropdowns, sliders,
  progress bars, and dock behavior.
- Simplified Chinese, English, and Japanese resources, with BA terms, Android settings, GitHub, shell, and MCP skill text localized for display surfaces.

## Platform Baseline

- Package: `os.kei`.
- ABI: `arm64-v8a`.
- Android baseline: Android 15+ (`minSdk 35`), `targetSdk=37`.
- UI stack: Jetpack Compose `1.12.1`, Miuix KMP, Lifecycle ViewModel Compose, custom v2 liquid-glass
  chrome, MMKV-backed preferences.
- Build baseline: Java 21, Gradle Wrapper `9.8.0`, Kotlin `2.4.21-RC`, Android Gradle Plugin
  `9.4.1`, Ktor `3.6.0`, generated Baseline Profiles, and shared build-logic conventions.
- Baseline Profile collection covers six bounded journeys. Capture results and release freshness
  are checked separately; the v1.17.0 capture completed on HyperOS Phone, including forced wide-window
  coverage. Native HyperOS Pad collection follows when that device is configured.
  See the [build guide](BUILD.md) and [collection plan](../docs/planning/baseline-profile-coverage.md).
