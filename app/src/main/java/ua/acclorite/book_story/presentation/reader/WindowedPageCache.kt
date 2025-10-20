/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.presentation.reader

import androidx.compose.runtime.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Manages a windowed cache of pages for lazy loading
 * Only keeps pages in memory that are within +/-2 of current page
 */
class WindowedPageCache(
    private val scope: CoroutineScope,
    private val totalElements: Int,
    private val calculatePageRange: suspend (startElement: Int, pagesToCalculate: Int) -> List<Page>
) {
    // Кэш страниц: ключ = номер страницы (0-based), значение = Page
    private val _pages = mutableStateMapOf<Int, Page>()
    val pages: Map<Int, Page> get() = _pages
    
    // Текущая задача расчета (чтобы можно было отменить)
    private var calculationJob: Job? = null
    
    // Запомним последний элемент, до которого мы посчитали
    private var lastCalculatedElement = 0
    
    // Оценка общего количества страниц
    var estimatedTotalPages by mutableStateOf(0)
        private set
    
    /**
     * Убедиться что страницы вокруг currentPage посчитаны
     * @param currentPage текущая страница (0-based)
     * @param window сколько страниц вперед и назад держать (по умолчанию 2)
     */
    fun ensurePagesAround(currentPage: Int, window: Int = 2) {
        // Какие страницы нам нужны
        val neededRange = (currentPage - window).coerceAtLeast(0) until (currentPage + window + 1)
        
        // Какие уже есть
        val existingPages = neededRange.filter { _pages.containsKey(it) }
        
        // Если все есть - ничего не делаем
        if (existingPages.size == neededRange.count()) {
            return
        }
        
        // Нужно посчитать недостающие
        calculateMissingPages(neededRange)
    }
    
    /**
     * Запустить расчет недостающих страниц
     */
    private fun calculateMissingPages(neededRange: IntRange) {
        // Отменяем предыдущий расчет если он еще идет
        calculationJob?.cancel()
        
        calculationJob = scope.launch {
            // Найдем первую недостающую страницу
            val firstMissing = neededRange.firstOrNull { !_pages.containsKey(it) } ?: return@launch
            
            // Определим с какого элемента начинать
            val startElement = if (firstMissing > 0 && _pages.containsKey(firstMissing - 1)) {
                // Если предыдущая страница есть - начинаем со следующего элемента
                _pages[firstMissing - 1]!!.endIndex + 1
            } else if (firstMissing == 0) {
                0
            } else {
                // Иначе используем оценку
                val avgElementsPerPage = if (_pages.isNotEmpty()) {
                    _pages.values.sumOf { it.content.size } / _pages.size
                } else 15 // начальная оценка
                
                firstMissing * avgElementsPerPage
            }
            
            if (startElement >= totalElements) {
                return@launch // Уже все посчитали
            }
            
            // Сколько страниц считать
            val pagesToCalculate = neededRange.count { !_pages.containsKey(it) }.coerceAtLeast(1)
            
            // Запускаем расчет
            val newPages = calculatePageRange(startElement, pagesToCalculate)
            
            // Добавляем в кэш
            var pageIndex = firstMissing
            for (page in newPages) {
                _pages[pageIndex] = page
                pageIndex++
                lastCalculatedElement = page.endIndex
            }
            
            // Обновляем оценку общего количества страниц
            updateEstimate()
        }
    }
    
    /**
     * Обновить оценку общего количества страниц
     */
    private fun updateEstimate() {
        if (_pages.isEmpty()) return
        
        val avgElementsPerPage = _pages.values.sumOf { it.content.size } / _pages.size.toFloat()
        estimatedTotalPages = (totalElements / avgElementsPerPage).toInt().coerceAtLeast(_pages.size)
    }
    
    /**
     * Очистить кэш (при смене книги/настроек)
     */
    fun clear() {
        calculationJob?.cancel()
        _pages.clear()
        lastCalculatedElement = 0
        estimatedTotalPages = 0
    }
    
    /**
     * Получить список страниц для отображения в HorizontalPager
     * Возвращает последовательный список от 0 до максимальной посчитанной страницы
     */
    fun getPagesList(): List<Page?> {
        if (_pages.isEmpty()) return emptyList()
        val maxPage = _pages.keys.maxOrNull() ?: return emptyList()
        return (0..maxPage).map { _pages[it] }
    }
}

