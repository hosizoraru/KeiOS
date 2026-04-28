#!/usr/bin/env bash
set -euo pipefail

DEVICE=""
PACKAGE_NAME="${PACKAGE_NAME:-os.kei.debug}"
APK_PATH="${APK_PATH:-app/build/outputs/apk/debug/app-debug.apk}"
OUT_DIR="${OUT_DIR:-artifacts/api37-p1/p1b-background-profile}"
IDLE_SECONDS="${IDLE_SECONDS:-600}"
BUILD_APK=1
ALLOW_PHYSICAL=0

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
    --idle-seconds)
      IDLE_SECONDS="$2"
      shift 2
      ;;
    --skip-build)
      BUILD_APK=0
      shift
      ;;
    --allow-physical)
      ALLOW_PHYSICAL=1
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
else
  emulator_devices=()
  while IFS= read -r serial; do
    emulator_devices+=( "$serial" )
  done < <(adb devices | awk '$2 == "device" && $1 ~ /^emulator-/ { print $1 }')
  if [[ "${#emulator_devices[@]}" -eq 1 ]]; then
    DEVICE="${emulator_devices[0]}"
    ADB+=( -s "$DEVICE" )
  elif [[ "${#emulator_devices[@]}" -eq 0 ]]; then
    echo "No Android emulator is connected. Start an Android 17 AVD or pass -s with --allow-physical intentionally." >&2
    exit 2
  else
    echo "Multiple emulators are connected. Pass -s <serial> for the target Android 17 AVD." >&2
    exit 2
  fi
fi

mkdir -p "$OUT_DIR"
REPORT="$OUT_DIR/report.txt"

if [[ "$BUILD_APK" -eq 1 ]]; then
  ./gradlew :app:assembleDebug
fi

if [[ ! -f "$APK_PATH" ]]; then
  echo "APK not found: $APK_PATH" >&2
  exit 1
fi

device_sdk="$("${ADB[@]}" shell getprop ro.build.version.sdk | tr -d '\r')"
device_release="$("${ADB[@]}" shell getprop ro.build.version.release | tr -d '\r')"
device_model="$("${ADB[@]}" shell getprop ro.product.model | tr -d '\r')"
is_emulator="$("${ADB[@]}" shell getprop ro.kernel.qemu | tr -d '\r')"
if [[ "$ALLOW_PHYSICAL" != "1" && "$is_emulator" != "1" ]]; then
  echo "Refusing to run P1-B idle profiling on a physical device. Use an Android 17 AVD or pass --allow-physical intentionally." >&2
  exit 3
fi
if [[ "${device_sdk:-0}" -lt 37 ]]; then
  echo "Android 17 / API 37 device required; got release=$device_release sdk=$device_sdk model=$device_model" >&2
  exit 4
fi

setting_get() {
  "${ADB[@]}" shell settings get "$1" "$2" | tr -d '\r'
}

ORIG_LOW_POWER="$(setting_get global low_power)"
ORIG_BUCKET="$("${ADB[@]}" shell am get-standby-bucket "$PACKAGE_NAME" 2>/dev/null | tr -d '\r' || true)"

restore_device_state() {
  "${ADB[@]}" shell cmd deviceidle unforce >/dev/null 2>&1 || true
  "${ADB[@]}" shell dumpsys deviceidle unforce >/dev/null 2>&1 || true
  "${ADB[@]}" shell cmd battery reset >/dev/null 2>&1 || true
  if [[ "$ORIG_LOW_POWER" == "null" || -z "$ORIG_LOW_POWER" ]]; then
    "${ADB[@]}" shell settings delete global low_power >/dev/null 2>&1 || true
  else
    "${ADB[@]}" shell settings put global low_power "$ORIG_LOW_POWER" >/dev/null 2>&1 || true
  fi
  if [[ -n "$ORIG_BUCKET" ]]; then
    "${ADB[@]}" shell am set-standby-bucket "$PACKAGE_NAME" "$ORIG_BUCKET" >/dev/null 2>&1 || true
  else
    "${ADB[@]}" shell am set-standby-bucket "$PACKAGE_NAME" active >/dev/null 2>&1 || true
  fi
  "${ADB[@]}" shell dumpsys batterystats disable full-history >/dev/null 2>&1 || true
}
trap restore_device_state EXIT

