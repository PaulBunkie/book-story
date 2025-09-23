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
    val content: String,
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
        paragraphIndentation: TextUnit
    ): List<Page> = withContext(Dispatchers.Default) {
        Log.d("PAGE_CALCULATOR", "=== Starting page calculation ===")
        Log.d("PAGE_CALCULATOR", "Text items count: ${text.size}")
        Log.d("PAGE_CALCULATOR", "Screen: ${screenWidth}x${screenHeight}")
        
        val availableWidth = screenWidth - (sidePadding.value * 2).toInt()
        val availableHeight = screenHeight - (paragraphHeight.value * 2).toInt()
        
        Log.d("PAGE_CALCULATOR", "Available space: ${availableWidth}x${availableHeight}")
        
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

        // Собираем весь текст в один блок
        Log.d("PAGE_CALCULATOR", "Building full text...")
        val fullText = buildString {
            for (readerText in text) {
                when (readerText) {
                    is ReaderText.Text -> append(readerText.line.text).append("\n")
                    is ReaderText.Chapter -> append("\n").append(readerText.title).append("\n\n")
                    is ReaderText.Separator -> append("---\n")
                    is ReaderText.Image -> append("\n[Изображение]\n\n")
                }
            }
        }
        Log.d("PAGE_CALCULATOR", "Full text length: ${fullText.length}")
        
        // Создаем StaticLayout для всего текста
        Log.d("PAGE_CALCULATOR", "Creating StaticLayout...")
        val staticLayout = StaticLayout.Builder
            .obtain(fullText, 0, fullText.length, textPaint, availableWidth)
            .setAlignment(getAlignment(textAlignment))
            .setLineSpacing(0f, getLineSpacingMultiplier(lineHeight, fontSize))
            .setIncludePad(false)
            .build()
        Log.d("PAGE_CALCULATOR", "StaticLayout created. Line count: ${staticLayout.lineCount}")
        
        // Рассчитываем количество строк на странице
        Log.d("PAGE_CALCULATOR", "Calculating lines per page...")
        val linesPerPage = calculateLinesPerPage(staticLayout, availableHeight)
        Log.d("PAGE_CALCULATOR", "Lines per page: $linesPerPage")
        
        Log.d("PAGE_CALCULATOR", "Splitting text into pages...")
        val pages = splitTextIntoPages(fullText, staticLayout, linesPerPage)
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
    
    private fun calculateLinesPerPage(staticLayout: StaticLayout, availableHeight: Int): Int {
        if (staticLayout.lineCount == 0) return 1
        
        // Берем высоту первой строки как эталон
        val firstLineHeight = staticLayout.getLineBottom(0) - staticLayout.getLineTop(0)
        
        // Рассчитываем сколько строк помещается на экране
        val linesPerPage = (availableHeight / firstLineHeight).toInt()
        
        return maxOf(1, linesPerPage) // Минимум 1 строка на странице
    }
    
    private fun splitTextIntoPages(
        fullText: String,
        staticLayout: StaticLayout,
        linesPerPage: Int
    ): List<Page> {
        val pages = mutableListOf<Page>()
        val totalLines = staticLayout.lineCount
        
        var currentLine = 0
        var pageIndex = 0
        
        while (currentLine < totalLines) {
            val endLine = minOf(currentLine + linesPerPage, totalLines)
            
            // Извлекаем символы для текущей страницы
            val startChar = staticLayout.getLineStart(currentLine)
            val endChar = if (endLine >= totalLines) {
                fullText.length
            } else {
                staticLayout.getLineStart(endLine)
            }
            
            val pageText = fullText.substring(startChar, endChar).trimEnd()
            
            pages.add(
                Page(
                    content = pageText,
                    startIndex = pageIndex,
                    endIndex = pageIndex
                )
            )
            
            currentLine = endLine
            pageIndex++
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
    
    private fun getCurrentPageHeight(content: String, textPaint: TextPaint, width: Int): Int {
        val staticLayout = StaticLayout.Builder
            .obtain(content, 0, content.length, textPaint, width)
            .setIncludePad(false)
            .build()
        return staticLayout.height
    }
}
