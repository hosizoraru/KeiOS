package os.kei.appwidget.github

import android.content.Context
import android.util.AtomicFile
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.RandomAccessFile
import java.util.UUID

internal class GitHubWidgetStateStore(private val directory: File) {
    constructor(context: Context) : this(File(context.filesDir, "desktop_widgets"))

    fun readSnapshot(): GitHubWidgetSnapshot = locked {
        val json = readJson("github_snapshot.json") ?: return@locked GitHubWidgetSnapshot()
        if (json.optInt("schema") != 1) return@locked GitHubWidgetSnapshot()
        GitHubWidgetSnapshot(
            available = json.optBoolean("available"),
            trackedCount = json.optInt("tracked").coerceAtLeast(0),
            updateCount = json.optInt("updates").coerceAtLeast(0),
            uncheckedCount = json.optInt("unchecked").coerceAtLeast(0),
            failedCount = json.optInt("failed").coerceAtLeast(0),
            ignoredCount = json.optInt("ignored").coerceAtLeast(0),
            lastCheckedAtMillis = json.optLong("checkedAt").coerceAtLeast(0L),
            pendingUpdates = json.optJSONArray("pending")?.let { pending ->
                (0 until minOf(pending.length(), 6)).mapNotNull { index ->
                    pending.optJSONObject(index)?.let {
                        GitHubWidgetPendingUpdate(
                            it.optString("name").filterNot(Char::isISOControl).take(80),
                            it.optString("version").filterNot(Char::isISOControl).take(48),
                            it.optBoolean("preRelease"),
                        )
                    }
                }
            } ?: json.optJSONArray("names")?.let { names ->
                (0 until minOf(names.length(), 6)).map {
                    GitHubWidgetPendingUpdate(names.optString(it).filterNot(Char::isISOControl).take(80))
                }
            }.orEmpty(),
            projects = json.optJSONArray("projects")?.let { rows ->
                (0 until minOf(rows.length(), 6)).mapNotNull { index ->
                    rows.optJSONObject(index)?.let { row ->
                        GitHubWidgetProject(
                            name = row.optString("name").filterNot(Char::isISOControl).take(80),
                            status = GitHubWidgetProjectStatus.fromStorageId(row.optString("status")),
                            version = row.optString("version").filterNot(Char::isISOControl).take(48),
                            preRelease = row.optBoolean("preRelease"),
                            localVersion = row.optString("localVersion").filterNot(Char::isISOControl).take(48),
                        )
                    }
                }
            }.orEmpty(),
        )
    }

    fun writeSnapshot(snapshot: GitHubWidgetSnapshot) = locked {
        writeJson(
            "github_snapshot.json",
            JSONObject()
                .put("schema", 1)
                .put("available", snapshot.available)
                .put("tracked", snapshot.trackedCount)
                .put("updates", snapshot.updateCount)
                .put("unchecked", snapshot.uncheckedCount)
                .put("failed", snapshot.failedCount)
                .put("ignored", snapshot.ignoredCount)
                .put("checkedAt", snapshot.lastCheckedAtMillis)
                .put("names", JSONArray(snapshot.pendingNames.take(6).map { it.take(80) }))
                .put("pending", JSONArray(snapshot.pendingUpdates.take(6).map {
                    JSONObject().put("name", it.name.filterNot(Char::isISOControl).take(80))
                        .put("version", it.version.filterNot(Char::isISOControl).take(48))
                        .put("preRelease", it.preRelease)
                }))
                .put("projects", JSONArray(snapshot.projects.take(6).map {
                    JSONObject().put("name", it.name.filterNot(Char::isISOControl).take(80))
                        .put("status", it.status.storageId)
                        .put("version", it.version.filterNot(Char::isISOControl).take(48))
                        .put("preRelease", it.preRelease)
                        .put("localVersion", it.localVersion.filterNot(Char::isISOControl).take(48))
                })),
        )
    }

    fun configuration(widgetId: Int): GitHubWidgetConfiguration = locked {
        GitHubWidgetConfiguration(
            showProjectNames = readJson("github_instances.json")
                ?.optJSONObject(widgetId.toString())?.optBoolean("showNames") ?: false,
        )
    }

    fun configure(widgetId: Int, configuration: GitHubWidgetConfiguration) = locked {
        require(widgetId > 0)
        val json = readJson("github_instances.json") ?: JSONObject()
        val config = json.optJSONObject(widgetId.toString()) ?: JSONObject()
        json.put(widgetId.toString(), config.put("showNames", configuration.showProjectNames))
        writeJson("github_instances.json", json)
    }

    fun delete(widgetIds: IntArray) = locked {
        val json = readJson("github_instances.json") ?: return@locked
        widgetIds.forEach { json.remove(it.toString()) }
        writeJson("github_instances.json", json)
    }

