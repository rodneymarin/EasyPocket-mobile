# EasyPocket — Plan de Implementación

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Portar EasyBuy (React Native + Expo, en `/home/rodney/Documents/Dev/easybuy-mobile/`) a una app Android nativa Kotlin llamada EasyPocket, misma UI y features, sin cloud, con Export/Import de data en JSON.

**Architecture:** Single-activity Jetpack Compose con `HorizontalPager` de 3 tabs (Listas, Productos, Tiendas) y Navigation Compose para pantallas de detalle/form. MVVM con StateFlow, Hilt para DI, Room para persistencia (esquema equivalente al SQLite de la original), DataStore para settings (tema/idioma).

**Tech Stack:** Kotlin 2.0.21, AGP 8.7.3, Compose BOM 2024.12.01, Room 2.6.1 (KSP), Hilt 2.52, kotlinx.serialization 1.7.3, DataStore 1.1.1, Navigation Compose 2.8.5, JUnit4 + Robolectric 4.14.1.

**Spec:** `docs/superpowers/specs/2026-08-27-easypocket-port-design.md` (leer junto con este plan)

## Global Constraints

- Package/applicationId: `com.easypocket.mobile`. versionName `1.0.0`, versionCode 1.
- minSdk 26, compileSdk 35, targetSdk 35. JDK 21 (disponible en el entorno). Gradle 8.14.3 (wrapper copiado del proyecto original).
- ANDROID_HOME ya configurado (`/home/rodney/Android/Sdk`), API 35 + build-tools 35.0.0 instalados.
- UI en inglés y español (port de `/home/rodney/Documents/Dev/easybuy-mobile/src/lib/i18n/translations.ts`). Idioma por defecto: inglés.
- Paleta exacta del spec (light/dark + 9 store colors). Primary `#4A5DF9`. NO dynamic color.
- Sin Supabase, sin login, sin web. Solo data local.
- Sin permisos declarados en el manifest (SAF no los requiere).
- Todos los commits en inglés, estilo convencional (`feat:`, `test:`, `chore:`).
- Nombre visible de la app: "EasyPocket".
- **Fuente de verdad para copiar detalles**: el proyecto original en `/home/rodney/Documents/Dev/easybuy-mobile/`. Cuando una tarea diga "portar X.ts", copiar el contenido fielmente (traduciendo TS→Kotlin), no reimprovisar.
- **Nota de refinamiento vs spec:** la tabla Room `settings` no se crea (redundante); tema e idioma viven solo en DataStore, al igual que la data exportable NO incluye settings.

---

### Task 1: Scaffold del proyecto Gradle + Compose

**Files:**
- Create: `settings.gradle.kts`, `build.gradle.kts` (raíz), `gradle/libs.versions.toml`, `app/build.gradle.kts`, `app/proguard-rules.pro`, `app/src/main/AndroidManifest.xml`, `app/src/main/java/com/easypocket/mobile/EasyPocketApp.kt`, `app/src/main/java/com/easypocket/mobile/MainActivity.kt`, `app/src/main/res/values/strings.xml`, `app/src/main/res/values/themes.xml`, `app/src/main/res/xml/backup_rules.xml`, `app/src/main/res/xml/data_extraction_rules.xml`, `.gitignore`
- Copy: wrapper de Gradle desde `/home/rodney/Documents/Dev/easybuy-mobile/android/` (`gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`, `gradle/wrapper/gradle-wrapper.properties`)

**Interfaces:**
- Produces: proyecto que compila con `./gradlew assembleDebug`; `MainActivity` muestra un `Text("EasyPocket")` centrado.

- [ ] **Step 1: Copiar wrapper y crear estructura**

```bash
cd /home/rodney/Documents/Dev/EasyPocket-mobile
mkdir -p app/src/main/java/com/easypocket/mobile app/src/main/res/values app/src/main/res/xml gradle
cp /home/rodney/Documents/Dev/easybuy-mobile/android/gradlew .
cp /home/rodney/Documents/Dev/easybuy-mobile/android/gradlew.bat .
mkdir -p gradle/wrapper
cp /home/rodney/Documents/Dev/easybuy-mobile/android/gradle/wrapper/gradle-wrapper.jar gradle/wrapper/
cp /home/rodney/Documents/Dev/easybuy-mobile/android/gradle/wrapper/gradle-wrapper.properties gradle/wrapper/
chmod +x gradlew
```

`.gitignore`:
```
*.iml
.gradle/
local.properties
.idea/
.DS_Store
build/
app/build/
captures/
.externalNativeBuild/
.cxx/
```

- [ ] **Step 2: `gradle/libs.versions.toml`**

```toml
[versions]
agp = "8.7.3"
kotlin = "2.0.21"
ksp = "2.0.21-1.0.28"
composeBom = "2024.12.01"
activityCompose = "1.9.3"
coreKtx = "1.15.0"
lifecycle = "2.8.7"
navigationCompose = "2.8.5"
hilt = "2.52"
hiltNavigationCompose = "1.2.0"
room = "2.6.1"
datastore = "1.1.1"
serializationJson = "1.7.3"
junit = "4.13.2"
robolectric = "4.14.1"
androidxTestCore = "1.6.1"
coroutinesTest = "1.9.0"
appcompat = "1.7.0"

[libraries]
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "coreKtx" }
androidx-activity-compose = { group = "androidx.activity", name = "activity-compose", version.ref = "activityCompose" }
androidx-lifecycle-runtime-ktx = { group = "androidx.lifecycle", name = "lifecycle-runtime-ktx", version.ref = "lifecycle" }
androidx-lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycle" }
compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
compose-ui = { group = "androidx.compose.ui", name = "ui" }
compose-ui-graphics = { group = "androidx.compose.ui", name = "ui-graphics" }
compose-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
compose-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
compose-material3 = { group = "androidx.compose.material3", name = "material3" }
compose-material-icons-extended = { group = "androidx.compose.material", name = "material-icons-extended" }
navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigationCompose" }
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler = { group = "com.google.dagger", name = "hilt-android-compiler", version.ref = "hilt" }
hilt-navigation-compose = { group = "androidx.hilt", name = "hilt-navigation-compose", version.ref = "hiltNavigationCompose" }
room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }
room-testing = { group = "androidx.room", name = "room-testing", version.ref = "room" }
datastore-preferences = { group = "androidx.datastore", name = "datastore-preferences", version.ref = "datastore" }
serialization-json = { group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-json", version.ref = "serializationJson" }
junit = { group = "junit", name = "junit", version.ref = "junit" }
robolectric = { group = "org.robolectric", name = "robolectric", version.ref = "robolectric" }
androidx-test-core = { group = "androidx.test", name = "core-ktx", version.ref = "androidxTestCore" }
coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "coroutinesTest" }
appcompat = { group = "androidx.appcompat", name = "appcompat", version.ref = "appcompat" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
```

- [ ] **Step 3: `settings.gradle.kts` y `build.gradle.kts` raíz**

`settings.gradle.kts`:
```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "EasyPocket"
include(":app")
```

`build.gradle.kts`:
```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
}
```

- [ ] **Step 4: `app/build.gradle.kts`**

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.easypocket.mobile"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.easypocket.mobile"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
    testOptions { unitTests { isIncludeAndroidResources = true } }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.navigation.compose)
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    ksp(libs.hilt.compiler)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.datastore.preferences)
    implementation(libs.serialization.json)
    debugImplementation(libs.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.room.testing)
}
```

- [ ] **Step 5: Manifest, resources y código base**

`app/src/main/AndroidManifest.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <application
        android:name=".EasyPocketApp"
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:supportsRtl="true"
        android:theme="@style/Theme.EasyPocket">
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:windowSoftInputMode="adjustResize">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

`app/src/main/res/values/strings.xml`:
```xml
<resources>
    <string name="app_name">EasyPocket</string>
</resources>
```

`app/src/main/res/values/themes.xml` (parent oscuro para evitar flash blanco; Compose controla el tema real):
```xml
<resources>
    <style name="Theme.EasyPocket" parent="android:Theme.Material.NoActionBar">
        <item name="android:statusBarColor">@android:color/transparent</item>
        <item name="android:windowLightStatusBar">false</item>
    </style>
</resources>
```

`app/src/main/res/xml/backup_rules.xml` y `data_extraction_rules.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<full-backup-content />
```
```xml
<?xml version="1.0" encoding="utf-8"?>
<data-extraction-rules>
    <cloud-backup />
</data-extraction-rules>
```

`EasyPocketApp.kt`:
```kotlin
package com.easypocket.mobile

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class EasyPocketApp : Application()
```

`MainActivity.kt`:
```kotlin
package com.easypocket.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("EasyPocket")
            }
        }
    }
}
```

Crear iconos launcher mínimos: `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml` y `ic_launcher_round.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
</adaptive-icon>
```
`res/values/colors.xml` con `<color name="ic_launcher_background">#D9006F</color>`.
`res/drawable/ic_launcher_foreground.xml`: vector de 108dp con un glifo simple de bolsa de compras centrado (path de un rectángulo redondeado con asa), blanco sobre transparente:
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp" android:height="108dp"
    android:viewportWidth="108" android:viewportHeight="108">
    <path android:fillColor="#FFFFFF"
        android:pathData="M42,42c0,-3.3 2.7,-6 6,-6h12c3.3,0 6,2.7 6,6v2h8c1.1,0 2,0.9 2,2v26c0,1.1 -0.9,2 -2,2H34c-1.1,0 -2,-0.9 -2,-2V46c0,-1.1 0.9,-2 2,-2h8v-2zM46,42v2h16v-2c0,-1.1 -0.9,-2 -2,-2h-12c-1.1,0 -2,0.9 -2,2z"/>
</vector>
```
Nota: el manifest de API 26+ con `mipmap-anydpi-v26` cubre todos los dispositivos (minSdk 26); no se necesitan PNGs por densidad.

- [ ] **Step 6: Verificar build**

```bash
./gradlew assembleDebug
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: Commit**

```bash
git add -A && git commit -m "chore: scaffold Android project with Compose, Hilt, Room setup"
```

---

### Task 2: Modelos de dominio

**Files:**
- Create: `app/src/main/java/com/easypocket/mobile/domain/Models.kt`, `app/src/main/java/com/easypocket/mobile/domain/Units.kt`
- Test: `app/src/test/java/com/easypocket/mobile/domain/UnitsTest.kt`

**Interfaces:**
- Produces: `Store(id: String, description: String, color: Int)`, `Price(storeId: String, value: Double)`, `Product(id: String, productName: String, unitOfMeasurement: UnitOfMeasurement, prices: List<Price>)`, `ShoppingListItem(id: Long, productId: String, quantity: Double, storeId: String?, done: Boolean, pinned: Boolean)`, `ShoppingList(id: String, title: String, items: List<ShoppingListItem>)`, `enum class UnitOfMeasurement { BAG, BOTTLE, BOX, CONTAINER, KG, LATA, LT, PACK, UNIT }` con `UnitOfMeasurement.fromRaw(String): UnitOfMeasurement?` y `raw: String`.

- [ ] **Step 1: Test failing**

`UnitsTest.kt`:
```kotlin
package com.easypocket.mobile.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UnitsTest {
    @Test
    fun `fromRaw maps known values`() {
        assertEquals(UnitOfMeasurement.KG, UnitOfMeasurement.fromRaw("kg"))
        assertEquals(UnitOfMeasurement.LATA, UnitOfMeasurement.fromRaw("lata"))
        assertEquals(UnitOfMeasurement.UNIT, UnitOfMeasurement.fromRaw("unit"))
    }

    @Test
    fun `fromRaw returns null for unknown`() {
        assertNull(UnitOfMeasurement.fromRaw("gallon"))
        assertNull(UnitOfMeasurement.fromRaw(""))
    }

    @Test
    fun `raw roundtrip`() {
        UnitOfMeasurement.entries.forEach { assertEquals(it, UnitOfMeasurement.fromRaw(it.raw)) }
    }
}
```

- [ ] **Step 2: Run test — verify fail**

```bash
./gradlew test --tests "com.easypocket.mobile.domain.UnitsTest"
```
Expected: FAIL (unresolved reference).

- [ ] **Step 3: Implementación**

`domain/Units.kt`:
```kotlin
package com.easypocket.mobile.domain

enum class UnitOfMeasurement(val raw: String) {
    BAG("bag"), BOTTLE("bottle"), BOX("box"), CONTAINER("container"),
    KG("kg"), LATA("lata"), LT("lt"), PACK("pack"), UNIT("unit");

    companion object {
        fun fromRaw(raw: String): UnitOfMeasurement? =
            entries.firstOrNull { it.raw == raw }
    }
}
```

`domain/Models.kt`:
```kotlin
package com.easypocket.mobile.domain

data class Store(val id: String, val description: String, val color: Int)
data class Price(val storeId: String, val value: Double)
data class Product(
    val id: String,
    val productName: String,
    val unitOfMeasurement: UnitOfMeasurement,
    val prices: List<Price> = emptyList(),
)
data class ShoppingListItem(
    val id: Long,
    val productId: String,
    val quantity: Double,
    val storeId: String?,
    val done: Boolean = false,
    val pinned: Boolean = false,
)
data class ShoppingList(val id: String, val title: String, val items: List<ShoppingListItem> = emptyList())
```

- [ ] **Step 4: Run test — verify pass**

