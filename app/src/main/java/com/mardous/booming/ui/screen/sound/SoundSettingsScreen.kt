package com.mardous.booming.ui.screen.sound

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mardous.booming.R
import com.mardous.booming.core.model.audiodevice.AudioDevice
import com.mardous.booming.core.model.equalizer.ReplayGainState
import com.mardous.booming.data.model.replaygain.ReplayGainMode
import com.mardous.booming.extensions.hasR
import com.mardous.booming.ui.component.compose.LabeledSwitch
import java.util.Locale
import kotlin.math.roundToInt

private val LocalCardColor = compositionLocalOf {
    Color.White
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoundSettingsSheet(
    viewModel: SoundSettingsViewModel
) {
    val context = LocalContext.current

    val outputDevice by viewModel.audioDeviceFlow.collectAsState()
    val volume by viewModel.volumeStateFlow.collectAsState()

    val audioFloatOutput by viewModel.audioFloatOutputFlow.collectAsState()
    val skipSilence by viewModel.skipSilenceFlow.collectAsState()

    val balanceState by viewModel.balanceFlow.collectAsState()
    val balance = balanceState.value

    val tempoState by viewModel.tempoFlow.collectAsState()
    val tempo = tempoState.value

    val replayGainState by viewModel.replayGainStateFlow.collectAsState()
    val replayGain = replayGainState.value

    var showAudioFloatOutputDialog by remember { mutableStateOf(false) }
    var showSkipSilenceDialog by remember { mutableStateOf(false) }

    if (showAudioFloatOutputDialog) {
        SoundOptionDescriptionDialog(
            title = stringResource(R.string.enable_audio_float_output_title),
            description = stringResource(R.string.enable_audio_float_output_description)
        ) { showAudioFloatOutputDialog = false }
    }

    if (showSkipSilenceDialog) {
        SoundOptionDescriptionDialog(
            title = stringResource(R.string.skip_silence_title),
            description = stringResource(R.string.skip_silence_description)
        ) { showSkipSilenceDialog = false }
    }

    CompositionLocalProvider(
        LocalCardColor provides MaterialTheme.colorScheme.surfaceContainerLowest
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainer,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(top = 24.dp, start = 24.dp, end = 24.dp)
            ) {
                BottomSheetDefaults.DragHandle(
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                TitledSection(R.string.sound_settings) {
                    AudioDevice(outputDevice) {
                        viewModel.showOutputDeviceSelector(context)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .background(LocalCardColor.current)
                    ) {
                        LabeledSwitch(
                            checked = audioFloatOutput,
                            text = stringResource(R.string.enable_audio_float_output_title),
                            textStyle = MaterialTheme.typography.bodyMedium,
                            icon = { SoundOptionInfoIcon { showAudioFloatOutputDialog = true } }
                        ) { checked ->
                            viewModel.setEnableAudioFloatOutput(checked)
                        }

                        LabeledSwitch(
                            checked = skipSilence,
                            enabled = audioFloatOutput.not(),
                            text = stringResource(R.string.skip_silence_title),
                            textStyle = MaterialTheme.typography.bodyMedium,
                            icon = { SoundOptionInfoIcon { showSkipSilenceDialog = true } }
                        ) { checked ->
                            viewModel.setEnableSkipSilences(checked)
                        }
                    }
                }

                TitledSection(
                    titleResource = R.string.volume_label,
                    modifier = Modifier.padding(top = 4.dp),
                    titleEndContent = { TitleEndText("${volume.volumePercent.roundToInt()}%") }
                ) {
                    Slider(
                        enabled = !volume.isFixed,
                        value = volume.currentVolume.toFloat(),
                        valueRange = volume.range,
                        onValueChange = {
                            viewModel.setVolume(it.toInt())
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    AnimatedVisibility(
                        visible = audioFloatOutput.not(),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {

                            Slider(
                                value = balance.left,
                                valueRange = balance.range,
                                onValueChange = {
                                    viewModel.setBalance(left = it, apply = false)
                                },
                                onValueChangeFinished = {
                                    viewModel.applyPendingState()
                                },
                                modifier = Modifier.weight(1f)
                            )
                            Slider(
                                value = balance.right,
                                valueRange = balance.range,
                                onValueChange = {
                                    viewModel.setBalance(right = it, apply = false)
                                },
                                onValueChangeFinished = {
                                    viewModel.applyPendingState()
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                AnimatedVisibility(visible = audioFloatOutput.not()) {
                    var preamp by mutableFloatStateOf(replayGain.preamp)

                    TitledSection(
                        titleResource = R.string.replay_gain,
                        titleEndContent = {
                            AnimatedVisibility(visible = replayGain.mode.isOn) {
                                TitleEndText("%+.1f dB".format(Locale.ROOT, preamp))
                            }
                        }
                    ) {
                        Column {
                            ReplayGainModeSelector(replayGain) {
                                viewModel.setReplayGain(mode = it, apply = true)
                            }

                            AnimatedVisibility(
                                visible = replayGainState.isEnabled,
                                modifier = Modifier.padding(top = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(onClick = { viewModel.setReplayGain(preamp = 0f) }) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_restart_alt_24dp),
                                            contentDescription = null
                                        )
                                    }

                                    Slider(
                                        value = preamp,
                                        onValueChange = {
                                            preamp = it
                                        },
                                        onValueChangeFinished = {
                                            viewModel.setReplayGain(
                                                preamp = (preamp / 0.2f).roundToInt() * 0.2f
                                            )
                                        },
                                        valueRange = -15f..15f,
                                        steps = 30 - 1,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }

                TitledSection(
                    titleResource = R.string.tempo_label,
                    titleEndContent = { TitleEndText(tempo.formattedSpeed) }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                        ) {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                IconButton(onClick = {
                                    viewModel.setTempo(speed = 1f)
                                }) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_speed_24dp),
                                        contentDescription = null
                                    )
                                }

                                Slider(
                                    value = tempo.speed,
                                    valueRange = tempo.speedRange,
                                    onValueChange = {
                                        viewModel.setTempo(speed = it, apply = false)
                                    },
                                    onValueChangeFinished = {
                                        viewModel.applyPendingState()
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            Row(modifier = Modifier.fillMaxWidth()) {
                                IconButton(onClick = {
                                    viewModel.setTempo(pitch = 1f)
                                }) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_graphic_eq_24dp),
                                        contentDescription = null
                                    )
                                }

                                Slider(
                                    value = tempo.actualPitch,
                                    valueRange = tempo.pitchRange,
                                    onValueChange = {
                                        viewModel.setTempo(pitch = it, apply = false)
                                    },
                                    onValueChangeFinished = {
                                        viewModel.applyPendingState()
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        Icon(
                            painter = painterResource(
                                if (tempo.isFixedPitch) {
                                    R.drawable.ic_lock_24dp
                                } else {
                                    R.drawable.ic_lock_open_24dp
                                }
                            ),
                            contentDescription = null,
                            modifier = Modifier
                                .size(28.dp)
                                .padding(4.dp)
                                .clickable(onClick = {
                                    viewModel.setTempo(isFixedPitch = !tempo.isFixedPitch)
                                })
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun AudioDevice(
    outputDevice: AudioDevice,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(LocalCardColor.current)
            .clickable(enabled = hasR(), onClick = onClick),
    ) {
        Icon(
            modifier = Modifier.padding(start = 16.dp),
            painter = painterResource(outputDevice.type.iconRes),
            contentDescription = null
        )
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = outputDevice.getDeviceName(LocalContext.current).toString(),
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (outputDevice.hasProductName) {
                Text(
                    text = stringResource(outputDevice.type.nameRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun TitledSection(
    titleResource: Int,
    modifier: Modifier = Modifier,
    titleEndContent: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(titleResource),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )

            if (titleEndContent != null) {
                titleEndContent()
            }
        }
        content()
    }
}

@Composable
private fun TitleEndText(text: String) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.onTertiaryContainer,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.tertiaryContainer)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

@Composable
private fun ReplayGainModeText(mode: ReplayGainMode) {
    Text(
        text = when (mode) {
            ReplayGainMode.Album -> stringResource(R.string.album)
            ReplayGainMode.Track -> stringResource(R.string.track)
            else -> stringResource(R.string.label_none)
        }
    )
}

@Composable
private fun ReplayGainModeIcon(mode: ReplayGainMode) {
    Icon(
        painter = when (mode) {
            ReplayGainMode.Album -> painterResource(R.drawable.ic_album_24dp)
            ReplayGainMode.Track -> painterResource(R.drawable.ic_music_note_24dp)
            else -> painterResource(R.drawable.ic_clear_24dp)
        },
        contentDescription = null
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReplayGainModeSelector(
    replayGain: ReplayGainState,
    onModeSelected: (ReplayGainMode) -> Unit
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        replayGain.availableModes.forEach { mode ->
            FilterChip(
                selected = mode == replayGain.mode,
                onClick = {
                    onModeSelected(mode)
                },
                label = { ReplayGainModeText(mode) },
                leadingIcon = { ReplayGainModeIcon(mode) }
            )
        }
    }
}

@Composable
private fun SoundOptionInfoIcon(onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(20.dp)
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_info_24dp),
            contentDescription = null,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun SoundOptionDescriptionDialog(title: String, description: String, onClose: () -> Unit) {
    AlertDialog(
        icon = {
            Icon(
                painter = painterResource(R.drawable.ic_info_24dp),
                contentDescription = null
            )
        },
        title = { Text(title) },
        text = { Text(description) },
        confirmButton = {
            Button(onClose) {
                Text(text = stringResource(R.string.close_action))
            }
        },
        onDismissRequest = onClose,
    )
}