package com.mardous.booming.core.appwidgets.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.appwidget.ImageProvider
import androidx.glance.appwidget.cornerRadius
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.ContentScale
import androidx.glance.layout.size
import com.mardous.booming.R
import com.mardous.booming.coil.CoverProvider
import com.mardous.booming.core.appwidgets.WidgetColors

/** URI artwork avoids resending bitmaps */
@Composable
fun WidgetArtwork(
    songId: Long?,
    colors: WidgetColors,
    modifier: GlanceModifier,
    shape: WidgetShape = WidgetShape.Card,
    contentDescription: String? = null
) {
    // The scallop is cut into the image the provider serves
    val baked = shape == WidgetShape.Scallop
    val uri = songId?.let {
        CoverProvider.getImageUri(
            CoverProvider.SONG_COVER_PATH, it, if (baked) CoverProvider.SHAPE_COOKIE else null
        )
    }

    when {
        uri != null -> Image(
            provider = ImageProvider(uri),
            // Labeled by nearby track text
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = if (baked) modifier else modifier.cornerRadius(shape.radiusRes)
        )

        else -> Box(
            modifier = modifier.fillBackground(colors.control, shape),
            contentAlignment = Alignment.Center
        ) {
            Image(
                provider = ImageProvider(R.drawable.ic_music_note_24dp),
                contentDescription = contentDescription,
                colorFilter = ColorFilter.tint(colors.onControl),
                modifier = GlanceModifier.size(24.dp)
            )
        }
    }
}
