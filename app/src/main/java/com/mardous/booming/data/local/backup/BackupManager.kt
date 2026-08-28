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

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Document
import android.provider.MediaStore.Audio.AudioColumns
import android.util.Log
import androidx.collection.LruCache
import androidx.core.content.edit
import androidx.core.content.pm.PackageInfoCompat
import androidx.documentfile.provider.DocumentFile
import com.mardous.booming.coil.CustomArtistImageManager
import com.mardous.booming.core.appwidgets.config.WidgetConfigStore
import com.mardous.booming.core.model.lyrics.LyricsViewSettings
import com.mardous.booming.data.local.room.LyricsDao
import com.mardous.booming.data.local.room.LyricsEntity
import com.mardous.booming.data.local.room.PlayCountDao
import com.mardous.booming.data.local.room.PlaylistEntity
import com.mardous.booming.data.mapper.toPlayCount
import com.mardous.booming.data.mapper.toSongEntity
import com.mardous.booming.data.model.Song
import com.mardous.booming.data.repository.RealSongRepository
import com.mardous.booming.data.repository.Repository
import com.mardous.booming.extensions.files.belongsTo
import com.mardous.booming.extensions.files.getFileProviderUri
import com.mardous.booming.extensions.files.getFormattedFileName
import com.mardous.booming.extensions.utilities.sanitize
import com.mardous.booming.playback.PersistentStorage
import com.mardous.booming.util.FileTypeVerifier
import com.mardous.booming.util.FileUtil
import com.mardous.booming.util.m3u.M3UWriter
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.koin.core.component.get
import java.io.File
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

