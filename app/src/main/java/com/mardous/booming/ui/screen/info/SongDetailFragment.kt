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
package com.mardous.booming.ui.screen.info

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.fragment.navArgs
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.mardous.booming.R
import com.mardous.booming.data.local.EditTarget
import com.mardous.booming.data.model.Song
import com.mardous.booming.ui.component.base.AbsTagEditorActivity
import com.mardous.booming.ui.component.base.goToDestination
import com.mardous.booming.ui.component.compose.BottomSheetDialogSurface
import com.mardous.booming.ui.component.compose.ErrorView
import com.mardous.booming.ui.component.compose.SmallHeader
import com.mardous.booming.ui.component.compose.TitledSurface
import com.mardous.booming.ui.screen.lyrics.LyricsEditorFragmentArgs
import com.mardous.booming.ui.screen.tageditor.SongTagEditorActivity
import com.mardous.booming.ui.theme.BoomingMusicTheme
import org.koin.androidx.viewmodel.ext.android.viewModel

data class SongInfoUiState(
    val isLoading: Boolean,
    val isSuccess: Boolean,
    val info: SongInfo = SongInfo.Empty
) {
    companion object {
        val Empty = SongInfoUiState(isLoading = false, isSuccess = false)
    }
}

class SongDetailFragment : BottomSheetDialogFragment() {

    private val navArgs: SongDetailFragmentArgs by navArgs()
    private val viewModel: InfoViewModel by viewModel()

