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
        verticalPadding: Dp
    ): List<Page> = withContext(Dispatchers.Default) {
        Log.d("PAGE_CALCULATOR", "=== Starting page calculation with paragraphs ===")
        Log.d("PAGE_CALCULATOR", "Text items count: ${text.size}")
        Log.d("PAGE_CALCULATOR", "Screen: ${screenWidth}x${screenHeight}")
        
        // Учитываем все отступы: contentPadding + verticalPadding + sidePadding
        val contentPaddingPx = contentPadding.calculateTopPadding() + contentPadding.calculateBottomPadding()
        val verticalPaddingPx = verticalPadding * 2
        val sidePaddingPx = sidePadding * 2
        
        val availableWidth = screenWidth - sidePaddingPx.value.toInt()
        val availableHeight = screenHeight - contentPaddingPx.value.toInt() - verticalPaddingPx.value.toInt()
        
        Log.d("PAGE_CALCULATOR", "Available space: ${availableWidth}x${availableHeight}")
        
        // Расчеты завершены
        
        Log.d("PAGE_CALCULATOR", "Creating TextPaint...")
        val textPaint = createTextPaint(
            fontSize = fontSize,
            fontFamily = fontFamily,
            fontThickness = fontThickness,
            fontStyle = fontStyle,
            textAlignment = textAlignment,
            letterSpacing = letterSpacing
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
            paragraphHeight = paragraphHeight
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
        paragraphHeight: Dp
    ): List<Page> {
        val pages = mutableListOf<Page>()
        val currentPageContent = mutableListOf<ReaderText>()
        var currentPageHeight = 0
        var pageIndex = 0
        
        for (readerText in text) {
            when (readerText) {
                is ReaderText.Text -> {
                    // Рассчитываем высоту абзаца
                    val paragraphHeightPx = calculateParagraphHeight(
                        text = readerText.line.text,
                        textPaint = textPaint,
                        availableWidth = availableWidth,
                        fontSize = fontSize,
                        lineHeight = lineHeight,
                        paragraphIndentation = paragraphIndentation
                    )
                    
                    // Если абзац помещается на текущую страницу
                    if (currentPageHeight + paragraphHeightPx <= availableHeight) {
                        currentPageContent.add(readerText)
                        currentPageHeight += paragraphHeightPx
                    } else {
                        // Если страница не пустая, сохраняем её
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
                        
                        // Если абзац слишком большой для одной страницы, разрываем его
                        if (paragraphHeightPx > availableHeight) {
                            // Рассчитываем доступную высоту с учетом уже имеющегося контента
                            val remainingHeight = availableHeight - currentPageHeight
                            val brokenParagraphs = breakParagraph(
                                paragraph = readerText,
                                textPaint = textPaint,
                                availableWidth = availableWidth,
                                availableHeight = remainingHeight,
                                fontSize = fontSize,
                                lineHeight = lineHeight,
                                paragraphIndentation = paragraphIndentation,
                                paragraphHeight = paragraphHeight
                            )
                            
                            // Добавляем разорванные части
                            for ((index, brokenPart) in brokenParagraphs.withIndex()) {
                                if (index == 0) {
                                    // Первая часть: пытаемся добавить на текущую страницу
                                    if (currentPageHeight + brokenPart.height <= availableHeight) {
                                        currentPageContent.add(brokenPart.readerText)
                                        currentPageHeight += brokenPart.height
                                    } else {
                                        // Не помещается на текущую страницу
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
                                        // Добавляем первую часть на новую страницу
                                        currentPageContent.add(brokenPart.readerText)
                                        currentPageHeight = brokenPart.height
                                    }
                                } else {
                                    // Последующие части: всегда начинают новую страницу
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
                                    // Добавляем часть на новую страницу
                                    currentPageContent.add(brokenPart.readerText)
                                    currentPageHeight = brokenPart.height
                                }
                            }
                        } else {
                            // Абзац помещается на новую страницу
                            currentPageContent.add(readerText)
                            currentPageHeight = paragraphHeightPx
                        }
                    }
                }
                
                is ReaderText.Chapter -> {
                    val chapterHeight = calculateChapterHeight(
                        title = readerText.title,
                        textPaint = textPaint,
                        availableWidth = availableWidth,
                        fontSize = fontSize,
                        lineHeight = lineHeight
                    )
                    
                    if (currentPageHeight + chapterHeight <= availableHeight) {
                        currentPageContent.add(readerText)
                        currentPageHeight += chapterHeight
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
                        
                        // Добавляем заголовок на новую страницу
                        currentPageContent.add(readerText)
                        currentPageHeight = chapterHeight
                    }
                }
                
                is ReaderText.Separator -> {
                    val separatorHeight = calculateSeparatorHeight(
                        textPaint = textPaint,
                        availableWidth = availableWidth,
                        fontSize = fontSize,
                        lineHeight = lineHeight
                    )
                    
                    if (currentPageHeight + separatorHeight <= availableHeight) {
                        currentPageContent.add(readerText)
                        currentPageHeight += separatorHeight
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
                        
                        // Добавляем разделитель на новую страницу
                        currentPageContent.add(readerText)
                        currentPageHeight = separatorHeight
                    }
                }
                
                is ReaderText.Image -> {
                    val imageHeight = 200 // Фиксированная высота для изображений
                    
                    if (currentPageHeight + imageHeight <= availableHeight) {
                        currentPageContent.add(readerText)
                        currentPageHeight += imageHeight
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
        
        // Страницы созданы
        
        return pages
    }
    
    private fun createTextPaint(
        fontSize: TextUnit,
        fontFamily: FontWithName,
        fontThickness: ReaderFontThickness,
        fontStyle: FontStyle,
        textAlignment: ReaderTextAlignment,
        letterSpacing: TextUnit
    ): TextPaint {
        return TextPaint().apply {
            this.textSize = fontSize.value
            this.typeface = Typeface.DEFAULT
            this.isFakeBoldText = fontThickness == ReaderFontThickness.MEDIUM
            this.textSkewX = if (fontStyle == FontStyle.Italic) -0.25f else 0f
            this.letterSpacing = letterSpacing.value
            this.isAntiAlias = true
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
        return lineHeight.value / fontSize.value
    }
    
    private fun calculateParagraphHeight(
        text: String,
        textPaint: TextPaint,
        availableWidth: Int,
        fontSize: TextUnit,
        lineHeight: TextUnit,
        paragraphIndentation: TextUnit
    ): Int {
        val staticLayout = StaticLayout.Builder
            .obtain(text, 0, text.length, textPaint, availableWidth)
            .setLineSpacing(0f, getLineSpacingMultiplier(lineHeight, fontSize))
            .setIncludePad(false)
            .build()
        return staticLayout.height
    }
    
    private fun calculateChapterHeight(
        title: String,
        textPaint: TextPaint,
        availableWidth: Int,
        fontSize: TextUnit,
        lineHeight: TextUnit
    ): Int {
        val staticLayout = StaticLayout.Builder
            .obtain(title, 0, title.length, textPaint, availableWidth)
            .setLineSpacing(0f, getLineSpacingMultiplier(lineHeight, fontSize))
            .setIncludePad(false)
            .build()
        return staticLayout.height + (lineHeight.value * 2).toInt() // Дополнительное пространство после заголовка
    }
    
    private fun calculateSeparatorHeight(
        textPaint: TextPaint,
        availableWidth: Int,
        fontSize: TextUnit,
        lineHeight: TextUnit
    ): Int {
        val separatorText = "---"
        val staticLayout = StaticLayout.Builder
            .obtain(separatorText, 0, separatorText.length, textPaint, availableWidth)
            .setLineSpacing(0f, getLineSpacingMultiplier(lineHeight, fontSize))
            .setIncludePad(false)
            .build()
        return staticLayout.height + (lineHeight.value * 2).toInt() // Дополнительное пространство
    }
    
    private fun breakParagraph(
        paragraph: ReaderText.Text,
        textPaint: TextPaint,
        availableWidth: Int,
        availableHeight: Int,
        fontSize: TextUnit,
        lineHeight: TextUnit,
        paragraphIndentation: TextUnit,
        paragraphHeight: Dp
    ): List<BrokenParagraphPart> {
        val text = paragraph.line.text
        val staticLayout = StaticLayout.Builder
            .obtain(text, 0, text.length, textPaint, availableWidth)
            .setLineSpacing(0f, getLineSpacingMultiplier(lineHeight, fontSize))
            .setIncludePad(false)
            .build()
        
        val brokenParts = mutableListOf<BrokenParagraphPart>()
        val totalLines = staticLayout.lineCount
        val lineHeightPx = (lineHeight.value).toInt()
        val paragraphHeightPx = (paragraphHeight.value).toInt()
        
        var currentLine = 0
        var isFirstPart = true
        
        while (currentLine < totalLines) {
            // Рассчитываем доступную высоту для этой части
            val effectiveAvailableHeight = if (isFirstPart) {
                availableHeight // Первая часть может использовать всю доступную высоту
            } else {
                availableHeight - paragraphHeightPx // Последующие части учитывают интервал
            }
            
            val linesPerPage = effectiveAvailableHeight / lineHeightPx
            val endLine = minOf(currentLine + linesPerPage, totalLines)
            
            // Извлекаем текст для этой части
            val startChar = staticLayout.getLineStart(currentLine)
            val endChar = if (endLine >= totalLines) {
                text.length
            } else {
                staticLayout.getLineStart(endLine)
            }
            
            val partText = text.substring(startChar, endChar)
            val partHeight = (endLine - currentLine) * lineHeightPx
            
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
            
            currentLine = endLine
            isFirstPart = false
        }
        
        // Абзац разбит на части
        
        return brokenParts
    }
    
    private data class BrokenParagraphPart(
        val readerText: ReaderText.Text,
        val height: Int
    )
    
}
