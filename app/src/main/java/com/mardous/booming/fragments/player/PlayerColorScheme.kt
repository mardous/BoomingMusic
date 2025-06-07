/*
 * Copyright (c) 2025 Christians Martínez Alvarado
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

package com.mardous.booming.fragments.player

import android.content.Context
import android.graphics.Color
import android.os.Build
import androidx.annotation.ColorInt
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import com.google.android.material.color.DynamicColors
import com.google.android.material.color.DynamicColorsOptions
import com.google.android.material.color.MaterialColors
import com.mardous.booming.R
import com.mardous.booming.extensions.isNightMode
import com.mardous.booming.extensions.resources.*
import com.mardous.booming.helper.color.MediaNotificationProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

typealias PlayerColorSchemeMode = PlayerColorScheme.Mode

typealias PlayerColorSchemeList = List<PlayerColorSchemeMode>

/**
 * Represents a cohesive set of UI colors tailored for the audio player interface.
 *
 * The color scheme is responsible for harmonizing media-derived colors (e.g. from album art)
 * with the app's current theme to achieve a visually pleasing and accessible appearance.
 * It ensures legibility, proper contrast, and consistency with Material Design principles.
 *
 * @property surfaceColor The background color used for player surfaces.
 * @property emphasisColor A visually prominent color used for highlights and accents.
 * @property primaryTextColor Main color used for text and foreground content.
 * @property secondaryTextColor Subtle color for less prominent text content.
 * @property primaryControlColor Color used for primary control icons and buttons (defaults to [primaryTextColor]).
 * @property secondaryControlColor Color used for secondary control elements (defaults to [secondaryTextColor]).
 *
 * @author Christians Martínez Alvarado (mardous)
 */
