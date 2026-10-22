package com.arlindodishi.mathwizard.engine

/**
 * Deterministic seed derivation, so a run (and the Daily Rift) can be replayed exactly.
 * Uses the SplitMix64 finalizer to mix inputs into well-spread 64-bit seeds.
 */
object Seeds {
    private const val GOLDEN_GAMMA = -0x61c8864680b583ebL
    private const val DAILY_SALT = 0x4D61746857697A64L // "MathWizd"

    fun mix(vararg parts: Long): Long {
        var h = 0L
        for (part in parts) {
            h = splitMix(h + GOLDEN_GAMMA + part)
        }
        return h
    }

    /** Seed for the [index]-th problem on [floor] of the run started with [runSeed]. */
    fun problem(runSeed: Long, floor: Int, index: Int): Long =
        mix(runSeed, floor.toLong(), index.toLong())

    /** Seed shared by every player for the Daily Rift of [epochDay] (days since 1970-01-01). */
    fun daily(epochDay: Long): Long = mix(DAILY_SALT, epochDay)

    private fun splitMix(x: Long): Long {
        var z = x
        z = (z xor (z ushr 30)) * -0x40a7b892e31b1a47L
        z = (z xor (z ushr 27)) * -0x6b2fb644ecceee15L
        return z xor (z ushr 31)
    }
}
