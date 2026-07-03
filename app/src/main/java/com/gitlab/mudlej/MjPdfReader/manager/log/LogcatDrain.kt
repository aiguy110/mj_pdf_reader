package com.gitlab.mudlej.MjPdfReader.manager.log

import android.os.Build
import android.os.Process
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Reads this process's own logcat on a background thread and feeds each line into [LogSpool],
 * so *all* existing `android.util.Log` / Timber output is captured with no call-site changes.
 *
 * Uses the `epoch` format (`<sec.millis> <pid> <tid> <LEVEL> <tag>: <msg>`). Continuation lines
 * (e.g. stack traces) are appended to the preceding entry. Only lines from our own PID are kept.
 */
class LogcatDrain(
    private val spool: LogSpool,
    private val sessionId: String,
    private val onNewLines: () -> Unit,
) {
    @Volatile private var running = false
    private var thread: Thread? = null

    fun start() {
        if (running) return
        running = true
        thread = Thread({ runLoop() }, "logcat-drain").apply { isDaemon = true; start() }
    }

    private fun runLoop() {
        val myPid = Process.myPid()
        val cmd = mutableListOf("logcat", "-v", "epoch", "-T", "500")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            cmd.add("--pid=$myPid") // pre-filter on-device; older APIs fall back to manual filtering
        }

        var buffered: LogEntry? = null
        var linesSinceFlush = 0

        fun flushSignal() {
            // Throttle: nudge the uploader roughly every batch of lines; WorkManager coalesces.
            if (linesSinceFlush >= NUDGE_EVERY) {
                linesSinceFlush = 0
                onNewLines()
            }
        }

        try {
            val process = ProcessBuilder(cmd).redirectErrorStream(false).start()
            BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                while (running) {
                    val line = reader.readLine() ?: break
                    val parsed = parse(line, myPid)
                    when {
                        parsed != null -> {
                            buffered?.let { spool.append(it); linesSinceFlush++; flushSignal() }
                            buffered = parsed
                        }
                        // Non-header line: a continuation of the current entry (stack trace, etc.).
                        buffered != null && line.isNotBlank() ->
                            buffered = buffered!!.copy(msg = buffered!!.msg + "\n" + line)
                    }
                }
            }
            buffered?.let { spool.append(it) }
        } catch (_: Exception) {
            // logcat unavailable / process died: give up quietly. Timber/Log still work normally.
        } finally {
            running = false
        }
    }

    /** Parses one epoch-format line; returns null for non-matching or foreign-PID lines. */
    private fun parse(line: String, myPid: Int): LogEntry? {
        val m = LINE_RE.matchEntire(line) ?: return null
        val (epoch, pid, level, rawTag, msg) = m.destructured
        if (pid.toIntOrNull() != myPid) return null
        val ts = (epoch.toDoubleOrNull()?.times(1000))?.toLong() ?: System.currentTimeMillis()
        val tag = rawTag.trim().ifEmpty { "-" }
        if (tag.startsWith(SELF_TAG_PREFIX)) return null // avoid feeding our own upload noise back in
        return LogEntry(ts = ts, level = levelName(level), tag = tag, msg = msg, sessionId = sessionId)
    }

    private fun levelName(c: String): String = when (c) {
        "V" -> "VERBOSE"; "D" -> "DEBUG"; "I" -> "INFO"
        "W" -> "WARN"; "E" -> "ERROR"; "F", "A" -> "ASSERT"
        else -> c
    }

    companion object {
        private const val NUDGE_EVERY = 25
        private const val SELF_TAG_PREFIX = "okhttp"
        // <epoch> <pid> <tid> <LEVEL> <tag>: <message>
        private val LINE_RE =
            Regex("""^\s*(\d+\.\d+)\s+(\d+)\s+\d+\s+([VDIWEFA])\s+(.*?):\s?(.*)$""")
    }
}