```bash
./gradlew test --tests "com.easypocket.mobile.domain.UnitsTest"
```
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add -A && git commit -m "feat: domain models and unit of measurement enum"
```

---

### Task 3: i18n (EN/ES) + settings DataStore

**Files:**
- Create: `app/src/main/java/com/easypocket/mobile/i18n/Translations.kt`, `app/src/main/java/com/easypocket/mobile/i18n/Language.kt`, `app/src/main/java/com/easypocket/mobile/settings/SettingsRepository.kt`, `app/src/main/java/com/easypocket/mobile/settings/ThemeMode.kt`
- Test: `app/src/test/java/com/easypocket/mobile/i18n/TranslationsTest.kt`

**Interfaces:**
- Produces:
  - `enum class Language(val code: String) { ENGLISH("en"), SPANISH("es") }` con `fromCode(String?)`.
  - `fun t(key: String, language: Language, params: Map<String, String> = emptyMap()): String` — devuelve la clave misma si no existe.
  - `enum class ThemeMode { LIGHT, DARK, SYSTEM }` con `fromName(String?)`.
  - `class SettingsRepository(private val context: Context)` con `suspend fun languageFlow(): Flow<Language>`, `themeModeFlow(): Flow<ThemeMode>`, `setLanguage(Language)`, `setThemeMode(ThemeMode)` (Hilt `@Singleton`, proveído en Task 5).

- [ ] **Step 1: Portear translations.ts**

Leer `/home/rodney/Documents/Dev/easybuy-mobile/src/lib/i18n/translations.ts` completo. Crear `i18n/Translations.kt` con un `Map<String, String>` por idioma (port de las ~190 claves). Estructura:

```kotlin
package com.easypocket.mobile.i18n

val englishStrings: Map<String, String> = mapOf(
    // ... TODAS las claves del objeto "en" de translations.ts, clave por clave.
    // Ejemplos (valores reales tomados del archivo):
    "lists.title" to "Lists",
    "common.cancel" to "Cancel",
    "common.save" to "Save",
    // ...
)

val spanishStrings: Map<String, String> = mapOf(
    // ... TODAS las claves del objeto "es", mismos keys.
)

fun t(key: String, language: Language, params: Map<String, String> = emptyMap()): String {
    val template = stringsFor(language)[key] ?: return key
    var result = template
    params.forEach { (k, v) -> result = result.replace("{$k}", v) }
    return result
}

fun stringsFor(language: Language): Map<String, String> =
    if (language == Language.SPANISH) spanishStrings else englishStrings
```

Reglas del port: copiar cada par clave/valor literalmente. Eliminar las claves relacionadas con cloud/auth/data-source que ya no aplican (`menu.dataSource`, `menu.cloud`, `menu.local`, `menu.signOut`, `auth.*`, `dataSource.*` y similares — enumerarlas mirando el archivo y dejar constancia en el commit). Mantener `menu.exportData` y `menu.importData` (existen en el original).

`i18n/Language.kt`:
```kotlin
package com.easypocket.mobile.i18n

enum class Language(val code: String) {
    ENGLISH("en"), SPANISH("es");
    companion object {
        fun fromCode(code: String?): Language =
            entries.firstOrNull { it.code == code } ?: ENGLISH
    }
}
```

- [ ] **Step 2: Test de interpolación y fallback**

`TranslationsTest.kt`:
```kotlin
package com.easypocket.mobile.i18n

import org.junit.Assert.assertEquals
import org.junit.Test

class TranslationsTest {
    @Test
    fun `missing key falls back to key`() {
        assertEquals("nope.missing", t("nope.missing", Language.ENGLISH))
    }

    @Test
    fun `interpolates params`() {
        val en = t("lists.showing", Language.ENGLISH, mapOf("count" to "3"))
        val es = t("lists.showing", Language.SPANISH, mapOf("count" to "3"))
        // Ajustar el nombre real de la clave "Mostrando N listas" según translations.ts
        assert(en.contains("3") && es.contains("3"))
    }

    @Test
    fun `english and spanish share the same key set`() {
        assertEquals(englishStrings.keys, spanishStrings.keys)
    }
}
```
Nota: si la clave real en translations.ts difiere (p. ej. `lists.showingCount`), usar la clave real y verificar que ambos tests compilen contra claves existentes. El tercer test es la protección importante: los dos mapas deben tener exactamente las mismas claves.

- [ ] **Step 3: Run tests — verify pass (corregir claves si fallan)**

```bash
./gradlew test --tests "com.easypocket.mobile.i18n.TranslationsTest"
```

- [ ] **Step 4: Settings DataStore**

`settings/ThemeMode.kt`:
```kotlin
package com.easypocket.mobile.settings

enum class ThemeMode {
    LIGHT, DARK, SYSTEM;
    companion object {
        fun fromName(name: String?): ThemeMode =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: SYSTEM
    }
}
```

`settings/SettingsRepository.kt`:
```kotlin
package com.easypocket.mobile.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.easypocket.mobile.i18n.Language
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    private object Keys {
        val LANGUAGE = stringPreferencesKey("language")
        val THEME = stringPreferencesKey("theme")
    }

    val languageFlow: Flow<Language> =
        context.settingsDataStore.data.map { p -> Language.fromCode(p[Keys.LANGUAGE]) }

    val themeModeFlow: Flow<ThemeMode> =
        context.settingsDataStore.data.map { p -> ThemeMode.fromName(p[Keys.THEME]) }

    suspend fun setLanguage(language: Language) {
        context.settingsDataStore.edit { it[Keys.LANGUAGE] = language.code }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.settingsDataStore.edit { it[Keys.THEME] = mode.name }
    }
}
```
(El binding Hilt se añade en Task 5; la clase queda lista aquí.)

- [ ] **Step 5: Run all tests + build**

```bash
./gradlew test assembleDebug
```
Expected: PASS, BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add -A && git commit -m "feat: i18n EN/ES ported from EasyBuy and DataStore settings"
```

---

### Task 4: Capa Room (entities, DAOs, database)

**Files:**
- Create: `app/src/main/java/com/easypocket/mobile/data/local/Entities.kt`, `app/src/main/java/com/easypocket/mobile/data/local/Daos.kt`, `app/src/main/java/com/easypocket/mobile/data/local/EasyPocketDatabase.kt`, `app/src/main/java/com/easypocket/mobile/data/local/Relations.kt`
- Test: `app/src/test/java/com/easypocket/mobile/data/local/DatabaseTest.kt`

**Interfaces:**
- Produces:
  - `@Database EasyPocketDatabase` (version 1) con `storeDao(): StoreDao`, `productDao(): ProductDao`, `priceDao(): PriceDao`, `listDao(): ShoppingListDao`.
  - DAOs (todas suspend salvo los Flow):
    - `StoreDao`: `getAll(): Flow<List<StoreEntity>>`, `insert(StoreEntity)`, `update(StoreEntity)`, `deleteByIds(ids: List<String>)`
    - `ProductDao`: `getAll(): Flow<List<ProductEntity>>`, `getByName(name: String): ProductEntity?`, `insert(ProductEntity)`, `update(ProductEntity)`, `deleteByIds(ids: List<String>)`
    - `PriceDao`: `getAll(): Flow<List<PriceEntity>>`, `insertAll(List<PriceEntity>)` (REPLACE), `deleteForProduct(productId: String)`, `deleteForStores(storeIds: List<String>)`
    - `ShoppingListDao`: `getAll(): Flow<List<ShoppingListWithItems>>`, `getById(id: String): ShoppingListWithItems?`, `insert(ShoppingListEntity)`, `updateTitle(id: String, title: String)`, `deleteById(id: String)`, `insertItem(ShoppingListItemEntity): Long`, `updateItem(ShoppingListItemEntity)`, `toggleItemDone(id: Long, done: Boolean)`, `removeItems(ids: List<Long>)`, `moveItems(ids: List<Long>, toListId: String)`, `pinItems(ids: List<Long>, pinned: Boolean)`, `unpinAll(listId: String)`, `removeCompleted(listId: String)`
  - `data class ShoppingListWithItems(@Embedded val list: ShoppingListEntity, @Relation val items: List<ShoppingListItemEntity>)`

- [ ] **Step 1: Entities y POJOs**

`data/local/Entities.kt`:
```kotlin
package com.easypocket.mobile.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "stores")
data class StoreEntity(
    @PrimaryKey val id: String,
    val description: String,
    val color: Int = 0,
)

@Entity(tableName = "products", indices = [Index("product_name")])
data class ProductEntity(
    @PrimaryKey val id: String,
    val productName: String,
    val unitOfMeasurement: String,
)

@Entity(
    tableName = "product_prices",
    primaryKeys = ["product_id", "store_id"],
    foreignKeys = [
        ForeignKey(entity = ProductEntity::class, parentColumns = ["id"],
            childColumns = ["product_id"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = StoreEntity::class, parentColumns = ["id"],
            childColumns = ["store_id"], onDelete = ForeignKey.CASCADE),
    ],
)
data class PriceEntity(
    val productId: String,
    val storeId: String,
    val value: Double,
)

@Entity(tableName = "shopping_lists")
data class ShoppingListEntity(
    @PrimaryKey val id: String,
    val title: String,
)

@Entity(
    tableName = "shopping_list_items",
    foreignKeys = [
        ForeignKey(entity = ShoppingListEntity::class, parentColumns = ["id"],
            childColumns = ["shopping_list_id"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = ProductEntity::class, parentColumns = ["id"],
            childColumns = ["product_id"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = StoreEntity::class, parentColumns = ["id"],
            childColumns = ["store_id"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [Index("shopping_list_id"), Index("product_id"), Index("store_id")],
)
data class ShoppingListItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val shoppingListId: String,
    val productId: String,
    val storeId: String?,
    val quantity: Double,
    val done: Boolean = false,
    val pinned: Boolean = false,
)
```

`data/local/Relations.kt`:
```kotlin
package com.easypocket.mobile.data.local

import androidx.room.Embedded
import androidx.room.Relation

data class ShoppingListWithItems(
    @Embedded val list: ShoppingListEntity,
    @Relation(parentColumn = "id", entityColumn = "shopping_list_id")
    val items: List<ShoppingListItemEntity>,
)
```

- [ ] **Step 2: Test failing de DAOs (Room in-memory + Robolectric)**

`DatabaseTest.kt`:
```kotlin
package com.easypocket.mobile.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DatabaseTest {
    private lateinit var db: EasyPocketDatabase

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, EasyPocketDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() { db.close() }

    @Test
    fun `delete store cascades prices and nulls item storeId`() = runTest {
        db.storeDao().insert(StoreEntity("s1", "Store 1", 0))
        db.productDao().insert(ProductEntity("p1", "Milk", "lt"))
        db.priceDao().insertAll(listOf(PriceEntity("p1", "s1", 10.0)))
        db.listDao().insert(ShoppingListEntity("l1", "Lista"))
        db.listDao().insertItem(ShoppingListItemEntity(shoppingListId = "l1", productId = "p1", storeId = "s1", quantity = 1.0))

        db.storeDao().deleteByIds(listOf("s1"))

        assertTrue(db.priceDao().getAll().first().isEmpty())
        val item = db.listDao().getById("l1")!!.items.first()
        assertNull(item.storeId)
    }

    @Test
    fun `delete product cascades prices and items`() = runTest {
        db.storeDao().insert(StoreEntity("s1", "Store 1", 0))
        db.productDao().insert(ProductEntity("p1", "Milk", "lt"))
        db.priceDao().insertAll(listOf(PriceEntity("p1", "s1", 10.0)))
        db.listDao().insert(ShoppingListEntity("l1", "Lista"))
        db.listDao().insertItem(ShoppingListItemEntity(shoppingListId = "l1", productId = "p1", storeId = null, quantity = 1.0))

        db.productDao().deleteByIds(listOf("p1"))

        assertTrue(db.priceDao().getAll().first().isEmpty())
        assertTrue(db.listDao().getById("l1")!!.items.isEmpty())
    }

    @Test
    fun `delete list cascades items`() = runTest {
        db.productDao().insert(ProductEntity("p1", "Milk", "lt"))
        db.listDao().insert(ShoppingListEntity("l1", "Lista"))
        db.listDao().insertItem(ShoppingListItemEntity(shoppingListId = "l1", productId = "p1", storeId = null, quantity = 1.0))

        db.listDao().deleteById("l1")

        assertTrue(db.listDao().getAll().first().isEmpty())
    }

    @Test
    fun `getByName is case insensitive`() = runTest {
        db.productDao().insert(ProductEntity("p1", "Leche Entera", "lt"))
        assertEquals("p1", db.productDao().getByName("leche entera")!!.id)
        assertNull(db.productDao().getByName("leche"))
    }

    @Test
    fun `moveItems relocates items to another list`() = runTest {
        db.productDao().insert(ProductEntity("p1", "Milk", "lt"))
        db.listDao().insert(ShoppingListEntity("l1", "A"))
        db.listDao().insert(ShoppingListEntity("l2", "B"))
        val id = db.listDao().insertItem(ShoppingListItemEntity(shoppingListId = "l1", productId = "p1", storeId = null, quantity = 1.0))

        db.listDao().moveItems(listOf(id), "l2")

        assertEquals(0, db.listDao().getById("l1")!!.items.size)
        assertEquals(1, db.listDao().getById("l2")!!.items.size)
    }

    @Test
    fun `removeCompleted deletes only done items`() = runTest {
        db.productDao().insert(ProductEntity("p1", "Milk", "lt"))
        db.listDao().insert(ShoppingListEntity("l1", "A"))
        db.listDao().insertItem(ShoppingListItemEntity(shoppingListId = "l1", productId = "p1", storeId = null, quantity = 1.0, done = true))
        db.listDao().insertItem(ShoppingListItemEntity(shoppingListId = "l1", productId = "p1", storeId = null, quantity = 2.0, done = false))

        db.listDao().removeCompleted("l1")

        assertEquals(1, db.listDao().getById("l1")!!.items.size)
    }
}
```

