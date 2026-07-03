package com.gitlab.mudlej.MjPdfReader.manager.log

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Drains the spool's upload queue one batch-file at a time. Runs under a CONNECTED network
 * constraint, so it only fires when there's connectivity and WorkManager retries it (with
 * exponential backoff) if a send fails — this is what makes logs survive periods offline.
 */
class LogUploadWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val spool = RemoteLogging.spool ?: return@withContext Result.success()
        val uploader = LogUploader()
        val device = RemoteLogging.deviceId(applicationContext)
        val version = RemoteLogging.appVersion

        // Ship every queued file. Cap iterations so a runaway queue can't loop forever in one run.
        repeat(MAX_BATCHES_PER_RUN) {
            val file = spool.nextBatchFile() ?: return@withContext Result.success()
            val entries = spool.readEntries(file)
            when (uploader.upload(device, version, entries)) {
                UploadResult.SUCCESS -> spool.markUploaded(file)
                UploadResult.RETRY -> return@withContext Result.retry()
                // Misconfigured token: drop this batch so we don't wedge the queue forever.
                UploadResult.PERMANENT_FAILURE -> spool.markUploaded(file)
            }
        }
        // More may remain; ask to be re-run.
        if (spool.hasPending()) Result.retry() else Result.success()
    }

    companion object {
        private const val MAX_BATCHES_PER_RUN = 20
    }
}
