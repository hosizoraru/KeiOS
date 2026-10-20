@file:Suppress("FunctionName")

package os.kei.ui.page.main.widget.glass

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.highlight.HighlightStyle
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.capsule.ContinuousCapsule
import os.kei.ui.page.main.widget.isAppInDarkTheme
import androidx.compose.ui.graphics.colorspace.ColorModel
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import os.kei.ui.page.main.widget.shape.appSquircleBackground
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun AppLiquidBadgedIcon(
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
    badgeLabel: String? = null,
    badgeColor: Color? = null,
    badgeContentColor: Color? = null,
    badgeBackdrop: Backdrop? = null,
) {
    val label = badgeLabel?.takeIf { it.isNotBlank() }
    if (label == null) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = modifier,
            tint = tint,
        )
        return
    }

    Box(
        modifier =
            Modifier
                .defaultMinSize(
                    minWidth = AppLiquidBadgedIconAnchorSize,
                    minHeight = AppLiquidBadgedIconAnchorSize,
                ).semantics { stateDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = modifier,
            tint = tint,
        )
        AppLiquidIconBadge(
            label = label,
            color = badgeColor,
            contentColor = badgeContentColor,
            backdrop = badgeBackdrop,
            modifier =
                Modifier
                    .appRoundHostBadgePlacement()
                    .clearAndSetSemantics {},
        )
    }
}

/**
 * A count badge.
 *
 * Pass [backdrop] to get the Liquid material: the badge then refracts what is behind it and carries a
 * rim highlight and inner shadow like the rest of the family, instead of reading as an opaque sticker
 * pasted onto the glass. Without a backdrop it keeps the flat squircle fill, which is correct for
 * badges sitting on ordinary content.
 *
 * The fill stays mostly opaque even on glass, and that is deliberate rather than timid. Apple's
 * Materials guidance is that Liquid Glass exists to *reveal* the content beneath it — but a badge's job
 * is the opposite, to be read instantly at 10sp. So the glass here supplies the optics (refraction at
 * the rim, the specular edge, depth) while the tint still carries the colour. The same guidance also
 * says to use the material sparingly, which is why this is opt-in per call site rather than switched on
 * globally for every badge in the app.
 */
