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
        // Добавляем коэффициент безопасности для учета Spacer между элементами
        val safetyMargin = 0.95f // 95% от доступной высоты
        val availableHeight = ((screenHeight - contentPaddingPx.value.toInt() - verticalPaddingPx.value.toInt()) * safetyMargin).toInt()
        
        Log.d("PAGE_CALCULATOR", "Available space: ${availableWidth}x${availableHeight}")
        
        // Расчеты завершены
        
        Log.d("PAGE_CALCULATOR", "Creating TextPaint...")
        Log.d("PAGE_CALCULATOR", "Font parameters: fontSize=${fontSize.value}sp, lineHeight=${lineHeight.value}sp, letterSpacing=${letterSpacing.value}em")
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
        val paragraphSpacingPx = paragraphHeight.value.toInt()
        
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
                        paragraphIndentation = paragraphIndentation
                    )
                    
                    Log.d("PAGE_CALCULATOR_DEBUG", "Page $pageIndex : Element $originalIndex : Position ${currentPageContent.size} : Height ${paragraphHeightPx}px : Remaining ${availableHeight - currentPageHeight}px")
                    
                    // Учитываем интервалы между абзацами (если это не первый элемент на странице)
                    val totalHeight = if (currentPageContent.isNotEmpty()) {
                        paragraphHeightPx + paragraphSpacingPx // высота текста + интервал
                    } else {
                        paragraphHeightPx // только высота текста для первого элемента
                    }
                    
                    // Если абзац помещается на текущую страницу
                    if (currentPageHeight + totalHeight <= availableHeight) {
                        Log.d("PAGE_CALCULATOR_DEBUG", "Page $pageIndex : Element $originalIndex : FITS! Text: ${readerText.line.text.take(30)}...")
                        currentPageContent.add(readerText)
                        currentPageHeight += totalHeight
                    } else {
                        Log.d("PAGE_CALCULATOR_DEBUG", "Page $pageIndex : Element $originalIndex : DOESN'T FIT! Need ${totalHeight}px, have ${availableHeight - currentPageHeight}px")
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
                        
                        // Если абзац слишком большой для одной страницы, разрываем его
                        if (paragraphHeightPx > availableHeight) {
                            Log.d("PAGE_CALCULATOR_DEBUG", "Page $pageIndex : BREAKING paragraph $originalIndex : Text: ${readerText.line.text.take(50)}...")
                            val brokenParagraphs = breakParagraph(
                                paragraph = readerText,
                                textPaint = textPaint,
                                availableWidth = availableWidth,
                                availableHeight = availableHeight,
                                fontSize = fontSize,
                                lineHeight = lineHeight,
                                paragraphIndentation = paragraphIndentation,
                                paragraphHeight = paragraphHeight
                            )
                            
                            // Добавляем разорванные части
                            for ((index, brokenPart) in brokenParagraphs.withIndex()) {
                                // Для всех частей разорванного абзаца используем одинаковую логику
                                val partHeight = if (currentPageContent.isNotEmpty()) {
                                    brokenPart.height + paragraphSpacingPx
                                } else {
                                    brokenPart.height
                                }
                                
                                if (currentPageHeight + partHeight <= availableHeight) {
                                    Log.d("PAGE_CALCULATOR_DEBUG", "Page $pageIndex : Broken part $index FITS! Text: ${brokenPart.readerText.line.text.take(30)}...")
                                    currentPageContent.add(brokenPart.readerText)
                                    currentPageHeight += partHeight
                                } else {
                                    Log.d("PAGE_CALCULATOR_DEBUG", "Page $pageIndex : Broken part $index DOESN'T FIT!")
                                    // Не помещается на текущую страницу
                                    if (currentPageContent.isNotEmpty()) {
                                        Log.d("PAGE_CALCULATOR_DEBUG", "Page $pageIndex : SAVING broken parts with ${currentPageContent.size} elements")
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
                                    Log.d("PAGE_CALCULATOR_DEBUG", "Page $pageIndex : Adding broken part to NEW page")
                                    currentPageContent.add(brokenPart.readerText)
                                    currentPageHeight = brokenPart.height
                                }
                            }
                        } else {
                            // Абзац помещается на новую страницу
                            Log.d("PAGE_CALCULATOR_DEBUG", "Page $pageIndex : Element $originalIndex FITS on NEW page! Text: ${readerText.line.text.take(30)}...")
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
                    
                    // Учитываем интервалы между элементами
                    val totalHeight = if (currentPageContent.isNotEmpty()) {
                        chapterHeight + paragraphSpacingPx
                    } else {
                        chapterHeight
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
                    
                    // Учитываем интервалы между элементами
                    val totalHeight = if (currentPageContent.isNotEmpty()) {
                        separatorHeight + paragraphSpacingPx
                    } else {
                        separatorHeight
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
                        
                        // Добавляем разделитель на новую страницу
                        currentPageContent.add(readerText)
                        currentPageHeight = separatorHeight
                    }
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
        letterSpacing: TextUnit
    ): TextPaint {
        return TextPaint().apply {
            this.textSize = fontSize.value
            this.typeface = Typeface.DEFAULT // TODO: Implement proper font conversion
            this.isFakeBoldText = fontThickness == ReaderFontThickness.MEDIUM
            this.textSkewX = if (fontStyle == FontStyle.Italic) -0.25f else 0f
            this.letterSpacing = letterSpacing.value.toFloat()
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
        return (lineHeight.value - fontSize.value) / fontSize.value
    }
    
    private fun calculateParagraphHeight(
        text: String,
        textPaint: TextPaint,
        availableWidth: Int,
        fontSize: TextUnit,
        lineHeight: TextUnit,
        paragraphIndentation: TextUnit
    ): Int {
        // Добавляем отступ первой строки к тексту (как в StyledText)
        val indentedText = if (paragraphIndentation.value > 0) {
            " ".repeat((paragraphIndentation.value / fontSize.value).toInt()) + text
        } else {
            text
        }
        
        val lineSpacingMultiplier = getLineSpacingMultiplier(lineHeight, fontSize)
        Log.d("PAGE_CALCULATOR", "StaticLayout params: textLength=${indentedText.length}, availableWidth=$availableWidth, lineSpacingMultiplier=$lineSpacingMultiplier")
        Log.d("PAGE_CALCULATOR", "TextPaint params: textSize=${textPaint.textSize}, letterSpacing=${textPaint.letterSpacing}")
        
        val staticLayout = StaticLayout.Builder
            .obtain(indentedText, 0, indentedText.length, textPaint, availableWidth)
            .setAlignment(getAlignment(ReaderTextAlignment.START))
            .setLineSpacing(0f, lineSpacingMultiplier)
            .setIncludePad(false)
            .build()
        
        Log.d("PAGE_CALCULATOR", "StaticLayout result: height=${staticLayout.height}, lineCount=${staticLayout.lineCount}")
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
            .setAlignment(getAlignment(ReaderTextAlignment.START)) // Добавляем выравнивание
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
            .setAlignment(getAlignment(ReaderTextAlignment.START)) // Добавляем выравнивание
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
            .setAlignment(getAlignment(ReaderTextAlignment.START)) // Добавляем выравнивание
            .setLineSpacing(0f, getLineSpacingMultiplier(lineHeight, fontSize))
            .setIncludePad(false)
            .build()
        
        val brokenParts = mutableListOf<BrokenParagraphPart>()
        val totalLines = staticLayout.lineCount
        val paragraphSpacingPx = paragraphHeight.value.toInt()
        
        var currentLine = 0
        var isFirstPart = true
        
        while (currentLine < totalLines) {
            // Рассчитываем доступную высоту для этой части
            // Для всех частей разорванного абзаца используем полную доступную высоту,
            // так как интервалы между частями одного абзаца не нужны
            val effectiveAvailableHeight = availableHeight
            
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
        val height: Int
    )
    
}
