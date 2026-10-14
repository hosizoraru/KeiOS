@file:Suppress("FunctionName")

package os.kei.ui.page.main.widget.glass

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import os.kei.ui.page.main.widget.isAppInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.kyant.backdrop.Backdrop
import os.kei.ui.page.main.widget.motion.AppMotionTokens
import os.kei.ui.page.main.widget.motion.LocalTransitionAnimationsEnabled
import os.kei.ui.page.main.widget.motion.appExpandIn
import os.kei.ui.page.main.widget.motion.appExpandOut
import os.kei.ui.page.main.widget.motion.appMotionFloatState
import os.kei.ui.page.main.widget.shape.appSquircleBackground
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

private val ActionMenuQuickActionSpacing = 4.dp
private val ActionMenuQuickActionRowHorizontalPadding = 4.dp
private val ActionMenuQuickActionRowVerticalPadding = 3.dp
private val ActionMenuQuickActionMinHeight = 52.dp
private val ActionMenuDividerHorizontalPadding = 23.dp
private val ActionMenuDividerVerticalPadding = 4.dp

data class LiquidGlassActionMenuQuickAction(
    val id: String,
    val icon: ImageVector,
    val label: String,
    val contentDescription: String = label,
    val enabled: Boolean = true,
    val variant: GlassVariant = GlassVariant.SheetAction,
    val testTag: String? = null,
    val onClick: () -> Unit,
)

sealed interface LiquidGlassActionMenuItem {
    val id: String
}

sealed interface LiquidGlassActionMenuSubmenuItem : LiquidGlassActionMenuItem

data class LiquidGlassActionMenuInfoRow(
    override val id: String,
    val text: String,
    val modifier: Modifier = Modifier,
    val leadingIcon: ImageVector? = null,
    val trailingIcon: ImageVector? = null,
    val subtitle: String? = null,
) : LiquidGlassActionMenuItem

data class LiquidGlassActionMenuActionRow(
    override val id: String,
    val text: String,
    val onClick: () -> Unit,
    val leadingIcon: ImageVector? = null,
    val trailingIcon: ImageVector? = null,
    val subtitle: String? = null,
    val enabled: Boolean = true,
    val highlighted: Boolean = false,
    val dismissOnClick: Boolean = true,
    val variant: GlassVariant = GlassVariant.SheetAction,
    /** A handle for a baseline profile journey, the way [LiquidGlassActionMenuQuickAction] has one. */
    val testTag: String? = null,
) : LiquidGlassActionMenuItem

data class LiquidGlassActionMenuSubmenuRow(
    override val id: String,
    val text: String,
    val submenuItems: List<LiquidGlassActionMenuSubmenuItem>,
    val leadingIcon: ImageVector? = null,
    val trailingIcon: ImageVector? = null,
    val subtitle: String? = null,
    val enabled: Boolean = true,
    val highlighted: Boolean = false,
    val backLeadingIcon: ImageVector? = null,
    val initialScrollItemIndex: Int? = null,
    val variant: GlassVariant = GlassVariant.SheetAction,
) : LiquidGlassActionMenuItem

data class LiquidGlassActionMenuSingleChoiceRow(
    override val id: String,
    val text: String,
    val selected: Boolean,
    val onClick: () -> Unit,
    val leadingIcon: ImageVector? = null,
    val trailingIcon: ImageVector? = null,
    val subtitle: String? = null,
    val enabled: Boolean = true,
    val variant: GlassVariant = GlassVariant.SheetAction,
) : LiquidGlassActionMenuSubmenuItem

data class LiquidGlassActionMenuMultipleChoiceRow(
    override val id: String,
    val text: String,
    val checked: Boolean,
    val onCheckedChange: (Boolean) -> Unit,
    val leadingIcon: ImageVector? = null,
    val trailingIcon: ImageVector? = null,
    val subtitle: String? = null,
    val enabled: Boolean = true,
    val variant: GlassVariant = GlassVariant.SheetAction,
) : LiquidGlassActionMenuSubmenuItem

