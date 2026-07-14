package com.mardous.booming.ui.screen.lyrics

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.graphics.Typeface
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.mardous.booming.core.model.lyrics.LyricsViewSettings
import com.mardous.booming.core.model.lyrics.LyricsViewSettings.BackgroundEffect
import com.mardous.booming.core.model.lyrics.LyricsViewSettings.Key
import com.mardous.booming.data.local.lyrics.InstrumentalDetector
import com.mardous.booming.data.local.repository.LyricsRepository
import com.mardous.booming.data.model.Song
import com.mardous.booming.data.model.lyrics.LyricsSource
import com.mardous.booming.data.model.lyrics.RawLyrics
import com.mardous.booming.data.model.network.NetworkFeature
import com.mardous.booming.data.model.network.NetworkFeature.Lyrics.BetterLyrics
import com.mardous.booming.data.model.network.NetworkFeature.Lyrics.LRCLib
import com.mardous.booming.data.model.network.NetworkFeature.Lyrics.Lyrically
import com.mardous.booming.extensions.media.isArtistNameUnknown
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import com.mardous.booming.core.model.lyrics.LyricsViewSettings.Mode as LyricsViewMode

/**
 * @author Christians M. A. (mardous)
 */
class LyricsViewModel(
    application: Application,
    private val preferences: SharedPreferences,
    private val repository: LyricsRepository
) : AndroidViewModel(application), OnSharedPreferenceChangeListener {

    private var instrumentalDetector: InstrumentalDetector

    private val _lyricsUiState = MutableStateFlow<LyricsUiState>(LyricsUiState.Empty(-1))
    val lyricsUiState = _lyricsUiState.asStateFlow()

    private val _lyricsEditorUiState = MutableStateFlow<LyricsEditorUiState>(LyricsEditorUiState.Disposed)
    val lyricsEditorUiState = _lyricsEditorUiState.asStateFlow()

    private val _lyricsDownloadEnabled = MutableStateFlow(isLyricsDownloadEnabled())
    val lyricsDownloadEnabled = _lyricsDownloadEnabled.asStateFlow()

    private val _saveEvent = Channel<LyricsEditorResult>(Channel.BUFFERED)
    val saveEvent = _saveEvent.receiveAsFlow()

    private val _downloadEvent = Channel<RawLyrics.Remote>(Channel.BUFFERED)
    val downloadEvent = _downloadEvent.receiveAsFlow()

    private val _permissionRequestEvent = Channel<List<Uri>>(Channel.BUFFERED)
    val permissionRequestEvent = _permissionRequestEvent.receiveAsFlow()

    private val _playerLyricsViewSettings = MutableStateFlow(createViewSettings(LyricsViewMode.Player))
    val playerLyricsViewSettings = _playerLyricsViewSettings.asStateFlow()

    private val _fullLyricsViewSettings = MutableStateFlow(createViewSettings(LyricsViewMode.Full))
    val fullLyricsViewSettings = _fullLyricsViewSettings.asStateFlow()

    private var lyricsJob: Job? = null

    init {
        instrumentalDetector = createInstrumentalDetector()
        preferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onCleared() {
        lyricsJob?.cancel()
        preferences.unregisterOnSharedPreferenceChangeListener(this)
        super.onCleared()
    }

    fun getSearchUrl(song: Song): String {
        val url = "https://lrclib.net/search/"
        val query = if (song.isArtistNameUnknown()) song.title
        else "${song.artistName} ${song.title}"
        return url + Uri.encode(query)
    }

    fun loadEditorContent(song: Song) = viewModelScope.launch(IO) {
        _lyricsEditorUiState.update {
            LyricsEditorUiState.Visible(isLoading = true)
        }

        val lyrics = getEditorLyricsBySources(song, LyricsSource.entries)
        _lyricsEditorUiState.value = LyricsEditorUiState.Visible(
            isLoading = false,
            lyrics = lyrics
        )
    }

    fun disposeEditorContent() = viewModelScope.launch(IO) {
        _lyricsEditorUiState.value = LyricsEditorUiState.Disposed
    }

    fun saveLyrics(song: Song, newLyrics: Map<LyricsSource, String>) = viewModelScope.launch(IO) {
        val uiState = _lyricsEditorUiState.updateAndGet {
            if (it is LyricsEditorUiState.Visible) {
                it.copy(isLoading = true)
            } else it
        }
        if (uiState is LyricsEditorUiState.Visible) {
            val event = when (val result = repository.saveLyrics(song, uiState.lyrics, newLyrics)) {
                null -> LyricsEditorResult.NoChanges
                else -> if (result) LyricsEditorResult.Success else LyricsEditorResult.Failed
            }

            _saveEvent.send(event)

            if (event == LyricsEditorResult.Success) {
                // Lyrics need to be updated to avoid unnecessary save operations
                val newLyrics = getEditorLyricsBySources(song, newLyrics.keys.toList())
                _lyricsEditorUiState.value = uiState.copy(isLoading = false, lyrics = newLyrics)

                // Update current song lyrics if necessary
                if (song.id == lyricsUiState.value.id) {
                    updateSong(song)
                }
            } else {
                _lyricsEditorUiState.value = uiState.copy(isLoading = false)
            }
        }
    }

    fun downloadLyrics(song: Song, title: String, artist: String) =
        viewModelScope.launch(IO) {
            val uiState = _lyricsEditorUiState.updateAndGet {
                if (it is LyricsEditorUiState.Visible) {
                    it.copy(isLoading = true)
                } else it
            }
            if (uiState is LyricsEditorUiState.Visible) {
                val onlineLyrics = repository.downloadLyrics(song, title, artist)
                if (onlineLyrics != null) {
                    _downloadEvent.send(onlineLyrics)
                } else {
                    _downloadEvent.send(RawLyrics.Remote())
                }
                _lyricsEditorUiState.value = uiState.copy(isLoading = false)
            }
        }

    fun preparePermissionRequest(song: Song) = viewModelScope.launch(IO) {
        _permissionRequestEvent.send(repository.writableUris(song))
    }

    fun deleteLyrics() = viewModelScope.launch(IO) {
        repository.deleteAllLyrics()
    }

    fun importCustomFont(context: Context, uri: Uri) = liveData(IO) {
        try {
            val fontsDir = File(context.filesDir, "fonts").apply { mkdirs() }
            val fileName = context.contentResolver.query(uri, null, null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1) cursor.getString(nameIndex) else null
                    } else null
                } ?: "custom_font_${System.currentTimeMillis()}.ttf"

            val outFile = File(fontsDir, fileName)

            var isValid = fileName.lowercase().endsWith(".ttf") || fileName.lowercase().endsWith(".otf")
            if (isValid) {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    val header = ByteArray(4)
                    if (input.read(header) == 4) {
                        val hex = header.joinToString("") { "%02X".format(it) }
                        isValid = hex == "00010000" || hex == "4F54544F" // TTF or OTF
                    }
                }
            }

            if (isValid) {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    outFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                preferences.edit(commit = true) {
                    putBoolean(Key.USE_CUSTOM_FONT, true)
                    putString(Key.SELECTED_CUSTOM_FONT, outFile.absolutePath)
                }
            } else {
                outFile.delete()
            }

            emit(isValid && outFile.length() > 0)
        } catch (e: Exception) {
            e.printStackTrace()
            emit(false)
        }
    }

    fun updateSong(song: Song) {
        lyricsJob?.cancel()
        lyricsJob = viewModelScope.launch {
            if (song == Song.emptySong) {
                _lyricsUiState.value = LyricsUiState.Empty(song.id)
            } else {
                _lyricsUiState.value = LyricsUiState.Loading(song.id)

                val lyricsState = getBestLyricsFromSources(
                    song = song,
                    sources = listOf(
                        LyricsSource.File,
                        LyricsSource.Embedded,
                        LyricsSource.Downloaded
                    )
                )
                if (isActive) {
                    _lyricsUiState.value = lyricsState
                }
            }
        }
    }

    private suspend fun getEditorLyricsBySources(
        song: Song,
        sources: List<LyricsSource>
    ) = sources.associateWith { source ->
        when (source) {
            LyricsSource.Downloaded -> repository.storedLyrics(song, false)
            LyricsSource.Embedded -> repository.embeddedLyrics(song)
            LyricsSource.File -> repository.fileLyrics(song)
        }
    }

    private suspend fun getBestLyricsFromSources(
        song: Song,
        sources: List<LyricsSource>
    ): LyricsUiState = withContext(IO) {
        var plainLyrics: String? = null
        if (instrumentalDetector.byTitle(song.title)) {
            return@withContext LyricsUiState.Instrumental(song.id)
        }
        for (source in sources) {
            when (source) {
                LyricsSource.File -> {
                    val fileLyrics = repository.fileLyrics(song)
                    if (fileLyrics != null) {
                        val lyrics = repository.parseRawLyrics(song, fileLyrics)
                        if (lyrics?.hasContent == true) {
                            return@withContext LyricsUiState.Synced(song.id, lyrics)
                        }
                    }
                }

                LyricsSource.Embedded -> {
                    val embeddedLyrics = repository.embeddedLyrics(song)
                    if (embeddedLyrics != null) {
                        if (instrumentalDetector.byLyrics(embeddedLyrics.lyrics)) {
                            return@withContext LyricsUiState.Instrumental(song.id)
                        }
                        val lyrics = repository.parseRawLyrics(song, embeddedLyrics)
                        if (lyrics?.hasContent == true) {
                            return@withContext LyricsUiState.Synced(song.id, lyrics)
                        } else {
                            if (plainLyrics.isNullOrEmpty()) {
                                plainLyrics = embeddedLyrics.lyrics
                            }
                        }
                    }
                }

                LyricsSource.Downloaded -> {
                    val downloadedLyrics = repository.storedLyrics(song, true)
                    if (downloadedLyrics != null) {
                        if (downloadedLyrics.instrumental) {
                            return@withContext LyricsUiState.Instrumental(song.id)
                        }
                        val lyrics = repository.parseRawLyrics(song, downloadedLyrics)
                        if (lyrics?.hasContent == true) {
                            return@withContext LyricsUiState.Synced(song.id, lyrics)
                        } else {
                            if (plainLyrics.isNullOrEmpty()) {
                                plainLyrics = downloadedLyrics.lyrics
                            }
                        }
                    }
                }
            }
        }
        if (!plainLyrics.isNullOrEmpty()) {
            return@withContext LyricsUiState.Plain(song.id, plainLyrics)
        }
        return@withContext LyricsUiState.Empty(song.id)
    }

    private fun createViewSettings(mode: LyricsViewMode): LyricsViewSettings {
        val background: BackgroundEffect =
            if (!mode.isFull) {
                BackgroundEffect.None
            } else when (preferences.getString(Key.BACKGROUND_EFFECT, null)) {
                "gradient" -> BackgroundEffect.Gradient
                "blur" -> BackgroundEffect.Blur
                else -> BackgroundEffect.None
            }
        val enableSyllableLyrics = preferences.getBoolean(Key.ENABLE_SYLLABLE_LYRICS, false)
        val enableKaraokeStyle = preferences.getBoolean(Key.ENABLE_KARAOKE_STYLE, false)
        val progressiveColoring = preferences.getBoolean(Key.PROGRESSIVE_COLORING, false)
        val showTranslation = preferences.getBoolean(Key.SHOW_TRANSLATION, true)
        val showTransliteration = preferences.getBoolean(Key.SHOW_TRANSLITERATION, false)
        val resumeOnSeek = preferences.getBoolean(Key.RESUME_ON_SEEK, false)
        val blurEffect = !background.isNone && preferences.getBoolean(Key.BLUR_EFFECT, false)
        val shadowEffect = !background.isNone && preferences.getBoolean(Key.SHADOW_EFFECT, false)
        val fontFamily: FontFamily = if (preferences.getBoolean(Key.USE_CUSTOM_FONT, false)) {
            try {
                preferences.getString(Key.SELECTED_CUSTOM_FONT, null)
                    ?.let { FontFamily(Typeface.createFromFile(it)) }
                    ?: FontFamily.Default
            } catch (_: Exception) {
                preferences.edit {
                    remove(Key.SELECTED_CUSTOM_FONT)
                }
                FontFamily.Default
            }
        } else {
            FontFamily.Default
        }
        val lineSpacing = preferences.getInt(Key.LINE_SPACING, 40)
        val syncedFontSize = if (mode == LyricsViewMode.Player) {
            preferences.getInt(Key.SYNCED_FONT_SIZE_PLAYER, 20)
        } else {
            preferences.getInt(Key.SYNCED_FONT_SIZE_FULL, 24)
        }
        val unsyncedFontSize = if (mode == LyricsViewMode.Player) {
            preferences.getInt(Key.UNSYNCED_FONT_SIZE_PLAYER, 16)
        } else {
            preferences.getInt(Key.UNSYNCED_FONT_SIZE_FULL, 20)
        }
        val syncedBoldFont = preferences.getBoolean(Key.SYNCED_BOLD_FONT, false)
        val syncedStyle = TextStyle(
            fontFamily = fontFamily,
            fontSize = syncedFontSize.sp,
            fontWeight = if (syncedBoldFont) FontWeight.Bold else FontWeight.Normal,
            lineHeight = (1f + (lineSpacing / 100f)).em
        )
        val unsyncedBoldFont = preferences.getBoolean(Key.UNSYNCED_BOLD_FONT, false)
        val unsyncedStyle = TextStyle(
            fontFamily = fontFamily,
            fontSize = unsyncedFontSize.sp,
            fontWeight = if (unsyncedBoldFont) FontWeight.Bold else FontWeight.Normal,
            lineHeight = (1f + (lineSpacing / 100f)).em
        )
        return LyricsViewSettings(
            mode = mode,
            isCenterCurrentLine = preferences.getBoolean(Key.CENTER_CURRENT_LINE, false),
            isCenterHorizontally = preferences.getBoolean(Key.CENTER_HORIZONTALLY, false),
            enableSyllableLyrics = enableSyllableLyrics,
            enableKaraokeStyle = enableKaraokeStyle,
            progressiveColoring = progressiveColoring,
            backgroundEffect = background,
            blurEffect = blurEffect,
            shadowEffect = shadowEffect,
            showTranslation = showTranslation,
            showTransliteration = showTransliteration,
            resumeOnSeek = resumeOnSeek,
            syncedStyle = syncedStyle,
            unsyncedStyle = unsyncedStyle,
            lineSpacing = ((lineSpacing / 2) + 8).coerceIn(8, 48)
        )
    }

    private fun isLyricsDownloadEnabled(): Boolean {
        return BetterLyrics.isEnabled || Lyrically.isEnabled || LRCLib.isEnabled
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            Key.ENABLE_SYLLABLE_LYRICS,
            Key.ENABLE_KARAOKE_STYLE,
            Key.CENTER_CURRENT_LINE,
            Key.CENTER_HORIZONTALLY,
            Key.USE_CUSTOM_FONT,
            Key.SELECTED_CUSTOM_FONT,
            Key.LINE_SPACING,
            Key.PROGRESSIVE_COLORING,
            Key.SHOW_TRANSLATION,
            Key.SHOW_TRANSLITERATION,
            Key.RESUME_ON_SEEK,
            Key.BACKGROUND_EFFECT,
            Key.BLUR_EFFECT,
            Key.SHADOW_EFFECT,
            Key.SYNCED_BOLD_FONT,
            Key.UNSYNCED_BOLD_FONT -> {
                _playerLyricsViewSettings.value = createViewSettings(LyricsViewMode.Player)
                _fullLyricsViewSettings.value = createViewSettings(LyricsViewMode.Full)
            }
            Key.SYNCED_FONT_SIZE_PLAYER,
            Key.UNSYNCED_FONT_SIZE_PLAYER -> {
                _playerLyricsViewSettings.value = createViewSettings(LyricsViewMode.Player)
            }
            Key.SYNCED_FONT_SIZE_FULL,
            Key.UNSYNCED_FONT_SIZE_FULL -> {
                _fullLyricsViewSettings.value = createViewSettings(LyricsViewMode.Full)
            }
            NetworkFeature.NETWORK_FEATURES_KEY,
            NetworkFeature.BETTERLYRICS_ENABLED_KEY,
            NetworkFeature.LYRICALLY_ENABLED_KEY,
            NetworkFeature.LRCLIB_ENABLED_KEY -> {
                _lyricsDownloadEnabled.value = isLyricsDownloadEnabled()
            }
            INSTRUMENTAL_TRACK_IDENTIFIERS,
            MARK_INSTRUMENTAL_BY_TITLE -> {
                instrumentalDetector = createInstrumentalDetector()
            }
        }
    }

    private fun createInstrumentalDetector() =
        InstrumentalDetector(
            identifiers = preferences.getString(INSTRUMENTAL_TRACK_IDENTIFIERS, null)
                ?.split(",").orEmpty().toSet(),
            markByTitle = preferences.getBoolean(MARK_INSTRUMENTAL_BY_TITLE, false),
            maxLength = INSTRUMENTAL_IDENTIFIER_MAX_LENGTH
        )

    companion object {
        private const val INSTRUMENTAL_IDENTIFIER_MAX_LENGTH = 50
        private const val INSTRUMENTAL_TRACK_IDENTIFIERS = "instrumental_track_identifiers"
        private const val MARK_INSTRUMENTAL_BY_TITLE = "mark_instrumental_tracks_by_title"
    }
}