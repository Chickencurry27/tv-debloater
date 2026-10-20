#!/bin/bash
# Usage: disable.sh <serial> <list-file>
# Disables each package (reversible), one pm call per shell call, and logs it to manifest/disabled.log.
set -u
serial=${1:?usage: disable.sh <serial> <list-file>}
list=${2:?usage: disable.sh <serial> <list-file>}
root=$(cd "$(dirname "$0")/.." && pwd)
log="$root/manifest/disabled.log"
touch "$log"
sed 's/#.*//; s/[[:space:]]//g; /^$/d' "$list" | while read -r pkg; do
  if grep -qx "$pkg" <(grep -v '^#' "$root/lists/keep.txt" | sed 's/#.*//; s/[[:space:]]//g'); then
    echo "SKIP (in keep list): $pkg"; continue
  fi
  state=$(adb -s "$serial" shell pm list packages -e "$pkg" </dev/null | tr -d '\r')
  case "$state" in
    *"package:$pkg"*) ;;
    *) echo "SKIP (not enabled/not present): $pkg"; continue ;;
  esac
  out=$(adb -s "$serial" shell pm disable-user --user 0 "$pkg" </dev/null 2>&1 | tr -d '\r')
  echo "$out"
  case "$out" in
    *"new state: disabled-user"*) printf '%s\tenabled\t%s\n' "$pkg" "$(date +%FT%T)" >> "$log" ;;
    *) echo "FAILED: $pkg" ;;
  esac
done
