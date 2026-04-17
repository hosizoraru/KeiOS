package com.example.keios.ui.page.main.student

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.example.keios.R
import com.example.keios.feature.ba.data.remote.GameKeeFetchHelper
import com.example.keios.ui.page.main.student.catalog.BaGuideCatalogStore
import com.example.keios.ui.page.main.student.catalog.BaGuideCatalogTab
import com.example.keios.ui.page.main.widget.FrostedBlock
import com.example.keios.ui.page.main.widget.GlassTextButton
import com.example.keios.ui.page.main.widget.GlassVariant
import com.example.keios.ui.page.main.widget.MiuixInfoItem
import com.example.keios.ui.page.main.widget.CopyModeSelectionContainer
import com.example.keios.ui.page.main.widget.buildTextCopyPayload
import com.example.keios.ui.page.main.widget.copyModeAwareRow
import com.example.keios.ui.page.main.widget.rememberLightTextCopyAction
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.capsule.ContinuousCapsule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.abs
import java.util.LinkedHashMap
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

internal const val GUIDE_SIMULATE_CACHE_MAX_SIZE = 96
internal val guideSimulateDataCache = object : LinkedHashMap<String, GuideSimulateData>(
    GUIDE_SIMULATE_CACHE_MAX_SIZE,
    0.75f,
    true
) {
    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, GuideSimulateData>?): Boolean {
        return size > GUIDE_SIMULATE_CACHE_MAX_SIZE
    }
}

internal data class GuideSimulateData(
    val initialHint: String = "",
    val initialRows: List<BaGuideRow> = emptyList(),
    val maxHint: String = "",
    val maxRows: List<BaGuideRow> = emptyList(),
    val weaponHint: String = "",
    val weaponRows: List<BaGuideRow> = emptyList(),
    val equipmentHint: String = "",
    val equipmentRows: List<BaGuideRow> = emptyList(),
    val favorHint: String = "",
    val favorRows: List<BaGuideRow> = emptyList(),
    val unlockHint: String = "",
    val unlockRows: List<BaGuideRow> = emptyList(),
    val bondHint: String = "",
    val bondRows: List<BaGuideRow> = emptyList()
)

internal fun sanitizeSimulateFavorRows(rows: List<BaGuideRow>): List<BaGuideRow> {
    if (rows.isEmpty()) return emptyList()
    return rows
        .filterNot { row ->
            row.key.isBlank() &&
                row.value.isBlank() &&
                row.imageUrl.isBlank() &&
                row.imageUrls.isEmpty()
        }
        .filterNot { row ->
            val normalizedKey = normalizeProfileFieldKey(row.key)
            val hasMedia = row.imageUrl.isNotBlank() || row.imageUrls.isNotEmpty()
            val isTierMetaKey = Regex(
                """^t\d+(效果|所需升级材料|技能图标)$""",
                RegexOption.IGNORE_CASE
            ).matches(normalizedKey)
            isTierMetaKey && row.value.isBlank() && !hasMedia
        }
        .filterNot { row ->
            val normalizedKey = normalizeProfileFieldKey(row.key)
            val normalizedValue = normalizeProfileFieldKey(row.value)
            val hasMedia = row.imageUrl.isNotBlank() || row.imageUrls.isNotEmpty()
            val hasNumericValue = Regex("""\d""").containsMatchIn(row.value)
            val isBrokenStatPair =
                isLikelySimulateStatLabel(row.key) &&
                    isLikelySimulateStatLabel(row.value) &&
                    !hasNumericValue &&
                    !hasMedia
            val isTierLabel = Regex("""^T\d+$""", RegexOption.IGNORE_CASE).matches(normalizedKey)
            val isTierOnlyPlaceholder =
                isTierLabel &&
                    normalizedValue.isBlank() &&
                    !hasMedia
            isBrokenStatPair || isTierOnlyPlaceholder
        }
        .distinctBy { row ->
            val packedImages = row.imageUrls.joinToString("|")
            "${normalizeProfileFieldKey(row.key)}|${row.value.trim()}|${row.imageUrl.trim()}|$packedImages"
        }
}

