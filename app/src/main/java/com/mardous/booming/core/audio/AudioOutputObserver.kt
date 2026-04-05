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
import android.hardware.usb.UsbManager
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.AudioMixerAttributes
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.media.AudioManagerCompat.getStreamMaxVolume
import androidx.media.AudioManagerCompat.getStreamMinVolume
import androidx.mediarouter.media.MediaControlIntent
import androidx.mediarouter.media.MediaRouter
import com.mardous.booming.core.model.audiodevice.AudioDevice
import com.mardous.booming.core.model.audiodevice.AudioDeviceType
import com.mardous.booming.core.model.audiodevice.BitPerfectState
import com.mardous.booming.core.model.audiodevice.getDeviceType
import com.mardous.booming.core.model.audiodevice.getMediaRouteType
import com.mardous.booming.core.model.equalizer.VolumeState
import com.mardous.booming.util.oem.SystemMediaControlResolver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class AudioOutputObserver(private val context: Context) : BroadcastReceiver() {

    private val _audioDevice = MutableStateFlow(AudioDevice.UnknownDevice)
    val audioDevice = _audioDevice.asStateFlow()

    private val _systemVolumeState = MutableStateFlow(VolumeState.Unspecified)
    val systemVolumeState = _systemVolumeState.asStateFlow()

    private val _bitPerfectState = MutableStateFlow<BitPerfectState>(BitPerfectState.Inactive(false))
    val bitPerfectState = _bitPerfectState.asStateFlow()

    private val _availableBitPerfectDevices = MutableStateFlow<List<AudioDeviceInfo>>(emptyList())
    val availableBitPerfectDevices = _availableBitPerfectDevices.asStateFlow()

    private var mediaRouter = MediaRouter.getInstance(context)
    var audioManager = context.getSystemService<AudioManager>()
        private set

    private var bitPerfectDevice: AudioDeviceInfo? = null
    private var isObserving = false
    
    private var userEnabledBitPerfect = false

    init {
        requestVolume()
        requestAudioDevice()
        scanForBitPerfectDevices()
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        when (action) {
            UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                scanForBitPerfectDevices()
                checkAndConfigureBitPerfect()
            }
            UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                scanForBitPerfectDevices()
                if (availableBitPerfectDevices.value.none { it == bitPerfectDevice }) {
                    disableBitPerfect()
                }
            }
            Intent.ACTION_HEADSET_PLUG -> {
                val state = intent.getIntExtra("state", -1)
                if (state == 1) {
                    checkAndConfigureBitPerfect()
                } else if (state == 0) {
                    disableBitPerfect()
                }
                requestVolume()
            }
            VOLUME_CHANGED_ACTION -> {
                requestVolume()
            }
        }
    }

    fun startObserver() {
        if (!isObserving) try {
            val filter = IntentFilter().apply {
                addAction(VOLUME_CHANGED_ACTION)
                addAction(Intent.ACTION_HEADSET_PLUG)
                addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
                addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
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

    fun setBitPerfectEnabled(enabled: Boolean) {
        if (userEnabledBitPerfect != enabled) {
            userEnabledBitPerfect = enabled
            if (enabled) {
                checkAndConfigureBitPerfect()
            } else {
                disableBitPerfect()
            }
        }
    }

    fun configureDeviceForBitPerfect(device: AudioDeviceInfo): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            Log.w(TAG, "Bit-perfect mode requires Android 14+")
            return false
        }

        if (!isDeviceUsbAudio(device)) {
            Log.w(TAG, "Device is not USB audio, bit-perfect not supported")
            return false
        }

        return configureBitPerfect(device)
    }

    /**
     * Scan and update the list of available bit-perfect capable devices.
     */
    fun scanForBitPerfectDevices() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            _availableBitPerfectDevices.value = emptyList()
            return
        }

        try {
            val devices = audioManager?.getDevices(AudioManager.GET_DEVICES_OUTPUTS) ?: emptyArray()
            val bitPerfectCapable = devices.filter { device ->
                isDeviceUsbAudio(device) && hasBitPerfectSupport(device)
            }
            _availableBitPerfectDevices.value = bitPerfectCapable
        } catch (e: Exception) {
            Log.e(TAG, "Error scanning for bit-perfect devices", e)
            _availableBitPerfectDevices.value = emptyList()
        }
    }

    private fun checkAndConfigureBitPerfect() {
        if (!userEnabledBitPerfect) {
            return
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            return
        }

        audioManager?.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            ?.firstOrNull { device ->
                isDeviceUsbAudio(device) && hasBitPerfectSupport(device)
            }?.let { device ->
                configureBitPerfect(device)
            } ?: run {
                disableBitPerfect()
            }
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private fun configureBitPerfect(device: AudioDeviceInfo): Boolean {
        try {
            val supportedAttributes = audioManager?.getSupportedMixerAttributes(device)
            if (supportedAttributes.isNullOrEmpty()) {
                Log.w(TAG, "No mixer attributes supported for device: ${device.productName}")
                disableBitPerfect()
                return false
            }

            val bitPerfectAttribute = supportedAttributes
                .filter { it.mixerBehavior == AudioMixerAttributes.MIXER_BEHAVIOR_BIT_PERFECT }
                .maxByOrNull { attr ->
                    attr.format.sampleRate * attr.format.channelCount
                }

            if (bitPerfectAttribute == null) {
                Log.w(TAG, "No bit-perfect mixer attribute found for device: ${device.productName}")
                disableBitPerfect()
                return false
            }

            val attributes = android.media.AudioAttributes.Builder()
                .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()

            val success = audioManager?.setPreferredMixerAttributes(attributes, device, bitPerfectAttribute)
            if (success == true) {
                bitPerfectDevice = device
                _bitPerfectState.value = BitPerfectState.Active(
                    deviceName = device.productName?.toString() ?: "Unknown",
                    sampleRate = bitPerfectAttribute.format.sampleRate,
                    channelCount = bitPerfectAttribute.format.channelCount,
                    encoding = bitPerfectAttribute.format.encoding,
                    isVolumeFixed = audioManager?.isVolumeFixed == true
                )
                requestVolume()
                Log.i(TAG, "Bit-perfect configured: ${device.productName}, ${bitPerfectAttribute.format.sampleRate}Hz, ${bitPerfectAttribute.format.channelCount}ch")
                return true
            } else {
                Log.w(TAG, "Failed to set preferred mixer attributes")
                disableBitPerfect()
                return false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error configuring bit-perfect mode", e)
            disableBitPerfect()
            return false
        }
    }

    private fun disableBitPerfect() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            return
        }

        try {
            val audioAttributes = android.media.AudioAttributes.Builder()
                .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()

            bitPerfectDevice?.let { device ->
                audioManager?.clearPreferredMixerAttributes(audioAttributes, device)
                Log.i(TAG, "Bit-perfect cleared for device: ${device.productName}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing preferred mixer attributes", e)
        } finally {
            bitPerfectDevice = null
            _bitPerfectState.value = BitPerfectState.Inactive(audioManager?.isVolumeFixed == true)
        }
    }

    private fun isDeviceUsbAudio(device: AudioDeviceInfo): Boolean {
        return device.type == AudioDeviceInfo.TYPE_USB_DEVICE ||
                device.type == AudioDeviceInfo.TYPE_USB_HEADSET ||
                device.type == AudioDeviceInfo.TYPE_USB_ACCESSORY
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private fun hasBitPerfectSupport(device: AudioDeviceInfo): Boolean {
        return try {
            val attributes = audioManager?.getSupportedMixerAttributes(device)
            attributes?.any { it.mixerBehavior == AudioMixerAttributes.MIXER_BEHAVIOR_BIT_PERFECT } == true
        } catch (e: Exception) {
            false
        }
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

    private fun requestVolume() {
        audioManager?.let {
            val maxVolume = getStreamMaxVolume(it, AudioManager.STREAM_MUSIC).toFloat()
            val minVolume = getStreamMinVolume(it, AudioManager.STREAM_MUSIC).toFloat()
            _systemVolumeState.value = VolumeState(
                currentVolume = it.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat(),
                volumeRange = minVolume..maxVolume,
                isFixed = it.isVolumeFixed
            )
        }
    }

    private fun requestAudioDevice() {
        _audioDevice.value = getCurrentAudioDevice()
        _systemVolumeState.value = systemVolumeState.value.copy(
            isFixed = audioManager?.isVolumeFixed == true
        )
    }

    private val audioDeviceCallback: AudioDeviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
            scanForBitPerfectDevices()
            requestAudioDevice()
            requestVolume()
            if (userEnabledBitPerfect) {
                checkAndConfigureBitPerfect()
            }
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
            scanForBitPerfectDevices()
            requestAudioDevice()
            requestVolume()
        }
    }

    companion object {
        private const val TAG = "AudioOutputObserver"
        private const val VOLUME_CHANGED_ACTION = "android.media.VOLUME_CHANGED_ACTION"
    }
}