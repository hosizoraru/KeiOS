#!/usr/bin/env bash
set -euo pipefail

DEVICE=""
PACKAGE_NAME="${PACKAGE_NAME:-os.kei.debug}"
APK_PATH="${APK_PATH:-app/build/outputs/apk/debug/app-debug.apk}"
OUT_DIR="${OUT_DIR:-artifacts/api36-p0/p0b-runtime-native}"
BUILD_APK=1
DEVICE_WORK_DIR=""
MEDIA_URL="${MEDIA_URL:-https://storage.googleapis.com/exoplayer-test-media-0/BigBuckBunny_320x180.mp4}"

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
    --apk)
      APK_PATH="$2"
      BUILD_APK=0
      shift 2
      ;;
    -o|--out)
      OUT_DIR="$2"
      shift 2
      ;;
    --device-work-dir)
      DEVICE_WORK_DIR="$2"
      shift 2
      ;;
    --media-url)
      MEDIA_URL="$2"
      shift 2
      ;;
    --skip-build)
      BUILD_APK=0
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

if [[ "$BUILD_APK" -eq 1 ]]; then
  ./gradlew :app:assembleDebug
fi

if [[ ! -f "$APK_PATH" ]]; then
  echo "APK not found: $APK_PATH" >&2
  exit 1
fi

SDK_DIR="$(sed -n 's/^sdk.dir=//p' local.properties 2>/dev/null | tail -n 1)"
if [[ -z "$SDK_DIR" ]]; then
  SDK_DIR="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
fi
BUILD_TOOLS_DIR="$(find "$SDK_DIR/build-tools" -mindepth 1 -maxdepth 1 -type d 2>/dev/null | sort | tail -n 1)"
ZIPALIGN="${BUILD_TOOLS_DIR:-}/zipalign"
AAPT2="${BUILD_TOOLS_DIR:-}/aapt2"
LLVM_OBJDUMP="$(find "$SDK_DIR/ndk" -path '*/toolchains/llvm/prebuilt/*/bin/llvm-objdump' -type f 2>/dev/null | sort | tail -n 1)"

mkdir -p "$OUT_DIR/libs"
REPORT="$OUT_DIR/report.txt"
ALERTS_FRESH="$OUT_DIR/logcat_fresh_alerts.txt"
ALERTS_UPGRADE="$OUT_DIR/logcat_upgrade_alerts.txt"
PACKAGE_DIR_SAFE="${PACKAGE_NAME//[^A-Za-z0-9._-]/_}"
DEVICE_WORK_DIR="${DEVICE_WORK_DIR:-/data/local/tmp/keios-api36-p0b/$PACKAGE_DIR_SAFE}"

cleanup() {
  "${ADB[@]}" shell rm -rf "$DEVICE_WORK_DIR" >/dev/null 2>&1 || true
}
trap cleanup EXIT

"${ADB[@]}" shell rm -rf "$DEVICE_WORK_DIR" >/dev/null 2>&1 || true
"${ADB[@]}" shell mkdir -p "$DEVICE_WORK_DIR" >/dev/null

screen_size="$("${ADB[@]}" shell wm size | sed -n 's/.*: //p' | tr -d '\r' | tail -n 1)"
width="${screen_size%x*}"
height="${screen_size#*x}"
if [[ -z "$width" || -z "$height" || "$width" == "$height" ]]; then
  width=1220
  height=2656
fi

capture() {
  local name="$1"
  local device_png="$DEVICE_WORK_DIR/${name}.png"
  local device_xml="$DEVICE_WORK_DIR/${name}.xml"
  "${ADB[@]}" shell screencap -p "$device_png" >/dev/null
  "${ADB[@]}" pull "$device_png" "$OUT_DIR/${name}.png" >/dev/null
  "${ADB[@]}" shell uiautomator dump "$device_xml" >/dev/null
  "${ADB[@]}" pull "$device_xml" "$OUT_DIR/${name}.xml" >/dev/null
}

grant_runtime_permissions() {
  "${ADB[@]}" shell pm grant "$PACKAGE_NAME" android.permission.POST_NOTIFICATIONS >/dev/null 2>&1 || true
  "${ADB[@]}" shell pm grant "$PACKAGE_NAME" android.permission.NEARBY_WIFI_DEVICES >/dev/null 2>&1 || true
}

