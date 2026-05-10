package com.sunflower.timetracker.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "time_sessions",
    foreignKeys = [
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("tagId")]
)
data class TimeSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tagId: Long,
    val startTime: Long,           // epoch millis
    val endTime: Long? = null,     // null = running or paused
    val durationMs: Long = 0,      // cached duration for finished sessions
    val isPaused: Boolean = false, // true while paused
    val pausedElapsedMs: Long = 0  // how many ms elapsed before pause
)