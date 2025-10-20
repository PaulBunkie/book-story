/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.presentation.reader

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
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
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ua.acclorite.book_story.domain.reader.FontWithName
import ua.acclorite.book_story.domain.reader.ReaderFontThickness
import ua.acclorite.book_story.domain.reader.ReaderText
import ua.acclorite.book_story.domain.reader.ReaderTextAlignment
import ua.acclorite.book_story.presentation.core.components.common.StyledText

/**
 * ЛЕНИВОЕ измерение страниц - сначала рассчитываем первые страницы, остальные по требованию
 */
@Composable
fun LazyPageLayoutMeasurer(
    bookId: Int,
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
    imagesCornersRoundness: Dp,
    imagesAlignment: ua.acclorite.book_story.domain.util.HorizontalAlignment,
    imagesWidth: Float,
    imagesColorEffects: ColorFilter?,
    initialPagesCount: Int = 5, // Сколько страниц считаем сразу
    onPagesCalculated: (List<Page>) -> Unit,
    onTotalPagesEstimate: (Int) -> Unit // Примерное количество страниц
) {
    val density = LocalDensity.current.density
    val scope = rememberCoroutineScope()

    Log.d("LAZY_PAGE_MEASURER", "=== Starting LAZY PAGE MEASUREMENT ===")
    Log.d("LAZY_PAGE_MEASURER", "Text items: ${text.size}")
    Log.d("LAZY_PAGE_MEASURER", "Screen: ${screenWidth}x${screenHeight}")
    Log.d("LAZY_PAGE_MEASURER", "Initial pages to calculate: $initialPagesCount")

    // Рассчитываем доступное пространство
    val layoutDirection = androidx.compose.ui.platform.LocalLayoutDirection.current
    val contentPaddingVerticalPx = with(LocalDensity.current) {
        (contentPadding.calculateTopPadding() + contentPadding.calculateBottomPadding()).toPx().toInt()
    }
    val contentPaddingHorizontalPx = with(LocalDensity.current) {
        (contentPadding.calculateStartPadding(layoutDirection) + contentPadding.calculateEndPadding(layoutDirection)).toPx().toInt()
    }
    val verticalPaddingPx = with(LocalDensity.current) {
        (verticalPadding * 2).toPx().toInt()
    }
    val sidePaddingPx = with(LocalDensity.current) {
        (sidePadding * 2).toPx().toInt()
    }

    val availableWidth = screenWidth - sidePaddingPx - contentPaddingHorizontalPx
    val availableHeight = screenHeight - contentPaddingVerticalPx - verticalPaddingPx

    Log.d("LAZY_PAGE_MEASURER", "Margins: contentH=$contentPaddingHorizontalPx, contentV=$contentPaddingVerticalPx, sideP=$sidePaddingPx, vertP=$verticalPaddingPx")
    Log.d("LAZY_PAGE_MEASURER", "Available: ${availableWidth}x${availableHeight}")

    // SubcomposeLayout для измерения элементов ОДИН РАЗ
    var hasCalculated by remember(bookId, text, fontSize, lineHeight, sidePadding, paragraphHeight) { 
        mutableStateOf(false) 
    }
    
    if (!hasCalculated) {
        SubcomposeLayout { constraints ->
            val measureConstraints = Constraints(
                minWidth = availableWidth,
                maxWidth = availableWidth,
                minHeight = 0,
                maxHeight = Constraints.Infinity
            )
            
            val pages = calculateInitialPages(
                text = text,
                initialPagesCount = initialPagesCount,
                availableWidth = availableWidth,
                availableHeight = availableHeight,
                density = density,
                elementHeights = mutableMapOf(),
                paragraphHeight = paragraphHeight,
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
                imagesCornersRoundness = imagesCornersRoundness,
                imagesAlignment = imagesAlignment,
                imagesWidth = imagesWidth,
                imagesColorEffects = imagesColorEffects,
                measurer = this,
                measureConstraints = measureConstraints
            )
            
            hasCalculated = true
            onPagesCalculated(pages)
            
            // Оценка общего количества страниц
            if (pages.isNotEmpty() && text.isNotEmpty()) {
                val avgElementsPerPage = pages.sumOf { it.content.size } / pages.size.toFloat()
                val estimatedTotalPages = (text.size / avgElementsPerPage).toInt()
                Log.d("LAZY_PAGE_MEASURER", "Estimated total pages: $estimatedTotalPages (avg $avgElementsPerPage elements/page)")
                onTotalPagesEstimate(estimatedTotalPages)
            }

            // Layout не рисует ничего - только измеряет
            layout(0, 0) {}
        }
    }
}

