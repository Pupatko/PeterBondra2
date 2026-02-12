package com.example.peterbondra

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Database as RoomDatabaseAnnotation

@RoomDatabaseAnnotation(entities = [Task::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "peterbondra_tasks.db",
                ).build().also { db -> instance = db }
            }
        }
    }
}
