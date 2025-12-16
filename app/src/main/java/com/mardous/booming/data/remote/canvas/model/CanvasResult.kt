package com.mardous.booming.data.remote.canvas.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class CanvasResult(
    @SerialName("ok")
    val success: Boolean,
    @SerialName("data")
    val canvases: Canvases
)

@Serializable
class Canvases(
    @SerialName("canvasesList")
    val data: List<CanvasData>
)

@Serializable
class CanvasData(
    val trackUri: String,
    val canvasUrl: String
)