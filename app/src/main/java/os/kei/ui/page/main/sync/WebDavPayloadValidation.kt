package os.kei.ui.page.main.sync

import org.json.JSONArray
import org.json.JSONObject
import os.kei.feature.github.domain.GitHubTrackedItemsTransferService
import os.kei.ui.page.main.ba.support.countBaAccountsExportJson

/** Reject malformed/foreign/partially parsed backups before any domain mutation. Empty arrays are valid. */
internal fun validateWebDavPayload(item: WebDavSyncItem, raw: String) {
    val trimmed = raw.trim()
    val root = if (trimmed.startsWith("[")) null else JSONObject(trimmed)
    val array = if (root == null) JSONArray(trimmed) else {
        val keys = when (item) {
            WebDavSyncItem.GitHubTracked -> listOf("items", "trackedItems")
            WebDavSyncItem.BaAccounts -> listOf("accounts")
            WebDavSyncItem.BaCatalogFavorites -> listOf("favorites", "catalogFavorites", "students")
            WebDavSyncItem.BaBgmFavorites -> listOf("favorites", "bgmFavorites")
            WebDavSyncItem.OsActivityCards, WebDavSyncItem.OsShellCards -> listOf("items")
        }
        keys.firstNotNullOfOrNull { root.optJSONArray(it) }
            ?: error("Backup does not contain the expected dataset")
    }
    root?.let {
        val maximum = if (item == WebDavSyncItem.GitHubTracked) 4 else 1
        require(it.optInt("schemaVersion", it.optInt("version", 1)) in 0..maximum) {
            "Backup was written by an unsupported version"
        }
    }
    if (item != WebDavSyncItem.BaCatalogFavorites) {
        require((0 until array.length()).all { array.optJSONObject(it) != null }) {
            "Backup contains malformed entries"
        }
    }
    when (item) {
        WebDavSyncItem.GitHubTracked -> require(GitHubTrackedItemsTransferService.parseImport(raw).invalidCount == 0)
        WebDavSyncItem.BaAccounts -> require(countBaAccountsExportJson(raw) == array.length()) {
            "Backup contains invalid accounts"
        }
        WebDavSyncItem.BaCatalogFavorites -> for (i in 0 until array.length()) {
            val entry = array.opt(i)
            val id = when (entry) {
                is Number -> entry.toLong()
                is String -> entry.toLongOrNull() ?: -1L
                is JSONObject -> entry.optLong("contentId", entry.optLong("id", -1))
                else -> -1L
            }
            require(id > 0) { "Invalid catalog favorite" }
        }
        WebDavSyncItem.BaBgmFavorites -> for (i in 0 until array.length()) {
            val entry = array.getJSONObject(i)
            require(entry.optString("audioUrl").isNotBlank()) {
                "Invalid BGM favorite"
            }
        }
        else -> Unit
    }
    val expectedSchema = when (item) {
        WebDavSyncItem.OsActivityCards -> "keios.os.activity.cards.v1"
        WebDavSyncItem.OsShellCards -> "keios.os.shell.cards.v1"
        else -> null
    }
    if (expectedSchema != null && root?.has("schema") == true) {
        require(root.optString("schema") == expectedSchema) { "Backup belongs to another dataset" }
    }
    val expectedType = when (item) {
        WebDavSyncItem.BaCatalogFavorites -> "keios.ba.catalog_favorites"
        WebDavSyncItem.BaBgmFavorites -> "keios.ba.bgm_favorites"
        else -> null
    }
    if (expectedType != null && root?.has("type") == true) {
        require(root.optString("type") in setOf(expectedType, "keios.ba.catalog_all_favorites")) {
            "Backup belongs to another dataset"
        }
    }
}
