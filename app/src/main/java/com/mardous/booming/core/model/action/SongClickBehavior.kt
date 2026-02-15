package com.mardous.booming.core.model.action

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.mardous.booming.R

enum class SongClickBehavior(
    @DrawableRes val iconRes: Int,
    @StringRes val titleRes: Int,
    @StringRes val summaryRes: Int,
    val isAbleToPlay: Boolean
) {
    PlayOnlyThisSong(
        iconRes = R.drawable.ic_play_24dp,
        titleRes = R.string.on_song_click_play_only_this_song_title,
        summaryRes = R.string.on_song_click_play_only_this_song_summary,
        isAbleToPlay = true
    ),
    PlayWholeList(
        iconRes = R.drawable.ic_playlist_play_24dp,
        titleRes = R.string.on_song_click_play_whole_list_title,
        summaryRes = R.string.on_song_click_play_whole_list_summary,
        isAbleToPlay = true
    ),
    QueueNext(
        iconRes = R.drawable.ic_queue_play_next_24dp,
        titleRes = R.string.on_song_click_queue_next_title,
        summaryRes = R.string.on_song_click_queue_next_summary,
        isAbleToPlay = false
    ),
    EnqueueAtEnd(
        iconRes = R.drawable.ic_playlist_add_24dp,
        titleRes = R.string.on_song_click_enqueue_at_end_title,
        summaryRes = R.string.on_song_click_enqueue_at_end_summary,
        isAbleToPlay = false
    )
}
