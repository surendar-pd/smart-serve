package com.smartserve.customerapp.ui.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class CategoryListUiState {
    data object Loading : CategoryListUiState()
    data class Success(val providers: List<CustomerProviderSummary>) : CategoryListUiState()
    data class Error(val message: String) : CategoryListUiState()
}

@HiltViewModel
class CategoryListViewModel @Inject constructor(
    private val repo: CustomerServicesRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<CategoryListUiState>(CategoryListUiState.Loading)
    val state: StateFlow<CategoryListUiState> = _state

    fun load(categoryId: String) {
        _state.value = CategoryListUiState.Loading
        viewModelScope.launch {
            val providers = repo.getProvidersByCategory(categoryId)
            _state.value = if (providers.isEmpty()) {
                CategoryListUiState.Success(emptyList())
            } else {
                CategoryListUiState.Success(providers)
            }
        }
    }
}
