# Historial de Compras — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Agregar historial de compras: acción "Guardar en historial" en el detalle de lista que archiva los items completados como snapshot, y una 4ª pestaña "History" con lista cronológica de registros y gráfico de área con filtros de rango de fechas.

**Architecture:** Nueva tabla Room `purchase_history` + `purchase_history_items` (snapshot congelado, sin referencias vivas), migración 2→3. Nuevo `PurchaseHistoryRepository` con `archiveCompleted()` transaccional. Nueva feature `ui/history/` (ViewModel + Screen + Chart) integrada como 4ª página del pager del home. Los montos se congelan al momento del archivo usando la misma regla de precios de `ListLogic` (precio de la tienda asignada del item; 0.0 si no hay tienda/precio).

**Tech Stack:** Kotlin 2.0.21, Jetpack Compose (BOM 2024.12.01, Material3), Room 2.6.1 (room-ktx disponible → `db.withTransaction`), Hilt 2.52, Navigation-Compose, Robolectric + JUnit4 para tests, `java.time` (minSdk 26).

**Spec:** `docs/superpowers/specs/2026-08-29-purchase-history-design.md`

## Global Constraints

- **Sin dependencias nuevas** — el gráfico se dibuja con Canvas de Compose.
- **Snapshot congelado:** el historial guarda copias de nombres/precios, nunca IDs de producto/tienda.
- **Patrón MVVM existente:** ViewModels `@HiltViewModel` con `UiState` inmutable expuesto por `StateFlow`; UI recolecta con `collectAsStateWithLifecycle()`.
- **i18n manual:** todo texto de UI vía `t(key, language)` con claves en ambos diccionarios (EN y ES) de `i18n/Translations.kt`.
- **Estilo UI:** usar componentes existentes (`AppHeader`, `AppItemList`, `ListItemRow`, `AppBottomSheet`, `ConfirmSheet`, `ListIconCircle`, `LocalAppColors`) y `formatAmount` estilo `String.format(Locale.US, "%.2f", value)`.
- **Montos:** `unitPrice` = precio del producto en la tienda asignada del item (misma regla que `ListLogic.itemTotal`); 0.0 si el item no tiene tienda o no hay precio.
- **Commits:** solo los archivos que cada tarea toca (el working tree tiene cambios no relacionados del usuario). Mensajes convencionales en español.
- **Tests:** Robolectric + Room in-memory (patrón de `ShoppingListRepositoryTest` / `ListListViewModelTest`); comando: `./gradlew :app:testDebugUnitTest --tests "<FQCN>"` desde `/home/rodney/Documents/Dev/EasyPocket-mobile`.

---

### Task 1: Capa de datos — entidades, relación, DAO, migración 2→3

**Files:**
- Modify: `app/src/main/java/com/easypocket/mobile/data/local/Entities.kt` (append al final)
- Modify: `app/src/main/java/com/easypocket/mobile/data/local/Relations.kt` (append al final)
- Modify: `app/src/main/java/com/easypocket/mobile/data/local/Daos.kt` (append al final)
- Modify: `app/src/main/java/com/easypocket/mobile/data/local/EasyPocketDatabase.kt`
- Modify: `app/src/main/java/com/easypocket/mobile/di/AppModule.kt`
- Test: `app/src/test/java/com/easypocket/mobile/data/local/DatabaseTest.kt`

**Interfaces:**
- Consumes: nada (primer task).
- Produces: `PurchaseHistoryEntity`, `PurchaseHistoryItemEntity`, `PurchaseHistoryWithItems(record, items)`, `PurchaseHistoryDao.observeAll(): Flow<List<PurchaseHistoryWithItems>>`, `PurchaseHistoryDao.insertHistory(history)`, `PurchaseHistoryDao.insertItems(items)`, `EasyPocketDatabase.purchaseHistoryDao()`, `EasyPocketDatabase.MIGRATION_2_3`.

- [ ] **Step 1: Escribir los tests que fallan** — agregar al final de `DatabaseTest.kt` (antes de la llave de cierre de la clase):

```kotlin
@Test
fun `migration 2 to 3 creates history tables`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val helper = FrameworkSQLiteOpenHelperFactory().create(
        SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(null)
            .callback(object : SupportSQLiteOpenHelper.Callback(2) {
                override fun onCreate(db: SupportSQLiteDatabase) {}
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()
    )
    val db = helper.writableDatabase

    EasyPocketDatabase.MIGRATION_2_3.migrate(db)

    db.execSQL(
        "INSERT INTO purchase_history (id, list_title, list_icon, date, total_amount, item_count) " +
            "VALUES ('h1', 'Lista', '$', 0, 20.0, 2)"
    )
    db.execSQL(
        "INSERT INTO purchase_history_items (history_id, product_name, store_name, quantity, unit_price, total_price) " +
            "VALUES ('h1', 'Milk', 'Store 1', 2.0, 10.0, 20.0)"
    )
    db.query("SELECT list_title FROM purchase_history WHERE id = 'h1'").use { cursor ->
        assertTrue(cursor.moveToFirst())
        assertEquals("Lista", cursor.getString(0))
    }
    helper.close()
}

@Test
fun `delete history record cascades items`() = runTest {
    db.purchaseHistoryDao().insertHistory(
        PurchaseHistoryEntity("h1", "Lista", "$", 0L, 20.0, 1)
    )
    db.purchaseHistoryDao().insertItems(
        listOf(
            PurchaseHistoryItemEntity(
                historyId = "h1", productName = "Milk", storeName = "Store 1",
                quantity = 2.0, unitPrice = 10.0, totalPrice = 20.0,
            )
        )
    )

    db.openHelper.writableDatabase.execSQL("DELETE FROM purchase_history WHERE id = 'h1'")

    assertTrue(db.purchaseHistoryDao().observeAll().first().isEmpty())
}
```

Nota: los imports ya existentes en `DatabaseTest.kt` cubren todo lo necesario.

- [ ] **Step 2: Verificar que los tests fallan (compilación)**

Run: `./gradlew :app:testDebugUnitTest --tests "com.easypocket.mobile.data.local.DatabaseTest"`
Expected: FAIL — `MIGRATION_2_3`, `purchaseHistoryDao()`, `PurchaseHistoryEntity` no existen.

- [ ] **Step 3: Implementar entidades y relación**

