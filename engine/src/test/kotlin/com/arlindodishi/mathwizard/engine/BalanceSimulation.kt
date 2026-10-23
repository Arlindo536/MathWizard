package com.arlindodishi.mathwizard.engine

import kotlin.random.Random

/** A simulated player: accuracy falls a little as difficulty rises; answer time is a share of the timer. */
data class Bot(val name: String, val accuracy: Double, val accuracyDropAt100: Double, val minTime: Double, val maxTime: Double)

data class SimResult(val bot: Bot, val bonuses: RunBonuses, val meanFloor: Double, val medianFloor: Int, val meanCrystals: Double, val meanAnswers: Double, val meanMinutes: Double)

object BalanceSimulation {
    val bots = listOf(
        Bot("beginner", accuracy = 0.80, accuracyDropAt100 = 0.30, minTime = 0.45, maxTime = 0.95),
        Bot("average", accuracy = 0.90, accuracyDropAt100 = 0.25, minTime = 0.30, maxTime = 0.75),
        Bot("strong", accuracy = 0.96, accuracyDropAt100 = 0.15, minTime = 0.15, maxTime = 0.50),
    )

    fun run(bot: Bot, bonuses: RunBonuses = RunBonuses(), runs: Int = 300, school: School = School.ARITHMETIC): SimResult {
        val floors = mutableListOf<Int>()
        var crystals = 0.0
        var answers = 0.0
        var seconds = 0.0
        repeat(runs) { i ->
            val r = Random(i * 31L + bot.name.hashCode())
            val e = RunEngine(RunConfig(seed = i.toLong(), school = school, bonuses = bonuses))
            while (e.state.phase != Phase.GAME_OVER && e.state.floor < 300) {
                if (e.state.phase == Phase.FLOOR_CLEARED) {
                    e.nextFloor(e.state.relicOffer.firstOrNull()?.id)
                    continue
                }
                val p = e.state.problem
                val chance = bot.accuracy - bot.accuracyDropAt100 * p.difficulty / 100.0
                val share = bot.minTime + r.nextDouble() * (bot.maxTime - bot.minTime)
                val elapsed = (e.state.timeLimitMillis * share).toLong()
                seconds += elapsed / 1000.0 + 0.9 // plus feedback time
                answers++
                if (r.nextDouble() < chance) e.answer(p.answer, elapsed) else e.answer(p.choices.first { it != p.answer }, elapsed)
            }
            floors += e.state.floor
            crystals += MetaRewards.crystalsFor(e.state.stats)
        }
        floors.sort()
        return SimResult(bot, bonuses, floors.average(), floors[floors.size / 2], crystals / runs, answers / runs, seconds / runs / 60)
    }

    /** Bonuses of a player who has bought every upgrade. */
    val maxed: RunBonuses = MetaProfile(levels = Upgrades.all.associate { it.id to it.maxLevel }).bonuses()
}
