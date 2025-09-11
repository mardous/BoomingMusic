package com.mardous.booming.ui.screen.sound

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mardous.booming.R
import com.mardous.booming.core.model.audiodevice.AudioDevice
import com.mardous.booming.extensions.hasR
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoundSettingsSheet(
    viewModel: SoundSettingsViewModel
) {
    val context = LocalContext.current

    val outputDevice by viewModel.audioDeviceFlow.collectAsState()
    val volume by viewModel.volumeStateFlow.collectAsState()

    val balanceState by viewModel.balanceFlow.collectAsState()
    val balance = balanceState.value

    val tempoState by viewModel.tempoFlow.collectAsState()
    val tempo = tempoState.value

    val crossfadeState by viewModel.crossfadeFlow.collectAsState()
    val crossfade = crossfadeState.value

    Surface {
        Column(modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
        ) {
            BottomSheetDefaults.DragHandle(
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            AudioDevice(outputDevice) {
                viewModel.showOutputDeviceSelector(context)
            }

            TitledSection(R.string.volume_label) {
                Slider(
                    enabled = !volume.isFixed,
                    value = volume.currentVolume.toFloat(),
                    valueRange = volume.range,
                    onValueChange = {
                        viewModel.setVolume(it.toInt())
                    },
                    modifier = Modifier.fillMaxWidth()
                )

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

            TitledSection(R.string.tempo_label) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
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

            TitledSection(R.string.crossfade_title) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Slider(
                        value = crossfade.crossfadeDuration.toFloat(),
                        onValueChange = {
                            viewModel.setCrossfade(crossfadeDuration = it.toInt(), apply = false)
                        },
                        onValueChangeFinished = {
                            viewModel.setCrossfade(apply = true)
                        },
                        valueRange = crossfade.crossfadeRange,
                        steps = 9,
                        modifier = Modifier.weight(1f)
                    )

                    Text(
                        text = String.format(
                            Locale.US,
                            "%d s",
                            crossfade.crossfadeDuration
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.widthIn(min = 56.dp)
                    )
                }
            }

            TitledSection(R.string.audio_fade_duration_title) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Slider(
                        value = crossfade.audioFadeDuration.toFloat(),
                        onValueChange = {
                            viewModel.setCrossfade(audioFadeDuration = it.toInt(), apply = false)
                        },
                        onValueChangeFinished = {
                            viewModel.setCrossfade(apply = true)
                        },
                        valueRange = crossfade.audioFadeRange,
                        steps = 9,
                        modifier = Modifier.weight(1f)
                    )

                    Text(
                        text = String.format(
                            Locale.US,
                            "%d ms",
                            crossfadeState.value.audioFadeDuration
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.widthIn(min = 56.dp)
                    )
                }
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
            .background(MaterialTheme.colorScheme.surfaceContainer)
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
    content: @Composable () -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(titleResource),
            style = MaterialTheme.typography.titleMedium,
            modifier = modifier.padding(vertical = 16.dp)
        )
        content()
    }
}