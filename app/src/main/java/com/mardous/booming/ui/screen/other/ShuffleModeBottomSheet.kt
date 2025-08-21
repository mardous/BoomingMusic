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

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mardous.booming.R
import com.mardous.booming.core.model.shuffle.ShuffleOperationState
import com.mardous.booming.core.model.shuffle.SpecialShuffleMode
import com.mardous.booming.ui.component.compose.lists.ShuffleModeItem
import com.mardous.booming.ui.screen.library.LibraryViewModel
import com.mardous.booming.ui.screen.library.ReloadType
import com.mardous.booming.ui.screen.player.PlayerViewModel

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

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth(),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item {
            Text(
                text = stringResource(R.string.advanced_shuffle_label),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp)
            )
        }
        items(modes) { mode ->
            ShuffleModeItem(
                mode = mode,
                isEnabled = allSongs.isNotEmpty() && !isBusy,
                isShuffling = shuffleState.mode == mode,
                onClick = {
                    playerViewModel.openSpecialShuffle(allSongs, mode)
                }
            )
        }
    }
}