- [ ] **Step 3: Run tests — verify fail**

```bash
./gradlew test --tests "com.easypocket.mobile.data.local.DatabaseTest"
```
Expected: FAIL (unresolved EasyPocketDatabase).

- [ ] **Step 4: DAOs y Database**

`data/local/Daos.kt`:
```kotlin
package com.easypocket.mobile.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface StoreDao {
    @Query("SELECT * FROM stores ORDER BY description COLLATE NOCASE")
    fun getAll(): Flow<List<StoreEntity>>

    @Insert
    suspend fun insert(store: StoreEntity)

    @Update
    suspend fun update(store: StoreEntity)

    @Query("DELETE FROM stores WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)
}

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY product_name COLLATE NOCASE")
    fun getAll(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE LOWER(product_name) = LOWER(:name) LIMIT 1")
    suspend fun getByName(name: String): ProductEntity?

    @Insert
    suspend fun insert(product: ProductEntity)

    @Update
    suspend fun update(product: ProductEntity)

    @Query("DELETE FROM products WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)
}

@Dao
interface PriceDao {
    @Query("SELECT * FROM product_prices")
    fun getAll(): Flow<List<PriceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(prices: List<PriceEntity>)

    @Query("DELETE FROM product_prices WHERE product_id = :productId")
    suspend fun deleteForProduct(productId: String)

    @Query("DELETE FROM product_prices WHERE store_id IN (:storeIds)")
    suspend fun deleteForStores(storeIds: List<String>)
}

@Dao
interface ShoppingListDao {
    @Transaction
    @Query("SELECT * FROM shopping_lists ORDER BY title COLLATE NOCASE")
    fun getAll(): Flow<List<ShoppingListWithItems>>

    @Transaction
    @Query("SELECT * FROM shopping_lists WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ShoppingListWithItems?

    @Insert
    suspend fun insert(list: ShoppingListEntity)

    @Query("UPDATE shopping_lists SET title = :title WHERE id = :id")
    suspend fun updateTitle(id: String, title: String)

    @Query("DELETE FROM shopping_lists WHERE id = :id")
    suspend fun deleteById(id: String)

    @Insert
    suspend fun insertItem(item: ShoppingListItemEntity): Long

    @Update
    suspend fun updateItem(item: ShoppingListItemEntity)

    @Query("UPDATE shopping_list_items SET done = :done WHERE id = :id")
    suspend fun toggleItemDone(id: Long, done: Boolean)

    @Query("DELETE FROM shopping_list_items WHERE id IN (:ids)")
    suspend fun removeItems(ids: List<Long>)

    @Query("UPDATE shopping_list_items SET shopping_list_id = :toListId WHERE id IN (:ids)")
    suspend fun moveItems(ids: List<Long>, toListId: String)

    @Query("UPDATE shopping_list_items SET pinned = :pinned WHERE id IN (:ids)")
    suspend fun pinItems(ids: List<Long>, pinned: Boolean)

    @Query("UPDATE shopping_list_items SET pinned = 0 WHERE shopping_list_id = :listId")
    suspend fun unpinAll(listId: String)

    @Query("DELETE FROM shopping_list_items WHERE shopping_list_id = :listId AND done = 1")
    suspend fun removeCompleted(listId: String)

    @Query("UPDATE shopping_list_items SET done = 0 WHERE shopping_list_id = :listId AND done = 1")
    suspend fun uncheckAll(listId: String)
}
```

`data/local/EasyPocketDatabase.kt`:
```kotlin
package com.easypocket.mobile.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        StoreEntity::class, ProductEntity::class, PriceEntity::class,
        ShoppingListEntity::class, ShoppingListItemEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class EasyPocketDatabase : RoomDatabase() {
    abstract fun storeDao(): StoreDao
    abstract fun productDao(): ProductDao
    abstract fun priceDao(): PriceDao
    abstract fun listDao(): ShoppingListDao
}
```

- [ ] **Step 5: Run tests — verify pass**

```bash
./gradlew test --tests "com.easypocket.mobile.data.local.DatabaseTest"
```
Expected: PASS (6 tests)

- [ ] **Step 6: Commit**

```bash
git add -A && git commit -m "feat: Room database with entities, DAOs and cascade deletes"
```

---

### Task 5: Repositories + Hilt modules + lógica de listas

**Files:**
- Create: `app/src/main/java/com/easypocket/mobile/di/AppModule.kt`, `app/src/main/java/com/easypocket/mobile/data/repository/StoreRepository.kt`, `app/src/main/java/com/easypocket/mobile/data/repository/ProductRepository.kt`, `app/src/main/java/com/easypocket/mobile/data/repository/ShoppingListRepository.kt`, `app/src/main/java/com/easypocket/mobile/domain/ListLogic.kt`
- Test: `app/src/test/java/com/easypocket/mobile/data/repository/RepositoriesTest.kt`, `app/src/test/java/com/easypocket/mobile/domain/ListLogicTest.kt`

**Interfaces:**
- Consumes: DAOs de Task 4, modelos de Task 2.
- Produces:
  - Hilt: `@Singleton EasyPocketDatabase provideDatabase(@ApplicationContext Context)`, `SettingsRepository provideSettingsRepository(Context)`, y los 3 repos como `@Singleton`.
  - `StoreRepository`: `suspend getAll(): List<Store>`, `observeAll(): Flow<List<Store>>`, `create(description: String): Store`, `update(store: Store)`, `deleteAll(ids: List<String>)`
  - `ProductRepository`: `suspend getAll(): List<Product>` (con precios), `getByName(name: String): Product?`, `create(name: String, unit: UnitOfMeasurement, prices: List<Price> = emptyList()): Product`, `update(product: Product)` (reemplaza precios), `deleteAll(ids: List<String>)`, `findDuplicate(name: String, excludeId: String?): Product?`
  - `ShoppingListRepository`: `suspend getAll(): List<ShoppingList>` (con items), `getById(id: String): ShoppingList?`, `create(title: String): ShoppingList`, `delete(id: String)`, `updateTitle(id: String, title: String)`, `addItem(listId: String, productId: String, storeId: String?, quantity: Double)`, `toggleItemDone(itemId: Long, done: Boolean)`, `updateItem(item: ShoppingListItem)`, `removeItems(ids: List<Long>)`, `moveItems(ids: List<Long>, toListId: String)`, `pinItems(ids: List<Long>, pinned: Boolean)`, `uncheckAll(listId: String)`, `removeCompleted(listId: String)`
  - `ListLogic` (objeto puro en domain):
    - `fun itemTotal(item: ShoppingListItem, product: Product?): Double` — 0 si product null, storeId null o sin precio en esa tienda; si no `price.value * quantity`
    - `fun totalAmount(list: ShoppingList, productsById: Map<String, Product>, storeFilter: String?): Double` — solo items no-done, respeta storeFilter
    - `fun cartAmount(list: ShoppingList, productsById: Map<String, Product>, storeFilter: String?): Double` — solo items done, respeta storeFilter
    - `fun orderedItems(items: List<ShoppingListItem>): List<ShoppingListItem>` — pendientes primero (pinned arriba, luego alfabético por nombre de producto — recibe `productsById` también), luego done (alfabético). Firma: `orderedItems(items: List<ShoppingListItem>, productsById: Map<String, Product>): List<ShoppingListItem>`
    - `fun clipboardText(list: ShoppingList, productsById: Map<String, Product>): String` — líneas `Nombre ... cantidad unidad` (unidad con singular/plural según cantidad, misma regla que tUnit de la original: quantity == 1.0 → singular)
    - `fun normalize(s: String): String` — lowercase + strip diacríticos (NFD)
    - `fun unitLabelKey(unit: UnitOfMeasurement, quantity: Double): String` — devuelve la clave i18n singular o plural (`units.kg.singular` / `units.kg.plural` según las claves reales de translations.ts)

- [ ] **Step 1: Test failing de ListLogic**

`ListLogicTest.kt`:
```kotlin
package com.easypocket.mobile.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ListLogicTest {
    private val milk = Product("p1", "Milk", UnitOfMeasurement.LT, listOf(Price("s1", 10.0), Price("s2", 8.5)))
    private val bread = Product("p2", "Bread", UnitOfMeasurement.UNIT, listOf(Price("s1", 2.0)))
    private val products = mapOf("p1" to milk, "p2" to bread)

    @Test
    fun `itemTotal uses price at item store`() {
        val item = ShoppingListItem(1, "p1", 2.0, "s1", done = false, pinned = false)
        assertEquals(20.0, ListLogic.itemTotal(item, milk), 0.001)
    }

    @Test
    fun `itemTotal without store is zero`() {
        val item = ShoppingListItem(1, "p1", 2.0, null, done = false, pinned = false)
        assertEquals(0.0, ListLogic.itemTotal(item, milk), 0.001)
    }

    @Test
    fun `itemTotal without price at store is zero`() {
        val item = ShoppingListItem(1, "p2", 2.0, "s2", done = false, pinned = false)
        assertEquals(0.0, ListLogic.itemTotal(item, bread), 0.001)
    }

    @Test
    fun `totalAmount counts only pending and respects store filter`() {
        val items = listOf(
            ShoppingListItem(1, "p1", 1.0, "s1", done = false, pinned = false), // 10
            ShoppingListItem(2, "p1", 1.0, "s2", done = true, pinned = false),  // 8.5 done
            ShoppingListItem(3, "p2", 3.0, "s1", done = false, pinned = false), // 6
        )
        val list = ShoppingList("l1", "Lista", items)
        assertEquals(16.0, ListLogic.totalAmount(list, products, null), 0.001)
        assertEquals(6.0, ListLogic.totalAmount(list, products, "s1"), 0.001) // solo items de s1 pendientes: 6
        assertEquals(8.5, ListLogic.cartAmount(list, products, null), 0.001)
    }

    @Test
    fun `orderedItems pending first pinned top then alphabetical`() {
        val items = listOf(
            ShoppingListItem(1, "p1", 1.0, null, done = true, pinned = false),   // Milk done
            ShoppingListItem(2, "p2", 1.0, null, done = false, pinned = true),   // Bread pinned
            ShoppingListItem(3, "p1", 1.0, null, done = false, pinned = false),  // Milk
        )
        val ordered = ListLogic.orderedItems(items, products)
        assertEquals(listOf(2L, 3L, 1L), ordered.map { it.id })
    }

    @Test
    fun `clipboardText formats name quantity unit`() {
        val items = listOf(
            ShoppingListItem(1, "p1", 2.0, null),
            ShoppingListItem(2, "p2", 1.0, null),
        )
        val text = ListLogic.clipboardText(ShoppingList("l1", "Lista", items), products)
        val lines = text.lines()
        assertEquals("Milk ... 2 lt", lines[0])      // unit label vía i18n EN: verificar clave real
        assertEquals("Bread ... 1 unit", lines[1])
    }

    @Test
    fun `normalize strips accents and lowercases`() {
        assertEquals("papa", ListLogic.normalize("Papá"))
        assertEquals("nino", ListLogic.normalize("NIÑO"))
    }
}
```
Nota: los asserts de `clipboardText` dependen del label EN real de `lt`/`unit` en translations.ts — verificar los valores exactos (`units.lt.singular`, `units.unit.singular`) y ajustar los strings esperados en consecuencia. La forma del formato (separador " ... ") viene del `copyListToClipboard` original en `ShoppingListDetailScreen.tsx` — leer ese archivo y replicar exactamente.

- [ ] **Step 2: Run — verify fail**

```bash
./gradlew test --tests "com.easypocket.mobile.domain.ListLogicTest"
```

- [ ] **Step 3: Implementar ListLogic**

