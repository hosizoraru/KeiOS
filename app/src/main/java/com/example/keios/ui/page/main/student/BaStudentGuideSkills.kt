package com.example.keios.ui.page.main.student

data class GuideSkillCardModel(
    val id: String,
    val type: String,
    val name: String,
    val iconUrl: String,
    val descriptionByLevel: Map<String, String>,
    val descriptionIconsByLevel: Map<String, List<String>> = emptyMap(),
    val costByLevel: Map<String, String>,
    val glossaryIcons: Map<String, String> = emptyMap(),
    val fallbackDescription: String = "",
    val fallbackDescriptionIcons: List<String> = emptyList()
) {
    val levelOptions: List<String>
        get() = descriptionByLevel.keys.sortedWith(compareBy { parseLevelNumber(it) ?: Int.MAX_VALUE })

    val defaultLevel: String
        get() = levelOptions.maxByOrNull { parseLevelNumber(it) ?: Int.MIN_VALUE }
            ?: levelOptions.firstOrNull()
            ?: "Lv.1"

    fun descriptionFor(level: String): String {
        return descriptionByLevel[level]
            ?: descriptionByLevel[defaultLevel]
            ?: fallbackDescription
    }

    fun descriptionIconsFor(level: String): List<String> {
        val fromLevel = descriptionIconsByLevel[level].orEmpty()
        if (fromLevel.isNotEmpty()) return fromLevel
        val fromDefault = descriptionIconsByLevel[defaultLevel].orEmpty()
        return if (fromDefault.isNotEmpty()) fromDefault else fallbackDescriptionIcons
    }

    fun costFor(level: String): String {
        return costByLevel[level]
            ?: costByLevel[defaultLevel]
            ?: costByLevel.values.firstOrNull()
            ?: ""
    }
}

data class GuideWeaponCardModel(
    val name: String,
    val imageUrl: String,
    val description: String,
    val statHeaders: List<String>,
    val statRows: List<GuideWeaponStatRow>,
    val extraStatRows: List<GuideWeaponStatRow> = emptyList(),
    val starEffects: List<GuideWeaponStarEffect>,
    val glossaryIcons: Map<String, String> = emptyMap()
)

data class GuideWeaponStatRow(
    val title: String,
    val values: List<String>
)

data class GuideWeaponStarEffect(
    val id: String,
    val starLabel: String,
    val starIconUrl: String = "",
    val name: String,
    val iconUrl: String,
    val descriptionByLevel: Map<String, String>,
    val descriptionIconsByLevel: Map<String, List<String>> = emptyMap(),
    val roleTag: String = "",
    val fallbackDescription: String = "",
    val fallbackDescriptionIcons: List<String> = emptyList()
) {
    val levelOptions: List<String>
        get() = descriptionByLevel.keys.sortedWith(compareBy { parseLevelNumber(it) ?: Int.MAX_VALUE })

    val defaultLevel: String
        get() = levelOptions.maxByOrNull { parseLevelNumber(it) ?: Int.MIN_VALUE }
            ?: levelOptions.firstOrNull()
            ?: "Lv.1"

    fun descriptionFor(level: String): String {
        return descriptionByLevel[level]
            ?: descriptionByLevel[defaultLevel]
            ?: fallbackDescription
    }

    fun descriptionIconsFor(level: String): List<String> {
        val fromLevel = descriptionIconsByLevel[level].orEmpty()
        if (fromLevel.isNotEmpty()) return fromLevel
        val fromDefault = descriptionIconsByLevel[defaultLevel].orEmpty()
        return if (fromDefault.isNotEmpty()) fromDefault else fallbackDescriptionIcons
    }
}

internal fun parseLevelNumber(label: String): Int? {
    return Regex("""(\d{1,2})""")
        .find(label)
        ?.groupValues
        ?.getOrNull(1)
        ?.toIntOrNull()
}
