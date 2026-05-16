/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.presentation.reader

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.runtime.key
import ua.acclorite.book_story.domain.reader.FontWithName
import ua.acclorite.book_story.domain.reader.ReaderFontThickness
import ua.acclorite.book_story.domain.reader.ReaderTextAlignment
import ua.acclorite.book_story.domain.reader.ReaderText
import ua.acclorite.book_story.ui.reader.ReaderEvent
import ua.acclorite.book_story.presentation.core.util.noRippleClickable
import ua.acclorite.book_story.presentation.core.components.common.SelectionContainer
import ua.acclorite.book_story.presentation.core.components.common.StyledText

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
    showMenu: Boolean,
    fullscreenMode: Boolean,
    onMenuVisibility: (ReaderEvent.OnMenuVisibility) -> Unit,
    highlightedReading: Boolean,
    highlightedReadingThickness: FontWeight,
    imagesCornersRoundness: Dp,
    imagesAlignment: ua.acclorite.book_story.domain.util.HorizontalAlignment,
    imagesWidth: Float,
    imagesColorEffects: ColorFilter?
) {
    // Сбрасываем состояние пейджера при изменении настроек (так как кэш пересчитывается с нуля)
    val pagerState = key(screenWidth, screenHeight, fontFamily, fontSize, lineHeight, sidePadding) {
        rememberPagerState(
            initialPage = 0,
            pageCount = { pages.size }
        )
    }
    
    LaunchedEffect(pagerState.currentPage) {
        onPageChanged(pagerState.currentPage)
    }
    
    SelectionContainer(
        onCopyRequested = {
            if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.S_V2) {
                // copied logic
            }
        },
        onShareRequested = { },
        onWebSearchRequested = { },
        onTranslateRequested = { },
        onDictionaryRequested = { }
    ) { toolbarHidden ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (toolbarHidden) {
                        Modifier.noRippleClickable(
                            onClick = {
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
                    } else Modifier
                )
        ) { pageIndex ->
            if (pageIndex < pages.size) {
                val page = pages[pageIndex]

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = sidePadding)
                ) {
                    var isFirstElement = true
                    
                    for (readerText in page.content) {
                        if (!isFirstElement) {
                            Spacer(modifier = Modifier.height(paragraphHeight))
                        }
                        isFirstElement = false
                        
                        when (readerText) {
                            is ReaderText.Text -> {
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
                                        lineBreak = LineBreak.Paragraph,
                                        textDirection = TextDirection.Content,
                                        platformStyle = PlatformTextStyle(includeFontPadding = false),
                                        lineHeightStyle = LineHeightStyle(
                                            alignment = LineHeightStyle.Alignment.Center,
                                            trim = LineHeightStyle.Trim.None
                                        )
                                    ),
                                    highlightText = highlightedReading,
                                    highlightThickness = highlightedReadingThickness,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            
                            is ReaderText.Chapter -> {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Spacer(modifier = Modifier.height(22.dp))
                                    
                                    StyledText(
                                        text = buildAnnotatedString { append(readerText.title) },
                                        modifier = Modifier.fillMaxWidth(),
                                        style = (if (!readerText.nested) MaterialTheme.typography.headlineMedium
                                        else MaterialTheme.typography.headlineSmall)
                                            .copy(
                                                color = fontColor,
                                                textAlign = textAlignment.textAlignment,
                                                textDirection = TextDirection.Content,
                                                platformStyle = PlatformTextStyle(includeFontPadding = false),
                                                lineHeightStyle = LineHeightStyle(
                                                    alignment = LineHeightStyle.Alignment.Center,
                                                    trim = LineHeightStyle.Trim.None
                                                )
                                            ),
                                        highlightText = highlightedReading,
                                        highlightThickness = highlightedReadingThickness
                                    )
                                    
                                    Spacer(modifier = Modifier.height(16.dp))
                                    HorizontalDivider(color = fontColor.copy(0.4f))
                                    Spacer(modifier = Modifier.height(16.dp))
                                }
                            }
                            
                            is ReaderText.Separator -> {
                                HorizontalDivider(
                                    thickness = 3.dp,
                                    modifier = Modifier.clip(CircleShape),
                                    color = fontColor.copy(0.3f)
                                )
                            }
                            
                            is ReaderText.Image -> {
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = imagesAlignment.alignment
                                ) {
                                    Image(
                                        modifier = Modifier
                                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(imagesCornersRoundness))
                                            .fillMaxWidth(imagesWidth),
                                        bitmap = readerText.imageBitmap,
                                        contentDescription = null,
                                        colorFilter = imagesColorEffects,
                                        contentScale = androidx.compose.ui.layout.ContentScale.FillWidth
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
