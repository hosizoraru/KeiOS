package com.example.keios.ui.page.main.student.tabcontent.simulate

import com.example.keios.ui.page.main.student.BaGuideRow
import com.example.keios.ui.page.main.student.fetch.normalizeGuideUrl
import com.example.keios.ui.page.main.student.tabcontent.profile.normalizeProfileFieldKey

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
