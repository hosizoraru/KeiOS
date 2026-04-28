package os.kei.ui.page.main.student

import android.content.Context
import androidx.media3.exoplayer.ExoPlayer
import java.util.concurrent.ConcurrentHashMap

internal object GuideBgmLoopStore {
    private val loopByScopedAudio = ConcurrentHashMap<String, Boolean>()

    private fun scopedKey(scopeKey: String, audioUrl: String): String {
        if (scopeKey.isBlank() || audioUrl.isBlank()) return ""
        return "$scopeKey|$audioUrl"
    }

    fun isEnabled(scopeKey: String, audioUrl: String): Boolean {
        val key = scopedKey(scopeKey, audioUrl)
        if (key.isBlank()) return false
        return loopByScopedAudio[key] == true
    }

    fun setEnabled(scopeKey: String, audioUrl: String, enabled: Boolean) {
        val key = scopedKey(scopeKey, audioUrl)
        if (key.isBlank()) return
        if (enabled) {
            loopByScopedAudio[key] = true
        } else {
            loopByScopedAudio.remove(key)
        }
    }

    fun clearScope(scopeKey: String) {
        if (scopeKey.isBlank()) return
        val prefix = "$scopeKey|"
        loopByScopedAudio.keys.forEach { key ->
            if (key.startsWith(prefix)) {
                loopByScopedAudio.remove(key)
            }
        }
    }
}

internal fun clearGuideBgmLoopScope(scopeKey: String) {
    GuideBgmLoopStore.clearScope(scopeKey)
}

internal object GuideBgmPlayerStore {
    private val playerByScopedAudio = ConcurrentHashMap<String, ExoPlayer>()

    private fun scopedKey(scopeKey: String, audioUrl: String): String {
        if (scopeKey.isBlank() || audioUrl.isBlank()) return ""
        return "$scopeKey|$audioUrl"
    }

    fun getOrCreate(context: Context, scopeKey: String, audioUrl: String): ExoPlayer? {
        val key = scopedKey(scopeKey, audioUrl)
        if (key.isBlank()) return null
        return playerByScopedAudio[key] ?: synchronized(this) {
            playerByScopedAudio[key] ?: ExoPlayer.Builder(context)
                .setMediaSourceFactory(createGameKeeMediaSourceFactory(context))
                .build()
                .also { created ->
                    playerByScopedAudio[key] = created
                }
        }
    }

    fun getExisting(scopeKey: String, audioUrl: String): ExoPlayer? {
        val key = scopedKey(scopeKey, audioUrl)
        if (key.isBlank()) return null
        return playerByScopedAudio[key]
    }

    fun pauseScopeExcept(scopeKey: String, audioUrl: String) {
        if (scopeKey.isBlank()) return
        val keepKey = scopedKey(scopeKey, audioUrl)
        val prefix = "$scopeKey|"
        playerByScopedAudio.forEach { (key, player) ->
            if (key.startsWith(prefix) && key != keepKey) {
                runCatching { player.pause() }
            }
        }
    }

    fun pauseScope(scopeKey: String) {
        if (scopeKey.isBlank()) return
        val prefix = "$scopeKey|"
        playerByScopedAudio.forEach { (key, player) ->
            if (key.startsWith(prefix)) {
                runCatching { player.pause() }
            }
        }
    }

    fun clearScope(scopeKey: String) {
        if (scopeKey.isBlank()) return
        val prefix = "$scopeKey|"
        val releaseKeys = playerByScopedAudio.keys.filter { key -> key.startsWith(prefix) }
        releaseKeys.forEach { key ->
            playerByScopedAudio.remove(key)?.let { player ->
                runCatching { player.stop() }
                runCatching { player.release() }
            }
        }
    }
}

internal fun clearGuideBgmPlaybackScope(scopeKey: String) {
    GuideBgmPlayerStore.clearScope(scopeKey)
}

internal fun pauseGuideBgmPlaybackScope(scopeKey: String) {
    GuideBgmPlayerStore.pauseScope(scopeKey)
}
