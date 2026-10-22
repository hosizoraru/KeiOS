# Android 17 (API 37) adaptation audit

> **Media follow-up, 2026-10-23:** see [Media notification revalidation](#media-notification-revalidation--2026-10-23)
> for the current Media3 integration and strict-mode evidence. The August captures below are historical;
> their shell command does not enable hardening on the October AOSP image.

> Source: *Android 17 应用适配指南*, `dev.mi.com` (小米澎湃OS 开发者平台), page updated **2026-05-06**.
> Audited **2026-08-18**. Every item in that guide is walked below with a verdict.
>
> **The fact that decides the scope:** this app is already `targetSdk = 37`, `compileSdk = 37`, `minSdk = 35`.
> So the guide's "targetSdkVersion >= 37" half is **live**, not latent — there is no grace period to plan for.

## Result

Most of the guide does not touch this app, and that is a finding rather than a shrug — each row below was
checked against the tree, not assumed. **Two things were already done** (one of them the largest item in the
guide), **three were changed**, and **two are deferred** with a reason.

| Change | Verdict |
|---|---|
| `usesCleartextTraffic` deprecation | **N/A.** Neither the attribute nor a `networkSecurityConfig` exists; nothing to migrate |
| Implicit URI grant restriction | **N/A for existing code.** The app's own `ACTION_SEND` builder is text-only (`EXTRA_TEXT`, no `EXTRA_STREAM`); every place it hands out a file URI already adds `FLAG_GRANT_READ_URI_PERMISSION`. Now probed — see below |
| Keystore key count limit | **N/A.** No `AndroidKeyStore` / `KeyGenerator` anywhere |
| IME visibility after rotation | **N/A.** Every activity declares `keyboardHidden` in `configChanges`, so it is not recreated on IME changes |
| Touchpad relative pointer capture | **N/A.** No `requestPointerCapture` |
| Background audio hardening | **Tested under the hardening flag, passes** — see below |
| Bluetooth autonomous re-pairing | **N/A.** No Bluetooth |
| Parcel use-after-recycle | **Safe.** The only `Parcel.recycle()` in the tree is the ITGSA reply, which recycles in `finally` after the `transact` and never reads again |
| Parcel size mismatch | **N/A.** No custom `writeToParcel` / `createFromParcel` in the app; `MiFocusProtocol` only *passes* framework Parcelables through a Bundle |
| Config changes no longer recreating the Activity | **Safe by construction** — see below |
| `ParcelFileDescriptor.parseMode` strict | **N/A.** No `parseMode` / `openFileDescriptor` calls |
| `setThreadPriority` range check | **N/A.** No `setThreadPriority` calls |
| MessageQueue lock-free (`DeliQueue`) | **Safe, with one note** — see below |
| `static final` immutable at runtime | **Safe.** The one reflection site (`ShizukuPackageInstallerBridge`) takes a *constructor* and sets `isAccessible`; it writes no fields |
| Notification custom view size | **N/A.** No `setCustomContentView` / `setCustomBigContentView` / `RemoteViews`; MiFocus uses standard templates plus bundle extras |
| NPU feature declaration | **N/A.** No NNAPI / TFLite / ML Kit |
| Complex-IME accessibility | **N/A.** No `AccessibilityService`, and the guide marks it an optional enhancement |
| ECH opportunistic | **No action.** Default-on is desirable and nothing pins SNI |
| `ACCESS_LOCAL_NETWORK` | **Already done** — see below |
| Physical-keyboard password hiding | **N/A.** The one password field uses `PasswordVisualTransformation`, which always masks |
| SMS OTP filtering | **N/A.** No SMS |
| BAL hardening / `IntentSender` | **Safe.** No `IntentSender.sendIntent()`; notification PendingIntents already set the creator BAL mode explicitly. Now also probed |
| Certificate Transparency default-on | **No action.** Public CAs only, no pinning, no user-store trust |
| Writable native DCL | **N/A.** No `System.load` / `loadLibrary` |
| Large-screen orientation opt-out closed | **One predicate fixed; real Pad work deferred** — see below |
| Advanced Protection Mode | **N/A.** No `AccessibilityService` to flag as a tool |

## The item that was already handled, and it is the big one

**`ACCESS_LOCAL_NETWORK`.** At `targetSdk >= 37` this dangerous permission must be declared *and requested*,
and the platform enforces it in **kernel BPF** — unauthorised packets are dropped silently, with no Java
exception. For an app that runs an MCP server (`127.0.0.1:38888`, and `0.0.0.0` when external access is
enabled) and syncs to WebDAV, silent packet loss would have been a miserable thing to debug.

It was already complete before this audit: declared in the manifest, wrapped in
`LocalNetworkPermissionCompat` (which also maps to `NEARBY_WIFI_DEVICES` on Android 16), requested through a
launcher in `MainActivity`, gating MCP start on the grant, granted by the baseline profile, and explained on
the About page. Nothing to do.

## What changed

### 1. The Home hero's short-viewport rule was internally inconsistent

`homePageUsesCompactLandscapeLayout` read:

```kotlin
availableWidth > availableHeight && availableHeight <= 480.dp
```

which encodes "a phone held sideways", not the constraint it is named for. Both halves were wrong once the app
targeted 37:

- **The orientation term.** Android 16 already ignores an orientation request on `sw >= 600dp` for
  `targetSdk >= 36`; Android 17 removes the `PROPERTY_COMPAT_ALLOW_RESTRICTED_RESIZABILITY` opt-out at
  `targetSdk >= 37`. This app declares `sensorPortrait`, so a wide viewport on a large screen was previously
  **unreachable** — a rule keyed on it could not be caught being wrong.
- **The 480dp cutoff.** A phone in landscape is ~426dp tall, so 480dp covered it. A large screen in landscape
  is 600–800dp: too tall to trip the cutoff, still far shorter than the ~950dp the tall hero is drawn for.

Reproduced on the API 37 AVD at 2856×1280 / density 320 (a 1428×640dp viewport): the tall hero pushed the
overview pill rows **underneath the floating dock** at rest. Scrollable — one swipe brought all four rows
clear, so nothing was unreachable — but the first paint read as broken.

Fixed by making the predicate the height budget it claims to be, `availableHeight < HomePageTallHeroMinHeight`
(700dp), chosen from the two measured geometries: 640dp overflows, 952dp has room. No phone geometry changes,
which the test pins with the real dp values for both.

**This is a one-predicate correction, not Pad adaptation.** Proper large-screen work is deferred to a dedicated
Pad AVD — driving a phone AVD into tablet geometry is the wrong tool for it.

### 2. StrictMode probes for the changes that bite in Android 18

`Android17StrictModeProbes`, debug builds only, `penaltyLog` only. The app had **no StrictMode at all**.

Both of these ship in Android 17 as *detection without enforcement*: the restriction flags are off, the old
behaviour still happens, the platform only logs — and Android 18 is where they throw. That gap is the whole
reason to install them, because the failure is invisible in ordinary Android 17 testing and a
`SecurityException` later.

- `detectImplicitUriPermissionGrant()` — added reflectively. The method is new in 37 and a direct call would
  compile against `compileSdk 37`, but a `NoSuchMethodError` on an Android 16 device would take the entire
  policy down with it; reflection keeps a miss local to the one detector.
- BAL detection, which `detectAll()` includes automatically for `targetSdk > 35`. The app launches activities
  from notification PendingIntents, which is exactly the traffic those rules govern.

`penaltyDeath` deliberately not used: neither detector is precise enough to bet a release build on, and the
platform has not finished making up its mind.

### 3. Fair running memory

Landed separately and documented in `itgsa-fair-memory.md`. Related to this guide only in that both are part
of the same Android 17 / alliance push.

## Safe, but for a reason worth writing down

### Config changes that no longer recreate the Activity

Android 17 stops recreating the Activity for `keyboard`, `keyboardHidden`, `navigation`, `touchscreen`,
`colorMode` and calls `onConfigurationChanged()` instead. **This app overrides `onConfigurationChanged`
nowhere**, which looks like a gap and is not: it is Compose-only, and Compose's own
`ProvideAndroidCompositionLocals` registers a `ComponentCallbacks` and republishes `LocalConfiguration` when
the configuration changes *without* a recreation. Anything reading `LocalConfiguration` — including the Home
hero predicate above — recomposes either way.

Activities already declare `keyboardHidden|orientation|screenLayout|screenSize|smallestScreenSize|uiMode`, so
the two that were already handled stay handled, and `android:recreateOnConfigChanges` is not needed.

### MessageQueue

No `mMessages` reflection anywhere, so the `DeliQueue` change has nothing to break. The note this section
used to carry — that the guide asks for **Robolectric 4.17+** with `@LooperMode(PAUSED)` when targeting 37
while this project sat on **4.16.1** — is settled: **4.17 landed on 2026-09-14**, verified across all 3227
unit tests in one pass. Espresso was already 3.7.0, which satisfies the guide.

Two things had to move with it, and both are environment rather than test logic.

- Five test `Application` subclasses were declared `private`, which compiles to a package-private JVM class.
  4.17 instantiates the application through `android.app.AppComponentFactory`, which cannot reach one, so
  each failed in `installAndCreateApplication` with `IllegalAccessException`. They are `internal` now — a
  public JVM class, and the same visibility the other eighteen `…TestApp` classes already had in practice.
- A test at `sdk = 36` or above boots `ApplicationSharedMemory`, and Robolectric's `FileDescriptorInterceptor`
  reaches `jdk.internal.access.SharedSecrets` to service it. `java.base` does not export that package, so the
  test dies during `setUpApplicationState` before any assertion runs. The root build now passes
  `--add-exports=java.base/jdk.internal.access=ALL-UNNAMED` to every module's unit tests.

That second one is the one that matters for this audit: it is the cost of a test *leaving* `@Config(sdk = [35])`,
and it is now paid up front for every module rather than discovered by whoever writes the first API 37 test.

### The pins cannot move yet, and the blocker is not this app

4.17 supporting SDK 37 is not the same as the suite being able to use it. Flipping all 164 files from
`sdk = [35]` to `[37]` and running every module gives **241 failures in `:app` alone**, and 412 across the
tree collapse to one cause:

```
java.lang.NoSuchMethodException: android.hardware.input.InputManager.getInstance()
  at androidx.test.espresso.Espresso.onIdle
  at androidx.compose.ui.test.RobolectricIdlingStrategy.runUntilIdle
```

Every Compose UI test goes through that idle path, so every Compose UI test dies before its first
assertion. The reflection is **Robolectric's own**, not Espresso's and not ours: of every jar on the unit
test runtime classpath, only `shadows-framework` carries the literal string
`android.hardware.input.InputManager`, and `ShadowInputManager` is the one class in it that pairs that
string with `getInstance`. The platform moved that static to `InputManagerGlobal` before 37 —
`ShadowInputManagerGlobal` exists alongside it — but the old reflector still runs and 37 no longer answers.

So the pins stay at 35 for now, and this is a wait, not a decision: it needs a Robolectric release that
routes `ShadowInputManager` through `InputManagerGlobal` at 37. The handful of non-Espresso failures in
that run (four in the GitHub share-import notification helpers) are worth re-reading when it unblocks;
they are real assertion failures rather than environment errors, and they are invisible while the 412 stand.

The experiment was reverted. Nothing in the tree is pinned at 37.

### Background audio hardening — tested, passes

**Exercised on the API 37 AVD with `adb shell cmd audio set-enable-hardening 1`**, which is the guide's own
switch for forcing the strict mode on. Not reasoned about — driven.

First, the shape of the app that makes this narrow. There is **no `AudioManager` usage anywhere** — no
`setStreamVolume`, `adjustStreamVolume` or `requestAudioFocus` — so the volume-API half of the hardening has
nothing to act on, and the BGM volume slider is Media3 player gain rather than a stream volume. Focus is
requested by Media3 on the app's behalf; `dumpsys audio` shows it as
`client: …media3.common.audio.AudioFocusManager… gain: GAIN loss: none notified: true sdk:37`.

**At the time of the test, the default configuration could not be reached by the hardening.** "Native media
notification" was **off by default**, so BGM did not join the system media session — `dumpsys media_session`
reported `have 0 sessions`. Backgrounding the app moved the player from `state:started` to `state:paused`, and
the control run with `set-enable-hardening 0` did **exactly the same thing**, which is what proves it was the
app's own behaviour and not the platform muting anything. `mutedState:none` throughout.

That default has since been flipped **on** — see *The default is now on* below. The hardened path in the next
table is therefore the one users get out of the box, which is the whole reason it had to be driven first.

**With the setting enabled — the path that can be affected — it still passes.** With hardening on:

| Step | Result |
|---|---|
| Play in the foreground | `state:started`, session `androidx.media3.session.id.ba_guide_bgm_media_session` active |
| Home, app backgrounded | `state:started`, **`mutedState:none`** — playback continues, unmuted |
| `cmd media_session dispatch pause` from the background | `state:paused` |
| `cmd media_session dispatch play` from the background | **`state:started`, `mutedState:none`** |
| `AudioHardening` reports across all of it | **none** |

The last row of that table is the guide's worst case — a play command arriving while the app is invisible —
and the reason it passes is visible in `dumpsys activity services`:

```
infoAllowStartForeground=[… uidState: TOP … allowWiu:12 … targetSdkVersion:37 …]
isForeground=true foregroundId=1001 types=0x00000002
```

**`allowWiu:12`** — the foreground service holds the While-In-Use capability, because Media3 starts it while
the app is `TOP` and keeps it alive across a pause. WIU is precisely what the hardening requires, so the focus
request succeeds and playback is never muted. The failure mode the guide describes needs an FGS *started* from
the background; this one never is.

**No code change was needed to pass.** The AVD was left as found: hardening switched back off and the
native-media-notification setting returned to what was then its default.

### The default is now on — `2026-08-18`

`BA_NATIVE_BGM_MEDIA_NOTIFICATION_DEFAULT = true`, in
`app/src/main/java/os/kei/ui/page/main/ba/support/BaNativeBgmMediaNotificationPrefs.kt`.

The two backends differ in more than a notification. `Lightweight` has no `MediaSession`, so the system has
nothing to route media keys or the shade's transport controls to, and the page's own lifecycle pauses playback
when it stops — which is why the default-off run above looked identical with hardening on and off. `NativeMedia`
runs the Media3 foreground service and keeps playing while the app is backgrounded, which is what someone
playing background music is asking for. It stayed off only because the hardened path was untested, and the run
above is what removed that reason.

One constant, referenced from all three places that need it — the prefs fallback, the route state's default,
and the view model's pre-load value — so the pre-load UI cannot disagree with what the store will return.
`BaGuideBgmPlaybackUiState.nativeMediaNotificationEnabled` deliberately stays `false`: it means "which backend
is live", not "what does the setting say", and seeding it with the default would make
`updateNativeMediaNotificationEnabled` early-return and skip the migration into the backend the setting names.

**Verified on a fresh install, Pad AVD, no toggling.** The MMKV key is absent from `files/mmkv/ba_page_settings`
— nothing has been written, so the fallback is what is in play — and playing one track from the catalog's Music
tab gives:

```
Media button session is os.kei.debug/androidx.media3.session.id.ba_guide_bgm_media_session/2
Sessions Stack - have 1 sessions:      <- was "have 0 sessions" under the old default
  active=true  state=PlaybackState {state=PLAYING(3), position=11895, …}
```

Pressing Home leaves it at `state=PLAYING(3)` with the position still advancing (11895 → 32814), on an FGS with
`isForeground=true types=0x00000002`, `allowWiu:12`, `uidState: TOP`, `targetSdkVersion:37`.

MMKV consults the default only when the key is absent, so anyone who deliberately turned the switch off keeps
it off across the update. `BaNativeBgmMediaNotificationPrefsTest` pins that.

## Phone verification — clean, `2026-08-18`

Run on the API 37 AVD at its phone geometry (1280×2856, density 480 → 426×952dp), **debug and release**.

### Debug

| Check | Result |
|---|---|
| Cold start, all five tabs walked | No crash, no ANR, no app-side `E` line |
| StrictMode probes | `Android17Probes: StrictMode probes installed`, and **no violation reported** |
| Every signal the guide names | **None fired.** Swept for `discontinued from Android 18`, `BadParcelable`, `Parcel used while recycled`, `consumed … bytes, but`, `AudioHardening`, `Bad mode:`, `Priority/niceness`, `Too many keys`, `NPU access is blocked`, `certificate transparency`, `Attempt to load writable` |
| Home hero after the predicate change | Unchanged — tall hero, all four pill groups clear of the dock, as the 952dp ≥ 700dp test predicts |

**Restricted non-SDK interfaces**, which the guide asks to check explicitly: exactly two accesses, both
`allowed` at `TargetSdkVersion=37`, and **neither is app code**:

- `ServiceManager.getService` from `rikka.shizuku.SystemServiceHelper` — Shizuku's own library
- `SystemProperties.addChangeCallback` from `androidx.compose.ui.platform.AndroidComposeView$Companion` — Compose itself

The app's one remaining hidden-API site of its own is `Class.forName("android.content.pm.IPackageInstaller")`
in the Shizuku install bridge, which did not run here because Shizuku is not active on this AVD. It is worth
re-checking on a device where Shizuku is running.

### Release

Release matters separately because R8 is where a runtime-registered receiver would quietly disappear.

- `assembleRelease` succeeds and installs.
- The receiver **is registered under R8**, proven independently of logging — release sets
  `DEFAULT_LOG_LEVEL_ID = "off"`, so `dumpsys activity broadcasts` was used instead and lists both
  `itgsa.intent.action.TRIM` and `itgsa.intent.action.KILL`.
- The release trim path works, measured rather than logged: `am send-trim-memory <pid> COMPLETE` on the
  backgrounded release process took **TOTAL PSS 45,810 KB → 40,973 KB, freeing ~4.7 MB**. So the release
  registry, the Coil eviction and the bitmap-cache eviction all survived minification.

### The navigation does not collapse at regular width

Reported from the AVD: scrolling a page made the top tab bar tuck itself into a corner dock, and took the page's
floating dock with it.

That collapse is a *bottom bar* affordance. A floating bar at the bottom of a phone sits on top of the content, so
getting out of the way while reading is a real trade. The top bar makes no such trade — it shares the row the
title and the actions already occupy and costs no vertical space — so hiding it buys nothing and loses exactly
what the HIG asks for: "make sure the tab bar is visible when people navigate to different sections of your app.
If you hide the tab bar, people can forget which area of the app they're in." The sidebar is the same argument in
the other axis: beside the content, not over it.

`appNavigationCollapsesOnScroll` is true only for `Bottom`, and the page floating docks read the same resolved
visibility, because they were shrinking to a single button alongside a bar that had no reason to shrink.

### Baseline profile — the two shapes are covered, from one device

> **Correction, `2026-08-18`.** When this section was written the journey had been added but never run to a
> pass, and the heading claimed a coverage that did not exist. The committed profile still came from an older
> generation: `MainPagerSidebar` and `AppNavigationPlacement` appeared in it **zero** times. Two defects in the
> journey were failing it every time — it passed `wm size` raw pixels, which is 1280x800dp only at the Pad
> AVD's density 320 and 853x533dp at the phone AVD's 480, and at that short side the manifest's
> `sensorPortrait` request is still honoured, so the window rotated to portrait and the sidebar toggle never
> composed; and its closing `waitForHome()` asserted Home while the journey sits on BA, which could only ever
> time out. Both are fixed, sizes are now expressed in dp against the device's real density, and a regenerated
> profile carries 36 `MainPagerSidebar` and 50 `AppNavigationPlacement` rules. The claim below is true as of
> that regeneration; it was not before it.

`tabletAndFoldNavigationShapes` in `BaselineProfileGenerator`. The shapes only exist above 600dp and 660dp, so a
profile generated on a phone never compiles them, and one generated on a tablet has the opposite hole because the
floating bottom bar never renders there. Rather than requiring two runs and a merge — a process step that gets
forgotten — the journey resizes the window itself with `wm size`: 1280dp for both tablet shapes, then 775dp and
500dp, which is a fold opening and closing across both thresholds while the app runs. `MainActivity` declares
`screenSize|screenLayout|smallestScreenSize` in `configChanges`, so that reflows rather than recreating the
Activity, and the reflow is the path worth compiling anyway.

The override is undone in a `finally`, because `wm size` outlives the process: a leaked 1280dp would invalidate
every later journey in the same run and every macrobenchmark on that device afterwards, and the symptom — a phone
profile missing its bottom bar — would read as a code problem rather than a leaked shell command.

The sidebar needed test tags to be reachable at all; the toggle had none and the rows built theirs from an ad-hoc
string. Both now come from `KeiOsTestTags`.

### Phone acceptance after the whole Pad round — clean, `2026-08-18`

Pad AVD shut down, `KeiOS_API37_Validation` booted (427×952dp), current build installed. Every change in this
round is meant to be a no-op below 600dp; that was reasoned at each step and is now measured end to end:

| Check | Result |
|---|---|
| Home | floating **bottom** bar, no top bar, no sidebar toggle, hero and pills full width, no gutter |
| OS, title and chrome | title **centred**, actions at the phone's 14dp margin |
| OS, scrolled hard | bottom bar **still collapses** into the corner dock, and the page's floating dock still shrinks to `…` — the placement-gated `appNavigationVisible` did not disturb the phone |
| BA | cards full width, title centred, bottom bar in place |
| BA guide catalog route | rows, header pills, mini player, tab strip and search FAB all unchanged — the ten `appPageEdgePaddingStart/End()` swaps are no-ops where the gutter is `0.dp` |

Suite **2939 tests, 0 failures**; debug and release both assemble.

### Still owed on a real phone

**Nothing.** The last open item — background audio hardening — was driven on the AVD once it turned out the
emulator does have network, and it passes in both configurations. See the section above.

Everything on the phone side is verified. Large-screen behaviour is deliberately **not** in scope here —
see the note on the Home predicate — and is deferred to a dedicated Pad AVD.

## The Pad AVD — created `2026-08-18`

`KeiOS_Pad_API37_Validation`, on the same system image the phone AVD uses
(`system-images;android-37.1;google_apis_ps16k;arm64-v8a`), device profile `pixel_tablet`.

```bash
"$HOME/Library/Android/sdk/emulator/emulator" -avd KeiOS_Pad_API37_Validation
```

`avdmanager` creates that profile with defaults that are wrong for this app, so six values were raised to match
the phone AVD: `hw.gpu.enabled=yes` / `hw.gpu.mode=host` (the profile ships `no`/`auto`, and every Liquid Glass
surface is a `RenderEffect` — software rendering would measure nothing real), `hw.ramSize` 2G → 6G,
`vm.heapSize` 192M → 512M, `hw.cpu.ncore` 4 → 6, `disk.cachePartition.size` 66MB → 512MB. Orientation was set
to `landscape`, which is the real Pixel Tablet's natural one.

What it reports, and this is the point of it:

```
config: … sw800dp-w1280dp-h800dp-xlarge-notlong-… land-… xhdpi-… 2560x1600-v37
```

**`sw800dp`.** Comfortably past the 600dp line, so the two things the guide says stop working on large screens
are live here for the first time: `android:screenOrientation` is ignored, and
`PROPERTY_COMPAT_ALLOW_RESTRICTED_RESIZABILITY` — the opt-out — no longer exists at `targetSdk 37`. `h800dp`
also sits **above** `HomePageTallHeroMinHeight` (700dp), so Home takes the tall-hero branch on a panel only
800dp tall in landscape. That threshold was derived from phone geometry and is the first thing to re-check.

The app installs and runs: all five tabs render, no crash. It is plainly the phone layout stretched to 1280dp —
the status pill rows span the full width — which is the work itself, not a defect to file.

## Pad round 1 — the content column

### What was actually wrong

Not layout *breakage*. Everything rendered, nothing overlapped, nothing was unreachable. The failure was that
every row is drawn "label at the leading edge, controls at the trailing edge", and at 1280dp the two halves end
up ~1100dp apart with nothing between them. A row stops reading as a row when the eye has to cross the whole
panel to connect a switch to the thing it switches.

So: cap the content column and centre it, rather than redesign the pages. The cap is **720dp**
(`AppPageContentMaxWidth`), picked so that it sits below the Pad's portrait width (800dp) as well as its
landscape width (1280dp) — a row is then laid out **identically in both orientations**, and rotating the tablet
re-flows nothing. That property is worth more here than any particular number, because rotation is newly
reachable and should be unremarkable.

`appPageSideGutterFor(availableWidth)` is `0.dp` below the cap, so **no phone moves by a pixel** — every phone
this app installs on is 360–440dp wide. `AppPageContentWidthTest` pins that, both orientations of the Pad, the
boundary at exactly 720dp, and the split-screen case.

### Where it had to be applied, and why not in one place

The gutter is not just list padding. Anything anchored to a window edge has to come in with the content, or the
page splits: a toolbar pinned to the true edge sits a gutter's width — 280dp in landscape — from the list it
acts on. So it went into, in order of how much each covers:

| Site | Covers |
|---|---|
| `AppPageLazyColumn` → `appPageContentPadding(sideGutter=)` | 21 list call sites, free |
| `appPageEdgePadding()` | the status hub each main page pins *above* its list — a sibling of the list, so it never saw the list's content padding and stayed full-bleed while every row under it narrowed |
| `AppTopEndActionBarOverlay`, `AppTopBar` actions | the top-right toolbars |
| `appFloatingDockSidePadding()` | the four floating docks, which had four hand-written copies of the same expression and the same `14.dp`; now one |
| `TabbedPageBottomChrome` outer padding | the BA catalog's category bar and search dock |
| `HomePage` | builds its own padding, hero included — capping the overview cards but not the hero pills would split the page down the middle |

### Manifest

`screenOrientation="sensorPortrait"` **stays**, and is now commented as the phone-only declaration it has
become. Android ignores it at `sw >= 600dp` from targetSdk 36, and 37 removes the opt-out — so on the Pad it
already does nothing. Changing it to `fullUser` would not affect the tablet at all; it would only unlock a
*phone* landscape geometry that nothing is drawn for.

`android:resizableActivity` was tried and **fails the build** at compileSdk 37 — `attribute
android:resizableActivity not found`. The attribute is gone; there is nothing to opt into.

### Verified

Reinstalled and swept on `KeiOS_Pad_API37_Validation`, **both orientations**: Home, OS, MCP, GitHub, BA. Rows,
status hubs, top-end toolbars, floating docks and the bottom dock all sit on the same column. Suite **2924
tests, 0 failures**; debug and release both assemble.

Home needed no change: `h800dp` in landscape is above `HomePageTallHeroMinHeight` (700dp), so the tall hero is
correct there and the pill rows clear the floating dock. The threshold flagged for re-derivation survives
contact with real Pad geometry.

### Not done, and deliberately

- **The BA guide catalog's inner tab layouts** (`BaGuideCatalogV2ListContent`, `BaGuideStudentBgmTabContent`,
  `BaGuideMemoryLobbyTabContent`, the music chrome) build several paddings of their own. Their lists go through
  `AppPageLazyColumn` and so are already capped, but their non-list elements have not been swept.
