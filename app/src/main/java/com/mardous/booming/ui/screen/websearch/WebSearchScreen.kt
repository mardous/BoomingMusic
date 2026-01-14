package com.mardous.booming.ui.screen.websearch

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
import com.mardous.booming.core.model.WebSearchEngine
import com.mardous.booming.data.model.Song
import com.mardous.booming.extensions.media.searchQuery
import com.mardous.booming.extensions.openWeb
import com.mardous.booming.extensions.toChooser
import com.mardous.booming.ui.component.compose.BottomSheetDialogSurface
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebSearchBottomSheet(
    song: Song?,
    context: Context
) {
    val engines = WebSearchEngine.entries.toTypedArray()

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
                    text = stringResource(R.string.web_search),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            context.startActivity(
                                song!!.searchQuery(engines[0]).openWeb().toChooser(context.getString(R.string.web_search))
                            )
                        },
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally),
                        enabled = true,
                        shape = RoundedCornerShape(30.dp),
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        Text(
                            text = WebSearchEngine.entries[0].name
                        )
                    }

                    Button(
                        onClick = {
                            context.startActivity(
                                song!!.searchQuery(engines[1]).openWeb().toChooser(context.getString(R.string.web_search))
                            )
                        },
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally),
                        enabled = true,
                        shape = RoundedCornerShape(30.dp),
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        Text(
                            text = WebSearchEngine.entries[1].name
                        )
                    }

                    Button(
                        onClick = {
                            context.startActivity(
                                song!!.searchQuery(engines[2]).openWeb().toChooser(context.getString(R.string.web_search))
                            )
                        },
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally),
                        enabled = true,
                        shape = RoundedCornerShape(30.dp),
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        Text(
                            text = WebSearchEngine.entries[2].name
                        )
                    }

                    Button(
                        onClick = {
                            context.startActivity(
                                song!!.searchQuery(engines[3]).openWeb().toChooser(context.getString(R.string.web_search))
                            )
                        },
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally),
                        enabled = true,
                        shape = RoundedCornerShape(30.dp),
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        Text(
                            text = WebSearchEngine.entries[3].name
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}