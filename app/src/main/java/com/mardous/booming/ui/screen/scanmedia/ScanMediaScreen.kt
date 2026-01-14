package com.mardous.booming.ui.screen.scanmedia

import androidx.appcompat.app.AppCompatActivity
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mardous.booming.R
import com.mardous.booming.extensions.showToast
import com.mardous.booming.ui.component.compose.BottomSheetDialogSurface
import com.mardous.booming.ui.screen.library.LibraryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanMediaBottomSheet(
    libraryViewModel: LibraryViewModel,
    owner: AppCompatActivity
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
                    text = stringResource(R.string.scan_media),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = stringResource(R.string.scan_media_message),
                    modifier = Modifier.padding(start = 8.dp) // Add spacing between checkbox and text
                )

                Button(
                    onClick = {
                        libraryViewModel.scanAllPaths(context).observe(owner) {
                            // TODO show detailed info about scanned songs
                            context.showToast(R.string.scan_finished)
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.End),
                    enabled = true,
                    shape = RoundedCornerShape(30.dp),
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    Text(
                        text = stringResource(R.string.scan_media_positive)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}