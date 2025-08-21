package com.mardous.booming.ui.screen.lyrics

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mardous.booming.R
import com.mardous.booming.core.model.LibraryMargin
import com.mardous.booming.data.model.lyrics.Lyrics
import com.mardous.booming.ui.component.compose.color.primaryTextColor
import com.mardous.booming.ui.component.compose.decoration.FadingEdges
import com.mardous.booming.ui.component.compose.decoration.fadingEdges
import com.mardous.booming.ui.screen.library.LibraryViewModel
import com.mardous.booming.ui.screen.player.PlayerColorScheme
import com.mardous.booming.ui.screen.player.PlayerViewModel
import com.mardous.booming.ui.theme.PlayerTheme

@Composable
fun LyricsScreen(
    libraryViewModel: LibraryViewModel,
    lyricsViewModel: LyricsViewModel,
    playerViewModel: PlayerViewModel,
    onEditClick: () -> Unit
) {
    val miniPlayerMargin by libraryViewModel.getMiniPlayerMargin().observeAsState(LibraryMargin(0))
    val lyricsResult by lyricsViewModel.lyricsResult.collectAsState()
    val songProgress by playerViewModel.currentProgressFlow.collectAsState()

    val lyricsViewState = remember(lyricsResult.syncedLyrics) {
        LyricsViewState(lyricsResult.syncedLyrics.content)
    }

    LaunchedEffect(songProgress) {
        lyricsViewState.updatePosition(songProgress.toLong())
    }

    val plainScrollState = rememberScrollState()
    LaunchedEffect(lyricsResult.id) {
        plainScrollState.scrollTo(0)
    }

    Scaffold(
        contentWindowInsets = WindowInsets
            .navigationBars
            .add(WindowInsets(bottom = miniPlayerMargin.totalMargin)),
        floatingActionButton = {
            FloatingActionButton(
                onClick = onEditClick,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_edit_note_24dp),
                    contentDescription = stringResource(R.string.open_lyrics_editor)
                )
            }
        }
    ) { innerPadding ->
        LyricsSurface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            lyricsResult = lyricsResult,
            lyricsViewState = lyricsViewState,
            contentPadding = PaddingValues(
                top = 96.dp,
                bottom = dimensionResource(R.dimen.fab_size_padding),
                start = 16.dp,
                end = 16.dp
            ),
            plainScrollState = plainScrollState,
            onSeekToLine = { playerViewModel.seekTo(it.startAt) }
        )
    }
}

@Composable
fun CoverLyricsScreen(
    lyricsViewModel: LyricsViewModel,
    playerViewModel: PlayerViewModel,
    onExpandClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lyricsResult by lyricsViewModel.lyricsResult.collectAsState()
    val songProgress by playerViewModel.currentProgressFlow.collectAsStateWithLifecycle(
        initialValue = 0,
        minActiveState = Lifecycle.State.RESUMED
    )
    val lyricsViewState = remember(lyricsResult.syncedLyrics) {
        LyricsViewState(lyricsResult.syncedLyrics.content)
    }

    LaunchedEffect(songProgress) {
        lyricsViewState.updatePosition(songProgress.toLong())
    }

    val plainScrollState = rememberScrollState()
    LaunchedEffect(lyricsResult.id) {
        plainScrollState.scrollTo(0)
    }

    val playerColorScheme by playerViewModel.colorSchemeFlow.collectAsState(
        initial = PlayerColorScheme.themeColorScheme(LocalContext.current)
    )
    PlayerTheme(playerColorScheme) {
        Box(modifier = modifier.fillMaxSize()) {
            LyricsSurface(
                modifier = Modifier.fillMaxSize(),
                lyricsResult = lyricsResult,
                lyricsViewState = lyricsViewState,
                contentPadding = PaddingValues(vertical = 72.dp, horizontal = 8.dp),
                syncedContentColor = MaterialTheme.colorScheme.onSurface,
                syncedFadingEdges = FadingEdges(top = 72.dp, bottom = 64.dp),
                syncedFontSize = 24.sp,
                plainFontSize = 16.sp,
                plainTextAlign = TextAlign.Center,
                plainScrollState = plainScrollState,
                plainContentColor = MaterialTheme.colorScheme.onSurface,
                onSeekToLine = { playerViewModel.seekTo(it.startAt) }
            )

            FilledIconButton(
                modifier = Modifier
                    .wrapContentSize()
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.onSurface,
                    contentColor = MaterialTheme.colorScheme.onSurface.primaryTextColor()
                ),
                onClick = onExpandClick
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_open_in_full_24dp),
                    contentDescription = stringResource(R.string.open_lyrics_editor)
                )
            }
        }
    }
}

@Composable
private fun LyricsSurface(
    lyricsResult: LyricsResult,
    lyricsViewState: LyricsViewState,
    onSeekToLine: (Lyrics.Line) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues,
    syncedContentColor: Color = MaterialTheme.colorScheme.secondary,
    syncedFadingEdges: FadingEdges = FadingEdges(top = 56.dp, bottom = 32.dp),
    syncedFontSize: TextUnit = 30.sp,
    plainContentColor: Color = Color.Unspecified,
    plainScrollState: ScrollState = rememberScrollState(),
    plainFontSize: TextUnit = 20.sp,
    plainTextAlign: TextAlign? = null
) {
    Box(modifier) {
        if (lyricsResult.loading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            when {
                lyricsViewState.lyrics != null -> {
                    LyricsView(
                        state = lyricsViewState,
                        onLineClick = { onSeekToLine(it) },
                        fadingEdges = syncedFadingEdges,
                        fontSize = syncedFontSize,
                        contentColor = syncedContentColor,
                        contentPadding = contentPadding
                    )
                }

                !lyricsResult.plainLyrics.content.isNullOrBlank() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .nestedScroll(rememberNestedScrollInteropConnection())
                            .fadingEdges(syncedFadingEdges)
                            .verticalScroll(plainScrollState)
                            .padding(contentPadding)
                    ) {
                        Text(
                            text = lyricsResult.plainLyrics.content,
                            textAlign = plainTextAlign,
                            fontSize = plainFontSize,
                            color = plainContentColor,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                else -> {
                    Text(
                        text = stringResource(R.string.no_lyrics_found),
                        style = MaterialTheme.typography.bodyLarge,
                        color = plainContentColor,
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .align(Alignment.Center)
                    )
                }
            }
        }
    }
}