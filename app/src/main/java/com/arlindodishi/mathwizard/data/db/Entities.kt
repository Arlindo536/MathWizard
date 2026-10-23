package com.arlindodishi.mathwizard.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/** One finished climb. [legacy] rows were imported from the pre-Room best floor. */
@Entity(tableName = "runs")
data class RunEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val endedAtMillis: Long,
    val seed: Long,
    val floor: Int,
    val answered: Int,
    val correct: Int,
    val bestStreak: Int,
    val bossesDefeated: Int,
    val crystalsEarned: Int,
    val legacy: Boolean = false,
)

@Entity(tableName = "upgrades")
data class UpgradeEntity(@PrimaryKey val id: String, val level: Int)

/** Single row (id 0) holding the crystal balance. */
@Entity(tableName = "wallet")
data class WalletEntity(@PrimaryKey val id: Int = 0, val crystals: Int)

data class RecordsRow(val runsPlayed: Int, val bestFloor: Int, val totalCrystals: Int)