En `Entities.kt`, append al final:

```kotlin
@Entity(tableName = "purchase_history")
data class PurchaseHistoryEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "list_title") val listTitle: String,
    @ColumnInfo(name = "list_icon") val listIcon: String,
    val date: Long,
    @ColumnInfo(name = "total_amount") val totalAmount: Double,
    @ColumnInfo(name = "item_count") val itemCount: Int,
)

@Entity(
    tableName = "purchase_history_items",
    foreignKeys = [
        ForeignKey(entity = PurchaseHistoryEntity::class, parentColumns = ["id"],
            childColumns = ["history_id"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("history_id")],
)
data class PurchaseHistoryItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "history_id") val historyId: String,
    @ColumnInfo(name = "product_name") val productName: String,
    @ColumnInfo(name = "store_name") val storeName: String?,
    val quantity: Double,
    @ColumnInfo(name = "unit_price") val unitPrice: Double,
    @ColumnInfo(name = "total_price") val totalPrice: Double,
)
```

En `Relations.kt`, append al final:

```kotlin
data class PurchaseHistoryWithItems(
    @Embedded val record: PurchaseHistoryEntity,
    @Relation(parentColumn = "id", entityColumn = "history_id")
    val items: List<PurchaseHistoryItemEntity>,
)
```

- [ ] **Step 4: Implementar DAO**

En `Daos.kt`, append al final:

```kotlin
@Dao
interface PurchaseHistoryDao {
    @Transaction
    @Query("SELECT * FROM purchase_history ORDER BY date DESC")
    fun observeAll(): Flow<List<PurchaseHistoryWithItems>>

    @Insert
    suspend fun insertHistory(history: PurchaseHistoryEntity)

    @Insert
    suspend fun insertItems(items: List<PurchaseHistoryItemEntity>)
}
```

- [ ] **Step 5: DB versión 3 + migración**

En `EasyPocketDatabase.kt`, agregar las entidades a la lista, subir versión, agregar `purchaseHistoryDao()` y la migración. El archivo queda:

```kotlin
@Database(
    entities = [
        StoreEntity::class, ProductEntity::class, PriceEntity::class,
        ShoppingListEntity::class, ShoppingListItemEntity::class,
        PurchaseHistoryEntity::class, PurchaseHistoryItemEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
abstract class EasyPocketDatabase : RoomDatabase() {
    abstract fun storeDao(): StoreDao
    abstract fun productDao(): ProductDao
    abstract fun priceDao(): PriceDao
    abstract fun listDao(): ShoppingListDao
    abstract fun purchaseHistoryDao(): PurchaseHistoryDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE shopping_lists ADD COLUMN icon TEXT NOT NULL DEFAULT '$'")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `purchase_history` (" +
                        "`id` TEXT NOT NULL PRIMARY KEY, " +
                        "`list_title` TEXT NOT NULL, " +
                        "`list_icon` TEXT NOT NULL, " +
                        "`date` INTEGER NOT NULL, " +
                        "`total_amount` REAL NOT NULL, " +
                        "`item_count` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `purchase_history_items` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`history_id` TEXT NOT NULL, " +
                        "`product_name` TEXT NOT NULL, " +
                        "`store_name` TEXT, " +
                        "`quantity` REAL NOT NULL, " +
                        "`unit_price` REAL NOT NULL, " +
                        "`total_price` REAL NOT NULL, " +
                        "FOREIGN KEY(`history_id`) REFERENCES `purchase_history`(`id`) " +
                        "ON UPDATE NO ACTION ON DELETE CASCADE)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_purchase_history_items_history_id` " +
                        "ON `purchase_history_items` (`history_id`)"
                )
            }
        }
    }
}
```

En `AppModule.kt`, registrar la migración y proveer el DAO:

```kotlin
Room.databaseBuilder(context, EasyPocketDatabase::class.java, "easypocket.db")
    .addMigrations(EasyPocketDatabase.MIGRATION_1_2, EasyPocketDatabase.MIGRATION_2_3)
    .fallbackToDestructiveMigration()
    .build()
```

```kotlin
@Provides
fun providePurchaseHistoryDao(db: EasyPocketDatabase): PurchaseHistoryDao = db.purchaseHistoryDao()
```

(importar `PurchaseHistoryDao` en AppModule).

- [ ] **Step 6: Verificar que los tests pasan**

Run: `./gradlew :app:testDebugUnitTest --tests "com.easypocket.mobile.data.local.DatabaseTest"`
Expected: PASS (todos, incluidos los 2 nuevos).

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/easypocket/mobile/data/local/Entities.kt app/src/main/java/com/easypocket/mobile/data/local/Relations.kt app/src/main/java/com/easypocket/mobile/data/local/Daos.kt app/src/main/java/com/easypocket/mobile/data/local/EasyPocketDatabase.kt app/src/main/java/com/easypocket/mobile/di/AppModule.kt app/src/test/java/com/easypocket/mobile/data/local/DatabaseTest.kt
git commit -m "feat: agrega tablas y migración de historial de compras"
```

---

### Task 2: PurchaseHistoryRepository con archiveCompleted transaccional

**Files:**
- Create: `app/src/main/java/com/easypocket/mobile/data/repository/PurchaseHistoryRepository.kt`
- Test: `app/src/test/java/com/easypocket/mobile/data/repository/PurchaseHistoryRepositoryTest.kt`

**Interfaces:**
- Consumes: `PurchaseHistoryDao`, `ShoppingListDao.getById(id): ShoppingListWithItems?`, `ShoppingListDao.removeCompleted(listId)`, `ProductDao.getAll()`, `PriceDao.getAll()`, `StoreDao.getAll()`, `db.withTransaction` (room-ktx).
- Produces: `PurchaseHistoryRepository.observeAll(): Flow<List<PurchaseHistoryWithItems>>` y `PurchaseHistoryRepository.archiveCompleted(listId: String, date: Long = System.currentTimeMillis())` — usados por Tasks 4 y 5.

- [ ] **Step 1: Escribir los tests que fallan** — crear `PurchaseHistoryRepositoryTest.kt`:

