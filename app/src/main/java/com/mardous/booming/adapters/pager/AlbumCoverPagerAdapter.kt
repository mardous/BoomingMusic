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

import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.annotation.ColorInt
import androidx.core.os.BundleCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.Target
import com.google.android.material.card.MaterialCardView
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.ShapeAppearanceModel
import com.mardous.booming.R
import com.mardous.booming.adapters.pager.AlbumCoverPagerAdapter.AlbumCoverFragment.ColorReceiver
import com.mardous.booming.extensions.EXTRA_SONG
import com.mardous.booming.extensions.glide.asBitmapPalette
import com.mardous.booming.extensions.glide.getSongGlideModel
import com.mardous.booming.extensions.glide.songOptions
import com.mardous.booming.extensions.requestContext
import com.mardous.booming.extensions.requestView
import com.mardous.booming.extensions.withArgs
import com.mardous.booming.glide.BoomingColoredTarget
import com.mardous.booming.helper.color.MediaNotificationProcessor
import com.mardous.booming.model.Song
import com.mardous.booming.model.theme.NowPlayingScreen
import com.mardous.booming.util.Preferences

class AlbumCoverPagerAdapter(fm: FragmentManager, private val dataSet: List<Song>) :
    CustomFragmentStatePagerAdapter(fm) {

    private var currentPaletteReceiver: ColorReceiver? = null
    private var currentColorReceiverPosition = -1

    override fun getItem(position: Int): Fragment {
        return AlbumCoverFragment.newInstance(dataSet[position])
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
     * Only the latest passed [AlbumCoverFragment.ColorReceiver] is guaranteed to receive a response
     */
    fun receiveColor(paletteReceiver: ColorReceiver, @ColorInt position: Int) {
        val fragment = getFragment(position) as AlbumCoverFragment?
        if (fragment != null) {
            currentPaletteReceiver = null
            currentColorReceiverPosition = -1
            fragment.receivePalette(paletteReceiver, position)
        } else {
            currentPaletteReceiver = paletteReceiver
            currentColorReceiverPosition = position
        }
    }

    class AlbumCoverFragment : Fragment() {

        private var isColorReady = false
        private lateinit var color: MediaNotificationProcessor
        private lateinit var song: Song
        private var colorReceiver: ColorReceiver? = null
        private var request = 0

        private var target: Target<*>? = null
        private var albumCover: ImageView? = null

        private val nowPlayingScreen: NowPlayingScreen
            get() = Preferences.nowPlayingScreen

        private fun getLayoutWithPlayerTheme(): Int {
            if (nowPlayingScreen.supportsCarouselEffect) {
                if (Preferences.isCarouselEffect) {
                    return R.layout.fragment_album_cover_carousel
                }
            }
            return nowPlayingScreen.albumCoverLayoutRes
        }

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            song = BundleCompat.getParcelable(requireArguments(), EXTRA_SONG, Song::class.java)!!
        }

        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
            return inflater.inflate(getLayoutWithPlayerTheme(), container, false)
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            albumCover = view.findViewById(R.id.player_image)
            setupImageStyle()
            loadAlbumCover()
        }

        override fun onDestroyView() {
            super.onDestroyView()
            Glide.with(this).clear(target)
            colorReceiver = null
        }

        private fun setupImageStyle() {
            if (!nowPlayingScreen.supportsCustomCornerRadius)
                return

            val shapeModel = requestContext {
                val cornerRadius = Preferences.getNowPlayingImageCornerRadius(requireContext())
                val cornerRadiusPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, cornerRadius.toFloat(), resources.displayMetrics)
                ShapeAppearanceModel.builder()
                    .setAllCorners(CornerFamily.ROUNDED, cornerRadiusPx)
                    .build()
            } ?: return

            when (val image = albumCover) {
                is ShapeableImageView -> image.shapeAppearanceModel = shapeModel
                else -> {
                    val card = requestView { it.findViewById<View>(R.id.player_image_card) }
                    if (card is MaterialCardView) {
                        card.shapeAppearanceModel = shapeModel
                    }
                }
            }
        }

        private fun loadAlbumCover() {
            Glide.with(this).clear(target)

            if (albumCover != null) {
                target = Glide.with(this)
                    .asBitmapPalette()
                    .load(song.getSongGlideModel())
                    .songOptions(song)
                    .dontAnimate()
                    .into(object : BoomingColoredTarget(albumCover!!) {
                        override fun onColorReady(colors: MediaNotificationProcessor) {
                            setPalette(colors)
                        }
                    })
            }
        }

        private fun setPalette(color: MediaNotificationProcessor) {
            this.color = color
            isColorReady = true
            if (colorReceiver != null) {
                colorReceiver!!.onColorReady(color, request)
                colorReceiver = null
            }
        }

        fun receivePalette(paletteReceiver: ColorReceiver, request: Int) {
            if (isColorReady) {
                paletteReceiver.onColorReady(color, request)
            } else {
                this.colorReceiver = paletteReceiver
                this.request = request
            }
        }

        interface ColorReceiver {
            fun onColorReady(color: MediaNotificationProcessor, request: Int)
        }

        companion object {
            fun newInstance(song: Song) = AlbumCoverFragment().withArgs {
                putParcelable(EXTRA_SONG, song)
            }
        }
    }
}