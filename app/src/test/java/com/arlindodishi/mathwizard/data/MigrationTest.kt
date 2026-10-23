package com.arlindodishi.mathwizard.data

import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import com.arlindodishi.mathwizard.data.db.AppDatabase
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), AppDatabase::class.java)

    @Test
    fun migrate1To2KeepsRunsAndMarksThemArithmetic() {
        helper.createDatabase(DB, 1).use { db ->
            db.execSQL(
                "INSERT INTO runs (endedAtMillis, seed, floor, answered, correct, bestStreak, bossesDefeated, crystalsEarned, legacy) " +
                    "VALUES (1, 2, 7, 30, 25, 6, 1, 40, 0)",
            )
            db.execSQL("INSERT INTO wallet (id, crystals) VALUES (0, 40)")
            db.execSQL("INSERT INTO upgrades (id, level) VALUES ('vitality', 1)")
        }

        helper.runMigrationsAndValidate(DB, 2, true, *AppDatabase.MIGRATIONS).use { db ->
            db.query("SELECT floor, school FROM runs").use { c ->
                c.moveToFirst()
                assertEquals(7, c.getInt(0))
                assertEquals("ARITHMETIC", c.getString(1))
            }
            db.query("SELECT crystals FROM wallet").use { c ->
                c.moveToFirst()
                assertEquals(40, c.getInt(0))
            }
            db.query("SELECT level FROM upgrades WHERE id = 'vitality'").use { c ->
                c.moveToFirst()
                assertEquals(1, c.getInt(0))
            }
        }
    }

    @Test
    fun migrate2To3AddsEmptyDailyTable() {
        helper.createDatabase(DB, 2).use { db ->
            db.execSQL(
                "INSERT INTO runs (endedAtMillis, seed, floor, answered, correct, bestStreak, bossesDefeated, crystalsEarned, legacy, school) " +
                    "VALUES (1, 2, 9, 30, 25, 6, 1, 40, 0, 'ALGEBRA')",
            )
        }
        helper.runMigrationsAndValidate(DB, 3, true, *AppDatabase.MIGRATIONS).use { db ->
            db.query("SELECT COUNT(*) FROM daily_runs").use { c -> c.moveToFirst(); assertEquals(0, c.getInt(0)) }
            db.query("SELECT school FROM runs").use { c -> c.moveToFirst(); assertEquals("ALGEBRA", c.getString(0)) }
        }
    }

    private companion object {
        const val DB = "migration-test"
    }
}
