package com.sunflower.timetracker.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {

    override fun migrate(db: SupportSQLiteDatabase) {

        // Create new table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS time_sessions_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                tagId INTEGER NOT NULL,
                startTime INTEGER NOT NULL,
                endTime INTEGER,
                durationMs INTEGER NOT NULL,
                isPaused INTEGER NOT NULL,
                latestStartTime INTEGER NOT NULL,
                FOREIGN KEY(tagId) REFERENCES tags(id) ON DELETE CASCADE
            )
        """.trimIndent())

        // Copy old data
        db.execSQL("""
            INSERT INTO time_sessions_new (
                id,
                tagId,
                startTime,
                endTime,
                durationMs,
                isPaused,
                latestStartTime
            )
            SELECT
                id,
                tagId,
                startTime,
                endTime,
                durationMs,
                isPaused,
                0
            FROM time_sessions
        """.trimIndent())

        // Delete old table
        db.execSQL("DROP TABLE time_sessions")

        // Rename new table
        db.execSQL("""
            ALTER TABLE time_sessions_new
            RENAME TO time_sessions
        """.trimIndent())

        // Recreate index
        db.execSQL("""
            CREATE INDEX IF NOT EXISTS
            index_time_sessions_tagId
            ON time_sessions(tagId)
        """.trimIndent())
    }
}