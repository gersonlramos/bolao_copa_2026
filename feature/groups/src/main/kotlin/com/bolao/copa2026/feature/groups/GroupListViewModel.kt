package com.bolao.copa2026.feature.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bolao.copa2026.domain.model.Group
import com.bolao.copa2026.domain.repository.AuthRepository
import com.bolao.copa2026.domain.repository.GroupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UpdateInfo(
    val latestVersionCode: Int,
    val downloadUrl: String,
    val releaseNotes: String
)

data class GroupListUiState(
    val groups: List<Group> = emptyList(),
    val isVip: Boolean = false,
    val isLoading: Boolean = true,
    val showPaywall: Boolean = false,
    val updateInfo: UpdateInfo? = null,
    val error: String? = null
)

@HiltViewModel
class GroupListViewModel @Inject constructor(
    private val groupRepository: GroupRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    val uiState: StateFlow<GroupListUiState> = combine(
        groupRepository.observeUserGroups(),
        authRepository.currentUser()
    ) { groups, user ->
        GroupListUiState(
            groups = groups,
            isVip = user?.isVip ?: false,
            isLoading = false
        )
    }
        .catch { e -> emit(GroupListUiState(isLoading = false, error = e.message)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GroupListUiState())

    private val _updateInfo = MutableStateFlow<UpdateInfo?>(null)
    val updateInfo: StateFlow<UpdateInfo?> = _updateInfo.asStateFlow()

    init {
        checkForUpdate()
    }

    private fun checkForUpdate() {
        viewModelScope.launch {
            runCatching {
                val latestCode = authRepository.getLatestVersionCode() ?: return@launch
                val currentCode = getCurrentVersionCode()
                if (latestCode > currentCode) {
                    _updateInfo.value = UpdateInfo(
                        latestVersionCode = latestCode,
                        downloadUrl = "https://github.com/gersonlramos/bolao_copa_2026/releases/latest/download/bolao-copa-2026.apk",
                        releaseNotes = "Nova versão disponível! Atualize para ter as últimas melhorias."
                    )
                }
            }
        }
    }

    fun dismissUpdate() { _updateInfo.value = null }

    fun onCreateGroupClicked(navigate: () -> Unit) = checkLimit(navigate)
    fun onJoinGroupClicked(navigate: () -> Unit) = checkLimit(navigate)

    private fun checkLimit(navigate: () -> Unit) {
        val s = uiState.value
        if (s.isVip || s.groups.isEmpty()) navigate()
        else _showPaywall.value = true
    }

    private val _showPaywall = MutableStateFlow(false)
    fun dismissPaywall() { _showPaywall.value = false }
    val paywallVisible: StateFlow<Boolean> = _showPaywall.asStateFlow()
}

// Expected to be replaced at compile time via BuildConfig
private fun getCurrentVersionCode(): Int =
    runCatching {
        Class.forName("com.bolao.copa2026.BuildConfig")
            .getField("VERSION_CODE").getInt(null)
    }.getOrDefault(1)
