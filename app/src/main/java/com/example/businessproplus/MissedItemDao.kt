package com.example.businessproplus

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface MissedItemDao {
    @Insert
    suspend fun insertMissedItem(item: MissedItem)

    @Query("SELECT * FROM missed_items_table ORDER BY id DESC")
    suspend fun getAllMissedItems(): List<MissedItem>

    // NEW: Get Top 5 Missed Items (Most frequently asked for)
    @Query("SELECT itemDescription, COUNT(*) as count FROM missed_items_table GROUP BY itemDescription ORDER BY count DESC LIMIT 5")
    suspend fun getTopMissedItems(): List<MissedItemSummary>

    // NEW: Get Monthly Loss Estimation (Sum of estimatedValue for current month)
    // Note: This assumes dateAsked is stored in a way that allows SQLite date functions, 
    // but since we use "dd/MM/yyyy", we might need to filter manually or use LIKE
    @Query("SELECT SUM(estimatedValue) FROM missed_items_table WHERE dateAsked LIKE '%' || :monthYear || '%'")
    suspend fun getMonthlyEstimatedLoss(monthYear: String): Double?

    // NEW: Get Reason Breakdown
    @Query("SELECT reason, COUNT(*) as count FROM missed_items_table GROUP BY reason")
    suspend fun getReasonBreakdown(): List<ReasonSummary>
}

data class MissedItemSummary(
    val itemDescription: String,
    val count: Int
)

data class ReasonSummary(
    val reason: String,
    val count: Int
)