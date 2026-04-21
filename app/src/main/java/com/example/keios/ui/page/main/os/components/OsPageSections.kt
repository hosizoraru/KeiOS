package com.example.keios.ui.page.main.os.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.keios.R
import com.example.keios.ui.page.main.os.InfoRow
import com.example.keios.ui.page.main.os.shortcut.OsActivityShortcutCard
import com.example.keios.ui.page.main.os.OsSectionCard
import com.example.keios.ui.page.main.os.shortcut.ShortcutActivityIcon
import com.example.keios.ui.page.main.os.appLucideCloseIcon
import com.example.keios.ui.page.main.os.shortcut.normalizeShortcutIntentExtras
import com.example.keios.ui.page.main.os.osLucideEnterIcon
import com.example.keios.ui.page.main.widget.core.AppStatusPillSize
import com.example.keios.ui.page.main.widget.core.AppTypographyTokens
import com.example.keios.ui.page.main.widget.glass.GlassIconButton
import com.example.keios.ui.page.main.widget.glass.GlassTextButton
import com.example.keios.ui.page.main.widget.glass.GlassVariant
import com.example.keios.ui.page.main.widget.glass.MiuixAccordionCard
import com.example.keios.ui.page.main.widget.sheet.SheetContentColumn
import com.example.keios.ui.page.main.widget.sheet.SheetControlRow
import com.example.keios.ui.page.main.widget.sheet.SheetDescriptionText
import com.example.keios.ui.page.main.widget.sheet.SheetSectionCard
import com.example.keios.ui.page.main.widget.sheet.SnapshotWindowBottomSheet
import com.example.keios.ui.page.main.widget.status.StatusPill
import com.kyant.backdrop.backdrops.LayerBackdrop
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

internal fun LazyListScope.addTopInfoCard(
    visible: Boolean,
    contentBackdrop: LayerBackdrop,
    displayedTopInfoRows: List<InfoRow>,
    groupedTopInfoRows: List<Pair<String, List<InfoRow>>>,
    query: String,
    noMatchedResultsText: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    exportAction: @Composable () -> Unit
) {
    if (!visible) return
    item {
        MiuixAccordionCard(
            backdrop = contentBackdrop,
            title = stringResource(R.string.os_section_top_info_title),
            subtitle = stringResource(R.string.common_item_count, displayedTopInfoRows.size),
            expanded = expanded,
            onExpandedChange = onExpandedChange,
            headerStartAction = {
                OsSectionHeaderIcon(card = OsSectionCard.TOP_INFO)
            },
            headerActions = exportAction
        ) {
            if (displayedTopInfoRows.isEmpty()) {
                Text(text = noMatchedResultsText, color = MiuixTheme.colorScheme.onBackgroundVariant)
            } else if (query.isBlank() && !expanded) {
                OsVirtualizedInfoRows(
                    rows = displayedTopInfoRows,
                    valueSingleLine = true,
                    labelMinWidth = 56.dp,
                    labelMaxWidth = 92.dp,
                    labelMaxLines = 1,
                    valueMinWidth = 140.dp
                )
            } else {
                OsVirtualizedGroupedTopInfoRows(groupedRows = groupedTopInfoRows)
            }
        }
    }
    item { Spacer(modifier = Modifier.height(8.dp)) }
}

