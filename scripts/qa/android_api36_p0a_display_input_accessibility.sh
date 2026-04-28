#!/usr/bin/env bash
set -euo pipefail

DEVICE=""
PACKAGE_NAME="${PACKAGE_NAME:-os.kei.debug}"
OUT_DIR="${OUT_DIR:-artifacts/api36-p0/p0a-display-input-accessibility}"
DEVICE_WORK_DIR=""
ENABLE_TALKBACK="${ENABLE_TALKBACK:-1}"

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
    -o|--out)
      OUT_DIR="$2"
      shift 2
      ;;
    --device-work-dir)
      DEVICE_WORK_DIR="$2"
      shift 2
      ;;
    --no-talkback)
      ENABLE_TALKBACK=0
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

mkdir -p "$OUT_DIR"
PACKAGE_DIR_SAFE="${PACKAGE_NAME//[^A-Za-z0-9._-]/_}"
DEVICE_WORK_DIR="${DEVICE_WORK_DIR:-/data/local/tmp/keios-api36-p0a/$PACKAGE_DIR_SAFE}"
CURRENT_XML="$OUT_DIR/current.xml"

screen_size="$("${ADB[@]}" shell wm size | sed -n 's/.*: //p' | tr -d '\r' | tail -n 1)"
WIDTH="${screen_size%x*}"
HEIGHT="${screen_size#*x}"
if [[ -z "$WIDTH" || -z "$HEIGHT" || "$WIDTH" == "$HEIGHT" ]]; then
  WIDTH=1220
  HEIGHT=2656
fi

setting_get() {
  "${ADB[@]}" shell settings get "$1" "$2" | tr -d '\r'
}

setting_restore() {
  local namespace="$1"
  local key="$2"
  local value="$3"
  if [[ "$value" == "null" || -z "$value" ]]; then
    "${ADB[@]}" shell settings delete "$namespace" "$key" >/dev/null 2>&1 || true
  else
    "${ADB[@]}" shell settings put "$namespace" "$key" "$value" >/dev/null 2>&1 || true
  fi
}

runtime_permission_state() {
  local permission="$1"
  "${ADB[@]}" shell dumpsys package "$PACKAGE_NAME" | awk -v permission="$permission" '
    index($0, permission ":") > 0 {
      if (index($0, "granted=true") > 0) {
        print "granted"
        found = 1
        exit
      }
      if (index($0, "granted=false") > 0) {
        print "denied"
        found = 1
        exit
      }
    }
    END {
      if (!found) {
        print "unknown"
      }
    }
  ' | tr -d '\r'
}

ORIG_FONT_SCALE="$(setting_get system font_scale)"
ORIG_HIGH_TEXT_CONTRAST="$(setting_get secure high_text_contrast_enabled)"
ORIG_FONT_WEIGHT="$(setting_get secure font_weight_adjustment)"
ORIG_A11Y_SERVICES="$(setting_get secure enabled_accessibility_services)"
ORIG_A11Y_ENABLED="$(setting_get secure accessibility_enabled)"
ORIG_TOUCH_EXPLORATION="$(setting_get secure touch_exploration_enabled)"
ORIG_POST_PERMISSION="$(runtime_permission_state android.permission.POST_NOTIFICATIONS)"

restore_device_state() {
  setting_restore system font_scale "$ORIG_FONT_SCALE"
  setting_restore secure high_text_contrast_enabled "$ORIG_HIGH_TEXT_CONTRAST"
  setting_restore secure font_weight_adjustment "$ORIG_FONT_WEIGHT"
  setting_restore secure enabled_accessibility_services "$ORIG_A11Y_SERVICES"
  setting_restore secure accessibility_enabled "$ORIG_A11Y_ENABLED"
  setting_restore secure touch_exploration_enabled "$ORIG_TOUCH_EXPLORATION"
  if [[ "$ORIG_POST_PERMISSION" == "granted" ]]; then
    "${ADB[@]}" shell pm grant "$PACKAGE_NAME" android.permission.POST_NOTIFICATIONS >/dev/null 2>&1 || true
  elif [[ "$ORIG_POST_PERMISSION" == "denied" ]]; then
    "${ADB[@]}" shell pm revoke "$PACKAGE_NAME" android.permission.POST_NOTIFICATIONS >/dev/null 2>&1 || true
  fi
  "${ADB[@]}" shell rm -rf "$DEVICE_WORK_DIR" >/dev/null 2>&1 || true
}
trap restore_device_state EXIT

