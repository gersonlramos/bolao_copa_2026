package com.bolao.copa2026.feature.groups

import android.content.Context
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bolao.copa2026.domain.model.Group
import com.bolao.copa2026.domain.repository.AuthRepository
import com.bolao.copa2026.domain.repository.GroupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    val error: String? = null
)

@HiltViewModel
class GroupListViewModel @Inject constructor(
    private val groupRepository: GroupRepository,
    private val authRepository: AuthRepository,
    @ApplicationContext private val context: Context
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
                val currentCode = installedVersionCode()
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

    @Suppress("DEPRECATION")
    private fun installedVersionCode(): Int {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
            info.longVersionCode.toInt()
        else
            info.versionCode
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
