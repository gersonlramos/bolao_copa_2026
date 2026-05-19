package com.bolao.copa2026.feature.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bolao.copa2026.domain.model.Group
import com.bolao.copa2026.domain.repository.GroupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class GroupListUiState(
    val groups: List<Group> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class GroupListViewModel @Inject constructor(
    private val groupRepository: GroupRepository
) : ViewModel() {

    val uiState: StateFlow<GroupListUiState> = groupRepository.observeUserGroups()
        .map { groups -> GroupListUiState(groups = groups, isLoading = false) }
        .catch { e -> emit(GroupListUiState(isLoading = false, error = e.message)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GroupListUiState())
}
