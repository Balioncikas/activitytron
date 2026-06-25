package com.example.activitytron.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "activities")
data class ActivityItem(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val description: String = "",
    val isDone: Boolean = false,
    val isCustom: Boolean = true,
    val category: String = "Other"
)