`domain/ListLogic.kt`:
```kotlin
package com.easypocket.mobile.domain

import java.text.Normalizer

object ListLogic {
    fun normalize(s: String): String =
        Normalizer.normalize(s, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .lowercase()

    fun itemTotal(item: ShoppingListItem, product: Product?): Double {
        val storeId = item.storeId ?: return 0.0
        val price = product?.prices?.firstOrNull { it.storeId == storeId }?.value ?: return 0.0
        return price * item.quantity
    }

    private fun matchesFilter(item: ShoppingListItem, storeFilter: String?): Boolean =
        storeFilter == null || item.storeId == storeFilter

    fun totalAmount(list: ShoppingList, productsById: Map<String, Product>, storeFilter: String?): Double =
        list.items.filter { !it.done && matchesFilter(it, storeFilter) }
            .sumOf { itemTotal(it, productsById[it.productId]) }

    fun cartAmount(list: ShoppingList, productsById: Map<String, Product>, storeFilter: String?): Double =
        list.items.filter { it.done && matchesFilter(it, storeFilter) }
            .sumOf { itemTotal(it, productsById[it.productId]) }

    fun orderedItems(items: List<ShoppingListItem>, productsById: Map<String, Product>): List<ShoppingListItem> {
        fun nameOf(i: ShoppingListItem) = productsById[i.productId]?.productName ?: ""
        val pending = items.filter { !it.done }.sortedWith(
            compareByDescending<ShoppingListItem> { it.pinned }.thenBy { normalize(nameOf(it)) }
        )
        val done = items.filter { it.done }.sortedBy { normalize(nameOf(it)) }
        return pending + done
    }

    fun unitLabelKey(unit: UnitOfMeasurement, quantity: Double): String {
        val suffix = if (quantity == 1.0) "singular" else "plural"
        return "units.${unit.raw}.$suffix"
    }

    fun clipboardText(list: ShoppingList, productsById: Map<String, Product>): String =
        list.items.joinToString("\n") { item ->
            val product = productsById[item.productId]
            val unitKey = product?.let { unitLabelKey(it.unitOfMeasurement, item.quantity) } ?: ""
            val unitLabel = com.easypocket.mobile.i18n.t(unitKey, com.easypocket.mobile.i18n.Language.ENGLISH)
            "${product?.productName ?: ""} ... ${trimQuantity(item.quantity)} $unitLabel"
        }

    fun trimQuantity(q: Double): String =
        if (q == q.toLong().toDouble()) q.toLong().toString() else q.toString()
}
```
Ajustar el formato de `clipboardText` para que coincida EXACTAMENTE con `copyListToClipboard` de la original (`/home/rodney/Documents/Dev/easybuy-mobile/src/features/shopping-lists/ShoppingListDetailScreen.tsx`), incluyendo el idioma seleccionado (pasar `Language` como parámetro en lugar de hardcode ENGLISH: `clipboardText(list, productsById, language)`).

- [ ] **Step 4: Repositories**

`data/repository/StoreRepository.kt`:
```kotlin
package com.easypocket.mobile.data.repository

import com.easypocket.mobile.data.local.StoreDao
import com.easypocket.mobile.data.local.StoreEntity
import com.easypocket.mobile.domain.Store
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class StoreRepository @Inject constructor(private val storeDao: StoreDao) {
    suspend fun getAll(): List<Store> = storeDao.getAll().let { flow -> listFrom(flow) }
    // Nota: implementar con first() de kotlinx.coroutines.flow.first
    suspend fun listFrom(flow: Flow<List<StoreEntity>>): List<Store> = TODO()

    suspend fun create(description: String): Store {
        val store = Store(UUID.randomUUID().toString(), description, 0)
        storeDao.insert(StoreEntity(store.id, store.description, store.color))
        return store
    }

    suspend fun update(store: Store) = storeDao.update(StoreEntity(store.id, store.description, store.color))

    suspend fun deleteAll(ids: List<String>) = storeDao.deleteByIds(ids)
}
```
(El TODO anterior es indicación: implementar `getAll()` con `storeDao.getAll().first().map { it.toDomain() }`; mismo patrón con extensión privada `fun StoreEntity.toDomain() = Store(id, description, color)`.)

`data/repository/ProductRepository.kt`:
```kotlin
package com.easypocket.mobile.data.repository

import com.easypocket.mobile.data.local.PriceDao
import com.easypocket.mobile.data.local.PriceEntity
import com.easypocket.mobile.data.local.ProductDao
import com.easypocket.mobile.data.local.ProductEntity
import com.easypocket.mobile.domain.Price
import com.easypocket.mobile.domain.Product
import com.easypocket.mobile.domain.UnitOfMeasurement
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

@Singleton
class ProductRepository @Inject constructor(
    private val productDao: ProductDao,
    private val priceDao: PriceDao,
) {
    suspend fun getAll(): List<Product> {
        val products = productDao.getAll().first()
        val prices = priceDao.getAll().first().groupBy { it.productId }
        return products.map { p ->
            Product(p.id, p.productName, UnitOfMeasurement.fromRaw(p.unitOfMeasurement) ?: UnitOfMeasurement.UNIT,
                prices[p.id].orEmpty().map { Price(it.storeId, it.value) })
        }
    }

    suspend fun getByName(name: String): Product? {
        val entity = productDao.getByName(name) ?: return null
        val prices = priceDao.getAll().first().filter { it.productId == entity.id }
        return Product(entity.id, entity.productName,
            UnitOfMeasurement.fromRaw(entity.unitOfMeasurement) ?: UnitOfMeasurement.UNIT,
            prices.map { Price(it.storeId, it.value) })
    }

    suspend fun findDuplicate(name: String, excludeId: String?): Product? {
        val found = productDao.getByName(name) ?: return null
        return if (found.id == excludeId) null else getByName(found.productName)
    }

    suspend fun create(name: String, unit: UnitOfMeasurement, prices: List<Price> = emptyList()): Product {
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

    suspend fun deleteAll(ids: List<String>) = productDao.deleteByIds(ids)
}
```

`data/repository/ShoppingListRepository.kt`:
```kotlin
package com.easypocket.mobile.data.repository

import com.easypocket.mobile.data.local.ShoppingListDao
import com.easypocket.mobile.data.local.ShoppingListItemEntity
import com.easypocket.mobile.data.local.ShoppingListEntity
import com.easypocket.mobile.domain.ShoppingList
import com.easypocket.mobile.domain.ShoppingListItem
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

@Singleton
class ShoppingListRepository @Inject constructor(private val listDao: ShoppingListDao) {

    private fun withItems(relation: com.easypocket.mobile.data.local.ShoppingListWithItems) =
        ShoppingList(
            relation.list.id,
            relation.list.title,
            relation.items.map { ShoppingListItem(it.id, it.productId, it.quantity, it.storeId, it.done, it.pinned) },
        )

    suspend fun getAll(): List<ShoppingList> = listDao.getAll().first().map(::withItems)

    suspend fun getById(id: String): ShoppingList? = listDao.getById(id)?.let(::withItems)

    suspend fun create(title: String): ShoppingList {
        val list = ShoppingList(UUID.randomUUID().toString(), title)
        listDao.insert(ShoppingListEntity(list.id, list.title))
        return list
    }

    suspend fun delete(id: String) = listDao.deleteById(id)
    suspend fun updateTitle(id: String, title: String) = listDao.updateTitle(id, title)

    suspend fun addItem(listId: String, productId: String, storeId: String?, quantity: Double): Long =
        listDao.insertItem(ShoppingListItemEntity(shoppingListId = listId, productId = productId, storeId = storeId, quantity = quantity))

    suspend fun toggleItemDone(itemId: Long, done: Boolean) = listDao.toggleItemDone(itemId, done)
    suspend fun updateItem(item: ShoppingListItem) =
        listDao.updateItem(ShoppingListItemEntity(item.id, listIdFor(item.id) ?: "", item.productId, item.storeId, item.quantity, item.done, item.pinned))
    suspend fun removeItems(ids: List<Long>) = listDao.removeItems(ids)
    suspend fun moveItems(ids: List<Long>, toListId: String) = listDao.moveItems(ids, toListId)
    suspend fun pinItems(ids: List<Long>, pinned: Boolean) = listDao.pinItems(ids, pinned)
    suspend fun uncheckAll(listId: String) = listDao.uncheckAll(listId)
    suspend fun removeCompleted(listId: String) = listDao.removeCompleted(listId)
}
```
Nota: `updateItem` no debe re-consultar el listId — mejor cambiar la firma del DAO a `@Query("UPDATE shopping_list_items SET product_id = :productId, store_id = :storeId, quantity = :quantity, done = :done, pinned = :pinned WHERE id = :id")` con `updateItemFields(id: Long, productId: String, storeId: String?, quantity: Double, done: Boolean, pinned: Boolean)`. Implementar esa variante (añadir al DAO de Task 4 y usarla aquí).

- [ ] **Step 5: Hilt module**

`di/AppModule.kt`:
```kotlin
package com.easypocket.mobile.di

import android.content.Context
import androidx.room.Room
import com.easypocket.mobile.data.local.EasyPocketDatabase
import com.easypocket.mobile.data.local.PriceDao
import com.easypocket.mobile.data.local.ProductDao
import com.easypocket.mobile.data.local.ShoppingListDao
import com.easypocket.mobile.data.local.StoreDao
import com.easypocket.mobile.settings.SettingsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): EasyPocketDatabase =
        Room.databaseBuilder(context, EasyPocketDatabase::class.java, "easypocket.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideStoreDao(db: EasyPocketDatabase): StoreDao = db.storeDao()
    @Provides fun provideProductDao(db: EasyPocketDatabase): ProductDao = db.productDao()
    @Provides fun providePriceDao(db: EasyPocketDatabase): PriceDao = db.priceDao()
    @Provides fun provideListDao(db: EasyPocketDatabase): ShoppingListDao = db.listDao()

    @Provides
    @Singleton
    fun provideSettingsRepository(@ApplicationContext context: Context): SettingsRepository =
        SettingsRepository(context)
}
```

- [ ] **Step 6: Tests de repositories (Robolectric, DB in-memory inyectada manualmente)**

`RepositoriesTest.kt`:
```kotlin
package com.easypocket.mobile.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.easypocket.mobile.data.local.EasyPocketDatabase
import com.easypocket.mobile.domain.Price
import com.easypocket.mobile.domain.UnitOfMeasurement
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RepositoriesTest {
    private lateinit var db: EasyPocketDatabase
    private lateinit var stores: StoreRepository
    private lateinit var products: ProductRepository
    private lateinit var lists: ShoppingListRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, EasyPocketDatabase::class.java)
            .allowMainThreadQueries().build()
        stores = StoreRepository(db.storeDao())
        products = ProductRepository(db.productDao(), db.priceDao())
        lists = ShoppingListRepository(db.listDao())
    }

    @After
    fun tearDown() { db.close() }

    @Test
    fun `create store and update`() = runTest {
        val store = stores.create("Demo Store")
        stores.update(store.copy(description = "Renamed", color = 3))
        val all = stores.getAll()
        assertEquals("Renamed", all.first().description)
        assertEquals(3, all.first().color)
    }

    @Test
    fun `product update replaces prices`() = runTest {
        val s1 = stores.create("S1")
        val s2 = stores.create("S2")
        val product = products.create("Milk", UnitOfMeasurement.LT, listOf(Price(s1.id, 10.0)))
        products.update(product.copy(prices = listOf(Price(s2.id, 12.0))))
        val updated = products.getByName("Milk")!!
        assertEquals(1, updated.prices.size)
        assertEquals(s2.id, updated.prices.first().storeId)
    }

    @Test
    fun `findDuplicate ignores self when editing`() = runTest {
        val p = products.create("Milk", UnitOfMeasurement.LT)
        assertNotNull(products.findDuplicate("milk", excludeId = null))
        assertNull(products.findDuplicate("milk", excludeId = p.id))
        assertNotNull(products.findDuplicate("MILK", excludeId = "other"))
    }

    @Test
    fun `list item lifecycle`() = runTest {
        val p = products.create("Milk", UnitOfMeasurement.LT)
        val list = lists.create("Weekly")
        val itemId = lists.addItem(list.id, p.id, null, 2.0)
        lists.toggleItemDone(itemId, true)
        var loaded = lists.getById(list.id)!!
        assertTrue(loaded.items.first().done)
        lists.uncheckAll(list.id)
        loaded = lists.getById(list.id)!!
        assertTrue(!loaded.items.first().done)
        lists.removeCompleted(list.id)
        assertEquals(1, lists.getById(list.id)!!.items.size)
        lists.removeItems(listOf(itemId))
        assertEquals(0, lists.getById(list.id)!!.items.size)
    }
}
```

- [ ] **Step 7: Run all tests — verify pass**

```bash
./gradlew test
```
Expected: PASS

- [ ] **Step 8: Commit**

```bash
git add -A && git commit -m "feat: repositories, Hilt modules and list business logic"
```

---

### Task 6: Seed + reset

**Files:**
- Create: `app/src/main/java/com/easypocket/mobile/data/seed/SeedData.kt`, `app/src/main/java/com/easypocket/mobile/data/seed/Seeder.kt`
- Test: `app/src/test/java/com/easypocket/mobile/data/seed/SeederTest.kt`

**Interfaces:**
- Consumes: repositorios de Task 5, DB de Task 4.
- Produces: `class Seeder @Inject constructor(db + repos)` con `suspend fun seedIfEmpty()` (inserta demo solo si no hay stores) y `suspend fun resetToSeed()` (borra todas las tablas y re-inserta). Borrado de tablas vía `db.clearAllTables()`.

- [ ] **Step 1: Portear seed.ts**

Leer `/home/rodney/Documents/Dev/easybuy-mobile/src/lib/seed.ts` completo y portarlo a `SeedData.kt`: 2 tiendas ("Demo Store" color 0, "Test Store" color 1), 10 productos (Papa, Cebolla, Tomate, Leche líquida, Yogurt Firme, Queso Blanco, Mantequilla, Pechuga de Pollo, Carne Molida, Filet de Merluza) con sus unidades, precios en ambas tiendas y 3 listas con sus items — **usar los UUIDs, precios, cantidades y unidades EXACTOS del archivo original**.

