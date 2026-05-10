package com.sunflower.timetracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.sunflower.timetracker.data.local.entity.TimeSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TimeSessionDao {

    @Insert
    suspend fun insertSession(session: TimeSessionEntity): Long

    @Update
    suspend fun updateSession(session: TimeSessionEntity)

    @Query("SELECT * FROM time_sessions WHERE endTime IS NULL LIMIT 1")
    fun getActiveSession(): Flow<TimeSessionEntity?>

    @Query("SELECT * FROM time_sessions WHERE endTime IS NULL LIMIT 1")
    suspend fun getActiveSessionOnce(): TimeSessionEntity?

    @Query("SELECT * FROM time_sessions WHERE tagId = :tagId ORDER BY startTime DESC")
    fun getSessionsByTagId(tagId: Long): Flow<List<TimeSessionEntity>>

    @Query("SELECT COUNT(*) FROM time_sessions WHERE tagId = :tagId AND endTime IS NOT NULL")
    fun getSessionCountForTag(tagId: Long): Flow<Int>

    @Query("""
        SELECT * FROM time_sessions 
        WHERE startTime >= :dayStart AND startTime < :dayEnd
        ORDER BY startTime DESC
    """)
    fun getSessionsForDay(dayStart: Long, dayEnd: Long): Flow<List<TimeSessionEntity>>

    @Query("""
        SELECT * FROM time_sessions 
        WHERE startTime >= :weekStart AND startTime < :weekEnd
        ORDER BY startTime DESC
    """)
    fun getSessionsForWeek(weekStart: Long, weekEnd: Long): Flow<List<TimeSessionEntity>>

    @Query("""
        SELECT tagId, SUM(durationMs) as totalMs
        FROM time_sessions
        WHERE startTime >= :from AND startTime < :to AND endTime IS NOT NULL
        GROUP BY tagId
    """)
    fun getTagDurationsForRange(from: Long, to: Long): Flow<List<TagDurationResult>>

    @Query("DELETE FROM time_sessions WHERE id = :id")
    suspend fun deleteSession(id: Long)
}

data class TagDurationResult(
    val tagId: Long,
    val totalMs: Long
)