internal fun sanitizeSimulateBondRows(rows: List<BaGuideRow>): List<BaGuideRow> {
    if (rows.isEmpty()) return emptyList()
    return rows.filterNot { row ->
        val normalizedKey = normalizeProfileFieldKey(row.key)
        Regex("""^羁绊角色\d+$""").matches(normalizedKey) &&
            row.value.isBlank() &&
            row.imageUrl.isBlank() &&
            row.imageUrls.isEmpty()
    }
}

@Composable
internal fun GuideSimulateAbilityCard(
    data: GuideSimulateData,
    backdrop: LayerBackdrop
) {
    var selectedAbility by rememberSaveable(
        data.initialRows.size,
        data.maxRows.size
    ) { mutableStateOf("最大培养") }
    val selectedRows = if (selectedAbility == "最大培养") data.maxRows else data.initialRows
    val selectedHint = if (selectedAbility == "最大培养") data.maxHint else data.initialHint
    val initialValueByKey = remember(data.initialRows) {
        linkedMapOf<String, String>().apply {
            data.initialRows.forEach { row ->
                val key = normalizeProfileFieldKey(row.key)
                if (key.isBlank() || row.value.isBlank()) return@forEach
                putIfAbsent(key, row.value.trim())
            }
        }
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "角色能力",
                    color = MiuixTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f),
                    fontWeight = FontWeight.Medium
                )
                listOf("初始能力", "最大培养").forEach { option ->
                    val selected = selectedAbility == option
                    GlassTextButton(
                        backdrop = backdrop,
                        text = option,
                        textColor = if (selected) Color(0xFF2563EB) else MiuixTheme.colorScheme.onBackgroundVariant,
                        containerColor = if (selected) Color(0x443B82F6) else null,
                        variant = GlassVariant.Compact,
                        onClick = { selectedAbility = option }
                    )
                }
            }
            selectedHint.takeIf { it.isNotBlank() }?.let { hint ->
                Text(
                    text = hint.trim('*').trim(),
                    color = Color(0xFF60A5FA),
                    style = MiuixTheme.textStyles.body2,
                    modifier = Modifier.guideTabCopyable(
                        buildGuideTabCopyPayload("角色能力说明", hint)
                    )
                )
            }
            if (selectedRows.isNotEmpty()) {
                selectedRows.forEach { row ->
                    val deltaText = if (selectedAbility == "最大培养") {
                        buildSimulateMaxDeltaText(
                            maxValue = row.value,
                            initialValue = initialValueByKey[normalizeProfileFieldKey(row.key)]
                        )
                    } else {
                        ""
                    }
                    GuideSimulateRowItem(
                        row = row,
                        backdrop = backdrop,
                        valueDelta = deltaText
                    )
                }
            } else {
                Text(
                    text = "暂无能力数据。",
                    color = MiuixTheme.colorScheme.onBackgroundVariant
                )
            }
        }
    }
}

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

internal data class SimulateEquipmentGroup(
    val slotLabel: String,
    val itemName: String,
    val tierText: String,
    val iconUrl: String,
    val statRows: List<BaGuideRow>
)

internal fun parseSimulateEquipmentGhostMediaUrl(row: BaGuideRow): String {
    val key = row.key.trim()
    if (key.isBlank()) return ""
    if (row.value.trim().isNotBlank()) return ""
    val looksLikeUrl = key.startsWith("//") ||
        key.startsWith("http://", ignoreCase = true) ||
        key.startsWith("https://", ignoreCase = true)
    if (!looksLikeUrl) return ""
    val isMediaUrl = Regex(
        """(?i)\.(png|jpe?g|webp|gif|bmp|mp4|webm|mkv|mov|mp3|ogg|wav|m4a)(\?.*)?$"""
    ).containsMatchIn(key)
    if (!isMediaUrl) return ""
    return normalizeGuideUrl(key)
}

