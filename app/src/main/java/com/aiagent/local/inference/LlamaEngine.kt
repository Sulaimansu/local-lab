package com.aiagent.local.inference

import android.content.Context
import android.util.Log
import io.aatricks.llmedge.LLMEdge
import io.aatricks.llmedge.model.ModelSpec
import io.aatricks.llmedge.text.TextGenerationRequest
import io.aatricks.llmedge.text.TextModelOptions
import io.aatricks.llmedge.text.runtime.SmolLM
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File

class LlamaEngine(private val context: Context, private val appScope: CoroutineScope) {

    companion object {
        private const val TAG = "LlamaEngine"
    }

    private var edge: LLMEdge? = null
    private var modelSpec: ModelSpec? = null
    private var currentModelPath: String? = null
    private var currentGpuLayers: Int = 0
    private var currentContextSize: Int = 2048
    private var currentThreads: Int = 4

    suspend fun initialize() {
        withContext(Dispatchers.Main) {
            Log.d(TAG, "Initializing LLMEdge engine...")
            try {
                edge = LLMEdge.create(context.applicationContext, appScope)
                Log.d(TAG, "LLMEdge engine initialized successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize LLMEdge", e)
                throw e
            }
        }
    }

    /**
     * Load model with full configuration
     */
    fun loadModel(
        path: String,
        gpuLayers: Int = 20,
        contextSize: Int = 2048,
        threads: Int = 4
    ) {
        Log.d(TAG, "Loading model from: $path")
        Log.d(TAG, "Config: gpuLayers=$gpuLayers, contextSize=$contextSize, threads=$threads")
        
        // Validate model file
        val modelFile = File(path)
        when {
            !modelFile.exists() -> {
                Log.e(TAG, "Model file does not exist: $path")
                throw IllegalStateException("Model file does not exist: $path")
            }
            !modelFile.name.endsWith(".gguf", ignoreCase = true) -> {
                Log.e(TAG, "File is not a GGUF model: ${modelFile.name}")
                throw IllegalArgumentException("File must be a .gguf model, got: ${modelFile.name}")
            }
            modelFile.length() == 0L -> {
                Log.e(TAG, "Model file is empty: $path")
                throw IllegalStateException("Model file is empty: $path")
            }
            modelFile.length() < 1024 * 1024 -> { // Less than 1MB is suspicious
                Log.w(TAG, "Model file seems too small: ${modelFile.length()} bytes")
            }
        }
        
        Log.d(TAG, "Model file validated: ${modelFile.length()} bytes")
        
        // Create ModelSpec - llmedge 0.3.9 uses simple factory method
        // Note: GPU layers and other params are set in TextModelOptions during generation
        modelSpec = ModelSpec.localFile(path)
        currentGpuLayers = gpuLayers
        currentContextSize = contextSize
        currentThreads = threads
        
        currentModelPath = path
        Log.i(TAG, "Model loaded and configured successfully (path=$path, gpu=$gpuLayers, ctx=$contextSize)")
    }

    /**
     * Blocking full-response generation with all inference parameters
     * Note: topP, topK, repeatPenalty are NOT used in llmedge 0.3.9 (not supported)
     */
    suspend fun complete(
        prompt: String,
        temperature: Float = 0.7f,
        maxTokens: Int = 512,
        topP: Float = 0.9f, // Ignored in llmedge 0.3.9
        topK: Int = 40,     // Ignored in llmedge 0.3.9
        repeatPenalty: Float = 1.1f // Ignored in llmedge 0.3.9
    ): String = withContext(Dispatchers.IO) {
        Log.d(TAG, "Starting text generation...")
        Log.d(TAG, "Parameters: temperature=$temperature, maxTokens=$maxTokens")
        Log.d(TAG, "Note: topP/topK/repeatPenalty not supported in llmedge 0.3.9")
        Log.d(TAG, "Model config: contextSize=$currentContextSize, threads=$currentThreads")
        
        val currentEdge = edge ?: throw IllegalStateException("Engine not initialized. Call initialize() first.")
        val spec = modelSpec ?: throw IllegalStateException("Model not loaded. Call loadModel() first.")

        // Check for cancellation before starting
        ensureActive()
        
        try {
            // Build TextModelOptions with EXACT llmedge 0.3.9 API
            // Parameters: contextSize, chatTemplate, numThreads, generationThreads, minP, temperature,
            //             useMmap, useMlock, useFlashAttention, thinkingMode, reasoningBudget, useVulkan
            // 
            // CRITICAL: thinkingMode is NOT nullable - must be SmolLM.ThinkingMode.DEFAULT or DISABLED
            // NOTE: topP, topK, repeatPenalty, gpuLayers are NOT supported in llmedge 0.3.9
            val options = TextModelOptions(
                contextSize = currentContextSize.toLong(),
                chatTemplate = null,
                numThreads = currentThreads,
                generationThreads = 1,
                minP = 0.05f,
                temperature = temperature,
                useMmap = true,
                useMlock = false,
                useFlashAttention = false,
                thinkingMode = SmolLM.ThinkingMode.DEFAULT,
                reasoningBudget = null,
                useVulkan = false
            )
            
            Log.d(TAG, "TextModelOptions created:")
            Log.d(TAG, "  - contextSize=${options.contextSize}")
            Log.d(TAG, "  - threads=${options.numThreads}")
            Log.d(TAG, "  - temperature=${options.temperature}")
            Log.d(TAG, "  - minP=${options.minP}")
            Log.d(TAG, "  - thinkingMode=${options.thinkingMode}")
            
            // Build TextGenerationRequest
            val request = TextGenerationRequest(
                prompt = prompt,
                model = spec,
                systemPrompt = null,
                options = options,
                maxTokens = maxTokens,
                batchSize = 1
            )
            
            Log.d(TAG, "TextGenerationRequest built, calling generate...")
            Log.d(TAG, "Prompt length: ${prompt.length}, maxTokens: $maxTokens")
            
            // Check for cancellation
            ensureActive()
            
            // Execute generation
            val response = currentEdge.text.generate(request)
            
            Log.i(TAG, "Generation completed!")
            Log.d(TAG, "Response length: ${response.length}")
            Log.d(TAG, "Preview: ${response.take(200)}...")
            
            response
        } catch (e: Exception) {
            Log.e(TAG, "Text generation FAILED", e)
            throw e
        }
    }

    fun isModelLoaded(): Boolean {
        val loaded = modelSpec != null && currentModelPath != null && edge != null
        Log.d(TAG, "isModelLoaded: $loaded (modelSpec!=null: ${modelSpec != null}, path: $currentModelPath, edge!=null: ${edge != null})")
        return loaded
    }

    fun getCurrentModelPath(): String? = currentModelPath

    fun close() {
        Log.i(TAG, "Closing LlamaEngine and releasing resources")
        try {
            edge?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing LLMEdge", e)
        }
        edge = null
        modelSpec = null
        currentModelPath = null
        currentGpuLayers = 0
        currentContextSize = 2048
        currentThreads = 4
    }
}