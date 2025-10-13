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
package com.mardous.booming.data.local

import android.net.Uri
import android.util.LruCache
import com.mardous.booming.data.model.Song
import com.mardous.booming.data.model.replaygain.ReplayGain

object ReplayGainTagExtractor {

    private val cache = LruCache<Uri, ReplayGain>(128)

    private const val TAG_TRACK_GAIN = "REPLAYGAIN_TRACK_GAIN"
    private const val TAG_ALBUM_GAIN = "REPLAYGAIN_ALBUM_GAIN"
    private const val TAG_TRACK_PEAK = "REPLAYGAIN_TRACK_PEAK"
    private const val TAG_ALBUM_PEAK = "REPLAYGAIN_ALBUM_PEAK"

    private const val ITUNES_PREFIX = "----:com.apple.iTunes:"
    private const val OPUS_TRACK_GAIN = "R128_TRACK_GAIN"
    private const val OPUS_ALBUM_GAIN = "R128_ALBUM_GAIN"

    fun getReplayGain(song: Song): ReplayGain {
        if (song == Song.emptySong) {
            return ReplayGain.Empty
        }
        var gainValues = cache.get(song.uri)
        if (gainValues == null) {
            val metadataReader = MetadataReader(song.uri)
            val rawTags = metadataReader.all()

            val gainTags = parseStandardTags(rawTags)
                .plus(parseItunesTags(rawTags))
                .plus(parseOpusR128(rawTags))

            gainValues = ReplayGain(
                albumGain = gainTags[TAG_ALBUM_GAIN] ?: 0f,
                trackGain = gainTags[TAG_TRACK_GAIN] ?: 0f,
                albumPeak = gainTags[TAG_ALBUM_PEAK] ?: 1f,
                trackPeak = gainTags[TAG_TRACK_PEAK] ?: 1f
            )

            cache.put(song.uri, gainValues)
        }
        return gainValues
    }

    fun removeFromCache(uri: Uri) {
        cache.remove(uri)
    }

    fun clearCache() {
        cache.evictAll()
    }

    private fun parseStandardTags(tags: Map<String, Array<String>>): Map<String, Float> {
        val result = mutableMapOf<String, Float>()
        tags[TAG_TRACK_GAIN]?.firstOrNull()?.let { result[TAG_TRACK_GAIN] = parse(it) }
        tags[TAG_ALBUM_GAIN]?.firstOrNull()?.let { result[TAG_ALBUM_GAIN] = parse(it) }
        tags[TAG_TRACK_PEAK]?.firstOrNull()?.let { result[TAG_TRACK_PEAK] = parse(it, 1f) }
        tags[TAG_ALBUM_PEAK]?.firstOrNull()?.let { result[TAG_ALBUM_PEAK] = parse(it, 1f) }
        return result
    }

    private fun parseItunesTags(tags: Map<String, Array<String>>): Map<String, Float> {
        val result = mutableMapOf<String, Float>()
        fun read(key: String, default: Float = 0f) {
            tags["$ITUNES_PREFIX$key"]?.firstOrNull()?.let {
                result[key] = parse(it, default)
            }
        }
        read(TAG_TRACK_GAIN)
        read(TAG_ALBUM_GAIN)
        read(TAG_TRACK_PEAK, 1f)
        read(TAG_ALBUM_PEAK, 1f)
        return result
    }

    private fun parseOpusR128(tags: Map<String, Array<String>>): Map<String, Float> {
        val result = mutableMapOf<String, Float>()
        val r128Track = tags[OPUS_TRACK_GAIN]?.firstOrNull()?.toFloatOrNull()
        val r128Album = tags[OPUS_ALBUM_GAIN]?.firstOrNull()?.toFloatOrNull()

        // R128 uses LUFS, ReplayGain uses dB. +5 dB is a common conversion.
        r128Track?.let { result[TAG_TRACK_GAIN] = it + 5f }
        r128Album?.let { result[TAG_ALBUM_GAIN] = it + 5f }

        return result
    }

    private fun parse(raw: String, default: Float = 0f): Float {
        return raw
            .replace("dB", "", ignoreCase = true)
            .replace("[^\\d+-.]".toRegex(), "")
            .toFloatOrNull() ?: default
    }
}
