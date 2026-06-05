package com.bolao.copa2026.feature.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bolao.copa2026.domain.model.Group
import com.bolao.copa2026.domain.repository.AuthRepository
import com.bolao.copa2026.domain.repository.GroupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

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

    fun onCreateGroupClicked(navigate: () -> Unit) = checkLimit(navigate)
    fun onJoinGroupClicked(navigate: () -> Unit) = checkLimit(navigate)

    private fun checkLimit(navigate: () -> Unit) {
        val s = uiState.value
        if (s.isVip || s.groups.isEmpty()) navigate()
        else _showPaywall.value = true
    }

    private val _showPaywall = MutableStateFlow(false)

    fun dismissPaywall() { _showPaywall.value = false }

    // Merge paywall flag into the main uiState so the screen has one source of truth
    val paywallVisible: StateFlow<Boolean> = _showPaywall.asStateFlow()
}
