package com.mardous.booming.data.model.network.lastfm

import androidx.annotation.StringRes
import com.mardous.booming.R

enum class LastFmFailure(
    private val codes: IntArray,
    @StringRes val messageRes: Int
) {
    Auth(
        codes = intArrayOf(9, 14),
        messageRes = R.string.error_lastfm_auth
    ),
    InvalidCredentials(
        codes = intArrayOf(4),
        messageRes = R.string.error_lastfm_credentials
    ),
    InvalidParams(
        codes = intArrayOf(2, 3, 6),
        messageRes = R.string.error_lastfm_params
    ),
    ServiceOffline(
        codes = intArrayOf(11, 16, 29),
        messageRes = R.string.error_lastfm_service
    ),
    RateLimit(
        codes = intArrayOf(26),
        messageRes = R.string.error_lastfm_rate_limit
    ),
    Unknown(
        codes = intArrayOf(),
        messageRes = R.string.error_lastfm_generic
    );

    companion object {
        fun fromCode(code: Any?): LastFmFailure {
            val numericCode = when(code) {
                is Int -> code
                is String -> code.toIntOrNull() ?: -1
                else -> -1
            }
            return entries.find { it.codes.contains(numericCode) } ?: Unknown
        }
    }
}