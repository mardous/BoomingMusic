package com.mardous.booming.ui.screen.sound

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.mardous.booming.R
import com.mardous.booming.core.model.audiodevice.AudioDevice
import com.mardous.booming.core.model.audiodevice.BitPerfectState
import com.mardous.booming.extensions.hasR
import com.mardous.booming.ui.component.compose.BottomSheetDialogSurface
import com.mardous.booming.ui.component.compose.IconifiedSliderTrack
import com.mardous.booming.ui.component.compose.ShapedText
import com.mardous.booming.ui.component.compose.TitledCard
import com.mardous.booming.ui.screen.equalizer.EqualizerViewModel
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

    val outputDevice by viewModel.audioDevice.collectAsState()
    val volume by viewModel.volumeState.collectAsState()
    val bitPerfectState by viewModel.bitPerfectState.collectAsState()

    val bitPerfect by viewModel.bitPerfectAudio.collectAsState()
    val audioOffload by viewModel.audioOffload.collectAsState()
    val audioFloatOutput by viewModel.audioFloatOutput.collectAsState()
    val skipSilence by viewModel.skipSilence.collectAsState()

    val isBitPerfectActuallyActive by remember {
        derivedStateOf { bitPerfect && bitPerfectState.isActive }
    }
    val enableAudioEffects by remember {
        derivedStateOf { audioOffload.not() && isBitPerfectActuallyActive.not() && audioFloatOutput.not() }
    }

    val balance by viewModel.balanceState.collectAsState()
    val tempo by viewModel.tempoState.collectAsState()

    var centerBalance by remember(balance.center) { mutableFloatStateOf(balance.center) }
    var tempoSpeed by remember(tempo.speed) { mutableFloatStateOf(tempo.speed) }
    var tempoPitch by remember(tempo.actualPitch) { mutableFloatStateOf(tempo.actualPitch) }

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
                    Text(
                        text = stringResource(R.string.sound_settings),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                item {
                    AudioDeviceInfo(
                        outputDevice = outputDevice,
                        bitPerfectState = bitPerfectState,
                        onClick = { viewModel.showOutputDeviceSelector(context) }
                    )
                }

                item {
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
                                enabled = !bitPerfectState.isActive || !bitPerfectState.isVolumeFixed,
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
                }

                item {
                    AnimatedVisibility(visible = isBitPerfectActuallyActive.not()) {
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
                    }
                }

                item {
                    TitledCard(
                        title = stringResource(R.string.advanced_settings),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                        ) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                                LabeledSwitch(
                                    checked = bitPerfect,
                                    title = stringResource(R.string.bit_perfect_title),
                                    description = stringResource(R.string.bit_perfect_description)
                                ) { checked ->
                                    viewModel.setEnableBitPerfect(checked)
                                }
                            }

                            LabeledSwitch(
                                checked = audioOffload,
                                title = stringResource(R.string.enable_audio_offload_title),
                                description = stringResource(R.string.enable_audio_offload_description),
                                enabled = bitPerfect.not()
                            ) { checked ->
                                viewModel.setEnableAudioOffload(checked)
                            }

                            LabeledSwitch(
                                checked = audioFloatOutput,
                                title = stringResource(R.string.enable_audio_float_output_title),
                                description = stringResource(R.string.enable_audio_float_output_description)
                            ) { checked ->
                                viewModel.setEnableAudioFloatOutput(checked)
                            }

                            LabeledSwitch(
                                checked = skipSilence,
                                enabled = enableAudioEffects,
                                title = stringResource(R.string.skip_silence_title),
                                description = stringResource(R.string.skip_silence_description)
                            ) { checked ->
                                viewModel.setEnableSkipSilences(checked)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AudioDeviceInfo(
    outputDevice: AudioDevice,
    bitPerfectState: BitPerfectState,
    onClick: () -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                alpha = SurfaceColorTokens.SurfaceVariantAlpha
            )
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = hasR(), onClick = onClick)
                .padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(outputDevice.type.iconRes),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = stringResource(R.string.listening_on),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = outputDevice.getDeviceName(LocalContext.current),
                    style = MaterialTheme.typography.bodyLargeEmphasized,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                AnimatedVisibility(
                    visible = bitPerfectState.isActive,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    ShapedText(
                        text = stringResource(
                            R.string.bit_perfect_info,
                            bitPerfectState.encodingLabel,
                            bitPerfectState.sampleRateLabel
                        ),
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun LabeledSwitch(
    checked: Boolean,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onStateChange: (Boolean) -> Unit
) {
    Row(
        modifier = modifier
            .clickable(
                enabled = enabled,
                role = Role.Switch,
                onClick = {
                    onStateChange(!checked)
                }
            )
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                maxLines = 1,
                style = MaterialTheme.typography.bodyMediumEmphasized,
                fontWeight = FontWeight.Medium
            )

            Text(
                text = description,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Switch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = {
                onStateChange(it)
            },
            thumbContent = {
                if (checked) {
                    Icon(
                        painter = painterResource(R.drawable.ic_check_24dp),
                        contentDescription = null,
                        modifier = Modifier.size(SwitchDefaults.IconSize)
                    )
                }
            }
        )
    }
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