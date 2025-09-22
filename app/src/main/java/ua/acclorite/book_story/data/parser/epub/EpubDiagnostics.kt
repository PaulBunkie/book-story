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

object EpubDiagnostics {
    
    private const val TAG = "EPUB_DIAGNOSTICS"
    
    /**
     * Performs comprehensive diagnostics on an EPUB file to identify potential issues.
     */
    fun diagnoseEpub(cachedFile: CachedFile): EpubDiagnosticResult {
        val rawFile = cachedFile.rawFile ?: return EpubDiagnosticResult(
            isValid = false,
            issues = listOf("Raw file is null"),
            structure = EpubStructure()
        )
        
        if (!rawFile.exists()) {
            return EpubDiagnosticResult(
                isValid = false,
                issues = listOf("File does not exist"),
                structure = EpubStructure()
            )
        }
        
        if (!rawFile.canRead()) {
            return EpubDiagnosticResult(
                isValid = false,
                issues = listOf("File cannot be read"),
                structure = EpubStructure()
            )
        }
        
        return try {
            ZipFile(rawFile).use { zip ->
                val entries = zip.entries().asSequence().toList()
                val structure = analyzeStructure(entries)
                val issues = identifyIssues(entries, structure)
                
                EpubDiagnosticResult(
                    isValid = issues.isEmpty(),
                    issues = issues,
                    structure = structure
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during EPUB diagnostics: ${cachedFile.name}", e)
            EpubDiagnosticResult(
                isValid = false,
                issues = listOf("ZIP error: ${e.message}"),
                structure = EpubStructure()
            )
        }
    }
    
    private fun analyzeStructure(entries: List<java.util.zip.ZipEntry>): EpubStructure {
        val opfFiles = entries.filter { it.name.endsWith(".opf", ignoreCase = true) }
        val ncxFiles = entries.filter { it.name.endsWith(".ncx", ignoreCase = true) }
        val htmlFiles = entries.filter { it.name.endsWith(".html", ignoreCase = true) || it.name.endsWith(".xhtml", ignoreCase = true) }
        val imageFiles = entries.filter { 
            it.name.endsWith(".jpg", ignoreCase = true) || 
            it.name.endsWith(".jpeg", ignoreCase = true) || 
            it.name.endsWith(".png", ignoreCase = true) ||
            it.name.endsWith(".gif", ignoreCase = true) ||
            it.name.endsWith(".svg", ignoreCase = true)
        }
        val cssFiles = entries.filter { it.name.endsWith(".css", ignoreCase = true) }
        
        return EpubStructure(
            totalEntries = entries.size,
            opfFiles = opfFiles.map { it.name },
            ncxFiles = ncxFiles.map { it.name },
            htmlFiles = htmlFiles.map { it.name },
            imageFiles = imageFiles.map { it.name },
            cssFiles = cssFiles.map { it.name },
            hasMimeType = entries.any { it.name == "mimetype" },
            hasContainer = entries.any { it.name == "META-INF/container.xml" }
        )
    }
    
    private fun identifyIssues(entries: List<java.util.zip.ZipEntry>, structure: EpubStructure): List<String> {
        val issues = mutableListOf<String>()
        
        // Check for required files
        if (structure.opfFiles.isEmpty()) {
            issues.add("No OPF file found")
        }
        
        if (structure.opfFiles.size > 1) {
            issues.add("Multiple OPF files found: ${structure.opfFiles}")
        }
        
        if (!structure.hasMimeType) {
            issues.add("Missing mimetype file")
        }
        
        if (!structure.hasContainer) {
            issues.add("Missing META-INF/container.xml")
        }
        
        if (structure.htmlFiles.isEmpty()) {
            issues.add("No HTML content files found")
        }
        
        // Check for suspicious entries
        val suspiciousEntries = entries.filter { entry ->
            entry.name.contains("..") || 
            entry.name.startsWith("/") ||
            entry.name.contains("\\")
        }
        
        if (suspiciousEntries.isNotEmpty()) {
            issues.add("Suspicious entries found: ${suspiciousEntries.map { it.name }}")
        }
        
        // Check for very long filenames
        val longFilenames = entries.filter { it.name.length > 200 }
        if (longFilenames.isNotEmpty()) {
            issues.add("Very long filenames found: ${longFilenames.map { it.name }}")
        }
        
        return issues
    }
}

data class EpubDiagnosticResult(
    val isValid: Boolean,
    val issues: List<String>,
    val structure: EpubStructure
)

data class EpubStructure(
    val totalEntries: Int = 0,
    val opfFiles: List<String> = emptyList(),
    val ncxFiles: List<String> = emptyList(),
    val htmlFiles: List<String> = emptyList(),
    val imageFiles: List<String> = emptyList(),
    val cssFiles: List<String> = emptyList(),
    val hasMimeType: Boolean = false,
    val hasContainer: Boolean = false
)
