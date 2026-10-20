#!/bin/bash
# Print IPs on the local /24 that have ADB (tcp 5555) open.
set -u
iface=$(route -n get default 2>/dev/null | awk '/interface:/{print $2}')
me=$(ipconfig getifaddr "$iface" 2>/dev/null)
[ -z "$me" ] && { echo "no network address found" >&2; exit 1; }
prefix=${me%.*}
echo "scanning $prefix.0/24 from $me" >&2
for i in $(seq 1 254); do
  ip="$prefix.$i"
  [ "$ip" = "$me" ] && continue
  ( nc -z -G 3 "$ip" 5555 >/dev/null 2>&1 && echo "$ip" ) &
done
wait
