package com.aiagent.local.data

sealed class ChatMessage {
    abstract val content: String
    abstract val timestamp: Long

    data class User(
        override val content: String,
        override val timestamp: Long = java.lang.System.currentTimeMillis()
    ) : ChatMessage()

    data class Bot(
        override val content: String,
        override val timestamp: Long = java.lang.System.currentTimeMillis(),
        val isStreaming: Boolean = false
    ) : ChatMessage()

    data class SystemAlert(
        override val content: String,
        override val timestamp: Long = java.lang.System.currentTimeMillis()
    ) : ChatMessage()

    data class ToolCall(
        val toolName: String,
        val arguments: Map<String, String>,
        val result: String? = null,
        override val timestamp: Long = java.lang.System.currentTimeMillis()
    ) : ChatMessage() {
        override val content: String
            get() = if (result != null) {
                "Tool: $toolName\nResult: $result"
            } else {
                "Calling tool: $toolName..."
            }
    }

    // Keep existing code using ChatMessage.System working
    companion object {
        // Typealias inside companion – ChatMessage.System refers to SystemAlert
        // We also provide a factory function for convenience
        fun System(content: String, timestamp: Long = java.lang.System.currentTimeMillis()) =
            SystemAlert(content, timestamp)
    }
}