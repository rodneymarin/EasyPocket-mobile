package com.easypocket.mobile.data.repository

import androidx.room.withTransaction
import com.easypocket.mobile.data.local.CategoryDao
import com.easypocket.mobile.data.local.CategoryEntity
import com.easypocket.mobile.data.local.EasyPocketDatabase
import com.easypocket.mobile.data.local.ProductLastCategoryDao
import com.easypocket.mobile.data.local.ProductLastCategoryEntity
import com.easypocket.mobile.data.local.ShoppingListDao
import com.easypocket.mobile.data.local.ShoppingListEntity
import com.easypocket.mobile.domain.Alphabet
import com.easypocket.mobile.domain.Category
import com.easypocket.mobile.domain.ListIcon
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

// A category delete un-links items and lists from it and drops the
// remembered product categories; this snapshot restores all of it.
data class DeletedCategories(
    val categories: List<CategoryEntity>,
    val itemIdsByCategory: Map<String, List<Long>>,
    val listIdsByCategory: Map<String, List<String>>,
    val lastCategories: List<ProductLastCategoryEntity>,
)

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

    suspend fun deleteAll(ids: List<String>): DeletedCategories? {
        if (ids.isEmpty()) return null
        return db.withTransaction {
            val categories = categoryDao.getAll().first().filter { it.id in ids }
            if (categories.isEmpty()) return@withTransaction null
            val snapshot = DeletedCategories(
                categories = categories,
                itemIdsByCategory = listDao.getItemsByCategoryIds(ids)
                    .filter { it.categoryId in ids }
                    .groupBy({ it.categoryId!! }, { it.id }),
                listIdsByCategory = listDao.getListsByCategoryIds(ids)
                    .filter { it.categoryId in ids }
                    .groupBy({ it.categoryId!! }, { it.id }),
                lastCategories = lastCategoryDao.getByCategoryIds(ids),
            )
            listDao.clearCategory(ids)
            listDao.clearListCategory(ids)
            lastCategoryDao.deleteForCategories(ids)
            categoryDao.deleteByIds(ids)
            snapshot
        }
    }

    suspend fun restore(deletion: DeletedCategories) {
        if (deletion.categories.isEmpty()) return
        db.withTransaction {
            deletion.categories.forEach { categoryDao.insert(it) }
            deletion.itemIdsByCategory.forEach { (categoryId, ids) ->
                listDao.setItemsCategoryByIds(ids, categoryId)
            }
            deletion.listIdsByCategory.forEach { (categoryId, ids) ->
                ids.forEach { listDao.updateCategory(it, categoryId) }
            }
            deletion.lastCategories.forEach { lastCategoryDao.upsert(it) }
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
