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

import androidx.annotation.StringRes
import com.mardous.booming.R

/**
 * @author Christians M. A. (mardous)
 */
enum class NowPlayingAction(@StringRes val titleRes: Int) {
    Lyrics(R.string.action_show_lyrics),
    LyricsEditor(R.string.open_lyrics_editor),
    AddToPlaylist(R.string.action_add_to_playlist),
    TogglePlayState(R.string.action_play_pause),
    OpenAlbum(R.string.action_go_to_album),
    OpenArtist(R.string.action_go_to_artist),
    OpenPlayQueue(R.string.playing_queue_label),
    ToggleFavoriteState(R.string.toggle_favorite),
    DeleteFromDevice(R.string.action_delete_from_device),
    TagEditor(R.string.action_tag_editor),
    SleepTimer(R.string.action_sleep_timer),
    SoundSettings(R.string.sound_settings),
    WebSearch(R.string.web_search),
    SaveAlbumCover(R.string.save_cover),
    SeekBackward(R.string.action_seek_backward_10s),
    SeekForward(R.string.action_seek_forward_10s),
    Nothing(R.string.label_nothing);
}