```kotlin
package com.easypocket.mobile.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.easypocket.mobile.data.local.EasyPocketDatabase
import com.easypocket.mobile.data.local.PriceEntity
import com.easypocket.mobile.data.local.ProductEntity
import com.easypocket.mobile.data.local.ShoppingListEntity
import com.easypocket.mobile.data.local.ShoppingListItemEntity
import com.easypocket.mobile.data.local.StoreEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PurchaseHistoryRepositoryTest {
    private lateinit var db: EasyPocketDatabase
    private lateinit var repo: PurchaseHistoryRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, EasyPocketDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = PurchaseHistoryRepository(
            db = db,
            historyDao = db.purchaseHistoryDao(),
            listDao = db.listDao(),
            productDao = db.productDao(),
            priceDao = db.priceDao(),
            storeDao = db.storeDao(),
        )
    }

    @After
    fun tearDown() { db.close() }

    @Test
    fun `archiveCompleted snapshots done items and keeps pending`() = runTest {
        db.storeDao().insert(StoreEntity("s1", "Store 1", 0))
        db.productDao().insert(ProductEntity("p1", "Milk", "u"))
        db.priceDao().insertAll(listOf(PriceEntity("p1", "s1", 10.0)))
        db.listDao().insert(ShoppingListEntity("l1", "Weekly", "🛒"))
        db.listDao().insertItem(ShoppingListItemEntity(shoppingListId = "l1", productId = "p1", storeId = "s1", quantity = 2.0, done = true))
        db.listDao().insertItem(ShoppingListItemEntity(shoppingListId = "l1", productId = "p1", storeId = null, quantity = 1.0, done = true))
        db.listDao().insertItem(ShoppingListItemEntity(shoppingListId = "l1", productId = "p1", storeId = null, quantity = 3.0, done = false))

        repo.archiveCompleted("l1", date = 1756400000000L)

        val records = repo.observeAll().first()
        assertEquals(1, records.size)
        val record = records.first()
        assertEquals("Weekly", record.record.listTitle)
        assertEquals("🛒", record.record.listIcon)
        assertEquals(1756400000000L, record.record.date)
        assertEquals(2, record.record.itemCount)
        assertEquals(20.0, record.record.totalAmount, 0.001)
        val byName = record.items.associateBy { it.productName }
        val milk = byName.getValue("Milk")
        assertEquals(10.0, milk.unitPrice, 0.001)
        assertEquals(20.0, milk.totalPrice, 0.001)
        assertEquals("Store 1", milk.storeName)
        val unknown = byName.getValue("")
        assertEquals(0.0, unknown.unitPrice, 0.001)
        assertEquals(null, unknown.storeName)

        val remaining = db.listDao().getById("l1")!!.items
        assertEquals(1, remaining.size)
        assertTrue(!remaining.first().done)
    }

    @Test
    fun `archiveCompleted without done items does nothing`() = runTest {
        db.productDao().insert(ProductEntity("p1", "Milk", "u"))
        db.listDao().insert(ShoppingListEntity("l1", "Weekly", "$"))
        db.listDao().insertItem(ShoppingListItemEntity(shoppingListId = "l1", productId = "p1", storeId = null, quantity = 1.0, done = false))

        repo.archiveCompleted("l1")

        assertTrue(repo.observeAll().first().isEmpty())
        assertEquals(1, db.listDao().getById("l1")!!.items.size)
    }
}
```

- [ ] **Step 2: Verificar que los tests fallan (compilación)**

Run: `./gradlew :app:testDebugUnitTest --tests "com.easypocket.mobile.data.repository.PurchaseHistoryRepositoryTest"`
Expected: FAIL — `PurchaseHistoryRepository` no existe.

- [ ] **Step 3: Implementar el repositorio** — crear `PurchaseHistoryRepository.kt`:

```kotlin
package com.easypocket.mobile.data.repository

import androidx.room.withTransaction
import com.easypocket.mobile.data.local.EasyPocketDatabase
import com.easypocket.mobile.data.local.PriceDao
import com.easypocket.mobile.data.local.ProductDao
import com.easypocket.mobile.data.local.PurchaseHistoryDao
import com.easypocket.mobile.data.local.PurchaseHistoryEntity
import com.easypocket.mobile.data.local.PurchaseHistoryItemEntity
import com.easypocket.mobile.data.local.PurchaseHistoryWithItems
import com.easypocket.mobile.data.local.ShoppingListDao
import com.easypocket.mobile.data.local.StoreDao
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

@Singleton
class PurchaseHistoryRepository @Inject constructor(
    private val db: EasyPocketDatabase,
    private val historyDao: PurchaseHistoryDao,
    private val listDao: ShoppingListDao,
    private val productDao: ProductDao,
    private val priceDao: PriceDao,
    private val storeDao: StoreDao,
) {

    fun observeAll(): Flow<List<PurchaseHistoryWithItems>> = historyDao.observeAll()

    suspend fun archiveCompleted(listId: String, date: Long = System.currentTimeMillis()) {
        db.withTransaction {
            val relation = listDao.getById(listId) ?: return@withTransaction
            val doneItems = relation.items.filter { it.done }
            if (doneItems.isEmpty()) return@withTransaction

            val products = productDao.getAll().first().associateBy { it.id }
            val pricesByProduct = priceDao.getAll().first().groupBy { it.productId }
            val stores = storeDao.getAll().first().associateBy { it.id }

            val historyId = UUID.randomUUID().toString()
            val historyItems = doneItems.map { item ->
                val unitPrice = item.storeId
                    ?.let { sid ->
                        pricesByProduct[item.productId].orEmpty().firstOrNull { it.storeId == sid }?.value
                    }
                    ?: 0.0
                PurchaseHistoryItemEntity(
                    historyId = historyId,
                    productName = products[item.productId]?.productName ?: "",
                    storeName = item.storeId?.let { stores[it]?.description },
                    quantity = item.quantity,
                    unitPrice = unitPrice,
                    totalPrice = unitPrice * item.quantity,
                )
            }
            val history = PurchaseHistoryEntity(
                id = historyId,
                listTitle = relation.list.title,
                listIcon = relation.list.icon,
                date = date,
                totalAmount = historyItems.sumOf { it.totalPrice },
                itemCount = historyItems.size,
            )
            historyDao.insertHistory(history)
            historyDao.insertItems(historyItems)
            listDao.removeCompleted(listId)
        }
    }
}
```

- [ ] **Step 4: Verificar que los tests pasan**

