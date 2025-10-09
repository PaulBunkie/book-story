/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.presentation.reader

import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.compose.runtime.Composable
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import ua.acclorite.book_story.domain.reader.ReaderText
import ua.acclorite.book_story.domain.reader.FontWithName
import ua.acclorite.book_story.domain.reader.ReaderFontThickness
import ua.acclorite.book_story.domain.reader.ReaderTextAlignment
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.platform.LocalTextToolbar

/**
 * Состояние видимости строк текста
 */
data class TextLineVisibilityState(
    val isTopLineCut: Boolean,
    val isBottomLineCut: Boolean
)

/**
 * Детектор обрезанных строк текста
 */
@Composable
fun TextLineVisibilityDetector(
    listState: LazyListState,
    text: List<ReaderText>,
    screenHeight: Int,
    fontSize: TextUnit,
    lineHeight: TextUnit,
    fontFamily: FontWithName,
    fontThickness: ReaderFontThickness,
    fontStyle: FontStyle,
    textAlignment: ReaderTextAlignment,
    letterSpacing: TextUnit,
    sidePadding: Dp
): TextLineVisibilityState {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    
    val visibleItems = listState.layoutInfo.visibleItemsInfo
    val firstItem = visibleItems.firstOrNull()
    val lastItem = visibleItems.lastOrNull()
    
    var isTopLineCut = false
    var isBottomLineCut = false
    
    android.util.Log.d("TEXT_LINE_DETECTOR", "=== Text Line Detection ===")
    android.util.Log.d("TEXT_LINE_DETECTOR", "Visible items: ${visibleItems.size}")
    android.util.Log.d("TEXT_LINE_DETECTOR", "Screen height: $screenHeight")
    
    firstItem?.let { item ->
        android.util.Log.d("TEXT_LINE_DETECTOR", "First item: index=${item.index}, offset=${item.offset}, size=${item.size}")
        if (item.index < text.size) {
            val readerText = text[item.index]
            android.util.Log.d("TEXT_LINE_DETECTOR", "First item type: ${readerText::class.simpleName}")
        }
    }
    
    lastItem?.let { item ->
        android.util.Log.d("TEXT_LINE_DETECTOR", "Last item: index=${item.index}, offset=${item.offset}, size=${item.size}")
        if (item.index < text.size) {
            val readerText = text[item.index]
            android.util.Log.d("TEXT_LINE_DETECTOR", "Last item type: ${readerText::class.simpleName}")
        }
    }
    
    // Создаем TextPaint для расчета высот строк
    val textPaint = TextMeasurementUtils.createTextPaint(
        fontSize = fontSize,
        fontFamily = fontFamily,
        fontThickness = fontThickness,
        fontStyle = fontStyle,
        textAlignment = textAlignment,
        letterSpacing = letterSpacing,
        density = density.density
    )
    
    val availableWidth = listState.layoutInfo.viewportSize.width - (sidePadding.value * 2 * density.density).toInt()
    
    // Проверяем первую строку (в первом элементе) - для ВСЕХ типов элементов
    firstItem?.let { item ->
        if (item.index < text.size) {
            val readerText = text[item.index]
            if (readerText is ReaderText.Text) {
                        isTopLineCut = isTopLineCutWithTextMeasurer(
                            item = item,
                            readerText = readerText,
                            textMeasurer = textMeasurer,
                            availableWidth = availableWidth,
                            fontSize = fontSize,
                            lineHeight = lineHeight,
                            fontFamily = fontFamily,
                            fontThickness = fontThickness,
                            fontStyle = fontStyle,
                            textAlignment = textAlignment,
                            letterSpacing = letterSpacing
                        )
            }
        }
    }
    
    // Проверяем последнюю строку (в последнем элементе)
    lastItem?.let { item ->
        if (item.index < text.size) {
            val readerText = text[item.index]
            if (readerText is ReaderText.Text) {
                        isBottomLineCut = isBottomLineCutWithTextMeasurer(
                            item = item,
                            readerText = readerText,
                            textMeasurer = textMeasurer,
                            availableWidth = availableWidth,
                            screenHeight = screenHeight,
                            fontSize = fontSize,
                            lineHeight = lineHeight,
                            fontFamily = fontFamily,
                            fontThickness = fontThickness,
                            fontStyle = fontStyle,
                            textAlignment = textAlignment,
                            letterSpacing = letterSpacing
                        )
            }
        }
    }
    
    return TextLineVisibilityState(
        isTopLineCut = isTopLineCut,
        isBottomLineCut = isBottomLineCut
    )
}

