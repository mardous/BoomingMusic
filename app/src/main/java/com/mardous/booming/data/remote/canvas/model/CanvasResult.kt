package com.mardous.booming.data.remote.canvas.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class CanvasResult(
    @SerialName("videoUrl")
    val url: String
)