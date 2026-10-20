#!/bin/bash
# Usage: tv-connect.sh <ip>   Prints the adb serial on success, exits 1 if not authorized.
set -u
ip=${1:?usage: tv-connect.sh <ip>}
serial="$ip:5555"
adb connect "$serial" >&2
for _ in 1 2 3 4 5 6; do
  state=$(adb -s "$serial" get-state 2>/dev/null)
  [ "$state" = "device" ] && { echo "$serial"; exit 0; }
  sleep 2
done
echo "TV not authorized: accept the 'Allow USB debugging?' prompt on the TV (tick Always allow), then re-run" >&2
exit 1
