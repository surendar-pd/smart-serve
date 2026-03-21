package com.smartserve.providerapp.di

import com.smartserve.sharedauth.ExpectedAppRole
import com.smartserve.sharedauth.UserRole
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object ProviderAuthModule {
    @Provides
    @ExpectedAppRole
    fun provideExpectedAppRole(): String = UserRole.PROVIDER.value
}
