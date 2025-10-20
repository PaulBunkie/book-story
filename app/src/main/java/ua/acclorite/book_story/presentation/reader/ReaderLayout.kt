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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
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
    filePath: String,
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
    openDictionary: (ReaderEvent.OnOpenDictionary) -> Unit,
    updatePagesProgress: (currentPage: Int, totalPages: Int, currentElementIndex: Int) -> Unit
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
            // Переключаемся между обычным режимом, Pages режимом и Experimental режимом
            if (horizontalGesture == ReaderHorizontalGesture.PAGES) {
                // Pages режим с ленивой загрузкой
                val configuration = LocalConfiguration.current
                val density = LocalDensity.current
                val screenWidth = (configuration.screenWidthDp * density.density).toInt()
                val screenHeight = (configuration.screenHeightDp * density.density).toInt()
                val scope = rememberCoroutineScope()
                
                // Кэш страниц и текущая страница
                val pageCache: androidx.compose.runtime.snapshots.SnapshotStateMap<Int, Page> = remember(
                    filePath,
                    fontSize,
                    lineHeight,
                    sidePadding,
                    paragraphHeight,
                    fontFamily,
                    fontThickness,
                    fontStyle,
                    textAlignment,
                    letterSpacing,
                    paragraphIndentation,
                    contentPadding,
                    verticalPadding
                ) { 
                    mutableStateMapOf<Int, Page>() 
                }
                
                var currentPage by remember { mutableStateOf(0) }
                var lastCalculatedElement by remember { mutableStateOf(0) }
                var estimatedTotalPages by remember { mutableStateOf(0) }
                
                // Функция для загрузки страниц вокруг текущей
                fun loadPagesAround(targetPage: Int) {
                    val window = 2
                    val neededRange = (targetPage - window).coerceAtLeast(0) until (targetPage + window + 1)
                    
                    // Проверяем какие страницы уже есть
                    val missing = neededRange.filter { !pageCache.containsKey(it) }
                    if (missing.isEmpty()) return
                    
                    Log.d("READER_LAYOUT", "Loading pages around $targetPage: $missing")
                    
                    // Определяем с какого элемента начинать расчет
                    val startElement = if (targetPage > 0 && pageCache.containsKey(targetPage - 1)) {
                        pageCache[targetPage - 1]!!.endIndex + 1
                    } else {
                        lastCalculatedElement
                    }
                    
                    if (startElement >= text.size) return
                    
                    // Здесь будем вызывать calculatePageRangeComposable
                    // Но для этого нужен отдельный composable, так что пока пропустим
                }
                
                // Инициализация - загружаем первые 5 страниц
                if (text.isNotEmpty() && pageCache.isEmpty()) {
                    LazyPageLayoutMeasurer(
                        bookId = 0,
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
                        verticalPadding = verticalPadding,
                        fontColor = fontColor,
                        highlightedReading = highlightedReading,
                        highlightedReadingThickness = highlightedReadingThickness,
                        imagesCornersRoundness = imagesCornersRoundness,
                        imagesAlignment = imagesAlignment,
                        imagesWidth = imagesWidth,
                        imagesColorEffects = imagesColorEffects,
                        initialPagesCount = 5, // Начальные 5 страниц
                        onPagesCalculated = { calculatedPages ->
                            Log.d("READER_LAYOUT", "Initial pages calculated: ${calculatedPages.size}")
                            calculatedPages.forEachIndexed { index, page ->
                                pageCache[index] = page
                                if (index == calculatedPages.size - 1) {
                                    lastCalculatedElement = page.endIndex + 1
                                }
                            }
                        },
                        onTotalPagesEstimate = { estimatedTotal ->
                            Log.d("READER_LAYOUT", "Estimated total pages: $estimatedTotal")
                            estimatedTotalPages = estimatedTotal
                        }
                    )
                }
                
                // Собираем список страниц для HorizontalPager
                val pages = (0 until pageCache.size).mapNotNull { pageCache[it] }
                
                // Флаг для запроса догрузки страниц
                var requestLoadMore by remember { mutableStateOf(0) }
                
                // Автоматическая догрузка страниц ПОСЛЕ перелистывания
                LaunchedEffect(currentPage, pages.size, estimatedTotalPages) {
                    // Обновляем прогресс чтения
                    if (pages.isNotEmpty() && currentPage < pages.size) {
                        val currentPageData = pages.getOrNull(currentPage)
                        val currentElementIndex = currentPageData?.startIndex ?: 0
                        val totalPages = if (estimatedTotalPages > 0) estimatedTotalPages else pages.size
                        updatePagesProgress(currentPage, totalPages, currentElementIndex)
                        Log.d("READER_LAYOUT", "Progress updated: page $currentPage/$totalPages, element $currentElementIndex/${text.size}")
                    }
                    
                    // Проверяем нужна ли догрузка
                    if (currentPage >= pages.size - 2 && lastCalculatedElement < text.size) {
                        Log.d("READER_LAYOUT", "Requesting load of next pages after scroll to page $currentPage")
                        requestLoadMore++ // Увеличиваем счетчик чтобы запустить новую загрузку
                    }
                }
                
                // Composable для догрузки страниц (запускается асинхронно)
                if (requestLoadMore > 0 && lastCalculatedElement < text.size) {
                    calculatePageRangeComposable(
                        text = text,
                        startElement = lastCalculatedElement,
                        pagesToCalculate = 5,
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
                        verticalPadding = verticalPadding,
                        fontColor = fontColor,
                        highlightedReading = highlightedReading,
                        highlightedReadingThickness = highlightedReadingThickness,
                        imagesCornersRoundness = imagesCornersRoundness,
                        imagesAlignment = imagesAlignment,
                        imagesWidth = imagesWidth,
                        imagesColorEffects = imagesColorEffects,
                        onPagesCalculated = { newPages ->
                            Log.d("READER_LAYOUT", "Loaded ${newPages.size} more pages")
                            val startIndex = pageCache.size
                            newPages.forEachIndexed { index, page ->
                                pageCache[startIndex + index] = page
                                if (index == newPages.size - 1) {
                                    lastCalculatedElement = page.endIndex + 1
                                }
                            }
                            requestLoadMore = 0 // Сбрасываем флаг после завершения
                        }
                    )
                }
                
                if (pages.isNotEmpty()) {
                    Log.d("READER_LAYOUT", "Showing ReaderPagesLayout with ${pages.size} pages")
                    ReaderPagesLayout(
                        pages = pages,
                        onPageChanged = { newPage ->
                            Log.d("READER_LAYOUT", "Page changed to: $newPage")
                            currentPage = newPage
                        },
                        activity = activity,
                        screenWidth = screenWidth,
                        screenHeight = screenHeight,
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
                        showMenu = showMenu,
                        fullscreenMode = fullscreenMode,
                        onMenuVisibility = menuVisibility,
                        highlightedReading = highlightedReading,
                        highlightedReadingThickness = highlightedReadingThickness,
                        imagesCornersRoundness = imagesCornersRoundness,
                        imagesAlignment = imagesAlignment,
                        imagesWidth = imagesWidth,
                        imagesColorEffects = imagesColorEffects
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
            } else if (horizontalGesture == ReaderHorizontalGesture.EXPERIMENTAL) {
                // Experimental режим с красными точками для обрезанных строк
                val configuration = LocalConfiguration.current
                val density = LocalDensity.current
                val screenWidth = (configuration.screenWidthDp * density.density).toInt()
                val screenHeight = (configuration.screenHeightDp * density.density).toInt()
                
                Box(modifier = Modifier.fillMaxSize()) {
                    // Вычисляем значения contentPadding для передачи в TextLineVisibilityDetector
                    val contentPaddingTopValue = (WindowInsets.displayCutout.asPaddingValues()
                        .calculateTopPadding() + paragraphHeight)
                        .coerceAtLeast(18.dp)
                    val contentPaddingBottomValue = (WindowInsets.displayCutout.asPaddingValues()
                        .calculateBottomPadding() + paragraphHeight)
                        .coerceAtLeast(18.dp)
                    
                    // Основной контент - обычный LazyColumn
                    LazyColumnWithScrollbar(
                        state = listState,
                        enableScrollbar = false,
                        parentModifier = Modifier.fillMaxSize(),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            top = contentPaddingTopValue,
                            bottom = contentPaddingBottomValue,
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
                    
                    // Детектируем обрезанные строки и показываем красные точки
                    val lineVisibilityState = TextLineVisibilityDetector(
                        listState = listState,
                        text = text,
                        screenHeight = screenHeight,
                        fontSize = fontSize,
                        lineHeight = lineHeight,
                        fontFamily = fontFamily,
                        fontThickness = fontThickness,
                        fontStyle = fontStyle,
                        textAlignment = textAlignment,
                        letterSpacing = letterSpacing,
                        sidePadding = sidePadding,
                        paragraphHeight = paragraphHeight,
                        contentPaddingTop = contentPaddingTopValue,
                        contentPaddingBottom = contentPaddingBottomValue
                    )
                    
                    // Красные точки-индикаторы
                    CutLineIndicators(
                        visibilityState = lineVisibilityState,
                        color = Color.Red
                    )
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