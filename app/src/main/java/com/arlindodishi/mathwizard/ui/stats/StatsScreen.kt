package com.arlindodishi.mathwizard.ui.stats

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arlindodishi.mathwizard.R
import com.arlindodishi.mathwizard.appContainer
import com.arlindodishi.mathwizard.data.RecentRun
import com.arlindodishi.mathwizard.data.Records
import com.arlindodishi.mathwizard.data.Totals
import com.arlindodishi.mathwizard.engine.School
import com.arlindodishi.mathwizard.engine.Streaks
import com.arlindodishi.mathwizard.epochDayNow
import com.arlindodishi.mathwizard.ui.pixel.PixelImage
import com.arlindodishi.mathwizard.ui.pixel.Sprites
import com.arlindodishi.mathwizard.ui.pixel.pixelPanel
import com.arlindodishi.mathwizard.ui.schools.schoolUi
import com.arlindodishi.mathwizard.ui.theme.SpireColors
import com.arlindodishi.mathwizard.ui.theme.Spacing
import com.arlindodishi.mathwizard.ui.theme.accent
import com.arlindodishi.mathwizard.ui.upgrades.ScreenHeader
import java.text.DateFormat
import java.util.Date
import kotlin.math.roundToInt

data class StatsUi(
    val records: Records = Records(),
    val totals: Totals = Totals(),
    val bestBySchool: Map<School, Int> = emptyMap(),
    val recent: List<RecentRun> = emptyList(),
    val dailyStreak: Int = 0,
    val bestDailyStreak: Int = 0,
    val classicBest: Int = 0,
)

@Composable
fun StatsRoute(onBack: () -> Unit) {
    val context = LocalContext.current
    val meta = context.appContainer.meta
    val records by meta.records.collectAsStateWithLifecycle(Records())
    val totals by meta.totals.collectAsStateWithLifecycle(Totals())
    val best by meta.bestBySchool.collectAsStateWithLifecycle(emptyMap())
    val recent by meta.recentRuns.collectAsStateWithLifecycle(emptyList())
    val daily by meta.dailyResults.collectAsStateWithLifecycle(emptyList())
    val classicBest = remember { context.getSharedPreferences("Scores", Context.MODE_PRIVATE).getInt("best_score", 0) }
    val days = daily.map { it.epochDay }.toSet()
    StatsScreen(
        StatsUi(records, totals, best, recent, Streaks.current(days, epochDayNow()), Streaks.best(days), classicBest),
        onBack,
    )
}

@Composable
fun StatsScreen(stats: StatsUi, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().background(SpireColors.Night).safeDrawingPadding()) {
        ScreenHeader(title = stringResource(R.string.high_score), onBack = onBack)
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = Spacing.m)) {
            val t = stats.totals
            val accuracy = if (t.answered == 0) 0 else (t.correct * 100.0 / t.answered).roundToInt()
            Column(Modifier.fillMaxWidth().pixelPanel(SpireColors.Gold).padding(Spacing.s)) {
                Row { Big(stringResource(R.string.stats_runs), stats.records.runsPlayed.toString(), Modifier.weight(1f)); Big(stringResource(R.string.stats_best_floor), stats.records.bestFloor.toString(), Modifier.weight(1f)) }
                Spacer(Modifier.height(Spacing.s))
                Row { Big(stringResource(R.string.stats_accuracy_label), "$accuracy%", Modifier.weight(1f)); Big(stringResource(R.string.stats_spells), t.correct.toString(), Modifier.weight(1f)) }
            }
            Spacer(Modifier.height(Spacing.m))
            Line(stringResource(R.string.stats_bosses), t.bosses.toString())
            Line(stringResource(R.string.stats_crystals), stats.records.totalCrystals.toString())
            Line(stringResource(R.string.stats_best_streak), t.bestStreak.toString())
            Line(stringResource(R.string.stats_daily_streak), "${stats.dailyStreak}  (${stringResource(R.string.daily_best_streak, stats.bestDailyStreak)})")

            Section(stringResource(R.string.stats_by_school))
            School.entries.forEach { school ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    PixelImage(Sprites.school(school), Modifier.size(22.dp))
                    Spacer(Modifier.width(Spacing.s))
                    Text(stringResource(schoolUi(school).name), style = MaterialTheme.typography.bodyLarge, color = SpireColors.Text, modifier = Modifier.weight(1f))
                    Text(stats.bestBySchool[school]?.toString() ?: "-", style = MaterialTheme.typography.titleLarge, color = school.accent())
                }
            }

            Section(stringResource(R.string.stats_recent))
            if (stats.recent.isEmpty()) {
                Text(stringResource(R.string.stats_none), style = MaterialTheme.typography.bodyLarge, color = SpireColors.TextMuted)
            }
            val dateFormat = remember { DateFormat.getDateInstance(DateFormat.SHORT) }
            stats.recent.forEach { run ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    PixelImage(Sprites.school(run.school), Modifier.size(22.dp))
                    Spacer(Modifier.width(Spacing.s))
                    Text(dateFormat.format(Date(run.endedAtMillis)), style = MaterialTheme.typography.bodyLarge, color = SpireColors.TextMuted, modifier = Modifier.weight(1f))
                    Text(stringResource(R.string.run_floor, run.floor), style = MaterialTheme.typography.titleMedium, color = run.school.accent())
                    Spacer(Modifier.width(Spacing.m))
                    PixelImage(Sprites.crystal, Modifier.size(14.dp))
                    Text(" ${run.crystals}", style = MaterialTheme.typography.titleMedium, color = SpireColors.Crystal)
                }
            }
            if (stats.classicBest > 0) {
                Spacer(Modifier.height(Spacing.l))
                Text(stringResource(R.string.stats_classic, stats.classicBest), style = MaterialTheme.typography.bodyMedium, color = SpireColors.TextMuted)
            }
            Spacer(Modifier.height(Spacing.l))
        }
    }
}

@Composable
private fun Big(label: String, value: String, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.displayMedium, color = SpireColors.Text)
        Text(label.uppercase(), style = MaterialTheme.typography.labelMedium, color = SpireColors.TextMuted)
    }
}

@Composable
private fun Line(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = SpireColors.TextMuted)
        Text(value, style = MaterialTheme.typography.titleLarge, color = SpireColors.Text)
    }
}

@Composable
private fun Section(title: String) {
    Spacer(Modifier.height(Spacing.l))
    Text(title.uppercase(), style = MaterialTheme.typography.titleLarge, color = SpireColors.Gold)
    Spacer(Modifier.height(Spacing.xs))
}