internal fun isSimulateEquipmentGhostMediaRow(row: BaGuideRow): Boolean {
    return parseSimulateEquipmentGhostMediaUrl(row).isNotBlank()
}

internal fun buildSimulateEquipmentGroups(rows: List<BaGuideRow>): List<SimulateEquipmentGroup> {
    if (rows.isEmpty()) return emptyList()

    val groups = mutableListOf<SimulateEquipmentGroup>()
    var currentSlot = ""
    var currentItemName = ""
    var currentTierText = ""
    var currentIcon = ""
    val currentStats = mutableListOf<BaGuideRow>()

    fun commitGroup() {
        if (currentSlot.isBlank() && currentItemName.isBlank() && currentStats.isEmpty()) return
        groups += SimulateEquipmentGroup(
            slotLabel = currentSlot.ifBlank { "装备" },
            itemName = currentItemName,
            tierText = currentTierText,
            iconUrl = currentIcon,
            statRows = currentStats.toList()
        )
        currentSlot = ""
        currentItemName = ""
        currentTierText = ""
        currentIcon = ""
        currentStats.clear()
    }

    rows.forEach { row ->
        val key = row.key.trim()
        val normalizedKey = normalizeProfileFieldKey(key)
        val rowIcon = row.imageUrl.trim().ifBlank { row.imageUrls.firstOrNull().orEmpty() }

        if (Regex("""^\d+号装备$""").matches(normalizedKey)) {
            commitGroup()
            currentSlot = key
            if (rowIcon.isNotBlank()) {
                currentIcon = rowIcon
            }
            return@forEach
        }

        val ghostMediaUrl = parseSimulateEquipmentGhostMediaUrl(row)
        if (ghostMediaUrl.isNotBlank()) {
            currentIcon = ghostMediaUrl
            return@forEach
        }

        val isMetaRow = !isLikelySimulateStatLabel(key) && !isSimulateSubHeader(key)
        if (currentItemName.isBlank() && isMetaRow) {
            currentItemName = key
            currentTierText = row.value.trim()
            if (currentIcon.isBlank() && rowIcon.isNotBlank()) {
                currentIcon = rowIcon
            }
            return@forEach
        }

        if (key.isBlank() && row.value.isBlank()) return@forEach
        currentStats += row.copy(imageUrl = "", imageUrls = emptyList())
    }
    commitGroup()

    return groups
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

internal data class SimulateBondGroup(
    val roleLabel: String,
    val iconUrl: String,
    val statRows: List<BaGuideRow>
)

internal data class SimulateUnlockViewData(
    val levelCapsule: String,
    val rows: List<BaGuideRow>
)

internal fun buildSimulateUnlockViewData(
    rows: List<BaGuideRow>,
    hint: String
): SimulateUnlockViewData {
    if (rows.isEmpty()) {
        return SimulateUnlockViewData(
            levelCapsule = extractSimulateLevelCapsule(hint),
            rows = emptyList()
        )
    }

    val levelRowIndex = rows.indexOfFirst { row ->
        Regex("""^\d+级$""").matches(normalizeProfileFieldKey(row.key))
    }
    val capsule = when {
        levelRowIndex >= 0 -> rows[levelRowIndex].key.trim()
        else -> extractSimulateLevelCapsule(hint)
    }
    val contentRows = if (levelRowIndex >= 0) {
        rows.filterIndexed { index, _ -> index != levelRowIndex }
    } else {
        rows
    }
    return SimulateUnlockViewData(
        levelCapsule = capsule,
        rows = contentRows
    )
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

internal fun buildSimulateBondGroups(rows: List<BaGuideRow>): List<SimulateBondGroup> {
    if (rows.isEmpty()) return emptyList()

    val groups = mutableListOf<SimulateBondGroup>()
    var currentRole = ""
    var currentIcon = ""
    val currentRows = mutableListOf<BaGuideRow>()

    fun commitGroup() {
        if (currentRole.isBlank() && currentRows.isEmpty()) return
        groups += SimulateBondGroup(
            roleLabel = currentRole.ifBlank { "羁绊角色" },
            iconUrl = currentIcon,
            statRows = currentRows.toList()
        )
        currentRole = ""
        currentIcon = ""
        currentRows.clear()
    }

    rows.forEach { row ->
        val key = row.key.trim()
        val normalizedKey = normalizeProfileFieldKey(key)
        val rowIcon = row.imageUrl.trim().ifBlank { row.imageUrls.firstOrNull().orEmpty() }

        if (Regex("""^羁绊角色\d+$""").matches(normalizedKey)) {
            commitGroup()
            currentRole = key
            currentIcon = rowIcon
            return@forEach
        }

        if (key.isBlank() && row.value.isBlank()) return@forEach
        currentRows += row.copy(
            imageUrl = "",
            imageUrls = emptyList()
        )
    }
    commitGroup()

    return groups
}

@Composable
internal fun GuideSimulateBondCard(
    title: String,
    rows: List<BaGuideRow>,
    hint: String,
    backdrop: LayerBackdrop
) {
    val groups = buildSimulateBondGroups(rows)
    val levelCapsule = extractSimulateLevelCapsule(hint)

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
                capsule = levelCapsule,
                backdrop = backdrop
            )

            if (groups.isNotEmpty()) {
                groups.forEach { group ->
                    val groupCopyPayload = buildGuideTabCopyPayload(
                        "羁绊角色",
                        group.roleLabel.ifBlank { "羁绊角色" }
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .guideTabCopyable(groupCopyPayload),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        if (group.iconUrl.isNotBlank()) {
                            Box(modifier = Modifier.width(112.dp)) {
                                GuideRemoteImage(
                                    imageUrl = group.iconUrl,
                                    imageHeight = 86.dp
                                )
                            }
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = group.roleLabel,
                                color = MiuixTheme.colorScheme.onBackgroundVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            group.statRows.forEach { stat ->
                                GuideSimulateRowItem(
                                    row = stat,
                                    backdrop = backdrop
                                )
                            }
                        }
                    }
                }
            } else {
                Text(
                    text = "暂无羁绊等级奖励数据。",
                    color = MiuixTheme.colorScheme.onBackgroundVariant
                )
            }
        }
    }
}

