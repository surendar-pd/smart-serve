package com.smartserve.providerapp.ui.app

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ActiveJobAssistedFactoryHolder @Inject constructor(
    val factory: ActiveJobViewModel.Factory,
) : ViewModel()