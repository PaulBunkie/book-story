/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.presentation.reader

import android.util.Log
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ua.acclorite.book_story.domain.reader.FontWithName
import ua.acclorite.book_story.domain.reader.ReaderFontThickness
import ua.acclorite.book_story.domain.reader.ReaderText
import ua.acclorite.book_story.domain.reader.ReaderTextAlignment

object PageCalculatorTest {
    
    suspend fun testPageCalculation() {
        Log.d("PAGE_CALCULATOR_TEST", "=== Testing Page Calculator ===")
        
        val calculator = PageCalculator()
        
        // Создаем тестовый текст
        val testText = listOf(
            ReaderText.Text(androidx.compose.ui.text.AnnotatedString("Это первая строка текста для тестирования.")),
            ReaderText.Text(androidx.compose.ui.text.AnnotatedString("Это вторая строка текста для тестирования.")),
            ReaderText.Text(androidx.compose.ui.text.AnnotatedString("Это третья строка текста для тестирования.")),
            ReaderText.Chapter(title = "Глава 1", nested = false),
            ReaderText.Text(androidx.compose.ui.text.AnnotatedString("Это текст после главы.")),
            ReaderText.Text(androidx.compose.ui.text.AnnotatedString("Еще одна строка текста.")),
            ReaderText.Text(androidx.compose.ui.text.AnnotatedString("И еще одна строка для полноты теста."))
        )
        
        val pages = calculator.calculatePages(
            text = testText,
            screenWidth = 1080, // Full HD width
            screenHeight = 1920, // Full HD height
            fontSize = 16.sp,
            lineHeight = 20.sp,
            sidePadding = 16.dp,
            paragraphHeight = 32.dp,
            fontFamily = FontWithName("default", ua.acclorite.book_story.domain.ui.UIText.StringResource(ua.acclorite.book_story.R.string.default_string), androidx.compose.ui.text.font.FontFamily.Default),
            fontThickness = ReaderFontThickness.NORMAL,
            fontStyle = FontStyle.Normal,
            textAlignment = ReaderTextAlignment.START,
            letterSpacing = 0.sp,
            paragraphIndentation = 0.sp,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(),
            verticalPadding = 0.dp
        )
        
        Log.d("PAGE_CALCULATOR_TEST", "Calculated ${pages.size} pages")
        
        pages.forEachIndexed { index, page ->
            Log.d("PAGE_CALCULATOR_TEST", "Page $index: ${page.content.take(50)}...")
        }
        
        Log.d("PAGE_CALCULATOR_TEST", "=== End Test ===")
    }
}