internal data class SimulateWeaponViewData(
    val imageUrl: String,
    val statRows: List<BaGuideRow>
)

internal fun buildSimulateWeaponViewData(rows: List<BaGuideRow>): SimulateWeaponViewData {
    if (rows.isEmpty()) return SimulateWeaponViewData("", emptyList())
    val imageUrl = rows.firstNotNullOfOrNull { row ->
        row.imageUrl.trim().ifBlank { row.imageUrls.firstOrNull().orEmpty() }.takeIf { it.isNotBlank() }
    }.orEmpty()
    val statRows = rows
        .filter { row ->
            val key = row.key.trim()
            val value = row.value.trim()
            key.isNotBlank() || value.isNotBlank()
        }
        .map { row ->
            val rowIcon = row.imageUrl.trim().ifBlank { row.imageUrls.firstOrNull().orEmpty() }
            if (imageUrl.isNotBlank() && rowIcon == imageUrl) {
                row.copy(imageUrl = "", imageUrls = emptyList())
            } else {
                row
            }
        }
    return SimulateWeaponViewData(
        imageUrl = imageUrl,
        statRows = statRows
    )
}

@Composable
internal fun GuideSimulateWeaponCard(
    title: String,
    rows: List<BaGuideRow>,
    hint: String,
    backdrop: LayerBackdrop
) {
    val viewData = buildSimulateWeaponViewData(rows)
    val levelCapsule = extractSimulateLevelCapsule(hint)

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
                capsule = levelCapsule,
                backdrop = backdrop
            )

            if (viewData.imageUrl.isNotBlank() || viewData.statRows.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    if (viewData.imageUrl.isNotBlank()) {
                        Box(modifier = Modifier.width(112.dp)) {
                            GuideRemoteImage(
                                imageUrl = viewData.imageUrl,
                                imageHeight = 72.dp
                            )
                        }
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        viewData.statRows.forEach { row ->
                            GuideSimulateRowItem(
                                row = row,
                                backdrop = backdrop
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = "暂无专武数据。",
                    color = MiuixTheme.colorScheme.onBackgroundVariant
                )
            }
        }
    }
}