@Composable
fun LiquidGlassActionMenu(
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = null,
    quickActions: List<LiquidGlassActionMenuQuickAction> = emptyList(),
    items: List<LiquidGlassActionMenuItem>,
    accentColor: Color = MiuixTheme.colorScheme.primary,
    minWidth: Dp = 252.dp,
    maxWidth: Dp = 312.dp,
    maxHeight: Dp = 420.dp,
    initialExpandedSubmenuId: String? = null,
    layout: LiquidMenuLayout? = null,
    onDismissRequest: () -> Unit = {},
) {
    if (!LocalLiquidPresentationOwnerActive.current) return
    // Apple defines the top row's size and whether it carries labels; the plan decides which layout is
    // actually achievable for the quick actions given, and lists anything that does not fit rather than
    // dropping it. See resolveLiquidMenuLayoutPlan.
    val layoutPlan = remember(layout, quickActions) { resolveLiquidMenuLayoutPlan(layout, quickActions) }
    val listedQuickActionRows =
        remember(layoutPlan) { layoutPlan.listed.map { action -> action.asMenuRow() } }
    val resolvedItems =
        remember(listedQuickActionRows, items) {
            if (listedQuickActionRows.isEmpty()) items else listedQuickActionRows + items
        }
    val availableSubmenuIds =
        resolvedItems
            .asSequence()
            .filterIsInstance<LiquidGlassActionMenuSubmenuRow>()
            .filter { item -> item.enabled && item.submenuItems.isNotEmpty() }
            .mapTo(linkedSetOf()) { item -> item.id }
    var expandedSubmenuId by remember(initialExpandedSubmenuId) {
        mutableStateOf(initialExpandedSubmenuId)
    }
    var renderedSubmenuId by remember(initialExpandedSubmenuId) {
        mutableStateOf(initialExpandedSubmenuId)
    }
    val submenuVisibilityState =
        remember(initialExpandedSubmenuId) {
            MutableTransitionState(initialExpandedSubmenuId != null).apply {
                targetState = initialExpandedSubmenuId != null
            }
        }
    val transitionAnimationsEnabled = LocalTransitionAnimationsEnabled.current
    val expandedSubmenu =
        resolvedItems
            .filterIsInstance<LiquidGlassActionMenuSubmenuRow>()
            .firstOrNull { item ->
                item.id == expandedSubmenuId &&
                    item.enabled &&
                    item.submenuItems.isNotEmpty()
            }
    val renderedSubmenu =
        resolvedItems
            .filterIsInstance<LiquidGlassActionMenuSubmenuRow>()
            .firstOrNull { item ->
                item.id == renderedSubmenuId &&
                    item.enabled &&
                    item.submenuItems.isNotEmpty()
            }
    LaunchedEffect(availableSubmenuIds, expandedSubmenuId, renderedSubmenuId) {
        if (expandedSubmenuId != null && expandedSubmenuId !in availableSubmenuIds) {
            expandedSubmenuId = null
            submenuVisibilityState.targetState = false
        }
        if (renderedSubmenuId != null && renderedSubmenuId !in availableSubmenuIds) {
            renderedSubmenuId = null
        }
    }
    LaunchedEffect(expandedSubmenu?.id) {
        if (expandedSubmenu != null) {
            renderedSubmenuId = expandedSubmenu.id
            submenuVisibilityState.targetState = true
        } else {
            submenuVisibilityState.targetState = false
        }
    }
    LaunchedEffect(
        submenuVisibilityState.isIdle,
        submenuVisibilityState.currentState,
        expandedSubmenuId,
    ) {
        if (submenuVisibilityState.isIdle &&
            !submenuVisibilityState.currentState &&
            expandedSubmenuId == null
        ) {
            renderedSubmenuId = null
        }
    }
    val collapseSubmenu = {
        expandedSubmenuId = null
        submenuVisibilityState.targetState = false
    }
    val expandSubmenu: (String) -> Unit = { id ->
        if (expandedSubmenuId == id) {
            collapseSubmenu()
        } else {
            renderedSubmenuId = id
            submenuVisibilityState.targetState = true
            expandedSubmenuId = id
        }
    }
    LiquidGlassActionMenuSubmenuBackHandler(
        enabled = expandedSubmenu != null,
        onBack = collapseSubmenu,
    )
    // Cache the two spring specs once. Without `remember`, every recomposition allocates
    // a fresh `spring(...)` instance and Compose's animateContentSize cannot reuse its
    // internal interpolator state — defeating spec caching.
    val collapseSpec =
        remember {
            spring<androidx.compose.ui.unit.IntSize>(dampingRatio = 0.92f, stiffness = 380f)
        }
    val expandSpec =
        remember {
            spring<androidx.compose.ui.unit.IntSize>(dampingRatio = 0.85f, stiffness = 460f)
        }
    val disabledSpec =
        remember {
            tween<androidx.compose.ui.unit.IntSize>(durationMillis = AppMotionTokens.disabledDurationMs)
        }
    val resolvedSpec =
        when {
            !transitionAnimationsEnabled -> disabledSpec
            expandedSubmenu == null -> collapseSpec
            else -> expandSpec
        }
    AppLiquidGlassDropdownColumn(
        modifier =
            modifier
                .animateContentSize(animationSpec = resolvedSpec)
                .semantics { testTagsAsResourceId = true },
        minWidth = minWidth,
        maxWidth = maxWidth,
        maxHeight = maxHeight,
        accentColor = accentColor,
        backdrop = backdrop,
        material = LiquidGlassDropdownMaterial.ActionMenu,
        initialScrollItemIndex = renderedSubmenu?.initialScrollItemIndex,
    ) {
        if (layoutPlan.topRow.isNotEmpty()) {
            LiquidGlassActionMenuQuickActionsRow(
                quickActions = layoutPlan.topRow,
                showLabels = layoutPlan.topRowShowsLabels,
                accentColor = accentColor,
                onActionClick = { action ->
                    action.onClick()
                    onDismissRequest()
                },
            )
            LiquidGlassActionMenuDivider()
        }
        if (renderedSubmenu == null) {
            LiquidGlassActionMenuItemsPanel(
                items = resolvedItems,
                expandedSubmenuId = expandedSubmenuId,
                accentColor = accentColor,
                onExpandSubmenu = expandSubmenu,
                onDismissRequest = onDismissRequest,
            )
        } else {
            LiquidGlassActionMenuSubmenuPanel(
                item = renderedSubmenu,
                choicesVisibleState = submenuVisibilityState,
                accentColor = accentColor,
                onCollapse = collapseSubmenu,
                onDismissRequest = {
                    collapseSubmenu()
                    onDismissRequest()
                },
            )
        }
    }
}

