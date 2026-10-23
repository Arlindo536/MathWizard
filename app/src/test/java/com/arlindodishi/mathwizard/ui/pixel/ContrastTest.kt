package com.arlindodishi.mathwizard.ui.pixel

import androidx.compose.ui.graphics.Color
import com.arlindodishi.mathwizard.engine.School
import com.arlindodishi.mathwizard.ui.theme.SpireColors
import com.arlindodishi.mathwizard.ui.theme.accent
import org.junit.Assert.assertTrue
import org.junit.Test

/** WCAG AA (4.5:1) for the theme's text colours and button labels. */
class ContrastTest {
    private val backgrounds = listOf(SpireColors.Night, SpireColors.Surface, SpireColors.SurfaceRaised)
    private val schoolColours = School.entries.map { it.accent() }

    @Test
    fun themeTextColoursPassOnEveryBackground() {
        val texts = listOf(SpireColors.Text, SpireColors.TextMuted, SpireColors.Gold, SpireColors.Crystal, SpireColors.PlayerHp, SpireColors.Danger, SpireColors.Success) + schoolColours
        for (t in texts) for (bg in backgrounds) {
            assertTrue("$t on $bg: ${contrast(t, bg)}", contrast(t, bg) >= 4.5f)
        }
    }

    @Test
    fun darkLabelsPassOnEveryButtonColour() {
        val dark = Color(0xFF1A0D10)
        (listOf(SpireColors.Gold, SpireColors.Crystal, SpireColors.Success, SpireColors.Danger) + schoolColours).forEach {
            assertTrue("$it: ${contrast(dark, it)}", contrast(dark, it) >= 4.5f)
        }
    }

    @Test
    fun lightLabelsPassOnGreyAndRaisedButtons() {
        listOf(SpireColors.Track, SpireColors.SurfaceRaised).forEach {
            assertTrue("$it: ${contrast(SpireColors.Text, it)}", contrast(SpireColors.Text, it) >= 4.5f)
        }
    }
}