internal fun LazyListScope.addShortcutActivityCards(
    cards: List<OsActivityShortcutCard>,
    contentBackdrop: LayerBackdrop,
    defaultCardTitle: String,
    expandedStates: Map<String, Boolean>,
    onExpandedChange: (String, Boolean) -> Unit,
    onOpenActivity: (OsActivityShortcutCard) -> Unit,
    onHeaderLongClick: (OsActivityShortcutCard) -> Unit
) {
    cards.filter { it.visible }.forEach { card ->
        val shortcutConfig = card.config
        item(key = "os-activity-${card.id}") {
            MiuixAccordionCard(
                backdrop = contentBackdrop,
                title = shortcutConfig.title.ifBlank { defaultCardTitle },
                subtitle = shortcutConfig.subtitle,
                expanded = expandedStates[card.id] == true,
                onExpandedChange = { expanded -> onExpandedChange(card.id, expanded) },
                headerStartAction = {
                    ShortcutActivityIcon(
                        packageName = shortcutConfig.packageName,
                        className = shortcutConfig.className,
                        size = 24.dp,
                        fallbackToPackageIcon = true
                    )
                },
                headerActions = {
                    Icon(
                        imageVector = osLucideEnterIcon(),
                        contentDescription = stringResource(R.string.os_google_system_service_cd_open_activity),
                        tint = MiuixTheme.colorScheme.primary,
                        modifier = Modifier.clickable { onOpenActivity(card) }
                    )
                },
                onHeaderLongClick = { onHeaderLongClick(card) }
            ) {
                val emptyValueText = stringResource(R.string.os_google_system_service_value_data_empty)
                OsSectionInfoRow(
                    label = stringResource(R.string.os_google_system_service_label_app_name),
                    value = shortcutConfig.appName
                )
                OsSectionInfoRow(
                    label = stringResource(R.string.os_google_system_service_label_package_name),
                    value = shortcutConfig.packageName
                )
                OsSectionInfoRow(
                    label = stringResource(R.string.os_google_system_service_label_class_name),
                    value = shortcutConfig.className
                )
                OsSectionInfoRow(
                    label = stringResource(R.string.os_google_system_service_label_intent_action),
                    value = shortcutConfig.intentAction
                )
                OsSectionInfoRow(
                    label = stringResource(R.string.os_google_system_service_label_intent_category),
                    value = shortcutConfig.intentCategory.ifBlank { emptyValueText }
                )
                OsSectionInfoRow(
                    label = stringResource(R.string.os_google_system_service_label_intent_flags),
                    value = shortcutConfig.intentFlags.ifBlank { emptyValueText }
                )
                OsSectionInfoRow(
                    label = stringResource(R.string.os_google_system_service_label_intent_data),
                    value = shortcutConfig.intentUriData.ifBlank { emptyValueText }
                )
                OsSectionInfoRow(
                    label = stringResource(R.string.os_google_system_service_label_intent_mime_type),
                    value = shortcutConfig.intentMimeType.ifBlank { emptyValueText }
                )
                val normalizedExtras = normalizeShortcutIntentExtras(shortcutConfig.intentExtras)
                if (normalizedExtras.isEmpty()) {
                    OsSectionInfoRow(
                        label = stringResource(R.string.os_google_system_service_label_intent_extras),
                        value = emptyValueText
                    )
                } else {
                    normalizedExtras.forEachIndexed { index, extra ->
                        val typeLabel = stringResource(extra.type.labelResId)
                        OsSectionInfoRow(
                            label = stringResource(
                                R.string.os_google_system_service_label_intent_extra_indexed,
                                index + 1
                            ),
                            value = "[$typeLabel] ${extra.key} = ${extra.value.ifBlank { emptyValueText }}"
                        )
                    }
                }
            }
        }
        item(key = "os-activity-space-${card.id}") { Spacer(modifier = Modifier.height(8.dp)) }
    }
}

internal fun LazyListScope.addKeyValueSectionCard(
    visible: Boolean,
    card: OsSectionCard,
    contentBackdrop: LayerBackdrop,
    title: String,
    subtitle: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    rows: List<InfoRow>,
    noMatchedResultsText: String,
    exportAction: @Composable () -> Unit
) {
    if (!visible) return
    item {
        MiuixAccordionCard(
            backdrop = contentBackdrop,
            title = title,
            subtitle = subtitle,
            expanded = expanded,
            onExpandedChange = onExpandedChange,
            headerStartAction = {
                OsSectionHeaderIcon(card = card)
            },
            headerActions = exportAction
        ) {
            if (rows.isEmpty()) {
                Text(text = noMatchedResultsText, color = MiuixTheme.colorScheme.onBackgroundVariant)
            } else {
                OsVirtualizedInfoRows(rows = rows)
            }
        }
    }
    item { Spacer(modifier = Modifier.height(8.dp)) }
}

