package com.example.businessproplus

import androidx.room.*
import androidx.paging.PagingSource
import kotlinx.coroutines.flow.Flow

@Dao
interface OrderDao {
    @Insert
    suspend fun insertOrder(order: Order)

    @Update
    suspend fun updateOrder(order: Order)

    @Delete
    suspend fun delete(order: Order)

    @Query("SELECT * FROM orders_table WHERE id = :orderId")
    suspend fun getOrderById(orderId: Int): Order?

    @Query("SELECT COUNT(id) FROM orders_table WHERE isDeleted = 0")
    fun getTotalOrderCountFlow(): Flow<Int>

    @Query("SELECT COUNT(id) FROM orders_table WHERE isDeleted = 0")
    suspend fun getTotalOrderCount(): Int

    @Query("SELECT COUNT(id) FROM orders_table WHERE isDeleted = 0 AND status = 'Pending'")
    fun getPendingOrderCountFlow(): Flow<Int>

    @Query("SELECT COUNT(id) FROM orders_table WHERE isDeleted = 0 AND status = 'Pending'")
    suspend fun getPendingOrderCount(): Int

    @Transaction
    suspend fun createOrderWithStockUpdate(order: Order, itemDao: ItemDao) {
        val item = itemDao.getItemByName(order.itemDescription)
        if (item != null) {
            if (item.currentStock < order.quantity) {
                throw Exception("Insufficient stock for ${item.itemName}")
            }
            item.currentStock -= order.quantity
            itemDao.updateItem(item)
        }
        insertOrder(order)
    }

    @Transaction
    suspend fun deleteOrderWithStockRestore(orderId: Int, itemDao: ItemDao) {
        val order = getOrderById(orderId) ?: return
        val item = itemDao.getItemByName(order.itemDescription)
        if (item != null) {
            item.currentStock += order.quantity
            itemDao.updateItem(item)
        }
        updateOrder(order.copy(isDeleted = true))
    }

    @Query("""
        SELECT * FROM orders_table 
        WHERE isDeleted = 0
        AND (:partyName == '' OR customerName LIKE '%' || :partyName || '%')
        AND (:status == 'All' OR status = :status)
        ORDER BY 
        CASE WHEN :sortType = 0 THEN orderDate END DESC,
        CASE WHEN :sortType = 1 THEN orderDate END ASC,
        CASE WHEN :sortType = 2 THEN deliveryDate END DESC,
        CASE WHEN :sortType = 3 THEN deliveryDate END ASC,
        id DESC
    """)
    fun getFilteredOrdersPaged(
        partyName: String,
        status: String,
        sortType: Int
    ): PagingSource<Int, Order>

    @Query("SELECT SUM(total) FROM orders_table WHERE isDeleted = 0")
    suspend fun getTotalRevenue(): Double?

    @Query("SELECT COUNT(id) FROM orders_table WHERE isDeleted = 0 AND orderDate >= :startDate AND orderDate <= :endDate")
    suspend fun getOrderCountForRange(startDate: String, endDate: String): Int

    @Query("SELECT SUM(total) FROM orders_table WHERE isDeleted = 0 AND orderDate >= :startDate AND orderDate <= :endDate")
    suspend fun getRevenueForRange(startDate: String, endDate: String): Double?

    @Query("SELECT SUM(remainingPayment) FROM orders_table WHERE isDeleted = 0")
    suspend fun getTotalToCollect(): Double?

    // 🚀 IMPROVED ANALYTICS QUERIES
    
    @Query("""
        SELECT itemDescription as name, SUM(quantity) as count 
        FROM orders_table 
        WHERE isDeleted = 0 AND orderDate >= :start AND orderDate <= :end
        GROUP BY itemDescription ORDER BY count DESC LIMIT :limit
    """)
    suspend fun getTopSellingItemsInRange(start: String, end: String, limit: Int): List<ReportItem>

    @Query("""
        SELECT itemDescription as name, SUM(total) as count 
        FROM orders_table 
        WHERE isDeleted = 0 AND orderDate >= :start AND orderDate <= :end
        GROUP BY itemDescription ORDER BY count DESC LIMIT :limit
    """)
    suspend fun getTopRevenueItemsInRange(start: String, end: String, limit: Int): List<ReportItem>

    @Query("""
        SELECT customerName as name, SUM(total) as count 
        FROM orders_table 
        WHERE isDeleted = 0 AND orderDate >= :start AND orderDate <= :end
        GROUP BY customerName ORDER BY count DESC LIMIT :limit
    """)
    suspend fun getTopCustomersInRange(start: String, end: String, limit: Int): List<ReportItem>

    @Query("""
        SELECT heaterType as name, SUM(total) as count 
        FROM orders_table 
        WHERE isDeleted = 0 AND orderDate >= :start AND orderDate <= :end AND heaterType != ''
        GROUP BY heaterType ORDER BY count DESC
    """)
    suspend fun getCategorySalesInRange(start: String, end: String): List<ReportItem>

    @Query("""
        SELECT 'Paid' as name, SUM(advancePayment) as count 
        FROM orders_table WHERE isDeleted = 0 AND orderDate >= :start AND orderDate <= :end
        UNION ALL
        SELECT 'Pending' as name, SUM(remainingPayment) as count 
        FROM orders_table WHERE isDeleted = 0 AND orderDate >= :start AND orderDate <= :end
    """)
    suspend fun getPaymentStatusInRange(start: String, end: String): List<ReportItem>

    @Query("SELECT o.orderDate, SUM(o.total) as dailyTotal FROM orders_table o WHERE o.isDeleted = 0 AND o.orderDate >= :startDate GROUP BY o.orderDate ORDER BY o.orderDate ASC")
    suspend fun getDailyRevenueTrend(startDate: String): List<TrendItem>

    @Query("SELECT * FROM orders_table WHERE customerName = :name AND isDeleted = 0")
    suspend fun getOrdersForCustomer(name: String): List<Order>

    @Query("SELECT MAX(orderDate) FROM orders_table WHERE customerName = :name AND isDeleted = 0")
    suspend fun getLatestOrderDateForCustomer(name: String): String?

    @Query("SELECT SUM(total) FROM orders_table WHERE customerName = :name AND isDeleted = 0")
    suspend fun getTotalRevenueForCustomer(name: String): Double?
    
    @Query("SELECT * FROM orders_table WHERE isDeleted = 0 ORDER BY id DESC")
    suspend fun getAllOrders(): List<Order>

    @Query("SELECT * FROM orders_table WHERE isDeleted = 0 AND remainingPayment > 0 AND isPaid = 0 ORDER BY orderDate ASC")
    suspend fun getPendingPayments(): List<Order>

    @Query("SELECT SUM(total) FROM orders_table WHERE isDeleted = 0 AND orderDate LIKE '%' || :monthLabel")
    suspend fun getMonthlyRevenue(monthLabel: String): Double?

    @Query("SELECT * FROM orders_table WHERE isDeleted = 0 AND orderDate LIKE '%' || :monthYear")
    suspend fun getOrdersByMonth(monthYear: String): List<Order>

    @Query("SELECT * FROM orders_table WHERE isDeleted = 0 AND itemDescription = :itemName")
    suspend fun getOrdersByItem(itemName: String): List<Order>

    @Query("SELECT SUM(total) FROM orders_table WHERE isDeleted = 0 AND orderDate = :date")
    suspend fun getDailyRevenue(date: String): Double?

    @Query("SELECT COUNT(id) FROM orders_table WHERE isDeleted = 0 AND orderDate = :date")
    suspend fun getDailyOrderCount(date: String): Int
}
