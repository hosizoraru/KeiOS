package os.kei.feature.github.data.local

import com.tencent.mmkv.MMKV
import org.json.JSONObject
import os.kei.feature.github.model.GitHubActionsDownloadRecord
import java.security.MessageDigest
import java.util.Locale

object GitHubActionsDownloadHistoryStore {
    private const val KV_ID = "github_actions_download_history"
    private const val KEY_INDEX = "entry_index"
    private const val MAX_RECORDS = 80

    private val store: MMKV by lazy { MMKV.mmkvWithID(KV_ID) }

    private fun kv(): MMKV = store

    fun recordDownload(record: GitHubActionsDownloadRecord) {
        val normalized = record.normalized()
        if (normalized.owner.isBlank() || normalized.repo.isBlank() || normalized.artifactName.isBlank()) {
            return
        }
        val kv = kv()
        val id = recordId(normalized)
        kv.encode(entryStoreKey(id), encodeRecord(normalized).toString())
        val index = loadIndex(kv)
        index.remove(id)
        index.add(id)
        trimIndex(index, kv)
        saveIndex(index, kv)
    }

    fun load(
        owner: String = "",
        repo: String = ""
    ): List<GitHubActionsDownloadRecord> {
        val normalizedOwner = owner.trim().lowercase(Locale.ROOT)
        val normalizedRepo = repo.trim().lowercase(Locale.ROOT)
        val kv = kv()
        return loadIndex(kv)
            .mapNotNull { id ->
                val raw = kv.decodeString(entryStoreKey(id)).orEmpty()
                decodeRecord(raw)?.takeIf { record ->
                    (normalizedOwner.isBlank() || record.owner.equals(normalizedOwner, ignoreCase = true)) &&
                        (normalizedRepo.isBlank() || record.repo.equals(normalizedRepo, ignoreCase = true))
                }
            }
            .sortedByDescending { it.downloadedAtMillis }
    }

    fun clear(owner: String = "", repo: String = "") {
        val normalizedOwner = owner.trim().lowercase(Locale.ROOT)
        val normalizedRepo = repo.trim().lowercase(Locale.ROOT)
        val kv = kv()
        if (normalizedOwner.isBlank() && normalizedRepo.isBlank()) {
            loadIndex(kv).forEach { id -> kv.removeValueForKey(entryStoreKey(id)) }
            kv.removeValueForKey(KEY_INDEX)
            kv.trim()
            return
        }
        val remaining = mutableSetOf<String>()
        loadIndex(kv).forEach { id ->
            val record = decodeRecord(kv.decodeString(entryStoreKey(id)).orEmpty())
            val matched = record != null &&
                (normalizedOwner.isBlank() || record.owner.equals(normalizedOwner, ignoreCase = true)) &&
                (normalizedRepo.isBlank() || record.repo.equals(normalizedRepo, ignoreCase = true))
            if (matched) {
                kv.removeValueForKey(entryStoreKey(id))
            } else {
                remaining += id
            }
        }
        saveIndex(remaining, kv)
    }

    fun cachedRecordCount(): Int = loadIndex().size

    internal fun encodeRecord(record: GitHubActionsDownloadRecord): JSONObject {
        return JSONObject()
            .put("owner", record.owner)
            .put("repo", record.repo)
            .put("workflowId", record.workflowId)
            .put("workflowName", record.workflowName)
            .put("workflowPath", record.workflowPath)
            .put("runId", record.runId)
            .put("runNumber", record.runNumber)
            .put("runAttempt", record.runAttempt)
            .put("runDisplayName", record.runDisplayName)
            .put("headBranch", record.headBranch)
            .put("headSha", record.headSha)
            .put("event", record.event)
            .put("status", record.status)
            .put("conclusion", record.conclusion)
            .put("artifactId", record.artifactId)
            .put("artifactName", record.artifactName)
            .put("artifactDigest", record.artifactDigest)
            .put("artifactSizeBytes", record.artifactSizeBytes)
            .put("sourceTrackId", record.sourceTrackId)
            .put("packageName", record.packageName)
            .put("downloadedAtMillis", record.downloadedAtMillis)
    }

