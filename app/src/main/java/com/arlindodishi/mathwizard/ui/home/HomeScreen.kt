package com.arlindodishi.mathwizard.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.arlindodishi.mathwizard.R
import com.arlindodishi.mathwizard.engine.School
import com.arlindodishi.mathwizard.ui.run.EnemyArt
import com.arlindodishi.mathwizard.ui.theme.SpireColors
import com.arlindodishi.mathwizard.ui.theme.Spacing
import com.arlindodishi.mathwizard.ui.theme.accent

@Composable
fun HomeScreen(
    bestFloor: Int,
    runsPlayed: Int,
    onClimb: () -> Unit,
    onClassic: () -> Unit,
    onStats: () -> Unit,
    onSettings: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(SpireColors.Night, SpireColors.NightRaised)))
            .safeDrawingPadding()
            .padding(Spacing.l),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Placeholder emblem until the animated spire (step 7).
        EnemyArt(enemyId = "wizard", accent = School.ARITHMETIC.accent(), defeated = false, modifier = Modifier.size(160.dp))
        Spacer(Modifier.height(Spacing.m))
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.displayLarge, color = SpireColors.Text)
        Text(
            stringResource(R.string.spire_title),
            style = MaterialTheme.typography.headlineMedium,
            color = SpireColors.Gold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Spacing.s))
        if (runsPlayed > 0) {
            Text(
                stringResource(R.string.home_best_floor, bestFloor) + "  ·  " + stringResource(R.string.home_runs, runsPlayed),
                style = MaterialTheme.typography.bodyLarge,
                color = SpireColors.TextMuted,
            )
        }
        Spacer(Modifier.height(Spacing.xl))
        Button(onClick = onClimb, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().height(60.dp)) {
            Text(stringResource(R.string.home_climb), style = MaterialTheme.typography.titleLarge)
        }
        Spacer(Modifier.height(Spacing.m))
        listOf(
            R.string.home_classic to onClassic,
            R.string.high_score to onStats,
            R.string.settings to onSettings,
        ).forEach { (label, action) ->
            OutlinedButton(
                onClick = action,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp).padding(vertical = 2.dp),
            ) {
                Text(stringResource(label), style = MaterialTheme.typography.titleMedium, color = SpireColors.Text)
            }
        }
    }
}
