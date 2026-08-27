# EasyPocket — Spec de Diseño

**Fecha:** 2026-08-27
**Estado:** Aprobado (diseño validado en conversación)

## Resumen

EasyPocket es el port nativo Android (Kotlin) de EasyBuy (React Native + Expo, ubicada en `/home/rodney/Documents/Dev/easybuy-mobile/`). Reproduce las mismas features y la misma UI que la original, con estos cambios:

- **Sin conexión remota/cloud**: se elimina Supabase, login/registro (AuthSheet), y el toggle de "Fuente de datos" del menú. La app trabaja 100% con data local.
- **Sin opción web**: app nativa Android exclusiva.
- **Nueva feature en el menú principal**: **Exportar Data / Importar Data** (JSON con Listas, Productos, Tiendas y precios).

## Decisiones aprobadas

| Decisión | Valor |
|---|---|
| Stack UI | Jetpack Compose |
| Persistencia | Room sobre SQLite |
| Arquitectura | MVVM + StateFlow + Hilt |
| Formato export/import | Archivo JSON vía Storage Access Framework |
| Comportamiento import | Reemplaza toda la data existente (backup/restore) |
| Idiomas | EN + ES con selector en menú (port de las ~190 claves) |
| Cloud/auth | Eliminado por completo |
| Seed demo | Mantenido (2 tiendas, 10 productos, 3 listas) |
| Package | `com.easypocket.mobile` |
| SDK | min 26 (Android 8.0), target/compile 35 |
| Versión inicial | 1.0.0 |

## 1. Arquitectura

### Estructura del proyecto

Raíz: `/home/rodney/Documents/Dev/EasyPocket-mobile/`

```
app/
├── data/
│   ├── local/          ← Room: EasyPocketDatabase, DAOs, entities
│   ├── repository/     ← Repositories (products, stores, shoppingLists, settings)
│   ├── backup/         ← Export/Import JSON (kotlinx.serialization)
│   └── seed/           ← Seed inicial demo
├── domain/
│   └── models          ← Store, Product, Price, ShoppingList, ShoppingListItem, UnitOfMeasurement
├── ui/
│   ├── theme/          ← Paletas light/dark, store colors, tipografía
│   ├── components/     ← Button, Tag, PressableCard, BottomSheet, SearchInput, Select, DropdownMenu, Toast
│   ├── navigation/     ← HorizontalPager + bottom bar
│   ├── mainmenu/       ← Drawer derecho con swipe-to-close
│   ├── lists/          ← Listas, detalle, form item
│   ├── products/       ← Catálogo, form producto
│   └── stores/         ← Tiendas, form tienda
├── settings/           ← Tema + idioma (DataStore), i18n
└── EasyPocketApp.kt    ← Application con Hilt
```

### Base de datos (Room)

Mismo esquema que EasyBuy local:

- `stores (id TEXT PK, description TEXT NOT NULL, color INTEGER NOT NULL DEFAULT 0)`
- `products (id TEXT PK, product_name TEXT NOT NULL, unit_of_measurement TEXT NOT NULL)`
- `product_prices (product_id, store_id, value REAL, PK(product_id, store_id), FKs a products/stores CASCADE)`
- `shopping_lists (id TEXT PK, title TEXT NOT NULL)`
- `shopping_list_items (id INTEGER PK AUTOINCREMENT, shopping_list_id FK CASCADE, product_id FK CASCADE, store_id FK NULLABLE, quantity REAL, done INTEGER DEFAULT 0, pinned INTEGER DEFAULT 0)`
- `settings (key TEXT PK, value TEXT NOT NULL)`

Diferencias con la original: FKs con `ON DELETE CASCADE` gestionadas por la BD (la original hacía la cascada manual); `color` como INTEGER (la original lo guardaba como TEXT local).

IDs: UUID string generados con `java.util.UUID`.

### ViewModels y estado

- ViewModel por pantalla con StateFlow para estado de UI.
- Recarga de datos al ganar foco de pantalla (equivalente a `useFocusEffect`).
- Toggle done optimista (actualiza UI antes de persistir).

### Settings

- Tema (light/dark/system) e idioma (en/es) persistidos en DataStore (equivalente a la tabla settings + AsyncStorage de la original).
- i18n: string maps en Kotlin con interpolación `{param}`; port directo de `src/lib/i18n/translations.ts` (~190 claves por idioma). Idioma por defecto: inglés.

## 2. Navegación y UI

### Navegación raíz

`HorizontalPager` con 3 páginas (Listas/inicio, Productos, Tiendas) con swipe horizontal entre ellas y bottom bar custom debajo: 3 ítems con iconos (outline cuando inactivos), activo en color primario, inactivo gris.

### Pantallas

Todas con `paddingTop ~60dp`, título centrado, botón hamburguesa derecha, botón back circular izquierdo cuando aplica.

