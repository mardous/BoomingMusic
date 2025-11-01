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

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.media.AudioManagerCompat.getStreamMaxVolume
import androidx.media.AudioManagerCompat.getStreamMinVolume
import androidx.mediarouter.media.MediaControlIntent
import androidx.mediarouter.media.MediaRouter
import com.mardous.booming.core.model.audiodevice.AudioDevice
import com.mardous.booming.core.model.audiodevice.AudioDeviceType
import com.mardous.booming.core.model.audiodevice.getDeviceType
import com.mardous.booming.core.model.audiodevice.getMediaRouteType
import com.mardous.booming.core.model.equalizer.VolumeState
import com.mardous.booming.util.oem.SystemMediaControlResolver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class AudioOutputObserver(private val context: Context) : BroadcastReceiver() {

    private val _volumeStateFlow = MutableStateFlow(VolumeState.Unspecified)
    val volumeStateFlow = _volumeStateFlow.asStateFlow()

    private val _audioDeviceFlow = MutableStateFlow(AudioDevice.UnknownDevice)
    val audioDeviceFlow = _audioDeviceFlow.asStateFlow()

    private var mediaRouter = MediaRouter.getInstance(context)
    var audioManager = context.getSystemService<AudioManager>()
        private set

    private var isObserving = false

    init {
        requestVolume()
        requestAudioDevice()
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action == VOLUME_CHANGED_ACTION || action == Intent.ACTION_HEADSET_PLUG) {
            requestVolume()
        }
    }

    fun startObserver() {
        if (!isObserving) try {
            val filter = IntentFilter().apply {
                addAction(VOLUME_CHANGED_ACTION)
                addAction(Intent.ACTION_HEADSET_PLUG)
            }
            ContextCompat.registerReceiver(context, this, filter, ContextCompat.RECEIVER_EXPORTED)
            audioManager?.registerAudioDeviceCallback(audioDeviceCallback, null)
            this.isObserving = true
        } catch (e: Throwable) {
            Log.e(TAG, "Unable to start audio output observer", e)
        }
    }

    fun stopObserver() {
        if (isObserving) try {
            context.unregisterReceiver(this)
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
                    productName = chosen.productName
                )
            } ?: AudioDevice.UnknownDevice
    }

    private fun requestVolume() {
        audioManager?.let {
            _volumeStateFlow.value = VolumeState(
                currentVolume = it.getStreamVolume(AudioManager.STREAM_MUSIC),
                maxVolume = getStreamMaxVolume(it, AudioManager.STREAM_MUSIC),
                minVolume = getStreamMinVolume(it, AudioManager.STREAM_MUSIC),
                isFixed = it.isVolumeFixed
            )
        }
    }

    private fun requestAudioDevice() {
        _audioDeviceFlow.value = getCurrentAudioDevice()
        _volumeStateFlow.value = volumeStateFlow.value.copy(
            isFixed = audioManager?.isVolumeFixed == true
        )
    }

    private val audioDeviceCallback: AudioDeviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
            requestAudioDevice()
            requestVolume()
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
            requestAudioDevice()
            requestVolume()
        }
    }

    companion object {
        private const val TAG = "AudioOutputObserver"
        private const val VOLUME_CHANGED_ACTION = "android.media.VOLUME_CHANGED_ACTION"
    }
}