@Composable
private fun OsVirtualizedInfoRows(
    rows: List<InfoRow>,
    valueSingleLine: Boolean = false,
    labelMinWidth: Dp = 72.dp,
    labelMaxWidth: Dp = 136.dp,
    labelMaxLines: Int = Int.MAX_VALUE,
    valueMinWidth: Dp = Dp.Unspecified
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 520.dp),
        userScrollEnabled = true
    ) {
        itemsIndexed(
            items = rows,
            key = { index, row -> "${row.key}-${row.value}-$index" }
        ) { _, row ->
            OsSectionInfoRow(
                label = row.key,
                value = row.value,
                valueSingleLine = valueSingleLine,
                labelMinWidth = labelMinWidth,
                labelMaxWidth = labelMaxWidth,
                labelMaxLines = labelMaxLines,
                valueMinWidth = valueMinWidth
            )
        }
    }
}

private sealed interface TopInfoVirtualizedItem {
    data class Header(val title: String) : TopInfoVirtualizedItem
    data class Entry(val row: InfoRow) : TopInfoVirtualizedItem
}

@Composable
private fun OsVirtualizedGroupedTopInfoRows(groupedRows: List<Pair<String, List<InfoRow>>>) {
    val rows = buildList {
        groupedRows.forEach { (type, entries) ->
            if (entries.isNotEmpty()) {
                add(TopInfoVirtualizedItem.Header(type))
                entries.forEach { entry -> add(TopInfoVirtualizedItem.Entry(entry)) }
            }
        }
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 520.dp),
        userScrollEnabled = true
    ) {
        itemsIndexed(
            items = rows,
            key = { index, item ->
                when (item) {
                    is TopInfoVirtualizedItem.Header -> "header-${item.title}-$index"
                    is TopInfoVirtualizedItem.Entry -> "entry-${item.row.key}-${item.row.value}-$index"
                }
            }
        ) { index, item ->
            when (item) {
                is TopInfoVirtualizedItem.Header -> {
                    Text(
                        text = item.title,
                        color = MiuixTheme.colorScheme.onBackground,
                        fontSize = AppTypographyTokens.CompactTitle.fontSize,
                        lineHeight = AppTypographyTokens.CompactTitle.lineHeight,
                        fontWeight = AppTypographyTokens.CompactTitle.fontWeight,
                        modifier = Modifier.padding(top = if (index == 0) 0.dp else 8.dp, bottom = 2.dp)
                    )
                }

                is TopInfoVirtualizedItem.Entry -> {
                    OsSectionInfoRow(
                        label = item.row.key,
                        value = item.row.value,
                        valueSingleLine = true,
                        labelMinWidth = 56.dp,
                        labelMaxWidth = 92.dp,
                        labelMaxLines = 1,
                        valueMinWidth = 140.dp
                    )
                }
            }
        }
    }
}

@Composable
internal fun OsCardVisibilityManagerSheet(
    show: Boolean,
    title: String,
    sheetBackdrop: LayerBackdrop,
    cardsHintText: String,
    onDismissRequest: () -> Unit,
    isCardVisible: (OsSectionCard) -> Boolean,
    onCardVisibilityChange: (OsSectionCard, Boolean) -> Unit
) {
    SnapshotWindowBottomSheet(
        show = show,
        title = title,
        onDismissRequest = onDismissRequest,
        startAction = {
            GlassIconButton(
                backdrop = sheetBackdrop,
                variant = GlassVariant.Bar,
                icon = appLucideCloseIcon(),
                contentDescription = stringResource(R.string.common_close),
                onClick = onDismissRequest
            )
        }
    ) {
        SheetContentColumn(
            scrollable = false,
            verticalSpacing = 10.dp
        ) {
            @Composable
            fun CardLabel(card: OsSectionCard, modifier: Modifier = Modifier) {
                Row(
                    modifier = modifier,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val iconModifier = Modifier
                        .size(18.dp)
                        .defaultMinSize(minHeight = 18.dp)
                    val icon = sectionCardIcon(card)
                    Icon(
                        imageVector = icon,
                        contentDescription = card.title,
                        tint = MiuixTheme.colorScheme.onBackground,
                        modifier = iconModifier
                    )
                    Text(text = card.title, color = MiuixTheme.colorScheme.onBackground)
                }
            }

            SheetSectionCard(verticalSpacing = 10.dp) {
                OsSectionCard.entries.filter { card ->
                    card != OsSectionCard.GOOGLE_SYSTEM_SERVICE &&
                        card != OsSectionCard.SHELL_RUNNER
                }.forEach { card ->
                    SheetControlRow(
                        labelContent = {
                            CardLabel(card = card, modifier = Modifier.defaultMinSize(minHeight = 24.dp))
                        }
                    ) {
                        Switch(
                            checked = isCardVisible(card),
                            onCheckedChange = { checked -> onCardVisibilityChange(card, checked) }
                        )
                    }
                }
            }

            SheetDescriptionText(text = cardsHintText)
        }
    }
}

