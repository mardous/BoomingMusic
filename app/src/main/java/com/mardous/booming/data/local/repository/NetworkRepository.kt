package com.mardous.booming.data.local.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import com.mardous.booming.BuildConfig
import com.mardous.booming.R
import com.mardous.booming.data.model.Song
import com.mardous.booming.data.model.network.LoginParams
import com.mardous.booming.data.model.network.LoginState
import com.mardous.booming.data.model.network.ScrobblingResult
import com.mardous.booming.data.model.network.ScrobblingService
import com.mardous.booming.data.model.network.lastfm.LastFmFailure
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
import com.mardous.booming.data.remote.listenbrainz.ListenBrainzService
import com.mardous.booming.data.remote.listenbrainz.model.ListenBrainzListen
import com.mardous.booming.data.remote.listenbrainz.model.ListenBrainzSubmission
import com.mardous.booming.data.remote.listenbrainz.model.ListenBrainzTrackAdditionalInfo
import com.mardous.booming.data.remote.listenbrainz.model.ListenBrainzTrackMetadata
import com.mardous.booming.extensions.media.displayArtistName
import com.mardous.booming.util.CryptoUtil
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.ConnectException
import java.net.SocketTimeoutException
import kotlin.io.encoding.Base64

interface NetworkRepository {
    fun getLoginState(service: ScrobblingService): Flow<LoginState>
    suspend fun loginToService(service: ScrobblingService, params: LoginParams)
    suspend fun logoutFromService(service: ScrobblingService)
    suspend fun scrobble(service: ScrobblingService, song: Song, timestamp: Long): ScrobblingResult
    suspend fun updateNowPlaying(service: ScrobblingService, song: Song): ScrobblingResult
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
    private val listenBrainzService: ListenBrainzService,
    private val deezerService: DeezerService
) : NetworkRepository {

    private val lastFmLoginStateFlow = MutableStateFlow<LoginState>(LoginState.Empty)
    private val lastFmLoginState get() = lastFmLoginStateFlow.value

    private val listenBrainzLoginStateFlow = MutableStateFlow<LoginState>(LoginState.Empty)
    private val listenBrainzLoginState get() = listenBrainzLoginStateFlow.value

    private val appName = context.getString(R.string.app_name_long)

    init {
        val lastFmSessionInfo = getLastFmSessionInfo()
        if (lastFmSessionInfo != null) {
            lastFmLoginStateFlow.value = LoginState.LoggedIn(
                username = lastFmSessionInfo.user.name,
                url = lastFmSessionInfo.user.url
            )
        }

        val listenBrainzSessionInfo = getListenBrainzSessionInfo()
        if (listenBrainzSessionInfo != null) {
            listenBrainzLoginStateFlow.value = LoginState.LoggedIn(
                username = listenBrainzSessionInfo.user,
                url = listenBrainzSessionInfo.url
            )
        }
    }

    override fun getLoginState(service: ScrobblingService): Flow<LoginState> {
        return when (service) {
            ScrobblingService.Lastfm -> lastFmLoginStateFlow
            ScrobblingService.ListenBrainz -> listenBrainzLoginStateFlow
        }
    }

    override suspend fun loginToService(service: ScrobblingService, params: LoginParams) {
        when (service) {
            ScrobblingService.Lastfm -> loginToLastFm(
                params["username"].orEmpty(),
                params["password"].orEmpty()
            )
            ScrobblingService.ListenBrainz -> loginToListenBrainz(
                params["token"].orEmpty()
            )
        }
    }

    override suspend fun logoutFromService(service: ScrobblingService) {
        when (service) {
            ScrobblingService.Lastfm -> logoutFromLastFm()
            ScrobblingService.ListenBrainz -> logoutFromListenBrainz()
        }
    }

    override suspend fun scrobble(
        service: ScrobblingService,
        song: Song,
        timestamp: Long
    ): ScrobblingResult {
        return when (service) {
            ScrobblingService.Lastfm -> scrobbleToLastfm(song, timestamp)
            ScrobblingService.ListenBrainz -> scrobbleToListenBrainz(song, timestamp)
        }
    }

    override suspend fun updateNowPlaying(
        service: ScrobblingService,
        song: Song
    ): ScrobblingResult {
        return when (service) {
            ScrobblingService.Lastfm -> updateNowPlayingOnLastfm(song)
            ScrobblingService.ListenBrainz -> updateNowPlayingOnListenBrainz(song)
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

    private suspend fun loginToLastFm(username: String, password: String) {
        val currentState = this.lastFmLoginState
        if (currentState is LoginState.LoggingIn) return
        if (currentState is LoginState.LoggedIn) {
            if (currentState.username == username) return
        }
        lastFmLoginStateFlow.value = LoginState.LoggingIn
        try {
            val userResponse = lastFmService.userInfo(username)
            val sessionResponse = lastFmService.createSession(userResponse.user.name, password)
            if (sessionResponse is LastFmSessionResponse) {
                val session = sessionResponse.session
                if (session != null && session.key.isNotBlank()) {
                    val isSuccess = setLastfmSessionInfo(userResponse.user, session.key)
                    if (isSuccess) {
                        lastFmLoginStateFlow.value = LoginState.LoggedIn(
                            userResponse.user.name,
                            userResponse.user.url
                        )
                        return
                    }
                }
                lastFmLoginStateFlow.value = LoginState.Failure(context.getString(R.string.error_lastfm_generic))
            } else if (sessionResponse is LastFmError) {
                val failure = LastFmFailure.fromCode(sessionResponse.error)
                lastFmLoginStateFlow.value = LoginState.Failure(
                    context.getString(failure.messageRes)
                )
                return
            }
        } catch (e: Exception) {
            when (e) {
                is ClientRequestException -> {
                    val failure = try {
                        val responseAsText = e.response.bodyAsText()
                        val lastFmError = Json.decodeFromString<LastFmError>(responseAsText)
                        LastFmFailure.fromCode(lastFmError.error)
                    } catch (_: Exception) {
                        LastFmFailure.Unknown
                    }
                    lastFmLoginStateFlow.value = LoginState.Failure(context.getString(failure.messageRes))
                }

                is ConnectException,
                is SocketTimeoutException -> {
                    lastFmLoginStateFlow.value = LoginState.Failure(
                        context.getString(R.string.error_network_timeout)
                    )
                }

                else -> {
                    Log.e(TAG, "Last.fm: log-in error", e)
                    lastFmLoginStateFlow.value = LoginState.Failure(
                        context.getString(R.string.error_lastfm_generic)
                    )
                }
            }
        }
    }

    private fun logoutFromLastFm() {
        try {
            preferences.edit(commit = true) {
                remove(LAST_FM_SESSION_INFO)
            }
            lastFmLoginStateFlow.value = LoginState.Empty
        } catch (e: Exception) {
            Log.e(TAG, "Last.fm: logout error", e)
        }
    }

    private suspend fun scrobbleToLastfm(song: Song, timestamp: Long): ScrobblingResult {
        val sessionInfo = getLastFmSessionInfoOrLogout()
            ?: return ScrobblingResult.Failure(context.getString(R.string.error_lastfm_auth))

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
                        ScrobblingResult.Success(song.id)
                    } else {
                        ScrobblingResult.Failure(ignoredMessage?.text)
                    }
                }

                is LastFmError -> {
                    response.toScrobblingResult()
                }

                else -> {
                    ScrobblingResult.Failure()
                }
            }

            return result
        } catch (e: Exception) {
            Log.e(TAG, "Last.fm: scrobble call failed!", e)
            return ScrobblingResult.Failure()
        }
    }

    private suspend fun updateNowPlayingOnLastfm(song: Song): ScrobblingResult {
        val sessionInfo = getLastFmSessionInfoOrLogout()
            ?: return ScrobblingResult.Failure()

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
                        ScrobblingResult.Success(song.id)
                    } else {
                        ScrobblingResult.Failure(ignoredMessage.text)
                    }
                }

                is LastFmError -> {
                    response.toScrobblingResult()
                }

                else -> {
                    ScrobblingResult.Failure()
                }
            }
            return result
        } catch (e: Exception) {
            Log.e(TAG, "Last.fm: updateNowPlaying call failed!", e)
            return ScrobblingResult.Failure()
        }
    }

    private fun getLastFmSessionInfoOrLogout(): LastFmSessionInfo? {
        val currentLoginState = lastFmLoginState
        if (currentLoginState is LoginState.LoggingIn)
            return null

        val sessionInfo = getLastFmSessionInfo()
        if (sessionInfo == null) {
            if (lastFmLoginState is LoginState.LoggedIn) {
                logoutFromLastFm()
            }
            return null
        }
        return sessionInfo
    }

    private fun LastFmError.toScrobblingResult(): ScrobblingResult {
        val errorCode = LastFmFailure.fromCode(this.error)
        if (errorCode == LastFmFailure.Auth ||
            errorCode == LastFmFailure.InvalidCredentials) {
            logoutFromLastFm()
        }
        return ScrobblingResult.Failure(context.getString(errorCode.messageRes))
    }

    private fun setLastfmSessionInfo(user: LastFmUser, sessionKey: String): Boolean {
        try {
            val encryptedKey = CryptoUtil.encrypt(sessionKey)
            val sessionInfo = Json.encodeToString(LastFmSessionInfo(user, encryptedKey))
            val encodedValue = Base64.encode(sessionInfo.toByteArray())
            preferences.edit(commit = true) {
                putString(LAST_FM_SESSION_INFO, encodedValue)
            }
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Couldn't save Last.fm session info.", e)
        }
        return false
    }

    private fun getLastFmSessionInfo(): LastFmSessionInfo? {
        if (preferences.contains("session")) {
            val sessionInfo = preferences.getString("session", null)
            preferences.edit {
                if (!preferences.contains(LAST_FM_SESSION_INFO)) {
                    putString(LAST_FM_SESSION_INFO, sessionInfo)
                }
                remove("session")
            }
        }
        val encodedValue = preferences.getString(LAST_FM_SESSION_INFO, null)
        if (!encodedValue.isNullOrBlank()) {
            try {
                val decodedValue = Base64.decode(encodedValue)
                val sessionInfo = Json.decodeFromString<LastFmSessionInfo>(String(decodedValue))
                return sessionInfo.copy(key = CryptoUtil.decrypt(sessionInfo.key))
            } catch (e: Exception) {
                Log.e(TAG, "Couldn't decrypt Last.fm session info. Removing...", e)
            }
        }
        return null
    }

    private suspend fun loginToListenBrainz(token: String) {
        if (listenBrainzLoginState is LoginState.LoggingIn) return
        listenBrainzLoginStateFlow.value = LoginState.LoggingIn
        try {
            val response = listenBrainzService.validateToken(token)
            if (response.valid && response.userName != null) {
                if (setListenBrainzToken(token, response.userName)) {
                    listenBrainzLoginStateFlow.value = LoginState.LoggedIn(
                        username = response.userName,
                        url = "https://listenbrainz.org/user/${response.userName}/"
                    )
                } else {
                    listenBrainzLoginStateFlow.value = LoginState.Failure(
                        context.getString(R.string.error_listenbrainz_generic)
                    )
                }
            } else {
                listenBrainzLoginStateFlow.value = LoginState.Failure(
                    context.getString(R.string.error_listenbrainz_invalid_token)
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "ListenBrainz: login error", e)
            listenBrainzLoginStateFlow.value = LoginState.Failure(
                context.getString(R.string.error_listenbrainz_invalid_token)
            )
        }
    }

    private fun logoutFromListenBrainz() {
        try {
            preferences.edit(commit = true) {
                remove(LISTEN_BRAINZ_SESSION_INFO)
            }
            listenBrainzLoginStateFlow.value = LoginState.Empty
        } catch (e: Exception) {
            Log.e(TAG, "ListenBrainz: logout error", e)
        }
    }

    private suspend fun scrobbleToListenBrainz(song: Song, timestamp: Long): ScrobblingResult {
        val sessionInfo = getListenBrainzSessionInfo()
            ?: return ScrobblingResult.Failure()
        try {
            val additionalInfo = if (!BuildConfig.DEBUG) {
                ListenBrainzTrackAdditionalInfo(
                    player = appName,
                    playerVersion = BuildConfig.VERSION_NAME
                )
            } else null

            val submission = ListenBrainzSubmission(
                listenType = "single",
                payload = listOf(
                    ListenBrainzListen(
                        listenedAt = timestamp,
                        trackMetadata = ListenBrainzTrackMetadata(
                            artistName = song.displayArtistName(),
                            trackName = song.title,
                            releaseName = song.albumName,
                            additionalInfo = additionalInfo
                        )
                    )
                )
            )
            val response = listenBrainzService.submitListen(sessionInfo.token, submission)
            return if (response.status == "ok") {
                ScrobblingResult.Success(song.id)
            } else {
                ScrobblingResult.Failure(response.error)
            }
        } catch (e: Exception) {
            Log.e(TAG, "ListenBrainz: scrobble call failed!", e)
            return ScrobblingResult.Failure(context.getString(R.string.error_listenbrainz_generic))
        }
    }

    private suspend fun updateNowPlayingOnListenBrainz(song: Song): ScrobblingResult {
        val sessionInfo = getListenBrainzSessionInfo()
            ?: return ScrobblingResult.Failure()
        try {
            val additionalInfo = if (!BuildConfig.DEBUG) {
                ListenBrainzTrackAdditionalInfo(
                    player = appName,
                    playerVersion = BuildConfig.VERSION_NAME
                )
            } else null

            val submission = ListenBrainzSubmission(
                listenType = "playing_now",
                payload = listOf(
                    ListenBrainzListen(
                        trackMetadata = ListenBrainzTrackMetadata(
                            artistName = song.displayArtistName(),
                            trackName = song.title,
                            releaseName = song.albumName,
                            additionalInfo = additionalInfo
                        )
                    )
                )
            )
            val response = listenBrainzService.submitListen(sessionInfo.token, submission)
            return if (response.status == "ok") {
                ScrobblingResult.Success(song.id)
            } else {
                ScrobblingResult.Failure(response.error)
            }
        } catch (e: Exception) {
            Log.e(TAG, "ListenBrainz: updateNowPlaying call failed!", e)
            return ScrobblingResult.Failure(context.getString(R.string.error_listenbrainz_generic))
        }
    }

    private fun setListenBrainzToken(token: String, userName: String): Boolean {
        return try {
            val encryptedToken = CryptoUtil.encrypt(token)
            val sessionInfo = Json.encodeToString(ListenBrainzSessionInfo(userName, encryptedToken))
            val encodedValue = Base64.encode(sessionInfo.toByteArray())
            preferences.edit(commit = true) {
                putString(LISTEN_BRAINZ_SESSION_INFO, encodedValue)
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Couldn't save ListenBrainz token.", e)
            false
        }
    }

    private fun getListenBrainzSessionInfo(): ListenBrainzSessionInfo? {
        val encodedValue = preferences.getString(LISTEN_BRAINZ_SESSION_INFO, null)
        if (!encodedValue.isNullOrBlank()) {
            try {
                val decodedValue = Base64.decode(encodedValue)
                val sessionInfo = Json.decodeFromString<ListenBrainzSessionInfo>(String(decodedValue))
                return sessionInfo.copy(token = CryptoUtil.decrypt(sessionInfo.token))
            } catch (e: Exception) {
                Log.e(TAG, "Couldn't decrypt ListenBrainz session info. Removing...", e)
            }
        }
        return null
    }

    @Serializable
    private data class LastFmSessionInfo(
        @SerialName("user")
        val user: LastFmUser,
        @SerialName("session")
        val key: String
    )

    @Serializable
    private data class ListenBrainzSessionInfo(
        @SerialName("user_name")
        val user: String,
        val token: String
    ) {
        val url = "https://listenbrainz.org/user/$user/"
    }

    companion object {
        private const val TAG = "NetworkRepository"

        private const val LAST_FM_SESSION_INFO = "lastfm_session"
        private const val LISTEN_BRAINZ_SESSION_INFO = "listenbrainz_session"
    }
}