    private val song: Song
        get() = navArgs.extraSong

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
            )
            setContent {
                BoomingMusicTheme {
                    SongInfoScreen(
                        viewModel = viewModel,
                        song = song,
                        onLyricsEditorClick = {
                            goToDestination(
                                requireActivity(),
                                R.id.nav_lyrics_editor,
                                LyricsEditorFragmentArgs.Builder(song)
                                    .build()
                                    .toBundle()
                            )
                        },
                        onTagEditorClick = {
                            val tagEditor =
                                Intent(requireContext(), SongTagEditorActivity::class.java)
                            tagEditor.putExtra(
                                AbsTagEditorActivity.EXTRA_TARGET,
                                EditTarget.song(song)
                            )
                            startActivity(tagEditor)
                        }
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun SongInfoScreen(
        viewModel: InfoViewModel,
        song: Song,
        onTagEditorClick: () -> Unit,
        onLyricsEditorClick: () -> Unit
    ) {
        val context = LocalContext.current
        val uiState by viewModel.songInfoUiState.collectAsState()

        LaunchedEffect(Unit) {
            viewModel.refreshSongInfo(context, song)
        }

        BottomSheetDialogSurface {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .nestedScroll(rememberNestedScrollInteropConnection())
            ) {
                BottomSheetDefaults.DragHandle(
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header containing basic song's info
                    SmallHeader(
                        title = uiState.info.title.orEmpty(),
                        subtitle = uiState.info.artist.orEmpty(),
                        additionalInfo = uiState.info.audioHeaderInfo?.let { it ->
                            if (it.lossless) {
                                "${it.format} • Loss-Less"
                            } else {
                                it.format.orEmpty()
                            }
                        },
                        imageModel = song,
                        showIndeterminateIndicator = uiState.isLoading,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Related actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = onLyricsEditorClick,
                            enabled = uiState.isLoading.not(),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_lyrics_outline_24dp),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.action_lyrics_editor))
                        }

                        Button(
                            onClick = onTagEditorClick,
                            enabled = uiState.isLoading.not(),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_edit_24dp),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.action_tag_editor))
                        }
                    }

                    if (!uiState.isLoading) {
                        if (uiState.isSuccess) {
                            // Info sections
                            MetadataInfoSection(uiState.info, Modifier.fillMaxWidth())
                            FileInfoSection(uiState.info, Modifier.fillMaxWidth())
                            PlayInfoSection(uiState.info, Modifier.fillMaxWidth())
                        } else {
                            ErrorView(
                                iconRes = R.drawable.ic_error_24dp,
                                text = stringResource(R.string.could_not_load_the_song_information),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }

    @Composable
    private fun MetadataInfoSection(songInfo: SongInfo, modifier: Modifier = Modifier) {
        TitledSurface(
            iconRes = R.drawable.ic_info_24dp,
            title = stringResource(R.string.metadata_label),
            collapsible = true,
            modifier = modifier
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                InfoView(
                    iconRes = R.drawable.ic_album_24dp,
                    title = stringResource(R.string.album),
                    content = songInfo.album,
                    modifier = Modifier.fillMaxWidth()
                )

                InfoView(
                    iconRes = R.drawable.ic_artist_24dp,
                    title = stringResource(R.string.artist),
                    content = songInfo.albumArtist,
                    modifier = Modifier.fillMaxWidth()
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    maxItemsInEachRow = 2
                ) {
                    InfoView(
                        iconRes = R.drawable.ic_radio_24dp,
                        title = stringResource(R.string.genre),
                        content = songInfo.genre,
                        modifier = Modifier.weight(1f)
                    )

                    InfoView(
                        iconRes = R.drawable.ic_event_24dp,
                        title = stringResource(R.string.year),
                        content = songInfo.albumYear,
                        modifier = Modifier.weight(1f)
                    )

                    InfoView(
                        iconRes = R.drawable.ic_format_list_numbered_24dp,
                        title = stringResource(R.string.track),
                        content = songInfo.trackNumber,
                        modifier = Modifier.weight(1f)
                    )

                    InfoView(
                        iconRes = R.drawable.ic_format_list_numbered_24dp,
                        title = stringResource(R.string.disc),
                        content = songInfo.discNumber,
                        modifier = Modifier.weight(1f)
                    )
                }

                InfoView(
                    iconRes = R.drawable.ic_person_24dp,
                    title = stringResource(R.string.composer),
                    content = songInfo.composer,
                    modifier = Modifier.fillMaxWidth()
                )

                InfoView(
                    iconRes = R.drawable.ic_headphones_24dp,
                    title = stringResource(R.string.conductor),
                    content = songInfo.conductor,
                    modifier = Modifier.fillMaxWidth()
                )

                InfoView(
                    iconRes = R.drawable.ic_music_note_24dp,
                    title = stringResource(R.string.publisher),
                    content = songInfo.publisher,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    @Composable
    fun FileInfoSection(songInfo: SongInfo, modifier: Modifier = Modifier) {
        TitledSurface(
            iconRes = R.drawable.ic_play_circle_24dp,
            title = stringResource(R.string.play_info),
            collapsible = true,
            modifier = modifier
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    InfoView(
                        iconRes = R.drawable.ic_play_24dp,
                        title = stringResource(R.string.played),
                        content = songInfo.playCount,
                        modifier = Modifier.weight(1f)
                    )

                    InfoView(
                        iconRes = R.drawable.ic_next_24dp,
                        title = stringResource(R.string.skipped),
                        content = songInfo.skipCount,
                        modifier = Modifier.weight(1f)
                    )
                }

                InfoView(
                    iconRes = R.drawable.ic_history_24dp,
                    title = stringResource(R.string.last_played),
                    content = songInfo.lastPlayedDate,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    @Composable
    private fun PlayInfoSection(songInfo: SongInfo, modifier: Modifier = Modifier) {
        TitledSurface(
            iconRes = R.drawable.ic_audio_file_24dp,
            title = stringResource(R.string.file_label),
            collapsible = true,
            modifier = modifier
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    maxItemsInEachRow = 2
                ) {
                    InfoView(
                        iconRes = R.drawable.ic_timer_24dp,
                        title = stringResource(R.string.length),
                        content = songInfo.trackLength,
                        modifier = Modifier.weight(1f)
                    )

                    InfoView(
                        iconRes = R.drawable.ic_audio_file_24dp,
                        title = stringResource(R.string.size),
                        content = songInfo.fileSize,
                        modifier = Modifier.weight(1f)
                    )

                    songInfo.audioHeaderInfo?.let { headerInfo ->
                        InfoView(
                            iconRes = R.drawable.ic_graphic_eq_24dp,
                            title = stringResource(R.string.label_bit_rate),
                            content = if (headerInfo.variableBitrate) {
                                "${headerInfo.bitrate} • Variable"
                            } else {
                                headerInfo.bitrate
                            },
                            modifier = Modifier.weight(1f)
                        )

                        InfoView(
                            iconRes = R.drawable.ic_equalizer_24dp,
                            title = stringResource(R.string.label_sampling_rate),
                            content = headerInfo.sampleRate,
                            modifier = Modifier.weight(1f)
                        )

                        InfoView(
                            iconRes = R.drawable.ic_edit_audio_24dp,
                            title = stringResource(R.string.label_channels),
                            content = headerInfo.channels,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                InfoView(
                    iconRes = R.drawable.ic_folder_24dp,
                    title = stringResource(R.string.label_file_path),
                    content = songInfo.filePath,
                    modifier = Modifier.fillMaxWidth()
                )

                InfoView(
                    iconRes = R.drawable.ic_history_24dp,
                    title = stringResource(R.string.label_last_modified),
                    content = songInfo.dateModified,
                    modifier = Modifier.fillMaxWidth()
                )

                InfoView(
                    iconRes = R.drawable.ic_comment_24dp,
                    title = stringResource(R.string.comment),
                    content = songInfo.comment,
                    modifier = Modifier.fillMaxWidth()
                )

                InfoView(
                    iconRes = R.drawable.ic_volume_up_24dp,
                    title = stringResource(R.string.replay_gain),
                    content = songInfo.replayGain,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    @Composable
    private fun InfoView(
        title: String,
        content: String?,
        @DrawableRes iconRes: Int = 0,
        modifier: Modifier = Modifier
    ) {
        if (!content.isNullOrEmpty()) {
            Column(
                modifier = modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    modifier = Modifier.wrapContentSize(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (iconRes != 0) {
                        Icon(
                            painter = painterResource(iconRes),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = content,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}