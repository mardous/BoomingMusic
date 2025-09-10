package com.mardous.booming.ui.screen.player.cover.page

import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.os.BundleCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import coil3.dispose
import coil3.load
import coil3.request.Disposable
import coil3.request.allowHardware
import coil3.request.crossfade
import coil3.toBitmap
import com.google.android.material.card.MaterialCardView
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.ShapeAppearanceModel
import com.mardous.booming.R
import com.mardous.booming.core.model.PaletteColor
import com.mardous.booming.core.model.theme.NowPlayingScreen
import com.mardous.booming.core.palette.PaletteProcessor
import com.mardous.booming.data.model.Song
import com.mardous.booming.extensions.EXTRA_SONG
import com.mardous.booming.extensions.requestContext
import com.mardous.booming.extensions.requestView
import com.mardous.booming.extensions.withArgs
import com.mardous.booming.util.Preferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ImageFragment : Fragment() {

    private var isColorReady = false
    private lateinit var color: PaletteColor
    private lateinit var song: Song
    private var colorReceiver: ColorReceiver? = null
    private var request = 0

    private var disposable: Disposable? = null
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
        albumCover?.dispose()
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
        disposable?.dispose()
        disposable = albumCover?.load(song) {
            crossfade(false)
            allowHardware(false)
            listener { request, result ->
                viewLifecycleOwner.lifecycleScope.launch {
                    val color = withContext(Dispatchers.Default) {
                        context?.let { fragmentCtx ->
                            PaletteProcessor.getPaletteColor(fragmentCtx, result.image.toBitmap())
                        }
                    }
                    if (isActive && color != null) {
                        setPalette(color)
                    }
                }
            }
        }
    }

    private fun setPalette(color: PaletteColor) {
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
        fun onColorReady(color: PaletteColor, request: Int)
    }

    companion object {
        fun newInstance(song: Song) = ImageFragment().withArgs {
            putParcelable(EXTRA_SONG, song)
        }
    }
}