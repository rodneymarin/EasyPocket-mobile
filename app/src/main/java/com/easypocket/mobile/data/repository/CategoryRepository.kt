package com.easypocket.mobile.data.repository

import androidx.room.withTransaction
import com.easypocket.mobile.data.local.CategoryDao
import com.easypocket.mobile.data.local.CategoryEntity
import com.easypocket.mobile.data.local.EasyPocketDatabase
import com.easypocket.mobile.data.local.ProductLastCategoryDao
import com.easypocket.mobile.data.local.ShoppingListDao
import com.easypocket.mobile.domain.Alphabet
import com.easypocket.mobile.domain.Category
import com.easypocket.mobile.domain.ListIcon
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

@Singleton
class CategoryRepository @Inject constructor(
    private val db: EasyPocketDatabase,
    private val categoryDao: CategoryDao,
    private val listDao: ShoppingListDao,
    private val lastCategoryDao: ProductLastCategoryDao,
) {

    fun observeAll(): Flow<List<Category>> =
        categoryDao.getAll().map { list -> list.map { it.toDomain() }.sortedWith(Alphabet.comparator { it.name }) }

    suspend fun getAll(): List<Category> = observeAll().first()

    suspend fun findDuplicate(name: String, excludeId: String?): Category? {
        val found = categoryDao.getByName(name) ?: return null
        return if (found.id == excludeId) null else found.toDomain()
    }

    suspend fun create(name: String, icon: String = ListIcon.DEFAULT): Category {
        val id = generateUniqueCode()
        val safeIcon = icon.ifBlank { ListIcon.DEFAULT }
        categoryDao.insert(CategoryEntity(id, name, safeIcon))
        return Category(id, name, safeIcon)
    }

    suspend fun update(category: Category) =
        categoryDao.update(CategoryEntity(category.id, category.name, category.icon))

    suspend fun deleteAll(ids: List<String>) {
        db.withTransaction {
            listDao.clearCategory(ids)
            listDao.clearListCategory(ids)
            lastCategoryDao.deleteForCategories(ids)
            categoryDao.deleteByIds(ids)
        }
    }

    private suspend fun generateUniqueCode(): String {
        val existing = categoryDao.getAllIds()
        while (true) {
            val code = randomCode()
            if (code !in existing) return code
        }
    }

    private fun CategoryEntity.toDomain() = Category(id, name, icon)

    companion object {
        private const val CODE_LENGTH = 6
        private const val CHARSET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"

        private fun randomCode(): String = buildString {
            repeat(CODE_LENGTH) { append(CHARSET[Random.nextInt(CHARSET.length)]) }
        }
    }
}
