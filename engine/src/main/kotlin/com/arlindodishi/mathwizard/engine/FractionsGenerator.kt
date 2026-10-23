package com.arlindodishi.mathwizard.engine

import com.arlindodishi.mathwizard.engine.ArithmeticGenerator.DIVIDE
import com.arlindodishi.mathwizard.engine.ArithmeticGenerator.MINUS
import com.arlindodishi.mathwizard.engine.ArithmeticGenerator.PLUS
import com.arlindodishi.mathwizard.engine.ArithmeticGenerator.TIMES
import kotlin.random.Random

/**
 * Water school: fractions and percentages. Answers are non-negative fractions in lowest
 * terms ("5/6", or "3" when whole). Tiers: part of a quantity (0), same-denominator sums and
 * easy percentages (20), unlike denominators and fraction × whole (45), fraction × fraction,
 * fraction ÷ fraction and harder percentages (70).
 */
object FractionsGenerator : ProblemGenerator {
    override val school = School.FRACTIONS

    private data class Draft(val prompt: String, val answer: Fraction, val candidates: List<Fraction>)

    private enum class Kind(val unlockAt: Int) {
        PART_OF(0), SAME_DEN(20), EASY_PERCENT(20), UNLIKE_DEN(45), TIMES_WHOLE(45), PERCENT(45),
        TIMES_FRACTION(70), DIVIDE_FRACTION(70), HARD_PERCENT(70),
    }

    override fun generate(difficulty: Int, random: Random): Problem {
        val d = difficulty.coerceIn(MIN_DIFFICULTY, MAX_DIFFICULTY)
        val draft = when (Kind.entries.filter { d >= it.unlockAt }.pickFavouringNewest(random)) {
            Kind.PART_OF -> partOf(d, random)
            Kind.SAME_DEN -> sameDenominator(d, random)
            Kind.EASY_PERCENT -> percent(listOf(10, 20, 25, 50), random)
            Kind.UNLIKE_DEN -> unlikeDenominators(random)
            Kind.TIMES_WHOLE -> timesWhole(random)
            Kind.PERCENT -> percent(listOf(5, 15, 30, 40, 75), random)
            Kind.TIMES_FRACTION -> timesFraction(random)
            Kind.DIVIDE_FRACTION -> divideFraction(random)
            Kind.HARD_PERCENT -> percent(listOf(12, 35, 45, 65, 85), random)
        }
        return problem(d, draft, random)
    }

    /** Boss: three fractions, or a sum multiplied by a whole number. */
    override fun generateBoss(difficulty: Int, random: Random): Problem {
        val d = difficulty.coerceIn(MIN_DIFFICULTY, MAX_DIFFICULTY)
        val draft = if (d < 45 || random.nextBoolean()) threeFractions(d, random) else sumTimesWhole(random)
        return problem(d, draft, random)
    }

    private fun problem(d: Int, draft: Draft, random: Random) = Problem(
        school = school,
        difficulty = d,
        prompt = draft.prompt,
        answer = draft.answer.toString(),
        choices = buildChoices(
            answer = draft.answer,
            candidates = draft.candidates + slips(draft.answer),
            random = random,
            isValid = { !it.isNegative },
            nearby = { step -> listOf(draft.answer + Fraction.of(step, draft.answer.den), draft.answer - Fraction.of(step, draft.answer.den)) },
        ),
    )

    private fun slips(answer: Fraction): List<Fraction> = listOf(
        answer + Fraction.of(1, answer.den),
        answer - Fraction.of(1, answer.den),
        Fraction.of(answer.num + 1, answer.den + 1),
    )

    private fun f(num: Int, den: Int) = Fraction.of(num, den)

    private fun show(num: Int, den: Int) = "$num/$den"

    private fun partOf(d: Int, random: Random): Draft {
        val den = random.nextInt(2, if (d < 10) 5 else 7)
        val num = if (d < 10) 1 else random.nextInt(1, den)
        val whole = den * random.nextInt(2, 11)
        val answer = f(num * whole, den)
        return Draft("${show(num, den)} $TIMES $whole", answer, listOf(f(whole, 1) - answer, f(whole / den, 1), f(whole * den, 1)))
    }

