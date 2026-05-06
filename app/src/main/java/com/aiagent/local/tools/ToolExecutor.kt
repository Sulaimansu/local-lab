package com.aiagent.local.tools

import android.content.Context
import com.aiagent.local.rag.Retriever

/**
 * Executes tool calls from the LLM and returns results.
 */
class ToolExecutor(private val context: Context) {

    private var retriever: Retriever? = null

    fun setRetriever(retriever: Retriever) {
        this.retriever = retriever
    }

    suspend fun execute(request: ToolCallRequest): ToolCallResult {
        return try {
            val result = when (request.tool) {
                "calculator" -> {
                    val expr = request.args["expression"] ?: "0"
                    ToolImplementations.calculator(expr)
                }

                "search_knowledge_base" -> {
                    val query = request.args["query"] ?: ""
                    retriever?.search(query) ?: "Knowledge base not available"
                }

                "get_current_time" -> {
                    ToolImplementations.getCurrentTime()
                }

                "get_battery_level" -> {
                    ToolImplementations.getBatteryLevel(context)
                }

                else -> "Unknown tool: ${request.tool}"
            }
            ToolCallResult(request.tool, true, result)
        } catch (e: Exception) {
            ToolCallResult(request.tool, false, "Error: ${e.message}")
        }
    }
}