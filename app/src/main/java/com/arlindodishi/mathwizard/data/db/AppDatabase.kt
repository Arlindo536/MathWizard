package com.arlindodishi.mathwizard.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [RunEntity::class, UpgradeEntity::class, WalletEntity::class], version = 2, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun metaDao(): MetaDao

    companion object {
        /** v2: runs remember their spell school; older runs were all Arithmetic. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE runs ADD COLUMN school TEXT NOT NULL DEFAULT 'ARITHMETIC'")
            }
        }

        val MIGRATIONS = arrayOf(MIGRATION_1_2)
    }
}
