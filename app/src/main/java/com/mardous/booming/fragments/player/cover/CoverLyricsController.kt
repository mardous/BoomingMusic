package com.mardous.booming.fragments.player.cover

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.view.View
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import com.mardous.booming.extensions.resources.BOOMING_ANIM_TIME
import com.mardous.booming.model.theme.NowPlayingScreen
import com.mardous.booming.util.Preferences

class CoverLyricsController(
    private val nps: NowPlayingScreen,
    private val lyricsView: View,
    private val coverView: View
) {

    var isShowLyricsOnCover: Boolean
        get() = Preferences.showLyricsOnCover
        private set(value) { Preferences.showLyricsOnCover = value }

    var isAnimatingLyrics: Boolean = false
        private set

    fun showLyrics(
        isForced: Boolean = false,
        onPrepared: (AnimatorSet) -> Unit,
        onStart: () -> Unit
    ) {
        if (!nps.supportsCoverLyrics || (!isShowLyricsOnCover && !isForced) || isAnimatingLyrics)
            return

        isAnimatingLyrics = true

        val animatorSet = AnimatorSet()
        animatorSet.playTogether(
            ObjectAnimator.ofFloat(lyricsView, View.ALPHA, 1f),
            ObjectAnimator.ofFloat(coverView, View.ALPHA, 0f)
        )
        animatorSet.duration = BOOMING_ANIM_TIME
        animatorSet.doOnEnd {
            isAnimatingLyrics = false
            coverView.isInvisible = true
            it.removeAllListeners()
        }
        animatorSet.doOnStart {
            onStart()
            isShowLyricsOnCover = true
            lyricsView.isVisible = true
        }
        onPrepared(animatorSet)
        animatorSet.start()
    }

    fun hideLyrics(
        isPermanent: Boolean = false,
        onPrepared: (AnimatorSet) -> Unit,
        onEnd: () -> Unit
    ) {
        if (!isShowLyricsOnCover || isAnimatingLyrics) return

        isAnimatingLyrics = true

        val animatorSet = AnimatorSet()
        animatorSet.playTogether(
            ObjectAnimator.ofFloat(lyricsView, View.ALPHA, 0f),
            ObjectAnimator.ofFloat(coverView, View.ALPHA, 1f)
        )
        animatorSet.duration = BOOMING_ANIM_TIME
        animatorSet.doOnStart {
            coverView.isInvisible = false
        }
        animatorSet.doOnEnd {
            onEnd()
            if (isPermanent) {
                isShowLyricsOnCover = false
            }
            lyricsView.isVisible = false
            isAnimatingLyrics = false
            it.removeAllListeners()
        }
        onPrepared(animatorSet)
        animatorSet.start()
    }
}