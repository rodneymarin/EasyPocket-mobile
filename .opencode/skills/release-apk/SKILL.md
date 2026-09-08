---
name: release-apk
description: Build and publish the signed EasyPocket release APK to Vercel Blob. Use when the user asks to "subir el APK", "publicar el APK", "generar y subir el release", "compilar release" or mentions EasyPocket.apk on vercel/blob.
---

# Release APK (EasyPocket)

Publishes the signed release APK to Vercel Blob, overwriting
`EasyPocket.apk` so the public URL stays stable.

## Steps

1. Run the unit tests first and confirm they pass:
   `./gradlew :app:testDebugUnitTest`
2. Build the signed APK (signing is automatic via `keystore.properties`,
   which lives outside git):
   `./gradlew assembleRelease`
   Output: `app/build/outputs/apk/release/EasyPocket.apk`
3. Upload it (token is read from `~/.config/easypocket/vercel-blob.token`;
   do not move, print, or commit the token):
   `./scripts/upload-apk.sh`
   This keeps the URL unchanged:
   `https://tyrpayj0fal1ajop.public.blob.vercel-storage.com/EasyPocket.apk`
4. Verify with `curl -sI` on the URL that content-length/etag match the new
   build, then report the download URL to the user.

## Notes

- Never commit `keystore.properties`, `EasyPocketKeystore.jks`, or any token.
- If `./scripts/upload-apk.sh` fails with a duplicate-suffixed blob
  (`EasyPocket-XXXX.apk`), delete it via the Vercel Blob API (`del` with the
  blob URL) and retry — the script must produce exactly one `EasyPocket.apk`.
- Bumping the version: edit `versionCode`/`versionName` in
  `app/build.gradle.kts` only when the user explicitly asks for a version
  bump.
