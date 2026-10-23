package com.arlindodishi.mathwizard.data

import com.arlindodishi.mathwizard.data.db.DailyRunEntity
import com.arlindodishi.mathwizard.data.db.MetaDao
import com.arlindodishi.mathwizard.data.db.RunEntity
import com.arlindodishi.mathwizard.engine.MetaProfile
import com.arlindodishi.mathwizard.engine.MetaRewards
import com.arlindodishi.mathwizard.engine.RunState
import com.arlindodishi.mathwizard.engine.School
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

data class Records(val runsPlayed: Int = 0, val bestFloor: Int = 0, val totalCrystals: Int = 0)

data class Totals(val answered: Int = 0, val correct: Int = 0, val bosses: Int = 0, val bestStreak: Int = 0)

data class RecentRun(val school: School, val floor: Int, val endedAtMillis: Long, val crystals: Int)

data class DailyResult(val epochDay: Long, val school: School, val floor: Int, val correct: Int, val answered: Int, val crystals: Int)

/** [newRecord] and [bestFloor] are for the run's own school. */
data class RunReward(val crystals: Int, val newRecord: Boolean, val bestFloor: Int)

/** Progress kept between runs: crystals, upgrades and finished climbs. */
interface MetaRepository {
    val profile: Flow<MetaProfile>
    val records: Flow<Records>

    /** Best floor reached in each school that has been played. */
    val bestBySchool: Flow<Map<School, Int>>

    val totals: Flow<Totals>
    val recentRuns: Flow<List<RecentRun>>

    /** Finished Daily Rifts, newest first. */
    val dailyResults: Flow<List<DailyResult>>

    /** Stores a finished Daily Rift; returns the crystals earned, or null if the day was already played. */
    suspend fun recordDaily(state: RunState, epochDay: Long): Int?

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

    override val bestBySchool: Flow<Map<School, Int>> =
        dao.observeBestBySchool().map { rows ->
            rows.mapNotNull { row -> School.entries.find { it.name == row.school }?.let { it to row.bestFloor } }.toMap()
        }

    override val totals: Flow<Totals> =
        dao.observeTotals().map { Totals(it.answered, it.correct, it.bosses, it.bestStreak) }

    override val recentRuns: Flow<List<RecentRun>> = dao.observeRecentRuns(RECENT_LIMIT).map { rows ->
        rows.mapNotNull { row -> schoolOf(row.school)?.let { RecentRun(it, row.floor, row.endedAtMillis, row.crystalsEarned) } }
    }

    override val dailyResults: Flow<List<DailyResult>> = dao.observeDailyRuns().map { rows ->
        rows.mapNotNull { row ->
            schoolOf(row.school)?.let { DailyResult(row.epochDay, it, row.floor, row.correct, row.answered, row.crystalsEarned) }
        }
    }

    override suspend fun recordDaily(state: RunState, epochDay: Long): Int? {
        val crystals = MetaRewards.crystalsFor(state.stats)
        val saved = dao.recordDaily(
            DailyRunEntity(
                epochDay = epochDay,
                school = state.problem.school.name,
                floor = state.floor,
                answered = state.stats.answered,
                correct = state.stats.correct,
                crystalsEarned = crystals,
                endedAtMillis = clock(),
            ),
        )
        return if (saved) crystals else null
    }

    private fun schoolOf(name: String): School? = School.entries.find { it.name == name }

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
                school = state.problem.school.name,
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

    private companion object {
        const val RECENT_LIMIT = 10
    }
}
