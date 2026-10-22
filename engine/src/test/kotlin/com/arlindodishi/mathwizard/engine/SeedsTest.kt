package com.arlindodishi.mathwizard.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class SeedsTest {

    @Test
    fun derivationIsDeterministic() {
        assertEquals(Seeds.problem(42L, 3, 1), Seeds.problem(42L, 3, 1))
        assertEquals(Seeds.daily(20_000L), Seeds.daily(20_000L))
    }

    @Test
    fun argumentOrderAndValuesMatter() {
        assertNotEquals(Seeds.problem(42L, 3, 1), Seeds.problem(42L, 1, 3))
        assertNotEquals(Seeds.problem(42L, 3, 1), Seeds.problem(43L, 3, 1))
        assertNotEquals(Seeds.daily(20_000L), Seeds.daily(20_001L))
    }

    @Test
    fun seedsAreWellSpread() {
        val seeds = (0 until 10_000).map { Seeds.problem(0L, it / 100, it % 100) }.toSet()
        assertEquals(10_000, seeds.size)
    }

    @Test
    fun dailyRiftIsReproducible() {
        val seed = Seeds.daily(20_383L)
        val first = (0 until 5).map { Problems.generate(School.ARITHMETIC, 30, Seeds.mix(seed, it.toLong())) }
        val second = (0 until 5).map { Problems.generate(School.ARITHMETIC, 30, Seeds.mix(seed, it.toLong())) }
        assertEquals(first, second)
    }
}
