# Diseño: Historial de Compras

**Fecha:** 2026-08-29
**Estado:** Aprobado (diseño validado en sesión de brainstorming)

## Objetivo

Agregar un historial de compras a EasyPocket. Los registros se crean cuando el
usuario ejecuta la acción de pasar los items completados de una lista de compra
al historial. La pantalla de historial muestra los registros en orden
cronológico y un gráfico de área con filtro de rango de fechas.

## Contexto

EasyPocket es una app Android nativa (Kotlin + Jetpack Compose, Room, Hilt,
Navigation-Compose, MVVM). Los montos de las listas no se persisten: se
calculan al vuelo con `domain/ListLogic.kt` a partir de `PriceEntity`
(precio por producto+tienda). Productos y tiendas pueden borrarse o cambiar de
precio, por lo que el historial debe guardar una **copia congelada** (snapshot),
no referencias vivas.

## Decisiones de diseño (validadas con el usuario)

1. **Destino de la lista original:** al pasar al historial, los items
   completados se borran de la lista; los pendientes permanecen en ella.
2. **Detalle del registro:** snapshot completo por item (nombre, cantidad,
   precio unitario, total del item) más total general y fecha.
3. **Ubicación de la acción:** dentro de `ListDetailScreen`.
4. **Acceso al historial:** cuarta pestaña del pager/`BottomBar` del home
   (Lists / Products / Stores / **History**).
5. **Filtro de fechas:** presets simples: 7 días / 30 días / 90 días / Todo.
6. **Gráfico:** dibujado con Canvas de Compose (sin dependencias nuevas).

## Modelo de datos (Room, versión 2 → 3)

Nuevas entidades en `data/local/Entities.kt`, DAO en `data/local/Daos.kt`,
relación en `data/local/Relations.kt`, DB bump a versión 3 en
`data/local/EasyPocketDatabase.kt` con migración `MIGRATION_2_3`.

### `PurchaseHistoryEntity` — tabla `purchase_history`

| Campo         | Tipo   | Notas                                  |
|---------------|--------|----------------------------------------|
| `id`          | String | UUID, PK                               |
| `listTitle`   | String | Nombre de la lista al momento del pase |
| `listIcon`    | String | Icono de la lista (mismo formato que `ShoppingListEntity.icon`, default `"$"`) |
| `date`        | Long   | Epoch millis del día en que se ejecutó el pase |
| `totalAmount` | Double | Suma de los totales de los items       |
| `itemCount`   | Int    | Cantidad de items del registro         |

### `PurchaseHistoryItemEntity` — tabla `purchase_history_items`

| Campo        | Tipo    | Notas                                    |
|--------------|---------|------------------------------------------|
| `id`         | Long    | Auto-generado, PK                        |
| `historyId`  | String  | FK → `purchase_history.id`, `CASCADE`    |
| `productName`| String  | Copia congelada del nombre del producto  |
| `storeName`  | String? | Copia congelada del nombre de la tienda (nullable) |
| `quantity`   | Double  | Cantidad comprada                        |
| `unitPrice`  | Double  | Precio unitario congelado (precio de la tienda usado por `ListLogic`) |
| `totalPrice` | Double  | `quantity × unitPrice` congelado         |

### Relación y consultas

- `PurchaseHistoryWithItems` (POJO `@Relation`) para lectura con items.
- `PurchaseHistoryDao` expone `Flow<List<PurchaseHistoryWithItems>>` ordenado
  por `date` descendente (cronológico, más reciente primero).
- Migración `MIGRATION_2_3`: `CREATE TABLE` de ambas tablas con índice en
  `purchase_history_items.historyId`.

## Acción "Pasar al historial"

- Ubicación: top bar de `ListDetailScreen` (visible solo si hay items `done`),
  con confirmación vía `ConfirmSheet` (patrón existente).
- Nuevo método `ShoppingListRepository.archiveCompleted(listId: String)`,
  ejecutado en una transacción Room (`withTransaction`):
  1. Lee los items `done` de la lista y construye el snapshot reutilizando la
     lógica de precios de `domain/ListLogic.kt` (precio de la tienda asignada
     del item, o el precio vigente según la misma regla que usa el total del
     carrito).
  2. Inserta `PurchaseHistoryEntity` (fecha = hoy, es decir al momento de
     ejecutar el pase) con sus `PurchaseHistoryItemEntity`.
  3. Llama a `removeCompleted(listId)`: la lista sobrevive con sus items
     pendientes.
- Si no hay items `done`, la acción no se muestra.

## Pantalla de Historial

- Nuevo paquete `ui/history/`: `HistoryScreen` + `HistoryViewModel`
  (patrón MVVM existente: `@HiltViewModel`, `UiState` inmutable,
  `StateFlow`, `collectAsStateWithLifecycle`).
- Acceso: `HomePagerScreen` pasa de 3 a 4 páginas; `BottomBar` agrega la
  pestaña "History" con icono de historia. No se agrega ruta nueva al
  `AppNavHost` (el historial vive dentro del pager del home, como las otras
  pestañas).
- Layout (de arriba a abajo):
  1. **Gráfico de área** del gasto total por día dentro del rango
     seleccionado, dibujado con Canvas de Compose (`Path` + relleno con
     gradiente). Días sin compras aportan 0. Presets de filtro como chips:
     **7d / 30d / 90d / Todo**.
  2. **Lista de registros** en orden cronológico (más reciente primero):
     título de la lista, fecha, cantidad de items y total.
  3. Tap en un registro → `AppBottomSheet` con el detalle de los items
     (nombre, tienda si existe, cantidad, total por item).
- Estado vacío (sin registros) con mensaje y texto i18n.
- i18n: nuevas claves EN/ES en `i18n/Translations.kt` (título de pestaña,
  presets, textos de estado vacío, etiquetas del detalle).

## Testing

- **Repositorio** (Robolectric + Room in-memory, patrón de tests existente en
  `app/src/test/`): `archiveCompleted` crea el registro con los datos
  correctos (snapshot, total, fecha) y elimina los items `done` de la lista
  preservando los pendientes.
- **Migración:** test `2→3` con `room-testing` (`MigrationTestHelper`).
- **Unitario:** bucketing de fechas del gráfico (agrupación de registros por
  día dentro del rango) y cálculo del gasto diario.
- **ViewModel:** filtro de rango actualiza el estado del gráfico/lista.

## Fuera de alcance

- Edición o borrado de registros históricos (quedan para una iteración futura).
- Comparación de períodos o métricas adicionales en el gráfico.
- Rango de fechas personalizado (date picker).
