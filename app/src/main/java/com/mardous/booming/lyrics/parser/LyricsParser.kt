package com.mardous.booming.lyrics.parser

import com.mardous.booming.lyrics.Lyrics
import com.mardous.booming.lyrics.LyricsFile
import java.io.IOException
import java.io.Reader

interface LyricsParser {

    fun parse(file: LyricsFile): Lyrics? = try {
        parse(reader = file.file.reader())
    } catch (e: IOException) {
        e.printStackTrace()
        null
    }

    fun parse(input: String): Lyrics? =
        if (input.isNotBlank()) parse(input.reader()) else null

    fun parse(reader: Reader): Lyrics?

    fun handles(file: LyricsFile): Boolean

    fun handles(input: String): Boolean =
        if (input.isNotBlank()) handles(input.reader()) else false

    fun handles(reader: Reader): Boolean
}

fun List<Lyrics.Line>.adjustLines(length: Long): List<Lyrics.Line> {
    return if (isNotEmpty()) {
        toMutableList().also { newList ->
            newList.sortBy { it.startAt }

            // Update durations
            for (i in 0 until lastIndex) {
                newList[i] = newList[i].copy(
                    durationMillis = newList[i + 1].startAt - newList[i].startAt,
                )
            }
            if (length != -1L) {
                val last = this.last()
                newList[lastIndex] = last.copy(
                    durationMillis = (length - last.startAt).takeIf { it > 0L }
                        ?: Long.MAX_VALUE,
                )
            } else {
                newList[lastIndex] = newList.last().copy(
                    durationMillis = Long.MAX_VALUE,
                )
            }
        }.distinctBy { it.id }
    } else this
}