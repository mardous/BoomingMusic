package com.mardous.booming.data.local.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import com.mardous.booming.data.model.Song
import com.mardous.booming.data.model.network.lastfm.LastFmFailure
import com.mardous.booming.data.model.network.lastfm.LastFmLoginState
import com.mardous.booming.data.model.network.lastfm.LastFmResult
import com.mardous.booming.data.remote.deezer.DeezerService
import com.mardous.booming.data.remote.deezer.model.DeezerAlbum
import com.mardous.booming.data.remote.deezer.model.DeezerArtist
import com.mardous.booming.data.remote.deezer.model.DeezerTrack
import com.mardous.booming.data.remote.lastfm.LastFmService
import com.mardous.booming.data.remote.lastfm.model.LastFmAlbum
import com.mardous.booming.data.remote.lastfm.model.LastFmArtist
import com.mardous.booming.data.remote.lastfm.model.LastFmError
import com.mardous.booming.data.remote.lastfm.model.LastFmSessionResponse
import com.mardous.booming.data.remote.lastfm.model.LastFmUser
import com.mardous.booming.data.remote.lastfm.model.NowPlayingResponse
import com.mardous.booming.data.remote.lastfm.model.ScrobbleResponse
import com.mardous.booming.extensions.media.displayArtistName
import com.mardous.booming.util.CryptoUtil
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64

interface NetworkRepository {
    fun getLastFmLoginState(): Flow<LastFmLoginState>
    suspend fun loginToLastFm(username: String, password: String)
    suspend fun logoutFromLastFm()
    suspend fun scrobble(song: Song, timestamp: Long): LastFmResult
    suspend fun updateNowPlaying(song: Song): LastFmResult
    suspend fun artistInfo(name: String, lang: String?, cache: String?): LastFmArtist?
    suspend fun albumInfo(artist: String, album: String, lang: String?): LastFmAlbum?
    suspend fun deezerTrack(artist: String, title: String): DeezerTrack?
    suspend fun deezerArtist(name: String, limit: Int, index: Int): DeezerArtist?
    suspend fun deezerAlbum(artist: String, name: String): DeezerAlbum?
}