- **Two-pane / list-detail.** A 1280dp panel could show a list and a detail side by side instead of one centred
  column, and for the catalog that is probably the right end state. It is a different and much larger piece of
  work — miuix-nav has no list-detail scaffold — and it is a design decision, not a defect. The centred column
  is correct on its own and is what a single pane should do at this width.

## Pad round 2 — the tab bar becomes a sidebar

Round 1 capped the content column, which was right but was still a phone layout with better margins. This round
replaces the *navigation* rather than repositioning it, following Apple's `sidebarAdaptable` tab view: one control
in two shapes, not two designs.

Sourced from the current Liquid Glass-era HIG (Tab bars, Sidebars, Materials — all revised 2025-12/2026-06), not
the pre-26 shape.

### Three placements, two thresholds that answer different questions

| Window | Navigation |
|---|---|
| `< 600dp` — phone, fold outer | floating **bottom** bar, exactly as before |
| `>= 600dp` | **top** tab bar, sharing the row with the title and the actions |
| `>= 660dp` **and** the user asked | **sidebar** |

**600dp, not round 1's 760dp.** 760dp is derived from two content columns and answers "is there room for two
panes". 600dp answers "is this a tablet-shaped window", and it is not a private boundary: it is what `sw600dp`
resource buckets key on and what the platform itself uses from targetSdk 36 to stop honouring an orientation
request. Navigation should change shape where the system already considers the app to be on a big screen.

