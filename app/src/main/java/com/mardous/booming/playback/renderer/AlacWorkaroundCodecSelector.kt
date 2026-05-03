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

package com.mardous.booming.playback.renderer

import androidx.annotation.OptIn
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.mediacodec.MediaCodecInfo
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector

@OptIn(UnstableApi::class)
class AlacWorkaroundCodecSelector : MediaCodecSelector {

    override fun getDecoderInfos(
        mimeType: String,
        requiresSecureDecoder: Boolean,
        requiresTunnelingDecoder: Boolean
    ): List<MediaCodecInfo> {
        return MediaCodecSelector.DEFAULT.getDecoderInfos(
            mimeType,
            requiresSecureDecoder,
            requiresTunnelingDecoder
        ).let { defaultInfos ->
            // Check specifically for Apple Lossless Audio Codec (ALAC).
            if (mimeType.equals(MimeTypes.AUDIO_ALAC, ignoreCase = true)) {
                /*
                 * Some device-specific hardware ALAC decoders are known to malfunction
                 * when processing high-bitrate M4A files.
                 *
                 * We filter out hardware-accelerated decoders to prioritize stable
                 * software-based decoding. If no software decoders are found in this list,
                 * ExoPlayer will attempt to use available extensions (like FFmpeg).
                 */
                defaultInfos.filterNot { it.hardwareAccelerated }
            } else {
                // For all other MIME types, proceed with the default selection logic.
                defaultInfos
            }
        }
    }
}