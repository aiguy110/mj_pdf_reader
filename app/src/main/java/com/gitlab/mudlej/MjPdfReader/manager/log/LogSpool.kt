package com.gitlab.mudlej.MjPdfReader.manager.log

import android.content.Context
import com.google.gson.Gson
import java.io.File

/**
 * On-disk log storage with two responsibilities, kept as separate files:
 *
 *  - **backup.jsonl** (+ backup.1.jsonl): durable local copy of every captured line, rotated by
 *    size and never touched by the uploader. This is the "pull it over USB / adb" fallback.
 *  - **pending.jsonl -> uploading.jsonl**: the upload queue. New lines append to pending; the
 *    uploader atomically renames pending -> uploading, ships it, then deletes it on success.
 *    A leftover uploading.jsonl (crash / failed send) is retried before the next rotation, so no
 *    line is lost or double-sent.
 *
 * All file mutations are guarded by [lock] so the drain thread and the WorkManager thread can't
 * interleave a rename with an append.
 */
class LogSpool(context: Context) {

    private val dir = File(context.filesDir, "logs").apply { mkdirs() }
    private val backup = File(dir, "backup.jsonl")
    private val backupOld = File(dir, "backup.1.jsonl")
    private val pending = File(dir, "pending.jsonl")
    private val uploading = File(dir, "uploading.jsonl")
    private val lock = Any()
    private val gson = Gson()

    fun append(entry: LogEntry) {
        val line = gson.toJson(entry) + "\n"
        synchronized(lock) {
            pending.appendText(line)
            backup.appendText(line)
            if (backup.length() > MAX_BACKUP_BYTES) {
                backupOld.delete()
                backup.renameTo(backupOld)
            }
        }
    }

    /**
     * Returns the file to upload next, or null if there's nothing queued. Prefers an existing
     * [uploading] file (a previous attempt that didn't finish); otherwise promotes [pending].
     */
    fun nextBatchFile(): File? = synchronized(lock) {
        if (uploading.exists() && uploading.length() > 0) return uploading
        if (!pending.exists() || pending.length() == 0L) return null
        pending.renameTo(uploading)
        if (uploading.exists() && uploading.length() > 0) uploading else null
    }

    fun readEntries(file: File): List<LogEntry> = synchronized(lock) {
        if (!file.exists()) return emptyList()
        file.readLines().mapNotNull { line ->
            if (line.isBlank()) null
            else runCatching { gson.fromJson(line, LogEntry::class.java) }.getOrNull()
        }
    }

    fun markUploaded(file: File) = synchronized(lock) { file.delete() }

    fun hasPending(): Boolean = synchronized(lock) {
        (pending.exists() && pending.length() > 0) || (uploading.exists() && uploading.length() > 0)
    }

    companion object {
        private const val MAX_BACKUP_BYTES = 1L * 1024 * 1024 // 1 MiB per backup file, 2 kept
    }
}
