package com.easypocket.mobile.data.backup

import android.content.ContentResolver
import android.net.Uri
import androidx.room.withTransaction
import com.easypocket.mobile.data.local.EasyPocketDatabase
import com.easypocket.mobile.data.local.PriceEntity
import com.easypocket.mobile.data.local.ProductEntity
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
        val products = db.productDao().getAll().first()
        val prices = db.priceDao().getAll().first()
        val lists = db.listDao().getAll().first()
        val backup = BackupData(
            version = SUPPORTED_VERSION,
            exportedAt = Instant.now().toString(),
            stores = stores.map { BackupStore(it.id, it.description, it.color) },
            products = products.map { BackupProduct(it.id, it.productName, it.unitOfMeasurement) },
            prices = prices.map { BackupPrice(it.productId, it.storeId, it.value) },
            shoppingLists = lists.map { BackupList(it.list.id, it.list.title) },
            listItems = lists.flatMap { it.items.map { i ->
                BackupListItem(i.id, i.shoppingListId, i.productId, i.storeId, i.quantity, i.done, i.pinned)
            } },
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
            backup.products.forEach { db.productDao().insert(ProductEntity(it.id, it.productName, it.unitOfMeasurement)) }
            if (backup.prices.isNotEmpty()) {
                db.priceDao().insertAll(backup.prices.map { PriceEntity(it.productId, it.storeId, it.value) })
            }
            backup.shoppingLists.forEach { db.listDao().insert(ShoppingListEntity(it.id, it.title)) }
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
    }
}