**660dp is derived, not declared.** The HIG says a sidebar needs "a large amount of vertical and horizontal
space" and that a tab bar is better when space is limited. So a sidebar is allowed only while what it leaves
behind is still a viable content column: `AppSidebarWidth` 280 + `AppPaneMinWidth` 380. Below that the preference
is **kept but not applied**, so widening the window brings the sidebar back — which is what makes rotation a shape
change rather than a lost setting, and is the "responds automatically to rotation and window resizing" the
adaptable style promises.

### The top row is shared, and that is the whole point

`[ title | tab bar centred | actions ]`, in the row the title and actions already occupied. The bar therefore
costs **no vertical space**, so there is no top reservation to add — and the 112dp the floating bottom bar needed
is removed rather than paid twice.

Two consequences had to be handled. The scaffold's `bottomBar` slot is measured against the window's bottom edge,
so a top-aligned bar cannot live there; at regular width the same bar is composed inside the content instead.
And round 1's content gutter was pulling the actions 280dp inward into the centred bar — correct while the row
belongs to the page, wrong once it belongs to the app, so it is dropped at `Top` and kept everywhere else.

Pages learn the placement from `LocalAppNavigationPlacement` rather than deriving it, because all five are
composed at once and a bar drawn by each page's chrome would exist five times and slide with the swipe.

