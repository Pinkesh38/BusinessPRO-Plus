package com.example.businessproplus

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AddItemViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    
    private val _categories = MutableLiveData<List<String>>()
    val categories: LiveData<List<String>> = _categories

    private val _itemSavedEvent = MutableLiveData<Boolean>()
    val itemSavedEvent: LiveData<Boolean> = _itemSavedEvent

    private val _existingItem = MutableLiveData<Item?>()
    val existingItem: LiveData<Item?> = _existingItem

    fun loadCategories() {
        viewModelScope.launch(Dispatchers.IO) {
            val categoryList = db.categoryDao().getAllCategories().map { it.categoryName }
            _categories.postValue(categoryList)
        }
    }

    fun loadItem(itemId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val item = db.itemDao().getItemById(itemId)
            _existingItem.postValue(item)
        }
    }

    fun saveItem(
        id: Int = 0,
        name: String,
        unit: String,
        price: Double,
        discount: Double,
        category: String,
        description: String,
        stock: Int,
        minStock: Int,
        photoPath: String?,
        videoPath: String?
    ) {
        val item = Item(
            id = id,
            itemName = name,
            unit = unit,
            salesPrice = price,
            discountPercent = discount,
            category = category,
            itemDescription = description,
            currentStock = stock,
            minStockLevel = minStock,
            photoPath = photoPath,
            videoPath = videoPath
        )
        viewModelScope.launch(Dispatchers.IO) {
            if (id == 0) {
                db.itemDao().insertItem(item)
            } else {
                db.itemDao().updateItem(item)
            }
            _itemSavedEvent.postValue(true)
        }
    }
    
    fun resetSavedEvent() {
        _itemSavedEvent.value = false
    }
}
