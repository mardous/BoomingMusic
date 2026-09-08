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

package com.mardous.booming.data.model

import androidx.annotation.StringRes

/**
 * @author Christians M. A. (mardous)
 */
class Suggestion(
    val type: ContentType,
    val items: List<Any>,
    @StringRes val titleRes: Int,
    @StringRes val subtitleRes: Int
) {
    companion object {
        const val REDISCOVER_MAX_ITEMS = 16
        const val TOP_CONTENT_MAX_ITEMS = 10
        const val FOR_YOU_MAX_ITEMS = 20
        const val FOR_YOU_MIN_ITEMS = 8
    }
}