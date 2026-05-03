package com.mardous.booming.data.model.network

import android.content.Context
import android.content.SharedPreferences
import com.mardous.booming.R
import com.mardous.booming.extensions.isOnline
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

sealed class NetworkFeature(
    private val preferenceKey: String,
    private val isOnByDefault: Boolean
) : KoinComponent {

    protected val preferences: SharedPreferences by inject()

    open fun isAvailable(context: Context, requireBeOnline: Boolean = true): Boolean {
        val defaultNetworkFeatures = context.resources.getBoolean(R.bool.enable_network_features)
        if (preferences.getBoolean(NETWORK_FEATURES_KEY, defaultNetworkFeatures)) {
            val isOnline = context.isOnline(preferences.getBoolean(ONLY_WIFI_NETWORK_KEY, true))
            if (isOnline || !requireBeOnline) {
                return preferences.getBoolean(preferenceKey, isOnByDefault)
            }
        }
        return false
    }

    object All : NetworkFeature(NETWORK_FEATURES_KEY, false) {
        override fun isAvailable(context: Context, requireBeOnline: Boolean): Boolean {
            return preferences.getBoolean(
                NETWORK_FEATURES_KEY,
                context.resources.getBoolean(R.bool.enable_network_features)
            )
        }
    }

    sealed class Images(preferenceKey: String, isOnByDefault: Boolean) :
        NetworkFeature(preferenceKey, isOnByDefault) {
        object Artists : Images("allow_online_artist_images", true)
        object Albums : Images("allow_online_album_covers", false)
    }

    sealed class Lyrics(preferenceKey: String, isOnByDefault: Boolean) :
        NetworkFeature(preferenceKey, isOnByDefault) {
        object BetterLyrics : Lyrics("betterlyrics_enabled", false)
        object SimpMusicLyrics : Lyrics("simpmusic_enabled", false)
        object LRCLib : Lyrics("lrclib_enabled", true)
    }

    sealed class Lastfm(preferenceKey: String, isOnByDefault: Boolean) :
        NetworkFeature(preferenceKey, isOnByDefault) {
        object Scrobbling : Lastfm("lastfm_scrobbling_enabled", false)
        object NowPlaying : Lastfm("lastfm_now_playing_enabled", false)
        object Biographies : Lastfm("lastfm_info_enabled", true)

        override fun isAvailable(context: Context, requireBeOnline: Boolean): Boolean {
            val isIntegrationEnabled = context.resources.getBoolean(R.bool.enable_lastfm_integration)
            return isIntegrationEnabled && super.isAvailable(context, requireBeOnline)
        }
    }

    sealed class ListenBrainz(preferenceKey: String, isOnByDefault: Boolean) :
        NetworkFeature(preferenceKey, isOnByDefault) {
        object Scrobbling : ListenBrainz("listenbrainz_scrobbling_enabled", false)
        object NowPlaying : ListenBrainz("listenbrainz_now_playing_enabled", false)
    }

    companion object {
        const val NETWORK_FEATURES_KEY = "network_features"
        const val ONLY_WIFI_NETWORK_KEY = "wifi_only_network"
    }
}