1. **Listas** (tab inicio): búsqueda con debounce 300ms y normalización de acentos (NFD), botón `+`. Tarjetas con título, tags "x/y items" y "Total: $X.XX", botón X (eliminar con confirmación). Contador "Mostrando N listas". Bottom sheet 75% para crear lista; al crear navega directo al detalle. Tap en tarjeta → detalle.

2. **Detalle de lista**: título tappable para renombrar (sheet), barra de chips de filtro por tienda ("Todas" + tiendas presentes en la lista, con overflow a dropdown si no caben), bloque de totales "Total: $X" / "Carrito: $Y" + botón `+`, menú `⋮` con: Copiar al portapapeles, Desmarcar todos, Eliminar completados. Items: círculo check (toggle done), nombre tachado si done, tag de tienda con color de tienda (o "Sin tienda" en itálica), tag de precio total ($precio×cantidad), tag de cantidad+unidad (singular/plural), icono pin si fijado. Orden: pendientes primero (fijados arriba, luego alfabético), sección "Completado" al final (alfabético). Long-press → modo selección múltiple con acciones: Mover a otra lista, Fijar/Desfijar, Eliminar (N).

3. **Form de item** (crear/editar): ProductPicker con búsqueda integrada, botones laterales editar producto (lápiz, disabled sin selección) y crear producto (+), select de tienda opcional ("-- Ninguna --"), input de cantidad (teclado decimal, validación regex `^\d*\.?\d*$`, default "1"), etiqueta de unidad singular/plural, resumen "Precio unitario: $X.XX" y "Total: $Y.YY". Footer: [Eliminar (solo edición)] [Cancelar] + botón primario "Guardar".

4. **Productos**: búsqueda + `+`, tarjetas con nombre + tag de unidad. Long-press → selección múltiple con contador y botón destructivo "Eliminar (N)" con confirmación de cascada (borra precios e items de listas).

5. **Form de producto**: input de nombre, select de unidad de medida (9 unidades: bag, bottle, box, container, kg, lata, lt, pack, unit), sección "Precios por tienda" con edición inline (fila tienda-precio, tap para editar, X para quitar, formulario alta con select de tienda disponible + input decimal + ✓/✗, botón "Agregar precio", estados vacíos). Validación: nombre no vacío, unidad seleccionada, nombre único case-insensitive (excluyendo el propio ID en edición).

6. **Tiendas**: mismo patrón que Productos, tarjetas con punto de color (12dp) + descripción, selección múltiple con borrado con confirmación de cascada (precios).

7. **Form de tienda**: input de nombre, selector de 9 swatches de color (círculos 32dp, selección con borde 3dp). Footer [Eliminar] [Cancelar] + Guardar.

### Main menu (drawer)

Drawer lateral derecho de 280dp, animado (300ms), backdrop 50% negro, cerrable por tap en backdrop o swipe. Título "EasyPocket" arriba. Opciones:

1. **Tema**: Claro, Oscuro, Sistema (check en activo).
2. **Idioma**: English, Español.
3. **Exportar Data**: abre SAF `ACTION_CREATE_DOCUMENT`.
4. **Importar Data**: abre SAF `ACTION_OPEN_DOCUMENT`.
5. **Restablecer valores** (rojo): confirmación — borra todo y re-inserta seed demo.
6. **Acerca de**: bottom sheet 40% con logo, "EasyPocket", "Developed by Rodney Marín", "Version 1.0.0".

### Componentes compartidos

- `Button` (variantes primary/secondary/destructive, tamaño default 40dp / icon 40×40dp, radius 8, spinner loading, disabled opacity 0.35).
- `BottomSheet` modal inferior deslizable para cerrar, esquinas 20dp, botón X, altura por fracción de pantalla.
- `Tag` pill radius 999.
- `PressableCard`, `SearchInput`, `Select`, `DropdownMenu` (custom como modales).
- Sistema de toasts con cola (success/error/info/warning, ~3s).
- Confirm dialogs destructivos (equivalente a ConfirmDeleteSheet).

### Tema

Paletas exactas de la original (`src/lib/theme/colors.ts`):

- Light: background `#fff`, cardBackground `#fff`, text `#000`, textSecondary `#666`, border `#e0e0e0`, surface `#f0f0f0`, surfaceText `#333`, primary `#4A5DF9`, destructive `#FFE0E0`, destructiveBorder `#E05555`, tabBarInactive `#8e8e93`, panelBackground `#fff`, panelText `#333`, panelBorder `#e0e0e0`, placeholderText `#999`.
- Dark: background `#121212`, cardBackground `#1c1c1c`, text `#f5f5f5`, textSecondary `#aaa`, border `#333`, surface `#2c2c2c`, surfaceText `#ccc`, primary `#4A5DF9`, destructive `#4A1C1C`, destructiveBorder `#d95050`, tabBarInactive `#636366`, panelBackground `#1c1c1c`, panelText `#f5f5f5`, panelBorder `#38383a`, placeholderText `#666`.

