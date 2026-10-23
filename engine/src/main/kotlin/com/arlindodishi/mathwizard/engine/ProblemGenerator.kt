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

    /** A harder, multi-step problem for boss floors. Defaults to a +20 difficulty problem. */
    fun generateBoss(difficulty: Int, random: Random): Problem =
        generate((difficulty + 20).coerceAtMost(MAX_DIFFICULTY), random)
}

object Problems {
    private val generators: Map<School, ProblemGenerator> = listOf<ProblemGenerator>(
        ArithmeticGenerator,
        FractionsGenerator,
        AlgebraGenerator,
        PowersGenerator,
        GeometryGenerator,
        SequencesGenerator,
    ).associateBy { it.school }

    val availableSchools: Set<School> get() = generators.keys

    fun generate(school: School, difficulty: Int, seed: Long, boss: Boolean = false): Problem {
        val generator = requireNotNull(generators[school]) { "No generator for $school yet" }
        val d = difficulty.coerceIn(MIN_DIFFICULTY, MAX_DIFFICULTY)
        return if (boss) generator.generateBoss(d, Random(seed)) else generator.generate(d, Random(seed))
    }
}

/**
 * Picks [CHOICE_COUNT] - 1 distinct wrong options from [candidates] (filtered by [isValid]),
 * tops up with [nearby] values (step 1, 2, ...) if needed, and returns all options shuffled.
 * Distinctness is by value ([T.equals]), so 2/4 and 1/2 never appear together.
 */
internal fun <T> buildChoices(
    answer: T,
    candidates: List<T>,
    random: Random,
    isValid: (T) -> Boolean,
    nearby: (step: Int) -> List<T>,
    format: (T) -> String = { it.toString() },
): List<String> {
    val wrong = candidates
        .filter { it != answer && isValid(it) }
        .distinct()
        .shuffled(random)
        .take(CHOICE_COUNT - 1)
        .toMutableList()
    var step = 1
    while (wrong.size < CHOICE_COUNT - 1) {
        for (candidate in nearby(step)) {
            if (wrong.size < CHOICE_COUNT - 1 && candidate != answer && isValid(candidate) && candidate !in wrong) {
                wrong += candidate
            }
        }
        step++
    }
    return (wrong + answer).shuffled(random).map(format)
}

/**
 * Picks one item of a list given in unlock order, favouring the most recently unlocked
 * (weights 1, 2, 3, ...), so higher difficulty really shows more of the harder kinds.
 */
internal fun <T> List<T>.pickFavouringNewest(random: Random): T {
    var r = random.nextInt(size * (size + 1) / 2)
    forEachIndexed { i, item ->
        r -= i + 1
        if (r < 0) return item
    }
    return last()
}

internal fun buildIntChoices(
    answer: Int,
    candidates: List<Int>,
    random: Random,
    isValid: (Int) -> Boolean = { it >= 0 },
): List<String> = buildChoices(answer, candidates, random, isValid, nearby = { listOf(answer + it, answer - it) })
