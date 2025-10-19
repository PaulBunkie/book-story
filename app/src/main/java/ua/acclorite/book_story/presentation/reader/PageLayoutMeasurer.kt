/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.presentation.reader

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import ua.acclorite.book_story.domain.reader.FontWithName
import ua.acclorite.book_story.domain.reader.ReaderFontThickness
import ua.acclorite.book_story.domain.reader.ReaderText
import ua.acclorite.book_story.domain.reader.ReaderTextAlignment
import ua.acclorite.book_story.presentation.core.components.common.StyledText

/**
 * Измеряет реальные размеры Compose элементов для расчета страниц
 */
@Composable
fun PageLayoutMeasurer(
    text: List<ReaderText>,
    screenWidth: Int,
    screenHeight: Int,
    fontSize: TextUnit,
    lineHeight: TextUnit,
    sidePadding: Dp,
    paragraphHeight: Dp,
    fontFamily: FontWithName,
    fontThickness: ReaderFontThickness,
    fontStyle: FontStyle,
    textAlignment: ReaderTextAlignment,
    letterSpacing: TextUnit,
    paragraphIndentation: TextUnit,
    contentPadding: PaddingValues,
    verticalPadding: Dp,
    fontColor: Color,
    highlightedReading: Boolean,
    highlightedReadingThickness: FontWeight,
    onPagesCalculated: (List<Page>) -> Unit
) {
    val density = LocalDensity.current.density

    Log.d("PAGE_MEASURER", "=== Starting PAGE MEASUREMENT ===")
    Log.d("PAGE_MEASURER", "Text items: ${text.size}")
    Log.d("PAGE_MEASURER", "Screen: ${screenWidth}x${screenHeight}")

    // Рассчитываем доступное пространство
    val contentPaddingPx = with(LocalDensity.current) {
        (contentPadding.calculateTopPadding() + contentPadding.calculateBottomPadding()).toPx().toInt()
    }
    val verticalPaddingPx = with(LocalDensity.current) {
        (verticalPadding * 2).toPx().toInt()
    }
    val sidePaddingPx = with(LocalDensity.current) {
        (sidePadding * 2).toPx().toInt()
    }

    val availableWidth = screenWidth - sidePaddingPx
    val availableHeight = screenHeight - contentPaddingPx - verticalPaddingPx

    Log.d("PAGE_MEASURER", "Available: ${availableWidth}x${availableHeight}")

    // SubcomposeLayout для измерения элементов
    SubcomposeLayout { constraints ->
        val pages = mutableListOf<Page>()
        var currentPage = mutableListOf<ReaderText>()
        var currentPageHeight = 0
        var pageStartIndex = 0

        val measureConstraints = Constraints(
            minWidth = availableWidth,
            maxWidth = availableWidth,
            minHeight = 0,
            maxHeight = Constraints.Infinity // Неограниченная высота для измерения
        )

        text.forEachIndexed { index, readerText ->
            // Измеряем элемент
            val elementHeight = measureElement(
                measurer = this,
                readerText = readerText,
                constraints = measureConstraints,
                fontSize = fontSize,
                lineHeight = lineHeight,
                fontFamily = fontFamily,
                fontThickness = fontThickness,
                fontStyle = fontStyle,
                textAlignment = textAlignment,
                letterSpacing = letterSpacing,
                paragraphIndentation = paragraphIndentation,
                fontColor = fontColor,
                highlightedReading = highlightedReading,
                highlightedReadingThickness = highlightedReadingThickness,
                paragraphHeight = paragraphHeight,
                isFirstElement = currentPage.isEmpty(),
                slotId = "element_$index",
                density = density
            )

            Log.d("PAGE_MEASURER", "Element $index: ${readerText.javaClass.simpleName}, height=$elementHeight")

            // Добавляем высоту spacing (если не первый элемент на странице)
            val spacingHeight = if (currentPage.isEmpty()) {
                0
            } else {
                (paragraphHeight.value * density).toInt()
            }

            val totalElementHeight = elementHeight + spacingHeight

            // Проверяем, влезает ли элемент на текущую страницу
            if (currentPageHeight + totalElementHeight <= availableHeight) {
                // Влезает - добавляем на текущую страницу
                currentPage.add(readerText)
                currentPageHeight += totalElementHeight
                Log.d("PAGE_MEASURER", "  -> Added to page ${pages.size}, height now: $currentPageHeight/$availableHeight")
            } else {
                // Не влезает - создаем новую страницу
                if (currentPage.isNotEmpty()) {
                    pages.add(Page(
                        content = currentPage.toList(),
                        startIndex = pageStartIndex,
                        endIndex = index - 1
                    ))
                    Log.d("PAGE_MEASURER", "Page ${pages.size - 1} completed: ${currentPage.size} elements, height=$currentPageHeight")
                }

                // Начинаем новую страницу с текущего элемента
                currentPage = mutableListOf(readerText)
                currentPageHeight = elementHeight // Без spacing, т.к. первый элемент
                pageStartIndex = index
                Log.d("PAGE_MEASURER", "  -> Started new page ${pages.size}, height: $currentPageHeight/$availableHeight")
            }
        }

        // Добавляем последнюю страницу
        if (currentPage.isNotEmpty()) {
            pages.add(Page(
                content = currentPage.toList(),
                startIndex = pageStartIndex,
                endIndex = text.size - 1
            ))
            Log.d("PAGE_MEASURER", "Final page ${pages.size - 1}: ${currentPage.size} elements, height=$currentPageHeight")
        }

        Log.d("PAGE_MEASURER", "=== MEASUREMENT COMPLETE: ${pages.size} pages ===")
        onPagesCalculated(pages)

        // Layout не рисует ничего - только измеряет
        layout(0, 0) {}
    }
}

