package com.example.businessproplus.di

import com.example.businessproplus.ItemDao
import com.example.businessproplus.OrderDao
import com.example.businessproplus.OrderRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideOrderRepository(orderDao: OrderDao, itemDao: ItemDao): OrderRepository {
        return OrderRepository(orderDao, itemDao)
    }
}