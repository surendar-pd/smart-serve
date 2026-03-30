package com.smartserve.providerapp.ui.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MyServicesViewModel @Inject constructor(
    private val repository: ProviderServicesRepository,
    private val auth: FirebaseAuth,
) : ViewModel() {

    private val _services = MutableStateFlow<List<ProviderServiceRow>>(emptyList())
    val services: StateFlow<List<ProviderServiceRow>> = _services.asStateFlow()

    init {
        viewModelScope.launch {
            val uid = auth.currentUser?.uid ?: return@launch
            repository.observeServicesForProvider(uid).collect { _services.value = it }
        }
    }
}
