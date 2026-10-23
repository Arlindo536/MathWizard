package com.arlindodishi.mathwizard.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** Shared sampling: every difficulty in steps of 5, normal and boss problems. */
private fun sample(school: School, boss: Boolean = false, perLevel: Int = 200): List<Problem> =
    (MIN_DIFFICULTY..MAX_DIFFICULTY step 5).flatMap { d ->
        (0 until perLevel).map { i -> Problems.generate(school, d, Seeds.mix(school.ordinal.toLong(), d.toLong(), i.toLong()), boss) }
    }

private fun allProblems(school: School) = sample(school) + sample(school, boss = true, perLevel = 80)

class SchoolContractTest {

    @Test
    fun everySchoolHasAGenerator() {
        assertEquals(School.entries.toSet(), Problems.availableSchools)
    }

    @Test
    fun choicesAreDistinctAndContainTheAnswer() {
        School.entries.forEach { school ->
            allProblems(school).forEach { p ->
                assertEquals(CHOICE_COUNT, p.choices.size, "$school $p")
                assertEquals(CHOICE_COUNT, p.choices.toSet().size, "$school duplicate in $p")
                assertTrue(p.answer in p.choices, "$school $p")
                assertTrue(p.prompt.isNotBlank())
            }
        }
    }

    @Test
    fun sameSeedSameProblem() {
        School.entries.forEach { school ->
            repeat(30) { i ->
                assertEquals(Problems.generate(school, 50, i.toLong()), Problems.generate(school, 50, i.toLong()))
                assertEquals(Problems.generate(school, 50, i.toLong(), boss = true), Problems.generate(school, 50, i.toLong(), boss = true))
            }
        }
    }

    @Test
    fun problemsVaryWithSeed() {
        School.entries.forEach { school ->
            val prompts = (0 until 200).map { Problems.generate(school, 60, it.toLong()).prompt }.toSet()
            // Powers has the smallest space (~55 problems at difficulty 60), so the bar is modest.
            assertTrue(prompts.size > 40, "$school only ${prompts.size} distinct prompts")
        }
    }

    @Test
    fun answerPositionIsSpread() {
        School.entries.forEach { school ->
            val positions = (0 until 200).map { Problems.generate(school, 40, it.toLong()).let { p -> p.choices.indexOf(p.answer) } }.toSet()
            assertEquals((0 until CHOICE_COUNT).toSet(), positions, "$school")
        }
    }

    @Test
    fun everySchoolHasEnemiesAndABoss() {
        School.entries.forEach { school ->
            assertTrue(Bestiary.forSchool(school).size >= 3, "$school")
            assertTrue(Bestiary.bossFor(school).boss)
        }
    }

    @Test
    fun runsWorkInEverySchool() {
        School.entries.forEach { school ->
            val e = RunEngine(RunConfig(seed = 5L, school = school))
            repeat(12) {
                while (e.state.phase == Phase.FIGHTING) e.answer(e.state.problem.answer, 1_000)
                assertEquals(school, e.state.problem.school)
                e.nextFloor()
            }
            assertEquals(13, e.state.floor)
        }
    }
}

class FractionsGeneratorTest {

    /** Independent exact rational arithmetic on Longs. */
    private data class Q(val n: Long, val d: Long) {
        fun norm(): Q {
            fun gcd(a: Long, b: Long): Long = if (b == 0L) a else gcd(b, a % b)
            val g = gcd(kotlin.math.abs(n), kotlin.math.abs(d)).coerceAtLeast(1)
            val s = if (d < 0) -1 else 1
            return Q(s * n / g, s * d / g)
        }
        operator fun plus(o: Q) = Q(n * o.d + o.n * d, d * o.d).norm()
        operator fun minus(o: Q) = Q(n * o.d - o.n * d, d * o.d).norm()
        operator fun times(o: Q) = Q(n * o.n, d * o.d).norm()
        operator fun div(o: Q) = Q(n * o.d, d * o.n).norm()
    }

    private fun parseValue(text: String): Q {
        val parts = text.split("/")
        return if (parts.size == 2) Q(parts[0].toLong(), parts[1].toLong()).norm() else Q(text.toLong(), 1)
    }

    /** Evaluates "a/b", whole numbers, "p%" and + − × ÷ with brackets. */
    private fun evaluate(expr: String): Q {
        val tokens = Regex("""\d+/\d+|\d+%|\d+|[+−×÷()]""").findAll(expr).map { it.value }.toList()
        var pos = 0
        fun expression(): Q {
            fun factor(): Q {
                val t = tokens[pos++]
                return when {
                    t == "(" -> expression().also { check(tokens[pos++] == ")") }
                    t.endsWith("%") -> Q(t.dropLast(1).toLong(), 100).norm()
                    else -> parseValue(t)
                }
            }
            fun term(): Q {
                var v = factor()
                while (pos < tokens.size && tokens[pos] in setOf("×", "÷")) {
                    v = if (tokens[pos++] == "×") v * factor() else v / factor()
                }
                return v
            }
            var v = term()
            while (pos < tokens.size && tokens[pos] in setOf("+", "−")) {
                v = if (tokens[pos++] == "+") v + term() else v - term()
            }
            return v
        }
        return expression().also { check(pos == tokens.size) { "Trailing tokens in $expr" } }
    }

