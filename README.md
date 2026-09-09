# EasyPocket

A fully offline-first Android app for managing shopping lists and tracking personal spending — built with Kotlin, Jetpack Compose, Room and Hilt.

**Download the signed APK:** [EasyPocket.apk](https://tyrpayj0fal1ajop.public.blob.vercel-storage.com/EasyPocket.apk)

---

## About this project

EasyPocket is more than a shopping list. Each list is a self-contained expense record: create one for the supermarket, one for a trip, one for the month — each with its own icon, color and running total. Products can have different prices per store, and picking a store in a list automatically applies the matching price. Completed purchases are archived into a spending history with per-day and per-category charts.

Everything runs **100% offline**: no accounts, no analytics, no network calls. Data lives locally in Room and can be exported to and restored from an open, human-readable JSON backup file.

This repository is part of my portfolio, so it is written to be reviewed: a clean layered architecture, hand-written Room migrations with matching tests, and a fast unit-test suite that runs on every change.

## Key features

- **Per-list expenses** — every list tracks its own spending total in real time.
- **Prices per store** — assign multiple prices to a product; the list uses the one matching the selected store.
- **Real-time cart totals** — check off items and the total updates instantly. Paste a multi-line text and bulk-add products with their last-used store.
- **Remembered categories** — assign a category to a product once; the app auto-selects it on future additions. New categories can be created inline.
- **Spending history** — archive completed purchases and visualize spending per day and per category with charts.
- **Personalization** — colors and emojis for lists, stores and categories.
- **Open backups** — export/import all data as a versioned JSON file. No lock-in.
- **Bilingual UI** — English and Spanish, powered by an in-app translation layer.

## Architecture

The app follows a layered, unidirectional structure:

```
ui/          Jetpack Compose screens + ViewModels (state holders), navigation, theme
domain/      Pure Kotlin logic: list math, units, icons, alphabet indexing
data/
  local/     Room database (entities, DAOs, relations, migrations)
  repository/ Repository layer over DAOs
  backup/    JSON backup/restore engine (kotlinx.serialization)
  seed/      First-run seed data
di/          Hilt modules
i18n/        Translation layer (en/es) exposed via CompositionLocals
```

Highlights:

- **Room schema evolution as a first-class concern.** The database is at version 9 with 8 hand-written SQL migrations, each covered by a dedicated migration test — a deliberate practice to keep schema changes safe for real users with existing data.
- **MVVM with Compose.** ViewModels expose state via `StateFlow`; Compose renders it. Hilt wires repositories, database and ViewModels.
- **Preferences via DataStore**, no `SharedPreferences`.
- **Charts with Vico** for the spending-history screens.

## Tech stack

| Layer | Technology |
|---|---|
| Language | Kotlin (JVM 17) |
| UI | Jetpack Compose (Material 3), Navigation Compose, extended icons |
| DI | Hilt |
| Persistence | Room + DataStore Preferences |
| Serialization | kotlinx.serialization |
| Charts | Vico |
| Testing | JUnit, Robolectric, Coroutines Test, Room Testing |
| Build | Gradle (Kotlin DSL), version catalog, KSP |

## Testing

**203 unit tests** across the repository, database, backup, seed, domain, i18n, theme and ViewModel layers, running on the JVM via Robolectric (no device or emulator required):

```bash
./gradlew :app:testDebugUnitTest
```

Room migrations get explicit regression tests in `DatabaseTest.kt` (fresh install + `version 1 → 9` upgrade paths).

## Building and running

```bash
./gradlew assembleDebug        # debug build
./gradlew assembleRelease      # signed release APK (see note)
```

Signing is loaded from a gitignored `keystore.properties` + local keystore file. If those files are absent, the release build still succeeds but produces an unsigned APK — so the project builds out of the box for anyone cloning it.

- minSdk 26 · targetSdk 35 · compileSdk 37
- Release artifact is renamed to `EasyPocket.apk` at build time.

## Distribution

The signed release APK is published to Vercel Blob with a stable public URL via a shell script (`scripts/upload-apk.sh`), so users always download the latest version from the same link. Credentials live outside the repository.

## Privacy

EasyPocket collects nothing. There are no third-party SDKs, no analytics, no ads and no network permission usage for data collection — the only network access is the user's own optional download of the APK. All data stays on-device, and backups are plain JSON files the user fully controls.

## Project status

- **Version:** 1.0.0 (production, publicly distributed)
- **Roadmap:** move products between lists, add products to lists from the product master, and a quick-list mode that parses plain-text product entries one per line.

## Author

Built by [Rodney Marín](https://github.com/rodneymarin) as a portfolio project. This repository: [rodneymarin/EasyPocket-mobile](https://github.com/rodneymarin/EasyPocket-mobile).
