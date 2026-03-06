package com.example.businessproplus

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "items_table",
    indices = [Index(value = ["itemName"]), Index(value = ["category"])]
)
data class Item(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    var itemName: String,
    var unit: String,
    var purchasePrice: Double = 0.0,
    var salesPrice: Double,
    var discountPercent: Double = 0.0,
    var category: String,
    var itemDescription: String,
    var currentStock: Int = 0,
    var openingStock: Int = 0,
    var minStockLevel: Int = 5,
    var batchNumber: String = "",
    var isDeleted: Boolean = false,
    var photoPath: String? = null, // NEW: Item Photo
    var videoPath: String? = null  // NEW: Item Video
)