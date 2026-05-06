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
            evaluateExpression(sanitized).toString()
        } catch (e: Exception) {
            "Error evaluating expression: ${e.message}"
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

    private fun evaluateExpression(expression: String): Double {
        // Simple recursive descent parser
        data class Token(val type: Type, val value: String) {
            enum class Type { NUMBER, OPERATOR, LPAREN, RPAREN, EOF }
        }

        fun tokenize(expr: String): List<Token> {
            val tokens = mutableListOf<Token>()
            var i = 0
            while (i < expr.length) {
                when {
                    expr[i].isDigit() || expr[i] == '.' -> {
                        val start = i
                        while (i < expr.length && (expr[i].isDigit() || expr[i] == '.')) i++
                        tokens.add(Token(Token.Type.NUMBER, expr.substring(start, i)))
                        continue
                    }
                    expr[i] in "+-*/" -> tokens.add(Token(Token.Type.OPERATOR, expr[i].toString()))
                    expr[i] == '(' -> tokens.add(Token(Token.Type.LPAREN, "("))
                    expr[i] == ')' -> tokens.add(Token(Token.Type.RPAREN, ")"))
                }
                i++
            }
            tokens.add(Token(Token.Type.EOF, ""))
            return tokens
        }

        val tokens = tokenize(expression)
        var pos = 0

        fun current() = tokens[pos]
        fun advance() { pos++ }

        fun parseExpression(): Double {
            var result = parseTerm()
            while (current().type == Token.Type.OPERATOR && current().value in "+-") {
                val op = current().value
                advance()
                val right = parseTerm()
                result = if (op == "+") result + right else result - right
            }
            return result
        }

        fun parseTerm(): Double {
            var result = parseFactor()
            while (current().type == Token.Type.OPERATOR && current().value in "*/") {
                val op = current().value
                advance()
                val right = parseFactor()
                result = if (op == "*") result * right else result / right
            }
            return result
        }

        fun parseFactor(): Double {
            return when (current().type) {
                Token.Type.NUMBER -> {
                    val value = current().value.toDouble()
                    advance()
                    value
                }
                Token.Type.LPAREN -> {
                    advance()
                    val result = parseExpression()
                    advance()
                    result
                }
                Token.Type.OPERATOR -> {
                    if (current().value == "-") {
                        advance()
                        -parseFactor()
                    } else throw IllegalArgumentException("Unexpected operator")
                }
                else -> throw IllegalArgumentException("Unexpected token")
            }
        }

        return parseExpression()
    }
}