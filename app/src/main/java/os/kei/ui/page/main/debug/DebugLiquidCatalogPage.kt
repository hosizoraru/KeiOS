package os.kei.ui.page.main.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import os.kei.R
import os.kei.ui.page.main.os.appLucideBackIcon
import os.kei.ui.page.main.widget.chrome.AppLiquidNavigationButton
import os.kei.ui.page.main.widget.chrome.AppPageLazyColumn
import os.kei.ui.page.main.widget.chrome.AppPageScaffold
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun DebugLiquidCatalogPage(
    onClose: () -> Unit
) {
    val listState = rememberLazyListState()
    val scrollBehavior = MiuixScrollBehavior()
    val accent = MiuixTheme.colorScheme.primary
    val pageBackdrop = rememberLayerBackdrop()

    AppPageScaffold(
        title = stringResource(R.string.debug_component_lab_liquid_catalog_title),
        modifier = Modifier.fillMaxSize(),
        scrollBehavior = scrollBehavior,
        topBarColor = Color.Transparent,
        navigationIcon = {
            AppLiquidNavigationButton(
                icon = appLucideBackIcon(),
                contentDescription = stringResource(R.string.common_close),
                onClick = onClose,
                backdrop = pageBackdrop
            )
        }
    ) { innerPadding ->
        Box(Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MiuixTheme.colorScheme.background,
                                accent.copy(alpha = if (isSystemInDarkTheme()) 0.12f else 0.08f),
                                MiuixTheme.colorScheme.background
                            )
                        )
                    )
                    .layerBackdrop(pageBackdrop)
            )
            AppPageLazyColumn(
                innerPadding = innerPadding,
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
                bottomExtra = 40.dp,
                sectionSpacing = 14.dp
            ) {
                item {
                    DebugLiquidCatalogIntroCard(accent = accent)
                }
                item {
                    DebugLiquidButtonsCard(
                        accent = accent,
                        backdrop = pageBackdrop
                    )
                }
                item {
                    DebugLiquidGlassDropdownCard(
                        accent = accent,
                        backdrop = pageBackdrop
                    )
                }
                item {
                    DebugLiquidBackdropCard(accent = accent)
                }
                item {
                    DebugLiquidTransparentButtonsCard(
                        accent = accent,
                        backdrop = pageBackdrop
                    )
                }
                item {
                    DebugLiquidSurfaceCardsCard(
                        accent = accent,
                        backdrop = pageBackdrop
                    )
                }
                item {
                    DebugLiquidParameterCard(
                        accent = accent,
                        backdrop = pageBackdrop
                    )
                }
                item {
                    DebugLiquidControlsCard(
                        accent = accent,
                        backdrop = pageBackdrop
                    )
                }
            }
        }
    }
}
