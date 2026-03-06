package com.example.businessproplus

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface UserActivityDao {
    @Insert
    suspend fun insertActivity(activity: UserActivity)

    @Query("SELECT * FROM user_activity_table ORDER BY id DESC LIMIT 100")
    suspend fun getRecentActivity(): List<UserActivity>
}