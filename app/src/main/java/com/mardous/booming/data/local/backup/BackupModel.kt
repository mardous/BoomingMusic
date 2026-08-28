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

import android.net.Uri
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class BackupContent { Settings, Lyrics, PlayInfo, ArtistImages, Playlists }

@Serializable
data class PlayInfoBackup(
    @SerialName("play_info")
    val playInfo: List<PlayInfoContent>
)

@Serializable
data class PlayInfoContent(
    @SerialName("id")
    val id: Long,
    @SerialName("data")
    val path: String,
    @SerialName("plays")
    val plays: Int,
    @SerialName("skips")
    val skips: Int,
    @SerialName("last_played")
    val lastPlayed: Long
)

@Serializable
data class PreferenceBackup(
    @SerialName("preferences")
    val prefs: Map<String, List<PreferenceContent>>
)

@Serializable
data class PreferenceContent(
    val key: String,
    val value: String,
    val type: Type
) {
    enum class Type {
        String, Set, Integer, Long, Float, Boolean
    }
}

@Serializable
data class BackupMetadata(
    @SerialName("backup_version")
    val backupVersion: Int,
    @SerialName("app_name")
    val appName: String? = null,
    @SerialName("app_version_name")
    val appVersionName: String? = null,
    @SerialName("app_version_code")
    val appVersionCode: Long = 0,
    @SerialName("contents")
    val contents: List<BackupContent> = emptyList()
) {
    val isNewerFormat = backupVersion > BackupComponent.CURRENT_BACKUP_VERSION
}

data class ZipItem(
    val zipPath: String,
    val filePath: String? = null,
    val fileContent: String? = null
)

data class BackupFile(
    val name: String,
    val uri: Uri,
    val size: Long,
    val lastModified: Long
)

data class BackupFileWithMetadata(
    val uri: Uri,
    val metadata: BackupMetadata
)