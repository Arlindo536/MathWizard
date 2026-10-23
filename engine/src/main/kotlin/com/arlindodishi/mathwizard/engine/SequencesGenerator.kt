package com.arlindodishi.mathwizard.engine

import kotlin.random.Random

/**
 * Shadow school: find the missing term of a number sequence ("3, 5, 7, 9, ?").
 * Tiers: small arithmetic steps (0), bigger or falling steps and doubling (20), ×2/×3,
 * squares and alternating steps (45), Fibonacci-like, growing steps, cubes and a missing
 * middle term (70). Wrong choices are checked so that none of them also fits a simple rule.
 */
object SequencesGenerator : ProblemGenerator {
    override val school = School.SEQUENCES

    const val BLANK = "?"

    private data class Draft(val terms: List<Int>, val missing: Int, val candidates: List<Int>)

    private enum class Kind(val unlockAt: Int) {
        SMALL_STEP(0), BIG_STEP(20), FALLING(20), DOUBLING(20),
        GEOMETRIC(45), SQUARES(45), ALTERNATING(45),
        FIBONACCI(70), GROWING_STEP(70), CUBES(70), MISSING_MIDDLE(70),
    }

    override fun generate(difficulty: Int, random: Random): Problem {
        val d = difficulty.coerceIn(MIN_DIFFICULTY, MAX_DIFFICULTY)
        return problem(d, draft(Kind.entries.filter { d >= it.unlockAt }.random(random), random), random)
    }

    /** Boss: a missing middle term or a two-rule sequence. */
    override fun generateBoss(difficulty: Int, random: Random): Problem {
        val d = difficulty.coerceIn(MIN_DIFFICULTY, MAX_DIFFICULTY)
        val kinds = if (d < 45) listOf(Kind.ALTERNATING, Kind.SQUARES) else listOf(Kind.MISSING_MIDDLE, Kind.ALTERNATING, Kind.GROWING_STEP, Kind.FIBONACCI)
        return problem(d, draft(kinds.random(random), random), random)
    }

    private fun problem(d: Int, draft: Draft, random: Random): Problem {
        val answer = draft.terms[draft.missing]
        val shown = draft.terms.mapIndexed { i, t -> if (i == draft.missing) BLANK else t.toString() }
        // A wrong choice must not complete the sequence under any simple rule.
        val fitsSomeRule = { v: Int -> SequenceRules.fitsAny(draft.terms.toMutableList().also { it[draft.missing] = v }) }
        return Problem(
            school = school,
            difficulty = d,
            prompt = shown.joinToString(", "),
            answer = answer.toString(),
            choices = buildIntChoices(answer, draft.candidates + listOf(answer + 1, answer - 1, answer + 2), random) {
                it >= 0 && !fitsSomeRule(it)
            },
        )
    }

    private fun draft(kind: Kind, random: Random): Draft = when (kind) {
        Kind.SMALL_STEP -> arithmetic(random.nextInt(1, 11), random.nextInt(1, 6), 5)
        Kind.BIG_STEP -> arithmetic(random.nextInt(1, 30), random.nextInt(6, 13), 5)
        Kind.FALLING -> {
            val step = random.nextInt(2, 10)
            arithmetic(step * 5 + random.nextInt(0, 20), -step, 5)
        }
        Kind.DOUBLING -> geometric(random.nextInt(1, 6), 2, 5)
        Kind.GEOMETRIC -> geometric(random.nextInt(1, 4), random.nextInt(2, 4), 5)
        Kind.SQUARES -> {
            val start = random.nextInt(1, 7)
            val terms = (start until start + 5).map { it * it }
            Draft(terms, 4, listOf(terms[3] + (terms[3] - terms[2]), terms[3] * 2))
        }
        Kind.ALTERNATING -> {
            var a = random.nextInt(1, 8)
            var b = random.nextInt(1, 8)
            if (a == b) b += 1
            if (random.nextBoolean()) a = b.also { b = a }
            val terms = (0 until 6).runningFold(random.nextInt(1, 15)) { acc, i -> acc + if (i % 2 == 0) a else b }.take(6)
            Draft(terms, 5, listOf(terms[4] + terms[4] - terms[3], terms[4] + a + b))
        }
        Kind.FIBONACCI -> {
            val terms = mutableListOf(random.nextInt(1, 6), random.nextInt(1, 6))
            while (terms.size < 6) terms += terms[terms.size - 1] + terms[terms.size - 2]
            Draft(terms, 5, listOf(terms[4] + terms[4] - terms[3], terms[4] * 2, terms[4] + terms[2]))
        }
        Kind.GROWING_STEP -> {
            val firstStep = random.nextInt(1, 5)
            val growth = random.nextInt(1, 4)
            val terms = (0 until 5).runningFold(random.nextInt(1, 10)) { acc, i -> acc + firstStep + i * growth }.take(6)
            Draft(terms, 5, listOf(terms[4] + terms[4] - terms[3], terms[4] + firstStep))
        }
        Kind.CUBES -> {
            val start = random.nextInt(1, 4)
            val terms = (start until start + 5).map { it * it * it }
            Draft(terms, 4, listOf(terms[3] + (terms[3] - terms[2]), terms[3] * 2))
        }
        Kind.MISSING_MIDDLE -> {
            val g = geometric(random.nextInt(1, 5), random.nextInt(2, 4), 5)
            val missing = random.nextInt(1, 4)
            Draft(g.terms, missing, listOf((g.terms[missing - 1] + g.terms[missing + 1]) / 2, g.terms[missing - 1] * 2 + 1))
        }
    }

    private fun arithmetic(start: Int, step: Int, known: Int): Draft {
        val terms = List(known + 1) { start + it * step }
        val last = terms[known - 1]
        return Draft(terms, known, listOf(last + 2 * step, last - step, last + step + 1))
    }

    private fun geometric(start: Int, ratio: Int, known: Int): Draft {
        val terms = List(known + 1) { i -> (0 until i).fold(start) { acc, _ -> acc * ratio } }
        val last = terms[known - 1]
        return Draft(terms, known, listOf(last + (last - terms[known - 2]), last * (ratio + 1), last + ratio))
    }
}

/** Simple rules a complete sequence may follow; used to keep wrong choices unambiguous. */
object SequenceRules {
    fun fitsAny(terms: List<Int>): Boolean =
        isArithmetic(terms) || isGeometric(terms) || hasConstantSecondDifference(terms) ||
            hasConstantThirdDifference(terms) || hasAlternatingSteps(terms) || isFibonacciLike(terms)

    private fun diffs(t: List<Int>) = t.zipWithNext { a, b -> b - a }

    fun isArithmetic(t: List<Int>) = diffs(t).toSet().size == 1

    fun isGeometric(t: List<Int>): Boolean =
        t.all { it != 0 } && t.zipWithNext().all { (a, b) -> b % a == 0 } && t.zipWithNext { a, b -> b / a }.toSet().size == 1

    fun hasConstantSecondDifference(t: List<Int>) = t.size >= 4 && diffs(diffs(t)).toSet().size == 1

    /** Cubes and other third-degree sequences. */
    fun hasConstantThirdDifference(t: List<Int>) = t.size >= 5 && diffs(diffs(diffs(t))).toSet().size == 1

    fun hasAlternatingSteps(t: List<Int>): Boolean {
        val d = diffs(t)
        return d.size >= 4 && d.indices.all { d[it] == d[it % 2] }
    }

    fun isFibonacciLike(t: List<Int>) = t.size >= 4 && (2 until t.size).all { t[it] == t[it - 1] + t[it - 2] }
}
