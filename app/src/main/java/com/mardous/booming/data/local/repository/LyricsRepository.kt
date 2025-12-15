package com.mardous.booming.data.local.repository

import android.content.ContentResolver
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import com.mardous.booming.appContext
import com.mardous.booming.core.model.task.Result
import com.mardous.booming.data.local.EditTarget
import com.mardous.booming.data.local.MetadataReader
import com.mardous.booming.data.local.MetadataWriter
import com.mardous.booming.data.local.lyrics.lrc.LrcLyricsParser
import com.mardous.booming.data.local.lyrics.ttml.TtmlLyricsParser
import com.mardous.booming.data.local.room.LyricsDao
import com.mardous.booming.data.local.room.toLyricsEntity
import com.mardous.booming.data.model.Song
import com.mardous.booming.data.model.lyrics.LyricsFile
import com.mardous.booming.data.model.lyrics.LyricsSource
import com.mardous.booming.data.remote.lyrics.LyricsDownloadService
import com.mardous.booming.data.remote.lyrics.model.DownloadedLyrics
import com.mardous.booming.extensions.files.getContentUri
import com.mardous.booming.extensions.hasR
import com.mardous.booming.extensions.isAllowedToDownloadMetadata
import com.mardous.booming.extensions.media.isArtistNameUnknown
import com.mardous.booming.ui.screen.lyrics.DisplayableLyrics
import com.mardous.booming.ui.screen.lyrics.EditableLyrics
import com.mardous.booming.ui.screen.lyrics.LyricsResult
import com.mardous.booming.ui.screen.lyrics.SaveLyricsResult
import java.io.File
import java.util.Collections
import java.util.regex.Pattern

interface LyricsRepository {
    suspend fun onlineLyrics(
        song: Song,
        searchTitle: String,
        searchArtist: String
    ): Result<DownloadedLyrics>

    suspend fun allLyrics(song: Song, allowDownload: Boolean, fromEditor: Boolean): LyricsResult
    suspend fun embeddedLyrics(song: Song, requirePlainText: Boolean): String?
    suspend fun saveLyrics(song: Song, plainLyrics: EditableLyrics?, syncedLyrics: EditableLyrics?): SaveLyricsResult
    suspend fun saveSyncedLyrics(song: Song, lyrics: String?): Boolean
    suspend fun importLyrics(song: Song, uri: Uri): Boolean
    suspend fun findLyricsFiles(song: Song): List<LyricsFile>
    suspend fun writableUris(song: Song): List<Uri>
    suspend fun shareSyncedLyrics(song: Song): Uri?
    suspend fun deleteAllLyrics()
}

