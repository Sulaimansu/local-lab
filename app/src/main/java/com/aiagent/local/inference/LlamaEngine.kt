package com.aiagent.local.inference

import android.content.Context
import io.aatricks.llmedge.LLMEdge
import io.aatricks.llmedge.model.ModelSpec
import io.aatricks.llmedge.text.TextGenerationRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LlamaEngine(private val context: Context, private val appScope: CoroutineScope) {

    private var edge: LLMEdge? = null
    private var modelSpec: ModelSpec? = null

    suspend fun initialize() {
        withContext(Dispatchers.Main) {
            edge = LLMEdge.create(context.applicationContext, appScope)
        }
    }

    fun loadModel(path: String) {
        modelSpec = ModelSpec.localFile(path)
    }

    /**
     * Blocking full-response generation – safe & works.
     */
    suspend fun complete(
        prompt: String,
        temperature: Float = 0.7f,
        maxTokens: Int = 512
    ): String = withContext(Dispatchers.IO) {
        val currentEdge = edge ?: throw IllegalStateException("Engine not initialized")
        val spec = modelSpec ?: throw IllegalStateException("Model not loaded")

        val request = TextGenerationRequest(
            prompt = prompt,
            model = spec,
            maxTokens = maxTokens,
        )

        currentEdge.text.generate(request)
    }

    fun isModelLoaded() = modelSpec != null

    fun close() {
        edge = null
        modelSpec = null
    }
}