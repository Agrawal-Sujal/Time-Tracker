package com.sunflower.timetracker.data.repository

import android.os.Build
import androidx.annotation.RequiresApi
import com.sunflower.timetracker.data.local.dao.TagDao
import com.sunflower.timetracker.data.local.dao.TimeSessionDao
import com.sunflower.timetracker.data.local.entity.TagEntity
import com.sunflower.timetracker.data.local.entity.TimeSessionEntity
import com.sunflower.timetracker.domain.model.Tag
import com.sunflower.timetracker.domain.model.TagStats
import com.sunflower.timetracker.domain.model.TimeSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimeTrackerRepository @Inject constructor(
    private val tagDao: TagDao,
    private val sessionDao: TimeSessionDao
) {

    // ── Tags ──────────────────────────────────────────────────────────────────

    fun getAllTags(): Flow<List<Tag>> =
        tagDao.getAllTags().map { list -> list.map { it.toDomain() } }

    suspend fun insertTag(tag: Tag): Long =
        tagDao.insertTag(tag.toEntity())

    suspend fun updateTag(tag: Tag) = tagDao.updateTag(tag.toEntity())

    suspend fun deleteTag(tag: Tag) = tagDao.deleteTag(tag.toEntity())

    // ── Sessions ──────────────────────────────────────────────────────────────

    fun getActiveSession(): Flow<TimeSession?> =
        sessionDao.getActiveSession().map { it?.toDomain() }

    suspend fun startSession(tagId: Long): Long {
        // Stop any existing active session first
        sessionDao.getActiveSessionOnce()?.let { active ->
            val now = System.currentTimeMillis()
            sessionDao.updateSession(
                active.copy(endTime = now, durationMs = now - active.startTime)
            )
        }
        return sessionDao.insertSession(
            TimeSessionEntity(tagId = tagId, startTime = System.currentTimeMillis())
        )
    }

    suspend fun pauseActiveSession() {
        sessionDao.getActiveSessionOnce()?.let { active ->
            if (!active.isPaused) {
                val elapsed = active.pausedElapsedMs + (System.currentTimeMillis() - active.startTime)
                sessionDao.updateSession(active.copy(isPaused = true, pausedElapsedMs = elapsed))
            }
        }
    }

    suspend fun resumeActiveSession() {
        sessionDao.getActiveSessionOnce()?.let { active ->
            if (active.isPaused) {
                sessionDao.updateSession(
                    active.copy(isPaused = false, startTime = System.currentTimeMillis())
                )
            }
        }
    }

    suspend fun stopActiveSession() {
        sessionDao.getActiveSessionOnce()?.let { active ->
            val now = System.currentTimeMillis()
            val duration = if (active.isPaused) {
                active.pausedElapsedMs
            } else {
                active.pausedElapsedMs + (now - active.startTime)
            }
            sessionDao.updateSession(
                active.copy(endTime = now, durationMs = duration, isPaused = false)
            )
        }
    }

    // ── Session detail & CRUD ─────────────────────────────────────────────────

    fun getSessionsByTagId(tagId: Long): Flow<List<TimeSession>> =
        sessionDao.getSessionsByTagId(tagId).map { list -> list.map { it.toDomain() } }

    suspend fun updateSession(session: TimeSession) =
        sessionDao.updateSession(session.toEntity())

    suspend fun deleteSession(sessionId: Long) =
        sessionDao.deleteSession(sessionId)

    suspend fun insertManualSession(session: TimeSession): Long =
        sessionDao.insertSession(session.toEntity())

    suspend fun getActiveSessionOnce(): TimeSession? =
        sessionDao.getActiveSessionOnce()?.toDomain()

    // ── Stats ─────────────────────────────────────────────────────────────────

    fun getDayStats(date: LocalDate = LocalDate.now()): Flow<List<TagStats>> {
        val zone = ZoneId.systemDefault()
        val dayStart = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val dayEnd = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return buildTagStats(dayStart, dayEnd)
    }

    fun getWeekStats(weekOf: LocalDate = LocalDate.now()): Flow<List<TagStats>> {
        val zone = ZoneId.systemDefault()
        val weekStart = weekOf.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            .atStartOfDay(zone).toInstant().toEpochMilli()
        val weekEnd = weekOf.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
            .plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return buildTagStats(weekStart, weekEnd)
    }

    private fun buildTagStats(from: Long, to: Long): Flow<List<TagStats>> =
        combine(
            tagDao.getAllTags(),
            sessionDao.getTagDurationsForRange(from, to)
        ) { tags, durations ->
            val durationMap = durations.associate { it.tagId to it.totalMs }
            val totalMs = durationMap.values.sum().coerceAtLeast(1L)
            tags.mapNotNull { tag ->
                val dur = durationMap[tag.id] ?: return@mapNotNull null
                TagStats(
                    tag = tag.toDomain(),
                    totalDurationMs = dur,
                    percentage = dur.toFloat() / totalMs * 100f
                )
            }
        }

    // ── Mappers ───────────────────────────────────────────────────────────────

    private fun TagEntity.toDomain() = Tag(id, name, colorHex, createdAt)
    private fun Tag.toEntity() = TagEntity(id, name, colorHex, createdAt)
    private fun TimeSessionEntity.toDomain() = TimeSession(id, tagId, startTime, endTime, durationMs, isPaused, pausedElapsedMs)
    private fun TimeSession.toEntity() = TimeSessionEntity(id, tagId, startTime, endTime, durationMs, isPaused, pausedElapsedMs)
}