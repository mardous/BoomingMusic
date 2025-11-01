package com.mardous.booming.data.local

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.provider.MediaStore

open class MediaStoreObserver(private val uiHandler: Handler, private val onChanged: () -> Unit) :
    ContentObserver(uiHandler), Runnable {

    override fun onChange(selfChange: Boolean) {
        // if a change is detected, remove any scheduled callback
        // then post a new one. This is intended to prevent closely
        // spaced events from generating multiple refresh calls
        uiHandler.removeCallbacks(this)
        uiHandler.postDelayed(this, REFRESH_DELAY)
    }

    override fun run() {
        onChanged()
    }

    fun init(context: Context) {
        context.contentResolver.registerContentObserver(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, true, this
        )
        context.contentResolver.registerContentObserver(
            MediaStore.Audio.Media.INTERNAL_CONTENT_URI, true, this
        )
    }

    fun stop(context: Context) {
        context.contentResolver.unregisterContentObserver(this)
    }

    companion object {
        // milliseconds to delay before calling refresh to aggregate events
        private const val REFRESH_DELAY: Long = 500
    }
}