package com.easypocket.mobile.data.backup

import android.content.ContentResolver
import android.net.Uri
import androidx.room.withTransaction
import com.easypocket.mobile.data.local.EasyPocketDatabase
import com.easypocket.mobile.data.local.CategoryEntity
import com.easypocket.mobile.data.local.PriceEntity
import com.easypocket.mobile.data.local.ProductEntity
import com.easypocket.mobile.data.local.PurchaseHistoryEntity
import com.easypocket.mobile.data.local.PurchaseHistoryItemEntity
import com.easypocket.mobile.data.local.ShoppingListItemEntity
import com.easypocket.mobile.data.local.ShoppingListEntity
import com.easypocket.mobile.data.local.StoreEntity
import com.easypocket.mobile.domain.UnitOfMeasurement
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json

@Singleton
class BackupManager @Inject constructor(private val db: EasyPocketDatabase) {

    companion object {
        private const val SUPPORTED_VERSION = 1
        private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }
    }

    suspend fun exportToString(): String {
        val stores = db.storeDao().getAll().first()
        val categories = db.categoryDao().getAll().first()
        val products = db.productDao().getAll().first()
        val prices = db.priceDao().getAll().first()
        val lists = db.listDao().getAll().first()
        val history = db.purchaseHistoryDao().observeAll().first()
        val backup = BackupData(
            version = SUPPORTED_VERSION,
            exportedAt = Instant.now().toString(),
            stores = stores.map { BackupStore(it.id, it.description, it.color) },
            categories = categories.map { BackupCategory(it.id, it.name) },
            products = products.map { BackupProduct(it.id, it.productName, it.unitOfMeasurement, it.categoryId) },
            prices = prices.map { BackupPrice(it.productId, it.storeId, it.value) },
            shoppingLists = lists.map { BackupList(it.list.id, it.list.title, it.list.icon) },
            listItems = lists.flatMap { it.items.map { i ->
                BackupListItem(i.id, i.shoppingListId, i.productId, i.storeId, i.quantity, i.done, i.pinned)
            } },
            purchaseHistory = history.map { BackupPurchaseHistory(
                it.record.id, it.record.listTitle, it.record.listIcon,
                it.record.date, it.record.totalAmount, it.record.itemCount,
            ) },
            purchaseHistoryItems = history.flatMap { h ->
                h.items.map { i ->
                    BackupPurchaseHistoryItem(
                        h.record.id, i.productName, i.storeName,
                        i.quantity, i.unitPrice, i.totalPrice, i.categoryCode,
                    )
                }
            },
        )
        return json.encodeToString(BackupData.serializer(), backup)
    }

    fun parse(jsonText: String): Result<BackupData> = runCatching {
        val backup = json.decodeFromString(BackupData.serializer(), jsonText)
        validate(backup)
        backup
    }

    suspend fun exportTo(uri: Uri, contentResolver: ContentResolver): Result<Unit> = runCatching {
        val output = contentResolver.openOutputStream(uri)
            ?: throw IllegalStateException("Could not open output stream for $uri")
        output.use { it.write(exportToString().toByteArray()) }
    }

    suspend fun parse(uri: Uri, contentResolver: ContentResolver): Result<BackupData> = runCatching {
        val input = contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("Could not open input stream for $uri")
        val text = input.use { it.readBytes().decodeToString() }
        parse(text).getOrThrow()
    }

    suspend fun importFrom(uri: Uri, contentResolver: ContentResolver): Result<Unit> = runCatching {
        val backup = parse(uri, contentResolver).getOrThrow()
        importData(backup)
    }

    suspend fun importData(backup: BackupData) {
        db.withTransaction {
            db.clearAllTables()
            backup.stores.forEach { db.storeDao().insert(StoreEntity(it.id, it.description, it.color)) }
            backup.categories.forEach { db.categoryDao().insert(CategoryEntity(it.id, it.name)) }
            backup.products.forEach { db.productDao().insert(ProductEntity(it.id, it.productName, it.unitOfMeasurement, it.categoryId)) }
            if (backup.prices.isNotEmpty()) {
                db.priceDao().insertAll(backup.prices.map { PriceEntity(it.productId, it.storeId, it.value) })
            }
            backup.shoppingLists.forEach { db.listDao().insert(ShoppingListEntity(it.id, it.title, it.icon)) }
            backup.listItems.forEach { item ->
                db.listDao().insertItem(
                    ShoppingListItemEntity(
                        id = item.id,
                        shoppingListId = item.shoppingListId,
                        productId = item.productId,
                        storeId = item.storeId,
                        quantity = item.quantity,
                        done = item.done,
                        pinned = item.pinned,
                    )
                )
            }
            backup.purchaseHistory.forEach { h ->
                db.purchaseHistoryDao().insertHistory(
                    PurchaseHistoryEntity(h.id, h.listTitle, h.listIcon, h.date, h.totalAmount, h.itemCount)
                )
            }
            if (backup.purchaseHistoryItems.isNotEmpty()) {
                db.purchaseHistoryDao().insertItems(backup.purchaseHistoryItems.map { i ->
                    PurchaseHistoryItemEntity(
                        historyId = i.historyId,
                        productName = i.productName,
                        storeName = i.storeName,
                        quantity = i.quantity,
                        unitPrice = i.unitPrice,
                        totalPrice = i.totalPrice,
                        categoryCode = i.categoryCode,
                    )
                })
            }
        }
    }

    private fun validate(backup: BackupData) {
        require(backup.version == SUPPORTED_VERSION) { "Unsupported backup version: ${backup.version}" }
        val storeIds = backup.stores.map { it.id }
        val productIds = backup.products.map { it.id }
        val listIds = backup.shoppingLists.map { it.id }
        require(storeIds.size == storeIds.toSet().size) { "Duplicate store ids in backup" }
        require(productIds.size == productIds.toSet().size) { "Duplicate product ids in backup" }
        require(listIds.size == listIds.toSet().size) { "Duplicate shopping list ids in backup" }
        backup.products.forEach {
            require(UnitOfMeasurement.fromRaw(it.unitOfMeasurement) != null) {
                "Unknown unit of measurement: ${it.unitOfMeasurement}"
            }
        }
        backup.prices.forEach {
            require(it.productId in productIds) { "Price references unknown product: ${it.productId}" }
            require(it.storeId in storeIds) { "Price references unknown store: ${it.storeId}" }
        }
        val itemIds = backup.listItems.map { it.id }
        require(itemIds.size == itemIds.toSet().size) { "Duplicate list item ids in backup" }
        backup.listItems.forEach { item ->
            require(item.shoppingListId in listIds) { "List item references unknown list: ${item.shoppingListId}" }
            require(item.productId in productIds) { "List item references unknown product: ${item.productId}" }
            require(item.storeId == null || item.storeId in storeIds) { "List item references unknown store: ${item.storeId}" }
        }
        val historyIds = backup.purchaseHistory.map { it.id }
        require(historyIds.size == historyIds.toSet().size) { "Duplicate history ids in backup" }
        backup.purchaseHistoryItems.forEach { item ->
            require(item.historyId in historyIds) { "History item references unknown history: ${item.historyId}" }
        }
    }
}
