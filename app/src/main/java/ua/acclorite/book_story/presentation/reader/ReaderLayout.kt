/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.presentation.reader

import android.os.Build
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import android.util.Log
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import ua.acclorite.book_story.R
import ua.acclorite.book_story.domain.reader.FontWithName
import ua.acclorite.book_story.domain.reader.ReaderFontThickness
import ua.acclorite.book_story.domain.reader.ReaderHorizontalGesture
import ua.acclorite.book_story.domain.reader.ReaderText
import ua.acclorite.book_story.domain.reader.ReaderTextAlignment
import ua.acclorite.book_story.domain.util.HorizontalAlignment
import ua.acclorite.book_story.presentation.core.components.common.AnimatedVisibility
import ua.acclorite.book_story.presentation.core.components.common.LazyColumnWithScrollbar
import ua.acclorite.book_story.presentation.core.components.common.SelectionContainer
import ua.acclorite.book_story.presentation.core.components.common.SpacedItem
import ua.acclorite.book_story.presentation.core.util.LocalActivity
import ua.acclorite.book_story.presentation.core.util.noRippleClickable
import ua.acclorite.book_story.presentation.core.util.showToast
import ua.acclorite.book_story.ui.reader.ReaderEvent

@Composable
fun ReaderLayout(
    text: List<ReaderText>,
    listState: LazyListState,
    contentPadding: PaddingValues,
    verticalPadding: Dp,
    horizontalGesture: ReaderHorizontalGesture,
    horizontalGestureScroll: Float,
    horizontalGestureSensitivity: Dp,
    horizontalGestureAlphaAnim: Boolean,
    horizontalGesturePullAnim: Boolean,
    highlightedReading: Boolean,
    highlightedReadingThickness: FontWeight,
    progress: String,
    progressBar: Boolean,
    progressBarPadding: Dp,
    progressBarAlignment: HorizontalAlignment,
    progressBarFontSize: TextUnit,
    paragraphHeight: Dp,
    sidePadding: Dp,
    backgroundColor: Color,
    fontColor: Color,
    images: Boolean,
    imagesCornersRoundness: Dp,
    imagesAlignment: HorizontalAlignment,
    imagesWidth: Float,
    imagesColorEffects: ColorFilter?,
    fontFamily: FontWithName,
    lineHeight: TextUnit,
    fontThickness: ReaderFontThickness,
    fontStyle: FontStyle,
    chapterTitleAlignment: ReaderTextAlignment,
    textAlignment: ReaderTextAlignment,
    horizontalAlignment: Alignment.Horizontal,
    fontSize: TextUnit,
    letterSpacing: TextUnit,
    paragraphIndentation: TextUnit,
    doubleClickTranslation: Boolean,
    fullscreenMode: Boolean,
    isLoading: Boolean,
    showMenu: Boolean,
    menuVisibility: (ReaderEvent.OnMenuVisibility) -> Unit,
    openShareApp: (ReaderEvent.OnOpenShareApp) -> Unit,
    openWebBrowser: (ReaderEvent.OnOpenWebBrowser) -> Unit,
    openTranslator: (ReaderEvent.OnOpenTranslator) -> Unit,
    openDictionary: (ReaderEvent.OnOpenDictionary) -> Unit
) {
    val activity = LocalActivity.current
    SelectionContainer(
        onCopyRequested = {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
                activity.getString(R.string.copied)
                    .showToast(context = activity, longToast = false)
            }
        },
        onShareRequested = { textToShare ->
            openShareApp(
                ReaderEvent.OnOpenShareApp(
                    textToShare = textToShare,
                    activity = activity
                )
            )
        },
        onWebSearchRequested = { textToSearch ->
            openWebBrowser(
                ReaderEvent.OnOpenWebBrowser(
                    textToSearch = textToSearch,
                    activity = activity
                )
            )
        },
        onTranslateRequested = { textToTranslate ->
            openTranslator(
                ReaderEvent.OnOpenTranslator(
                    textToTranslate = textToTranslate,
                    translateWholeParagraph = false,
                    activity = activity
                )
            )
        },
        onDictionaryRequested = { textToDefine ->
            openDictionary(
                ReaderEvent.OnOpenDictionary(
                    textToDefine,
                    activity = activity
                )
            )
        }
    ) { toolbarHidden ->
        Column(
            Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .then(
                    if (!isLoading && toolbarHidden) {
                        Modifier.noRippleClickable(
                            onClick = {
                                menuVisibility(
                                    ReaderEvent.OnMenuVisibility(
                                        show = !showMenu,
                                        fullscreenMode = fullscreenMode,
                                        saveCheckpoint = true,
                                        activity = activity
                                    )
                                )
                            }
                        )
                    } else Modifier
                )
                .padding(contentPadding)
                .padding(vertical = verticalPadding)
                .readerHorizontalGesture(
                    listState = listState,
                    text = text,
                    horizontalGesture = horizontalGesture,
                    horizontalGestureScroll = horizontalGestureScroll,
                    horizontalGestureSensitivity = horizontalGestureSensitivity,
                    horizontalGestureAlphaAnim = horizontalGestureAlphaAnim,
                    horizontalGesturePullAnim = horizontalGesturePullAnim,
                    isLoading = isLoading
                )
        ) {
            // Переключаемся между обычным режимом и Pages режимом
            if (horizontalGesture == ReaderHorizontalGesture.PAGES) {
                // Pages режим
                val configuration = LocalConfiguration.current
                val screenWidth = configuration.screenWidthDp
                val screenHeight = configuration.screenHeightDp
                
                var pages by remember(horizontalGesture) { mutableStateOf<List<Page>?>(null) }
                var isLoadingPages by remember(horizontalGesture) { mutableStateOf(true) }
                
                LaunchedEffect(text, screenWidth, screenHeight, fontSize, lineHeight, sidePadding, paragraphHeight, fontFamily, fontThickness, fontStyle, textAlignment, letterSpacing, paragraphIndentation, contentPadding, verticalPadding) {
                    Log.d("READER_LAYOUT", "=== Starting Pages mode calculation ===")
                    Log.d("READER_LAYOUT", "Text items: ${text.size}")
                    Log.d("READER_LAYOUT", "Screen: ${screenWidth}x${screenHeight}")
                    
                    // Не запускаем расчет если текст пустой
                    if (text.isEmpty()) {
                        Log.d("READER_LAYOUT", "Text is empty, skipping page calculation")
                        pages = emptyList()
                        isLoadingPages = false
                        return@LaunchedEffect
                    }
                    
                    isLoadingPages = true
                    try {
                        Log.d("READER_LAYOUT", "Creating PageCalculator...")
                        val calculator = PageCalculator()
                        
                        Log.d("READER_LAYOUT", "Calling calculatePages...")
                        val calculatedPages = calculator.calculatePages(
                            text = text,
                            screenWidth = screenWidth,
                            screenHeight = screenHeight,
                            fontSize = fontSize,
                            lineHeight = lineHeight,
                            sidePadding = sidePadding,
                            paragraphHeight = paragraphHeight,
                            fontFamily = fontFamily,
                            fontThickness = fontThickness,
                            fontStyle = fontStyle,
                            textAlignment = textAlignment,
                            letterSpacing = letterSpacing,
                            paragraphIndentation = paragraphIndentation,
                            contentPadding = contentPadding,
                            verticalPadding = verticalPadding
                        )
                        
                        Log.d("READER_LAYOUT", "Pages calculation completed: ${calculatedPages.size} pages")
                        pages = calculatedPages
                    } catch (e: Exception) {
                        Log.e("READER_LAYOUT", "Error calculating pages: ${e.message}", e)
                        // В случае ошибки возвращаемся к обычному режиму
                        pages = emptyList()
                    } finally {
                        Log.d("READER_LAYOUT", "Pages calculation finished")
                        isLoadingPages = false
                    }
                }
                
                val currentPages = pages
                if (isLoadingPages || currentPages == null) {
                    Log.d("READER_LAYOUT", "Showing loading indicator. isLoadingPages: $isLoadingPages, pages: ${currentPages?.size}")
                    // Показываем индикатор загрузки
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        // TODO: Добавить индикатор загрузки
                    }
                } else if (currentPages.isNotEmpty()) {
                    Log.d("READER_LAYOUT", "Showing ReaderPagesLayout with ${currentPages.size} pages")
                    ReaderPagesLayout(
                        pages = currentPages,
                        activity = activity,
                        fontFamily = fontFamily,
                        fontColor = fontColor,
                        lineHeight = lineHeight,
                        fontThickness = fontThickness,
                        fontStyle = fontStyle,
                        textAlignment = textAlignment,
                        fontSize = fontSize,
                        letterSpacing = letterSpacing,
                        sidePadding = sidePadding,
                        paragraphIndentation = paragraphIndentation,
                        paragraphHeight = paragraphHeight,
                        contentPadding = contentPadding,
                        verticalPadding = verticalPadding,
                        onPageChanged = { /* TODO: Handle page change */ },
                        showMenu = showMenu,
                        fullscreenMode = fullscreenMode,
                        onMenuVisibility = menuVisibility,
                        highlightedReading = highlightedReading,
                        highlightedReadingThickness = highlightedReadingThickness
                    )
                } else {
                    Log.d("READER_LAYOUT", "Pages calculation failed, showing fallback LazyColumn")
                    // Если страницы не удалось рассчитать, показываем обычный режим
                    LazyColumnWithScrollbar(
                        state = listState,
                        enableScrollbar = false,
                        parentModifier = Modifier.weight(1f),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            top = (WindowInsets.displayCutout.asPaddingValues()
                                .calculateTopPadding() + paragraphHeight)
                                .coerceAtLeast(18.dp),
                            bottom = (WindowInsets.displayCutout.asPaddingValues()
                                .calculateBottomPadding() + paragraphHeight)
                                .coerceAtLeast(18.dp),
                        )
                    ) {
                        itemsIndexed(
                            text,
                            key = { index, _ -> index }
                        ) { index, entry ->
                            when {
                                !images && entry is ReaderText.Image -> return@itemsIndexed
                                else -> {
                                    SpacedItem(
                                        index = index,
                                        spacing = paragraphHeight
                                    ) {
                                        ReaderLayoutText(
                                            activity = activity,
                                            showMenu = showMenu,
                                            entry = entry,
                                            imagesCornersRoundness = imagesCornersRoundness,
                                            imagesAlignment = imagesAlignment,
                                            imagesWidth = imagesWidth,
                                            imagesColorEffects = imagesColorEffects,
                                            fontFamily = fontFamily,
                                            fontColor = fontColor,
                                            lineHeight = lineHeight,
                                            fontThickness = fontThickness,
                                            fontStyle = fontStyle,
                                            chapterTitleAlignment = chapterTitleAlignment,
                                            textAlignment = textAlignment,
                                            horizontalAlignment = horizontalAlignment,
                                            fontSize = fontSize,
                                            letterSpacing = letterSpacing,
                                            sidePadding = sidePadding,
                                            paragraphIndentation = paragraphIndentation,
                                            fullscreenMode = fullscreenMode,
                                            doubleClickTranslation = doubleClickTranslation,
                                            highlightedReading = highlightedReading,
                                            highlightedReadingThickness = highlightedReadingThickness,
                                            toolbarHidden = toolbarHidden,
                                            openTranslator = openTranslator,
                                            menuVisibility = menuVisibility
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Обычный режим с LazyColumn
                LazyColumnWithScrollbar(
                    state = listState,
                    enableScrollbar = false,
                    parentModifier = Modifier.weight(1f),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = (WindowInsets.displayCutout.asPaddingValues()
                            .calculateTopPadding() + paragraphHeight)
                            .coerceAtLeast(18.dp),
                        bottom = (WindowInsets.displayCutout.asPaddingValues()
                            .calculateBottomPadding() + paragraphHeight)
                            .coerceAtLeast(18.dp),
                    )
                ) {
                itemsIndexed(
                    text,
                    key = { index, _ -> index }
                ) { index, entry ->
                    when {
                        !images && entry is ReaderText.Image -> return@itemsIndexed
                        else -> {
                            SpacedItem(
                                index = index,
                                spacing = paragraphHeight
                            ) {
                                ReaderLayoutText(
                                    activity = activity,
                                    showMenu = showMenu,
                                    entry = entry,
                                    imagesCornersRoundness = imagesCornersRoundness,
                                    imagesAlignment = imagesAlignment,
                                    imagesWidth = imagesWidth,
                                    imagesColorEffects = imagesColorEffects,
                                    fontFamily = fontFamily,
                                    fontColor = fontColor,
                                    lineHeight = lineHeight,
                                    fontThickness = fontThickness,
                                    fontStyle = fontStyle,
                                    chapterTitleAlignment = chapterTitleAlignment,
                                    textAlignment = textAlignment,
                                    horizontalAlignment = horizontalAlignment,
                                    fontSize = fontSize,
                                    letterSpacing = letterSpacing,
                                    sidePadding = sidePadding,
                                    paragraphIndentation = paragraphIndentation,
                                    fullscreenMode = fullscreenMode,
                                    doubleClickTranslation = doubleClickTranslation,
                                    highlightedReading = highlightedReading,
                                    highlightedReadingThickness = highlightedReadingThickness,
                                    toolbarHidden = toolbarHidden,
                                    openTranslator = openTranslator,
                                    menuVisibility = menuVisibility
                                )
                            }
                        }
                    }
                }
            }
            }

            AnimatedVisibility(
                visible = !showMenu && progressBar,
                enter = slideInVertically { it } + expandVertically(),
                exit = slideOutVertically { it } + shrinkVertically()
            ) {
                ReaderProgressBar(
                    progress = progress,
                    progressBarPadding = progressBarPadding,
                    progressBarAlignment = progressBarAlignment,
                    progressBarFontSize = progressBarFontSize,
                    fontColor = fontColor,
                    sidePadding = sidePadding
                )
            }
        }
    }
}