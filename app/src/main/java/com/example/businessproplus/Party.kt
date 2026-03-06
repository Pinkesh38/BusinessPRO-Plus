package com.example.businessproplus

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "party_table",
    indices = [Index(value = ["companyName"], unique = true), Index(value = ["partyType"])]
)
data class Party(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val partyType: String,
    var companyName: String,
    var contactPerson: String,
    var contactNo: String,
    var contactNo2: String = "",
    var address: String,
    var gstNo: String = "",
    var creditLimit: Double,
    var creditPeriodDays: Int,
    var notes: String,
    var isBlacklisted: Boolean = false,
    var lastOrderDate: String = "",
    var isDeleted: Boolean = false // Added for Soft Delete
)