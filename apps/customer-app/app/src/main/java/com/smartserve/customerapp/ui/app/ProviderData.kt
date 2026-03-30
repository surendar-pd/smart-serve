package com.smartserve.customerapp.ui.app

data class CartItem(
    val providerUid: String = "",
    val serviceId: String = "",
    val providerName: String,
    val serviceName: String,
    val price: String,
    val date: String = "",
    val time: String = "",
    val timeRange: String = "",
)
