package com.example.businessproplus

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "calculation_history_table")
data class CalculationHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val voltage: String,
    val ampere: String,
    val ohms: String,
    val watts: String,
    val timestamp: String
)