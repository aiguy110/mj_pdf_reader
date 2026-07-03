package com.gitlab.mudlej.MjPdfReader.manager.log

import com.google.gson.annotations.SerializedName

/** One captured log line. Serialized as one JSON object per line (JSONL) in the spool files. */
data class LogEntry(
    @SerializedName("ts") val ts: Long,          // epoch millis
    @SerializedName("level") val level: String,  // VERBOSE/DEBUG/INFO/WARN/ERROR/ASSERT
    @SerializedName("tag") val tag: String,
    @SerializedName("msg") val msg: String,
    @SerializedName("session_id") val sessionId: String,
)

/** Batch POST body sent to the mj-logs ingest endpoint. */
data class LogBatch(
    @SerializedName("device_id") val deviceId: String,
    @SerializedName("app_version") val appVersion: String,
    @SerializedName("entries") val entries: List<LogEntry>,
)
