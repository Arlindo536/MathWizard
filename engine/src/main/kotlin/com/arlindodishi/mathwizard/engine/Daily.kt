package com.arlindodishi.mathwizard.engine

/**
 * The Daily Rift: one fixed-seed climb per day, the same for every player. The school rotates
 * through all six (unlocked or not), upgrades do not apply and difficulty does not adapt.
 */
object DailyRift {
    fun school(epochDay: Long): School = School.entries[Math.floorMod(epochDay, School.entries.size.toLong()).toInt()]

    fun config(epochDay: Long, relaxed: Boolean = false): RunConfig =
        RunConfig(seed = Seeds.daily(epochDay), school = school(epochDay), relaxed = relaxed, adaptive = false)
}

/** Day streaks from the set of days (epoch days) on which a Daily Rift was finished. */
object Streaks {
    /** Consecutive days ending today, or ending yesterday if today is not played yet. */
    fun current(days: Set<Long>, today: Long): Int {
        var day = if (today in days) today else today - 1
        var count = 0
        while (day in days) {
            count++
            day--
        }
        return count
    }

    fun best(days: Set<Long>): Int {
        var best = 0
        for (day in days) {
            if (day - 1 in days) continue // only count from the start of each run of days
            var length = 0
            while (day + length in days) length++
            best = maxOf(best, length)
        }
        return best
    }
}

/** Practice: no HP and no timer, one fixed difficulty per level. */
enum class PracticeLevel(val difficulty: Int) { EASY(10), MEDIUM(35), HARD(60), EXPERT(85) }
