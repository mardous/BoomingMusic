/*
 * Copyright (c) 2026 Christians Martínez Alvarado
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

package com.mardous.booming.ui.screen

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.session.MediaController
import com.mardous.booming.MediaControllerOwner
import com.mardous.booming.R
import com.mardous.booming.core.model.player.ProgressState
import com.mardous.booming.core.model.shuffle.OpenShuffleMode
import com.mardous.booming.data.model.QueueSong
import com.mardous.booming.data.model.Song
import com.mardous.booming.extensions.launchAndRepeatWithViewLifecycle
import com.mardous.booming.extensions.media.displayArtistName
import com.mardous.booming.extensions.showToast
import com.mardous.booming.ui.component.compose.MediaImage
import com.mardous.booming.ui.component.compose.player.NowPlayingSlider
import com.mardous.booming.ui.screen.library.LibraryViewModel
import com.mardous.booming.ui.screen.player.PlayerViewModel
import com.mardous.booming.ui.theme.BoomingMusicTheme
import org.koin.androidx.viewmodel.ext.android.viewModel
import android.graphics.Color as AndroidColor

class AudioPreviewActivity : ComponentActivity(), MediaController.Listener {

    private val libraryViewModel: LibraryViewModel by viewModel()
    private val playerViewModel: PlayerViewModel by viewModel()

    private lateinit var mediaControllerOwner: MediaControllerOwner

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                AndroidColor.TRANSPARENT,
                AndroidColor.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.auto(
                AndroidColor.TRANSPARENT,
                AndroidColor.TRANSPARENT
            )
        )
        super.onCreate(savedInstanceState)
        setContent {
            BoomingMusicTheme {
                PlayerContent(
                    onDismiss = { dismiss() },
                    onOpenFullPlayer = { startActivity(Intent(this, MainActivity::class.java)) },
                    playerViewModel = playerViewModel
                )
            }
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                dismiss()
            }
        })

        mediaControllerOwner = MediaControllerOwner(this, listener = this)
        mediaControllerOwner.attachTo(this)

        launchAndRepeatWithViewLifecycle {
            mediaControllerOwner.isConnected.collect { isConnected ->
                if (isConnected.getContentIfNotConsumed() == true) {
                    mediaControllerOwner.get()?.let { controller ->
                        mediaControllerOwner.addPlayerListener(playerViewModel, lifecycle)
                        playerViewModel.setMediaController(controller)
                        handleIntent()
                    }
                }
            }
        }
    }

    private fun dismiss() {
        mediaControllerOwner.isTransient = true
        finish()
    }

    private fun handleIntent() {
        libraryViewModel.handleIntent(this.intent).observe(this) {
            if (it.handled) {
                if (it.songs.isNotEmpty()) {
                    playerViewModel.openQueue(
                        queue = it.songs,
                        position = it.position,
                        startPlaying = true,
                        shuffleMode = OpenShuffleMode.Off
                    )
                }
                this.intent = Intent()
            }
            if (it.failed) {
                showToast(R.string.unplayable_file)
                finish()
            }
        }
    }

    @Composable
    private fun PlayerContent(
        onDismiss: () -> Unit,
        onOpenFullPlayer: () -> Unit,
        playerViewModel: PlayerViewModel
    ) {
        val cfg = LocalConfiguration.current
        val isLandscape = cfg.orientation == Configuration.ORIENTATION_LANDSCAPE

        val song by playerViewModel.currentSongFlow.collectAsState()
        val isPlaying by playerViewModel.isPlayingFlow.collectAsStateWithLifecycle()

        val progress by playerViewModel.progressFlow.collectAsStateWithLifecycle()
        val duration by playerViewModel.durationFlow.collectAsStateWithLifecycle()
        val progressState by remember {
            derivedStateOf { ProgressState(progress, duration) }
        }

        val sliderState = rememberSliderState(
            value = progressState.fraction
        )
        sliderState.onValueChangeFinished = {
            playerViewModel.seekTo((duration * sliderState.value).toLong())
        }

        LaunchedEffect(progressState) {
            if (progressState.mayUpdateUI) {
                sliderState.value = progressState.fraction
            }
        }

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = .4f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss
                    )
            )

            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier
                    .fillMaxWidth(fraction = if (isLandscape) 0.75f else 0.90f)
                    .navigationBarsPadding()
                    .padding(bottom = 48.dp)
            ) {
                if (song == Song.emptySong) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp)
                    ) {
                        CircularWavyProgressIndicator()
                    }
                } else {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MediaImage(
                                model = song,
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(16.dp))
                            )

                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = song.title,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1
                                )
                                Text(
                                    text = song.displayArtistName(),
                                    style = MaterialTheme.typography.bodyLarge,
                                    maxLines = 1
                                )
                            }

                            if ((song as? QueueSong)?.isUnindexed == false) {
                                IconButton(onClick = onOpenFullPlayer) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_open_in_new_24dp),
                                        contentDescription = "Open app"
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                NowPlayingSlider(
                                    sliderState = sliderState,
                                    isPlaying = isPlaying
                                )
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp)
                                ) {
                                    Text(
                                        text = progressState.progressAsString,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        text = progressState.totalAsString,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }

                            val interactionSource =
                                remember { MutableInteractionSource() }
                            val isPressed by interactionSource.collectIsPressedAsState()
                            FilledIconButton(
                                onClick = { playerViewModel.togglePlayPause() },
                                shape = if (isPressed) {
                                    IconButtonDefaults.mediumPressedShape
                                } else {
                                    IconButtonDefaults.mediumSquareShape
                                },
                                interactionSource = interactionSource,
                                modifier = Modifier.size(IconButtonDefaults.mediumContainerSize())
                            ) {
                                Icon(
                                    painter = if (isPlaying) {
                                        painterResource(R.drawable.ic_pause_m3_24dp)
                                    } else {
                                        painterResource(R.drawable.ic_play_m3_24dp)
                                    },
                                    contentDescription = stringResource(R.string.action_play_pause),
                                    modifier = Modifier.size(IconButtonDefaults.mediumIconSize)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
    }

    override fun onDisconnected(controller: MediaController) {
        playerViewModel.setMediaController(null)
    }
}