    internal fun decodeRecord(raw: String): GitHubActionsDownloadRecord? {
        if (raw.isBlank()) return null
        return runCatching {
            val obj = JSONObject(raw)
            val owner = obj.optString("owner").trim()
            val repo = obj.optString("repo").trim()
            val artifactName = obj.optString("artifactName").trim()
            val downloadedAtMillis = obj.optLong("downloadedAtMillis", 0L)
            if (owner.isBlank() || repo.isBlank() || artifactName.isBlank() || downloadedAtMillis <= 0L) {
                return@runCatching null
            }
            GitHubActionsDownloadRecord(
                owner = owner,
                repo = repo,
                workflowId = obj.optLong("workflowId", 0L),
                workflowName = obj.optString("workflowName").trim(),
                workflowPath = obj.optString("workflowPath").trim(),
                runId = obj.optLong("runId", 0L),
                runNumber = obj.optLong("runNumber", 0L),
                runAttempt = obj.optInt("runAttempt", 0),
                runDisplayName = obj.optString("runDisplayName").trim(),
                headBranch = obj.optString("headBranch").trim(),
                headSha = obj.optString("headSha").trim(),
                event = obj.optString("event").trim(),
                status = obj.optString("status").trim(),
                conclusion = obj.optString("conclusion").trim(),
                artifactId = obj.optLong("artifactId", 0L),
                artifactName = artifactName,
                artifactDigest = obj.optString("artifactDigest").trim(),
                artifactSizeBytes = obj.optLong("artifactSizeBytes", 0L),
                sourceTrackId = obj.optString("sourceTrackId").trim(),
                packageName = obj.optString("packageName").trim(),
                downloadedAtMillis = downloadedAtMillis
            )
        }.getOrNull()
    }

    private fun GitHubActionsDownloadRecord.normalized(): GitHubActionsDownloadRecord {
        return copy(
            owner = owner.trim().lowercase(Locale.ROOT),
            repo = repo.trim().lowercase(Locale.ROOT),
            workflowName = workflowName.trim(),
            workflowPath = workflowPath.trim(),
            runDisplayName = runDisplayName.trim(),
            headBranch = headBranch.trim(),
            headSha = headSha.trim(),
            event = event.trim(),
            status = status.trim(),
            conclusion = conclusion.trim(),
            artifactName = artifactName.trim(),
            artifactDigest = artifactDigest.trim(),
            sourceTrackId = sourceTrackId.trim(),
            packageName = packageName.trim(),
            downloadedAtMillis = downloadedAtMillis.takeIf { it > 0L } ?: System.currentTimeMillis()
        )
    }

    private fun trimIndex(index: MutableSet<String>, kv: MMKV) {
        if (index.size <= MAX_RECORDS) return
        val sorted = index
            .mapNotNull { id ->
                val record = decodeRecord(kv.decodeString(entryStoreKey(id)).orEmpty())
                record?.let { id to it.downloadedAtMillis }
            }
            .sortedByDescending { it.second }
        val keep = sorted.take(MAX_RECORDS).map { it.first }.toSet()
        index.filter { it !in keep }.forEach { id ->
            kv.removeValueForKey(entryStoreKey(id))
        }
        index.retainAll(keep)
    }

    private fun recordId(record: GitHubActionsDownloadRecord): String {
        return sha1(
            listOf(
                record.owner,
                record.repo,
                record.workflowId.toString(),
                record.workflowPath,
                record.runId.toString(),
                record.artifactId.toString(),
                record.artifactName
            ).joinToString("|")
        )
    }

    private fun entryStoreKey(id: String): String = "entry_$id"

    private fun loadIndex(kv: MMKV = store): MutableSet<String> {
        val raw = kv.decodeString(KEY_INDEX).orEmpty()
        if (raw.isBlank()) return mutableSetOf()
        return raw.split(',')
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toMutableSet()
    }

    private fun saveIndex(index: Set<String>, kv: MMKV = store) {
        kv.encode(KEY_INDEX, index.filter { it.isNotBlank() }.joinToString(","))
    }

    private fun sha1(text: String): String {
        val digest = MessageDigest.getInstance("SHA-1")
        return digest.digest(text.toByteArray())
            .joinToString("") { byte -> "%02x".format(byte) }
    }
}
