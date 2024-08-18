package com.grytsyna.fixitpro.common

import android.util.Log

object LogWrapper {
    private const val TAG = "FIP_APP"

    fun d(message: String) {
        Log.d(TAG, message)
    }

    fun e(message: String) {
        Log.e(TAG, message)
    }

    fun i(message: String) {
        Log.i(TAG, message)
    }

    fun v(message: String) {
        Log.v(TAG, message)
    }

    fun w(message: String) {
        Log.w(TAG, message)
    }

    fun d(tag: String, message: String) {
        Log.d("$TAG: $tag", message)
    }

    fun e(tag: String, message: String) {
        Log.e("$TAG: $tag", message)
    }

    fun i(tag: String, message: String) {
        Log.i("$TAG: $tag", message)
    }

    fun v(tag: String, message: String) {
        Log.v("$TAG: $tag", message)
    }

    fun w(tag: String, message: String) {
        Log.w("$TAG: $tag", message)
    }

    fun e(tag: String, message: String, throwable: Throwable) {
        Log.e("$TAG: $tag", message, throwable)
    }
}
