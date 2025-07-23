package com.mardous.booming.lyrics

import android.net.Uri
import java.io.File

class LyricsFile(val file: File, val format: Format) {
    constructor(file: File, format: String) : this(
        file,
        Format.entries.first { it.value == format })

    enum class Format(val value: String) {
        TTML("ttml"),
        LRC("lrc")
    }

    companion object {
        fun isSupportedFormat(uri: Uri) = uri.path?.let { path ->
            Format.entries.any { path.endsWith(".${it.value}") }
        } == true
    }
}