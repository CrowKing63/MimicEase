package com.mimicease

import com.google.gson.Gson
import com.mimicease.data.local.dao.ProfileDao
import com.mimicease.data.local.dao.TriggerDao
import com.mimicease.data.local.entity.ProfileEntity
import com.mimicease.data.local.entity.ProfileWithTriggers
import com.mimicease.data.local.entity.TriggerEntity
import com.mimicease.data.repository.ProfileRepositoryImpl
import com.mimicease.domain.model.Profile
import com.mimicease.domain.repository.ImportResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ProfileTransferTest {

    private lateinit var profileDao: FakeProfileDao
    private lateinit var triggerDao: FakeTriggerDao
    private lateinit var repository: ProfileRepositoryImpl

    @Before
    fun setup() {
        profileDao = FakeProfileDao()
        triggerDao = FakeTriggerDao()
        repository = ProfileRepositoryImpl(profileDao, triggerDao)
    }

    @Test
    fun `전체 프로필 내보내기 및 가져오기 성공 라운드트립`() = runBlocking {
        // Given: 한 개의 프로필과 트리거 준비
        val p1 = ProfileEntity("p1", "Original", "😊", true)
        val t1 = TriggerEntity("t1", "p1", "T1", "blink", 0.5f, actionType = "GlobalHome")
        profileDao.profileWithTriggersMap["p1"] = ProfileWithTriggers(p1, listOf(t1))

        // When: Export
        val json = repository.exportProfiles(listOf("p1"))
        
        // Then: JSON 검증
        assertTrue(json.contains("Original"))
        assertTrue(json.contains("GlobalHome"))

        // When: Import into clean DB
        profileDao.profileWithTriggersMap.clear()
        val result = repository.importProfiles(json)

        // Then: Success
        assertTrue(result is ImportResult.Success)
        assertEquals(1, (result as ImportResult.Success).importedCount)
        assertEquals(1, profileDao.insertedProfiles.size)
        assertEquals("Original", profileDao.insertedProfiles[0].name)
    }

    @Test
    fun `이름 충돌 시 (Imported) 접미사 추가 확인`() = runBlocking {
        // Given: 이미 "Target" 이라는 이름의 프로필이 있음
        profileDao.existingNames.add("Target")
        
        val json = """
            {
                "version": 1,
                "profiles": [
                    {
                        "profile": { "id": "old-id", "name": "Target", "icon": "😊", "isActive": true },
                        "triggers": []
                    }
                ]
            }
        """.trimIndent()

        // When: Import
        repository.importProfiles(json)

        // Then: 이름이 변경되어 삽입됨
        assertEquals("Target (Imported)", profileDao.insertedProfiles[0].name)
    }

    @Test
    fun `손상된 JSON 가져오기 시도 시 에러 반환`() = runBlocking {
        val result = repository.importProfiles("{ invalid json }")
        assertTrue(result is ImportResult.Error)
    }

    @Test
    fun `지원하지 않는 높은 버전의 파일 가져오기 시도 시 거부`() = runBlocking {
        val json = """{ "version": 999, "profiles": [] }"""
        val result = repository.importProfiles(json)
        assertTrue(result is ImportResult.Error)
        assertTrue((result as ImportResult.Error).message.contains("version"))
    }

    // ─── Fake DAOs ────────────────────────────────────────────────────────

    private class FakeProfileDao : ProfileDao {
        val profileWithTriggersMap = mutableMapOf<String, ProfileWithTriggers>()
        val insertedProfiles = mutableListOf<ProfileEntity>()
        val existingNames = mutableListOf<String>()

        override fun getAllProfilesWithTriggers(): Flow<List<ProfileWithTriggers>> = emptyFlow()
        override fun getActiveProfileWithTriggers(): Flow<ProfileWithTriggers?> = emptyFlow()
        override suspend fun insert(profile: ProfileEntity) { insertedProfiles.add(profile) }
        override suspend fun update(profile: ProfileEntity) {}
        override suspend fun delete(profile: ProfileEntity) {}
        override suspend fun deactivateAll() {}
        override suspend fun activate(id: String) {}
        
        override suspend fun insertAll(profiles: List<ProfileEntity>) {
            insertedProfiles.addAll(profiles)
        }

        override suspend fun getProfileWithTriggers(id: String): ProfileWithTriggers? = profileWithTriggersMap[id]
        
        override suspend fun getAllProfileNames(): List<String> = existingNames
    }

    private class FakeTriggerDao : TriggerDao {
        val insertedTriggers = mutableListOf<TriggerEntity>()
        override fun getTriggersByProfile(profileId: String): Flow<List<TriggerEntity>> = emptyFlow()
        override suspend fun insert(trigger: TriggerEntity) { insertedTriggers.add(trigger) }
        override suspend fun update(trigger: TriggerEntity) {}
        override suspend fun delete(trigger: TriggerEntity) {}
        override suspend fun setEnabled(id: String, enabled: Boolean) {}
        override suspend fun insertAll(triggers: List<TriggerEntity>) { insertedTriggers.addAll(triggers) }
        override suspend fun deleteByProfile(profileId: String) {}
    }
}