@Composable
internal fun OsActivityVisibilityManagerSheet(
    show: Boolean,
    title: String,
    sheetBackdrop: LayerBackdrop,
    activityHintText: String,
    cards: List<OsActivityShortcutCard>,
    defaultCardTitle: String,
    transferInProgress: Boolean,
    onExportAllCards: () -> Unit,
    onImportAllCards: () -> Unit,
    onDismissRequest: () -> Unit,
    onCardVisibilityChange: (String, Boolean) -> Unit
) {
    SnapshotWindowBottomSheet(
        show = show,
        title = title,
        onDismissRequest = onDismissRequest,
        startAction = {
            GlassIconButton(
                backdrop = sheetBackdrop,
                variant = GlassVariant.Bar,
                icon = appLucideCloseIcon(),
                contentDescription = stringResource(R.string.common_close),
                onClick = onDismissRequest
            )
        }
    ) {
        SheetContentColumn(
            scrollable = false,
            verticalSpacing = 10.dp
        ) {
            val activityVisibilityItems = cards.map { card ->
                OsActivityVisibilityItem(
                    id = card.id,
                    title = card.config.title.ifBlank { defaultCardTitle },
                    packageName = card.config.packageName,
                    className = card.config.className,
                    builtInSample = card.isBuiltInSample,
                    visible = card.visible
                )
            }
            SheetSectionCard(verticalSpacing = 10.dp) {
                activityVisibilityItems.forEach { item ->
                    SheetControlRow(
                        labelContent = {
                            Row(
                                modifier = Modifier.defaultMinSize(minHeight = 24.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (item.packageName.isNotBlank() || item.className.isNotBlank()) {
                                    ShortcutActivityIcon(
                                        packageName = item.packageName,
                                        className = item.className,
                                        size = 18.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = osLucideEnterIcon(),
                                        contentDescription = item.title,
                                        tint = MiuixTheme.colorScheme.onBackground,
                                        modifier = Modifier
                                            .size(18.dp)
                                            .defaultMinSize(minHeight = 18.dp)
                                    )
                                }
                                Text(
                                    text = item.title,
                                    color = MiuixTheme.colorScheme.onBackground
                                )
                                if (item.builtInSample) {
                                    StatusPill(
                                        label = stringResource(R.string.os_activity_card_builtin_badge),
                                        color = Color(0xFF3B82F6),
                                        size = AppStatusPillSize.Compact
                                    )
                                }
                            }
                        }
                    ) {
                        Switch(
                            checked = item.visible,
                            onCheckedChange = { checked ->
                                onCardVisibilityChange(item.id, checked)
                            }
                        )
                    }
                }
            }
            SheetSectionCard(verticalSpacing = 8.dp) {
                Text(
                    text = stringResource(R.string.os_activity_sheet_transfer_title),
                    color = MiuixTheme.colorScheme.onBackground
                )
                SheetDescriptionText(text = stringResource(R.string.os_activity_sheet_transfer_desc))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GlassTextButton(
                        backdrop = sheetBackdrop,
                        text = stringResource(R.string.os_sheet_action_export_cards),
                        onClick = onExportAllCards,
                        modifier = Modifier.weight(1f),
                        enabled = !transferInProgress,
                        variant = GlassVariant.SheetAction
                    )
                    GlassTextButton(
                        backdrop = sheetBackdrop,
                        text = stringResource(R.string.os_sheet_action_import_cards),
                        onClick = onImportAllCards,
                        modifier = Modifier.weight(1f),
                        enabled = !transferInProgress,
                        variant = GlassVariant.SheetAction
                    )
                }
            }
            SheetDescriptionText(text = activityHintText)
        }
    }
}