@Composable
internal fun AppLiquidIconBadge(
    label: String?,
    modifier: Modifier = Modifier,
    color: Color? = null,
    contentColor: Color? = null,
    backdrop: Backdrop? = null,
) {
    val resolvedLabel = label?.takeIf { it.isNotBlank() } ?: return
    val isDark = isAppInDarkTheme()
    val colors =
        resolveLiquidBadgeColors(
            containerColor = color,
            contentColor = contentColor,
            defaultContainerColor = MiuixTheme.colorScheme.error,
            defaultContentColor = MiuixTheme.colorScheme.onError,
            surfaceColor = MiuixTheme.colorScheme.surfaceContainer,
        )
    val activeBackdrop = activeGlassBackdrop(backdrop)
    val liquidFlat = liquidFlatField(activeBackdrop)
    val shape = remember { ContinuousCapsule }
    val badgeHighlight: () -> Highlight? = {
        Highlight(
            width = 0.8.dp,
            blurRadius = 1.4.dp,
            alpha = if (isDark) 0.46f else 0.72f,
            style = HighlightStyle.Ambient(if (isDark) 0.74f else 0.60f),
        )
    }
    // Not `Shadow.Default`: its 24dp blur spreads 48dp around an 18dp badge, which leaves
    // no corner rounding visible at all and puts a right angle under a capsule.
    val badgeShadow: () -> Shadow? = {
        liquidGlassShadow(Color.Black.copy(alpha = if (isDark) 0.22f else 0.16f))
    }
    val badgeInnerShadow: () -> InnerShadow? = {
        InnerShadow(radius = 2.dp, alpha = if (isDark) 0.20f else 0.14f)
    }
    val badgeSurface: DrawScope.() -> Unit = {
        drawRect(colors.containerColor.copy(alpha = AppLiquidIconBadgeFillAlpha))
    }
    val surfaceModifier =
        if (activeBackdrop != null && liquidFlat != null) {
            Modifier.drawFlatLiquidBackdrop(
                field = liquidFlat,
                shape = { shape },
                highlight = badgeHighlight,
                shadow = badgeShadow,
                innerShadow = badgeInnerShadow,
                onDrawSurface = badgeSurface,
            )
        } else if (activeBackdrop != null) {
            Modifier.drawBackdrop(
                backdrop = activeBackdrop,
                shape = { shape },
                effects = pooledLiquidEffects(LocalLiquidShaderPooling.current) {
                    vibrancy()
                    blur(AppLiquidIconBadgeBlur.toPx())
                    safeLiquidLens(
                        AppLiquidIconBadgeLensHeight.toPx(),
                        AppLiquidIconBadgeLensAmount.toPx(),
                    )
                },
                highlight = badgeHighlight,
                shadow = badgeShadow,
                innerShadow = badgeInnerShadow,
                onDrawSurface = badgeSurface,
            )
        } else {
            Modifier.appSquircleBackground(
                color = colors.containerColor,
                cornerRadius = AppLiquidIconBadgeMinSize / 2,
            )
        }
    Box(
        modifier =
            modifier
                .defaultMinSize(
                    minWidth = AppLiquidIconBadgeMinSize,
                    minHeight = AppLiquidIconBadgeMinSize,
                ).then(surfaceModifier)
                // Tighter than it was. Solving the placement against the host's circle instead of its
                // bounding box necessarily pulls the badge inward, which buys clearance from the rim at
                // the cost of covering more of the icon — so the badge gives some width back.
                .padding(horizontal = 4.dp, vertical = 1.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = resolvedLabel,
            color = colors.contentColor,
            fontSize = 10.sp,
            lineHeight = 12.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/**
 * Places a badge inside a round host so its corner lands on the rim rather than through it.
 *
 * Reads the host's size from its own incoming constraints, which is exact here because the badge is a
 * direct child of a fixed-size `Box`. See [appFloatingDockBadgeOffsetPx] for why aligning to the
 * bounding box instead is the one placement guaranteed to escape a capsule.
 */
internal fun Modifier.appRoundHostBadgePlacement(inlay: Dp = AppLiquidIconBadgeRimInlay): Modifier =
    layout { measurable, constraints ->
        val placeable = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
        if (!constraints.hasBoundedWidth || !constraints.hasBoundedHeight) {
            return@layout layout(placeable.width, placeable.height) { placeable.place(0, 0) }
        }
        val offset =
            appFloatingDockBadgeOffsetPx(
                hostWidthPx = constraints.maxWidth.toFloat(),
                hostHeightPx = constraints.maxHeight.toFloat(),
                badgeWidthPx = placeable.width.toFloat(),
                badgeHeightPx = placeable.height.toFloat(),
                inlayPx = inlay.toPx(),
            )
        layout(constraints.maxWidth, constraints.maxHeight) {
            placeable.place(offset.x.roundToInt(), offset.y.roundToInt())
        }
    }

/** Keeps the badge clear of the host's glass rim highlight rather than resting against it. */
internal val AppLiquidIconBadgeRimInlay = 1.5.dp

internal data class LiquidBadgeColors(
    val containerColor: Color,
    val contentColor: Color,
)

internal fun resolveLiquidBadgeColors(
    containerColor: Color?,
    contentColor: Color?,
    defaultContainerColor: Color,
    defaultContentColor: Color,
    surfaceColor: Color,
): LiquidBadgeColors {
    val validDefaultContainer = defaultContainerColor.finiteRgbOrNull()
    val requestedContainer = containerColor.finiteRgbOrNull()
    val resolvedContainer = requestedContainer ?: validDefaultContainer ?: Color.Black

    contentColor.finiteRgbOrNull()?.let { explicitContent ->
        return LiquidBadgeColors(
            containerColor = resolvedContainer,
            contentColor = explicitContent,
        )
    }

    val validDefaultContent = defaultContentColor.finiteRgbOrNull()
    val usesDefaultContainer = requestedContainer == null || requestedContainer == validDefaultContainer
    if (usesDefaultContainer && validDefaultContainer != null && validDefaultContent != null) {
        return LiquidBadgeColors(
            containerColor = resolvedContainer,
            contentColor = validDefaultContent,
        )
    }

    val effectiveSurface = surfaceColor.finiteRgbOrNull()?.copy(alpha = 1f) ?: Color.White
    val effectiveContainer = resolvedContainer.compositeOver(effectiveSurface)
    val blackContrast = glassContrastRatio(Color.Black, effectiveContainer)
    val whiteContrast = glassContrastRatio(Color.White, effectiveContainer)
    return LiquidBadgeColors(
        containerColor = resolvedContainer,
        contentColor = if (blackContrast >= whiteContrast) Color.Black else Color.White,
    )
}

private fun Color?.finiteRgbOrNull(): Color? {
    val candidate = this ?: return null
    if (!candidate.isSpecified || candidate.colorSpace.model != ColorModel.Rgb) return null
    return candidate.takeIf {
        candidate.red.isFinite() &&
            candidate.green.isFinite() &&
            candidate.blue.isFinite() &&
            candidate.alpha.isFinite()
    }
}

private val AppLiquidBadgedIconAnchorSize = 34.dp
internal val AppLiquidIconBadgeMinSize = 18.dp

private val AppLiquidIconBadgeBlur = 3.dp
private val AppLiquidIconBadgeLensHeight = 7.dp
private val AppLiquidIconBadgeLensAmount = 11.dp

/**
 * How much of the tint survives on glass.
 *
 * High on purpose. A notification count has to be legible at 10sp against whatever happens to be
 * behind the dock, so the refraction and rim do the Liquid work while the colour stays a colour.
 */
private const val AppLiquidIconBadgeFillAlpha = 0.90f
