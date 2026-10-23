package com.arlindodishi.mathwizard.engine

import kotlin.test.Test
import kotlin.test.assertTrue

/** Guards the run length targets (5-15 min for a typical new player) against accidental changes. */
class BalanceTest {
    private fun floors(bot: String, bonuses: RunBonuses = RunBonuses()) =
        BalanceSimulation.run(BalanceSimulation.bots.first { it.name == bot }, bonuses, runs = 120)

    @Test
    fun newPlayersReachSensibleFloors() {
        val beginner = floors("beginner")
        val average = floors("average")
        val strong = floors("strong")
        assertTrue(beginner.meanFloor in 4.0..12.0, "beginner ${beginner.meanFloor}")
        assertTrue(average.meanFloor in 12.0..30.0, "average ${average.meanFloor}")
        assertTrue(strong.meanFloor > average.meanFloor, "strong ${strong.meanFloor}")
        assertTrue(average.meanMinutes in 5.0..18.0, "average run ${average.meanMinutes} min")
    }

    @Test
    fun upgradesHelpButRunsStillEnd() {
        val plain = floors("average")
        val maxed = floors("average", BalanceSimulation.maxed)
        assertTrue(maxed.meanFloor > plain.meanFloor * 1.2, "upgrades should matter: ${plain.meanFloor} -> ${maxed.meanFloor}")
        assertTrue(floors("strong", BalanceSimulation.maxed).medianFloor < 300, "even a maxed strong player eventually falls")
    }
}
