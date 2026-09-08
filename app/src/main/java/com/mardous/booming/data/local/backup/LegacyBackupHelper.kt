package com.mardous.booming.data.local.backup

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

object LegacyBackupHelper : BackupComponent() {

    private const val LEGACY_PLAYLISTS_PATH = "Playlists"
    private const val LEGACY_PREFS_PATH = "prefs"
    private const val LEGACY_CUSTOM_ARTIST_IMAGES_PATH = "artistImages"

    suspend fun restoreLegacyBackupV1(
        context: Context,
        zipFile: ZipFile,
        contents: List<BackupContent>,
        onRestorePlaylist: suspend (ZipEntry) -> Boolean,
        onRestoreLyrics: suspend (ZipEntry) -> Boolean,
        onRestoreCustomArtistImages: suspend (ZipEntry) -> Boolean
    ): Boolean = withContext(IO) {
        try {
            var errors = 0
            for (entry in zipFile.entries()) {
                if (entry.isLegacyPrefsEntry() && contents.contains(BackupContent.Settings)) {
                    if (!restoreLegacyPreferences(context, zipFile, entry)) errors++
                } else if (entry.isLegacyPlaylistsEntry() && contents.contains(BackupContent.Playlists)) {
                    if (!onRestorePlaylist(entry)) errors++
                } else if (entry.isLyricsEntry() && contents.contains(BackupContent.Lyrics)) {
                    if (!onRestoreLyrics(entry)) errors++
                } else if (entry.isLegacyCustomArtistImagesEntry() && contents.contains(BackupContent.ArtistImages)) {
                    if (entry.isLegacyCustomArtistImagesPrefsEntry()) {
                        if (!restoreLegacyPreferences(context, zipFile, entry)) errors++
                    } else if (entry.isLegacyCustomArtistImagesFileEntry()) {
                        if (!onRestoreCustomArtistImages(entry)) errors++
                    }
                }
            }
            errors == 0
        } catch (e: Exception) {
            Log.e("BackupManager", "Error restoring legacy backup", e)
            false
        }
    }

    private fun restoreLegacyPreferences(context: Context, zipFile: ZipFile, zipEntry: ZipEntry): Boolean {
        try {
            val prefName = zipEntry.getFileName()
            val dataDir = context.filesDir.parentFile
            if (prefName.endsWith(".xml") && dataDir != null && dataDir.exists()) {
                val sharedPrefsDir = File(dataDir, "shared_prefs")
                if (sharedPrefsDir.exists() || sharedPrefsDir.mkdirs()) {
                    val file = File(sharedPrefsDir, prefName)
                    if (file.exists()) {
                        file.delete()
                    }
                    file.outputStream().buffered().use { bos ->
                        zipFile.getInputStream(zipEntry).copyTo(bos)
                    }
                    return true
                }
            }
        } catch (e: Exception) {
            Log.e("BackupManager", "Error restoring preferences", e)
        }
        return false
    }

    private fun ZipEntry.isLegacyPrefsEntry(): Boolean {
        return name.startsWith(LEGACY_PREFS_PATH)
    }

    private fun ZipEntry.isLegacyPlaylistsEntry(): Boolean {
        return name.startsWith(LEGACY_PLAYLISTS_PATH)
    }

    private fun ZipEntry.isLegacyCustomArtistImagesEntry(): Boolean {
        return name.startsWith(LEGACY_CUSTOM_ARTIST_IMAGES_PATH)
    }

    private fun ZipEntry.isLegacyCustomArtistImagesFileEntry(): Boolean {
        return name.startsWith(LEGACY_CUSTOM_ARTIST_IMAGES_PATH) && name.contains("custom_artist_images")
    }

    private fun ZipEntry.isLegacyCustomArtistImagesPrefsEntry(): Boolean {
        return name.startsWith(LEGACY_CUSTOM_ARTIST_IMAGES_PATH) && name.contains("prefs")
    }
}