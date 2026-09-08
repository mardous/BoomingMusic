package com.mardous.booming.data.model.lyrics

import com.mardous.booming.R

enum class LyricsSource(val titleRes: Int) {
    Embedded(R.string.embedded_lyrics),
    Downloaded(R.string.downloaded_lyrics),
    File(R.string.file_lyrics)
}

enum class LyricsMode(val titleRes: Int) {
    Plain(R.string.plain_lyrics),
    Synced(R.string.synced_lyrics)
}
