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
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.Density
import ua.acclorite.book_story.domain.reader.FontWithName
import ua.acclorite.book_story.domain.reader.ReaderFontThickness
import ua.acclorite.book_story.domain.reader.ReaderText
import ua.acclorite.book_story.domain.reader.ReaderTextAlignment
import ua.acclorite.book_story.presentation.core.components.common.StyledText
import kotlin.math.roundToInt

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
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val layoutDirection = androidx.compose.ui.platform.LocalLayoutDirection.current
    
    val contentPaddingVerticalPx = with(density) {
        (contentPadding.calculateTopPadding() + contentPadding.calculateBottomPadding()).roundToPx()
    }
    val contentPaddingHorizontalPx = with(density) {
        (contentPadding.calculateStartPadding(layoutDirection) + contentPadding.calculateEndPadding(layoutDirection)).roundToPx()
    }
    val verticalPaddingPx = with(density) {
        (verticalPadding * 2).roundToPx()
    }
    val sidePaddingPx = with(density) {
        (sidePadding * 2).roundToPx()
    }
    
    val progressBarHeightPx = if (progressBar) {
        with(density) {
            (progressBarFontSize.toDp() + progressBarPadding * 2).roundToPx()
        }
    } else 0

    // БУФЕР ШИРИНЫ: Уменьшаем ширину в измерителе на 8 пикселей. 
    // Это заставит измеритель переносить слова РАНЬШЕ, чем это сделает экран.
    val availableWidth = (screenWidth - sidePaddingPx - contentPaddingHorizontalPx - 8).coerceAtLeast(0)
    
    // БУФЕР ВЫСОТЫ: 2 пикселя для компенсации Float
    val availableHeight = (screenHeight - contentPaddingVerticalPx - verticalPaddingPx - progressBarHeightPx - 2).coerceAtLeast(0)

    val paragraphSpacingPx = with(density) { paragraphHeight.roundToPx() }

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
                paragraphHeightPx = paragraphSpacingPx,
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
    density: Density,
    elementHeights: MutableMap<Int, Int>,
    paragraphHeightPx: Int,
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
                slotId = "element_${index}_rem_${pagesCalculated}",
                density = density,
                imagesCornersRoundness = imagesCornersRoundness,
                imagesAlignment = imagesAlignment,
                imagesWidth = imagesWidth,
                imagesColorEffects = imagesColorEffects,
                chapterStyleMedium = chapterStyleMedium,
                chapterStyleSmall = chapterStyleSmall,
                textMeasurer = textMeasurer
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
                    slotId = "element_$index",
                    density = density,
                    imagesCornersRoundness = imagesCornersRoundness,
                    imagesAlignment = imagesAlignment,
                    imagesWidth = imagesWidth,
                    imagesColorEffects = imagesColorEffects,
                    chapterStyleMedium = chapterStyleMedium,
                    chapterStyleSmall = chapterStyleSmall,
                    textMeasurer = textMeasurer
                )
            }
        }

        val currentSpacing = if (currentPage.isEmpty()) 0 else paragraphHeightPx
        val totalElementHeight = elementHeight + currentSpacing

        if (currentPageHeight + totalElementHeight <= availableHeight) {
            currentPage.add(readerText)
            currentPageHeight += totalElementHeight
            remainingTextPart = null
            index++
        } else {
            // Разбиение текста
            if (readerText is ReaderText.Text) {
                val effectiveAvailableHeight = availableHeight - currentPageHeight - currentSpacing
                
                if (effectiveAvailableHeight > with(density) { fontSize.toPx() }) {
                     val textStyle = TextStyle(
                        fontFamily = fontFamily.font,
                        fontWeight = fontThickness.thickness,
                        textAlign = textAlignment.textAlignment,
                        textIndent = if (readerText.line.text.startsWith("\u200B")) TextIndent.None else TextIndent(firstLine = paragraphIndentation),
                        fontStyle = fontStyle,
                        letterSpacing = letterSpacing,
                        fontSize = fontSize,
                        lineHeight = lineHeight,
                        lineBreak = LineBreak.Paragraph,
                        textDirection = TextDirection.Content, // Синхронизируем направление
                        platformStyle = PlatformTextStyle(includeFontPadding = false),
                        lineHeightStyle = LineHeightStyle(
                            alignment = LineHeightStyle.Alignment.Center,
                            trim = LineHeightStyle.Trim.None
                        )
                    )
                    
                    val measuredText = if (highlightedReading) {
                        TextMeasurementUtils.applyHighlighting(readerText.line, highlightedReadingThickness)
                    } else {
                        readerText.line
                    }
                    
                    val layoutResult = textMeasurer.measure(
                        text = measuredText,
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
                        var splitOffset = layoutResult.getLineEnd(lastFittingLine)
                        
                        // ГАРАНТИЯ ЦЕЛОГО СЛОВА: если мы прерываемся посреди слова, откатываемся до ближайшего пробела
                        val textString = measuredText.text
                        if (splitOffset < textString.length && !textString[splitOffset].isWhitespace()) {
                            val lastSpace = textString.lastIndexOf(' ', splitOffset)
                            val lineStart = layoutResult.getLineStart(lastFittingLine)
                            if (lastSpace > lineStart) {
                                splitOffset = lastSpace + 1
                            }
                        }
                        
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
            endIndex = if (index > text.lastIndex) text.lastIndex else index - 1,
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
    slotId: String,
    density: Density,
    imagesCornersRoundness: Dp,
    imagesAlignment: ua.acclorite.book_story.domain.util.HorizontalAlignment,
    imagesWidth: Float,
    imagesColorEffects: ColorFilter?,
    chapterStyleMedium: TextStyle,
    chapterStyleSmall: TextStyle,
    textMeasurer: TextMeasurer
): Int {
    return when (readerText) {
        is ReaderText.Text -> {
            val isContinuation = readerText.line.text.startsWith("\u200B")
            val textStyle = TextStyle(
                fontFamily = fontFamily.font,
                fontWeight = fontThickness.thickness,
                textAlign = textAlignment.textAlignment,
                textIndent = if (isContinuation) TextIndent.None else TextIndent(firstLine = paragraphIndentation),
                fontStyle = fontStyle,
                letterSpacing = letterSpacing,
                fontSize = fontSize,
                lineHeight = lineHeight,
                color = fontColor,
                lineBreak = LineBreak.Paragraph,
                textDirection = TextDirection.Content,
                platformStyle = PlatformTextStyle(includeFontPadding = false),
                lineHeightStyle = LineHeightStyle(
                    alignment = LineHeightStyle.Alignment.Center,
                    trim = LineHeightStyle.Trim.None
                )
            )
            
            val measuredText = if (highlightedReading) {
                TextMeasurementUtils.applyHighlighting(readerText.line, highlightedReadingThickness)
            } else {
                readerText.line
            }
            
            val layoutResult = textMeasurer.measure(
                text = measuredText,
                style = textStyle,
                constraints = constraints
            )
            layoutResult.size.height
        }

        is ReaderText.Chapter -> {
            val placeable = measurer.subcompose(slotId) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.height(22.dp))
                    StyledText(
                        text = buildAnnotatedString { append(readerText.title) },
                        style = (if (!readerText.nested) chapterStyleMedium else chapterStyleSmall)
                            .copy(
                                color = fontColor,
                                textAlign = textAlignment.textAlignment,
                                textDirection = TextDirection.Content,
                                platformStyle = PlatformTextStyle(includeFontPadding = false),
                                lineHeightStyle = LineHeightStyle(
                                    alignment = LineHeightStyle.Alignment.Center,
                                    trim = LineHeightStyle.Trim.None
                                )
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
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val layoutDirection = androidx.compose.ui.platform.LocalLayoutDirection.current
    
    val contentPaddingVerticalPx = with(density) {
        (contentPadding.calculateTopPadding() + contentPadding.calculateBottomPadding()).roundToPx()
    }
    val contentPaddingHorizontalPx = with(density) {
        (contentPadding.calculateStartPadding(layoutDirection) + contentPadding.calculateEndPadding(layoutDirection)).roundToPx()
    }
    val verticalPaddingPx = with(density) {
        (verticalPadding * 2).roundToPx()
    }
    val sidePaddingPx = with(density) {
        (sidePadding * 2).roundToPx()
    }
    
    val progressBarHeightPx = if (progressBar) {
        with(density) {
            (progressBarFontSize.toDp() + progressBarPadding * 2).roundToPx()
        }
    } else 0

    // БУФЕР ШИРИНЫ (синхронно с основным измерителем)
    val availableWidth = (screenWidth - sidePaddingPx - contentPaddingHorizontalPx - 20).coerceAtLeast(0)
    val availableHeight = (screenHeight - contentPaddingVerticalPx - verticalPaddingPx - progressBarHeightPx - 2).coerceAtLeast(0)

    val paragraphSpacingPx = with(density) { paragraphHeight.roundToPx() }

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
            paragraphHeightPx = paragraphSpacingPx,
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
