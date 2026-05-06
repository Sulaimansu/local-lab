package com.aiagent.local.inference

import android.content.Context
import io.aatricks.llmedge.LLMEdge
import io.aatricks.llmedge.model.ModelSpec
import io.aatricks.llmedge.text.TextGenerationRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
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

    fun generateStream(
        prompt: String,
        temperature: Float = 0.7f,
        maxTokens: Int = 512
    ): Flow<String> = flow {
        val currentEdge = edge ?: throw IllegalStateException("LLMEdge not initialized")
        val spec = modelSpec ?: throw IllegalStateException("Model not loaded")

        val request = TextGenerationRequest(
            prompt = prompt,
            model = spec,
            maxTokens = maxTokens,
        )

        currentEdge.text.stream(request).collect { token ->
            emit(token)
        }
    }.flowOn(Dispatchers.IO)

    suspend fun complete(
        prompt: String,
        temperature: Float = 0.7f,
        maxTokens: Int = 512
    ): String = withContext(Dispatchers.IO) {
        val currentEdge = edge ?: throw IllegalStateException("LLMEdge not initialized")
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