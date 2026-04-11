package com.example.keios.ui.page.main

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.keios.ui.page.main.widget.FrostedBlock
import com.example.keios.ui.page.main.widget.MiuixInfoItem
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

private const val BA_GUIDE_KV_ID = "ba_student_guide"
private const val BA_GUIDE_KEY_CURRENT_URL = "current_url"
private const val BA_GUIDE_KEY_CACHE_PREFIX = "cache_"
private const val BA_GUIDE_CONNECT_TIMEOUT_MS = 12_000
private const val BA_GUIDE_READ_TIMEOUT_MS = 12_000

data class BaStudentGuideInfo(
    val sourceUrl: String,
    val title: String,
    val subtitle: String,
    val description: String,
    val imageUrl: String,
    val summary: String,
    val stats: List<Pair<String, String>>,
    val syncedAtMs: Long
)

object BaStudentGuideStore {
    private fun kv(): MMKV = MMKV.mmkvWithID(BA_GUIDE_KV_ID)

    fun setCurrentUrl(url: String) {
        kv().encode(BA_GUIDE_KEY_CURRENT_URL, url.trim())
    }

    fun loadCurrentUrl(): String = kv().decodeString(BA_GUIDE_KEY_CURRENT_URL, "").orEmpty()

    private fun cacheKey(url: String): String {
        val id = url.trim().hashCode().toUInt().toString(16)
        return BA_GUIDE_KEY_CACHE_PREFIX + id
    }

    fun saveInfo(info: BaStudentGuideInfo) {
        val statsArr = JSONArray().apply {
            info.stats.forEach { (k, v) ->
                put(JSONObject().apply {
                    put("k", k)
                    put("v", v)
                })
            }
        }
        val raw = JSONObject().apply {
            put("sourceUrl", info.sourceUrl)
            put("title", info.title)
            put("subtitle", info.subtitle)
            put("description", info.description)
            put("imageUrl", info.imageUrl)
            put("summary", info.summary)
            put("syncedAtMs", info.syncedAtMs)
            put("stats", statsArr)
        }.toString()
        kv().encode(cacheKey(info.sourceUrl), raw)
    }

    fun loadInfo(url: String): BaStudentGuideInfo? {
        val source = url.trim()
        if (source.isBlank()) return null
        val raw = kv().decodeString(cacheKey(source), "").orEmpty()
        if (raw.isBlank()) return null
        return runCatching {
            val obj = JSONObject(raw)
            val stats = mutableListOf<Pair<String, String>>()
            val arr = obj.optJSONArray("stats") ?: JSONArray()
            for (i in 0 until arr.length()) {
                val row = arr.optJSONObject(i) ?: continue
                val k = row.optString("k").trim()
                val v = row.optString("v").trim()
                if (k.isNotBlank() && v.isNotBlank()) stats += k to v
            }
            BaStudentGuideInfo(
                sourceUrl = obj.optString("sourceUrl").ifBlank { source },
                title = obj.optString("title"),
                subtitle = obj.optString("subtitle"),
                description = obj.optString("description"),
                imageUrl = obj.optString("imageUrl"),
                summary = obj.optString("summary"),
                stats = stats,
                syncedAtMs = obj.optLong("syncedAtMs", 0L)
            )
        }.getOrNull()
    }
}

private fun normalizeGuideUrl(raw: String): String {
    val value = raw.trim()
    if (value.isBlank()) return ""
    return when {
        value.startsWith("http://") || value.startsWith("https://") -> value
        value.startsWith("//") -> "https:$value"
        value.startsWith("/") -> "https://www.gamekee.com$value"
        else -> "https://www.gamekee.com/$value"
    }
}

private fun decodeBasicHtmlEntity(raw: String): String {
    return raw
        .replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
}

private fun stripHtml(raw: String): String {
    return decodeBasicHtmlEntity(raw.replace(Regex("<[^>]+>"), " "))
        .replace(Regex("\\s+"), " ")
        .trim()
}

private fun extractMeta(html: String, key: String): String {
    val escaped = Regex.escape(key)
    val regexes = listOf(
        Regex(
            """<meta[^>]+(?:property|name)\s*=\s*["']$escaped["'][^>]*content\s*=\s*["']([^"']+)["'][^>]*>""",
            setOf(RegexOption.IGNORE_CASE)
        ),
        Regex(
            """<meta[^>]*content\s*=\s*["']([^"']+)["'][^>]+(?:property|name)\s*=\s*["']$escaped["'][^>]*>""",
            setOf(RegexOption.IGNORE_CASE)
        )
    )
    regexes.forEach { regex ->
        val value = regex.find(html)?.groupValues?.getOrNull(1).orEmpty().trim()
        if (value.isNotBlank()) return decodeBasicHtmlEntity(value)
    }
    return ""
}

