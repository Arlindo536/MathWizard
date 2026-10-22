package com.arlindodishi.mathwizard.engine

import com.arlindodishi.mathwizard.engine.ArithmeticGenerator.DIVIDE
import com.arlindodishi.mathwizard.engine.ArithmeticGenerator.MINUS
import com.arlindodishi.mathwizard.engine.ArithmeticGenerator.TIMES
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class ArithmeticGeneratorTest {

    private val seedsPerLevel = 400

    private fun sample(difficulty: Int): Sequence<Problem> = (0 until seedsPerLevel).asSequence()
        .map { Problems.generate(School.ARITHMETIC, difficulty, Seeds.mix(difficulty.toLong(), it.toLong())) }

    private fun allProblems(): Sequence<Problem> =
        (MIN_DIFFICULTY..MAX_DIFFICULTY step 5).asSequence().flatMap { sample(it) }

    @Test
    fun answerMatchesIndependentEvaluation() {
        allProblems().forEach { p ->
            assertEquals(ExpressionEvaluator.evaluate(p.prompt).toString(), p.answer, p.prompt)
        }
    }

    @Test
    fun choicesAreFourDistinctNonNegativeIntegersIncludingAnswer() {
        allProblems().forEach { p ->
            assertEquals(CHOICE_COUNT, p.choices.size, p.toString())
            assertEquals(CHOICE_COUNT, p.choices.toSet().size, "duplicate choice in $p")
            assertTrue(p.answer in p.choices, p.toString())
            assertTrue(p.choices.all { (it.toIntOrNull() ?: -1) >= 0 }, "bad choice in $p")
        }
    }

    @Test
    fun answersAreNonNegativeAndNoOperandIsZero() {
        allProblems().forEach { p ->
            assertTrue(p.answer.toInt() >= 0, p.prompt)
            assertFalse(Regex("""\b0\b""").containsMatchIn(p.prompt), "zero operand in ${p.prompt}")
        }
    }

    @Test
    fun operatorsUnlockAtTheirThresholds() {
        (0 until 15).forEach { d -> assertTrue(sample(d).none { MINUS in it.prompt }) }
        (0 until 35).forEach { d -> assertTrue(sample(d).none { TIMES in it.prompt }) }
        (0 until 55).forEach { d -> assertTrue(sample(d).none { DIVIDE in it.prompt }) }
        assertTrue(sample(15).any { MINUS in it.prompt })
        assertTrue(sample(35).any { TIMES in it.prompt })
        assertTrue(sample(55).any { DIVIDE in it.prompt })
        assertTrue(sample(75).any { it.prompt.split(' ').size == 5 || '(' in it.prompt })
    }

    @Test
    fun easiestLevelStaysSmall() {
        sample(0).forEach { p -> assertTrue(p.answer.toInt() <= 10, p.prompt) }
    }

    @Test
    fun timesTablesReachTwelveBeforeTwoDigitLevels() {
        val factors = (70..79).flatMap { d -> sample(d).filter { TIMES in it.prompt && it.prompt.split(' ').size == 3 }.toList() }
            .flatMap { p -> p.prompt.split(" $TIMES ").map(String::toInt) }
        assertEquals(12, factors.max())
    }

    @Test
    fun difficultyGrowsAnswerSize() {
        fun meanAnswer(d: Int) = sample(d).map { it.answer.toInt() }.average()
        assertTrue(meanAnswer(10) < meanAnswer(50))
        assertTrue(meanAnswer(50) < meanAnswer(95))
    }

    @Test
    fun sameSeedGivesSameProblem() {
        repeat(50) { i ->
            val seed = Seeds.mix(i.toLong())
            assertEquals(Problems.generate(School.ARITHMETIC, 60, seed), Problems.generate(School.ARITHMETIC, 60, seed))
        }
    }

    @Test
    fun differentSeedsGiveVariety() {
        val prompts = sample(50).map { it.prompt }.toSet()
        assertTrue(prompts.size > seedsPerLevel / 2, "only ${prompts.size} distinct prompts")
    }

    @Test
    fun answerIsNotAlwaysInTheSamePosition() {
        val positions = sample(40).map { it.choices.indexOf(it.answer) }.toSet()
        assertEquals((0 until CHOICE_COUNT).toSet(), positions)
    }

    @Test
    fun outOfRangeDifficultyIsClamped() {
        assertEquals(MAX_DIFFICULTY, Problems.generate(School.ARITHMETIC, 500, 1L).difficulty)
        assertEquals(MIN_DIFFICULTY, Problems.generate(School.ARITHMETIC, -5, 1L).difficulty)
    }

    @Test
    fun isCorrectTrimsInput() {
        val p = Problems.generate(School.ARITHMETIC, 20, 7L)
        assertTrue(p.isCorrect(" ${p.answer} "))
        assertFalse(p.isCorrect(p.choices.first { it != p.answer }))
        assertNotEquals("", p.answer)
    }
}

class ArithmeticBossTest {

    private fun bosses(d: Int) = (0 until 300).map { Problems.generate(School.ARITHMETIC, d, Seeds.mix(d.toLong(), it.toLong(), 77L), boss = true) }

    private fun operatorCount(prompt: String) = prompt.count { it in "+−×÷" }

    @Test
    fun bossProblemsAreCorrectAndMultiStepAtEveryLevel() {
        for (d in MIN_DIFFICULTY..MAX_DIFFICULTY step 5) {
            bosses(d).forEach { p ->
                assertEquals(ExpressionEvaluator.evaluate(p.prompt).toString(), p.answer, p.prompt)
                assertTrue(operatorCount(p.prompt) >= 2, "single step boss problem ${p.prompt}")
                assertEquals(CHOICE_COUNT, p.choices.toSet().size, p.toString())
                assertTrue(p.answer in p.choices)
                assertTrue(p.choices.all { (it.toIntOrNull() ?: -1) >= 0 }, p.toString())
            }
        }
    }

    @Test
    fun lowLevelBossesAvoidMultiplication() {
        bosses(10).forEach { assertTrue(ArithmeticGenerator.TIMES !in it.prompt, it.prompt) }
    }
}
