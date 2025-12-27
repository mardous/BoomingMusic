/*
 * Copyright (c) 2024 Christians Martínez Alvarado
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.mardous.booming.core.model.action

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.mardous.booming.R

/**
 * @author Christians M. A. (mardous)
 */
enum class NowPlayingAction(
    @DrawableRes val iconRes: Int,
    @StringRes val titleRes: Int
) {
    Lyrics(
        iconRes = R.drawable.ic_lyrics_outline_24dp,
        titleRes = R.string.action_show_lyrics
    ),
    LyricsEditor(
        iconRes = R.drawable.ic_edit_note_24dp,
        titleRes = R.string.action_lyrics_editor
    ),
    AddToPlaylist(
        iconRes = R.drawable.ic_playlist_add_24dp,
        titleRes = R.string.action_add_to_playlist
    ),
    TogglePlayState(
        iconRes = R.drawable.ic_play_24dp,
        titleRes = R.string.action_play_pause
    ),
    OpenAlbum(
        iconRes = R.drawable.ic_album_24dp,
        titleRes = R.string.action_go_to_album
    ),
    OpenArtist(
        iconRes = R.drawable.ic_artist_24dp,
        titleRes = R.string.action_go_to_artist
    ),
    OpenPlayQueue(
        iconRes = R.drawable.ic_queue_music_24dp,
        R.string.playing_queue_label
    ),
    ToggleFavoriteState(
        iconRes = R.drawable.ic_favorite_outline_24dp,
        titleRes = R.string.toggle_favorite
    ),
    DeleteFromDevice(
        iconRes = R.drawable.ic_delete_24dp,
        titleRes = R.string.action_delete_from_device
    ),
    TagEditor(
        iconRes = R.drawable.ic_edit_24dp,
        titleRes = R.string.action_tag_editor
    ),
    SleepTimer(
        iconRes = R.drawable.ic_timer_24dp,
        titleRes = R.string.action_sleep_timer
    ),
    SoundSettings(
        iconRes = R.drawable.ic_media_output_24dp,
        titleRes = R.string.sound_settings
    ),
    WebSearch(
        iconRes = R.drawable.ic_search_24dp,
        titleRes = R.string.web_search
    ),
    SaveAlbumCover(
        iconRes = R.drawable.ic_image_24dp,
        titleRes = R.string.save_cover
    ),
    SeekBackward(
        iconRes = R.drawable.ic_fast_rewind_24dp,
        titleRes = R.string.action_seek_backward
    ),
    SeekForward(
        iconRes = R.drawable.ic_fast_forward_24dp,
        titleRes = R.string.action_seek_forward
    ),
    Nothing(
        iconRes = R.drawable.ic_clear_24dp,
        titleRes = R.string.label_nothing
    );
}