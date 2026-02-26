package com.mimicease.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.mimicease.data.local.dao.ProfileDao
import com.mimicease.data.local.dao.TriggerDao
import com.mimicease.data.local.entity.ProfileEntity
import com.mimicease.data.local.entity.TriggerEntity

@Database(entities = [ProfileEntity::class, TriggerEntity::class], version = 1, exportSchema = false)
abstract class MimicDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun triggerDao(): TriggerDao
}
