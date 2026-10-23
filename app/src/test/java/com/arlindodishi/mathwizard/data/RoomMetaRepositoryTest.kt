package com.arlindodishi.mathwizard.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.arlindodishi.mathwizard.data.db.AppDatabase
import com.arlindodishi.mathwizard.engine.RunConfig
import com.arlindodishi.mathwizard.engine.RunEngine
import com.arlindodishi.mathwizard.engine.RunState
import com.arlindodishi.mathwizard.engine.RunStats
import com.arlindodishi.mathwizard.engine.School
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RoomMetaRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var repo: RoomMetaRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        repo = RoomMetaRepository(db.metaDao(), clock = { 1_000L })
    }

    @After
    fun tearDown() = db.close()

    private fun finishedRun(floor: Int, stats: RunStats, school: School = School.ARITHMETIC): RunState =
        RunEngine(RunConfig(seed = 1L, school = school)).state.copy(floor = floor, stats = stats)

    @Test
    fun emptyDatabaseHasNoProgress() = runTest {
        assertEquals(0, repo.profile.first().crystals)
        assertEquals(Records(), repo.records.first())
    }

    @Test
    fun recordingRunsCreditsCrystalsAndTracksBest() = runTest {
        val first = repo.recordRun(finishedRun(6, RunStats(correct = 25, floorsCleared = 5, bossesDefeated = 1)), seed = 7L)
        assertEquals(5 * 3 + 10 + 5, first.crystals)
        assertTrue(first.newRecord)
        assertEquals(6, first.bestFloor)

        val second = repo.recordRun(finishedRun(3, RunStats(correct = 10, floorsCleared = 2)), seed = 8L)
        assertFalse(second.newRecord)
        assertEquals(6, second.bestFloor)

        assertEquals(first.crystals + second.crystals, repo.profile.first().crystals)
        assertEquals(Records(runsPlayed = 2, bestFloor = 6, totalCrystals = first.crystals + second.crystals), repo.records.first())
    }

    @Test
    fun buyingSpendsCrystalsAndPersistsLevel() = runTest {
        repo.recordRun(finishedRun(10, RunStats(floorsCleared = 10)), seed = 1L) // 30 crystals
        assertTrue(repo.buy("vitality"))
        val profile = repo.profile.first()
        assertEquals(10, profile.crystals)
        assertEquals(1, profile.level("vitality"))
        assertFalse("second level costs 35", repo.buy("vitality"))
        assertFalse("locked behind focus", repo.buy("hint"))
        assertEquals(10, repo.profile.first().crystals)
    }

    @Test
    fun legacyBestFloorIsImportedOnceIntoEmptyHistory() = runTest {
        repo.importLegacyBest(9)
        repo.importLegacyBest(12)
        assertEquals(Records(runsPlayed = 1, bestFloor = 9, totalCrystals = 0), repo.records.first())
    }

    @Test
    fun legacyImportSkippedWhenRunsExist() = runTest {
        repo.recordRun(finishedRun(2, RunStats()), seed = 1L)
        repo.importLegacyBest(9)
        assertEquals(2, repo.records.first().bestFloor)
    }

    @Test
    fun recordsAreKeptPerSchool() = runTest {
        repo.recordRun(finishedRun(8, RunStats()), seed = 1L)
        val fractions = repo.recordRun(finishedRun(3, RunStats(), School.FRACTIONS), seed = 2L)
        assertTrue("first Fractions run is a Fractions record", fractions.newRecord)
        assertEquals(3, fractions.bestFloor)
        assertEquals(mapOf(School.ARITHMETIC to 8, School.FRACTIONS to 3), repo.bestBySchool.first())
        assertEquals(8, repo.records.first().bestFloor)
    }

    @Test
    fun dailyIsRecordedOncePerDayAndCreditsCrystals() = runTest {
        val state = finishedRun(6, RunStats(correct = 20, floorsCleared = 5, bossesDefeated = 1), School.POWERS)
        val first = repo.recordDaily(state, epochDay = 20_000)
        assertEquals(5 * 3 + 10 + 4, first)
        assertEquals(null, repo.recordDaily(state, epochDay = 20_000))
        repo.recordDaily(state.copy(floor = 3), epochDay = 20_001)
        assertEquals(listOf(20_001L, 20_000L), repo.dailyResults.first().map { it.epochDay })
        assertEquals(School.POWERS, repo.dailyResults.first().last().school)
        assertEquals(2 * first!!, repo.profile.first().crystals)
        assertEquals("daily runs do not count as climbs", 0, repo.records.first().runsPlayed)
    }

    @Test
    fun totalsAndRecentRunsSummariseClimbs() = runTest {
        repo.recordRun(finishedRun(4, RunStats(answered = 12, correct = 10, bestStreak = 6, bossesDefeated = 0)), seed = 1L)
        repo.recordRun(finishedRun(7, RunStats(answered = 20, correct = 15, bestStreak = 9, bossesDefeated = 1), School.ALGEBRA), seed = 2L)
        assertEquals(Totals(answered = 32, correct = 25, bosses = 1, bestStreak = 9), repo.totals.first())
        assertEquals(listOf(7, 4), repo.recentRuns.first().map { it.floor })
        repo.importLegacyBest(30)
        assertEquals("legacy rows are not recent runs", 2, repo.recentRuns.first().size)
    }
}
