package com.example.businessproplus

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_activity_table")
data class UserActivity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val userName: String,
    val action: String,
    val timestamp: String
)