package com.smartserve.providerapp.ui.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

// Follows exact same pattern as RequestDetailAssistedFactoryHolder
@HiltViewModel
class ProviderNavigationAssistedFactoryHolder @Inject constructor(
    val factory: ProviderNavigationViewModel.Factory,
) : ViewModel()

// Follows exact same pattern as provideRequestDetailViewModel
fun provideNavigationViewModel(
    factory: ProviderNavigationViewModel.Factory,
    customerLat: Double,
    customerLng: Double,
    customerAddress: String,
): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return factory.create(customerLat, customerLng, customerAddress) as T
    }
}