Run: `./gradlew :app:testDebugUnitTest --tests "com.easypocket.mobile.data.repository.PurchaseHistoryRepositoryTest"`
Expected: PASS (2 tests).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/easypocket/mobile/data/repository/PurchaseHistoryRepository.kt app/src/test/java/com/easypocket/mobile/data/repository/PurchaseHistoryRepositoryTest.kt
git commit -m "feat: agrega PurchaseHistoryRepository con archiveCompleted transaccional"
```

---

### Task 3: HistoryMath — bucketing de fechas del gráfico (puro)

**Files:**
- Create: `app/src/main/java/com/easypocket/mobile/ui/history/HistoryMath.kt`
- Test: `app/src/test/java/com/easypocket/mobile/ui/history/HistoryMathTest.kt`

**Interfaces:**
- Consumes: `PurchaseHistoryEntity` (Task 1).
- Produces (usados por Tasks 4, 6 y 7):
  - `data class HistoryChartPoint(val date: LocalDate, val total: Double)`
  - `HistoryMath.isInRange(record: PurchaseHistoryEntity, rangeDays: Int?, today: LocalDate): Boolean`
  - `HistoryMath.filterRecords(records: List<PurchaseHistoryEntity>, rangeDays: Int?, today: LocalDate): List<PurchaseHistoryEntity>`
  - `HistoryMath.dailyTotals(records: List<PurchaseHistoryEntity>, rangeDays: Int?, today: LocalDate): List<HistoryChartPoint>`

- [ ] **Step 1: Escribir los tests que fallan** — crear `HistoryMathTest.kt`:

```kotlin
package com.easypocket.mobile.ui.history

import com.easypocket.mobile.data.local.PurchaseHistoryEntity
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class HistoryMathTest {
    private val zone = ZoneId.systemDefault()
    private val today = LocalDate.of(2026, 8, 27)

    private fun entity(date: String, total: Double) = PurchaseHistoryEntity(
        id = date + total.toString(),
        listTitle = "L",
        listIcon = "$",
        date = LocalDate.parse(date).atStartOfDay(zone).plusHours(12).toInstant().toEpochMilli(),
        totalAmount = total,
        itemCount = 1,
    )

    @Test
    fun `dailyTotals fills every day in range with zeros`() {
        val records = listOf(entity("2026-08-27", 50.0), entity("2026-08-27", 20.0), entity("2026-08-25", 30.0))

        val points = HistoryMath.dailyTotals(records, 7, today)

        assertEquals(7, points.size)
        assertEquals(LocalDate.of(2026, 8, 21), points.first().date)
        assertEquals(LocalDate.of(2026, 8, 27), points.last().date)
        assertEquals(30.0, points[4].total, 0.001) // 2026-08-25
        assertEquals(70.0, points[6].total, 0.001) // 2026-08-27
        assertEquals(0.0, points[0].total, 0.001)
    }

    @Test
    fun `dailyTotals with null range returns only days with purchases`() {
        val records = listOf(entity("2026-08-27", 50.0), entity("2026-08-01", 30.0))

        val points = HistoryMath.dailyTotals(records, null, today)

        assertEquals(2, points.size)
        assertEquals(LocalDate.of(2026, 8, 1), points.first().date)
        assertEquals(30.0, points.first().total, 0.001)
        assertEquals(50.0, points.last().total, 0.001)
    }

    @Test
    fun `filterRecords excludes records older than range`() {
        val records = listOf(entity("2026-08-27", 50.0), entity("2026-08-01", 30.0))

        val filtered = HistoryMath.filterRecords(records, 7, today)

        assertEquals(1, filtered.size)
        assertEquals(50.0, filtered.first().totalAmount, 0.001)
    }

    @Test
    fun `isInRange with null range includes everything`() {
        val record = entity("2020-01-01", 1.0)
        assertEquals(true, HistoryMath.isInRange(record, null, today))
        assertEquals(false, HistoryMath.isInRange(record, 7, today))
    }
}
```

- [ ] **Step 2: Verificar que los tests fallan (compilación)**

Run: `./gradlew :app:testDebugUnitTest --tests "com.easypocket.mobile.ui.history.HistoryMathTest"`
Expected: FAIL — `HistoryMath` no existe.

- [ ] **Step 3: Implementar HistoryMath** — crear `HistoryMath.kt`:

```kotlin
package com.easypocket.mobile.ui.history

import com.easypocket.mobile.data.local.PurchaseHistoryEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class HistoryChartPoint(val date: LocalDate, val total: Double)

object HistoryMath {

    private fun rangeStart(rangeDays: Int?, today: LocalDate): LocalDate? =
        rangeDays?.let { today.minusDays((it - 1).toLong()) }

    private fun toMillis(date: LocalDate): Long =
        date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    fun isInRange(record: PurchaseHistoryEntity, rangeDays: Int?, today: LocalDate): Boolean {
        val start = rangeStart(rangeDays, today) ?: return true
        return record.date >= toMillis(start)
    }

    fun filterRecords(
        records: List<PurchaseHistoryEntity>,
        rangeDays: Int?,
        today: LocalDate,
    ): List<PurchaseHistoryEntity> =
        if (rangeDays == null) records else records.filter { isInRange(it, rangeDays, today) }

    fun dailyTotals(
        records: List<PurchaseHistoryEntity>,
        rangeDays: Int?,
        today: LocalDate,
    ): List<HistoryChartPoint> {
        val zone = ZoneId.systemDefault()
        val filtered = filterRecords(records, rangeDays, today)
        val byDay = filtered
            .groupBy { Instant.ofEpochMilli(it.date).atZone(zone).toLocalDate() }
            .mapValues { (_, dayRecords) -> dayRecords.sumOf { it.totalAmount } }
        val start = rangeStart(rangeDays, today)
        val days = if (start != null) {
            generateSequence(start) { it.plusDays(1) }.takeWhile { !it.isAfter(today) }.toList()
        } else {
            byDay.keys.sorted()
        }
        return days.map { HistoryChartPoint(it, byDay[it] ?: 0.0) }
    }
}
```

- [ ] **Step 4: Verificar que los tests pasan**

Run: `./gradlew :app:testDebugUnitTest --tests "com.easypocket.mobile.ui.history.HistoryMathTest"`
Expected: PASS (4 tests).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/easypocket/mobile/ui/history/HistoryMath.kt app/src/test/java/com/easypocket/mobile/ui/history/HistoryMathTest.kt
git commit -m "feat: agrega HistoryMath con bucketing diario para el gráfico"
```

