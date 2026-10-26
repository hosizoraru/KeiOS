# Liquid Glass: MeiloX study, material and interaction

Date: 2026-10-26.

## Inputs and scope

KeiOS baseline: `46901371b8dd402eda9bc3f9f039cb307a55c0f2`.
MeiloX reference: `0d05b4ab30b05d87e1a393d8fef11cda9d54d9f9`, inspected from the local checkout.
Both projects resolve Backdrop **2.0.1**. KeiOS uses Compose **1.12.1**, Miuix
`0.9.4-685dceb7-SNAPSHOT`, and Android **35** as its minimum API.

This is a source-informed engineering study, not a formal clean-room exercise.
KeiOS retains its Apache-2.0 license. This change imports no MeiloX source,
shaders, assets, or tuning constants. Its implementation follows the existing
KeiOS renderer, its regression cases, Android rendering contracts, and the
shared Apache-2.0 Backdrop API.

The visual direction remains the existing Miuix glass: continuous corners,
screen-anchored refraction, layered highlights, and native foreground rendering.
The first iteration changes invalidation and cache correctness. The follow-up
improves gesture lifetime and the distinction between value motion and press
feedback. Neither iteration retunes the material, blur, refraction, color
separation, motion specifications, or capture resolution.

## What the reference demonstrates

