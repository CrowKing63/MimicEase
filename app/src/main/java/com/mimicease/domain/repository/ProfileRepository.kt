package com.mimicease.domain.repository

import com.mimicease.domain.model.Profile
import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    fun getAllProfiles(): Flow<List<Profile>>
    fun getActiveProfile(): Flow<Profile?>
    suspend fun saveProfile(profile: Profile)
    suspend fun deleteProfile(profile: Profile)
    suspend fun activateProfile(id: String)

    // ── Import/Export ───────────────────────────
    suspend fun exportProfiles(ids: List<String>): String
    suspend fun importProfiles(json: String): ImportResult
}

sealed class ImportResult {
    data class Success(val importedCount: Int) : ImportResult()
    data class Error(val message: String) : ImportResult()
}
