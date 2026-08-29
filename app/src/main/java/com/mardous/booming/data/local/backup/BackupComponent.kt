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

package com.mardous.booming.data.local.backup

import org.koin.core.component.KoinComponent
import java.io.File
import java.util.zip.ZipEntry

open class BackupComponent : KoinComponent {

    companion object {
        const val BACKUP_MIME_TYPE = "application/octet-stream"
        const val BACKUP_EXTENSION = "bmgbak"

        const val METADATA_NAME = "metadata.json"
        const val MAIN_SETTINGS_NAME = "main.json"
        const val ARTIST_IMAGES_SETTINGS_NAME = "artist_images.json"
        const val DEFAULT_PLAY_INFO_NAME = "default.json"

        const val PLAYLISTS_PATH = "playlists"
        const val SETTINGS_PATH = "settings"
        const val LYRICS_PATH = "lyrics"
        const val PLAY_INFO_PATH = "play_info"
        const val CUSTOM_ARTISTS_PATH = "artist_images"

        const val FIRST_BACKUP_VERSION = 1
        const val CURRENT_BACKUP_VERSION = 2
    }

    protected fun ZipEntry.isSettingsEntry(settingsName: String): Boolean {
        return name.startsWith(SETTINGS_PATH) && name.contains(settingsName)
    }

    protected fun ZipEntry.isPlayInfoEntry(playInfoName: String): Boolean {
        return name.startsWith(PLAY_INFO_PATH) && name.contains(playInfoName)
    }

    protected fun ZipEntry.isPlaylistEntry(): Boolean {
        return name.startsWith(PLAYLISTS_PATH)
    }

    protected fun ZipEntry.isLyricsEntry(): Boolean {
        return name.startsWith(LYRICS_PATH)
    }

    protected fun ZipEntry.isCustomArtistImageEntry(): Boolean {
        return name.startsWith(CUSTOM_ARTISTS_PATH)
    }

    protected fun ZipEntry.getFileName(): String {
        return name.substringAfterLast(File.separator)
    }
}