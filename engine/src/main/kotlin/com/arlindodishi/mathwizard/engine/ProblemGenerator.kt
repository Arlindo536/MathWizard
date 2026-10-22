package com.arlindodishi.mathwizard.engine

import kotlin.random.Random

const val MIN_DIFFICULTY = 0
const val MAX_DIFFICULTY = 100
const val CHOICE_COUNT = 4

/** Produces problems for one school. Implementations must be pure: same inputs, same problem. */
interface ProblemGenerator {
    val school: School

    /** [difficulty] is in [MIN_DIFFICULTY]..[MAX_DIFFICULTY]. */
    fun generate(difficulty: Int, random: Random): Problem
}

object Problems {
    private val generators: Map<School, ProblemGenerator> = listOf<ProblemGenerator>(
        ArithmeticGenerator,
    ).associateBy { it.school }

    val availableSchools: Set<School> get() = generators.keys

    fun generate(school: School, difficulty: Int, seed: Long): Problem {
        val generator = requireNotNull(generators[school]) { "No generator for $school yet" }
        return generator.generate(difficulty.coerceIn(MIN_DIFFICULTY, MAX_DIFFICULTY), Random(seed))
    }
}

/**
 * Picks [CHOICE_COUNT] - 1 distinct wrong options from [candidates] (filtered by [isValid]),
 * tops up with nearby values if needed, and returns all options shuffled.
 */
internal fun buildIntChoices(
    answer: Int,
    candidates: List<Int>,
    random: Random,
    isValid: (Int) -> Boolean = { it >= 0 },
): List<String> {
    val wrong = candidates
        .filter { it != answer && isValid(it) }
        .distinct()
        .shuffled(random)
        .take(CHOICE_COUNT - 1)
        .toMutableList()
    var step = 1
    while (wrong.size < CHOICE_COUNT - 1) {
        for (candidate in listOf(answer + step, answer - step)) {
            if (wrong.size < CHOICE_COUNT - 1 && isValid(candidate) && candidate !in wrong) {
                wrong += candidate
            }
        }
        step++
    }
    return (wrong + answer).shuffled(random).map { it.toString() }
}
