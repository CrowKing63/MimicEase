package com.mimicease.data.model

import com.google.gson.annotations.SerializedName
import com.mimicease.data.local.entity.ProfileEntity
import com.mimicease.data.local.entity.TriggerEntity

/**
 * 전송용 프로필 데이터 모델.
 * JSON 직렬화 시 하위 호환성을 위해 버전 필드를 포함한다.
 */
data class ProfileTransferModel(
    @SerializedName("version")
    val version: Int = FORMAT_VERSION,
    @SerializedName("exported_at")
    val exportedAt: Long = System.currentTimeMillis(),
    @SerializedName("profiles")
    val profiles: List<ExportedProfile>
) {
    companion object {
        const val FORMAT_VERSION = 1
    }
}

/**
 * 개별 프로필과 해당 트리거들을 묶은 모델.
 */
data class ExportedProfile(
    @SerializedName("profile")
    val profile: ProfileEntity,
    @SerializedName("triggers")
    val triggers: List<TriggerEntity>
)
