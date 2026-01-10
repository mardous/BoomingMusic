package com.mardous.booming.ui.screen.ringtone

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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.unit.dp
import com.mardous.booming.R
import com.mardous.booming.data.model.Song
import com.mardous.booming.extensions.media.configureRingtone
import com.mardous.booming.ui.component.compose.BottomSheetDialogSurface
import androidx.compose.ui.res.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RingtoneBottomSheet(
    song: Song?
) {
    var checkedState by remember { mutableStateOf(false) }
    val context = LocalContext.current

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
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        text = AnnotatedString.fromHtml(stringResource(R.string.x_will_be_set_as_ringtone, song!!.title)),
                        style = MaterialTheme.typography.titleLarge
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically // Aligns the checkbox and text vertically in the center
                    ) {
                        Checkbox(
                            checked = checkedState, // Current state
                            onCheckedChange = { isChecked ->
                                checkedState = isChecked // Update the state when clicked
                            }
                        )
                        Text(
                            text = stringResource(R.string.use_also_as_alarm_alert),
                            modifier = Modifier.padding(start = 8.dp) // Add spacing between checkbox and text
                        )
                    }

                    Button(
                        onClick = {
                            song.configureRingtone(context, checkedState)
                        },
                        modifier = Modifier
                            .align(Alignment.End),
                        enabled = true,
                        shape = RoundedCornerShape(30.dp),
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        Text(
                            text = stringResource(R.string.action_set_as_ringtone)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}