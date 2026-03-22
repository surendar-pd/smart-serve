package com.smartserve.customerapp.di

import com.smartserve.sharedauth.ExpectedAppRole
import com.smartserve.sharedauth.UserRole
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object CustomerAuthModule {
    @Provides
    @ExpectedAppRole
    fun provideExpectedAppRole(): String = UserRole.CUSTOMER.value
}
