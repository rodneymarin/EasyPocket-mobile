# Diseño: Categorías de Productos

**Fecha:** 2026-08-29
**Estado:** Aprobado (diseño validado en sesión de brainstorming)

## Objetivo

Agregar categorías asociadas a productos. El usuario crea y administra sus
propias categorías (solo nombre) desde un maestro accesible como quinta
pestaña del navegador principal. Cada categoría recibe un ID alfanumérico de
6 caracteres generado por el sistema, oculto para el usuario, persistido en la
BD e incluido en el export JSON. Los productos pueden (opcionalmente) tener
una categoría, y esa categoría se registra como código (snapshot) en cada item
del historial de compras.

## Contexto

EasyPocket es una app Android nativa (Kotlin + Jetpack Compose, Room, Hilt,
Navigation-Compose, MVVM). Existe un patrón de maestro CRUD en Stores
(pantalla + formulario + repositorio) y backup JSON en `BackupData.kt` con
`ignoreUnknownKeys` y campos con defaults (retrocompatible).

## Decisiones de diseño (validadas con el usuario)

1. **Acceso al maestro:** nueva 5ta pestaña "Categories" en el pager del home.
   Orden: Lists / Products / **Categories** / Stores / History.
2. **Modelo de categoría:** solo nombre (sin color ni icono).
3. **Asignación:** opcional al crear el producto, editable después desde el
   formulario del producto. La lista de productos muestra la categoría como
   tag y permite filtrar por categoría.
4. **Enfoque técnico:** sin foreign keys (Opción A). Limpieza a nivel de
   repositorio: al borrar una categoría, los productos que apuntaban a ella
   quedan con `categoryId = NULL` en la misma transacción.
5. **Historial:** snapshot del código de categoría por item al momento de
   archivar. Si al mostrar no existe una categoría con ese código, se muestra
   "Sin categoría" (fallback solo visual; el código permanece en BD y export).
6. **ID:** 6 caracteres `A-Z0-9`, generado por el sistema, con verificación de
   colisión. Nunca se muestra en UI; solo vive en BD y en el JSON.

## Modelo de datos (Room, versión 3 → 4)

Cambios en `data/local/Entities.kt`, `data/local/Daos.kt`,
`data/local/EasyPocketDatabase.kt`.

### Nueva entidad `CategoryEntity` — tabla `categories`

| Campo  | Tipo   | Notas                                        |
|--------|--------|----------------------------------------------|
| `id`   | String | Código alfanumérico de 6 caracteres, PK      |
| `name` | String | Nombre definido por el usuario, índice único lógico (validación en repositorio) |

### Columnas nuevas (nullable, sin FK)

- `products.category_id: String?` — código de la categoría del producto.
- `purchase_history_items.category_code: String?` — snapshot del código al
  archivar la compra.

### Migración `MIGRATION_3_4`

`CREATE TABLE categories` (con índice en `name`) + `ALTER TABLE products ADD
COLUMN category_id TEXT` + `ALTER TABLE purchase_history_items ADD COLUMN
category_code TEXT`. Datos existentes quedan intactos (sin categoría).

### DAOs

- `CategoryDao`: `getAll(): Flow<List<CategoryEntity>>` (orden alfabético
  COLLATE NOCASE), `getById`, `getByName` (case-insensitive), `insert`,
  `update`, `deleteByIds`.
- `ProductDao`: nuevo `clearCategory(ids: List<String>)` — `UPDATE products
  SET category_id = NULL WHERE category_id IN (:ids)`.

### Dominio

- `Category(id: String, name: String)` en `domain/Models.kt`.
- `Product` gana `categoryId: String? = null`.

## Repositorios

### `CategoryRepository` (nuevo, espejo de `StoreRepository`)

- `getAll(): Flow<List<Category>>`
- `create(name)`: valida nombre no vacío y sin duplicado (case-insensitive);
  genera ID de 6 caracteres `A-Z0-9` verificando colisión contra los IDs
  existentes; inserta.
- `update(category)`: valida duplicado excluyendo la propia categoría.
- `deleteAll(ids)`: en `db.withTransaction` — primero
  `productDao.clearCategory(ids)` (los productos quedan sin categoría), luego
  `categoryDao.deleteByIds(ids)`. El historial NO se toca (sus códigos son
  snapshots inmutables).
