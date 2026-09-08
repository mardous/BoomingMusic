package com.mardous.booming.ui.component.compose

import android.view.View
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

val LocalBottomSheetBehavior = compositionLocalOf<BottomSheetBehavior<*>?> { null }

interface BottomSheetDialogScope {
    val nestedScrollInteropConnection: NestedScrollConnection

    @Composable
    fun Modifier.bottomSheetScrollable(
        scope: CoroutineScope,
        nestedScrollConnection: NestedScrollConnection
    ): Modifier
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomSheetDialogSurface(
    modifier: Modifier = Modifier,
    dragHandle: @Composable ColumnScope.() -> Unit = {
        BottomSheetDefaults.DragHandle(Modifier.align(Alignment.CenterHorizontally))
    },
    title: @Composable (ColumnScope.() -> Unit)? = null,
    titleStyle: TextStyle = MaterialTheme.typography.headlineMediumEmphasized.copy(
        fontWeight = FontWeight.SemiBold
    ),
    headingContentPadding: PaddingValues = if (title != null) {
        PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp)
    } else {
        PaddingValues(horizontal = 16.dp)
    },
    content: @Composable BottomSheetDialogScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        val bottomSheetBehavior = rememberBottomSheetBehavior()
        CompositionLocalProvider(LocalBottomSheetBehavior provides bottomSheetBehavior) {
            with(rememberBottomSheetDialogScope()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .nestedScroll(nestedScrollInteropConnection)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(headingContentPadding)
                            .bottomSheetScrollable(
                                scope = rememberCoroutineScope(),
                                nestedScrollConnection = nestedScrollInteropConnection
                            )
                    ) {
                        dragHandle()
                        if (title != null) {
                            ProvideTextStyle(titleStyle) { title() }
                        }
                    }
                    content()
                }
            }
        }
    }
}

@Composable
private fun rememberBottomSheetBehavior(view: View = LocalView.current): BottomSheetBehavior<*>? {
    return remember(view) {
        try {
            var current: View? = view
            while (current != null && current.parent is View) {
                val parent = current.parent as View
                if (parent is CoordinatorLayout) {
                    return@remember BottomSheetBehavior.from(current)
                }
                current = parent
            }
            null
        } catch (_: IllegalArgumentException) {
            null
        }
    }
}

@Composable
private fun rememberBottomSheetDialogScope(): BottomSheetDialogScope {
    val nestedScrollInteropConnection = rememberNestedScrollInteropConnection()
    return remember { BottomSheetDialogScopeInstance(nestedScrollInteropConnection) }
}

private class BottomSheetDialogScopeInstance(
    override val nestedScrollInteropConnection: NestedScrollConnection
) : BottomSheetDialogScope {
    @Composable
    override fun Modifier.bottomSheetScrollable(
        scope: CoroutineScope,
        nestedScrollConnection: NestedScrollConnection
    ): Modifier {
        return pointerInput(Unit) {
            detectVerticalDragGestures(
                onDragEnd = {
                    scope.launch {
                        nestedScrollConnection.onPostFling(
                            consumed = Velocity.Zero,
                            available = Velocity.Zero
                        )
                    }
                }
            ) { _, dragAmount ->
                nestedScrollConnection.onPreScroll(
                    available = Offset(0f, dragAmount),
                    source = NestedScrollSource.UserInput
                )
            }
        }
    }
}