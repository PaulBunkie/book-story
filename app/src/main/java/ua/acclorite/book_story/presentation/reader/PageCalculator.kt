/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.presentation.reader

import android.graphics.Paint
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.toFontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.layout.PaddingValues
import kotlin.math.roundToInt
import android.graphics.Typeface
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ua.acclorite.book_story.domain.reader.ReaderText
import ua.acclorite.book_story.domain.reader.ReaderTextAlignment
import ua.acclorite.book_story.domain.reader.ReaderFontThickness
import ua.acclorite.book_story.domain.reader.FontWithName
import kotlin.math.ceil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log
import ua.acclorite.book_story.presentation.reader.TextMeasurementUtils

data class Page(
    val content: List<ReaderText>,
    val startIndex: Int,
    val endIndex: Int
)

class PageCalculator {
    
    suspend fun calculatePages(
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
        density: Float
    ): List<Page> = withContext(Dispatchers.Default) {
        Log.d("PAGE_CALCULATOR", "=== Starting page calculation with paragraphs ===")
        Log.d("PAGE_CALCULATOR", "Text items count: ${text.size}")
        Log.d("PAGE_CALCULATOR", "Screen: ${screenWidth}x${screenHeight}")
        
        // Учитываем все отступы: contentPadding + verticalPadding + sidePadding
        val contentPaddingPx = contentPadding.calculateTopPadding().value + contentPadding.calculateBottomPadding().value
        val verticalPaddingPx = verticalPadding.value * 2 * density
        val sidePaddingPx = sidePadding.value * 2 * density
        
        val availableWidth = (screenWidth - sidePaddingPx).toInt()
        // Добавляем коэффициент безопасности для учета Spacer между элементами
        val safetyMargin = 1.00f // 100% от доступной высоты
        val availableHeight = ((screenHeight - contentPaddingPx - verticalPaddingPx) * safetyMargin).toInt()
        
        Log.d("PAGE_CALCULATOR", "Available space: ${availableWidth}x${availableHeight}")
        Log.d("PAGE_CALCULATOR", "Screen dimensions: ${screenWidth}x${screenHeight}")
        Log.d("PAGE_CALCULATOR", "Side padding: ${sidePadding.value}dp = ${sidePaddingPx}px")
        Log.d("PAGE_CALCULATOR", "Content padding: ${contentPaddingPx}px")
        Log.d("PAGE_CALCULATOR", "Vertical padding: ${verticalPaddingPx}px")
        Log.d("PAGE_CALCULATOR", "Density: ${density}")
        Log.d("PAGE_CALCULATOR_DEBUG", "CALCULATOR availableHeight = ${availableHeight}px")
        Log.d("PAGE_CALCULATOR_AVAILABLE_HEIGHT", "CALCULATOR availableHeight = ${availableHeight}px")
        
        // Расчеты завершены
        
        Log.d("PAGE_CALCULATOR", "Creating TextPaint...")
        Log.d("PAGE_CALCULATOR", "Font parameters: fontSize=${fontSize.value}sp, lineHeight=${lineHeight.value}sp, letterSpacing=${letterSpacing.value}em")
        Log.d("PAGE_CALCULATOR", "Density parameter: ${density}")
        val textPaint = TextMeasurementUtils.createTextPaint(
            fontSize = fontSize,
            fontFamily = fontFamily,
            fontThickness = fontThickness,
            fontStyle = fontStyle,
            textAlignment = textAlignment,
            letterSpacing = letterSpacing,
            density = density
        )
        Log.d("PAGE_CALCULATOR", "TextPaint created successfully")

        Log.d("PAGE_CALCULATOR", "Calculating pages with paragraph breaks...")
        val pages = calculatePagesWithParagraphBreaks(
            text = text,
            textPaint = textPaint,
            availableWidth = availableWidth,
            availableHeight = availableHeight,
            fontSize = fontSize,
            lineHeight = lineHeight,
            paragraphIndentation = paragraphIndentation,
            paragraphHeight = paragraphHeight,
            density = density,
            textAlignment = textAlignment
        )
        
        Log.d("PAGE_CALCULATOR", "Pages created: ${pages.size}")
        
        // Ограничиваем количество страниц для предотвращения зависания
        val maxPages = 100
        if (pages.size > maxPages) {
            Log.w("PAGE_CALCULATOR", "Too many pages (${pages.size}), limiting to $maxPages")
            pages.take(maxPages)
        } else {
            pages
        }
    }
    