private fun normalizeImageUrl(sourceUrl: String, imageRaw: String): String {
    val img = imageRaw.trim()
    if (img.isBlank()) return ""
    if (img.startsWith("http://") || img.startsWith("https://")) return img
    if (img.startsWith("//")) return "https:$img"
    return if (img.startsWith("/")) {
        val source = runCatching { Uri.parse(sourceUrl) }.getOrNull()
        val host = source?.scheme?.plus("://")?.plus(source.host.orEmpty()).orEmpty()
        (host.ifBlank { "https://www.gamekee.com" }) + img
    } else {
        "https://www.gamekee.com/$img"
    }
}

private fun extractStatsFromHtml(html: String): List<Pair<String, String>> {
    val rows = mutableListOf<Pair<String, String>>()
    val trRegex = Regex(
        """<tr[^>]*>\s*<th[^>]*>(.*?)</th>\s*<td[^>]*>(.*?)</td>\s*</tr>""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )
    trRegex.findAll(html).forEach { m ->
        val key = stripHtml(m.groupValues[1])
        val value = stripHtml(m.groupValues[2])
        if (key.isNotBlank() && value.isNotBlank()) rows += key to value
    }
    if (rows.isNotEmpty()) return rows.take(8)

    val dlRegex = Regex(
        """<dt[^>]*>(.*?)</dt>\s*<dd[^>]*>(.*?)</dd>""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )
    dlRegex.findAll(html).forEach { m ->
        val key = stripHtml(m.groupValues[1])
        val value = stripHtml(m.groupValues[2])
        if (key.isNotBlank() && value.isNotBlank()) rows += key to value
    }
    return rows.take(8)
}

private fun fetchGuideInfo(sourceUrl: String): BaStudentGuideInfo {
    val target = normalizeGuideUrl(sourceUrl)
    require(target.isNotBlank()) { "empty url" }
    val conn = (URL(target).openConnection() as HttpURLConnection).apply {
        requestMethod = "GET"
        connectTimeout = BA_GUIDE_CONNECT_TIMEOUT_MS
        readTimeout = BA_GUIDE_READ_TIMEOUT_MS
        setRequestProperty("Accept", "text/html,application/xhtml+xml")
        setRequestProperty("Accept-Language", "zh-CN")
        setRequestProperty("User-Agent", "Mozilla/5.0 (Android) KeiOS/1.0")
        setRequestProperty("Referer", "https://www.gamekee.com/ba/")
    }
    try {
        val code = conn.responseCode
        val html = (if (code in 200..299) conn.inputStream else conn.errorStream)
            ?.bufferedReader()
            ?.use { it.readText() }
            .orEmpty()
        if (code !in 200..299 || html.isBlank()) error("HTTP $code")

        val ogTitle = extractMeta(html, "og:title")
        val titleTag = Regex("<title[^>]*>(.*?)</title>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
            .find(html)
            ?.groupValues
            ?.getOrNull(1)
            .orEmpty()
        val rawTitle = if (ogTitle.isNotBlank()) ogTitle else stripHtml(titleTag)
        val title = rawTitle.ifBlank { "图鉴信息" }

        val siteName = extractMeta(html, "og:site_name").ifBlank { "GameKee" }
        val description = extractMeta(html, "og:description")
            .ifBlank { extractMeta(html, "description") }
            .ifBlank { "未解析到详细描述，可点击下方来源查看完整图鉴。" }
        val imageUrl = normalizeImageUrl(target, extractMeta(html, "og:image"))

        val paragraphRegex = Regex(
            "<p[^>]*>(.*?)</p>",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        )
        val summary = paragraphRegex.findAll(html)
            .map { stripHtml(it.groupValues[1]) }
            .firstOrNull { it.length >= 12 }
            ?: description

        val stats = extractStatsFromHtml(html)

        return BaStudentGuideInfo(
            sourceUrl = target,
            title = title,
            subtitle = siteName,
            description = description,
            imageUrl = imageUrl,
            summary = summary,
            stats = stats,
            syncedAtMs = System.currentTimeMillis()
        )
    } finally {
        conn.disconnect()
    }
}

@Composable
private fun GuideRemoteImage(
    imageUrl: String
) {
    val target = remember(imageUrl) { normalizeGuideUrl(imageUrl) }
    if (target.isBlank()) return
    val bitmap by produceState<Bitmap?>(initialValue = null, target) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val conn = (URL(target).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = BA_GUIDE_CONNECT_TIMEOUT_MS
                    readTimeout = BA_GUIDE_READ_TIMEOUT_MS
                    setRequestProperty("Accept", "image/*,*/*")
                    setRequestProperty("User-Agent", "Mozilla/5.0 (Android) KeiOS/1.0")
                    setRequestProperty("Referer", "https://www.gamekee.com/")
                }
                try {
                    val code = conn.responseCode
                    if (code !in 200..299) return@runCatching null
                    conn.inputStream.use { BitmapFactory.decodeStream(it) }
                } finally {
                    conn.disconnect()
                }
            }.getOrNull()
        }
    }
    val rendered = bitmap ?: return
    Image(
        bitmap = rendered.asImageBitmap(),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
    )
}

