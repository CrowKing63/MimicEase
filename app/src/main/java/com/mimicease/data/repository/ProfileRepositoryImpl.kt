package com.mimicease.data.repository

import com.google.gson.Gson
import com.mimicease.data.local.dao.ProfileDao
import com.mimicease.data.local.dao.TriggerDao
import com.mimicease.data.local.entity.ProfileEntity
import com.mimicease.data.local.entity.ProfileWithTriggers
import com.mimicease.data.local.entity.TriggerEntity
import com.mimicease.data.model.ExportedProfile
import com.mimicease.data.model.ProfileTransferModel
import com.mimicease.domain.model.Profile
import com.mimicease.domain.repository.ImportResult
import com.mimicease.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class ProfileRepositoryImpl(
    private val profileDao: ProfileDao,
    private val triggerDao: TriggerDao
) : ProfileRepository {
    private val gson = Gson()

    override fun getAllProfiles(): Flow<List<Profile>> {
        return profileDao.getAllProfilesWithTriggers().map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getActiveProfile(): Flow<Profile?> {
        return profileDao.getActiveProfileWithTriggers().map { it?.toDomain() }
    }

    override suspend fun saveProfile(profile: Profile) {
        val entity = ProfileEntity(
            id = profile.id,
            name = profile.name,
            icon = profile.icon,
            isActive = profile.isActive,
            sensitivity = profile.sensitivity,
            globalCooldownMs = profile.globalCooldownMs,
            createdAt = profile.createdAt,
            updatedAt = profile.updatedAt
        )
        if (profile.isActive) {
            profileDao.deactivateAll()
        }
        profileDao.insert(entity)
    }

    override suspend fun deleteProfile(profile: Profile) {
        val entity = ProfileEntity(
            id = profile.id,
            name = profile.name,
            icon = profile.icon,
            isActive = profile.isActive,
            sensitivity = profile.sensitivity,
            globalCooldownMs = profile.globalCooldownMs,
            createdAt = profile.createdAt,
            updatedAt = profile.updatedAt
        )
        profileDao.delete(entity)
    }

    override suspend fun activateProfile(id: String) {
        profileDao.deactivateAll()
        profileDao.activate(id)
    }

    override suspend fun exportProfiles(ids: List<String>): String {
        val exportedProfiles = ids.mapNotNull { id ->
            profileDao.getProfileWithTriggers(id)?.let { pw ->
                ExportedProfile(pw.profile, pw.triggers)
            }
        }
        val model = ProfileTransferModel(profiles = exportedProfiles)
        return gson.toJson(model)
    }

    override suspend fun importProfiles(json: String): ImportResult {
        return try {
            val model = gson.fromJson(json, ProfileTransferModel::class.java)
                ?: return ImportResult.Error("Failed to parse JSON")
            
            if (model.version > ProfileTransferModel.FORMAT_VERSION) {
                return ImportResult.Error("Unsupported version: ${model.version}")
            }

            val currentNames = profileDao.getAllProfileNames()
            val profilesToInsert = mutableListOf<ProfileEntity>()
            val triggersToInsert = mutableListOf<TriggerEntity>()

            model.profiles.forEach { exported ->
                val originProfile = exported.profile
                
                // Name suffixing (collision handling)
                var newName = originProfile.name
                while (currentNames.contains(newName)) {
                    newName = "$newName (Imported)"
                }
                
                // New ID for uniqueness safety (ignore existing ID to follow "replicate to new profile" strategy)
                val newProfileId = UUID.randomUUID().toString()
                val now = System.currentTimeMillis()
                
                profilesToInsert.add(originProfile.copy(
                    id = newProfileId,
                    name = newName,
                    isActive = false, // Never activate on import for safety
                    createdAt = now,
                    updatedAt = now
                ))

                exported.triggers.forEach { trigger ->
                    triggersToInsert.add(trigger.copy(
                        id = UUID.randomUUID().toString(),
                        profileId = newProfileId,
                        createdAt = now,
                        updatedAt = now
                    ))
                }
            }

            profileDao.insertAll(profilesToInsert)
            triggerDao.insertAll(triggersToInsert)

            ImportResult.Success(profilesToInsert.size)
        } catch (e: Exception) {
            ImportResult.Error("Import failed: ${e.localizedMessage}")
        }
    }
}

fun ProfileWithTriggers.toDomain(): Profile = Profile(
    id = profile.id,
    name = profile.name,
    icon = profile.icon,
    isActive = profile.isActive,
    sensitivity = profile.sensitivity,
    globalCooldownMs = profile.globalCooldownMs,
    triggers = triggers.map { it.toDomain() },
    createdAt = profile.createdAt,
    updatedAt = profile.updatedAt
)
