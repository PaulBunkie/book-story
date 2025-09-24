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

@Composable
fun ReaderPagesLayout(
    pages: List<Page>,
    activity: ComponentActivity,
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
                           for (readerText in page.content) {
                               // Добавляем интервал между элементами (кроме первого)
                               if (!isFirstElement) {
                                   Spacer(modifier = Modifier.height(paragraphHeight))
                               }
                               isFirstElement = false
                               
                               when (readerText) {
                                   is ReaderText.Text -> {
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
                       }
                   }
               }
    }
}

