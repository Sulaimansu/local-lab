package com.aiagent.local.tools

import android.content.Context

class ToolExecutor(private val context: Context) {

    fun execute(request: ToolCallRequest): ToolCallResult {
        return try {
            val result = when (request.tool) {
                "calculator" -> {
                    val expr = request.args["expression"] ?: "0"
                    ToolImplementations.calculator(expr)
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