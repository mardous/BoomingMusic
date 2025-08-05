package com.mardous.booming.service.playback

import android.os.Handler
import android.os.Looper

private typealias ProgressCallback = () -> Unit

/**
 * A simple handler that runs continuously at a given interval.
 */
class ProgressObserver(private val intervalMs: Long = DEFAULT_INTERVAL) : Handler(Looper.getMainLooper()) {

    companion object {
        private const val DEFAULT_INTERVAL = 500L
    }

    private var callback: ProgressCallback? = null
    private var isStarted = false

    private val runnable = object : Runnable {
        override fun run() {
            callback?.invoke()
            postDelayed(this, intervalMs)
        }
    }

    fun start(callback: ProgressCallback) {
        if (isStarted) return
        isStarted = true
        this.callback = callback
        post(runnable)
    }

    fun stop() {
        isStarted = false
        this.callback = null
        removeCallbacks(runnable)
    }
}
