package com.mardous.booming.data

import com.mardous.booming.data.model.lyrics.Lyrics
import com.mardous.booming.data.model.lyrics.LyricsFile
import java.io.IOException
import java.io.Reader

interface LyricsParser {

    fun parse(file: LyricsFile, trackLength: Long): Lyrics? = try {
        parse(reader = file.file.reader(), trackLength)
    } catch (e: IOException) {
        e.printStackTrace()
        null
    }

    fun parse(input: String, trackLength: Long): Lyrics? =
        if (input.isNotBlank()) parse(input.reader(), trackLength) else null

    fun parse(reader: Reader, trackLength: Long): Lyrics?

    fun handles(file: LyricsFile): Boolean

    fun handles(input: String): Boolean =
        if (input.isNotBlank()) handles(input.reader()) else false

    fun handles(reader: Reader): Boolean
}