data class PlayerColorScheme(
    val mode: Mode,
    @ColorInt val surfaceColor: Int,
    @ColorInt val emphasisColor: Int,
    @ColorInt val primaryTextColor: Int,
    @ColorInt val secondaryTextColor: Int,
    @ColorInt val primaryControlColor: Int = primaryTextColor,
    @ColorInt val secondaryControlColor: Int = secondaryTextColor
) {

    enum class Mode(
        @StringRes val titleRes: Int,
        @StringRes val descriptionRes: Int,
        val preferredAnimDuration: Long = 500
    ) {
        AppTheme(
            R.string.player_color_mode_app_theme_title,
            R.string.player_color_mode_app_theme_description
        ),
        SimpleColor(
            R.string.player_color_mode_simple_color_title,
            R.string.player_color_mode_simple_color_description
        ),
        VibrantColor(
            R.string.player_color_mode_vibrant_color_title,
            R.string.player_color_mode_vibrant_color_description
        ),
        MaterialYou(
            R.string.player_color_mode_material_you_title,
            R.string.player_color_mode_material_you_description,
            preferredAnimDuration = 1000
        )
    }

    companion object {

        /**
         * Returns a default color scheme based on the current app theme.
         *
         * It retrieves standard theme attributes such as primary color, text color, etc.
         *
         * @param context Context used to resolve theme attributes.
         * @return A [PlayerColorScheme] derived from theme defaults.
         */
        fun themeColorScheme(context: Context, mode: Mode = Mode.AppTheme): PlayerColorScheme {
            val primaryTextColor = context.textColorPrimary()
            val controlColor = context.controlColorNormal().takeUnless { it == Color.TRANSPARENT }
                ?: primaryTextColor
            val secondaryControlColor = controlColor.withAlpha(0.2f)
            return PlayerColorScheme(
                mode = mode,
                surfaceColor = context.surfaceColor(),
                emphasisColor = context.primaryColor(),
                primaryTextColor = context.textColorPrimary(),
                secondaryTextColor = primaryTextColor,
                primaryControlColor = controlColor,
                secondaryControlColor = secondaryControlColor
            )
        }

        /**
         * Creates a color scheme using the raw colors extracted from media (album art, etc).
         *
         * @param color A [MediaNotificationProcessor] with extracted media colors.
         * @return A raw [PlayerColorScheme] using unmodified colors.
         */
        fun simpleColorScheme(
            context: Context,
            color: MediaNotificationProcessor
        ): PlayerColorScheme {
            return themeColorScheme(context).copy(
                mode = Mode.SimpleColor,
                emphasisColor = color.primaryTextColor
            )
        }

        /**
         * Creates a color scheme using the raw colors extracted from media (album art, etc).
         *
         * @param color A [MediaNotificationProcessor] with extracted media colors.
         * @return A raw [PlayerColorScheme] using unmodified colors.
         */
        fun vibrantColorScheme(color: MediaNotificationProcessor): PlayerColorScheme {
            return PlayerColorScheme(
                mode = Mode.VibrantColor,
                surfaceColor = color.backgroundColor,
                emphasisColor = color.backgroundColor,
                primaryTextColor = color.primaryTextColor,
                secondaryTextColor = color.secondaryTextColor
            )
        }

        /**
         * Generates a [PlayerColorScheme] approximating Material You dynamic theming,
         * intended for devices running Android 11 or below (where native dynamic color APIs are not available).
         *
         * This function blends the extracted album or media colors with the app's current theme surface color,
         * harmonizing and adjusting emphasis and text colors for visual coherence and sufficient contrast.
         *
         * - In light mode, the background (`surfaceColor`) is subtly tinted to retain a neutral appearance.
         * - The `emphasisColor` is derived by blending and adjusting saturation and luminance for visibility.
         * - `primaryTextColor` is blended toward the theme’s default to ensure legibility.
         * - All contrast adjustments aim for at least WCAG AA compliance where feasible.
         *
         * @param baseContext The context used to resolve theme and resource attributes.
         * @param color The [MediaNotificationProcessor] containing the colors extracted from media metadata.
         *
         * @return A [PlayerColorScheme] instance representing a balanced, theme-aware color palette.
         *
         * @see PlayerColorScheme
         * @see androidx.core.graphics.ColorUtils
         * @see com.google.android.material.color.MaterialColors.harmonize
         */
        fun emulatedDynamicColorScheme(
            baseContext: Context,
            color: MediaNotificationProcessor
        ): PlayerColorScheme {
            val themeSurfaceColor = if (baseContext.isNightMode) {
                ContextCompat.getColor(baseContext, R.color.footerDark)
            } else {
                ContextCompat.getColor(baseContext, R.color.footerLight)
            }

            val themePrimaryTextColor = getPrimaryTextColor(baseContext)

            val rawSurfaceColor = ColorUtils.blendARGB(color.backgroundColor, themeSurfaceColor, 0.96f)
            val rawEmphasisColor = ColorUtils.blendARGB(color.backgroundColor, themeSurfaceColor, 0.4f)

            val surfaceColor = MaterialColors.harmonize(rawSurfaceColor, themeSurfaceColor)
            val emphasisColor = rawEmphasisColor
                .ensureContrastAgainst(surfaceColor)
                .adjustSaturationIfTooHigh(surfaceColor, baseContext.isNightMode)
                .desaturateIfTooDarkComparedTo(surfaceColor)

            val primaryTextColor = ColorUtils.blendARGB(emphasisColor, themePrimaryTextColor, 0.84f)
                .ensureContrastAgainst(surfaceColor, 4.5)
            val secondaryTextColor = ColorUtils.setAlphaComponent(primaryTextColor, 0x99)

            return PlayerColorScheme(
                mode = Mode.MaterialYou,
                surfaceColor = surfaceColor,
                emphasisColor = emphasisColor,
                primaryTextColor = primaryTextColor,
                secondaryTextColor = secondaryTextColor,
            )
        }

        /**
         * Generates a [PlayerColorScheme] using Android 12+ dynamic color APIs (Material You).
         *
         * This method applies dynamic theming based on a `seedColor` and wraps the context accordingly.
         *
         * @param baseContext The context for theme resolution.
         * @param seedColor The base color used to derive a dynamic palette.
         * @return A [PlayerColorScheme] based on the system's dynamic color generation.
         */
        @RequiresApi(Build.VERSION_CODES.S)
        suspend fun dynamicColorScheme(
            baseContext: Context,
            seedColor: Int
        ) = withContext(Dispatchers.IO) {
            val options = DynamicColorsOptions.Builder()
                .setContentBasedSource(seedColor)
                .build()

            themeColorScheme(
                context = DynamicColors.wrapContextIfAvailable(baseContext, options),
                mode = Mode.MaterialYou
            )
        }

        /**
         * Generates a [PlayerColorScheme] using system-provided Dynamic Colors (Material You) on Android 12+,
         * or falls back to a custom Material3-inspired emulation on older versions.
         *
         * This function automatically selects the appropriate color strategy based on API level.
         *
         * @param context A context used to resolve theme and colors.
         * @param mediaColor The [MediaNotificationProcessor] containing media-derived colors.
         *
         * @return A [PlayerColorScheme] suitable for the current device and theme.
         */
        suspend fun autoDynamicColorScheme(
            context: Context,
            mediaColor: MediaNotificationProcessor
        ): PlayerColorScheme {
            return if (DynamicColors.isDynamicColorAvailable()) {
                dynamicColorScheme(context, mediaColor.backgroundColor)
            } else {
                emulatedDynamicColorScheme(context, mediaColor)
            }
        }

        suspend fun autoColorScheme(
            context: Context,
            mediaColor: MediaNotificationProcessor,
            schemeMode: PlayerColorSchemeMode
        ): PlayerColorScheme {
            val colorScheme = when (schemeMode) {
                Mode.AppTheme -> themeColorScheme(context)
                Mode.SimpleColor -> simpleColorScheme(context, mediaColor)
                Mode.VibrantColor -> vibrantColorScheme(mediaColor)
                Mode.MaterialYou -> autoDynamicColorScheme(context, mediaColor)
            }
            check(schemeMode == colorScheme.mode)
            return colorScheme
        }
    }
}