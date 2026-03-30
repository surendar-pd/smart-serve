package com.smartserve.providerapp.ui.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

fun provideActiveJobViewModel(
    factory: ActiveJobViewModel.Factory,
    bookingId: String,
): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return factory.create(bookingId) as T
    }
}