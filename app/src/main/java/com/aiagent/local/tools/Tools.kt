package com.aiagent.local.tools

import kotlinx.serialization.Serializable

/**
 * Tool call definitions and results.
 */
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

/**
 * Individual tool implementations.
 * Each tool returns a result string.
 */
object ToolImplementations {

    fun calculator(expression: String): String {
        return try {
            // Simple arithmetic evaluator (safe subset)
            val sanitized = expression.replace(Regex("[^0-9+\\-*/.() ]"), "")
            val result = evaluateExpression(sanitized)
            result.toString()
        } catch (e: Exception) {
            "Error evaluating expression: ${e.message}"
        }
    }

    fun searchKnowledgeBase(query: String, retriever: suspend (String) -> String): String {
        // retriever is a lambda passed from RAG module
        return "search_knowledge_base called with: $query"
    }

    fun getCurrentTime(): String {
        return java.text.SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss",
            java.util.Locale.getDefault()
        ).format(java.util.Date())
    }

    fun getBatteryLevel(context: android.content.Context): String {
        val batteryManager = context.getSystemService(android.content.Context.BATTERY_SERVICE)
                as android.os.BatteryManager
        val level = batteryManager.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)
        return "$level%"
    }

    /**
     * Simple expression evaluator for basic arithmetic.
     * Replace with exp4j or similar library for production use.
     */
    private fun evaluateExpression(expression: String): Double {
        // Tokenize and evaluate in a simple recursive descent parser
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
            while (current().type == Token.Type.OPERATOR &&
                current().value in "+-") {
                val op = current().value
                advance()
                val right = parseTerm()
                result = if (op == "+") result + right else result - right
            }
            return result
        }

        fun parseTerm(): Double {
            var result = parseFactor()
            while (current().type == Token.Type.OPERATOR &&
                current().value in "*/") {
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
                    advance() // skip (
                    val result = parseExpression()
                    advance() // skip )
                    result
                }
                Token.Type.OPERATOR -> {
                    if (current().value == "-") {
                        advance()
                        -parseFactor()
                    } else {
                        throw IllegalArgumentException("Unexpected operator")
                    }
                }
                else -> throw IllegalArgumentException("Unexpected token")
            }
        }

        return parseExpression()
    }
}