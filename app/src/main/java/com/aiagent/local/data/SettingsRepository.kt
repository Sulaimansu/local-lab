package com.aiagent.local.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "ai_settings")

data class InferenceSettings(
    val modelPath: String = "",
    val embeddingModelPath: String = "",
    val systemPrompt: String = "You are a helpful, precise assistant. When you need to use a tool, respond ONLY with a JSON object inside <tool_call> tags.",
    val temperature: Float = 0.7f,
    val topP: Float = 0.9f,
    val topK: Int = 40,
    val maxTokens: Int = 512,
    val repeatPenalty: Float = 1.1f,
    val gpuLayers: Int = 20,
    val contextSize: Int = 2048,
    val enabledTools: Set<String> = setOf("calculator", "search_knowledge_base")
)

class SettingsRepository(private val context: Context) {

    companion object {
        val KEY_MODEL_PATH = stringPreferencesKey("model_path")
        val KEY_EMBEDDING_MODEL_PATH = stringPreferencesKey("embedding_model_path")
        val KEY_SYSTEM_PROMPT = stringPreferencesKey("system_prompt")
        val KEY_TEMPERATURE = floatPreferencesKey("temperature")
        val KEY_TOP_P = floatPreferencesKey("top_p")
        val KEY_TOP_K = intPreferencesKey("top_k")
        val KEY_MAX_TOKENS = intPreferencesKey("max_tokens")
        val KEY_REPEAT_PENALTY = floatPreferencesKey("repeat_penalty")
        val KEY_GPU_LAYERS = intPreferencesKey("gpu_layers")
        val KEY_CONTEXT_SIZE = intPreferencesKey("context_size")
        val KEY_ENABLED_TOOLS = stringSetPreferencesKey("enabled_tools")
    }

    val settingsFlow: Flow<InferenceSettings> = context.dataStore.data.map { prefs ->
        InferenceSettings(
            modelPath = prefs[KEY_MODEL_PATH] ?: "",
            embeddingModelPath = prefs[KEY_EMBEDDING_MODEL_PATH] ?: "",
            systemPrompt = prefs[KEY_SYSTEM_PROMPT] ?: "You are a helpful, precise assistant. When you need to use a tool, respond ONLY with a JSON object inside <tool_call> tags.",
            temperature = prefs[KEY_TEMPERATURE] ?: 0.7f,
            topP = prefs[KEY_TOP_P] ?: 0.9f,
            topK = prefs[KEY_TOP_K] ?: 40,
            maxTokens = prefs[KEY_MAX_TOKENS] ?: 512,
            repeatPenalty = prefs[KEY_REPEAT_PENALTY] ?: 1.1f,
            gpuLayers = prefs[KEY_GPU_LAYERS] ?: 20,
            contextSize = prefs[KEY_CONTEXT_SIZE] ?: 2048,
            enabledTools = prefs[KEY_ENABLED_TOOLS] ?: setOf("calculator", "search_knowledge_base")
        )
    }

    suspend fun updateModelPath(path: String) {
        context.dataStore.edit { it[KEY_MODEL_PATH] = path }
    }

    suspend fun updateEmbeddingModelPath(path: String) {
        context.dataStore.edit { it[KEY_EMBEDDING_MODEL_PATH] = path }
    }

    suspend fun updateSystemPrompt(prompt: String) {
        context.dataStore.edit { it[KEY_SYSTEM_PROMPT] = prompt }
    }

    suspend fun updateTemperature(temp: Float) {
        context.dataStore.edit { it[KEY_TEMPERATURE] = temp }
    }

    suspend fun updateTopP(topP: Float) {
        context.dataStore.edit { it[KEY_TOP_P] = topP }
    }

    suspend fun updateTopK(topK: Int) {
        context.dataStore.edit { it[KEY_TOP_K] = topK }
    }

    suspend fun updateMaxTokens(maxTokens: Int) {
        context.dataStore.edit { it[KEY_MAX_TOKENS] = maxTokens }
    }

    suspend fun updateRepeatPenalty(penalty: Float) {
        context.dataStore.edit { it[KEY_REPEAT_PENALTY] = penalty }
    }

    suspend fun updateGpuLayers(layers: Int) {
        context.dataStore.edit { it[KEY_GPU_LAYERS] = layers }
    }

    suspend fun updateContextSize(size: Int) {
        context.dataStore.edit { it[KEY_CONTEXT_SIZE] = size }
    }

    suspend fun updateEnabledTools(tools: Set<String>) {
        context.dataStore.edit { it[KEY_ENABLED_TOOLS] = tools }
    }
}