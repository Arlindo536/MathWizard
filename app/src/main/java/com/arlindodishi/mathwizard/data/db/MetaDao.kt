package com.arlindodishi.mathwizard.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.arlindodishi.mathwizard.engine.MetaProfile
import kotlinx.coroutines.flow.Flow

@Dao
abstract class MetaDao {

    @Query("SELECT COALESCE((SELECT crystals FROM wallet WHERE id = 0), 0)")
    abstract fun observeCrystals(): Flow<Int>

    @Query("SELECT * FROM upgrades")
    abstract fun observeUpgrades(): Flow<List<UpgradeEntity>>

    @Query(
        "SELECT COUNT(*) AS runsPlayed, COALESCE(MAX(floor), 0) AS bestFloor, " +
            "COALESCE(SUM(crystalsEarned), 0) AS totalCrystals FROM runs",
    )
    abstract fun observeRecords(): Flow<RecordsRow>

    @Query("SELECT school, MAX(floor) AS bestFloor FROM runs GROUP BY school")
    abstract fun observeBestBySchool(): Flow<List<SchoolBestRow>>

    @Query("SELECT COALESCE((SELECT crystals FROM wallet WHERE id = 0), 0)")
    abstract suspend fun crystals(): Int

    @Query("SELECT * FROM upgrades")
    abstract suspend fun upgrades(): List<UpgradeEntity>

    @Query("SELECT COALESCE(MAX(floor), 0) FROM runs WHERE school = :school")
    abstract suspend fun bestFloor(school: String): Int

    @Query("SELECT COUNT(*) FROM runs")
    abstract suspend fun runCount(): Int

    @Insert
    abstract suspend fun insertRun(run: RunEntity): Long

    @Upsert
    abstract suspend fun upsertWallet(wallet: WalletEntity)

    @Upsert
    abstract suspend fun upsertUpgrade(upgrade: UpgradeEntity)

    /** Saves the run and credits its crystals atomically. Returns the previous best in its school. */
    @Transaction
    open suspend fun recordRun(run: RunEntity): Int {
        val previousBest = bestFloor(run.school)
        insertRun(run)
        upsertWallet(WalletEntity(crystals = crystals() + run.crystalsEarned))
        return previousBest
    }

    /** Buys one level of [upgradeId] if the rules in [MetaProfile.buy] allow it. */
    @Transaction
    open suspend fun buy(upgradeId: String): Boolean {
        val profile = MetaProfile(crystals(), upgrades().associate { it.id to it.level })
        val next = profile.buy(upgradeId) ?: return false
        upsertWallet(WalletEntity(crystals = next.crystals))
        upsertUpgrade(UpgradeEntity(upgradeId, next.level(upgradeId)))
        return true
    }
}
