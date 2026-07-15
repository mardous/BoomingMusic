/*
 * Copyright (c) 2026 Christians Martínez Alvarado
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

package com.mardous.booming.ui.screen.queue

import android.annotation.SuppressLint
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TonalToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.media3.common.Player
import androidx.preference.PreferenceManager
import com.mardous.booming.R
import com.mardous.booming.data.model.QueueSong
import com.mardous.booming.data.model.Song
import com.mardous.booming.extensions.media.displayArtistName
import com.mardous.booming.extensions.media.songCountStr
import com.mardous.booming.extensions.media.songInfo
import com.mardous.booming.extensions.media.songsDurationStr
import com.mardous.booming.extensions.observeKeyAsState
import com.mardous.booming.extensions.showToast
import com.mardous.booming.extensions.utilities.buildInfoString
import com.mardous.booming.ui.component.compose.AnimatedEqBars
import com.mardous.booming.ui.component.compose.MediaImage
import com.mardous.booming.ui.component.compose.ObserveAsEvent
import com.mardous.booming.ui.component.compose.SmallHeader
import com.mardous.booming.ui.component.compose.menu.MenuDefaults
import com.mardous.booming.ui.component.compose.menu.MenuItem
import com.mardous.booming.ui.component.compose.menu.OverflowMenu
import com.mardous.booming.ui.screen.player.PlayerViewModel
import com.mardous.booming.ui.screen.player.QUEUE_DEBOUNCE
import com.mardous.booming.util.LOCKED_QUEUE
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import my.nanihadesuka.compose.LazyColumnScrollbar
import my.nanihadesuka.compose.ScrollbarSettings
import org.koin.compose.viewmodel.koinActivityViewModel
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import kotlin.time.Duration.Companion.milliseconds

@SuppressLint("LocalContextGetResourceValueCall")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueScreen(
    onAddToPlaylistClick: (List<Song>) -> Unit,
    onDeleteSongClick: (Song) -> Unit,
    onShareClick: (Song) -> Unit,
    onDetailsClick: (Song) -> Unit,
    playerViewModel: PlayerViewModel = koinActivityViewModel()
) {
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current

    val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    val coroutineScope = rememberCoroutineScope()

    val themeColors = MaterialTheme.colorScheme

    val isPlaying by playerViewModel.isPlayingFlow.collectAsState()
    val currentSong by playerViewModel.currentSongFlow.collectAsState()
    val position by playerViewModel.positionFlow.collectAsState()
    val playQueue by playerViewModel.queueFlow.collectAsState()
    val repeatMode by playerViewModel.repeatModeFlow.collectAsState()
    val shuffleMode by playerViewModel.shuffleModeFlow.collectAsState()

    val queueLocked by sharedPreferences.observeKeyAsState(LOCKED_QUEUE, false)

    var shouldLocateCurrentTrackOnUpdate by remember { mutableStateOf(false) }
    var showLocateCurrentTrack by remember { mutableStateOf(false) }

    var reorderInfo by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var reorderingQueue by remember { mutableStateOf<List<QueueSong>?>(null) }

    val listBottomPadding by animateDpAsState(
        targetValue = if (showLocateCurrentTrack) 96.dp else 16.dp,
        animationSpec = tween(200)
    )

    val listState = rememberLazyListState(position.current)
    val reorderableListState = rememberReorderableLazyListState(listState) { from, to ->
        hapticFeedback.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)

        reorderInfo = if (reorderInfo == null)
            Pair(from.index, to.index) else reorderInfo!!.first to to.index

        reorderingQueue = reorderingQueue?.toMutableList()?.apply {
            add(to.index, removeAt(from.index))
        }
    }

    ObserveAsEvent(playerViewModel.stopAfterPosition) { (title, canceled) ->
        if (title != null) {
            if (canceled) {
                context.showToast(context.getString(R.string.sleep_timer_stop_after_x_canceled, title))
            } else {
                context.showToast(context.getString(R.string.sleep_timer_stop_after_x, title))
            }
        }
    }

    fun toggleHapticFeedback(checked: Boolean) {
        if (checked) {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.ToggleOn)
        } else {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.ToggleOff)
        }
    }

    LaunchedEffect(playQueue) {
        delay(QUEUE_DEBOUNCE.milliseconds)
        reorderingQueue = null
    }

    LaunchedEffect(position, playQueue) {
        if (shouldLocateCurrentTrackOnUpdate ||
            listState.firstVisibleItemIndex == position.previous) {
            listState.animateScrollToItem(position.current)
            if (shouldLocateCurrentTrackOnUpdate) {
                shouldLocateCurrentTrackOnUpdate = false
            }
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo }
            .collect { itemInfos ->
                showLocateCurrentTrack = itemInfos.none { it.index == position.current }
            }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .nestedScroll(rememberNestedScrollInteropConnection())
    ) {
        BottomSheetDefaults.DragHandle(
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        SmallHeader(
            title = currentSong.title,
            subtitle = currentSong.displayArtistName(),
            imageModel = currentSong,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )

        Spacer(Modifier.height(16.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            TonalToggleButton(
                checked = shuffleMode,
                onCheckedChange = { checked ->
                    toggleHapticFeedback(checked)
                    shouldLocateCurrentTrackOnUpdate = true
                    playerViewModel.toggleShuffleMode()
                },
                shapes = ButtonGroupDefaults.connectedLeadingButtonShapes(),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_shuffle_24dp),
                    contentDescription = stringResource(R.string.action_toggle_shuffle)
                )
            }

            TonalToggleButton(
                checked = repeatMode != Player.REPEAT_MODE_OFF,
                onCheckedChange = { checked ->
                    toggleHapticFeedback(checked)
                    playerViewModel.cycleRepeatMode()
                },
                shapes = ButtonGroupDefaults.connectedMiddleButtonShapes(),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    painter = when(repeatMode) {
                        Player.REPEAT_MODE_ALL -> painterResource(R.drawable.ic_repeat_24dp)
                        Player.REPEAT_MODE_ONE -> painterResource(R.drawable.ic_repeat_one_24dp)
                        else -> painterResource(R.drawable.ic_repeat_24dp)
                    },
                    contentDescription = stringResource(R.string.action_cycle_repeat)
                )
            }

            TonalToggleButton(
                checked = queueLocked,
                onCheckedChange = { checked ->
                    toggleHapticFeedback(checked)
                    // TODO migrate to DataStore Preferences
                    sharedPreferences.edit {
                        putBoolean(LOCKED_QUEUE, checked)
                    }
                },
                shapes = ButtonGroupDefaults.connectedTrailingButtonShapes(),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    painter = if (queueLocked) {
                        painterResource(R.drawable.ic_lock_24dp)
                    } else {
                        painterResource(R.drawable.ic_lock_open_24dp)
                    },
                    contentDescription = stringResource(R.string.action_toggle_queue_lock))
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.up_next),
                    style = MaterialTheme.typography.titleMediumEmphasized,
                    color = themeColors.primary,
                    maxLines = 1
                )

                Text(
                    text = buildInfoString(
                        playQueue.songCountStr(context),
                        playQueue.songsDurationStr()
                    ),
                    style = MaterialTheme.typography.titleSmall,
                    color = themeColors.onSurface
                )
            }

            OverflowMenu(
                items = listOf(
                    MenuItem.Button.DropDown(
                        icon = painterResource(R.drawable.ic_playlist_add_24dp),
                        text = stringResource(R.string.action_add_to_playlist),
                        onClick = { onAddToPlaylistClick(playQueue) }
                    ),
                    MenuItem.Button.DropDown(
                        icon = painterResource(R.drawable.ic_clear_all_24dp),
                        text = stringResource(R.string.clear_queue),
                        dangerous = true,
                        onClick = { playerViewModel.clearQueue() }
                    ),
                ),
                colors = MenuDefaults.dropDownMenuColors(themeColors.surfaceContainerHigh)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box {
            LazyColumnScrollbar(
                state = listState,
                settings = ScrollbarSettings(
                    hideDelayMillis = 3000,
                    thumbMaxLength = 0.1f,
                    thumbSelectedColor = themeColors.primary,
                    thumbUnselectedColor = themeColors.primary
                )
            ) {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(8.dp) + PaddingValues(bottom = listBottomPadding),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    itemsIndexed(
                        items = reorderingQueue ?: playQueue,
                        key = { _, item -> item.key }
                    ) { index, song ->
                        ReorderableItem(reorderableListState, key = song.key) {
                            val isCurrentSong = remember(position) { index == position.current }

                            val alpha by animateFloatAsState(
                                targetValue = if (isCurrentSong || index >= position.current) 1f else .5f
                            )
                            val color by animateColorAsState(
                                targetValue = if (isCurrentSong) {
                                    themeColors.primaryContainer
                                } else {
                                    themeColors.surfaceContainer
                                },
                                animationSpec = tween(400)
                            )
                            val contentColor by animateColorAsState(
                                targetValue = if (isCurrentSong) {
                                    themeColors.onPrimaryContainer
                                } else {
                                    themeColors.onSurfaceVariant
                                },
                                animationSpec = tween(400)
                            )
                            val cornerRadius by animateDpAsState(
                                targetValue = if (isPlaying && isCurrentSong) 50.dp else 12.dp,
                                animationSpec = tween(400)
                            )
                            val imageCornerRadius by animateDpAsState(
                                targetValue = if (isPlaying && isCurrentSong) 50.dp else 8.dp,
                                animationSpec = tween(400)
                            )

                            Surface(
                                onClick = {
                                    if (isCurrentSong) {
                                        playerViewModel.togglePlayPause()
                                    } else {
                                        playerViewModel.playSongAt(index)
                                    }
                                },
                                shape = RoundedCornerShape(cornerRadius),
                                color = color.copy(alpha),
                                contentColor = contentColor.copy(alpha)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 8.dp)
                                ) {
                                    Crossfade(
                                        targetState = isPlaying && isCurrentSong,
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(RoundedCornerShape(imageCornerRadius))
                                    ) { isPlayingSong ->
                                        if (isPlayingSong) {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(themeColors.primary)
                                            ) {
                                                AnimatedEqBars(
                                                    color = themeColors.onPrimary,
                                                    isPlaying = isPlaying,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        } else {
                                            MediaImage(
                                                model = song,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                    }

                                    Spacer(Modifier.width(16.dp))

                                    Column(
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = song.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            overflow = TextOverflow.Ellipsis,
                                            maxLines = 1
                                        )

                                        Text(
                                            text = song.songInfo(),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = contentColor.copy(alpha = 0.8f),
                                            overflow = TextOverflow.Ellipsis,
                                            maxLines = 1
                                        )
                                    }

                                    Spacer(Modifier.width(8.dp))

                                    if (!queueLocked) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_drag_handle_24dp),
                                            contentDescription = stringResource(R.string.reorder_action),
                                            modifier = Modifier.draggableHandle(
                                                onDragStarted = {
                                                    hapticFeedback.performHapticFeedback(
                                                        HapticFeedbackType.GestureThresholdActivate
                                                    )
                                                    reorderInfo = null
                                                    reorderingQueue = playQueue
                                                },
                                                onDragStopped = {
                                                    hapticFeedback.performHapticFeedback(
                                                        HapticFeedbackType.GestureEnd
                                                    )
                                                    reorderInfo?.let { (from, to) ->
                                                        playerViewModel.moveSong(from, to)
                                                    }
                                                }
                                            )
                                        )
                                    }

                                    Spacer(Modifier.width(8.dp))

                                    QueueItemMenu(
                                        itemIndex = index,
                                        queueIndex = position.current,
                                        onStopAfterThisTrackClick = {
                                            playerViewModel.stopAt(index)
                                        },
                                        onPutAfterCurrentTrackClick = {
                                            playerViewModel.moveToNextPosition(index)
                                        },
                                        onRemoveFromQueueClick = {
                                            playerViewModel.removePosition(index)
                                        },
                                        onAddToPlaylistClick = {
                                            onAddToPlaylistClick(listOf(song))
                                        },
                                        onShareClick = {
                                            onShareClick(song)
                                        },
                                        onDeleteFromDeviceClick = {
                                            onDeleteSongClick(song)
                                        },
                                        onDetailsClick = {
                                            onDetailsClick(song)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = showLocateCurrentTrack,
                enter = slideInVertically { it },
                exit = slideOutVertically { it },
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .align(Alignment.BottomCenter)
            ) {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            listState.animateScrollToItem(position.current)
                        }
                    },
                    contentPadding = ButtonDefaults.MediumContentPadding
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_recenter_24dp),
                        contentDescription = null
                    )
                    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                    Text(stringResource(R.string.go_to_current_track))
                }
            }
        }
    }
}

@Composable
private fun QueueItemMenu(
    itemIndex: Int,
    queueIndex: Int,
    onStopAfterThisTrackClick: () -> Unit,
    onPutAfterCurrentTrackClick: () -> Unit,
    onRemoveFromQueueClick: () -> Unit,
    onAddToPlaylistClick: () -> Unit,
    onShareClick: () -> Unit,
    onDeleteFromDeviceClick: () -> Unit,
    onDetailsClick: () -> Unit
) {
    OverflowMenu(
        items = listOf(
            MenuItem.Button.DropDown(
                icon = painterResource(R.drawable.ic_stop_circle_24dp),
                text = stringResource(R.string.sleep_timer_stop_after_this_track),
                enabled = itemIndex >= queueIndex,
                onClick = onStopAfterThisTrackClick
            ),
            MenuItem.Button.DropDown(
                icon = painterResource(R.drawable.ic_queue_play_next_24dp),
                text = stringResource(R.string.put_after_current_track),
                enabled = itemIndex > (queueIndex + 1),
                onClick = onPutAfterCurrentTrackClick
            ),
            MenuItem.Button.DropDown(
                icon = painterResource(R.drawable.ic_remove_from_queue_24dp),
                text = stringResource(R.string.action_remove_from_playing_queue),
                onClick = onRemoveFromQueueClick
            ),
            MenuItem.Button.DropDown(
                icon = painterResource(R.drawable.ic_playlist_add_24dp),
                text = stringResource(R.string.action_add_to_playlist),
                onClick = onAddToPlaylistClick
            ),
            MenuItem.Button.DropDown(
                icon = painterResource(R.drawable.ic_share_24dp),
                text = stringResource(R.string.action_share),
                onClick = onShareClick
            ),
            MenuItem.Button.DropDown(
                icon = painterResource(R.drawable.ic_delete_24dp),
                text = stringResource(R.string.action_delete_from_device),
                onClick = onDeleteFromDeviceClick
            ),
            MenuItem.Button.DropDown(
                icon = painterResource(R.drawable.ic_info_24dp),
                text = stringResource(R.string.action_details),
                onClick = onDetailsClick
            )
        ),
        colors = MenuDefaults.dropDownMenuColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    )
}