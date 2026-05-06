package com.aiagent.local.rag

import android.content.Context
import android.net.Uri

/**
 * Full RAG pipeline orchestrator using MiniLM embedding + SQLite vector store.
 * In the Off-Grid pattern, they use MiniLM + op-sqlite. We use the same embedding
 * model but with plain SQLite.
 */
class Retriever(private val context: Context) {

    private val embeddingEngine = EmbeddingEngine()
    private val vectorDb = VectorDatabase(context)
    private val docProcessor = DocumentProcessor(context)

    private var isInitialized = false

    fun initialize(embeddingModelPath: String): Result<Unit> {
        return embeddingEngine.loadEmbeddingModel(embeddingModelPath).also {
            if (it.isSuccess) isInitialized = true
        }
    }

    fun isReady(): Boolean = isInitialized && embeddingEngine.isLoaded()

    /**
     * Add a document to the knowledge base.
     */
    suspend fun addDocument(uri: Uri, sourceName: String): Result<Int> {
        if (!isInitialized) return Result.failure(IllegalStateException("RAG not initialized"))

        return try {
            // Remove old chunks for this source
            vectorDb.deleteBySource(sourceName)

            val chunks = docProcessor.processDocument(uri, sourceName)
            for (chunk in chunks) {
                val embeddingResult = embeddingEngine.embed(chunk.text)
                if (embeddingResult.isSuccess) {
                    vectorDb.insertChunk(
                        chunk.text,
                        embeddingResult.getOrDefault(floatArrayOf()),
                        sourceName,
                        chunk.index
                    )
                }
            }
            Result.success(chunks.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Search the knowledge base for relevant context.
     * Returns concatenated relevant chunks.
     */
    suspend fun search(query: String, topK: Int = 3): String {
        if (!isInitialized) return ""

        val queryEmbedding = embeddingEngine.embed(query).getOrNull() ?: return ""

        val results = vectorDb.searchSimilar(queryEmbedding, topK)
        return if (results.isEmpty()) {
            "No relevant documents found."
        } else {
            results.joinToString("\n---\n") { it.first }
        }
    }

    fun getDocumentCount(): Int = vectorDb.getChunkCount()

    fun close() {
        embeddingEngine.close()
        vectorDb.close()
    }
}