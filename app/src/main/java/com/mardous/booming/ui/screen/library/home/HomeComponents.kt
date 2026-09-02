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

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.carousel.HorizontalCenteredHeroCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.star
import com.mardous.booming.R
import com.mardous.booming.data.model.Album
import com.mardous.booming.data.model.Artist
import com.mardous.booming.data.model.ContentType
import com.mardous.booming.data.model.Song
import com.mardous.booming.data.model.Suggestion
import com.mardous.booming.extensions.media.displayArtistName
import com.mardous.booming.ui.component.compose.MediaImage
import com.mardous.booming.ui.component.compose.RoundedPolygonShape

private val HORIZONTAL_GRID_HEIGHT = 154.dp

@Composable
fun HomeTopActions(
    onActionClick: (ContentType) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        HomeActionButton(
            text = stringResource(R.string.top_tracks_label),
            icon = painterResource(R.drawable.ic_trending_up_24dp),
            onClick = { onActionClick(ContentType.TopTracks) },
            modifier = Modifier.weight(.33f)
        )
        HomeActionButton(
            text = stringResource(R.string.last_added_label),
            icon = painterResource(R.drawable.ic_library_add_24dp),
            onClick = { onActionClick(ContentType.RecentSongs) },
            modifier = Modifier.weight(.33f)
        )
        HomeActionButton(
            text = stringResource(R.string.history_label),
            icon = painterResource(R.drawable.ic_history_24dp),
            onClick = { onActionClick(ContentType.History) },
            modifier = Modifier.weight(.33f)
        )
    }
}