"${ADB[@]}" shell rm -rf "$DEVICE_WORK_DIR" >/dev/null 2>&1 || true
"${ADB[@]}" shell mkdir -p "$DEVICE_WORK_DIR" >/dev/null
"${ADB[@]}" shell pm grant "$PACKAGE_NAME" android.permission.POST_NOTIFICATIONS >/dev/null 2>&1 || true
"${ADB[@]}" shell pm grant "$PACKAGE_NAME" android.permission.NEARBY_WIFI_DEVICES >/dev/null 2>&1 || true
SCREENSHOT_DISPLAY_ID="$("${ADB[@]}" shell dumpsys SurfaceFlinger --display-id 2>/dev/null | awk '/^Display / {print $2; exit}')"

capture() {
  local name="$1"
  local device_png="$DEVICE_WORK_DIR/${name}.png"
  local device_xml="$DEVICE_WORK_DIR/${name}.xml"
  if [[ -n "$SCREENSHOT_DISPLAY_ID" ]]; then
    "${ADB[@]}" shell screencap -d "$SCREENSHOT_DISPLAY_ID" -p "$device_png" >/dev/null
  else
    "${ADB[@]}" shell screencap -p "$device_png" >/dev/null
  fi
  "${ADB[@]}" pull "$device_png" "$OUT_DIR/${name}.png" >/dev/null
  "${ADB[@]}" shell uiautomator dump "$device_xml" >/dev/null
  "${ADB[@]}" pull "$device_xml" "$OUT_DIR/${name}.xml" >/dev/null
}

dump_current() {
  local device_xml="$DEVICE_WORK_DIR/current.xml"
  "${ADB[@]}" shell uiautomator dump "$device_xml" >/dev/null
  "${ADB[@]}" pull "$device_xml" "$CURRENT_XML" >/dev/null
}

tap_rel() {
  local x=$(( WIDTH * $1 / 1000 ))
  local y=$(( HEIGHT * $2 / 1000 ))
  "${ADB[@]}" shell input tap "$x" "$y" >/dev/null
  sleep "${3:-1}"
}

tap_node_contains() {
  local needle="$1"
  dump_current
  local coords
  coords="$(NODE_NEEDLE="$needle" perl -Mutf8 -CS -ne '
    BEGIN { $needle = $ENV{"NODE_NEEDLE"}; }
    next unless /bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"/;
    $x = int(($1 + $3) / 2);
    $y = int(($2 + $4) / 2);
    $text = "";
    $desc = "";
    $text = $1 if /text="([^"]*)"/;
    $desc = $1 if /content-desc="([^"]*)"/;
    if (index($text, $needle) >= 0 || index($desc, $needle) >= 0) {
      print "$x $y\n";
      exit 0;
    }
  ' "$CURRENT_XML" | head -n 1)"
  if [[ -z "$coords" ]]; then
    return 1
  fi
  "${ADB[@]}" shell input tap $coords >/dev/null
  sleep 1.2
}

edge_back() {
  local y=$(( HEIGHT / 2 ))
  local x2=$(( WIDTH / 2 ))
  "${ADB[@]}" shell input swipe 3 "$y" "$x2" "$y" 240 >/dev/null
  sleep 1.2
}

key_back() {
  "${ADB[@]}" shell input keyevent KEYCODE_BACK >/dev/null
  sleep 1.2
}

start_main() {
  "${ADB[@]}" shell am force-stop "$PACKAGE_NAME" >/dev/null 2>&1 || true
  "${ADB[@]}" shell am start -n "$PACKAGE_NAME/os.kei.MainActivity" "$@" >/dev/null
  sleep 2
}

start_target_page() {
  local page="$1"
  start_main --es os.kei.extra.TARGET_BOTTOM_PAGE "$page"
  sleep 1
}

start_settings() {
  start_main
  tap_rel 902 98 1.5
}

start_about() {
  start_main
  tap_rel 765 98 1.5
}

start_share_import() {
  "${ADB[@]}" shell am broadcast \
    -a os.kei.debug.qa.ENABLE_GITHUB_SHARE_IMPORT \
    -n "$PACKAGE_NAME/os.kei.debug.ApiCompatQaReceiver" >/dev/null
  "${ADB[@]}" shell am start \
    -a android.intent.action.SEND \
    -t text/plain \
    -n "$PACKAGE_NAME/os.kei.ui.page.main.github.share.GitHubShareImportActivity" \
    --es android.intent.extra.TEXT "https://github.com/topjohnwu/Magisk/releases/download/v27.0/Magisk-v27.0.apk" >/dev/null
  sleep 5
}

start_shell_runner() {
  "${ADB[@]}" shell am start -n "$PACKAGE_NAME/os.kei.debug.ApiCompatQaActivity" \
    -a os.kei.debug.qa.SHELL_RUNNER >/dev/null
  sleep 2
}

start_ba_catalog() {
  "${ADB[@]}" shell am start -n "$PACKAGE_NAME/os.kei.debug.ApiCompatQaActivity" \
    -a os.kei.debug.qa.BA_GUIDE_CATALOG >/dev/null
  sleep 3
}