/**
 * Измеряет высоту одного ReaderText элемента
 */
private fun measureElement(
    measurer: androidx.compose.ui.layout.SubcomposeMeasureScope,
    readerText: ReaderText,
    constraints: Constraints,
    fontSize: TextUnit,
    lineHeight: TextUnit,
    fontFamily: FontWithName,
    fontThickness: ReaderFontThickness,
    fontStyle: FontStyle,
    textAlignment: ReaderTextAlignment,
    letterSpacing: TextUnit,
    paragraphIndentation: TextUnit,
    fontColor: Color,
    highlightedReading: Boolean,
    highlightedReadingThickness: FontWeight,
    paragraphHeight: Dp,
    isFirstElement: Boolean,
    slotId: String,
    density: Float
): Int {
    return when (readerText) {
        is ReaderText.Text -> {
            // Измеряем StyledText
            val placeable = measurer.subcompose(slotId) {
                StyledText(
                    text = readerText.line,
                    style = TextStyle(
                        fontFamily = fontFamily.font,
                        fontWeight = fontThickness.thickness,
                        textAlign = textAlignment.textAlignment,
                        textIndent = TextIndent(firstLine = paragraphIndentation),
                        fontStyle = fontStyle,
                        letterSpacing = letterSpacing,
                        fontSize = fontSize,
                        lineHeight = lineHeight,
                        color = fontColor,
                        lineBreak = LineBreak.Paragraph
                    ),
                    highlightText = highlightedReading,
                    highlightThickness = highlightedReadingThickness,
                    modifier = Modifier.fillMaxWidth()
                )
            }.first().measure(constraints)
            placeable.height
        }

        is ReaderText.Chapter -> {
            // Измеряем Chapter с полоской
            val placeable = measurer.subcompose(slotId) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.height(22.dp))
                    Text(
                        text = readerText.title,
                        style = TextStyle(
                            fontFamily = fontFamily.font,
                            fontWeight = FontWeight.Bold,
                            textAlign = textAlignment.textAlignment,
                            fontSize = fontSize * 1.2f,
                            lineHeight = lineHeight * 1.2f,
                            color = fontColor
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = fontColor.copy(0.4f))
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }.first().measure(constraints)
            placeable.height
        }

        is ReaderText.Separator -> {
            // Измеряем Separator
            val placeable = measurer.subcompose(slotId) {
                Text(
                    text = "---",
                    style = TextStyle(
                        fontFamily = fontFamily.font,
                        fontWeight = fontThickness.thickness,
                        textAlign = TextAlign.Center,
                        fontSize = fontSize,
                        lineHeight = lineHeight,
                        color = fontColor
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                )
            }.first().measure(constraints)
            placeable.height
        }

        is ReaderText.Image -> {
            // Фиксированная высота для изображений (как в реальном рендере)
            ((200 + 32) * density).toInt() // 200dp image + 32dp vertical padding
        }
    }
}

