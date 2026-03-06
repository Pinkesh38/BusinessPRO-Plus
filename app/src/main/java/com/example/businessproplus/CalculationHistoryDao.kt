package com.example.businessproplus

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface CalculationHistoryDao {
    @Insert
    suspend fun insertCalculation(calculation: CalculationHistory)

    @Query("SELECT * FROM calculation_history_table ORDER BY id DESC LIMIT 50")
    suspend fun getRecentCalculations(): List<CalculationHistory>

    @Query("DELETE FROM calculation_history_table")
    suspend fun clearHistory()
}