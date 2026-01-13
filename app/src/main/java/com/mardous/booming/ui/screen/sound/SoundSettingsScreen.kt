package com.mardous.booming.ui.screen.sound

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mardous.booming.R
import com.mardous.booming.core.model.audiodevice.AudioDevice
import com.mardous.booming.extensions.hasR
import com.mardous.booming.ui.component.compose.BottomSheetDialogSurface
import com.mardous.booming.ui.component.compose.LabeledSwitch
import com.mardous.booming.ui.component.compose.ShapedText
import com.mardous.booming.ui.component.compose.TitleShapedText
import com.mardous.booming.ui.component.compose.TitledCard
import com.mardous.booming.ui.screen.equalizer.EqualizerViewModel
import com.mardous.booming.ui.theme.CornerRadiusTokens
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoundSettingsSheet(
    viewModel: EqualizerViewModel
) {
    val context = LocalContext.current

    var expandedSoundSettings by remember { mutableStateOf(false) }
    val outputDevice by viewModel.audioDevice.collectAsState()
    val volume by viewModel.volumeState.collectAsState()

    val audioOffload by viewModel.audioOffload.collectAsState()
    val audioFloatOutput by viewModel.audioFloatOutput.collectAsState()
    val skipSilence by viewModel.skipSilence.collectAsState()

    val enableAudioEffects by remember {
        derivedStateOf { audioOffload.not() && audioFloatOutput.not() }
    }

    val balance by viewModel.balanceState.collectAsState()
    val tempo by viewModel.tempoState.collectAsState()

    var balanceLeft by remember { mutableFloatStateOf(balance.left) }
    var balanceRight by remember { mutableFloatStateOf(balance.right) }
    var tempoSpeed by remember { mutableFloatStateOf(tempo.speed) }
    var tempoPitch by remember { mutableFloatStateOf(tempo.pitch) }

    var showAudioOffloadDialog by remember { mutableStateOf(false) }
    var showAudioFloatOutputDialog by remember { mutableStateOf(false) }
    var showSkipSilenceDialog by remember { mutableStateOf(false) }

    if (showAudioOffloadDialog) {
        SoundOptionDescriptionDialog(
            title = stringResource(R.string.enable_audio_offload_title),
            description = stringResource(R.string.enable_audio_offload_description)
        ) { showAudioOffloadDialog = false }
    }

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

    BottomSheetDialogSurface {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
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
                Text(
                    text = stringResource(R.string.sound_settings),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                AudioDeviceInfo(
                    outputDevice = outputDevice,
                    expanded = expandedSoundSettings,
                    onClick = {
                        viewModel.showOutputDeviceSelector(context)
                    },
                    onExpandClick = {
                        expandedSoundSettings = expandedSoundSettings.not()
                    }
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        LabeledSwitch(
                            checked = audioOffload,
                            text = stringResource(R.string.enable_audio_offload_title),
                            textStyle = MaterialTheme.typography.bodyMedium,
                            icon = { SoundOptionInfoIcon { showAudioOffloadDialog = true } }
                        ) { checked ->
                            viewModel.setEnableAudioOffload(checked)
                        }

                        LabeledSwitch(
                            checked = audioFloatOutput,
                            text = stringResource(R.string.enable_audio_float_output_title),
                            textStyle = MaterialTheme.typography.bodyMedium,
                            icon = {
                                SoundOptionInfoIcon {
                                    showAudioFloatOutputDialog = true
                                }
                            }
                        ) { checked ->
                            viewModel.setEnableAudioFloatOutput(checked)
                        }

                        LabeledSwitch(
                            checked = skipSilence,
                            enabled = enableAudioEffects,
                            text = stringResource(R.string.skip_silence_title),
                            textStyle = MaterialTheme.typography.bodyMedium,
                            icon = { SoundOptionInfoIcon { showSkipSilenceDialog = true } }
                        ) { checked ->
                            viewModel.setEnableSkipSilences(checked)
                        }
                    }
                }

                TitledCard(
                    title = stringResource(R.string.volume_label),
                    iconRes = R.drawable.ic_volume_up_24dp,
                    titleEndContent = { TitleShapedText("${volume.volumePercent.roundToInt()}%") },
                    modifier = Modifier.fillMaxWidth()
                ) { cardContentPadding ->
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(cardContentPadding)
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

                        AnimatedVisibility(visible = enableAudioEffects) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(20.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    ShapedText(
                                        text = "L",
                                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                                        shape = RoundedCornerShape(4.dp),
                                        shapeColor = MaterialTheme.colorScheme.tertiaryContainer,
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                    Slider(
                                        value = balanceLeft,
                                        valueRange = balance.range,
                                        onValueChange = { balanceLeft = it },
                                        onValueChangeFinished = {
                                            viewModel.setBalance(left = balanceLeft)
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    ShapedText(
                                        text = "R",
                                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                                        shape = RoundedCornerShape(4.dp),
                                        shapeColor = MaterialTheme.colorScheme.tertiaryContainer,
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                    Slider(
                                        value = balanceRight,
                                        valueRange = balance.range,
                                        onValueChange = { balanceRight = it },
                                        onValueChangeFinished = {
                                            viewModel.setBalance(right = balanceRight)
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }

                TitledCard(
                    title = stringResource(R.string.tempo_label),
                    iconRes = R.drawable.ic_graphic_eq_24dp,
                    titleEndContent = {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TitleShapedText(
                                text = "%.1fx".format(Locale.US, tempoSpeed),
                                onClick = {
                                    viewModel.setTempo(isFixedPitch = tempo.isFixedPitch.not())
                                }
                            )

                            AnimatedVisibility(visible = tempo.isFixedPitch.not()) {
                                TitleShapedText(
                                    text = "%.1fx".format(Locale.US, tempoPitch),
                                    onClick = {
                                        viewModel.setTempo(isFixedPitch = true)
                                    }
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { cardContentPadding ->
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(cardContentPadding)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            IconButton(
                                onClick = { viewModel.setTempo(speed = 1f) }
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_speed_24dp),
                                    contentDescription = null
                                )
                            }

                            Slider(
                                value = tempoSpeed,
                                valueRange = tempo.speedRange,
                                onValueChange = { tempoSpeed = it },
                                onValueChangeFinished = {
                                    viewModel.setTempo(speed = tempoSpeed)
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Row(modifier = Modifier.fillMaxWidth()) {
                            IconButton(
                                enabled = tempo.isFixedPitch.not(),
                                onClick = { viewModel.setTempo(pitch = 1f) }
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_graphic_eq_24dp),
                                    contentDescription = null
                                )
                            }

                            Slider(
                                enabled = tempo.isFixedPitch.not(),
                                value = if (tempo.isFixedPitch) tempoSpeed else tempoPitch,
                                valueRange = tempo.pitchRange,
                                onValueChange = { tempoPitch = it },
                                onValueChangeFinished = {
                                    viewModel.setTempo(pitch = tempoPitch)
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun AudioDeviceInfo(
    outputDevice: AudioDevice,
    expanded: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onExpandClick: () -> Unit,
    expandableContent: @Composable () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val shapeCornerRadius by animateDpAsState(
        targetValue = if (expanded) CornerRadiusTokens.SurfaceSmall else CornerRadiusTokens.SurfaceMedium
    )
    val shapeColor by animateColorAsState(
        targetValue = if (expanded) colorScheme.surfaceContainerLowest else colorScheme.primaryContainer
    )
    val contentColor by animateColorAsState(
        targetValue = if (expanded) colorScheme.onSurface else colorScheme.onTertiaryContainer
    )
    val expandIconRotation by animateFloatAsState(targetValue = if (expanded) 180f else 0f)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(shapeCornerRadius))
            .background(shapeColor)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .clickable(enabled = hasR(), onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = contentColor.copy(alpha = .1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(outputDevice.type.iconRes),
                        tint = contentColor,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .weight(1f)
            ) {
                Text(
                    text = outputDevice.getDeviceName(LocalContext.current).toString(),
                    color = contentColor,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(outputDevice.type.nameRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = contentColor.copy(alpha = 0.75f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onExpandClick) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_drop_down_24dp),
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.rotate(expandIconRotation)
                )
            }
        }
        AnimatedVisibility(visible = expanded) {
            expandableContent()
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