internal fun extractSimulateLevelCapsule(rawHint: String): String {
    val hint = rawHint.trim().trim('*').trim()
    if (hint.isBlank()) return ""

    Regex("""(?i)Lv\s*\d+""")
        .find(hint)
        ?.value
        ?.replace(" ", "")
        ?.let { raw ->
            val digits = Regex("""\d+""").find(raw)?.value.orEmpty()
            if (digits.isNotBlank()) return "Lv$digits"
        }

    Regex("""(?i)T\d+""")
        .find(hint)
        ?.value
        ?.replace(" ", "")
        ?.uppercase()
        ?.let { return it }

    Regex("""\d+级""")
        .find(hint)
        ?.value
        ?.let { return it }

    return ""
}

@Composable
internal fun GuideSimulateCardTitleRow(
    title: String,
    capsule: String,
    backdrop: LayerBackdrop
) {
    val copyPayload = remember(title, capsule) {
        buildGuideTabCopyPayload(title, capsule.ifBlank { "-" })
    }
    CopyModeSelectionContainer {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .guideTabCopyable(copyPayload),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = MiuixTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            capsule.takeIf { it.isNotBlank() }?.let { label ->
                GuideSimulateInlineCapsule(
                    text = label,
                    backdrop = backdrop
                )
            }
        }
    }
}

@Composable
internal fun GuideSimulateInlineCapsule(
    text: String,
    backdrop: LayerBackdrop
) {
    GlassTextButton(
        backdrop = backdrop,
        text = text,
        enabled = false,
        textColor = Color(0xFF60A5FA),
        variant = GlassVariant.Compact,
        onClick = {}
    )
}

@Composable
internal fun GuideSimulateRowItem(
    row: BaGuideRow,
    backdrop: LayerBackdrop,
    valueDelta: String = ""
) {
    val key = row.key.trim().ifBlank { "信息" }
    val value = row.value.trim()
    val rowCopyAction = rememberGuideTabCopyAction(buildGuideTabCopyPayload(key, value.ifBlank { "-" }))
    val iconUrl = row.imageUrl.trim().ifBlank { row.imageUrls.firstOrNull().orEmpty() }
    val statGlyph = simulateStatGlyphForKey(key)
    if (isSimulateSubHeader(key)) {
        CopyModeSelectionContainer {
            Row(
                modifier = Modifier.copyModeAwareRow(
                    copyPayload = buildGuideTabCopyPayload(key, value.ifBlank { "-" }),
                    onLongClick = rowCopyAction
                ),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (iconUrl.isNotBlank()) {
                    GuideRemoteIcon(
                        imageUrl = iconUrl,
                        iconWidth = 24.dp,
                        iconHeight = 24.dp
                    )
                }
                GlassTextButton(
                    backdrop = backdrop,
                    text = key,
                    enabled = false,
                    textColor = Color(0xFF3B82F6),
                    variant = GlassVariant.Compact,
                    onClick = {}
                )
            }
        }
        return
    }

    val valueColor = when {
        value.contains("%") -> Color(0xFF5FA8FF)
        value.matches(Regex("""(?i)^T\d+.*$""")) -> Color(0xFF5FA8FF)
        value.matches(Regex("""(?i)^Lv\d+.*$""")) -> Color(0xFF5FA8FF)
        key.contains("COST", ignoreCase = true) -> Color(0xFF5FA8FF)
        else -> MiuixTheme.colorScheme.onBackground
    }

    CopyModeSelectionContainer {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .copyModeAwareRow(
                    copyPayload = buildGuideTabCopyPayload(key, value.ifBlank { "-" }),
                    onLongClick = rowCopyAction
                ),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(0.45f),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (iconUrl.isNotBlank()) {
                    GuideRemoteIcon(
                        imageUrl = iconUrl,
                        iconWidth = 24.dp,
                        iconHeight = 24.dp
                    )
                } else if (statGlyph != null) {
                    Text(
                        text = statGlyph,
                        color = MiuixTheme.colorScheme.onBackgroundVariant,
                        modifier = Modifier.width(20.dp),
                        textAlign = TextAlign.Center
                    )
                }
                Text(
                    text = key,
                    color = MiuixTheme.colorScheme.onBackgroundVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(
                modifier = Modifier.weight(0.55f),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = value.ifBlank { "-" },
                    color = valueColor,
                    textAlign = TextAlign.End,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                valueDelta.takeIf { it.isNotBlank() }?.let { delta ->
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = delta,
                        color = Color(0xFFE3B547),
                        maxLines = 1,
                        overflow = TextOverflow.Clip
                    )
                }
            }
        }
    }
}

