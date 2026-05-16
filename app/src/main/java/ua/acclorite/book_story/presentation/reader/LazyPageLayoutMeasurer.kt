/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.presentation.reader

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import ua.acclorite.book_story.domain.reader.FontWithName
import ua.acclorite.book_story.domain.reader.ReaderFontThickness
import ua.acclorite.book_story.domain.reader.ReaderText
import ua.acclorite.book_story.domain.reader.ReaderTextAlignment
import ua.acclorite.book_story.presentation.core.components.common.StyledText

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
    initialPagesCount: Int = 5,
    progressBar: Boolean,
    progressBarPadding: Dp,
    progressBarFontSize: TextUnit,
    startElement: Int = 0,
    startCarryOverText: ReaderText.Text? = null,
    onPagesCalculated: (List<Page>) -> Unit,
    onTotalPagesEstimate: (Int) -> Unit
) {
    val density = LocalDensity.current.density
    val layoutDirection = androidx.compose.ui.platform.LocalLayoutDirection.current
    val textMeasurer = rememberTextMeasurer()
    
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
    
    val progressBarHeightPx = if (progressBar) {
        with(LocalDensity.current) {
            (progressBarFontSize.toDp() + progressBarPadding * 2).toPx().toInt()
        }
    } else 0

    val availableWidth = screenWidth - sidePaddingPx - contentPaddingHorizontalPx
    val availableHeight = screenHeight - contentPaddingVerticalPx - verticalPaddingPx - progressBarHeightPx - 16

    var hasCalculated by remember(bookId, text, fontSize, lineHeight, sidePadding, paragraphHeight, availableHeight, startElement, startCarryOverText) { 
        mutableStateOf(false) 
    }
    
    if (!hasCalculated) {
        val chapterStyleMedium = MaterialTheme.typography.headlineMedium
        val chapterStyleSmall = MaterialTheme.typography.headlineSmall
        
        SubcomposeLayout { constraints ->
            val measureConstraints = Constraints(
                minWidth = availableWidth,
                maxWidth = availableWidth,
                minHeight = 0,
                maxHeight = Constraints.Infinity
            )
            
            val pages = calculatePagesCore(
                text = text,
                startElement = startElement,
                startCarryOverText = startCarryOverText,
                pagesToCalculate = initialPagesCount,
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
                measureConstraints = measureConstraints,
                chapterStyleMedium = chapterStyleMedium,
                chapterStyleSmall = chapterStyleSmall,
                textMeasurer = textMeasurer
            )
            
            hasCalculated = true
            onPagesCalculated(pages)
            
            if (pages.isNotEmpty() && text.isNotEmpty()) {
                val avgElementsPerPage = pages.sumOf { it.content.size } / pages.size.toFloat()
                val estimatedTotalPages = (text.size / avgElementsPerPage).toInt()
                onTotalPagesEstimate(estimatedTotalPages)
            }

            layout(0, 0) {}
        }
    }
}