/**
 * Проверяет обрезана ли первая строка текстового элемента с TextMeasurer
 */
private fun isTopLineCutWithTextMeasurer(
    item: androidx.compose.foundation.lazy.LazyListItemInfo,
    readerText: ReaderText.Text,
    textMeasurer: TextMeasurer,
    availableWidth: Int,
    fontSize: TextUnit,
    lineHeight: TextUnit,
    fontFamily: FontWithName,
    fontThickness: ReaderFontThickness,
    fontStyle: FontStyle,
    textAlignment: ReaderTextAlignment,
    letterSpacing: TextUnit
): Boolean {
    if (item.offset >= 0) {
        android.util.Log.d("TEXT_LINE_DETECTOR", "Top line check - offset >= 0, not cut")
        return false
    }
    
    // 1. Создаём TextStyle с теми же параметрами
    val textStyle = TextStyle(
        fontSize = fontSize,
        lineHeight = lineHeight,
        fontFamily = null, // TODO: Implement proper font family conversion
        fontWeight = TextMeasurementUtils.getFontWeight(fontThickness),
        fontStyle = fontStyle,
        textAlign = TextMeasurementUtils.getTextAlign(textAlignment),
        letterSpacing = letterSpacing
    )
    
    // 2. Создаём Constraints
    val constraints = Constraints(
        maxWidth = availableWidth,
        maxHeight = Int.MAX_VALUE
    )
    
    // 3. Измеряем текст с TextMeasurer
    val textLayoutResult = textMeasurer.measure(
        text = readerText.line,
        style = textStyle,
        constraints = constraints
    )
    
    // 4. D - скрытое пространство сверху
    val D = -item.offset.toFloat()
    
    // 5. S_plus_I - точная высота строки из TextMeasurer
    val lineCount = textLayoutResult.lineCount
    val totalHeight = textLayoutResult.size.height
    val S_plus_I = if (lineCount > 0) totalHeight.toFloat() / lineCount else 0f
    
    // 6. N - сколько строк помещается
    val N = if (S_plus_I > 0) (D / S_plus_I).toInt() else 0
    
    // 7. Проверка обрезки с порогом
    val remainder = D - S_plus_I * N
    val threshold = S_plus_I * 0.2f  // Порог 20% от высоты строки
    val isCut = remainder > threshold  // Строка обрезана если остаток больше порога
    
    android.util.Log.d("TEXT_LINE_DETECTOR", "Top line check (TextMeasurer) - D: $D, S_plus_I: $S_plus_I, N: $N, remainder: $remainder, threshold: $threshold, isCut: $isCut, lineCount: $lineCount, totalHeight: $totalHeight")
    return isCut
}

/**
 * Проверяет обрезана ли первая строка текстового элемента
 */
private fun isTopLineCut(
    item: androidx.compose.foundation.lazy.LazyListItemInfo,
    readerText: ReaderText.Text,
    textPaint: TextPaint,
    availableWidth: Int,
    fontSize: TextUnit,
    lineHeight: TextUnit,
    density: Float
): Boolean {
    // Если offset >= 0, первый элемент не обрезан сверху
    if (item.offset >= 0) {
        android.util.Log.d("TEXT_LINE_DETECTOR", "Top line check - offset >= 0, not cut")
        return false
    }
    
    // 1. Создаём StaticLayout с ВСЕМИ параметрами текста
    val text = readerText.line.text
    val lineSpacingMultiplier = TextMeasurementUtils.getLineSpacingMultiplier(lineHeight, fontSize)
    val layout = StaticLayout.Builder.obtain(
        text,
        0,
        text.length,
        textPaint,
        availableWidth
    )
        .setLineSpacing(0f, lineSpacingMultiplier)
        .setIncludePad(false)
        .build()

    // 2. D - скрытое пространство сверху
    val D = -item.offset.toFloat()
    
    // 3. S - точная высота строки из StaticLayout (включая интервал)
    val S_plus_I = if (layout.lineCount > 0) layout.height.toFloat() / layout.lineCount else 0f
    
    // 4. N - сколько строк помещается
    val N = (D / S_plus_I).toInt()
    
    // 5. Проверка обрезки
    val isCut = D > S_plus_I * N  // Если осталось место больше чем одна строка
    
    android.util.Log.d("TEXT_LINE_DETECTOR", "Top line check - D: $D, S_plus_I: $S_plus_I, N: $N, threshold: ${S_plus_I * N}, isCut: $isCut, layoutHeight: ${layout.height}, lineCount: ${layout.lineCount}")
    return isCut
}

