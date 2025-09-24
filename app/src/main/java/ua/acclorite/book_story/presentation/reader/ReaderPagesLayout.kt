/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.presentation.reader

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.toFontFamily
import android.graphics.Typeface
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import ua.acclorite.book_story.domain.reader.FontWithName
import ua.acclorite.book_story.domain.reader.ReaderFontThickness
import ua.acclorite.book_story.domain.reader.ReaderTextAlignment
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
    onPageChanged: (Int) -> Unit
) {
    Log.d("READER_PAGES_LAYOUT", "=== Creating ReaderPagesLayout ===")
    Log.d("READER_PAGES_LAYOUT", "Pages count: ${pages.size}")
    
    val density = LocalDensity.current

    AndroidView(
        factory = { context ->
            Log.d("READER_PAGES_LAYOUT", "Creating ViewPager2...")
            ViewPager2(context).apply {
                orientation = ViewPager2.ORIENTATION_HORIZONTAL
                
                
                Log.d("READER_PAGES_LAYOUT", "Creating PageAdapter...")
                adapter = PageAdapter(
                    pages = pages,
                    fontFamily = fontFamily,
                    fontColor = fontColor,
                    lineHeight = lineHeight,
                    fontThickness = fontThickness,
                    fontStyle = fontStyle,
                    textAlignment = textAlignment,
                    fontSize = fontSize,
                    letterSpacing = letterSpacing,
                    sidePadding = sidePadding,
                    paragraphIndentation = paragraphIndentation
                )
                Log.d("READER_PAGES_LAYOUT", "PageAdapter created successfully")

                registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                    override fun onPageSelected(position: Int) {
                        Log.d("READER_PAGES_LAYOUT", "Page selected: $position")
                        onPageChanged(position)
                    }
                    
                    override fun onPageScrollStateChanged(state: Int) {
                        val stateName = when (state) {
                            ViewPager2.SCROLL_STATE_IDLE -> "IDLE"
                            ViewPager2.SCROLL_STATE_DRAGGING -> "DRAGGING"
                            ViewPager2.SCROLL_STATE_SETTLING -> "SETTLING"
                            else -> "UNKNOWN"
                        }
                        Log.d("READER_PAGES_LAYOUT", "Page scroll state: $stateName")
                    }
                    
                    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                        Log.d("READER_PAGES_LAYOUT", "Page scrolled: position=$position, offset=$positionOffset")
                    }
                })
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

private class PageAdapter(
    private val pages: List<Page>,
    private val fontFamily: FontWithName,
    private val fontColor: Color,
    private val lineHeight: TextUnit,
    private val fontThickness: ReaderFontThickness,
    private val fontStyle: FontStyle,
    private val textAlignment: ReaderTextAlignment,
    private val fontSize: TextUnit,
    private val letterSpacing: TextUnit,
    private val sidePadding: Dp,
    private val paragraphIndentation: TextUnit
) : RecyclerView.Adapter<PageViewHolder>() {
    
    // Кэш для ViewHolder'ов чтобы избежать пересоздания
    private val viewHolderCache = mutableMapOf<Int, PageViewHolder>()
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        Log.d("PAGE_ADAPTER", "Creating ViewHolder for position: $viewType")
        val textView = TextView(parent.context).apply {
            setTextColor(fontColor.toArgb())
            textSize = fontSize.value
            typeface = Typeface.DEFAULT
            setPadding(
                sidePadding.value.toInt(),
                32,
                sidePadding.value.toInt(),
                32
            )
            // Устанавливаем параметры макета для заполнения всей области
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            
            // Настраиваем TextView для выделения текста
            setTextIsSelectable(true)
            movementMethod = android.text.method.LinkMovementMethod.getInstance()
        }

        val viewHolder = PageViewHolder(textView)
        viewHolderCache[viewType] = viewHolder
        return viewHolder
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        Log.d("PAGE_ADAPTER", "Binding ViewHolder for position: $position")
        if (position < pages.size) {
            holder.bind(pages[position])
        }
    }

    override fun getItemCount(): Int {
        Log.d("PAGE_ADAPTER", "Item count: ${pages.size}")
        return pages.size
    }
}

private class PageViewHolder(private val textView: TextView) : RecyclerView.ViewHolder(textView) {
    fun bind(page: Page) {
        Log.d("PAGE_VIEW_HOLDER", "Binding page content: ${page.content.take(50)}...")
        textView.text = page.content
    }
}
