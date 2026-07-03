package com.gitlab.mudlej.MjPdfReader.manager.log

import android.content.Context
import androidx.preference.PreferenceManager
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.gitlab.mudlej.MjPdfReader.BuildConfig
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Entry point for remote logging. Call [init] once from [com.gitlab.mudlej.MjPdfReader.App].
 *
 * Pipeline: LogcatDrain (captures this process's logcat) -> LogSpool (local backup + upload queue)
 * -> LogUploadWorker (batched, network-constrained, retrying POST to the mj-logs sink on outpost).
 */
object RemoteLogging {

    @Volatile var spool: LogSpool? = null
        private set

    /** Per-process-launch id, so a viewer can group a single run's lines. */
    val sessionId: String = UUID.randomUUID().toString()

    val appVersion: String = BuildConfig.VERSION_NAME

    private const val PREF_DEVICE_ID = "remote_log_device_id"
    private const val UNIQUE_UPLOAD = "mj-log-upload"
    private const val UNIQUE_PERIODIC = "mj-log-upload-periodic"

    fun init(context: Context) {
        val app = context.applicationContext
        val store = LogSpool(app)
        spool = store

        // Capture everything already going through android.util.Log / Timber.
        LogcatDrain(store, sessionId, onNewLines = { enqueueUpload(app) }).start()

        // Safety-net flush so queued lines ship even if the debounced one-shot was dropped.
        val periodic = PeriodicWorkRequestBuilder<LogUploadWorker>(30, TimeUnit.MINUTES)
            .setConstraints(networkConstraints())
            .build()
        WorkManager.getInstance(app)
            .enqueueUniquePeriodicWork(UNIQUE_PERIODIC, ExistingPeriodicWorkPolicy.KEEP, periodic)

        enqueueUpload(app)
    }

    /** Debounced one-shot upload. KEEP coalesces bursts into the already-scheduled run. */
    fun enqueueUpload(context: Context) {
        val work = OneTimeWorkRequestBuilder<LogUploadWorker>()
            .setConstraints(networkConstraints())
            .setInitialDelay(15, TimeUnit.SECONDS)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context.applicationContext)
            .enqueueUniqueWork(UNIQUE_UPLOAD, ExistingWorkPolicy.KEEP, work)
    }

    fun deviceId(context: Context): String {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
        prefs.getString(PREF_DEVICE_ID, null)?.let { return it }
        val id = "android-" + UUID.randomUUID().toString().take(8)
        prefs.edit().putString(PREF_DEVICE_ID, id).apply()
        return id
    }

    private fun networkConstraints() =
        Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
}
