package com.smartserve.customerapp.ui.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repo: CustomerServicesRepository,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    private val _allServices = MutableStateFlow<List<CustomerServiceListing>>(emptyList())
    val isLoading = MutableStateFlow(false)

    val results: StateFlow<List<CustomerServiceListing>> = combine(_query, _allServices) { q, all ->
        if (q.isBlank()) emptyList()
        else all.filter { svc ->
            svc.title.contains(q, ignoreCase = true) ||
                svc.description.contains(q, ignoreCase = true) ||
                svc.providerName.contains(q, ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setQuery(q: String) {
        _query.value = q
    }

    fun loadAll() {
        if (_allServices.value.isNotEmpty()) return
        viewModelScope.launch {
            isLoading.value = true
            _allServices.value = repo.getAllActiveServices()
            isLoading.value = false
        }
    }
}
