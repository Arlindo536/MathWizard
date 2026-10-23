package com.arlindodishi.mathwizard.engine

import com.arlindodishi.mathwizard.engine.ArithmeticGenerator.DIVIDE
import com.arlindodishi.mathwizard.engine.ArithmeticGenerator.MINUS
import com.arlindodishi.mathwizard.engine.ArithmeticGenerator.PLUS
import kotlin.random.Random

/**
 * Lightning school: solve a linear equation for x. The answer is always a positive whole
 * number. Tiers: x ± a = b (0), ax = b, x ÷ a = b, ax + b = c (30), ax − b = c and
 * a(x ± b) = c (60), x on both sides and x ÷ a + b = c (85).
 */
object AlgebraGenerator : ProblemGenerator {
    override val school = School.ALGEBRA

    private data class Draft(val prompt: String, val x: Int, val candidates: List<Int>)

    private enum class Form(val unlockAt: Int) {
        PLUS_CONST(0), MINUS_CONST(0), TIMES(30), DIVIDED(30), TWO_STEP_PLUS(30),
        TWO_STEP_MINUS(60), BRACKET_PLUS(60), BRACKET_MINUS(60), BOTH_SIDES(85), DIVIDED_PLUS(85),
    }

    override fun generate(difficulty: Int, random: Random): Problem {
        val d = difficulty.coerceIn(MIN_DIFFICULTY, MAX_DIFFICULTY)
        val form = Form.entries.filter { d >= it.unlockAt }.pickFavouringNewest(random)
        return problem(d, draft(form, d, random), random)
    }

    /** Boss: always a two-step form, with x on both sides from difficulty 50. */
    override fun generateBoss(difficulty: Int, random: Random): Problem {
        val d = difficulty.coerceIn(MIN_DIFFICULTY, MAX_DIFFICULTY)
        val forms = when {
            d < 25 -> listOf(Form.TWO_STEP_PLUS, Form.TWO_STEP_MINUS)
            d < 50 -> listOf(Form.TWO_STEP_MINUS, Form.BRACKET_PLUS, Form.BRACKET_MINUS)
            else -> listOf(Form.BOTH_SIDES, Form.DIVIDED_PLUS, Form.BRACKET_MINUS)
        }
        return problem(d, draft(forms.random(random), maxOf(d, 40), random), random)
    }

    private fun problem(d: Int, draft: Draft, random: Random) = Problem(
        school = school,
        difficulty = d,
        prompt = draft.prompt,
        answer = draft.x.toString(),
        choices = buildIntChoices(draft.x, draft.candidates + listOf(draft.x + 1, draft.x - 1, draft.x + 2), random) { it > 0 },
    )

    /** Largest x: 10 at d=0 rising to 30. */
    private fun xMax(d: Int) = 10 + d / 5

    private fun coefficient(d: Int, random: Random) = random.nextInt(2, if (d < 60) 7 else 10)

    private fun draft(form: Form, d: Int, random: Random): Draft {
        val x = random.nextInt(1, xMax(d) + 1)
        val a = coefficient(d, random)
        val b = random.nextInt(1, 10 + d / 5)
        return when (form) {
            Form.PLUS_CONST -> Draft("x $PLUS $b = ${x + b}", x, listOf(x + 2 * b, b))
            Form.MINUS_CONST -> {
                val c = x // x − b = c, so the solution is c + b
                Draft("x $MINUS $b = $c", c + b, listOf(c, c - b))
            }
            Form.TIMES -> Draft("${a}x = ${a * x}", x, listOf(a * x - a, a * x + a, a * x / 2))
            Form.DIVIDED -> {
                val quotient = x // x ÷ a = quotient, so the solution is a · quotient
                Draft("x $DIVIDE $a = $quotient", a * quotient, listOf(quotient, a + quotient, a * quotient + a))
            }
            Form.TWO_STEP_PLUS -> {
                val c = a * x + b
                Draft("${a}x $PLUS $b = $c", x, listOf(c - b, (c + b) / a, c / a))
            }
            Form.TWO_STEP_MINUS -> {
                val c = a * x - b
                if (c <= 0) return draft(Form.TWO_STEP_PLUS, d, random)
                Draft("${a}x $MINUS $b = $c", x, listOf(c + b, (c - b) / a, c / a))
            }
            Form.BRACKET_PLUS -> {
                val c = a * (x + b)
                Draft("$a(x $PLUS $b) = $c", x, listOf(c / a + b, c - b, (c - b) / a))
            }
            Form.BRACKET_MINUS -> {
                val xx = x + b
                val c = a * (xx - b)
                Draft("$a(x $MINUS $b) = $c", xx, listOf(c / a, c + b, (c + b) / a - 1))
            }
            Form.BOTH_SIDES -> {
                val smaller = random.nextInt(1, a)
                val e = random.nextInt(1, 15)
                // a·x + e = smaller·x + f  =>  f = (a − smaller)·x + e
                val f = (a - smaller) * x + e
                val left = if (a == 1) "x" else "${a}x"
                val right = if (smaller == 1) "x" else "${smaller}x"
                Draft("$left $PLUS $e = $right $PLUS $f", x, listOf((f - e) / (a + smaller), f - e, (f + e) / (a - smaller)))
            }
            Form.DIVIDED_PLUS -> {
                val c = x + b
                Draft("x $DIVIDE $a $PLUS $b = $c", a * x, listOf(c - b, a * c, a * (c + b)))
            }
        }
    }
}
