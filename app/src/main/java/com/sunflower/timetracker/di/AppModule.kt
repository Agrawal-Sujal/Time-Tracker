package com.sunflower.timetracker.di

import android.content.Context
import androidx.room.Room
import com.sunflower.timetracker.data.local.AppDatabase
import com.sunflower.timetracker.data.local.dao.TagDao
import com.sunflower.timetracker.data.local.dao.TimeSessionDao
import com.sunflower.timetracker.data.local.migration.MIGRATION_1_2
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "timetracker.db")
            .addMigrations(MIGRATION_1_2)
            .build()

    @Provides
    fun provideTagDao(db: AppDatabase): TagDao = db.tagDao()

    @Provides
    fun provideTimeSessionDao(db: AppDatabase): TimeSessionDao = db.timeSessionDao()
}