9 colores de tienda con variante light/dark (de `src/lib/store-colors.ts`): light `#D4D4D8, #99F6E4, #86EFAC, #93C5FD, #C4B5FD, #F9A8D4, #FCA5A5, #FDBA74, #FDE047` / dark `#6B7280, #0D9488, #059669, #2563EB, #7C3AED, #BE185D, #DC2626, #EA580C, #CA8A04`. Tags de tienda: fondo alpha 0.35 (light) / 0.2 (dark), texto shadeColor -0.55 (light) / +0.75 (dark).

Fuente del sistema, dynamic color desactivado (fidelidad visual).

## 3. Exportar / Importar Data

### Exportar

1. Menú → "Exportar Data" → `ACTION_CREATE_DOCUMENT` (mime `application/json`, nombre `easypocket-backup-YYYYMMDD-HHmm.json`).
2. Serializa con kotlinx.serialization:

```json
{
  "version": 1,
  "exportedAt": "2026-08-27T12:00:00Z",
  "stores": [ { "id": "...", "description": "...", "color": 0 } ],
  "products": [ { "id": "...", "productName": "...", "unitOfMeasurement": "kg" } ],
  "prices": [ { "productId": "...", "storeId": "...", "value": 10.5 } ],
  "shoppingLists": [ { "id": "...", "title": "..." } ],
  "listItems": [ { "id": 1, "shoppingListId": "...", "productId": "...", "storeId": null, "quantity": 2, "done": false, "pinned": false } ]
}
```

3. Escribe al stream del URI devuelto por SAF. Éxito → toast success; error → toast error.

### Importar

1. Menú → "Importar Data" → `ACTION_OPEN_DOCUMENT` (mime `application/json` + `application/octet-stream`).
2. Parseo y validación: estructura correcta, referencias de items → listas/productos existentes en el archivo, precios → productos/tiendas existentes. Archivo inválido → toast error, no toca la DB.
3. Confirmación destructiva: "esto reemplazará todos tus datos actuales".
4. Transacción Room: borra todas las tablas, inserta contenido del archivo (conservando los `id` de items para no romper el orden).
5. Refresca todas las pantallas; toast success o error.

### Restablecer valores

Borra todas las tablas y re-inserta el seed demo (2 tiendas "Demo Store"/"Test Store", 10 productos con precios en ambas tiendas, 3 listas "Lista Semanal - Verduras", "Lácteos y Desayuno", "Carnicería"). Idéntico al seed de `src/lib/seed.ts`.

## 4. Lógica de negocio

- **Total de lista** = Σ(precio del producto en la tienda del item × cantidad). Item sin tienda o sin precio en esa tienda → aporta 0. **Carrito** = Σ solo de items done. Ambos respetan el filtro de tienda activo.
- **Copia al portapapeles** (ClipboardManager): formato `Nombre ... cantidad unidad` por línea.
- **Búsqueda**: normaliza acentos (NFD, strip diacríticos), case-insensitive, debounce 300ms.
- **Orden de items**: pendientes (fijados primero, luego alfabético) → sección "Completado" (alfabético).
- **Cantidad**: decimal positiva, validación `^\d*\.?\d*$`.
- **Validación nombre producto**: único case-insensitive (excluyendo ID propio en edición).

## 5. Manejo de errores

- Fallo de inicialización de DB → pantalla de error con botón Retry (equivalente a la original).
- Errores de IO/DB/export/import → toasts.
- Confirmaciones destructivas para: eliminar lista, eliminar items completados, eliminar selección, reset de fábrica, import de datos, borrado en cascada de productos/tiendas.

## 6. Testing

- **Unit tests** (Room in-memory + Robolectric):
  - Repositorios: CRUD completo, cascada de borrados, validaciones.
  - Lógica de totales y ordenamiento de items.
  - Parser de backup: JSON válido, corrupto, con referencias rotas.
  - Validaciones: nombre duplicado, cantidad.
- **UI**: verificación manual.

## 7. Build

- Gradle Kotlin DSL, version catalog.
- minSdk 26, compileSdk/targetSdk 35.
- Dependencias: Compose BOM, Material3, Navigation Compose, Hilt, Room (KSP), kotlinx.serialization, DataStore Preferences.
- Sin permisos especiales (SAF no requiere permisos de storage).
- `applicationId com.easypocket.mobile`, `versionName 1.0.0`, `versionCode 1`.
- Icono adaptativo con fondo `#d9006f` (reutilizando el arte de la original con el nuevo branding).

## 8. Fuera de alcance

- Cloud/Sincronización/Auth (eliminado por diseño).
- Versión web.
- Escaneo de códigos de barras, notificaciones push, historial de compras (no existían en la original).
- Migración de datos desde EasyBuy instalada (la importación JSON cubre el caso manualmente).
