#!/bin/bash
# Usage: backup.sh <serial>   Read-only snapshot of the TV's state into backup/<timestamp>/.
set -u
serial=${1:?usage: backup.sh <serial>}
root=$(cd "$(dirname "$0")/.." && pwd)
out="$root/backup/$(date +%Y%m%d-%H%M%S)"
mkdir -p "$out"
sh() { adb -s "$serial" shell "$@"; }

# package id, apk path, and enabled/disabled state, all in one file
sh pm list packages -f -u        > "$out/packages-all.txt"
sh pm list packages -e           > "$out/packages-enabled.txt"
sh pm list packages -d           > "$out/packages-disabled.txt"
sh pm list packages -s           > "$out/packages-system.txt"
sh pm list packages -3           > "$out/packages-thirdparty.txt"
for ns in system secure global; do sh settings list "$ns" > "$out/settings-$ns.txt"; done
sh cmd package resolve-activity --brief -c android.intent.category.HOME -a android.intent.action.MAIN > "$out/home-activity.txt"
sh getprop                       > "$out/getprop.txt"
sh dumpsys meminfo               > "$out/dumpsys-meminfo.txt"
sh dumpsys tv_input              > "$out/dumpsys-tv_input.txt"
sh dumpsys role                  > "$out/dumpsys-role.txt"
echo "$out"
