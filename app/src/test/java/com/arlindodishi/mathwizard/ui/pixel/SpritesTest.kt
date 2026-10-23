package com.arlindodishi.mathwizard.ui.pixel

import com.arlindodishi.mathwizard.engine.Bestiary
import com.arlindodishi.mathwizard.engine.Relics
import com.arlindodishi.mathwizard.engine.School
import com.arlindodishi.mathwizard.engine.Upgrades
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Test

class SpritesTest {

    private fun assertWellFormed(name: String, sprite: PixelSprite) {
        assertTrue("$name is empty", sprite.width > 0 && sprite.height > 0)
        sprite.rows.forEachIndexed { i, row -> assertEquals("$name row $i width", sprite.width, row.length) }
        sprite.rows.joinToString("").toSet().minus(PixelSprite.TRANSPARENT).forEach { c ->
            assertTrue("$name uses '$c' without a colour", c in sprite.palette)
        }
        assertTrue("$name draws nothing", sprite.runs.isNotEmpty())
    }

    @Test
    fun everyEnemyHasItsOwnSprite() {
        Bestiary.all.forEach { type ->
            assertTrue("no sprite for ${type.id}", type.id in Sprites.enemyIds)
            val sprite = Sprites.enemy(type.id)
            assertWellFormed(type.id, sprite)
            assertEquals("${type.id} size", if (type.boss) 24 else 16, sprite.width)
        }
        assertEquals(Bestiary.all.map { it.id }.toSet(), Sprites.enemyIds)
    }

    @Test
    fun everyRelicHasAnIcon() {
        assertEquals(Relics.all.map { it.id }.toSet(), Sprites.relicIds)
        Relics.all.forEach { assertWellFormed(it.id, Sprites.relic(it.id)) }
    }

    @Test
    fun everyUpgradeHasARealIcon() {
        Upgrades.all.forEach { u ->
            val icon = Sprites.upgrade(u.id)
            assertWellFormed(u.id, icon)
            assertNotSame("${u.id} fell back to the crystal icon", Sprites.crystal, icon)
        }
    }

    @Test
    fun schoolAndInterfaceIconsAreWellFormed() {
        School.entries.forEach { assertWellFormed(it.name, Sprites.school(it)) }
        listOf(
            "wizard" to Sprites.wizard, "heart" to Sprites.heart, "crystal" to Sprites.crystal, "hint" to Sprites.hint,
            "lock" to Sprites.lock, "soundOn" to Sprites.soundOn, "soundOff" to Sprites.soundOff,
            "musicOn" to Sprites.musicOn, "musicOff" to Sprites.musicOff,
        ).forEach { (name, sprite) -> assertWellFormed(name, sprite) }
    }

    @Test
    fun runsCoverEveryOpaquePixelExactlyOnce() {
        val sprite = Sprites.enemy("ember_imp")
        val opaque = sprite.rows.sumOf { row -> row.count { it != PixelSprite.TRANSPARENT } }
        assertEquals(opaque, sprite.runs.sumOf { it.length })
    }
}
