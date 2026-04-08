package com.example.keios.ui.page.main.widget

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.RoundedRectangle
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Text

@Composable
fun MiuixExpandableSection(
    backdrop: Backdrop?,
    title: String,
    subtitle: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (backdrop != null) {
                    Modifier.drawBackdrop(
                        backdrop = backdrop,
                        shape = { RoundedRectangle(22.dp) },
                        effects = {},
                        highlight = { Highlight.Default.copy(alpha = 0.75f) },
                        shadow = { Shadow.Default.copy(color = Color.Black.copy(alpha = 0.08f)) },
                        onDrawSurface = {
                            drawRect(Color.White.copy(alpha = 0.52f))
                        }
                    )
                } else {
                    Modifier.background(Color.White.copy(alpha = 0.62f))
                }
            )
            .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(22.dp))
    ) {
        BasicComponent(
            title = title,
            summary = subtitle,
            onClick = { onExpandedChange(!expanded) },
            endActions = {
                Text(text = if (expanded) "收起" else "展开")
            }
        )
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                content()
            }
        }
    }
}
