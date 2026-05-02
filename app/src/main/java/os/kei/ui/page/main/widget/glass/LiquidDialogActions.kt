package os.kei.ui.page.main.widget.glass

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun LiquidDialogActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color? = null,
    textColor: Color = if (containerColor != null) {
        MiuixTheme.colorScheme.onPrimary
    } else {
        MiuixTheme.colorScheme.primary
    },
    variant: GlassVariant = if (containerColor != null) {
        GlassVariant.SheetPrimaryAction
    } else {
        GlassVariant.SheetAction
    }
) {
    GlassTextButton(
        backdrop = null,
        text = text,
        onClick = onClick,
        modifier = modifier,
        textColor = textColor,
        containerColor = containerColor,
        enabled = enabled,
        variant = variant,
        minHeight = 40.dp,
        horizontalPadding = 12.dp,
        verticalPadding = 8.dp,
        textMaxLines = 1,
        textOverflow = TextOverflow.Ellipsis,
        textSoftWrap = false
    )
}
