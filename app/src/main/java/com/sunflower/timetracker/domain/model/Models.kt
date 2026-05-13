package com.sunflower.timetracker.domain.model

data class Tag(
    val id: Long = 0,
    val name: String,
    val colorHex: String,
    val createdAt: Long = System.currentTimeMillis()
)

data class TimeSession(
    val id: Long = 0,
    val tagId: Long,
    val startTime: Long,
    val endTime: Long? = null,
    val durationMs: Long = 0,
    val isPaused: Boolean = false,
    val latestStartTime: Long
)

data class TagStats(
    val tag: Tag,
    val totalDurationMs: Long,
    val percentage: Float,
    val sessionCount: Int = 0
)

data class ActiveSessionState(
    val session: TimeSession?,
    val tag: Tag?,
    val elapsedMs: Long = 0
)

// Preset tag color palette
val TAG_COLORS = listOf(
    "#FF6B6B", // Red
    "#FF9F43", // Orange
    "#FECA57", // Yellow
    "#48DBFB", // Cyan
    "#54A0FF", // Blue
    "#5F27CD", // Purple
    "#1DD1A1", // Teal
    "#FF9FF3", // Pink
    "#C8D6E5", // Silver
    "#EE5A24", // Deep orange
    "#0652DD", // Deep blue
    "#009432"  // Green
)