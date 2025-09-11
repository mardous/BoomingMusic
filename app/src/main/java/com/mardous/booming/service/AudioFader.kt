package com.mardous.booming.service

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.media.MediaPlayer
import android.provider.Settings
import androidx.core.animation.doOnEnd
import com.mardous.booming.extensions.execSafe
import com.mardous.booming.service.playback.Playback

class AudioFader {
    companion object {
        fun createFadeAnimator(
            context: Context,
            fadeInMp: MediaPlayer,
            fadeOutMp: MediaPlayer,
            crossFadeDuration: Int,
            balance: FloatArray,
            endAction: (animator: Animator) -> Unit, /* Code to run when Animator Ends*/
        ): Animator? {
            // Get Global animator scale
            val animScale = Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)

            // Set duration according to the global animation scale, so cross-fade actually lasts for the duration set by the user
            val duration = (crossFadeDuration * 1000) / animScale
            if (duration == 0F) {
                return null
            }
            return ValueAnimator.ofFloat(0f, 1f).apply {
                this.duration = duration.toLong()
                addUpdateListener { animation: ValueAnimator ->
                    val progress = animation.animatedValue as Float

                    val inLeftVol = (progress * balance[0]).coerceIn(0f, 1f)
                    val inRightVol = (progress * balance[1]).coerceIn(0f, 1f)
                    fadeInMp.execSafe { setVolume(inLeftVol, inRightVol) }

                    val outLeftVol = ((1f - progress) * balance[0]).coerceIn(0f, 1f)
                    val outRightVol = ((1f - progress) * balance[1]).coerceIn(0f, 1f)
                    fadeOutMp.execSafe { setVolume(outLeftVol, outRightVol) }
                }
                doOnEnd {
                    endAction(it)
                }
            }
        }

        fun startFadeAnimator(
            playback: Playback,
            fadeDuration: Int,
            balanceLeft: Float,
            balanceRight: Float,
            fadeIn: Boolean, /* fadeIn -> true  fadeOut -> false*/
            callback: Runnable? = null, /* Code to run when Animator Ends*/
        ) {
            if (fadeDuration == 0) {
                callback?.run()
                return
            }
            val startValue = if (fadeIn) 0.0f else 1.0f
            val endValue = if (fadeIn) 1.0f else 0.0f
            val animator = ValueAnimator.ofFloat(startValue, endValue)
            animator.duration = fadeDuration.toLong()
            animator.addUpdateListener { animation: ValueAnimator ->
                val progress = animation.animatedValue as Float
                playback.setVolume(progress * balanceLeft, progress * balanceRight)
            }
            animator.doOnEnd {
                callback?.run()
            }
            animator.start()
        }
    }
}