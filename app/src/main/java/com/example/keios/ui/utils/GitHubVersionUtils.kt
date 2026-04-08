package com.example.keios.ui.utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import com.tencent.mmkv.MMKV
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.util.Locale

data class GitHubTrackedApp(
    val repoUrl: String,
    val owner: String,
    val repo: String,
    val packageName: String,
    val appLabel: String
) {
    val id: String
        get() = "$owner/$repo|$packageName"
}

data class InstalledAppItem(
    val label: String,
    val packageName: String
)

object GitHubTrackStore {
    private const val KV_ID = "github_track_store"
    private const val KEY_ITEMS = "tracked_items"

    private fun kv(): MMKV = MMKV.mmkvWithID(KV_ID)

    fun load(): List<GitHubTrackedApp> {
        val raw = kv().decodeString(KEY_ITEMS).orEmpty()
        if (raw.isBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    val repoUrl = obj.optString("repoUrl").trim()
                    val owner = obj.optString("owner").trim()
                    val repo = obj.optString("repo").trim()
                    val packageName = obj.optString("packageName").trim()
                    val appLabel = obj.optString("appLabel").trim()
                    if (repoUrl.isNotBlank() && owner.isNotBlank() && repo.isNotBlank() && packageName.isNotBlank()) {
                        add(
                            GitHubTrackedApp(
                                repoUrl = repoUrl,
                                owner = owner,
                                repo = repo,
                                packageName = packageName,
                                appLabel = appLabel.ifBlank { packageName }
                            )
                        )
                    }
                }
            }
        }.getOrDefault(emptyList())
    }

    fun save(items: List<GitHubTrackedApp>) {
        val array = JSONArray()
        items.forEach { item ->
            val obj = JSONObject()
                .put("repoUrl", item.repoUrl)
                .put("owner", item.owner)
                .put("repo", item.repo)
                .put("packageName", item.packageName)
                .put("appLabel", item.appLabel)
            array.put(obj)
        }
        kv().encode(KEY_ITEMS, array.toString())
    }
}

object GitHubVersionUtils {
    fun buildReleaseUrl(owner: String, repo: String): String {
        return "https://github.com/$owner/$repo/releases"
    }

