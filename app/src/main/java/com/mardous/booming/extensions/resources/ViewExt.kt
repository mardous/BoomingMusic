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

package com.mardous.booming.extensions.resources

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.text.TextUtils
import android.util.TypedValue
import android.view.*
import android.view.animation.AccelerateInterpolator
import android.view.animation.AnimationUtils
import android.view.animation.DecelerateInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.annotation.MenuRes
import androidx.appcompat.widget.Toolbar
import androidx.core.animation.doOnEnd
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.core.graphics.drawable.toDrawable
import androidx.core.graphics.toColorInt
import androidx.core.view.*
import androidx.core.widget.ImageViewCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationBarView
import com.google.android.material.navigationrail.NavigationRailView
import com.google.android.material.progressindicator.BaseProgressIndicator
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.slider.Slider
import com.mardous.booming.R
import com.mardous.booming.extensions.dip
import com.mardous.booming.extensions.resolveColor
import com.mardous.booming.util.Preferences
import com.skydoves.balloon.*
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.Markwon
import io.noties.markwon.core.MarkwonTheme
import io.noties.markwon.html.HtmlPlugin
import me.zhanghai.android.fastscroll.FastScroller
import me.zhanghai.android.fastscroll.FastScrollerBuilder
import com.google.android.material.R as M3R

const val BOOMING_ANIM_TIME = 350L

val View.backgroundColor: Int
    get() = (background as? ColorDrawable)?.color
        ?: (background as? MaterialShapeDrawable)?.fillColor?.defaultColor
        ?: Color.TRANSPARENT

fun View.showBounceAnimation() {
    clearAnimation()
    scaleX = 0.9f
    scaleY = 0.9f
    isVisible = true
    pivotX = (width / 2).toFloat()
    pivotY = (height / 2).toFloat()

    animate().setDuration(200)
        .setInterpolator(DecelerateInterpolator())
        .scaleX(1.1f)
        .scaleY(1.1f)
        .withEndAction {
            animate().setDuration(200)
                .setInterpolator(AccelerateInterpolator())
                .scaleX(1f)
                .scaleY(1f)
                .alpha(1f)
                .start()
        }
        .start()
}

typealias AnimationCompleted = () -> Unit

fun View.show(animate: Boolean = false, onCompleted: AnimationCompleted? = null) {
    if (!animate) {
        alpha = 1f
        isVisible = true
        onCompleted?.invoke()
    } else {
        if (isVisible && alpha == 1f) {
            onCompleted?.invoke()
            return
        }
        this.animate()
            .alpha(1f)
            .setDuration(BOOMING_ANIM_TIME)
            .withStartAction {
                setLayerType(View.LAYER_TYPE_HARDWARE, null)
                isVisible = true
            }
            .withEndAction {
                setLayerType(View.LAYER_TYPE_NONE, null)
                onCompleted?.invoke()
            }
            .start()
    }
}

fun View.hide(animate: Boolean = false, onCompleted: AnimationCompleted? = null) {
    if (!animate) {
        isVisible = false
        onCompleted?.invoke()
    } else {
        if (!isVisible) {
            onCompleted?.invoke()
            return
        }
        this.animate()
            .alpha(0f)
            .setDuration(BOOMING_ANIM_TIME)
            .withStartAction {
                setLayerType(View.LAYER_TYPE_HARDWARE, null)
            }
            .withEndAction {
                setLayerType(View.LAYER_TYPE_NONE, null)
                isVisible = false
                onCompleted?.invoke()
            }
            .start()
    }
}

fun View.reactionToKey(targetKeyCode: Int, action: (KeyEvent) -> Unit) {
    setOnKeyListener { view, keyCode, keyEvent ->
        if (keyEvent.hasNoModifiers()) {
            if (keyEvent.action == KeyEvent.ACTION_UP) {
                if (keyCode == targetKeyCode) {
                    view.cancelLongPress()
                    action(keyEvent)
                    return@setOnKeyListener true
                }
            }
        }
        false
    }
}

