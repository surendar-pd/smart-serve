package com.smartserve.providerapp.di

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.smartserve.providerapp.ui.app.BookingRepository
import com.smartserve.providerapp.ui.app.ProviderServicesRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
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

    @Provides
    @Singleton
    fun provideProviderServicesRepository(
        firestore: FirebaseFirestore,
        storage: FirebaseStorage,
        @ApplicationContext context: Context,
    ): ProviderServicesRepository = ProviderServicesRepository(firestore, storage, context)

    @Provides
    @Singleton
    fun provideFirebaseStorage(): FirebaseStorage {
        val bucket = FirebaseApp.getInstance().options.storageBucket
        if (bucket.isNullOrBlank()) return FirebaseStorage.getInstance()
        val uri = if (bucket.startsWith("gs://")) bucket else "gs://$bucket"
        return FirebaseStorage.getInstance(uri)
    }
}