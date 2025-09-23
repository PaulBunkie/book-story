/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.presentation.reader

import android.util.Log
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.launch
import ua.acclorite.book_story.domain.reader.ReaderHorizontalGesture
import ua.acclorite.book_story.domain.reader.ReaderText
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun Modifier.readerHorizontalGesture(
    listState: LazyListState,
    text: List<ReaderText>,
    horizontalGesture: ReaderHorizontalGesture,
    horizontalGestureScroll: Float,
    horizontalGestureSensitivity: Dp,
    horizontalGestureAlphaAnim: Boolean,
    horizontalGesturePullAnim: Boolean,
    isLoading: Boolean
): Modifier {
    if (horizontalGesture == ReaderHorizontalGesture.OFF || horizontalGesture == ReaderHorizontalGesture.PAGES || isLoading) return this
    val inverted = rememberUpdatedState(horizontalGesture == ReaderHorizontalGesture.INVERSE)
    val sensitivity = rememberUpdatedState(horizontalGestureSensitivity)
    val alphaAnim = rememberUpdatedState(horizontalGestureAlphaAnim)
    val pullAnim = rememberUpdatedState(horizontalGesturePullAnim)

    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    val screenHeight = remember {
        mutableFloatStateOf(0f)
    }

    fun scrollBackward() {
        scope.launch {
            when (horizontalGesture) {
                ReaderHorizontalGesture.PAGES -> {
                    // В режиме PAGES всегда скроллим на 100% высоты экрана
                    listState.scrollBy(-screenHeight.floatValue)
                    
                    // Логирование после скролла
                    val firstVisibleIndex = listState.firstVisibleItemIndex
                    val firstVisibleOffset = listState.firstVisibleItemScrollOffset
                    val textContent = if (firstVisibleIndex < text.size) {
                        when (val item = text[firstVisibleIndex]) {
                            is ReaderText.Text -> item.line.text.take(10)
                            is ReaderText.Chapter -> item.title.take(10)
                            else -> "Other"
                        }
                    } else "Out of bounds"
                    
                    Log.d("HORIZONTAL_SCROLL", "BACKWARD - Index: $firstVisibleIndex, Offset: $firstVisibleOffset, Text: '$textContent'")
                }
                else -> {
                    listState.scrollBy(-screenHeight.floatValue)
                }
            }
        }
    }

    fun scrollForward() {
        scope.launch {
            when (horizontalGesture) {
                ReaderHorizontalGesture.PAGES -> {
                    // В режиме PAGES всегда скроллим на 100% высоты экрана
                    listState.scrollBy(screenHeight.floatValue)
                    
                    // Логирование после скролла
                    val firstVisibleIndex = listState.firstVisibleItemIndex
                    val firstVisibleOffset = listState.firstVisibleItemScrollOffset
                    val textContent = if (firstVisibleIndex < text.size) {
                        when (val item = text[firstVisibleIndex]) {
                            is ReaderText.Text -> item.line.text.take(10)
                            is ReaderText.Chapter -> item.title.take(10)
                            else -> "Other"
                        }
                    } else "Out of bounds"
                    
                    Log.d("HORIZONTAL_SCROLL", "FORWARD - Index: $firstVisibleIndex, Offset: $firstVisibleOffset, Text: '$textContent'")
                }
                else -> {
                    listState.scrollBy(screenHeight.floatValue)
                }
            }
        }
    }

    val horizontalOffset = remember {
        mutableFloatStateOf(0f)
    }
    val animatedHorizontalOffset = animateFloatAsState(
        horizontalOffset.floatValue
    )

    @Composable
    fun calculateAlpha(): Float {
        return animateFloatAsState(
            if (!alphaAnim.value) 1f
            else 1f - abs(with(density) { horizontalOffset.floatValue.toDp() }.value)
                .div(sensitivity.value.value)
        ).value
    }

    return this
        .onGloballyPositioned {
            screenHeight.floatValue = it.size.height * horizontalGestureScroll
        }
        .alpha(calculateAlpha())
        .offset {
            IntOffset(
                x = if (pullAnim.value) animatedHorizontalOffset.value.roundToInt()
                else 0,
                y = 0
            )
        }
        .pointerInput(Unit) {
            detectHorizontalDragGestures(
                onDragStart = {
                    horizontalOffset.floatValue = 0f
                },
                onHorizontalDrag = { _, dragAmount ->
                    horizontalOffset.floatValue += (dragAmount * 0.15f)
                },
                onDragCancel = {
                    horizontalOffset.floatValue = 0f
                },
                onDragEnd = {
                    val horizontalOffsetDp = with(density) { horizontalOffset.floatValue.toDp() }

                    when {
                        horizontalOffsetDp > sensitivity.value -> {
                            if (inverted.value) scrollForward()
                            else scrollBackward()
                        }

                        horizontalOffsetDp < -sensitivity.value -> {
                            if (inverted.value) scrollBackward()
                            else scrollForward()
                        }
                    }
                    horizontalOffset.floatValue = 0f
                }
            )
        }
}