@Composable
fun BaStudentGuidePage(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val accent = MiuixTheme.colorScheme.primary
    val surfaceColor = MiuixTheme.colorScheme.surface
    val backdrop: LayerBackdrop = rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }
    val scrollBehavior = MiuixScrollBehavior()

    val sourceUrl = remember { BaStudentGuideStore.loadCurrentUrl() }
    var info by remember(sourceUrl) { mutableStateOf(BaStudentGuideStore.loadInfo(sourceUrl)) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var refreshSignal by remember { mutableStateOf(0) }

    fun openExternal(url: String) {
        val target = normalizeGuideUrl(url)
        if (target.isBlank()) return
        runCatching {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(target)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }.onFailure {
            Toast.makeText(context, "无法打开链接", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(sourceUrl, refreshSignal) {
        if (sourceUrl.isBlank()) return@LaunchedEffect
        val cached = BaStudentGuideStore.loadInfo(sourceUrl)
        if (cached != null) info = cached
        loading = true
        val result = withContext(Dispatchers.IO) {
            runCatching { fetchGuideInfo(sourceUrl) }
        }
        result.onSuccess { latest ->
            info = latest
            error = null
            withContext(Dispatchers.IO) { BaStudentGuideStore.saveInfo(latest) }
        }.onFailure {
            error = if (info != null) "网络请求失败，已显示本地缓存" else "图鉴信息加载失败"
        }
        loading = false
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = "BA 图鉴",
                scrollBehavior = scrollBehavior,
                color = MiuixTheme.colorScheme.surface,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = MiuixIcons.Regular.Back,
                            contentDescription = null,
                            tint = MiuixTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            refreshSignal += 1
                        }
                    ) {
                        Icon(
                            imageVector = MiuixIcons.Regular.Refresh,
                            contentDescription = "刷新",
                            tint = MiuixTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding(),
                bottom = innerPadding.calculateBottomPadding() + 16.dp,
                start = 16.dp,
                end = 16.dp
            )
        ) {
            item { SmallTitle("学生图鉴") }
            item { Spacer(modifier = Modifier.height(14.dp)) }

            if (sourceUrl.isBlank()) {
                item {
                    FrostedBlock(
                        backdrop = backdrop,
                        title = "未选择学生",
                        subtitle = "请从 BA 卡池信息中点击对应卡池进入",
                        accent = accent
                    )
                }
            } else {
                item {
                    FrostedBlock(
                        backdrop = backdrop,
                        title = info?.title?.ifBlank { "图鉴信息" } ?: "图鉴信息",
                        subtitle = info?.subtitle?.ifBlank { "GameKee" } ?: "GameKee",
                        accent = accent,
                        content = {
                            if (showLoadingText(loading = loading, hasInfo = info != null)) {
                                Text("同步中...", color = MiuixTheme.colorScheme.onBackgroundVariant)
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            error?.takeIf { it.isNotBlank() }?.let {
                                Text(
                                    text = it,
                                    color = MiuixTheme.colorScheme.error,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            val guide = info
                            if (guide != null) {
                                if (guide.imageUrl.isNotBlank()) {
                                    GuideRemoteImage(guide.imageUrl)
                                    Spacer(modifier = Modifier.height(10.dp))
                                }
                                Text(
                                    text = guide.summary.ifBlank { guide.description },
                                    color = MiuixTheme.colorScheme.onBackground
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                guide.stats.forEach { (k, v) ->
                                    MiuixInfoItem(k, v)
                                    Spacer(modifier = Modifier.height(4.dp))
                                }
                                MiuixInfoItem(
                                    "来源",
                                    guide.sourceUrl,
                                    onClick = { openExternal(guide.sourceUrl) }
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}

private fun showLoadingText(loading: Boolean, hasInfo: Boolean): Boolean {
    return loading && hasInfo
}

