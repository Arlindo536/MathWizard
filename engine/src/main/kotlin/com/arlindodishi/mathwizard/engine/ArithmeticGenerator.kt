package com.arlindodishi.mathwizard.engine

import kotlin.random.Random

/**
 * Fire school: + − × ÷ and, at high difficulty, two-step expressions with precedence.
 * All answers are non-negative integers; division is always exact.
 *
 * Unlock thresholds: − at 15, × at 35, ÷ at 55, two-step at 75.
 */
object ArithmeticGenerator : ProblemGenerator {
    override val school = School.ARITHMETIC

    const val PLUS = "+"
    const val MINUS = "−"
    const val TIMES = "×"
    const val DIVIDE = "÷"

    internal enum class Op(val unlockAt: Int) { ADD(0), SUB(15), MUL(35), DIV(55), TWO_STEP(75) }

    internal fun unlockedOps(difficulty: Int): List<Op> = Op.entries.filter { difficulty >= it.unlockAt }

    override fun generate(difficulty: Int, random: Random): Problem {
        val d = difficulty.coerceIn(MIN_DIFFICULTY, MAX_DIFFICULTY)
        val (prompt, answer, candidates) = when (unlockedOps(d).random(random)) {
            Op.ADD -> addition(d, random)
            Op.SUB -> subtraction(d, random)
            Op.MUL -> multiplication(d, random)
            Op.DIV -> division(d, random)
            Op.TWO_STEP -> twoStep(d, random)
        }
        return Problem(
            school = school,
            difficulty = d,
            prompt = prompt,
            answer = answer.toString(),
            choices = buildIntChoices(answer, candidates, random),
        )
    }

    private data class Draft(val prompt: String, val answer: Int, val candidates: List<Int>)

    /** Largest operand for + and −: 5 at d=0, 20 at d=30, 100 at d=70, 490 at d=100. */
    internal fun addMax(d: Int): Int = when {
        d < 30 -> 5 + d / 2
        d < 70 -> 20 + (d - 30) * 2
        else -> 100 + (d - 70) * 13
    }

    /** Largest "big" factor for × and ÷: tables up to 5..12, then two-digit factors. */
    internal fun factorMax(d: Int): Int = when {
        d < 80 -> (5 + (d - 35).coerceAtLeast(0) / 6).coerceAtMost(12)
        else -> 12 + (d - 80) / 2
    }

    private fun operand(max: Int, random: Random): Int = random.nextInt((max / 4).coerceAtLeast(1), max + 1)

    /** Off-by-one/two slips, plus carry/borrow slips (±10) once the answer has two digits. */
    private fun commonSlips(answer: Int): List<Int> =
        listOf(answer + 1, answer - 1, answer + 2, answer - 2) +
            if (answer >= 10) listOf(answer + 10, answer - 10) else emptyList()

    private fun addition(d: Int, random: Random): Draft {
        val a = operand(addMax(d), random)
        val b = operand(addMax(d), random)
        return Draft("$a $PLUS $b", a + b, commonSlips(a + b))
    }

    private fun subtraction(d: Int, random: Random): Draft {
        val x = operand(addMax(d), random)
        val y = operand(addMax(d), random)
        val a = maxOf(x, y)
        val b = minOf(x, y)
        return Draft("$a $MINUS $b", a - b, commonSlips(a - b) + (a + b))
    }

    /** Small factor stays a table factor (≤ 9 at two-digit levels) so problems stay mental. */
    private fun factors(d: Int, random: Random): Pair<Int, Int> {
        val big = random.nextInt(2, factorMax(d) + 1)
        val small = random.nextInt(2, minOf(factorMax(d), if (d >= 80) 9 else 12) + 1)
        return if (random.nextBoolean()) big to small else small to big
    }

    private fun multiplication(d: Int, random: Random): Draft {
        val (a, b) = factors(d, random)
        val p = a * b
        return Draft("$a $TIMES $b", p, listOf(a * (b + 1), a * (b - 1), (a + 1) * b, (a - 1) * b, a + b) + commonSlips(p))
    }

    private fun division(d: Int, random: Random): Draft {
        val (divisor, quotient) = factors(d, random)
        val dividend = divisor * quotient
        return Draft("$dividend $DIVIDE $divisor", quotient, commonSlips(quotient) + divisor)
    }

    /** a + b × c, a × b − c, or (a + b) × c. Wrong evaluation order is the key distractor. */
    private fun twoStep(d: Int, random: Random): Draft {
        val small = (factorMax(d) - 2).coerceIn(4, 12)
        val a = random.nextInt(2, small + 1)
        val b = random.nextInt(2, small + 1)
        val c = random.nextInt(2, small + 1)
        val add = random.nextInt(2, addMax(d) / 5 + 2)
        return when (random.nextInt(3)) {
            0 -> Draft("$add $PLUS $b $TIMES $c", add + b * c, listOf((add + b) * c, add + b + c, add * b + c))
            1 -> {
                val minus = random.nextInt(1, a * b)
                Draft("$a $TIMES $b $MINUS $minus", a * b - minus, listOf(a * (b - minus), a * b + minus, a * b - minus + 10))
            }
            else -> Draft("($a $PLUS $b) $TIMES $c", (a + b) * c, listOf(a + b * c, a * b + c, (a + b) * (c + 1)))
        }.let { it.copy(candidates = it.candidates + commonSlips(it.answer)) }
    }
}
