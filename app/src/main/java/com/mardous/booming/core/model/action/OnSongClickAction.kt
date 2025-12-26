package com.mardous.booming.core.model.action

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.mardous.booming.R

enum class OnSongClickAction(
    @DrawableRes iconRes: Int,
    @StringRes titleRes: Int,
    @StringRes summaryRes: Int
) {
    PlayOnlyThisSong(
        iconRes = R.drawable.ic_music_note_24dp,
        titleRes = R.string.on_song_click_play_only_this_song_title,
        summaryRes = R.string.on_song_click_play_only_this_song_summary
    ),
    PlayWholeList(
        iconRes = R.drawable.ic_queue_music_24dp,
        titleRes = R.string.on_song_click_play_whole_list_title,
        summaryRes = R.string.on_song_click_play_whole_list_summary
    ),
    EnqueueToExistingList(
        iconRes = R.drawable.ic_playlist_add_24dp,
        titleRes = R.string.on_song_click_enqueue_to_existing_list_title,
        summaryRes = R.string.on_song_click_enqueue_to_existing_list_summary
    )
}
