package com.arlindodishi.mathwizard.engine

import com.arlindodishi.mathwizard.engine.ArithmeticGenerator.DIVIDE
import com.arlindodishi.mathwizard.engine.ArithmeticGenerator.MINUS
import com.arlindodishi.mathwizard.engine.ArithmeticGenerator.PLUS
import com.arlindodishi.mathwizard.engine.ArithmeticGenerator.TIMES
import kotlin.random.Random

/**
 * Earth school: powers and roots, written with superscripts (7², 2⁵) and √ / ∛.
 * Tiers: squares to 10² (0), squares to 12², square roots, small cubes (20), powers of 2, 3, 5,
 * and 10, squares and roots to 17 (45) then 20 (70), sums and differences of squares, √(a × b), cube roots,
 * quotients of powers (70). Roots are always exact.
 */
object PowersGenerator : ProblemGenerator {
    override val school = School.POWERS

    const val SQRT = "√"
    const val CBRT = "∛"

    private const val SUPERSCRIPTS = "⁰¹²³⁴⁵⁶⁷⁸⁹"

    fun superscript(n: Int): String = n.toString().map { SUPERSCRIPTS[it - '0'] }.joinToString("")

    private data class Draft(val prompt: String, val answer: Int, val candidates: List<Int>)

    private enum class Kind(val unlockAt: Int) {
        SQUARE(0), SQUARE_ROOT(20), CUBE(20), SMALL_BASE_POWER(45), POWER_OF_TEN(45),
        SUM_OF_SQUARES(70), DIFFERENCE_OF_SQUARES(70), ROOT_OF_PRODUCT(70), CUBE_ROOT(70), POWER_QUOTIENT(70),
    }

    override fun generate(difficulty: Int, random: Random): Problem {
        val d = difficulty.coerceIn(MIN_DIFFICULTY, MAX_DIFFICULTY)
        return problem(d, draft(Kind.entries.filter { d >= it.unlockAt }.pickFavouringNewest(random), d, random), random)
    }

    /** Boss: always a combined expression. */
    override fun generateBoss(difficulty: Int, random: Random): Problem {
        val d = difficulty.coerceIn(MIN_DIFFICULTY, MAX_DIFFICULTY)
        val kinds = if (d < 45) {
            listOf(Kind.SUM_OF_SQUARES, Kind.DIFFERENCE_OF_SQUARES)
        } else {
            listOf(Kind.SUM_OF_SQUARES, Kind.DIFFERENCE_OF_SQUARES, Kind.ROOT_OF_PRODUCT, Kind.POWER_QUOTIENT)
        }
        return problem(d, draft(kinds.random(random), d, random), random)
    }

    private fun problem(d: Int, draft: Draft, random: Random) = Problem(
        school = school,
        difficulty = d,
        prompt = draft.prompt,
        answer = draft.answer.toString(),
        choices = buildIntChoices(draft.answer, draft.candidates + listOf(draft.answer + 1, draft.answer - 1), random),
    )

    private fun squareBase(d: Int, random: Random) = random.nextInt(2, if (d < 20) 11 else if (d < 45) 13 else if (d < 70) 18 else 21)

    private fun draft(kind: Kind, d: Int, random: Random): Draft = when (kind) {
        Kind.SQUARE -> {
            val n = squareBase(d, random)
            Draft("$n${superscript(2)}", n * n, listOf(2 * n, n * n + n, (n + 1) * (n + 1)))
        }
        Kind.SQUARE_ROOT -> {
            val n = squareBase(d, random)
            Draft("$SQRT${n * n}", n, listOf(n * n / 2, n + 1, n - 1, 2 * n))
        }
        Kind.CUBE -> {
            val n = random.nextInt(2, if (d < 45) 5 else 6)
            Draft("$n${superscript(3)}", n * n * n, listOf(3 * n, n * n, n * n * n + n))
        }
        Kind.SMALL_BASE_POWER -> {
            // Small bases: 2 up to 2¹⁰, 3 up to 3⁶, 5 up to 5⁴.
            val (base, maxExponent) = listOf(2 to 10, 2 to 10, 3 to 6, 5 to 4).random(random)
            val e = random.nextInt(2, maxExponent + 1)
            val value = (1..e).fold(1) { acc, _ -> acc * base }
            Draft("$base${superscript(e)}", value, listOf(base * e, value / base * (base + 1), value * base))
        }
        Kind.POWER_OF_TEN -> {
            val e = random.nextInt(2, 5)
            val value = (1..e).fold(1) { acc, _ -> acc * 10 }
            Draft("10${superscript(e)}", value, listOf(10 * e, value * 10, value / 10))
        }
        Kind.SUM_OF_SQUARES -> {
            val a = random.nextInt(2, if (d < 70) 7 else 11)
            val b = random.nextInt(2, if (d < 70) 7 else 11)
            Draft("$a${superscript(2)} $PLUS $b${superscript(2)}", a * a + b * b, listOf((a + b) * (a + b), 2 * a + 2 * b, a * a + b))
        }
        Kind.DIFFERENCE_OF_SQUARES -> {
            val a = random.nextInt(3, if (d < 70) 9 else 13)
            val b = random.nextInt(2, a)
            Draft("$a${superscript(2)} $MINUS $b${superscript(2)}", a * a - b * b, listOf((a - b) * (a - b), 2 * a - 2 * b, a * a - b))
        }
        Kind.ROOT_OF_PRODUCT -> {
            val a = random.nextInt(2, 8)
            val b = random.nextInt(2, 8)
            Draft("$SQRT(${a * a} $TIMES ${b * b})", a * b, listOf(a + b, a * a * b, a * b + 1))
        }
        Kind.CUBE_ROOT -> {
            val n = random.nextInt(2, 7)
            Draft("$CBRT${n * n * n}", n, listOf(n * n, n * n * n / 3, n + 1))
        }
        Kind.POWER_QUOTIENT -> {
            val base = random.nextInt(2, 4)
            val m = random.nextInt(2, 6)
            val n = m + random.nextInt(1, 4)
            val answer = (1..(n - m)).fold(1) { acc, _ -> acc * base }
            Draft(
                "$base${superscript(n)} $DIVIDE $base${superscript(m)}",
                answer,
                listOf(n - m, base * (n - m), (1..(n / m).coerceAtLeast(1)).fold(1) { acc, _ -> acc * base } + 1),
            )
        }
    }
}
