# GitHub Widget: System Layout and Material Study

Research date: 2026-10-27. This records a design reference and implementation
boundary, not Xiaomi catalog admission or physical-device acceptance.

## References and observations

- Xiaomi's [Super Island expanded-state guide](https://dev.mi.com/xiaomihyperos/documentation/detail?pId=2142),
  rechecked on 2026-10-27, reports an update time of 2026-01-29. Its image/text
  examples group an identifying image, primary information and supporting text
  closely together. The local template PDF provides the visual examples. This
  hierarchy is useful for a persistent widget; its notification protocol and
  template restrictions are not widget APIs.
- The installed Calendar 18.0.1.30 includes date, month, agenda and countdown
  previews. The date card combines a large day number with a complete month grid;
  the agenda card places a primary date beside repeated title/detail pairs.
  These are packaged preview resources, not recordings of live widget behavior.
- Notes 3.2.7.9's small preview separates the content area from its primary action.
  Its neutral placeholders do not establish how a populated note renders.
- App Vault 25.41.83.10-09291040 includes a classic clock MAML widget. Its
  `manifest.xml` uses `BackgroundBlurConfig`, `GlassConfig`, a material-state
  updater, wallpaper light/dark state and a separate stroke treatment. Glass is
  disabled inside the picker. Accordingly, a picker screenshot cannot establish
  the final material effect.
- Calendar resources with `blur` in their names include ordinary solid rounded
  drawables. A resource name alone does not prove background sampling.

The inspected versions, APK hashes and extracted references are recorded in
ignored local evidence.
No Xiaomi artwork, shader, MAML implementation or parameter array is shipped in
KeiOS.

## Design applied in KeiOS

The direction is **persistent tracking panel**: a quiet summary rail and closely
grouped project information, with a brighter accent reserved for actionable
updates. The layout makes the middle of the card useful:

- Wide content from 300 dp uses two columns. A 160 dp panel shows four projects;
  a 220 dp panel shows six. Each group combines status icon, name and candidate
  version or check state. Normal text sizes can also show the installed version
  recorded by the same valid check.
- Narrow wide panels use one detail column. Tall panels place the same
  name/detail groups vertically and keep the check action at the bottom.
  When a tall expanded panel contains at most four projects, normal text sizes
  use the available row height for installed-version detail instead of leaving
  that space empty.
- The wide panel's 48 dp check target occupies the summary rail, rather than taking height
  away from the entire detail panel.
- Supporting totals and the actual check timestamp remain next to the data.
  Unknown, failed, paused and ignored-release results retain their own meaning.
- Larger fonts reduce secondary detail and row count before essential text is
  clipped. Privacy mode replaces project details with six count statistics;
  updates replace the content view so old accessibility descriptions are not reused.

The portable material uses a translucent neutral base, a diffuse radial light,
a diagonal reflection, an outer edge and a quieter inner edge. Project groups
and the check action have a separate translucent surface. Day/night color roles
protect text contrast. Native text, vectors and XML drawables remain sharp when
the host resizes; no bitmap of the entire widget is transported.
Surface regression checks composite the actual background, tile and action
drawables over white and black. They require at least 4.5:1 contrast for the
small primary, secondary and warning text roles in the sampled text regions.
Night surfaces use a stronger base and quieter reflections to keep highlights
from washing out labels.

## Material boundary

The observed clock is a MAML widget rendered by Xiaomi's host. It is not an
example of `GlassConfig` accepted by a standard Android `RemoteViews` tree.
Android's documented [widget enhancement APIs](https://developer.android.com/develop/ui/views/appwidgets/enhance)
and the local framework sources were checked: remote method invocation requires
host-side `RemotableViewMethod` support. A hidden API invoked in KeiOS cannot
by itself add a custom renderer to the launcher's process.

This implementation provides the layered soft-light appearance. It does not
claim live wallpaper blur, refraction, wallpaper-dependent native material
switching or parity with Xiaomi's clock glass. No catalog approval flags,
privileged host changes, wallpaper capture, continuous animation or periodic
material polling are introduced. A future native material integration needs a
verified host contract, admission path and an ordinary-widget fallback.

See [the widget implementation](github-project-widget.md) for usage and
verification boundaries.
