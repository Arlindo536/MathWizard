package com.arlindodishi.mathwizard.ui.upgrades

import androidx.annotation.StringRes
import androidx.compose.foundation.background
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.arlindodishi.mathwizard.Sfx
import com.arlindodishi.mathwizard.appContainer
import com.arlindodishi.mathwizard.engine.Branch
import com.arlindodishi.mathwizard.engine.MetaProfile
import com.arlindodishi.mathwizard.engine.School
import com.arlindodishi.mathwizard.engine.Upgrade
import com.arlindodishi.mathwizard.engine.Upgrades
import com.arlindodishi.mathwizard.ui.pixel.IconValue
import com.arlindodishi.mathwizard.ui.pixel.PixelButton
import com.arlindodishi.mathwizard.ui.pixel.PixelImage
import com.arlindodishi.mathwizard.ui.pixel.PixelSprite
import com.arlindodishi.mathwizard.ui.pixel.Sprites
import com.arlindodishi.mathwizard.ui.pixel.pixelPanel
import com.arlindodishi.mathwizard.ui.schools.schoolUi
import com.arlindodishi.mathwizard.ui.theme.SpireColors
import com.arlindodishi.mathwizard.ui.theme.Spacing

@Composable
fun UpgradesRoute(onBack: () -> Unit) {
    val context = LocalContext.current.applicationContext
    val feedback = context.appContainer.feedback
    val viewModel: UpgradesViewModel = viewModel(
        factory = viewModelFactory { initializer { UpgradesViewModel(context.appContainer.meta) } },
    )
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val current = profile ?: MetaProfile()
    UpgradesScreen(
        profile = current,
        onBuy = { id ->
            if (current.canBuy(id)) feedback.play(Sfx.BUY)
            viewModel.buy(id)
        },
        onBack = onBack,
    )
}

@Composable
fun UpgradesScreen(profile: MetaProfile, onBuy: (String) -> Unit, onBack: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(SpireColors.Night)
            .safeDrawingPadding(),
    ) {
        ScreenHeader(title = stringResource(R.string.upgrades_title), onBack = onBack) {
            IconValue(Sprites.crystal, profile.crystals.toString(), SpireColors.Crystal)
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

/** Back button, title and an optional trailing slot, shared by the secondary screens. */
@Composable
fun ScreenHeader(title: String, onBack: () -> Unit, trailing: @Composable () -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = Spacing.m, vertical = Spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val back = stringResource(R.string.upgrades_back)
        PixelButton(
            onClick = onBack,
            color = SpireColors.SurfaceRaised,
            contentColor = SpireColors.Text,
            modifier = Modifier.size(52.dp).semantics { contentDescription = back },
        ) {
            Text("<", style = MaterialTheme.typography.titleLarge)
        }
        Spacer(Modifier.width(Spacing.m))
        Text(title.uppercase(), style = MaterialTheme.typography.headlineMedium, color = SpireColors.Text, modifier = Modifier.weight(1f))
        trailing()
    }
}

@Composable
private fun BranchHeader(branch: Branch) {
    val ui = branchUi(branch)
    Row(Modifier.padding(top = Spacing.m, bottom = Spacing.xs), verticalAlignment = Alignment.CenterVertically) {
        PixelImage(ui.icon, Modifier.size(28.dp))
        Spacer(Modifier.width(Spacing.s))
        Column {
            Text(stringResource(ui.name).uppercase(), style = MaterialTheme.typography.headlineMedium, color = SpireColors.Gold)
            Text(stringResource(ui.description), style = MaterialTheme.typography.bodyMedium, color = SpireColors.TextMuted)
        }
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
            .pixelPanel(if (unlocked) SpireColors.Track else SpireColors.Ink)
            .padding(Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PixelImage(if (unlocked) ui.icon else Sprites.lock, Modifier.size(40.dp))
        Spacer(Modifier.width(Spacing.m))
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
                stringResource(if (upgrade.maxLevel == 1) R.string.upgrade_owned else R.string.upgrade_max).uppercase(),
                style = MaterialTheme.typography.titleLarge,
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
                PixelButton(
                    onClick = { onBuy(upgrade.id) },
                    enabled = profile.canBuy(upgrade.id),
                    color = SpireColors.Crystal,
                    modifier = Modifier.semantics { contentDescription = description },
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PixelImage(Sprites.crystal, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("$cost", style = MaterialTheme.typography.titleLarge)
                    }
                }
            }
        }
    }
}

@Composable
private fun LevelPips(level: Int, max: Int) {
    val label = stringResource(R.string.upgrade_level, level, max)
    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        modifier = Modifier.semantics { contentDescription = label },
    ) {
        repeat(max) { i ->
            Box(
                Modifier
                    .size(12.dp)
                    .background(SpireColors.Ink)
                    .padding(2.dp)
                    .background(if (i < level) SpireColors.Crystal else SpireColors.Track),
            )
        }
    }
}

private data class LabelUi(val icon: PixelSprite, @param:StringRes val name: Int, @param:StringRes val description: Int)

private fun branchUi(branch: Branch): LabelUi = when (branch) {
    Branch.SCHOOLS -> LabelUi(Sprites.branchSchools, R.string.branch_schools, R.string.branch_schools_desc)
    Branch.VITALITY -> LabelUi(Sprites.branchVitality, R.string.branch_vitality, R.string.branch_vitality_desc)
    Branch.FOCUS -> LabelUi(Sprites.branchFocus, R.string.branch_focus, R.string.branch_focus_desc)
    Branch.ARCANA -> LabelUi(Sprites.branchArcana, R.string.branch_arcana, R.string.branch_arcana_desc)
}

private fun upgradeUi(id: String): LabelUi {
    val icon = Sprites.upgrade(id)
    return when (id) {
        "vitality" -> LabelUi(icon, R.string.upgrade_vitality, R.string.upgrade_vitality_desc)
        "second_wind" -> LabelUi(icon, R.string.upgrade_second_wind, R.string.upgrade_second_wind_desc)
        "focus" -> LabelUi(icon, R.string.upgrade_focus, R.string.upgrade_focus_desc)
        "hint" -> LabelUi(icon, R.string.upgrade_hint, R.string.upgrade_hint_desc)
        "power" -> LabelUi(icon, R.string.upgrade_power, R.string.upgrade_power_desc)
        "relic_insight" -> LabelUi(icon, R.string.upgrade_relic_insight, R.string.upgrade_relic_insight_desc)
        "starting_relic" -> LabelUi(icon, R.string.upgrade_starting_relic, R.string.upgrade_starting_relic_desc)
        else -> School.entries.find { Upgrades.unlockFor(it) == id }
            ?.let { LabelUi(icon, schoolUi(it).name, R.string.upgrade_school_desc) }
            ?: error("No UI for upgrade $id")
    }
}