fun View.focusAndShowKeyboard() {
    /**
     * This is to be called when the window already has focus.
     */
    fun View.showTheKeyboardNow() {
        if (isFocused) {
            post {
                // We still post the call, just in case we are being notified of the windows focus
                // but InputMethodManager didn't get properly setup yet.
                val imm = context.getSystemService<InputMethodManager>()
                imm?.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
            }
        }
    }

    requestFocus()
    if (hasWindowFocus()) {
        // No need to wait for the window to get focus.
        showTheKeyboardNow()
    } else {
        // We need to wait until the window gets focus.
        viewTreeObserver.addOnWindowFocusChangeListener(
            object : ViewTreeObserver.OnWindowFocusChangeListener {
                override fun onWindowFocusChanged(hasFocus: Boolean) {
                    // This notification will arrive just before the InputMethodManager gets set up.
                    if (hasFocus) {
                        this@focusAndShowKeyboard.showTheKeyboardNow()
                        // It’s very important to remove this listener once we are done.
                        viewTreeObserver.removeOnWindowFocusChangeListener(this)
                    }
                }
            })
    }
}

fun View.addPaddingRelative(
    start: Int = 0,
    top: Int = 0,
    end: Int = 0,
    bottom: Int = 0
) {
    updatePaddingRelative(paddingStart + start, paddingTop + top, paddingEnd + end, paddingBottom + bottom)
}

fun View.centerPivot() {
    post {
        pivotX = (width / 2).toFloat()
        pivotY = (height / 2).toFloat()
    }
}

fun View.hitTest(x: Int, y: Int): Boolean {
    val tx = (translationX + 0.5f)
    val ty = (translationY + 0.5f)
    val left = left + tx
    val right = right + tx
    val top = top + ty
    val bottom = bottom + ty

    return x >= left && x <= right && y >= top && y <= bottom
}

fun View.animateBackgroundColor(
    toColor: Int,
    duration: Long = 300,
    onCompleted: AnimationCompleted? = null
): Animator {
    val fromColor = backgroundColor
    return ObjectAnimator.ofArgb(this, "backgroundColor", fromColor, toColor).apply {
        this.doOnEnd { onCompleted?.invoke() }
        this.duration = duration
    }
}

fun View.animateTintColor(
    fromColor: Int,
    toColor: Int,
    duration: Long = 300,
    isIconButton: Boolean = false
): Animator {
    return ValueAnimator.ofArgb(fromColor, toColor).apply {
        this.duration = duration
        addUpdateListener { animation ->
            val animatedColor = animation.animatedValue as Int
            val colorStateList = animatedColor.toColorStateList()

            when (this@animateTintColor) {
                is Toolbar -> colorizeToolbar(animatedColor)
                is Slider -> applyColor(animatedColor)
                is SeekBar -> applyColor(animatedColor)
                is FloatingActionButton -> applyColor(animatedColor)
                is MaterialButton -> applyColor(animatedColor, isIconButton)
                is ImageView -> ImageViewCompat.setImageTintList(this@animateTintColor, colorStateList)
                is TextView -> applyColor(animatedColor)
                else -> backgroundTintList = colorStateList
            }
        }
    }
}

fun ImageView.removeHorizontalMarginIfRequired() {
    if (Preferences.largerHeaderImage) {
        doOnLayout {
            updateLayoutParams<ViewGroup.MarginLayoutParams> { marginStart = 0; marginEnd = 0; }
        }
    }
}

fun EditText.requestInputMethod() {
    requestFocus()
    post {
        if (isAttachedToWindow) {
            val imm = context.getSystemService<InputMethodManager>()
            imm?.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
        }
    }
}

fun TextView.setMarquee(marquee: Boolean) {
    isFocusable = marquee
    isFocusableInTouchMode = marquee
    setHorizontallyScrolling(marquee)
    if (marquee) {
        ellipsize = TextUtils.TruncateAt.MARQUEE
        marqueeRepeatLimit = -1
        isHorizontalFadingEdgeEnabled = true
    } else {
        ellipsize = TextUtils.TruncateAt.END
    }
    isSelected = marquee
}

fun TextView.setMarkdownText(str: String) {
    val markwon = Markwon.builder(context)
        .usePlugin(HtmlPlugin.create()) // basic Html tags
        .usePlugin(object : AbstractMarkwonPlugin() {
            override fun configureTheme(builder: MarkwonTheme.Builder) {
                val typedColor = TypedValue()
                context.theme.resolveAttribute(com.google.android.material.R.attr.dividerColor, typedColor, true)

                builder.headingBreakColor("#00ffffff".toColorInt())
                    .thematicBreakColor(typedColor.data)
                    .thematicBreakHeight(2)
                    .bulletWidth(12)
                    .headingTextSizeMultipliers(
                        floatArrayOf(2f, 1.5f, 1f, .83f, .67f, .55f)
                    )
            }
        })
        .build()

    markwon.setMarkdown(this, str)
}

fun ImageView.useAsIcon() {
    val iconPadding = context.dip(R.dimen.list_item_image_icon_padding)
    setPadding(iconPadding, iconPadding, iconPadding, iconPadding)
    clearColorFilter()
}

