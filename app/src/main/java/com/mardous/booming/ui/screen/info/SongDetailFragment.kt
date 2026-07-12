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

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.fragment.navArgs
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.mardous.booming.R
import com.mardous.booming.data.local.EditTarget
import com.mardous.booming.data.model.Song
import com.mardous.booming.extensions.isLandscape
import com.mardous.booming.ui.component.base.AbsTagEditorActivity
import com.mardous.booming.ui.component.base.goToDestination
import com.mardous.booming.ui.component.compose.BottomSheetDialogSurface
import com.mardous.booming.ui.component.compose.ErrorView
import com.mardous.booming.ui.component.compose.ShapedText
import com.mardous.booming.ui.component.compose.SmallHeader
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

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        if (isLandscape()) {
            (dialog as? BottomSheetDialog)?.let {
                it.behavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }
        return dialog
    }

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
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp)
                ) {
                    item {
                        // Header containing basic song's info
                        SmallHeader(
                            title = uiState.info.title.orEmpty(),
                            subtitle = uiState.info.artist,
                            trailingContent = {
                                AnimatedVisibility(
                                    visible = uiState.isLoading,
                                    modifier = Modifier.padding(start = 8.dp)
                                ) {
                                    CircularWavyProgressIndicator(
                                        stroke = Stroke(
                                            width = with(LocalDensity.current) { 3.dp.toPx() },
                                            cap = StrokeCap.Round
                                        ),
                                        trackStroke = Stroke(
                                            width = with(LocalDensity.current) { 3.dp.toPx() },
                                            cap = StrokeCap.Round
                                        ),
                                        wavelength = 10.dp,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            },
                            imageModel = song,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
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
                    }

                    if (!uiState.isLoading) {
                        if (uiState.isSuccess) {
                            // Content sections
                            if (!uiState.info.isMissingMetadata) {
                                item {
                                    MetadataInfoSection(uiState.info, Modifier.fillMaxWidth())
                                }
                            }
                            item {
                                PlayInfoSection(uiState.info, Modifier.fillMaxWidth())
                            }
                            item {
                                FileInfoSection(uiState.info, Modifier.fillMaxWidth())
                            }
                        } else {
                            item {
                                ErrorView(
                                    iconRes = R.drawable.ic_error_24dp,
                                    text = stringResource(R.string.could_not_load_the_song_information),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun MetadataInfoSection(songInfo: SongInfo, modifier: Modifier = Modifier) {
        InfoSection(
            icon = painterResource(R.drawable.ic_info_24dp),
            title = stringResource(R.string.metadata_label),
            modifier = modifier.fillMaxWidth()
        ) {
            InfoView(
                title = stringResource(R.string.album),
                content = songInfo.album
            )

            InfoView(
                title = stringResource(R.string.artist),
                content = songInfo.albumArtist
            )

            InfoView(
                title = stringResource(R.string.genre),
                content = songInfo.genre
            )

            InfoView(
                title = stringResource(R.string.year),
                content = songInfo.albumYear
            )

            InfoView(
                title = stringResource(R.string.track),
                content = songInfo.trackNumber
            )

            InfoView(
                title = stringResource(R.string.disc),
                content = songInfo.discNumber
            )

            InfoView(
                title = stringResource(R.string.composer),
                content = songInfo.composer
            )

            InfoView(
                title = stringResource(R.string.conductor),
                content = songInfo.conductor
            )

            InfoView(
                title = stringResource(R.string.publisher),
                content = songInfo.publisher
            )

            InfoView(
                title = stringResource(R.string.lyricist),
                content = songInfo.lyricist
            )

            InfoView(
                title = stringResource(R.string.arranger),
                content = songInfo.arranger
            )
        }
    }

    @Composable
    fun PlayInfoSection(songInfo: SongInfo, modifier: Modifier = Modifier) {
        InfoSection(
            icon = painterResource(R.drawable.ic_play_circle_24dp),
            title = stringResource(R.string.play_info),
            modifier = modifier.fillMaxWidth()
        ) {
            InfoView(
                title = stringResource(R.string.played),
                content = songInfo.playCount
            )

            InfoView(
                title = stringResource(R.string.skipped),
                content = songInfo.skipCount
            )

            InfoView(
                title = stringResource(R.string.last_played),
                content = songInfo.lastPlayedDate
            )
        }
    }

    @Composable
    private fun FileInfoSection(songInfo: SongInfo, modifier: Modifier = Modifier) {
        InfoSection(
            icon = painterResource(R.drawable.ic_audio_file_24dp),
            title = stringResource(R.string.file_label),
            modifier = modifier.fillMaxWidth()
        ) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                itemVerticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                songInfo.audioHeaderInfo?.let {
                    if (!it.format.isNullOrBlank()) {
                        ShapedText(
                            text = it.format,
                            style = MaterialTheme.typography.bodySmall,
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            shape = CircleShape
                        )
                    }
                    if (it.lossless) {
                        ShapedText(
                            text = stringResource(R.string.label_loss_less),
                            style = MaterialTheme.typography.bodySmall,
                            shape = CircleShape
                        )
                    }
                    if (!it.bitrate.isNullOrBlank()) {
                        ShapedText(
                            text = if (it.variableBitrate) {
                                "${it.bitrate} • ${stringResource(R.string.label_variable_bitrate)}"
                            } else {
                                it.bitrate
                            },
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            style = MaterialTheme.typography.bodySmall,
                            shape = CircleShape
                        )
                    }
                    if (!it.sampleRate.isNullOrBlank()) {
                        ShapedText(
                            text = it.sampleRate,
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            style = MaterialTheme.typography.bodySmall,
                            shape = CircleShape
                        )
                    }
                }
            }

            InfoView(
                title = stringResource(R.string.length),
                content = songInfo.trackLength
            )

            InfoView(
                title = stringResource(R.string.size),
                content = songInfo.fileSize
            )

            songInfo.audioHeaderInfo?.let { headerInfo ->
                InfoView(
                    title = stringResource(R.string.label_channels),
                    content = headerInfo.channels
                )
            }

            InfoView(
                title = stringResource(R.string.label_file_path),
                content = songInfo.filePath
            )

            InfoView(
                title = stringResource(R.string.label_last_modified),
                content = songInfo.dateModified
            )

            InfoView(
                title = stringResource(R.string.comment),
                content = songInfo.comment
            )

            InfoView(
                title = stringResource(R.string.replay_gain),
                content = songInfo.replayGain
            )
        }
    }

    @Composable
    private fun InfoView(
        title: String,
        content: String?,
        modifier: Modifier = Modifier
    ) {
        if (!content.isNullOrEmpty()) {
            Row(
                modifier = modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = title,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = content,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.End,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1.5f)
                )
            }
        }
    }

    @Composable
    private fun InfoSection(
        icon: Painter,
        title: String,
        modifier: Modifier = Modifier,
        content: @Composable () -> Unit
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ),
            modifier = modifier
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(
                    painter = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp)
            ) {
                content()
            }
        }
    }
}