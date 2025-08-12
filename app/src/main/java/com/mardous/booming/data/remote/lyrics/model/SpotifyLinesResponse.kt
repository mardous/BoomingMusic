package com.mardous.booming.data.remote.lyrics.model

import kotlinx.serialization.Serializable

@Serializable
class SyncedLinesResponse(val error: Boolean, val lines: List<Line>) {
    @Serializable
    class Line(val timeTag: String?, val words: String)
}