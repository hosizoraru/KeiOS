package com.example.keios.ui.page.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.keios.R
import com.example.keios.ui.page.main.widget.AppTypographyTokens
import com.example.keios.ui.page.main.widget.GlassIconButton
import com.example.keios.ui.page.main.widget.GlassVariant
import com.example.keios.ui.page.main.widget.MiuixAccordionCard
import com.example.keios.ui.page.main.widget.SheetContentColumn
import com.example.keios.ui.page.main.widget.SheetControlRow
import com.example.keios.ui.page.main.widget.SheetDescriptionText
import com.example.keios.ui.page.main.widget.SheetSectionCard
import com.example.keios.ui.page.main.widget.SnapshotWindowBottomSheet
import com.kyant.backdrop.backdrops.LayerBackdrop
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.Play
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
            } else {
                if (query.isBlank() && !expanded) {
                    displayedTopInfoRows.forEach { row ->
                        OsSectionInfoRow(label = row.key, value = row.value)
                    }
                } else {
                    groupedTopInfoRows.forEachIndexed { index, (type, rows) ->
                        Text(
                            text = type,
                            color = MiuixTheme.colorScheme.onBackground,
                            fontSize = AppTypographyTokens.CompactTitle.fontSize,
                            lineHeight = AppTypographyTokens.CompactTitle.lineHeight,
                            fontWeight = AppTypographyTokens.CompactTitle.fontWeight,
                            modifier = Modifier.padding(top = if (index == 0) 0.dp else 8.dp, bottom = 2.dp)
                        )
                        rows.forEach { row ->
                            OsSectionInfoRow(label = row.key, value = row.value)
                        }
                    }
                }
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
                        imageVector = MiuixIcons.Regular.Play,
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
                rows.forEach { row -> OsSectionInfoRow(label = row.key, value = row.value) }
            }
        }
    }
    item { Spacer(modifier = Modifier.height(8.dp)) }
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
                icon = MiuixIcons.Regular.Close,
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
                OsSectionCard.entries.filter { it != OsSectionCard.GOOGLE_SYSTEM_SERVICE }.forEach { card ->
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
                icon = MiuixIcons.Regular.Close,
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
                                        imageVector = MiuixIcons.Regular.Play,
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
            SheetDescriptionText(text = activityHintText)
        }
    }
}
