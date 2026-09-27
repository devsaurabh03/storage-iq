package com.storage.intelligence.di

import com.storage.intelligence.core.billing.BillingManager
import com.storage.intelligence.core.billing.BillingManagerImpl
import com.storage.intelligence.data.repository.DuplicateRepositoryImpl
import com.storage.intelligence.data.repository.StorageRepositoryImpl
import com.storage.intelligence.data.repository.SubscriptionRepositoryImpl
import com.storage.intelligence.data.repository.TimelineRepositoryImpl
import com.storage.intelligence.domain.repository.DuplicateRepository
import com.storage.intelligence.domain.repository.StorageRepository
import com.storage.intelligence.domain.repository.SubscriptionRepository
import com.storage.intelligence.domain.repository.TimelineRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindStorageRepository(impl: StorageRepositoryImpl): StorageRepository

    @Binds
    @Singleton
    abstract fun bindDuplicateRepository(impl: DuplicateRepositoryImpl): DuplicateRepository

    @Binds
    @Singleton
    abstract fun bindTimelineRepository(impl: TimelineRepositoryImpl): TimelineRepository

    @Binds
    @Singleton
    abstract fun bindSubscriptionRepository(impl: SubscriptionRepositoryImpl): SubscriptionRepository

    @Binds
    @Singleton
    abstract fun bindBillingManager(impl: BillingManagerImpl): BillingManager
}
