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

/**
 * Centered location for application constants including URLs and global identifiers.
 */
object Constants {
    // GitHub Links
    const val AUTHOR_GITHUB_URL = "https://www.github.com/mardous"
    const val GITHUB_URL = "https://www.github.com/mardous/BoomingMusic"
    const val RELEASES_LINK = "https://www.github.com/mardous/BoomingMusic/releases"
    const val ISSUE_TRACKER_LINK = "https://www.github.com/mardous/BoomingMusic/issues"
    const val COMMUNITY_LINK = "https://www.github.com/mardous/BoomingMusic/wiki/Community"
    const val FAQ_LINK = "https://www.github.com/mardous/BoomingMusic/wiki/FAQ"

    // External Links
    const val TELEGRAM_COMMUNITY_LINK = "https://t.me/mardousdev"
    const val DONATION_LINK = "https://ko-fi.com/christiaam"
    const val TRANSLATIONS_LINK = "https://hosted.weblate.org/engage/booming-music/"
    const val FDROID_LINK = "https://f-droid.org/packages/com.mardous.booming/"
    const val PLAY_STORE_LINK = "https://play.google.com/store/apps/details?id=com.mardous.booming"

    // API URLs
    const val GITHUB_API_URL = "https://api.github.com/"
    const val LASTFM_API_URL = "https://ws.audioscrobbler.com/2.0/"
    const val LISTENBRAINZ_API_URL = "https://api.listenbrainz.org/1/"

    // Web Search URLs
    const val GOOGLE_SEARCH_URL = "https://www.google.com/search?q="
    const val LASTFM_MUSIC_URL = "https://www.last.fm/music/"
    const val LASTFM_LOCALIZED_MUSIC_URL = "https://www.last.fm/%s/music/"
    const val WIKIPEDIA_SEARCH_URL = "https://www.wikipedia.org/wiki/Special:Search?search="
    const val WIKIPEDIA_LOCALIZED_SEARCH_URL = "https://%s.wikipedia.org/wiki/Special:Search?search="
    const val YOUTUBE_SEARCH_URL = "https://www.youtube.com/results?search_query="
    
    // Support Email
    const val DOWNLOAD_URL = RELEASES_LINK
    const val SUPPORT_EMAIL = "mardous.contact@gmail.com"

    // App basics
    const val USER_AGENT = "BoomingMusic/${BuildConfig.VERSION_NAME} ($GITHUB_URL)"
}