### The sidebar

**Regular Liquid Glass, not clear.** The Materials guidance names this case: "use the regular variant when
background content might create legibility issues, or when components have a significant amount of text, such as
alerts, **sidebars**, or popovers". Only the *selected* row takes a glass fill of its own — five glass plates
inside a glass rail would be the overuse the same guidance warns against, and would flatten the distinction the
material exists to draw.

**The toggle exists in both shapes**, as the adaptable style requires: at the leading edge of the top row in
tab-bar shape, and inside the rail in sidebar shape. A sidebar is never a state the user cannot leave.

**Only the closing half of the edge gesture is ported.** The HIG says iPadOS users expect the built-in edge
swipe, but on Android the leading edge belongs to the **system back gesture**, so claiming a leading-edge drag to
*open* a sidebar would fight predictive back on the same pixels. This repo has been bitten there before — issue
#21 was `navSwipeDismiss` claiming horizontal drags parent-first and taking them from sliders, switches and text
fields. So the gesture lives on the rail: drag it inward past half its width and it converts back.

**The preference is persisted** in `UiPrefs.sidebarNavigationPreferred`, not held in composition state. It is a
choice, and the adaptable style already keeps it through a window too narrow to honour it; losing it on a cold
start would make the sidebar something the user re-asks for every launch.

