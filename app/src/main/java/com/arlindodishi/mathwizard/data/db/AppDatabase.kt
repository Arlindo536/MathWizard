package com.arlindodishi.mathwizard.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [RunEntity::class, UpgradeEntity::class, WalletEntity::class, DailyRunEntity::class],
    version = 3,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun metaDao(): MetaDao

    companion object {
        /** v2: runs remember their spell school; older runs were all Arithmetic. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE runs ADD COLUMN school TEXT NOT NULL DEFAULT 'ARITHMETIC'")
            }
        }

        /** v3: Daily Rift results, one row per day. */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS daily_runs (epochDay INTEGER NOT NULL, school TEXT NOT NULL, " +
                        "floor INTEGER NOT NULL, answered INTEGER NOT NULL, correct INTEGER NOT NULL, " +
                        "crystalsEarned INTEGER NOT NULL, endedAtMillis INTEGER NOT NULL, PRIMARY KEY(epochDay))",
                )
            }
        }

        val MIGRATIONS = arrayOf(MIGRATION_1_2, MIGRATION_2_3)
    }
}
