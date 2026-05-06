package com.aiagent.local.inference

import android.util.Log
import de.kherud.llama.InferenceParameters
import de.kherud.llama.LlamaModel
import de.kherud.llama.ModelParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/**
 * Wraps de.kherud:llama (llama.cpp) for streaming GGUF inference.
 * Uses LlamaModel.generate() which returns an Iterable<LlamaOutput>.
 */
class LlamaEngine {

    private var model: LlamaModel? = null
    private var modelPath: String = ""
    private var isLoaded = false

    /**
     * Load a GGUF model from absolute file path.
     * Only one model can be loaded at a time; previous is closed.
     */
    fun loadModel(
        path: String,
        gpuLayers: Int = 20,
        contextSize: Int = 2048
    ): Result<Unit> {
        return try {
            // Close previous model if any
            close()

            Log.d("LlamaEngine", "Loading model from: $path")

            val params = ModelParameters()
                .setModel(path)
                .setNGpuLayers(gpuLayers)
                .setCtxSize(contextSize)

            model = LlamaModel(params)
            modelPath = path
            isLoaded = true
            Log.d("LlamaEngine", "Model loaded successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("LlamaEngine", "Failed to load model", e)
            Result.failure(e)
        }
    }

    /**
     * Stream inference tokens via Kotlin Flow.
     * LlamaModel.generate() is synchronous, so we wrap in flowOn(IO).
     */
    fun generateStream(
        prompt: String,
        temperature: Float = 0.7f,
        topP: Float = 0.9f,
        topK: Int = 40,
        maxTokens: Int = 512,
        repeatPenalty: Float = 1.1f,
        stopStrings: List<String> = listOf("</s>", "<|im_end|>", "[INST]"),
        grammar: String? = null
    ): Flow<String> = flow {
        val currentModel = model ?: throw IllegalStateException("Model not loaded")
        val currentPath = modelPath

        val params = InferenceParameters(prompt)
            .setTemperature(temperature)
            .setTopP(topP)
            .setTopK(topK)
            .setNPredict(maxTokens)
            .setRepeatPenalty(repeatPenalty)
            .setStopStrings(stopStrings)

        if (grammar != null) {
            params.setGrammar(grammar)
        }

        val generator = try {
            currentModel.generate(params)
        } catch (e: Exception) {
            Log.e("LlamaEngine", "Generate failed", e)
            throw e
        }

        try {
            for (output in generator) {
                emit(output.toString())
            }
        } catch (e: Exception) {
            Log.e("LlamaEngine", "Stream error", e)
            throw e
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Blocking completion (non-streaming).
     */
    fun complete(
        prompt: String,
        temperature: Float = 0.7f,
        topP: Float = 0.9f,
        topK: Int = 40,
        maxTokens: Int = 512,
        grammar: String? = null
    ): Result<String> {
        return try {
            val currentModel = model ?: throw IllegalStateException("Model not loaded")

            val params = InferenceParameters(prompt)
                .setTemperature(temperature)
                .setTopP(topP)
                .setTopK(topK)
                .setNPredict(maxTokens)

            if (grammar != null) {
                params.setGrammar(grammar)
            }

            val result = currentModel.complete(params)
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Generate text embeddings. Requires model loaded with embedding=true
     * (use a dedicated embedding GGUF like all-MiniLM-L6-v2).
     */
    fun embed(text: String): Result<FloatArray> {
        return try {
            val currentModel = model ?: throw IllegalStateException("Model not loaded")
            val embedding = currentModel.embed(text)
            Result.success(embedding)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun isModelLoaded(): Boolean = isLoaded && model != null

    fun close() {
        try {
            model?.close()
        } catch (e: Exception) {
            Log.w("LlamaEngine", "Error closing model", e)
        }
        model = null
        isLoaded = false
        modelPath = ""
    }
}