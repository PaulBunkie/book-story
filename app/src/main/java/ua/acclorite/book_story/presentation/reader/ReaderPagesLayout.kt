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
import androidx.compose.foundation.Image
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.HorizontalDivider
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
import ua.acclorite.book_story.presentation.reader.TextMeasurementUtils
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
                           // Применяем тот же коэффициент безопасности, что и в PageCalculator
                           val safetyMargin = 1.00f // 100% от доступной высоты (синхронизировано с PageCalculator)
                           val availableHeight = ((screenHeight - contentPaddingPx.value.toInt() - verticalPaddingPx.value.toInt()) * safetyMargin).toInt()
                           
                           // Создаем TextPaint для расчета высот (синхронизировано с PageCalculator)
                           val textPaint = TextMeasurementUtils.createTextPaint(
                               fontSize = fontSize,
                               fontFamily = fontFamily,
                               fontThickness = fontThickness,
                               fontStyle = fontStyle,
                               textAlignment = textAlignment,
                               letterSpacing = letterSpacing,
                               density = density.density
                           )
                           
                           Log.d("PAGE_RENDER_DEBUG", "=== Page $pageIndex Render Analysis ===")
                           Log.d("PAGE_RENDER_DEBUG", "Screen: ${screenWidth}x${screenHeight}")
                           Log.d("PAGE_RENDER_DEBUG", "Available: ${availableWidth}x${availableHeight} (with ${(safetyMargin * 100).toInt()}% safety margin)")
                           
                           // Подсчет высоты будет в конце после рендеринга всех элементов
                           
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
                                       val textHeight = TextMeasurementUtils.calculateTextHeight(
                                           text = readerText.line.text,
                                           textPaint = textPaint,
                                           availableWidth = availableWidth,
                                           fontSize = fontSize,
                                           lineHeight = lineHeight,
                                           paragraphIndentation = paragraphIndentation,
                                           textAlignment = textAlignment
                                       )
                                       totalCalculatedHeight += textHeight
                                       
                                       Log.d("PAGE_RENDER_DEBUG", "Element $elementIndex (Text): ${textHeight}px - '${readerText.line.text.take(30)}...'")
                                       
                                       // Проверяем, является ли это разорванным параграфом
                                       // \u200B = продолжение, \u200C = первая часть
                                       val isBrokenParagraph = readerText.line.text.startsWith("\u200B") || readerText.line.text.startsWith("\u200C")
                                       val isContinuation = readerText.line.text.startsWith("\u200B")
                                       
                                       StyledText(
                                           text = readerText.line,
                                           style = TextStyle(
                                               fontFamily = fontFamily.font,
                                               fontWeight = fontThickness.thickness,
                                               textAlign = textAlignment.textAlignment,
                                               textIndent = if (isContinuation) TextIndent.None else TextIndent(firstLine = paragraphIndentation),
                                               fontStyle = fontStyle,
                                               letterSpacing = letterSpacing,
                                               fontSize = fontSize,
                                               lineHeight = lineHeight,
                                               color = fontColor,
                                               lineBreak = if (isBrokenParagraph) LineBreak.Simple else LineBreak.Paragraph
                                           ),
                                           highlightText = highlightedReading,
                                           highlightThickness = highlightedReadingThickness,
                                           modifier = Modifier.fillMaxWidth()
                                       )
                                   }
                                   
                                   is ReaderText.Chapter -> {
                                       // Рассчитываем высоту главы
                                       val chapterHeight = TextMeasurementUtils.calculateChapterHeight(
                                           title = readerText.title,
                                           textPaint = textPaint,
                                           availableWidth = availableWidth,
                                           fontSize = fontSize,
                                           lineHeight = lineHeight,
                                           paragraphHeight = paragraphHeight,
                                           density = density.density
                                       )
                                       totalCalculatedHeight += chapterHeight
                                       Log.d("PAGE_RENDER_DEBUG", "Element $elementIndex (Chapter): ${chapterHeight}px - '${readerText.title.take(30)}...'")
                                       
                                       // Рендерим главу как в Scroll режиме - с полоской
                                       Column(
                                           modifier = Modifier.fillMaxWidth()
                                       ) {
                                           Spacer(modifier = Modifier.height(22.dp))
                                           
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
                                                   .padding(horizontal = sidePadding)
                                           )
                                           
                                           Spacer(modifier = Modifier.height(16.dp))
                                           HorizontalDivider(
                                               color = fontColor.copy(0.4f),
                                               modifier = Modifier.padding(horizontal = sidePadding)
                                           )
                                           Spacer(modifier = Modifier.height(16.dp))
                                       }
                                   }
                                   
                                   is ReaderText.Separator -> {
                                       // Рассчитываем высоту разделителя
                                       val separatorHeight = TextMeasurementUtils.calculateSeparatorHeight(
                                           textPaint = textPaint,
                                           availableWidth = availableWidth,
                                           fontSize = fontSize,
                                           lineHeight = lineHeight,
                                           paragraphHeight = paragraphHeight,
                                           density = density.density
                                       )
                                       totalCalculatedHeight += separatorHeight
                                       Log.d("PAGE_RENDER_DEBUG", "Element $elementIndex (Separator): ${separatorHeight}px")
                                       
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
                                       // Рассчитываем высоту изображения
                                       val imageHeight = 200 // Фиксированная высота для изображений
                                       totalCalculatedHeight += imageHeight
                                       
                                       Log.d("PAGE_RENDER_DEBUG", "Element $elementIndex (Image): ${imageHeight}px")
                                       
                                       // Рендерим реальное изображение
                                       Image(
                                           bitmap = readerText.imageBitmap,
                                           contentDescription = "Изображение из книги",
                                           modifier = Modifier
                                               .fillMaxWidth()
                                               .height(imageHeight.dp)
                                               .padding(vertical = 16.dp),
                                           contentScale = androidx.compose.ui.layout.ContentScale.Fit
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

// Функция для расчета высоты текста (синхронизирована с PageCalculator.calculateParagraphHeight)

