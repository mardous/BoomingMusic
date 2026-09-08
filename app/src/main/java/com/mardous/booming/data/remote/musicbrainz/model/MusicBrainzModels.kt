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

package com.mardous.booming.data.remote.musicbrainz.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MusicBrainzSearchResponse(
    val recordings: List<MusicBrainzRecording> = emptyList()
)

@Serializable
data class MusicBrainzRecording(
    val id: String,
    val title: String,
    val disambiguation: String? = null,
    @SerialName("artist-credit")
    val artistCredit: List<MusicBrainzArtistCredit> = emptyList(),
    val releases: List<MusicBrainzRelease> = emptyList(),
    @SerialName("first-release-date")
    val firstReleaseDate: String? = null,
    val genres: List<MusicBrainzGenre> = emptyList(),
    val relations: List<MusicBrainzRelation> = emptyList()
)

@Serializable
data class MusicBrainzRelation(
    val type: String,
    val direction: String? = null,
    @SerialName("target-type")
    val targetType: String? = null,
    val artist: MusicBrainzArtist? = null,
    val work: MusicBrainzWork? = null
)

@Serializable
data class MusicBrainzWork(
    val id: String,
    val title: String,
    val relations: List<MusicBrainzRelation> = emptyList()
)

@Serializable
data class MusicBrainzGenre(
    val name: String,
    val count: Int? = null
)

@Serializable
data class MusicBrainzArtistCredit(
    val name: String,
    val artist: MusicBrainzArtist
)

@Serializable
data class MusicBrainzArtist(
    val id: String,
    val name: String
)

@Serializable
data class MusicBrainzRelease(
    val id: String,
    val title: String,
    val date: String? = null,
    @SerialName("media")
    val media: List<MusicBrainzMedia> = emptyList(),
    @SerialName("artist-credit")
    val artistCredit: List<MusicBrainzArtistCredit> = emptyList(),
    val genres: List<MusicBrainzGenre> = emptyList(),
)

@Serializable
data class MusicBrainzMedia(
    @SerialName("track-count")
    val trackCount: Int? = null,
    val position: Int? = null,
    val tracks: List<MusicBrainzTrack> = emptyList()
)

@Serializable
data class MusicBrainzTrack(
    val id: String? = null,
    val number: String? = null,
    val title: String? = null
)
