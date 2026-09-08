package com.mardous.booming.ui.component.compose

import androidx.annotation.DrawableRes
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import coil3.compose.AsyncImage
import com.mardous.booming.R

@Composable
fun MediaImage(
    model: Any?,
    modifier: Modifier = Modifier,
    placeholderIcon: Int = R.drawable.ic_music_note_24dp,
    contentDescription: String? = null
) {
    AsyncImage(
        model = model,
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        error = rememberMediaPlaceholderPainter(placeholderIcon),
        placeholder = rememberMediaPlaceholderPainter(placeholderIcon),
        modifier = modifier
    )
}

@Composable
fun rememberMediaPlaceholderPainter(
    @DrawableRes iconRes: Int,
    iconScale: Float = 0.5f,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    iconColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
): Painter {
    val iconPainter = painterResource(iconRes)
    return remember(iconPainter, backgroundColor, iconColor, iconScale) {
        MediaPlaceholderPainter(
            iconPainter = iconPainter,
            backgroundColor = backgroundColor,
            iconColor = iconColor,
            iconScale = iconScale
        )
    }
}

class MediaPlaceholderPainter(
    private val iconPainter: Painter,
    private val backgroundColor: Color,
    private val iconColor: Color,
    private val iconScale: Float = 0.5f
) : Painter() {

    override val intrinsicSize: Size = Size.Unspecified

    override fun DrawScope.onDraw() {
        drawRect(color = backgroundColor)

        val minDim = size.minDimension
        val iconSize = Size(minDim * iconScale, minDim * iconScale)

        translate(
            left = (size.width - iconSize.width) / 2f,
            top = (size.height - iconSize.height) / 2f
        ) {
            with(iconPainter) {
                draw(
                    size = iconSize,
                    colorFilter = ColorFilter.tint(iconColor)
                )
            }
        }
    }
}