internal fun isSimulateSubHeader(key: String): Boolean {
    val normalized = normalizeProfileFieldKey(key)
    if (Regex("""^\d+号装备$""").matches(normalized)) return true
    if (Regex("""^羁绊角色\d+$""").matches(normalized)) return true
    if (Regex("""^\d+级$""").matches(normalized)) return true
    return false
}

internal fun simulateStatGlyphForKey(raw: String): String? {
    val key = normalizeProfileFieldKey(raw)
    return when (key) {
        normalizeProfileFieldKey("攻击力") -> "✢"
        normalizeProfileFieldKey("防御力") -> "⛨"
        normalizeProfileFieldKey("生命值") -> "♥"
        normalizeProfileFieldKey("治愈力") -> "✚"
        normalizeProfileFieldKey("命中值") -> "◎"
        normalizeProfileFieldKey("闪避值") -> "◌"
        normalizeProfileFieldKey("暴击值") -> "✶"
        normalizeProfileFieldKey("暴击伤害") -> "✹"
        normalizeProfileFieldKey("稳定值") -> "≋"
        normalizeProfileFieldKey("射程") -> "➚"
        normalizeProfileFieldKey("群控强化力") -> "⬆"
        normalizeProfileFieldKey("群控抵抗力") -> "⬡"
        normalizeProfileFieldKey("装弹数") -> "☰"
        normalizeProfileFieldKey("防御无视值") -> "⊘"
        normalizeProfileFieldKey("受恢复率") -> "⟳"
        normalizeProfileFieldKey("COST恢复力") -> "⌛"
        normalizeProfileFieldKey("暴击抵抗值") -> "⛯"
        normalizeProfileFieldKey("暴伤抵抗率"),
        normalizeProfileFieldKey("暴击伤害抵抗率") -> "✺"
        else -> null
    }
}

internal fun buildSimulateMaxDeltaText(
    maxValue: String,
    initialValue: String?
): String {
    val maxText = maxValue.trim()
    val initialText = initialValue?.trim().orEmpty()
    if (maxText.isBlank() || initialText.isBlank()) return ""
    if (Regex("""\([+-]\d+(\.\d+)?\)""").containsMatchIn(maxText)) return ""

    val maxNumber = extractComparableNumber(maxText) ?: return ""
    val initialNumber = extractComparableNumber(initialText) ?: return ""
    val diff = maxNumber - initialNumber
    if (abs(diff) < 0.0001) return ""

    val sign = if (diff > 0) "+" else "-"
    val absDiff = abs(diff)
    val deltaText = if (abs(absDiff - absDiff.toLong().toDouble()) < 0.0001) {
        absDiff.toLong().toString()
    } else {
        String.format(Locale.US, "%.2f", absDiff).trimEnd('0').trimEnd('.')
    }
    return "($sign$deltaText)"
}