---

### Task 4: Acción "Guardar en historial" en ListDetail (ViewModel + UI + i18n)

**Files:**
- Modify: `app/src/main/java/com/easypocket/mobile/ui/lists/ListDetailViewModel.kt`
- Modify: `app/src/main/java/com/easypocket/mobile/ui/lists/ListDetailScreen.kt`
- Modify: `app/src/main/java/com/easypocket/mobile/i18n/Translations.kt`

**Interfaces:**
- Consumes: `PurchaseHistoryRepository.archiveCompleted(listId)` (Task 2), `ListDetailUiState.hasDoneItems`, `DropdownMenu`/`DropdownItem`, `ConfirmSheet`, `LocalToastState`.
- Produces: `ListDetailViewModel.archiveCompleted()` (suspend). Claves i18n `listDetail.archiveToHistory`, `listDetail.archiveConfirmMessage`, `listDetail.archiveConfirm`, `toast.listArchived`.

- [ ] **Step 1: Agregar claves i18n** — en `Translations.kt`, en el diccionario **inglés** (después de la línea `"listDetail.unpinSelected" to "Unpin",`):

```kotlin
"listDetail.archiveToHistory" to "Save to history",
"listDetail.archiveConfirmMessage" to "Move all completed items to the purchase history? They will be removed from this list.",
"listDetail.archiveConfirm" to "Save",
"toast.listArchived" to "Saved to history",
```

En el diccionario **español** (después de `"listDetail.unpinSelected" to "Desfijar",`):

```kotlin
"listDetail.archiveToHistory" to "Guardar en historial",
"listDetail.archiveConfirmMessage" to "¿Mover todos los items completados al historial de compras? Se eliminarán de esta lista.",
"listDetail.archiveConfirm" to "Guardar",
"toast.listArchived" to "Guardado en historial",
```

- [ ] **Step 2: Agregar `archiveCompleted` al ViewModel** — en `ListDetailViewModel.kt`:

a) Inyectar el repositorio (agregar al constructor y a los imports):

```kotlin
import com.easypocket.mobile.data.repository.PurchaseHistoryRepository
```

```kotlin
class ListDetailViewModel @Inject constructor(
    private val listsRepository: ShoppingListRepository,
    private val storesRepository: StoreRepository,
    private val productsRepository: ProductRepository,
    private val historyRepository: PurchaseHistoryRepository,
) : ViewModel() {
```

b) Agregar el método (después de `removeCompleted()`):

```kotlin
suspend fun archiveCompleted() {
    val current = _uiState.value.list ?: return
    val doneIds = current.items.filter { it.done }.map { it.id }
    if (doneIds.isEmpty()) return
    historyRepository.archiveCompleted(current.id)
    _uiState.value = _uiState.value.copy(list = current.copy(items = current.items.filter { !it.done }))
    resetFilterIfEmpty()
}
```

- [ ] **Step 3: Conectar la acción en la UI** — en `ListDetailScreen.kt`:

a) Estado local (junto a `showRemoveCompleted`):

```kotlin
var showArchive by rememberSaveable { mutableStateOf(false) }
```

b) Nuevo parámetro en `ActionBar` (agregar `onArchive: () -> Unit,` después de `onRemoveCompleted` y actualizar la llamada):

```kotlin
onArchive = { showArchive = true },
```

c) Nuevo item de menú en `ActionBar`, después del `DropdownItem` de "removeCompleted":

```kotlin
DropdownItem(
    label = t("listDetail.archiveToHistory", language),
    onClick = {
        onDismissMenu()
        onArchive()
    },
    enabled = uiState.hasDoneItems,
)
```

d) Nuevo `ConfirmSheet` (después del de `showRemoveCompleted`):

```kotlin
ConfirmSheet(
    visible = showArchive,
    title = t("listDetail.archiveToHistory", language),
    message = t("listDetail.archiveConfirmMessage", language),
    confirmLabel = t("listDetail.archiveConfirm", language),
    onConfirm = {
        scope.launch {
            vm.archiveCompleted()
            showArchive = false
            toast.show(t("toast.listArchived", language), ToastType.SUCCESS)
        }
    },
    onDismiss = { showArchive = false },
)
```

- [ ] **Step 4: Verificar compilación y suite existente**

Run: `./gradlew :app:compileDebugKotlin :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL, todos los tests pasan (la acción comparte la lógica ya testeada del repositorio).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/easypocket/mobile/ui/lists/ListDetailViewModel.kt app/src/main/java/com/easypocket/mobile/ui/lists/ListDetailScreen.kt app/src/main/java/com/easypocket/mobile/i18n/Translations.kt
git commit -m "feat: acción guardar en historial desde el detalle de lista"
```

---

### Task 5: HistoryViewModel + UiState

**Files:**
- Create: `app/src/main/java/com/easypocket/mobile/ui/history/HistoryViewModel.kt`
- Test: `app/src/test/java/com/easypocket/mobile/ui/history/HistoryViewModelTest.kt`

**Interfaces:**
- Consumes: `PurchaseHistoryRepository.observeAll()` (Task 2), `HistoryMath` (Task 3).
- Produces (usados por Tasks 6 y 7):
  - `data class HistoryUiState(records, rangeDays, isLoading)` con `filteredRecords: List<PurchaseHistoryWithItems>`, `chartPoints: List<HistoryChartPoint>`, `grandTotal: Double`
  - `HistoryViewModel.uiState: StateFlow<HistoryUiState>`
  - `HistoryViewModel.setRange(days: Int?)` (`null` = Todo)

- [ ] **Step 1: Escribir los tests que fallan** — crear `HistoryViewModelTest.kt` (usa `MainDispatcherRule`, patrón de `ListListViewModelTest`):

