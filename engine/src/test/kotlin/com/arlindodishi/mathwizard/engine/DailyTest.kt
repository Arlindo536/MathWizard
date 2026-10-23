package com.arlindodishi.mathwizard.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals

class DailyTest {

    private fun play(config: RunConfig, answers: Int, wrongEvery: Int): List<Problem> {
        val e = RunEngine(config)
        val seen = mutableListOf<Problem>()
        repeat(answers) { i ->
            if (e.state.phase == Phase.FLOOR_CLEARED) e.nextFloor()
            if (e.state.phase != Phase.FIGHTING) return seen
            seen += e.state.problem
            val wrong = wrongEvery > 0 && i % wrongEvery == 0
            e.answer(if (wrong) e.state.problem.choices.first { it != e.state.problem.answer } else e.state.problem.answer, 1_000L + i * 300)
        }
        return seen
    }

    @Test
    fun schoolRotatesThroughAllSix() {
        assertEquals(School.entries.toSet(), (0L until 6L).map(DailyRift::school).toSet())
        assertEquals(DailyRift.school(10), DailyRift.school(16))
        assertEquals(DailyRift.school(-1), School.SEQUENCES)
    }

    @Test
    fun sameDaySameRunDifferentDayDifferentRun() {
        assertEquals(DailyRift.config(20_000), DailyRift.config(20_000))
        assertNotEquals(DailyRift.config(20_000).seed, DailyRift.config(20_001).seed)
        assertFalse(DailyRift.config(20_000).adaptive)
    }

    @Test
    fun dailyProblemsDoNotDependOnHowFastOrWellYouAnswer() {
        // Two players answering differently still meet the same problems on the same floors.
        val fast = play(DailyRift.config(20_123), answers = 6, wrongEvery = 0)
        val sloppy = play(DailyRift.config(20_123), answers = 6, wrongEvery = 3)
        assertEquals(fast.take(4), sloppy.take(4))
    }

    @Test
    fun currentStreakCountsBackFromTodayOrYesterday() {
        assertEquals(0, Streaks.current(emptySet(), 100))
        assertEquals(3, Streaks.current(setOf(98, 99, 100), 100))
        assertEquals(2, Streaks.current(setOf(98, 99), 100), "today not played yet keeps the streak")
        assertEquals(0, Streaks.current(setOf(97, 98), 100), "a missed day breaks it")
        assertEquals(1, Streaks.current(setOf(95, 96, 100), 100))
    }

    @Test
    fun bestStreakIsTheLongestRun() {
        assertEquals(0, Streaks.best(emptySet()))
        assertEquals(4, Streaks.best(setOf(1, 2, 3, 4, 10, 11, 20)))
        assertEquals(1, Streaks.best(setOf(5, 7, 9)))
    }

    @Test
    fun practiceLevelsRise() {
        assertEquals(PracticeLevel.entries.sortedBy { it.difficulty }, PracticeLevel.entries)
    }
}
