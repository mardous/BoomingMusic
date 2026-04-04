package com.mardous.booming.core.appwidgets

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.TypedValue
import android.view.KeyEvent
import androidx.annotation.ColorInt
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.glance.GlanceTheme
import androidx.glance.action.Action
import androidx.glance.appwidget.action.actionStartService
import androidx.glance.color.ColorProvider
import androidx.glance.color.ColorProviders
import androidx.glance.color.colorProviders
import com.mardous.booming.core.appwidgets.state.WidgetTheme
import com.mardous.booming.playback.PlaybackService
import com.mardous.booming.ui.theme.PaletteStyle
import com.mardous.booming.ui.theme.dynamicColorSchemes

fun Dp.toPx(context: Context): Int {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        this.value,
        context.resources.displayMetrics
    ).toInt()
}

fun WidgetTheme(
    @ColorInt
    sourceColor: Int,
    style: PaletteStyle = PaletteStyle.Fidelity
): WidgetTheme {
    val colorSchemes = dynamicColorSchemes(
        keyColor = Color(sourceColor),
        style = style,
        contrastLevel = 0.75
    )
    return WidgetTheme(
        lightSurfaceColor = colorSchemes.lightColorScheme.surface.toArgb(),
        lightOnSurfaceColor = colorSchemes.lightColorScheme.onSurface.toArgb(),
        lightOnSurfaceVariantColor = colorSchemes.lightColorScheme.onSurfaceVariant.toArgb(),
        lightPrimaryColor = colorSchemes.lightColorScheme.primary.toArgb(),
        lightOnPrimaryColor = colorSchemes.lightColorScheme.onPrimary.toArgb(),
        lightPrimaryContainerColor = colorSchemes.lightColorScheme.primaryContainer.toArgb(),
        lightOnPrimaryContainerColor = colorSchemes.lightColorScheme.onPrimaryContainer.toArgb(),
        lightTertiaryContainerColor = colorSchemes.lightColorScheme.tertiaryContainer.toArgb(),
        lightOnTertiaryContainerColor = colorSchemes.lightColorScheme.onTertiaryContainer.toArgb(),
        darkSurfaceColor = colorSchemes.darkColorScheme.surface.toArgb(),
        darkOnSurfaceColor = colorSchemes.darkColorScheme.onSurface.toArgb(),
        darkOnSurfaceVariantColor = colorSchemes.darkColorScheme.onSurfaceVariant.toArgb(),
        darkPrimaryColor = colorSchemes.darkColorScheme.primary.toArgb(),
        darkOnPrimaryColor = colorSchemes.darkColorScheme.onPrimary.toArgb(),
        darkPrimaryContainerColor = colorSchemes.darkColorScheme.primaryContainer.toArgb(),
        darkOnPrimaryContainerColor = colorSchemes.darkColorScheme.onPrimaryContainer.toArgb(),
        darkTertiaryContainerColor = colorSchemes.darkColorScheme.tertiaryContainer.toArgb(),
        darkOnTertiaryContainerColor = colorSchemes.lightColorScheme.onTertiaryContainer.toArgb()
    )
}

@Composable
fun WidgetTheme?.getColors(): ColorProviders {
    val themeColors = GlanceTheme.colors
    return if (this == null) themeColors else {
        colorProviders(
            primary = ColorProvider(
                day = Color(lightPrimaryColor),
                night = Color(darkPrimaryColor)
            ),
            onPrimary = ColorProvider(
                day = Color(lightOnPrimaryColor),
                night = Color(darkOnPrimaryColor)
            ),
            primaryContainer = ColorProvider(
                day = Color(lightPrimaryContainerColor),
                night = Color(darkPrimaryContainerColor)
            ),
            onPrimaryContainer = ColorProvider(
                day = Color(lightOnPrimaryContainerColor),
                night = Color(darkOnPrimaryContainerColor)
            ),
            secondary = themeColors.secondary,
            onSecondary = themeColors.onSecondary,
            secondaryContainer = themeColors.secondaryContainer,
            onSecondaryContainer = themeColors.onSecondaryContainer,
            tertiary = themeColors.tertiary,
            onTertiary = themeColors.onTertiary,
            tertiaryContainer = ColorProvider(
                day = Color(lightTertiaryContainerColor),
                night = Color(darkTertiaryContainerColor)
            ),
            onTertiaryContainer = ColorProvider(
                day = Color(lightOnTertiaryContainerColor),
                night = Color(darkOnTertiaryContainerColor)
            ),
            error = themeColors.error,
            errorContainer = themeColors.errorContainer,
            onError = themeColors.onError,
            onErrorContainer = themeColors.onErrorContainer,
            background = themeColors.background,
            onBackground = themeColors.onBackground,
            surface = ColorProvider(
                day = Color(lightSurfaceColor),
                night = Color(darkSurfaceColor)
            ),
            onSurface = ColorProvider(
                day = Color(lightOnSurfaceColor),
                night = Color(darkOnSurfaceColor)
            ),
            surfaceVariant = themeColors.surfaceVariant,
            onSurfaceVariant = ColorProvider(
                day = Color(lightOnSurfaceVariantColor),
                night = Color(darkOnSurfaceVariantColor)
            ),
            outline = themeColors.outline,
            inverseOnSurface = themeColors.inverseOnSurface,
            inverseSurface = themeColors.inverseSurface,
            inversePrimary = themeColors.inversePrimary,
            widgetBackground = themeColors.widgetBackground
        )
    }
}

fun playbackAction(context: Context, mediaKeyCode: Int): Action {
    val intent = Intent(Intent.ACTION_MEDIA_BUTTON)
    intent.setComponent(ComponentName(context, PlaybackService::class.java))
    intent.putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent(KeyEvent.ACTION_DOWN, mediaKeyCode))
    return actionStartService(intent, true)
}

fun toggleShuffleAction(context: Context): Action {
    val intent = Intent(PlaybackService.ACTION_TOGGLE_SHUFFLE)
    intent.setComponent(ComponentName(context, PlaybackService::class.java))
    return actionStartService(intent)
}

fun cycleRepeatAction(context: Context): Action {
    val intent = Intent(PlaybackService.ACTION_CYCLE_REPEAT)
    intent.setComponent(ComponentName(context, PlaybackService::class.java))
    return actionStartService(intent)
}

fun toggleFavoriteAction(context: Context): Action {
    val intent = Intent(PlaybackService.ACTION_TOGGLE_FAVORITE)
    intent.setComponent(ComponentName(context, PlaybackService::class.java))
    return actionStartService(intent)
}