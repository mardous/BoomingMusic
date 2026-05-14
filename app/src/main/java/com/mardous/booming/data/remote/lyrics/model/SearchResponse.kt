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

package com.mardous.booming.data.remote.lyrics.model

import kotlinx.serialization.Serializable

@Serializable
data class AppleMusicSearchResponse(
    val results: AppleMusicResults,
    val resources: AppleMusicResources? = null
)

@Serializable
data class AppleMusicResults(
    val songs: AppleMusicSongsResult? = null
)

@Serializable
data class AppleMusicSongsResult(
    val data: List<AppleMusicSongData>
)

@Serializable
data class AppleMusicSongData(
    val id: String,
    val type: String,
    val href: String
)

@Serializable
data class AppleMusicResources(
    val songs: Map<String, AppleMusicSongDetail>? = null
)

@Serializable
data class AppleMusicSongDetail(
    val id: String,
    val type: String,
    val attributes: AppleMusicSongAttributes
)

@Serializable
data class AppleMusicSongAttributes(
    val name: String,
    val artistName: String,
    val albumName: String,
    val durationInMillis: Long? = null
)