```kotlin
package com.easypocket.mobile.data.seed

import com.easypocket.mobile.domain.Price
import com.easypocket.mobile.domain.Product
import com.easypocket.mobile.domain.ShoppingList
import com.easypocket.mobile.domain.ShoppingListItem
import com.easypocket.mobile.domain.Store
import com.easypocket.mobile.domain.UnitOfMeasurement

object SeedData {
    val stores: List<Store> = listOf(
        // valores exactos de seed.ts
        Store("...", "Demo Store", 0),
        Store("...", "Test Store", 1),
    )
    val products: List<Product> = listOf(/* 10 productos con prices, IDs exactos de seed.ts */)
    val lists: List<ShoppingList> = listOf(/* 3 listas, con items como pares (productId, storeId, quantity, done, pinned) de seed.ts */)
}
```
Nota: como los items usan `rowId` autoincrement y en seed.ts se insertan por orden, modelar las listas seed con items en el MISMO orden del archivo original (`List<SeedListItem>` con productId/storeId/quantity/done/pinned) para insertarlos en orden y reproducir rowIds equivalentes.

- [ ] **Step 2: Seeder + test failing**

`data/seed/Seeder.kt`:
```kotlin
package com.easypocket.mobile.data.seed

import com.easypocket.mobile.data.local.EasyPocketDatabase
import com.easypocket.mobile.data.local.PriceEntity
import com.easypocket.mobile.data.local.ProductEntity
import com.easypocket.mobile.data.local.ShoppingListItemEntity
import com.easypocket.mobile.data.local.ShoppingListEntity
import com.easypocket.mobile.data.local.StoreEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Seeder @Inject constructor(private val db: EasyPocketDatabase) {

    suspend fun seedIfEmpty() {
        if (db.storeDao().getAll().kotlinx.coroutines.flow.first().isEmpty()) insertSeed()
    }

    suspend fun resetToSeed() {
        db.withTransaction {
            db.clearAllTables()
        }
        insertSeed()
    }

    private suspend fun insertSeed() {
        SeedData.stores.forEach { db.storeDao().insert(StoreEntity(it.id, it.description, it.color)) }
        SeedData.products.forEach { p ->
            db.productDao().insert(ProductEntity(p.id, p.productName, p.unitOfMeasurement.raw))
            db.priceDao().insertAll(p.prices.map { PriceEntity(p.id, it.storeId, it.value) })
        }
        SeedData.lists.forEach { l ->
            db.listDao().insert(ShoppingListEntity(l.id, l.title))
            l.items.forEach { item ->
                db.listDao().insertItem(ShoppingListItemEntity(
                    shoppingListId = l.id, productId = item.productId,
                    storeId = item.storeId, quantity = item.quantity,
                    done = item.done, pinned = item.pinned))
            }
        }
    }
}
```
(Corregir imports; usar `androidx.room.withTransaction` y `first()` correctamente. `clearAllTables()` no es suspend — llamarlo fuera de la transacción.)

`SeederTest.kt`:
```kotlin
package com.easypocket.mobile.data.seed

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.easypocket.mobile.data.local.EasyPocketDatabase
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SeederTest {
    private lateinit var db: EasyPocketDatabase
    private lateinit var seeder: Seeder

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, EasyPocketDatabase::class.java)
            .allowMainThreadQueries().build()
        seeder = Seeder(db)
    }

    @After
    fun tearDown() { db.close() }

    @Test
    fun `seedIfEmpty inserts demo data once`() = runTest {
        seeder.seedIfEmpty()
        val count = db.storeDao().getAll().let { flow -> kotlinx.coroutines.flow.first(flow).size }
        assertEquals(2, count)
        seeder.seedIfEmpty() // no duplica
        assertEquals(2, kotlinx.coroutines.flow.first(db.storeDao().getAll()).size)
        assertEquals(10, kotlinx.coroutines.flow.first(db.productDao().getAll()).size)
        assertEquals(3, kotlinx.coroutines.flow.first(db.listDao().getAll()).size)
    }

    @Test
    fun `resetToSeed clears and reseeds`() = runTest {
        seeder.seedIfEmpty()
        db.productDao().insert(com.easypocket.mobile.data.local.ProductEntity("extra", "Extra", "kg"))
        seeder.resetToSeed()
        val products = kotlinx.coroutines.flow.first(db.productDao().getAll())
        assertEquals(10, products.size)
        assertEquals(0, products.count { it.id == "extra" })
    }
}
```
(Usar imports correctos de `first` en vez del qualified call.)

- [ ] **Step 3: Run tests — verify pass**

```bash
./gradlew test --tests "com.easypocket.mobile.data.seed.SeederTest"
```

- [ ] **Step 4: Commit**

```bash
git add -A && git commit -m "feat: demo seed and reset-to-seed"
```

---

### Task 7: Tema (paletas, store colors) y componentes UI base

**Files:**
- Create: `app/src/main/java/com/easypocket/mobile/ui/theme/Color.kt`, `app/src/main/java/com/easypocket/mobile/ui/theme/Theme.kt`, `app/src/main/java/com/easypocket/mobile/ui/theme/StoreColors.kt`, `app/src/main/java/com/easypocket/mobile/ui/components/Button.kt`, `app/src/main/java/com/easypocket/mobile/ui/components/Tag.kt`, `app/src/main/java/com/easypocket/mobile/ui/components/PressableCard.kt`, `app/src/main/java/com/easypocket/mobile/ui/components/SearchInput.kt`, `app/src/main/java/com/easypocket/mobile/ui/components/ConfirmSheet.kt`, `app/src/main/java/com/easypocket/mobile/ui/components/AppBottomSheet.kt`, `app/src/main/java/com/easypocket/mobile/ui/components/Select.kt`, `app/src/main/java/com/easypocket/mobile/ui/components/DropdownMenu.kt`, `app/src/main/java/com/easypocket/mobile/ui/components/Toasts.kt`, `app/src/main/java/com/easypocket/mobile/ui/components/ScreenHeader.kt`
- Test: `app/src/test/java/com/easypocket/mobile/ui/theme/StoreColorsTest.kt`

**Interfaces:**
- Consumes: nada previo de UI.
- Produces (usados por TODAS las pantallas):
  - `EasyPocketTheme(themeMode: ThemeMode, content)` — Material3 con `lightColorScheme`/`darkColorScheme` mapeados desde la paleta del spec; `LocalAppColors` (`CompositionLocal<AppColors>`) con TODOS los colores del spec (background, cardBackground, text, textSecondary, border, surface, surfaceText, primary, destructive, destructiveBorder, tabBarInactive, panelBackground, panelText, panelBorder, placeholderText).
  - `StoreColors.get(index: Int, isDark: Boolean): Color`, `StoreColors.textColor(index: Int, isDark: Boolean): Color` (shade -0.55 light / +0.75 dark), `StoreColors.backgroundAlpha(index: Int, isDark: Boolean): Color` (alpha 0.35 light / 0.2 dark), `StoreColors.shade(color: Color, factor: Float): Color`, `hexToRgba`.
  - `AppButton(text, onClick, variant: ButtonVariant = PRIMARY, modifier, isLoading, enabled, icon: ImageVector?)` — PRIMARY/SECONDARY/DESTRUCTIVE, altura 40dp, radius 8dp, disabled opacity 0.35, spinner cuando isLoading.
  - `IconButtonCircle(icon, onClick, variant)` — 40×40, radius 8.
  - `Tag(text, color: Color? = null, modifier)` — pill radius 999, font 11-13.
  - `PressableCard(onClick, onLongClick, modifier, content)` — fondo cardBackground, borde border, radius 8, escala/oscurecimiento al presionar.
  - `SearchInput(value, onValueChange, placeholder, modifier)` — campo redondeado con icono lupa.
  - `AppBottomSheet(visible, onDismiss, heightFraction: Float, title: String?, content)` — ModalBottomSheet con drag-to-dismiss, esquinas 20dp, botón X, altura según fracción.
  - `SelectField(label?, options: List<SelectOption>, selectedId: String?, onSelect, placeholder)` — abre dropdown modal; `data class SelectOption(val id: String, val label: String, val trailing: String? = null)`.
  - `ConfirmSheet(visible, title, message, warning?, confirmLabel, onConfirm, onDismiss)` — bottom sheet destructivo estilo ConfirmDeleteSheet de la original.
  - `ToastHost(state: ToastState)` y `class ToastState` con `fun show(message: String, type: ToastType = SUCCESS)`; cola, 3s, colores success/error/info/warning. Se consume vía `LocalToastState`.
  - `ScreenHeader(title, onMenuClick, onBackClick? = null)` — título centrado 24sp bold, hamburguesa derecha, back circular izquierdo opcional; layout paddingTop 60dp lo maneja la pantalla.

- [ ] **Step 1: Test failing de StoreColors**

`StoreColorsTest.kt`:
```kotlin
package com.easypocket.mobile.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class StoreColorsTest {
    @Test
    fun `palette has 9 light and 9 dark colors`() {
        assertEquals(9, StoreColors.light.size)
        assertEquals(9, StoreColors.dark.size)
    }

    @Test
    fun `get returns color by index`() {
        assertEquals(Color(0xFFD4D4D8), StoreColors.get(0, isDark = false))
        assertEquals(Color(0xFFCA8A04), StoreColors.get(8, isDark = true))
    }

    @Test
    fun `get clamps out of range index`() {
        assertEquals(Color(0xFFD4D4D8), StoreColors.get(-1, isDark = false))
        assertEquals(Color(0xFF6B7280), StoreColors.get(99, isDark = true))
    }
}
```

- [ ] **Step 2: Run — verify fail** → `./gradlew test --tests "com.easypocket.mobile.ui.theme.StoreColorsTest"`

- [ ] **Step 3: Implementar Color.kt, StoreColors.kt, Theme.kt**

`ui/theme/Color.kt` — paletas EXACTAS del spec (sección Tema). Definir:
```kotlin
package com.easypocket.mobile.ui.theme

import androidx.compose.ui.graphics.Color

data class AppColors(
    val background: Color, val cardBackground: Color, val text: Color,
    val textSecondary: Color, val border: Color, val surface: Color,
    val surfaceText: Color, val primary: Color, val destructive: Color,
    val destructiveBorder: Color, val tabBarInactive: Color,
    val panelBackground: Color, val panelText: Color, val panelBorder: Color,
    val placeholderText: Color,
)

val LightColors = AppColors(
    background = Color(0xFFFFFFFF), cardBackground = Color(0xFFFFFFFF),
    text = Color(0xFF000000), textSecondary = Color(0xFF666666),
    border = Color(0xFFE0E0E0), surface = Color(0xFFF0F0F0),
    surfaceText = Color(0xFF333333), primary = Color(0xFF4A5DF9),
    destructive = Color(0xFFFFE0E0), destructiveBorder = Color(0xFFE05555),
    tabBarInactive = Color(0xFF8E8E93), panelBackground = Color(0xFFFFFFFF),
    panelText = Color(0xFF333333), panelBorder = Color(0xFFE0E0E0),
    placeholderText = Color(0xFF999999),
)

val DarkColors = AppColors(
    background = Color(0xFF121212), cardBackground = Color(0xFF1C1C1C),
    text = Color(0xFFF5F5F5), textSecondary = Color(0xFFAAAAAA),
    border = Color(0xFF333333), surface = Color(0xFF2C2C2C),
    surfaceText = Color(0xFFCCCCCC), primary = Color(0xFF4A5DF9),
    destructive = Color(0xFF4A1C1C), destructiveBorder = Color(0xFFD95050),
    tabBarInactive = Color(0xFF636366), panelBackground = Color(0xFF1C1C1C),
    panelText = Color(0xFFF5F5F5), panelBorder = Color(0xFF38383A),
    placeholderText = Color(0xFF666666),
)
```

`ui/theme/StoreColors.kt`:
```kotlin
package com.easypocket.mobile.ui.theme

import androidx.compose.ui.graphics.Color

object StoreColors {
    val light = listOf(
        Color(0xFFD4D4D8), Color(0xFF99F6E4), Color(0xFF86EFAC), Color(0xFF93C5FD),
        Color(0xFFC4B5FD), Color(0xFFF9A8D4), Color(0xFFFCA5A5), Color(0xFFFDBA74),
        Color(0xFFFDE047),
    )
    val dark = listOf(
        Color(0xFF6B7280), Color(0xFF0D9488), Color(0xFF059669), Color(0xFF2563EB),
        Color(0xFF7C3AED), Color(0xFFBE185D), Color(0xFFDC2626), Color(0xFFEA580C),
        Color(0xFFCA8A04),
    )

    fun get(index: Int, isDark: Boolean): Color {
        val palette = if (isDark) dark else light
        return palette[index.coerceIn(0, palette.lastIndex)]
    }

    fun shade(color: Color, factor: Float): Color {
        // factor negativo oscurece, positivo aclara (lerp hacia negro/blanco)
        val target = if (factor < 0) Color.Black else Color.White
        return lerp(color, target, kotlin.math.abs(factor))
    }

    private fun lerp(a: Color, b: Color, t: Float) = Color(
        red = a.red + (b.red - a.red) * t,
        green = a.green + (b.green - a.green) * t,
        blue = a.blue + (b.blue - a.blue) * t,
        alpha = 1f,
    )

    fun textColor(index: Int, isDark: Boolean): Color =
        shade(get(index, isDark), if (isDark) 0.75f else -0.55f)

    fun backgroundAlpha(index: Int, isDark: Boolean): Color =
        get(index, isDark).copy(alpha = if (isDark) 0.2f else 0.35f)
}
```
(Cruzar con `/home/rodney/Documents/Dev/easybuy-mobile/src/lib/store-colors.ts` y `shadeColor`/`hexToRgba` originales para fidelidad del shade.)

