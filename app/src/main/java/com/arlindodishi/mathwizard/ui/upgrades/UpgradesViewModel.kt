package com.arlindodishi.mathwizard.ui.upgrades

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arlindodishi.mathwizard.data.MetaRepository
import com.arlindodishi.mathwizard.engine.MetaProfile
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class UpgradesViewModel(private val meta: MetaRepository) : ViewModel() {

    val profile: StateFlow<MetaProfile?> =
        meta.profile.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun buy(upgradeId: String) {
        viewModelScope.launch { meta.buy(upgradeId) }
    }
}
