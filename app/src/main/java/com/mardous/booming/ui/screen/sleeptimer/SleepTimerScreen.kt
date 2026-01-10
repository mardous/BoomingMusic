package com.mardous.booming.ui.screen.sleeptimer

import android.content.Context
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mardous.booming.R
import com.mardous.booming.extensions.resources.withAlpha
import com.mardous.booming.playback.SleepTimer
import com.mardous.booming.ui.component.compose.BottomSheetDialogSurface
import com.mardous.booming.ui.component.compose.TitleShapedText
import com.mardous.booming.ui.component.compose.TitledSurface
import com.mardous.booming.util.Preferences
import kotlin.math.round

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepTimerBottomSheet(
    timerViewModel: SleepTimerViewModel,
    context: Context,
    sleepTimer: SleepTimer
) {
    var sliderPosition by remember { mutableFloatStateOf(Preferences.lastSleepTimerValue.toFloat()) }
    var checkedState by remember { mutableStateOf(Preferences.isSleepTimerFinishMusic) }
    var buttonText by remember { mutableStateOf(if (sleepTimer.isRunning) context.getString(R.string.sleep_timer_cancel_current_timer) else context.getString(
        R.string.sleep_timer_set_action
    )) }

    BottomSheetDialogSurface {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    text = stringResource(R.string.action_sleep_timer),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                TitledSurface(
                    title = "",
                    titleEndContent = {
                        TitleShapedText(
                            "${round(sliderPosition).toInt()} mins"
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Slider(
                            steps = 16,
                            value = sliderPosition,
                            onValueChange = {
                                sliderPosition = round(it)
                                Preferences.lastSleepTimerValue = round(it).toInt()
                            }, // This updates the state and triggers recomposition
                            valueRange = 5f..90f,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically // Aligns the checkbox and text vertically in the center
                        ) {
                            Checkbox(
                                checked = checkedState, // Current state
                                onCheckedChange = { isChecked ->
                                    checkedState = isChecked // Update the state when clicked
                                    Preferences.isSleepTimerFinishMusic = isChecked
                                },
                            )
                            Text(
                                text = context.getString(R.string.sleep_timer_finish_current_music),
                                modifier = Modifier.padding(start = 8.dp) // Add spacing between checkbox and text
                            )
                        }

                        Button(
                            onClick = {
                                if (sleepTimer.isRunning) timerViewModel.cancelTimer(
                                    context,
                                    sleepTimer
                                ) else timerViewModel.startTimer(context, sleepTimer)
                                buttonText =
                                    if (sleepTimer.isRunning) context.getString(R.string.sleep_timer_cancel_current_timer) else context.getString(
                                        R.string.sleep_timer_set_action
                                    )
                            },
                            modifier = Modifier
                                .align(Alignment.End),
                            enabled = true,
                            shape = RoundedCornerShape(30.dp),
                            interactionSource = remember { MutableInteractionSource() }
                        ) {
                            Text(
                                text = buttonText
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}