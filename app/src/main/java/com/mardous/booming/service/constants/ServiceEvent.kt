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

package com.mardous.booming.service.constants

import com.mardous.booming.BuildConfig

/**
 * @author Christians M. A. (mardous)
 */
class ServiceEvent {
    companion object {
        const val BOOMING_PACKAGE_NAME = BuildConfig.APPLICATION_ID
        const val PLAY_STATE_CHANGED = "$BOOMING_PACKAGE_NAME.playstatechanged"
        const val META_CHANGED = "$BOOMING_PACKAGE_NAME.metachanged"
        const val QUEUE_CHANGED = "$BOOMING_PACKAGE_NAME.queuechanged"
        const val REPEAT_MODE_CHANGED = "$BOOMING_PACKAGE_NAME.repeatmodechanged"
        const val SHUFFLE_MODE_CHANGED = "$BOOMING_PACKAGE_NAME.shufflemodechanged"
        const val MEDIA_STORE_CHANGED = "$BOOMING_PACKAGE_NAME.mediastorechanged"
        const val FAVORITE_STATE_CHANGED = "$BOOMING_PACKAGE_NAME.favoritestatechanged"
    }
}