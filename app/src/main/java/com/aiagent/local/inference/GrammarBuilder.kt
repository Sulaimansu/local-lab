package com.aiagent.local.inference

import com.aiagent.local.tools.ToolRegistry

/**
 * Builds GBNF grammar strings for llama.cpp constrained decoding.
 * This ensures model output is always valid JSON for tool calls.
 *
 * Based on the pattern from the llama.cpp GBNF workshop:
 * - Define root structure
 * - Restrict tool names to known set
 * - Force valid JSON shape
 */
object GrammarBuilder {

    /**
     * Build grammar that forces JSON tool call format:
     * {"tool": "<name>", "args": {"<key>": "<value>", ...}}
     */
    fun buildToolCallGrammar(toolRegistry: ToolRegistry): String {
        val toolNames = toolRegistry.getToolNames()
        val toolNameAlternatives = toolNames.joinToString(" | ") { "\"$it\"" }

        return """
            root ::= "{" ws "\"tool\"" ws ":" ws tool-name ws "," ws "\"args\"" ws ":" ws "{" ws args ws "}" ws "}"
            tool-name ::= $toolNameAlternatives
            args ::= pair ("," ws pair)*
            pair ::= string ws ":" ws value
            value ::= string | number | bool
            bool ::= "true" | "false"
            string ::= "\"" [^"]* "\""
            number ::= "-"? [0-9]+ ("." [0-9]+)?
            ws ::= [ \t\n]*
        """.trimIndent()
    }

    /**
     * Build grammar for final answer (plain text, no structure required).
     */
    fun buildFreeTextGrammar(): String {
        return "root ::= [\\s\\S]*"
    }
}