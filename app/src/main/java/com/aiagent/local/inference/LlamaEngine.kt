package com.aiagent.local.inference

import android.content.Context
import androidx.lifecycle.lifecycleScope
import io.aatricks.llmedge.LLMEdge
import io.aatricks.llmedge.model.ModelSpec
import io.aatricks.llmedge.text.TextGenerationRequest
import io.aatricks.llmedge.text.TextStreamEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

/**
 * Wraps llmedge (io.github.aatricks:llmedge) for GGUF inference on Android.
 * Uses the LLMEdge facade which bundles llama.cpp native .so files.
 */
class LlamaEngine(private val context: Context, private val appScope: CoroutineScope) {

    private var edge: LLMEdge? = null
    private var isLoaded = false
    private var modelSpec: ModelSpec? = null

    /**
     * Initialize the LLMEdge facade. Call once from a coroutine scope.
     */
    fun initialize(): Result<Unit> {
        return try {
            edge = LLMEdge.create(
                context = context.applicationContext,
                scope = appScope
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Load a GGUF model from absolute file path.
     * ModelSpec.localFile() tells llmedge to load from device storage.
     */
    fun loadModel(path: String): Result<Unit> {
        return try {
            modelSpec = ModelSpec.localFile(path)
            isLoaded = true
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Stream tokens using llmedge's edge.text.stream().
     */
    fun generateStream(
        prompt: String,
        temperature: Float = 0.7f,
        topP: Float = 0.9f,
        maxTokens: Int = 512,
    ): Flow<String> = flow {
        val currentEdge = edge ?: throw IllegalStateException("LLMEdge not initialized")
        val spec = modelSpec ?: throw IllegalStateException("Model not loaded")

        // Create a text generation request for streaming.
        // llmedge handles the native llama.cpp context internally.
        val request = TextGenerationRequest(
            prompt = prompt,
            model = spec,
            maxTokens = maxTokens,
        )

        // Stream events: each TextStreamEvent contains a token fragment
        currentEdge.text.stream(request).collect { event ->
            when (event) {
                is TextStreamEvent.Token -> emit(event.text)
                is TextStreamEvent.Error -> throw RuntimeException(event.message)
                else -> {} // ignore other events like Start, End
            }
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Blocking (non-streaming) completion.
     */
    suspend fun complete(
        prompt: String,
        temperature: Float = 0.7f,
        topP: Float = 0.9f,
        maxTokens: Int = 512,
    ): Result<String> {
        return try {
            val currentEdge = edge ?: throw IllegalStateException("LLMEdge not initialized")
            val spec = modelSpec ?: throw IllegalStateException("Model not loaded")

            val request = TextGenerationRequest(
                prompt = prompt,
                model = spec,
                maxTokens = maxTokens,
            )

            val result = currentEdge.text.generate(request)
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Embed text using llmedge's built-in RAG pipeline.
     * llmedge uses ONNX all-MiniLM-L6-v2 embeddings (not GGUF).
     * Returns the raw embedding as a FloatArray.
     */
    fun embed(text: String): Result<FloatArray> {
        return try {
            val currentEdge = edge ?: throw IllegalStateException("LLMEdge not initialized")
            val session = currentEdge.rag.createSession()
            session.init()

            // Index the text (creates embedding internally)
            val embedding = session.embed(text)
            Result.success(embedding)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun isModelLoaded(): Boolean = isLoaded && edge != null

    fun close() {
        edge = null
        modelSpec = null
        isLoaded = false
    }
}