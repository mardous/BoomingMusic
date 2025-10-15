package com.mardous.booming.ui.screen.lyrics

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.ui.keepScreenOn
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.toBitmap
import com.mardous.booming.R
import com.mardous.booming.core.model.LibraryMargin
import com.mardous.booming.data.model.lyrics.Lyrics
import com.mardous.booming.extensions.isPowerSaveMode
import com.mardous.booming.ui.component.compose.color.extractGradientColors
import com.mardous.booming.ui.component.compose.decoration.FadingEdges
import com.mardous.booming.ui.component.compose.decoration.animatedGradient
import com.mardous.booming.ui.component.compose.decoration.fadingEdges
import com.mardous.booming.ui.screen.library.LibraryViewModel
import com.mardous.booming.ui.screen.player.PlayerColorScheme
import com.mardous.booming.ui.screen.player.PlayerViewModel
import com.mardous.booming.ui.theme.PlayerTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun LyricsScreen(
    libraryViewModel: LibraryViewModel,
    lyricsViewModel: LyricsViewModel,
    playerViewModel: PlayerViewModel,
    onEditClick: () -> Unit
) {
    val context = LocalContext.current
    val isPowerSaveMode = context.isPowerSaveMode()

    val miniPlayerMargin by libraryViewModel.getMiniPlayerMargin().observeAsState(LibraryMargin(0))

    val lyricsViewSettings by lyricsViewModel.fullLyricsViewSettings.collectAsState()
    val lyricsResult by lyricsViewModel.lyricsResult.collectAsState()

    val isPlaying by playerViewModel.isPlayingFlow.collectAsState()

    val lyricsViewState = remember(lyricsResult.syncedLyrics) {
        LyricsViewState(lyricsResult.syncedLyrics.content)
    }

    val songProgress by playerViewModel.progressFlow.collectAsState()
    LaunchedEffect(songProgress) {
        lyricsViewState.updatePosition(songProgress)
    }

    val plainScrollState = rememberScrollState()
    LaunchedEffect(lyricsResult.id) {
        plainScrollState.scrollTo(0)
    }

    var gradientColors by remember { mutableStateOf<List<Color>>(emptyList()) }
    LaunchedEffect(lyricsResult.id) {
        if (!lyricsViewSettings.gradientBackground || isPowerSaveMode)
            return@LaunchedEffect

        withContext(Dispatchers.Default) {
            val result = SingletonImageLoader.get(context).execute(
                ImageRequest.Builder(context)
                    .data(playerViewModel.currentSong)
                    .build()
            )
            gradientColors = if (result is SuccessResult) {
                result.image.toBitmap().extractGradientColors()
            } else {
                emptyList()
            }
        }
    }

    val isGradientBackground by remember {
        derivedStateOf { lyricsViewSettings.gradientBackground && gradientColors.size >= 2 }
    }
    val contentColor = if (isGradientBackground) {
        Color.White
    } else {
        MaterialTheme.colorScheme.secondary
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
        },
        modifier = Modifier.keepScreenOn()
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            AnimatedContent(
                targetState = gradientColors,
                transitionSpec = {
                    fadeIn(tween(1000)).togetherWith(fadeOut(tween(1000)))
                }
            ) { colors ->
                if (isGradientBackground) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .animatedGradient(colors, isPlaying)
                    )
                }
            }

            LyricsSurface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                result = lyricsResult,
                state = lyricsViewState,
                settings = lyricsViewSettings,
                contentColor = contentColor,
                fadingEdges = FadingEdges(top = 56.dp, bottom = 32.dp),
                scrollState = plainScrollState,
                textAlign = TextAlign.Start,
                isPowerSaveMode = isPowerSaveMode,
                hasGradientBackground = isGradientBackground
            ) { playerViewModel.seekTo(it.startAt) }
        }
    }
}

@Composable
fun CoverLyricsScreen(
    lyricsViewModel: LyricsViewModel,
    playerViewModel: PlayerViewModel,
    onExpandClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isPowerSaveMode = context.isPowerSaveMode()

    val lyricsViewSettings by lyricsViewModel.playerLyricsViewSettings.collectAsState()

    val lyricsResult by lyricsViewModel.lyricsResult.collectAsState()
    val songProgress by playerViewModel.progressFlow.collectAsStateWithLifecycle(
        initialValue = 0,
        minActiveState = Lifecycle.State.RESUMED
    )
    val lyricsViewState = remember(lyricsResult.syncedLyrics) {
        LyricsViewState(lyricsResult.syncedLyrics.content)
    }

    LaunchedEffect(songProgress) {
        lyricsViewState.updatePosition(songProgress)
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
                result = lyricsResult,
                state = lyricsViewState,
                settings = lyricsViewSettings,
                contentColor = MaterialTheme.colorScheme.onSurface,
                fadingEdges = FadingEdges(top = 72.dp, bottom = 64.dp),
                scrollState = plainScrollState,
                textAlign = TextAlign.Center,
                isPowerSaveMode = isPowerSaveMode,
                hasGradientBackground = false
            ) { playerViewModel.seekTo(it.startAt) }

            FilledIconButton(
                modifier = Modifier
                    .wrapContentSize()
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.onSurface,
                    contentColor = MaterialTheme.colorScheme.surface
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
    result: LyricsResult,
    state: LyricsViewState,
    settings: LyricsViewSettings,
    contentColor: Color,
    fadingEdges: FadingEdges,
    scrollState: ScrollState,
    textAlign: TextAlign?,
    isPowerSaveMode: Boolean,
    hasGradientBackground: Boolean,
    modifier: Modifier = Modifier,
    onSeekToLine: (Lyrics.Line) -> Unit
) {
    CompositionLocalProvider(
        LocalContentColor provides contentColor
    ) {
        Box(modifier) {
            if (result.loading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                when {
                    state.lyrics != null -> {
                        LyricsView(
                            state = state,
                            settings = settings,
                            fadingEdges = fadingEdges,
                            isPowerSaveMode = isPowerSaveMode,
                            hasGradientBackground = hasGradientBackground
                        ) { onSeekToLine(it) }
                    }

                    !result.plainLyrics.content.isNullOrBlank() -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .nestedScroll(rememberNestedScrollInteropConnection())
                                .fadingEdges(fadingEdges)
                                .verticalScroll(scrollState)
                                .padding(settings.contentPadding)
                        ) {
                            Text(
                                text = result.plainLyrics.content,
                                color = contentColor,
                                textAlign = textAlign,
                                style = settings.unsyncedStyle,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    else -> {
                        Text(
                            text = stringResource(R.string.no_lyrics_found),
                            color = contentColor,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .align(Alignment.Center)
                        )
                    }
                }
            }
        }
    }
}