/**
 * Рассчитывает первые N страниц
 */
private fun calculateInitialPages(
    text: List<ReaderText>,
    initialPagesCount: Int,
    availableWidth: Int,
    availableHeight: Int,
    density: Float,
    elementHeights: MutableMap<Int, Int>,
    paragraphHeight: Dp,
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
    imagesCornersRoundness: Dp,
    imagesAlignment: ua.acclorite.book_story.domain.util.HorizontalAlignment,
    imagesWidth: Float,
    imagesColorEffects: ColorFilter?,
    measurer: androidx.compose.ui.layout.SubcomposeMeasureScope,
    measureConstraints: Constraints
): List<Page> {
    val pages = mutableListOf<Page>()
    var currentPage = mutableListOf<ReaderText>()
    var currentPageHeight = 0
    var pageStartIndex = 0
    var pagesCalculated = 0

    for (index in text.indices) {
        // Останавливаемся после расчёта нужного количества страниц
        if (pagesCalculated >= initialPagesCount) {
            break
        }

        val readerText = text[index]
        
        // Измеряем элемент (с кешем)
        val elementHeight = elementHeights.getOrPut(index) {
            measureElement(
                measurer = measurer,
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
                density = density,
                imagesCornersRoundness = imagesCornersRoundness,
                imagesAlignment = imagesAlignment,
                imagesWidth = imagesWidth,
                imagesColorEffects = imagesColorEffects
            )
        }

        Log.d("LAZY_PAGE_MEASURER", "Element $index: ${readerText.javaClass.simpleName}, height=$elementHeight")

        // Добавляем высоту spacing (если не первый элемент на странице)
        val spacingHeight = if (currentPage.isEmpty()) 0 else (paragraphHeight.value * density).toInt()
        val totalElementHeight = elementHeight + spacingHeight

        // Проверяем, влезает ли элемент на текущую страницу
        if (currentPageHeight + totalElementHeight <= availableHeight) {
            // Влезает - добавляем на текущую страницу
            currentPage.add(readerText)
            currentPageHeight += totalElementHeight
            Log.d("LAZY_PAGE_MEASURER", "  -> Added to page $pagesCalculated, height now: $currentPageHeight/$availableHeight")
        } else {
            // Не влезает - создаем новую страницу
            if (currentPage.isNotEmpty()) {
                pages.add(Page(
                    content = currentPage.toList(),
                    startIndex = pageStartIndex,
                    endIndex = index - 1
                ))
                Log.d("LAZY_PAGE_MEASURER", "Page $pagesCalculated completed: ${currentPage.size} elements, height=$currentPageHeight")
                pagesCalculated++
            }

            // Начинаем новую страницу с текущего элемента
            currentPage = mutableListOf(readerText)
            currentPageHeight = elementHeight // Без spacing, т.к. первый элемент
            pageStartIndex = index
            Log.d("LAZY_PAGE_MEASURER", "  -> Started new page $pagesCalculated, height: $currentPageHeight/$availableHeight")
        }
    }

    // Добавляем последнюю страницу
    if (currentPage.isNotEmpty()) {
        pages.add(Page(
            content = currentPage.toList(),
            startIndex = pageStartIndex,
            endIndex = text.lastIndex.coerceAtMost(pageStartIndex + currentPage.size - 1)
        ))
        Log.d("LAZY_PAGE_MEASURER", "Final page $pagesCalculated: ${currentPage.size} elements, height=$currentPageHeight")
    }

    Log.d("LAZY_PAGE_MEASURER", "=== LAZY MEASUREMENT COMPLETE: ${pages.size} pages (of $initialPagesCount requested) ===")
    return pages
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
    density: Float,
    imagesCornersRoundness: Dp,
    imagesAlignment: ua.acclorite.book_story.domain.util.HorizontalAlignment,
    imagesWidth: Float,
    imagesColorEffects: ColorFilter?
): Int {
    return when (readerText) {
        is ReaderText.Text -> {
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
            // Измеряем РЕАЛЬНУЮ высоту изображения через SubcomposeLayout
            val placeable = measurer.subcompose(slotId) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = imagesAlignment.alignment
                ) {
                    Image(
                        modifier = Modifier
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(imagesCornersRoundness))
                            .fillMaxWidth(imagesWidth),
                        bitmap = readerText.imageBitmap,
                        contentDescription = null,
                        colorFilter = imagesColorEffects,
                        contentScale = androidx.compose.ui.layout.ContentScale.FillWidth
                    )
                }
            }.first().measure(constraints)
            placeable.height
        }
    }
}

