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

package com.mardous.booming.util

import com.mardous.booming.BuildConfig

object Constants {
    // GitHub Links
    const val AUTHOR_GITHUB_URL = BuildConfig.AUTHOR_GITHUB_URL
    const val GITHUB_URL = BuildConfig.GITHUB_URL
    const val RELEASES_LINK = BuildConfig.RELEASES_LINK
    const val ISSUE_TRACKER_LINK = BuildConfig.ISSUE_TRACKER_LINK
    const val COMMUNITY_LINK = BuildConfig.COMMUNITY_LINK
    const val FAQ_LINK = BuildConfig.FAQ_LINK

    // External Links
    const val DOWNLOAD_URL = BuildConfig.DOWNLOAD_URL
    const val TELEGRAM_COMMUNITY_LINK = BuildConfig.TELEGRAM_COMMUNITY_LINK
    const val DONATION_LINK = BuildConfig.DONATION_LINK
    const val TRANSLATIONS_LINK = BuildConfig.TRANSLATIONS_LINK

    // API URLs
    const val GITHUB_API_URL = BuildConfig.GITHUB_API_URL
    const val LASTFM_API_URL = BuildConfig.LASTFM_API_URL
    const val LISTENBRAINZ_API_URL = BuildConfig.LISTENBRAINZ_API_URL
    const val LRCLIB_API_URL = BuildConfig.LRCLIB_API_URL
    const val BETTERLYRICS_API_URL = BuildConfig.BETTERLYRICS_API_URL
    const val LYRICALLY_API_URL = BuildConfig.LYRICALLY_API_URL

    // Web Search URLs
    const val GOOGLE_SEARCH_URL = BuildConfig.GOOGLE_SEARCH_URL
    const val LASTFM_MUSIC_URL = BuildConfig.LASTFM_MUSIC_URL
    const val LASTFM_LOCALIZED_MUSIC_URL = BuildConfig.LASTFM_LOCALIZED_MUSIC_URL
    const val WIKIPEDIA_SEARCH_URL = BuildConfig.WIKIPEDIA_SEARCH_URL
    const val WIKIPEDIA_LOCALIZED_SEARCH_URL = BuildConfig.WIKIPEDIA_LOCALIZED_SEARCH_URL
    const val YOUTUBE_SEARCH_URL = BuildConfig.YOUTUBE_SEARCH_URL

    // Support Email
    const val SUPPORT_EMAIL = BuildConfig.SUPPORT_EMAIL

    // App basics
    const val USER_AGENT = "BoomingMusic/${BuildConfig.VERSION_NAME} ($GITHUB_URL)"
    const val LYRICALLY_REQUEST_REFERRER = BuildConfig.LYRICALLY_REQUEST_REFERRER
}