    fun buildAppListPermissionIntent(context: Context): Intent? {
        val pm = context.packageManager
        val miuiIntent = Intent("miui.intent.action.APP_PERM_EDITOR").apply {
            putExtra("extra_pkgname", context.packageName)
        }
        if (miuiIntent.resolveActivity(pm) != null) return miuiIntent

        val detailIntent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            android.net.Uri.parse("package:${context.packageName}")
        )
        if (detailIntent.resolveActivity(pm) != null) return detailIntent
        return null
    }

    fun parseOwnerRepo(input: String): Pair<String, String>? {
        val normalized = input.trim().let { raw ->
            if (raw.startsWith("http://") || raw.startsWith("https://")) raw else "https://$raw"
        }
        return runCatching {
            val uri = URI(normalized)
            val host = uri.host?.lowercase(Locale.ROOT).orEmpty()
            if (!host.contains("github.com")) return null
            val segments = uri.path.orEmpty()
                .split("/")
                .filter { it.isNotBlank() }
            if (segments.size < 2) return null
            val owner = segments[0]
            val repo = segments[1].removeSuffix(".git")
            if (owner.isBlank() || repo.isBlank()) null else owner to repo
        }.getOrNull()
    }

    fun fetchLatestTag(owner: String, repo: String): Result<String> {
        val releaseUrl = "https://api.github.com/repos/$owner/$repo/releases/latest"
        return requestJsonObject(releaseUrl).fold(
            onSuccess = { obj ->
                val tag = obj.optString("tag_name").trim()
                if (tag.isNotBlank()) Result.success(tag) else Result.failure(IllegalStateException("release tag 为空"))
            },
            onFailure = {
                val tagsUrl = "https://api.github.com/repos/$owner/$repo/tags"
                requestJsonArray(tagsUrl).mapCatching { array ->
                    val latest = array.optJSONObject(0)?.optString("name").orEmpty().trim()
                    require(latest.isNotBlank()) { "tag 列表为空" }
                    latest
                }
            }
        )
    }

    fun compareVersion(local: String, remote: String): Int? {
        val localCore = extractNumericCore(local)
        val remoteCore = extractNumericCore(remote)
        if (localCore != null && remoteCore != null) {
            val coreCmp = compareNumericCore(localCore, remoteCore)
            if (coreCmp != 0) return coreCmp
            // Ignore build/commit suffix differences when numeric core is the same,
            // e.g. 2.0.3 and 2.0.3-634166078 are treated as the same version.
            return 0
        }

        val a = tokenizeVersion(local)
        val b = tokenizeVersion(remote)
        if (a.isEmpty() || b.isEmpty()) return null
        val max = maxOf(a.size, b.size)
        for (i in 0 until max) {
            val left = a.getOrNull(i)
            val right = b.getOrNull(i)
            if (left == right) continue
            if (left == null) return -1
            if (right == null) return 1
            val li = left.toLongOrNull()
            val ri = right.toLongOrNull()
            val cmp = when {
                li != null && ri != null -> li.compareTo(ri)
                else -> left.compareTo(right)
            }
            if (cmp != 0) return cmp
        }
        return 0
    }

    fun queryInstalledLaunchableApps(context: Context): List<InstalledAppItem> {
        val pm = context.packageManager
        val packages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.getInstalledPackages(0)
        }
        return packages
            .asSequence()
            .filter { info -> pm.getLaunchIntentForPackage(info.packageName) != null }
            .map { info ->
                val pkg = info.packageName
                val appInfo = info.applicationInfo
                val label = runCatching {
                    if (appInfo != null) pm.getApplicationLabel(appInfo).toString() else pkg
                }.getOrDefault(pkg)
                InstalledAppItem(label = label.ifBlank { pkg }, packageName = pkg)
            }
            .distinctBy { it.packageName }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
            .toList()
    }

    fun localVersionName(context: Context, packageName: String): String {
        val info = context.packageManager.getPackageInfoCompat(packageName)
        return info.versionName ?: "unknown"
    }

    private fun requestJsonObject(url: String): Result<JSONObject> {
        return runCatching {
            val (code, body) = request(url)
            if (code in 200..299) JSONObject(body) else error("HTTP $code")
        }
    }

    private fun requestJsonArray(url: String): Result<JSONArray> {
        return runCatching {
            val (code, body) = request(url)
            if (code in 200..299) JSONArray(body) else error("HTTP $code")
        }
    }

    private fun request(url: String): Pair<Int, String> {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 8000
            readTimeout = 8000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "KeiOS-App")
        }
        return try {
            val code = conn.responseCode
            val text = runCatching {
                val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            }.getOrDefault("")
            code to text
        } finally {
            conn.disconnect()
        }
    }

    private fun tokenizeVersion(raw: String): List<String> {
        return raw.lowercase(Locale.ROOT)
            .removePrefix("v")
            .split(Regex("[^0-9a-zA-Z]+"))
            .filter { it.isNotBlank() }
    }

    private fun extractNumericCore(raw: String): List<Long>? {
        val input = raw.lowercase(Locale.ROOT).removePrefix("v")
        val match = Regex("\\d+(?:\\.\\d+)+").find(input) ?: return null
        val numbers = match.value.split(".").mapNotNull { it.toLongOrNull() }
        if (numbers.isEmpty()) return null
        return trimTrailingZeros(numbers)
    }

    private fun compareNumericCore(a: List<Long>, b: List<Long>): Int {
        val max = maxOf(a.size, b.size)
        for (i in 0 until max) {
            val ai = a.getOrElse(i) { 0L }
            val bi = b.getOrElse(i) { 0L }
            if (ai != bi) return ai.compareTo(bi)
        }
        return 0
    }

    private fun trimTrailingZeros(value: List<Long>): List<Long> {
        var end = value.size
        while (end > 1 && value[end - 1] == 0L) end--
        return value.subList(0, end)
    }
}

private fun PackageManager.getPackageInfoCompat(packageName: String): PackageInfo {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
    } else {
        @Suppress("DEPRECATION")
        getPackageInfo(packageName, 0)
    }
}
