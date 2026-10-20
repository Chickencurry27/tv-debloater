# tv-debloater

Debloat a Philips Google TV (tested on the TA1, Android 14) over ADB and replace the home screen with a small, fast launcher.
Nothing is uninstalled. Packages are disabled or suspended, every change is logged, and `tools/restore.sh` undoes it.

## What it does
- `tools/find-tv.sh`, `tools/tv-connect.sh <ip>`: find the TV on the LAN and connect over ADB.
- `tools/backup.sh <serial>`: read-only snapshot (package lists, settings, home activity, props, memory) into `backup/<timestamp>/`.
- `lists/`: `disable-clear.txt` (ads, telemetry, junk), `disable-approved.txt` (things the owner said yes to), `keep.txt` (never touched).
- `tools/disable.sh <serial> <list>`: disables each package, one `pm` call per call, and logs it to `manifest/disabled.log`.
- `launcher/`: plain-Android launcher, no network permission, no background service. Greeting and clock, HDMI pills (names and icons set in `InputsRow.java`), a dock of favourite apps (set in `HomeActivity.java`), an Apps button (Play Store) and a Settings gear. 12 nature wallpapers crossfade every 10 minutes.
- `launcher/build-and-install.sh [serial]`: builds with the Android SDK command-line tools (no Gradle) and installs.
- `tools/get-wallpapers.py`: downloads the wallpapers from Wikimedia Commons; credits in `WALLPAPER-CREDITS.md`.

## Restore to stock
```
adb connect <tv-ip>:5555
tools/restore.sh <tv-ip>:5555
```
This re-enables everything in `manifest/disabled.log` (including Google's stock launcher and setup app), unsuspends suspended packages, makes the stock home the default again. Our launcher stays installed but inactive; remove it with `adb uninstall com.tvhome.launcher` if you want.
Then compare with the backup (verified on 2026-10-20: the disabled list was identical to the original): `adb shell pm list packages -d` should match `backup/<timestamp>/packages-disabled.txt`.
Last resort if the TV is unusable: unplug it for 30 seconds. A factory reset also returns it to stock.

## Things learned on this TV
- Wireless debugging and Wi-Fi may be off after the TV restarts. Re-enable them in Settings > System > Developer options.
- The Home key is hard-wired to Google's launcher. It only goes to a custom launcher after both `com.google.android.apps.tv.launcherx` and `com.google.android.tungsten.setupwraith` are disabled, and the TV was restarted.
- `ru.ivi.client` and `com.tpv.moments` are vendor-protected, so they are suspended instead of disabled.
- The remote's Menu key does not work here. The gear tile at the end of the dock opens TV Settings.
- `adb` inside a `while read` loop swallows stdin. Scripts use `</dev/null`.
- HDMI video can't be screenshotted (blank image), so inputs are checked by the active activity instead.
