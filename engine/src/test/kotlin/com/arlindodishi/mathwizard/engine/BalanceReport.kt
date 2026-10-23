package com.arlindodishi.mathwizard.engine

import kotlin.test.Test

/** Prints the balance table: ./gradlew :engine:test --tests '*BalanceReport*' -i */
class BalanceReport {
    @Test
    fun report() {
        for (bonuses in listOf("no upgrades" to RunBonuses(), "all upgrades" to BalanceSimulation.maxed)) {
            for (bot in BalanceSimulation.bots) {
                val r = BalanceSimulation.run(bot, bonuses.second)
                println(
                    "BALANCE %-12s %-9s floor mean %5.1f median %3d | crystals %5.1f | answers %5.1f | %4.1f min"
                        .format(bonuses.first, bot.name, r.meanFloor, r.medianFloor, r.meanCrystals, r.meanAnswers, r.meanMinutes),
                )
            }
        }
    }
}
