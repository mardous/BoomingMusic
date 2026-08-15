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
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mardous.booming.core.model.shuffle.ShuffleOperationState
import com.mardous.booming.core.model.shuffle.SpecialShuffleMode
import com.mardous.booming.presentation.components.BottomSheetDialogSurface
import com.mardous.booming.presentation.viewmodel.LibraryViewModel
import com.mardous.booming.presentation.viewmodel.PlayerViewModel
import com.mardous.booming.presentation.viewmodel.ReloadType

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

    var maxItemHeight by remember { mutableIntStateOf(0) }

    BottomSheetDialogSurface {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(bottom = 16.dp)
                .padding(horizontal = 16.dp)
                .nestedScroll(rememberNestedScrollInteropConnection())
        ) {
            BottomSheetDefaults.DragHandle(
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp)),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(modes) { mode ->
                    ShuffleModeItem(
                        mode = mode,
                        isEnabled = allSongs.isNotEmpty() && !isBusy,
                        isShuffling = shuffleState.mode == mode,
                        onClick = {
                            playerViewModel.openSpecialShuffle(allSongs, mode)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (maxItemHeight > 0)
                                    with(LocalDensity.current) {
                                        Modifier.height(maxItemHeight.toDp())
                                    }
                                else Modifier
                            )
                            .onGloballyPositioned { coordinates ->
                                val height = coordinates.size.height
                                if (height > maxItemHeight) {
                                    maxItemHeight = height
                                }
                            }
                    )
                }
            }
        }
    }
}

@Composable
private fun ShuffleModeItem(
    mode: SpecialShuffleMode,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    isShuffling: Boolean = false,
    onClick: () -> Unit
) {
    val animatedAlpha by animateFloatAsState(
        targetValue = if (isEnabled) 1f else 0.5f,
        animationSpec = tween(500)
    )
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = .75f),
        shape = RoundedCornerShape(4.dp),
        modifier = modifier.clickable(enabled = isEnabled, onClick = onClick)
    ) {
        Row(
            modifier = modifier
                .alpha(animatedAlpha)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Crossfade(
                targetState = isShuffling,
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
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Column {
                Text(
                    text = stringResource(mode.titleRes),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = stringResource(mode.descriptionRes),
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}