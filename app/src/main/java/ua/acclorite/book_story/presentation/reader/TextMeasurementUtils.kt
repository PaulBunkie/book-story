/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.presentation.reader

import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import ua.acclorite.book_story.domain.reader.ReaderFontThickness
import ua.acclorite.book_story.domain.reader.ReaderTextAlignment
import ua.acclorite.book_story.domain.reader.FontWithName
import ua.acclorite.book_story.domain.reader.ReaderText
import kotlin.math.roundToInt

object TextMeasurementUtils {
    
    fun createTextPaint(
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
            this.letterSpacing = letterSpacing.value.toFloat() * density // Convert em to px
            this.isAntiAlias = true
        }
    }
    
    fun getAlignment(textAlignment: ReaderTextAlignment): Layout.Alignment {
        return when (textAlignment) {
            ReaderTextAlignment.START -> Layout.Alignment.ALIGN_NORMAL
            ReaderTextAlignment.CENTER -> Layout.Alignment.ALIGN_CENTER
            ReaderTextAlignment.END -> Layout.Alignment.ALIGN_OPPOSITE
            ReaderTextAlignment.JUSTIFY -> Layout.Alignment.ALIGN_NORMAL
        }
    }
    
    fun getLineSpacingMultiplier(lineHeight: TextUnit, fontSize: TextUnit): Float {
        val ratio = lineHeight.value / fontSize.value
        return if (ratio > 0f) ratio else 1f
    }
    
    fun getLineSpacingAdd(lineHeight: TextUnit, fontSize: TextUnit, density: Float): Float {
        val extraSpacing = (lineHeight.value - fontSize.value) * density
        return extraSpacing.coerceAtLeast(0f)
    }
    
    fun getFontWeight(fontThickness: ReaderFontThickness): FontWeight {
        return fontThickness.thickness
    }
    
    fun getTextAlign(textAlignment: ReaderTextAlignment): TextAlign {
        return when (textAlignment) {
            ReaderTextAlignment.START -> TextAlign.Start
            ReaderTextAlignment.CENTER -> TextAlign.Center
            ReaderTextAlignment.END -> TextAlign.End
            ReaderTextAlignment.JUSTIFY -> TextAlign.Justify
        }
    }

    /**
     * Применяет эффект подсветки текста (Bionic Reading) без изменения исходной строки
     */
    fun applyHighlighting(text: AnnotatedString, highlightThickness: FontWeight): AnnotatedString {
        return buildAnnotatedString {
            append(text) // Сохраняем оригинальные индексы и скрытые символы
            
            val wordRegex = Regex("\\S+")
            wordRegex.findAll(text.text).forEach { matchResult ->
                val wordString = matchResult.value
                if (wordString.none { it.isLetter() }) {
                    return@forEach
                }

                val textWord = wordString.dropWhile { !it.isLetter() }.dropLastWhile { !it.isLetter() }
                var digitsAtStart = 0
                for (char in wordString) {
                    if (!char.isLetter()) digitsAtStart++
                    else break
                }
                
                val highlightArea = when (textWord.length) {
                    3 -> 1
                    else -> (textWord.length * 0.5f).roundToInt()
                } + digitsAtStart

                val startOffset = matchResult.range.first + digitsAtStart
                val endOffset = matchResult.range.first + highlightArea
                
                if (startOffset < endOffset) {
                    addStyle(
                        style = SpanStyle(fontWeight = highlightThickness),
                        start = startOffset,
                        end = endOffset
                    )
                }
            }
        }
    }
    
    fun calculateTextHeight(
        text: String,
        textPaint: TextPaint,
        availableWidth: Int,
        fontSize: TextUnit,
        lineHeight: TextUnit,
        paragraphIndentation: TextUnit,
        textAlignment: ReaderTextAlignment
    ): Int {
        val indentedText = text
        val lineSpacingMultiplier = getLineSpacingMultiplier(lineHeight, fontSize)
        
        val staticLayout = StaticLayout.Builder
            .obtain(indentedText, 0, indentedText.length, textPaint, availableWidth)
            .setAlignment(getAlignment(textAlignment))
            .setLineSpacing(0f, lineSpacingMultiplier)
            .setIncludePad(false)
            .build()
        
        return staticLayout.height
    }
    
    fun calculateChapterHeight(
        title: String,
        textPaint: TextPaint,
        availableWidth: Int,
        fontSize: TextUnit,
        lineHeight: TextUnit,
        density: Float
    ): Int {
        val titleTextPaint = TextPaint(textPaint).apply {
            textSize = (fontSize * 1.2f).value
            isFakeBoldText = true
        }

        val staticLayout = StaticLayout.Builder
            .obtain(title, 0, title.length, titleTextPaint, availableWidth)
            .setAlignment(getAlignment(ReaderTextAlignment.START))
            .setLineSpacing(0f, getLineSpacingMultiplier(lineHeight * 1.2f, fontSize * 1.2f))
            .setIncludePad(false)
            .build()

        val topSpacer = (22.dp.value * density).roundToInt()
        val bottomSpacer1 = (16.dp.value * density).roundToInt()
        val bottomSpacer2 = (16.dp.value * density).roundToInt()
        val dividerHeight = (1.dp.value * density).roundToInt()

        return topSpacer + staticLayout.height + bottomSpacer1 + dividerHeight + bottomSpacer2
    }
    
    fun calculateSeparatorHeight(
        textPaint: TextPaint,
        availableWidth: Int,
        density: Float
    ): Int {
        val dividerHeight = (3.dp.value * density).roundToInt()
        return dividerHeight
    }
    
    fun calculateElementHeight(
        readerText: ReaderText,
        textPaint: TextPaint,
        availableWidth: Int,
        fontSize: TextUnit,
        lineHeight: TextUnit,
        paragraphIndentation: TextUnit,
        density: Float
    ): Int {
        return when (readerText) {
            is ReaderText.Text -> {
                calculateTextHeight(
                    text = readerText.line.text,
                    textPaint = textPaint,
                    availableWidth = availableWidth,
                    fontSize = fontSize,
                    lineHeight = lineHeight,
                    paragraphIndentation = paragraphIndentation,
                    textAlignment = ReaderTextAlignment.START
                )
            }
            
            is ReaderText.Chapter -> {
                calculateChapterHeight(
                    title = readerText.title,
                    textPaint = textPaint,
                    availableWidth = availableWidth,
                    fontSize = fontSize,
                    lineHeight = lineHeight,
                    density = density
                )
            }
            
            is ReaderText.Separator -> {
                calculateSeparatorHeight(
                    textPaint = textPaint,
                    availableWidth = availableWidth,
                    density = density
                )
            }
            
            is ReaderText.Image -> {
                200 
            }
        }
    }
    
    fun calculatePageHeight(
        pageContent: List<ReaderText>,
        textPaint: TextPaint,
        availableWidth: Int,
        fontSize: TextUnit,
        lineHeight: TextUnit,
        paragraphIndentation: TextUnit,
        paragraphHeight: Dp,
        density: Float
    ): Int {
        var totalHeight = 0
        val paragraphSpacingPx = (paragraphHeight.value * density).roundToInt()
        
        for ((index, readerText) in pageContent.withIndex()) {
            if (index > 0) {
                totalHeight += paragraphSpacingPx
            }
            
            totalHeight += calculateElementHeight(
                readerText = readerText,
                textPaint = textPaint,
                availableWidth = availableWidth,
                fontSize = fontSize,
                lineHeight = lineHeight,
                paragraphIndentation = paragraphIndentation,
                density = density
            )
        }
        
        return totalHeight
    }
}
