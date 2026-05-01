package os.kei.ui.page.main.student.section.gallery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import os.kei.R
import os.kei.ui.page.main.widget.glass.LiquidCircularProgressBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun GuideInlineVideoUnavailableHint() {
    Text(
        text = stringResource(R.string.guide_gallery_video_unavailable),
        color = MiuixTheme.colorScheme.onBackgroundVariant
    )
}

@Composable
internal fun GuideInlineVideoStatusHints(
    isBuffering: Boolean,
    loadError: String?
) {
    if (isBuffering) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LiquidCircularProgressBar(
                progress = { 0.35f },
                size = 14.dp,
                strokeWidth = 2.dp,
                activeColor = Color(0xFF60A5FA),
                inactiveColor = Color(0x3360A5FA)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.guide_gallery_video_loading),
                color = MiuixTheme.colorScheme.onBackgroundVariant
            )
        }
    }

    loadError?.takeIf { it.isNotBlank() }?.let { err ->
        Text(
            text = stringResource(R.string.guide_gallery_video_failed_with_reason, err),
            color = MiuixTheme.colorScheme.error,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
