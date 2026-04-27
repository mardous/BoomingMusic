/*
 * Copyright (c) 2024 Christians Martínez Alvarado
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

package com.mardous.booming.ui.dialogs.playlists

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.DialogFragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.mardous.booming.R
import com.mardous.booming.data.local.room.PlaylistWithSongs
import com.mardous.booming.data.model.Song
import com.mardous.booming.extensions.EXTRA_SONGS
import com.mardous.booming.extensions.extraNotNull
import com.mardous.booming.extensions.media.songCountStr
import com.mardous.booming.extensions.showToast
import com.mardous.booming.extensions.withArgs
import com.mardous.booming.ui.component.compose.BottomSheetDialogSurface
import com.mardous.booming.ui.component.compose.EmptyView
import com.mardous.booming.ui.component.compose.MediaImage
import com.mardous.booming.ui.screen.library.LibraryViewModel
import com.mardous.booming.ui.theme.BoomingMusicTheme
import org.koin.androidx.viewmodel.ext.android.activityViewModel

sealed class AddToPlaylistUiState(open val isLoading: Boolean) {
    data object Loading : AddToPlaylistUiState(true)
    data class Empty(val searchQuery: String?) : AddToPlaylistUiState(false)
    data class Ready(
        val playlists: List<PlaylistWithSongs>,
        override val isLoading: Boolean = false
    ) : AddToPlaylistUiState(isLoading)

    data class Completed(val isSuccess: Boolean) : AddToPlaylistUiState(false)
}

class AddToPlaylistDialog : BottomSheetDialogFragment() {

    private val libraryViewModel: LibraryViewModel by activityViewModel()
    private val songs by extraNotNull<List<Song>>(EXTRA_SONGS)

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        (dialog as? BottomSheetDialog)?.let {
            it.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        }
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
            )
            setContent {
                BoomingMusicTheme {
                    AddToPlaylistScreen(
                        songs = songs,
                        onCreatePlaylistClick = {
                            val dialog = CreatePlaylistDialog.create(songs)
                            dialog.callback(object : CreatePlaylistDialog.PlaylistCreatedCallback {
                                override fun playlistCreated() {
                                    dismiss()
                                }
                            })
                            dialog.show(childFragmentManager, "CREATE_PLAYLIST")
                        },
                        onDismiss = { dismiss() }
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
    @Composable
    private fun AddToPlaylistScreen(
        songs: List<Song>,
        onCreatePlaylistClick: () -> Unit,
        onDismiss: () -> Unit
    ) {
        val context = LocalContext.current
        val uiState by libraryViewModel.addToPlaylistUiState.collectAsState()

        val checkedPlaylists = rememberSaveable { mutableStateListOf<Long>() }
        var searchQuery by rememberSaveable { mutableStateOf("") }

        val listBottomPadding by animateDpAsState(
            targetValue = if (checkedPlaylists.isNotEmpty()) 116.dp else 16.dp,
            animationSpec = tween(1000)
        )

        LaunchedEffect(searchQuery) {
            if (uiState == null || uiState?.isLoading == false) {
                libraryViewModel.prepareToAddToPlaylist(searchQuery)
            }
        }

        DisposableEffect(Unit) {
            onDispose {
                libraryViewModel.finishAddingToPlaylists()
            }
        }

        BottomSheetDialogSurface {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .nestedScroll(rememberNestedScrollInteropConnection())
            ) {
                BottomSheetDefaults.DragHandle(
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Box {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            bottom = listBottomPadding
                        ),
                        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)
                    ) {
                        item {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = stringResource(R.string.select_playlists_title),
                                        style = MaterialTheme.typography.headlineSmallEmphasized,
                                        maxLines = 1
                                    )

                                    Text(
                                        text = songs.songCountStr(context),
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 1
                                    )
                                }

                                Button(
                                    onClick = onCreatePlaylistClick,
                                    contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                                    enabled = uiState?.isLoading?.not() ?: false
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_playlist_add_24dp),
                                        contentDescription = null
                                    )
                                    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                                    Text(stringResource(R.string.new_playlist_action))
                                }
                            }
                        }

                        item { Spacer(Modifier.height(16.dp)) }

                        item {
                            TextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                singleLine = true,
                                label = { Text(stringResource(R.string.search_playlists)) },
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_search_24dp),
                                        contentDescription = null
                                    )
                                },
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(
                                                painter = painterResource(R.drawable.ic_cancel_24dp),
                                                contentDescription = null
                                            )
                                        }
                                    }
                                },
                                shape = CircleShape,
                                colors = TextFieldDefaults.colors(
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    errorIndicatorColor = Color.Transparent,
                                    disabledIndicatorColor = Color.Transparent
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        item { Spacer(Modifier.height(16.dp)) }

                        when (val state = uiState) {
                            is AddToPlaylistUiState.Loading -> {
                                item {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 72.dp)
                                    ) {
                                        CircularWavyProgressIndicator()
                                    }
                                }
                            }

                            is AddToPlaylistUiState.Empty -> {
                                item {
                                    EmptyView(
                                        icon = painterResource(R.drawable.ic_queue_music_24dp),
                                        title = stringResource(R.string.no_playlists_label),
                                        subtitle = if (state.searchQuery.isNullOrBlank()) {
                                            stringResource(R.string.no_playlists_create_new)
                                        } else {
                                            stringResource(
                                                R.string.no_playlists_that_match_x,
                                                state.searchQuery
                                            )
                                        },
                                        modifier = Modifier.fillParentMaxSize()
                                    )
                                }
                            }

                            is AddToPlaylistUiState.Ready -> {
                                if (state.isLoading) {
                                    item {
                                        LinearWavyProgressIndicator(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(bottom = 16.dp)
                                        )
                                    }
                                }

                                itemsIndexed(
                                    state.playlists,
                                    { _, playlist -> playlist.playlistEntity.playListId }
                                ) { index, playlist ->
                                    val isSelected = checkedPlaylists.contains(playlist.playlistEntity.playListId)
                                    PlaylistItem(
                                        onClick = {
                                            if (isSelected) {
                                                checkedPlaylists.remove(playlist.playlistEntity.playListId)
                                            } else {
                                                checkedPlaylists.add(playlist.playlistEntity.playListId)
                                            }
                                        },
                                        index = index,
                                        count = state.playlists.size,
                                        selected = isSelected,
                                        enabled = !state.isLoading,
                                        playlist = playlist
                                    )
                                }
                            }

                            is AddToPlaylistUiState.Completed -> {
                                if (state.isSuccess) {
                                    context.showToast(R.string.songs_added_to_playlists)
                                }
                                onDismiss()
                            }

                            else -> {}
                        }
                    }

                    androidx.compose.animation.AnimatedVisibility(
                        visible = checkedPlaylists.isNotEmpty(),
                        enter = fadeIn() + slideInVertically { it },
                        exit = fadeOut() + slideOutVertically { it },
                        modifier = Modifier
                            .padding(bottom = 16.dp)
                            .align(Alignment.BottomCenter)
                    ) {
                        ElevatedCard(
                            shape = CircleShape
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(8.dp)
                            ) {
                                Text(
                                    text = pluralStringResource(
                                        R.plurals.x_playlists_selected,
                                        checkedPlaylists.size,
                                        checkedPlaylists.size
                                    ),
                                    style = MaterialTheme.typography.labelLarge,
                                    maxLines = 2,
                                    modifier = Modifier.padding(start = 8.dp)
                                )

                                Button(
                                    onClick = {
                                        libraryViewModel.addToPlaylists(checkedPlaylists, songs)
                                    },
                                    contentPadding = ButtonDefaults.MediumContentPadding
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_check_24dp),
                                        contentDescription = null
                                    )
                                    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                                    Text(stringResource(R.string.add_action))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    @Composable
    private fun PlaylistItem(
        onClick: () -> Unit,
        index: Int,
        count: Int,
        selected: Boolean,
        enabled: Boolean,
        playlist: PlaylistWithSongs,
        modifier: Modifier = Modifier
    ) {
        val imageCornerRadius by animateDpAsState(
            targetValue = if (selected) 50.dp else 8.dp,
            animationSpec = tween(500)
        )
        SegmentedListItem(
            checked = selected,
            onCheckedChange = {
                onClick()
            },
            shapes = ListItemDefaults.segmentedShapes(index, count),
            leadingContent = {
                MediaImage(
                    model = playlist,
                    placeholderIcon = R.drawable.ic_queue_music_24dp,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(imageCornerRadius))
                )
            },
            trailingContent = {
                Checkbox(
                    checked = selected,
                    enabled = enabled,
                    onCheckedChange = null
                )
            },
            colors = ListItemDefaults.segmentedColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ),
            enabled = enabled,
            modifier = modifier
        ) {
            Text(
                text = playlist.playlistEntity.playlistName,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1
            )
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        val fragment = childFragmentManager.findFragmentByTag("CREATE_PLAYLIST")
        if (fragment is DialogFragment) {
            fragment.dismiss()
        }
    }

    companion object {
        fun create(song: Song) = create(listOf(song))

        fun create(songs: List<Song>): AddToPlaylistDialog {
            return AddToPlaylistDialog().withArgs {
                putParcelableArrayList(EXTRA_SONGS, ArrayList(songs))
            }
        }
    }
}