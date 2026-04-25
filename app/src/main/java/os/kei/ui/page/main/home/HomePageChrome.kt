package os.kei.ui.page.main.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.BasicText
import kotlin.math.max
import os.kei.R
import os.kei.feature.home.model.HomeOverviewCard
import os.kei.ui.page.main.model.BottomPage
import os.kei.ui.page.main.os.appLucideCloseIcon
import os.kei.ui.page.main.widget.glass.GlassIconButton
import os.kei.ui.page.main.widget.glass.GlassVariant
import os.kei.ui.page.main.widget.sheet.SheetContentColumn
import os.kei.ui.page.main.widget.sheet.SheetControlRow
import os.kei.ui.page.main.widget.sheet.SheetDescriptionText
import os.kei.ui.page.main.widget.sheet.SheetSectionCard
import os.kei.ui.page.main.widget.sheet.SheetSectionTitle
import os.kei.ui.page.main.widget.sheet.SnapshotWindowBottomSheet
import os.kei.ui.page.main.widget.status.StatusLabelText
import os.kei.ui.page.main.widget.status.StatusPill
import com.kyant.backdrop.Backdrop
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.ui.graphics.Shadow as ComposeTextShadow

private val HOME_KEI_TITLE_GRADIENT_COLORS = listOf(
    Color(0xFFFFD2DE),
    Color(0xFFFFCAD9),
    Color(0xFFFF99BB),
    Color(0xFFFF76A5),
    Color(0xFFFF6098),
    Color(0xFFFF5893)
)
private val HOME_HERO_ICON_AVOIDANCE_LIFT = 88.dp
private val HOME_HERO_TITLE_AVOIDANCE_LIFT = 70.dp
private val HOME_HERO_SUMMARY_AVOIDANCE_LIFT = 56.dp
private const val HOME_HERO_AVOIDANCE_ALPHA_WEIGHT = 0.28f

internal data class HomeHeaderStatusPillState(
    val label: String,
    val color: Color,
    val minWidth: Dp,
    val contentPadding: PaddingValues? = null
)

@Composable
internal fun HomePageControlSheet(
    show: Boolean,
    actionBarBackdrop: Backdrop,
    visibleBottomPages: Set<BottomPage>,
    visibleOverviewCards: Set<HomeOverviewCard>,
    homeSheetTitle: String,
    visiblePagesTitle: String,
    visiblePagesDesc: String,
    visibleCardsTitle: String,
    visibleCardsDesc: String,
    homeCardMcp: String,
    homeCardGitHub: String,
    homeCardBa: String,
    onDismissRequest: () -> Unit,
    onBottomPageVisibilityChange: (BottomPage, Boolean) -> Unit,
    onOverviewCardVisibilityChange: (HomeOverviewCard, Boolean) -> Unit
) {
    SnapshotWindowBottomSheet(
        show = show,
        title = homeSheetTitle,
        onDismissRequest = onDismissRequest,
        startAction = {
            GlassIconButton(
                backdrop = actionBarBackdrop,
                variant = GlassVariant.Bar,
                icon = appLucideCloseIcon(),
                contentDescription = androidx.compose.ui.res.stringResource(R.string.common_close),
                onClick = onDismissRequest
            )
        }
    ) {
        SheetContentColumn(
            scrollable = false,
            verticalSpacing = 10.dp
        ) {
            SheetSectionTitle(visiblePagesTitle)
            SheetSectionCard(verticalSpacing = 10.dp) {
                SheetControlRow(
                    labelContent = {
                        HomeBottomPageLabel(
                            page = BottomPage.Home,
                            modifier = Modifier.defaultMinSize(minHeight = 24.dp)
                        )
                    }
                ) {
                    StatusPill(
                        label = StatusLabelText.FixedVisible,
                        color = Color(0xFF2563EB)
                    )
                }

                BottomPage.entries
                    .filter { it != BottomPage.Home }
                    .forEach { page ->
                        SheetControlRow(
                            labelContent = {
                                HomeBottomPageLabel(
                                    page = page,
                                    modifier = Modifier.defaultMinSize(minHeight = 24.dp)
                                )
                            }
                        ) {
                            Switch(
                                checked = visibleBottomPages.contains(page),
                                onCheckedChange = { checked ->
                                    onBottomPageVisibilityChange(page, checked)
                                }
                            )
                        }
                    }
            }
            SheetSectionTitle(visibleCardsTitle)
            SheetSectionCard(verticalSpacing = 10.dp) {
                SheetControlRow(label = homeCardMcp) {
                    Switch(
                        checked = visibleOverviewCards.contains(HomeOverviewCard.MCP),
                        onCheckedChange = { checked ->
                            onOverviewCardVisibilityChange(HomeOverviewCard.MCP, checked)
                        }
                    )
                }
                SheetControlRow(label = homeCardGitHub) {
                    Switch(
                        checked = visibleOverviewCards.contains(HomeOverviewCard.GITHUB),
                        onCheckedChange = { checked ->
                            onOverviewCardVisibilityChange(HomeOverviewCard.GITHUB, checked)
                        }
                    )
                }
                SheetControlRow(label = homeCardBa) {
                    Switch(
                        checked = visibleOverviewCards.contains(HomeOverviewCard.BA),
                        onCheckedChange = { checked ->
                            onOverviewCardVisibilityChange(HomeOverviewCard.BA, checked)
                        }
                    )
                }
            }
            SheetDescriptionText(text = visiblePagesDesc)
            SheetDescriptionText(text = visibleCardsDesc)
        }
    }
}

