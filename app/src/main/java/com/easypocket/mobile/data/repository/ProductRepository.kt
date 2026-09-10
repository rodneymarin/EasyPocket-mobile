package com.easypocket.mobile.data.repository

import com.easypocket.mobile.data.local.EasyPocketDatabase
import com.easypocket.mobile.data.local.PriceDao
import com.easypocket.mobile.data.local.PriceEntity
import com.easypocket.mobile.data.local.ProductDao
import com.easypocket.mobile.data.local.ProductEntity
import com.easypocket.mobile.data.local.ProductLastCategoryDao
import com.easypocket.mobile.data.local.ProductLastCategoryEntity
import com.easypocket.mobile.data.local.ShoppingListItemEntity
import com.easypocket.mobile.domain.Alphabet
import com.easypocket.mobile.domain.Price
import com.easypocket.mobile.domain.Product
import com.easypocket.mobile.domain.UnitOfMeasurement
import androidx.room.withTransaction
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

// Everything a product delete takes down (prices, remembered categories and
// the product's rows in shopping lists), kept so it can all be restored.
data class DeletedProducts(
    val products: List<ProductEntity>,
    val prices: List<PriceEntity>,
    val lastCategories: List<ProductLastCategoryEntity>,
    val listItems: List<ShoppingListItemEntity>,
)

@Singleton
class ProductRepository @Inject constructor(
    private val db: EasyPocketDatabase,
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
            )
        }.sortedWith(Alphabet.comparator { it.productName })
    }

    suspend fun getByName(name: String): Product? {
        val entity = productDao.getByName(name) ?: return null
        val prices = priceDao.getAll().first().filter { it.productId == entity.id }
        return Product(
            entity.id,
            entity.productName,
            UnitOfMeasurement.fromRaw(entity.unitOfMeasurement) ?: UnitOfMeasurement.UNIT,
            prices.map { Price(it.storeId, it.value) },
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
    ): Product {
        val id = UUID.randomUUID().toString()
        productDao.insert(ProductEntity(id, name, unit.raw))
        priceDao.insertAll(prices.map { PriceEntity(id, it.storeId, it.value) })
        return Product(id, name, unit, prices)
    }

    suspend fun update(product: Product) {
        productDao.update(ProductEntity(product.id, product.productName, product.unitOfMeasurement.raw))
        priceDao.deleteForProduct(product.id)
        priceDao.insertAll(product.prices.map { PriceEntity(product.id, it.storeId, it.value) })
    }

    suspend fun deleteAll(ids: List<String>): DeletedProducts? {
        if (ids.isEmpty()) return null
        return db.withTransaction {
            val products = productDao.getAll().first().filter { it.id in ids }
            if (products.isEmpty()) return@withTransaction null
            val snapshot = DeletedProducts(
                products = products,
                prices = priceDao.getAll().first().filter { it.productId in ids },
                lastCategories = db.productLastCategoryDao().getByProductIds(ids),
                listItems = db.listDao().getItemsByProductIds(ids),
            )
            db.productLastCategoryDao().deleteForProducts(ids)
            productDao.deleteByIds(ids)
            snapshot
        }
    }

    suspend fun restore(deletion: DeletedProducts) {
        if (deletion.products.isEmpty()) return
        db.withTransaction {
            productDao.insertAll(deletion.products)
            priceDao.insertAll(deletion.prices)
            deletion.lastCategories.forEach { db.productLastCategoryDao().upsert(it) }
            if (deletion.listItems.isNotEmpty()) db.listDao().insertItems(deletion.listItems)
        }
    }
}