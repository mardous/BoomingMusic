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

package com.mardous.booming.core.model

import androidx.annotation.StringRes
import com.mardous.booming.R
import java.net.URLEncoder
import java.util.Locale

import com.mardous.booming.util.Constants.GOOGLE_SEARCH_URL
import com.mardous.booming.util.Constants.LASTFM_LOCALIZED_MUSIC_URL
import com.mardous.booming.util.Constants.LASTFM_MUSIC_URL
import com.mardous.booming.util.Constants.WIKIPEDIA_LOCALIZED_SEARCH_URL
import com.mardous.booming.util.Constants.WIKIPEDIA_SEARCH_URL
import com.mardous.booming.util.Constants.YOUTUBE_SEARCH_URL

enum class WebSearchEngine(
    @StringRes val nameRes: Int,
    private val baseUrl: String,
    private val localizedUrl: String? = null
) {
    Google(R.string.google, GOOGLE_SEARCH_URL),
    LastFm(R.string.lastfm, LASTFM_MUSIC_URL, LASTFM_LOCALIZED_MUSIC_URL),
    Wikipedia(R.string.wikipedia, WIKIPEDIA_SEARCH_URL, WIKIPEDIA_LOCALIZED_SEARCH_URL),
    YouTube(R.string.youtube, YOUTUBE_SEARCH_URL);


    fun getURLForQuery(query: String, locale: Locale = Locale.getDefault()): String {
        val url = if (locale != Locale.ENGLISH && localizedUrl != null) {
            String.format(localizedUrl, locale.language)
        } else {
            baseUrl
        }
        return runCatching { url + URLEncoder.encode(query, "UTF-8") }.getOrDefault(url + query)
    }
}