    private fun calculatePagesWithParagraphBreaks(
        text: List<ReaderText>,
        textPaint: TextPaint,
        availableWidth: Int,
        availableHeight: Int,
        fontSize: TextUnit,
        lineHeight: TextUnit,
        paragraphIndentation: TextUnit,
        paragraphHeight: Dp,
        density: Float,
        textAlignment: ReaderTextAlignment
    ): List<Page> {
        val pages = mutableListOf<Page>()
        val currentPageContent = mutableListOf<ReaderText>()
        var pageIndex = 0
        
        for ((originalIndex, readerText) in text.withIndex()) {
            Log.d("PAGE_CALCULATOR_DEBUG", "Processing element $originalIndex: ${readerText::class.simpleName}")
            when (readerText) {
                is ReaderText.Text -> {
                    // Проверяем, помещается ли элемент на текущую страницу
                    val testPageContent = currentPageContent + readerText
                    val testPageHeight = TextMeasurementUtils.calculatePageHeight(
                        pageContent = testPageContent,
                        textPaint = textPaint,
                        availableWidth = availableWidth,
                        fontSize = fontSize,
                        lineHeight = lineHeight,
                        paragraphIndentation = paragraphIndentation,
                        paragraphHeight = paragraphHeight,
                        density = density,
                        textAlignment = textAlignment
                    )
                    
                    Log.d("PAGE_CALCULATOR_DEBUG", "Page $pageIndex : Element $originalIndex : Test height ${testPageHeight}px, available ${availableHeight}px")
                    
                    if (testPageHeight <= availableHeight) {
                        // Элемент помещается - добавляем его
                        Log.d("PAGE_CALCULATOR_DEBUG", "Page $pageIndex : Element $originalIndex : FITS COMPLETELY! Text: ${readerText.line.text.take(30)}...")
                        currentPageContent.add(readerText)
                    } else {
                        // Элемент не помещается - нужно разбить или перенести
                        val singleLineHeight = (fontSize.value * density * TextMeasurementUtils.getLineSpacingMultiplier(lineHeight, fontSize)).toInt()
                        val currentPageHeight = TextMeasurementUtils.calculatePageHeight(
                            pageContent = currentPageContent,
                            textPaint = textPaint,
                            availableWidth = availableWidth,
                            fontSize = fontSize,
                            lineHeight = lineHeight,
                            paragraphIndentation = paragraphIndentation,
                            paragraphHeight = paragraphHeight,
                            density = density,
                            textAlignment = textAlignment
                        )
                        val remainingSpace = availableHeight - currentPageHeight
                        
                        Log.d("PAGE_CALCULATOR_DEBUG", "Page $pageIndex : Element $originalIndex : NO SPACE! Current height: ${currentPageHeight}px, remaining: ${remainingSpace}px")
                        
                        // Если есть место хотя бы для одной строки - разбиваем параграф
                        if (remainingSpace >= singleLineHeight * 0.3f) {
                            Log.d("PAGE_CALCULATOR_DEBUG", "Page $pageIndex : BREAKING paragraph $originalIndex")
                            Log.d("PAGE_CALCULATOR_DEBUG", "paragraphHeight=${paragraphHeight.value}dp, density=$density, paragraphSpacingPx=${paragraphHeight.value.toInt()}")
                            
                            val brokenParts = breakParagraphForCurrentPage(
                                paragraph = readerText,
                                textPaint = textPaint,
                                availableWidth = availableWidth,
                                remainingSpace = remainingSpace,
                                fontSize = fontSize,
                                lineHeight = lineHeight,
                                paragraphIndentation = paragraphIndentation,
                                paragraphSpacingPx = paragraphHeight.value.toInt(),
                                density = density
                            )
                            
                            // Добавляем первую часть на текущую страницу
                            if (brokenParts.firstPart != null) {
                                currentPageContent.add(brokenParts.firstPart.readerText)
                            }
                            
                            // Сохраняем текущую страницу
                            val finalPageHeight = TextMeasurementUtils.calculatePageHeight(
                                pageContent = currentPageContent,
                                textPaint = textPaint,
                                availableWidth = availableWidth,
                                fontSize = fontSize,
                                lineHeight = lineHeight,
                                paragraphIndentation = paragraphIndentation,
                                paragraphHeight = paragraphHeight,
                                density = density,
                                textAlignment = textAlignment
                            )
                            Log.d("PAGE_CALCULATOR_HEIGHT", "Page $pageIndex : CALCULATOR HEIGHT = ${finalPageHeight}px")
                            
                            pages.add(
                                Page(
                                    content = currentPageContent.toList(),
                                    startIndex = pageIndex,
                                    endIndex = pageIndex
                                )
                            )
                            pageIndex++
                            currentPageContent.clear()
                            
                            // Добавляем остаток на новую страницу
                            if (brokenParts.remainingPart != null) {
                                currentPageContent.add(brokenParts.remainingPart.readerText)
                            }
                        } else {
                            // Нет места даже для разбиения - переносим на новую страницу
                            Log.d("PAGE_CALCULATOR_DEBUG", "Page $pageIndex : Element $originalIndex : NO SPACE FOR BREAKING! Moving to NEW page")
                            
                            // Сохраняем текущую страницу, если она не пустая
                            if (currentPageContent.isNotEmpty()) {
                                val finalPageHeight = TextMeasurementUtils.calculatePageHeight(
                                    pageContent = currentPageContent,
                                    textPaint = textPaint,
                                    availableWidth = availableWidth,
                                    fontSize = fontSize,
                                    lineHeight = lineHeight,
                                    paragraphIndentation = paragraphIndentation,
                                    paragraphHeight = paragraphHeight,
                                    density = density,
                                    textAlignment = textAlignment
                                )
                                Log.d("PAGE_CALCULATOR_HEIGHT", "Page $pageIndex : CALCULATOR HEIGHT = ${finalPageHeight}px")
                                
                                pages.add(
                                    Page(
                                        content = currentPageContent.toList(),
                                        startIndex = pageIndex,
                                        endIndex = pageIndex
                                    )
                                )
                                pageIndex++
                                currentPageContent.clear()
                            }
                            
                            // Добавляем элемент на новую страницу
                            currentPageContent.add(readerText)
                        }
                    }
                }
                
                is ReaderText.Chapter -> {
                    Log.d("PAGE_CALCULATOR_DEBUG", "=== CHAPTER DETECTED! Element $originalIndex ===")
                    
                    // Глава всегда начинает новую страницу
                    // Сохраняем текущую страницу, если она не пустая
                    if (currentPageContent.isNotEmpty()) {
                        val finalPageHeight = TextMeasurementUtils.calculatePageHeight(
                            pageContent = currentPageContent,
                            textPaint = textPaint,
                            availableWidth = availableWidth,
                            fontSize = fontSize,
                            lineHeight = lineHeight,
                            paragraphIndentation = paragraphIndentation,
                            paragraphHeight = paragraphHeight,
                            density = density,
                            textAlignment = textAlignment
                        )
                        Log.d("PAGE_CALCULATOR_HEIGHT", "Page $pageIndex : CALCULATOR HEIGHT = ${finalPageHeight}px")
                        
                        pages.add(
                            Page(
                                content = currentPageContent.toList(),
                                startIndex = pageIndex,
                                endIndex = pageIndex
                            )
                        )
                        pageIndex++
                        currentPageContent.clear()
                    }
                    
                    // Добавляем главу на новую страницу
                    currentPageContent.add(readerText)
                }
                
                is ReaderText.Separator -> {
                    Log.d("PAGE_CALCULATOR_DEBUG", "=== SEPARATOR DETECTED! Element $originalIndex ===")
                    
                    // Разделитель всегда начинает новую страницу
                    // Сохраняем текущую страницу, если она не пустая
                    if (currentPageContent.isNotEmpty()) {
                        val finalPageHeight = TextMeasurementUtils.calculatePageHeight(
                            pageContent = currentPageContent,
                            textPaint = textPaint,
                            availableWidth = availableWidth,
                            fontSize = fontSize,
                            lineHeight = lineHeight,
                            paragraphIndentation = paragraphIndentation,
                            paragraphHeight = paragraphHeight,
                            density = density,
                            textAlignment = textAlignment
                        )
                        Log.d("PAGE_CALCULATOR_HEIGHT", "Page $pageIndex : CALCULATOR HEIGHT = ${finalPageHeight}px")
                        
                        pages.add(
                            Page(
                                content = currentPageContent.toList(),
                                startIndex = pageIndex,
                                endIndex = pageIndex
                            )
                        )
                        pageIndex++
                        currentPageContent.clear()
                    }
                    
                    // Добавляем разделитель на новую страницу
                    currentPageContent.add(readerText)
                }
                
                is ReaderText.Image -> {
                    // Проверяем, помещается ли изображение на текущую страницу
                    val testPageContent = currentPageContent + readerText
                    val testPageHeight = TextMeasurementUtils.calculatePageHeight(
                        pageContent = testPageContent,
                        textPaint = textPaint,
                        availableWidth = availableWidth,
                        fontSize = fontSize,
                        lineHeight = lineHeight,
                        paragraphIndentation = paragraphIndentation,
                        paragraphHeight = paragraphHeight,
                        density = density,
                        textAlignment = textAlignment
                    )
                    
                    Log.d("PAGE_CALCULATOR_DEBUG", "Page $pageIndex : Element $originalIndex : Test height ${testPageHeight}px, available ${availableHeight}px")
                    
                    if (testPageHeight <= availableHeight) {
                        // Изображение помещается - добавляем его
                        Log.d("PAGE_CALCULATOR_DEBUG", "Page $pageIndex : Element $originalIndex : Image FITS COMPLETELY!")
                        currentPageContent.add(readerText)
                    } else {
                        // Изображение не помещается - переносим на новую страницу
                        Log.d("PAGE_CALCULATOR_DEBUG", "Page $pageIndex : Element $originalIndex : Image DOESN'T FIT! Moving to NEW page")
                        
                        // Сохраняем текущую страницу, если она не пустая
                        if (currentPageContent.isNotEmpty()) {
                            val finalPageHeight = TextMeasurementUtils.calculatePageHeight(
                                pageContent = currentPageContent,
                                textPaint = textPaint,
                                availableWidth = availableWidth,
                                fontSize = fontSize,
                                lineHeight = lineHeight,
                                paragraphIndentation = paragraphIndentation,
                                paragraphHeight = paragraphHeight,
                                density = density,
                                textAlignment = textAlignment
                            )
                            Log.d("PAGE_CALCULATOR_HEIGHT", "Page $pageIndex : CALCULATOR HEIGHT = ${finalPageHeight}px")
                            
                            pages.add(
                                Page(
                                    content = currentPageContent.toList(),
                                    startIndex = pageIndex,
                                    endIndex = pageIndex
                                )
                            )
                            pageIndex++
                            currentPageContent.clear()
                        }
                        
                        // Добавляем изображение на новую страницу
                        currentPageContent.add(readerText)
                    }
                }
            }
        }
        
        // Добавляем последнюю страницу, если есть содержимое
        if (currentPageContent.isNotEmpty()) {
            val finalPageHeight = TextMeasurementUtils.calculatePageHeight(
                pageContent = currentPageContent,
                textPaint = textPaint,
                availableWidth = availableWidth,
                fontSize = fontSize,
                lineHeight = lineHeight,
                paragraphIndentation = paragraphIndentation,
                paragraphHeight = paragraphHeight,
                density = density,
                textAlignment = ReaderTextAlignment.START
            )
            Log.d("PAGE_CALCULATOR_HEIGHT", "Page $pageIndex : CALCULATOR HEIGHT = ${finalPageHeight}px")
            
            pages.add(
                Page(
                    content = currentPageContent.toList(),
                    startIndex = pageIndex,
                    endIndex = pageIndex
                )
            )
        }
        
        return pages
    }
    
    
    
    
    
    
    private fun breakParagraphForCurrentPage(
        paragraph: ReaderText.Text,
        textPaint: TextPaint,
        availableWidth: Int,
        remainingSpace: Int,
        fontSize: TextUnit,
        lineHeight: TextUnit,
        paragraphIndentation: TextUnit,
        paragraphSpacingPx: Int,
        density: Float
    ): BrokenParagraphParts {
        val text = paragraph.line.text
        val staticLayout = StaticLayout.Builder
            .obtain(text, 0, text.length, textPaint, availableWidth)
            .setAlignment(TextMeasurementUtils.getAlignment(ReaderTextAlignment.START))
            .setLineSpacing(0f, TextMeasurementUtils.getLineSpacingMultiplier(lineHeight, fontSize))
            .setIncludePad(false)
            .build()
        
        val totalLines = staticLayout.lineCount
        
        // ПРЯМОЙ РАСЧЕТ: сколько строк помещается без ебучих циклов!
        val singleLineHeight = (fontSize.value * density * TextMeasurementUtils.getLineSpacingMultiplier(lineHeight, fontSize)).toInt()
        val maxLinesForCurrentPage = ((remainingSpace - paragraphSpacingPx).toDouble() / singleLineHeight).roundToInt()
        
        Log.d("PAGE_CALCULATOR_DEBUG", "BREAKING: totalLines=$totalLines, singleLineHeight=$singleLineHeight, remainingSpace=$remainingSpace, paragraphSpacingPx=$paragraphSpacingPx, maxLinesForCurrentPage=$maxLinesForCurrentPage")
        
        // Если ничего не помещается, возвращаем null для первой части
        if (maxLinesForCurrentPage == 0) {
            return BrokenParagraphParts(
                firstPart = null,
                remainingPart = BrokenParagraphPart(
                    readerText = paragraph,
                    height = staticLayout.height
                )
            )
        }
        
        // Создаем первую часть (то, что помещается на текущую страницу) - БЕЗ ЦИКЛОВ!
        Log.d("PAGE_CALCULATOR_DEBUG", "BREAKING: totalLines=$totalLines, maxLinesForCurrentPage=$maxLinesForCurrentPage")
        
        val actualLines = minOf(maxLinesForCurrentPage, totalLines)
        val firstPartEndChar = if (actualLines > 0) staticLayout.getLineEnd(actualLines - 1) else 0
        val firstPartText = text.substring(0, firstPartEndChar)
        
        Log.d("PAGE_CALCULATOR_DEBUG", "BREAKING: First part text (${firstPartText.length} chars): '${firstPartText.take(50)}...'")
        
        val firstPartLayout = StaticLayout.Builder
            .obtain(firstPartText, 0, firstPartText.length, textPaint, availableWidth)
            .setAlignment(TextMeasurementUtils.getAlignment(ReaderTextAlignment.START))
            .setLineSpacing(0f, TextMeasurementUtils.getLineSpacingMultiplier(lineHeight, fontSize))
            .setIncludePad(false)
            .build()
        
        val firstPart = BrokenParagraphPart(
            readerText = ReaderText.Text(
                line = androidx.compose.ui.text.AnnotatedString(firstPartText)
            ),
            height = firstPartLayout.height,
            isContinuation = false // Это первая часть
        )
        
        // Создаем остаток (то, что идет на следующую страницу) - БЕЗ ЦИКЛОВ!
        Log.d("PAGE_CALCULATOR_DEBUG", "BREAKING: Creating remaining part from lines $actualLines to $totalLines")
        val remainingPartStartChar = if (actualLines < totalLines) staticLayout.getLineStart(actualLines) else text.length
        val remainingPartText = if (remainingPartStartChar < text.length) text.substring(remainingPartStartChar) else ""
        
        Log.d("PAGE_CALCULATOR_DEBUG", "BREAKING: Remaining part text (${remainingPartText.length} chars): '${remainingPartText.take(50)}...'")
        
        val remainingPart = if (remainingPartText.length > 0) {
            Log.d("PAGE_CALCULATOR_DEBUG", "BREAKING: Creating remainingPart with text length ${remainingPartText.length}")
            // Убираем отступ первой строки для остатка
            val trimmedText = remainingPartText.trimStart()
            val remainingPartLayout = StaticLayout.Builder
                .obtain(trimmedText, 0, trimmedText.length, textPaint, availableWidth)
                .setAlignment(TextMeasurementUtils.getAlignment(ReaderTextAlignment.START))
                .setLineSpacing(0f, TextMeasurementUtils.getLineSpacingMultiplier(lineHeight, fontSize))
                .setIncludePad(false)
                .build()
            
            BrokenParagraphPart(
                readerText = ReaderText.Text(
                    line = androidx.compose.ui.text.AnnotatedString(trimmedText)
                ),
                height = remainingPartLayout.height,
                isContinuation = true // Помечаем как продолжение параграфа
            )
        } else {
            Log.d("PAGE_CALCULATOR_DEBUG", "BREAKING: remainingPart is NULL - no remaining text")
            null
        }
        
        return BrokenParagraphParts(
            firstPart = firstPart,
            remainingPart = remainingPart
        )
    }
    