/**
 * Проверяет обрезана ли последняя строка текстового элемента с TextMeasurer
 */
private fun isBottomLineCutWithTextMeasurer(
    item: androidx.compose.foundation.lazy.LazyListItemInfo,
    readerText: ReaderText.Text,
    textMeasurer: TextMeasurer,
    availableWidth: Int,
    screenHeight: Int,
    fontSize: TextUnit,
    lineHeight: TextUnit,
    fontFamily: FontWithName,
    fontThickness: ReaderFontThickness,
    fontStyle: FontStyle,
    textAlignment: ReaderTextAlignment,
    letterSpacing: TextUnit
): Boolean {
    // 1. Создаём TextStyle с теми же параметрами
    val textStyle = TextStyle(
        fontSize = fontSize,
        lineHeight = lineHeight,
        fontFamily = null, // TODO: Implement proper font family conversion
        fontWeight = TextMeasurementUtils.getFontWeight(fontThickness),
        fontStyle = fontStyle,
        textAlign = TextMeasurementUtils.getTextAlign(textAlignment),
        letterSpacing = letterSpacing
    )
    
    // 2. Создаём Constraints
    val constraints = Constraints(
        maxWidth = availableWidth,
        maxHeight = Int.MAX_VALUE
    )
    
    // 3. Измеряем текст с TextMeasurer
    val textLayoutResult = textMeasurer.measure(
        text = readerText.line,
        style = textStyle,
        constraints = constraints
    )
    
    // 4. D - доступное пространство под последнюю секцию
    val D = screenHeight - item.offset
    
    // 5. S_plus_I - точная высота строки из TextMeasurer
    val lineCount = textLayoutResult.lineCount
    val totalHeight = textLayoutResult.size.height
    val S_plus_I = if (lineCount > 0) totalHeight.toFloat() / lineCount else 0f
    
    // 6. N - сколько строк помещается
    val N = if (S_plus_I > 0) (D / S_plus_I).toInt() else 0
    
    // 7. Проверка обрезки с порогом
    val remainder = D - S_plus_I * N
    val threshold = S_plus_I * 0.2f  // Порог 20% от высоты строки
    val isCut = remainder > threshold  // Строка обрезана если остаток больше порога
    
    android.util.Log.d("TEXT_LINE_DETECTOR", "Bottom line check (TextMeasurer) - D: $D, S_plus_I: $S_plus_I, N: $N, remainder: $remainder, threshold: $threshold, isCut: $isCut, lineCount: $lineCount, totalHeight: $totalHeight")
    return isCut
}

/**
 * Проверяет обрезана ли последняя строка текстового элемента
 */
private fun isBottomLineCut(
    item: androidx.compose.foundation.lazy.LazyListItemInfo,
    readerText: ReaderText.Text,
    textPaint: TextPaint,
    availableWidth: Int,
    screenHeight: Int,
    fontSize: TextUnit,
    lineHeight: TextUnit,
    density: Float
): Boolean {
    // 1. Создаём StaticLayout с ВСЕМИ параметрами текста
    val text = readerText.line.text
    val lineSpacingMultiplier = TextMeasurementUtils.getLineSpacingMultiplier(lineHeight, fontSize)
    val layout = StaticLayout.Builder.obtain(
        text,
        0,
        text.length,
        textPaint,
        availableWidth
    )
        .setLineSpacing(0f, lineSpacingMultiplier)
        .setIncludePad(false)
        .build()

    // 2. D - доступное пространство под последнюю секцию
    val D = screenHeight - item.offset
    
    // 3. S - точная высота строки из StaticLayout (включая интервал)
    val S_plus_I = if (layout.lineCount > 0) layout.height.toFloat() / layout.lineCount else 0f
    
    // 4. N - сколько строк помещается
    val N = (D / S_plus_I).toInt()
    
    // 5. Проверка обрезки
    val isCut = D > S_plus_I * N  // Если осталось место больше чем одна строка
    
    android.util.Log.d("TEXT_LINE_DETECTOR", "Bottom line check - D: $D, S_plus_I: $S_plus_I, N: $N, threshold: ${S_plus_I * N}, isCut: $isCut, layoutHeight: ${layout.height}, lineCount: ${layout.lineCount}")
    return isCut
}
