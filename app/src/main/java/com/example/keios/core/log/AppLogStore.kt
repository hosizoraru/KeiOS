package com.example.keios.core.log

import android.content.Context
import android.net.Uri
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object AppLogStore {
    data class Stats(
        val totalBytes: Long,
        val fileCount: Int,
        val latestModifiedAtMs: Long
    ) {
        companion object {
            val Empty = Stats(
                totalBytes = 0L,
                fileCount = 0,
                latestModifiedAtMs = 0L
            )
        }
    }

    private data class LogBlob(
        val name: String,
        val bytes: ByteArray
    )

    private const val ROOT_DIR_NAME = "keios_logs"
    private const val ACTIVE_FILE_NAME = "keios-current.log"
    private const val ARCHIVE_FILE_PREFIX = "keios-"
    private const val ARCHIVE_FILE_SUFFIX = ".log"
    private const val MAX_ACTIVE_FILE_BYTES = 512L * 1024L
    private const val MAX_ARCHIVE_FILE_COUNT = 8
    private const val MAX_TOTAL_BYTES = 8L * 1024L * 1024L

    private val lock = Any()

    fun appendLine(context: Context, line: String) {
        val payload = (line + "\n").toByteArray(Charsets.UTF_8)
        if (payload.isEmpty()) return
        synchronized(lock) {
            val dir = ensureRootDirLocked(context)
            val active = File(dir, ACTIVE_FILE_NAME)
            if (active.exists() && active.length() + payload.size > MAX_ACTIVE_FILE_BYTES) {
                rotateActiveLocked(dir, active)
            }
            FileOutputStream(active, true).use { stream ->
                stream.write(payload)
            }
            trimLocked(dir)
        }
    }

    fun stats(context: Context): Stats {
        synchronized(lock) {
            val dir = ensureRootDirLocked(context)
            val files = listLogFilesLocked(dir)
            if (files.isEmpty()) return Stats.Empty
            return Stats(
                totalBytes = files.sumOf { it.length() },
                fileCount = files.size,
                latestModifiedAtMs = files.maxOfOrNull { it.lastModified() } ?: 0L
            )
        }
    }

    fun clear(context: Context) {
        synchronized(lock) {
            val dir = ensureRootDirLocked(context)
            listLogFilesLocked(dir).forEach { file ->
                runCatching { file.delete() }
            }
        }
    }

    fun exportZipToUri(context: Context, uri: Uri): Result<Unit> = runCatching {
        val blobs = synchronized(lock) {
            snapshotBlobsLocked(context)
        }
        context.contentResolver.openOutputStream(uri, "w").use { output ->
            checkNotNull(output) { "openOutputStream returned null" }
            ZipOutputStream(BufferedOutputStream(output)).use { zip ->
                if (blobs.isEmpty()) {
                    zip.putNextEntry(ZipEntry("README.txt"))
                    zip.write("No logs captured yet.\n".toByteArray(Charsets.UTF_8))
                    zip.closeEntry()
                } else {
                    blobs.forEach { blob ->
                        zip.putNextEntry(ZipEntry(blob.name))
                        zip.write(blob.bytes)
                        zip.closeEntry()
                    }
                }
            }
        }
    }

    private fun snapshotBlobsLocked(context: Context): List<LogBlob> {
        val dir = ensureRootDirLocked(context)
        return listLogFilesLocked(dir)
            .sortedBy { it.name }
            .mapNotNull { file ->
                runCatching {
                    LogBlob(
                        name = file.name,
                        bytes = file.readBytes()
                    )
                }.getOrNull()
            }
    }

    private fun ensureRootDirLocked(context: Context): File {
        return File(context.filesDir, ROOT_DIR_NAME).apply { mkdirs() }
    }

    private fun listLogFilesLocked(dir: File): List<File> {
        return dir.listFiles()
            ?.asSequence()
            ?.filter { it.isFile }
            ?.filter { file ->
                file.name == ACTIVE_FILE_NAME ||
                    (file.name.startsWith(ARCHIVE_FILE_PREFIX) && file.name.endsWith(ARCHIVE_FILE_SUFFIX))
            }
            ?.toList()
            .orEmpty()
    }

    private fun rotateActiveLocked(dir: File, active: File) {
        val archiveName = ARCHIVE_FILE_PREFIX + System.currentTimeMillis() + ARCHIVE_FILE_SUFFIX
        val archive = File(dir, archiveName)
        val renamed = runCatching { active.renameTo(archive) }.getOrDefault(false)
        if (renamed) return
        runCatching {
            active.copyTo(archive, overwrite = true)
            active.delete()
        }
    }

    private fun trimLocked(dir: File) {
        val allFiles = listLogFilesLocked(dir)
        val archives = allFiles
            .filter { it.name != ACTIVE_FILE_NAME }
            .sortedByDescending { it.lastModified() }
            .toMutableList()

        if (archives.size > MAX_ARCHIVE_FILE_COUNT) {
            archives.drop(MAX_ARCHIVE_FILE_COUNT).forEach { file ->
                runCatching { file.delete() }
            }
        }

        var merged = listLogFilesLocked(dir)
        var totalBytes = merged.sumOf { it.length() }
        if (totalBytes <= MAX_TOTAL_BYTES) return

        merged = merged
            .filter { it.name != ACTIVE_FILE_NAME }
            .sortedBy { it.lastModified() }
        merged.forEach { file ->
            if (totalBytes <= MAX_TOTAL_BYTES) return@forEach
            val len = file.length()
            if (runCatching { file.delete() }.getOrDefault(false)) {
                totalBytes -= len
            }
        }
    }
}
