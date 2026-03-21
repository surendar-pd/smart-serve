package com.smartserve.sharedauth

/**
 * Whether [userActiveRole] (Firestore `activeRole`) may use an APK built for [expectedAppRole].
 * Accounts with role [UserRole.BOTH] may use either app.
 */
object AppRoleGate {
    fun isAllowed(expectedAppRole: String, userActiveRole: String): Boolean {
        if (userActiveRole == UserRole.BOTH.value) return true
        return when (expectedAppRole) {
            UserRole.CUSTOMER.value -> userActiveRole == UserRole.CUSTOMER.value
            UserRole.PROVIDER.value -> userActiveRole == UserRole.PROVIDER.value
            else -> false
        }
    }
}
