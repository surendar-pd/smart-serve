package com.smartserve.customerapp.ui.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.smartserve.sharedauth.AuthCollections
import com.smartserve.sharedauth.ServiceCategoryOption
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class HomeUiState(
    val categories: List<ServiceCategoryOption> = emptyList(),
    val topProviders: List<CustomerProviderSummary> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repo: CustomerServicesRepository,
    private val firestore: FirebaseFirestore,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = HomeUiState(isLoading = true)

            val categoriesDeferred = launch {
                val snap = firestore.collection(AuthCollections.CATEGORIES).get().await()
                val cats = snap.documents.mapNotNull { doc ->
                    val label = doc.getString("label") ?: return@mapNotNull null
                    ServiceCategoryOption(id = doc.id, label = label)
                }
                _state.value = _state.value.copy(categories = cats)
            }

            val providersDeferred = launch {
                val providers = repo.getTopProviders(limit = 5)
                _state.value = _state.value.copy(topProviders = providers)
            }

            categoriesDeferred.join()
            providersDeferred.join()
            _state.value = _state.value.copy(isLoading = false)
        }
    }
}
