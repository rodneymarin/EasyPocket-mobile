package com.easypocket.mobile.data.repository

import com.easypocket.mobile.data.local.PriceDao
import com.easypocket.mobile.data.local.PriceEntity
import com.easypocket.mobile.data.local.ProductDao
import com.easypocket.mobile.data.local.ProductEntity
import com.easypocket.mobile.domain.Price
import com.easypocket.mobile.domain.Product
import com.easypocket.mobile.domain.UnitOfMeasurement
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

@Singleton
class ProductRepository @Inject constructor(
    private val productDao: ProductDao,
    private val priceDao: PriceDao,
) {
    suspend fun getAll(): List<Product> {
        val products = productDao.getAll().first()
        val prices = priceDao.getAll().first().groupBy { it.productId }
        return products.map { p ->
            Product(
                p.id,
                p.productName,
                UnitOfMeasurement.fromRaw(p.unitOfMeasurement) ?: UnitOfMeasurement.UNIT,
                prices[p.id].orEmpty().map { Price(it.storeId, it.value) },
                p.categoryId,
            )
        }
    }

    suspend fun getByName(name: String): Product? {
        val entity = productDao.getByName(name) ?: return null
        val prices = priceDao.getAll().first().filter { it.productId == entity.id }
        return Product(
            entity.id,
            entity.productName,
            UnitOfMeasurement.fromRaw(entity.unitOfMeasurement) ?: UnitOfMeasurement.UNIT,
            prices.map { Price(it.storeId, it.value) },
            entity.categoryId,
        )
    }

    suspend fun findDuplicate(name: String, excludeId: String?): Product? {
        val found = productDao.getByName(name) ?: return null
        return if (found.id == excludeId) null else getByName(found.productName)
    }

    suspend fun create(
        name: String,
        unit: UnitOfMeasurement,
        prices: List<Price> = emptyList(),
        categoryId: String? = null,
    ): Product {
        val id = UUID.randomUUID().toString()
        productDao.insert(ProductEntity(id, name, unit.raw, categoryId))
        priceDao.insertAll(prices.map { PriceEntity(id, it.storeId, it.value) })
        return Product(id, name, unit, prices, categoryId)
    }

    suspend fun update(product: Product) {
        productDao.update(ProductEntity(product.id, product.productName, product.unitOfMeasurement.raw, product.categoryId))
        priceDao.deleteForProduct(product.id)
        priceDao.insertAll(product.prices.map { PriceEntity(product.id, it.storeId, it.value) })
    }

    suspend fun deleteAll(ids: List<String>) = productDao.deleteByIds(ids)
}