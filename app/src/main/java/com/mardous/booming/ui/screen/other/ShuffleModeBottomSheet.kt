/*
 * Copyright (c) 2025 Christians Martínez Alvarado
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.mardous.booming.ui.screen.other

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mardous.booming.core.model.shuffle.ShuffleOperationState
import com.mardous.booming.core.model.shuffle.SpecialShuffleMode
import com.mardous.booming.ui.component.compose.BottomSheetDialogSurface
import com.mardous.booming.ui.screen.library.LibraryViewModel
import com.mardous.booming.ui.screen.library.ReloadType
import com.mardous.booming.ui.screen.player.PlayerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShuffleModeBottomSheet(
    libraryViewModel: LibraryViewModel,
    playerViewModel: PlayerViewModel,
    modes: Array<SpecialShuffleMode> = SpecialShuffleMode.entries.toTypedArray()
) {
    val allSongs by libraryViewModel.getSongs().observeAsState(emptyList())
    val shuffleState by playerViewModel.shuffleOperationState.collectAsState()

    val isBusy = shuffleState.status == ShuffleOperationState.Status.InProgress

    LaunchedEffect(Unit) {
        if (allSongs.isEmpty()) {
            libraryViewModel.forceReload(ReloadType.Songs)
        }
    }

    BottomSheetDialogSurface {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)
        ) {
            itemsIndexed(modes) { index, mode ->
                SegmentedListItem(
                    enabled = allSongs.isNotEmpty() && !isBusy,
                    shapes = ListItemDefaults.segmentedShapes(index, modes.size),
                    onClick = { playerViewModel.openSpecialShuffle(allSongs, mode) },
                    leadingContent = {
                        Crossfade(
                            targetState = shuffleState.mode == mode,
                            animationSpec = tween(500)
                        ) { loading ->
                            if (loading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    painter = painterResource(mode.iconRes),
                                    contentDescription = null
                                )
                            }
                        }
                    },
                    supportingContent = { Text(stringResource(mode.descriptionRes)) },
                    contentPadding = PaddingValues(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(mode.titleRes))
                }
            }
        }
    }
}