install_apk_with_retry() {
  local target_log="$1"
  local attempts=0
  : > "$target_log"
  while (( attempts < 3 )); do
    attempts=$((attempts + 1))
    {
      echo "attempt=$attempts"
      "${ADB[@]}" install -r "$APK_PATH"
    } >> "$target_log" 2>&1 && return 0
    sleep 4
  done
  return 1
}

start_main() {
  "${ADB[@]}" shell am start -n "$PACKAGE_NAME/os.kei.MainActivity" >/dev/null
  sleep 3
}

start_video_fullscreen() {
  "${ADB[@]}" shell am start -n "$PACKAGE_NAME/os.kei.debug.ApiCompatQaActivity" \
    -a os.kei.debug.qa.VIDEO_FULLSCREEN \
    --es media_url "$MEDIA_URL" >/dev/null
  sleep 5
}

pull_runtime_report() {
  local target="$1"
  "${ADB[@]}" exec-out run-as "$PACKAGE_NAME" cat files/api36_p0_runtime_report.txt > "$target" 2>/dev/null || {
    echo "runtime report unavailable" > "$target"
  }
}

filter_runtime_alerts() {
  local source="$1"
  local target="$2"
  grep -iE "os\\.kei|UnsatisfiedLinkError|dlopen failed|hiddenapi|Accessing hidden|internal structure|fatal exception|AndroidRuntime|ART" "$source" > "$target" || true
}

inspect_native_libs() {
  local bad_libs=0
  local libs_file="$OUT_DIR/native_libs.txt"
  unzip -Z1 "$APK_PATH" | grep -E '^lib/.+\.so$' > "$libs_file" || true
  if [[ ! -s "$libs_file" ]]; then
    echo "No native libraries found in $APK_PATH" >> "$REPORT"
    return 0
  fi

  while IFS= read -r lib; do
    [[ -z "$lib" ]] && continue
    local safe="${lib//\//_}"
    local so_path="$OUT_DIR/libs/$safe"
    local objdump_path="$OUT_DIR/libs/$safe.objdump.txt"
    unzip -p "$APK_PATH" "$lib" > "$so_path"
    echo "" >> "$REPORT"
    echo "== $lib ==" >> "$REPORT"
    ls -l "$so_path" >> "$REPORT"
    if [[ -x "$LLVM_OBJDUMP" ]]; then
      "$LLVM_OBJDUMP" -p "$so_path" > "$objdump_path"
      local alignments
      alignments="$(perl -ne 'if (/LOAD off.*align 2\*\*(\d+)/) { printf "%d\n", 2 ** $1; }' "$objdump_path")"
      if [[ -z "$alignments" ]]; then
        echo "WARN no LOAD alignment found" >> "$REPORT"
        bad_libs=1
      else
        local min_align=999999999
        while IFS= read -r align; do
          [[ -z "$align" ]] && continue
          if (( align < min_align )); then
            min_align="$align"
          fi
        done <<< "$alignments"
        perl -ne 'if (/LOAD off.*align 2\*\*(\d+)/) { printf "LOAD align=2**%d bytes=%d\n", $1, 2 ** $1; }' "$objdump_path" >> "$REPORT"
        if (( min_align < 16384 )); then
          echo "FAIL min LOAD alignment ${min_align} < 16384" >> "$REPORT"
          bad_libs=1
        else
          echo "PASS min LOAD alignment ${min_align} >= 16384" >> "$REPORT"
        fi
      fi
    else
      echo "WARN llvm-objdump unavailable; recorded stored APK entry only" >> "$REPORT"
    fi
  done < "$libs_file"
  return "$bad_libs"
}

