#!/bin/bash
# Usage: restore.sh <serial>
# Re-enables every package this project disabled, and puts the stock home back.
set -u
serial=${1:?usage: restore.sh <serial>}
root=$(cd "$(dirname "$0")/.." && pwd)
log="$root/manifest/disabled.log"
[ -f "$log" ] || { echo "no manifest at $log, nothing to restore"; exit 0; }
cut -f1 "$log" | sort -u | while read -r pkg; do
  [ -z "$pkg" ] && continue
  adb -s "$serial" shell pm enable --user 0 "$pkg" </dev/null 2>&1 | tr -d '\r'
  adb -s "$serial" shell pm unsuspend --user 0 "$pkg" </dev/null 2>&1 | tr -d '\r'
done
# stock home back as default, our launcher (if installed) removed from the HOME role
adb -s "$serial" shell pm enable com.google.android.apps.tv.launcherx 2>&1 | tr -d '\r'
adb -s "$serial" shell pm uninstall --user 0 com.tvhome.launcher 2>/dev/null | tr -d '\r'
adb -s "$serial" shell cmd package set-home-activity com.google.android.apps.tv.launcherx/.home.HomeActivity 2>&1 | tr -d '\r'
adb -s "$serial" shell input keyevent KEYCODE_HOME
echo "restore done. Compare: adb -s $serial shell pm list packages -d  with backup/*/packages-disabled.txt"
