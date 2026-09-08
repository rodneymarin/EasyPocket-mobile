package com.easypocket.mobile.data.repository

import com.easypocket.mobile.data.local.ProductLastCategoryDao
import com.easypocket.mobile.data.local.ProductLastCategoryEntity
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

@Singleton
class ProductLastCategoryRepository @Inject constructor(
    private val lastCategoryDao: ProductLastCategoryDao,
) {

    suspend fun getFor(productId: String): String? =
        lastCategoryDao.getByProductId(productId)?.categoryId

    suspend fun set(productId: String, categoryId: String?) {
        if (categoryId == null) {
            lastCategoryDao.deleteForProduct(productId)
        } else {
            lastCategoryDao.upsert(ProductLastCategoryEntity(productId, categoryId))
        }
    }

    suspend fun getAll(): List<ProductLastCategoryEntity> = lastCategoryDao.getAll().first()
}
