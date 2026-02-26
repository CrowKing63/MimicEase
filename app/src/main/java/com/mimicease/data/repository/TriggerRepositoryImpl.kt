package com.mimicease.data.repository

import com.mimicease.data.local.dao.TriggerDao
import com.mimicease.data.local.entity.TriggerEntity
import com.mimicease.data.model.ActionSerializer
import com.mimicease.domain.model.Trigger
import com.mimicease.domain.repository.TriggerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TriggerRepositoryImpl(
    private val triggerDao: TriggerDao
) : TriggerRepository {

    override fun getTriggersByProfile(profileId: String): Flow<List<Trigger>> {
        return triggerDao.getTriggersByProfile(profileId).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun saveTrigger(trigger: Trigger) {
        triggerDao.insert(trigger.toEntity())
    }

    override suspend fun deleteTrigger(trigger: Trigger) {
        triggerDao.delete(trigger.toEntity())
    }

    override suspend fun setTriggerEnabled(id: String, enabled: Boolean) {
        triggerDao.setEnabled(id, enabled)
    }
}

fun TriggerEntity.toDomain(): Trigger = Trigger(
    id = id,
    profileId = profileId,
    name = name,
    blendShape = blendShape,
    threshold = threshold,
    holdDurationMs = holdDurationMs,
    cooldownMs = cooldownMs,
    action = ActionSerializer.deserialize(actionType, actionParams),
    isEnabled = isEnabled,
    priority = priority
)

fun Trigger.toEntity(): TriggerEntity {
    val (type, params) = ActionSerializer.serialize(action)
    return TriggerEntity(
        id = id, profileId = profileId, name = name,
        blendShape = blendShape, threshold = threshold,
        holdDurationMs = holdDurationMs, cooldownMs = cooldownMs,
        actionType = type, actionParams = params,
        isEnabled = isEnabled, priority = priority
    )
}
