package com.example.businessproplus

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class OrderRepository(private val orderDao: OrderDao, private val itemDao: ItemDao, private val partyDao: PartyDao) {

    suspend fun getOrderById(id: Int) = withContext(Dispatchers.IO) {
        orderDao.getOrderById(id)
    }

    suspend fun saveOrder(order: Order, qtyDelta: Int) = withContext(Dispatchers.IO) {
        // Atomic Business Logic: Update Stock and Save Order
        val item = order.itemDescription.let { itemDao.getItemByName(it) }
        if (item != null) {
            item.currentStock -= qtyDelta
            itemDao.updateItem(item)
        }
        
        if (order.id == 0) {
            orderDao.insertOrder(order)
        } else {
            orderDao.updateOrder(order)
        }
    }

    suspend fun deleteOrder(order: Order) = withContext(Dispatchers.IO) {
        orderDao.delete(order)
    }
    
    suspend fun ensurePartyExists(name: String, contact: String) = withContext(Dispatchers.IO) {
        if (partyDao.getPartyByName(name) == null) {
            partyDao.insertParty(Party(
                partyType = "Customer", 
                companyName = name, 
                contactPerson = name, 
                contactNo = contact, 
                address = "", 
                creditLimit = 0.0, 
                creditPeriodDays = 0, 
                notes = "Auto-created"
            ))
        }
    }
}