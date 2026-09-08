package com.mardous.booming.core.appwidgets

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.glance.GlanceTheme
import androidx.glance.color.ColorProvider as dayNightColorProvider
import androidx.glance.unit.ColorProvider
import android.util.LruCache
import androidx.annotation.ColorInt
import com.mardous.booming.core.appwidgets.state.PlaybackState
import com.mardous.booming.core.model.theme.ColorSchemes
import com.mardous.booming.ui.theme.PaletteStyle
import com.mardous.booming.ui.theme.dynamicColorSchemes

@Immutable
data class WidgetColors(
    val surface: ColorProvider,
    val onSurface: ColorProvider,
    val artist: ColorProvider,
    val accent: ColorProvider,
    val onAccent: ColorProvider,
    val control: ColorProvider,
    val onControl: ColorProvider,
    val trackRemaining: ColorProvider
)

/** Every role a widget paints */
@Composable
fun PlaybackState.widgetColors(useAlbumPalette: Boolean): WidgetColors {
    val seed = seedColor.takeIf { useAlbumPalette }
    return if (seed != null) albumColors(seed) else systemColors()
}

@Composable
private fun systemColors(): WidgetColors {
    val colors = GlanceTheme.colors
    return WidgetColors(
        surface = colors.widgetBackground,
        onSurface = colors.onSurface,
        artist = colors.onSurfaceVariant,
        accent = colors.primary,
        onAccent = colors.onPrimary,
        control = colors.tertiary,
        onControl = colors.onTertiary,
        trackRemaining = colors.outline
    )
}

/** Generated from the current track */
@Composable
private fun albumColors(@ColorInt seedColor: Int): WidgetColors {
    val schemes = widgetSchemes(seedColor)
    return WidgetColors(
        surface = schemes.pair { surfaceContainerHigh },
        onSurface = schemes.pair { onSurface },
        artist = schemes.pair { onSurfaceVariant },
        accent = schemes.pair { primary },
        onAccent = schemes.pair { onPrimary },
        control = schemes.pair { tertiary },
        onControl = schemes.pair { onTertiary },
        trackRemaining = schemes.pair { outline }
    )
}

/** Lets the launcher switch light and dark colors without waking the app. */
private fun ColorSchemes.pair(selector: ColorScheme.() -> Color): ColorProvider =
    dayNightColorProvider(
        day = lightColorScheme.selector(),
        night = darkColorScheme.selector()
    )

/** two full Material schemes per composition, and widgets can seed differently. */
private val schemeCache = LruCache<Int, ColorSchemes>(4)

private fun widgetSchemes(@ColorInt seedColor: Int): ColorSchemes =
    schemeCache.get(seedColor) ?: dynamicColorSchemes(
        keyColor = Color(seedColor),
        style = PaletteStyle.TonalSpot,
        contrastLevel = 0.0
    ).also { schemeCache.put(seedColor, it) }
