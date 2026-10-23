package com.arlindodishi.mathwizard.ui.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.arlindodishi.mathwizard.R
import com.arlindodishi.mathwizard.engine.School
import com.arlindodishi.mathwizard.ui.pixel.IconValue
import com.arlindodishi.mathwizard.ui.pixel.PixelButton
import com.arlindodishi.mathwizard.ui.pixel.PixelImage
import com.arlindodishi.mathwizard.ui.pixel.PixelSprite
import com.arlindodishi.mathwizard.ui.pixel.SpireBackdrop
import com.arlindodishi.mathwizard.ui.pixel.Sprites
import com.arlindodishi.mathwizard.ui.theme.SpireColors
import com.arlindodishi.mathwizard.ui.theme.Spacing
import com.arlindodishi.mathwizard.ui.theme.accent

@Composable
fun HomeScreen(
    bestFloor: Int,
    runsPlayed: Int,
    crystals: Int,
    soundOn: Boolean,
    musicOn: Boolean,
    dailyDone: Boolean,
    dailyStreak: Int,
    onClimb: () -> Unit,
    onDaily: () -> Unit,
    onPractice: () -> Unit,
    onUpgrades: () -> Unit,
    onClassic: () -> Unit,
    onStats: () -> Unit,
    onSettings: () -> Unit,
    onToggleSound: () -> Unit,
    onToggleMusic: () -> Unit,
) {
    // The spire scrolls slowly forever, and the wizard bobs in two steps.
    val time = rememberInfiniteTransition(label = "home")
    val climb by time.animateFloat(0f, 24f, infiniteRepeatable(tween(60_000, easing = LinearEasing), RepeatMode.Restart), label = "climb")
    val bob by time.animateFloat(0f, 1f, infiniteRepeatable(tween(1000, easing = LinearEasing), RepeatMode.Restart), label = "bob")

    Box(Modifier.fillMaxSize().background(SpireColors.Night)) {
        SpireBackdrop(climb = climb, torch = School.ARITHMETIC.accent(), modifier = Modifier.fillMaxSize())
        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 44.dp, vertical = Spacing.m),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                AudioToggle(
                    sprite = if (soundOn) Sprites.soundOn else Sprites.soundOff,
                    label = stringResource(R.string.sound_toggle_cd),
                    on = soundOn,
                    onClick = onToggleSound,
                )
                AudioToggle(
                    sprite = if (musicOn) Sprites.musicOn else Sprites.musicOff,
                    label = stringResource(R.string.music_toggle_cd),
                    on = musicOn,
                    onClick = onToggleMusic,
                )
            }
            Spacer(Modifier.weight(1f))
            PixelImage(
                Sprites.wizard,
                Modifier
                    .size(128.dp)
                    .offset { IntOffset(0, if (bob < 0.5f) 0 else -4.dp.roundToPx()) },
            )
            Spacer(Modifier.height(Spacing.m))
            Text(stringResource(R.string.app_name).uppercase(), style = MaterialTheme.typography.displayLarge, color = SpireColors.Text, textAlign = TextAlign.Center)
            Text(stringResource(R.string.spire_title).uppercase(), style = MaterialTheme.typography.headlineMedium, color = SpireColors.Gold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(Spacing.s))
            if (runsPlayed > 0) {
                Text(
                    stringResource(R.string.home_best_floor, bestFloor) + "  ·  " + stringResource(R.string.home_runs, runsPlayed),
                    style = MaterialTheme.typography.bodyLarge,
                    color = SpireColors.TextMuted,
                )
            }
            Spacer(Modifier.height(Spacing.s))
            IconValue(Sprites.crystal, crystals.toString(), SpireColors.Crystal)
            Spacer(Modifier.height(Spacing.l))
            PixelButton(onClick = onClimb, color = School.ARITHMETIC.accent(), modifier = Modifier.fillMaxWidth().height(64.dp)) {
                Text(stringResource(R.string.home_climb).uppercase(), style = MaterialTheme.typography.titleLarge)
            }
            Spacer(Modifier.height(Spacing.s))
            PixelButton(onClick = onDaily, color = School.SEQUENCES.accent(), modifier = Modifier.fillMaxWidth().height(56.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.home_daily).uppercase(), style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.width(Spacing.m))
                    Text(
                        if (dailyDone) "\u2713" else stringResource(R.string.daily_ready).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    if (dailyStreak > 0) {
                        Spacer(Modifier.width(Spacing.s))
                        Text("\u00B7 $dailyStreak", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
            Spacer(Modifier.height(Spacing.s))
            listOf(
                listOf(R.string.home_practice to onPractice, R.string.home_upgrades to onUpgrades),
                listOf(R.string.high_score to onStats, R.string.settings to onSettings),
            ).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s), modifier = Modifier.padding(vertical = 3.dp)) {
                    row.forEach { (label, action) ->
                        PixelButton(
                            onClick = action,
                            color = SpireColors.SurfaceRaised,
                            contentColor = SpireColors.Text,
                            modifier = Modifier.weight(1f).height(52.dp),
                        ) {
                            Text(stringResource(label).uppercase(), style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
            TextButton(onClick = onClassic) {
                Text(stringResource(R.string.home_classic), style = MaterialTheme.typography.titleMedium, color = SpireColors.TextMuted)
            }
            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun AudioToggle(sprite: PixelSprite, label: String, on: Boolean, onClick: () -> Unit) {
    val state = stringResource(if (on) R.string.toggle_on else R.string.toggle_off)
    PixelButton(
        onClick = onClick,
        color = SpireColors.SurfaceRaised,
        modifier = Modifier
            .padding(start = Spacing.s)
            .size(52.dp)
            .semantics {
                contentDescription = label
                stateDescription = state
            },
    ) {
        PixelImage(sprite, Modifier.size(24.dp))
    }
}
