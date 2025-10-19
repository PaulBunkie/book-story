/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.presentation.reader

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Красные точки-индикаторы обрезанных строк текста
 */
@Composable
fun CutLineIndicators(
    visibilityState: TextLineVisibilityState,
    color: Color = Color.Red,
    size: androidx.compose.ui.unit.Dp = 6.dp,
    padding: androidx.compose.ui.unit.Dp = 16.dp
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Красная точка сверху
        androidx.compose.animation.AnimatedVisibility(
            visible = visibilityState.isTopLineCut,
            enter = scaleIn(animationSpec = tween(200)) + fadeIn(),
            exit = scaleOut(animationSpec = tween(200)) + fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .padding(top = padding)
                    .size(size)
                    .background(
                        color = color,
                        shape = CircleShape
                    )
                    .align(Alignment.TopCenter)
            )
        }
        
        // Красная точка снизу
        androidx.compose.animation.AnimatedVisibility(
            visible = visibilityState.isBottomLineCut,
            enter = scaleIn(animationSpec = tween(200)) + fadeIn(),
            exit = scaleOut(animationSpec = tween(200)) + fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .size(size)
                    .background(
                        color = color,
                        shape = CircleShape
                    )
                    .align(Alignment.BottomCenter)
                    .padding(bottom = padding)
            )
        }
    }
}
