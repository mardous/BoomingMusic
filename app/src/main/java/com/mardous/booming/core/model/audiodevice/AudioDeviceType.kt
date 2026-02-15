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

package com.mardous.booming.core.model.audiodevice

import android.media.AudioDeviceInfo
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.mediarouter.media.MediaRouter
import com.mardous.booming.R

enum class AudioDeviceType(
    @param:DrawableRes val iconRes: Int,
    @param:StringRes internal val nameRes: Int,
    internal val isProduct: Boolean,
    internal val audioDeviceTypes: Array<Int>,
    internal val mediaRouteTypes: Array<Int>
) {
    BluetoothA2dp(
        iconRes = R.drawable.ic_media_bluetooth_on_24dp,
        nameRes = R.string.audio_device_bluetooth_a2dp,
        isProduct = true,
        audioDeviceTypes = arrayOf(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP),
        mediaRouteTypes = arrayOf(MediaRouter.RouteInfo.DEVICE_TYPE_BLUETOOTH_A2DP)
    ),
    UsbHeadset(
        iconRes = R.drawable.ic_usb_24dp,
        nameRes = R.string.audio_device_usb_headset,
        isProduct = true,
        audioDeviceTypes = arrayOf(AudioDeviceInfo.TYPE_USB_HEADSET),
        mediaRouteTypes = arrayOf(MediaRouter.RouteInfo.DEVICE_TYPE_USB_HEADSET)
    ),
    UsbDevice(
        iconRes = R.drawable.ic_usb_24dp,
        nameRes = R.string.audio_device_usb_device,
        isProduct = true,
        audioDeviceTypes = arrayOf(
            AudioDeviceInfo.TYPE_USB_ACCESSORY,
            AudioDeviceInfo.TYPE_USB_DEVICE
        ),
        mediaRouteTypes = arrayOf(
            MediaRouter.RouteInfo.DEVICE_TYPE_USB_ACCESSORY,
            MediaRouter.RouteInfo.DEVICE_TYPE_USB_DEVICE
        )
    ),
    Headset(
        iconRes = R.drawable.ic_headphones_24dp,
        nameRes = R.string.audio_device_headset,
        isProduct = false,
        audioDeviceTypes = arrayOf(
            AudioDeviceInfo.TYPE_WIRED_HEADSET,
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES
        ),
        mediaRouteTypes = arrayOf(
            MediaRouter.RouteInfo.DEVICE_TYPE_WIRED_HEADSET,
            MediaRouter.RouteInfo.DEVICE_TYPE_WIRED_HEADPHONES
        )
    ),
    Hdmi(
        iconRes = R.drawable.ic_hdmi_24dp,
        nameRes = R.string.audio_device_hdmi,
        isProduct = false,
        audioDeviceTypes = arrayOf(
            AudioDeviceInfo.TYPE_HDMI,
            AudioDeviceInfo.TYPE_HDMI_ARC,
            AudioDeviceInfo.TYPE_HDMI_EARC
        ),
        mediaRouteTypes = arrayOf(
            MediaRouter.RouteInfo.DEVICE_TYPE_HDMI,
            MediaRouter.RouteInfo.DEVICE_TYPE_HDMI_ARC,
            MediaRouter.RouteInfo.DEVICE_TYPE_HDMI_EARC
        )
    ),
    Aux(
        iconRes = R.drawable.ic_speaker_24dp,
        nameRes = R.string.audio_device_aux_line,
        isProduct = false,
        audioDeviceTypes = arrayOf(AudioDeviceInfo.TYPE_AUX_LINE),
        mediaRouteTypes = emptyArray()
    ),
    BuiltinSpeaker(
        iconRes = R.drawable.ic_speaker_24dp,
        nameRes = R.string.audio_device_builtin_speaker,
        isProduct = false,
        audioDeviceTypes = arrayOf(
            AudioDeviceInfo.TYPE_BUILTIN_SPEAKER,
            AudioDeviceInfo.TYPE_BUILTIN_SPEAKER_SAFE,
            AudioDeviceInfo.TYPE_BUILTIN_EARPIECE
        ),
        mediaRouteTypes = arrayOf(
            MediaRouter.RouteInfo.DEVICE_TYPE_BUILTIN_SPEAKER
        )
    ),
    Unknown(
        iconRes = R.drawable.ic_speaker_24dp,
        nameRes = R.string.audio_device_builtin_speaker,
        isProduct = false,
        audioDeviceTypes = arrayOf(AudioDeviceInfo.TYPE_UNKNOWN),
        mediaRouteTypes = emptyArray()
    );

    val isThisDeviceOutput: Boolean
        get() = this == BuiltinSpeaker || this == Unknown
}

fun AudioDeviceInfo?.getDeviceType() =
    AudioDeviceType.entries.firstOrNull {
        it.audioDeviceTypes.contains(this?.type ?: 0)
    } ?: AudioDeviceType.Unknown

fun MediaRouter.RouteInfo?.getMediaRouteType() =
    AudioDeviceType.entries.firstOrNull {
        it.mediaRouteTypes.contains(this?.deviceType ?: 0)
    } ?: AudioDeviceType.Unknown