/*
 * Copyright (c) 2024 Christians Martínez Alvarado
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

package com.mardous.booming.core.model.theme

import androidx.annotation.DrawableRes
import androidx.annotation.LayoutRes
import androidx.annotation.StringRes
import com.mardous.booming.R
import com.mardous.booming.ui.screen.player.PlayerColorSchemeList
import com.mardous.booming.ui.screen.player.PlayerColorSchemeMode

enum class NowPlayingScreen(
    @param:StringRes
    val titleRes: Int,
    @param:DrawableRes
    val drawableResId: Int,
    @param:LayoutRes
    val albumCoverLayoutRes: Int,
    val buttonStyle: NowPlayingButtonStyle,
    val supportsCoverLyrics: Boolean,
    val supportsCarouselEffect: Boolean,
    val supportsCustomCornerRadius: Boolean
) {
    Default(
        R.string.normal,
        R.drawable.np_normal,
        R.layout.fragment_album_cover_default,
        buttonStyle = NowPlayingButtonStyle.Normal,
        supportsCoverLyrics = true,
        supportsCarouselEffect = true,
        supportsCustomCornerRadius = true
    ),
    FullCover(
        R.string.full_cover,
        R.drawable.np_full,
        R.layout.fragment_album_cover,
        buttonStyle = NowPlayingButtonStyle.Normal,
        supportsCoverLyrics = false,
        supportsCarouselEffect = false,
        supportsCustomCornerRadius = false
    ),
    Gradient(
        R.string.gradient,
        R.drawable.np_gradient,
        R.layout.fragment_album_cover,
        buttonStyle = NowPlayingButtonStyle.Normal,
        supportsCoverLyrics = true,
        supportsCarouselEffect = false,
        supportsCustomCornerRadius = false
    ),
    Plain(
        R.string.plain,
        R.drawable.np_plain,
        R.layout.fragment_album_cover_default,
        buttonStyle = NowPlayingButtonStyle.Normal,
        supportsCoverLyrics = true,
        supportsCarouselEffect = true,
        supportsCustomCornerRadius = true
    ),
    M3(
        R.string.m3_style,
        R.drawable.np_m3,
        R.layout.fragment_album_cover_m3,
        buttonStyle = NowPlayingButtonStyle.Material3,
        supportsCoverLyrics = true,
        supportsCarouselEffect = true,
        supportsCustomCornerRadius = true
    ),
    Peek(
        R.string.peek,
        R.drawable.np_peek,
        R.layout.fragment_album_cover_peek,
        buttonStyle = NowPlayingButtonStyle.Normal,
        supportsCoverLyrics = false,
        supportsCarouselEffect = false,
        supportsCustomCornerRadius = false
    ),
    Peek2(
        R.string.peek2,
        R.drawable.np_peek2,
        R.layout.fragment_album_cover_default,
        buttonStyle = NowPlayingButtonStyle.Normal,
        supportsCoverLyrics = true,
        supportsCarouselEffect = true,
        supportsCustomCornerRadius = true
    );

    val defaultColorScheme: PlayerColorSchemeMode
        get() = when (this) {
            Default -> PlayerColorSchemeMode.AppTheme
            M3 -> PlayerColorSchemeMode.MaterialYou
            FullCover, Gradient, Peek2 -> PlayerColorSchemeMode.VibrantColor
            Plain, Peek -> PlayerColorSchemeMode.SimpleColor
        }

    val supportedColorSchemes: PlayerColorSchemeList
        get() = when (this) {
            Default,
            Plain -> listOf(
                PlayerColorSchemeMode.AppTheme,
                PlayerColorSchemeMode.SimpleColor,
                PlayerColorSchemeMode.MaterialYou
            )
            FullCover,
            Gradient, Peek2 -> listOf(
                PlayerColorSchemeMode.VibrantColor
            )
            Peek,
            M3 -> listOf(
                PlayerColorSchemeMode.AppTheme,
                PlayerColorSchemeMode.MaterialYou
            )
        }
}