```kotlin
package com.easypocket.mobile.ui.history

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.easypocket.mobile.data.local.EasyPocketDatabase
import com.easypocket.mobile.data.local.PurchaseHistoryEntity
import com.easypocket.mobile.data.local.PurchaseHistoryWithItems
import com.easypocket.mobile.data.repository.PurchaseHistoryRepository
import com.easypocket.mobile.util.MainDispatcherRule
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HistoryViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var db: EasyPocketDatabase

    @After
    fun tearDown() { db.close() }

    private fun createVm(): HistoryViewModel {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, EasyPocketDatabase::class.java)
            .allowMainThreadQueries().build()
        val repo = PurchaseHistoryRepository(
            db = db,
            historyDao = db.purchaseHistoryDao(),
            listDao = db.listDao(),
            productDao = db.productDao(),
            priceDao = db.priceDao(),
            storeDao = db.storeDao(),
        )
        return HistoryViewModel(repo)
    }

    private fun entity(date: String, total: Double) = PurchaseHistoryEntity(
        id = date + total.toString(),
        listTitle = "L",
        listIcon = "$",
        date = LocalDate.parse(date).atStartOfDay(ZoneId.systemDefault()).plusHours(12)
            .toInstant().toEpochMilli(),
        totalAmount = total,
        itemCount = 1,
    )

    @Test
    fun `setRange updates filter state`() = runTest {
        val vm = createVm()
        advanceUntilIdle()
        assertEquals(7, vm.uiState.value.rangeDays)
        vm.setRange(30)
        assertEquals(30, vm.uiState.value.rangeDays)
        vm.setRange(null)
        assertEquals(null, vm.uiState.value.rangeDays)
    }

    @Test
    fun `uiState filters records by range and computes chart points`() {
        val records = listOf(
            PurchaseHistoryWithItems(entity("2026-08-27", 50.0), emptyList()),
            PurchaseHistoryWithItems(entity("2026-08-01", 30.0), emptyList()),
        )
        val today = LocalDate.of(2026, 8, 27)
        val state = HistoryUiState(records = records, rangeDays = 7, isLoading = false)

        assertEquals(1, state.filteredRecordsOf(today).size)
        assertEquals(7, state.chartPointsOf(today, 7).size)
        assertEquals(50.0, state.chartPointsOf(today, 7).last().total, 0.001)
        assertEquals(2, state.chartPointsOf(today, null).size)
        assertEquals(50.0, state.grandTotalOf(today), 0.001)
    }
}
```

Nota: el segundo test es puro (sin Room ni Flows) y usa helpers que inyectan `today`, que se agregan al `HistoryUiState` en el Step 3.

- [ ] **Step 2: Verificar que los tests fallan (compilación)**

Run: `./gradlew :app:testDebugUnitTest --tests "com.easypocket.mobile.ui.history.HistoryViewModelTest"`
Expected: FAIL — `HistoryViewModel` no existe.

- [ ] **Step 3: Implementar el ViewModel** — crear `HistoryViewModel.kt`:

```kotlin
package com.easypocket.mobile.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.easypocket.mobile.data.local.PurchaseHistoryEntity
import com.easypocket.mobile.data.local.PurchaseHistoryWithItems
import com.easypocket.mobile.data.repository.PurchaseHistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HistoryUiState(
    val records: List<PurchaseHistoryWithItems> = emptyList(),
    val rangeDays: Int? = 7,
    val isLoading: Boolean = true,
) {
    val filteredRecords: List<PurchaseHistoryWithItems>
        get() = filteredRecordsOf(LocalDate.now())

    val chartPoints: List<HistoryChartPoint>
        get() = chartPointsOf(LocalDate.now(), rangeDays)

    val grandTotal: Double
        get() = grandTotalOf(LocalDate.now())

    fun filteredRecordsOf(today: LocalDate): List<PurchaseHistoryWithItems> =
        records.filter { HistoryMath.isInRange(it.record, rangeDays, today) }

    fun chartPointsOf(today: LocalDate, rangeDays: Int?): List<HistoryChartPoint> =
        HistoryMath.dailyTotals(records.map { it.record }, rangeDays, today)

    fun grandTotalOf(today: LocalDate): Double =
        records.filter { HistoryMath.isInRange(it.record, this.rangeDays, today) }
            .sumOf { it.record.totalAmount }
}

@HiltViewModel
class HistoryViewModel @Inject constructor(
    historyRepository: PurchaseHistoryRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            historyRepository.observeAll().collect { records ->
                _uiState.value = _uiState.value.copy(records = records, isLoading = false)
            }
        }
    }

    fun setRange(days: Int?) {
        _uiState.value = _uiState.value.copy(rangeDays = days)
    }
}
```

- [ ] **Step 4: Verificar que los tests pasan**

Run: `./gradlew :app:testDebugUnitTest --tests "com.easypocket.mobile.ui.history.HistoryViewModelTest"`
Expected: PASS (2 tests).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/easypocket/mobile/ui/history/HistoryViewModel.kt app/src/test/java/com/easypocket/mobile/ui/history/HistoryViewModelTest.kt
git commit -m "feat: agrega HistoryViewModel con filtro de rango de fechas"
```

---

### Task 6: HistoryAreaChart (Canvas de Compose)

**Files:**
- Create: `app/src/main/java/com/easypocket/mobile/ui/history/HistoryAreaChart.kt`

**Interfaces:**
- Consumes: `HistoryChartPoint` (Task 3), `LocalAppColors`.
- Produces: `HistoryAreaChart(points: List<HistoryChartPoint>, modifier: Modifier = Modifier)` — composable que dibuja el área de gasto por día con etiqueta de valor máximo y fechas inicial/final. Usado por Task 7.

- [ ] **Step 1: Implementar el gráfico** — crear `HistoryAreaChart.kt` (composable de presentación; su lógica ya está testeada en `HistoryMath`, la verificación es compilación + visual):

```kotlin
package com.easypocket.mobile.ui.history

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.easypocket.mobile.ui.theme.LocalAppColors
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DATE_LABEL_FORMAT = DateTimeFormatter.ofPattern("dd/MM")

