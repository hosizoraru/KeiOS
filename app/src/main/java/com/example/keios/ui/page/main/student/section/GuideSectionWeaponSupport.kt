package com.example.keios.ui.page.main.student.section

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import com.example.keios.R
import com.example.keios.ui.page.main.widget.glass.AppDropdownSelector
import com.example.keios.ui.page.main.widget.glass.GlassVariant
import com.example.keios.ui.page.main.widget.motion.appMotionFloatState
import com.kyant.backdrop.Backdrop
import top.yukonga.miuix.kmp.basic.Text

@Composable
internal fun GuidePressableMediaSurface(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressScale by appMotionFloatState(
        targetValue = if (pressed) 0.994f else 1f,
        durationMillis = 120,
        label = "guide_media_press_scale"
    )
    val pressOverlayAlpha by appMotionFloatState(
        targetValue = if (pressed) 0.065f else 0f,
        durationMillis = 130,
        label = "guide_media_press_overlay"
    )
    Box(
        modifier = modifier
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        content()
        if (pressOverlayAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF1E3A8A).copy(alpha = pressOverlayAlpha))
            )
        }
    }
}

@Composable
internal fun GuideEffectLevelPicker(
    backdrop: Backdrop?,
    levelOptions: List<String>,
    selectedLevel: String,
    showLevelPopup: Boolean,
    onTogglePopup: () -> Unit,
    onDismissPopup: () -> Unit,
    onLevelSelected: (Int) -> Unit
) {
    if (levelOptions.isEmpty()) return
    var levelPopupAnchorBounds by remember { mutableStateOf<IntRect?>(null) }
    AppDropdownSelector(
        selectedText = selectedLevel,
        options = levelOptions,
        selectedIndex = levelOptions.indexOf(selectedLevel).coerceAtLeast(0),
        expanded = showLevelPopup,
        anchorBounds = levelPopupAnchorBounds,
        onExpandedChange = { expanded ->
            if (expanded) onTogglePopup() else onDismissPopup()
        },
        onSelectedIndexChange = onLevelSelected,
        onAnchorBoundsChange = { levelPopupAnchorBounds = it },
        backdrop = backdrop,
        variant = GlassVariant.Compact
    )
}

@Composable
internal fun GuideWeaponStarBadgeRow(
    starLabel: String,
    iconSize: Dp
) {
    val count = parseWeaponStarCount(starLabel)
    if (count <= 0) {
        Text(
            text = starLabel,
            color = Color(0xFFEC4899)
        )
        return
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(count.coerceAtMost(5)) {
            Image(
                painter = painterResource(R.drawable.ba_weapon_star_badge),
                contentDescription = null,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

internal fun parseWeaponStarCount(starLabel: String): Int {
    return Regex("""(\d{1,2})""")
        .find(starLabel)
        ?.groupValues
        ?.getOrNull(1)
        ?.toIntOrNull()
        ?: 0
}

internal fun showLoadingText(loading: Boolean, hasInfo: Boolean): Boolean {
    return loading && hasInfo
}
