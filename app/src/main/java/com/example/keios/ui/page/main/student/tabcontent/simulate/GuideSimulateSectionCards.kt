package com.example.keios.ui.page.main.student.tabcontent.simulate

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.keios.ui.page.main.student.BaGuideRow
import com.example.keios.ui.page.main.student.GuideRemoteIcon
import com.example.keios.ui.page.main.student.buildGuideTabCopyPayload
import com.example.keios.ui.page.main.student.guideTabCopyable
import com.example.keios.ui.page.main.student.tabcontent.profile.normalizeProfileFieldKey
import com.kyant.backdrop.backdrops.LayerBackdrop
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun GuideSimulateSectionCard(
    title: String,
    rows: List<BaGuideRow>,
    hint: String,
    backdrop: LayerBackdrop
) {
    val levelRowIndex = if (title == "能力解放") {
        rows.indexOfFirst { row ->
            Regex("""^\d+级$""").matches(normalizeProfileFieldKey(row.key))
        }
    } else {
        -1
    }
    val levelCapsule = when {
        levelRowIndex >= 0 -> rows[levelRowIndex].key.trim()
        else -> extractSimulateLevelCapsule(hint)
    }
    val displayRows = if (levelRowIndex >= 0) {
        rows.filterIndexed { index, _ -> index != levelRowIndex }
    } else {
        rows
    }
    val displayCapsule = if (title == "爱用品" && displayRows.isEmpty()) {
        ""
    } else {
        levelCapsule
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.defaultColors(
            color = Color(0x223B82F6),
            contentColor = MiuixTheme.colorScheme.onBackground
        ),
        onClick = {}
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GuideSimulateCardTitleRow(
                title = title,
                capsule = displayCapsule,
                backdrop = backdrop
            )
            if (displayRows.isNotEmpty()) {
                displayRows.forEach { row ->
                    GuideSimulateRowItem(
                        row = row,
                        backdrop = backdrop
                    )
                }
            } else {
                Text(
                    text = if (title == "爱用品") "此学生暂未佩戴爱用品。" else "暂无${title}数据。",
                    color = MiuixTheme.colorScheme.onBackgroundVariant
                )
            }
        }
    }
}

@Composable
internal fun GuideSimulateEquipmentCard(
    title: String,
    rows: List<BaGuideRow>,
    hint: String,
    backdrop: LayerBackdrop
) {
    val groups = buildSimulateEquipmentGroups(rows)
    val hintCapsule = extractSimulateLevelCapsule(hint)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.defaultColors(
            color = Color(0x223B82F6),
            contentColor = MiuixTheme.colorScheme.onBackground
        ),
        onClick = {}
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GuideSimulateCardTitleRow(
                title = title,
                capsule = hintCapsule,
                backdrop = backdrop
            )

            if (groups.isNotEmpty()) {
                groups.forEach { group ->
                    val groupCopyPayload = buildGuideTabCopyPayload(
                        group.slotLabel.ifBlank { "装备" },
                        listOf(group.itemName.trim(), group.tierText.trim())
                            .filter { it.isNotBlank() }
                            .joinToString(" · ")
                            .ifBlank { "-" }
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .guideTabCopyable(groupCopyPayload),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(0.52f),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (group.iconUrl.isNotBlank()) {
                                GuideRemoteIcon(
                                    imageUrl = group.iconUrl,
                                    iconWidth = 40.dp,
                                    iconHeight = 40.dp
                                )
                            }
                            group.itemName.takeIf { it.isNotBlank() }?.let { name ->
                                Text(
                                    text = name,
                                    color = MiuixTheme.colorScheme.onBackgroundVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.weight(0.48f),
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            GuideSimulateInlineCapsule(
                                text = group.slotLabel,
                                backdrop = backdrop
                            )
                        }
                    }

                    group.statRows.forEach { row ->
                        GuideSimulateRowItem(
                            row = row,
                            backdrop = backdrop
                        )
                    }
                }
            } else if (rows.isNotEmpty()) {
                rows.forEach { row ->
                    GuideSimulateRowItem(
                        row = row,
                        backdrop = backdrop
                    )
                }
            } else {
                Text(
                    text = "暂无装备数据。",
                    color = MiuixTheme.colorScheme.onBackgroundVariant
                )
            }
        }
    }
}

@Composable
internal fun GuideSimulateUnlockCard(
    title: String,
    rows: List<BaGuideRow>,
    hint: String,
    backdrop: LayerBackdrop
) {
    val viewData = buildSimulateUnlockViewData(rows, hint)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.defaultColors(
            color = Color(0x223B82F6),
            contentColor = MiuixTheme.colorScheme.onBackground
        ),
        onClick = {}
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GuideSimulateCardTitleRow(
                title = title,
                capsule = viewData.levelCapsule,
                backdrop = backdrop
            )

            if (viewData.rows.isNotEmpty()) {
                viewData.rows.forEach { row ->
                    val iconUrl = row.imageUrl.trim().ifBlank { row.imageUrls.firstOrNull().orEmpty() }
                    if (iconUrl.isNotBlank()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            GuideRemoteIcon(
                                imageUrl = iconUrl,
                                iconWidth = 78.dp,
                                iconHeight = 62.dp
                            )
                            GuideSimulateRowItem(
                                row = row.copy(imageUrl = "", imageUrls = emptyList()),
                                backdrop = backdrop
                            )
                        }
                    } else {
                        GuideSimulateRowItem(
                            row = row,
                            backdrop = backdrop
                        )
                    }
                }
            } else {
                Text(
                    text = "暂无能力解放数据。",
                    color = MiuixTheme.colorScheme.onBackgroundVariant
                )
            }
        }
    }
}
