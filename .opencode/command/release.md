---
description: Build the signed release APK, run tests, and upload it to Vercel Blob keeping the same URL.
agent: build
---

Build and publish the release APK:

1. Run the unit tests (`./gradlew :app:testDebugUnitTest`) and stop on failure.
2. Build the signed APK (`./gradlew assembleRelease`).
3. Upload with `./scripts/upload-apk.sh` (keeps the public URL stable).
4. Verify the URL serves the new build and report the download link.

$ARGUMENTS
