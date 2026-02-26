package com.mimicease.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey
    val id: String,                          // UUID.randomUUID().toString()
    val name: String,
    val icon: String,                        // 이모지 문자 또는 Material Icon 이름
    val isActive: Boolean,
    val sensitivity: Float = 1.0f,           // 0.5 ~ 2.0
    val globalCooldownMs: Int = 300,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "triggers",
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE   // 프로필 삭제 시 트리거도 삭제
        )
    ],
    indices = [Index("profileId")]
)
data class TriggerEntity(
    @PrimaryKey
    val id: String,                          // UUID
    val profileId: String,                   // FK → ProfileEntity.id
    val name: String,
    val blendShape: String,                  // 블렌드쉐이프 ID (예: "eyeBlinkRight")
    val threshold: Float,                    // 0.0 ~ 1.0
    val holdDurationMs: Int = 200,
    val cooldownMs: Int = 1000,
    val actionType: String,                  // Action sealed class의 타입 이름
    val actionParams: String = "{}",         // JSON 직렬화된 파라미터
    val isEnabled: Boolean = true,
    val priority: Int = 100,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class ProfileWithTriggers(
    @Embedded val profile: ProfileEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "profileId"
    )
    val triggers: List<TriggerEntity>
)
