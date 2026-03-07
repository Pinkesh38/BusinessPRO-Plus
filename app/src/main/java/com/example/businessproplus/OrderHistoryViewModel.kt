package com.example.businessproplus

import androidx.lifecycle.*
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class OrderHistoryViewModel @Inject constructor(
    private val repository: OrderRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _statusFilter = MutableStateFlow("All")
    private val _sortType = MutableStateFlow(0)

    val orders: Flow<PagingData<Order>> = combine(_searchQuery, _statusFilter, _sortType) { query, status, sort ->
        Triple(query, status, sort)
    }.flatMapLatest { (query, status, sort) ->
        repository.getFilteredOrdersPaged(query, status, sort)
    }.cachedIn(viewModelScope)

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setStatusFilter(status: String) {
        _statusFilter.value = status
    }

    fun setSortType(sortType: Int) {
        _sortType.value = sortType
    }

    fun deleteOrder(order: Order) = viewModelScope.launch {
        repository.deleteOrderWithStockRestore(order.id)
    }
}
