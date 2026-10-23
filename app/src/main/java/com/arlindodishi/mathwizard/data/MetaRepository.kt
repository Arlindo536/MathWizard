package com.arlindodishi.mathwizard.data

import com.arlindodishi.mathwizard.data.db.MetaDao
import com.arlindodishi.mathwizard.data.db.RunEntity
import com.arlindodishi.mathwizard.engine.MetaProfile
import com.arlindodishi.mathwizard.engine.MetaRewards
import com.arlindodishi.mathwizard.engine.RunState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

data class Records(val runsPlayed: Int = 0, val bestFloor: Int = 0, val totalCrystals: Int = 0)

data class RunReward(val crystals: Int, val newRecord: Boolean, val bestFloor: Int)

/** Progress kept between runs: crystals, upgrades and finished climbs. */
interface MetaRepository {
    val profile: Flow<MetaProfile>
    val records: Flow<Records>

    /** Stores a finished run and credits its crystals. */
    suspend fun recordRun(state: RunState, seed: Long): RunReward

    /** Buys one level of an upgrade; false if not affordable or locked. */
    suspend fun buy(upgradeId: String): Boolean
}

class RoomMetaRepository(
    private val dao: MetaDao,
    private val clock: () -> Long = System::currentTimeMillis,
) : MetaRepository {

    override val profile: Flow<MetaProfile> =
        combine(dao.observeCrystals(), dao.observeUpgrades()) { crystals, upgrades ->
            MetaProfile(crystals, upgrades.associate { it.id to it.level })
        }

    override val records: Flow<Records> =
        dao.observeRecords().map { Records(it.runsPlayed, it.bestFloor, it.totalCrystals) }

    override suspend fun recordRun(state: RunState, seed: Long): RunReward {
        val crystals = MetaRewards.crystalsFor(state.stats)
        val previousBest = dao.recordRun(
            RunEntity(
                endedAtMillis = clock(),
                seed = seed,
                floor = state.floor,
                answered = state.stats.answered,
                correct = state.stats.correct,
                bestStreak = state.stats.bestStreak,
                bossesDefeated = state.stats.bossesDefeated,
                crystalsEarned = crystals,
            ),
        )
        return RunReward(crystals, newRecord = state.floor > previousBest, bestFloor = maxOf(previousBest, state.floor))
    }

    override suspend fun buy(upgradeId: String): Boolean = dao.buy(upgradeId)

    /** Keeps the best floor from the SharedPreferences era (step 3) as one legacy run. */
    suspend fun importLegacyBest(bestFloor: Int) {
        if (bestFloor <= 0 || dao.runCount() > 0) return
        dao.insertRun(
            RunEntity(
                endedAtMillis = clock(),
                seed = 0,
                floor = bestFloor,
                answered = 0,
                correct = 0,
                bestStreak = 0,
                bossesDefeated = 0,
                crystalsEarned = 0,
                legacy = true,
            ),
        )
    }
}
