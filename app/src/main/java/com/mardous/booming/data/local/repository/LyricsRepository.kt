package com.mardous.booming.data.local.repository

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import com.mardous.booming.data.local.EditTarget
import com.mardous.booming.data.local.MetadataReader
import com.mardous.booming.data.local.MetadataWriter
import com.mardous.booming.data.local.lyrics.lrc.LrcLyricsParser
import com.mardous.booming.data.local.lyrics.ttml.TtmlLyricsParser
import com.mardous.booming.data.local.room.LyricsDao
import com.mardous.booming.data.local.room.LyricsEntity
import com.mardous.booming.data.model.Song
import com.mardous.booming.data.model.lyrics.LyricsFile
import com.mardous.booming.data.model.lyrics.LyricsSource
import com.mardous.booming.data.model.lyrics.RawLyrics
import com.mardous.booming.data.model.lyrics.SyncedLyrics
import com.mardous.booming.data.remote.lyrics.LyricsDownloadService
import com.mardous.booming.extensions.hasR
import com.mardous.booming.extensions.media.isArtistNameUnknown
import com.mardous.booming.util.Preferences.requireString
import org.mozilla.universalchardet.UniversalDetector
import java.io.BufferedInputStream
import java.io.File
import java.io.IOException
import java.nio.charset.Charset
import java.util.regex.Pattern

interface LyricsRepository {
    suspend fun parseRawLyrics(song: Song, rawLyrics: RawLyrics): SyncedLyrics?

    suspend fun fileLyrics(song: Song): RawLyrics.File?
    suspend fun embeddedLyrics(song: Song): RawLyrics.Embedded?
    suspend fun storedLyrics(song: Song, allowDownload: Boolean): RawLyrics.Stored?
    suspend fun downloadLyrics(song: Song, searchTitle: String, searchArtist: String): RawLyrics.Remote?

    suspend fun saveLyrics(
        song: Song,
        originalLyricsBySource: Map<LyricsSource, RawLyrics?>,
        newContentBySource: Map<LyricsSource, String>
    ): Boolean?

    suspend fun writableUris(song: Song): List<Uri>
    suspend fun deleteAllLyrics()
}

