package com.mardous.booming.data.remote.canvas

import com.mardous.booming.data.remote.canvas.model.CanvasResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.encodeURLParameter

class CanvasService(private val client: HttpClient) {
    suspend fun canvas(artistName: String, title: String) =
        client.get("https://booming-music-api.vercel.app/api/canvas") {
            url { encodedParameters.append("q", "$artistName $title".encodeURLParameter()) }
        }.body<CanvasResult>()
}