@Composable
private fun HomeActionButton(
    onClick: () -> Unit,
    text: String,
    icon: Painter,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    iconColor: Color = MaterialTheme.colorScheme.onSurface
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val iconBackgroundAlpha by animateFloatAsState(
        targetValue = if (isPressed) 0.8f else 1f,
        animationSpec = tween(500)
    )

    Box(
        modifier = modifier
            .padding(horizontal = 8.dp)
            .clickable(
                onClick = onClick,
                interactionSource = interactionSource,
                indication = null
            )
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Surface(
                shape = CircleShape,
                color = color.copy(alpha = iconBackgroundAlpha),
                contentColor = iconColor,
                modifier = Modifier.size(72.dp)
            ) {
                Box(Modifier.fillMaxSize()) {
                    Icon(
                        painter = icon,
                        contentDescription = null,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmallEmphasized,
                textAlign = TextAlign.Center,
                overflow = TextOverflow.Ellipsis,
                maxLines = 2
            )
        }
    }
}

@Composable
fun SuggestionSection(
    suggestion: Suggestion,
    onSongItemClick: (List<Song>, Int) -> Unit,
    onItemClick: (Any) -> Unit,
    onPlayAlbumClick: (Album) -> Unit,
    onShuffleClick: () -> Unit,
    onOpenClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cookiePolygon = remember {
        RoundedPolygon.star(
            numVerticesPerRadius = 12,
            innerRadius = 0.92f,
            rounding = CornerRounding(radius = 0.1f)
        )
    }
    val cookieShape = remember(cookiePolygon) {
        RoundedPolygonShape(cookiePolygon)
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val showOpenButton = suggestion.type == ContentType.TopAlbums ||
            suggestion.type == ContentType.TopArtists ||
            suggestion.type == ContentType.NotRecentlyPlayed

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = stringResource(suggestion.titleRes),
                    style = MaterialTheme.typography.titleMediumEmphasized,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(suggestion.subtitleRes),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (showOpenButton) {
                FilledIconButton(
                    onClick = onOpenClick,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                    shape = cookieShape
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_forward_24dp),
                        contentDescription = stringResource(R.string.action_more),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        when (suggestion.type) {
            ContentType.TopAlbums -> AlbumCarousel(
                albums = suggestion.items.filterIsInstance<Album>(),
                onAlbumClick = { onItemClick(it) },
                onPlayClick = { onPlayAlbumClick(it) }
            )

            ContentType.TopArtists -> LazyRow(
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                items(suggestion.items.filterIsInstance<Artist>()) { artist ->
                    ArtistItem(artist = artist, onClick = { onItemClick(artist) })
                }
            }

            ContentType.Favorites -> {
                if (isLandscape) {
                    ForYouHorizontalGrid(
                        songs = suggestion.items.filterIsInstance<Song>(),
                        onItemClick = onSongItemClick,
                        onShuffleClick = onShuffleClick
                    )
                } else {
                    ForYouBentoGrid(
                        songs = suggestion.items.filterIsInstance<Song>(),
                        onItemClick = onSongItemClick,
                        onShuffleClick = onShuffleClick
                    )
                }
            }

            ContentType.NotRecentlyPlayed -> {
                ForgottenTracksGrid(
                    songs = suggestion.items.filterIsInstance<Song>(),
                    onItemClick = onSongItemClick
                )
            }
            else -> {}
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlbumCarousel(
    albums: List<Album>,
    onAlbumClick: (Album) -> Unit,
    onPlayClick: (Album) -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val carouselState = rememberCarouselState { albums.size }
    HorizontalCenteredHeroCarousel(
        state = carouselState,
        maxItemWidth = 260.dp,
        itemSpacing = 8.dp,
        contentPadding = PaddingValues(horizontal = 8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(horizontal = 8.dp)
    ) { index ->
        val album = albums[index]
        AlbumCarouselItem(
            album = album,
            isCurrentItem = index == carouselState.currentItem,
            onClick = { onAlbumClick(album) },
            onPlayClick = { onPlayClick(album) },
            modifier = Modifier
                .height(if (isLandscape) 160.dp else 200.dp)
                .fillMaxWidth()
                .maskClip(MaterialTheme.shapes.medium)
        )
    }
}

@Composable
private fun AlbumCarouselItem(
    album: Album,
    isCurrentItem: Boolean,
    onClick: () -> Unit,
    onPlayClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            MediaImage(
                model = album,
                modifier = Modifier.fillMaxSize()
            )
            AnimatedContent(
                targetState = isCurrentItem,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300))
                        .togetherWith(fadeOut(animationSpec = tween(300)))
                },
                contentAlignment = Alignment.BottomCenter
            ) { canShowItemInfo ->
                Box(Modifier.fillMaxSize()) {
                    if (canShowItemInfo) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                                    )
                                )
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 16.dp)
                        ) {
                            Column(Modifier.fillMaxWidth(0.7f)) {
                                Text(
                                    text = album.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = album.artistName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.8f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            FilledIconButton(onClick = onPlayClick) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_play_24dp),
                                    contentDescription = stringResource(R.string.action_play_x, album.name)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ForYouHorizontalGrid(
    songs: List<Song>,
    onItemClick: (List<Song>, Int) -> Unit,
    onShuffleClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyHorizontalGrid(
        rows = GridCells.Fixed(2),
        contentPadding = PaddingValues(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier.height(HORIZONTAL_GRID_HEIGHT)
    ) {
        item(span = { GridItemSpan(2) }) {
            ShuffleBentoButton(
                onClick = onShuffleClick,
                modifier = Modifier.aspectRatio(1f)
            )
        }
        itemsIndexed(songs, key = { _, song -> song.id }) { index, song ->
            HorizontalGridItem(
                onClick = {
                    onItemClick(songs, index)
                },
                song = song,
                modifier = Modifier.width(200.dp)
            )
        }
    }
}

@Composable
private fun ForYouBentoGrid(
    songs: List<Song>,
    onItemClick: (List<Song>, Int) -> Unit,
    onShuffleClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (songs.size < Suggestion.FOR_YOU_MIN_ITEMS) return

    val horizontalItems: @Composable (IntRange) -> Unit = { indices ->
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            for (index in indices) {
                songs.getOrNull(index)?.let { song ->
                    SongBentoTile(
                        onClick = { onItemClick(songs, index) },
                        song = song,
                        modifier = Modifier
                            .aspectRatio(1f)
                            .weight(1f)
                    )
                }
            }
        }
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(.4f)
        ) {
            ShuffleBentoButton(
                onClick = onShuffleClick,
                modifier = Modifier.aspectRatio(1f)
            )
            horizontalItems(0..1)
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .weight(.2f)
                .padding(horizontal = 2.dp)
        ) {
            for (i in 2 until 5) {
                songs.getOrNull(i)?.let { song ->
                    SongBentoTile(
                        onClick = { onItemClick(songs, i) },
                        song = song,
                        modifier = Modifier
                            .aspectRatio(1f)
                    )
                }
            }
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(.4f)
        ) {
            horizontalItems(5..6)
            songs.getOrNull(7)?.let { song ->
                SongBentoTile(
                    onClick = { onItemClick(songs, 7) },
                    song = song,
                    modifier = Modifier.aspectRatio(1f)
                )
            }
        }
    }
}

@Composable
private fun ShuffleBentoButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val cornerRadius by animateDpAsState(if (isPressed) 32.dp else 24.dp)
    Box(
        modifier = modifier
            .background(
                MaterialTheme.colorScheme.primaryContainer,
                RoundedCornerShape(cornerRadius)
            )
            .clip(RoundedCornerShape(cornerRadius))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
    ) {
        Text(
            text = stringResource(R.string.home_shuffle_title),
            style = MaterialTheme.typography.titleLargeEmphasized,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 12.dp)
                .padding(bottom = 12.dp)
        )
    }
}

@Composable
private fun SongBentoTile(
    onClick: () -> Unit,
    song: Song,
    modifier: Modifier = Modifier,
    showOverlay: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val cornerRadius by animateDpAsState(if (isPressed) 20.dp else 16.dp)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .clickable(interactionSource = interactionSource, onClick = onClick)
    ) {
        MediaImage(
            model = song,
            modifier = Modifier.fillMaxSize()
        )
        if (showOverlay) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                            startY = 300f
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
            ) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artistName,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ForgottenTracksGrid(
    songs: List<Song>,
    onItemClick: (List<Song>, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyHorizontalGrid(
        rows = GridCells.Fixed(2),
        contentPadding = PaddingValues(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier.height(HORIZONTAL_GRID_HEIGHT)
    ) {
        itemsIndexed(songs, key = { _, item -> item.id }) { index, item ->
            HorizontalGridItem(
                onClick = { onItemClick(songs, index) },
                song = item,
                modifier = Modifier.width(200.dp)
            )
        }
    }
}

@Composable
private fun HorizontalGridItem(
    onClick: () -> Unit,
    song: Song,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        modifier = modifier
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(8.dp)
        ) {
            MediaImage(
                model = song,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
            )
            Column {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1
                )
                Text(
                    text = song.displayArtistName(),
                    style = MaterialTheme.typography.bodySmall,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun ArtistItem(
    artist: Artist,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(156.dp)
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        MediaImage(
            model = artist,
            modifier = Modifier
                .size(140.dp)
                .clip(CircleShape)
        )
        Text(
            text = artist.name,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 12.dp)
        )
    }
}