fun Toolbar.inflateMenu(
    @MenuRes menuId: Int,
    itemClickListener: Toolbar.OnMenuItemClickListener,
    menuConsumer: ((Menu) -> Unit)? = null
) {
    inflateMenu(menuId)
    setOnMenuItemClickListener(itemClickListener)
    menuConsumer?.invoke(menu)
}

fun RecyclerView.useLinearLayout() {
    layoutManager = LinearLayoutManager(context)
}

fun RecyclerView.safeUpdateWithRetry(
    maxRetries: Int = 5,
    delayMillis: Long = 16L, // ~1 frame on 60fps
    block: RecyclerView.() -> Unit
) {
    fun tryUpdate(attempt: Int) {
        if (!isAnimating && !isComputingLayout) {
            block()
        } else if (attempt < maxRetries) {
            postDelayed({ tryUpdate(attempt + 1) }, delayMillis)
        }
    }
    tryUpdate(0)
}

fun RecyclerView.destroyOnDetach() {
    layoutManager?.let {
        if (it is LinearLayoutManager) {
            it.recycleChildrenOnDetach = true
        }
    }
}

fun RecyclerView.onVerticalScroll(
    lifecycleOwner: LifecycleOwner,
    onScrollUp: () -> Unit = {},
    onScrollDown: () -> Unit = {}
) {
    val scrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            if (dy > 0) {
                onScrollDown()
            } else if (dy < 0) {
                onScrollUp()
            }
        }
    }
    addOnScrollListener(scrollListener)
    lifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
        override fun onStop(owner: LifecycleOwner) {
            removeOnScrollListener(scrollListener)
        }
    })
}

fun ViewGroup.createFastScroller(disablePopup: Boolean = false): FastScroller {
    val thumbDrawable = ContextCompat.getDrawable(context, R.drawable.scroller_thumb)
    val trackDrawable = ContextCompat.getDrawable(context, R.drawable.scroller_track)
    val fastScrollerBuilder = FastScrollerBuilder(this)
    fastScrollerBuilder.useMd2Style()
    if (thumbDrawable != null) {
        fastScrollerBuilder.setThumbDrawable(thumbDrawable)
    }
    if (trackDrawable != null) {
        fastScrollerBuilder.setTrackDrawable(trackDrawable)
    }
    if (disablePopup) {
        fastScrollerBuilder.setPopupTextProvider { _, _ -> "" }
    }
    return fastScrollerBuilder.build()
}

/**
 * Potentially animate showing a [BottomNavigationView].
 *
 * Abruptly changing the visibility leads to a re-layout of main content, animating
 * `translationY` leaves a gap where the view was that content does not fill.
 *
 * Instead, take a snapshot of the view, and animate this in, only changing the visibility (and
 * thus layout) when the animation completes.
 */
