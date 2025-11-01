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

enum class WebSearchEngine(
    @StringRes val nameRes: Int,
    private val baseUrl: String,
    private val localizedUrl: String? = null
) {
    Google(R.string.google, "https://www.google.com/search?q="),
    LastFm(R.string.lastfm, "https://wwww.last.fm/music/", "https://www.last.fm/%s/music/"),
    Wikipedia(R.string.wikipedia, "https://www.wikipedia.org/wiki/Special:Search?search=", "https://%s.wikipedia.org/wiki/Special:Search?search="),
    YouTube(R.string.youtube, "https://www.youtube.com/results?search_query=");

    fun getURLForQuery(query: String, locale: Locale = Locale.getDefault()): String {
        val url = if (locale != Locale.ENGLISH && localizedUrl != null) {
            String.format(localizedUrl, locale.language)
        } else {
            baseUrl
        }
        return runCatching { url + URLEncoder.encode(query, "UTF-8") }.getOrDefault(url + query)
    }
}