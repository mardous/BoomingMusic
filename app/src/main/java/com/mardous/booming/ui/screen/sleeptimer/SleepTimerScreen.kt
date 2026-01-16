@file:SuppressLint("LocalContextGetResourceValueCall")
package com.mardous.booming.ui.screen.sleeptimer

import android.annotation.SuppressLint
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mardous.booming.R
import com.mardous.booming.extensions.showToast
import com.mardous.booming.ui.component.compose.BottomSheetDialogSurface
import kotlin.math.round

data class SleepTimerUiState(
    val isRunning: Boolean,
    val waitingFor: SleepTimerWaitingFor?,
    val isFinishMusic: Boolean,
    val timerValue: Float
)

sealed class SleepTimerWaitingFor {
    class Countdown(val formattedTimeUntilFinish: String) : SleepTimerWaitingFor()
    object PendingQuit : SleepTimerWaitingFor()
}

sealed class SleepTimerEvent {
    class Set(val minutes: Long) : SleepTimerEvent()
    object Canceled : SleepTimerEvent()
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SleepTimerBottomSheet(
    viewModel: SleepTimerViewModel
) {
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current

    val uiState by viewModel.uiState.collectAsState()
    val sleepTimerEvent by viewModel.sleepTimerEvent.collectAsState(null)

    LaunchedEffect(sleepTimerEvent) {
        sleepTimerEvent?.let { event ->
            when (event) {
                SleepTimerEvent.Canceled -> {
                    context.showToast(R.string.sleep_timer_canceled)
                }
                is SleepTimerEvent.Set -> {
                    context.showToast(context.getString(R.string.sleep_timer_set, event.minutes))
                }
            }
        }
    }

    val colors = MaterialTheme.colorScheme
    val animatedButtonContentColor by animateColorAsState(
        targetValue = if (uiState.isRunning) colors.onErrorContainer else colors.onPrimary,
        animationSpec = tween(300)
    )
    val animatedButtonColor by animateColorAsState(
        targetValue = if (uiState.isRunning) colors.errorContainer else colors.primary,
        animationSpec = tween(300)
    )
    val animatedButtonRadius by animateDpAsState(
        targetValue = if (uiState.isRunning) 16.dp else 50.dp,
        animationSpec = tween(300)
    )
    val animatedButtonPaddingHorizontal by animateDpAsState(
        targetValue = if (uiState.isRunning) 56.dp else 32.dp
    )
    val animatedButtonPaddingVertical by animateDpAsState(
        targetValue = if (uiState.isRunning) 24.dp else 16.dp
    )

    var sliderPosition by remember(uiState.timerValue) { mutableFloatStateOf(uiState.timerValue) }

    BottomSheetDialogSurface {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(horizontal = 24.dp)
                .padding(bottom = 16.dp)
        ) {
            BottomSheetDefaults.DragHandle(
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Text(
                text = stringResource(R.string.action_sleep_timer),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = when (val waitingFor = uiState.waitingFor) {
                            is SleepTimerWaitingFor.Countdown -> {
                                waitingFor.formattedTimeUntilFinish
                            }
                            is SleepTimerWaitingFor.PendingQuit -> {
                                stringResource(R.string.sleep_timer_waiting_pending_quit)
                            }
                            else -> {
                                stringResource(R.string.sleep_timer_x_mins, sliderPosition.toInt())
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .align(Alignment.End)
                    )

                    Slider(
                        value = sliderPosition,
                        onValueChange = { sliderPosition = round(it) },
                        onValueChangeFinished = {
                            viewModel.setTimerState(value = sliderPosition)
                            hapticFeedback.performHapticFeedback(
                                HapticFeedbackType.SegmentFrequentTick
                            )
                        },
                        valueRange = 1f..90f,
                        track = { sliderState ->
                            SliderDefaults.Track(
                                sliderState = sliderState,
                                modifier = Modifier.height(32.dp)
                            )
                        },
                        enabled = uiState.isRunning.not(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(4.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = {
                                sliderPosition = 15f
                                viewModel.setTimerState(value = sliderPosition)
                            },
                            shape = ButtonGroupDefaults.connectedLeadingButtonShape,
                            enabled = uiState.isRunning.not(),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = stringResource(R.string.sleep_timer_15_mins),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                sliderPosition = 45f
                                viewModel.setTimerState(value = sliderPosition)
                            },
                            shape = ShapeDefaults.Small,
                            enabled = uiState.isRunning.not(),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = stringResource(R.string.sleep_timer_45_mins),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                sliderPosition = 60f
                                viewModel.setTimerState(value = sliderPosition)
                            },
                            shape = ButtonGroupDefaults.connectedTrailingButtonShape,
                            enabled = uiState.isRunning.not(),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = stringResource(R.string.sleep_timer_1_hour),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(50))
                        .background(
                            MaterialTheme.colorScheme.surfaceContainerHighest.copy(
                                alpha = 0.6f
                            )
                        )
                        .toggleable(
                            enabled = uiState.isRunning.not(),
                            value = uiState.isFinishMusic,
                            role = Role.Switch,
                            onValueChange = { isChecked ->
                                viewModel.setTimerState(isFinishMusic = isChecked)
                            }
                        )
                        .padding(vertical = 16.dp)
                        .padding(start = 24.dp, end = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.sleep_timer_finish_current_music),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Switch(
                        checked = uiState.isFinishMusic,
                        onCheckedChange = null,
                    )
                }

                Button(
                    onClick = {
                        if (uiState.isRunning) {
                            viewModel.cancelTimer()
                        } else {
                            viewModel.startTimer()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = animatedButtonColor,
                        contentColor = animatedButtonContentColor
                    ),
                    contentPadding = PaddingValues(
                        horizontal = animatedButtonPaddingHorizontal,
                        vertical = animatedButtonPaddingVertical
                    ),
                    shape = RoundedCornerShape(animatedButtonRadius),
                ) {
                    if (uiState.isRunning) {
                        Icon(
                            painter = painterResource(R.drawable.ic_close_24dp),
                            contentDescription = null
                        )
                        Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                    }
                    Text(
                        text = if (uiState.isRunning) {
                            stringResource(R.string.sleep_timer_cancel_current_timer)
                        } else {
                            stringResource(R.string.sleep_timer_set_action)
                        }
                    )
                }
            }
        }
    }
}