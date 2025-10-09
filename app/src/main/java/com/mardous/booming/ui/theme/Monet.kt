package com.mardous.booming.ui.theme

import android.app.UiModeManager
import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.getSystemService
import com.kyant.m3color.hct.Hct
import com.kyant.m3color.scheme.*

@Stable
@Composable
fun getSystemContrast(): Double {
    val context = LocalContext.current
    val uiManager = context.getSystemService<UiModeManager>()
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        uiManager?.contrast?.toDouble() ?: 0.0
    } else {
        0.0
    }
}

@Stable
fun dynamicColorScheme(
    keyColor: Color,
    isDark: Boolean,
    style: PaletteStyle,
    contrastLevel: Double
): ColorScheme {
    val hct = Hct.fromInt(keyColor.toArgb())
    val scheme = when (style) {
        PaletteStyle.TonalSpot -> SchemeTonalSpot(hct, isDark, contrastLevel)
        PaletteStyle.Neutral -> SchemeNeutral(hct, isDark, contrastLevel)
        PaletteStyle.Vibrant -> SchemeVibrant(hct, isDark, contrastLevel)
        PaletteStyle.Expressive -> SchemeExpressive(hct, isDark, contrastLevel)
        PaletteStyle.Rainbow -> SchemeRainbow(hct, isDark, contrastLevel)
        PaletteStyle.FruitSalad -> SchemeFruitSalad(hct, isDark, contrastLevel)
        PaletteStyle.Monochrome -> SchemeMonochrome(hct, isDark, contrastLevel)
        PaletteStyle.Fidelity -> SchemeFidelity(hct, isDark, contrastLevel)
        PaletteStyle.Content -> SchemeContent(hct, isDark, contrastLevel)
    }

    return ColorScheme(
        primary = scheme.primary.toColor(),
        onPrimary = scheme.onPrimary.toColor(),
        primaryContainer = scheme.primaryContainer.toColor(),
        onPrimaryContainer = scheme.onPrimaryContainer.toColor(),
        inversePrimary = scheme.inversePrimary.toColor(),
        secondary = scheme.secondary.toColor(),
        onSecondary = scheme.onSecondary.toColor(),
        secondaryContainer = scheme.secondaryContainer.toColor(),
        onSecondaryContainer = scheme.onSecondaryContainer.toColor(),
        tertiary = scheme.tertiary.toColor(),
        onTertiary = scheme.onTertiary.toColor(),
        tertiaryContainer = scheme.tertiaryContainer.toColor(),
        onTertiaryContainer = scheme.onTertiaryContainer.toColor(),
        background = scheme.background.toColor(),
        onBackground = scheme.onBackground.toColor(),
        surface = scheme.surface.toColor(),
        onSurface = scheme.onSurface.toColor(),
        surfaceVariant = scheme.surfaceVariant.toColor(),
        onSurfaceVariant = scheme.onSurfaceVariant.toColor(),
        surfaceTint = scheme.surfaceTint.toColor(),
        inverseSurface = scheme.inverseSurface.toColor(),
        inverseOnSurface = scheme.inverseOnSurface.toColor(),
        error = scheme.error.toColor(),
        onError = scheme.onError.toColor(),
        errorContainer = scheme.errorContainer.toColor(),
        onErrorContainer = scheme.onErrorContainer.toColor(),
        outline = scheme.outline.toColor(),
        outlineVariant = scheme.outlineVariant.toColor(),
        scrim = scheme.scrim.toColor(),
        surfaceBright = scheme.surfaceBright.toColor(),
        surfaceDim = scheme.surfaceDim.toColor(),
        surfaceContainer = scheme.surfaceContainer.toColor(),
        surfaceContainerHigh = scheme.surfaceContainerHigh.toColor(),
        surfaceContainerHighest = scheme.surfaceContainerHighest.toColor(),
        surfaceContainerLow = scheme.surfaceContainerLow.toColor(),
        surfaceContainerLowest = scheme.surfaceContainerLowest.toColor(),
        primaryFixed = scheme.primaryFixed.toColor(),
        primaryFixedDim = scheme.primaryFixedDim.toColor(),
        onPrimaryFixed = scheme.onPrimaryFixed.toColor(),
        onPrimaryFixedVariant = scheme.onPrimaryFixedVariant.toColor(),
        secondaryFixed = scheme.secondaryFixed.toColor(),
        secondaryFixedDim = scheme.secondaryFixedDim.toColor(),
        onSecondaryFixed = scheme.onSecondaryFixed.toColor(),
        onSecondaryFixedVariant = scheme.onSecondaryFixedVariant.toColor(),
        tertiaryFixed = scheme.tertiaryFixed.toColor(),
        tertiaryFixedDim = scheme.tertiaryFixedDim.toColor(),
        onTertiaryFixed = scheme.onTertiaryFixed.toColor(),
        onTertiaryFixedVariant = scheme.onTertiaryFixedVariant.toColor()
    )
}

@Suppress("NOTHING_TO_INLINE")
private inline fun Int.toColor(): Color = Color(this)

enum class PaletteStyle {
    TonalSpot,
    Neutral,
    Vibrant,
    Expressive,
    Rainbow,
    FruitSalad,
    Monochrome,
    Fidelity,
    Content,
}