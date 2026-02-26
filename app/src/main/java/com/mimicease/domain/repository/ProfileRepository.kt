package com.mimicease.domain.repository

import com.mimicease.domain.model.Profile
import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    fun getAllProfiles(): Flow<List<Profile>>
    fun getActiveProfile(): Flow<Profile?>
    suspend fun saveProfile(profile: Profile)
    suspend fun deleteProfile(profile: Profile)
    suspend fun activateProfile(id: String)
}