    @Test
    fun answersAreExactAndInLowestTerms() {
        allProblems(School.FRACTIONS).forEach { p ->
            val expected = evaluate(p.prompt)
            assertEquals(expected, parseValue(p.answer), p.prompt)
            assertEquals(if (expected.d == 1L) "${expected.n}" else "${expected.n}/${expected.d}", p.answer, "not in lowest terms: $p")
        }
    }

    @Test
    fun choicesAreDistinctNonNegativeValues() {
        allProblems(School.FRACTIONS).forEach { p ->
            val values = p.choices.map(::parseValue)
            assertEquals(CHOICE_COUNT, values.toSet().size, "equal values in $p")
            assertTrue(values.all { it.n >= 0 }, "negative choice in $p")
        }
    }

    @Test
    fun easiestLevelIsPartOfAWhole() {
        sample(School.FRACTIONS).filter { it.difficulty < 20 }.forEach { p ->
            assertTrue(p.prompt.matches(Regex("""\d+/\d+ × \d+""")), p.prompt)
            assertEquals(1L, parseValue(p.answer).d, "should be whole: $p")
        }
    }
}

class AlgebraGeneratorTest {

    /** Substitutes x and evaluates both sides; supports "3x" and "3(x + 2)". */
    private fun sides(equation: String, x: Int): Pair<Int, Int> {
        val withX = equation
            .replace(Regex("""(\d)\("""), "$1 × (")
            .replace(Regex("""(\d)x"""), "$1 × ($x)")
            .replace("x", "($x)")
        val (left, right) = withX.split("=")
        return ExpressionEvaluator.evaluate(left) to ExpressionEvaluator.evaluate(right)
    }

    private fun satisfies(equation: String, x: Int): Boolean = runCatching { sides(equation, x).let { it.first == it.second } }.getOrDefault(false)

    @Test
    fun answerSolvesTheEquationAndNothingElseDoes() {
        allProblems(School.ALGEBRA).forEach { p ->
            val x = p.answer.toInt()
            assertTrue(x > 0, p.toString())
            assertTrue(satisfies(p.prompt, x), "x = $x does not solve ${p.prompt}")
            p.choices.filter { it != p.answer }.forEach { wrong ->
                assertTrue(!satisfies(p.prompt, wrong.toInt()), "$wrong also solves ${p.prompt}")
            }
        }
    }

    @Test
    fun easyLevelsAreOneStep() {
        sample(School.ALGEBRA).filter { it.difficulty < 30 }.forEach { p ->
            assertTrue(p.prompt.matches(Regex("""x [+−] \d+ = \d+""")), p.prompt)
        }
    }

    @Test
    fun bossesAreTwoStep() {
        sample(School.ALGEBRA, boss = true).forEach { p ->
            val left = p.prompt.substringBefore("=")
            assertTrue(left.count { it in "+−÷(" } >= 1 && left.any { it.isDigit() }, p.prompt)
            assertTrue(!p.prompt.matches(Regex("""x [+−] \d+ = \d+""")), p.prompt)
        }
    }
}

class PowersGeneratorTest {

    @Test
    fun answersMatchIndependentEvaluation() {
        allProblems(School.POWERS).forEach { p ->
            assertEquals(ExpressionEvaluator.evaluate(p.prompt).toString(), p.answer, p.prompt)
        }
    }

    @Test
    fun choicesAreNonNegativeIntegers() {
        allProblems(School.POWERS).forEach { p -> assertTrue(p.choices.all { (it.toIntOrNull() ?: -1) >= 0 }, p.toString()) }
    }

    @Test
    fun easiestLevelIsSquares() {
        sample(School.POWERS).filter { it.difficulty < 20 }.forEach { p -> assertTrue(p.prompt.endsWith("²"), p.prompt) }
    }

    @Test
    fun superscriptsRenderMultiDigitExponents() {
        assertEquals("¹⁰", PowersGenerator.superscript(10))
        assertEquals("²", PowersGenerator.superscript(2))
    }
}

class GeometryGeneratorTest {

    private val triples = setOf(listOf(3, 4, 5), listOf(5, 12, 13), listOf(8, 15, 17), listOf(7, 24, 25))

