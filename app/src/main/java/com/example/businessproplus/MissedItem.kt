package com.example.businessproplus

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "missed_items_table")
data class MissedItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    var itemDescription: String,
    var partyName: String = "",
    var dateAsked: String,
    var reason: String = "Out of Stock", // Out of Stock, Price Too High, Quality, etc.
    var estimatedValue: Double = 0.0,
    var competitorInfo: String = ""
)