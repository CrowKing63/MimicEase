package com.mimicease.domain.repository

import com.mimicease.domain.model.Trigger
import kotlinx.coroutines.flow.Flow

interface TriggerRepository {
    fun getTriggersByProfile(profileId: String): Flow<List<Trigger>>
    suspend fun saveTrigger(trigger: Trigger)
    suspend fun deleteTrigger(trigger: Trigger)
    suspend fun setTriggerEnabled(id: String, enabled: Boolean)
}
