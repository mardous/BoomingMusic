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

package com.mardous.booming.ui.screen.tageditor

data class TagEditorResult(
    val title: String? = null,
    val album: String? = null,
    val artist: String? = null,
    val albumArtist: String? = null,
    val composer: String? = null,
    val conductor: String? = null,
    val publisher: String? = null,
    val genre: String? = null,
    val year: String? = null,
    val trackNumber: String? = null,
    val trackTotal: String? = null,
    val discNumber: String? = null,
    val discTotal: String? = null,
    val lyrics: String? = null,
    val lyricist: String? = null,
    val comment: String? = null
)

class SaveTagsResult(
    val isLoading: Boolean,
    val isSuccess: Boolean,
    val scanned: Int = 0,
    val failed: Int = 0
)