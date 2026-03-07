package com.example.businessproplus

data class ReportItem(
    val name: String,
    val count: Int
)

data class TrendItem(
    val orderDate: String,
    val dailyTotal: Double
)

data class ReportData(
    val revenue: Double,
    val orderCount: Int,
    val totalPending: Double,
    val growth: Double,
    val topItems: List<ReportItem>,
    val trends: List<TrendItem>
)