class NetworkRepositoryImpl(
    private val context: Context,
    private val preferences: SharedPreferences,
    private val lastFmService: LastFmService,
    private val deezerService: DeezerService
) : NetworkRepository {

    private val lastFmLoginStateFlow = MutableStateFlow<LastFmLoginState>(LastFmLoginState.Empty)
    private val lastFmLoginState get() = lastFmLoginStateFlow.value

    init {
        val sessionInfo = getLastFmSessionInfo()
        if (sessionInfo != null) {
            lastFmLoginStateFlow.value = LastFmLoginState.LoggedIn(sessionInfo.user)
        }
    }

    override fun getLastFmLoginState(): Flow<LastFmLoginState> {
        return lastFmLoginStateFlow
    }

    override suspend fun loginToLastFm(username: String, password: String) {
        val currentState = this.lastFmLoginState
        if (currentState is LastFmLoginState.LoggingIn) return
        if (currentState is LastFmLoginState.LoggedIn) {
            if (currentState.user.name == username) return
        }
        lastFmLoginStateFlow.value = LastFmLoginState.LoggingIn
        try {
            val userResponse = lastFmService.userInfo(username)
            val sessionResponse = lastFmService.createSession(userResponse.user.name, password)
            if (sessionResponse is LastFmSessionResponse) {
                val session = sessionResponse.session
                if (session != null && session.key.isNotBlank()) {
                    val isSuccess = setLastfmSessionInfo(userResponse.user, session.key)
                    if (isSuccess) {
                        lastFmLoginStateFlow.value = LastFmLoginState.LoggedIn(userResponse.user)
                        return
                    }
                }
                lastFmLoginStateFlow.value = LastFmLoginState.Failure(context)
            } else if (sessionResponse is LastFmError) {
                val failure = LastFmFailure.fromCode(sessionResponse.error)
                lastFmLoginStateFlow.value = LastFmLoginState.Failure(
                    context.getString(failure.messageRes)
                )
                return
            }
        } catch (e: Exception) {
            Log.e(TAG, "Last.fm: log-in error", e)
            lastFmLoginStateFlow.value = LastFmLoginState.Failure(context)
            return
        }
    }

    override suspend fun logoutFromLastFm() {
        try {
            preferences.edit(commit = true) {
                remove(SESSION_INFO)
            }
            lastFmLoginStateFlow.value = LastFmLoginState.Empty
        } catch (e: Exception) {
            Log.e(TAG, "Last.fm: logout error", e)
        }
    }

    override suspend fun scrobble(song: Song, timestamp: Long): LastFmResult {
        val sessionInfo = getLastFmSessionInfoOrLogout()
            ?: return LastFmResult.Failure()

        try {
            val response = lastFmService.scrobble(
                artist = song.displayArtistName(),
                track = song.title,
                album = song.albumName,
                timestamp = timestamp,
                sk = sessionInfo.key
            )

            val result = when (response) {
                is ScrobbleResponse -> {
                    val scrobbleData = response.scrobbles.scrobble.firstOrNull()
                    val ignoredMessage = scrobbleData?.ignoredMessage
                    if (response.scrobbles.attr.accepted > 0 && ignoredMessage?.code == "0") {
                        LastFmResult.ScrobbleSuccess(song.id)
                    } else {
                        LastFmResult.Failure(ignoredMessage?.text)
                    }
                }

                is LastFmError -> {
                    response.toLastFmResult()
                }

                else -> {
                    LastFmResult.Failure()
                }
            }

            return result
        } catch (e: Exception) {
            Log.e(TAG, "Last.fm: scrobble call failed!", e)
            return LastFmResult.Failure(context)
        }
    }

    override suspend fun updateNowPlaying(song: Song): LastFmResult {
        val sessionInfo = getLastFmSessionInfoOrLogout()
            ?: return LastFmResult.Failure()

        try {
            val response = lastFmService.updateNowPlaying(
                artist = song.displayArtistName(),
                track = song.title,
                sk = sessionInfo.key
            )

            val result = when (response) {
                is NowPlayingResponse -> {
                    val nowPlayingData = response.nowplaying
                    val ignoredMessage = nowPlayingData.ignoredMessage
                    if (ignoredMessage.code == "0") {
                        LastFmResult.NowPlayingSuccess(song.id)
                    } else {
                        LastFmResult.Failure(ignoredMessage.text)
                    }
                }

                is LastFmError -> {
                    response.toLastFmResult()
                }

                else -> {
                    LastFmResult.Failure()
                }
            }
            return result
        } catch (e: Exception) {
            Log.e(TAG, "Last.fm: updateNowPlaying call failed!", e)
            return LastFmResult.Failure(context)
        }
    }

    override suspend fun artistInfo(name: String, lang: String?, cache: String?): LastFmArtist? {
        return try {
            lastFmService.artistInfo(name, lang, cache)
        } catch (e: Exception) {
            Log.e(TAG, "Last.fm: artist info couldn't be retrieved!", e)
            null
        }
    }

    override suspend fun albumInfo(artist: String, album: String, lang: String?): LastFmAlbum? {
        return try {
            lastFmService.albumInfo(album, artist, lang)
        } catch (e: Exception) {
            Log.e(TAG, "Last.fm: album info couldn't be retrieved!", e)
            null
        }
    }

    override suspend fun deezerTrack(artist: String, title: String): DeezerTrack? {
        return try {
            deezerService.track(artist, title)
        } catch (e: Exception) {
            Log.e(TAG, "Deezer: track info couldn't be retrieved!", e)
            null
        }
    }

    override suspend fun deezerArtist(name: String, limit: Int, index: Int): DeezerArtist? {
        return try {
            deezerService.artist(name, limit, index)
        } catch (e: Exception) {
            Log.e(TAG, "Deezer: artist info couldn't be retrieved!", e)
            null
        }
    }

    override suspend fun deezerAlbum(artist: String, name: String): DeezerAlbum? {
        return try {
            deezerService.album(artist, name)
        } catch (e: Exception) {
            Log.e(TAG, "Deezer: album info couldn't be retrieved!", e)
            null
        }
    }

    private suspend fun getLastFmSessionInfoOrLogout(): SessionInfo? {
        val currentLoginState = lastFmLoginState
        if (currentLoginState is LastFmLoginState.LoggingIn)
            return null

        val sessionInfo = getLastFmSessionInfo()
        if (sessionInfo == null) {
            if (lastFmLoginState is LastFmLoginState.LoggedIn) {
                logoutFromLastFm()
            }
            return null
        }
        return sessionInfo
    }

    private suspend fun LastFmError.toLastFmResult(): LastFmResult {
        val errorCode = LastFmFailure.fromCode(this.error)
        if (errorCode == LastFmFailure.Auth ||
            errorCode == LastFmFailure.InvalidCredentials) {
            logoutFromLastFm()
        }
        return LastFmResult.Failure(context, errorCode)
    }

    private fun setLastfmSessionInfo(user: LastFmUser, sessionKey: String): Boolean {
        try {
            val encryptedKey = CryptoUtil.encrypt(sessionKey)
            val sessionInfo = Json.encodeToString(SessionInfo(user, encryptedKey))
            val encodedValue = Base64.encode(sessionInfo.toByteArray())
            preferences.edit(commit = true) {
                putString(SESSION_INFO, encodedValue)
            }
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Couldn't save Last.fm session info.", e)
        }
        return false
    }

    private fun getLastFmSessionInfo(): SessionInfo? {
        val encodedValue = preferences.getString(SESSION_INFO, null)
        if (!encodedValue.isNullOrBlank()) {
            try {
                val decodedValue = Base64.decode(encodedValue)
                val sessionInfo = Json.decodeFromString<SessionInfo>(String(decodedValue))
                return sessionInfo.copy(key = CryptoUtil.decrypt(sessionInfo.key))
            } catch (e: Exception) {
                Log.e(TAG, "Couldn't decrypt Last.fm session info. Removing...", e)
            }
        }
        return null
    }

    @Serializable
    private data class SessionInfo(
        @SerialName("user")
        val user: LastFmUser,
        @SerialName("session")
        val key: String
    )

    companion object {
        private const val TAG = "NetworkRepository"

        private const val SESSION_INFO = "session"
    }
}