The relevant MeiloX sources are
[GlassBackdrop](https://github.com/NEORUAA/MeiloX/blob/0d05b4ab30b05d87e1a393d8fef11cda9d54d9f9/app/src/main/java/com/ljyh/mei/ui/glass/GlassBackdrop.kt),
[GlassMorphRenderer](https://github.com/NEORUAA/MeiloX/blob/0d05b4ab30b05d87e1a393d8fef11cda9d54d9f9/app/src/main/java/com/ljyh/mei/ui/glass/GlassMorphRenderer.kt),
[SheetBackdropRenderer](https://github.com/NEORUAA/MeiloX/blob/0d05b4ab30b05d87e1a393d8fef11cda9d54d9f9/app/src/main/java/com/ljyh/mei/ui/glass/SheetBackdropRenderer.kt),
and [GlassBottomBar](https://github.com/NEORUAA/MeiloX/blob/0d05b4ab30b05d87e1a393d8fef11cda9d54d9f9/app/src/main/java/com/ljyh/mei/ui/glass/GlassBottomBar.kt).
Its [third-party notices](https://github.com/NEORUAA/MeiloX/blob/0d05b4ab30b05d87e1a393d8fef11cda9d54d9f9/THIRD_PARTY_NOTICES)
also identify the shared Backdrop and interaction references.

| Mechanism | Observation | Application to KeiOS |
| --- | --- | --- |
| Separate capture and overlays | Glass controls live outside the content that supplies their backdrop. | Preserve the existing scene/material/foreground separation and avoid sampling children back into their own parent material. |
| Stable source and effect layers | Morphs reuse graphics resources and invalidate a capture when a new transition begins. | Retain work whose inputs are unchanged, while preserving live source and geometry invalidation. |
| Coordinate mapping | Cross-window sampling uses screen transforms; stationary sampling compensates for animated glass transforms. | Preserve KeiOS's window boundary and inverse sampling behavior; a faster cache is invalid if it drags the background with the glass. |
| Filter bandwidth | The morph renderer defaults to half-scale filtered buffers, and the sheet blur uses quarter-scale buffers. Foreground and edge treatments have separate paths. | These are different rendering inputs. Their frame cost alone cannot establish an equivalent-quality speedup over KeiOS's native-resolution path. |
| Interaction state | Tab dragging separates preview position, release selection, and current callbacks. | Preserve the current Miuix interaction contract and keep business updates distinct from visual animation. |
| Resource lifetime | Graphics layers and auxiliary renderers are scoped and released. | Keep the existing detach cleanup and bounded shader pool; do not retain a sampled screen globally. |

MeiloX's appearance and frame rate also depend on its scenes, control density,
material settings, and device. This study does not claim a measured ranking
between the applications. The controlled comparison is KeiOS before and after
the patch, with the same release configuration and journey.

KeiOS already has analytic rendering for uniform backdrops, retained sheet
material, pooled shader builders, and cached decorations. Replacing these with
another renderer would discard useful work and repeat earlier rejected
experiments. The actionable difference here is the lifetime of each retained
recording.

## Implemented changes

The owning implementation remains in
[SheetGlassBackdrop.kt](../../ui-liquid-glass/src/main/java/os/kei/ui/page/main/widget/glass/SheetGlassBackdrop.kt).

Previously every modifier update rebuilt the optical chain, including updates
that changed only the surface fill, foreground callback, export destination, or
sampling transform. The existing separate snapshot observation scopes also fed
one shared export-dirty flag, so a foreground update re-recorded the material
that nested controls sampled.

The updated renderer handles each dependency separately:

| Changed input | Optical chain | Source recording | Material export | Foreground export |
| --- | --- | --- | --- | --- |
| Effect/shape callback or observed optical state | Rebuilt | Invalidated | Invalidated | Invalidated |
| Size, pixel density, font scale, or layout direction | Rebuilt | Invalidated | Invalidated | Invalidated |
| Sampling transform callback | Retained | Invalidated | Invalidated | Retained unless its own inputs change |
| Surface callback or observed surface state | Retained | Retained when safe | Invalidated | Retained |
| Foreground callback or observed foreground state | Retained | Retained when safe | Retained | Invalidated |
| Export destination | Retained | Retained when safe | Reconnected | Reconnected |

Unknown or non-layer backdrop implementations still use the existing
conservative source recording path. Position changes still update sampling and
foreground coordinates. A single combined export still re-records when either
of its constituent draws changes; independent export reuse requires the existing
separate material and foreground layers.

Equivalent outline providers keep their cached geometry across modifier
updates. The shared
[outline cache](../../ui-liquid-glass/src/main/java/os/kei/ui/page/main/widget/glass/FlatLiquidBackdrop.kt)
also includes `Density.fontScale`: a custom shape is allowed to depend on it,
so equal pixel density alone is insufficient for reusing an outline.

This follows the dependency model described in Android's
[Compose phases](https://developer.android.com/develop/ui/compose/performance/phases)
and [drawing modifiers](https://developer.android.com/develop/ui/compose/graphics/draw/modifiers)
guides. The pinned upstream
[effect scope](https://github.com/Kyant0/AndroidLiquidGlass/blob/2.0.1/backdrop/src/commonMain/kotlin/com/kyant/backdrop/BackdropEffectScope.kt)
remains responsible for effect geometry, padding, and filter construction.

## Source and build verification

- Two new regressions failed against the baseline: material recoloring and
  sampling-transform replacement both configured the unchanged optical chain
  a second time.
- **66 focused tests passed**, covering the retained renderer, live source
  updates, sampling transforms, material exports, independent foreground
  updates, optical dependency changes, font-scale invalidation, flat rendering,
  sheet resizing/dismissal/nested scroll, and control accessibility.
- The native Robolectric tests check pixels and invalidation behavior. They do
  not execute hardware RenderEffect shaders or establish GPU performance.
- Debug and releaseDiagnostic APK builds passed, including R8 and release
  diagnostic lint checks. Minimum API and dependency versions are unchanged.

Raw build logs, regression results, APK hashes, device captures, and measurement
inputs are kept in the ignored `.planning/liquid-glass-meilox-20261026/` task
folder. The baseline and candidate diagnostic APKs package Baseline Profiles
from the same checked-in input files. Their rewritten profile binaries need not
be identical. A new profile was not collected for this comparison.

## Physical-device verification

The device reported model `25098PN5AC`, API **37**, a **1220 x 2656** display,
and density **520 dpi**. The target was the separately installed `os.kei.diag`
releaseDiagnostic package with R8 enabled. Every trial checked the installed
APK hash. The normal installation and the existing Phone AVD were not targets.

The first comparison using only the pre-existing profile produced different
draw-stage distributions, including slower candidate resize recordings. It is
not evidence of a win. To control ART compilation coverage, the final comparison
compiled **both** diagnostic APKs using `cmd package compile -m speed -f` and
used the same warm-up sequence.

Eight completed trials followed two ABBA blocks, with four trials per variant.
The same GitHub check-logic sheet was opened, toggled four times back to the
original draft, partially resized, expanded, and scrolled. There were **3,613
completed frames** in the main comparison. Frame intervals were approximately
**8.32-8.33 ms**. No screen mirroring or screen recording ran during timing.
The reported thermal status remained 0; this is a device report, not independent
proof of thermal headroom.

| Journey | Baseline total p50, ms | Candidate total p50, ms |
| --- | ---: | ---: |
| Open sheet | 24.88 | 24.67 |
| Switch roundtrip | 11.69 | 11.54 |
| Partial resize | 20.86 | 20.63 |
| Expand | 29.02 | 27.93 |
| Fixed-endpoint scroll, separate control | 32.98 | 32.66 |

Values are medians of trial-level p50s, not pooled-frame confidence estimates.
The main journeys have overlapping trial ranges; these results do **not**
establish an overall frame-rate improvement.

The original swipe's final content anchors varied, so its static image and
scroll timing were insufficient to decide a regression. A separate ABBA
control used the same pointer-move sequence and a pause before release to
eliminate the variable fling. All four trials had matching final content
anchors, **40 frames each**, and both baseline/candidate pairs had
**byte-identical** sheet-interior pixels. Its timing also remains close to
baseline; the extra input processes make its absolute time unsuitable as a
shipping scroll budget.

For all four main pairs, **1,711,976 half-height interior pixels were
byte-identical**. Full-height comparisons differed in at most two pixels by
more than two channel levels; the same pixels also varied between repeated
captures of the same variant. Resize poses with different coordinates and
fling poses with different content anchors were excluded from direct pixel
comparison instead of being counted as optical differences.

The supported conclusion is narrower than a frame-rate claim: unchanged
optical callbacks no longer reconfigure the filter chain during material
updates, and observed foreground changes retain the separately exported
material. The regression tests prove those reductions and the hardware checks
preserve the tested appearance. The remaining frame cost still includes
full-resolution optical rendering and compositing.

The final installed candidate was also inspected at half height, after changing
a switch (`Synced` to `Unsaved`), after restoring it, during resize, and at full
height. A separate motion recording was reviewed outside the timing runs.
The diagnostic draft returned to `Synced`, its process was stopped, the prior
foreground app was restored, and task-owned remote recording files were removed.

This does not establish cold-install performance, fresh Baseline Profile
coverage, battery savings, other devices, or tablet acceptance. Profile
collection and publication gates remain separate from this renderer change.

## Follow-up: interaction ownership and authoritative state

The additional reference is
[GlassControls](https://github.com/NEORUAA/MeiloX/blob/0d05b4ab30b05d87e1a393d8fef11cda9d54d9f9/app/src/main/java/com/ljyh/mei/ui/glass/GlassControls.kt):
local previews return to the selected state on cancellation, current callbacks
are separate from remembered animation objects, and restoring preferences can
move a control without representing a thumb press. These are behavioral
requirements for the independent KeiOS implementation, rather than imported
MeiloX code. Android's
[gesture guide](https://developer.android.com/develop/ui/compose/touch-input/pointer-input/understand-gestures)
documents pointer-input keys and event propagation.

The changes stay in `ui-liquid-glass`:

| Area | Previous failure | Result |
| --- | --- | --- |
| Gesture lifetime | Resetting a pointer-input key could abandon an active gesture without invoking cancellation. | Every started inspection terminates with either completion or cancellation, including coroutine cancellation. Damped controls release their press state even if a completion/cancellation callback throws. |
| Remembered animation ownership | Replacing an animation or highlight could leave its existing pointer-input node driving the old object. | Input restarts for the new owner and cancels the previous preview. A slider's new range, or a highlight's changed appearance, takes effect on the next gesture. |
| Value motion versus press feedback | External switch updates and navigation settlement could manufacture another press/release sequence. | Those transitions animate the value without starting a new press. Actual touch feedback and the existing explicit slider-tap response remain. |
| Controlled switches | When the parent declined an update without changing `checked`, the track and thumb could remain in the optimistic state while semantics still reported the original value. | Each completed interaction reconciles with the parent's authoritative value, including rejected updates. Canceling a drag discards its local preview. |
| Switch update path | A snapshot observer animated every local fraction change in addition to direct drag updates. | Dragging updates the thumb directly; external state and release reconciliation own the settling animation. |

The initial seven new regressions exposed four failures before the fix:
pointer-input reset, animation-owner replacement, highlight-owner replacement,
and rejected switch updates. Final focused verification includes **89 tests
across 21 classes**, with no failures, errors, or skips. Coverage includes:

- Key reset, target removal, system cancellation, and subsequent gestures.
- Releasing an old highlight and rebinding the new animation owner.
- Value-only motion without a phantom press or releasing an existing finger.
- Rejected switch updates and canceled drags returning to the original pixels.
- Disable/re-enable during a press, slider range changes, callbacks, vertical
  scrolling, RTL, reduced motion, and control accessibility.
- Navigation preview without premature selection, single completion on release,
  cancellation, disablement, RTL ordering, and callback replacement during drag.
- Existing material invalidation, outline, button, card, navigation and
  recomposition regressions.

The preceding renderer timing and pixel results belong to the first iteration.
They are not measurements of this follow-up. Follow-up build and physical
evidence is recorded separately in the ignored
`.planning/liquid-glass-meilox-followup-20261026/` folder.

Debug and releaseDiagnostic builds passed, including R8, resource shrinking and
lintVital. Both APK signatures and package identities were verified; minSdk
remains 35 and targetSdk 37. The follow-up diagnostic APK was installed and its
device-side SHA-256 matched the built artifact.

On the same physical API 37 phone:

- A real switch drag followed by `CANCEL` left the check-logic draft `Synced`.
  A tap changed it to `Unsaved`; another tap restored it. No modified draft was
  saved.
- Dragging the navigation capsule from GitHub toward OS kept GitHub visible
  during preview and after cancellation. Repeating and releasing selected OS.
- With the diagnostic background setting temporarily enabled, the opacity
  slider moved from 35% to the 42% preset after release. Tapping restored 35%;
  cancellation followed by another interaction also worked. Both opacity and
  the originally disabled background setting were restored. The device's UI
  hierarchy reported an enabled node even when the owning background setting
  disabled the slider; acceptance used the actual setting and observed value.
- All **1,926,000 pixels** in the selected static sheet region matched the
  accepted first-iteration APK. That region also matched after cancellation
  and after the switch roundtrip. The first-switch crop matched independently.
  These are static comparisons of the same pose, not proof of animation timing.

Held switch and slider captures, navigation preview/cancel/release captures,
and sampled frames from an 18-second hardware recording were reviewed. They
retain the inspected press deformation, refraction and foreground rendering.

No new frame-time study or Baseline Profile collection was performed for the
follow-up. The supported result is more reliable input and state reconciliation,
with the inspected glass material and touch feedback retained.
