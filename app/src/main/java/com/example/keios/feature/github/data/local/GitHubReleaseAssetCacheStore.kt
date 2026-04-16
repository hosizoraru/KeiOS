package com.example.keios.feature.github.data.local

import com.example.keios.feature.github.data.remote.GitHubReleaseAssetBundle
import com.example.keios.feature.github.data.remote.GitHubReleaseAssetFile
import com.tencent.mmkv.MMKV
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest

internal object GitHubReleaseAssetCacheStore {
    private const val KV_ID = "github_release_asset_cache"
    private const val KEY_INDEX = "entry_index"

    private val store: MMKV by lazy { MMKV.mmkvWithID(KV_ID) }

    private fun kv(): MMKV = store

    fun buildCacheKey(
        owner: String,
        repo: String,
        rawTag: String,
        releaseUrl: String,
        preferHtml: Boolean,
        aggressiveFiltering: Boolean,
        includeAllAssets: Boolean,
        hasApiToken: Boolean
    ): String {
        return listOf(
            owner.trim().lowercase(),
            repo.trim().lowercase(),
            rawTag.trim().lowercase(),
            releaseUrl.trim(),
            preferHtml.toString(),
            aggressiveFiltering.toString(),
            includeAllAssets.toString(),
            hasApiToken.toString()
        ).joinToString("|")
    }

    private fun keyId(cacheKey: String): String = sha1(cacheKey)

    private fun entryStoreKey(id: String): String = "entry_$id"

    private fun loadIndex(kv: MMKV = store): MutableSet<String> {
        val raw = kv.decodeString(KEY_INDEX, "").orEmpty()
        if (raw.isBlank()) return mutableSetOf()
        return raw.split(',')
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toMutableSet()
    }

    private fun saveIndex(index: Set<String>, kv: MMKV = store) {
        kv.encode(KEY_INDEX, index.filter { it.isNotBlank() }.sorted().joinToString(","))
    }

    private fun addIndex(id: String, kv: MMKV = store) {
        val index = loadIndex(kv)
        index.add(id)
        saveIndex(index, kv)
    }

    private fun removeIndex(id: String, kv: MMKV = store) {
        val index = loadIndex(kv)
        index.remove(id)
        saveIndex(index, kv)
    }

    fun load(
        cacheKey: String,
        refreshIntervalHours: Int,
        nowMs: Long = System.currentTimeMillis()
    ): GitHubReleaseAssetBundle? {
        val normalizedKey = cacheKey.trim()
        if (normalizedKey.isBlank()) return null
        val id = keyId(normalizedKey)
        val raw = kv().decodeString(entryStoreKey(id), "").orEmpty()
        if (raw.isBlank()) return null
        val root = runCatching { JSONObject(raw) }.getOrNull() ?: return null
        if (root.optString("cacheKey").trim() != normalizedKey) return null
        val syncedAtMs = root.optLong("syncedAtMs", 0L).coerceAtLeast(0L)
        val intervalMs = refreshIntervalHours.coerceAtLeast(1) * 60L * 60L * 1000L
        val expired = syncedAtMs <= 0L || (nowMs - syncedAtMs).coerceAtLeast(0L) >= intervalMs
        if (expired) {
            kv().removeValueForKey(entryStoreKey(id))
            removeIndex(id)
            return null
        }
        return decodeBundle(root.optJSONObject("bundle")) ?: run {
            kv().removeValueForKey(entryStoreKey(id))
            removeIndex(id)
            null
        }
    }

    fun save(
        cacheKey: String,
        bundle: GitHubReleaseAssetBundle,
        syncedAtMs: Long = System.currentTimeMillis()
    ) {
        val normalizedKey = cacheKey.trim()
        if (normalizedKey.isBlank()) return
        val id = keyId(normalizedKey)
        val payload = JSONObject().apply {
            put("cacheKey", normalizedKey)
            put("syncedAtMs", syncedAtMs.coerceAtLeast(0L))
            put("bundle", encodeBundle(bundle))
        }.toString()
        kv().encode(entryStoreKey(id), payload)
        addIndex(id)
    }

    fun clearAll() {
        val kv = kv()
        loadIndex(kv).forEach { id ->
            kv.removeValueForKey(entryStoreKey(id))
        }
        kv.removeValueForKey(KEY_INDEX)
        kv.trim()
    }

    fun cachedEntryCount(): Int = loadIndex().size

    private fun encodeBundle(bundle: GitHubReleaseAssetBundle): JSONObject {
        return JSONObject().apply {
            put("releaseName", bundle.releaseName)
            put("tagName", bundle.tagName)
            put("htmlUrl", bundle.htmlUrl)
            put("releaseUpdatedAtMillis", bundle.releaseUpdatedAtMillis ?: 0L)
            put("showingAllAssets", bundle.showingAllAssets)
            put("shortCommitSha", bundle.shortCommitSha)
            put(
                "assets",
                JSONArray().apply {
                    bundle.assets.forEach { asset ->
                        put(
                            JSONObject().apply {
                                put("name", asset.name)
                                put("downloadUrl", asset.downloadUrl)
                                put("apiAssetUrl", asset.apiAssetUrl)
                                put("sizeBytes", asset.sizeBytes)
                                put("downloadCount", asset.downloadCount)
                                put("contentType", asset.contentType)
                                put("updatedAtMillis", asset.updatedAtMillis ?: 0L)
                            }
                        )
                    }
                }
            )
        }
    }

    private fun decodeBundle(obj: JSONObject?): GitHubReleaseAssetBundle? {
        obj ?: return null
        val tagName = obj.optString("tagName").trim()
        if (tagName.isBlank()) return null
        val assetsArray = obj.optJSONArray("assets") ?: JSONArray()
        val assets = buildList {
            for (index in 0 until assetsArray.length()) {
                val assetObj = assetsArray.optJSONObject(index) ?: continue
                val name = assetObj.optString("name").trim()
                val downloadUrl = assetObj.optString("downloadUrl").trim()
                if (name.isBlank() || downloadUrl.isBlank()) continue
                add(
                    GitHubReleaseAssetFile(
                        name = name,
                        downloadUrl = downloadUrl,
                        apiAssetUrl = assetObj.optString("apiAssetUrl").trim(),
                        sizeBytes = assetObj.optLong("sizeBytes", 0L),
                        downloadCount = assetObj.optInt("downloadCount", 0),
                        contentType = assetObj.optString("contentType").trim(),
                        updatedAtMillis = assetObj.optLong("updatedAtMillis", 0L)
                            .takeIf { it > 0L }
                    )
                )
            }
        }
        return GitHubReleaseAssetBundle(
            releaseName = obj.optString("releaseName").trim(),
            tagName = tagName,
            htmlUrl = obj.optString("htmlUrl").trim(),
            releaseUpdatedAtMillis = obj.optLong("releaseUpdatedAtMillis", 0L).takeIf { it > 0L },
            assets = assets,
            showingAllAssets = obj.optBoolean("showingAllAssets", false),
            shortCommitSha = obj.optString("shortCommitSha").trim()
        )
    }

    private fun sha1(text: String): String {
        val digest = MessageDigest.getInstance("SHA-1")
        return digest.digest(text.toByteArray())
            .joinToString("") { byte -> "%02x".format(byte) }
    }
}