start_student_detail() {
  "${ADB[@]}" shell am start -n "$PACKAGE_NAME/os.kei.debug.ApiCompatQaActivity" \
    -a os.kei.debug.qa.STUDENT_GUIDE_DETAIL >/dev/null
  sleep 4
}

start_image_fullscreen() {
  "${ADB[@]}" shell am start -n "$PACKAGE_NAME/os.kei.debug.ApiCompatQaActivity" \
    -a os.kei.debug.qa.IMAGE_FULLSCREEN >/dev/null
  sleep 2
}

write_metadata() {
  {
    echo "KeiOS API36 P0-A display/input/accessibility smoke"
    date
    echo ""
    echo "== Device =="
    "${ADB[@]}" shell getprop ro.build.version.release
    "${ADB[@]}" shell getprop ro.build.version.sdk
    "${ADB[@]}" shell getprop ro.product.manufacturer
    "${ADB[@]}" shell getprop ro.product.model
    "${ADB[@]}" shell wm size
    "${ADB[@]}" shell wm density
    echo ""
    echo "== Package =="
    "${ADB[@]}" shell dumpsys package "$PACKAGE_NAME" | grep -E "versionName|targetSdk|POST_NOTIFICATIONS|NEARBY_WIFI_DEVICES" || true
    echo ""
    echo "== Original settings =="
    echo "font_scale=$ORIG_FONT_SCALE"
    echo "high_text_contrast_enabled=$ORIG_HIGH_TEXT_CONTRAST"
    echo "font_weight_adjustment=$ORIG_FONT_WEIGHT"
    echo "enabled_accessibility_services=$ORIG_A11Y_SERVICES"
    echo "accessibility_enabled=$ORIG_A11Y_ENABLED"
    echo "touch_exploration_enabled=$ORIG_TOUCH_EXPLORATION"
    echo "post_notifications=$ORIG_POST_PERMISSION"
    echo "screenshot_display_id=${SCREENSHOT_DISPLAY_ID:-default}"
    echo ""
    echo "== Navigation mode =="
    "${ADB[@]}" shell settings get secure navigation_mode || true
    "${ADB[@]}" shell cmd overlay list | grep -E "navbar|navigation" || true
  } > "$OUT_DIR/metadata.txt"
}

write_metadata

echo "== Typography large/high-contrast smoke ==" > "$OUT_DIR/steps.txt"
"${ADB[@]}" shell settings put system font_scale 1.35 >/dev/null 2>&1 || true
"${ADB[@]}" shell settings put secure high_text_contrast_enabled 1 >/dev/null 2>&1 || true
"${ADB[@]}" shell settings put secure font_weight_adjustment 200 >/dev/null 2>&1 || true
sleep 1

start_main
capture typography_home_large
start_target_page GitHub
capture typography_github_large
start_target_page Ba
capture typography_ba_large
start_ba_catalog
capture typography_ba_catalog_large
key_back
start_settings
capture typography_settings_large
key_back
start_about
capture typography_about_large
key_back
start_shell_runner
capture typography_os_shell_large
key_back
start_share_import
capture typography_github_share_import_large
key_back

echo "== IME focused input smoke ==" >> "$OUT_DIR/steps.txt"
start_shell_runner
tap_node_contains "输入要执行的 shell 命令" || tap_node_contains "shell 输入" || tap_rel 500 825 1
"${ADB[@]}" shell input text "echo_api36_ime" >/dev/null || true
sleep 1
capture ime_os_shell_runner
"${ADB[@]}" shell dumpsys input_method > "$OUT_DIR/ime_os_shell_runner.txt" || true
key_back
key_back

"${ADB[@]}" shell am broadcast \
  -a os.kei.debug.qa.SET_GITHUB_TOKEN_STRATEGY \
  -n "$PACKAGE_NAME/os.kei.debug.ApiCompatQaReceiver" >/dev/null
start_target_page GitHub
tap_node_contains "编辑抓取方案" || tap_rel 905 98 1.5
tap_node_contains "GitHub API Token" || true
tap_node_contains "GitHub API token" || tap_rel 500 570 1
"${ADB[@]}" shell input text "ghp_api36_token" >/dev/null || true
sleep 1
capture ime_github_token
"${ADB[@]}" shell dumpsys input_method > "$OUT_DIR/ime_github_token.txt" || true
key_back
key_back

start_ba_catalog
tap_node_contains "搜索" || tap_rel 500 135 1
"${ADB[@]}" shell input text "api36" >/dev/null || true
sleep 1
capture ime_ba_catalog_filter
"${ADB[@]}" shell dumpsys input_method > "$OUT_DIR/ime_ba_catalog_filter.txt" || true
key_back
key_back

