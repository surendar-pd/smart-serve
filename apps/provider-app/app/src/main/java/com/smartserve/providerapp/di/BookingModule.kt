package com.smartserve.providerapp.di

import com.google.firebase.firestore.FirebaseFirestore
import com.smartserve.providerapp.ui.app.BookingRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// NOTE: FirebaseFirestore is already @Provides'd by shared-auth AuthModule.
// We only bind BookingRepository here — no duplicate bindings.
@Module
@InstallIn(SingletonComponent::class)
object BookingModule {

    @Provides
    @Singleton
    fun provideBookingRepository(
        firestore: FirebaseFirestore,
    ): BookingRepository = BookingRepository(firestore)
}