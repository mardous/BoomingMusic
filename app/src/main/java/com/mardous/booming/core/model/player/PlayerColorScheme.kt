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

package com.mardous.booming.core.model.player

import android.content.Context
import android.graphics.Color
import androidx.annotation.ColorInt
import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.core.graphics.ColorUtils
import com.kyant.m3color.hct.Hct
import com.kyant.m3color.scheme.SchemeContent
import com.mardous.booming.R
import com.mardous.booming.core.model.PaletteColor
import com.mardous.booming.extensions.isNightMode
import com.mardous.booming.extensions.resolveColor
import com.mardous.booming.extensions.resources.*
import com.mardous.booming.extensions.systemContrast
import com.mardous.booming.ui.component.compose.color.onThis
import com.mardous.booming.util.Preferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.google.android.material.R as M3R

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
 * @property primaryColor A visually prominent color used for highlights and accents.
 * @property onSurfaceColor Main color used for foreground content.
 * @property onSurfaceVariantColor Subtle color for less prominent foreground content.
 *
 * @author Christians Martínez Alvarado (mardous)
 */
@Immutable
data class PlayerColorScheme(
    val mode: Mode,
    val appThemeToken: AppThemeToken,
    @param:ColorInt val surfaceColor: Int,
    @param:ColorInt val surfaceContainerColor: Int,
    @param:ColorInt val primaryColor: Int,
    @param:ColorInt val tonalColor: Int,
    @param:ColorInt val onSurfaceColor: Int,
    @param:ColorInt val onSurfaceVariantColor: Int
) {

    val primary = androidx.compose.ui.graphics.Color(primaryColor)
    val onPrimary = androidx.compose.ui.graphics.Color(primaryColor).onThis()
    val surface = androidx.compose.ui.graphics.Color(surfaceColor)
    val onSurface = androidx.compose.ui.graphics.Color(onSurfaceColor)
    val onSurfaceVariant = androidx.compose.ui.graphics.Color(onSurfaceVariantColor)

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

    class AppThemeToken(private val isDark: Boolean, private val isMaterialYou: Boolean) {
        fun isValid(context: Context): Boolean {
            return context.isNightMode == isDark && Preferences.isMaterialYouTheme == isMaterialYou
        }

        companion object {
            val None = AppThemeToken(isDark = false, isMaterialYou = false)
        }
    }

    companion object {

        val Unspecified = PlayerColorScheme(
            mode = Mode.SimpleColor,
            appThemeToken = AppThemeToken.None,
            surfaceColor = Color.TRANSPARENT,
            surfaceContainerColor = Color.TRANSPARENT,
            primaryColor = Color.TRANSPARENT,
            tonalColor = Color.TRANSPARENT,
            onSurfaceColor = Color.TRANSPARENT,
            onSurfaceVariantColor = Color.TRANSPARENT
        )

        /**
         * Returns a default color scheme based on the current app theme.
         *
         * It retrieves standard theme attributes such as primary color, text color, etc.
         *
         * @param context Context used to resolve theme attributes.
         * @return A [PlayerColorScheme] derived from theme defaults.
         */
        fun themeColorScheme(context: Context): PlayerColorScheme {
            val onSurfaceColor = context.onSurfaceColor()
            val onSurfaceVariantColor = onSurfaceColor.withAlpha(0.6f)
            return PlayerColorScheme(
                mode = Mode.AppTheme,
                appThemeToken = AppThemeToken(
                    isDark = context.isNightMode,
                    isMaterialYou = Preferences.isMaterialYouTheme
                ),
                surfaceColor = context.surfaceColor(),
                surfaceContainerColor = context.resolveColor(M3R.attr.colorSurfaceContainerHigh),
                primaryColor = context.primaryColor(),
                tonalColor = context.resolveColor(M3R.attr.colorSecondaryContainer),
                onSurfaceColor = onSurfaceColor,
                onSurfaceVariantColor = onSurfaceVariantColor
            )
        }

        /**
         * Creates a color scheme using the raw colors extracted from media (album art, etc).
         *
         * @param color A [PaletteColor] with extracted media colors.
         * @return A raw [PlayerColorScheme] using unmodified colors.
         */
        fun simpleColorScheme(context: Context, color: PaletteColor): PlayerColorScheme {
            val themeColorScheme = themeColorScheme(context)
            val backgroundColor = themeColorScheme.surfaceColor
            val emphasisColor = color.primaryTextColor
                .ensureContrastAgainst(backgroundColor, 4.8)
                .adjustSaturationIfTooHigh(backgroundColor, context.isNightMode)
                .desaturateIfTooDarkComparedTo(backgroundColor)
            return themeColorScheme.copy(mode = Mode.SimpleColor, primaryColor = emphasisColor)
        }

        /**
         * Creates a color scheme using the raw colors extracted from media (album art, etc).
         *
         * @param color A [PaletteColor] with extracted media colors.
         * @return A raw [PlayerColorScheme] using unmodified colors.
         */
        fun vibrantColorScheme(color: PaletteColor): PlayerColorScheme {
            return PlayerColorScheme(
                mode = Mode.VibrantColor,
                appThemeToken = AppThemeToken.None,
                surfaceColor = color.backgroundColor,
                surfaceContainerColor = ColorUtils.blendARGB(color.backgroundColor, color.primaryTextColor, 0.7f),
                primaryColor = color.backgroundColor,
                tonalColor = ColorUtils.blendARGB(color.backgroundColor, color.secondaryTextColor, 0.4f),
                onSurfaceColor = color.primaryTextColor,
                onSurfaceVariantColor = color.secondaryTextColor
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
                appThemeToken = AppThemeToken.None,
                surfaceColor = colorScheme.surface,
                surfaceContainerColor = colorScheme.surfaceContainerHigh,
                primaryColor = colorScheme.primary,
                tonalColor = colorScheme.secondaryContainer,
                onSurfaceColor = colorScheme.onSurface,
                onSurfaceVariantColor = colorScheme.onSurfaceVariant.withAlpha(0.7f)
            )
        }

        suspend fun autoColorScheme(
            context: Context,
            color: PaletteColor,
            mode: PlayerColorSchemeMode
        ): PlayerColorScheme {
            val colorScheme = when (mode) {
                Mode.AppTheme -> themeColorScheme(context)
                Mode.SimpleColor -> simpleColorScheme(context, color)
                Mode.VibrantColor -> vibrantColorScheme(color)
                Mode.MaterialYou -> dynamicColorScheme(context, color.backgroundColor)
            }
            check(mode == colorScheme.mode)
            return colorScheme
        }
    }
}