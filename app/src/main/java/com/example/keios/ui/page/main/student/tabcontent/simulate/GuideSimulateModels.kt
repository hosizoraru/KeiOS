package com.example.keios.ui.page.main.student.tabcontent.simulate

import com.example.keios.ui.page.main.student.BaGuideRow

internal const val GUIDE_SIMULATE_CACHE_MAX_SIZE = 96
internal val guideSimulateDataCache = object : java.util.LinkedHashMap<String, GuideSimulateData>(
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

internal data class SimulateEquipmentGroup(
    val slotLabel: String,
    val itemName: String,
    val tierText: String,
    val iconUrl: String,
    val statRows: List<BaGuideRow>
)

internal data class SimulateBondGroup(
    val roleLabel: String,
    val iconUrl: String,
    val statRows: List<BaGuideRow>
)

internal data class SimulateUnlockViewData(
    val levelCapsule: String,
    val rows: List<BaGuideRow>
)

internal data class SimulateWeaponViewData(
    val imageUrl: String,
    val statRows: List<BaGuideRow>
)
