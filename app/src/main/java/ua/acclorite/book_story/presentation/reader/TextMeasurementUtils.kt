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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import ua.acclorite.book_story.domain.reader.ReaderFontThickness
import ua.acclorite.book_story.domain.reader.ReaderTextAlignment
import ua.acclorite.book_story.domain.reader.FontWithName

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
    
    fun calculateTextHeight(
        text: String,
        textPaint: TextPaint,
        availableWidth: Int,
        fontSize: TextUnit,
        lineHeight: TextUnit,
        paragraphIndentation: TextUnit,
        textAlignment: ReaderTextAlignment
    ): Int {
        // Используем оригинальный текст без добавления пробелов
        // Отступ первой строки будет учитываться в рендере
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
        paragraphHeight: Dp,
        density: Float
    ): Int {
        val titleTextPaint = TextPaint(textPaint).apply {
            textSize = (fontSize * 1.2f).value * density
            isFakeBoldText = true
        }

        val staticLayout = StaticLayout.Builder
            .obtain(title, 0, title.length, titleTextPaint, availableWidth)
            .setAlignment(getAlignment(ReaderTextAlignment.START))
            .setLineSpacing(0f, getLineSpacingMultiplier(lineHeight * 1.2f, fontSize * 1.2f))
            .setIncludePad(false)
            .build()

        val topSpacer = (22.dp.value * density).toInt()     // Реальное значение из рендера
        val bottomSpacer1 = (16.dp.value * density).toInt() // Реальное значение из рендера
        val bottomSpacer2 = (16.dp.value * density).toInt() // Реальное значение из рендера

        return topSpacer + staticLayout.height + bottomSpacer1 + bottomSpacer2
    }
    
    fun calculateSeparatorHeight(
        textPaint: TextPaint,
        availableWidth: Int,
        fontSize: TextUnit,
        lineHeight: TextUnit,
        paragraphHeight: Dp,
        density: Float
    ): Int {
        val separatorText = "---"
        val staticLayout = StaticLayout.Builder
            .obtain(separatorText, 0, separatorText.length, textPaint, availableWidth)
            .setAlignment(getAlignment(ReaderTextAlignment.CENTER))
            .setLineSpacing(0f, getLineSpacingMultiplier(lineHeight, fontSize))
            .setIncludePad(false)
            .build()

        val topPadding = (paragraphHeight.value * density * 0.8).toInt()
        val bottomPadding = (paragraphHeight.value * density * 0.8).toInt()

        return topPadding + staticLayout.height + bottomPadding
    }
}
