package com.example.businessproplus

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface OrderDao {
    @Insert
    suspend fun insertOrder(order: Order)

    @Update
    suspend fun updateOrder(order: Order)

    @Delete
    suspend fun delete(order: Order)

    @Query("SELECT * FROM orders_table WHERE isDeleted = 0 ORDER BY id DESC")
    suspend fun getAllOrders(): List<Order>

    @Query("SELECT * FROM orders_table WHERE id = :orderId")
    suspend fun getOrderById(orderId: Int): Order?

    @Query("SELECT SUM(total) FROM orders_table WHERE isDeleted = 0")
    suspend fun getTotalRevenue(): Double?

    @Query("SELECT SUM(remainingPayment) FROM orders_table WHERE isDeleted = 0")
    suspend fun getTotalToCollect(): Double?

    @Query("SELECT COUNT(id) FROM orders_table WHERE isDeleted = 0")
    suspend fun getTotalOrderCount(): Int

    @Query("SELECT COUNT(id) FROM orders_table WHERE isDeleted = 0 AND status = 'Pending'")
    suspend fun getPendingOrderCount(): Int

    @Query("SELECT * FROM orders_table WHERE customerName LIKE '%' || :customerName || '%' AND isDeleted = 0 ORDER BY id DESC")
    suspend fun getOrdersForCustomer(customerName: String): List<Order>

    @Query("SELECT * FROM orders_table WHERE itemDescription LIKE '%' || :itemName || '%' AND isDeleted = 0 ORDER BY id DESC")
    suspend fun getOrdersByItem(itemName: String): List<Order>

    @Query("SELECT SUM(total) FROM orders_table WHERE customerName = :customerName AND isDeleted = 0")
    suspend fun getTotalRevenueForCustomer(customerName: String): Double?

    @Query("SELECT SUM(total) FROM orders_table WHERE isDeleted = 0 AND orderDate >= :startDate AND orderDate <= :endDate")
    suspend fun getRevenueForRange(startDate: String, endDate: String): Double?

    @Query("SELECT COUNT(id) FROM orders_table WHERE isDeleted = 0 AND orderDate >= :startDate AND orderDate <= :endDate")
    suspend fun getOrderCountForRange(startDate: String, endDate: String): Int

    @Query("SELECT * FROM orders_table WHERE orderDate LIKE '%' || :monthYear || '%' AND isDeleted = 0 ORDER BY id DESC")
    suspend fun getOrdersByMonth(monthYear: String): List<Order>

    @Query("SELECT SUM(total) FROM orders_table WHERE isDeleted = 0 AND orderDate LIKE '%' || :monthYear || '%'")
    suspend fun getMonthlyRevenue(monthYear: String): Double?

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
        LIMIT :limit OFFSET :offset
    """)
    suspend fun getFilteredOrders(
        partyName: String,
        status: String,
        sortType: Int,
        limit: Int,
        offset: Int
    ): List<Order>

    @Query("SELECT SUM(total) FROM orders_table WHERE isDeleted = 0 AND orderDate = :date")
    suspend fun getDailyRevenue(date: String): Double?

    @Query("SELECT COUNT(id) FROM orders_table WHERE isDeleted = 0 AND orderDate = :date")
    suspend fun getDailyOrderCount(date: String): Int

    @Query("""
        SELECT itemDescription as name, SUM(quantity) as count 
        FROM orders_table 
        WHERE isDeleted = 0 
        GROUP BY itemDescription 
        ORDER BY count DESC 
        LIMIT :limit
    """)
    suspend fun getTopSellingItems(limit: Int): List<ReportItem>

    @Query("""
        SELECT customerName as name, SUM(total) as count 
        FROM orders_table 
        WHERE isDeleted = 0 
        GROUP BY customerName 
        ORDER BY count DESC 
        LIMIT :limit
    """)
    suspend fun getMostActiveParties(limit: Int): List<ReportItem>

    @Query("""
        SELECT o.orderDate, SUM(o.total) as dailyTotal 
        FROM orders_table o 
        WHERE o.isDeleted = 0 AND o.orderDate >= :startDate 
        GROUP BY o.orderDate 
        ORDER BY o.orderDate ASC
    """)
    suspend fun getDailyRevenueTrend(startDate: String): List<TrendItem>
    
    @Query("SELECT id, customerName, itemDescription, quantity, status, orderDate FROM orders_table WHERE isDeleted = 0 ORDER BY id DESC")
    suspend fun getOrderSummaries(): List<OrderSummary>

    @Query("SELECT SUM(dueAmount) FROM orders_table WHERE isDeleted = 0 AND isPaid = 0")
    suspend fun getTotalOutstandingPayment(): Double?

    @Query("""
        SELECT customerName, SUM(dueAmount) as totalDue 
        FROM orders_table 
        WHERE isDeleted = 0 AND isPaid = 0 
        GROUP BY customerName 
        HAVING totalDue > 0 
        ORDER BY totalDue DESC
    """)
    suspend fun getClientWisePendingPayments(): List<ClientPendingPayment>

    @Query("SELECT * FROM orders_table WHERE id = :orderId")
    fun getOrderByIdLive(orderId: Int): androidx.lifecycle.LiveData<Order?>
}

data class OrderSummary(
    val id: Int,
    val customerName: String,
    val itemDescription: String,
    val quantity: Int,
    val status: String,
    val orderDate: String
)

data class ClientPendingPayment(
    val customerName: String,
    val totalDue: Double
)

data class ReportItem(val name: String, val count: Double)
data class TrendItem(val orderDate: String, val dailyTotal: Double)
