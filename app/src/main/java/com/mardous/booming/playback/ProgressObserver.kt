/*
 * Copyright (c) 2026 Christians Martínez Alvarado
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.mardous.booming.playback

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
