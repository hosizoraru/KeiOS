package com.example.keios.ui.page.main.ba

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.keios.R
import com.example.keios.ui.page.main.widget.GlassTextButton
import com.example.keios.ui.page.main.widget.GlassVariant
import com.kyant.backdrop.Backdrop

@Composable
internal fun BaDebugCard(
    backdrop: Backdrop?,
    onSendApTestNotification: () -> Unit,
    onSendCafeVisitTestNotification: () -> Unit,
    onSendArenaRefreshTestNotification: () -> Unit,
    onTestCafePlus3Hours: () -> Unit,
) {
    val accentAmber = Color(0xFFF59E0B)

    BaGlassCard(
        backdrop = backdrop,
        accentColor = accentAmber,
        accentAlpha = 0f,
    ) {
        BaCardHeader(title = "Debug")

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GlassTextButton(
                    backdrop = backdrop,
                    text = stringResource(R.string.ba_debug_action_ap_notification),
                    textColor = Color(0xFF3B82F6),
                    variant = GlassVariant.SheetPrimaryAction,
                    onClick = onSendApTestNotification,
                )
                GlassTextButton(
                    backdrop = backdrop,
                    text = stringResource(R.string.ba_debug_action_arena_refresh_notification),
                    textColor = Color(0xFF3B82F6),
                    variant = GlassVariant.SheetPrimaryAction,
                    onClick = onSendArenaRefreshTestNotification,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GlassTextButton(
                    backdrop = backdrop,
                    text = stringResource(R.string.ba_debug_action_cafe_plus_3h_ap),
                    textColor = Color(0xFF3B82F6),
                    variant = GlassVariant.SheetPrimaryAction,
                    onClick = onTestCafePlus3Hours,
                )
                GlassTextButton(
                    backdrop = backdrop,
                    text = stringResource(R.string.ba_debug_action_cafe_visit_notification),
                    textColor = Color(0xFF3B82F6),
                    variant = GlassVariant.SheetPrimaryAction,
                    onClick = onSendCafeVisitTestNotification,
                )
            }
        }
    }
}
