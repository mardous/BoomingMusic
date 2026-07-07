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
    // Support Email
    const val SUPPORT_EMAIL = "mardous.contact@gmail.com"

    // App-related URLs
    const val AUTHOR_GITHUB_URL = "https://www.github.com/mardous"
    const val APP_GITHUB_URL = "https://www.github.com/mardous/BoomingMusic"
    const val RELEASES_LINK = "$APP_GITHUB_URL/releases"
    const val ISSUE_TRACKER_LINK = "$APP_GITHUB_URL/issues"
    const val COMMUNITY_LINK = "$APP_GITHUB_URL/wiki/Community"
    const val FAQ_LINK = "$APP_GITHUB_URL/wiki/FAQ"
    const val TRANSLATIONS_LINK = "https://hosted.weblate.org/engage/booming-music/"
    const val TELEGRAM_LINK = "https://t.me/mardousdev"

    // External Links
    const val DOWNLOAD_URL = BuildConfig.DOWNLOAD_URL
    const val DONATION_LINK = BuildConfig.DONATION_LINK

    const val USER_AGENT = "BoomingMusic/${BuildConfig.VERSION_NAME} ($APP_GITHUB_URL)"
}