    private fun sameDenominator(d: Int, random: Random): Draft {
        val den = random.nextInt(3, if (d < 35) 10 else 13)
        val a = random.nextInt(1, den)
        val b = random.nextInt(1, den)
        return if (random.nextBoolean() || a == b) {
            Draft("${show(a, den)} $PLUS ${show(b, den)}", f(a + b, den), listOf(f(a + b, den * 2), f(a * b, den)))
        } else {
            val (x, y) = maxOf(a, b) to minOf(a, b)
            Draft("${show(x, den)} $MINUS ${show(y, den)}", f(x - y, den), listOf(f(x + y, den), f(x - y, 2 * den)))
        }
    }

    private val denominators = listOf(2, 3, 4, 5, 6, 8, 10, 12)

    private fun properFraction(random: Random): Pair<Int, Int> {
        val den = denominators.random(random)
        return random.nextInt(1, den) to den
    }

    private fun unlikeDenominators(random: Random): Draft {
        var (a, b) = properFraction(random)
        var (c, e) = properFraction(random)
        while (e == b) {
            val next = properFraction(random)
            c = next.first
            e = next.second
        }
        val x = f(a, b)
        val y = f(c, e)
        val naive = f(a + c, b + e) // the classic "add tops and bottoms" mistake
        return if (random.nextBoolean() || x == y) {
            Draft("${show(a, b)} $PLUS ${show(c, e)}", x + y, listOf(naive, x * y))
        } else {
            if (x < y) {
                a = c.also { c = a }
                b = e.also { e = b }
            }
            Draft("${show(a, b)} $MINUS ${show(c, e)}", f(a, b) - f(c, e), listOf(f(a - c, b - e).takeIf { b != e } ?: naive, f(a, b) + f(c, e)))
        }
    }

    private fun timesWhole(random: Random): Draft {
        val (a, b) = properFraction(random)
        val whole = b * random.nextInt(2, 9)
        val answer = f(a * whole, b)
        return Draft("${show(a, b)} $TIMES $whole", answer, listOf(f(whole, a * b), f(a * whole, 1), f(whole / b, 1)))
    }

    private fun percent(percents: List<Int>, random: Random): Draft {
        val p = percents.random(random)
        // Choose the base so the result is whole: base is a multiple of 100 / gcd(p, 100).
        val step = 100 / Fraction.gcd(p, 100)
        val base = step * random.nextInt(1, 200 / step + 2).coerceAtLeast(1)
        val answer = f(p * base, 100)
        return Draft("$p% $TIMES $base", answer, listOf(f(base - p * base / 100, 1), f(p, 1), answer * f(10, 1), answer * f(1, 10)))
    }

    private fun timesFraction(random: Random): Draft {
        val (a, b) = properFraction(random)
        val (c, e) = properFraction(random)
        return Draft("${show(a, b)} $TIMES ${show(c, e)}", f(a * c, b * e), listOf(f(a * e, b * c), f(a + c, b + e), f(a * c, b + e)))
    }

    private fun divideFraction(random: Random): Draft {
        val (a, b) = properFraction(random)
        val (c, e) = properFraction(random)
        return Draft("${show(a, b)} $DIVIDE ${show(c, e)}", f(a * e, b * c), listOf(f(a * c, b * e), f(b * c, a * e), f(a, b)))
    }

    private fun threeFractions(d: Int, random: Random): Draft {
        val den = if (d < 45) random.nextInt(3, 10) else listOf(6, 8, 10, 12).random(random)
        val parts = if (d < 45) List(3) { f(random.nextInt(1, den), den) } else {
            List(3) { val divisor = listOf(1, 2, 3).random(random).takeIf { den % it == 0 } ?: 1; f(random.nextInt(1, den / divisor), den / divisor) }
        }
        val answer = parts[0] + parts[1] + parts[2]
        val prompt = parts.joinToString(" $PLUS ") { "${it.num}/${it.den}" }
        return Draft(prompt, answer, listOf(parts[0] + parts[1], answer + parts[2], f(parts.sumOf { it.num }, parts.sumOf { it.den })))
    }

    private fun sumTimesWhole(random: Random): Draft {
        val (a, b) = properFraction(random)
        var (c, e) = properFraction(random)
        if (e == b) c = c.coerceAtLeast(1)
        val sum = f(a, b) + f(c, e)
        val whole = Fraction.lcm(b, e) * random.nextInt(1, 4)
        val answer = sum * f(whole, 1)
        return Draft(
            "(${show(a, b)} $PLUS ${show(c, e)}) $TIMES $whole",
            answer,
            listOf(f(a, b) + f(c * whole, e), sum, f(a + c, b + e) * f(whole, 1)),
        )
    }
}