class BackupManager(
    private val context: Context,
    private val repository: Repository,
    private val playCountDao: PlayCountDao,
    private val lyricsDao: LyricsDao,
    private val customArtistImageManager: CustomArtistImageManager
) : BackupComponent() {

    companion object {
        private const val TAG = "BackupManager"
        private const val APPEND_EXTENSION = ".$BACKUP_EXTENSION"
    }

    private val backupFiles = object : LruCache<Uri, File>(10) {
        override fun entryRemoved(evicted: Boolean, key: Uri, oldValue: File, newValue: File?) {
            super.entryRemoved(evicted, key, oldValue, newValue)
            oldValue.takeIf { it.exists() }?.delete()
        }
    }

    private val json = Json {
        ignoreUnknownKeys = true
    }

    suspend fun createBackup(
        backupDirectory: Uri,
        enteredName: String,
        contents: List<BackupContent>
    ) = withContext(IO) {
        if (backupDirectory == Uri.EMPTY || contents.isEmpty())
            return@withContext false

        val directory = DocumentFile.fromTreeUri(context, backupDirectory)
            ?: return@withContext false

        val fileName = enteredName.trim { it.isWhitespace() || it == '.' }
            .sanitize().ifEmpty { getFormattedFileName("Backup", BACKUP_EXTENSION) }
            .let { if (it.endsWith(APPEND_EXTENSION)) it else "$it$APPEND_EXTENSION" }

        val file = try {
            directory.createFile(BACKUP_MIME_TYPE, fileName)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating backup file", e)
            null
        } ?: return@withContext false

        val tempDir = File(context.cacheDir, "/backup_${System.currentTimeMillis()}/")
        if (tempDir.mkdirs()) {
            try {
                val backedUpContents = mutableListOf<BackupContent>()
                val zipItems = mutableListOf<ZipItem>()

                // Preferences
                if (contents.contains(BackupContent.Settings)) {
                    val mainPreferences = setOf(
                        "${context.packageName}_preferences",
                        PersistentStorage.PREFERENCE_NAME,
                        WidgetConfigStore.PREFERENCES_NAME
                    )
                    zipItems.add(
                        createSettingsContent(
                            preferences = mainPreferences,
                            settingsName = MAIN_SETTINGS_NAME
                        )
                    )
                    backedUpContents.add(BackupContent.Settings)
                }

                // Lyrics
                if (contents.contains(BackupContent.Lyrics)) {
                    if (zipItems.addNotNull(createLyricsContent())) {
                        backedUpContents.add(BackupContent.Lyrics)
                    }
                }

                // Custom Artist Images
                if (contents.contains(BackupContent.ArtistImages)) {
                    if (zipItems.addAll(createCustomArtistImagesContent())) {
                        backedUpContents.add(BackupContent.ArtistImages)
                    }
                }

                // Playlists
                if (contents.contains(BackupContent.Playlists)) {
                    if (zipItems.addAll(createPlaylistsContent(tempDir))) {
                        backedUpContents.add(BackupContent.Playlists)
                    }
                }

                // Play info
                if (contents.contains(BackupContent.PlayInfo)) {
                    if (zipItems.addNotNull(createPlayInfoContent())) {
                        backedUpContents.add(BackupContent.PlayInfo)
                    }
                }

                // Metadata (only the contents actually written to the backup)
                val metadata = createMetadata(context, backedUpContents.sortedBy { it.ordinal })
                    ?: throw IllegalStateException("Cannot create necessary metadata")
                zipItems.add(0, metadata)

                // Create backup zip file
                if (zipItems.isNotEmpty()) {
                    return@withContext context.contentResolver.openOutputStream(file.uri)
                        ?.use { createZipFile(zipItems, it) } ?: false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error creating backup", e)
            } finally {
                tempDir.deleteRecursively()
            }
        }
        return@withContext false
    }

    suspend fun restoreBackup(source: Uri, contents: List<BackupContent>) = withContext(IO) {
        context.contentResolver.openBackupZipFile(
            backupUri = source,
            onZipFile = { zipFile ->
                val metadataEntry = zipFile.getEntry("metadata.json")
                if (metadataEntry == null) {
                    restoreLegacyBackup(contents, zipFile, FIRST_BACKUP_VERSION)
                } else {
                    val metadata = zipFile.getInputStream(metadataEntry).use { metadataInput ->
                        json.decodeFromString<BackupMetadata>(
                            metadataInput.reader().use { reader -> reader.readText() }
                        )
                    }
                    if (metadata.backupVersion == CURRENT_BACKUP_VERSION) {
                        restoreBackupWithMetadata(contents, zipFile)
                    } else if (metadata.backupVersion < CURRENT_BACKUP_VERSION) {
                        restoreLegacyBackup(contents, zipFile, metadata.backupVersion)
                    } else {
                        throw IllegalStateException("Unsupported backup version")
                    }
                }
            },
            onError = {
                Log.e(TAG, "Error restoring backup", it)
                false
            }
        )
    }

    suspend fun getBackupsInDirectory(directoryUri: Uri): List<BackupFile> = withContext(IO) {
        val documentId = DocumentsContract.getTreeDocumentId(directoryUri)
        val childUri = DocumentsContract.buildChildDocumentsUriUsingTree(directoryUri, documentId)
        context.contentResolver.query(
            childUri,
            arrayOf(
                Document.COLUMN_DOCUMENT_ID,
                Document.COLUMN_DISPLAY_NAME,
                Document.COLUMN_LAST_MODIFIED,
                Document.COLUMN_SIZE
            ),
            "${Document.COLUMN_MIME_TYPE} != ?",
            arrayOf(Document.MIME_TYPE_DIR),
            null
        )?.use { cursor ->
            buildList {
                if (cursor.moveToFirst()) do {
                    val name = cursor.getString(cursor.getColumnIndexOrThrow(Document.COLUMN_DISPLAY_NAME))
                    val extension = name.substringAfterLast('.', "").lowercase()
                    if (extension == BACKUP_EXTENSION) {
                        val documentId = cursor.getString(cursor.getColumnIndexOrThrow(Document.COLUMN_DOCUMENT_ID))
                        val lastModified = cursor.getLong(cursor.getColumnIndexOrThrow(Document.COLUMN_LAST_MODIFIED))
                        val size = cursor.getLong(cursor.getColumnIndexOrThrow(Document.COLUMN_SIZE))
                        val uri = DocumentsContract.buildDocumentUriUsingTree(directoryUri, documentId)
                        add(
                            BackupFile(
                                name = name.substringBeforeLast('.'),
                                uri = uri,
                                size = size,
                                lastModified = lastModified
                            )
                        )
                    }
                } while (cursor.moveToNext())

                sortByDescending { it.lastModified }
            }
        } ?: emptyList()
    }

    suspend fun getBackupFileInfo(backupUri: Uri): BackupFileWithMetadata? {
        return context.contentResolver.openBackupZipFile(
            backupUri = backupUri,
            onZipFile = { zipFile ->
                val metadataEntry = zipFile.getEntry("metadata.json")
                if (metadataEntry == null) {
                    BackupFileWithMetadata(backupUri, BackupMetadata(FIRST_BACKUP_VERSION))
                } else {
                    val metadata = zipFile.getInputStream(metadataEntry).use { metadataInput ->
                        json.decodeFromString<BackupMetadata>(
                            metadataInput.reader().use { reader -> reader.readText() }
                        )
                    }
                    BackupFileWithMetadata(backupUri, metadata)
                }
            },
            onError = {
                Log.e(TAG, "Error getting backup file info", it)
                null
            }
        )
    }

    suspend fun createShareUriForBackup(backupUri: Uri): Uri? = withContext(IO) {
        try {
            val documentFile = DocumentFile.fromSingleUri(context, backupUri)
                ?: return@withContext null

            val fileName = documentFile.name?.sanitize() ?: "backup.$BACKUP_EXTENSION"
            val shareDir = File(context.externalCacheDir, "backups")
            if (!shareDir.exists()) shareDir.mkdirs()

            val destFile = File(shareDir, fileName)
            if (destFile.belongsTo(shareDir)) {
                context.contentResolver.openInputStream(backupUri)?.use { input ->
                    destFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                destFile.getFileProviderUri(context)
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "Error creating share Uri for backup", e)
            null
        }
    }

    suspend fun deleteBackup(backupUri: Uri): Boolean = withContext(IO) {
        try {
            DocumentsContract.deleteDocument(context.contentResolver, backupUri)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting backup", e)
            false
        }
    }

    private suspend fun createZipFile(
        items: List<ZipItem>,
        output: OutputStream
    ) = withContext(IO) {
        try {
            ZipOutputStream(output.buffered()).use { out ->
                for (zipItem in items) {
                    if (zipItem.filePath != null) {
                        val itemFile = File(zipItem.filePath)
                        if (itemFile.isFile) {
                            val entry = ZipEntry(zipItem.zipPath)
                            out.putNextEntry(entry)
                            itemFile.inputStream().buffered().use { origin ->
                                origin.copyTo(out)
                            }
                            out.closeEntry()
                        }
                    } else if (!zipItem.fileContent.isNullOrEmpty()) {
                        val entry = ZipEntry(zipItem.zipPath)
                        out.putNextEntry(entry)
                        out.write(zipItem.fileContent.toByteArray(Charsets.UTF_8))
                        out.closeEntry()
                    }
                }
            }
            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "Error creating backup", e)
            return@withContext false
        }
    }

    private fun createMetadata(context: Context, contents: List<BackupContent>): ZipItem? {
        val packageInfo = try {
            context.packageManager.getPackageInfo(context.packageName, 0)
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
        if (packageInfo != null) {
            val metadata = BackupMetadata(
                backupVersion = CURRENT_BACKUP_VERSION,
                appName = packageInfo.packageName,
                appVersionName = packageInfo.versionName,
                appVersionCode = PackageInfoCompat.getLongVersionCode(packageInfo),
                contents = contents
            )
            return ZipItem(
                zipPath = METADATA_NAME,
                fileContent = json.encodeToString(metadata)
            )
        }
        return null
    }

    private fun createSettingsContent(preferences: Set<String>, settingsName: String): ZipItem {
        val prefs = mutableMapOf<String, List<PreferenceContent>>()
        for (preferenceName in preferences) {
            val sharedPrefs = context.getSharedPreferences(preferenceName, Context.MODE_PRIVATE)
            val contents = sharedPrefs.all.mapNotNull {
                val typeWithValue = when (val value = it.value) {
                    is String -> PreferenceContent.Type.String to value
                    is Int -> PreferenceContent.Type.Integer to value.toString()
                    is Long -> PreferenceContent.Type.Long to value.toString()
                    is Float -> PreferenceContent.Type.Float to value.toString()
                    is Boolean -> PreferenceContent.Type.Boolean to value.toString()
                    is Set<*> -> PreferenceContent.Type.Set to value.joinToString(",")
                    else -> null
                }
                typeWithValue?.let { (type, value) ->
                    PreferenceContent(
                        key = it.key,
                        value = value,
                        type = type
                    )
                }
            }
            if (contents.isNotEmpty()) {
                prefs[preferenceName] = contents
            }
        }
        val backup = PreferenceBackup(prefs)
        val backupJson = json.encodeToString(backup)
        return ZipItem(
            zipPath = SETTINGS_PATH.child(settingsName),
            fileContent = backupJson,
        )
    }

    private suspend fun createLyricsContent(): ZipItem? {
        val allLyrics = lyricsDao.getAllLyrics()
        if (allLyrics.isNotEmpty()) {
            return ZipItem(
                zipPath = LYRICS_PATH.child("lyrics.json"),
                fileContent = json.encodeToString(allLyrics)
            )
        }
        return null
    }

    private fun createCustomArtistImagesContent(): List<ZipItem> {
        val zipItems = mutableListOf<ZipItem>()
        val customImageFiles = customArtistImageManager.getImageFiles()
        if (customImageFiles.isNotEmpty()) {
            zipItems.add(
                createSettingsContent(
                    preferences = setOf(CustomArtistImageManager.PREFERENCE_NAME),
                    settingsName = ARTIST_IMAGES_SETTINGS_NAME
                )
            )
            zipItems.addAll(
                customImageFiles.map {
                    ZipItem(
                        zipPath = CUSTOM_ARTISTS_PATH.child(it.name),
                        filePath = it.absolutePath
                    )
                }
            )
        }
        return zipItems
    }

    private suspend fun createPlaylistsContent(tempDir: File): List<ZipItem> {
        val zipItems = mutableListOf<ZipItem>()
        // Cache Playlist files in App storage
        val playlistFolder = File(tempDir, PLAYLISTS_PATH)
        if (playlistFolder.mkdirs()) {
            for (playlist in repository.playlistsWithSongs()) {
                runCatching {
                    M3UWriter.writeToDirectory(playlistFolder, playlist)
                }.onSuccess { playlistFile ->
                    if (playlistFile.exists()) {
                        zipItems.add(
                            ZipItem(
                                zipPath = PLAYLISTS_PATH.child(playlistFile.name),
                                filePath = playlistFile.absolutePath
                            )
                        )
                    }
                }
            }
        }
        return zipItems
    }

    private suspend fun createPlayInfoContent(): ZipItem? {
        val songs = repository.playCountSongs()
        val playInfos = repository.findSongsInPlayCount(songs)
        if (playInfos.isNotEmpty()) {
            val playInfoBackup = PlayInfoBackup(
                playInfos.map {
                    PlayInfoContent(
                        id = it.id,
                        path = it.data,
                        plays = it.playCount,
                        skips = it.skipCount,
                        lastPlayed = it.timePlayed
                    )
                }
            )
            return ZipItem(
                zipPath = PLAY_INFO_PATH.child(DEFAULT_PLAY_INFO_NAME),
                fileContent = json.encodeToString(playInfoBackup)
            )
        }
        return null
    }

    private suspend fun restoreBackupWithMetadata(
        contents: List<BackupContent>,
        zipFile: ZipFile
    ): Boolean {
        return try {
            var errors = 0
            for (entry in zipFile.entries()) {
                when {
                    entry.isSettingsEntry(MAIN_SETTINGS_NAME) -> {
                        if (contents.contains(BackupContent.Settings)) {
                            if (!restoreSettings(zipFile, entry)) errors++
                        } else Log.d(TAG, "Skipping settings entry")
                    }

                    entry.isSettingsEntry(ARTIST_IMAGES_SETTINGS_NAME) -> {
                        if (contents.contains(BackupContent.ArtistImages)) {
                            if (!restoreSettings(zipFile, entry)) errors++
                        } else Log.d(TAG, "Skipping artist images entry")
                    }

                    entry.isPlayInfoEntry(DEFAULT_PLAY_INFO_NAME) -> {
                        if (contents.contains(BackupContent.PlayInfo)) {
                            if (!restorePlayInfo(zipFile, entry)) errors++
                        } else Log.d(TAG, "Skipping play info entry")
                    }

                    entry.isLyricsEntry() -> {
                        if (contents.contains(BackupContent.Lyrics)) {
                            if (!restoreLyrics(zipFile, entry)) errors++
                        } else Log.d(TAG, "Skipping lyrics entry")
                    }

                    entry.isPlaylistEntry() -> {
                        if (contents.contains(BackupContent.Playlists)) {
                            if (!restorePlaylist(zipFile, entry)) errors++
                        } else Log.d(TAG, "Skipping playlist entry")
                    }

                    entry.isCustomArtistImageEntry() -> {
                        if (contents.contains(BackupContent.ArtistImages)) {
                            if (!restoreCustomArtistImages(zipFile, entry)) errors++
                        } else Log.d(TAG, "Skipping custom artist image entry")
                    }
                }
            }
            errors == 0
        } catch (e: Exception) {
            Log.e(TAG, "Error restoring backup with metadata", e)
            false
        }
    }

    @SuppressLint("ApplySharedPref")
    private suspend fun restoreSettings(zipFile: ZipFile, entry: ZipEntry) = withContext(IO) {
        try {
            val preferenceBackup = zipFile.getInputStream(entry).use { stream ->
                json.decodeFromString<PreferenceBackup>(stream.bufferedReader().use { it.readText() })
            }
            if (!preferenceBackup.prefs.isEmpty()) {
                for (entry in preferenceBackup.prefs) {
                    val sharedPrefs = context.getSharedPreferences(entry.key, Context.MODE_PRIVATE)
                    val modifiedKeys = mutableSetOf<String>()
                    val currentKeys = sharedPrefs.all.keys
                    sharedPrefs.edit(commit = true) {
                        for (content in entry.value) {
                            if (content.key.isEmpty()) continue
                            if (content.key == LyricsViewSettings.Key.SELECTED_CUSTOM_FONT) {
                                if (!isValidFontPath(content.value)) continue
                            }
                            when (content.type) {
                                PreferenceContent.Type.String -> putString(content.key, content.value)
                                PreferenceContent.Type.Integer -> putInt(content.key, content.value.toInt())
                                PreferenceContent.Type.Long -> putLong(content.key, content.value.toLong())
                                PreferenceContent.Type.Float -> putFloat(content.key, content.value.toFloat())
                                PreferenceContent.Type.Boolean -> putBoolean(content.key, content.value.toBoolean())
                                PreferenceContent.Type.Set -> putStringSet(content.key, content.value.split(",").toSet())
                            }
                            modifiedKeys.add(content.key)
                        }
                        if (currentKeys.isNotEmpty() && modifiedKeys.isNotEmpty()) {
                            for (currentKey in currentKeys) {
                                if (!modifiedKeys.contains(currentKey)) {
                                    remove(currentKey)
                                }
                            }
                        }
                    }
                }
                true
            } else false
        } catch (e: Exception) {
            Log.e(TAG, "Error restoring settings", e)
            false
        }
    }

    private suspend fun restoreLyrics(zipFile: ZipFile, entry: ZipEntry) = withContext(IO) {
        try {
            val serializedLyrics = zipFile.getInputStream(entry).use { zis ->
                zis.bufferedReader().use { it.readText() }
            }
            val lyrics = json.decodeFromString<List<LyricsEntity>>(serializedLyrics)
            if (lyrics.isNotEmpty()) {
                lyricsDao.insertLyrics(lyrics)
                true
            } else {
                throw IllegalArgumentException("Lyrics list is empty")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error restoring lyrics", e)
            false
        }
    }

    private suspend fun restorePlaylist(zipFile: ZipFile, zipEntry: ZipEntry): Boolean {
        try {
            val playlistName = zipEntry.getFileName().substringBeforeLast(".")
            val songs = mutableListOf<Song>()

            withContext(IO) {
                zipFile.getInputStream(zipEntry)
            }.bufferedReader().use {
                it.lineSequence().forEach { line ->
                    if (line.startsWith(File.separator)) {
                        if (File(line).exists()) {
                            songs.add(repository.songByFilePath(line, ignoreBlacklist = true))
                        }
                    }
                }
            }

            val playlistEntity = repository.checkPlaylistExists(playlistName).singleOrNull()
            if (playlistEntity != null) {
                val songEntities = songs.map {
                    it.toSongEntity(playlistEntity.playListId)
                }
                repository.insertSongsInPlaylist(songEntities)
            } else {
                val playListId = repository.createPlaylist(PlaylistEntity(playlistName = playlistName))
                val songEntities = songs.map {
                    it.toSongEntity(playListId)
                }
                repository.insertSongsInPlaylist(songEntities)
            }
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error restoring playlist", e)
            return false
        }
    }

    private suspend fun restorePlayInfo(zipFile: ZipFile, entry: ZipEntry) = withContext(IO) {
        try {
            val playInfoBackup = zipFile.getInputStream(entry).use { stream ->
                json.decodeFromString<PlayInfoBackup>(stream.bufferedReader().use { it.readText() })
            }
            if (playInfoBackup.playInfo.isNotEmpty()) {
                val repository = get<RealSongRepository>()

                val backupMap = playInfoBackup.playInfo.associateBy { it.path }
                val paths = backupMap.keys.toList()

                paths.chunked(900).forEach { chunk ->
                    val selection = "${AudioColumns.DATA} IN (${chunk.joinToString(",") { "?" }})"
                    val selectionArgs = chunk.toTypedArray()

                    val songs = repository.songs(repository.makeSongCursor(selection, selectionArgs))
                    val entities = songs.mapNotNull { song ->
                        backupMap[song.data]?.let { backup ->
                            song.toPlayCount(
                                timePlayed = backup.lastPlayed,
                                playCount = backup.plays,
                                skipCount = backup.skips
                            )
                        }
                    }
                    if (entities.isNotEmpty()) {
                        playCountDao.upsertSongsInPlayCount(entities)
                    }
                }
                true
            } else false
        } catch (e: Exception) {
            Log.e(TAG, "Error restoring play info", e)
            false
        }
    }

    private fun restoreCustomArtistImages(zipFile: ZipFile, zipEntry: ZipEntry): Boolean {
        try {
            val customArtistImagesDir = FileUtil.customArtistImagesDirectory()
                ?: return false

            zipFile.getInputStream(zipEntry).buffered().use { input ->
                input.mark(3) //limit to 3 bytes to check for JPEG
                val isJpegImage = with(FileTypeVerifier) { input.isJpegImage() }
                if (isJpegImage) {
                    input.reset()
                    val file = File(customArtistImagesDir, zipEntry.getFileName())
                    val bytesCopied = file.outputStream().buffered().use { bos ->
                        input.copyTo(bos)
                    }
                    // basic check since some ZIP stream headers may have 0 size
                    return bytesCopied == zipEntry.size || zipEntry.size == -1L
                }
            }
        } catch (e: Exception) {
            Log.e("BackupManager", "Error restoring custom artist images", e)
        }
        return false
    }

    private suspend fun restoreLegacyBackup(
        contents: List<BackupContent>,
        zipFile: ZipFile,
        backupVersion: Int
    ): Boolean {
        return if (backupVersion == FIRST_BACKUP_VERSION) {
            LegacyBackupHelper.restoreLegacyBackupV1(
                context = context,
                zipFile = zipFile,
                contents = contents,
                onRestorePlaylist = { entry -> restorePlaylist(zipFile, entry) },
                onRestoreLyrics = { entry -> restoreLyrics(zipFile, entry) },
                onRestoreCustomArtistImages = { entry -> restoreCustomArtistImages(zipFile, entry) }
            )
        } else throw IllegalStateException("Unsupported legacy backup version")
    }

    private fun getBackupFileFromCacheDir(backupUri: Uri): File {
        val backupId = if (DocumentsContract.isDocumentUri(context, backupUri)) {
            DocumentsContract.getDocumentId(backupUri)
                .sanitize().ifEmpty { backupUri.hashCode().toString() }
        } else {
            backupUri.hashCode().toString()
        }
        return File(context.cacheDir, "backup_$backupId.zip")
    }

    private suspend fun <T> ContentResolver.openBackupZipFile(
        backupUri: Uri,
        onZipFile: suspend (ZipFile) -> T,
        onError: (Exception) -> T
    ): T = withContext(IO) {
        try {
            val cachedFile = backupFiles[backupUri]
            if (cachedFile != null && cachedFile.isFile) {
                ZipFile(cachedFile).use { zipFile ->
                    onZipFile(zipFile)
                }
            } else {
                val tempFile = getBackupFileFromCacheDir(backupUri)
                openInputStream(backupUri)?.use { inputStream ->
                    tempFile.outputStream().use { os -> inputStream.copyTo(os) }
                } ?: throw Exception("Error opening backup file")

                backupFiles.put(backupUri, tempFile)

                ZipFile(tempFile).use { zipFile ->
                    onZipFile(zipFile)
                }
            }
        } catch (e: Exception) {
            onError(e)
        }
    }

    private fun isValidFontPath(path: String): Boolean {
        if (path.endsWith(".ttf") || path.endsWith(".otf")) {
            val fontsDir = FileUtil.fontsDirectory() ?: return false
            val fontFile = File(path)
            if (fontFile.belongsTo(fontsDir)) {
                return with(FileTypeVerifier) { fontFile.inputStream().use { it.isFontFile() } }
            }
        }
        return false
    }

    private fun MutableList<ZipItem>.addNotNull(zipItem: ZipItem?): Boolean {
        if (zipItem == null) return false
        return this.add(zipItem)
    }

    private fun String.child(child: String) = this + File.separator + child
}