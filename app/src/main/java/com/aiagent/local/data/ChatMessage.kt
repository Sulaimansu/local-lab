package com.aiagent.local.data

sealed class ChatMessage {
    abstract val content: String
    abstract val timestamp: Long

    data class User(
        override val content: String,
        override val timestamp: Long = System.currentTimeMillis()
    ) : ChatMessage()

    data class Bot(
        override val content: String,
        override val timestamp: Long = System.currentTimeMillis(),
        val isStreaming: Boolean = false
    ) : ChatMessage()

    data class System(
        override val content: String,
        override val timestamp: Long = System.currentTimeMillis()
    ) : ChatMessage()

    data class ToolCall(
        val toolName: String,
        val arguments: Map<String, String>,
        val result: String? = null,
        override val timestamp: Long = System.currentTimeMillis()
    ) : ChatMessage() {
        override val content: String
            get() = if (result != null) {
                "Tool: $toolName\nResult: $result"
            } else {
                "Calling tool: $toolName..."
            }
    }
}