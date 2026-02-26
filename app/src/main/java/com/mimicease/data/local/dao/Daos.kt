package com.mimicease.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.mimicease.data.local.entity.ProfileEntity
import com.mimicease.data.local.entity.ProfileWithTriggers
import com.mimicease.data.local.entity.TriggerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Transaction
    @Query("SELECT * FROM profiles ORDER BY createdAt ASC")
    fun getAllProfilesWithTriggers(): Flow<List<ProfileWithTriggers>>

    @Transaction
    @Query("SELECT * FROM profiles WHERE isActive = 1 LIMIT 1")
    fun getActiveProfileWithTriggers(): Flow<ProfileWithTriggers?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: ProfileEntity)

    @Update
    suspend fun update(profile: ProfileEntity)

    @Delete
    suspend fun delete(profile: ProfileEntity)

    @Query("UPDATE profiles SET isActive = 0")
    suspend fun deactivateAll()

    @Query("UPDATE profiles SET isActive = 1 WHERE id = :id")
    suspend fun activate(id: String)
}

@Dao
interface TriggerDao {
    @Query("SELECT * FROM triggers WHERE profileId = :profileId ORDER BY priority ASC")
    fun getTriggersByProfile(profileId: String): Flow<List<TriggerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(trigger: TriggerEntity)

    @Update
    suspend fun update(trigger: TriggerEntity)

    @Delete
    suspend fun delete(trigger: TriggerEntity)

    @Query("UPDATE triggers SET isEnabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: String, enabled: Boolean)

    @Query("DELETE FROM triggers WHERE profileId = :profileId")
    suspend fun deleteByProfile(profileId: String)
}
