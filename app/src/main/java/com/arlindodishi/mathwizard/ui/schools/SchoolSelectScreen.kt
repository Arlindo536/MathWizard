package com.arlindodishi.mathwizard.ui.schools

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.arlindodishi.mathwizard.R
import com.arlindodishi.mathwizard.engine.MetaProfile
import com.arlindodishi.mathwizard.engine.School
import com.arlindodishi.mathwizard.ui.pixel.PixelImage
import com.arlindodishi.mathwizard.ui.pixel.Sprites
import com.arlindodishi.mathwizard.ui.pixel.pixelPanel
import com.arlindodishi.mathwizard.ui.theme.SpireColors
import com.arlindodishi.mathwizard.ui.theme.Spacing
import com.arlindodishi.mathwizard.ui.theme.accent
import com.arlindodishi.mathwizard.ui.upgrades.ScreenHeader

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
            .background(SpireColors.Night)
            .safeDrawingPadding(),
    ) {
        ScreenHeader(title = stringResource(R.string.schools_title), onBack = onBack)
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
            .clickable(role = Role.Button, onClick = onClick)
            .pixelPanel(if (unlocked) accent else SpireColors.Track)
            .padding(Spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PixelImage(if (unlocked) Sprites.school(school) else Sprites.lock, Modifier.size(44.dp))
        Spacer(Modifier.width(Spacing.m))
        Column(Modifier.weight(1f).alpha(if (unlocked) 1f else 0.6f)) {
            Text(stringResource(ui.name).uppercase(), style = MaterialTheme.typography.titleLarge, color = SpireColors.Text)
            Text(stringResource(ui.description), style = MaterialTheme.typography.bodyMedium, color = SpireColors.TextMuted)
            Text(
                when {
                    !unlocked -> stringResource(R.string.school_locked)
                    best != null -> stringResource(R.string.school_best, best)
                    else -> stringResource(R.string.school_new)
                },
                style = MaterialTheme.typography.labelLarge,
                color = if (unlocked) accent else SpireColors.TextMuted,
                modifier = Modifier.padding(top = Spacing.xs),
            )
        }
    }
}
