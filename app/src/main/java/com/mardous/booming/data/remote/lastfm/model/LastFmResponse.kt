package com.mardous.booming.data.remote.lastfm.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonTransformingSerializer

@Serializable
data class NowPlayingResponse(
    val nowplaying: NowPlayingData
)

@Serializable
data class NowPlayingData(
    val track: CorrectableField,
    val artist: CorrectableField,
    val album: CorrectableField,
    val albumArtist: CorrectableField,
    val ignoredMessage: IgnoredMessage
)

@Serializable
data class ScrobbleResponse(
    val scrobbles: ScrobbleContainer
)

@Serializable
data class ScrobbleContainer(
    @SerialName("@attr")
    val attr: ScrobbleAttributes,
    @Serializable(with = ScrobbleListSerializer::class)
    val scrobble: List<ScrobbleData>
)

@Serializable
data class ScrobbleAttributes(
    val accepted: Int,
    val ignored: Int
)

@Serializable
data class ScrobbleData(
    val track: CorrectableField,
    val artist: CorrectableField,
    val album: CorrectableField,
    val albumArtist: CorrectableField,
    val timestamp: String,
    val ignoredMessage: IgnoredMessage
)

@Serializable
data class CorrectableField(
    val corrected: String,
    @SerialName("#text")
    val text: String
)

@Serializable
data class IgnoredMessage(
    val code: String,
    @SerialName("#text")
    val text: String
)

object ScrobbleListSerializer :
    JsonTransformingSerializer<List<ScrobbleData>>(ListSerializer(ScrobbleData.serializer())) {
    override fun transformDeserialize(element: JsonElement): JsonElement {
        return element as? JsonArray ?: JsonArray(listOf(element))
    }
}