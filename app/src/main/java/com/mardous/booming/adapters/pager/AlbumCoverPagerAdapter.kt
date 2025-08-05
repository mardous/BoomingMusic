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

package com.mardous.booming.adapters.pager

import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.mardous.booming.fragments.player.cover.page.ImageFragment
import com.mardous.booming.fragments.player.cover.page.ImageFragment.ColorReceiver
import com.mardous.booming.model.Song

class AlbumCoverPagerAdapter(fm: FragmentManager, private val dataSet: List<Song>) :
    CustomFragmentStatePagerAdapter(fm) {

    private var currentPaletteReceiver: ColorReceiver? = null
    private var currentColorReceiverPosition = -1

    override fun getItem(position: Int): Fragment {
        return ImageFragment.newInstance(dataSet[position])
    }

    override fun getCount(): Int {
        return dataSet.size
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val o = super.instantiateItem(container, position)
        if (currentPaletteReceiver != null && currentColorReceiverPosition == position) {
            receiveColor(currentPaletteReceiver!!, currentColorReceiverPosition)
        }
        return o
    }

    /**
     * Only the latest passed [ImageFragment.ColorReceiver] is guaranteed to receive a response
     */
    fun receiveColor(paletteReceiver: ColorReceiver, @ColorInt position: Int) {
        val fragment = getFragment(position) as ImageFragment?
        if (fragment != null) {
            currentPaletteReceiver = null
            currentColorReceiverPosition = -1
            fragment.receivePalette(paletteReceiver, position)
        } else {
            currentPaletteReceiver = paletteReceiver
            currentColorReceiverPosition = position
        }
    }
}