@Composable
internal fun HomePageHero(
    homeIconHdrEnabled: Boolean,
    hdrSweepProgress: Float,
    homeHeaderSinkOffset: Dp,
    logoPadding: PaddingValues,
    layoutDirection: LayoutDirection,
    homeAppName: String,
    homeTagline: String,
    appVersionText: String,
    avoidanceProgress: Float,
    iconProgress: Float,
    titleProgress: Float,
    summaryProgress: Float,
    statusPills: List<HomeHeaderStatusPillState>,
    onHeroHeightChanged: (Int) -> Unit,
    onIconBottomChanged: (Float) -> Unit,
    onTitleBottomChanged: (Float) -> Unit,
    onSummaryBottomChanged: (Float) -> Unit
) {
    val density = LocalDensity.current
    val iconAvoidanceLiftPx = with(density) { HOME_HERO_ICON_AVOIDANCE_LIFT.toPx() }
    val titleAvoidanceLiftPx = with(density) { HOME_HERO_TITLE_AVOIDANCE_LIFT.toPx() }
    val summaryAvoidanceLiftPx = with(density) { HOME_HERO_SUMMARY_AVOIDANCE_LIFT.toPx() }
    val iconExitProgress = max(iconProgress, avoidanceProgress * HOME_HERO_AVOIDANCE_ALPHA_WEIGHT)
    val titleExitProgress = max(titleProgress, avoidanceProgress * HOME_HERO_AVOIDANCE_ALPHA_WEIGHT)
    val summaryExitProgress = max(summaryProgress, avoidanceProgress * HOME_HERO_AVOIDANCE_ALPHA_WEIGHT)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = logoPadding.calculateTopPadding() + 36.dp + homeHeaderSinkOffset,
                start = logoPadding.calculateStartPadding(layoutDirection),
                end = logoPadding.calculateEndPadding(layoutDirection)
            )
            .onSizeChanged { size -> onHeroHeightChanged(size.height) },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(88.dp)
                .graphicsLayer {
                    alpha = 1f - iconExitProgress
                    translationY = -iconAvoidanceLiftPx * max(iconProgress, avoidanceProgress)
                    scaleX = 1f - (iconExitProgress * 0.05f)
                    scaleY = 1f - (iconExitProgress * 0.05f)
                }
                .onGloballyPositioned { coordinates ->
                    onIconBottomChanged(coordinates.positionInWindow().y + coordinates.size.height)
                }
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_kei_logo_color),
                contentDescription = null,
                modifier = Modifier
                    .size(88.dp)
                    .graphicsLayer {
                        alpha = (1f - iconExitProgress) * 0.95f
                    }
                    .homeKeiHdrAccent(
                        enabled = homeIconHdrEnabled,
                        sweepProgress = hdrSweepProgress,
                        radialAlpha = 0.30f,
                        radialRadiusScale = 0.72f,
                        radialCenterX = 0.5f,
                        radialCenterY = 0.48f
                    )
            )
        }

        BasicText(
            text = homeAppName,
            style = TextStyle(
                brush = Brush.linearGradient(
                    colors = HOME_KEI_TITLE_GRADIENT_COLORS,
                    start = Offset(14f, 6f),
                    end = Offset(260f, 104f)
                ),
                fontWeight = FontWeight.Bold,
                fontSize = 30.sp,
                shadow = ComposeTextShadow(
                    color = Color(0x55FF74A6),
                    offset = Offset(0f, 3f),
                    blurRadius = 16f
                )
            ),
            modifier = Modifier
                .padding(top = 10.dp, bottom = 4.dp)
                .onGloballyPositioned { coordinates ->
                    onTitleBottomChanged(coordinates.positionInWindow().y + coordinates.size.height)
                }
                .graphicsLayer {
                    alpha = 1f - titleExitProgress
                    translationY = -titleAvoidanceLiftPx * max(titleProgress, avoidanceProgress)
                    scaleX = 1f - (titleExitProgress * 0.05f)
                    scaleY = 1f - (titleExitProgress * 0.05f)
                }
                .homeKeiHdrAccent(
                    enabled = homeIconHdrEnabled,
                    sweepProgress = hdrSweepProgress,
                    radialAlpha = 0.26f,
                    radialRadiusScale = 0.82f,
                    radialCenterX = 0.32f,
                    radialCenterY = 0.34f
                )
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    alpha = 1f - summaryExitProgress
                    translationY = -summaryAvoidanceLiftPx * max(summaryProgress, avoidanceProgress)
                    scaleX = 1f - (summaryExitProgress * 0.05f)
                    scaleY = 1f - (summaryExitProgress * 0.05f)
                }
                .onGloballyPositioned { coordinates ->
                    onSummaryBottomChanged(coordinates.positionInWindow().y + coordinates.size.height)
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = homeTagline,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
            )
            Text(
                text = appVersionText,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 2.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
            ) {
                statusPills.forEach { pill ->
                    val modifier = Modifier.defaultMinSize(minWidth = pill.minWidth)
                    if (pill.contentPadding == null) {
                        StatusPill(
                            label = pill.label,
                            color = pill.color,
                            modifier = modifier
                        )
                    } else {
                        StatusPill(
                            label = pill.label,
                            color = pill.color,
                            modifier = modifier,
                            contentPadding = pill.contentPadding
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun HomePageHeroSpacer(
    logoHeightDp: Dp,
    logoPadding: PaddingValues,
    listContentPadding: PaddingValues,
    homeHeaderSinkOffset: Dp,
    onLogoHeightPxChanged: (Int) -> Unit,
    onLogoAreaBottomChanged: (Float) -> Unit
) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(
                logoHeightDp + 36.dp +
                    logoPadding.calculateTopPadding() -
                    listContentPadding.calculateTopPadding() + 90.dp +
                    homeHeaderSinkOffset
            )
            .onSizeChanged { size -> onLogoHeightPxChanged(size.height) }
            .onGloballyPositioned { coordinates ->
                onLogoAreaBottomChanged(coordinates.positionInWindow().y + coordinates.size.height)
            }
    )
}

@Composable
internal fun HomePageOverviewCards(
    visibleOverviewCards: Set<HomeOverviewCard>,
    homeCardBackdrop: Backdrop,
    blurEnabled: Boolean,
    homeNa: String,
    homeCardMcp: String,
    mcpStats: List<HomeCardStatItem>,
    homeCardGitHub: String,
    githubStats: List<HomeCardStatItem>,
    homeCardBa: String,
    baStats: List<HomeCardStatItem>
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Top
    ) {
        if (visibleOverviewCards.contains(HomeOverviewCard.MCP)) {
            HomeInfoCard(
                backdrop = homeCardBackdrop,
                blurEnabled = blurEnabled
            ) {
                HomeInfoGridCard(
                    title = homeCardMcp,
                    naText = homeNa,
                    columns = 3,
                    stats = mcpStats
                )
            }
        }

        if (visibleOverviewCards.contains(HomeOverviewCard.GITHUB)) {
            HomeInfoCard(
                backdrop = homeCardBackdrop,
                blurEnabled = blurEnabled
            ) {
                HomeInfoGridCard(
                    title = homeCardGitHub,
                    naText = homeNa,
                    columns = 3,
                    stats = githubStats
                )
            }
        }

        if (visibleOverviewCards.contains(HomeOverviewCard.BA)) {
            HomeInfoCard(
                backdrop = homeCardBackdrop,
                blurEnabled = blurEnabled
            ) {
                HomeInfoGridCard(
                    title = homeCardBa,
                    naText = homeNa,
                    stats = baStats
                )
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
    }
}
