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

package com.mardous.booming.ui.screen.player

import android.content.Context
import android.graphics.Color
import androidx.annotation.ColorInt
import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.kyant.m3color.hct.Hct
import com.kyant.m3color.scheme.SchemeContent
import com.mardous.booming.R
import com.mardous.booming.extensions.isNightMode
import com.mardous.booming.extensions.resources.*
import com.mardous.booming.extensions.systemContrast
import com.mardous.booming.ui.component.compose.color.onThis
import com.mardous.booming.util.color.MediaNotificationProcessor
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
@Immutable
data class PlayerColorScheme(
    val mode: Mode,
    val isDark: Boolean,
    @param:ColorInt val surfaceColor: Int,
    @param:ColorInt val emphasisColor: Int,
    @param:ColorInt val primaryTextColor: Int,
    @param:ColorInt val secondaryTextColor: Int,
    @param:ColorInt val primaryControlColor: Int = primaryTextColor,
    @param:ColorInt val secondaryControlColor: Int = secondaryTextColor
) {

    val primary = androidx.compose.ui.graphics.Color(emphasisColor)
    val onPrimary = androidx.compose.ui.graphics.Color(emphasisColor).onThis()
    val surface = androidx.compose.ui.graphics.Color(surfaceColor)
    val onSurface = androidx.compose.ui.graphics.Color(primaryTextColor)
    val onSurfaceVariant = androidx.compose.ui.graphics.Color(secondaryTextColor)

    enum class Mode(
        @param:StringRes val titleRes: Int,
        @param:StringRes val descriptionRes: Int,
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

        val Unspecified = PlayerColorScheme(
            mode = Mode.SimpleColor,
            isDark = false,
            surfaceColor = Color.TRANSPARENT,
            emphasisColor = Color.TRANSPARENT,
            primaryTextColor = Color.TRANSPARENT,
            secondaryTextColor = Color.TRANSPARENT
        )

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
                isDark = context.isNightMode,
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
        fun simpleColorScheme(context: Context, color: MediaNotificationProcessor): PlayerColorScheme {
            val themeColorScheme = themeColorScheme(context)
            val backgroundColor = themeColorScheme.surfaceColor
            val emphasisColor = color.primaryTextColor
                .ensureContrastAgainst(backgroundColor, 4.8)
                .adjustSaturationIfTooHigh(backgroundColor, context.isNightMode)
                .desaturateIfTooDarkComparedTo(backgroundColor)
            return themeColorScheme.copy(mode = Mode.SimpleColor, emphasisColor = emphasisColor)
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
                isDark = !color.backgroundColor.isColorLight,
                surfaceColor = color.backgroundColor,
                emphasisColor = color.backgroundColor,
                primaryTextColor = color.primaryTextColor,
                secondaryTextColor = color.secondaryTextColor,
                secondaryControlColor = color.secondaryTextColor.withAlpha(0.45f)
            )
        }

        /**
         * Generates a [PlayerColorScheme] using the m3color library (Monet). This allows us
         * to generate Material You-based colors for all devices, without directly relying
         * on Android 12 APIs.
         *
         * This method applies dynamic theming based on a `seedColor`.
         *
         * @param baseContext The context for theme resolution.
         * @param seedColor The base color used to derive a dynamic palette.
         * @return A [PlayerColorScheme] based on the system's dynamic color generation.
         */
        suspend fun dynamicColorScheme(
            baseContext: Context,
            seedColor: Int
        ) = withContext(Dispatchers.IO) {
            val sourceHct = Hct.fromInt(seedColor)
            val colorScheme = SchemeContent(
                sourceHct,
                baseContext.isNightMode,
                baseContext.systemContrast.toDouble()
            )
            PlayerColorScheme(
                mode = Mode.MaterialYou,
                isDark = colorScheme.isDark,
                surfaceColor = colorScheme.surface,
                emphasisColor = colorScheme.primary,
                primaryTextColor = colorScheme.onSurface,
                secondaryTextColor = colorScheme.onSurfaceVariant,
                secondaryControlColor = colorScheme.onSurfaceVariant.withAlpha(0.45f)
            )
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
                Mode.MaterialYou -> dynamicColorScheme(context, mediaColor.backgroundColor)
            }
            check(schemeMode == colorScheme.mode)
            return colorScheme
        }
    }
}