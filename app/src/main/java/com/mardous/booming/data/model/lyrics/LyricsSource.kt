package com.mardous.booming.data.model.lyrics

import androidx.annotation.IdRes
import androidx.annotation.StringRes
import com.mardous.booming.R

enum class LyricsSource(
    @field:IdRes
    val applicableButtonId: Int,
    @field:StringRes
    val titleRes: Int,
    @field:StringRes
    val descriptionRes: Int = 0,
    val tooltipKey: String = ""
) {
    Embedded(
        applicableButtonId = R.id.embeddedButton,
        titleRes = R.string.embedded_lyrics
    ),
    Downloaded(
        applicableButtonId = R.id.externalButton,
        titleRes = R.string.downloaded_lyrics
    ),
    File(
        applicableButtonId = R.id.externalButton,
        titleRes = R.string.external_file_lyrics,
        descriptionRes = R.string.lyrics_source_external_file,
        tooltipKey = "external_file_lyrics_tip"
    );

    val isEditable: Boolean
        get() = this != File

    val isExternalSource: Boolean
        get() = this != Embedded
}