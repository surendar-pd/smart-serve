package com.smartserve.sharedauth

import kotlin.annotation.AnnotationRetention
import kotlin.annotation.Retention
import javax.inject.Qualifier

/** Binds the role this APK expects (`customer` or `provider`) for [AuthViewModel] and session bootstrap. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ExpectedAppRole
