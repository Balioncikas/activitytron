package com.example.activitytron.data.repository

import com.example.activitytron.data.local.ActivityDao
import com.example.activitytron.data.local.ActivityItem
import kotlinx.coroutines.flow.Flow

class ActivityRepository(private val activityDao: ActivityDao) {
    fun getAllActivitiesStream(): Flow<List<ActivityItem>> = activityDao.getAllActivities()

    suspend fun getActivity(id: Int): ActivityItem? = activityDao.getActivityById(id)

    suspend fun insertActivity(activity: ActivityItem) = activityDao.insertActivity(activity)

    suspend fun updateActivity(activity: ActivityItem) = activityDao.updateActivity(activity)

    suspend fun deleteActivity(activity: ActivityItem) = activityDao.deleteActivity(activity)

    suspend fun getRandomActivity(): ActivityItem? = activityDao.getRandomActivity()
}
