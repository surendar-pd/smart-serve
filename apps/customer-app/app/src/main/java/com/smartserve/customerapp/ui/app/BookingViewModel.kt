package com.smartserve.customerapp.ui.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BookingViewModel @Inject constructor(
    private val repo: CustomerServicesRepository,
) : ViewModel() {

    private val _homeAddress = MutableStateFlow("")
    val homeAddress: StateFlow<String> = _homeAddress

    init {
        viewModelScope.launch {
            _homeAddress.value = repo.getCustomerHomeAddress()
        }
    }
}
