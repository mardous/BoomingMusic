package com.mardous.booming.repository

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.mardous.booming.appContext
import com.mardous.booming.database.LyricsDao
import com.mardous.booming.database.toLyricsEntity
import com.mardous.booming.extensions.files.getContentUri
import com.mardous.booming.extensions.hasR
import com.mardous.booming.extensions.isAllowedToDownloadMetadata
import com.mardous.booming.extensions.media.isArtistNameUnknown
import com.mardous.booming.http.Result
import com.mardous.booming.http.lyrics.LyricsDownloadService
import com.mardous.booming.lyrics.LyricsFile
import com.mardous.booming.lyrics.LyricsSource
import com.mardous.booming.lyrics.parser.LrcLyricsParser
import com.mardous.booming.lyrics.parser.TtmlLyricsParser
import com.mardous.booming.model.DownloadedLyrics
import com.mardous.booming.model.Song
import com.mardous.booming.taglib.EditTarget
import com.mardous.booming.taglib.MetadataReader
import com.mardous.booming.taglib.MetadataWriter
import com.mardous.booming.viewmodels.lyrics.model.DisplayableLyrics
import com.mardous.booming.viewmodels.lyrics.model.EditableLyrics
import com.mardous.booming.viewmodels.lyrics.model.LyricsResult
import java.io.File
import java.util.regex.Pattern

interface LyricsRepository {
    suspend fun onlineLyrics(
        song: Song,
        searchTitle: String,
        searchArtist: String
    ): Result<DownloadedLyrics>

    suspend fun allLyrics(song: Song, allowDownload: Boolean, fromEditor: Boolean): LyricsResult
    suspend fun embeddedLyrics(song: Song, requirePlainText: Boolean): String?
    suspend fun saveLyrics(song: Song, plainLyrics: EditableLyrics?, syncedLyrics: EditableLyrics?): Boolean
    suspend fun saveSyncedLyrics(song: Song, lyrics: String?): Boolean
    suspend fun importLyrics(song: Song, uri: Uri): Boolean
    suspend fun findLyricsFiles(song: Song): List<LyricsFile>
    suspend fun writableUris(song: Song): List<Uri>
    suspend fun shareSyncedLyrics(song: Song): Uri?
    suspend fun deleteAllLyrics()
}

class RealLyricsRepository(
    private val context: Context,
    private val contentResolver: ContentResolver,
    private val lyricsDownloadService: LyricsDownloadService,
    private val lyricsDao: LyricsDao
) : LyricsRepository {

    private val lrcLyricsParser = LrcLyricsParser()
    private val ttmlLyricsParser = TtmlLyricsParser()

    private val lyricsParsers = listOf(lrcLyricsParser, ttmlLyricsParser)

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
            return LyricsResult(song.id)
        }

        val embeddedLyrics = embeddedLyrics(song, requirePlainText = false).orEmpty()
        val embeddedLyricsParser = lyricsParsers.firstOrNull { it.handles(embeddedLyrics) }
        val embeddedSynced = embeddedLyricsParser?.parse(embeddedLyrics)

        val fileLyrics = findLyricsFiles(song).firstNotNullOfOrNull { file ->
            lyricsParsers.firstOrNull { it.handles(file) }
                ?.parse(file)
        }
        if (fileLyrics?.hasContent == true) {
            return LyricsResult(
                id = song.id,
                plainLyrics = DisplayableLyrics(embeddedLyrics, LyricsSource.Embedded),
                syncedLyrics = DisplayableLyrics(fileLyrics, LyricsSource.File)
            )
        }

        val storedSynced = lyricsDao.getLyrics(song.id)?.let { stored ->
            lyricsParsers.firstOrNull { it.handles(stored.syncedLyrics) }
                ?.parse(stored.syncedLyrics)
        }
        if (embeddedSynced?.hasContent == true) {
            return if (fromEditor) {
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
        }

        if (storedSynced?.hasContent == true) {
            return LyricsResult(
                id = song.id,
                plainLyrics = DisplayableLyrics(embeddedLyrics, LyricsSource.Embedded),
                syncedLyrics = DisplayableLyrics(storedSynced, LyricsSource.Downloaded)
            )
        }

        if (allowDownload && appContext().isAllowedToDownloadMetadata()) {
            val downloaded = runCatching { lyricsDownloadService.getLyrics(song) }.getOrNull()
            if (downloaded?.isSynced == true) {
                val syncedData = lrcLyricsParser.parse(downloaded.syncedLyrics!!)
                if (syncedData?.hasContent == true) {
                    lyricsDao.insertLyrics(
                        song.toLyricsEntity(
                            syncedData.rawText,
                            autoDownload = true
                        )
                    )
                    return LyricsResult(
                        id = song.id,
                        plainLyrics = DisplayableLyrics(embeddedLyrics, LyricsSource.Embedded),
                        syncedLyrics = DisplayableLyrics(syncedData, LyricsSource.Downloaded)
                    )
                }
            }
        }

        return LyricsResult(
            id = song.id,
            plainLyrics = DisplayableLyrics(embeddedLyrics, LyricsSource.Embedded)
        )
    }

    override suspend fun embeddedLyrics(song: Song, requirePlainText: Boolean): String? {
        if (song.id != Song.emptySong.id) {
            val metadataReader = MetadataReader(song.mediaStoreUri)
            val lyrics = metadataReader.value(MetadataReader.LYRICS)
            if (requirePlainText && !lyrics.isNullOrBlank()) {
                val parser = lyricsParsers.firstOrNull { it.handles(lyrics) }
                val syncedData = parser?.parse(lyrics)
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
    ): Boolean {
        val savedPlain = if (plainLyrics != null) {
            val target = EditTarget.song(song)
            val metadataWriter = MetadataWriter()
            metadataWriter.propertyMap(
                propertyMap = hashMapOf(MetadataReader.LYRICS to plainLyrics.content)
            )
            metadataWriter.write(this.context, target).isNotEmpty()
        } else {
            true
        }
        val savedSynced = if (syncedLyrics != null) {
            if (syncedLyrics.source == LyricsSource.Downloaded) {
                saveSyncedLyrics(song, syncedLyrics.content)
            } else false
        } else {
            true
        }
        return savedPlain && savedSynced
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
            val parsedLyrics = parser?.parse(lyrics)
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
            return listOf(song.mediaStoreUri).filterNot { it == Uri.EMPTY }
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
    }
}