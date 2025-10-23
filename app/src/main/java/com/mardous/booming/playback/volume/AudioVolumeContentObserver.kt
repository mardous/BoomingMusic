/*
 * Copyright (c) 2019 Hemanth Savarala.
 *
 * Licensed under the GNU General Public License v3
 *
 * This is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by
 *  the Free Software Foundation either version 3 of the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 */
package com.mardous.booming.playback.volume

import android.database.ContentObserver
import android.media.AudioManager
import android.net.Uri
import android.os.Handler

class AudioVolumeContentObserver internal constructor(
    handler: Handler,
    audioManager: AudioManager,
    audioStreamType: Int,
    listener: OnAudioVolumeChangedListener
) : ContentObserver(handler) {
    private val mListener: OnAudioVolumeChangedListener = listener
    private val mAudioManager: AudioManager = audioManager
    private val mAudioStreamType: Int = audioStreamType
    private var mLastVolume: Float

    /** Depending on the handler this method may be executed on the UI thread  */
    override fun onChange(selfChange: Boolean, uri: Uri?) {
        val maxVolume = mAudioManager.getStreamMaxVolume(mAudioStreamType)
        val currentVolume = mAudioManager.getStreamVolume(mAudioStreamType)
        if (currentVolume.toFloat() != mLastVolume) {
            mLastVolume = currentVolume.toFloat()
            mListener.onAudioVolumeChanged(currentVolume, maxVolume)
        }
    }

    init {
        mLastVolume = audioManager.getStreamVolume(mAudioStreamType).toFloat()
    }
}