filter_package_context() {
  local source="$1"
  local target="$2"
  grep -F "$PACKAGE_NAME" -C 8 "$source" > "$target" || true
}

collect_state() {
  local label="$1"
  "${ADB[@]}" shell dumpsys alarm > "$OUT_DIR/${label}_alarm.txt" || true
  filter_package_context "$OUT_DIR/${label}_alarm.txt" "$OUT_DIR/${label}_alarm_${PACKAGE_NAME}.txt"
  "${ADB[@]}" shell dumpsys jobscheduler > "$OUT_DIR/${label}_jobscheduler.txt" || true
  filter_package_context "$OUT_DIR/${label}_jobscheduler.txt" "$OUT_DIR/${label}_jobscheduler_${PACKAGE_NAME}.txt"
  "${ADB[@]}" shell dumpsys download > "$OUT_DIR/${label}_download.txt" 2>/dev/null || true
  filter_package_context "$OUT_DIR/${label}_download.txt" "$OUT_DIR/${label}_download_${PACKAGE_NAME}.txt"
  "${ADB[@]}" shell dumpsys batterystats --wakeups > "$OUT_DIR/${label}_wakeups.txt" || true
  filter_package_context "$OUT_DIR/${label}_wakeups.txt" "$OUT_DIR/${label}_wakeups_${PACKAGE_NAME}.txt"
  "${ADB[@]}" shell dumpsys activity exit-info "$PACKAGE_NAME" > "$OUT_DIR/${label}_exit_info.txt" || true
  "${ADB[@]}" shell dumpsys deviceidle > "$OUT_DIR/${label}_deviceidle.txt" || true
}

"${ADB[@]}" install -r "$APK_PATH" > "$OUT_DIR/install.txt"
"${ADB[@]}" shell pm clear "$PACKAGE_NAME" > "$OUT_DIR/pm_clear.txt" || true
"${ADB[@]}" shell pm grant "$PACKAGE_NAME" android.permission.POST_NOTIFICATIONS >/dev/null 2>&1 || true
"${ADB[@]}" shell pm grant "$PACKAGE_NAME" android.permission.NEARBY_WIFI_DEVICES >/dev/null 2>&1 || true
"${ADB[@]}" shell cmd activity clear-exit-info "$PACKAGE_NAME" >/dev/null 2>&1 || true
"${ADB[@]}" shell dumpsys batterystats --reset >/dev/null 2>&1 || true
"${ADB[@]}" shell dumpsys batterystats enable full-history >/dev/null 2>&1 || true
"${ADB[@]}" logcat -c || true

"${ADB[@]}" shell am start -n "$PACKAGE_NAME/os.kei.MainActivity" > "$OUT_DIR/start_main.txt"
sleep 3
"${ADB[@]}" shell am broadcast \
  -a os.kei.debug.qa.PREPARE_P1B_BACKGROUND_PROFILE \
  -n "$PACKAGE_NAME/os.kei.debug.ApiCompatQaReceiver" > "$OUT_DIR/prepare_broadcast.txt"
sleep 2
"${ADB[@]}" exec-out run-as "$PACKAGE_NAME" cat files/api37_p1b_background_profile_report.txt \
  > "$OUT_DIR/prep_report.txt" 2>/dev/null || echo "prep report unavailable" > "$OUT_DIR/prep_report.txt"

collect_state initial

"${ADB[@]}" shell input keyevent KEYCODE_HOME >/dev/null 2>&1 || true
"${ADB[@]}" shell am make-uid-idle "$PACKAGE_NAME" >/dev/null 2>&1 || true
"${ADB[@]}" shell am set-standby-bucket "$PACKAGE_NAME" working_set >/dev/null 2>&1 || true
"${ADB[@]}" shell cmd battery unplug >/dev/null 2>&1 || true
"${ADB[@]}" shell settings put global low_power 1 >/dev/null 2>&1 || true
"${ADB[@]}" shell cmd deviceidle force-idle >/dev/null 2>&1 || \
  "${ADB[@]}" shell dumpsys deviceidle force-idle >/dev/null 2>&1 || true
sleep "$IDLE_SECONDS"

collect_state after_idle
"${ADB[@]}" logcat -d -v time > "$OUT_DIR/logcat.txt" || true
grep -iE "A17AnomalyProfiler|os\\.kei|AndroidRuntime|FATAL EXCEPTION|MemoryLimiter|excessive resource|Profiling" \
  "$OUT_DIR/logcat.txt" > "$OUT_DIR/logcat_filtered.txt" || true
