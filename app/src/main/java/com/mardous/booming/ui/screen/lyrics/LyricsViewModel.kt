package com.mardous.booming.ui.screen.lyrics

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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.mardous.booming.core.model.task.Result
import com.mardous.booming.data.local.repository.LyricsRepository
import com.mardous.booming.data.model.Song
import com.mardous.booming.ui.screen.lyrics.LyricsViewSettings.Key
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import com.mardous.booming.ui.screen.lyrics.LyricsViewSettings.Mode as LyricsViewMode

/**
 * @author Christians M. A. (mardous)
 */
class LyricsViewModel(
    private val preferences: SharedPreferences,
    private val lyricsRepository: LyricsRepository
) : ViewModel(), OnSharedPreferenceChangeListener {

    private val silentHandler = CoroutineExceptionHandler { _, _ -> }

    private val _lyricsResult = MutableStateFlow(LyricsResult.Empty)
    val lyricsResult = _lyricsResult.asStateFlow()

    private val _playerLyricsViewSettings = MutableStateFlow(createViewSettings(LyricsViewMode.Player))
    val playerLyricsViewSettings = _playerLyricsViewSettings.asStateFlow()

    private val _fullLyricsViewSettings = MutableStateFlow(createViewSettings(LyricsViewMode.Full))
    val fullLyricsViewSettings = _fullLyricsViewSettings.asStateFlow()

    private var lyricsJob: Job? = null

    init {
        preferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onCleared() {
        lyricsJob?.cancel()
        preferences.unregisterOnSharedPreferenceChangeListener(this)
        super.onCleared()
    }

    fun getOnlineLyrics(song: Song, title: String, artist: String) = liveData(Dispatchers.IO) {
        emit(Result.Loading)
        emit(lyricsRepository.onlineLyrics(song, title, artist))
    }

    fun getAllLyrics(
        song: Song,
        allowDownload: Boolean = false,
        fromEditor: Boolean = false
    ) = liveData(Dispatchers.IO + silentHandler) {
        emit(LyricsResult(id = song.id, loading = true))
        emit(lyricsRepository.allLyrics(song, allowDownload, fromEditor))
    }

    fun getLyrics(song: Song) = liveData(Dispatchers.IO) {
        emit(lyricsRepository.embeddedLyrics(song, requirePlainText = true))
    }

    fun shareSyncedLyrics(song: Song) = liveData(Dispatchers.IO) {
        emit(lyricsRepository.shareSyncedLyrics(song))
    }

    fun getWritableUris(song: Song) = liveData(Dispatchers.IO) {
        val uris = lyricsRepository.writableUris(song)
        delay(500)
        emit(uris)
    }

    fun saveLyrics(song: Song, plainLyrics: EditableLyrics?, syncedLyrics: EditableLyrics?) =
        liveData(Dispatchers.IO) {
            val saveResult = lyricsRepository.saveLyrics(song, plainLyrics, syncedLyrics)
            if (saveResult.hasChanged && song.id == lyricsResult.value.id) {
                updateSong(song)
            }
            emit(saveResult)
        }

    fun importLyrics(song: Song, uri: Uri) = liveData(Dispatchers.IO) {
        emit(lyricsRepository.importLyrics(song, uri))
    }

    fun deleteLyrics() = viewModelScope.launch(Dispatchers.IO) {
        lyricsRepository.deleteAllLyrics()
    }

    fun importCustomFont(context: Context, uri: Uri) = liveData(Dispatchers.IO) {
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
                _lyricsResult.value = LyricsResult.Empty
            } else {
                _lyricsResult.value = LyricsResult(id = song.id, loading = true)
                val result = withContext(Dispatchers.IO + silentHandler) {
                    lyricsRepository.allLyrics(song, allowDownload = true, fromEditor = false)
                }
                if (isActive) {
                    _lyricsResult.value = result
                }
            }
        }
    }

    private fun createViewSettings(mode: LyricsViewMode): LyricsViewSettings {
        val enableSyllableLyrics = preferences.getBoolean(Key.ENABLE_SYLLABLE_LYRICS, false)
        val progressiveColoring = preferences.getBoolean(Key.PROGRESSIVE_COLORING, false)
        val gradientBackground = mode.isFull && preferences.getBoolean(Key.GRADIENT_BACKGROUND, false)
        val blurEffect = gradientBackground && preferences.getBoolean(Key.BLUR_EFFECT, false)
        val shadowEffect = gradientBackground && preferences.getBoolean(Key.SHADOW_EFFECT, false)
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
            preferences.getInt(Key.SYNCED_FONT_SIZE_PLAYER, 24)
        } else {
            preferences.getInt(Key.SYNCED_FONT_SIZE_FULL, 30)
        }
        val unsyncedFontSize = if (mode == LyricsViewMode.Player) {
            preferences.getInt(Key.UNSYNCED_FONT_SIZE_PLAYER, 16)
        } else {
            preferences.getInt(Key.UNSYNCED_FONT_SIZE_FULL, 20)
        }
        val syncedBoldFont = preferences.getBoolean(Key.SYNCED_BOLD_FONT, true)
        val syncedStyle = TextStyle(
            fontFamily = fontFamily,
            fontSize = syncedFontSize.sp,
            fontWeight = if (syncedBoldFont) FontWeight.Bold else FontWeight.Normal,
            lineHeight = (1f + (lineSpacing / 100f)).em
        )
        val unsyncedBoldFont = preferences.getBoolean(Key.UNSYNCED_BOLD_FONT, true)
        val unsyncedStyle = TextStyle(
            fontFamily = fontFamily,
            fontSize = unsyncedFontSize.sp,
            fontWeight = if (unsyncedBoldFont) FontWeight.Bold else FontWeight.Normal,
            lineHeight = (1f + (lineSpacing / 100f)).em
        )
        return LyricsViewSettings(
            mode = mode,
            isCenterCurrentLine = preferences.getBoolean(Key.CENTER_CURRENT_LINE, false),
            enableSyllableLyrics = enableSyllableLyrics,
            progressiveColoring = progressiveColoring,
            gradientBackground = gradientBackground,
            blurEffect = blurEffect,
            shadowEffect = shadowEffect,
            syncedStyle = syncedStyle,
            unsyncedStyle = unsyncedStyle
        )
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            Key.ENABLE_SYLLABLE_LYRICS,
            Key.CENTER_CURRENT_LINE,
            Key.USE_CUSTOM_FONT,
            Key.SELECTED_CUSTOM_FONT,
            Key.LINE_SPACING,
            Key.PROGRESSIVE_COLORING,
            Key.GRADIENT_BACKGROUND,
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
        }
    }
}