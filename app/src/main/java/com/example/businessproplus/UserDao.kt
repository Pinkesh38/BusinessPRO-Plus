package com.example.businessproplus

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface UserDao {
    @Query("UPDATE users_table SET role = :newRole WHERE id = :userId")
    suspend fun updateUserRole(userId: Int, newRole: String)

    @androidx.room.Delete
    suspend fun deleteUser(user: User)

    @Query("SELECT * FROM users_table WHERE id = :userId")
    suspend fun getUserById(userId: Int): User?

    @Query("UPDATE users_table SET pinCode = :newPin WHERE id = :userId")
    suspend fun updatePin(userId: Int, newPin: String)

    @Insert
    suspend fun insertUser(user: User)

    @Update
    suspend fun updateUser(user: User)

    @Query("SELECT * FROM users_table WHERE pinCode = :pin")
    suspend fun getUserByPin(pin: String): User?

    @Query("SELECT * FROM users_table")
    suspend fun getAllUsers(): List<User>

    @Query("SELECT COUNT(*) FROM users_table")
    suspend fun getUserCount(): Int
}