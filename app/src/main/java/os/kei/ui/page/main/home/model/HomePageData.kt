package os.kei.ui.page.main.home.model

import java.util.concurrent.TimeUnit

internal fun formatGitHubCacheAgo(
    lastRefreshMs: Long,
    notRefreshedText: String,
    justNowText: String,
    nowMs: Long = System.currentTimeMillis()
): String {
    if (lastRefreshMs <= 0L) return notRefreshedText
    val deltaMs = (nowMs - lastRefreshMs).coerceAtLeast(0L)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(deltaMs)
    if (minutes <= 0L) return justNowText
    if (minutes < 60L) return "${minutes}m"
    val hours = minutes / 60L
    val remainMinutes = minutes % 60L
    return if (remainMinutes == 0L) "${hours}h" else "${hours}h ${remainMinutes}m"
}
