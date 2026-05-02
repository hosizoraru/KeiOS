package os.kei.ui.page.main.ba.card

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import os.kei.ui.page.main.ba.BaLiquidPanel
import os.kei.ui.page.main.widget.glass.AppLiquidIconButton
import os.kei.ui.page.main.widget.glass.AppLiquidTextButton
import os.kei.ui.page.main.widget.glass.GlassVariant
import com.kyant.backdrop.Backdrop
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun BaCardHeader(
    title: String,
    modifier: Modifier = Modifier,
    titleIconRes: Int? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                color = MiuixTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            titleIconRes?.let { iconRes ->
                Image(
                    painter = painterResource(id = iconRes),
                    contentDescription = title,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        trailing?.let {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                content = it,
            )
        }
    }
}

@Composable
internal fun BaInlineActionPanel(
    backdrop: Backdrop?,
    buttonText: String,
    buttonIconRes: Int? = null,
    countdownText: String,
    timeText: String,
    accentColor: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    val countdownBlue = Color(0xFF60A5FA)
    BaLiquidPanel(
        backdrop = backdrop,
        modifier = Modifier.fillMaxWidth(),
        accentColor = accentColor,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (buttonIconRes != null) {
                AppLiquidIconButton(
                    backdrop = backdrop,
                    painter = painterResource(id = buttonIconRes),
                    contentDescription = buttonText,
                    onClick = {
                        if (enabled) onClick()
                    },
                    onLongClick = onLongClick,
                    variant = GlassVariant.Content,
                    width = 52.dp,
                    height = 40.dp,
                    iconTint = Color.Unspecified
                )
            } else {
                AppLiquidTextButton(
                    backdrop = backdrop,
                    text = buttonText,
                    textColor = accentColor,
                    containerColor = accentColor,
                    enabled = enabled,
                    variant = GlassVariant.Content,
                    onClick = onClick,
                    onLongClick = onLongClick,
                )
            }
            Text(
                text = countdownText,
                color = countdownBlue,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = timeText,
                color = accentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
