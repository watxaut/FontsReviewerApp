package com.watxaut.fontsreviewer.util

import android.util.Log
import com.watxaut.fontsreviewer.BuildConfig

/**
 * Secure logging utility that only logs in debug builds.
 * In production, all logs are stripped by ProGuard/R8.
 * 
 * Usage:
 *   SecureLog.d(TAG, "Debug message")
 *   SecureLog.e(TAG, "Error occurred", exception)
 */
object SecureLog {
    
    private val ENABLED = BuildConfig.DEBUG
    
    /**
     * Debug log - only in debug builds
     */
    fun d(tag: String, message: String) {
        if (ENABLED) {
            Log.d(tag, message)
        }
    }
    
    /**
     * Info log - only in debug builds
     */
    fun i(tag: String, message: String) {
        if (ENABLED) {
            Log.i(tag, message)
        }
    }
    
    /**
     * Warning log - only in debug builds
     */
    fun w(tag: String, message: String, throwable: Throwable? = null) {
        if (ENABLED) {
            if (throwable != null) {
                Log.w(tag, message, throwable)
            } else {
                Log.w(tag, message)
            }
        }
    }
    
    /**
     * Error log - only in debug builds
     */
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (ENABLED) {
            if (throwable != null) {
                Log.e(tag, message, throwable)
            } else {
                Log.e(tag, message)
            }
        }
    }
    
    /**
     * Verbose log - only in debug builds
     */
    fun v(tag: String, message: String) {
        if (ENABLED) {
            Log.v(tag, message)
        }
    }
    
    /**
     * Log sanitized information (removes PII)
     */
    fun sanitized(tag: String, message: String) {
        if (ENABLED) {
            val sanitized = sanitizeMessage(message)
            Log.d(tag, sanitized)
        }
    }
    
    /**
     * Sanitize message by removing potential PII
     */
    private fun sanitizeMessage(message: String): String {
        return message
            .replace(Regex("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b"), "[EMAIL]")
            .replace(Regex("\\b\\d{3}-\\d{3}-\\d{4}\\b"), "[PHONE]")
            .replace(Regex("password[\"']?\\s*[:=]\\s*[\"']?[^\\s,\"']+", RegexOption.IGNORE_CASE), "password=[REDACTED]")
    }
    
    /**
     * For production analytics events (non-PII only)
     * This should integrate with your analytics solution
     */
    fun analytics(event: String, params: Map<String, Any> = emptyMap()) {
        // TODO: Integrate with Firebase Analytics or your preferred solution
        // Make sure NO PII is logged to analytics
        if (ENABLED) {
            Log.i("Analytics", "Event: $event, Params: $params")
        }
    }
}
