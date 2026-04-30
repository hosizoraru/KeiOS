package os.kei.ui.page.main.ba.support

import android.content.Context
import os.kei.R

private val legacyBaPoolTagLabels = setOf(
    "\u5e38\u9a7b",
    "\u9650\u5b9a",
    "FES\u9650\u5b9a",
    "FES \u9650\u5b9a",
    "\u8054\u52a8",
    "\u590d\u523b",
    "\u56de\u5fc6\u62db\u52df",
    "\u5361\u6c60",
    "\u5176\u4ed6"
)

internal fun normalizeBaCalendarKindFallback(raw: String): String {
    return when (val value = raw.trim()) {
        "\u5176\u4ed6" -> ""
        else -> value
    }
}

internal fun normalizeBaPoolTagFallback(raw: String): String {
    val value = raw.trim()
    return if (value in legacyBaPoolTagLabels) "" else value
}

internal fun Context.baCalendarKindLabel(kindId: Int, fallback: String): String {
    val fallbackText = normalizeBaCalendarKindFallback(fallback)
    return when (kindId) {
        14 -> getString(R.string.ba_calendar_kind_event)
        15 -> getString(R.string.ba_calendar_kind_total_grand_assault)
        16 -> getString(R.string.ba_calendar_kind_double_rewards)
        17 -> getString(R.string.ba_calendar_kind_tower)
        18 -> getString(R.string.ba_calendar_kind_guide_mission)
        19 -> getString(R.string.ba_calendar_kind_tactical_test)
        31 -> getString(R.string.ba_calendar_kind_other)
        else -> fallbackText.ifBlank { getString(R.string.ba_calendar_kind_other) }
    }
}

internal fun Context.baPoolTagLabel(tagId: Int, fallback: String): String {
    val fallbackText = normalizeBaPoolTagFallback(fallback)
    return when (tagId) {
        5 -> getString(R.string.ba_pool_tag_permanent)
        6 -> getString(R.string.ba_pool_tag_limited)
        7 -> getString(R.string.ba_pool_tag_fes_limited)
        8 -> getString(R.string.ba_pool_tag_collab)
        9 -> getString(R.string.ba_pool_tag_rerun)
        92 -> getString(R.string.ba_pool_tag_recollection)
        else -> fallbackText.ifBlank { getString(R.string.ba_pool_tag_recruitment) }
    }
}