echo "== Permission prompt smoke ==" >> "$OUT_DIR/steps.txt"
"${ADB[@]}" shell pm revoke "$PACKAGE_NAME" android.permission.POST_NOTIFICATIONS >/dev/null 2>&1 || true
start_main
capture accessibility_notification_permission_prompt
key_back
start_settings
capture settings_permission_restricted
tap_node_contains "申请授权" || true
sleep 1
capture settings_permission_request_prompt
key_back
"${ADB[@]}" shell pm grant "$PACKAGE_NAME" android.permission.POST_NOTIFICATIONS >/dev/null 2>&1 || true

echo "== Predictive back gesture/key smoke ==" >> "$OUT_DIR/steps.txt"
"${ADB[@]}" logcat -c || true
start_target_page GitHub
capture back_main_github_before
edge_back
capture back_main_github_after_edge

start_settings
capture back_settings_before_edge
edge_back
capture back_settings_after_edge
start_settings
capture back_settings_before_key
key_back
capture back_settings_after_key

start_student_detail
capture back_student_detail_before_edge
edge_back
capture back_student_detail_after_edge
start_student_detail
capture back_student_detail_before_key
key_back
capture back_student_detail_after_key

start_image_fullscreen
capture back_image_fullscreen_before_edge
edge_back
capture back_image_fullscreen_after_edge
start_image_fullscreen
capture back_image_fullscreen_before_key
key_back
capture back_image_fullscreen_after_key

start_shell_runner
capture back_os_shell_before_edge
edge_back
capture back_os_shell_after_edge
start_shell_runner
capture back_os_shell_before_key
key_back
capture back_os_shell_after_key

start_share_import
capture back_share_import_before_edge
edge_back
capture back_share_import_after_edge
start_share_import
capture back_share_import_before_key
key_back
capture back_share_import_after_key
"${ADB[@]}" logcat -d -v time > "$OUT_DIR/back_logcat.txt" || true
grep -iE "OnBack|BackEvent|CoreBackPreview|predictive|os\\.kei" "$OUT_DIR/back_logcat.txt" > "$OUT_DIR/back_logcat_filtered.txt" || true

echo "== TalkBack/accessibility smoke ==" >> "$OUT_DIR/steps.txt"
TALKBACK_SERVICE=""
if "${ADB[@]}" shell pm path com.google.android.marvin.talkback >/dev/null 2>&1; then
  TALKBACK_SERVICE="com.google.android.marvin.talkback/.TalkBackService"
fi

{
  echo "talkback_service=$TALKBACK_SERVICE"
  "${ADB[@]}" shell dumpsys accessibility | sed -n '1,180p' || true
} > "$OUT_DIR/accessibility_before_talkback.txt"

if [[ "$ENABLE_TALKBACK" == "1" && -n "$TALKBACK_SERVICE" ]]; then
  if [[ "$ORIG_A11Y_SERVICES" == "null" || -z "$ORIG_A11Y_SERVICES" ]]; then
    "${ADB[@]}" shell settings put secure enabled_accessibility_services "$TALKBACK_SERVICE" >/dev/null 2>&1 || true
  elif [[ "$ORIG_A11Y_SERVICES" == *"$TALKBACK_SERVICE"* ]]; then
    "${ADB[@]}" shell settings put secure enabled_accessibility_services "$ORIG_A11Y_SERVICES" >/dev/null 2>&1 || true
  else
    "${ADB[@]}" shell settings put secure enabled_accessibility_services "$ORIG_A11Y_SERVICES:$TALKBACK_SERVICE" >/dev/null 2>&1 || true
  fi
  "${ADB[@]}" shell settings put secure accessibility_enabled 1 >/dev/null 2>&1 || true
  "${ADB[@]}" shell settings put secure touch_exploration_enabled 1 >/dev/null 2>&1 || true
  sleep 4
  "${ADB[@]}" shell dumpsys accessibility > "$OUT_DIR/accessibility_talkback_enabled.txt" || true

  start_share_import
  capture talkback_github_share_import
  key_back
  start_ba_catalog
  capture talkback_ba_catalog_fetch_save_export_surface
  key_back
  start_settings
  capture talkback_settings_permissions
  key_back
  start_shell_runner
  capture talkback_os_shell_output
  key_back
  "${ADB[@]}" shell pm revoke "$PACKAGE_NAME" android.permission.POST_NOTIFICATIONS >/dev/null 2>&1 || true
  start_main
  capture talkback_notification_permission_prompt
  key_back
  "${ADB[@]}" shell pm grant "$PACKAGE_NAME" android.permission.POST_NOTIFICATIONS >/dev/null 2>&1 || true
  "${ADB[@]}" shell dumpsys accessibility > "$OUT_DIR/accessibility_after_talkback_surfaces.txt" || true
else
  echo "TalkBack service unavailable or disabled by --no-talkback" > "$OUT_DIR/accessibility_talkback_enabled.txt"
fi

echo "Wrote API36 P0-A display/input/accessibility artifacts to $OUT_DIR"