internal fun extractComparableNumber(raw: String): Double? {
    val normalized = raw.replace(",", "").trim()
    val numberText = Regex("""-?\d+(\.\d+)?""")
        .find(normalized)
        ?.value
        .orEmpty()
    return numberText.toDoubleOrNull()
}

internal fun buildGuideSimulateCacheKey(rows: List<BaGuideRow>): String {
    var hash = 17
    rows.forEach { row ->
        hash = 31 * hash + normalizeProfileFieldKey(row.key).hashCode()
        hash = 31 * hash + row.value.trim().hashCode()
        hash = 31 * hash + row.imageUrl.trim().hashCode()
        row.imageUrls.forEach { image ->
            hash = 31 * hash + image.trim().hashCode()
        }
    }
    return "${rows.size}|$hash"
}

internal fun buildGuideSimulateData(rows: List<BaGuideRow>): GuideSimulateData {
    if (rows.isEmpty()) return GuideSimulateData()
    val cacheKey = buildGuideSimulateCacheKey(rows)
    synchronized(guideSimulateDataCache) {
        guideSimulateDataCache[cacheKey]?.let { return it }
    }
    val sections = linkedMapOf<String, MutableList<BaGuideRow>>()
    val hints = mutableMapOf<String, String>()
    var currentSection = ""

    rows.forEach { row ->
        val header = resolveSimulateSectionName(row.key)
        if (header != null) {
            currentSection = header
            sections.getOrPut(header) { mutableListOf() }
            val hint = row.value.trim().trim('*').trim()
            if (hint.isNotBlank()) {
                hints[header] = hint
            }
            return@forEach
        }
        if (currentSection.isBlank()) return@forEach

        val cleaned = row.copy(
            key = row.key.trim(),
            value = row.value.trim(),
            imageUrl = row.imageUrl.trim(),
            imageUrls = row.imageUrls.map { it.trim() }.filter { it.isNotBlank() }.distinct()
        )
        if (
            cleaned.key.isBlank() &&
            cleaned.value.isBlank() &&
            cleaned.imageUrl.isBlank() &&
            cleaned.imageUrls.isEmpty()
        ) return@forEach
        sections.getOrPut(currentSection) { mutableListOf() } += cleaned
    }

    val computed = GuideSimulateData(
        initialHint = hints["初始数据"].orEmpty(),
        initialRows = expandSimulateRows(sections["初始数据"].orEmpty()),
        maxHint = hints["顶级数据"].orEmpty(),
        maxRows = expandSimulateRows(sections["顶级数据"].orEmpty()),
        weaponHint = hints["专武"].orEmpty(),
        weaponRows = expandSimulateRows(sections["专武"].orEmpty()),
        equipmentHint = hints["装备"].orEmpty(),
        equipmentRows = expandSimulateRows(sections["装备"].orEmpty()),
        favorHint = hints["爱用品"].orEmpty(),
        favorRows = sanitizeSimulateFavorRows(
            expandSimulateRows(sections["爱用品"].orEmpty())
        ),
        unlockHint = hints["能力解放"].orEmpty(),
        unlockRows = expandSimulateRows(sections["能力解放"].orEmpty()),
        bondHint = hints["羁绊等级奖励"].orEmpty(),
        bondRows = sanitizeSimulateBondRows(
            expandSimulateRows(sections["羁绊等级奖励"].orEmpty())
        )
    )
    synchronized(guideSimulateDataCache) {
        guideSimulateDataCache[cacheKey] = computed
    }
    return computed
}

