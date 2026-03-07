package com.example.businessproplus.di

import android.content.Context
import com.example.businessproplus.AppDatabase
import com.example.businessproplus.ItemDao
import com.example.businessproplus.OrderDao
import com.example.businessproplus.PartyDao
import com.example.businessproplus.UserDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    @Provides
    fun provideOrderDao(database: AppDatabase): OrderDao = database.orderDao()

    @Provides
    fun provideItemDao(database: AppDatabase): ItemDao = database.itemDao()

    @Provides
    fun providePartyDao(database: AppDatabase): PartyDao = database.partyDao()

    @Provides
    fun provideUserDao(database: AppDatabase): UserDao = database.userDao()
}