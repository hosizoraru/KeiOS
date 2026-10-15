# Compose recomposition investigation — 2026-10-13

The cloud investigation reproduced avoidable composition work in shared Sheet content, IME-driven
floating chrome, Slider key points, guide-media progress, page insets, and bottom-bar/account-pager
state. A second residual scan also reproduced the BGM player's playback-clock updates. The changes remove the hot
state reads or move them into a smaller component. They preserve the current material, input
behavior, and module ownership.

Source baseline: `08d87c55454a1fc174be6acb911734c2e8ca1cb3`. Kotlin `2.4.21-RC`, Compose `1.12.1`,
Miuix `0.9.4-5c91d5e5-SNAPSHOT`, API 35 minimum,
and API 37.0 compile SDK remain the build inputs.

## Reproduced work and resulting scope

| Flow | Evidence on original source | Result on changed source |
| --- | --- | --- |
| Resize an overflowing Sheet viewport eight times, with height read only in layout | Eight extra Recomposer changes, and eight duplicate `true` overflow reports | Zero extra Recomposer changes; reports change only with overflow or the reporter callback |
| Change IME height ten times with floating chrome mounted | Ten host compositions beyond the initial composition | Zero extra host compositions; final placement follows the newest inset |
| Cross Slider key points through six progress updates | Six recomposition rounds in each light/LTR and dark/RTL fixture | Runtime composition tracing records no `LiquidTrackSlider` executions during updates; only the relevant dot scopes update |
| Advance voice playback progress ten times | Source collected and read the complete voice snapshot at the guide-page root | Zero extra page compositions in the playback fixture; the progress ring reads the final progress, and the playback action still switches correctly |
| Advance the guide loading animation through 20 measured frames | 20 extra page compositions | Zero extra page compositions; the indicator still advances to 0.9 |
| Animate the search placeholder height | 15 extra body compositions in the original direct-padding fixture; expanding the shared padding helper still caused 12 in the follow-up fixture | Zero extra body compositions during the measured animation; the body moves by the full search height |
| Change a live inset eight times in a tablet/RTL single-list, dual-list (with/without header), and dual-pane fixture | 32 executions of the four mounted page-layout scopes | Zero executions; all four content/header markers still move by the expected 32 dp |
| Hold the selected bottom-bar tab down through 20 measured animation frames | 16 extra tab-item executions | Zero extra tab-item executions; text grows under the press transform and returns to its resting size after release |
| Update gallery audio through nine 200 ms ticks | Nine full media-card executions | Zero media-card executions; the seek slider updates separately and the time labels follow whole seconds |
| Change gallery seek preview eight times during one seek | Eight polling-effect scope executions | Zero during preview deltas; ending the seek still invalidates the observer |
| Move the account pager through eight offsets inside the same effects range | Eight executions for each of the two page-content probes | Zero; crossing the 0.99-page boundary still switches effects correctly |
| Update BGM playback through nine 200 ms ticks | Nine full mini-player executions | Zero; slider semantics, the displayed clock, and play/pause controls still follow runtime state |

These are composition counts from JVM Compose tests, not device frame times. The original Sheet,
IME, Slider, and residual regression cases failed before their fixes. The voice fixture verifies the new
deferred path and playback actions; no on-device playback timing comparison was collected.

### Sheet content

`SheetContentColumn` used `scrollState.maxValue` as a `LaunchedEffect` key. Resizing its viewport
therefore recomposed the container and restarted the effect every time layout changed that maximum.
It now observes the boolean overflow decision with `snapshotFlow`. `SheetContentLazyColumn` uses the
same approach for `canScrollForward || canScrollBackward`, so reaching a list edge does not pull the
container into composition merely to rediscover that it still overflows.

The observers also track reporter replacement without disposing the live scroll source. New
callbacks receive the current decision, and disposal resets the latest callbacks. Nested-scroll
arbitration continues to read the actual scroll state synchronously through `LiquidSheetContentScroll`.
It does not depend on the asynchronous overflow report.

The regression tests cover actual grabber drags in both directions, bottom anchoring, overflow
transitions, lazy-list edges, and callback replacement/disposal. The existing Sheet tests cover
adaptive height, nested scrolling, flings, dismissal, unsaved changes, and presentation boundaries.

### Floating chrome and the IME

Returning `State<Dp>` had already kept animated placement reads out of composition, but computing
the animation target still read IME padding at the calling composition scope on every inset frame.
`rememberAppFloatingKeyboardLiftState` now observes the inset target in a coroutine and exposes the
`Animatable` state. New targets cancel the preceding animation and retain the existing 160 ms tween.

Initialization samples the current inset without registering a composition read. This is limited
to the initial sample: the lifetime observer is installed by `LaunchedEffect`. It preserves correct
placement when mounted with an already-visible keyboard. Tests also cover overlapping inset updates,
the resting bottom gap, and returning to zero after the keyboard closes.

### Slider key points