{
  echo "KeiOS API36 P0-B runtime/native smoke"
  date
  echo ""
  echo "== Tools =="
  echo "SDK_DIR=$SDK_DIR"
  echo "ZIPALIGN=$ZIPALIGN"
  echo "AAPT2=$AAPT2"
  echo "LLVM_OBJDUMP=$LLVM_OBJDUMP"
  echo ""
  echo "== APK =="
  ls -l "$APK_PATH"
  unzip -l "$APK_PATH" 'lib/*.so' 2>/dev/null || true
  if [[ -x "$AAPT2" ]]; then
    "$AAPT2" dump badging "$APK_PATH" 2>/dev/null | head -n 20 || true
  fi
  echo ""
  echo "== Device =="
  "${ADB[@]}" shell getprop ro.build.version.release
  "${ADB[@]}" shell getprop ro.build.version.sdk
  "${ADB[@]}" shell getprop ro.product.manufacturer
  "${ADB[@]}" shell getprop ro.product.model
  "${ADB[@]}" shell getprop ro.product.cpu.abi
  echo "PAGE_SIZE=$("${ADB[@]}" shell getconf PAGE_SIZE | tr -d '\r')"
  echo "screen=${width}x${height}"
} > "$REPORT"

if [[ -x "$ZIPALIGN" ]]; then
  set +e
  "$ZIPALIGN" -c -P 16 -v 4 "$APK_PATH" > "$OUT_DIR/zipalign_16k.txt" 2>&1
  zipalign_rc=$?
  set -e
  {
    echo ""
    echo "== zipalign -P 16 =="
    echo "exit=$zipalign_rc"
    sed -n '1,220p' "$OUT_DIR/zipalign_16k.txt"
  } >> "$REPORT"
else
  echo "WARN zipalign unavailable" >> "$REPORT"
  zipalign_rc=1
fi

native_rc=0
inspect_native_libs || native_rc=$?

"${ADB[@]}" logcat -c || true
"${ADB[@]}" uninstall "$PACKAGE_NAME" >/dev/null 2>&1 || true
sleep 2
install_apk_with_retry "$OUT_DIR/install_fresh.txt"
grant_runtime_permissions
start_main
capture fresh_startup
"${ADB[@]}" shell am broadcast -a os.kei.debug.qa.RUNTIME_SMOKE -n "$PACKAGE_NAME/os.kei.debug.ApiCompatQaReceiver" >/dev/null
sleep 4
pull_runtime_report "$OUT_DIR/runtime_report_fresh.txt"
start_video_fullscreen
capture media3_video_fullscreen
"${ADB[@]}" shell input keyevent KEYCODE_BACK >/dev/null || true
sleep 1
"${ADB[@]}" logcat -d -v time > "$OUT_DIR/logcat_fresh.txt"
filter_runtime_alerts "$OUT_DIR/logcat_fresh.txt" "$ALERTS_FRESH"

"${ADB[@]}" logcat -c || true
install_apk_with_retry "$OUT_DIR/install_upgrade.txt"
grant_runtime_permissions
start_main
capture upgrade_startup
"${ADB[@]}" shell am broadcast -a os.kei.debug.qa.RUNTIME_SMOKE -n "$PACKAGE_NAME/os.kei.debug.ApiCompatQaReceiver" >/dev/null
sleep 4
pull_runtime_report "$OUT_DIR/runtime_report_upgrade.txt"
"${ADB[@]}" logcat -d -v time > "$OUT_DIR/logcat_upgrade.txt"
filter_runtime_alerts "$OUT_DIR/logcat_upgrade.txt" "$ALERTS_UPGRADE"

{
  echo ""
  echo "== Install smoke =="
  echo "fresh install:"
  cat "$OUT_DIR/install_fresh.txt"
  echo "upgrade install:"
  cat "$OUT_DIR/install_upgrade.txt"
  echo ""
  echo "== Runtime report fresh =="
  cat "$OUT_DIR/runtime_report_fresh.txt"
  echo ""
  echo "== Runtime report upgrade =="
  cat "$OUT_DIR/runtime_report_upgrade.txt"
  echo ""
  echo "== Runtime alerts fresh =="
  if [[ -s "$ALERTS_FRESH" ]]; then cat "$ALERTS_FRESH"; else echo "none"; fi
  echo ""
  echo "== Runtime alerts upgrade =="
  if [[ -s "$ALERTS_UPGRADE" ]]; then cat "$ALERTS_UPGRADE"; else echo "none"; fi
} >> "$REPORT"

if [[ "$zipalign_rc" -ne 0 || "$native_rc" -ne 0 ]]; then
  echo "P0-B native validation failed; see $REPORT" >&2
  exit 1
fi

echo "Wrote API36 P0-B runtime/native artifacts to $OUT_DIR"