`ui/theme/Theme.kt`:
```kotlin
package com.easypocket.mobile.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import com.easypocket.mobile.settings.ThemeMode

val LocalAppColors = staticCompositionLocalOf { LightColors }

@Composable
fun EasyPocketTheme(themeMode: ThemeMode, content: @Composable () -> Unit) {
    val systemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) { ThemeMode.LIGHT -> false; ThemeMode.DARK -> true; ThemeMode.SYSTEM -> systemDark }
    val appColors = if (isDark) DarkColors else LightColors
    val scheme = if (isDark) darkColorScheme(
        primary = appColors.primary, background = appColors.background,
        surface = appColors.surface, onSurface = appColors.surfaceText,
        error = appColors.destructiveBorder,
    ) else lightColorScheme(
        primary = appColors.primary, background = appColors.background,
        surface = appColors.surface, onSurface = appColors.surfaceText,
        error = appColors.destructiveBorder,
    )
    CompositionLocalProvider(LocalAppColors provides appColors) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
```

- [ ] **Step 4: Run StoreColorsTest — verify pass**

- [ ] **Step 5: Componentes UI**

Implementar cada componente listado en Interfaces. Referencias visuales exactas de la original:
- Button → `src/components/ui/button/Button.tsx`
- Tag → `src/components/ui/Tag.tsx` (o donde esté; buscar con grep en `src/components/ui/`)
- PressableCard → `src/components/ui/PressableCard.tsx`
- SearchInput → `src/components/ui/SearchInput.tsx`
- BottomSheet → `src/components/ui/BottomSheet.tsx` (drag-to-dismiss, esquinas 20, X)
- Select/DropdownMenu → `src/components/ui/Select.tsx`, `DropdownMenu.tsx`
- ConfirmDeleteSheet → `src/components/ui/ConfirmDeleteSheet.tsx`
- Toasts → `src/components/ui/toast/` (cola, tipos, 3s)
- ScreenTitle → `src/components/ui/ScreenTitle.tsx`

Leer cada archivo original y portar dimensiones/colores/comportamientos. Toast state:
```kotlin
@Stable
class ToastState {
    private val _toasts = mutableStateListOf<ToastItem>()
    val toasts: List<ToastItem> get() = _toasts
    fun show(message: String, type: ToastType = ToastType.SUCCESS) { /* enqueue, auto-remove tras 3s */ }
}
data class ToastItem(val id: Long, val message: String, val type: ToastType)
enum class ToastType { SUCCESS, ERROR, INFO, WARNING }
val LocalToastState = staticCompositionLocalOf { ToastState() }
```
`ToastHost` dibuja la cola (una sobre otra, deslizándose) usando `LaunchedEffect` por item con `delay(3000)`.

- [ ] **Step 6: Build + test**

```bash
./gradlew test assembleDebug
```

- [ ] **Step 7: Commit**

```bash
git add -A && git commit -m "feat: theme palettes, store colors and base UI components"
```

---

### Task 8: Shell de navegación (pager + bottom bar) + main menu + About + reset

**Files:**
- Create: `app/src/main/java/com/easypocket/mobile/ui/navigation/AppRoot.kt`, `app/src/main/java/com/easypocket/mobile/ui/navigation/BottomBar.kt`, `app/src/main/java/com/easypocket/mobile/ui/mainmenu/MainMenu.kt`, `app/src/main/java/com/easypocket/mobile/ui/mainmenu/AboutSheet.kt`, `app/src/main/java/com/easypocket/mobile/AppViewModel.kt`
- Modify: `app/src/main/java/com/easypocket/mobile/MainActivity.kt`

**Interfaces:**
- Consumes: `EasyPocketTheme`, `SettingsRepository`, `Seeder`, i18n `t()`, componentes de Task 7.
- Produces:
  - `AppViewModel` (`@HiltViewModel`): `val themeMode: StateFlow<ThemeMode>`, `val language: StateFlow<Language>`, `val menuVisible: Boolean` + `openMenu()/closeMenu()`, `fun setThemeMode(ThemeMode)`, `fun setLanguage(Language)`, `suspend fun resetToSeed()`, `val isReady: StateFlow<Boolean>` (false hasta que seedIfEmpty() corre; si falla → `val fatalError: StateFlow<Throwable?>` + `retryInit()`).
  - `AppRoot(vm: AppViewModel)`: Scaffold con BottomBar + HorizontalPager (3 páginas: ListsScreenHost, ProductsScreenHost, StoresScreenHost — en esta tarea son placeholders `Box` con el título del tab) + MainMenu drawer + ToastHost + pantalla de error DB con botón Retry si `fatalError != null`.
  - `BottomBar(pageCount=3, currentPage, onPageSelected, labels)` — 3 ítems con iconos `Icons.Outlined.List`/`Icons.Filled.List`, `Icons.Outlined.Cube`/`Icons.Filled.Cube`, `Icons.Outlined.Storefront`/`Icons.Filled.Storefront`, activo primary, inactivo tabBarInactive, label 10sp, borde superior 1dp border.
  - `MainMenu(visible, language, themeMode, onDismiss, onThemeSelected, onLanguageSelected, onExport, onImport, onReset, onAbout)`: drawer derecho 280dp animado 300ms, backdrop negro 50%, swipe-to-close (con `AnchoredDraggable` o `draggable` + animación), título "EasyPocket" arriba, secciones: Tema (3 opciones con check en la activa), Idioma (English/Español), separador, Exportar Data, Importar Data, Restablecer valores (rojo), Acerca de. Port visual de `/home/rodney/Documents/Dev/easybuy-mobile/src/components/ui/main-menu/MainMenu.tsx`.
  - Rutas de Navigation Compose dentro de AppRoot: `"home"` (pager), `"listDetail/{listId}"`, `"itemForm/{listId}/{itemId}"` (`itemId = -1` para nuevo), `"productForm/{productId}"` (`productId = "new"` para nuevo), `"storeForm/{storeId}"` (`storeId = "new"` para nuevo). En esta tarea solo existe `home` con placeholders; las demás rutas se registran vacías (`{}`) y se llenan en sus tareas. La página actual del pager se guarda en `rememberSaveable` para restaurarse al volver de un form.
  - `AboutSheet(visible, onDismiss)`: bottom sheet 40% altura con icono de la app, "EasyPocket", "Developed by Rodney Marín", "Version 1.0.0".

- [ ] **Step 1: AppViewModel**

```kotlin
package com.easypocket.mobile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.easypocket.mobile.data.seed.Seeder
import com.easypocket.mobile.i18n.Language
import com.easypocket.mobile.settings.SettingsRepository
import com.easypocket.mobile.settings.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class AppViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val seeder: Seeder,
) : ViewModel() {
    val themeMode: StateFlow<ThemeMode> = settings.themeModeFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.SYSTEM)
    val language: StateFlow<Language> = settings.languageFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, Language.ENGLISH)

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady

    private val _fatalError = MutableStateFlow<Throwable?>(null)
    val fatalError: StateFlow<Throwable?> = _fatalError

    private val _menuVisible = MutableStateFlow(false)
    val menuVisible: StateFlow<Boolean> = _menuVisible

    init { init() }

    fun init() {
        _fatalError.value = null
        viewModelScope.launch {
            try {
                seeder.seedIfEmpty()
                _isReady.value = true
            } catch (t: Throwable) { _fatalError.value = t }
        }
    }

    fun openMenu() { _menuVisible.value = true }
    fun closeMenu() { _menuVisible.value = false }
    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { settings.setThemeMode(mode) }
    fun setLanguage(language: Language) = viewModelScope.launch { settings.setLanguage(language) }
    fun resetToSeed() = viewModelScope.launch {
        try { seeder.resetToSeed() } catch (t: Throwable) { _fatalError.value = t }
    }
}
```

- [ ] **Step 2: MainActivity + AppRoot + BottomBar + MainMenu + AboutSheet**

`MainActivity.kt` actualizado:
```kotlin
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val vm: AppViewModel = hiltViewModel()
            EasyPocketTheme(themeMode = vm.themeMode.collectAsStateWithLifecycle().value) {
                AppRoot(vm)
            }
        }
    }
}
```

`ui/navigation/AppRoot.kt`:
```kotlin
@Composable
fun AppRoot(vm: AppViewModel, mainViewModelProvider: @Composable () -> Unit = {}) {
    val navController = rememberNavController()
    val toastState = remember { ToastState() }
    val language by vm.language.collectAsStateWithLifecycle()
    val fatalError by vm.fatalError.collectAsStateWithLifecycle()
    val isReady by vm.isReady.collectAsStateWithLifecycle()

    CompositionLocalProvider(LocalToastState provides toastState, LocalLanguage provides language) {
        when {
            fatalError != null -> ErrorScreen(onRetry = { vm.init() })  // "Unable to initialize the database." + "Retry", colores de la original
            !isReady -> Box(Modifier.fillMaxSize().background(LocalAppColors.current.background))
            else -> AppNavHost(navController, vm)
        }
    }
}
```
`AppNavHost` contiene `NavHost(navController, startDestination = "home")` con la página home = `HomePagerScreen(vm, navController)`; `LocalLanguage` es un `CompositionLocal<Language>` definido en `i18n/` (añadirlo en este task: `val LocalLanguage = staticCompositionLocalOf { Language.ENGLISH }`). `HomePagerScreen` = Scaffold(bottomBar = { BottomBar(...) }) { HorizontalPager(state = rememberPagerState(pageCount = { 3 })...) con 3 placeholders y currentPage en `rememberSaveable` }.

`MainMenu` y `AboutSheet`: port visual del original (`MainMenu.tsx`, `About` en `src/components/ui/about/`). Export/Import en el menú llaman callbacks que en esta tarea solo muestran un toast "not implemented" — se conectan en Task 14. Reset pide confirmación con `ConfirmSheet` (texto del spec: "Todos tus datos locales se eliminarán y se restaurarán los datos semilla por defecto." / "Esta acción no se puede deshacer." — usar claves i18n correspondientes del original).

- [ ] **Step 3: Build + smoke manual**

```bash
./gradlew assembleDebug
```
Expected: BUILD SUCCESSFUL. Instalar en emulador si hay uno corriendo (`adb install -r app/build/outputs/apk/debug/app-debug.apk`) y verificar: seed visible no aplica (pantallas placeholder), menú abre/cierra con swipe, cambio de tema e idioma funciona.

- [ ] **Step 4: Commit**

```bash
git add -A && git commit -m "feat: app shell with pager navigation, bottom bar, main menu and about"
```

---

### Task 9: Pantalla Listas

**Files:**
- Create: `app/src/main/java/com/easypocket/mobile/ui/lists/ListListViewModel.kt`, `app/src/main/java/com/easypocket/mobile/ui/lists/ListsScreen.kt`
- Modify: `ui/navigation/AppRoot.kt` (página 0 del pager = ListsScreen)
- Test: `app/src/test/java/com/easypocket/mobile/ui/lists/ListListViewModelTest.kt`

**Interfaces:**
- Consumes: `ShoppingListRepository`, `ProductRepository`, `ListLogic`, i18n, componentes Task 7.
- Produces: `ListListViewModel` con `val uiState: StateFlow<ListsUiState>`; `data class ListsUiState(val lists: List<ListCardData>, val search: String, val filtered: List<ListCardData>, val isLoading: Boolean)`; `data class ListCardData(val list: ShoppingList, val itemCount: Int, val doneCount: Int, val total: Double)`; funciones `refresh()`, `setSearch(String)`, `deleteList(id)`, `createList(title): String?` (devuelve id de la nueva lista).

- [ ] **Step 1: ViewModel test (Robolectric, repos reales con DB in-memory)**

`ListListViewModelTest.kt`:
```kotlin
package com.easypocket.mobile.ui.lists

// setup idéntico a RepositoriesTest (db in-memory, repos reales)
class ListListViewModelTest {
    @Test
    fun `search filters by normalized title`() = runTest {
        val vm = createVm() // helper: ListListViewModel(listsRepo, productsRepo, MainDispatcherRule)
        vm.refresh()
        vm.setSearch("lacteos") // existe "Lácteos y Desayuno" en seed
        assertEquals(1, vm.uiState.value.filtered.size)
        vm.setSearch("")
        assertEquals(3, vm.uiState.value.filtered.size)
    }

    @Test
    fun `card data computes counts and total`() = runTest {
        val vm = createVm()
        vm.refresh()
        val first = vm.uiState.value.lists.first { it.list.title == "Lista Semanal - Verduras" }
        // assert itemCount/doneCount/total según los datos exactos de seed.ts (leer seed.ts para los valores esperados)
        assertTrue(first.itemCount > 0)
    }

    @Test
    fun `createList returns new id and refreshes`() = runTest {
        val vm = createVm()
        vm.refresh()
        val id = vm.createList("Nueva")
        assertNotNull(id)
        assertTrue(vm.uiState.value.lists.any { it.list.id == id })
    }

    @Test
    fun `deleteList removes it`() = runTest { /* create + delete + assert not in lists */ }
}
```
(Completar helpers `createVm()` con los repos in-memory; añadir un `MainDispatcherRule` con `Dispatchers.setMain(testDispatcher)` en un archivo compartido `app/src/test/java/com/easypocket/mobile/util/MainDispatcherRule.kt`.)

- [ ] **Step 2: Run — verify fail** → `./gradlew test --tests "com.easypocket.mobile.ui.lists.ListListViewModelTest"`

- [ ] **Step 3: ViewModel + Screen**

