/*
 * Copyright (c) 2026 Christians Martínez Alvarado
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

package com.mardous.booming.data.model

import androidx.annotation.StringRes
import com.mardous.booming.R

enum class ChannelMode(val channels: Int, @StringRes val labelRes: Int) {
    MONO(1, R.string.channel_mono),
    STEREO(2, R.string.channel_stereo),
    STEREO_2_1(3, R.string.channel_stereo_2_1),
    QUADRAPHONIC(4, R.string.channel_quadraphonic),
    SURROUND_5_0(5, R.string.channel_surround_5_0),
    SURROUND_5_1(6, R.string.channel_surround_5_1),
    SURROUND_6_1(7, R.string.channel_surround_6_1),
    SURROUND_7_1(8, R.string.channel_surround_7_1),
    SURROUND_9_1(10, R.string.channel_surround_9_1),
    SURROUND_11_1(12, R.string.channel_surround_11_1),
    SURROUND_13_1(14, R.string.channel_surround_13_1);

    companion object {
        fun fromChannels(channels: Int): ChannelMode? {
            return entries.find { it.channels == channels }
        }
    }
}
