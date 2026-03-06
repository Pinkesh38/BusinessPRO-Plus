package com.example.businessproplus

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users_table")
data class User(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    var username: String,
    var pinCode: String,
    var role: String, // "Admin" or "Staff"
    var mobileNo: String = "",
    var imagePath: String? = null,
    var lastLogin: String? = null,
    var isApproved: Boolean = true // 🛡️ Restored to match existing database schema
)