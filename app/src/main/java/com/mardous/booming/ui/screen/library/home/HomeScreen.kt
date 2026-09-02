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

package com.mardous.booming.ui.screen.library.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.preference.PreferenceManager
import com.mardous.booming.R
import com.mardous.booming.core.model.LibraryMargin
import com.mardous.booming.data.model.Album
import com.mardous.booming.data.model.ContentType
import com.mardous.booming.data.model.Song
import com.mardous.booming.data.model.Suggestion
import com.mardous.booming.extensions.observeKeyAsState
import com.mardous.booming.ui.component.compose.BoomingMusicTopAppBar
import com.mardous.booming.ui.component.compose.EmptyView
import com.mardous.booming.ui.component.compose.menu.MenuDefaults
import com.mardous.booming.ui.component.compose.menu.MenuItem
import com.mardous.booming.ui.component.compose.menu.TopAppBarMenu
import com.mardous.booming.ui.component.compose.rememberTopAppBarScrollBehavior
import com.mardous.booming.ui.component.views.TopAppBarLayout
import com.mardous.booming.ui.screen.library.LibraryViewModel
import com.mardous.booming.util.APPBAR_MODE
import com.mardous.booming.util.AppBarMode
import org.koin.compose.viewmodel.koinActivityViewModel
import org.koin.compose.viewmodel.koinViewModel

sealed class HomeUiState {
    data object Loading : HomeUiState()
    data object Empty : HomeUiState()
    data class Success(val suggestions: List<Suggestion>) : HomeUiState()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onActionClick: (ContentType) -> Unit,
    onShuffleClick: () -> Unit,
    onSongItemClick: (List<Song>, Int) -> Unit,
    onItemClick: (Any) -> Unit,
    onPlayAlbumClick: (Album) -> Unit,
    onOpenSuggestion: (Suggestion) -> Unit,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit,
    scrollToTopTrigger: Boolean = false,
    onScrollToTopDone: () -> Unit = {},
    homeViewModel: HomeViewModel = koinViewModel(),
    libraryViewModel: LibraryViewModel = koinActivityViewModel()
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    val uiState by homeViewModel.uiState.collectAsStateWithLifecycle()
    val miniPlayerMargin by libraryViewModel.getMiniPlayerMargin().observeAsState(LibraryMargin(0))

    val listState = rememberLazyListState()
    val bottomContentPadding by remember {
        derivedStateOf { with(density) { miniPlayerMargin.totalMargin.toDp() } + 16.dp }
    }

    val preferences = remember { PreferenceManager.getDefaultSharedPreferences(context) }
    val headerMode by preferences.observeKeyAsState(APPBAR_MODE, TopAppBarLayout.AppBarMode.SIMPLE) {
        if (preferences.getString(it, null) == AppBarMode.COMPACT) {
            TopAppBarLayout.AppBarMode.SIMPLE
        } else TopAppBarLayout.AppBarMode.COLLAPSING
    }

    val scrollingBehavior = rememberTopAppBarScrollBehavior(headerMode)

    LaunchedEffect(scrollToTopTrigger) {
        if (scrollToTopTrigger) {
            listState.animateScrollToItem(0)
            onScrollToTopDone()
        }
    }

    Scaffold(
        topBar = {
            BoomingMusicTopAppBar(
                title = {
                    Text(
                        text = buildAnnotatedString {
                            withStyle(
                                style = SpanStyle(color = MaterialTheme.colorScheme.onSurface)
                            ) { append("Booming") }
                            append(" ")
                            withStyle(
                                style = SpanStyle(
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            ) { append("Music") }
                        }
                    )
                },
                actions = {
                    TopAppBarMenu(
                        items = listOf(
                            MenuItem.Button.Action(
                                icon = painterResource(R.drawable.ic_search_24dp),
                                text = stringResource(R.string.search_label),
                                onClick = onSearchClick
                            ),
                            MenuItem.Button.Action(
                                icon = painterResource(R.drawable.ic_settings_24dp),
                                text = stringResource(R.string.settings_title),
                                onClick = onSettingsClick
                            )
                        ),
                        colors = MenuDefaults.topAppBarMenuColors(
                            actionContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        )
                    )
                },
                headerMode = headerMode,
                scrollBehavior = scrollingBehavior,
                contentPadding = PaddingValues(end = 8.dp)
            )
        },
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollingBehavior.nestedScrollConnection)
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when(val currentState = uiState) {
                is HomeUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                is HomeUiState.Empty -> {
                    EmptyView(
                        icon = painterResource(R.drawable.ic_library_music_24dp),
                        title = stringResource(R.string.home_empty_title),
                        subtitle = stringResource(R.string.home_empty_subtitle),
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                is HomeUiState.Success -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = bottomContentPadding)
                    ) {
                        item { HomeTopActions(onActionClick = onActionClick) }
                        items(currentState.suggestions) { suggestion ->
                            SuggestionSection(
                                suggestion = suggestion,
                                onSongItemClick = onSongItemClick,
                                onItemClick = onItemClick,
                                onPlayAlbumClick = onPlayAlbumClick,
                                onShuffleClick = onShuffleClick,
                                onOpenClick = { onOpenSuggestion(suggestion) }
                            )
                        }
                    }
                }
            }
        }
    }
}