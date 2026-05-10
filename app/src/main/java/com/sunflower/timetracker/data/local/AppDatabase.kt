package com.sunflower.timetracker.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.sunflower.timetracker.data.local.dao.TagDao
import com.sunflower.timetracker.data.local.dao.TimeSessionDao
import com.sunflower.timetracker.data.local.entity.TagEntity
import com.sunflower.timetracker.data.local.entity.TimeSessionEntity

@Database(
    entities = [TagEntity::class, TimeSessionEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tagDao(): TagDao
    abstract fun timeSessionDao(): TimeSessionDao
}
