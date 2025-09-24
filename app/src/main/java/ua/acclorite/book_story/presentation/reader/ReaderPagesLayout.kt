/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.presentation.reader

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import ua.acclorite.book_story.presentation.core.util.noRippleClickable
import ua.acclorite.book_story.presentation.core.components.common.SelectionContainer
import ua.acclorite.book_story.presentation.core.components.common.StyledText
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.toFontFamily
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import ua.acclorite.book_story.domain.reader.FontWithName
import ua.acclorite.book_story.domain.reader.ReaderFontThickness
import ua.acclorite.book_story.domain.reader.ReaderTextAlignment
import ua.acclorite.book_story.domain.reader.ReaderText
import ua.acclorite.book_story.ui.reader.ReaderEvent
import android.util.Log
import android.graphics.Paint
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.graphics.Typeface

@Composable
fun ReaderPagesLayout(
    pages: List<Page>,
    activity: ComponentActivity,
    screenWidth: Int,
    screenHeight: Int,
    fontFamily: FontWithName,
    fontColor: Color,
    lineHeight: TextUnit,
    fontThickness: ReaderFontThickness,
    fontStyle: FontStyle,
    textAlignment: ReaderTextAlignment,
    fontSize: TextUnit,
    letterSpacing: TextUnit,
    sidePadding: Dp,
    paragraphIndentation: TextUnit,
    paragraphHeight: Dp,
    contentPadding: PaddingValues,
    verticalPadding: Dp,
    onPageChanged: (Int) -> Unit,
    // Добавляем параметры для обработки тапов меню
    showMenu: Boolean,
    fullscreenMode: Boolean,
    onMenuVisibility: (ReaderEvent.OnMenuVisibility) -> Unit,
    // Параметры для подсветки чтения (как в обычном режиме)
    highlightedReading: Boolean,
    highlightedReadingThickness: FontWeight
) {
    Log.d("READER_PAGES_LAYOUT", "=== Creating ReaderPagesLayout ===")
    Log.d("READER_PAGES_LAYOUT", "Pages count: ${pages.size}")
    
    val density = LocalDensity.current

    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { pages.size }
    )
    
    // Отслеживаем изменения страниц
    LaunchedEffect(pagerState.currentPage) {
        Log.d("READER_PAGES_LAYOUT", "Page changed to: ${pagerState.currentPage}")
        onPageChanged(pagerState.currentPage)
    }
    
    SelectionContainer(
        onCopyRequested = { /* TODO: Handle copy */ },
        onShareRequested = { /* TODO: Handle share */ },
        onWebSearchRequested = { /* TODO: Handle web search */ },
        onTranslateRequested = { /* TODO: Handle translate */ },
        onDictionaryRequested = { /* TODO: Handle dictionary */ }
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .noRippleClickable(
                    onClick = {
                        Log.d("READER_PAGES_LAYOUT", "Pager tapped - toggling menu")
                        onMenuVisibility(
                            ReaderEvent.OnMenuVisibility(
                                show = !showMenu,
                                fullscreenMode = fullscreenMode,
                                saveCheckpoint = true,
                                activity = activity
                            )
                        )
                    }
                )
               ) { pageIndex ->
                   if (pageIndex < pages.size) {
                       val page = pages[pageIndex]

                       // Рендерим каждый ReaderText элемент отдельно
                       Column(
                           modifier = Modifier
                               .fillMaxSize()
                               .padding(contentPadding)
                               .padding(vertical = verticalPadding)
                               .padding(
                                   start = sidePadding,
                                   end = sidePadding
                               )
                       ) {
                           var isFirstElement = true
                           var totalElements = 0
                           var totalTextElements = 0
                           var totalCalculatedHeight = 0
                           
                           // Рассчитываем доступную высоту
                           val contentPaddingPx = contentPadding.calculateTopPadding() + contentPadding.calculateBottomPadding()
                           val verticalPaddingPx = verticalPadding * 2
                           val sidePaddingPx = sidePadding * 2
                           val availableWidth = screenWidth - sidePaddingPx.value.toInt()
                           val availableHeight = screenHeight - contentPaddingPx.value.toInt() - verticalPaddingPx.value.toInt()
                           
                           // Создаем TextPaint для расчета высот
                           val textPaint = TextPaint().apply {
                               textSize = fontSize.value
                               typeface = Typeface.DEFAULT
                               isFakeBoldText = fontThickness == ReaderFontThickness.MEDIUM
                               textSkewX = if (fontStyle == FontStyle.Italic) -0.25f else 0f
                               this.letterSpacing = letterSpacing.value.toFloat()
                           }
                           
                           Log.d("PAGE_RENDER_DEBUG", "=== Page $pageIndex Render Analysis ===")
                           Log.d("PAGE_RENDER_DEBUG", "Screen: ${screenWidth}x${screenHeight}")
                           Log.d("PAGE_RENDER_DEBUG", "Available: ${availableWidth}x${availableHeight}")
                           Log.d("PAGE_RENDER_DEBUG", "Elements: ${page.content.size}")
                           
                           for ((elementIndex, readerText) in page.content.withIndex()) {
                               // Добавляем интервал между элементами (кроме первого)
                               if (!isFirstElement) {
                                   Spacer(modifier = Modifier.height(paragraphHeight))
                                   totalCalculatedHeight += paragraphHeight.value.toInt()
                               }
                               isFirstElement = false
                               totalElements++
                               
                               when (readerText) {
                                   is ReaderText.Text -> {
                                       totalTextElements++
                                       
                                       // Рассчитываем высоту текста
                                       val textHeight = calculateTextHeight(
                                           text = readerText.line.text,
                                           textPaint = textPaint,
                                           availableWidth = availableWidth,
                                           fontSize = fontSize,
                                           lineHeight = lineHeight,
                                           paragraphIndentation = paragraphIndentation
                                       )
                                       totalCalculatedHeight += textHeight
                                       
                                       Log.d("PAGE_RENDER_DEBUG", "Element $elementIndex (Text):")
                                       Log.d("PAGE_RENDER_DEBUG", "  Text: ${readerText.line.text.take(50)}...")
                                       Log.d("PAGE_RENDER_DEBUG", "  Height: ${textHeight}px")
                                       
                                       StyledText(
                                           text = readerText.line,
                                           style = TextStyle(
                                               fontFamily = fontFamily.font,
                                               fontWeight = fontThickness.thickness,
                                               textAlign = textAlignment.textAlignment,
                                               textIndent = TextIndent(firstLine = paragraphIndentation),
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
                                   }
                                   
                                   is ReaderText.Chapter -> {
                                       Text(
                                           text = readerText.title,
                                           style = TextStyle(
                                               fontFamily = fontFamily.font,
                                               fontWeight = FontWeight.Bold,
                                               textAlign = textAlignment.textAlignment,
                                               fontSize = fontSize * 1.2f,
                                               lineHeight = lineHeight * 1.2f,
                                               color = fontColor
                                           ),
                                           modifier = Modifier
                                               .fillMaxWidth()
                                               .padding(vertical = 16.dp)
                                       )
                                   }
                                   
                                   is ReaderText.Separator -> {
                                       Text(
                                           text = "---",
                                           style = TextStyle(
                                               fontFamily = fontFamily.font,
                                               fontWeight = fontThickness.thickness,
                                               textAlign = TextAlign.Center,
                                               fontSize = fontSize,
                                               lineHeight = lineHeight,
                                               color = fontColor
                                           ),
                                           modifier = Modifier
                                               .fillMaxWidth()
                                               .padding(vertical = 16.dp)
                                       )
                                   }
                                   
                                   is ReaderText.Image -> {
                                       // TODO: Реализовать отображение изображений
                                       Text(
                                           text = "[Изображение]",
                                           style = TextStyle(
                                               fontFamily = fontFamily.font,
                                               fontWeight = fontThickness.thickness,
                                               textAlign = TextAlign.Center,
                                               fontSize = fontSize,
                                               lineHeight = lineHeight,
                                               color = fontColor
                                           ),
                                           modifier = Modifier
                                               .fillMaxWidth()
                                               .padding(vertical = 16.dp)
                                       )
                                   }
                               }
                           }
                           
                           // Логируем итоговую статистику рендера страницы
                           Log.d("PAGE_RENDER_DEBUG", "=== Page $pageIndex Summary ===")
                           Log.d("PAGE_RENDER_DEBUG", "Total elements: $totalElements")
                           Log.d("PAGE_RENDER_DEBUG", "Total text elements: $totalTextElements")
                           Log.d("PAGE_RENDER_DEBUG", "Total calculated height: ${totalCalculatedHeight}px")
                           Log.d("PAGE_RENDER_DEBUG", "Available height: ${availableHeight}px")
                           val difference = totalCalculatedHeight - availableHeight
                           Log.d("PAGE_RENDER_DEBUG", "Difference: ${difference}px")
                           Log.d("PAGE_RENDER_DEBUG", "Fits: ${totalCalculatedHeight <= availableHeight}")
                           if (totalCalculatedHeight > availableHeight) {
                               Log.d("PAGE_RENDER_DEBUG", "⚠️ OVERFLOW DETECTED! Page $pageIndex exceeds available height by ${difference}px")
                           }
                           Log.d("PAGE_RENDER_DEBUG", "Page $pageIndex rendering completed")
                       }
                   }
               }
    }
}

// Функция для расчета высоты текста (копия из PageCalculator)
private fun calculateTextHeight(
    text: String,
    textPaint: TextPaint,
    availableWidth: Int,
    fontSize: TextUnit,
    lineHeight: TextUnit,
    paragraphIndentation: TextUnit
): Int {
    val lineHeightPx = lineHeight.value.toInt()
    val lineSpacingMultiplier = getLineSpacingMultiplier(lineHeight, fontSize)
    
    val staticLayout = StaticLayout.Builder.obtain(
        text, 0, text.length, textPaint, availableWidth
    )
        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
        .setLineSpacing(0f, lineSpacingMultiplier)
        .setIncludePad(false)
        .build()
    
    return staticLayout.height
}

private fun getLineSpacingMultiplier(lineHeight: TextUnit, fontSize: TextUnit): Float {
    return if (lineHeight.value > fontSize.value) {
        (lineHeight.value - fontSize.value) / fontSize.value
    } else {
        0f
    }
}