### Found on the device, not by reasoning

Three defects, all invisible in source review:

1. **The bar did not render at all.** Composed as the first child of the content box, it was covered by the page.
   Navigation is a functional layer *above* the content layer, so it has to be the last child.
2. **The rail and a redundant tab bar coexisted.** The overlay condition was `!= Bottom` instead of `== Top`, and
   the page's own title card was hidden underneath a bar that no longer had a job.
3. **The centred bar overlapped the trailing toolbar at 650dp.** At 1280dp a 388dp bar and the actions have room
   to spare. The bar's safe width is the window minus *twice* the toolbar's reserve — symmetric, because a
   centred element is only clear of one side if it is equally clear of the other — and where that still is not
   enough, the title yields, since at `Top` the tab bar already names the section.

### Verified

| Geometry | Result |
|---|---|
| Pad 1280×800dp landscape | both shapes; title, bar, actions clear of each other |
| Pad 800×1280dp portrait | sidebar honoured (800 ≥ 660) |
| 650dp | falls back to the top bar; no toggle offered, because nothing there could fit one; title yields |
| 500dp | falls back to the floating bottom bar |
| force-stop and relaunch | returns in the sidebar shape |
| drag the rail inward | converts back to the tab bar |
| **Phone AVD, 427×952dp** | **unchanged** — bottom bar, centred title, 14dp margins, no gutter, no toggle |

