package com.mimicease.di

import com.mimicease.data.local.dao.ProfileDao
import com.mimicease.data.local.dao.TriggerDao
import com.mimicease.data.repository.ProfileRepositoryImpl
import com.mimicease.data.repository.TriggerRepositoryImpl
import com.mimicease.data.repository.SettingsRepositoryImpl
import com.mimicease.data.repository.UpdateRepositoryImpl
import com.mimicease.domain.repository.ProfileRepository
import com.mimicease.domain.repository.TriggerRepository
import com.mimicease.domain.repository.SettingsRepository
import com.mimicease.domain.repository.UpdateRepository
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
    fun provideProfileRepository(
        profileDao: ProfileDao,
        triggerDao: TriggerDao
    ): ProfileRepository {
        return ProfileRepositoryImpl(profileDao, triggerDao)
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

    @Provides
    @Singleton
    fun provideUpdateRepository(impl: UpdateRepositoryImpl): UpdateRepository {
        return impl
    }
}
