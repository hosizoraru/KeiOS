#!/usr/bin/env bash
set -euo pipefail

DEVICE=""
PACKAGE_NAME="${PACKAGE_NAME:-os.kei.debug}"
ENABLE_LOCAL_NETWORK_COMPAT=0

while [[ $# -gt 0 ]]; do
  case "$1" in
    -s|--serial)
      DEVICE="$2"
      shift 2
      ;;
    -p|--package)
      PACKAGE_NAME="$2"
      shift 2
      ;;
    --enable-local-network-compat)
      ENABLE_LOCAL_NETWORK_COMPAT=1
      shift
      ;;
    *)
      echo "Unknown argument: $1" >&2
      exit 2
      ;;
  esac
done

ADB=(adb)
if [[ -n "$DEVICE" ]]; then
  ADB+=( -s "$DEVICE" )
fi

echo "== Device =="
"${ADB[@]}" shell getprop ro.build.version.release
"${ADB[@]}" shell getprop ro.build.version.sdk
"${ADB[@]}" shell getprop ro.product.manufacturer
"${ADB[@]}" shell getprop ro.product.model

echo "== Package =="
"${ADB[@]}" shell dumpsys package "$PACKAGE_NAME" | grep -E "versionName|targetSdk|ACCESS_LOCAL_NETWORK|NEARBY_WIFI_DEVICES|POST_PROMOTED|FOREGROUND_SERVICE_SPECIAL_USE" || true

echo "== AppOps =="
"${ADB[@]}" shell cmd appops get "$PACKAGE_NAME" | grep -E "ACCESS_LOCAL_NETWORK|NEARBY_WIFI|POST_NOTIFICATION" || true

if [[ "$ENABLE_LOCAL_NETWORK_COMPAT" -eq 1 ]]; then
  echo "== Enable Android 16 local-network compat flag =="
  "${ADB[@]}" shell am compat enable RESTRICT_LOCAL_NETWORK "$PACKAGE_NAME" || true
  "${ADB[@]}" shell am compat enabled "$PACKAGE_NAME" | grep RESTRICT_LOCAL_NETWORK || true
else
  echo "== Compat flag =="
  echo "Pass --enable-local-network-compat to enable RESTRICT_LOCAL_NETWORK for this package."
fi
