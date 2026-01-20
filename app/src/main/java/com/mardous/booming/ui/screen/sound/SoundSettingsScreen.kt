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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.mardous.booming.R
import com.mardous.booming.core.model.audiodevice.AudioDevice
import com.mardous.booming.extensions.hasR
import com.mardous.booming.ui.component.compose.BottomSheetDialogSurface
import com.mardous.booming.ui.component.compose.IconifiedSliderTrack
import com.mardous.booming.ui.component.compose.LabeledSwitch
import com.mardous.booming.ui.component.compose.TitledCard
import com.mardous.booming.ui.screen.equalizer.EqualizerViewModel
import com.mardous.booming.ui.theme.CornerRadiusTokens
import com.mardous.booming.ui.theme.SliderTokens
import com.mardous.booming.ui.theme.SurfaceColorTokens
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SoundSettingsSheet(
    viewModel: EqualizerViewModel
) {
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current

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

    var centerBalance by remember(balance.center) { mutableFloatStateOf(balance.center) }
    var tempoSpeed by remember(tempo.speed) { mutableFloatStateOf(tempo.speed) }
    var tempoPitch by remember(tempo.actualPitch) { mutableFloatStateOf(tempo.actualPitch) }

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
                    titleEndContent = {
                        IconButton(
                            onClick = {
                                hapticFeedback.performHapticFeedback(
                                    HapticFeedbackType.Confirm
                                )
                                viewModel.setVolume(1f)
                                viewModel.setBalance(0f)
                            },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_restart_alt_24dp),
                                tint = MaterialTheme.colorScheme.secondary,
                                contentDescription = stringResource(R.string.reset_balance),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { cardContentPadding ->
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(cardContentPadding)
                    ) {
                        Slider(
                            value = volume.currentVolume,
                            valueRange = volume.volumeRange,
                            onValueChange = {
                                viewModel.setVolume(it)
                            },
                            onValueChangeFinished = {
                                hapticFeedback.performHapticFeedback(
                                    HapticFeedbackType.SegmentFrequentTick
                                )
                            },
                            track = { sliderState ->
                                IconifiedSliderTrack(
                                    state = sliderState,
                                    icon = when {
                                        volume.volumePercent > 50 -> painterResource(R.drawable.ic_volume_up_24dp)
                                        volume.volumePercent > 10 -> painterResource(R.drawable.ic_volume_down_24dp)
                                        else -> painterResource(R.drawable.ic_volume_mute_24dp)
                                    },
                                    disabledIcon = painterResource(R.drawable.ic_volume_off_24dp),
                                    modifier = Modifier.height(SliderTokens.LargeTrackHeight)
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        AnimatedVisibility(visible = enableAudioEffects) {
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Slider(
                                        value = centerBalance,
                                        valueRange = balance.range,
                                        onValueChange = { centerBalance = it },
                                        onValueChangeFinished = {
                                            hapticFeedback.performHapticFeedback(
                                                HapticFeedbackType.SegmentFrequentTick
                                            )
                                            viewModel.setBalance(center = centerBalance)
                                        },
                                        track = {
                                            SliderDefaults.CenteredTrack(
                                                sliderState = it,
                                                trackCornerSize = SliderTokens.TrackCornerSize,
                                                modifier = Modifier.height(SliderTokens.LargeTrackHeight)
                                            )
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp)

                                    ) {
                                        Text(
                                            text = stringResource(R.string.balance_left),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            fontWeight = FontWeight.SemiBold,
                                            textAlign = TextAlign.Start,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            style = MaterialTheme.typography.bodySmall
                                        )

                                        Text(
                                            text = stringResource(R.string.balance_right),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            fontWeight = FontWeight.SemiBold,
                                            textAlign = TextAlign.End,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                TitledCard(
                    title = stringResource(R.string.speed_and_pitch_label),
                    titleEndContent = {
                        IconButton(
                            onClick = {
                                hapticFeedback.performHapticFeedback(
                                    HapticFeedbackType.Confirm
                                )
                                viewModel.setTempo(isFixedPitch = tempo.isFixedPitch.not())
                            },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                painter = if (tempo.isFixedPitch) {
                                    painterResource(R.drawable.ic_lock_24dp)
                                } else {
                                    painterResource(R.drawable.ic_lock_open_24dp)
                                },
                                tint = MaterialTheme.colorScheme.secondary,
                                contentDescription = stringResource(R.string.unlock_pitch_adjustment),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                hapticFeedback.performHapticFeedback(
                                    HapticFeedbackType.Confirm
                                )
                                viewModel.setTempo(speed = 1f, pitch = 1f)
                            },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_restart_alt_24dp),
                                tint = MaterialTheme.colorScheme.secondary,
                                contentDescription = stringResource(R.string.reset_tempo),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { cardContentPadding ->
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(cardContentPadding)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Slider(
                                value = tempoSpeed,
                                valueRange = tempo.speedRange,
                                onValueChange = { tempoSpeed = it },
                                onValueChangeFinished = {
                                    hapticFeedback.performHapticFeedback(
                                        HapticFeedbackType.SegmentFrequentTick
                                    )
                                    viewModel.setTempo(speed = tempoSpeed)
                                },
                                track = { sliderState ->
                                    IconifiedSliderTrack(
                                        state = sliderState,
                                        icon = painterResource(R.drawable.ic_speed_24dp),
                                        modifier = Modifier.height(SliderTokens.LargeTrackHeight)
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )

                            SoundSettingsValueText(
                                text = "%.2fx".format(Locale.US, tempoSpeed),
                                modifier = Modifier.widthIn(min = 48.dp)
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val sliderValue = if (tempo.isFixedPitch) tempoSpeed else tempoPitch
                            Slider(
                                enabled = tempo.isFixedPitch.not(),
                                value = sliderValue,
                                valueRange = tempo.pitchRange,
                                onValueChange = { tempoPitch = it },
                                onValueChangeFinished = {
                                    hapticFeedback.performHapticFeedback(
                                        HapticFeedbackType.SegmentFrequentTick
                                    )
                                    viewModel.setTempo(pitch = tempoPitch)
                                },
                                track = { sliderState ->
                                    IconifiedSliderTrack(
                                        state = sliderState,
                                        icon = painterResource(R.drawable.ic_edit_audio_24dp),
                                        modifier = Modifier.height(SliderTokens.LargeTrackHeight)
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )

                            SoundSettingsValueText(
                                text = "%.2fx".format(Locale.US, sliderValue),
                                modifier = Modifier.widthIn(min = 48.dp)
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedButton(
                                onClick = {
                                    hapticFeedback.performHapticFeedback(
                                        HapticFeedbackType.ContextClick
                                    )
                                    viewModel.setTempo(speed = 0.5f)
                                },
                                shape = ButtonGroupDefaults.connectedLeadingButtonShape,
                                contentPadding = PaddingValues(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = stringResource(R.string.speed_0_5x),
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    hapticFeedback.performHapticFeedback(
                                        HapticFeedbackType.ContextClick
                                    )
                                    viewModel.setTempo(speed = 0.8f)
                                },
                                shape = ShapeDefaults.Small,
                                contentPadding = PaddingValues(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = stringResource(R.string.speed_0_8x),
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    hapticFeedback.performHapticFeedback(
                                        HapticFeedbackType.ContextClick
                                    )
                                    viewModel.setTempo(speed = 1.0f)
                                },
                                shape = ShapeDefaults.Small,
                                contentPadding = PaddingValues(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = stringResource(R.string.speed_1_0x),
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    hapticFeedback.performHapticFeedback(
                                        HapticFeedbackType.ContextClick
                                    )
                                    viewModel.setTempo(speed = 1.2f)
                                },
                                shape = ShapeDefaults.Small,
                                contentPadding = PaddingValues(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = stringResource(R.string.speed_1_2x),
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    hapticFeedback.performHapticFeedback(
                                        HapticFeedbackType.ContextClick
                                    )
                                    viewModel.setTempo(speed = 1.5f)
                                },
                                shape = ButtonGroupDefaults.connectedTrailingButtonShape,
                                contentPadding = PaddingValues(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = stringResource(R.string.speed_1_5x),
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
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
        targetValue = if (expanded) CornerRadiusTokens.SurfaceSmall else CornerRadiusTokens.SurfaceLarge
    )
    val shapeColor by animateColorAsState(
        targetValue = if (expanded) {
            colorScheme.surfaceVariant.copy(alpha = SurfaceColorTokens.SurfaceVariantAlpha)
        } else {
            colorScheme.primaryContainer
        }
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

@Composable
private fun SoundSettingsValueText(
    text: String,
    color: Color = MaterialTheme.colorScheme.secondary,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        color = color,
        maxLines = 1,
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center,
        modifier = modifier
    )
}