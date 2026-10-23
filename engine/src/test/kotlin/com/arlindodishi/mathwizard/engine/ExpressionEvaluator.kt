package com.arlindodishi.mathwizard.engine

/**
 * Independent checker for generated prompts: a tiny recursive-descent evaluator over
 * + − × ÷, parentheses, superscript exponents (7²) and √ / ∛ prefixes.
 * Fails if a division or a root is not exact.
 */
object ExpressionEvaluator {
    private const val SUPERSCRIPTS = "⁰¹²³⁴⁵⁶⁷⁸⁹"

    fun evaluate(expression: String): Int {
        val tokens = Regex("""\d+|[⁰¹²³⁴⁵⁶⁷⁸⁹]+|[+−×÷()√∛]""").findAll(expression).map { it.value }.toList()
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
            val token = tokens[pos]
            if (token == "√" || token == "∛") {
                pos++
                val inner = factor()
                val degree = if (token == "√") 2 else 3
                val root = (0..inner).first { power(it, degree) >= inner }
                check(power(root, degree) == inner) { "Inexact root of $inner" }
                return root
            }
            var value = primary()
            if (!done() && tokens[pos][0] in SUPERSCRIPTS) {
                val exponent = tokens[pos++].map { SUPERSCRIPTS.indexOf(it) }.joinToString("").toInt()
                value = power(value, exponent)
            }
            return value
        }

        private fun primary(): Int {
            val token = tokens[pos++]
            if (token == "(") {
                val value = expression()
                check(tokens[pos++] == ")")
                return value
            }
            return token.toInt()
        }

        private fun power(base: Int, exponent: Int): Int = (0 until exponent).fold(1) { acc, _ -> acc * base }
    }
}
