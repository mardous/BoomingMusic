package com.mardous.booming.data.remote.lastfm.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class LastFmUserResponse(
    val user: LastFmUser
)

@Serializable
data class LastFmUser(
    val name: String,
    @SerialName("realname")
    val realName: String,
    val url: String,
)