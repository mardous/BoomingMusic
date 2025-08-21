package com.mardous.booming.ui.screen.lyrics

import androidx.compose.runtime.*
import com.mardous.booming.data.model.lyrics.Lyrics
import kotlin.math.abs

@Stable
class LyricsViewState(val lyrics: Lyrics?) {

    var position by mutableLongStateOf(0L)
        private set

    internal var currentLineIndex by mutableIntStateOf(-1)
        private set

    private var previousLineIndex by mutableIntStateOf(-1)

    internal var currentWordIndex by mutableIntStateOf(-1)
        private set

    internal var currentBackgroundIndex by mutableIntStateOf(-1)
        private set

    private var shouldCrossfade by mutableStateOf(false)

    fun updatePosition(newPosition: Long) {
        position = newPosition

        val newLineIndex = findLineIndexAt(position)
        val lineJump = abs(newLineIndex - currentLineIndex)

        shouldCrossfade = lineJump > 1

        previousLineIndex = if (lineJump <= 1) currentLineIndex else -1
        currentLineIndex = newLineIndex
        currentWordIndex = findWordIndexAt(position, currentLineIndex)
        currentBackgroundIndex = findBackgroundIndexAt(position,  currentLineIndex)
    }

    private fun findLineIndexAt(position: Long): Int {
        if (position < 0 || lyrics == null) return -1
        val lines = lyrics.lines
        for (i in lines.lastIndex downTo 0) {
            if (position >= lines[i].startAt) {
                return i
            }
        }
        return -1
    }

    private fun findWordIndexAt(position: Long, lineIndex: Int): Int {
        if (lyrics == null || lineIndex !in lyrics.lines.indices) return -1
        val words = lyrics.lines[lineIndex].main
        for (i in words.indices) {
            if (position < words[i].startMillis) {
                return i - 1
            }
        }
        return words.lastIndex
    }

    private fun findBackgroundIndexAt(position: Long, lineIndex: Int): Int {
        if (lyrics == null || lineIndex !in lyrics.lines.indices) return -1
        val line = lyrics.lines[lineIndex]
        if (!line.hasBackground) return -1
        val backgrounds = line.background
        for (i in backgrounds.indices) {
            if (position < backgrounds[i].startMillis) {
                return i - 1
            }
        }
        return backgrounds.lastIndex
    }
}

