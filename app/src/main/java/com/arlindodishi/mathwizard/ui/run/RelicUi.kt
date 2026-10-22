package com.arlindodishi.mathwizard.ui.run

import androidx.annotation.StringRes
import com.arlindodishi.mathwizard.R

/** Placeholder icon and localised texts for a relic id; art comes in the visual pass (step 7). */
data class RelicUi(val emoji: String, @param:StringRes val name: Int, @param:StringRes val description: Int)

fun relicUi(id: String): RelicUi = when (id) {
    "ember_focus" -> RelicUi("🔥", R.string.relic_ember_focus, R.string.relic_ember_focus_desc)
    "quicksilver_quill" -> RelicUi("🪶", R.string.relic_quicksilver_quill, R.string.relic_quicksilver_quill_desc)
    "heartstone" -> RelicUi("💗", R.string.relic_heartstone, R.string.relic_heartstone_desc)
    "healing_draught" -> RelicUi("🧪", R.string.relic_healing_draught, R.string.relic_healing_draught_desc)
    "iron_ward" -> RelicUi("🛡️", R.string.relic_iron_ward, R.string.relic_iron_ward_desc)
    "swift_sigil" -> RelicUi("⚡", R.string.relic_swift_sigil, R.string.relic_swift_sigil_desc)
    "combo_crown" -> RelicUi("👑", R.string.relic_combo_crown, R.string.relic_combo_crown_desc)
    "mana_shield" -> RelicUi("🔮", R.string.relic_mana_shield, R.string.relic_mana_shield_desc)
    "vampiric_rune" -> RelicUi("🩸", R.string.relic_vampiric_rune, R.string.relic_vampiric_rune_desc)
    "oracle_eye" -> RelicUi("👁️", R.string.relic_oracle_eye, R.string.relic_oracle_eye_desc)
    "phoenix_feather" -> RelicUi("🕊️", R.string.relic_phoenix_feather, R.string.relic_phoenix_feather_desc)
    "giant_slayer" -> RelicUi("🗡️", R.string.relic_giant_slayer, R.string.relic_giant_slayer_desc)
    else -> error("No UI for relic $id")
}