`ListListViewModel.kt`: search con debounce 300ms (`snapshotFlow`/`delay` en viewModelScope), normalización con `ListLogic.normalize` sobre título; refresh() carga lists + products y computa `ListCardData` (total con `ListLogic.totalAmount(list, productsById, null)`).

`ListsScreen.kt` — port de `/home/rodney/Documents/Dev/easybuy-mobile/src/app/(tabs)/inicio.tsx`:
- `ScreenHeader("Listas", onMenuClick)` con paddingTop 60dp.
- Barra: `SearchInput` + `IconButtonCircle(Icons.Filled.Add)` primary.
- LazyColumn de `PressableCard`: título, fila de Tags (`t("lists.itemCount", params{done,total})`, `t("lists.total", params{amount})`), botón X (abre `ConfirmSheet`, luego `vm.deleteList` + toast).
- Texto contador "Mostrando N listas".
- FAB/flujo crear: `AppBottomSheet(0.75f)` con `ListTitleForm` (TextField + AppButton "Crear"); al crear → navegar a `"listDetail/{id}"` (navController).
- Tap tarjeta → navega a `"listDetail/{id}"`.
- Refresh con `LaunchedEffect` al entrar en composition (la pantalla vive en el pager; recargar también al volver del detalle usando `navController.currentBackStackEntryAsState` y comparando ruta visible).

- [ ] **Step 4: Run tests + build** → `./gradlew test assembleDebug` — Expected PASS

- [ ] **Step 5: Commit** → `git add -A && git commit -m "feat: shopping lists screen"`

---

### Task 10: Detalle de lista

**Files:**
- Create: `app/src/main/java/com/easypocket/mobile/ui/lists/ListDetailViewModel.kt`, `app/src/main/java/com/easypocket/mobile/ui/lists/ListDetailScreen.kt`, `app/src/main/java/com/easypocket/mobile/ui/lists/StoreFilterBar.kt`
- Modify: `ui/navigation/AppRoot.kt` (ruta `listDetail/{listId}`)
- Test: `app/src/test/java/com/easypocket/mobile/ui/lists/ListDetailViewModelTest.kt`

**Interfaces:**
- Consumes: `ShoppingListRepository`, `StoreRepository`, `ProductRepository`, `ListLogic`, clipboard manager, i18n.
- Produces: `ListDetailViewModel` con:
  - `val uiState: StateFlow<ListDetailUiState>`; `data class ListDetailUiState(val list: ShoppingList?, val stores: List<Store>, val productsById: Map<String, Product>, val storeFilter: String?, val selection: Set<Long>, val isLoading: Boolean)`
  - `fun load(listId: String)`, `setStoreFilter(String?)`, `toggleDone(item: ShoppingListItem)` (optimista: actualiza estado local y persiste), `renameList(String)`, `uncheckAll()`, `removeCompleted()`, `copyToClipboard(context, language)`, `setSelection`, `clearSelection`, `deleteSelected()`, `moveSelected(toListId: String)`, `pinSelected(pinned: Boolean)`, `deleteList()`.

- [ ] **Step 1: ViewModel tests**

Tests (Robolectric + DB in-memory, seed previa):
- `toggleDone updates optimistically and persists`
- `setStoreFilter changes totals` — usar `ListDetailUiState` expuesto o helper de UI state con totals (`val visibleTotal: Double`, `val cartTotal: Double` computados en el UiState con ListLogic).
- `uncheckAll leaves pending items and clears done flags`
- `removeCompleted deletes only done`
- `pinSelected reorders pinned first` (assert orden via `uiState.orderedItems`)
- `moveSelected relocates items to target list`
- `copyToClipboard produces expected text` — usar `ShadowClipboard` de Robolectric (`Shadows.shadowOf(context.getSystemService(...))`) y comparar con el formato exacto del original.
- `deleteSelected removes exactly selected`

- [ ] **Step 2: Run — verify fail**

- [ ] **Step 3: Implementar ViewModel + StoreFilterBar + Screen**

`StoreFilterBar.kt`: chips Toggle "Todas" + una chip por tienda presente en la lista (label = descripción, color de fondo `StoreColors.backgroundAlpha`, texto `StoreColors.textColor`); si no caben (medición con `onSizeChanged`/`BoxWithConstraints`, heurística de ~8.5dp por carácter como la original), las que sobran van a un `DropdownMenu` "⋮+N". Port de `src/features/shopping-lists/components/StoreFilterBar.tsx`.

`ListDetailScreen.kt` — port de `src/features/shopping-lists/ShoppingListDetailScreen.tsx`:
- Header: back circular, título centrado (tappable → sheet renombrar), sin hamburguesa.
- `StoreFilterBar`.
- Bloque totales: "Total: $X" / "Carrito: $Y" (2 decimales, símbolo $) + `IconButtonCircle(Add)` primary a la derecha → navega `"itemForm/{listId}/-1"`.
- Menú `⋮` (IconButtonCircle con `Icons.Filled.MoreVert`): Copiar al portapapeles (+toast), Desmarcar todos, Eliminar completados (ConfirmSheet).
- LazyColumn items: `PressableCard` con `combinedClickable(onClick = si !selectionMode → editar item`, `onLongClick = entrar selección)`; fila: círculo check (border 2dp primary cuando !done, fondo primary + check blanco cuando done), nombre (lineThrough si done), tag tienda o "Sin tienda" itálica textSecondary, tag precio `$X.XX` (o sin tag si total 0 — replicar comportamiento original), tag cantidad `trimQuantity(q) unidad`, icono pin `Icons.Filled.PushPin` si pinned. Fondo de la card destacado cuando seleccionada (surface).
- Sección "Completado": si hay items done, header con label + mismos items done (alfabético).
- Modo selección: barra inferior `SelectionActions` con "N seleccionados", botones Mover (abre sheet con lista de otras listas), Fijar/Desfijar, Eliminar (ConfirmSheet), X para salir. Al eliminar la tienda del filtro activo en selección → resetea filtro a "Todas" (comportamiento original).
- Sheets: renombrar (`ListTitleForm` reutilizada), `MoveItemsSheet`, `ConfirmSheet` para eliminar completados/selección.

- [ ] **Step 4: Run tests + build** → `./gradlew test assembleDebug`

- [ ] **Step 5: Commit** → `git add -A && git commit -m "feat: shopping list detail with filters, selection mode and totals"`

---

### Task 11: Form de item de lista

**Files:**
- Create: `app/src/main/java/com/easypocket/mobile/ui/lists/ItemFormViewModel.kt`, `app/src/main/java/com/easypocket/mobile/ui/lists/ItemFormScreen.kt`, `app/src/main/java/com/easypocket/mobile/ui/products/ProductPickerSheet.kt`, `app/src/main/java/com/easypocket/mobile/ui/products/ProductFormContent.kt` (composable reutilizable de form de producto para los sheets embebidos), `app/src/main/java/com/easypocket/mobile/ui/products/ProductFormViewModel.kt`
- Modify: `ui/navigation/AppRoot.kt` (ruta `itemForm/{listId}/{itemId}`)
- Test: `app/src/test/java/com/easypocket/mobile/ui/lists/ItemFormViewModelTest.kt`

**Interfaces:**
- Consumes: repos, `ProductRepository.findDuplicate`, `ListLogic.unitLabelKey`, i18n.
- Produces:
  - `ItemFormViewModel(savedStateHandle, productsRepo, storesRepo, listsRepo)`: `val uiState: StateFlow<ItemFormUiState>`; `data class ItemFormUiState(val isEdit: Boolean, val productId: String?, val storeId: String?, val quantityText: String, val products: List<Product>, val stores: List<Store>, val unitLabelKey: String?, val unitPrice: Double?, val totalPrice: Double?, val isLoading: Boolean)`; funciones `load(listId, itemId)`, `selectProduct(String?)`, `setStore(String?)`, `setQuantity(String)`, `save(onSaved: () -> Unit)`, `delete(onDeleted: () -> Unit)`, `createProduct(name, unit, prices): String` (crea y selecciona), `updateProduct(Product)`.
  - `ProductFormContent(vm: ProductFormViewModel, isSheet: Boolean, onSaved, onDeleted)` — composable reutilizado por la pantalla completa (Task 12) y por los sheets embebidos de crear/editar producto.
  - `ProductFormViewModel` (completo, se crea aquí): `uiState: ProductFormUiState(productId?, name, unit, prices: List<ProductPriceRow>, availableStores: List<Store>, nameError: Boolean, isEdit)`, `data class ProductPriceRow(storeId, storeName, value: String)`, funciones `load(productId?)`, `setName`, `setUnit`, `addPrice(storeId, value)`, `updatePrice`, `removePrice(storeId)`, `save(onSaved)`, `delete(onDeleted)`.

- [ ] **Step 1: ItemFormViewModel tests**

- `quantity validation accepts decimal and rejects garbage` — exponer `val isQuantityValid: Boolean` en UiState (regex `^\d*\.?\d*$` y > 0 al guardar).
- `selecting product and store computes unit price and total` — producto con precio 10 en s1: unitPrice 10.0, con quantity "2" → total 20.0; sin tienda → null/null.
- `unitLabelKey switches singular-plural` — quantity "1" vs "2".
- `save inserts new item` / `save updates existing item` / `delete removes item`.
- `createProduct creates selects and dedupes` — crear producto desde el form, queda seleccionado; crear duplicado falla (nameError o Result).

- [ ] **Step 2: Run — verify fail**

- [ ] **Step 3: Implementar ViewModels y pantallas**

`ItemFormScreen.kt` — port de `src/features/shopping-lists/components/ShoppingListItemFormScreen.tsx`:
- Header: back, título "Agregar item"/"Editar item".
- ProductPicker: `SelectField`-like que abre `ProductPickerSheet` (bottom sheet ~350dp con `SearchInput` autofocus + lista de productos, debounce y búsqueda normalizada). Botones laterales: lápiz (editar producto seleccionado; disabled sin selección) y `+` (crear producto nuevo) — ambos abren `AppBottomSheet` con `ProductFormContent` en modo sheet (0.8f).
- Select de tienda: opciones "-- Ninguna --" + tiendas; opción con precio lleva indicador (texto del precio como `trailing` del `SelectOption`).
- Input cantidad: `KeyboardType.Decimal`, validación en vivo, default "1".
- Label de unidad dinámica singular/plural.
- Resumen "Precio unitario: $X.XX" / "Total: $Y.YY" (o "—" si no aplica).
- Footer fijo: fila [Eliminar (solo edición, destructive)] [Cancelar (secondary)] + AppButton "Guardar" ancho completo; `Modifier.imePadding()` para que suba con el teclado.

- [ ] **Step 4: Run tests + build** → `./gradlew test assembleDebug`

- [ ] **Step 5: Commit** → `git add -A && git commit -m "feat: list item form with product picker and embedded product forms"`

---

### Task 12: Pantalla Productos + Form de producto

**Files:**
- Create: `app/src/main/java/com/easypocket/mobile/ui/products/ProductListViewModel.kt`, `app/src/main/java/com/easypocket/mobile/ui/products/ProductsScreen.kt`, `app/src/main/java/com/easypocket/mobile/ui/products/ProductFormScreen.kt`
- Modify: `ui/navigation/AppRoot.kt` (página 1 del pager + ruta `productForm/{productId}`)
- Test: `app/src/test/java/com/easypocket/mobile/ui/products/ProductListViewModelTest.kt`, `app/src/test/java/com/easypocket/mobile/ui/products/ProductFormViewModelTest.kt`

**Interfaces:**
- Consumes: `ProductRepository`, `ProductFormViewModel` (Task 11), i18n, componentes.
- Produces: `ProductListViewModel`: `uiState: ProductListUiState(products, search, filtered, selection: Set<String>, isLoading)`, `refresh()`, `setSearch(String)`, `setSelection`, `clearSelection()`, `deleteSelected()`. `ProductFormScreen(vm: ProductFormViewModel, onBack)` usa `ProductFormContent` en modo pantalla completa.

- [ ] **Step 1: Tests**

`ProductListViewModelTest`:
- search normalizada filtra por nombre ("leche" encuentra "Leche líquida"; "papa" también).
- deleteSelected elimina productos + sus precios + items de listas que los referencian (sembrar un item y verificar que desaparece).

`ProductFormViewModelTest`:
- `save validates empty name and missing unit` (nameError/deshabilita guardar).
- `save rejects duplicate name case-insensitive, allows same product when editing`.
- `addPrice then save persists prices` / `removePrice excludes store` / `updatePrice replaces value`.
- `availableStores excludes stores that already have a price`.

- [ ] **Step 2: Run — verify fail**

- [ ] **Step 3: Implementar pantallas**

`ProductsScreen.kt` — port de `src/app/(tabs)/productos.tsx`:
- Header "Productos" + hamburguesa, SearchInput + `+` → `"productForm/new"`.
- Tarjetas: nombre + Tag unidad (clave `unitLabelKey`).
- Modo selección long-press: header cambia a X (salir) + "N seleccionados" + AppButton destructive "Eliminar (N)" → ConfirmSheet con warning de cascada (claves i18n de la original).

