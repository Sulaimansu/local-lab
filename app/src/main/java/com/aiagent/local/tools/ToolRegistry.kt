package com.aiagent.local.tools

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

class ToolRegistry {

    private val json = Json { ignoreUnknownKeys = true }

    data class ToolDefinition(
        val name: String,
        val description: String,
        val parameters: Map<String, String>
    )

    private val tools = mutableMapOf<String, ToolDefinition>()

    fun registerTool(name: String, description: String, parameters: Map<String, String>) {
        tools[name] = ToolDefinition(name, description, parameters)
    }

    fun getToolNames(): List<String> = tools.keys.toList()

    fun getToolDefinitions(): List<ToolDefinition> = tools.values.toList()

    fun parseToolCall(jsonString: String): ToolCallRequest? {
        return try {
            val obj = json.decodeFromString<JsonObject>(jsonString)
            val toolName = obj["tool"]?.jsonPrimitive?.content ?: return null
            val argsObj = obj["args"] as? JsonObject ?: return null
            val args = argsObj.entries.associate {
                it.key to it.value.jsonPrimitive.content
            }
            ToolCallRequest(toolName, args)
        } catch (e: Exception) {
            null
        }
    }
}