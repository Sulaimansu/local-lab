package com.aiagent.local.data

import kotlinx.serialization.Serializable

sealed class Message {
    abstract val content: String
    abstract val timestamp: Long

    data class User(
        override val content: String,
        override val timestamp: Long = System.currentTimeMillis()
    ) : Message()

    data class Bot(
        override val content: String,
        override val timestamp: Long = System.currentTimeMillis(),
        val isStreaming: Boolean = false
    ) : Message()

    data class System(
        override val content: String,
        override val timestamp: Long = System.currentTimeMillis()
    ) : Message()

    data class ToolCall(
        val toolName: String,
        val arguments: Map<String, String>,
        val result: String? = null,
        override val timestamp: Long = System.currentTimeMillis()
    ) : Message() {
        override val content: String
            get() = if (result != null) {
                "Tool: $toolName\nResult: $result"
            } else {
                "Calling tool: $toolName..."
            }
    }
}