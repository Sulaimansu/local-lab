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
            Evaluator(sanitized).eval().toString()
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

    // ----- Simple expression evaluator using a class (no nesting issues) -----
    private class Evaluator(private val expr: String) {
        private val tokens = tokenize(expr)
        private var pos = 0

        private fun current() = tokens[pos]
        private fun advance() { pos++ }

        fun eval(): Double {
            val result = expression()
            if (current().type != TokenType.EOF) throw Exception("Unexpected token")
            return result
        }

        private fun expression(): Double {
            var result = term()
            while (current().type == TokenType.OP && current().value in "+-") {
                val op = current().value
                advance()
                val right = term()
                result = if (op == "+") result + right else result - right
            }
            return result
        }

        private fun term(): Double {
            var result = factor()
            while (current().type == TokenType.OP && current().value in "*/") {
                val op = current().value
                advance()
                val right = factor()
                result = if (op == "*") result * right else result / right
            }
            return result
        }

        private fun factor(): Double {
            return when (current().type) {
                TokenType.NUMBER -> {
                    val value = current().value.toDouble()
                    advance()
                    value
                }
                TokenType.LPAREN -> {
                    advance()
                    val result = expression()
                    if (current().type != TokenType.RPAREN) throw Exception("Missing ')'")
                    advance()
                    result
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

        companion object {
            private data class Token(val type: TokenType, val value: String)
            private enum class TokenType { NUMBER, OP, LPAREN, RPAREN, EOF }

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
                        c == '(' -> tokens.add(Token(TokenType.LPAREN, "("))
                        c == ')' -> tokens.add(Token(TokenType.RPAREN, ")"))
                    }
                    i++
                }
                tokens.add(Token(TokenType.EOF, ""))
                return tokens
            }
        }
    }
}