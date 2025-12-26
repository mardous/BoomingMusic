package com.mardous.booming.ui.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import coil3.compose.AsyncImagePainter.State
import coil3.compose.LocalPlatformContext
import coil3.compose.rememberAsyncImagePainter
import coil3.compose.rememberConstraintsSizeResolver
import coil3.request.ImageRequest
import com.mardous.booming.R

@Composable
fun MediaImage(
    model: Any?,
    placeholderIcon: Int = R.drawable.ic_music_note_24dp,
    contentDescription: String? = null,
    modifier: Modifier = Modifier
) {
    val sizeResolver = rememberConstraintsSizeResolver()
    val painter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(LocalPlatformContext.current)
            .data(model)
            .size(sizeResolver)
            .build(),
        contentScale = ContentScale.Crop
    )
    val state by painter.state.collectAsState()
    when {
        state is State.Error || state is State.Loading -> {
            MediaPlaceholder(
                iconRes = placeholderIcon,
                modifier = modifier
            )
        }
        else -> {
            Image(
                painter = painter,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = modifier.then(sizeResolver)
            )
        }
    }
}

@Composable
fun MediaPlaceholder(
    @DrawableRes iconRes: Int,
    iconScale: Float = 0.5f,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxSize(iconScale)
        )
    }
}