Suite **2937 tests, 0 failures**; debug and release both assemble.

### Still owed

- ~~The background extension effect~~ **done, and it was my own debt.** The rail used to get its strip by
  insetting the *pager*, which insets a page's background along with its content: OS looked fine because it paints
  nothing, Home showed a seam where its gradient stopped at the rail's outer edge. Nothing is inset now. The pager
  fills the window, so a page's artwork runs the full width and shows through the rail's glass, and content is
  pushed clear by an **asymmetric** gutter instead — `appPageSideGutterStart()` adds the rail's width, the trailing
  side does not, because only the leading edge has a rail on it. That is the same arrangement the top tab bar
  already used, and the one the guidance describes for the whole functional layer.

  The cost was the sweep I predicted: 31 paired edge paddings across 12 files went from one symmetric helper to
  two, plus seven sites that had stored the symmetric value in a local and had to be split by hand (Home, the BA
  page, both bottom chromes, the floating docks, the top bar's leading chrome). `appContentWidth()` now excludes
  the rail, so the gutter centres the column in the area beside it rather than in the window. The source assertion
  moved with it and now pins that the two sides use *different* helpers, which is the part worth holding.
- **The split view** — a section on the left and the route it opened on the right — is untouched. It needs
  `NavDisplay` restructured so a route occupies a pane instead of the window, which is the highest-risk change in
  this repo and belongs in its own round.
- ~~The BA guide catalog's inner tab layouts~~ **done.** Round 1's note said these hosts went through
  `AppPageLazyColumn` and would inherit the cap; they did not. The keep-alive wrapper was converted, the column
  underneath it is a raw `LazyColumn` with its own `contentPadding`, so the gutter never reached the route — the
  whole catalog was still 1280dp wide on the Pad. Ten page-edge paddings across seven files now go through
  `appPageEdgePadding()`, and the BGM bottom chrome is capped at its `BoxWithConstraints`, which caps the tab
  strip, the mini player and the search field together because all three derive from its `maxWidth`. A source
  assertion that pinned the raw token was updated to pin the helper instead, which is the more useful contract:
  the token stays right *inside* a card, where there is no page edge to centre against.

## API 37 is versioned in minors, and the app compiles against 37.0

Recorded 2026-08-31, after a local SDK refresh installed the 37.2 platform. This is a landscape note,
not a change: nothing was migrated, and the reason is below.

API 37 ships minor revisions that each carry their own `android.jar` and their own SDK extension level:

| platform | extension level | android.jar | system images published | AVDs |
| --- | --- | --- | --- | --- |
| `android-37.0` | 22 | 43.0 MB | 5 variants x 2 arch, incl. wear and playstore | — |
| `android-37.1` | 23 | 43.3 MB | 2 variants x 2 arch | all four run this |
| `android-37.2` | 24 | 43.8 MB | **none under the stable id** | — |
| `android-37.2-beta1/2/3` | — | not installed | `google_apis_ps16k` + `google_apis_playstore_ps16k`, both arches | — |

That last row is the correction to an earlier draft of this section, which said flatly that no 37.2
system image exists. It does not exist under the *stable* `android-37.2` id — but three of them are
published under the **beta** platform ids, and `android-37.2-beta3/google_apis_ps16k/arm64-v8a` is the
same variant family every KeiOS AVD already runs. "Not published yet" and "published under a name I did
not grep for" are different facts, and only the second one is true here.

The shape of that table is itself the thing to watch: Google is still iterating this API level. 37.0
shipped the full image matrix, 37.1 narrowed to two variants, and 37.2 stable currently ships a compile
surface with **no** stable emulator image behind it at all.

The app pins `compileSdkMinor = 0` in **two** files — `app/build.gradle.kts` and
`ui-liquid-glass/build.gradle.kts` — so `compileSdk = 37` means 37.0 specifically, regardless of what
the local SDK has installed. A newer platform appearing on the machine changes nothing about the build,
which is the point of the pin.

Worth knowing that the emulators run a **higher** minor than the app compiles against: every AVD is on
`android-37.1`. That is fine in the direction it runs — a 37.1 device executing a 37.0-compiled app —
and it is why an extension-23 API can be present at runtime on the AVD while invisible at compile time.

### What 37.2 would add, and why it is not adopted

Diffed class-for-class against 37.0: **184 classes added, 0 removed** — purely additive, so raising the
minor could not break anything. But most of the surface is unreachable for an app like this one:

| package | classes | reachable here |
| --- | --- | --- |
| `android.app.personalcontext.*` | 38 | no — assistant-role APIs |
| `android.app.privatecompute.*` | 6 | no — system/assistant |
| `android.app.contentsafety.*`, `android.agenticon.*` | 8 | no |
| `android.hardware.input.*` customization | 7 | no |
| `android.app.MemoryBudgetManager`, `android.content.pm.MemoryBudgetInfo` | 2 | **yes** |

`MemoryBudgetManager` is the one genuinely interesting entry, because it is app-usable — self-imposed
package and process budgets with over-budget listeners:

```
setProcessBudgetBytes(long) / getProcessCurrentUsageBytes()
registerProcessOverBudgetListener(Looper, OnOverBudgetListener)
```

That is a live lead for `os/kei/memory/AppMemoryRelease.kt`, which drives release levels off
`ComponentCallbacks2.TRIM_MEMORY_COMPLETE`, `TRIM_MEMORY_RUNNING_CRITICAL` and `TRIM_MEMORY_MODERATE` —
all three of which the platform now marks deprecated, and the compiler warns about on every full build.

It is **not** taken yet, but the reason is narrower than it first looked. There is no *stable* 37.2
emulator image, so the exact platform the app would compile against cannot be booted. What can be
booted is `android-37.2-beta3`, whose `google_apis_ps16k/arm64-v8a` image matches the variant the
existing AVDs use — so a budget-listener path is testable, on a beta of the minor rather than the minor
itself.

That leaves a real question rather than a blocker: whether `MemoryBudgetManager`'s signatures are
identical between `37.2-beta3` and stable `37.2`. Until that is checked against beta3's own
`android.jar`, a beta3 AVD proves the API runs, not that it runs as the shipping platform declares it.
The honest status is "runnable on a beta, unverified against stable", and the class diff above is the
receipt for picking it up.

The NDK also gained `30.0.16138531` in the same refresh. Irrelevant here — this app has no
`externalNativeBuild`, no `ndkVersion`, and no `CMakeLists.txt` outside the vendored
`.tmp/Shizuku-API-reference` tree.

## Media notification revalidation — 2026-10-23

This follow-up covers the media changes after published v1.18.0. It does not describe a new
published release. Minimum API 35 and target API 37 are preserved.

### Platform and dependency contract

Android 13's PlaybackState-derived controls remain relevant, but they are not the complete Android 17
contract. [Android 17 background audio hardening](https://developer.android.com/about/versions/17/changes/bg-audio)
also requires a visible activity or an eligible foreground service; a background app targeting 37 needs
the service's while-in-use capability. Media3 continues to own the media session, audio focus, foreground
notification and standard controls. Transient interruptions retain its normal lifecycle behavior.

The [Media3 release page](https://developer.android.com/jetpack/androidx/releases/media3) lists **1.11.1**
as the latest stable release on this audit date. The runtime graph resolves all used Media3 modules to
that version, including the now-explicit datasource dependency. A scoped constraint resolves the external
`androidx.media:media` compatibility dependency from 1.7.0 to stable 1.8.0. This does not replace Media3's
internally bundled legacy-session implementation. The available 1.12 alpha is outside this project's
stable/RC dependency policy.

[Xiaomi's media notification contract](https://dev.mi.com/xiaomihyperos/documentation/detail?pId=2161)
uses standard session state and may hide custom controls. The optional
[Island share extension](https://dev.mi.com/xiaomihyperos/documentation/detail?pId=2162) decorates a clone
of the Media3 notification; it does not substitute the token, cover, actions, channel or content intent.
Each asynchronous result keeps its original share snapshot. A missing share URL clears the extension.
Non-HyperOS devices receive the delegate result unchanged. The small icon is monochrome on all devices;
the optional HyperOS extension uses the OS version properties instead of broad manufacturer heuristics.

### Artwork and lifecycle changes

The session URI loader now uses the existing GameKee HTTP configuration, preserving its Referer, user
agent and other request headers. Both the URI path and later artwork hydration use the MediaSession bitmap
limit. Hydration no longer replaces the initial cover with the catalog's 256-pixel thumbnail. Decode uses
a relaxed sampling bound before precise aspect-preserving scaling to the session limit; PNG encoding is
lossless. The payload cache is bounded by both 32 entries and 8 MiB.

Controller connection generations reject obsolete completions, release pending futures correctly and
invalidate disconnected controllers. Artwork can join an intentional connection but cannot open a new
session itself. Reordered queues use the new queue's index. Mutating custom commands require a trusted
controller or the recognized notification controller. Explicit Stop acknowledges the command before
releasing the session/player, withdrawing the notification and stopping the service, including when
another controller remains bound. A subsequent explicit connection can create a new session.

### Current verification

The minified, non-debuggable validation package is isolated as `os.kei.mediaqa20261023`. Its entry point
invokes the production native controller, mapper and service. It uses two real BGM URLs and portrait
covers obtained from the current GameKee student content API/CDN. The source-input hashes, APK hashes,
signatures, mappings, decoded cover exports and system dumps are retained locally under
`.planning/media-notification-android17-20261023/`.

| Gate | Observed result |
|---|---|
| Focused regression tests | 89 tests in 24 classes; no failures, errors or skips |
| Release artifact | Production Release and isolated validation ReleaseDiagnostic assemble with R8; signatures, min/target SDK, 16 KiB ZIP alignment and embedded Profile/intermediate equality verified |
| Physical HyperOS 4 / API 37 | Playing prepare, background playback, pause/resume, seek, repeat, A/B switching, queue reorder, terminal Stop and reconnection exercised; standard MediaStyle notification/token and framework session recognized |
| Physical audio focus | Real granted transient, duck and permanent requests exercised; transient loss suppresses playback with playWhenReady retained and automatically resumes on gain; permanent loss clears playWhenReady and requires explicit resume |
| Physical native Island controls | Three consecutive Next taps update A/B title, portrait and duration; expanded and collapsed covers inspected; native pause/resume and Stop verified against framework state |
| Physical cover data | Both metadata keys retain 404×456 pixels; ALBUM_ART and DISPLAY_ICON are byte-for-byte equal after decoding. Every opaque pixel matches the corresponding source portrait |
| Framework notification covers | Expected A/B identity retained; NotificationManager scales them to 138×156 on the phone and 116×132 on AOSP. These platform thumbnails do not replace the session's full cover |
| AOSP API 37 | Foreground playback, Home/background playback, background media-key pause/resume, next, seek, repeat, reorder, terminal Stop and reconnection pass |
| Strict-mode readback | `mHardeningOverride=3`, corresponding to THROW; background AudioTrack reports `state:started`, `mutedState:none`; service reports mediaPlayback type `0x2` and `allowWiu:12` |
| Extra binding excluded | The strict background/pause/resume sequence was repeated after revoking the validation package's notification-listener access and restarting its process; no listener service remained bound |
| Terminal cleanup | Live session, service and notification disappear even with the inspection controller bound; an explicit reconnection succeeds |
| Visual inspection | AOSP SystemUI and physical HyperOS notification/Island show the current artwork and standard transport controls |
| Device preservation | Seven existing package identities and notification-listener settings unchanged; both existing phone players restored to their initial PLAYING state; no existing app was uninstalled or cleared |

The physical phone was `25098PN5AC`, HyperOS `OS4.0.4.0.XBLCNXM`, API 37. The first physical round stayed
paused to preserve existing playback; the subsequent user-authorized round exercised actual playing
state and native SystemUI controls. Its evidence is under `.planning/media-physical-full-20261023/`.
The updated isolated validation APK has SHA-256
`ce0cb060bc99c9369c9cd51626298d410aa77c03923930c3c9cf893beee11864`.
The same 16 production/test/dependency inputs still match the accepted test and Release build manifest.

The phone's existing `mIgnrMusicFocusReq=true` policy allows music focus requests to coexist. The focus
probe therefore used real AudioManager requests with voice-communication/speech attributes; it did not
inject player callbacks or change the OEM policy. Framework duck loss state and uninterrupted playback were
verified. Media volume was already zero and was preserved, so neither this check nor the playback checks
claim acoustic listening or measured duck attenuation. No QA notification-listener access was granted.
Transient/permanent interruption tests also paused the two pre-existing players; both were explicitly
restored through their uniquely identified sessions after the QA session was stopped. The isolated QA
app remains installed but stopped, with no live session, service, notification or focus request.

The AOSP image was
`Android/sdk_phone64_arm64/emu64a:17/CE2A.260420.051.A3/16571990:userdebug/test-keys`, running as a task-owned
AVD with a regular app UID. Its emulator used `-no-audio`: AudioTrack state was measured, not acoustic output.
The HyperOS-AVD reserved by another project was not used for this validation.

Physical global-media-key routing is not an accepted gate: no global key was dispatched to the phone.
After cleanup, both original players were PLAYING with started AudioTracks, but their stream/port volume
was muted and the framework media-button target was null. A targeted original-session pause/play did not
restore the previous target. In the
[AOSP Android 17 session stack](https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android17-release/services/core/java/com/android/server/media/MediaSessionStack.java),
removing the selected session clears that target. Its
[audio playback configuration](https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android17-release/media/java/android/media/AudioPlaybackConfiguration.java)
also excludes volume-muted tracks from active playback. The phone's observation is consistent with those
platform mechanisms; this does not establish the exact OEM implementation or prove that its previous
global routing was restored. Accepted physical controls are the targeted session and native Island
operations above. No global session policy or OEM hook was changed to force a target.

The current AOSP image implements the following command in its
[android17-release shell source](https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android17-release/services/core/java/com/android/server/audio/AudioManagerShellCommand.java):

```sh
adb -P 5057 -s emulator-5590 shell cmd audio set-hardening throw
adb -P 5057 -s emulator-5590 shell dumpsys audio
# Verify mHardeningOverride=3; exit status alone is insufficient.
```

The guide's older `set-enable-hardening` spelling exits successfully without enabling this image's
override. The August records remain historical and do not establish current strict-mode coverage.
No physical-phone hardening setting was changed. Failed initial harness entry/file-fixture attempts are
retained for diagnosis and excluded from the accepted device results.

Full `lintRelease` remains blocked by 111 errors located in files unchanged by this task; no Error/Fatal
location belongs to the changed media inputs. `lintVitalRelease` passes. The committed-ref Profile
freshness check at the pre-change base passed and the existing profiles are packaged correctly; that
result does not establish coverage of the new runtime changes. The final committed-ref check reports
STALE for seven runtime paths and the compatibility dependency change. New Profile collection and the
normal release gates remain necessary before shipping these changes as a new release.
