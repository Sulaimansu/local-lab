package com.aiagent.local.tools

import android.content.Context
import kotlinx.serialization.Serializable

@Serializable
data class ToolCallRequest(
    val tool: String,
    val args: Map<String, String>
)

@Serializable
data class ToolCallResult(
    val tool: String,
    val success: Boolean,
    val result: String
)

object ToolImplementations {

    fun calculator(expression: String): String {
        return try {
            val sanitized = expression.replace(Regex("[^0-9+\\-*/.() ]"), "")
            evaluateSimple(sanitized).toString()
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }

    fun getCurrentTime(): String {
        return java.text.SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss",
            java.util.Locale.getDefault()
        ).format(java.util.Date())
    }

    fun getBatteryLevel(context: Context): String {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE)
                as android.os.BatteryManager
        val level = batteryManager.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)
        return "$level%"
    }

    // --- Simple expression evaluator (recursive descent) using top-level private functions ---

    private data class Token(val type: TokenType, val value: String)

    private enum class TokenType { NUMBER, OP, LPAR, RPAR, EOF }

    private fun tokenize(expr: String): List<Token> {
        val tokens = mutableListOf<Token>()
        var i = 0
        while (i < expr.length) {
            val c = expr[i]
            when {
                c.isDigit() || c == '.' -> {
                    val start = i
                    while (i < expr.length && (expr[i].isDigit() || expr[i] == '.')) i++
                    tokens.add(Token(TokenType.NUMBER, expr.substring(start, i)))
                    continue
                }
                c in "+-*/" -> tokens.add(Token(TokenType.OP, c.toString()))
                c == '(' -> tokens.add(Token(TokenType.LPAR, "("))
                c == ')' -> tokens.add(Token(TokenType.RPAR, ")"))
            }
            i++
        }
        tokens.add(Token(TokenType.EOF, ""))
        return tokens
    }

    private fun evaluateSimple(expression: String): Double {
        val tokens = tokenize(expression)
        var pos = 0

        fun current() = tokens[pos]
        fun advance() { pos++ }

        fun expr(): Double {
            var result = term()
            while (current().type == TokenType.OP && current().value in "+-") {
                val op = current().value
                advance()
                val right = term()
                result = if (op == "+") result + right else result - right
            }
            return result
        }

        fun term(): Double {
            var result = factor()
            while (current().type == TokenType.OP && current().value in "*/") {
                val op = current().value
                advance()
                val right = factor()
                result = if (op == "*") result * right else result / right
            }
            return result
        }

        fun factor(): Double {
            return when (current().type) {
                TokenType.NUMBER -> {
                    val v = current().value.toDouble()
                    advance()
                    v
                }
                TokenType.LPAR -> {
                    advance()
                    val v = expr()
                    if (current().type != TokenType.RPAR) throw Exception("Missing ')")
                    advance()
                    v
                }
                TokenType.OP -> {
                    if (current().value == "-") {
                        advance()
                        -factor()
                    } else throw Exception("Unexpected operator")
                }
                else -> throw Exception("Unexpected token")
            }
        }

        val result = expr()
        if (current().type != TokenType.EOF) throw Exception("Extra tokens")
        return result
    }
}