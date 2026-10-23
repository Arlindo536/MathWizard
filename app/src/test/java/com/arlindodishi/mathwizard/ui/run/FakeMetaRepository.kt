package com.arlindodishi.mathwizard.ui.run

import com.arlindodishi.mathwizard.data.MetaRepository
import com.arlindodishi.mathwizard.data.Records
import com.arlindodishi.mathwizard.data.RunReward
import com.arlindodishi.mathwizard.engine.MetaProfile
import com.arlindodishi.mathwizard.engine.MetaRewards
import com.arlindodishi.mathwizard.engine.RunState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class FakeMetaRepository(initial: MetaProfile = MetaProfile()) : MetaRepository {
    override val profile = MutableStateFlow(initial)
    override val records = MutableStateFlow(Records())
    override val bestBySchool = MutableStateFlow(emptyMap<com.arlindodishi.mathwizard.engine.School, Int>())
    override val totals = MutableStateFlow(com.arlindodishi.mathwizard.data.Totals())
    override val recentRuns = MutableStateFlow(emptyList<com.arlindodishi.mathwizard.data.RecentRun>())
    override val dailyResults = MutableStateFlow(emptyList<com.arlindodishi.mathwizard.data.DailyResult>())
    val recorded = mutableListOf<RunState>()
    val recordedDaily = mutableListOf<Pair<Long, RunState>>()

    override suspend fun recordDaily(state: RunState, epochDay: Long): Int? {
        if (recordedDaily.any { it.first == epochDay }) return null
        recordedDaily += epochDay to state
        return MetaRewards.crystalsFor(state.stats)
    }

    override suspend fun recordRun(state: RunState, seed: Long): RunReward {
        recorded += state
        val crystals = MetaRewards.crystalsFor(state.stats)
        val previousBest = records.value.bestFloor
        records.update { Records(it.runsPlayed + 1, maxOf(it.bestFloor, state.floor), it.totalCrystals + crystals) }
        profile.update { it.copy(crystals = it.crystals + crystals) }
        return RunReward(crystals, state.floor > previousBest, maxOf(previousBest, state.floor))
    }

    override suspend fun buy(upgradeId: String): Boolean {
        val next = profile.value.buy(upgradeId) ?: return false
        profile.value = next
        return true
    }
}
