/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

@file:OptIn(ExperimentalCoroutinesApi::class)

package ua.acclorite.book_story.data.parser.epub

import android.util.Log
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import org.jsoup.Jsoup
import ua.acclorite.book_story.data.parser.DocumentParser
import ua.acclorite.book_story.data.parser.TextParser
import ua.acclorite.book_story.domain.file.CachedFile
import ua.acclorite.book_story.domain.reader.ReaderText
import ua.acclorite.book_story.presentation.core.constants.provideImageExtensions
import ua.acclorite.book_story.presentation.core.util.addAll
import ua.acclorite.book_story.presentation.core.util.containsVisibleText
import java.io.File
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import javax.inject.Inject

private const val EPUB_TAG = "EPUB Parser"
private typealias Source = String

private val dispatcher = Dispatchers.IO.limitedParallelism(3)

class EpubTextParser @Inject constructor(
    private val documentParser: DocumentParser
) : TextParser {

    override suspend fun parse(cachedFile: CachedFile): List<ReaderText> {
        Log.i(EPUB_TAG, "Started EPUB parsing: ${cachedFile.name}.")

        return try {
            yield()
            var readerText = listOf<ReaderText>()

            val rawFile = cachedFile.rawFile
            if (rawFile == null || !rawFile.exists() || !rawFile.canRead()) return emptyList()

            withContext(Dispatchers.IO) {
                try {
                    ZipFile(rawFile).use { zip ->
                        val tocEntry = zip.entries().toList().find { entry ->
                            entry.name.endsWith(".ncx", ignoreCase = true)
                        }
                        val opfEntry = zip.entries().toList().find { entry ->
                            entry.name.endsWith(".opf", ignoreCase = true)
                        }

                        val chapterEntries = zip.getChapterEntries(opfEntry)
                        val imageEntries = zip.entries().toList().filter {
                            provideImageExtensions().any { format ->
                                it.name.endsWith(format, ignoreCase = true)
                            }
                        }
                        val chapterTitleEntries = zip.getChapterTitleMapFromToc(tocEntry)

                        Log.i(EPUB_TAG, "TOC Entry: ${tocEntry?.name ?: "no toc.ncx"}")
                        Log.i(EPUB_TAG, "OPF Entry: ${opfEntry?.name ?: "no .opf entry"}")
                        Log.i(EPUB_TAG, "Chapter entries, size: ${chapterEntries.size}")
                        Log.i(EPUB_TAG, "Title entries, size: ${chapterTitleEntries?.size}")

                        readerText = zip.parseEpub(
                            chapterEntries = chapterEntries,
                            imageEntries = imageEntries,
                            chapterTitleEntries = chapterTitleEntries
                        )
                    }
                } catch (e: java.util.zip.ZipException) {
                    if (e.message?.contains("Duplicate entry") == true) {
                        Log.i(EPUB_TAG, "Duplicate entries detected, using alternative parsing: ${cachedFile.name}")
                        readerText = parseWithDuplicateHandling(cachedFile)
                    } else {
                        throw e
                    }
                }
            }

            yield()

            if (
                readerText.filterIsInstance<ReaderText.Text>().isEmpty() ||
                readerText.filterIsInstance<ReaderText.Chapter>().isEmpty()
            ) {
                Log.e(EPUB_TAG, "Could not extract text from EPUB.")
                return emptyList()
            }

            Log.i(EPUB_TAG, "Successfully finished EPUB parsing.")
            readerText
        } catch (e: Exception) {
            Log.e(EPUB_TAG, "Could not parse EPUB: ${cachedFile.name}.", e)
            when (e) {
                is java.util.zip.ZipException -> {
                    Log.e(EPUB_TAG, "Invalid ZIP structure in EPUB: ${cachedFile.name}")
                }
                is org.jsoup.UnsupportedMimeTypeException -> {
                    Log.e(EPUB_TAG, "Unsupported MIME type in EPUB: ${cachedFile.name}")
                }
                is java.io.IOException -> {
                    Log.e(EPUB_TAG, "IO error reading EPUB: ${cachedFile.name}", e)
                }
                is java.lang.SecurityException -> {
                    Log.e(EPUB_TAG, "Security error accessing EPUB: ${cachedFile.name}", e)
                }
                is java.nio.charset.MalformedInputException -> {
                    Log.e(EPUB_TAG, "Character encoding error in EPUB: ${cachedFile.name}", e)
                }
                else -> {
                    Log.e(EPUB_TAG, "Unknown error parsing EPUB: ${cachedFile.name}", e)
                }
            }
            emptyList()
        }
    }

    /**
     * Parses text and chapters from EPUB.
     * Uses toc.ncx(if present) to retrieve titles, otherwise uses first line as title.
     *
     * @param chapterTitleEntries Titles extracted from toc.ncx.
     * @param chapterEntries [ZipEntry]s to parse.
     *
     * @return Null if could not parse.
     */
    private suspend fun ZipFile.parseEpub(
        chapterEntries: List<ZipEntry>,
        imageEntries: List<ZipEntry>,
        chapterTitleEntries: Map<Source, ReaderText.Chapter>?
    ): List<ReaderText> {

        val readerText = mutableListOf<ReaderText>()
        withContext(Dispatchers.IO) {
            val unformattedText = ConcurrentLinkedQueue<Pair<Int, List<ReaderText>>>()

            // Asynchronously getting all chapters with text
            val jobs = chapterEntries.mapIndexed { index, entry ->
                async(dispatcher) {
                    yield()

                    unformattedText.parseZipEntry(
                        zip = this@parseEpub,
                        index = index,
                        entry = entry,
                        imageEntries = imageEntries,
                        chapterTitleMap = chapterTitleEntries
                    )

                    yield()
                }
            }
            jobs.awaitAll()

            // Sorting chapters in correct order
            readerText.addAll {
                unformattedText.toList()
                    .sortedBy { (index, _) -> index }
                    .map { it.second }
                    .flatten()
            }
        }

        return readerText
    }
    
    /**
     * Alternative parsing method for EPUB files with duplicate entries.
     */
    private suspend fun parseWithDuplicateHandling(cachedFile: CachedFile): List<ReaderText> {
        return try {
            val parseResult = EpubDuplicateHandler.tryParseWithDuplicates(cachedFile)
            if (parseResult == null || !parseResult.success) {
                Log.e(EPUB_TAG, "Duplicate handling failed: ${parseResult?.message ?: "null result"}")
                return emptyList()
            }
            
            Log.i(EPUB_TAG, "Successfully parsed EPUB text with duplicate handling: ${cachedFile.name}")
            
            // Find OPF and NCX files
            val opfEntry = parseResult.entries.find { entry ->
                entry.name.endsWith(".opf", ignoreCase = true)
            }
            val tocEntry = parseResult.entries.find { entry ->
                entry.name.endsWith(".ncx", ignoreCase = true)
            }
            
            if (opfEntry == null) {
                Log.e(EPUB_TAG, "No OPF file found in duplicate handling")
                return emptyList()
            }
            
            // Get OPF content
            val opfContent = if (parseResult.entryData.containsKey(opfEntry.name)) {
                String(parseResult.entryData[opfEntry.name]!!)
            } else {
                // Fallback: read from file
                val rawFile = cachedFile.rawFile ?: return emptyList()
                java.io.FileInputStream(rawFile).use { fis ->
                    java.util.zip.ZipInputStream(fis).use { zis ->
                        var entry: java.util.zip.ZipEntry? = zis.nextEntry
                        while (entry != null) {
                            if (entry.name == opfEntry.name) {
                                return@use zis.readBytes().toString(Charsets.UTF_8)
                            }
                            zis.closeEntry()
                            entry = zis.nextEntry
                        }
                        return emptyList()
                    }
                }
            }
            
            // Parse OPF content to get chapter entries
            val document = Jsoup.parse(opfContent)
            val chapterEntries = parseResult.entries.filter { entry ->
                entry.name.endsWith(".html", ignoreCase = true) || 
                entry.name.endsWith(".xhtml", ignoreCase = true)
            }
            
            val imageEntries = parseResult.entries.filter {
                provideImageExtensions().any { format ->
                    it.name.endsWith(format, ignoreCase = true)
                }
            }
            
            // Create a simple reader text list
            val readerText = mutableListOf<ReaderText>()
            
            // Add chapters
            chapterEntries.forEachIndexed { index, entry ->
                val content = if (parseResult.entryData.containsKey(entry.name)) {
                    String(parseResult.entryData[entry.name]!!)
                } else {
                    // Fallback: read from file
                    val rawFile = cachedFile.rawFile ?: return emptyList()
                    java.io.FileInputStream(rawFile).use { fis ->
                        java.util.zip.ZipInputStream(fis).use { zis ->
                            var zipEntry: java.util.zip.ZipEntry? = zis.nextEntry
                            while (zipEntry != null) {
                                if (zipEntry.name == entry.name) {
                                    return@use zis.readBytes().toString(Charsets.UTF_8)
                                }
                                zis.closeEntry()
                                zipEntry = zis.nextEntry
                            }
                            ""
                        }
                    }
                }
                
                val parsedContent = documentParser.parseDocument(
                    document = Jsoup.parse(content),
                    zipFile = null, // We can't use ZipFile here due to duplicates
                    imageEntries = imageEntries,
                    includeChapter = true
                )
                
                readerText.addAll(parsedContent)
            }
            
            readerText.toList()
            
        } catch (e: Exception) {
            Log.e(EPUB_TAG, "Error in duplicate handling for text parsing: ${cachedFile.name}", e)
            emptyList()
        }
    }

    /**
     * Parses [entry] to get it's text and chapter.
     * Adds parsed entry in [ConcurrentLinkedQueue].
     *
     * @param zip [ZipFile] of the [entry].
     * @param index Index of the [entry].
     * @param entry [ZipEntry].
     * @param chapterTitleMap Titles from [getChapterTitleMapFromToc].
     */
    private suspend fun ConcurrentLinkedQueue<Pair<Int, List<ReaderText>>>.parseZipEntry(
        zip: ZipFile,
        index: Int,
        entry: ZipEntry,
        imageEntries: List<ZipEntry>,
        chapterTitleMap: Map<Source, ReaderText.Chapter>?
    ) {
        // Getting all text
        val content = withContext(Dispatchers.IO) {
            zip.getInputStream(entry)
        }.bufferedReader().use { it.readText() }
        var readerText = documentParser.parseDocument(
            document = Jsoup.parse(content),
            zipFile = zip,
            imageEntries = imageEntries,
            includeChapter = false
        ).toMutableList()

        // Adding chapter title from TOC if found
        getChapterTitleFromToc(
            chapterSource = entry.name,
            chapterTitleMap = chapterTitleMap
        ).apply {
            val chapter = this ?: run {
                val firstVisibleText = readerText.firstOrNull { line ->
                    line is ReaderText.Text && line.line.text.containsVisibleText()
                } as? ReaderText.Text ?: return

                return@run ReaderText.Chapter(
                    title = firstVisibleText.line.text,
                    nested = false
                )
            }

            readerText = readerText.dropWhile { line ->
                (line is ReaderText.Text && line.line.text.lowercase() == chapter.title.lowercase())
            }.toMutableList()

            readerText.add(
                0,
                chapter
            )
        }

        if (
            readerText.filterIsInstance<ReaderText.Text>().isEmpty() ||
            readerText.filterIsInstance<ReaderText.Chapter>().isEmpty()
        ) {
            Log.w(EPUB_TAG, "Could not extract text from [${entry.name}].")
            return
        }

        add(index to readerText)
    }

    /**
     * Getting all titles from [tocEntry].
     *
     * @return null if [tocEntry] is null.
     */
    private suspend fun ZipFile.getChapterTitleMapFromToc(
        tocEntry: ZipEntry?
    ): Map<Source, ReaderText.Chapter>? {
        val tocContent = tocEntry?.let {
            withContext(Dispatchers.IO) {
                getInputStream(it)
            }.bufferedReader().use { it.readText() }
        }
        val tocDocument = tocContent?.let { Jsoup.parse(it) }

        if (tocDocument == null) return null
        val titleMap = mutableMapOf<Source, ReaderText.Chapter>()

        tocDocument.select("navPoint").forEach { navPoint ->
            val title = navPoint.selectFirst("navLabel > text")?.text()
                .let { title ->
                    if (title.isNullOrBlank()) return@forEach
                    title.trim()
                }

            val source = navPoint.selectFirst("content")?.attr("src")?.trim()
                .let { source ->
                    if (source.isNullOrBlank()) return@forEach
                    source.toUri().path ?: source
                }.substringAfterLast(File.separator)

            val parent = navPoint.parent()
                .let { parent ->
                    if (parent == null) return@let null
                    if (!parent.tagName().equals("navPoint", ignoreCase = true)) return@let null

                    val parentSource = parent.selectFirst("content")?.attr("src")?.trim()
                        .let { parentSource ->
                            if (parentSource.isNullOrBlank()) return@forEach
                            parentSource.toUri().path ?: parentSource
                        }.substringAfterLast(File.separator)
                    if (parentSource == source) return@let null
                    return@let parentSource
                }

            val chapter = ReaderText.Chapter(
                title = titleMap[source]?.title.run {
                    if (this == null) return@run title
                    return@run "$this / $title"
                },
                nested = titleMap[source]?.nested ?: (parent != null)
            )
            titleMap[source] = chapter
        }

        return titleMap
    }

    /**
     * Getting title from [chapterTitleMap].
     *
     * @return Null if did not find matching chapters to the [chapterSource].
     */
    private fun getChapterTitleFromToc(
        chapterSource: String,
        chapterTitleMap: Map<Source, ReaderText.Chapter>?
    ): ReaderText.Chapter? {
        if (chapterTitleMap.isNullOrEmpty()) return null
        return chapterTitleMap.getOrElse(chapterSource.substringAfterLast(File.separator)) {
            null
        }
    }

    /**
     * Getting all chapter entries.
     * If [opfEntry] is not null, then getting chapters from Spine.
     * If [opfEntry] is null, then getting chapters from the whole [ZipFile] and manually sorting them.
     *
     * @param opfEntry OPF entry. May be null.
     *
     * @return List of chapter entries in correct order (do not reorder).
     */
    private fun ZipFile.getChapterEntries(opfEntry: ZipEntry?): List<ZipEntry> {
        opfEntry?.let {
            val opfContent = getInputStream(opfEntry).bufferedReader().use {
                it.readText()
            }
            val document = Jsoup.parse(opfContent)
            val zipEntries = entries().toList()

            val manifestItems = document.select("manifest > item").associate {
                it.attr("id") to it.attr("href")
            }

            document.select("spine > itemref").mapNotNull { itemRef ->
                val spineId = itemRef.attr("idref")
                val chapterSource = manifestItems[spineId]
                    ?.substringAfterLast(File.separator)
                    ?.lowercase()
                    ?: return@mapNotNull null

                zipEntries.find { entry ->
                    entry.name.substringAfterLast(File.separator).lowercase() == chapterSource
                }
            }.also { entries ->
                if (entries.isEmpty()) return@let

                Log.i(EPUB_TAG, "Successfully parsed OPF to get entries from spine.")
                return entries
            }
        }

        Log.w(EPUB_TAG, "Could not parse OPF, manual filtering.")
        return entries().toList().filter { entry ->
            listOf(".html", ".htm", ".xhtml").any {
                entry.name.endsWith(it, ignoreCase = true)
            }
        }.sortedBy {
            it.name.filter { char -> char.isDigit() }.toBigIntegerOrNull()
        }
    }
}