@Composable
fun HistoryAreaChart(points: List<HistoryChartPoint>, modifier: Modifier = Modifier) {
    val appColors = LocalAppColors.current
    val maxValue = points.maxOfOrNull { it.total }?.coerceAtLeast(1.0) ?: return
    Column(modifier.fillMaxWidth()) {
        Text(
            text = "$${String.format(Locale.US, "%.2f", maxValue)}",
            color = appColors.textSecondary,
            fontSize = 11.sp,
            modifier = Modifier.align(Alignment.End).padding(end = 4.dp),
        )
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(140.dp)
                .padding(horizontal = 4.dp),
        ) {
            val linePath = Path()
            points.forEachIndexed { index, point ->
                val x = if (points.size == 1) size.width / 2f else index * size.width / (points.size - 1)
                val y = size.height * (1f - (point.total / maxValue).toFloat())
                if (index == 0) linePath.moveTo(x, y) else linePath.lineTo(x, y)
            }
            val fillPath = Path().apply {
                addPath(linePath)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
            drawPath(
                fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(appColors.primary.copy(alpha = 0.35f), Color.Transparent),
                ),
            )
            drawPath(linePath, color = appColors.primary, style = Stroke(width = 2.dp.toPx()))
        }
        Row(Modifier.fillMaxWidth()) {
            Text(
                text = points.first().date.format(DATE_LABEL_FORMAT),
                color = appColors.textSecondary,
                fontSize = 10.sp,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = points.last().date.format(DATE_LABEL_FORMAT),
                color = appColors.textSecondary,
                fontSize = 10.sp,
            )
        }
    }
}
```

Nota: `FontWeight` no se usa en este archivo; si el compilador lo marca como import sin usar, eliminarlo.

- [ ] **Step 2: Verificar compilación**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/easypocket/mobile/ui/history/HistoryAreaChart.kt
git commit -m "feat: gráfico de área de gasto diario con Canvas"
```

---

### Task 7: HistoryScreen (chips de rango, lista cronológica, detalle, i18n)

**Files:**
- Create: `app/src/main/java/com/easypocket/mobile/ui/history/HistoryScreen.kt`
- Modify: `app/src/main/java/com/easypocket/mobile/i18n/Translations.kt`

**Interfaces:**
- Consumes: `HistoryViewModel` (Task 5), `HistoryAreaChart` (Task 6), `AppHeader`, `AppItemList`, `ListItemRow`, `AppBottomSheet`, `ListIconCircle`, `LocalAppColors`, `ListLogic.trimQuantity`.
- Produces: `HistoryScreen(onMenuClick: () -> Unit)` — usado por Task 8. Claves i18n `tab.history`, `history.*`.

- [ ] **Step 1: Agregar claves i18n** — en `Translations.kt`:

Diccionario **inglés** (después de `"tab.stores" to "Stores",`):

```kotlin
"tab.history" to "History",
```

Y bloque de historial (al final del diccionario inglés, junto a las demás claves de feature):

```kotlin
"history.empty" to "No purchases recorded yet",
"history.range.7" to "7 days",
"history.range.30" to "30 days",
"history.range.90" to "90 days",
"history.range.all" to "All",
"history.showingCount" to "{count} purchases",
"history.total" to "Total",
"history.itemsCount" to "{count} items",
```

Diccionario **español** (después de `"tab.stores" to "Tiendas",`):

```kotlin
"tab.history" to "Historial",
```

Y bloque (al final del diccionario español):

```kotlin
"history.empty" to "Aún no hay compras registradas",
"history.range.7" to "7 días",
"history.range.30" to "30 días",
"history.range.90" to "90 días",
"history.range.all" to "Todo",
"history.showingCount" to "{count} compras",
"history.total" to "Total",
"history.itemsCount" to "{count} items",
```

- [ ] **Step 2: Implementar la pantalla** — crear `HistoryScreen.kt`:

```kotlin
package com.easypocket.mobile.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.easypocket.mobile.data.local.PurchaseHistoryWithItems
import com.easypocket.mobile.domain.ListLogic
import com.easypocket.mobile.i18n.Language
import com.easypocket.mobile.i18n.LocalLanguage
import com.easypocket.mobile.i18n.t
import com.easypocket.mobile.ui.components.AppBottomSheet
import com.easypocket.mobile.ui.components.AppHeader
import com.easypocket.mobile.ui.components.AppItemList
import com.easypocket.mobile.ui.components.ListIconCircle
import com.easypocket.mobile.ui.components.ListItemRow
import com.easypocket.mobile.ui.theme.LocalAppColors
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy")
private val ZONE: ZoneId = ZoneId.systemDefault()

@Composable
fun HistoryScreen(onMenuClick: () -> Unit) {
    val vm: HistoryViewModel = hiltViewModel()
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val language = LocalLanguage.current
    val appColors = LocalAppColors.current

    var selectedRecord by remember { mutableStateOf<PurchaseHistoryWithItems?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(appColors.background)
            .padding(top = 60.dp),
    ) {
        AppHeader(title = t("tab.history", language), onMenuClick = onMenuClick)
        when {
            uiState.isLoading -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(t("common.loading", language), color = appColors.textSecondary, fontSize = 16.sp)
            }
            uiState.records.isEmpty() -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    t("history.empty", language),
                    color = appColors.textSecondary,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                )
            }
            else -> {
                RangeChips(selected = uiState.rangeDays, onSelect = { vm.setRange(it) })
                HistoryAreaChart(
                    points = uiState.chartPoints,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
                RecordsList(
                    records = uiState.filteredRecords,
                    language = language,
                    onRecordPress = { selectedRecord = it },
                )
            }
        }
    }

    RecordDetailSheet(
        record = selectedRecord,
        language = language,
        onDismiss = { selectedRecord = null },
    )
}

@Composable
private fun RangeChips(selected: Int?, onSelect: (Int?) -> Unit) {
    val appColors = LocalAppColors.current
    val language = LocalLanguage.current
    val options = listOf(
        7 to "history.range.7",
        30 to "history.range.30",
        90 to "history.range.90",
        null to "history.range.all",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { (days, key) ->
            val isSelected = selected == days
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (isSelected) appColors.primary else appColors.surface)
                    .clickable { onSelect(days) }
                    .padding(horizontal = 14.dp, vertical = 6.dp),
            ) {
                Text(
                    text = t(key, language),
                    color = if (isSelected) Color.White else appColors.textSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun RecordsList(
    records: List<PurchaseHistoryWithItems>,
    language: Language,
    onRecordPress: (PurchaseHistoryWithItems) -> Unit,
) {
    AppItemList(
        footerText = t("history.showingCount", language, mapOf("count" to records.size.toString())),
    ) {
        itemsIndexed(records, key = { _, entry -> entry.record.id }) { index, record ->
            ListItemRow(
                onClick = { onRecordPress(record) },
                isFirst = index == 0,
                isLast = index == records.lastIndex,
            ) {
                RecordCardContent(record = record)
            }
        }
    }
}

@Composable
private fun RecordCardContent(record: PurchaseHistoryWithItems) {
    val appColors = LocalAppColors.current
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        ListIconCircle(icon = record.record.listIcon, modifier = Modifier.size(40.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = record.record.listTitle,
                color = appColors.text,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = formatDate(record.record.date) + " · " +
                    t("history.itemsCount", LocalLanguage.current, mapOf("count" to record.record.itemCount.toString())),
                color = appColors.textSecondary,
                fontSize = 12.sp,
            )
        }
        Text(
            text = "$${formatAmount(record.record.totalAmount)}",
            color = appColors.text,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun RecordDetailSheet(
    record: PurchaseHistoryWithItems?,
    language: Language,
    onDismiss: () -> Unit,
) {
    val appColors = LocalAppColors.current
    AppBottomSheet(
        visible = record != null,
        onDismiss = onDismiss,
        heightFraction = 0.7f,
        title = record?.record?.listTitle ?: "",
    ) {
        val current = record ?: return@AppBottomSheet
        Text(formatDate(current.record.date), color = appColors.textSecondary, fontSize = 13.sp)
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            current.items.forEach { item ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(item.productName, color = appColors.text, fontSize = 15.sp)
                        Text(
                            text = listOfNotNull(
                                item.storeName ?: t("listDetail.noStore", language),
                                "${ListLogic.trimQuantity(item.quantity)} x $${formatAmount(item.unitPrice)}",
                            ).joinToString(" · "),
                            color = appColors.textSecondary,
                            fontSize = 12.sp,
                        )
                    }
                    Text(
                        text = "$${formatAmount(item.totalPrice)}",
                        color = appColors.text,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Text(t("history.total", language), color = appColors.textSecondary, fontSize = 15.sp)
            Spacer(Modifier.width(8.dp))
            Text(
                text = "$${formatAmount(current.record.totalAmount)}",
                color = appColors.text,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

private fun formatDate(date: Long): String =
    Instant.ofEpochMilli(date).atZone(ZONE).toLocalDate().format(DATE_FORMAT)

private fun formatAmount(value: Double): String = String.format(Locale.US, "%.2f", value)
```