@Composable
private fun LiquidGlassActionMenuSubmenuBackHandler(
    enabled: Boolean,
    onBack: () -> Unit,
) {
    if (LocalNavigationEventDispatcherOwner.current != null) {
        val navigationEventState =
            rememberNavigationEventState(currentInfo = NavigationEventInfo.None)
        NavigationBackHandler(
            state = navigationEventState,
            isBackEnabled = enabled,
            onBackCompleted = onBack,
        )
    } else {
        BackHandler(enabled = enabled, onBack = onBack)
    }
}

@Composable
private fun LiquidGlassActionMenuItemsPanel(
    items: List<LiquidGlassActionMenuItem>,
    expandedSubmenuId: String?,
    accentColor: Color,
    onExpandSubmenu: (String) -> Unit,
    onDismissRequest: () -> Unit,
) {
    items.forEachIndexed { index, item ->
        LiquidGlassActionMenuItemRow(
            item = item,
            index = index,
            optionSize = items.size,
            expanded = expandedSubmenuId == item.id,
            accentColor = accentColor,
            onExpandSubmenu = onExpandSubmenu,
            onDismissRequest = onDismissRequest,
        )
    }
}

@Composable
private fun LiquidGlassActionMenuItemRow(
    item: LiquidGlassActionMenuItem,
    index: Int,
    optionSize: Int,
    expanded: Boolean,
    accentColor: Color,
    onExpandSubmenu: (String) -> Unit,
    onDismissRequest: () -> Unit,
) {
    when (item) {
        is LiquidGlassActionMenuInfoRow -> {
            LiquidGlassDropdownInfoItem(
                text = item.text,
                modifier = item.modifier,
                index = index,
                optionSize = optionSize,
                leadingIcon = item.leadingIcon,
                trailingIcon = item.trailingIcon,
                subtitle = item.subtitle,
                accentColor = accentColor,
            )
        }

        is LiquidGlassActionMenuActionRow -> {
            LiquidGlassDropdownActionItem(
                text = item.text,
                onClick = {
                    item.onClick()
                    if (item.dismissOnClick) {
                        onDismissRequest()
                    }
                },
                index = index,
                optionSize = optionSize,
                leadingIcon = item.leadingIcon,
                trailingIcon = item.trailingIcon,
                subtitle = item.subtitle,
                accentColor = accentColor,
                variant = item.variant,
                enabled = item.enabled,
                highlighted = item.highlighted,
                modifier = item.testTag?.let { tag -> Modifier.testTag(tag) } ?: Modifier,
            )
        }

        is LiquidGlassActionMenuSingleChoiceRow -> {
            LiquidGlassDropdownSingleChoiceItem(
                text = item.text,
                optionSize = optionSize,
                isSelected = item.selected,
                index = index,
                onSelectedIndexChange = {
                    item.onClick()
                    onDismissRequest()
                },
                leadingIcon = item.leadingIcon,
                trailingIcon = item.trailingIcon,
                subtitle = item.subtitle,
                accentColor = accentColor,
                variant = item.variant,
                enabled = item.enabled,
            )
        }

        is LiquidGlassActionMenuMultipleChoiceRow -> {
            LiquidGlassDropdownMultipleChoiceItem(
                text = item.text,
                checked = item.checked,
                onCheckedChange = { checked ->
                    item.onCheckedChange(checked)
                    onDismissRequest()
                },
                index = index,
                optionSize = optionSize,
                leadingIcon = item.leadingIcon,
                trailingIcon = item.trailingIcon,
                subtitle = item.subtitle,
                accentColor = accentColor,
                variant = item.variant,
                enabled = item.enabled,
            )
        }

        is LiquidGlassActionMenuSubmenuRow -> {
            LiquidGlassDropdownActionItem(
                text = item.text,
                onClick = { onExpandSubmenu(item.id) },
                index = index,
                optionSize = optionSize,
                leadingIcon = item.leadingIcon,
                trailingIcon = item.trailingIcon,
                subtitle =
                    item.subtitle
                        ?: item.submenuItems
                            .filterIsInstance<LiquidGlassActionMenuSingleChoiceRow>()
                            .firstOrNull { choice -> choice.selected }
                            ?.text,
                accentColor = accentColor,
                variant = item.variant,
                enabled = item.enabled && item.submenuItems.isNotEmpty(),
                highlighted = item.highlighted || expanded,
            )
        }
    }
}

