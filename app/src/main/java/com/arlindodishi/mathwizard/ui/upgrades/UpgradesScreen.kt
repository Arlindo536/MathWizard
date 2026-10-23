package com.arlindodishi.mathwizard.ui.upgrades

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.arlindodishi.mathwizard.R
import com.arlindodishi.mathwizard.appContainer
import com.arlindodishi.mathwizard.engine.Branch
import com.arlindodishi.mathwizard.engine.MetaProfile
import com.arlindodishi.mathwizard.engine.School
import com.arlindodishi.mathwizard.engine.Upgrade
import com.arlindodishi.mathwizard.engine.Upgrades
import com.arlindodishi.mathwizard.ui.schools.schoolUi
import com.arlindodishi.mathwizard.ui.theme.SpireColors
import com.arlindodishi.mathwizard.ui.theme.Spacing

@Composable
fun UpgradesRoute(onBack: () -> Unit) {
    val context = LocalContext.current.applicationContext
    val viewModel: UpgradesViewModel = viewModel(
        factory = viewModelFactory { initializer { UpgradesViewModel(context.appContainer.meta) } },
    )
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    UpgradesScreen(profile = profile ?: MetaProfile(), onBuy = viewModel::buy, onBack = onBack)
}

@Composable
fun UpgradesScreen(profile: MetaProfile, onBuy: (String) -> Unit, onBack: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(SpireColors.Night, SpireColors.NightRaised)))
            .safeDrawingPadding(),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = Spacing.s, vertical = Spacing.s),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onBack) {
                Text("← " + stringResource(R.string.upgrades_back), color = SpireColors.TextMuted)
            }
            Text(
                stringResource(R.string.upgrades_title),
                style = MaterialTheme.typography.headlineMedium,
                color = SpireColors.Text,
                modifier = Modifier.weight(1f),
            )
            CrystalBadge(profile.crystals)
        }
        LazyColumn(
            contentPadding = PaddingValues(horizontal = Spacing.m, vertical = Spacing.s),
            verticalArrangement = Arrangement.spacedBy(Spacing.s),
        ) {
            Branch.entries.forEach { branch ->
                item(key = branch.name) { BranchHeader(branch) }
                items(Upgrades.all.filter { it.branch == branch }, key = { it.id }) { upgrade ->
                    UpgradeCard(upgrade, profile, onBuy)
                }
            }
        }
    }
}

@Composable
fun CrystalBadge(crystals: Int, modifier: Modifier = Modifier) {
    Text(
        "💎 $crystals",
        style = MaterialTheme.typography.titleLarge,
        color = SpireColors.Crystal,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(SpireColors.Surface)
            .padding(horizontal = Spacing.m, vertical = Spacing.xs),
    )
}

@Composable
private fun BranchHeader(branch: Branch) {
    val ui = branchUi(branch)
    Column(Modifier.padding(top = Spacing.m, bottom = Spacing.xs)) {
        Text(
            "${ui.emoji}  " + stringResource(ui.name),
            style = MaterialTheme.typography.headlineMedium,
            color = SpireColors.Gold,
        )
        Text(stringResource(ui.description), style = MaterialTheme.typography.bodyMedium, color = SpireColors.TextMuted)
    }
}

@Composable
private fun UpgradeCard(upgrade: Upgrade, profile: MetaProfile, onBuy: (String) -> Unit) {
    val ui = upgradeUi(upgrade.id)
    val level = profile.level(upgrade.id)
    val unlocked = profile.prerequisiteMet(upgrade.id)
    val cost = profile.nextCost(upgrade.id)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SpireColors.Surface)
            .border(1.dp, if (unlocked) SpireColors.Outline else SpireColors.Outline.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .padding(Spacing.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(ui.emoji, style = MaterialTheme.typography.displayMedium, modifier = Modifier.padding(end = Spacing.m))
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(ui.name),
                style = MaterialTheme.typography.titleLarge,
                color = if (unlocked) SpireColors.Text else SpireColors.TextMuted,
            )
            Text(stringResource(ui.description), style = MaterialTheme.typography.bodyMedium, color = SpireColors.TextMuted)
            Spacer(Modifier.height(Spacing.s))
            LevelPips(level, upgrade.maxLevel)
        }
        Spacer(Modifier.width(Spacing.s))
        when {
            cost == null -> Text(
                stringResource(R.string.upgrade_max),
                style = MaterialTheme.typography.titleMedium,
                color = SpireColors.Gold,
            )
            !unlocked -> {
                val required = upgrade.requires!!
                Text(
                    stringResource(R.string.upgrade_requires, stringResource(upgradeUi(required).name), upgrade.requiresLevel),
                    style = MaterialTheme.typography.labelMedium,
                    color = SpireColors.TextMuted,
                    modifier = Modifier.width(88.dp),
                )
            }
            else -> {
                val description = stringResource(R.string.upgrade_buy_cd, stringResource(ui.name), cost)
                Button(
                    onClick = { onBuy(upgrade.id) },
                    enabled = profile.canBuy(upgrade.id),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SpireColors.Crystal, contentColor = SpireColors.Night),
                    modifier = Modifier.semantics { contentDescription = description },
                ) {
                    Text("💎 $cost", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
private fun LevelPips(level: Int, max: Int) {
    val label = stringResource(R.string.upgrade_level, level, max)
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        modifier = Modifier.semantics { contentDescription = label },
    ) {
        repeat(max) { i ->
            Box(
                Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (i < level) SpireColors.Crystal else SpireColors.Outline),
            )
        }
    }
}

private data class LabelUi(val emoji: String, @param:StringRes val name: Int, @param:StringRes val description: Int)

private fun branchUi(branch: Branch): LabelUi = when (branch) {
    Branch.SCHOOLS -> LabelUi("📚", R.string.branch_schools, R.string.branch_schools_desc)
    Branch.VITALITY -> LabelUi("❤️", R.string.branch_vitality, R.string.branch_vitality_desc)
    Branch.FOCUS -> LabelUi("⏳", R.string.branch_focus, R.string.branch_focus_desc)
    Branch.ARCANA -> LabelUi("✨", R.string.branch_arcana, R.string.branch_arcana_desc)
}

private fun upgradeUi(id: String): LabelUi = when (id) {
    "vitality" -> LabelUi("💪", R.string.upgrade_vitality, R.string.upgrade_vitality_desc)
    "second_wind" -> LabelUi("🌬️", R.string.upgrade_second_wind, R.string.upgrade_second_wind_desc)
    "focus" -> LabelUi("🧘", R.string.upgrade_focus, R.string.upgrade_focus_desc)
    "hint" -> LabelUi("💡", R.string.upgrade_hint, R.string.upgrade_hint_desc)
    "power" -> LabelUi("🔥", R.string.upgrade_power, R.string.upgrade_power_desc)
    "relic_insight" -> LabelUi("🔍", R.string.upgrade_relic_insight, R.string.upgrade_relic_insight_desc)
    "starting_relic" -> LabelUi("🎁", R.string.upgrade_starting_relic, R.string.upgrade_starting_relic_desc)
    else -> School.entries.find { Upgrades.unlockFor(it) == id }
        ?.let { schoolUi(it) }
        ?.let { LabelUi(it.emoji, it.name, R.string.upgrade_school_desc) }
        ?: error("No UI for upgrade $id")
}