internal fun resolveSimulateSectionName(rawKey: String): String? {
    val normalized = normalizeProfileFieldKey(rawKey)
    return when {
        normalized == normalizeProfileFieldKey("初始数据") -> "初始数据"
        normalized == normalizeProfileFieldKey("顶级数据") -> "顶级数据"
        normalized == normalizeProfileFieldKey("专武") -> "专武"
        normalized == normalizeProfileFieldKey("装备") -> "装备"
        normalized == normalizeProfileFieldKey("爱用品") -> "爱用品"
        normalized == normalizeProfileFieldKey("能力解放") -> "能力解放"
        normalized == normalizeProfileFieldKey("羁绊等级奖励") -> "羁绊等级奖励"
        else -> null
    }
}

internal fun isLikelySimulateStatLabel(raw: String): Boolean {
    val normalized = normalizeProfileFieldKey(raw)
    if (normalized in normalizedTopDataStatKeys) return true
    val extraStatKeys = setOf(
        "暴伤抵抗率",
        "暴击抵抗值",
        "暴伤抵抗值"
    ).map(::normalizeProfileFieldKey).toSet()
    if (normalized in extraStatKeys) return true
    return normalized.endsWith("值") || normalized.endsWith("率")
}

internal fun expandSimulateRows(rows: List<BaGuideRow>): List<BaGuideRow> {
    if (rows.isEmpty()) return emptyList()
    val expanded = mutableListOf<BaGuideRow>()
    rows.forEach { row ->
        val key = row.key.trim()
        val value = row.value.trim()
        val icon = row.imageUrl.trim().ifBlank { row.imageUrls.firstOrNull().orEmpty() }
        val images = row.imageUrls.ifEmpty { listOfNotNull(icon.takeIf { it.isNotBlank() }) }
        if (key.isBlank() && value.isBlank() && images.isEmpty()) return@forEach

        if (value.isBlank()) {
            if (key.isNotBlank() || icon.isNotBlank()) {
                expanded += BaGuideRow(
                    key = key.ifBlank { "信息" },
                    value = "",
                    imageUrl = icon,
                    imageUrls = images
                )
            }
            return@forEach
        }

        val tokens = splitGuideCompositeValues(value)
        if (tokens.isEmpty()) {
            expanded += BaGuideRow(
                key = key.ifBlank { "信息" },
                value = value,
                imageUrl = icon,
                imageUrls = images
            )
            return@forEach
        }

        val firstTokenLooksLikeStat = isLikelySimulateStatLabel(tokens.first())
        var index = 0
        if (!firstTokenLooksLikeStat) {
            expanded += BaGuideRow(
                key = key.ifBlank { "等级" },
                value = tokens.first().trim(),
                imageUrl = icon,
                imageUrls = images
            )
            index = 1
        } else if (key.isNotBlank() && !isLikelySimulateStatLabel(key) && !isSimulateSubHeader(key)) {
            expanded += BaGuideRow(
                key = key,
                value = "",
                imageUrl = icon,
                imageUrls = images
            )
        } else if (icon.isNotBlank() && key.isNotBlank()) {
            expanded += BaGuideRow(
                key = key,
                value = "",
                imageUrl = icon,
                imageUrls = images
            )
        }

        var pairIndex = 0
        while (index + 1 < tokens.size) {
            val statKey = tokens[index].trim()
            val statValue = tokens[index + 1].trim()
            if (statKey.isNotBlank() && statValue.isNotBlank()) {
                val pairIcon = if (images.size > 1) images.getOrNull(pairIndex).orEmpty() else ""
                expanded += BaGuideRow(
                    key = statKey,
                    value = statValue,
                    imageUrl = pairIcon,
                    imageUrls = listOfNotNull(pairIcon.takeIf { it.isNotBlank() })
                )
            }
            pairIndex += 1
            index += 2
        }
    }

    return expanded.distinctBy { row ->
        val packedImages = row.imageUrls.joinToString("|")
        "${normalizeProfileFieldKey(row.key)}|${row.value.trim()}|${row.imageUrl.trim()}|$packedImages"
    }
}
