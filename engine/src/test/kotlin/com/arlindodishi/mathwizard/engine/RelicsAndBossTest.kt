package com.arlindodishi.mathwizard.engine

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class RelicsAndBossTest {

    private fun engine(seed: Long = 1L) = RunEngine(RunConfig(seed = seed))

    private fun RunEngine.correct(elapsed: Long = state.timeLimitMillis - 1) = answer(state.problem.answer, elapsed)

    private fun RunEngine.wrong() = answer(state.problem.choices.first { it != state.problem.answer }, 1_000)

    private fun RunEngine.clearFloor() {
        while (state.phase == Phase.FIGHTING) correct()
    }

    /** Clears floors until [id] is offered, then takes it. Returns false if never offered. */
    private fun RunEngine.takeRelic(id: String): Boolean {
        repeat(60) {
            clearFloor()
            if (state.relicOffer.any { it.id == id }) {
                nextFloor(id)
                return true
            }
            nextFloor()
        }
        return false
    }

    /** Engine on a fresh non-boss floor holding [id] (searches seeds for an early offer). */
    private fun engineWith(id: String): RunEngine {
        for (seed in 1L..500L) {
            val e = engine(seed)
            e.clearFloor()
            if (e.state.relicOffer.any { it.id == id }) {
                e.nextFloor(id)
                return e
            }
        }
        error("$id never offered on floor 1")
    }

    @Test
    fun thereAreTwelveUniqueRelics() {
        assertEquals(12, Relics.all.size)
        assertEquals(12, Relics.all.map { it.id }.toSet().size)
        Relics.all.forEach { assertTrue(it != Relic(it.id), "${it.id} has no effect") }
    }

    @Test
    fun offerHasThreeDistinctUnheldRelics() {
        val held = Relics.all.take(4)
        repeat(100) { i ->
            val offer = Relics.offer(held, Random(i))
            assertEquals(3, offer.size)
            assertEquals(3, offer.toSet().size)
            assertTrue(offer.none { it in held })
        }
        assertEquals(1, Relics.offer(Relics.all.drop(1), Random(1)).size)
        assertTrue(Relics.offer(Relics.all, Random(1)).isEmpty())
    }

    @Test
    fun clearingFloorOffersRelicsDeterministically() {
        val a = engine(9L).apply { clearFloor() }.state.relicOffer
        val b = engine(9L).apply { clearFloor() }.state.relicOffer
        assertEquals(3, a.size)
        assertEquals(a, b)
    }

    @Test
    fun takingRelicAddsItAndClearsOffer() {
        val e = engine()
        e.clearFloor()
        val pick = e.state.relicOffer.first()
        e.nextFloor(pick.id)
        assertEquals(listOf(pick), e.state.relics)
        assertTrue(e.state.relicOffer.isEmpty())
        assertEquals(2, e.state.floor)
    }

    @Test
    fun relicNotOnOfferIsRejected() {
        val e = engine()
        e.clearFloor()
        val notOffered = Relics.all.first { it !in e.state.relicOffer }
        assertFailsWith<IllegalArgumentException> { e.nextFloor(notOffered.id) }
    }

    @Test
    fun allRelicsCanBeCollectedAndThenOffersStop() {
        val e = engine(3L)
        var floors = 0
        while (e.state.relics.size < Relics.all.size && floors < 40) {
            e.clearFloor()
            e.nextFloor(e.state.relicOffer.firstOrNull()?.id)
            floors++
        }
        assertEquals(Relics.all.size, e.state.relics.size)
        e.clearFloor()
        assertTrue(e.state.relicOffer.isEmpty())
    }

    @Test
    fun emberFocusRaisesDamage() {
        val e = engineWith("ember_focus")
        e.wrong() // reset the streak carried over from floor 1
        val hit = assertIs<AnswerOutcome.Hit>(e.correct())
        assertEquals((Balance.SPELL_DAMAGE * 1.2).toInt(), hit.damage)
    }

    @Test
    fun quicksilverQuillExtendsTimer() {
        val e = engineWith("quicksilver_quill")
        val base = Balance.timeLimitMillis(e.state.problem.difficulty, relaxed = false)
        assertEquals((base * 1.25).toLong(), e.state.timeLimitMillis)
    }

    @Test
    fun heartstoneRaisesMaxHp() {
        val e = engineWith("heartstone")
        assertEquals(Balance.PLAYER_MAX_HP + 25, e.state.playerMaxHp)
        assertEquals(Balance.PLAYER_MAX_HP + 25, e.state.playerHp)
    }

    @Test
    fun healingDraughtHealsMoreBetweenFloors() {
        val e = engineWith("healing_draught")
        repeat(3) { e.wrong() }
        val hurt = e.state.playerHp
        e.clearFloor()
        e.nextFloor()
        assertEquals(hurt + Balance.FLOOR_HEAL + 15, e.state.playerHp)
    }

    @Test
    fun ironWardReducesEnemyDamage() {
        val e = engineWith("iron_ward")
        val miss = assertIs<AnswerOutcome.Miss>(e.wrong())
        val raw = Balance.enemyDamage(e.state.floor, e.state.enemy.type)
        assertEquals((raw * 0.75).let { kotlin.math.round(it).toInt() }, miss.damageTaken)
    }

    @Test
    fun swiftSigilWidensFastWindow() {
        val e = engineWith("swift_sigil")
        e.wrong() // reset the streak carried over from floor 1
        val hit = assertIs<AnswerOutcome.Hit>(e.correct(elapsed = e.state.timeLimitMillis * 45 / 100))
        assertTrue(hit.fast)
        assertEquals((Balance.SPELL_DAMAGE * 1.5).toInt() + 1, hit.damage) // 37.5 rounds to 38
    }

    @Test
    fun comboCrownStartsComboEarlier() {
        val e = engineWith("combo_crown")
        e.wrong() // reset the streak carried over from floor 1
        e.correct()
        val second = assertIs<AnswerOutcome.Hit>(e.correct())
        assertEquals(1.25, second.comboMultiplier)
    }

    @Test
    fun manaShieldBlocksFirstMissEachFloor() {
        val e = engineWith("mana_shield")
        val first = assertIs<AnswerOutcome.Miss>(e.wrong())
        assertTrue(first.shielded)
        assertEquals(0, first.damageTaken)
        assertEquals(Balance.PLAYER_MAX_HP, e.state.playerHp)
        val second = assertIs<AnswerOutcome.Miss>(e.wrong())
        assertFalse(second.shielded)
        assertTrue(second.damageTaken > 0)
        e.clearFloor()
        e.nextFloor()
        assertTrue(e.state.shieldReady)
    }

    @Test
    fun vampiricRuneHealsOnHit() {
        val e = engineWith("vampiric_rune")
        e.wrong()
        val hurt = e.state.playerHp
        val hit = assertIs<AnswerOutcome.Hit>(e.correct())
        assertEquals(3, hit.healed)
        assertEquals(hurt + 3, e.state.playerHp)
    }

    @Test
    fun oracleEyeRemovesOneWrongChoice() {
        val e = engineWith("oracle_eye")
        repeat(10) {
            assertEquals(CHOICE_COUNT - 1, e.state.problem.choices.size)
            assertTrue(e.state.problem.answer in e.state.problem.choices)
            e.correct()
            if (e.state.phase == Phase.FLOOR_CLEARED) e.nextFloor()
        }
    }

    @Test
    fun phoenixFeatherRevivesOnce() {
        val e = engineWith("phoenix_feather")
        var outcome: AnswerOutcome.Miss
        do outcome = assertIs<AnswerOutcome.Miss>(e.wrong()) while (!outcome.revived)
        assertEquals(30, e.state.playerHp)
        assertFalse(e.state.reviveReady)
        while (e.state.phase == Phase.FIGHTING) e.wrong()
        assertEquals(Phase.GAME_OVER, e.state.phase)
    }

    @Test
    fun giantSlayerHitsBossesHarder() {
        val e = engine(4L)
        assertTrue(e.takeRelic("giant_slayer"))
        while (!e.state.isBossFloor) {
            e.clearFloor()
            e.nextFloor()
        }
        val hit = assertIs<AnswerOutcome.Hit>(e.correct())
        assertEquals((Balance.SPELL_DAMAGE * 1.5 * Balance.comboMultiplier(e.state.streak)).let { kotlin.math.round(it).toInt() }, hit.damage)
    }

    @Test
    fun everyFifthFloorIsABoss() {
        val e = engine()
        repeat(15) {
            assertEquals(e.state.floor % 5 == 0, e.state.isBossFloor, "floor ${e.state.floor}")
            if (e.state.isBossFloor) assertEquals(Bestiary.bossFor(School.ARITHMETIC), e.state.enemy.type)
            e.clearFloor()
            e.nextFloor()
        }
    }

    @Test
    fun bossesAreTougherAndGiveMoreTime() {
        val type = Bestiary.bossFor(School.ARITHMETIC)
        val regular = Bestiary.forSchool(School.ARITHMETIC).first { it.hpFactor == 1.0 }
        assertTrue(Balance.enemyMaxHp(5, type) > Balance.enemyMaxHp(5, regular))
        assertTrue(Balance.enemyDamage(5, type) > Balance.enemyDamage(5, regular))
        val e = engine()
        repeat(4) { e.clearFloor(); e.nextFloor() }
        assertTrue(e.state.isBossFloor)
        assertEquals(
            (Balance.timeLimitMillis(e.state.problem.difficulty, false) * Balance.BOSS_TIME_MULTIPLIER).toLong(),
            e.state.timeLimitMillis,
        )
    }

    @Test
    fun defeatingBossCounts() {
        val e = engine()
        repeat(5) { e.clearFloor(); if (e.state.floor < 5) e.nextFloor() }
        assertEquals(1, e.state.stats.bossesDefeated)
    }
}
