package com.mardous.booming.ui.screen.ringtone

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.unit.dp
import com.mardous.booming.R
import com.mardous.booming.data.model.Song
import com.mardous.booming.ui.component.compose.BottomSheetDialogSurface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RingtonePermissionBottomSheet(
    song: Song?
) {
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
                Text(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    text = stringResource(R.string.permissions_denied),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = AnnotatedString.fromHtml(stringResource(R.string.permission_request_write_settings, song!!.title)),
                        modifier = Modifier.padding(start = 8.dp) // Add spacing between checkbox and text
                    )
                }

                Button(
                    onClick = {
                        try {
                            context.startActivity(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            })
                        } catch (_: ActivityNotFoundException) {
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.End),
                    enabled = true,
                    shape = RoundedCornerShape(30.dp),
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    Text(
                        text = stringResource(R.string.action_grant)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}