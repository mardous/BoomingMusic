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
interface ServiceAction {
    interface Extras {
        companion object {
            const val EXTRA_CONTENT_TYPE = "$BOOMING_PACKAGE_NAME.extra.contenttype"
            const val EXTRA_SHUFFLE_MODE = "$BOOMING_PACKAGE_NAME.extra.shufflemode"
            const val EXTRA_APP_WIDGET_NAME = "$BOOMING_PACKAGE_NAME.app_widget_name"
        }
    }

    companion object {
        const val BOOMING_PACKAGE_NAME = BuildConfig.APPLICATION_ID
        const val ACTION_TOGGLE_PAUSE = "$BOOMING_PACKAGE_NAME.togglepause"
        const val ACTION_PLAY = "$BOOMING_PACKAGE_NAME.play"
        const val ACTION_PLAY_PLAYLIST = "$BOOMING_PACKAGE_NAME.play.playlist"
        const val ACTION_PAUSE = "$BOOMING_PACKAGE_NAME.pause"
        const val ACTION_STOP = "$BOOMING_PACKAGE_NAME.stop"
        const val ACTION_QUIT = "$BOOMING_PACKAGE_NAME.quit"
        const val ACTION_PENDING_QUIT = "$BOOMING_PACKAGE_NAME.pendingquit"
        const val ACTION_NEXT = "$BOOMING_PACKAGE_NAME.next"
        const val ACTION_PREVIOUS = "$BOOMING_PACKAGE_NAME.previous"
        const val ACTION_TOGGLE_FAVORITE = "$BOOMING_PACKAGE_NAME.togglefavorite"
        const val ACTION_APP_WIDGET_UPDATE = "$BOOMING_PACKAGE_NAME.appwidgetupdate"
    }
}