- [ ] **Step 3: Verificar compilación**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL (si `ListItemRow` no acepta omitir `onLongClick`, pasar `onLongClick = null` o `onLongClick = { }` según su firma real en `ui/components/`).

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/easypocket/mobile/ui/history/HistoryScreen.kt app/src/main/java/com/easypocket/mobile/i18n/Translations.kt
git commit -m "feat: pantalla de historial con gráfico y lista cronológica"
```

---

### Task 8: Integración en la navegación (4ª pestaña)

**Files:**
- Modify: `app/src/main/java/com/easypocket/mobile/ui/navigation/AppNavHost.kt:28,33-37`
- Modify: `app/src/main/java/com/easypocket/mobile/ui/navigation/HomePagerScreen.kt:18,46-62`
- Modify: `app/src/main/java/com/easypocket/mobile/ui/navigation/BottomBar.kt:14-20,33-39`

**Interfaces:**
- Consumes: `HistoryScreen(onMenuClick)` (Task 7), clave i18n `tab.history` (Task 7).
- Produces: nada (integración final).

- [ ] **Step 1: Agregar iconos al BottomBar** — en `BottomBar.kt`:

```kotlin
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.outlined.History
```

```kotlin
private val unselectedIcons = listOf(
    Icons.Outlined.List, Icons.Outlined.ViewInAr, Icons.Outlined.Storefront, Icons.Outlined.History,
)

private val selectedIcons = listOf(
    Icons.Filled.List, Icons.Filled.ViewInAr, Icons.Filled.Storefront, Icons.Filled.History,
)
```

- [ ] **Step 2: PAGE_COUNT 3→4 y label** — en `AppNavHost.kt`:

```kotlin
private const val PAGE_COUNT = 4
```

```kotlin
val labels = listOf(
    t("tab.lists", language),
    t("tab.products", language),
    t("tab.stores", language),
    t("tab.history", language),
)
```

- [ ] **Step 3: Agregar página al pager** — en `HomePagerScreen.kt`:

```kotlin
private const val PAGE_COUNT = 4
```

(importar `com.easypocket.mobile.ui.history.HistoryScreen`) y en el `when (page)`:

```kotlin
3 -> HistoryScreen(
    onMenuClick = { vm.openMenu() },
)
```

- [ ] **Step 4: Verificar compilación y suite completa**

Run: `./gradlew :app:testDebugUnitTest :app:assembleDebug`
Expected: BUILD SUCCESSFUL — todos los tests pasan y el APK compila.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/easypocket/mobile/ui/navigation/AppNavHost.kt app/src/main/java/com/easypocket/mobile/ui/navigation/HomePagerScreen.kt app/src/main/java/com/easypocket/mobile/ui/navigation/BottomBar.kt
git commit -m "feat: integra historial como cuarta pestaña del home"
```

---

### Task 9: Verificación final

**Files:** ninguno (verificación).

- [ ] **Step 1: Suite completa de tests**

Run: `./gradlew :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL — incluye los nuevos tests de Tasks 1–5 y los preexistentes (DatabaseTest, ShoppingListRepositoryTest, BackupManagerTest, ListLogicTest, ListListViewModelTest, ListIconTest).

- [ ] **Step 2: Lint/compile release**

Run: `./gradlew :app:assembleRelease`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Verificar working tree limpio respecto a los archivos del plan**

Run: `git status --short`
Expected: solo quedan los cambios preexistentes del usuario (que no forman parte de este plan); ningún archivo del plan sin commitear.

---

## Notes for executors

- **Backup (fuera de alcance a propósito):** `BackupData.kt` no incluye las tablas de historial. Los registros no se exportan/importan; no es un error.
- **`fallbackToDestructiveMigration()`** sigue activo en AppModule, pero la migración 2→3 existe formalmente para instalaciones reales que ya tienen la v2.
- **Zona horaria:** todo el bucketing usa `ZoneId.systemDefault()`; los tests computan expectativas con la misma zona, por lo que son estables en cualquier TZ.
- Si `ListItemRow`/`DropdownItem` tienen firmas distintas a las asumidas, ajustar la llamada al componente existente sin cambiar el comportamiento.
