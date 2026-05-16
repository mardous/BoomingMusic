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
        object Lyrically : Lyrics("lyrically_enabled", false)
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

    data object Updater : NetworkFeature("update_search_mode", false) {
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