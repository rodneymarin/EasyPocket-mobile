package com.easypocket.mobile.di

import android.content.Context
import androidx.room.Room
import com.easypocket.mobile.data.local.CategoryDao
import com.easypocket.mobile.data.local.EasyPocketDatabase
import com.easypocket.mobile.data.local.PriceDao
import com.easypocket.mobile.data.local.ProductDao
import com.easypocket.mobile.data.local.PurchaseHistoryDao
import com.easypocket.mobile.data.local.ShoppingListDao
import com.easypocket.mobile.data.local.StoreDao
import com.easypocket.mobile.settings.SettingsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): EasyPocketDatabase =
        Room.databaseBuilder(context, EasyPocketDatabase::class.java, "easypocket.db")
            .addMigrations(
                EasyPocketDatabase.MIGRATION_1_2,
                EasyPocketDatabase.MIGRATION_2_3,
                EasyPocketDatabase.MIGRATION_3_4,
                EasyPocketDatabase.MIGRATION_4_5,
            )
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideStoreDao(db: EasyPocketDatabase): StoreDao = db.storeDao()

    @Provides
    fun provideCategoryDao(db: EasyPocketDatabase): CategoryDao = db.categoryDao()

    @Provides
    fun provideProductDao(db: EasyPocketDatabase): ProductDao = db.productDao()

    @Provides
    fun providePriceDao(db: EasyPocketDatabase): PriceDao = db.priceDao()

    @Provides
    fun provideListDao(db: EasyPocketDatabase): ShoppingListDao = db.listDao()

    @Provides
    fun providePurchaseHistoryDao(db: EasyPocketDatabase): PurchaseHistoryDao = db.purchaseHistoryDao()

    @Provides
    @Singleton
    fun provideSettingsRepository(@ApplicationContext context: Context): SettingsRepository =
        SettingsRepository(context)
}