`ProductFormScreen.kt` — port de `src/features/products/ProductFormScreen.tsx`:
- Header "Nuevo producto"/"Editar producto" + back.
- TextField nombre, SelectField unidad (9 unidades, labels i18n).
- Sección "Precios por tienda" (label uppercase 13sp letterSpacing 1): filas tienda-precio (tap → modo edición inline con input decimal + ✓/✗, X para quitar), formulario de alta (select tienda disponible + input precio + ✓/✗), AppButton secondary "Agregar precio" (muestra solo si hay tiendas disponibles), textos vacío: "Todas las tiendas tienen precio" / "Sin tiendas disponibles".
- Footer: [Eliminar (solo edición)] [Cancelar] + "Guardar".

- [ ] **Step 4: Run tests + build** → `./gradlew test assembleDebug`

- [ ] **Step 5: Commit** → `git add -A && git commit -m "feat: products catalog and product form with per-store prices"`

---

### Task 13: Pantalla Tiendas + Form de tienda

**Files:**
- Create: `app/src/main/java/com/easypocket/mobile/ui/stores/StoreListViewModel.kt`, `app/src/main/java/com/easypocket/mobile/ui/stores/StoresScreen.kt`, `app/src/main/java/com/easypocket/mobile/ui/stores/StoreFormViewModel.kt`, `app/src/main/java/com/easypocket/mobile/ui/stores/StoreFormScreen.kt`
- Modify: `ui/navigation/AppRoot.kt` (página 2 del pager + ruta `storeForm/{storeId}`)
- Test: `app/src/test/java/com/easypocket/mobile/ui/stores/StoresTest.kt`

**Interfaces:**
- Consumes: `StoreRepository`, `ProductRepository.deleteAll` para cascada, `StoreColors`.
- Produces: `StoreListViewModel` (`uiState: StoreListUiState(stores, search, filtered, selection, isLoading)`, `refresh()`, `setSearch`, `setSelection`, `clearSelection`, `deleteSelected()`); `StoreFormViewModel` (`uiState: StoreFormUiState(store: Store?, name: String, color: Int, isEdit)`, `load(storeId?)`, `setName`, `setColor(Int)`, `save(onSaved)`, `delete(onDeleted)`).

- [ ] **Step 1: Tests**

- search filtra tiendas por descripción normalizada.
- `save validates non-empty name`.
- `deleteSelected removes stores and their prices` (items sobreviven con storeId null — verificar comportamiento FK SET NULL).
- `save creates with selected color` / `update keeps id`.

- [ ] **Step 2: Run — verify fail**

- [ ] **Step 3: Implementar pantallas**

`StoresScreen.kt` — port de `src/app/(tabs)/tiendas.tsx`: tarjetas con círculo de color 12dp (`StoreColors.get(color, isDark)`) + descripción; selección múltiple con ConfirmSheet de cascada de precios; `+` → `"storeForm/new"`.

`StoreFormScreen.kt` — port de `src/features/stores/StoreFormScreen.tsx`: TextField nombre, grid de 9 swatches circulares 32dp (seleccionado con borde 3dp primary), footer [Eliminar] [Cancelar] + Guardar.

- [ ] **Step 4: Run tests + build** → `./gradlew test assembleDebug`

- [ ] **Step 5: Commit** → `git add -A && git commit -m "feat: stores screen and store form with color picker"`

---

### Task 14: Export / Import Data (SAF + JSON)

**Files:**
- Create: `app/src/main/java/com/easypocket/mobile/data/backup/BackupData.kt`, `app/src/main/java/com/easypocket/mobile/data/backup/BackupManager.kt`, `app/src/main/java/com/easypocket/mobile/ui/mainmenu/BackupViewModel.kt`
- Modify: `ui/navigation/AppRoot.kt` / `MainActivity.kt` (lanzar SAF intents con `rememberLauncherForActivityResult(CreateDocument("application/json"))` y `OpenDocument`; conectar callbacks del MainMenu; ConfirmSheet previo al import)
- Test: `app/src/test/java/com/easypocket/mobile/data/backup/BackupManagerTest.kt`

**Interfaces:**
- Consumes: `EasyPocketDatabase` (DAOs), `kotlinx.serialization`, Context contentResolver.
- Produces:
  - `@Serializable data class BackupData(version: Int, exportedAt: String, stores: List<BackupStore>, products: List<BackupProduct>, prices: List<BackupPrice>, shoppingLists: List<BackupList>, listItems: List<BackupListItem>)` con sub-DTOs (todos campos String/Double/Boolean/Long como el spec).
  - `class BackupManager @Inject constructor(private val db: EasyPocketDatabase)`:
    - `suspend fun exportTo(uri: Uri, contentResolver: ContentResolver): Result<Unit>`
    - `suspend fun parse(uri: Uri, contentResolver: ContentResolver): Result<BackupData>` (valida estructura + referencias)
    - `suspend fun importData(backup: BackupData)` (transacción: clearAllTables + insertar todo, conservando `id` de items)
  - `BackupViewModel`: `fun export(uri, contentResolver, onDone: (Result<Unit>) -> Unit)`, `fun import(uri, contentResolver, onDone: (Result<Unit>) -> Unit)`.

- [ ] **Step 1: Tests failing de BackupManager**

`BackupManagerTest.kt` (Robolectric, DB in-memory + seed; construir JSON strings literales para parse):
```kotlin
@Test
fun `parse accepts valid backup`() = runTest {
    val json = """
        {"version":1,"exportedAt":"2026-08-27T00:00:00Z",
         "stores":[{"id":"s1","description":"Store 1","color":0}],
         "products":[{"id":"p1","productName":"Milk","unitOfMeasurement":"lt"}],
         "prices":[{"productId":"p1","storeId":"s1","value":10.5}],
         "shoppingLists":[{"id":"l1","title":"Lista"}],
         "listItems":[{"id":1,"shoppingListId":"l1","productId":"p1","storeId":"s1","quantity":2.0,"done":false,"pinned":false}]}
    """.trimIndent()
    // escribir a archivo temporal y resolver con Uri.fromFile, o refactorear parse(String) + parse(Uri)
    val result = manager.parse(json) // variante parse(json: String): Result<BackupData>
    assertTrue(result.isSuccess)
}

@Test
fun `parse rejects malformed json`() = runTest {
    assertTrue(manager.parse("{not json").isFailure)
}

@Test
fun `parse rejects broken references`() = runTest {
    // item cuyo productId no existe en products del mismo backup → failure
    // price cuyo storeId no existe → failure
}

@Test
fun `parse rejects unknown unit of measurement`() = runTest { /* unitOfMeasurement "gallon" → failure */ }

@Test
fun `import replaces all data`() = runTest {
    seeder.seedIfEmpty()
    val backup = validBackupWith(storeCount = 1, productCount = 1, listCount = 1)
    manager.importData(backup)
    assertEquals(1, db.storeDao().getAll().first().size)
    assertEquals(1, db.productDao().getAll().first().size)
    // item conserva su id
    assertEquals(1L, db.listDao().getById("l1")!!.items.first().id)
}

@Test
fun `export produces json containing seeded data`() = runTest {
    seeder.seedIfEmpty()
    val json = manager.exportToString()
    val parsed = manager.parse(json).getOrThrow()
    assertEquals(2, parsed.stores.size)
    assertEquals(10, parsed.products.size)
    assertEquals(3, parsed.shoppingLists.size)
}
```
(Estructurar BackupManager con `exportToString(): String` y `parse(json: String)` para testabilidad pura; los métodos con Uri solo envuelven IO.)

- [ ] **Step 2: Run — verify fail**

- [ ] **Step 3: Implementar**

`BackupData.kt` — DTOs `@Serializable` EXACTOS al formato del spec:
```kotlin
@Serializable
data class BackupData(
    val version: Int,
    val exportedAt: String,
    val stores: List<BackupStore>,
    val products: List<BackupProduct>,
    val prices: List<BackupPrice>,
    val shoppingLists: List<BackupList>,
    val listItems: List<BackupListItem>,
)

@Serializable data class BackupStore(val id: String, val description: String, val color: Int)
@Serializable data class BackupProduct(val id: String, val productName: String, val unitOfMeasurement: String)
@Serializable data class BackupPrice(val productId: String, val storeId: String, val value: Double)
@Serializable data class BackupList(val id: String, val title: String)
@Serializable data class BackupListItem(
    val id: Long, val shoppingListId: String, val productId: String,
    val storeId: String? = null, val quantity: Double,
    val done: Boolean = false, val pinned: Boolean = false,
)
```

`BackupManager.kt`:
- `exportToString()`: lee todas las tablas (`first()` de cada DAO), mapea a DTOs, `exportedAt = Instant.now().toString()`, `version = 1`, serializa con `Json { prettyPrint = true; ignoreUnknownKeys = true }`.
- `parse(json)`: `runCatching` + validaciones: versión soportada (== 1), units válidas (`UnitOfMeasurement.fromRaw != null`), referencias de prices → products/stores existentes, listItems → lists/products (+ storeId si no-null → stores), IDs de items únicos. Cualquier violación → `Result.failure(IllegalArgumentException(...))`.
- `importData(backup)`: `db.withTransaction { db.clearAllTables(); insertar stores, products, prices, lists, items (conservando ids) }`.
- `exportTo(uri, contentResolver)`: `contentResolver.openOutputStream(uri)?.use { it.write(exportToString().toByteArray()) } ?: error(...)`; `importFrom(uri, contentResolver)`: lee + parse + importData.

Integración en UI (`AppRoot`/MainMenu):
- `rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri -> uri?.let { vm.export(it) { result -> toast } } }` lanzado con nombre sugerido `"easypocket-backup-" + SimpleDateFormat("yyyyMMdd-HHmm").format(Date()) + ".json"`.
- `rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let { vm.parseConfirm(it) } }` lanzado con `arrayOf("application/json", "application/octet-stream", "text/plain")`.
- Import: primero parse; si OK → `ConfirmSheet` con texto destructivo ("Esto reemplazará todos tus datos actuales" — clave i18n nueva si no existe en el original: añadir `backup.importWarning.title`/`backup.importWarning.message` a ambos mapas); confirmar → import + toast success + refresh global (los ViewModels recargan al ganar foco; para forzar recarga inmediata, exponer `refreshTick` en AppViewModel y que las pantallas lo observen, o simplemente volver a cargar con `navController` back-stack change — implementar `refreshTick: StateFlow<Int>` en AppViewModel, incrementado tras import/reset, observado por los refresh de las 3 pantallas).
- Eliminar los toasts "not implemented" del Task 8.

- [ ] **Step 4: Run tests + build** → `./gradlew test assembleDebug`

- [ ] **Step 5: Commit** → `git add -A && git commit -m "feat: export and import data via SAF JSON backup"`

---

### Task 15: Pulido final, verificación completa y smoke test

**Files:**
- Modify: según se encuentre (ajustes de UI menores).
- Create: `docs/superpowers/plans/` no — sin docs nuevas.

**Interfaces:**
- Consumes: todo lo anterior.

- [ ] **Step 1: Verificación de fidelidad UI (checklist)**

Comparar lado a lado con la original (ejecutarla si es posible desde `/home/rodney/Documents/Dev/easybuy-mobile/` o guiarse por el código):
- [ ] Padding 60dp superior, título centrado, hamburguesa derecha en las 3 pantallas de tabs.
- [ ] Bottom bar: iconos outline/filled, primary activo, borde superior, safe-area padding inferior.
- [ ] Bottom sheets: esquinas 20dp, botón X, drag-to-dismiss.
- [ ] Tags pills radius 999 con colores de tienda (alpha background + texto shaded).
- [ ] Confirm dialogs destructivos con warning.
- [ ] Dark mode: todos los fondos/bordes de la paleta (sin blancos hardcodeados).
- [ ] Toasts con colores por tipo.
- [ ] Totales con formato `$X.XX` (2 decimales).
- [ ] Teclado decimal en cantidad; footer sube con teclado.
- [ ] Long-press selección en las 3 listas; salir con X.
- [ ] i18n completo (cambiar idioma en menú y revisar TODAS las pantallas).

- [ ] **Step 2: Suite completa de tests + build release**

```bash
./gradlew test assembleDebug assembleRelease
```
Expected: todos los tests PASS, ambos builds SUCCESSFUL.

- [ ] **Step 3: Smoke test en emulador (si hay AVD disponible)**

```bash
adb devices
# si hay emulador corriendo:
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.easypocket.mobile/.MainActivity
```
Verificar manualmente: seed cargado, CRUD de listas/productos/tiendas, crear item, toggle done, totales, export (ver archivo en Downloads), import (restaurar), reset, cambio de tema/idioma.

- [ ] **Step 4: Commit final**

```bash
git add -A && git commit -m "chore: final polish and verification"
```

---

## Self-Review (ejecutado al escribir el plan)

- **Spec coverage**: esquema DB → Task 4; i18n/settings → Task 3; tema/colores → Task 7; shell + menú + About + reset + error screen → Task 8; Listas → Task 9; Detalle (filtros, selección, clipboard, totales) → Task 10; Form item → Task 11; Productos → Task 12; Tiendas → Task 13; Export/Import → Task 14; seed → Task 6; icono/versioning → Task 1. Historial/notificaciones/barcode: fuera de alcance por spec. ✓
- **Placeholders**: los snippets con TODO/notas son instrucciones de ajuste explícitas con fuente señalada, no trabajo sin definir. ✓
- **Type consistency**: `ListLogic.orderedItems(items, productsById)`, `clipboardText(list, productsById[, language])`, `findDuplicate(name, excludeId)`, `SelectOption(id, label, trailing)`, `ToastState.show(message, type)` usados de forma consistente entre tareas. ✓
