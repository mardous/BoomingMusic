package com.mardous.booming.data.remote.listenbrainz

import com.mardous.booming.data.remote.listenbrainz.model.ListenBrainzResponse
import com.mardous.booming.data.remote.listenbrainz.model.ListenBrainzSubmission
import com.mardous.booming.data.remote.listenbrainz.model.ListenBrainzTokenValidationResponse
import com.mardous.booming.util.Constants.LISTENBRAINZ_API_URL
import com.mardous.booming.util.Constants.USER_AGENT
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.userAgent

class ListenBrainzService(private val client: HttpClient) {

    suspend fun validateToken(token: String): ListenBrainzTokenValidationResponse {
        return client.get("${LISTENBRAINZ_API_URL}validate-token") {
            userAgent(USER_AGENT)
            parameter("token", token)
        }.body()
    }

    suspend fun submitListen(token: String, submission: ListenBrainzSubmission): ListenBrainzResponse {
        val response = client.post("${LISTENBRAINZ_API_URL}submit-listens") {
            userAgent(USER_AGENT)
            header("Authorization", "Token $token")
            contentType(ContentType.Application.Json)
            setBody(submission)
        }
        return response.body()
    }
}
