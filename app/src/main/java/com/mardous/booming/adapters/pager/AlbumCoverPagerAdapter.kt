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

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.TypedValue
import android.view.*
import android.widget.ImageView
import androidx.annotation.ColorInt
import androidx.core.os.BundleCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.Target
import com.google.android.material.card.MaterialCardView
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.ShapeAppearanceModel
import com.mardous.booming.R
import com.mardous.booming.adapters.pager.AlbumCoverPagerAdapter.AlbumCoverFragment.CoverEventReceiver
import com.mardous.booming.extensions.EXTRA_SONG
import com.mardous.booming.extensions.glide.asBitmapPalette
import com.mardous.booming.extensions.glide.getSongGlideModel
import com.mardous.booming.extensions.glide.songOptions
import com.mardous.booming.extensions.requestContext
import com.mardous.booming.extensions.requestView
import com.mardous.booming.extensions.withArgs
import com.mardous.booming.glide.BoomingColoredTarget
import com.mardous.booming.helper.color.MediaNotificationProcessor
import com.mardous.booming.model.GestureOnCover
import com.mardous.booming.model.Song
import com.mardous.booming.model.theme.NowPlayingScreen
import com.mardous.booming.util.Preferences

class AlbumCoverPagerAdapter(f: Fragment) : FragmentStateAdapter(f) {

    private val asyncListDiffer = AsyncListDiffer(this, object : DiffUtil.ItemCallback<Song>() {
        override fun areItemsTheSame(oldItem: Song, newItem: Song): Boolean {
            return oldItem.id == newItem.id && oldItem.dateModified == newItem.dateModified
        }

        override fun areContentsTheSame(oldItem: Song, newItem: Song): Boolean {
            return oldItem == newItem
        }
    })

    private val fragmentManager = f.childFragmentManager

    private var currentPaletteReceiver: CoverEventReceiver? = null
    private var currentColorReceiverPosition = -1

    override fun createFragment(position: Int): Fragment {
        val fragment = AlbumCoverFragment.newInstance(asyncListDiffer.currentList[position])
        if (currentPaletteReceiver != null && currentColorReceiverPosition == position) {
            receiveColor(currentPaletteReceiver!!, currentColorReceiverPosition)
        }
        return fragment
    }

    override fun getItemCount(): Int {
        return asyncListDiffer.currentList.size
    }

    override fun getItemId(position: Int): Long {
        return asyncListDiffer.currentList[position].id
    }

    override fun containsItem(itemId: Long): Boolean {
        return asyncListDiffer.currentList.any { it.id == itemId }
    }

    /**
     * Only the latest passed [AlbumCoverFragment.CoverEventReceiver] is guaranteed to receive a response
     */
    fun receiveColor(paletteReceiver: CoverEventReceiver, @ColorInt position: Int) {
        val fragment = fragmentManager.findFragmentByTag("f${getItemId(position)}") as? AlbumCoverFragment
        if (fragment != null) {
            currentPaletteReceiver = null
            currentColorReceiverPosition = -1
            fragment.receivePalette(paletteReceiver, position)
        } else {
            currentPaletteReceiver = paletteReceiver
            currentColorReceiverPosition = position
        }
    }

    fun updateData(newData: List<Song>, onCompleted: () -> Unit) {
        asyncListDiffer.submitList(newData, onCompleted)
    }

    class AlbumCoverFragment : Fragment() {

        private var isColorReady = false
        private lateinit var color: MediaNotificationProcessor
        private lateinit var song: Song

        private var gestureDetector: GestureDetector? = null
        private var coverEventReceiver: CoverEventReceiver? = null
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

        @SuppressLint("ClickableViewAccessibility")
        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            albumCover = view.findViewById(R.id.player_image)
            albumCover?.setOnTouchListener { _, event -> gestureDetector?.onTouchEvent(event) == true }
            gestureDetector = GestureDetector(activity, object : GestureDetector.SimpleOnGestureListener() {
                override fun onSingleTapConfirmed(event: MotionEvent): Boolean {
                    return consumeGesture(GestureOnCover.Tap)
                }

                override fun onDoubleTap(event: MotionEvent): Boolean {
                    return consumeGesture(GestureOnCover.DoubleTap)
                }

                override fun onLongPress(e: MotionEvent) {
                    consumeGesture(GestureOnCover.LongPress)
                }
            })
            setupImageStyle()
            loadAlbumCover()
        }

        override fun onDestroyView() {
            Glide.with(this).clear(target)
            super.onDestroyView()
            gestureDetector = null
            coverEventReceiver = null
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
            if (coverEventReceiver != null) {
                coverEventReceiver!!.onColorReady(color, request)
                coverEventReceiver = null
            }
        }

        fun receivePalette(paletteReceiver: CoverEventReceiver, request: Int) {
            if (isColorReady) {
                paletteReceiver.onColorReady(color, request)
            } else {
                this.coverEventReceiver = paletteReceiver
                this.request = request
            }
        }

        private fun consumeGesture(gesture: GestureOnCover): Boolean {
            return coverEventReceiver?.onGestureEvent(gesture) ?: false
        }

        interface CoverEventReceiver {
            fun onGestureEvent(gesture: GestureOnCover): Boolean
            fun onColorReady(color: MediaNotificationProcessor, request: Int)
        }

        companion object {
            fun newInstance(song: Song) = AlbumCoverFragment().withArgs {
                putParcelable(EXTRA_SONG, song)
            }
        }
    }
}