package com.aiagent.local.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aiagent.local.data.ChatMessage
import com.aiagent.local.data.InferenceSettings
import com.aiagent.local.data.SettingsRepository
import com.aiagent.local.inference.GrammarBuilder
import com.aiagent.local.inference.LlamaEngine
import com.aiagent.local.inference.ModelManager
import com.aiagent.local.rag.Retriever
import com.aiagent.local.tools.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepo = SettingsRepository(application)
    private val modelManager = ModelManager(application)
    private val llamaEngine = LlamaEngine()
    private val toolRegistry = ToolRegistry()
    private val toolExecutor = ToolExecutor(application)
    private val retriever = Retriever(application)

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isModelLoaded = MutableStateFlow(false)
    val isModelLoaded: StateFlow<Boolean> = _isModelLoaded.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _statusText = MutableStateFlow("No model loaded")
    val statusText: StateFlow<String> = _statusText.asStateFlow()

    private var currentSettings: InferenceSettings = InferenceSettings()

    init {
        // Register tools
        toolRegistry.registerTool(
            "calculator",
            "Evaluate a mathematical expression",
            mapOf("expression" to "The math expression to evaluate")
        )
        toolRegistry.registerTool(
            "search_knowledge_base",
            "Search the knowledge base for relevant information",
            mapOf("query" to "The search query")
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

        // Observe settings
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
                val result = llamaEngine.loadModel(
                    path = path,
                    gpuLayers = currentSettings.gpuLayers,
                    contextSize = currentSettings.contextSize
                )

                if (result.isSuccess) {
                    _isModelLoaded.value = true
                    _statusText.value = "Model loaded!"
                    _messages.value = _messages.value + ChatMessage.System(
                        "Model loaded successfully from: ${Uri.parse(path).lastPathSegment ?: path}"
                    )
                } else {
                    _statusText.value = "Failed to load model"
                    _messages.value = _messages.value + ChatMessage.System(
                        "Error: ${result.exceptionOrNull()?.message}"
                    )
                }
            } catch (e: Exception) {
                _statusText.value = "Error: ${e.message}"
                _messages.value = _messages.value + ChatMessage.System("Error: ${e.message}")
            }
        }
    }

    fun loadEmbeddingModelFromUri(uri: Uri) {
        viewModelScope.launch {
            try {
                _statusText.value = "Copying embedding model..."
                val path = modelManager.resolveModelPath(uri)
                settingsRepo.updateEmbeddingModelPath(path)

                _statusText.value = "Loading embedding model..."
                val result = retriever.initialize(path)
                if (result.isSuccess) {
                    _statusText.value = "Embedding model loaded!"
                    toolExecutor.setRetriever(retriever)
                } else {
                    _statusText.value = "Failed to load embedding model"
                }
            } catch (e: Exception) {
                _statusText.value = "Error: ${e.message}"
            }
        }
    }

    fun addDocumentToKnowledge(uri: Uri, sourceName: String) {
        viewModelScope.launch {
            try {
                _statusText.value = "Processing document..."
                val result = retriever.addDocument(uri, sourceName)
                if (result.isSuccess) {
                    _messages.value = _messages.value + ChatMessage.System(
                        "Document '$sourceName' added to knowledge base (${result.getOrDefault(0)} chunks)"
                    )
                } else {
                    _messages.value = _messages.value + ChatMessage.System(
                        "Failed to add document: ${result.exceptionOrNull()?.message}"
                    )
                }
                _statusText.value = "Model loaded"
            } catch (e: Exception) {
                _statusText.value = "Error: ${e.message}"
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
            val botMessage = ChatMessage.Bot("", isStreaming = true)
            _messages.value = _messages.value + botMessage

            try {
                // Build prompt with system message, tools, and conversation
                val prompt = buildPrompt(content)

                // Determine if we should use tool-calling grammar
                val useToolGrammar = currentSettings.enabledTools.isNotEmpty()
                val grammar = if (useToolGrammar) {
                    GrammarBuilder.buildToolCallGrammar(toolRegistry)
                } else {
                    null
                }

                var fullResponse = ""
                var toolCallDetected = false

                llamaEngine.generateStream(
                    prompt = prompt,
                    temperature = currentSettings.temperature,
                    topP = currentSettings.topP,
                    topK = currentSettings.topK,
                    maxTokens = currentSettings.maxTokens,
                    repeatPenalty = currentSettings.repeatPenalty,
                    grammar = grammar
                ).collect { token ->
                    fullResponse += token
                    updateLastBotMessage(fullResponse, isStreaming = true)
                }

                // Check for tool call in response
                val toolCallMatch = Regex(
                    "<tool_call>(.*?)</tool_call>",
                    RegexOption.DOT_MATCHES_ALL
                ).find(fullResponse)

                if (toolCallMatch != null) {
                    val jsonStr = toolCallMatch.groupValues[1]
                    val toolCall = toolRegistry.parseToolCall(jsonStr)
                    if (toolCall != null) {
                        toolCallDetected = true
                        _messages.value = _messages.value + ChatMessage.ToolCall(
                            toolCall.tool,
                            toolCall.args
                        )

                        val result = toolExecutor.execute(toolCall)
                        _messages.value = _messages.value + ChatMessage.System(
                            "Tool result: ${result.result}"
                        )

                        // Continue conversation with tool result
                        sendMessage("Tool result for ${toolCall.tool}: ${result.result}")
                    }
                }

                if (!toolCallDetected) {
                    updateLastBotMessage(fullResponse, isStreaming = false)
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
        // Retrieve RAG context if knowledge base is populated
        val ragContext = if (retriever.isReady()) {
            retriever.search(userMessage)
        } else {
            ""
        }

        val toolDefinitions = toolRegistry.getToolDefinitions().joinToString("\n") { tool ->
            "  - ${tool.name}: ${tool.description} " +
                    tool.parameters.entries.joinToString(", ") { "${it.key}: ${it.value}" }
        }

        return buildString {
            append("<|system|>\n")
            append(currentSettings.systemPrompt)
            append("\n\nAvailable tools:\n")
            append(toolDefinitions)
            append("\n\nTo use a tool, output: <tool_call>{\"tool\":\"name\",\"args\":{...}}</tool_call>\n")
            if (ragContext.isNotEmpty()) {
                append("\nRelevant context from knowledge base:\n$ragContext\n")
            }
            append("<|end|>\n")
            append("<|user|>\n")
            append(userMessage)
            append("\n<|end|>\n<|assistant|>")
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
        retriever.close()
    }
}