/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.data.parser.epub

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import ua.acclorite.book_story.R
import ua.acclorite.book_story.data.parser.FileParser
import ua.acclorite.book_story.domain.file.CachedFile
import ua.acclorite.book_story.domain.library.book.Book
import ua.acclorite.book_story.domain.library.book.BookWithCover
import ua.acclorite.book_story.domain.library.category.Category
import ua.acclorite.book_story.domain.ui.UIText
import java.io.File
import java.io.FileInputStream
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream
import javax.inject.Inject

class EpubFileParser @Inject constructor() : FileParser {

    override suspend fun parse(cachedFile: CachedFile): BookWithCover? {
        return try {
            var book: BookWithCover? = null

            val rawFile = cachedFile.rawFile
            if (rawFile == null || !rawFile.exists() || !rawFile.canRead()) return null
            
            // Run diagnostics first
            val diagnosticResult = EpubDiagnostics.diagnoseEpub(cachedFile)
            if (!diagnosticResult.isValid) {
                Log.e("EPUB_PARSER", "EPUB diagnostics failed for ${cachedFile.name}: ${diagnosticResult.issues}")
                
                // Check if it's a duplicate entries issue
                if (diagnosticResult.issues.any { it.contains("Duplicate entries") }) {
                    Log.i("EPUB_PARSER", "Attempting to parse EPUB with duplicate handling: ${cachedFile.name}")
                    return tryParseWithDuplicateHandling(cachedFile)
                }
                
                // Run detailed test for debugging
                EpubTestUtils.testEpubFile(cachedFile)
                return null
            }
            
            Log.d("EPUB_PARSER", "EPUB diagnostics passed for ${cachedFile.name}")

            withContext(Dispatchers.IO) {
                try {
                    ZipFile(rawFile).use { zip ->
                    // Log EPUB structure for debugging
                    val entries = zip.entries().asSequence().toList()
                    Log.d("EPUB_PARSER", "EPUB entries count: ${entries.size}")
                    Log.d("EPUB_PARSER", "EPUB entries: ${entries.map { it.name }.take(10)}")
                    
                    val opfEntry = entries.find { entry ->
                        entry.name.endsWith(".opf", ignoreCase = true)
                    }
                    
                    if (opfEntry == null) {
                        Log.e("EPUB_PARSER", "No OPF file found in EPUB: ${cachedFile.name}")
                        return@withContext
                    }
                    
                    Log.d("EPUB_PARSER", "Found OPF file: ${opfEntry.name}")

                    val opfContent = zip
                        .getInputStream(opfEntry)
                        .bufferedReader()
                        .use { it.readText() }
                    val document = Jsoup.parse(opfContent)

                    val title = document.select("metadata > dc|title").text().trim().run {
                        ifBlank {
                            cachedFile.name.substringBeforeLast(".").trim()
                        }
                    }

                    val author = document.select("metadata > dc|creator").text().trim().run {
                        if (isBlank()) {
                            UIText.StringResource(R.string.unknown_author)
                        } else {
                            UIText.StringValue(this)
                        }
                    }

                    val description = Jsoup.parse(
                        document.select("metadata > dc|description").text()
                    ).text().run {
                        ifBlank {
                            null
                        }
                    }

                    val coverImage = document
                        .select("metadata > meta[name=cover]")
                        .attr("content")
                        .run {
                            if (isNotBlank()) {
                                document
                                    .select("manifest > item[id=$this]")
                                    .attr("href")
                                    .apply { if (isNotBlank()) return@run this }
                            }

                            document
                                .select("manifest > item[media-type*=image]")
                                .firstOrNull()?.attr("href")
                        }

                    book = BookWithCover(
                        book = Book(
                            title = title,
                            author = author,
                            description = description,
                            scrollIndex = 0,
                            scrollOffset = 0,
                            progress = 0f,
                            filePath = cachedFile.path,
                            lastOpened = null,
                            category = Category.entries[0],
                            coverImage = null
                        ),
                        coverImage = extractCoverImageBitmap(rawFile, coverImage)
                    )
                }
            }
            book
        } catch (e: Exception) {
            Log.e("EPUB_PARSER", "Failed to parse EPUB: ${cachedFile.name}", e)
            when (e) {
                is java.util.zip.ZipException -> {
                    Log.e("EPUB_PARSER", "Invalid ZIP structure in EPUB: ${cachedFile.name}")
                }
                is org.jsoup.UnsupportedMimeTypeException -> {
                    Log.e("EPUB_PARSER", "Unsupported MIME type in EPUB: ${cachedFile.name}")
                }
                is java.io.IOException -> {
                    Log.e("EPUB_PARSER", "IO error reading EPUB: ${cachedFile.name}", e)
                }
                is java.lang.SecurityException -> {
                    Log.e("EPUB_PARSER", "Security error accessing EPUB: ${cachedFile.name}", e)
                }
                else -> {
                    Log.e("EPUB_PARSER", "Unknown error parsing EPUB: ${cachedFile.name}", e)
                }
            }
            null
        }
    }

    private fun extractCoverImageBitmap(file: File, coverImagePath: String?): Bitmap? {
        if (coverImagePath.isNullOrBlank()) {
            return null
        }

        ZipFile(file).use { zip ->
            zip.entries().asSequence().forEach { entry ->
                if (entry.name.endsWith(coverImagePath)) {
                    val imageBytes = zip.getInputStream(entry).readBytes()
                    return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                }
            }
        }

        return null
    }
    
    /**
     * Attempts to parse EPUB with duplicate entries using alternative method.
     */
    private suspend fun tryParseWithDuplicateHandling(cachedFile: CachedFile): BookWithCover? {
        return try {
            val parseResult = EpubDuplicateHandler.tryParseWithDuplicates(cachedFile)
            if (!parseResult.success) {
                Log.e("EPUB_PARSER", "Duplicate handling failed: ${parseResult.message}")
                return null
            }
            
            Log.i("EPUB_PARSER", "Successfully parsed EPUB with duplicate handling: ${cachedFile.name}")
            
            // Find OPF file
            val opfEntry = parseResult.entries.find { entry ->
                entry.name.endsWith(".opf", ignoreCase = true)
            } ?: return null
            
            // Get OPF content
            val opfContent = if (parseResult.entryData.containsKey(opfEntry.name)) {
                String(parseResult.entryData[opfEntry.name]!!)
            } else {
                // Fallback: read from file
                val rawFile = cachedFile.rawFile ?: return null
                FileInputStream(rawFile).use { fis ->
                    ZipInputStream(fis).use { zis ->
                        var entry: ZipEntry? = zis.nextEntry
                        while (entry != null) {
                            if (entry.name == opfEntry.name) {
                                return@use zis.readBytes().toString(Charsets.UTF_8)
                            }
                            zis.closeEntry()
                            entry = zis.nextEntry
                        }
                        return null
                    }
                }
            }
            
            // Parse OPF content
            val document = Jsoup.parse(opfContent)
            
            val title = document.select("metadata > dc|title").text().trim().run {
                ifBlank {
                    cachedFile.name.substringBeforeLast(".").trim()
                }
            }
            
            val author = document.select("metadata > dc|creator").text().trim().run {
                ifBlank {
                    "Unknown Author"
                }
            }
            
            val description = document.select("metadata > dc|description").text().trim()
            
            val coverImage = document.select("metadata > meta[name=cover]").attr("content")
                .takeIf { it.isNotBlank() }
                ?: document.select("metadata > meta[property=cover]").attr("content")
                .takeIf { it.isNotBlank() }
            
            BookWithCover(
                book = Book(
                    title = title,
                    author = UIText.StringResource(author),
                    description = description,
                    scrollIndex = 0,
                    scrollOffset = 0,
                    progress = 0f,
                    filePath = cachedFile.path,
                    lastOpened = null,
                    category = Category.entries[0],
                    coverImage = null
                ),
                coverImage = null // Skip cover extraction for now to avoid complexity
            )
            
        } catch (e: Exception) {
            Log.e("EPUB_PARSER", "Error in duplicate handling: ${cachedFile.name}", e)
            null
        }
    }
}