package com.arlindodishi.mathwizard

import android.app.Application
import android.content.Context
import androidx.core.content.edit
import androidx.room.Room
import com.arlindodishi.mathwizard.data.MetaRepository
import com.arlindodishi.mathwizard.data.RoomMetaRepository
import com.arlindodishi.mathwizard.data.db.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MathWizardApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}

/** Manual dependency container; one instance per process. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val database = Room.databaseBuilder(appContext, AppDatabase::class.java, "mathwizard.db")
        .addMigrations(*AppDatabase.MIGRATIONS)
        .build()

    private val roomMeta = RoomMetaRepository(database.metaDao())
    val meta: MetaRepository = roomMeta

    val feedback: GameFeedback by lazy { GameFeedback(appContext) }

    init {
        scope.launch { importLegacyRecords() }
    }

    private suspend fun importLegacyRecords() {
        val prefs = appContext.getSharedPreferences(LEGACY_PREFS, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_IMPORTED, false)) return
        roomMeta.importLegacyBest(prefs.getInt(KEY_BEST_FLOOR, 0))
        prefs.edit { putBoolean(KEY_IMPORTED, true) }
    }

    private companion object {
        const val LEGACY_PREFS = "Spire"
        const val KEY_BEST_FLOOR = "best_floor"
        const val KEY_IMPORTED = "imported_to_room"
    }
}

val Context.appContainer: AppContainer get() = (applicationContext as MathWizardApplication).container
