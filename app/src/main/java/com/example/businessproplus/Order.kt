package com.example.businessproplus

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "orders_table",
    indices = [Index(value = ["customerName"]), Index(value = ["orderDate"]), Index(value = ["status"])]
)
data class Order(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    var manualOrderNo: String = "",
    val customerName: String,
    val contactNumber: String,
    val itemDescription: String,
    val quantity: Int,
    val price: Double,
    val total: Double,
    val advancePayment: Double,
    val remainingPayment: Double,
    val orderDate: String,
    val deliveryDate: String,

    // STATUS: Pending, Working, Completed, Cancelled, Delayed, Delivered
    var status: String = "Pending",
    var processedOn: String = "",
    var heaterType: String = "",
    var blockSize: String = "",
    var holesOrCut: String = "",
    var connectionType: String = "",
    var voltage: String = "",
    var ampere: String = "",
    var ohms: String = "",
    var stripeOrWire: String = "",
    var turns: String = "",
    var finalAmpere: String = "",
    var completedOn: String = "",
    var deliveredOn: String = "", // NEW: Delivered On Date

    var photoPath: String? = null,
    var videoPath: String? = null,
    var audioPath: String? = null,
    var remarks: String = "",
    
    var isDeleted: Boolean = false,
    var archived: Boolean = false,
    
    // NEW: Auditing
    var processedBy: String = "",

    // 💰 NEW: Payment Tracking
    var isPaid: Boolean = false,
    var dueAmount: Double = 0.0
)