package com.mardous.booming.core.appwidgets

import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.ContentScale
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.wrapContentSize
import androidx.glance.unit.ColorProvider
import com.mardous.booming.R
import com.mardous.booming.core.appwidgets.state.PlaybackState

@Composable
fun AlbumArtGlance(
    playbackState: PlaybackState,
    placeholderColor: ColorProvider = GlanceTheme.colors.tertiaryContainer,
    placeholderIconColor: ColorProvider = GlanceTheme.colors.onTertiaryContainer,
    modifier: GlanceModifier = GlanceModifier
) {
    val bitmap = playbackState.artworkData?.let {
        try {
            val options = BitmapFactory.Options()
            BitmapFactory.decodeByteArray(it, 0, it.size, options)
        } catch (t: Throwable) {
            Log.e("BoomingGlanceWidget", "Cannot decode artwork bitmap", t)
            null
        }
    }
    if (bitmap != null) {
        Image(
            provider = ImageProvider(bitmap),
            contentDescription = "Album Art",
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    } else {
        Box(
            modifier = modifier.background(placeholderColor),
            contentAlignment = Alignment.Center
        ) {
            Image(
                provider = ImageProvider(R.drawable.ic_music_note_24dp),
                colorFilter = ColorFilter.tint(placeholderIconColor),
                contentDescription = "Album Art",
                modifier = GlanceModifier.wrapContentSize()
            )
        }
    }
}

@Composable
fun ControlIconGlance(
    resId: Int,
    tint: ColorProvider,
    contentDescription: String,
    modifier: GlanceModifier = GlanceModifier
) {
    Image(
        provider = ImageProvider(resId),
        contentDescription = contentDescription,
        modifier = modifier,
        colorFilter = ColorFilter.tint(tint)
    )
}

@Composable
fun CircularControlIconGlance(
    resId: Int,
    size: Dp,
    iconTint: ColorProvider,
    backgroundTint: ColorProvider,
    contentDescription: String,
    onClick: GlanceModifier
) {
    ShapeableControlIconGlance(
        resId = resId,
        size = size,
        cornerRadius = size / 2,
        innerPadding = size / 6,
        iconTint = iconTint,
        backgroundTint = backgroundTint,
        contentDescription = contentDescription,
        onClick = onClick
    )
}

@Composable
fun ShapeableControlIconGlance(
    resId: Int,
    size: Dp = 48.dp,
    cornerRadius: Dp = 16.dp,
    innerPadding: Dp = 16.dp,
    iconTint: ColorProvider = GlanceTheme.colors.onPrimaryContainer,
    backgroundTint: ColorProvider = GlanceTheme.colors.primaryContainer,
    contentDescription: String? = null,
    onClick: GlanceModifier = GlanceModifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = GlanceModifier
            .size(size)
            .cornerRadius(cornerRadius)
            .padding(innerPadding)
            .background(backgroundTint)
    ) {
        Image(
            provider = ImageProvider(resId),
            contentDescription = contentDescription,
            modifier = GlanceModifier
                .fillMaxSize()
                .then(onClick),
            colorFilter = ColorFilter.tint(iconTint)
        )
    }
}