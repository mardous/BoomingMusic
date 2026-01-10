package com.mardous.booming.ui.screen.sharesong

import android.content.Context
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mardous.booming.R
import com.mardous.booming.extensions.getShareNowPlayingIntent
import com.mardous.booming.extensions.getShareSongIntent
import com.mardous.booming.extensions.toChooser
import com.mardous.booming.ui.component.compose.BottomSheetDialogSurface
import com.mardous.booming.ui.screen.player.PlayerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareSongBottomSheet(
    playerViewModel: PlayerViewModel,
    context: Context
) {

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
                    text = stringResource(R.string.action_share),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Button(
                    onClick = {
                        context.startActivity(context.getShareSongIntent(playerViewModel.currentSong).toChooser())
                    },
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally),
                    enabled = true,
                    shape = RoundedCornerShape(30.dp),
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    Text(
                        text = context.getString(R.string.the_audio_file)
                    )
                }

                Button(
                    onClick = {
                        context.startActivity(context.getShareNowPlayingIntent(playerViewModel.currentSong).toChooser())
                    },
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally),
                    enabled = true,
                    shape = RoundedCornerShape(30.dp),
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    Text(
                        text = context.getString(R.string.i_am_listening)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}