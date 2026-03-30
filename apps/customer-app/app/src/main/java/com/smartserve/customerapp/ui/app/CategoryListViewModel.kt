package com.smartserve.customerapp.ui.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ProviderSortOrder { Rating, PriceLow, PriceHigh }

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

    private val _sortOrder = MutableStateFlow(ProviderSortOrder.Rating)
    val sortOrder: StateFlow<ProviderSortOrder> = _sortOrder

    // Raw unsorted list kept here so re-sorting doesn't need a re-fetch
    private var rawProviders: List<CustomerProviderSummary> = emptyList()

    fun load(categoryId: String) {
        _state.value = CategoryListUiState.Loading
        viewModelScope.launch {
            try {
                rawProviders = repo.getProvidersByCategory(categoryId)
                applySort()
            } catch (e: Exception) {
                _state.value = CategoryListUiState.Error(
                    e.localizedMessage ?: "Could not load providers"
                )
            }
        }
    }

    fun setSortOrder(order: ProviderSortOrder) {
        _sortOrder.value = order
        if (rawProviders.isNotEmpty()) applySort()
    }

    private fun applySort() {
        val sorted = when (_sortOrder.value) {
            ProviderSortOrder.Rating    -> rawProviders.sortedByDescending { it.avgRating }
            ProviderSortOrder.PriceLow  -> rawProviders.sortedBy { it.categoryServiceRate }
            ProviderSortOrder.PriceHigh -> rawProviders.sortedByDescending { it.categoryServiceRate }
        }
        _state.value = CategoryListUiState.Success(sorted)
    }
}
