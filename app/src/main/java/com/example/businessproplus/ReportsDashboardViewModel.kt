package com.example.businessproplus

import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReportsDashboardViewModel @Inject constructor(
    private val orderDao: OrderDao
) : ViewModel() {

    private val _uiState = MutableStateFlow<ReportUiState>(ReportUiState.Loading)
    val uiState: StateFlow<ReportUiState> = _uiState

    private var loadJob: Job? = null

    fun loadData(startStr: String, endStr: String, prevStartStr: String, prevEndStr: String) {
        loadJob?.cancel()
        
        loadJob = viewModelScope.launch {
            _uiState.value = ReportUiState.Loading
            
            try {
                val data = coroutineScope {
                    val revenueDeferred = async { orderDao.getRevenueForRange(startStr, endStr) }
                    val countDeferred = async { orderDao.getOrderCountForRange(startStr, endStr) }
                    val pendingDeferred = async { orderDao.getTotalToCollect() }
                    val prevRevenueDeferred = async { orderDao.getRevenueForRange(prevStartStr, prevEndStr) }
                    val topItemsDeferred = async { orderDao.getTopSellingItems(5) }
                    val trendsDeferred = async { orderDao.getDailyRevenueTrend(startStr) }

                    val revenue = revenueDeferred.await() ?: 0.0
                    val orderCount = countDeferred.await()
                    val totalPending = pendingDeferred.await() ?: 0.0
                    val prevRevenue = prevRevenueDeferred.await() ?: 0.0
                    val growth = if (prevRevenue > 0) ((revenue - prevRevenue) / prevRevenue) * 100 else 0.0

                    ReportData(
                        revenue = revenue,
                        orderCount = orderCount,
                        totalPending = totalPending,
                        growth = growth,
                        topItems = topItemsDeferred.await(),
                        trends = trendsDeferred.await()
                    )
                }

                _uiState.value = ReportUiState.Success(data)
            } catch (e: Exception) {
                if (e !is kotlinx.coroutines.CancellationException) {
                    _uiState.value = ReportUiState.Error(e.message ?: "Unknown Error")
                }
            }
        }
    }
}

sealed class ReportUiState {
    object Loading : ReportUiState()
    data class Success(val data: ReportData) : ReportUiState()
    data class Error(val message: String) : ReportUiState()
}