class RealLyricsRepository(
    private val context: Context,
    private val preferences: SharedPreferences,
    private val contentResolver: ContentResolver,
    private val lyricsDownloadService: LyricsDownloadService,
    private val lyricsDao: LyricsDao
) : LyricsRepository {

    private val lrcLyricsParser = LrcLyricsParser()
    private val ttmlLyricsParser = TtmlLyricsParser()

    private val lyricsParsers = listOf(lrcLyricsParser, ttmlLyricsParser)
    private val lyricsCache: MutableMap<Long, LyricsResult> = Collections.synchronizedMap(
        object : LinkedHashMap<Long, LyricsResult>(CACHE_SIZE, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Long, LyricsResult>?): Boolean {
                return size > CACHE_SIZE
            }
        }
    )

    private fun getCachedLyrics(songId: Long): LyricsResult? {
        return lyricsCache[songId]
    }

    private fun cacheLyrics(songId: Long, result: LyricsResult): LyricsResult {
        lyricsCache[songId] = result
        return result
    }

    private fun invalidateCache(songId: Long) {
        lyricsCache.remove(songId)
    }

    override suspend fun onlineLyrics(
        song: Song,
        searchTitle: String,
        searchArtist: String
    ): Result<DownloadedLyrics> {
        return if (song.id == Song.emptySong.id) {
            Result.Error(IllegalArgumentException("Song is not valid"))
        } else {
            if (searchArtist.isArtistNameUnknown()) {
                Result.Error(IllegalArgumentException("Artist name is <unknown>"))
            } else {
                try {
                    Result.Success(lyricsDownloadService.getLyrics(song, searchTitle, searchArtist))
                } catch (e: Exception) {
                    Result.Error(e)
                }
            }
        }
    }

    override suspend fun allLyrics(
        song: Song,
        allowDownload: Boolean,
        fromEditor: Boolean
    ): LyricsResult {
        if (song.id == Song.emptySong.id) {
            return LyricsResult.Empty
        }

        if (!fromEditor) {
            getCachedLyrics(song.id)?.let { return it }
        }

        val instrumentalIdentifiers = preferences.getString(INSTRUMENTAL_TRACK_IDENTIFIERS, null)
            ?.split(",").orEmpty()
        if (preferences.getBoolean(MARK_INSTRUMENTAL_BY_TITLE, false)) {
            if (instrumentalIdentifiers.any { song.title.contains(it, ignoreCase = true) }) {
                return cacheLyrics(song.id, LyricsResult(song.id, instrumental = true))
            }
        }

        val embeddedLyrics = embeddedLyrics(song, requirePlainText = false).orEmpty()
        if (embeddedLyrics.length <= INSTRUMENTAL_IDENTIFIER_MAX_LENGTH &&
            instrumentalIdentifiers.contains(embeddedLyrics)) {
            return cacheLyrics(song.id, LyricsResult(song.id, instrumental = true))
        }

        val storedLyrics = lyricsDao.getLyrics(song.id)
        if (storedLyrics != null &&
            storedLyrics.syncedLyrics.length <= INSTRUMENTAL_IDENTIFIER_MAX_LENGTH &&
            instrumentalIdentifiers.contains(storedLyrics.syncedLyrics)) {
            return cacheLyrics(song.id, LyricsResult(song.id, instrumental = true))
        }

        val embeddedLyricsParser = lyricsParsers.firstOrNull { it.handles(embeddedLyrics) }
        val embeddedSynced = embeddedLyricsParser?.parse(embeddedLyrics, song.duration)

        val fileLyrics = findLyricsFiles(song).firstNotNullOfOrNull { file ->
            lyricsParsers.firstOrNull { it.handles(file) }
                ?.parse(file, song.duration)
        }
        if (fileLyrics?.hasContent == true) {
            return cacheLyrics(song.id, LyricsResult(
                id = song.id,
                plainLyrics = DisplayableLyrics(embeddedLyrics, LyricsSource.Embedded),
                syncedLyrics = DisplayableLyrics(fileLyrics, LyricsSource.File)
            ))
        }

        val storedSynced = storedLyrics?.let { stored ->
            lyricsParsers.firstOrNull { it.handles(stored.syncedLyrics) }
                ?.parse(stored.syncedLyrics, song.duration)
        }
        if (embeddedSynced?.hasContent == true) {
            val result = if (fromEditor) {
                val lrcData = if (storedSynced?.hasContent == true) storedSynced else null
                LyricsResult(
                    id = song.id,
                    plainLyrics = DisplayableLyrics(embeddedLyrics, LyricsSource.Embedded),
                    syncedLyrics = DisplayableLyrics(lrcData, LyricsSource.Downloaded)
                )
            } else {
                LyricsResult(
                    id = song.id,
                    syncedLyrics = DisplayableLyrics(embeddedSynced, LyricsSource.Embedded)
                )
            }
            return cacheLyrics(song.id, result)
        }

        if (storedSynced?.hasContent == true) {
            val result = LyricsResult(
                id = song.id,
                plainLyrics = DisplayableLyrics(embeddedLyrics, LyricsSource.Embedded),
                syncedLyrics = DisplayableLyrics(storedSynced, LyricsSource.Downloaded)
            )
            return cacheLyrics(song.id, result)
        }

        if (allowDownload && appContext().isAllowedToDownloadMetadata()) {
            val downloaded = runCatching { lyricsDownloadService.getLyrics(song) }.getOrNull()
            if (downloaded?.instrumental == true) {
                lyricsDao.insertLyrics(
                    song.toLyricsEntity(DEFAULT_INSTRUMENTAL_IDENTIFIER, autoDownload = true)
                )
                val result = LyricsResult(id = song.id, instrumental = true)
                return cacheLyrics(song.id, result)
            }
            if (downloaded?.isSynced == true) {
                val syncedData = lrcLyricsParser.parse(downloaded.syncedLyrics!!, song.duration)
                if (syncedData?.hasContent == true) {
                    lyricsDao.insertLyrics(
                        song.toLyricsEntity(
                            syncedData.rawText,
                            autoDownload = true
                        )
                    )
                    val result = LyricsResult(
                        id = song.id,
                        plainLyrics = DisplayableLyrics(embeddedLyrics, LyricsSource.Embedded),
                        syncedLyrics = DisplayableLyrics(syncedData, LyricsSource.Downloaded)
                    )
                    return cacheLyrics(song.id, result)
                }
            }
        }

        val result = LyricsResult(
            id = song.id,
            plainLyrics = DisplayableLyrics(embeddedLyrics, LyricsSource.Embedded)
        )
        return cacheLyrics(song.id, result)
    }

    override suspend fun embeddedLyrics(song: Song, requirePlainText: Boolean): String? {
        if (song.id != Song.emptySong.id) {
            val metadataReader = MetadataReader(song.uri)
            val lyrics = metadataReader.value(MetadataReader.LYRICS)
            if (requirePlainText && !lyrics.isNullOrBlank()) {
                val parser = lyricsParsers.firstOrNull { it.handles(lyrics) }
                val syncedData = parser?.parse(lyrics, song.duration)
                if (syncedData?.hasContent == true) {
                    return syncedData.plainText
                }
            }
            return lyrics
        }
        return null
    }

    override suspend fun saveLyrics(
        song: Song,
        plainLyrics: EditableLyrics?,
        syncedLyrics: EditableLyrics?
    ): SaveLyricsResult {
        var saveResult = SaveLyricsResult()
        if (plainLyrics?.hasChanged == true) {
            val result = runCatching {
                val target = EditTarget.song(song)
                val metadataWriter = MetadataWriter()
                metadataWriter.propertyMap(
                    propertyMap = hashMapOf(MetadataReader.LYRICS to plainLyrics.content)
                )
                metadataWriter.write(this.context, target).isSuccess
            }
            saveResult = saveResult.copy(
                plainLyricsState = if (result.getOrDefault(false)) {
                    SaveLyricsResult.State.Wrote
                } else {
                    SaveLyricsResult.State.Failed
                }
            )
        }
        if (syncedLyrics?.hasChanged == true && syncedLyrics.source == LyricsSource.Downloaded) {
            val result = runCatching {
                saveSyncedLyrics(song, syncedLyrics.content)
            }
            saveResult = saveResult.copy(
                syncedLyricsState = if (result.getOrDefault(false)) {
                    SaveLyricsResult.State.Wrote
                } else {
                    SaveLyricsResult.State.Failed
                }
            )
        }
        if (saveResult.hasChanged) {
            invalidateCache(song.id)
        }
        return saveResult
    }

    override suspend fun saveSyncedLyrics(song: Song, lyrics: String?): Boolean {
        if (lyrics.isNullOrEmpty()) {
            val lyrics = lyricsDao.getLyrics(song.id)
            if (lyrics != null) {
                if (lyrics.autoDownload) {
                    // The user has deleted an automatically downloaded lyrics, perhaps
                    // because it was incorrect. In this case we do not delete the
                    // registry, we simply clean it, this way it will prevent us from
                    // trying to download it again in the future.
                    lyricsDao.insertLyrics(song.toLyricsEntity("", userCleared = true))
                } else if (!lyrics.userCleared) {
                    lyricsDao.removeLyrics(song.id)
                }
            }
            return true
        } else {
            val parser = lyricsParsers.firstOrNull { it.handles(lyrics) }
            val parsedLyrics = parser?.parse(lyrics, song.duration)
            if (parsedLyrics?.hasContent == true) {
                lyricsDao.insertLyrics(song.toLyricsEntity(parsedLyrics.rawText))
                return true
            }
        }
        return false
    }

    override suspend fun importLyrics(song: Song, uri: Uri): Boolean {
        if (LyricsFile.isSupportedFormat(uri)) {
            return contentResolver.openInputStream(uri).use { stream ->
                val result = runCatching { stream?.reader()?.readText() }
                if (result.isSuccess) {
                    val fileContent = result.getOrThrow()
                    if (fileContent != null && lyricsParsers.any { it.handles(fileContent) }) {
                        lyricsDao.insertLyrics(song.toLyricsEntity(fileContent))
                        invalidateCache(song.id)
                        true
                    } else {
                        false
                    }
                } else {
                    false
                }
            }
        } else {
            return false
        }
    }

    override suspend fun findLyricsFiles(song: Song): List<LyricsFile> {
        val songFile = File(song.data)
        val parentDir = songFile.parentFile ?: return emptyList()

        val baseNames = listOf(
            songFile.nameWithoutExtension,
            "${song.artistName} - ${song.title}"
        ).map { Pattern.quote(it) }

        val patterns = baseNames.map { base ->
            Regex(".*$base.*\\.(lrc|ttml)", RegexOption.IGNORE_CASE)
        }

        return parentDir.listFiles()
            ?.filter { file -> file.isFile && patterns.any { it.matches(file.name) } }
            ?.map { LyricsFile(it, it.extension) }
            .orEmpty()
    }

    override suspend fun writableUris(song: Song): List<Uri> {
        if (hasR()) {
            return listOf(song.uri).filterNot { it == Uri.EMPTY }
        }
        return emptyList()
    }

    override suspend fun shareSyncedLyrics(song: Song): Uri? {
        if (song.id == Song.emptySong.id) {
            return null
        } else {
            val lyrics = lyricsDao.getLyrics(song.id)
            if (lyrics != null) {
                val tempFile = context.externalCacheDir
                    ?.resolve("${song.artistName} - ${song.title}.lrc")
                if (tempFile == null) {
                    return null
                } else {
                    val result = runCatching {
                        tempFile.bufferedWriter().use {
                            it.write(lyrics.syncedLyrics)
                        }
                        tempFile.getContentUri(context)
                    }
                    return if (result.isSuccess) {
                        result.getOrThrow()
                    } else null
                }
            } else {
                return null
            }
        }
    }

    override suspend fun deleteAllLyrics() {
        lyricsDao.removeLyrics()
        lyricsCache.clear()
    }

    companion object {
        private const val CACHE_SIZE = 50
        private const val INSTRUMENTAL_IDENTIFIER_MAX_LENGTH = 50
        private const val DEFAULT_INSTRUMENTAL_IDENTIFIER = "[Instrumental]"
        private const val INSTRUMENTAL_TRACK_IDENTIFIERS = "instrumental_track_identifiers"
        private const val MARK_INSTRUMENTAL_BY_TITLE = "mark_instrumental_tracks_by_title"
    }
}