package com.mardous.booming.ui.screen.player

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import com.mardous.booming.data.model.Song
import org.apache.commons.lang3.builder.HashCodeBuilder
import kotlin.math.abs

class PlayerGesturesController(
    context: Context,
    private val acceptedGestures: Set<GestureType>,
    private val listener: Listener
) : View.OnTouchListener {

    private var view: View? = null

    private val onGestureListener = object : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapConfirmed(event: MotionEvent): Boolean {
            return consumeGesture(GestureType.Tap)
        }

        override fun onDoubleTap(event: MotionEvent): Boolean {
            val width = view?.width ?: 0
            val x = event.x

            val gesture = if (x < width * 0.35f) {
                GestureType.DoubleTapLeft
            } else if (x > width * 0.65f) {
                GestureType.DoubleTapRight
            } else {
                GestureType.DoubleTap
            }

            return consumeGesture(gesture) || consumeGesture(GestureType.DoubleTap)
        }

        override fun onLongPress(e: MotionEvent) {
            consumeGesture(GestureType.LongPress)
        }

        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            return try {
                val diffY = e2.y - e1!!.y
                val diffX = e2.x - e1.x

                if (abs(diffX) > abs(diffY)) {
                    // Horizontal swipe
                    if (abs(diffX) > 0 && abs(velocityX) > 0) {
                        if (diffX > 0) {
                            consumeGesture(GestureType.Fling(GestureType.Fling.DIRECTION_RIGHT))
                        } else {
                            consumeGesture(GestureType.Fling(GestureType.Fling.DIRECTION_LEFT))
                        }
                    } else false
                } else {
                    // Vertical swipe
                    if (abs(diffY) > 0 && abs(velocityY) > 0) {
                        if (diffY < 0) {
                            consumeGesture(GestureType.Fling(GestureType.Fling.DIRECTION_UP))
                        } else {
                            consumeGesture(GestureType.Fling(GestureType.Fling.DIRECTION_BOTTOM))
                        }
                    } else false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to detect fling gesture", e)
                false
            }
        }
    }

    private var gestureDetector: GestureDetector? = null

    init {
        gestureDetector = GestureDetector(context, onGestureListener)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View, event: MotionEvent?): Boolean {
        if (event == null)
            return false
        view = v

        return gestureDetector?.onTouchEvent(event) == true
    }

    fun release() {
        gestureDetector = null
    }

    private fun consumeGesture(gestureType: GestureType): Boolean {
        if (acceptedGestures.contains(gestureType)) {
            return listener.gestureDetected(gestureType)
        }
        return false
    }

    sealed class GestureType {

        object Tap : GestureType()

        object DoubleTap : GestureType()

        object DoubleTapLeft : GestureType()

        object DoubleTapRight : GestureType()

        object LongPress : GestureType()

        class Fling(val direction: Int) : GestureType() {

            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (other == null || javaClass != other.javaClass) return false
                val fling = other as Fling
                return fling.direction == this.direction
            }

            override fun hashCode(): Int {
                return HashCodeBuilder()
                    .append(direction)
                    .toHashCode()
            }

            companion object {
                const val DIRECTION_LEFT = 0
                const val DIRECTION_RIGHT = 1
                const val DIRECTION_UP = 2
                const val DIRECTION_BOTTOM = 3
            }
        }
    }

    interface Listener {
        fun gestureDetected(gestureType: GestureType): Boolean
    }

    companion object {
        private const val TAG = "PlayerGestureController"
    }
}