The active-color `derivedStateOf` was read inside the slider's `BoxWithConstraints` composition.
Crossing a point therefore re-entered the material and gesture subtree. Each dot now has its own
restart scope. Explicitly colored dots do not subscribe to active-color changes. The animation's
initial value is sampled without subscribing the slider to its first external progress update;
the existing `snapshotFlow` continues to own subsequent updates.

Miuix's SDF background stays in use. A direct cached-path drawing experiment changed light-mode
edge pixels and was rejected. The accepted implementation produced byte-identical RGBA pixels for
all 12 captured Slider fixtures: six values each in light/LTR and dark/RTL, including a dot with an
explicit color. These fixtures use the no-backdrop fallback; they do not establish arbitrary glass
scene equivalence. The glass effect code and parameters were not altered.

### Guide voice playback

The route continues to collect the same lifecycle-aware voice `StateFlow`. A derived playback
snapshot exposes only URL and playing state to the page. Progress travels as a stable provider
through the pager, tab renderer, and voice card, and is read by the progress ring's draw and
accessibility semantics. Other tabs receive an inactive progress provider.

### Loading, gallery audio, and BGM clocks

The guide loading animation now returns its `State<Float>` instead of reading the animated value at
the route. The same 520/1,800/260 ms animations remain, with progress read at the indicator.

Gallery audio passes a progress provider to the existing music slider. Time formatting runs in a
leaf scope and its derived values change on whole-second boundaries. The playback polling effect
observes whether a seek is active, rather than the continuously changing preview fraction. Starting
and ending a seek retain their lifecycle behavior.

The BGM mini-player receives the lifecycle-collected runtime `State` and derives only playback
controls, duration, and volume at its root. Playback progress and the whole-second clock are read by
the slider and time-label leaf. Seek-preview projection retains the current duration and clamped
position calculation. The regression verifies progress semantics, clock text, and the pause action.
The floating BGM chrome's expansion still changes font and artwork geometry during its transition;
those composition reads were retained because the selected visual transition requires them.

### Page insets and navigation controls

Maintenance follow-up (2026-10-16): the old top-bar search host and scaffold search switches had
no production consumers; only their dedicated fixtures enabled them. That path, its animated
placeholder, three search-only scaffold cases, and the top-bar search screenshot have been retired.
The production floating search dock and its tests remain. The live-inset measurement behavior and
tablet/RTL regression below remain relevant; the earlier search-placeholder result is historical.

Search-height padding is represented by a remembered `PaddingValues` wrapper. The shared content
padding helper and dual-column layouts also preserve live inset getters, including the header's
top inset and the separate column-bottom padding. The inset is therefore read during padding/list
measurement instead of rebuilding page content each frame. Existing tablet sidebar-gutter and
dual-pane geometry tests, the new tablet/RTL moving-inset fixture, and search interaction/screenshot
checks cover the affected layout contracts.

Bottom-bar press deformation previously supplied a new animation target through composition on
every frame. A `snapshotFlow` observer now retargets the item's `Animatable`, keeping the existing
160 ms tween and graphics-layer transform. Physical press/release tests verify both scope counts
and transformed text width. Continuous pager selection was already discretized, and its regression
passed before any bottom-bar change; that protection remains.

The account pager derives its effects-enabled boolean from the offset. It only invalidates page
content when that boolean changes. The 0.99-page activation distance and Miuix spring remain.

## Verification and reproduction

The final combined run passed 144 tests: 115 in `ui-liquid-glass` and 29 in `app`, with no failures,
errors, or skipped tests. This includes the existing search screenshot, Sheet behaviors, tablet
layout contracts, and the added runtime-scope regressions. Debug consumers and full Release Kotlin
compilation passed.
The R8-minified `releaseDiagnostic` build also passed its release lint and packaging tasks. Its
`os.kei.diag` APK is `app/build/outputs/apk/releaseDiagnostic/app-releaseDiagnostic.apk`, version
`1.17.0-diag` / `11700999`, with existing `baseline.prof` and `baseline.profm` assets present.
The existing Compose QA gate now runs 11 relevant regression/interaction classes in quick mode:

```bash
scripts/qa/compose_audit_gate.sh --quick --no-fetch
```

The cloud run uses `--offline` with the preinstalled dependency cache. The ordinary online invocation
attempted to refresh Miuix SNAPSHOT metadata and received GitHub Packages HTTP 401; this did not
affect the completed cached builds. `--offline` also disables fetching the optional audit init script.
The complete quick gate passed with `--quick --offline`, including the existing Markdown parser
tests and all 11 regression/interaction classes. The gate defaults to two Gradle workers; set
`COMPOSE_QA_MAX_WORKERS` to override it. The cloud retry cleared idle compiler daemons after an
OOM kill interrupted the first gate attempt. APK signature verification also passed; manifest
inspection confirmed `minSdk=35`, `targetSdk=37`, and `arm64-v8a`.

For the broader affected tests and a Release compiler report:

```bash
./gradlew \
  :ui-liquid-glass:testDebugUnitTest \
  --tests 'os.kei.ui.page.main.widget.sheet.*' \
  --tests 'os.kei.ui.page.main.widget.glass.AppFloatingKeyboardLift*' \
  --tests 'os.kei.ui.page.main.widget.glass.LiquidSlider*' \
  --tests 'os.kei.ui.page.main.widget.chrome.Liquid*BottomBar*' \
  :app:testDebugUnitTest \
  --tests os.kei.ui.page.main.student.section.GuideSectionVoiceInteractionTest \
  --tests os.kei.ui.page.main.student.page.support.GuideSyncProgressRecompositionTest \
  --tests os.kei.ui.page.main.student.section.gallery.GuideGalleryAudioRecompositionTest \
  --tests os.kei.ui.page.main.student.catalog.component.BaGuideBgmMiniPlayerRecompositionTest \
  --tests os.kei.ui.page.main.student.catalog.component.bgm.BaGuideBgmChromeMiniPlayerTest \
  --tests os.kei.ui.page.main.student.StudentGuideLocalBackdropFallbackTest \
  --tests 'os.kei.ui.page.main.ba.card.BaAccountPager*' \
  --tests os.kei.ui.page.main.widget.chrome.AppPageScaffoldInteractionTest \
  --tests os.kei.ui.page.main.widget.chrome.AppPageInsetRecompositionTest \
  --tests os.kei.ui.page.main.widget.chrome.AppPageSidebarGutterTest \
  --tests os.kei.ui.page.main.widget.chrome.AppPageTwoColumnPanesTest \
  :ui-liquid-glass:compileReleaseKotlin :app:compileReleaseKotlin \
  -PcomposeCompilerReports=true -Pkotlin.incremental=false \
  -Proborazzi.test.verify=true --max-workers=2
```

Incremental compiler reports can describe only the files compiled during that invocation. The
initial incremental `app` report contained just 24 composables, whereas full compilation reported
3,630 at baseline. Full Release reports confirm Strong Skipping was already enabled. There is no
compiler-flag change or blanket stability annotation in this work. Skippable counts alone do not
measure the dynamic state-read problems addressed here.

Local evidence is in the ignored `.planning/compose-performance-20261013/` directory:

- `baseline/`: full compiler metric JSON, original failing-test XML/logs, and original Slider PNGs.
- `after/`: final compiler metric JSON, full focused-test XML/logs, and accepted Slider PNGs.
- `test-summary.json` and `pixel-comparison.json`: test totals and pixel comparisons.
- `after/apk-summary.json`: first-round diagnostic artifact details.
- `residual/baseline/` and `residual/after/`: additional failing regressions and final validation.
- `residual/after/test-summary.json`: final 144-test totals, with XML and full Release module reports
  in the module subdirectories.
- `residual/after/apk-summary.json`: final diagnostic APK identity, size, SHA-256, packaged profile
  sizes, and signature/manifest verification. It supersedes the first-round artifact evidence.

Setting `KEIOS_COMPOSE_CAPTURE_DIR` before the Slider test saves the six PNGs for each fixture to that
local directory. The capture hook is optional; the runtime scope and semantics assertions always run.

## Device evidence still required

This cloud worker has no usable Android target or KVM/AVD. Robolectric native graphics at API 35
establishes the tested composition, semantics, layout, and fixture-rendering behavior; it cannot
establish Android RenderThread cost, GPU time, refresh-rate deadlines, or a frame-rate improvement.

The existing [Sheet frame-cost record](../planning/liquid-sheet-frame-cost.md) documents substantial
historical glass-layer cost while dragging or scrolling. The current source still draws those
layers, so removing the reproduced composition work does not establish that every frame spike is
resolved. A new comparable release-device capture must identify the remaining stage before further
rendering changes are accepted.

Use `os.kei.diag` with local fixture data and the same phone/tablet, window size, material settings,
refresh regime, and mirroring state for each comparison. Measure these phases separately:

1. Open a long eager-content Sheet and a lazy-content Strategy Sheet.
2. Drag each grabber up and down, then scroll its content in both directions.
3. Open and close the keyboard in a Sheet input while floating chrome is present.
4. Scrub a key-point Slider and play voice audio while scrolling its tab.
5. Expand/collapse search, press bottom-bar tabs, swipe account cards, and play gallery/BGM audio.

Capture Perfetto and per-frame `gfxinfo` stages under the project's
[measurement setup](../../.claude/skills/keios-frame-performance/references/measurement.md).
The existing `MainNavigationFrameBenchmarks.liquidSheetMotionBaselineProfile` journey separates
Sheet expansion, content scrolling, and collapse; use the same journey for a comparable capture.
Only claim Compose timing if the capture actually contains the corresponding runtime events.

The committed-ref Baseline Profile freshness gate passed for the source baseline. The runtime edits
add helper/dot methods and change media signatures, so the existing profile is not fresh for this
series of commits.
Recollect the profile on the final source with an explicitly selected disposable collector before
shipping; packaged existing profile files alone do not establish freshness for this change.
