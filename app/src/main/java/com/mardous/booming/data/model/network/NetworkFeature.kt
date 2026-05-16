package com.mardous.booming.data.model.network

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.core.content.getSystemService
import com.mardous.booming.R
import com.mardous.booming.util.Preferences
import com.mardous.booming.util.UpdateSearchMode
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.component.inject
import java.util.concurrent.TimeUnit

sealed class NetworkFeature(
    protected val preferenceKey: String,
    protected val isOnByDefault: Boolean
) : KoinComponent {

    protected val preferences: SharedPreferences by inject()

    open fun isAvailable(context: Context, requireBeOnline: Boolean = true): Boolean {
        val networkFeaturesEnabled = context.resources.getBoolean(R.bool.enable_network_features)
        if (preferences.getBoolean(NETWORK_FEATURES_KEY, networkFeaturesEnabled)) {
            if (isOnline(context) || !requireBeOnline) {
                return preferences.getBoolean(preferenceKey, isOnByDefault)
            }
        }
        return false
    }

    sealed class Images(preferenceKey: String, isOnByDefault: Boolean) :
        NetworkFeature(preferenceKey, isOnByDefault) {
        object Artists : Images(ALLOW_ONLINE_ARTIST_IMAGES_KEY, true)
        object Albums : Images(ALLOW_ONLINE_ALBUM_COVERS_KEY, false)
    }

    sealed class Lyrics(preferenceKey: String, isOnByDefault: Boolean) :
        NetworkFeature(preferenceKey, isOnByDefault) {
        object BetterLyrics : Lyrics(BETTERLYRICS_ENABLED_KEY, false)
        object Lyrically : Lyrics(LYRICALLY_ENABLED_KEY, false)
        object LRCLib : Lyrics(LRCLIB_ENABLED_KEY, true)
    }

    sealed class Lastfm(preferenceKey: String, isOnByDefault: Boolean) :
        NetworkFeature(preferenceKey, isOnByDefault) {
        object Scrobbling : Lastfm(LASTFM_SCROBBLING_ENABLED_KEY, false)
        object NowPlaying : Lastfm(LASTFM_NOW_PLAYING_ENABLED_KEY, false)
        object Biographies : Lastfm(LASTFM_INFO_ENABLED_KEY, true)

        override fun isAvailable(context: Context, requireBeOnline: Boolean): Boolean {
            val isIntegrationEnabled = context.resources.getBoolean(R.bool.enable_lastfm_integration)
            return isIntegrationEnabled && super.isAvailable(context, requireBeOnline)
        }
    }

    sealed class ListenBrainz(preferenceKey: String, isOnByDefault: Boolean) :
        NetworkFeature(preferenceKey, isOnByDefault) {
        object Scrobbling : ListenBrainz(LISTENBRAINZ_SCROBBLING_ENABLED_KEY, false)
        object NowPlaying : ListenBrainz(LISTENBRAINZ_NOW_PLAYING_ENABLED_KEY, false)
    }

    data object Updater : NetworkFeature(UPDATE_SEARCH_MODE_KEY, false) {
        override fun isAvailable(context: Context, requireBeOnline: Boolean): Boolean {
            if (!context.resources.getBoolean(R.bool.enable_app_update))
                return false

            val updateMode = preferences.getString(preferenceKey, UpdateSearchMode.WEEKLY)
            val minElapsedMillis = when (updateMode) {
                UpdateSearchMode.EVERY_DAY -> TimeUnit.DAYS.toMillis(1)
                UpdateSearchMode.EVERY_FIFTEEN_DAYS -> TimeUnit.DAYS.toMillis(15)
                UpdateSearchMode.WEEKLY -> TimeUnit.DAYS.toMillis(7)
                UpdateSearchMode.MONTHLY -> TimeUnit.DAYS.toMillis(30)
                else -> -1
            }
            val elapsedMillis = System.currentTimeMillis() - Preferences.lastUpdateSearch
            if ((minElapsedMillis > -1) && elapsedMillis >= minElapsedMillis) {
                return isOnline(context)
            }
            return false
        }
    }

    companion object : KoinComponent {
        const val NETWORK_FEATURES_KEY = "network_features"
        const val ONLY_WIFI_NETWORK_KEY = "wifi_only_network"
        const val ALLOW_ONLINE_ARTIST_IMAGES_KEY = "allow_online_artist_images"
        const val ALLOW_ONLINE_ALBUM_COVERS_KEY = "allow_online_album_covers"
        const val BETTERLYRICS_ENABLED_KEY = "betterlyrics_enabled"
        const val LYRICALLY_ENABLED_KEY = "lyrically_enabled"
        const val LRCLIB_ENABLED_KEY = "lrclib_enabled"
        const val LASTFM_SCROBBLING_ENABLED_KEY = "lastfm_scrobbling_enabled"
        const val LASTFM_NOW_PLAYING_ENABLED_KEY = "lastfm_now_playing_enabled"
        const val LASTFM_INFO_ENABLED_KEY = "lastfm_info_enabled"
        const val LISTENBRAINZ_SCROBBLING_ENABLED_KEY = "listenbrainz_scrobbling_enabled"
        const val LISTENBRAINZ_NOW_PLAYING_ENABLED_KEY = "listenbrainz_now_playing_enabled"
        const val UPDATE_SEARCH_MODE_KEY = "update_search_mode"

        fun isOnline(context: Context, ignoreWifiSetting: Boolean = false): Boolean {
            val cm = context.getSystemService<ConnectivityManager>() ?: return false
            val requireWifi = !ignoreWifiSetting && get<SharedPreferences>()
                .getBoolean(ONLY_WIFI_NETWORK_KEY, true)
            val nc = cm.getNetworkCapabilities(cm.activeNetwork)
            if (nc != null) {
                return nc.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        (nc.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) && !requireWifi)
            }
            return false
        }
    }
}