    fun restore(oldIds: IntArray, newIds: IntArray) = locked {
        val json = readJson("github_instances.json") ?: return@locked
        val moves = oldIds.zip(newIds.toList()).filter { (old, new) ->
            old > 0 && new > 0 &&
                json.optJSONObject(new.toString())?.optInt("restoredFrom", -1) != old
        }
        if (moves.isEmpty()) return@locked
        val saved = moves.map { (old, new) ->
            val source = json.optJSONObject(old.toString()) ?: JSONObject().put("showNames", false)
            new to JSONObject(source.toString()).put("restoredFrom", old)
        }
        moves.forEach { (old, _) -> json.remove(old.toString()) }
        saved.forEach { (new, config) ->
            json.put(new.toString(), config)
        }
        writeJson("github_instances.json", json)
    }

    fun beginPinRequest(
        configuration: GitHubWidgetConfiguration,
        existingIds: IntArray,
        nowMillis: Long = System.currentTimeMillis(),
    ): String = locked {
        require(existingIds.size <= 1024)
        val json = readJson("github_instances.json") ?: JSONObject()
        val requests = json.optJSONObject("pinRequests") ?: JSONObject()
        requests.keys().asSequence().toList().forEach { token ->
            val created = requests.optJSONObject(token)?.optLong("createdAt") ?: 0L
            if (nowMillis - created !in 0..PIN_LIFETIME_MILLIS) requests.remove(token)
        }
        while (requests.length() >= 8) {
            val oldest = requests.keys().asSequence().minBy {
                requests.getJSONObject(it).optLong("createdAt")
            }
            requests.remove(oldest)
        }
        val token = UUID.randomUUID().toString()
        requests.put(
            token,
            JSONObject().put("createdAt", nowMillis)
                .put("showNames", configuration.showProjectNames)
                .put("existingIds", JSONArray(existingIds.toList())),
        )
        json.put("pinRequests", requests)
        writeJson("github_instances.json", json)
        token
    }

    fun completePinRequest(
        token: String,
        widgetId: Int,
        nowMillis: Long = System.currentTimeMillis(),
    ): Boolean = locked {
        val json = readJson("github_instances.json") ?: return@locked false
        val request = json.optJSONObject("pinRequests")?.optJSONObject(token) ?: return@locked false
        if (widgetId <= 0 || request.optInt("completedId") > 0 ||
            nowMillis - request.optLong("createdAt") !in 0..PIN_LIFETIME_MILLIS) return@locked false
        val existing = request.optJSONArray("existingIds") ?: return@locked false
        if ((0 until existing.length()).any { existing.optInt(it) == widgetId }) return@locked false
        json.put(widgetId.toString(), JSONObject().put("showNames", request.optBoolean("showNames")))
        request.put("completedId", widgetId)
        writeJson("github_instances.json", json)
        true
    }

    fun pinResult(token: String): Int? = locked {
        readJson("github_instances.json")?.optJSONObject("pinRequests")?.optJSONObject(token)
            ?.optInt("completedId")?.takeIf { it > 0 }
    }

    fun abandonPinRequest(token: String) = locked {
        val json = readJson("github_instances.json") ?: return@locked
        json.optJSONObject("pinRequests")?.remove(token)
        writeJson("github_instances.json", json)
    }

    private fun readJson(name: String): JSONObject? {
        return try {
            val bytes = AtomicFile(File(directory, name)).openRead().use { it.readNBytes(MAX_BYTES + 1) }
            if (bytes.size > MAX_BYTES) return null
            JSONObject(bytes.toString(Charsets.UTF_8))
        } catch (_: FileNotFoundException) {
            null
        } catch (_: JSONException) {
            null
        }
    }

    private fun writeJson(name: String, json: JSONObject) {
        val bytes = json.toString().toByteArray(Charsets.UTF_8)
        check(bytes.size <= MAX_BYTES)
        val file = AtomicFile(File(directory, name))
        val stream = file.startWrite()
        try {
            stream.write(bytes)
            file.finishWrite(stream)
        } catch (error: Exception) {
            file.failWrite(stream)
            throw error
        }
    }

    // AtomicFile recovery also mutates files. Readers and writers must share the OS lock.
    private fun <T> locked(block: () -> T): T = synchronized(processLock) {
        if (!directory.isDirectory && !directory.mkdirs()) {
            throw IOException("Cannot create widget storage")
        }
        RandomAccessFile(File(directory, "github.lock"), "rw").use { file ->
            file.channel.lock().use { block() }
        }
    }

    private companion object {
        const val MAX_BYTES = 64 * 1024
        const val PIN_LIFETIME_MILLIS = 15 * 60 * 1000L
        val processLock = Any()
    }
}
