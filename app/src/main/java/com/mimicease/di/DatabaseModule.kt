package com.mimicease.di

import android.content.Context
import androidx.room.Room
import com.mimicease.data.local.dao.ProfileDao
import com.mimicease.data.local.dao.TriggerDao
import com.mimicease.data.local.database.MimicDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MimicDatabase {
        return Room.databaseBuilder(
            context,
            MimicDatabase::class.java,
            "mimic_database"
        ).build()
    }

    @Provides
    fun provideProfileDao(database: MimicDatabase): ProfileDao {
        return database.profileDao()
    }

    @Provides
    fun provideTriggerDao(database: MimicDatabase): TriggerDao {
        return database.triggerDao()
    }
}