class RealLyricsRepository(
    private val context: Context,
    private val preferences: SharedPreferences,
    private val lyricsDownloadService: LyricsDownloadService,
    private val lyricsDao: LyricsDao
) : LyricsRepository {

    private val charsetDetector = UniversalDetector()

    private val lrcLyricsParser = LrcLyricsParser()
    private val ttmlLyricsParser = TtmlLyricsParser()

    private val lyricsParsers = listOf(lrcLyricsParser, ttmlLyricsParser)

    override suspend fun parseRawLyrics(song: Song, rawLyrics: RawLyrics): SyncedLyrics? {
        val ignoreBlankLines = preferences.getBoolean(IGNORE_BLANK_LINES, false)
        try {
            return when (rawLyrics) {
                is RawLyrics.File -> {
                    if (rawLyrics.lyrics.isNotEmpty()) {
                        lyricsParsers.firstOrNull { it.handles(rawLyrics.file) }
                            ?.parse(rawLyrics.lyrics, song.duration, ignoreBlankLines)
                    } else null
                }

                is RawLyrics.Embedded -> {
                    if (!rawLyrics.lyrics.isNullOrEmpty()) {
                        lyricsParsers.firstOrNull { it.handles(rawLyrics.lyrics) }
                            ?.parse(rawLyrics.lyrics, song.duration, ignoreBlankLines)
                    } else null
                }

                is RawLyrics.Stored -> {
                    if (!rawLyrics.lyrics.isNullOrEmpty()) {
                        lyricsParsers.firstOrNull { it.handles(rawLyrics.lyrics) }
                            ?.parse(rawLyrics.lyrics, song.duration, ignoreBlankLines)
                            ?.copy(provider = rawLyrics.provider)
                    } else null
                }

                else -> null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Couldn't parse lyrics for song ${song.data}", e)
        }
        return null
    }

    override suspend fun fileLyrics(song: Song): RawLyrics.File? {
        try {
            val preferredFormatValue =
                preferences.requireString("preferred_lyrics_file_format", "ttml")
            val preferredFormat =
                LyricsFile.Format.entries.firstOrNull { it.value == preferredFormatValue }

            val rawLyricsList = mutableListOf<RawLyrics.File>()
            for (file in findLyricsFiles(song)) {
                val actualFile = File(file.path)
                val lyrics = runCatching {
                    actualFile.inputStream().buffered().use { stream ->
                        val charset = detectEncoding(stream)
                        stream.reader(charset).use { it.readText() }
                    }
                }.getOrNull() ?: continue

                if (lyrics.isNotEmpty()) {
                    val rawLyrics = RawLyrics.File(file, lyrics)
                    if (file.format == preferredFormat) {
                        return rawLyrics
                    }
                    rawLyricsList.add(rawLyrics)
                }
            }

            return rawLyricsList.firstOrNull()
        } catch (e: Exception) {
            Log.e(TAG, "Couldn't find/read lyrics files for song ${song.data}", e)
        }
        return null
    }

    override suspend fun embeddedLyrics(song: Song): RawLyrics.Embedded? {
        if (song.id != Song.emptySong.id) {
            try {
                val metadataReader = MetadataReader(song.uri)
                val lyrics = metadataReader.value(MetadataReader.LYRICS)
                return RawLyrics.Embedded(lyrics)
            } catch (e: Exception) {
                Log.e(TAG, "Couldn't read embedded lyrics for song ${song.data}", e)
            }
        }
        return null
    }

    override suspend fun storedLyrics(song: Song, allowDownload: Boolean): RawLyrics.Stored? {
        if (song.id != Song.emptySong.id) try {
            val storedLyrics = lyricsDao.getLyrics(song.id)
            if (storedLyrics == null && allowDownload) {
                val storableLyrics = lyricsDownloadService.remoteLyrics(song)
                    .prepareToStore()
                if (storableLyrics != null) {
                    if (storableLyrics.instrumental) {
                        lyricsDao.insertLyrics(
                            LyricsEntity(song.id, instrumental = true)
                        )
                    } else {
                        lyricsDao.insertLyrics(
                            LyricsEntity(
                                id = song.id,
                                lyrics = storableLyrics.lyrics,
                                provider = storableLyrics.provider
                            )
                        )
                    }
                    return storableLyrics
                }
            } else if (storedLyrics != null) {
                return RawLyrics.Stored(
                    lyrics = storedLyrics.lyrics,
                    provider = storedLyrics.provider,
                    instrumental = storedLyrics.instrumental
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Couldn't fetch/download lyrics for song ${song.data}", e)
        }
        return null
    }

    override suspend fun downloadLyrics(
        song: Song,
        searchTitle: String,
        searchArtist: String
    ): RawLyrics.Remote? {
        if (song.id == Song.emptySong.id || searchArtist.isArtistNameUnknown()) {
            return null
        }
        return try {
            lyricsDownloadService.remoteLyrics(song, searchTitle, searchArtist)
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun saveLyrics(
        song: Song,
        originalLyricsBySource: Map<LyricsSource, RawLyrics?>,
        newContentBySource: Map<LyricsSource, String>
    ): Boolean? {
        try {
            val pathWithoutExtension = song.data.removeRange(
                song.data.lastIndexOf('.'),
                song.data.length
            )

            val editedLyrics = newContentBySource.mapNotNull {
                val originalLyrics = originalLyricsBySource[it.key] ?: when (it.key) {
                    LyricsSource.Embedded -> RawLyrics.Embedded(null)
                    LyricsSource.Downloaded -> RawLyrics.Stored()
                    LyricsSource.File -> RawLyrics.File(
                        file = LyricsFile("$pathWithoutExtension.lrc", LyricsFile.Format.LRC),
                        lyrics = ""
                    )
                }
                if (originalLyrics.lyrics != it.value) {
                    RawLyrics.Edited(
                        originalLyrics = originalLyrics,
                        newContent = it.value
                    )
                } else null
            }

            if (editedLyrics.isEmpty()) {
                return null
            }

            return editedLyrics.all {
                when (it.originalLyrics) {
                    is RawLyrics.Embedded -> {
                        runCatching {
                            val metadataWriter = MetadataWriter()
                            metadataWriter.propertyMap(hashMapOf(MetadataReader.LYRICS to it.newContent))
                            metadataWriter.write(this.context, EditTarget.song(song)).isSuccess
                        }.getOrDefault(false)
                    }

                    is RawLyrics.Stored -> {
                        runCatching {
                            lyricsDao.insertLyrics(
                                LyricsEntity(
                                    id = song.id,
                                    lyrics = it.newContent,
                                    provider = it.newContentProvider,
                                    instrumental = it.instrumental
                                )
                            )
                            true
                        }.getOrDefault(false)
                    }

                    else -> false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error while saving lyrics for song ${song.data}", e)
        }
        return false
    }

    override suspend fun writableUris(song: Song): List<Uri> {
        if (hasR()) {
            return listOf(song.uri).filterNot { it == Uri.EMPTY }
        }
        return emptyList()
    }

    override suspend fun deleteAllLyrics() {
        lyricsDao.removeLyrics()
    }

    private fun findLyricsFiles(song: Song): List<LyricsFile> {
        val songFile = File(song.data)
        val parentDir = songFile.parentFile ?: return emptyList()

        val baseNames = listOf(
            songFile.nameWithoutExtension,
            "${song.artistName} - ${song.title}"
        ).filter { it.isNotBlank() }.map { Pattern.quote(it) }

        val patterns = baseNames.map { base ->
            Regex(".*$base.*\\.(lrc|ttml)", RegexOption.IGNORE_CASE)
        }

        return parentDir.listFiles()
            ?.filter { file -> file.isFile && patterns.any { it.matches(file.name) } }
            ?.mapNotNull { file ->
                val extension = file.extension.lowercase()
                LyricsFile.Format.entries.firstOrNull { it.value == extension }?.let { format ->
                    LyricsFile(file.absolutePath, format)
                }
            }
            .orEmpty()
    }

    private fun detectEncoding(bis: BufferedInputStream): Charset {
        return if (preferences.getBoolean(FORCE_UTF_8_ENCODING, true)) {
            Charsets.UTF_8
        } else try {
            charsetDetector.reset()
            bis.mark(BUFFER_SIZE)

            val buf = ByteArray(BUFFER_SIZE)
            var nread: Int
            while ((bis.read(buf).also { nread = it }) > 0 && !charsetDetector.isDone) {
                charsetDetector.handleData(buf, 0, nread)
            }

            charsetDetector.dataEnd()
            charsetDetector.detectedCharset?.let {
                Charset.forName(it)
            } ?: Charsets.UTF_8
        } catch (e: IOException) {
            Log.e(TAG, "Couldn't detect lyrics file encoding", e)
            Charsets.UTF_8
        } finally {
            bis.reset()
            charsetDetector.reset()
        }
    }

    companion object {
        private const val TAG = "LyricsRepository"

        private const val BUFFER_SIZE = 4096
        private const val FORCE_UTF_8_ENCODING = "force_utf8_encoding_for_lyrics"
        private const val IGNORE_BLANK_LINES = "ignore_blank_lines_in_lyrics"
    }
}