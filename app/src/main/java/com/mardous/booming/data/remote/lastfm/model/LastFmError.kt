package com.mardous.booming.data.remote.lastfm.model

import kotlinx.serialization.Serializable

@Serializable
class LastFmError(
    val message: String,
    val error: Int
)