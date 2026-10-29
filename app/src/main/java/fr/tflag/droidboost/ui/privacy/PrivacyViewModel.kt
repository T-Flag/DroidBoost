package fr.tflag.droidboost.ui.privacy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.tflag.droidboost.AppContainer
import fr.tflag.droidboost.core.privacy.SecurityCheck
import fr.tflag.droidboost.privacy.PrivacyInspector
import fr.tflag.droidboost.privacy.PrivacyReport
import fr.tflag.droidboost.shell.ShizukuState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PrivacyState(
    val loading: Boolean = true,
    val includeSystem: Boolean = false,
    val report: PrivacyReport? = null,
    val security: List<SecurityCheck> = emptyList(),
    val shizuku: ShizukuState = ShizukuState.Unavailable,
)

/** Shared by the privacy and security screens: both come from the same read-only audit. */
class PrivacyViewModel(private val container: AppContainer) : ViewModel() {

    private val inspector = PrivacyInspector(container.appContext, container.shell)
    private val content = MutableStateFlow(PrivacyState())

    val state: StateFlow<PrivacyState> = combine(content, container.shell.state) { privacy, shizuku -> privacy.copy(shizuku = shizuku) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PrivacyState())

    init {
        refresh()
    }

    fun setIncludeSystem(value: Boolean) {
        content.value = content.value.copy(includeSystem = value)
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            content.value = content.value.copy(loading = true)
            val report = inspector.privacy(content.value.includeSystem)
            val security = inspector.security()
            content.value = content.value.copy(loading = false, report = report, security = security)
        }
    }
}
