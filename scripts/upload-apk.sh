#!/usr/bin/env bash
set -euo pipefail

STORE_HOST="https://blob.vercel-storage.com"
PATHNAME="EasyPocket.apk"
TOKEN_FILE="${HOME}/.config/easypocket/vercel-blob.token"
APK="app/build/outputs/apk/release/EasyPocket.apk"

if [[ ! -f "$TOKEN_FILE" ]]; then
  echo "Missing token file: $TOKEN_FILE" >&2
  exit 1
fi

if [[ ! -f "$APK" ]]; then
  echo "APK not found: $APK. Run './gradlew assembleRelease' first." >&2
  exit 1
fi

token=$(<"$TOKEN_FILE")
url="${STORE_HOST}/${PATHNAME}"
size=$(du -h "$APK" | cut -f1)

echo "Uploading ${size} -> ${url}"
response=$(curl -sf -X PUT \
  -H "authorization: Bearer ${token}" \
  -H "x-add-random-suffix: 0" \
  -H "x-allow-overwrite: 1" \
  -H "content-type: application/vnd.android.package-archive" \
  --data-binary @"${APK}" \
  "${url}")

sed -n 's/.*"url":"\([^"]*\)".*/Download URL: \1/p' <<< "$response"