/**
 * Рассчитать диапазон страниц начиная с определенного элемента
 * Используется для ленивой загрузки страниц по мере прокрутки
 */
@Composable
fun calculatePageRangeComposable(
    text: List<ReaderText>,
    startElement: Int,
    pagesToCalculate: Int,
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
    imagesCornersRoundness: Dp,
    imagesAlignment: ua.acclorite.book_story.domain.util.HorizontalAlignment,
    imagesWidth: Float,
    imagesColorEffects: ColorFilter?,
    onPagesCalculated: (List<Page>) -> Unit
) {
    val density = LocalDensity.current.density
    val layoutDirection = androidx.compose.ui.platform.LocalLayoutDirection.current
    
    // Рассчитываем доступное пространство
    val contentPaddingVerticalPx = with(LocalDensity.current) {
        (contentPadding.calculateTopPadding() + contentPadding.calculateBottomPadding()).toPx().toInt()
    }
    val contentPaddingHorizontalPx = with(LocalDensity.current) {
        (contentPadding.calculateStartPadding(layoutDirection) + contentPadding.calculateEndPadding(layoutDirection)).toPx().toInt()
    }
    val verticalPaddingPx = with(LocalDensity.current) {
        (verticalPadding * 2).toPx().toInt()
    }
    val sidePaddingPx = with(LocalDensity.current) {
        (sidePadding * 2).toPx().toInt()
    }
    
    val availableWidth = screenWidth - sidePaddingPx - contentPaddingHorizontalPx
    val availableHeight = screenHeight - contentPaddingVerticalPx - verticalPaddingPx
    
    // SubcomposeLayout для измерения элементов
    SubcomposeLayout { constraints ->
        val measureConstraints = Constraints(
            minWidth = availableWidth,
            maxWidth = availableWidth,
            minHeight = 0,
            maxHeight = Constraints.Infinity
        )
        
        val elementHeights = mutableMapOf<Int, Int>()
        
        val pages = calculatePagesFromElement(
            text = text,
            startElement = startElement,
            pagesToCalculate = pagesToCalculate,
            availableWidth = availableWidth,
            availableHeight = availableHeight,
            density = density,
            elementHeights = elementHeights,
            paragraphHeight = paragraphHeight,
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
            imagesCornersRoundness = imagesCornersRoundness,
            imagesAlignment = imagesAlignment,
            imagesWidth = imagesWidth,
            imagesColorEffects = imagesColorEffects,
            measurer = this,
            measureConstraints = measureConstraints
        )
        
        onPagesCalculated(pages)
        
        // Layout не рисует ничего - только измеряет
        layout(0, 0) {}
    }
}

