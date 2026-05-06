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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

        viewModelScope.launch {
            _isGenerating.value = true
            try {
                val prompt = buildPrompt(content)

                // Blocking call on IO dispatcher
                val fullResponse = withContext(Dispatchers.IO) {
                    llamaEngine.complete(
                        prompt = prompt,
                        temperature = currentSettings.temperature,
                        maxTokens = currentSettings.maxTokens
                    )
                }

                // Add the bot message with the full response
                _messages.value = _messages.value + ChatMessage.Bot(fullResponse)

                // Check for tool call
                val toolCallMatch = Regex(
                    "<tool_call>(.*?)</tool_call>",
                    RegexOption.DOT_MATCHES_ALL
                ).find(fullResponse)

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
                        // Continue conversation with tool result
                        sendMessage("Tool result for ${toolCall.tool}: ${result.result}")
                    }
                }
            } catch (e: Exception) {
                _messages.value = _messages.value + ChatMessage.System(
                    "Generation error: ${e.message}"
                )
            } finally {
                _isGenerating.value = false
            }
        }
    }

    private suspend fun buildPrompt(userMessage: String): String {
        val toolDefs = toolRegistry.getToolDefinitions().joinToString("\n") { tool ->
            "  - ${tool.name}: ${tool.description} " +
                    tool.parameters.entries.joinToString(", ") { "${it.key}: ${it.value}" }
        }
        return buildString {
            append("<|system|>\n")
            append(currentSettings.systemPrompt)
            append("\n\nAvailable tools:\n")
            append(toolDefs)
            append("\n\nTo use a tool, output: <tool_call>{\"tool\":\"name\",\"args\":{...}}</tool_call>\n")
            append("<|end|>\n")
            append("<|user|>\n")
            append(userMessage)
            append("\n<|end|>\n<|assistant|>")
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