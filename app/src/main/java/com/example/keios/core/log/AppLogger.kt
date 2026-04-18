package com.example.keios.core.log

import android.util.Log
import com.example.keios.BuildConfig
import com.example.keios.KeiOSApp
import com.example.keios.core.prefs.UiPrefs
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AppLogger {
    private const val INTERNAL_TAG = "KeiLogger"
    private const val MAX_MESSAGE_LENGTH = 3200
    private const val MAX_STACK_LENGTH = 6000

    @Volatile
    private var debugEnabled: Boolean = BuildConfig.LOG_DEBUG_DEFAULT

    private val lineTimeFormatter = object : ThreadLocal<SimpleDateFormat>() {
        override fun initialValue(): SimpleDateFormat {
            return SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
        }
    }

    fun refreshEnabledFromPrefs() {
        debugEnabled = UiPrefs.isLogDebugEnabled(defaultValue = BuildConfig.LOG_DEBUG_DEFAULT)
    }

    fun setDebugEnabled(enabled: Boolean) {
        debugEnabled = enabled
    }

    fun isDebugEnabled(): Boolean = debugEnabled

    fun d(tag: String, message: String) {
        if (!debugEnabled) return
        Log.d(tag, message)
        append(level = "D", tag = tag, message = message)
    }

    fun i(tag: String, message: String) {
        if (!debugEnabled) return
        Log.i(tag, message)
        append(level = "I", tag = tag, message = message)
    }

    fun w(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable == null) {
            Log.w(tag, message)
        } else {
            Log.w(tag, message, throwable)
        }
        if (!debugEnabled) return
        append(level = "W", tag = tag, message = message, throwable = throwable)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable == null) {
            Log.e(tag, message)
        } else {
            Log.e(tag, message, throwable)
        }
        if (!debugEnabled) return
        append(level = "E", tag = tag, message = message, throwable = throwable)
    }

    private fun append(
        level: String,
        tag: String,
        message: String,
        throwable: Throwable? = null
    ) {
        val timestamp = lineTimeFormatter.get()?.format(Date()).orEmpty()
        val compactMessage = compact(message, MAX_MESSAGE_LENGTH)
        val throwablePart = throwable?.let {
            val stack = compact(Log.getStackTraceString(it), MAX_STACK_LENGTH)
            " | stack=$stack"
        }.orEmpty()
        val line = "$timestamp | $level | $tag | $compactMessage$throwablePart"
        runCatching {
            AppLogStore.appendLine(KeiOSApp.appContext, line)
        }.onFailure {
            Log.w(INTERNAL_TAG, "append failed: ${it.message ?: it.javaClass.simpleName}")
        }
    }

    private fun compact(raw: String, maxLength: Int): String {
        val normalized = raw
            .replace('\n', ' ')
            .replace('\r', ' ')
            .replace('\t', ' ')
            .trim()
        if (normalized.length <= maxLength) return normalized
        return normalized.take(maxLength) + "…"
    }
}
