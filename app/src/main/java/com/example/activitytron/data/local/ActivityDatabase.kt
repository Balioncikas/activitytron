package com.example.activitytron.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ActivityItem::class], version = 2, exportSchema = false)
abstract class ActivityDatabase : RoomDatabase() {
    abstract fun activityDao(): ActivityDao

    companion object {
        @Volatile
        private var Instance: ActivityDatabase? = null

        fun getDatabase(context: Context): ActivityDatabase {
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(context, ActivityDatabase::class.java, "activity_database")
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { Instance = it }
            }
        }
    }
}
