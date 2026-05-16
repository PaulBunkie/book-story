/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.presentation.core.components.common

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import ua.acclorite.book_story.presentation.reader.TextMeasurementUtils

@Composable
fun HighlightedText(
    modifier: Modifier = Modifier,
    text: AnnotatedString,
    highlightThickness: FontWeight,
    style: TextStyle,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    overflow: TextOverflow = TextOverflow.Ellipsis
) {
    val highlightedText = remember(text, highlightThickness) {
        TextMeasurementUtils.applyHighlighting(text, highlightThickness)
    }

    BasicText(
        text = highlightedText,
        modifier = modifier,
        style = style,
        maxLines = maxLines,
        minLines = minLines,
        overflow = overflow
    )
}
