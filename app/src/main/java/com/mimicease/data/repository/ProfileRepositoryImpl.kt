package com.mimicease.data.repository

import com.mimicease.data.local.dao.ProfileDao
import com.mimicease.data.local.entity.ProfileEntity
import com.mimicease.data.local.entity.ProfileWithTriggers
import com.mimicease.domain.model.Profile
import com.mimicease.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ProfileRepositoryImpl(
    private val profileDao: ProfileDao
) : ProfileRepository {

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
