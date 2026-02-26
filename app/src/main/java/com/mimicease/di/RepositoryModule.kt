package com.mimicease.di

import com.mimicease.data.local.dao.ProfileDao
import com.mimicease.data.local.dao.TriggerDao
import com.mimicease.data.repository.ProfileRepositoryImpl
import com.mimicease.data.repository.TriggerRepositoryImpl
import com.mimicease.data.repository.SettingsRepositoryImpl
import com.mimicease.domain.repository.ProfileRepository
import com.mimicease.domain.repository.TriggerRepository
import com.mimicease.domain.repository.SettingsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideProfileRepository(profileDao: ProfileDao): ProfileRepository {
        return ProfileRepositoryImpl(profileDao)
    }

    @Provides
    @Singleton
    fun provideTriggerRepository(triggerDao: TriggerDao): TriggerRepository {
        return TriggerRepositoryImpl(triggerDao)
    }

    @Provides
    @Singleton
    fun provideSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository {
        return impl
    }
}
