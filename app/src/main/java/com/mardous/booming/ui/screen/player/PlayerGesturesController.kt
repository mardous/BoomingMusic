package com.mardous.booming.ui.screen.player

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
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

            val type = if (x < width * LEFT_EDGE_DOUBLE_TAP_THRESHOLD) {
                GestureType.DoubleTap.TYPE_LEFT_EDGE
            } else if (x > width * RIGHT_EDGE_DOUBLE_TAP_THRESHOLD) {
                GestureType.DoubleTap.TYPE_RIGHT_EDGE
            } else {
                GestureType.DoubleTap.TYPE_CENTER
            }

            return consumeGesture(GestureType.DoubleTap(type))
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

        object LongPress : GestureType()

        class DoubleTap(val type: Int) : GestureType() {

            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (other == null || javaClass != other.javaClass) return false
                val tap = other as DoubleTap
                return tap.type == this.type
            }

            override fun hashCode(): Int {
                return HashCodeBuilder()
                    .append(type)
                    .toHashCode()
            }

            companion object {
                const val TYPE_CENTER = 0
                const val TYPE_LEFT_EDGE = 1
                const val TYPE_RIGHT_EDGE = 2
            }
        }

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

        private const val LEFT_EDGE_DOUBLE_TAP_THRESHOLD = 0.30f
        private const val RIGHT_EDGE_DOUBLE_TAP_THRESHOLD = 0.70f
    }
}