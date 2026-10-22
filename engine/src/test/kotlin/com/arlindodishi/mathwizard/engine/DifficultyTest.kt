package com.arlindodishi.mathwizard.engine

import com.arlindodishi.mathwizard.engine.AdaptiveDifficulty.Answer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DifficultyTest {

    private val fast = Answer(correct = true, millis = 1_000, targetMillis = 10_000)
    private val slow = Answer(correct = true, millis = 9_000, targetMillis = 10_000)
    private val miss = Answer(correct = false, millis = 9_000, targetMillis = 10_000)

    @Test
    fun curveStartsAtZeroIsMonotonicAndBounded() {
        assertEquals(0, DifficultyCurve.base(1))
        var previous = 0
        for (floor in 1..1_000) {
            val base = DifficultyCurve.base(floor)
            assertTrue(base >= previous, "dropped at floor $floor")
            assertTrue(base <= MAX_DIFFICULTY)
            previous = base
        }
        assertTrue(DifficultyCurve.base(10) in 20..35)
        assertTrue(DifficultyCurve.base(50) in 70..90)
    }

    @Test
    fun noHistoryMeansBaseDifficulty() {
        val adaptive = AdaptiveDifficulty()
        assertEquals(DifficultyCurve.base(20), adaptive.difficultyFor(School.ARITHMETIC, 20))
    }

    @Test
    fun fastCorrectAnswersRaiseDifficulty() {
        val adaptive = AdaptiveDifficulty()
        repeat(4) { adaptive.record(School.ARITHMETIC, fast) }
        assertTrue(adaptive.difficultyFor(School.ARITHMETIC, 10) > DifficultyCurve.base(10))
        assertTrue(adaptive.offset(School.ARITHMETIC) > 0)
    }

    @Test
    fun fastAnswersBeatSlowAnswers() {
        val a = AdaptiveDifficulty().apply { repeat(4) { record(School.ARITHMETIC, fast) } }
        val b = AdaptiveDifficulty().apply { repeat(4) { record(School.ARITHMETIC, slow) } }
        assertTrue(a.offset(School.ARITHMETIC) > b.offset(School.ARITHMETIC))
    }

    @Test
    fun consecutiveMissesLowerDifficultyButNeverBelowFloorMinimum() {
        val adaptive = AdaptiveDifficulty()
        repeat(8) { adaptive.record(School.ARITHMETIC, miss) }
        assertEquals(AdaptiveDifficulty.MIN_OFFSET, adaptive.offset(School.ARITHMETIC))
        for (floor in 1..200) {
            val d = adaptive.difficultyFor(School.ARITHMETIC, floor)
            assertTrue(d >= DifficultyCurve.minimum(floor), "floor $floor: $d")
            assertTrue(d < DifficultyCurve.base(floor) || DifficultyCurve.base(floor) == 0)
        }
    }

    @Test
    fun twoMissesInARowCostMoreThanTwoSeparateMisses() {
        val inARow = AdaptiveDifficulty().apply { listOf(slow, miss, miss).forEach { record(School.ARITHMETIC, it) } }
        val apart = AdaptiveDifficulty().apply { listOf(miss, slow, miss).forEach { record(School.ARITHMETIC, it) } }
        assertTrue(inARow.offset(School.ARITHMETIC) < apart.offset(School.ARITHMETIC))
    }

    @Test
    fun neverExceedsMaximum() {
        val adaptive = AdaptiveDifficulty()
        repeat(20) { adaptive.record(School.ARITHMETIC, fast) }
        assertEquals(AdaptiveDifficulty.MAX_OFFSET, adaptive.offset(School.ARITHMETIC))
        assertEquals(MAX_DIFFICULTY, adaptive.difficultyFor(School.ARITHMETIC, 500))
    }

    @Test
    fun windowForgetsOldAnswers() {
        val adaptive = AdaptiveDifficulty(windowSize = 4)
        repeat(4) { adaptive.record(School.ARITHMETIC, miss) }
        repeat(4) { adaptive.record(School.ARITHMETIC, fast) }
        assertEquals(4, adaptive.recent(School.ARITHMETIC).size)
        assertEquals(4 * AdaptiveDifficulty.FAST_BONUS, adaptive.offset(School.ARITHMETIC))
    }

    @Test
    fun schoolsAreTrackedSeparately() {
        val adaptive = AdaptiveDifficulty()
        repeat(5) { adaptive.record(School.ARITHMETIC, miss) }
        assertEquals(0, adaptive.offset(School.FRACTIONS))
    }
}