private fun calculatePagesCore(
    text: List<ReaderText>,
    startElement: Int,
    startCarryOverText: ReaderText.Text?,
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
    measureConstraints: Constraints,
    chapterStyleMedium: TextStyle,
    chapterStyleSmall: TextStyle,
    textMeasurer: TextMeasurer
): List<Page> {
    val pages = mutableListOf<Page>()
    var currentPage = mutableListOf<ReaderText>()
    var currentPageHeight = 0
    var pageStartIndex = startElement
    var pagesCalculated = 0

    var index = startElement
    var remainingTextPart: ReaderText.Text? = startCarryOverText

    while (index < text.size && pagesCalculated < pagesToCalculate) {
        val readerText = remainingTextPart ?: text[index]
        
        val elementHeight = if (remainingTextPart != null) {
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
                slotId = "element_${index}_rem_${pagesCalculated}",
                density = density,
                imagesCornersRoundness = imagesCornersRoundness,
                imagesAlignment = imagesAlignment,
                imagesWidth = imagesWidth,
                imagesColorEffects = imagesColorEffects,
                chapterStyleMedium = chapterStyleMedium,
                chapterStyleSmall = chapterStyleSmall
            )
        } else {
            elementHeights.getOrPut(index) {
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
                    imagesColorEffects = imagesColorEffects,
                    chapterStyleMedium = chapterStyleMedium,
                    chapterStyleSmall = chapterStyleSmall
                )
            }
        }

        val spacingHeight = if (currentPage.isEmpty()) 0 else (paragraphHeight.value * density).toInt()
        val totalElementHeight = elementHeight + spacingHeight

        if (currentPageHeight + totalElementHeight <= availableHeight) {
            currentPage.add(readerText)
            currentPageHeight += totalElementHeight
            remainingTextPart = null
            index++
        } else {
            if (readerText is ReaderText.Text) {
                val effectiveAvailableHeight = availableHeight - currentPageHeight - spacingHeight
                
                if (effectiveAvailableHeight > (fontSize.value * density * 2)) {
                     val textStyle = TextStyle(
                        fontFamily = fontFamily.font,
                        fontWeight = fontThickness.thickness,
                        textAlign = textAlignment.textAlignment,
                        textIndent = if (readerText.line.text.startsWith("\u200B")) TextIndent.None else TextIndent(firstLine = paragraphIndentation),
                        fontStyle = fontStyle,
                        letterSpacing = letterSpacing,
                        fontSize = fontSize,
                        lineHeight = lineHeight,
                        lineBreak = LineBreak.Paragraph
                    )
                    
                    val layoutResult = textMeasurer.measure(
                        text = readerText.line,
                        style = textStyle,
                        constraints = Constraints(maxWidth = availableWidth)
                    )
                    
                    var lastFittingLine = -1
                    for (i in 0 until layoutResult.lineCount) {
                        if (layoutResult.getLineBottom(i) <= effectiveAvailableHeight) {
                            lastFittingLine = i
                        } else break
                    }
                    
                    if (lastFittingLine >= 0) {
                        val splitOffset = layoutResult.getLineEnd(lastFittingLine)
                        
                        if (splitOffset > 0 && splitOffset < readerText.line.length) {
                             val firstPart = ReaderText.Text(readerText.line.subSequence(0, splitOffset))
                             currentPage.add(firstPart)
                             
                             val remainingAnnotated = readerText.line.subSequence(splitOffset, readerText.line.length)
                             remainingTextPart = ReaderText.Text(
                                 buildAnnotatedString {
                                     append("\u200B")
                                     append(remainingAnnotated)
                                 }
                             )
                             
                             pages.add(Page(currentPage.toList(), pageStartIndex, index, remainingTextPart))
                             pagesCalculated++
                             currentPage = mutableListOf()
                             currentPageHeight = 0
                             pageStartIndex = index
                             continue
                        }
                    }
                }
            }

            if (currentPage.isNotEmpty()) {
                pages.add(Page(
                    content = currentPage.toList(),
                    startIndex = pageStartIndex,
                    endIndex = index - 1
                ))
                pagesCalculated++
            }

            currentPage = mutableListOf(readerText)
            currentPageHeight = elementHeight
            pageStartIndex = index
            remainingTextPart = null
            index++
        }
    }

    if (currentPage.isNotEmpty() && pagesCalculated < pagesToCalculate) {
        pages.add(Page(
            content = currentPage.toList(),
            startIndex = pageStartIndex,
            endIndex = text.lastIndex.coerceAtMost(index),
            carryOverText = remainingTextPart
        ))
    }

    return pages
}

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
    imagesColorEffects: ColorFilter?,
    chapterStyleMedium: TextStyle,
    chapterStyleSmall: TextStyle
): Int {
    return when (readerText) {
        is ReaderText.Text -> {
            val isContinuation = readerText.line.text.startsWith("\u200B")
            val placeable = measurer.subcompose(slotId) {
                StyledText(
                    text = readerText.line,
                    style = TextStyle(
                        fontFamily = fontFamily.font,
                        fontWeight = fontThickness.thickness,
                        textAlign = textAlignment.textAlignment,
                        textIndent = if (isContinuation) TextIndent.None else TextIndent(firstLine = paragraphIndentation),
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
                    StyledText(
                        text = buildAnnotatedString { append(readerText.title) },
                        style = (if (!readerText.nested) chapterStyleMedium
                               else chapterStyleSmall)
                            .copy(
                                color = fontColor,
                                textAlign = textAlignment.textAlignment
                            ),
                        highlightText = highlightedReading,
                        highlightThickness = highlightedReadingThickness,
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
                HorizontalDivider(
                    thickness = 3.dp,
                    modifier = Modifier.clip(CircleShape),
                    color = fontColor.copy(0.3f)
                )
            }.first().measure(constraints)
            placeable.height
        }

        is ReaderText.Image -> {
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

@Composable
fun calculatePageRangeComposable(
    text: List<ReaderText>,
    startElement: Int,
    startCarryOverText: ReaderText.Text? = null,
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
    progressBar: Boolean,
    progressBarPadding: Dp,
    progressBarFontSize: TextUnit,
    onPagesCalculated: (List<Page>) -> Unit
) {
    val density = LocalDensity.current.density
    val layoutDirection = androidx.compose.ui.platform.LocalLayoutDirection.current
    val textMeasurer = rememberTextMeasurer()
    
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
    
    val progressBarHeightPx = if (progressBar) {
        with(LocalDensity.current) {
            (progressBarFontSize.toDp() + progressBarPadding * 2).toPx().toInt()
        }
    } else 0

    val availableWidth = screenWidth - sidePaddingPx - contentPaddingHorizontalPx
    val availableHeight = screenHeight - contentPaddingVerticalPx - verticalPaddingPx - progressBarHeightPx - 16

    val chapterStyleMedium = MaterialTheme.typography.headlineMedium
    val chapterStyleSmall = MaterialTheme.typography.headlineSmall

    SubcomposeLayout { constraints ->
        val measureConstraints = Constraints(
            minWidth = availableWidth,
            maxWidth = availableWidth,
            minHeight = 0,
            maxHeight = Constraints.Infinity
        )
        
        val elementHeights = mutableMapOf<Int, Int>()
        
        val pages = calculatePagesCore(
            text = text,
            startElement = startElement,
            startCarryOverText = startCarryOverText,
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
            measureConstraints = measureConstraints,
            chapterStyleMedium = chapterStyleMedium,
            chapterStyleSmall = chapterStyleSmall,
            textMeasurer = textMeasurer
        )
        
        onPagesCalculated(pages)
        layout(0, 0) {}
    }
}
