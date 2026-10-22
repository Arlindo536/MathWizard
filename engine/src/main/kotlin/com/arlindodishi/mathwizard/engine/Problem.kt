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
 */
data class Problem(
    val school: School,
    val difficulty: Int,
    val prompt: String,
    val answer: String,
    val choices: List<String>,
) {
    fun isCorrect(input: String): Boolean = input.trim() == answer
}