/**
 * Внутренняя функция для расчета страниц начиная с определенного элемента
 */
private fun calculatePagesFromElement(
    text: List<ReaderText>,
    startElement: Int,
    pagesToCalculate: Int,
    availableWidth: Int,
    availableHeight: Int,
    density: Float,
    elementHeights: MutableMap<Int, Int>,
    paragraphHeight: Dp,
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
    imagesCornersRoundness: Dp,
    imagesAlignment: ua.acclorite.book_story.domain.util.HorizontalAlignment,
    imagesWidth: Float,
    imagesColorEffects: ColorFilter?,
    measurer: androidx.compose.ui.layout.SubcomposeMeasureScope,
    measureConstraints: Constraints
): List<Page> {
    val pages = mutableListOf<Page>()
    var currentPage = mutableListOf<ReaderText>()
    var currentPageHeight = 0
    var pageStartIndex = startElement
    var pagesCalculated = 0
    
    Log.d("LAZY_PAGE_MEASURER", "=== Calculating pages from element $startElement ===")
    
    for (index in startElement until text.size) {
        // Останавливаемся после расчёта нужного количества страниц
        if (pagesCalculated >= pagesToCalculate) {
            break
        }
        
        val readerText = text[index]
        
        // Измеряем элемент (с кешем)
        val elementHeight = elementHeights.getOrPut(index) {
            measureElement(
                measurer = measurer,
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
                isFirstElement = (index == startElement),
                slotId = "page_element_$index",
                density = density,
                imagesCornersRoundness = imagesCornersRoundness,
                imagesAlignment = imagesAlignment,
                imagesWidth = imagesWidth,
                imagesColorEffects = imagesColorEffects
            )
        }
        
        Log.d("LAZY_PAGE_MEASURER", "Element $index: ${readerText.javaClass.simpleName}, height=$elementHeight")
        
        // Проверяем влезет ли элемент на текущую страницу
        val paragraphSpacingPx = if (currentPage.isNotEmpty()) {
            (paragraphHeight.value * density).toInt()
        } else 0
        
        if (currentPageHeight + paragraphSpacingPx + elementHeight <= availableHeight) {
            // Влезает - добавляем на текущую страницу
            currentPage.add(readerText)
            currentPageHeight += paragraphSpacingPx + elementHeight
            Log.d("LAZY_PAGE_MEASURER", "  -> Added to page $pagesCalculated, height now: $currentPageHeight/$availableHeight")
        } else {
            // Не влезает - завершаем текущую страницу и начинаем новую
            if (currentPage.isNotEmpty()) {
                Log.d("LAZY_PAGE_MEASURER", "Page $pagesCalculated completed: ${currentPage.size} elements, height=$currentPageHeight")
                pages.add(
                    Page(
                        content = currentPage.toList(),
                        startIndex = pageStartIndex,
                        endIndex = index - 1
                    )
                )
                pagesCalculated++
            }
            
            // Начинаем новую страницу с текущего элемента
            currentPage = mutableListOf(readerText)
            currentPageHeight = elementHeight
            pageStartIndex = index
            Log.d("LAZY_PAGE_MEASURER", "  -> Started new page $pagesCalculated, height: $elementHeight/$availableHeight")
        }
    }
    
    // Добавляем последнюю страницу если она не пустая
    if (currentPage.isNotEmpty() && pagesCalculated < pagesToCalculate) {
        Log.d("LAZY_PAGE_MEASURER", "Final page $pagesCalculated: ${currentPage.size} elements, height=$currentPageHeight")
        pages.add(
            Page(
                content = currentPage,
                startIndex = pageStartIndex,
                endIndex = text.size - 1
            )
        )
    }
    
    Log.d("LAZY_PAGE_MEASURER", "=== Calculated ${pages.size} pages starting from element $startElement ===")
    
    return pages
}

