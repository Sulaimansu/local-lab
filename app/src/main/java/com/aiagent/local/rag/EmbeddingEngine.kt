package com.aiagent.local.rag

import android.util.Log
import de.kherud.llama.LlamaModel
import de.kherud.llama.ModelParameters

/**
 * Dedicated embedding engine using a small GGUF model (e.g., all-MiniLM-L6-v2-Q8_0.gguf).
 * Loaded with embedding=true for generating dense vector embeddings.
 */
class EmbeddingEngine {

    private var model: LlamaModel? = null

    fun loadEmbeddingModel(path: String): Result<Unit> {
        return try {
            // Close previous if any
            close()

            Log.d("EmbeddingEngine", "Loading embedding model: $path")
            val params = ModelParameters()
                .setModel(path)
                .setNGpuLayers(0) // CPU-only for embedding
                .setEmbedding(true)
                .setPoolingType(LlamaModel.PoolingType.MEAN)

            model = LlamaModel(params)
            Log.d("EmbeddingEngine", "Embedding model loaded")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("EmbeddingEngine", "Failed to load embedding model", e)
            Result.failure(e)
        }
    }

    /**
     * Generate embedding for text. Returns float array (typically 384-dim for MiniLM).
     */
    fun embed(text: String): Result<FloatArray> {
        return try {
            val currentModel = model ?: throw IllegalStateException("Embedding model not loaded")
            val embedding = currentModel.embed(text)
            Result.success(embedding)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun isLoaded(): Boolean = model != null

    fun close() {
        try {
            model?.close()
        } catch (e: Exception) {
            Log.w("EmbeddingEngine", "Error closing embedding model", e)
        }
        model = null
    }
}