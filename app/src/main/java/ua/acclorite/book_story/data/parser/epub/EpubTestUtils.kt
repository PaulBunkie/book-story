/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.data.parser.epub

import android.util.Log
import ua.acclorite.book_story.domain.file.CachedFile
import java.io.File
import java.util.zip.ZipFile

object EpubTestUtils {
    
    private const val TAG = "EPUB_TEST_UTILS"
    
    /**
     * Tests a specific EPUB file and logs detailed information about its structure.
     * This is useful for debugging problematic EPUB files.
     */
    fun testEpubFile(cachedFile: CachedFile) {
        Log.i(TAG, "=== Testing EPUB file: ${cachedFile.name} ===")
        
        val rawFile = cachedFile.rawFile
        if (rawFile == null) {
            Log.e(TAG, "Raw file is null")
            return
        }
        
        if (!rawFile.exists()) {
            Log.e(TAG, "File does not exist: ${rawFile.absolutePath}")
            return
        }
        
        if (!rawFile.canRead()) {
            Log.e(TAG, "File cannot be read: ${rawFile.absolutePath}")
            return
        }
        
        Log.i(TAG, "File size: ${rawFile.length()} bytes")
        Log.i(TAG, "File path: ${rawFile.absolutePath}")
        
        try {
            ZipFile(rawFile).use { zip ->
                val entries = zip.entries().asSequence().toList()
                Log.i(TAG, "Total entries: ${entries.size}")
                
                // Log all entries
                entries.forEachIndexed { index, entry ->
                    Log.d(TAG, "Entry $index: ${entry.name} (${entry.size} bytes)")
                }
                
                // Check for OPF files
                val opfFiles = entries.filter { it.name.endsWith(".opf", ignoreCase = true) }
                Log.i(TAG, "OPF files found: ${opfFiles.size}")
                opfFiles.forEach { opf ->
                    Log.i(TAG, "OPF file: ${opf.name}")
                    try {
                        val content = zip.getInputStream(opf).bufferedReader().use { it.readText() }
                        Log.d(TAG, "OPF content preview: ${content.take(500)}...")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error reading OPF file: ${opf.name}", e)
                    }
                }
                
                // Check for NCX files
                val ncxFiles = entries.filter { it.name.endsWith(".ncx", ignoreCase = true) }
                Log.i(TAG, "NCX files found: ${ncxFiles.size}")
                ncxFiles.forEach { ncx ->
                    Log.i(TAG, "NCX file: ${ncx.name}")
                }
                
                // Check for HTML files
                val htmlFiles = entries.filter { 
                    it.name.endsWith(".html", ignoreCase = true) || 
                    it.name.endsWith(".xhtml", ignoreCase = true) 
                }
                Log.i(TAG, "HTML files found: ${htmlFiles.size}")
                
                // Check for images
                val imageFiles = entries.filter { 
                    it.name.endsWith(".jpg", ignoreCase = true) || 
                    it.name.endsWith(".jpeg", ignoreCase = true) || 
                    it.name.endsWith(".png", ignoreCase = true) ||
                    it.name.endsWith(".gif", ignoreCase = true) ||
                    it.name.endsWith(".svg", ignoreCase = true)
                }
                Log.i(TAG, "Image files found: ${imageFiles.size}")
                
                // Check for CSS files
                val cssFiles = entries.filter { it.name.endsWith(".css", ignoreCase = true) }
                Log.i(TAG, "CSS files found: ${cssFiles.size}")
                
                // Check for required files
                val hasMimeType = entries.any { it.name == "mimetype" }
                val hasContainer = entries.any { it.name == "META-INF/container.xml" }
                
                Log.i(TAG, "Has mimetype file: $hasMimeType")
                Log.i(TAG, "Has container.xml: $hasContainer")
                
                if (hasMimeType) {
                    try {
                        val mimeTypeEntry = entries.find { it.name == "mimetype" }
                        if (mimeTypeEntry != null) {
                            val mimeType = zip.getInputStream(mimeTypeEntry).bufferedReader().use { it.readText() }
                            Log.i(TAG, "MIME type: $mimeType")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error reading mimetype file", e)
                    }
                }
                
                if (hasContainer) {
                    try {
                        val containerEntry = entries.find { it.name == "META-INF/container.xml" }
                        if (containerEntry != null) {
                            val container = zip.getInputStream(containerEntry).bufferedReader().use { it.readText() }
                            Log.d(TAG, "Container.xml content: $container")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error reading container.xml file", e)
                    }
                }
                
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error testing EPUB file: ${cachedFile.name}", e)
        }
        
        Log.i(TAG, "=== End testing EPUB file: ${cachedFile.name} ===")
    }
}
