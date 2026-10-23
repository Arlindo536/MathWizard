package com.arlindodishi.mathwizard.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [RunEntity::class, UpgradeEntity::class, WalletEntity::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun metaDao(): MetaDao
}
