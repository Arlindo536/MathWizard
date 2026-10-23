package com.arlindodishi.mathwizard.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MetaTest {

    @Test
    fun upgradeIdsAreUniqueAndPrerequisitesExist() {
        assertEquals(Upgrades.all.size, Upgrades.all.map { it.id }.toSet().size)
        Upgrades.all.forEach { u ->
            assertTrue(u.costs.isNotEmpty())
            assertEquals(u.costs.sorted(), u.costs, "${u.id} costs should rise")
            u.requires?.let { req -> assertTrue(u.requiresLevel <= Upgrades.byId(req).maxLevel) }
        }
    }

    @Test
    fun buyingDeductsCostAndRaisesLevel() {
        val p = MetaProfile(crystals = 50).buy("vitality")
        assertNotNull(p)
        assertEquals(30, p.crystals)
        assertEquals(1, p.level("vitality"))
        assertEquals(35, p.nextCost("vitality"))
    }

    @Test
    fun cannotBuyWithoutEnoughCrystals() {
        assertNull(MetaProfile(crystals = 19).buy("vitality"))
    }

    @Test
    fun cannotBuyPastMaxLevel() {
        val maxed = MetaProfile(crystals = 10_000, levels = mapOf("relic_insight" to 1, "power" to 5))
        assertTrue(maxed.isMaxed("relic_insight"))
        assertNull(maxed.nextCost("relic_insight"))
        assertNull(maxed.buy("relic_insight"))
    }

    @Test
    fun prerequisitesGateUpgrades() {
        val rich = MetaProfile(crystals = 10_000)
        assertFalse(rich.canBuy("second_wind"))
        assertFalse(rich.canBuy("relic_insight"))
        assertTrue(rich.copy(levels = mapOf("vitality" to 1)).canBuy("second_wind"))
        assertFalse(rich.copy(levels = mapOf("power" to 1)).canBuy("relic_insight"))
        assertTrue(rich.copy(levels = mapOf("power" to 2)).canBuy("relic_insight"))
    }

    @Test
    fun everythingCanEventuallyBeBought() {
        var p = MetaProfile(crystals = 100_000)
        repeat(100) { Upgrades.all.forEach { u -> p.buy(u.id)?.let { p = it } } }
        Upgrades.all.forEach { assertTrue(p.isMaxed(it.id), it.id) }
        assertEquals(100_000 - Upgrades.all.sumOf { it.costs.sum() }, p.crystals)
    }

    @Test
    fun bonusesFollowLevels() {
        val b = MetaProfile(
            levels = mapOf("vitality" to 3, "second_wind" to 2, "focus" to 2, "hint" to 1, "power" to 4, "relic_insight" to 1, "starting_relic" to 1),
        ).bonuses()
        assertEquals(30, b.maxHpBonus)
        assertEquals(10, b.floorHealBonus)
        assertEquals(1.16, b.timeMultiplier, 1e-9)
        assertEquals(1.2, b.damageMultiplier, 1e-9)
        assertEquals(1, b.hints)
        assertEquals(4, b.relicOfferSize)
        assertTrue(b.startingRelic)
        assertEquals(RunBonuses(), MetaProfile().bonuses())
    }

    @Test
    fun crystalRewardsFormula() {
        assertEquals(0, MetaRewards.crystalsFor(RunStats()))
        assertEquals(7 * 3 + 10 + 35 / 5, MetaRewards.crystalsFor(RunStats(correct = 35, floorsCleared = 7, bossesDefeated = 1)))
    }

    // --- bonuses inside a run ---

    private fun engine(bonuses: RunBonuses, seed: Long = 1L) = RunEngine(RunConfig(seed = seed, bonuses = bonuses))

    @Test
    fun maxHpBonusRaisesStartingHealth() {
        val s = engine(RunBonuses(maxHpBonus = 30)).state
        assertEquals(130, s.playerMaxHp)
        assertEquals(130, s.playerHp)
    }

    @Test
    fun damageAndTimeBonusesApply() {
        val e = engine(RunBonuses(damageMultiplier = 1.2, timeMultiplier = 1.16))
        val base = Balance.timeLimitMillis(e.state.problem.difficulty, relaxed = false)
        assertEquals((base * 1.16).toLong(), e.state.timeLimitMillis)
        val hit = e.answer(e.state.problem.answer, e.state.timeLimitMillis - 1) as AnswerOutcome.Hit
        assertEquals(30, hit.damage)
    }

    @Test
    fun floorHealBonusApplies() {
        val e = engine(RunBonuses(floorHealBonus = 10))
        repeat(3) { e.answer(e.state.problem.choices.first { it != e.state.problem.answer }, 1_000) }
        val hurt = e.state.playerHp
        while (e.state.phase == Phase.FIGHTING) e.answer(e.state.problem.answer, 1_000)
        e.nextFloor()
        assertEquals(hurt + Balance.FLOOR_HEAL + 10, e.state.playerHp)
    }

    @Test
    fun relicInsightOffersFour() {
        val e = engine(RunBonuses(relicOfferSize = 4))
        while (e.state.phase == Phase.FIGHTING) e.answer(e.state.problem.answer, 1_000)
        assertEquals(4, e.state.relicOffer.size)
    }

    @Test
    fun startingRelicIsDeterministic() {
        val a = engine(RunBonuses(startingRelic = true), seed = 11L).state.relics
        val b = engine(RunBonuses(startingRelic = true), seed = 11L).state.relics
        assertEquals(1, a.size)
        assertEquals(a, b)
        assertTrue(engine(RunBonuses()).state.relics.isEmpty())
    }

    @Test
    fun startingHeartstoneRaisesHp() {
        val seed = (1L..500L).first { engine(RunBonuses(startingRelic = true), it).state.relics.single().id == "heartstone" }
        assertEquals(125, engine(RunBonuses(startingRelic = true), seed).state.playerHp)
    }

    @Test
    fun hintsLeaveTwoChoicesAndRunOut() {
        val e = engine(RunBonuses(hints = 2))
        assertEquals(2, e.state.hintsLeft)
        assertTrue(e.useHint())
        assertEquals(2, e.state.problem.choices.size)
        assertTrue(e.state.problem.answer in e.state.problem.choices)
        assertFalse(e.useHint(), "already down to two choices")
        assertEquals(1, e.state.hintsLeft)
        e.answer(e.state.problem.answer, 1_000)
        assertTrue(e.useHint())
        assertEquals(0, e.state.hintsLeft)
        e.answer(e.state.problem.answer, 1_000)
        assertFalse(e.useHint())
        assertEquals(CHOICE_COUNT, e.state.problem.choices.size)
    }

    @Test
    fun noHintsByDefault() {
        val e = engine(RunBonuses())
        assertFalse(e.useHint())
    }
}