@Composable
private fun LiquidGlassActionMenuSubmenuPanel(
    item: LiquidGlassActionMenuSubmenuRow,
    choicesVisibleState: MutableTransitionState<Boolean>,
    accentColor: Color,
    onCollapse: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    LiquidGlassDropdownActionItem(
        text = item.text,
        onClick = onCollapse,
        index = 0,
        optionSize = 1,
        leadingIcon = item.backLeadingIcon ?: item.leadingIcon,
        subtitle =
            item.subtitle
                ?: item.submenuItems
                    .filterIsInstance<LiquidGlassActionMenuSingleChoiceRow>()
                    .firstOrNull { choice -> choice.selected }
                    ?.text,
        trailingContent = {
            item.trailingIcon?.takeIf { item.backLeadingIcon == null }?.let { icon ->
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint =
                        MiuixTheme.colorScheme.onBackgroundVariant.copy(
                            alpha = if (isAppInDarkTheme()) 0.82f else 0.70f,
                        ),
                    modifier =
                        Modifier
                            .size(18.dp)
                            .graphicsLayer { rotationZ = 90f },
                )
            }
        },
        accentColor = accentColor,
        variant = item.variant,
        enabled = item.enabled,
    )
    AnimatedVisibility(
        visibleState = choicesVisibleState,
        // Submenu reveal: spring-based fade + vertical expand. Feels organic and nests well inside
        // the parent menu's container-size spring without competing animation curves.
        enter =
            fadeIn(
                animationSpec = spring(dampingRatio = 0.92f, stiffness = 500f),
            ) +
                expandVertically(
                    animationSpec = spring(dampingRatio = 0.85f, stiffness = 460f),
                    expandFrom = Alignment.Top,
                ),
        exit =
            fadeOut(
                animationSpec = spring(dampingRatio = 0.95f, stiffness = 700f),
            ) +
                shrinkVertically(
                    animationSpec = spring(dampingRatio = 0.92f, stiffness = 600f),
                    shrinkTowards = Alignment.Top,
                ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            LiquidGlassActionMenuDivider()
            val optionSize = item.submenuItems.size
            item.submenuItems.forEachIndexed { choiceIndex, choice ->
                when (choice) {
                    is LiquidGlassActionMenuSingleChoiceRow -> {
                        LiquidGlassDropdownSingleChoiceItem(
                            text = choice.text,
                            optionSize = optionSize,
                            isSelected = choice.selected,
                            index = choiceIndex,
                            onSelectedIndexChange = {
                                choice.onClick()
                                onDismissRequest()
                            },
                            leadingIcon = choice.leadingIcon,
                            trailingIcon = choice.trailingIcon,
                            subtitle = choice.subtitle,
                            accentColor = accentColor,
                            variant = choice.variant,
                            enabled = choice.enabled,
                        )
                    }

                    is LiquidGlassActionMenuMultipleChoiceRow -> {
                        LiquidGlassDropdownMultipleChoiceItem(
                            text = choice.text,
                            checked = choice.checked,
                            onCheckedChange = choice.onCheckedChange,
                            index = choiceIndex,
                            optionSize = optionSize,
                            leadingIcon = choice.leadingIcon,
                            trailingIcon = choice.trailingIcon,
                            subtitle = choice.subtitle,
                            accentColor = accentColor,
                            variant = choice.variant,
                            enabled = choice.enabled,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LiquidGlassActionMenuQuickActionsRow(
    quickActions: List<LiquidGlassActionMenuQuickAction>,
    showLabels: Boolean,
    accentColor: Color,
    onActionClick: (LiquidGlassActionMenuQuickAction) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = ActionMenuQuickActionRowHorizontalPadding,
                    vertical = ActionMenuQuickActionRowVerticalPadding,
                ),
        horizontalArrangement = Arrangement.spacedBy(ActionMenuQuickActionSpacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        quickActions.forEach { action ->
            LiquidGlassActionMenuQuickActionButton(
                action = action,
                showLabel = showLabels,
                accentColor = accentColor,
                onActionClick = onActionClick,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun LiquidGlassActionMenuQuickActionButton(
    action: LiquidGlassActionMenuQuickAction,
    showLabel: Boolean,
    accentColor: Color,
    onActionClick: (LiquidGlassActionMenuQuickAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDark = isAppInDarkTheme()
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scaleState =
        appMotionFloatState(
            targetValue = if (pressed && action.enabled) AppInteractiveTokens.pressedScale else 1f,
            durationMillis = 110,
            label = "liquid_glass_action_menu_quick_action_scale",
        )
    val scaleProvider = remember(scaleState) { { scaleState.value } }
    val contentColor =
        actionMenuContentColor(
            isDark = isDark,
            accentColor = accentColor,
            variant = action.variant,
            enabled = action.enabled,
            primary = true,
        )
    val surfaceColor =
        when {
            pressed -> MiuixTheme.colorScheme.onBackground.copy(alpha = if (isDark) 0.14f else 0.08f)
            action.variant == GlassVariant.SheetPrimaryAction -> accentColor.copy(alpha = if (isDark) 0.18f else 0.12f)
            else -> Color.Transparent
        }
    val accessibilityLabel = action.contentDescription.ifBlank { action.label }
    Column(
        modifier =
            modifier
                .testTag(action.testTag ?: "liquid_action_menu_quick_${action.id}")
                .defaultMinSize(minHeight = ActionMenuQuickActionMinHeight)
                .graphicsLayer {
                    val scale = scaleProvider()
                    scaleX = scale
                    scaleY = scale
                }.appSquircleBackground(surfaceColor, 18.dp)
                .semantics(mergeDescendants = true) {
                    contentDescription = accessibilityLabel
                }.clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = action.enabled,
                    role = Role.Button,
                    onClick = { onActionClick(action) },
                ).padding(horizontal = 2.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically),
    ) {
        Icon(
            imageVector = action.icon,
            contentDescription = null,
            tint = contentColor,
            // A symbol-only row has to carry the whole meaning, so it gets a larger glyph.
            modifier = Modifier.size(if (showLabel) 21.dp else 24.dp),
        )
        if (showLabel) {
            Text(
                text = action.label,
                color = contentColor,
                fontSize = 12.sp,
                lineHeight = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                // The row above already carries the label as its contentDescription; repeating it here
                // would make a screen reader announce every action twice.
                modifier = Modifier.clearAndSetSemantics {},
            )
        }
    }
}

@Composable
private fun LiquidGlassActionMenuDivider() {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = ActionMenuDividerHorizontalPadding,
                    vertical = ActionMenuDividerVerticalPadding,
                ).height(1.dp)
                .background(
                    color =
                        MiuixTheme.colorScheme.onBackground.copy(
                            alpha = if (isAppInDarkTheme()) 0.12f else 0.10f,
                        ),
                ),
    )
}

private fun actionMenuContentColor(
    isDark: Boolean,
    accentColor: Color,
    variant: GlassVariant,
    enabled: Boolean,
    primary: Boolean,
): Color {
    val color =
        when (variant) {
            GlassVariant.SheetDangerAction -> {
                Color(0xFFE25B6A)
            }

            GlassVariant.SheetPrimaryAction -> {
                accentColor
            }

            else -> {
                if (primary) {
                    if (accentColor == Color.Unspecified) {
                        if (isDark) Color(0xFF71ADFF) else Color(0xFF3B82F6)
                    } else {
                        accentColor
                    }
                } else {
                    if (isDark) Color.White.copy(alpha = 0.92f) else Color(0xFF111827).copy(alpha = 0.88f)
                }
            }
        }
    return if (enabled) color else color.copy(alpha = 0.38f)
}
