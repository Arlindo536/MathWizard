package com.arlindodishi.mathwizard.data

import android.content.Context
import androidx.core.content.edit

/** Best floor and number of climbs. Moves to Room/DataStore with meta progression (step 5). */
interface SpireRecords {
    val bestFloor: Int
    val runsPlayed: Int

    /** Stores a finished run; returns true if [floor] is a new best. */
    fun recordRun(floor: Int): Boolean
}

class PrefsSpireRecords(context: Context) : SpireRecords {
    private val prefs = context.applicationContext.getSharedPreferences("Spire", Context.MODE_PRIVATE)

    override val bestFloor: Int get() = prefs.getInt(KEY_BEST_FLOOR, 0)
    override val runsPlayed: Int get() = prefs.getInt(KEY_RUNS, 0)

    override fun recordRun(floor: Int): Boolean {
        val newBest = floor > bestFloor
        prefs.edit {
            putInt(KEY_RUNS, runsPlayed + 1)
            if (newBest) putInt(KEY_BEST_FLOOR, floor)
        }
        return newBest
    }

    private companion object {
        const val KEY_BEST_FLOOR = "best_floor"
        const val KEY_RUNS = "runs_played"
    }
}
