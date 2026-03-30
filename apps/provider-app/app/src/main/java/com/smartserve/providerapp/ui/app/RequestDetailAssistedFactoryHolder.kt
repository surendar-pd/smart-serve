package com.smartserve.providerapp.ui.app

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

// Thin HiltViewModel that simply holds the AssistedFactory so Hilt can inject it
@HiltViewModel
class RequestDetailAssistedFactoryHolder @Inject constructor(
    val factory: RequestDetailViewModel.Factory,
) : ViewModel()