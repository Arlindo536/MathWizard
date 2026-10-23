package com.arlindodishi.mathwizard.ui.schools

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.arlindodishi.mathwizard.R
import com.arlindodishi.mathwizard.engine.MetaProfile
import com.arlindodishi.mathwizard.engine.School
import com.arlindodishi.mathwizard.ui.theme.SpireColors
import com.arlindodishi.mathwizard.ui.theme.Spacing
import com.arlindodishi.mathwizard.ui.theme.accent

@Composable
fun SchoolSelectScreen(
    profile: MetaProfile,
    bestBySchool: Map<School, Int>,
    onPick: (School) -> Unit,
    onLocked: () -> Unit,
    onBack: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(SpireColors.Night, SpireColors.NightRaised)))
            .safeDrawingPadding(),
    ) {
        Row(Modifier.padding(Spacing.s), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) {
                Text("← " + stringResource(R.string.upgrades_back), color = SpireColors.TextMuted)
            }
            Text(stringResource(R.string.schools_title), style = MaterialTheme.typography.headlineMedium, color = SpireColors.Text)
        }
        LazyColumn(
            contentPadding = PaddingValues(horizontal = Spacing.m, vertical = Spacing.s),
            verticalArrangement = Arrangement.spacedBy(Spacing.s),
        ) {
            items(School.entries, key = { it.name }) { school ->
                SchoolCard(
                    school = school,
                    unlocked = profile.isUnlocked(school),
                    best = bestBySchool[school],
                    onClick = { if (profile.isUnlocked(school)) onPick(school) else onLocked() },
                )
            }
        }
    }
}

@Composable
private fun SchoolCard(school: School, unlocked: Boolean, best: Int?, onClick: () -> Unit) {
    val ui = schoolUi(school)
    val accent = school.accent()
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.horizontalGradient(listOf(accent.copy(alpha = 0.22f), SpireColors.Surface)))
            .border(1.dp, accent.copy(alpha = if (unlocked) 0.7f else 0.2f), RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(Spacing.m)
            .alpha(if (unlocked) 1f else 0.55f),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(if (unlocked) ui.emoji else "🔒", style = MaterialTheme.typography.displayMedium, modifier = Modifier.padding(end = Spacing.m))
        Column(Modifier.weight(1f)) {
            Text(stringResource(ui.name), style = MaterialTheme.typography.titleLarge, color = SpireColors.Text)
            Text(stringResource(ui.description), style = MaterialTheme.typography.bodyMedium, color = SpireColors.TextMuted)
            Text(
                when {
                    !unlocked -> stringResource(R.string.school_locked)
                    best != null -> stringResource(R.string.school_best, best)
                    else -> stringResource(R.string.school_new)
                },
                style = MaterialTheme.typography.labelMedium,
                color = if (unlocked) accent else SpireColors.TextMuted,
                modifier = Modifier.padding(top = Spacing.xs),
            )
        }
    }
}
