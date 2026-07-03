package com.gitlab.mudlej.MjPdfReader.manager.log

import com.gitlab.mudlej.MjPdfReader.BuildConfig
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/** Result of a single batch POST, used by the worker to decide success / retry / drop. */
enum class UploadResult { SUCCESS, RETRY, PERMANENT_FAILURE }

/** Posts a [LogBatch] to the mj-logs ingest endpoint, mirroring the OkHttp+Gson pattern used by OpenRouterClient. */
class LogUploader {

    fun upload(deviceId: String, appVersion: String, entries: List<LogEntry>): UploadResult {
        if (entries.isEmpty()) return UploadResult.SUCCESS
        val token = BuildConfig.LOG_INGEST_TOKEN
        if (token.isBlank()) return UploadResult.PERMANENT_FAILURE // uploads disabled (no key configured)

        val body = gson.toJson(LogBatch(deviceId, appVersion, entries)).toRequestBody(JSON)
        val request = Request.Builder()
            .url(BuildConfig.LOG_INGEST_URL)
            .addHeader("Authorization", "Bearer $token")
            .post(body)
            .build()

        return try {
            httpClient.newCall(request).execute().use { resp ->
                when {
                    resp.isSuccessful -> UploadResult.SUCCESS
                    resp.code == 401 || resp.code == 403 -> UploadResult.PERMANENT_FAILURE
                    else -> UploadResult.RETRY // 5xx / 429 / transient
                }
            }
        } catch (e: Exception) {
            UploadResult.RETRY // network down: keep the batch, WorkManager retries with backoff
        }
    }

    companion object {
        private val JSON = "application/json".toMediaType()
        private val gson = Gson()
        private val httpClient = OkHttpClient.Builder()
            .callTimeout(30, TimeUnit.SECONDS)
            .build()
    }
}
