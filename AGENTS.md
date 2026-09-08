# EasyPocket-mobile

Android app (Kotlin + Compose, Room, Hilt).

## Release / publish flow

- Signed release APK: `./gradlew assembleRelease` — signing is wired via
  `keystore.properties` (gitignored, contains keystore credentials) and
  `EasyPocketKeystore.jks` (also gitignored). Without those files the build
  stays unsigned but still succeeds.
- Upload to Vercel Blob (stable public URL): `./scripts/upload-apk.sh`.
  Token lives outside the repo at `~/.config/easypocket/vercel-blob.token`.
- Full flow + gotchas: see the `release-apk` skill in `.opencode/skills/`
  or use the `/release` command.

## Conventions

- Spanish commit messages, conventional-ish prefixes (feat:, fix:, ui:, chore:).
- Unit tests run with `./gradlew :app:testDebugUnitTest`; Robolectric-based.
- Room migrations live in `EasyPocketDatabase.kt` with hand-written SQL and a
  matching test in `DatabaseTest.kt`.
