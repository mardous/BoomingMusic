package com.mardous.booming.data.model.lyrics

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
class LyricsFile(
    val path: String,
    val format: Format
) : Parcelable {
    enum class Format(val value: String) {
        TTML("ttml"),
        LRC("lrc")
    }
}