/*package com.smartserve.providerapp.navigation

object Routes {
    const val Auth = "auth"
    const val App  = "app"
}*/
package com.smartserve.providerapp.navigation

object Routes {
    const val Bootstrap = "bootstrap"
    const val Auth      = "auth"
    const val App       = "app"
}

object HomeRoutes {
    const val Home          = "home"
    const val RequestDetail = "requestDetail/{bookingId}"
    const val ActiveJob     = "activeJob/{bookingId}"
    const val Chat          = "chat/{bookingId}"

    fun requestDetail(bookingId: String) = "requestDetail/$bookingId"
    fun activeJob(bookingId: String)     = "activeJob/$bookingId"
    fun chat(bookingId: String)          = "chat/$bookingId"
}