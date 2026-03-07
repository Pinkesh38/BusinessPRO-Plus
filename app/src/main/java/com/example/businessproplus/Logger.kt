package com.example.businessproplus

import android.util.Log

object Logger {
    private const val TAG = "BusinessPRO_PLUS"

    fun d(message: String) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, message)
        }
    }

    fun e(message: String, throwable: Throwable? = null) {
        // We log errors even in production, but without sensitive details
        Log.e(TAG, message, throwable)
    }

    fun i(message: String) {
        if (BuildConfig.DEBUG) {
            Log.i(TAG, message)
        }
    }
}