- `findDuplicate(name, excludeId)` para validación de formulario.

### `ProductRepository`

- `create(name, unit, prices, categoryId = null)` y `update(product)` con
  `categoryId`. Default `null` para no romper llamadas existentes (seeder,
  backup import).

### `PurchaseHistoryRepository.archiveCompleted`

- En la transacción ya carga `products` por id; para cada item `done` guarda
  `categoryCode = products[productId]?.categoryId`. Snapshot inmutable.

## UI

### Pestaña Categories

- `PAGE_COUNT` 4→5 en `AppNavHost` y `HomePagerScreen`; `BottomBar` agrega
  `Icons.Filled/Outlined.Category` a las listas de iconos. Label i18n
  `tab.categories`.
- `ui/categories/CategoriesScreen` + `CategoriesViewModel`: replica el patrón
  de Stores (lista con búsqueda, estado vacío, borrado con confirmación via
  `ConfirmSheet`, taps → `categoryForm/{categoryId}`).
- `ui/categories/CategoryFormScreen` + `CategoryFormViewModel`: espejo de los
  de Store; campo de nombre con `FormTextField`, validación de obligatorio y
  duplicado, borrado si es edición.
- Ruta `categoryForm/{categoryId}` en `AppNavHost`.
- `AppModule` provee el DAO/repositorio siguiendo el patrón Hilt existente.

### Products

- Formulario (`ProductFormScreen`/`ProductFormViewModel`): selector
  "Categoría" opcional con el componente `Select` existente; opciones =
  "Sin categoría" + categorías disponibles. `ProductFormUiState` gana
  `categoryId`; se precarga al editar y se persiste al guardar.
- Lista (`ProductsScreen`/`ProductListViewModel`): tag con el nombre de la
  categoría (componente `Tag` existente) en cada producto que tenga una;
  filtro por categoría combinable con la búsqueda por nombre (opción "Todas").

### History

- `HistoryViewModel` resuelve `categoryCode → Category.name` (observa las
  categorías del repositorio); fallback "Sin categoría" si el código es `null`
  o no existe. El detalle del registro muestra la categoría de cada item.

## Backup JSON (retrocompatible)

- `BackupData` gana `categories: List<BackupCategory> = emptyList()`.
- `BackupProduct` gana `categoryId: String? = null`.
- `BackupPurchaseHistoryItem` gana `categoryCode: String? = null`.
- Defaults en todos los campos nuevos + `ignoreUnknownKeys`: los JSON viejos
  importan igual y `version: 1` se mantiene.
- Export: categorías completas (con IDs) + códigos en productos e historial.
- Import: orden categorías → productos → historial. Sin validación estricta
  de que el código de categoría exista en el maestro (un backup puede traer
  códigos de categorías ya borradas — válido por diseño, se muestra
  "Sin categoría").

## i18n

Nuevas claves en `i18n/Translations.kt` para todos los idiomas soportados:
título de pestaña, maestro, formulario, validaciones, confirmación de borrado,
estado vacío, "Sin categoría", etiqueta de categoría en productos/historial,
opción "Todas" del filtro.

## Testing

- **Room/DAO** (`DatabaseTest`): migración 3→4, CRUD de categorías,
  `clearCategory` al borrar, snapshot en historial.
- **Repositorios**: `CategoryRepository` (ID de 6 caracteres válido y único,
  duplicados por nombre, delete limpia productos y preserva historial),
  `ProductRepository` (asignación/cambio/clear), `PurchaseHistoryRepository`
  (código registrado por item).
- **Backup** (`BackupManagerTest`): export incluye categorías y códigos;
  import de backup sin categorías (viejo) y con categorías (nuevo).
- **ViewModels**: `CategoryFormViewModel` (validaciones), `ProductFormViewModel`
  (categoría opcional), filtro en `ProductListViewModel`, resolución de nombre
  en `HistoryViewModel`.

## Fuera de alcance

- Reasignación masiva de categorías (bulk edit).
- Categorías con color/icono o jerarquía (subcategorías).
- Reasignar retroactivamente el historial cuando cambie la categoría de un
  producto (el snapshot es inmutable por diseño).
