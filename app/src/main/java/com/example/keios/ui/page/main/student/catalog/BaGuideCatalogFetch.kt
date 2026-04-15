package com.example.keios.ui.page.main.student.catalog

import com.example.keios.R
import com.example.keios.feature.ba.data.remote.GameKeeFetchHelper
import org.json.JSONObject
import java.util.Locale

private const val BA_GUIDE_SECOND_PAGE_ID = 23941
private const val BA_GUIDE_STUDENT_PID = 49443
private const val BA_GUIDE_NPC_SATELLITE_PID = 107619
private const val BA_GUIDE_INDEX_REFERER_PATH = "/ba/second/$BA_GUIDE_SECOND_PAGE_ID"
internal const val BA_GUIDE_INDEX_URL = "https://www.gamekee.com$BA_GUIDE_INDEX_REFERER_PATH"

internal enum class BaGuideCatalogTab(
    val label: String,
    val iconRes: Int
) {
    Student(label = "实装学生", iconRes = R.drawable.ba_tab_profile),
    NpcSatellite(label = "NPC及卫星", iconRes = R.drawable.ba_tab_skill),
}

internal data class BaGuideCatalogEntry(
    val entryId: Int,
    val pid: Int,
    val contentId: Long,
    val name: String,
    val alias: String,
    val iconUrl: String,
    val type: Int,
    val order: Int,
    val tab: BaGuideCatalogTab
) {
    val detailUrl: String
        get() = "https://www.gamekee.com/ba/tj/$contentId.html"

    val aliasDisplay: String
        get() = alias
            .split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .joinToString(" · ")
}

internal data class BaGuideCatalogBundle(
    val entriesByTab: Map<BaGuideCatalogTab, List<BaGuideCatalogEntry>>,
    val syncedAtMs: Long
) {
    fun entries(tab: BaGuideCatalogTab): List<BaGuideCatalogEntry> {
        return entriesByTab[tab].orEmpty()
    }

    companion object {
        val EMPTY = BaGuideCatalogBundle(
            entriesByTab = BaGuideCatalogTab.entries.associateWith { emptyList() },
            syncedAtMs = 0L
        )
    }
}

private data class RawEntry(
    val entryId: Int,
    val pid: Int,
    val contentId: Long,
    val name: String,
    val alias: String,
    val iconUrl: String,
    val type: Int,
    val order: Int
)

internal fun fetchBaGuideCatalogBundle(): BaGuideCatalogBundle {
    val studentRaw = fetchRawEntriesByPid(BA_GUIDE_STUDENT_PID)
    val npcSatelliteRaw = fetchRawEntriesByPid(BA_GUIDE_NPC_SATELLITE_PID)

    val studentEntries = studentRaw.map { raw ->
        raw.toCatalogEntry(tab = BaGuideCatalogTab.Student)
    }

    val npcSatelliteEntries = npcSatelliteRaw.map { raw ->
        raw.toCatalogEntry(tab = BaGuideCatalogTab.NpcSatellite)
    }

    return BaGuideCatalogBundle(
        entriesByTab = mapOf(
            BaGuideCatalogTab.Student to studentEntries.sortedBy { it.order },
            BaGuideCatalogTab.NpcSatellite to npcSatelliteEntries.sortedBy { it.order },
        ),
        syncedAtMs = System.currentTimeMillis()
    )
}

internal fun List<BaGuideCatalogEntry>.filterByQuery(query: String): List<BaGuideCatalogEntry> {
    val keyword = query.trim().lowercase(Locale.ROOT)
    if (keyword.isBlank()) return this
    return filter { entry ->
        entry.name.lowercase(Locale.ROOT).contains(keyword) ||
            entry.alias.lowercase(Locale.ROOT).contains(keyword) ||
            entry.contentId.toString().contains(keyword)
    }
}

private fun fetchRawEntriesByPid(pid: Int): List<RawEntry> {
    val body = GameKeeFetchHelper.fetchJson(
        pathOrUrl = "/v1/entry/treesByPid?pid=$pid",
        refererPath = BA_GUIDE_INDEX_REFERER_PATH,
        extraHeaders = mapOf(
            "device-num" to "1",
            "game-alias" to "ba"
        )
    )
    val root = JSONObject(body)
    if (root.optInt("code", -1) != 0) {
        error("catalog api code=${root.optInt("code", -1)} pid=$pid")
    }
    val data = root.optJSONArray("data") ?: return emptyList()
    val out = mutableListOf<RawEntry>()
    for (index in 0 until data.length()) {
        val item = data.optJSONObject(index) ?: continue
        val contentId = item.optLong("content_id", 0L)
        if (contentId <= 0L) continue
        val name = item.optString("name")
            .trim()
            .ifBlank { item.optString("title").trim() }
        if (name.isBlank()) continue
        out += RawEntry(
            entryId = item.optInt("id", 0),
            pid = item.optInt("pid", pid),
            contentId = contentId,
            name = name,
            alias = item.optString("name_alias").trim(),
            iconUrl = normalizeCatalogImageUrl(item.optString("icon")),
            type = item.optInt("type", 0),
            order = index
        )
    }
    return out
}

private fun RawEntry.toCatalogEntry(tab: BaGuideCatalogTab): BaGuideCatalogEntry {
    return BaGuideCatalogEntry(
        entryId = entryId,
        pid = pid,
        contentId = contentId,
        name = name,
        alias = alias,
        iconUrl = iconUrl,
        type = type,
        order = order,
        tab = tab
    )
}

private fun normalizeCatalogImageUrl(raw: String): String {
    val value = raw.trim()
    if (value.isBlank()) return ""
    if (value.startsWith("http://") || value.startsWith("https://")) return value
    if (value.startsWith("//")) return "https:$value"
    return if (value.startsWith("/")) {
        "https://www.gamekee.com$value"
    } else {
        "https://www.gamekee.com/$value"
    }
}
