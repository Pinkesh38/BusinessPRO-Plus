package com.example.businessproplus

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OrderRepository @Inject constructor(
    private val orderDao: OrderDao, 
    private val itemDao: ItemDao
) {

    fun getFilteredOrdersPaged(partyName: String, status: String, sortType: Int): Flow<PagingData<Order>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                enablePlaceholders = false,
                initialLoadSize = 20
            ),
            pagingSourceFactory = { orderDao.getFilteredOrdersPaged(partyName, status, sortType) }
        ).flow
    }

    suspend fun createOrderWithStockUpdate(order: Order) {
        orderDao.createOrderWithStockUpdate(order, itemDao)
    }

    suspend fun deleteOrderWithStockRestore(orderId: Int) {
        orderDao.deleteOrderWithStockRestore(orderId, itemDao)
    }

    suspend fun getOrderById(orderId: Int): Order? {
        return orderDao.getOrderById(orderId)
    }
}
