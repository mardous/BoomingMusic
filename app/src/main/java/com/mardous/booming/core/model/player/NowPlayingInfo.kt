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

package com.mardous.booming.core.model.player

import android.os.Parcelable
import com.mardous.booming.R
import com.mardous.booming.data.local.MetadataReader
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import org.jaudiotagger.audio.AudioHeader

@Parcelize
@Serializable
class NowPlayingInfo(val info: Info, var isEnabled: Boolean) : Parcelable {

    @Serializable
    enum class Info(val displayNameRes: Int, val key: String?) {
        Album(R.string.album, MetadataReader.ALBUM),
        AlbumArtist(R.string.album_artist, MetadataReader.ALBUM_ARTIST),
        Genre(R.string.genre, MetadataReader.GENRE),
        Year(R.string.year, MetadataReader.YEAR),
        Composer(R.string.composer, MetadataReader.COMPOSER),
        Conductor(R.string.conductor, MetadataReader.PRODUCER),
        Publisher(R.string.publisher, MetadataReader.COPYRIGHT),
        Format(R.string.label_file_format, null),
        Bitrate(R.string.label_bit_rate, null),
        SampleRate(R.string.label_sampling_rate, null);

        fun toReadableFormat(metadataReader: MetadataReader, header: AudioHeader?): String? {
            return when (key) {
                null -> {
                    when (this) {
                        Format -> header?.let { "%s %s".format(it.format, metadataReader.channelName()) }
                        Bitrate -> metadataReader.bitrate()
                        SampleRate -> metadataReader.sampleRate()
                        else -> null
                    }
                }
                MetadataReader.GENRE -> metadataReader.genre()
                else -> metadataReader.first(key)
            }
        }
    }
}