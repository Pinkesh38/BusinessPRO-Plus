package com.example.businessproplus

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete

@Dao
interface ItemDao {
    @Insert
    suspend fun insertItem(item: Item)

    @Update
    suspend fun updateItem(item: Item)

    @Delete
    suspend fun deleteItem(item: Item)

    @Query("SELECT * FROM items_table WHERE isDeleted = 0")
    suspend fun getAllItems(): List<Item>

    @Query("SELECT * FROM items_table WHERE id = :itemId")
    suspend fun getItemById(itemId: Int): Item?

    @Query("SELECT * FROM items_table WHERE itemName = :name AND isDeleted = 0 LIMIT 1")
    suspend fun getItemByName(name: String): Item?

    @Query("UPDATE items_table SET salesPrice = :newPrice WHERE id = :itemId")
    suspend fun updateItemPrice(itemId: Int, newPrice: Double)

    // --- NEW INVENTORY ANALYTICS ---

    @Query("SELECT * FROM items_table WHERE isDeleted = 0 AND currentStock <= minStockLevel")
    suspend fun getLowStockItems(): List<Item>

    @Query("""
        SELECT * FROM items_table 
        WHERE isDeleted = 0 
        AND id NOT IN (SELECT DISTINCT itemDescription FROM orders_table WHERE isDeleted = 0)
    """)
    suspend fun getDeadStockItems(): List<Item>

    @Query("SELECT SUM(currentStock * purchasePrice) FROM items_table WHERE isDeleted = 0")
    suspend fun getTotalStockValue(): Double?
}