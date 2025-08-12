package com.mardous.booming.core.model

import com.mardous.booming.service.constants.SessionEvent

enum class MediaEvent(val code: String) {
    MediaContentChanged(SessionEvent.Companion.MEDIA_CONTENT_CHANGED),
    FavoriteContentChanged(SessionEvent.Companion.FAVORITE_CONTENT_CHANGED),
    PlaybackRestored(SessionEvent.Companion.PLAYBACK_RESTORED),
    PlaybackStarted(SessionEvent.Companion.PLAYBACK_STARTED);

    companion object {
        fun fromSessionEvent(sessionEvent: String?) =
            entries.firstOrNull { event -> event.code == sessionEvent }
    }
}