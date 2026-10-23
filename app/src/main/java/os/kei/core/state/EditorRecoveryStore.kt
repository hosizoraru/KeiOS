package os.kei.core.state

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import os.kei.core.json.encodeCompact
import os.kei.core.json.optLong
import os.kei.core.json.optString
import os.kei.core.json.parseJsonObjectOrNull
import os.kei.core.log.AppLogger
import os.kei.core.prefs.KeiMmkv

internal enum class EditorRecoverySlot {
    OsCard,
    GitHubTrack,
}

internal interface EditorRecoveryStorage {
    fun read(key: String): String?
    fun write(key: String, value: String?): Boolean
    fun sync()
}

/** Stores prepared editor snapshots, never callbacks into a retained page or Compose state. */
internal class EditorRecoveryStore(
    private val storage: EditorRecoveryStorage,
    private val nowMs: () -> Long = System::currentTimeMillis,
) {
    private val pending = mutableMapOf<EditorRecoverySlot, String?>()

    @Synchronized
    fun load(slot: EditorRecoverySlot): String? {
        val raw = if (pending.containsKey(slot)) pending[slot] else storage.read(slot.name)
        val record = raw?.takeIf { it.length <= MAX_RECORD_CHARS }?.parseJsonObjectOrNull() ?: return null
        val age = nowMs() - record.optLong("savedAt")
        if (record.optLong("version") != 1L || age !in 0..MAX_AGE_MS) return null
        return record.optString("payload").takeIf(String::isNotEmpty)
    }

    @Synchronized
    fun save(slot: EditorRecoverySlot, payload: String?): Boolean {
        val record = payload?.let {
            buildJsonObject {
                put("version", 1)
                put("savedAt", nowMs())
                put("payload", it)
            }.encodeCompact()
        }
        // Do not truncate commands, intents or URLs into a different, executable draft.
        val value = record?.takeIf { it.length <= MAX_RECORD_CHARS }
        pending[slot] = value
        return writePending(slot)
    }

    @Synchronized
    fun checkpoint() {
        var complete = true
        pending.keys.toList().forEach { slot ->
            if (!writePending(slot)) complete = false
        }
        storage.sync()
        check(complete) { "Some editor snapshots could not be written" }
    }

    private fun writePending(slot: EditorRecoverySlot): Boolean {
        val written = runCatching { storage.write(slot.name, pending[slot]) }
            .onFailure { AppLogger.w("EditorRecovery", "snapshot write failed: $slot", it) }
            .getOrDefault(false)
        if (written) pending.remove(slot)
        return written
    }

    companion object {
        internal const val MAX_RECORD_CHARS = 256 * 1024
        internal const val MAX_AGE_MS = 7 * 24 * 60 * 60 * 1000L

        private val sharedDelegate = lazy {
            val mmkv = KeiMmkv.byId("editor_recovery")
            EditorRecoveryStore(object : EditorRecoveryStorage {
                override fun read(key: String): String? = mmkv.decodeString(key)
                override fun write(key: String, value: String?): Boolean {
                    if (value == null) {
                        mmkv.removeValueForKey(key)
                        return true
                    }
                    return mmkv.encode(key, value)
                }
                override fun sync() = mmkv.sync()
            })
        }
        val shared: EditorRecoveryStore get() = sharedDelegate.value

        fun checkpointIfInitialized() {
            if (sharedDelegate.isInitialized()) sharedDelegate.value.checkpoint()
        }
    }
}