fun NavigationBarView.show() {
    if (this is NavigationRailView) return
    if (isVisible) return

    val parent = parent as ViewGroup
    // View needs to be laid out to create a snapshot & know position to animate. If view isn't
    // laid out yet, need to do this manually.
    if (!isLaidOut) {
        measure(
            View.MeasureSpec.makeMeasureSpec(parent.width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(parent.height, View.MeasureSpec.AT_MOST)
        )
        layout(parent.left, parent.height - measuredHeight, parent.right, parent.height)
    }

    val drawable = drawToBitmap().toDrawable(context.resources)
    drawable.setBounds(left, parent.height, right, parent.height + height)
    parent.overlay.add(drawable)
    ValueAnimator.ofInt(parent.height, top).apply {
        duration = BOOMING_ANIM_TIME
        interpolator = AnimationUtils.loadInterpolator(context, android.R.interpolator.accelerate_decelerate)
        addUpdateListener {
            val newTop = it.animatedValue as Int
            drawable.setBounds(left, newTop, right, newTop + height)
        }
        doOnEnd {
            parent.overlay.remove(drawable)
            isVisible = true
        }
        start()
    }
}

/**
 * Potentially animate hiding a [BottomNavigationView].
 *
 * Abruptly changing the visibility leads to a re-layout of main content, animating
 * `translationY` leaves a gap where the view was that content does not fill.
 *
 * Instead, take a snapshot, instantly hide the view (so content lays out to fill), then animate
 * out the snapshot.
 */
fun NavigationBarView.hide() {
    if (this is NavigationRailView) return
    if (isGone) return

    if (!isLaidOut) {
        isGone = true
        return
    }

    val drawable = drawToBitmap().toDrawable(context.resources)
    val parent = parent as ViewGroup
    drawable.setBounds(left, top, right, bottom)
    parent.overlay.add(drawable)
    isGone = true
    ValueAnimator.ofInt(top, parent.height).apply {
        duration = BOOMING_ANIM_TIME
        interpolator = AnimationUtils.loadInterpolator(context, android.R.interpolator.accelerate_decelerate)
        addUpdateListener {
            val newTop = it.animatedValue as Int
            drawable.setBounds(left, newTop, right, newTop + height)
        }
        doOnEnd {
            parent.overlay.remove(drawable)
        }
        start()
    }
}

fun BaseProgressIndicator<*>.setWavy(isWavy: Boolean) {
    val oldAmplitude = this.waveAmplitude
    val newAmplitude = if (!isWavy) 0 else {
        resources.getDimensionPixelSize(
            M3R.dimen.m3_comp_progress_indicator_circular_active_indicator_wave_amplitude
        )
    }
    val waveLength = if (isIndeterminate) wavelengthIndeterminate else wavelengthDeterminate

    // Cancel any existing animator to avoid multiple animators competing
    (getTag(R.id.id_wave_amplitude_animator) as? ValueAnimator)?.cancel()
    setTag(R.id.id_wave_amplitude_animator, null)

    // If we have no wavelength or the value is already what we want, just snap to it
    if (waveLength <= 0 || oldAmplitude == newAmplitude) {
        waveAmplitude = newAmplitude
        return
    }

    val animator = ValueAnimator.ofInt(oldAmplitude, newAmplitude).apply {
        duration = BOOMING_ANIM_TIME
        interpolator = DecelerateInterpolator()
        addUpdateListener { animator ->
            waveAmplitude = animator.animatedValue as Int
        }
        addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                // Clear reference when done
                setTag(R.id.id_wave_amplitude_animator, null)
            }

            override fun onAnimationCancel(animation: Animator) {
                // Also clear on cancel to avoid leaking the animator
                setTag(R.id.id_wave_amplitude_animator, null)
            }
        })
    }

    // Store animator so subsequent calls can cancel it
    setTag(R.id.id_wave_amplitude_animator, animator)
    animator.start()
}

fun BaseProgressIndicator<*>.setAnimatedWave(isAnimatedWave: Boolean) {
    waveSpeed = if (isAnimatedWave) {
        resources.getDimensionPixelSize(R.dimen.m3e_progress_indicator_animation_speed)
    } else 0
}

typealias TrackingTouchListener = (Slider) -> Unit

fun Slider.setTrackingTouchListener(
    onStart: TrackingTouchListener? = null,
    onStop: TrackingTouchListener? = null
) {
    if (onStart == null && onStop == null)
        return

    addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
        override fun onStartTrackingTouch(slider: Slider) {
            onStart?.invoke(slider)
        }

        override fun onStopTrackingTouch(slider: Slider) {
            onStop?.invoke(slider)
        }
    })
}

inline fun Context.createBoomingMusicBalloon(
    tooltipId: String,
    lifecycleOwner: LifecycleOwner,
    crossinline block: Balloon.Builder.() -> Unit
): Balloon {
    val bgColor = resolveColor(M3R.attr.colorTertiaryContainer)
    val textColor = resolveColor(M3R.attr.colorOnTertiaryContainer)
    return createBalloon(this) {
        setBackgroundColor(bgColor)
        setTextColor(textColor)
        setWidthRatio(0.8f)
        setPadding(10)
        setHeight(BalloonSizeSpec.WRAP)
        setBalloonAnimation(BalloonAnimation.CIRCULAR)
        setArrowPositionRules(ArrowPositionRules.ALIGN_ANCHOR)
        setArrowPosition(0.5f)
        setCornerRadiusResource(R.dimen.m3_card_corner_radius)
        setDismissWhenTouchOutside(true)
        setAutoDismissDuration(5000)
        setLifecycleOwner(lifecycleOwner)
        setPreferenceName(tooltipId)
        block(this)
    }
}

fun BottomSheetBehavior<*>.peekHeightAnimate(value: Int): Animator {
    return ObjectAnimator.ofInt(this, "peekHeight", value).apply {
        duration = BOOMING_ANIM_TIME
        start()
    }
}

fun AppBarLayout.setupStatusBarForeground() {
    statusBarForeground = MaterialShapeDrawable.createWithElevationOverlay(context)
}

fun CompoundButton.animateToggle() = post { isChecked = !isChecked }