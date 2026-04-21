package com.example.keios.ui.page.main.host.main

import android.net.Uri
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import com.example.keios.ui.page.main.about.util.openExternalUrl
import com.example.keios.ui.page.main.student.fetch.extractGuideContentIdFromUrl
import com.example.keios.ui.page.main.student.fetch.normalizeGuideUrl

private val mainScreenGuideDetailPathRegex = Regex("""^/ba/tj/\d+(?:\.html)?$""", RegexOption.IGNORE_CASE)

private fun isMainScreenGuideDetailLink(rawUrl: String): Boolean {
    val normalized = normalizeGuideUrl(rawUrl)
    if (normalized.isBlank()) return false
    val uri = runCatching { Uri.parse(normalized) }.getOrNull() ?: return false
    val host = uri.host?.lowercase().orEmpty()
    val hostAccepted = host == "www.gamekee.com" || host == "gamekee.com"
    if (!hostAccepted) return false
    return mainScreenGuideDetailPathRegex.matches(uri.path.orEmpty())
}

@Composable
internal fun rememberMainScreenOpenGuideDetailAction(
    poolGuideMissingText: String,
    externalOpenFailureText: String,
    onNavigateToCanonicalGuide: (String) -> Unit
): (String) -> Unit {
    val context = LocalContext.current
    val latestNavigate by rememberUpdatedState(onNavigateToCanonicalGuide)
    val latestMissingToastText by rememberUpdatedState(poolGuideMissingText)
    val latestExternalOpenFailureText by rememberUpdatedState(externalOpenFailureText)
    return remember(context) {
        { rawUrl ->
            val normalized = normalizeGuideUrl(rawUrl)
            if (normalized.isBlank()) {
                Toast.makeText(context, latestMissingToastText, Toast.LENGTH_SHORT).show()
            } else if (isMainScreenGuideDetailLink(normalized)) {
                val contentId = extractGuideContentIdFromUrl(normalized)
                if (contentId == null || contentId <= 0L) {
                    Toast.makeText(context, latestMissingToastText, Toast.LENGTH_SHORT).show()
                } else {
                    latestNavigate("https://www.gamekee.com/ba/tj/$contentId.html")
                }
            } else {
                if (!openExternalUrl(context, normalized)) {
                    Toast.makeText(context, latestExternalOpenFailureText, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
