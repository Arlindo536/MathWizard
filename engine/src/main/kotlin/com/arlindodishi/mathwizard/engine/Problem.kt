package com.arlindodishi.mathwizard.engine

/** A spell school is a math topic; schools are unlocked progressively. */
enum class School {
    ARITHMETIC,
    FRACTIONS,
    ALGEBRA,
    POWERS,
    GEOMETRY,
    SEQUENCES,
}

/**
 * One generated problem. [answer] and [choices] are display strings so that every school
 * (integers, fractions, ...) shares one shape; [choices] always contains [answer].
 *
 * Symbolic prompts ("3/4 + 1/6", "2x + 3 = 11") are language-neutral. Word problems set
 * [promptKey] and [promptArgs] for the app to localise; [prompt] is then an English fallback.
 */
data class Problem(
    val school: School,
    val difficulty: Int,
    val prompt: String,
    val answer: String,
    val choices: List<String>,
    val promptKey: String? = null,
    val promptArgs: List<String> = emptyList(),
) {
    fun isCorrect(input: String): Boolean = input.trim() == answer
}
