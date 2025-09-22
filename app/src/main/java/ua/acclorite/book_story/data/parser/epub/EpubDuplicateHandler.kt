/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.data.parser.epub

import android.util.Log
import ua.acclorite.book_story.domain.file.CachedFile
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

object EpubDuplicateHandler {
    
    private const val TAG = "EPUB_DUPLICATE_HANDLER"
    
    /**
     * Attempts to parse an EPUB file with duplicate entries by using ZipInputStream
     * instead of ZipFile, which can handle duplicates by taking the last occurrence.
     */
    fun tryParseWithDuplicates(cachedFile: CachedFile): EpubParseResult? {
        val rawFile = cachedFile.rawFile ?: return null
        
        if (!rawFile.exists() || !rawFile.canRead()) return null
        
        return try {
            Log.i(TAG, "Attempting to parse EPUB with duplicates: ${cachedFile.name}")
            
            val entries = mutableMapOf<String, ZipEntry>()
            val entryData = mutableMapOf<String, ByteArray>()
            
            // First pass: collect all entries, keeping the last occurrence of duplicates
            FileInputStream(rawFile).use { fis ->
                ZipInputStream(fis).use { zis ->
                    var entry: ZipEntry? = zis.nextEntry
                    while (entry != null) {
                        val name = entry.name
                        entries[name] = entry
                        
                        // Read entry data for small files (like OPF, NCX)
                        if (entry.size < 1024 * 1024) { // Less than 1MB
                            val data = zis.readBytes()
                            entryData[name] = data
                        }
                        
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }
            }
            
            Log.i(TAG, "Successfully read ${entries.size} entries from EPUB with duplicates")
            
            EpubParseResult(
                success = true,
                entries = entries.values.toList(),
                entryData = entryData,
                message = "Successfully parsed EPUB with duplicate handling"
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse EPUB with duplicates: ${cachedFile.name}", e)
            EpubParseResult(
                success = false,
                entries = emptyList(),
                entryData = emptyMap(),
                message = "Failed to parse: ${e.message}"
            )
        }
    }
}

data class EpubParseResult(
    val success: Boolean,
    val entries: List<ZipEntry>,
    val entryData: Map<String, ByteArray>,
    val message: String
)
