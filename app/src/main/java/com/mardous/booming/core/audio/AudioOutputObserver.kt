/*
 * Copyright (c) 2024 Christians Martínez Alvarado
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

package com.mardous.booming.core.audio

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.util.Log
import androidx.core.content.getSystemService
import androidx.mediarouter.media.MediaControlIntent
import androidx.mediarouter.media.MediaRouter
import com.mardous.booming.core.model.audiodevice.AudioDevice
import com.mardous.booming.core.model.audiodevice.AudioDeviceType
import com.mardous.booming.core.model.audiodevice.getDeviceType
import com.mardous.booming.core.model.audiodevice.getMediaRouteType
import com.mardous.booming.util.oem.SystemMediaControlResolver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class AudioOutputObserver(context: Context) {

    private val _audioDevice = MutableStateFlow(AudioDevice.UnknownDevice)
    val audioDevice = _audioDevice.asStateFlow()

    private var mediaRouter = MediaRouter.getInstance(context)
    var audioManager = context.getSystemService<AudioManager>()
        private set

    private var isObserving = false

    init {
        requestAudioDevice()
    }

    fun startObserver() {
        if (!isObserving) try {
            audioManager?.registerAudioDeviceCallback(audioDeviceCallback, null)
            this.isObserving = true
        } catch (e: Throwable) {
            Log.e(TAG, "Unable to start audio output observer", e)
        }
    }

    fun stopObserver() {
        if (isObserving) try {
            audioManager?.unregisterAudioDeviceCallback(audioDeviceCallback)
            this.isObserving = false
        } catch (e: Throwable) {
            Log.e(TAG, "Unable to stop audio output observer", e)
        }
    }

    fun showOutputDeviceSelector(context: Context) {
        SystemMediaControlResolver.openMediaOutputSwitcher(context)
    }

    private fun getCurrentAudioDevice(): AudioDevice {
        var audioDevice: AudioDevice? = null
        val route = mediaRouter.selectedRoute
        val isConnected = route.connectionState == MediaRouter.RouteInfo.CONNECTION_STATE_CONNECTED
        if (isConnected && route.isEnabled && route.supportsControlCategory(MediaControlIntent.CATEGORY_LIVE_AUDIO)) {
            audioDevice = AudioDevice(
                type = route.getMediaRouteType(),
                productName = route.name
            )
        }
        return audioDevice ?: audioManager?.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            ?.minByOrNull { info ->
                AudioDeviceType.entries.indexOf(info.getDeviceType())
            }
            ?.let { chosen ->
                AudioDevice(
                    type = chosen.getDeviceType(),
                    productName = chosen.productName.toString()
                )
            } ?: AudioDevice.UnknownDevice
    }

    private fun requestAudioDevice() {
        _audioDevice.value = getCurrentAudioDevice()
    }

    private val audioDeviceCallback: AudioDeviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
            requestAudioDevice()
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
            requestAudioDevice()
        }
    }

    companion object {
        private const val TAG = "AudioOutputObserver"
    }
}