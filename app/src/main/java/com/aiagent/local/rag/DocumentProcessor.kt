package com.aiagent.local.rag

import android.content.Context
import android.net.Uri
import java.io.File

/**
 * Processes documents (.txt, .pdf via text extraction) into chunks
 * for embedding and storage.
 *
 * Off-Grid uses paragraph-aware chunking with sliding-window fallback.
 * We implement a similar strategy.
 */
class DocumentProcessor(private val context: Context) {

    companion object {
        const val MAX_CHUNK_SIZE = 500
        const val CHUNK_OVERLAP = 50
    }

    data class Chunk(
        val text: String,
        val sourceFile: String,
        val index: Int
    )

    /**
     * Extract text from a file URI and split into chunks.
     */
    suspend fun processDocument(uri: Uri, sourceName: String): List<Chunk> {
        val text = readTextFromUri(uri)
        return chunkText(text, sourceName)
    }

    fun chunkText(text: String, sourceName: String): List<Chunk> {
        val chunks = mutableListOf<Chunk>()

        // Paragraph-aware splitting (Off-Grid pattern)
        val paragraphs = text.split(Regex("(?<=[.!?])\\s+"))
        var currentChunk = StringBuilder()
        var index = 0

        for (paragraph in paragraphs) {
            val trimmed = paragraph.trim()
            if (trimmed.isEmpty()) continue

            if (currentChunk.length + trimmed.length + 1 <= MAX_CHUNK_SIZE) {
                if (currentChunk.isNotEmpty()) currentChunk.append(" ")
                currentChunk.append(trimmed)
            } else {
                // Save current chunk
                if (currentChunk.isNotEmpty()) {
                    chunks.add(Chunk(currentChunk.toString().trim(), sourceName, index++))
                }

                // Handle long paragraphs with sliding window
                if (trimmed.length > MAX_CHUNK_SIZE) {
                    val subChunks = splitLongText(trimmed)
                    for (sub in subChunks) {
                        chunks.add(Chunk(sub, sourceName, index++))
                    }
                    currentChunk = StringBuilder()
                } else {
                    currentChunk = StringBuilder(trimmed)
                }
            }
        }

        // Add remaining
        if (currentChunk.isNotEmpty()) {
            chunks.add(Chunk(currentChunk.toString().trim(), sourceName, index))
        }

        return chunks
    }

    private fun splitLongText(text: String): List<String> {
        val chunks = mutableListOf<String>()
        var start = 0
        while (start < text.length) {
            val end = minOf(start + MAX_CHUNK_SIZE, text.length)
            chunks.add(text.substring(start, end))
            start += (MAX_CHUNK_SIZE - CHUNK_OVERLAP)
        }
        return chunks
    }

    private suspend fun readTextFromUri(uri: Uri): String = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        context.contentResolver.openInputStream(uri)?.use { input ->
            input.bufferedReader().readText()
        } ?: throw IllegalStateException("Cannot read document")
    }
}