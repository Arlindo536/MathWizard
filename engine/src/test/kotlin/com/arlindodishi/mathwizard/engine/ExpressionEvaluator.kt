package com.arlindodishi.mathwizard.engine

/**
 * Independent checker for generated prompts: a tiny recursive-descent evaluator over
 * + − × ÷ and parentheses. Fails if a division is not exact.
 */
object ExpressionEvaluator {
    fun evaluate(expression: String): Int {
        val tokens = Regex("""\d+|[+−×÷()]""").findAll(expression).map { it.value }.toList()
        val parser = Parser(tokens)
        val value = parser.expression()
        check(parser.done()) { "Trailing tokens in '$expression'" }
        return value
    }

    private class Parser(private val tokens: List<String>) {
        private var pos = 0

        fun done() = pos == tokens.size

        fun expression(): Int {
            var value = term()
            while (!done() && tokens[pos] in setOf("+", "−")) {
                val op = tokens[pos++]
                val rhs = term()
                value = if (op == "+") value + rhs else value - rhs
            }
            return value
        }

        private fun term(): Int {
            var value = factor()
            while (!done() && tokens[pos] in setOf("×", "÷")) {
                val op = tokens[pos++]
                val rhs = factor()
                value = if (op == "×") {
                    value * rhs
                } else {
                    check(rhs != 0 && value % rhs == 0) { "Inexact division $value / $rhs" }
                    value / rhs
                }
            }
            return value
        }

        private fun factor(): Int {
            val token = tokens[pos++]
            if (token == "(") {
                val value = expression()
                check(tokens[pos++] == ")")
                return value
            }
            return token.toInt()
        }
    }
}