    private fun breakParagraph(
        paragraph: ReaderText.Text,
        textPaint: TextPaint,
        availableWidth: Int,
        availableHeight: Int,
        remainingSpace: Int,
        fontSize: TextUnit,
        lineHeight: TextUnit,
        paragraphIndentation: TextUnit,
        paragraphHeight: Dp,
        density: Float
    ): List<BrokenParagraphPart> {
        val text = paragraph.line.text
        val staticLayout = StaticLayout.Builder
            .obtain(text, 0, text.length, textPaint, availableWidth)
            .setAlignment(TextMeasurementUtils.getAlignment(ReaderTextAlignment.START)) // Добавляем выравнивание
            .setLineSpacing(0f, TextMeasurementUtils.getLineSpacingMultiplier(lineHeight, fontSize))
            .setIncludePad(false)
            .build()
        
        val brokenParts = mutableListOf<BrokenParagraphPart>()
        val totalLines = staticLayout.lineCount
        val paragraphSpacingPx = paragraphHeight.value.toInt() // paragraphHeight уже умножен на density в ReaderScreen.kt
        Log.d("PAGE_CALCULATOR_DEBUG", "paragraphHeight=${paragraphHeight.value}dp, density=$density, paragraphSpacingPx=$paragraphSpacingPx")
        
        var currentLine = 0
        var isFirstPart = true
        
        while (currentLine < totalLines) {
            // Для первой части используем оставшееся место на текущей странице
            // Для последующих частей используем полную высоту страницы
            val effectiveAvailableHeight = if (currentLine == 0) remainingSpace else availableHeight
            
            // Используем StaticLayout для точного расчета высоты
            var endLine = currentLine + 1
            var bestEndLine = endLine
            
            while (endLine <= totalLines) {
                val partText = StringBuilder()
                for (lineIndex in currentLine until endLine) {
                    val startChar = staticLayout.getLineStart(lineIndex)
                    val endChar = staticLayout.getLineEnd(lineIndex)
                    partText.append(text.substring(startChar, endChar))
                }
                val partLayout = StaticLayout.Builder
                    .obtain(partText.toString(), 0, partText.length, textPaint, availableWidth)
                    .setAlignment(TextMeasurementUtils.getAlignment(ReaderTextAlignment.START))
                    .setLineSpacing(0f, TextMeasurementUtils.getLineSpacingMultiplier(lineHeight, fontSize))
                    .setIncludePad(false)
                    .build()
                
                val partHeight = partLayout.height
                
                if (partHeight <= effectiveAvailableHeight) {
                    bestEndLine = endLine
                    endLine++
                } else {
                    break
                }
            }
            
            // Извлекаем текст для лучшей части
            val partText = StringBuilder()
            for (lineIndex in currentLine until bestEndLine) {
                val startChar = staticLayout.getLineStart(lineIndex)
                val endChar = staticLayout.getLineEnd(lineIndex)
                partText.append(text.substring(startChar, endChar))
            }
            val partLayout = StaticLayout.Builder
                .obtain(partText.toString(), 0, partText.length, textPaint, availableWidth)
                .setAlignment(TextMeasurementUtils.getAlignment(ReaderTextAlignment.START))
                .setLineSpacing(0f, TextMeasurementUtils.getLineSpacingMultiplier(lineHeight, fontSize))
                .setIncludePad(false)
                .build()
            
            val partHeight = partLayout.height
            
            // Создаем новый ReaderText.Text для этой части
            val partReaderText = ReaderText.Text(
                line = androidx.compose.ui.text.AnnotatedString(partText.toString())
            )
            
            brokenParts.add(
                BrokenParagraphPart(
                    readerText = partReaderText,
                    height = partHeight
                )
            )
            
            currentLine = bestEndLine
            isFirstPart = false
        }
        
        return brokenParts
    }
    
    private data class BrokenParagraphPart(
        val readerText: ReaderText.Text,
        val height: Int,
        val isContinuation: Boolean = false // Продолжение разорванного параграфа
    )
    
    private data class BrokenParagraphParts(
        val firstPart: BrokenParagraphPart?,
        val remainingPart: BrokenParagraphPart?
    )
    
}
