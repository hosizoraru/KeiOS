package os.kei.ui.page.main.widget.chrome

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.LayerBackdrop
import os.kei.ui.page.main.widget.glass.GlassSearchField
import os.kei.ui.page.main.widget.glass.GlassVariant
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun AppTopBarSection(
    title: String,
    modifier: Modifier = Modifier,
    largeTitle: String = title,
    color: Color = Color.Transparent,
    scrollBehavior: ScrollBehavior? = null,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    searchBarVisible: Boolean = false,
    searchBarAnimationLabelPrefix: String = "appTopBarSearch",
    searchBarContent: (@Composable BoxScope.() -> Unit)? = null
) {
    val collapsedFraction = scrollBehavior?.state?.collapsedFraction ?: 0f
    val titleAlpha by animateFloatAsState(
        targetValue = 1f - collapsedFraction.coerceIn(0f, 1f),
        label = "appTopBarTitleAlpha"
    )
    Column(modifier = modifier) {
        TopAppBar(
            title = title,
            largeTitle = largeTitle,
            scrollBehavior = scrollBehavior,
            color = color,
            titleColor = MiuixTheme.colorScheme.onSurface.copy(alpha = titleAlpha),
            navigationIcon = navigationIcon ?: {},
            actions = actions
        )
        if (searchBarContent != null) {
            SearchBarHost(
                visible = searchBarVisible,
                animationLabelPrefix = searchBarAnimationLabelPrefix,
                content = searchBarContent
            )
        }
    }
}

@Composable
fun AppTopBarSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    backdrop: LayerBackdrop? = null,
    singleLine: Boolean = true
) {
    Column {
        GlassSearchField(
            modifier = modifier,
            value = value,
            onValueChange = onValueChange,
            label = label,
            backdrop = backdrop,
            variant = GlassVariant.Bar,
            singleLine = singleLine
        )
        Spacer(modifier = Modifier.height(AppChromeTokens.searchFieldBottomSpacing))
    }
}
