package com.aiagent.local.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aiagent.local.data.ChatMessage
import com.aiagent.local.data.InferenceSettings
import com.aiagent.local.data.SettingsRepository
import com.aiagent.local.inference.LlamaEngine
import com.aiagent.local.inference.ModelManager
import com.aiagent.local.tools.ToolExecutor
import com.aiagent.local.tools.ToolRegistry
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepo = SettingsRepository(application)
    private val modelManager = ModelManager(application)
    private val llamaEngine = LlamaEngine(application, viewModelScope)
    private val toolRegistry = ToolRegistry()
    private val toolExecutor = ToolExecutor(application)

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isModelLoaded = MutableStateFlow(false)
    val isModelLoaded: StateFlow<Boolean> = _isModelLoaded.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _statusText = MutableStateFlow("Initializing engine...")
    val statusText: StateFlow<String> = _statusText.asStateFlow()

    private var currentSettings: InferenceSettings = InferenceSettings()
    private var generationJob: Job? = null

    init {
        viewModelScope.launch {
            try {
                llamaEngine.initialize()
                _statusText.value = "No model loaded"
            } catch (e: Exception) {
                _statusText.value = "Engine init failed: ${e.message}"
            }
        }

        toolRegistry.registerTool(
            "calculator",
            "Evaluate a mathematical expression",
            mapOf("expression" to "The math expression to evaluate")
        )
        toolRegistry.registerTool(
            "get_current_time",
            "Get the current date and time",
            emptyMap()
        )
        toolRegistry.registerTool(
            "get_battery_level",
            "Get the current battery level of the device",
            emptyMap()
        )

        viewModelScope.launch {
            settingsRepo.settingsFlow.collect { settings ->
                currentSettings = settings
            }
        }
    }

    fun loadModelFromUri(uri: Uri) {
        viewModelScope.launch {
            try {
                _statusText.value = "Copying model file..."
                val path = modelManager.resolveModelPath(uri)

                _statusText.value = "Loading GGUF model..."
                llamaEngine.loadModel(path)
                _isModelLoaded.value = true
                _statusText.value = "Model loaded!"
                _messages.value = _messages.value + ChatMessage.System(
                    "Model loaded from: ${uri.lastPathSegment ?: path}"
                )
            } catch (e: Exception) {
                _statusText.value = "Error: ${e.message}"
                _messages.value = _messages.value + ChatMessage.System("Error: ${e.message}")
            }
        }
    }

    fun sendMessage(content: String) {
        if (!llamaEngine.isModelLoaded()) {
            _messages.value = _messages.value + ChatMessage.System(
                "No model loaded. Please load a GGUF model first."
            )
            return
        }

        _messages.value = _messages.value + ChatMessage.User(content)

        // Cancel any running generation
        generationJob?.cancel()

        generationJob = viewModelScope.launch(Dispatchers.Main) {
            _isGenerating.value = true
            val placeholder = ChatMessage.Bot("Generating…", isStreaming = false)
            _messages.value = _messages.value + placeholder

            try {
                val prompt = buildPrompt(content)

                // Run blocking call on IO, then update UI
                val response = withContext(Dispatchers.IO) {
                    llamaEngine.complete(
                        prompt = prompt,
                        temperature = currentSettings.temperature,
                        maxTokens = currentSettings.maxTokens
                    )
                }

                // Replace placeholder with actual response
                updateLastBotMessage(response, isStreaming = false)

                // Check for tool call
                val toolCallMatch = Regex(
                    "<tool_call>(.*?)</tool_call>",
                    RegexOption.DOT_MATCHES_ALL
                ).find(response)

                if (toolCallMatch != null) {
                    val jsonStr = toolCallMatch.groupValues[1]
                    val toolCall = toolRegistry.parseToolCall(jsonStr)
                    if (toolCall != null) {
                        _messages.value = _messages.value + ChatMessage.ToolCall(
                            toolName = toolCall.tool,
                            arguments = toolCall.args
                        )
                        val result = toolExecutor.execute(toolCall)
                        _messages.value = _messages.value + ChatMessage.System(
                            "Tool result: ${result.result}"
                        )
                        sendMessage("Tool result for ${toolCall.tool}: ${result.result}")
                    }
                }
            } catch (e: CancellationException) {
                updateLastBotMessage("[Stopped]", isStreaming = false)
            } catch (e: Exception) {
                updateLastBotMessage("[Error: ${e.message}]", isStreaming = false)
            } finally {
                _isGenerating.value = false
            }
        }
    }

    fun stopGeneration() {
        generationJob?.cancel()
    }

    private suspend fun buildPrompt(userMessage: String): String {
        val toolDefs = toolRegistry.getToolDefinitions().joinToString("\n") { tool ->
            "  - ${tool.name}: ${tool.description} " +
                    tool.parameters.entries.joinToString(", ") { "${it.key}: ${it.value}" }
        }
        // Generic template (Llama 3 style)
        return buildString {
            append("<|system|>\n")
            append(currentSettings.systemPrompt)
            append("\n\nAvailable tools:\n")
            append(toolDefs)
            append("\n\nTo use a tool, output: <tool_call>{\"tool\":\"name\",\"args\":{...}}</tool_call>\n")
            append("<|end|>\n")
            append("<|user|>\n")
            append(userMessage)
            append("\n<|end|>\n<|assistant|>\n")
        }
    }

    private fun updateLastBotMessage(content: String, isStreaming: Boolean) {
        val list = _messages.value.toMutableList()
        val lastIndex = list.indexOfLast { it is ChatMessage.Bot }
        if (lastIndex >= 0) {
            val botMsg = list[lastIndex] as ChatMessage.Bot
            list[lastIndex] = botMsg.copy(content = content, isStreaming = isStreaming)
            _messages.value = list
        }
    }

    fun clearChat() {
        _messages.value = emptyList()
    }

    override fun onCleared() {
        super.onCleared()
        llamaEngine.close()
    }
}