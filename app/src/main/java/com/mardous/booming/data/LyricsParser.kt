package com.mardous.booming.data

import com.mardous.booming.data.model.lyrics.SyncedLyrics
import com.mardous.booming.data.model.lyrics.LyricsFile
import java.io.Reader

interface LyricsParser {

    fun parse(input: String, trackLength: Long, ignoreBlankLines: Boolean): SyncedLyrics? =
        if (input.isNotBlank()) input.reader().use { parse(it, trackLength, ignoreBlankLines) } else null

    fun parse(reader: Reader, trackLength: Long, ignoreBlankLines: Boolean): SyncedLyrics?

    fun handles(file: LyricsFile): Boolean

    fun handles(input: String): Boolean =
        if (input.isNotBlank()) input.reader().use { handles(it) } else false

    fun handles(reader: Reader): Boolean
}