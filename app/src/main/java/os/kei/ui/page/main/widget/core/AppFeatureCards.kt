package os.kei.ui.page.main.widget.core

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.shapes.RoundedRectangle
import os.kei.ui.page.main.widget.glass.GlassVariant
import os.kei.ui.page.main.widget.glass.LiquidSurface
import os.kei.ui.page.main.widget.glass.UiPerformanceBudget
import os.kei.ui.page.main.widget.glass.resolvedGlassBlurDp
import os.kei.ui.page.main.widget.glass.resolvedGlassLensDp
import os.kei.ui.page.main.widget.motion.appExpandIn
import os.kei.ui.page.main.widget.motion.appExpandOut
import os.kei.ui.page.main.widget.motion.appMotionFloatState
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun AppSurfaceCard(
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = null,
    containerColor: Color = MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.64f),
    borderColor: Color = MiuixTheme.colorScheme.onBackgroundVariant.copy(alpha = 0.16f),
    contentColor: Color = MiuixTheme.colorScheme.onBackground,
    showIndication: Boolean = true,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(CardLayoutRhythm.cardCornerRadius)
    val interactionSource = remember { MutableInteractionSource() }
    val clickable = onClick != null || onLongClick != null
    val useLiquidClick = onClick != null && onLongClick == null
    val clickModifier = if (clickable && !useLiquidClick) {
        Modifier.combinedClickable(
            interactionSource = interactionSource,
            indication = null,
            role = Role.Button,
            onClick = { onClick?.invoke() },
            onLongClick = onLongClick
        )
    } else {
        Modifier
    }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressedScale by appMotionFloatState(
        targetValue = if (showIndication && clickable && !useLiquidClick && isPressed) {
            0.992f
        } else {
            1f
        },
        durationMillis = 120,
        label = "app_surface_card_press_scale"
    )
    val localBackdrop = rememberLayerBackdrop()
    val cardBackdrop = backdrop ?: localBackdrop
    val blurRadius = resolvedGlassBlurDp(UiPerformanceBudget.backdropBlur, GlassVariant.Content)
    val lensRadius = resolvedGlassLensDp(UiPerformanceBudget.backdropLens, GlassVariant.Content)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = pressedScale
                scaleY = pressedScale
            }
    ) {
        if (backdrop == null) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .layerBackdrop(localBackdrop)
            )
        }
        LiquidSurface(
            backdrop = cardBackdrop,
            modifier = Modifier
                .fillMaxWidth()
                .then(clickModifier),
            shape = RoundedRectangle(CardLayoutRhythm.cardCornerRadius),
            isInteractive = showIndication && clickable,
            surfaceColor = containerColor,
            blurRadius = blurRadius,
            lensRadius = lensRadius,
            interactionSource = interactionSource,
            onClick = if (useLiquidClick) onClick else null
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                content = content
            )
        }
    }
}

@Composable
fun AppFeatureCard(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = null,
    eyebrow: String? = null,
    eyebrowColor: Color = MiuixTheme.colorScheme.onBackgroundVariant.copy(alpha = 0.74f),
    containerColor: Color = MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.64f),
    borderColor: Color = MiuixTheme.colorScheme.onBackgroundVariant.copy(alpha = 0.16f),
    contentColor: Color = MiuixTheme.colorScheme.onBackground,
    titleColor: Color = contentColor,
    subtitleColor: Color = MiuixTheme.colorScheme.onBackgroundVariant.copy(alpha = 0.90f),
    sectionIcon: ImageVector? = null,
    collapsible: Boolean = false,
    expanded: Boolean = true,
    onExpandedChange: (Boolean) -> Unit = {},
    showIndication: Boolean = true,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    headerEndActions: (@Composable RowScope.() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(
        start = CardLayoutRhythm.cardHorizontalPadding,
        end = CardLayoutRhythm.cardHorizontalPadding,
        bottom = CardLayoutRhythm.cardVerticalPadding
    ),
    contentVerticalSpacing: Dp = CardLayoutRhythm.sectionGap,
    content: @Composable ColumnScope.() -> Unit
) {
    val headerClick = when {
        collapsible -> ({ onExpandedChange(!expanded) })
        onClick != null -> onClick
        else -> null
    }
    AppSurfaceCard(
        modifier = modifier,
        backdrop = backdrop,
        containerColor = containerColor,
        borderColor = borderColor,
        contentColor = contentColor,
        showIndication = showIndication,
        onClick = if (collapsible) null else onClick,
        onLongClick = onLongClick
    ) {
        AppCardHeader(
            title = title,
            subtitle = subtitle,
            eyebrow = eyebrow,
            eyebrowColor = eyebrowColor,
            titleColor = titleColor,
            subtitleColor = subtitleColor,
            startAction = sectionIcon?.let { icon ->
                {
                    top.yukonga.miuix.kmp.basic.Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = titleColor
                    )
                }
            },
            endActions = headerEndActions,
            expandable = collapsible,
            expanded = expanded,
            expandTint = titleColor,
            onClick = headerClick,
            onLongClick = onLongClick
        )
        AnimatedVisibility(
            visible = !collapsible || expanded,
            enter = appExpandIn(),
            exit = appExpandOut()
        ) {
            AppCardBodyColumn(
                contentPadding = contentPadding,
                verticalSpacing = contentVerticalSpacing,
                content = content
            )
        }
    }
}
