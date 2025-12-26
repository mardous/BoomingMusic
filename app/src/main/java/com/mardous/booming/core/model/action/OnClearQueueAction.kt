package com.mardous.booming.core.model.action

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.mardous.booming.R

enum class OnClearQueueAction(
    @DrawableRes val iconRes: Int,
    @StringRes val titleRes: Int,
    @StringRes val summaryRes: Int
) {
    RemoveAllSongs(
        iconRes = R.drawable.ic_clear_all_24dp,
        titleRes = R.string.on_clear_queue_remove_all_songs_title,
        summaryRes = R.string.on_clear_queue_remove_all_songs_summary
    ),
    RemoveAllSongsExceptCurrentlyPlaying(
        iconRes = R.drawable.ic_queue_music_24dp,
        titleRes = R.string.on_clear_queue_remove_all_songs_except_current_title,
        summaryRes = R.string.on_clear_queue_remove_all_songs_except_current_summary
    )
}