    /** Recomputes the answer from the formula behind each prompt key. */
    private fun expected(key: String, a: List<Int>): Int = when (key) {
        "square_perimeter" -> 4 * a[0]
        "rect_perimeter" -> 2 * (a[0] + a[1])
        "rect_area" -> a[0] * a[1]
        "triangle_area" -> (a[0] * a[1]).also { check(it % 2 == 0) } / 2
        "triangle_angle" -> 180 - a[0] - a[1]
        "box_volume" -> a[0] * a[1] * a[2]
        "hypotenuse" -> kotlin.math.sqrt((a[0] * a[0] + a[1] * a[1]).toDouble()).toInt()
            .also { check(it * it == a[0] * a[0] + a[1] * a[1]) }
        "quad_angle" -> 360 - a[0] - a[1] - a[2]
        "trapezoid_area" -> ((a[0] + a[1]) * a[2]).also { check(it % 2 == 0) } / 2
        "missing_leg" -> kotlin.math.sqrt((a[1] * a[1] - a[0] * a[0]).toDouble()).toInt()
            .also { check(it * it == a[1] * a[1] - a[0] * a[0]) }
        "polygon_angle_sum" -> (a[0] - 2) * 180
        "regular_polygon_angle" -> ((a[0] - 2) * 180).also { check(it % a[0] == 0) } / a[0]
        "cutout_area" -> a[0] * a[1] - a[2] * a[2]
        else -> error("Unknown key $key")
    }

    @Test
    fun answersMatchTheFormulas() {
        allProblems(School.GEOMETRY).forEach { p ->
            val key = assertNotNull(p.promptKey, p.toString())
            val answer = expected(key, p.promptArgs.map(String::toInt))
            assertEquals(answer.toString(), p.answer, p.prompt)
            assertTrue(answer > 0, p.toString())
        }
    }

    @Test
    fun anglesAndShapesAreValid() {
        allProblems(School.GEOMETRY).forEach { p ->
            val a = p.promptArgs.map(String::toInt)
            when (p.promptKey) {
                "triangle_angle" -> assertTrue(a.sum() < 180 && a.all { it > 0 }, p.prompt)
                "quad_angle" -> assertTrue(a.sum() < 360, p.prompt)
                "cutout_area" -> assertTrue(a[2] < a[0] && a[2] < a[1], p.prompt)
                "hypotenuse", "missing_leg" -> assertTrue(a.all { it > 0 })
            }
        }
    }

    @Test
    fun everyKeyHasATemplateAndEnglishPrompt() {
        allProblems(School.GEOMETRY).forEach { p ->
            assertTrue(p.promptKey in GeometryGenerator.templates, p.toString())
            assertTrue('%' !in p.prompt, "unfilled template: ${p.prompt}")
            p.promptArgs.forEach { assertTrue(it in p.prompt, p.prompt) }
        }
        assertTrue(triples.isNotEmpty())
    }
}

class SequencesGeneratorTest {

    /** Independent rule families; a complete sequence "makes sense" if any one fits. */
    private fun fitsSomeRule(t: List<Long>): Boolean {
        val d = t.zipWithNext { a, b -> b - a }
        val arithmetic = d.distinct().size == 1
        val geometric = t.none { it == 0L } && t.zipWithNext().all { (a, b) -> b % a == 0L } &&
            t.zipWithNext { a, b -> b / a }.distinct().size == 1
        val second = d.zipWithNext { a, b -> b - a }
        val quadratic = t.size >= 4 && second.distinct().size == 1
        val cubic = t.size >= 5 && second.zipWithNext { a, b -> b - a }.distinct().size == 1
        val alternating = d.size >= 4 && d.withIndex().all { (i, v) -> v == d[i % 2] }
        val fibonacci = t.size >= 4 && (2 until t.size).all { t[it] == t[it - 1] + t[it - 2] }
        return arithmetic || geometric || quadratic || cubic || alternating || fibonacci
    }

    private fun complete(prompt: String, value: String): List<Long> =
        prompt.split(", ").map { if (it == "?") value.toLong() else it.toLong() }

    @Test
    fun exactlyTheAnswerCompletesTheSequence() {
        allProblems(School.SEQUENCES).forEach { p ->
            assertEquals(1, p.prompt.split(", ").count { it == "?" }, p.prompt)
            assertTrue(fitsSomeRule(complete(p.prompt, p.answer)), "answer breaks the pattern: $p")
            p.choices.filter { it != p.answer }.forEach { wrong ->
                assertTrue(!fitsSomeRule(complete(p.prompt, wrong)), "$wrong also fits ${p.prompt}")
            }
        }
    }

    @Test
    fun showsAtLeastFourKnownTerms() {
        allProblems(School.SEQUENCES).forEach { p -> assertTrue(p.prompt.split(", ").size >= 5, p.prompt) }
    }

    @Test
    fun termsAreNonNegative() {
        allProblems(School.SEQUENCES).forEach { p ->
            assertTrue(p.prompt.split(", ").filter { it != "?" }.all { it.toLong() >= 0 }, p.prompt)
            assertTrue(p.choices.all { it.toLong() >= 0 }, p.toString())
        }
    }
}