grep -iE "EXCESSIVE_RESOURCE_USAGE|MemoryLimiter|excessive" \
  "$OUT_DIR/after_idle_exit_info.txt" > "$OUT_DIR/exit_resource_alerts.txt" || true
grep -iE "AndroidRuntime|FATAL EXCEPTION" \
  "$OUT_DIR/logcat_filtered.txt" > "$OUT_DIR/logcat_fatal_alerts.txt" || true

github_alarm_count="$(grep -c "os.kei.background.action.GITHUB_TICK" "$OUT_DIR/after_idle_alarm_${PACKAGE_NAME}.txt" || true)"
ba_alarm_count="$(grep -c "os.kei.background.action.BA_AP_TICK" "$OUT_DIR/after_idle_alarm_${PACKAGE_NAME}.txt" || true)"
job_count="$(grep -c "$PACKAGE_NAME" "$OUT_DIR/after_idle_jobscheduler_${PACKAGE_NAME}.txt" || true)"
download_count="$(grep -c "$PACKAGE_NAME" "$OUT_DIR/after_idle_download_${PACKAGE_NAME}.txt" || true)"
wakeup_count="$(grep -c "$PACKAGE_NAME" "$OUT_DIR/after_idle_wakeups_${PACKAGE_NAME}.txt" || true)"
resource_exit_count="$(wc -l < "$OUT_DIR/exit_resource_alerts.txt" | tr -d ' ')"
fatal_alert_count="$(wc -l < "$OUT_DIR/logcat_fatal_alerts.txt" | tr -d ' ')"

{
  echo "KeiOS API37 P1-B background scheduling long-idle profile"
  date
  echo ""
  echo "== Device =="
  echo "release=$device_release"
  echo "sdk=$device_sdk"
  "${ADB[@]}" shell getprop ro.product.manufacturer | tr -d '\r'
  echo "model=$device_model"
  echo "qemu=$is_emulator"
  echo ""
  echo "== Package =="
  "${ADB[@]}" shell dumpsys package "$PACKAGE_NAME" | grep -E "versionName|targetSdk|POST_NOTIFICATIONS|NEARBY_WIFI_DEVICES" || true
  echo ""
  echo "== Prep report =="
  cat "$OUT_DIR/prep_report.txt"
  echo ""
  echo "== Summary after idle =="
  echo "idle_seconds=$IDLE_SECONDS"
  echo "github_alarm_records=$github_alarm_count"
  echo "ba_alarm_records=$ba_alarm_count"
  echo "jobscheduler_records=$job_count"
  echo "download_records=$download_count"
  echo "batterystats_wakeup_records=$wakeup_count"
  echo "resource_exit_alerts=$resource_exit_count"
  echo "fatal_alerts=$fatal_alert_count"
  echo ""
  echo "== Alarm records after idle =="
  if [[ -s "$OUT_DIR/after_idle_alarm_${PACKAGE_NAME}.txt" ]]; then
    cat "$OUT_DIR/after_idle_alarm_${PACKAGE_NAME}.txt"
  else
    echo "none"
  fi
  echo ""
  echo "== Wakeup records after idle =="
  if [[ -s "$OUT_DIR/after_idle_wakeups_${PACKAGE_NAME}.txt" ]]; then
    cat "$OUT_DIR/after_idle_wakeups_${PACKAGE_NAME}.txt"
  else
    echo "none"
  fi
  echo ""
  echo "== Exit resource alerts =="
  if [[ -s "$OUT_DIR/exit_resource_alerts.txt" ]]; then
    cat "$OUT_DIR/exit_resource_alerts.txt"
  else
    echo "none"
  fi
  echo ""
  echo "== Fatal logcat alerts =="
  if [[ -s "$OUT_DIR/logcat_fatal_alerts.txt" ]]; then
    cat "$OUT_DIR/logcat_fatal_alerts.txt"
  else
    echo "none"
  fi
} > "$REPORT"

if [[ "$resource_exit_count" != "0" || "$fatal_alert_count" != "0" ]]; then
  echo "P1-B background profile validation failed; see $REPORT" >&2
  exit 5
fi

echo "Wrote API37 P1-B background profile artifacts to $OUT_DIR"
