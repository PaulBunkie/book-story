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
        
        // Расчеты завершены
        
        Log.d("PAGE_CALCULATOR", "Creating TextPaint...")
        Log.d("PAGE_CALCULATOR", "Font parameters: fontSize=${fontSize.value}sp, lineHeight=${lineHeight.value}sp, letterSpacing=${letterSpacing.value}em")
        Log.d("PAGE_CALCULATOR", "Density parameter: ${density}")
        val textPaint = createTextPaint(
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
            density = density
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
        density: Float
    ): List<Page> {
        val pages = mutableListOf<Page>()
        val currentPageContent = mutableListOf<ReaderText>()
        var currentPageHeight = 0
        var pageIndex = 0
        val paragraphSpacingPx = (paragraphHeight.value * density).toInt()
        
        for ((originalIndex, readerText) in text.withIndex()) {
            Log.d("PAGE_CALCULATOR_DEBUG", "Processing element $originalIndex: ${readerText::class.simpleName}")
            when (readerText) {
                is ReaderText.Text -> {
                    // Рассчитываем высоту абзаца
                    val paragraphHeightPx = calculateParagraphHeight(
                        text = readerText.line.text,
                        textPaint = textPaint,
                        availableWidth = availableWidth,
                        fontSize = fontSize,
                        lineHeight = lineHeight,
                        paragraphIndentation = paragraphIndentation,
                        density = density
                    )
                    
                    Log.d("PAGE_CALCULATOR_DEBUG", "Page $pageIndex : Element $originalIndex : Position ${currentPageContent.size} : Height ${paragraphHeightPx}px : Remaining ${availableHeight - currentPageHeight}px")
                    
                    // Учитываем интервалы между абзацами (если это не первый элемент на странице)
                    val totalHeight = if (currentPageContent.isNotEmpty()) {
                        paragraphHeightPx + paragraphSpacingPx // высота текста + интервал
                    } else {
                        paragraphHeightPx // только высота текста для первого элемента
                    }
                    
                    // Если абзац помещается на текущую страницу целиком
                    if (currentPageHeight + totalHeight <= availableHeight) {
                        Log.d("PAGE_CALCULATOR_DEBUG", "Page $pageIndex : Element $originalIndex : FITS! Text: ${readerText.line.text.take(30)}...")
                        currentPageContent.add(readerText)
                        currentPageHeight += totalHeight
                    } else {
                        Log.d("PAGE_CALCULATOR_DEBUG", "Page $pageIndex : Element $originalIndex : DOESN'T FIT! Need ${totalHeight}px, have ${availableHeight - currentPageHeight}px")
                        
                        // Проверяем, можем ли разбить параграф на текущей странице
                        val currentRemainingSpace = availableHeight - currentPageHeight
                        val singleLineHeight = (fontSize.value * density * getLineSpacingMultiplier(lineHeight, fontSize)).toInt()
                        
                        Log.d("PAGE_CALCULATOR_DEBUG", "Page $pageIndex : Breaking check: paragraphHeight=${paragraphHeightPx}px, currentRemainingSpace=${currentRemainingSpace}px, singleLineHeight=${singleLineHeight}px")
                        
                        // Если в оставшемся месте может поместиться хотя бы одна строка
                        if (currentRemainingSpace >= singleLineHeight) {
                            Log.d("PAGE_CALCULATOR_DEBUG", "Page $pageIndex : BREAKING paragraph $originalIndex : Text: ${readerText.line.text.take(50)}...")
                            Log.d("PAGE_CALCULATOR_DEBUG", "Page $pageIndex : Current remaining space: ${currentRemainingSpace}px, paragraph height: ${paragraphHeightPx}px")
                            
                            // Разбиваем параграф: часть на текущую страницу, остаток на следующую
                            val brokenParts = breakParagraphForCurrentPage(
                                paragraph = readerText,
                                textPaint = textPaint,
                                availableWidth = availableWidth,
                                remainingSpace = currentRemainingSpace,
                                fontSize = fontSize,
                                lineHeight = lineHeight,
                                paragraphIndentation = paragraphIndentation,
                                paragraphSpacingPx = paragraphSpacingPx,
                                density = density
                            )
                            
                            // Добавляем первую часть на текущую страницу
                            if (brokenParts.firstPart != null) {
                                Log.d("PAGE_CALCULATOR_DEBUG", "Page $pageIndex : Adding first part to CURRENT page: ${brokenParts.firstPart.readerText.line.text.take(30)}...")
                                currentPageContent.add(brokenParts.firstPart.readerText)
                                currentPageHeight += brokenParts.firstPart.height + paragraphSpacingPx
                            }
                            
                            // Сохраняем текущую страницу
                            Log.d("PAGE_CALCULATOR_DEBUG", "Page $pageIndex : SAVING with ${currentPageContent.size} elements")
                            pages.add(
                                Page(
                                    content = currentPageContent.toList(),
                                    startIndex = pageIndex,
                                    endIndex = pageIndex
                                )
                            )
                            pageIndex++
                            currentPageContent.clear()
                            currentPageHeight = 0
                            
                            // Добавляем остаток на новую страницу
                            if (brokenParts.remainingPart != null) {
                                Log.d("PAGE_CALCULATOR_DEBUG", "Page $pageIndex : Adding remaining part to NEW page: ${brokenParts.remainingPart.readerText.line.text.take(30)}...")
                                currentPageContent.add(brokenParts.remainingPart.readerText)
                                currentPageHeight = brokenParts.remainingPart.height
                            }
                        } else {
                            // Недостаточно места даже для одной строки, переносим абзац целиком на новую страницу
                            // Если страница не пустая, сохраняем её
                            if (currentPageContent.isNotEmpty()) {
                                Log.d("PAGE_CALCULATOR_DEBUG", "Page $pageIndex : SAVING with ${currentPageContent.size} elements")
                                pages.add(
                                    Page(
                                        content = currentPageContent.toList(),
                                        startIndex = pageIndex,
                                        endIndex = pageIndex
                                    )
                                )
                                pageIndex++
                                currentPageContent.clear()
                                currentPageHeight = 0
                            }
                            
                            // Абзац помещается на новую страницу
                            Log.d("PAGE_CALCULATOR_DEBUG", "Page $pageIndex : Element $originalIndex FITS on NEW page! Text: ${readerText.line.text.take(30)}...")
                            currentPageContent.add(readerText)
                            currentPageHeight = paragraphHeightPx
                        }
                    }
                }
                
                is ReaderText.Chapter -> {
                    Log.d("PAGE_CALCULATOR_DEBUG", "=== CHAPTER DETECTED! Element $originalIndex ===")
                    Log.d("PAGE_CALCULATOR_DEBUG", "Page $pageIndex : CURRENT PAGE HAS ${currentPageContent.size} ELEMENTS")
                    Log.d("PAGE_CALCULATOR_DEBUG", "Page $pageIndex : CURRENT PAGE HEIGHT: ${currentPageHeight}px")
                    
                    val chapterHeight = calculateChapterHeight(
                        title = readerText.title,
                        textPaint = textPaint,
                        availableWidth = availableWidth,
                        fontSize = fontSize,
                        lineHeight = lineHeight,
                        density = density
                    )
                    
                    Log.d("PAGE_CALCULATOR_DEBUG", "Page $pageIndex : CHAPTER HEIGHT: ${chapterHeight}px")
                    
                    // Глава всегда начинает новую страницу (как разделитель)
                    // Сохраняем текущую страницу, если она не пустая
                    if (currentPageContent.isNotEmpty()) {
                        Log.d("PAGE_CALCULATOR_DEBUG", "Page $pageIndex : SAVING CURRENT PAGE WITH ${currentPageContent.size} ELEMENTS BEFORE CHAPTER")
                        pages.add(
                            Page(
                                content = currentPageContent.toList(),
                                startIndex = pageIndex,
                                endIndex = pageIndex
                            )
                        )
                        Log.d("PAGE_CALCULATOR_DEBUG", "Page $pageIndex : PAGE SAVED! Moving to page ${pageIndex + 1}")
                        pageIndex++
                        currentPageContent.clear()
                        currentPageHeight = 0
            } else {
                        Log.d("PAGE_CALCULATOR_DEBUG", "Page $pageIndex : CURRENT PAGE IS EMPTY, NO NEED TO SAVE")
                    }
                    
                    // Добавляем главу на новую страницу
                    Log.d("PAGE_CALCULATOR_DEBUG", "Page $pageIndex : ADDING CHAPTER TO NEW PAGE")
                    Log.d("PAGE_CALCULATOR_DEBUG", "Page $pageIndex : CHAPTER ADDED! NEW PAGE HEIGHT: ${chapterHeight}px")
                    currentPageContent.add(readerText)
                    currentPageHeight = chapterHeight
                    Log.d("PAGE_CALCULATOR_DEBUG", "=== CHAPTER PROCESSING COMPLETE ===")
                }
                
                is ReaderText.Separator -> {
                    Log.d("PAGE_CALCULATOR_DEBUG", "=== SEPARATOR DETECTED! Element $originalIndex ===")
                    Log.d("PAGE_CALCULATOR_DEBUG", "Page $pageIndex : CURRENT PAGE HAS ${currentPageContent.size} ELEMENTS")
                    Log.d("PAGE_CALCULATOR_DEBUG", "Page $pageIndex : CURRENT PAGE HEIGHT: ${currentPageHeight}px")
                    
                    val separatorHeight = calculateSeparatorHeight(
                        textPaint = textPaint,
                        availableWidth = availableWidth,
                        fontSize = fontSize,
                        lineHeight = lineHeight,
                        density = density
                    )
                    
                    Log.d("PAGE_CALCULATOR_DEBUG", "Page $pageIndex : SEPARATOR HEIGHT: ${separatorHeight}px")
                    
                    // Разделитель всегда начинает новую страницу
                    // Сохраняем текущую страницу, если она не пустая
                    if (currentPageContent.isNotEmpty()) {
                        Log.d("PAGE_CALCULATOR_DEBUG", "Page $pageIndex : SAVING CURRENT PAGE WITH ${currentPageContent.size} ELEMENTS BEFORE SEPARATOR")
            pages.add(
                Page(
                                content = currentPageContent.toList(),
                    startIndex = pageIndex,
                    endIndex = pageIndex
                )
            )
                        Log.d("PAGE_CALCULATOR_DEBUG", "Page $pageIndex : PAGE SAVED! Moving to page ${pageIndex + 1}")
                        pageIndex++
                        currentPageContent.clear()
                        currentPageHeight = 0
                    } else {
                        Log.d("PAGE_CALCULATOR_DEBUG", "Page $pageIndex : CURRENT PAGE IS EMPTY, NO NEED TO SAVE")
                    }
                    
                    // Добавляем разделитель на новую страницу
                    Log.d("PAGE_CALCULATOR_DEBUG", "Page $pageIndex : ADDING SEPARATOR TO NEW PAGE")
                    Log.d("PAGE_CALCULATOR_DEBUG", "Page $pageIndex : SEPARATOR ADDED! NEW PAGE HEIGHT: ${separatorHeight}px")
                    currentPageContent.add(readerText)
                    currentPageHeight = separatorHeight
                    Log.d("PAGE_CALCULATOR_DEBUG", "=== SEPARATOR PROCESSING COMPLETE ===")
                }
                
                is ReaderText.Image -> {
                    val imageHeight = 200 // Фиксированная высота для изображений
                    
                    // Учитываем интервалы между элементами
                    val totalHeight = if (currentPageContent.isNotEmpty()) {
                        imageHeight + paragraphSpacingPx
                    } else {
                        imageHeight
                    }
                    
                    if (currentPageHeight + totalHeight <= availableHeight) {
                        currentPageContent.add(readerText)
                        currentPageHeight += totalHeight
                    } else {
                        // Сохраняем текущую страницу
                        if (currentPageContent.isNotEmpty()) {
                            pages.add(
                                Page(
                                    content = currentPageContent.toList(),
                                    startIndex = pageIndex,
                                    endIndex = pageIndex
                                )
                            )
            pageIndex++
                            currentPageContent.clear()
                            currentPageHeight = 0
                        }
                        
                        // Добавляем изображение на новую страницу
                        currentPageContent.add(readerText)
                        currentPageHeight = imageHeight
                    }
                }
            }
        }
        
        // Добавляем последнюю страницу, если есть содержимое
        if (currentPageContent.isNotEmpty()) {
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
    
    private fun createTextPaint(
        fontSize: TextUnit,
        fontFamily: FontWithName,
        fontThickness: ReaderFontThickness,
        fontStyle: FontStyle,
        textAlignment: ReaderTextAlignment,
        letterSpacing: TextUnit,
        density: Float
    ): TextPaint {
        return TextPaint().apply {
            this.textSize = fontSize.value * density // Convert sp to px
            this.typeface = Typeface.DEFAULT // TODO: Implement proper font conversion
            this.isFakeBoldText = fontThickness == ReaderFontThickness.MEDIUM
            this.textSkewX = if (fontStyle == FontStyle.Italic) -0.25f else 0f
            this.letterSpacing = letterSpacing.value.toFloat() * fontSize.value * density // Convert em to px
            this.isAntiAlias = true
            
            Log.d("PAGE_CALCULATOR", "TextPaint created: textSize=${this.textSize}px, letterSpacing=${this.letterSpacing}px")
        }
    }
    
    private fun getAlignment(textAlignment: ReaderTextAlignment): Layout.Alignment {
        return when (textAlignment) {
            ReaderTextAlignment.START -> Layout.Alignment.ALIGN_NORMAL
            ReaderTextAlignment.CENTER -> Layout.Alignment.ALIGN_CENTER
            ReaderTextAlignment.END -> Layout.Alignment.ALIGN_OPPOSITE
            ReaderTextAlignment.JUSTIFY -> Layout.Alignment.ALIGN_NORMAL
        }
    }
    
    private fun getLineSpacingMultiplier(lineHeight: TextUnit, fontSize: TextUnit): Float {
        val ratio = lineHeight.value / fontSize.value
        return if (ratio > 0f) ratio else 1f
    }
    
    private fun calculateParagraphHeight(
        text: String,
        textPaint: TextPaint,
        availableWidth: Int,
        fontSize: TextUnit,
        lineHeight: TextUnit,
        paragraphIndentation: TextUnit,
        density: Float
    ): Int {
        // Добавляем отступ первой строки к тексту (как в StyledText)
        val indentedText = if (paragraphIndentation.value > 0) {
            val indentPx = paragraphIndentation.value * density
            val fontSizePx = fontSize.value * density
            " ".repeat((indentPx / fontSizePx).toInt()) + text
        } else {
            text
        }
        
        val lineSpacingMultiplier = getLineSpacingMultiplier(lineHeight, fontSize)
        Log.d("PAGE_CALCULATOR", "StaticLayout params: textLength=${indentedText.length}, availableWidth=$availableWidth, lineSpacingMultiplier=$lineSpacingMultiplier")
        Log.d("PAGE_CALCULATOR", "TextPaint params: textSize=${textPaint.textSize}, letterSpacing=${textPaint.letterSpacing}")
        Log.d("PAGE_CALCULATOR", "Original text: '${text.take(100)}...'")
        Log.d("PAGE_CALCULATOR", "Indented text: '${indentedText.take(100)}...'")
        
        val staticLayout = StaticLayout.Builder
            .obtain(indentedText, 0, indentedText.length, textPaint, availableWidth)
            .setAlignment(getAlignment(ReaderTextAlignment.START))
            .setLineSpacing(0f, lineSpacingMultiplier)
            .setIncludePad(false)
            .build()
        
        Log.d("PAGE_CALCULATOR", "StaticLayout result: height=${staticLayout.height}, lineCount=${staticLayout.lineCount}")
        
        // Debug: Check line details
        for (i in 0 until staticLayout.lineCount) {
            val start = staticLayout.getLineStart(i)
            val end = staticLayout.getLineEnd(i)
            val lineText = indentedText.substring(start, end)
            Log.d("PAGE_CALCULATOR", "Line $i: '$lineText' (chars $start-$end)")
        }
        
        return staticLayout.height
    }
    
    private fun calculateChapterHeight(
        title: String,
        textPaint: TextPaint,
        availableWidth: Int,
        fontSize: TextUnit,
        lineHeight: TextUnit,
        density: Float
    ): Int {
        val staticLayout = StaticLayout.Builder
            .obtain(title, 0, title.length, textPaint, availableWidth)
            .setAlignment(getAlignment(ReaderTextAlignment.START)) // Добавляем выравнивание
            .setLineSpacing(0f, getLineSpacingMultiplier(lineHeight, fontSize))
            .setIncludePad(false)
            .build()
        return staticLayout.height + (lineHeight.value * density * 2).toInt() // Дополнительное пространство после заголовка
    }
    
    private fun calculateSeparatorHeight(
        textPaint: TextPaint,
        availableWidth: Int,
        fontSize: TextUnit,
        lineHeight: TextUnit,
        density: Float
    ): Int {
        val separatorText = "---"
        val staticLayout = StaticLayout.Builder
            .obtain(separatorText, 0, separatorText.length, textPaint, availableWidth)
            .setAlignment(getAlignment(ReaderTextAlignment.START)) // Добавляем выравнивание
            .setLineSpacing(0f, getLineSpacingMultiplier(lineHeight, fontSize))
            .setIncludePad(false)
            .build()
        return staticLayout.height + (lineHeight.value * density * 2).toInt() // Дополнительное пространство
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
            .setAlignment(getAlignment(ReaderTextAlignment.START))
            .setLineSpacing(0f, getLineSpacingMultiplier(lineHeight, fontSize))
            .setIncludePad(false)
            .build()
        
        val totalLines = staticLayout.lineCount
        
        // Находим максимальное количество строк, которое помещается в оставшееся место
        var maxLinesForCurrentPage = 0
        for (lineIndex in 0 until totalLines) {
            val startChar = staticLayout.getLineStart(0)
            val endChar = staticLayout.getLineStart(lineIndex + 1)
            val partText = text.substring(startChar, endChar)
            
            val partLayout = StaticLayout.Builder
                .obtain(partText, 0, partText.length, textPaint, availableWidth)
                .setAlignment(getAlignment(ReaderTextAlignment.START))
                .setLineSpacing(0f, getLineSpacingMultiplier(lineHeight, fontSize))
                .setIncludePad(false)
                .build()
            
            if (partLayout.height <= remainingSpace) {
                maxLinesForCurrentPage = lineIndex + 1
            } else {
                break
            }
        }
        
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
        
        // Создаем первую часть (то, что помещается на текущую страницу)
        val firstPartStartChar = staticLayout.getLineStart(0)
        val firstPartEndChar = staticLayout.getLineStart(maxLinesForCurrentPage)
        val firstPartText = text.substring(firstPartStartChar, firstPartEndChar)
        
        val firstPartLayout = StaticLayout.Builder
            .obtain(firstPartText, 0, firstPartText.length, textPaint, availableWidth)
            .setAlignment(getAlignment(ReaderTextAlignment.START))
            .setLineSpacing(0f, getLineSpacingMultiplier(lineHeight, fontSize))
            .setIncludePad(false)
            .build()
        
        val firstPart = BrokenParagraphPart(
            readerText = ReaderText.Text(
                line = androidx.compose.ui.text.AnnotatedString("\u200C$firstPartText\u00A0") // Добавляем невидимый символ как маркер + невидимый пробел в конец
            ),
            height = firstPartLayout.height,
            isContinuation = false // Это первая часть
        )
        
        // Создаем остаток (то, что идет на следующую страницу)
        // Убираем отступ первой строки для остатка разорванного параграфа
        val remainingPartStartChar = firstPartEndChar
        val remainingPartText = if (remainingPartStartChar < text.length) {
            text.substring(remainingPartStartChar)
        } else {
            ""
        }
        
        val remainingPart = if (remainingPartText.isNotEmpty()) {
            // Убираем отступ первой строки для остатка
            val trimmedText = remainingPartText.trimStart()
            val remainingPartLayout = StaticLayout.Builder
                .obtain(trimmedText, 0, trimmedText.length, textPaint, availableWidth)
                .setAlignment(getAlignment(ReaderTextAlignment.START))
                .setLineSpacing(0f, getLineSpacingMultiplier(lineHeight, fontSize))
                .setIncludePad(false)
                .build()
            
            BrokenParagraphPart(
                readerText = ReaderText.Text(
                    line = androidx.compose.ui.text.AnnotatedString("\u200B$trimmedText") // Добавляем невидимый символ как маркер продолжения
                ),
                height = remainingPartLayout.height,
                isContinuation = true // Помечаем как продолжение параграфа
            )
        } else {
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
            .setAlignment(getAlignment(ReaderTextAlignment.START)) // Добавляем выравнивание
            .setLineSpacing(0f, getLineSpacingMultiplier(lineHeight, fontSize))
            .setIncludePad(false)
            .build()
        
        val brokenParts = mutableListOf<BrokenParagraphPart>()
        val totalLines = staticLayout.lineCount
        val paragraphSpacingPx = (paragraphHeight.value * density).toInt()
        
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
                val startChar = staticLayout.getLineStart(currentLine)
                val endChar = if (endLine >= totalLines) {
                    text.length
                } else {
                    staticLayout.getLineStart(endLine)
                }
                
                val partText = text.substring(startChar, endChar)
                val partLayout = StaticLayout.Builder
                    .obtain(partText, 0, partText.length, textPaint, availableWidth)
                    .setAlignment(getAlignment(ReaderTextAlignment.START))
                    .setLineSpacing(0f, getLineSpacingMultiplier(lineHeight, fontSize))
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
            val startChar = staticLayout.getLineStart(currentLine)
            val endChar = if (bestEndLine >= totalLines) {
                text.length
            } else {
                staticLayout.getLineStart(bestEndLine)
            }
            
            val partText = text.substring(startChar, endChar)
            val partLayout = StaticLayout.Builder
                .obtain(partText, 0, partText.length, textPaint, availableWidth)
                .setAlignment(getAlignment(ReaderTextAlignment.START))
                .setLineSpacing(0f, getLineSpacingMultiplier(lineHeight, fontSize))
                .setIncludePad(false)
                .build()
            
            val partHeight = partLayout.height
            
            // Создаем новый ReaderText.Text для этой части
            val partReaderText = ReaderText.Text(
                line = androidx.compose.ui.text.AnnotatedString(partText)
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
