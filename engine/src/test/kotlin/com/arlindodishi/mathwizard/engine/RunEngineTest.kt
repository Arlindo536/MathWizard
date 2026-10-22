package com.arlindodishi.mathwizard.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class RunEngineTest {

    private fun engine(seed: Long = 1L, relaxed: Boolean = false) = RunEngine(RunConfig(seed = seed, relaxed = relaxed))

    private fun RunEngine.correct(elapsed: Long = state.timeLimitMillis / 2) = answer(state.problem.answer, elapsed)

    private fun RunEngine.wrong() = answer(state.problem.choices.first { it != state.problem.answer }, 1_000)

    private fun RunEngine.clearFloor(elapsed: (RunEngine) -> Long = { it.state.timeLimitMillis / 2 }): Int {
        var answers = 0
        while (state.phase == Phase.FIGHTING) {
            correct(elapsed(this))
            answers++
        }
        return answers
    }

    @Test
    fun runStartsOnFloorOneAtFullHealth() {
        val s = engine().state
        assertEquals(1, s.floor)
        assertEquals(Balance.PLAYER_MAX_HP, s.playerHp)
        assertEquals(s.enemy.maxHp, s.enemy.hp)
        assertEquals(Phase.FIGHTING, s.phase)
        assertTrue(s.timeLimitMillis >= 8_000)
    }

    @Test
    fun correctAnswerDamagesEnemyAndMovesToNextProblem() {
        val e = engine()
        val before = e.state
        val outcome = assertIs<AnswerOutcome.Hit>(e.correct())
        assertEquals(before.problem, outcome.problem)
        assertEquals(before.enemy.hp - outcome.damage, e.state.enemy.hp)
        assertNotEquals(before.problem, e.state.problem)
        assertEquals(1, e.state.streak)
    }

    @Test
    fun fasterAnswersDealMoreDamage() {
        val fast = assertIs<AnswerOutcome.Hit>(engine().let { it.correct(elapsed = 500) })
        val slow = assertIs<AnswerOutcome.Hit>(engine().let { it.correct(elapsed = it.state.timeLimitMillis - 1) })
        assertTrue(fast.fast)
        assertTrue(fast.damage > slow.damage)
        assertEquals(Balance.SPELL_DAMAGE, slow.damage)
    }

    @Test
    fun streakBuildsCombo() {
        val e = engine(seed = 99L).apply { repeat(10) { nextFloorIfCleared() } }
        val hits = (1..5).map { e.nextFloorIfCleared(); assertIs<AnswerOutcome.Hit>(e.correct(e.state.timeLimitMillis - 1)) }
        assertEquals(listOf(1.0, 1.0, 1.25, 1.25, 1.5), hits.map { it.comboMultiplier })
    }

    private fun RunEngine.nextFloorIfCleared() {
        if (state.phase == Phase.FLOOR_CLEARED) nextFloor()
    }

    @Test
    fun wrongAnswerHurtsPlayerAndResetsStreak() {
        val e = engine()
        e.correct()
        val outcome = assertIs<AnswerOutcome.Miss>(e.wrong())
        assertEquals(Balance.PLAYER_MAX_HP - outcome.damageTaken, e.state.playerHp)
        assertEquals(0, e.state.streak)
        assertTrue(outcome.damageTaken > 0)
    }

    @Test
    fun timeoutCountsAsMiss() {
        val e = engine()
        val outcome = assertIs<AnswerOutcome.Miss>(e.timeOut())
        assertTrue(outcome.timedOut)
        assertEquals(1, e.state.stats.answered)
        assertEquals(0, e.state.stats.correct)
    }

    @Test
    fun floorTakesThreeToSixAnswersAtModerateSpeedAndBossesUpToNine() {
        for (seed in 1L..50L) {
            val e = engine(seed)
            repeat(30) {
                val floor = e.state.floor
                val answers = e.clearFloor()
                val range = if (Balance.isBossFloor(floor)) 4..9 else 3..6
                assertTrue(answers in range, "seed $seed floor $floor: $answers answers")
                e.nextFloor()
            }
        }
    }

    @Test
    fun clearingFloorHealsAndSpawnsNextEnemy() {
        val e = engine()
        e.wrong()
        e.wrong()
        val hurt = e.state.playerHp
        e.clearFloor()
        assertEquals(Phase.FLOOR_CLEARED, e.state.phase)
        assertEquals(1, e.state.stats.floorsCleared)
        e.nextFloor()
        assertEquals(2, e.state.floor)
        assertEquals(Phase.FIGHTING, e.state.phase)
        assertEquals((hurt + Balance.FLOOR_HEAL).coerceAtMost(Balance.PLAYER_MAX_HP), e.state.playerHp)
        assertEquals(e.state.enemy.maxHp, e.state.enemy.hp)
    }

    @Test
    fun healingNeverExceedsMaxHp() {
        val e = engine()
        e.clearFloor()
        e.nextFloor()
        assertEquals(Balance.PLAYER_MAX_HP, e.state.playerHp)
    }

    @Test
    fun runEndsWhenHealthReachesZero() {
        val e = engine()
        var last: AnswerOutcome? = null
        while (e.state.phase == Phase.FIGHTING) last = e.wrong()
        assertEquals(Phase.GAME_OVER, e.state.phase)
        assertEquals(0, e.state.playerHp)
        assertTrue(assertIs<AnswerOutcome.Miss>(last).playerDefeated)
    }

    @Test
    fun actionsAreRejectedInTheWrongPhase() {
        val e = engine()
        assertFailsWith<IllegalStateException> { e.nextFloor() }
        e.clearFloor()
        assertFailsWith<IllegalStateException> { e.correct() }
        assertFailsWith<IllegalStateException> { e.timeOut() }
    }

    @Test
    fun sameSeedAndAnswersReplayIdentically() {
        fun play(seed: Long): List<RunState> {
            val e = engine(seed)
            val states = mutableListOf(e.state)
            repeat(40) { i ->
                when {
                    e.state.phase == Phase.FLOOR_CLEARED -> e.nextFloor()
                    e.state.phase == Phase.GAME_OVER -> return states
                    i % 4 == 3 -> e.wrong()
                    else -> e.correct(elapsed = 1_000L * (i % 7))
                }
                states += e.state
            }
            return states
        }
        assertEquals(play(5L), play(5L))
        assertNotEquals(play(5L), play(6L))
    }

    @Test
    fun difficultyRisesWithFloors() {
        val e = engine()
        repeat(40) {
            e.clearFloor()
            e.nextFloor()
        }
        assertTrue(e.state.problem.difficulty > DifficultyCurve.minimum(41))
        assertTrue(e.state.problem.difficulty > 50)
    }

    @Test
    fun relaxedModeGivesLongerTimers() {
        assertTrue(engine(relaxed = true).state.timeLimitMillis > engine(relaxed = false).state.timeLimitMillis)
    }

    @Test
    fun enemyDamageIsCappedAndGrowsWithFloor() {
        val type = Bestiary.all.maxBy { it.damageFactor }
        assertTrue(Balance.enemyDamage(1, type) < Balance.enemyDamage(20, type))
        assertEquals(35, Balance.enemyDamage(1_000, type))
    }

    @Test
    fun everySchoolWithGeneratorHasEnemies() {
        Problems.availableSchools.forEach { assertTrue(Bestiary.forSchool(it).isNotEmpty(), "$it has no enemies") }
    }
}
