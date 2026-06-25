package com.example.activitytron.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityDao {
    @Query("SELECT * FROM activities")
    fun getAllActivities(): Flow<List<ActivityItem>>

    @Query("SELECT * FROM activities WHERE id = :id")
    suspend fun getActivityById(id: Int): ActivityItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivity(activity: ActivityItem)

    @Update
    suspend fun updateActivity(activity: ActivityItem)

    @Delete
    suspend fun deleteActivity(activity: ActivityItem)

    @Query("SELECT * FROM activities ORDER BY RANDOM() LIMIT 1")
    suspend fun getRandomActivity(): ActivityItem?
}
