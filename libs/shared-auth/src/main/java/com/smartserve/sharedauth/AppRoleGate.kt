package com.smartserve.sharedauth

/**
 * Whether [userRole] (Firestore `users.role`) may use an APK built for [expectedAppRole].
 * Accounts with role [UserRole.BOTH] may use either app.
 */
object AppRoleGate {
    fun isAllowed(expectedAppRole: String, userRole: String): Boolean {
        if (userRole == UserRole.BOTH.value) return true
        return when (expectedAppRole) {
            UserRole.CUSTOMER.value -> userRole == UserRole.CUSTOMER.value
            UserRole.PROVIDER.value -> userRole == UserRole.PROVIDER.value
            else -> false
        }
    }
}
