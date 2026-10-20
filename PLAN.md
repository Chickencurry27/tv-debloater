# Philips TA1 Debloat and Launcher Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Back up, debloat (reversibly) and replace the home screen of the owner's Philips Google TV, with a one-command restore.

**Architecture:** Shell/Python scripts in `~/www/tv-debloater/tools/` drive the TV over ADB (one `pm` call per shell call). A small plain-Android launcher (Kotlin, Views, no network permission) lives in `~/www/tv-debloater/launcher/` and is built with Gradle, then installed and set as Home. Every package change is logged to a manifest that `restore.sh` replays in reverse.

**Tech Stack:** adb (Homebrew `android-platform-tools`), Python 3 with `adb-shell` as fallback transport, JDK 17, Android SDK command-line tools, Gradle, Kotlin.

**Spec:** `/Users/ferry/www/tv-debloater/DESIGN.md`

## Global Constraints

- Disable only. Never `pm uninstall`, never root, never touch system partitions.
- Keep working: Chromecast, HDMI inputs/CEC, audio (optical out to soundbar, TV speakers), Play Store, the 10 dock apps.
- Dock order: YouTube, Netflix, Prime Video, F1 TV, HBO Max, Apple TV, Jellyfin, SkyShowtime, NPO Start, Videoland, then a divider and an Apps button that opens the Play Store.
- Layout: greeting + date top left, clock top right, HDMI pills centered directly above the dock, nature wallpaper behind, rotating about every 10 minutes.
- Launcher has no INTERNET permission and no background service.
- Ask the owner before disabling anything that is not clearly ads/telemetry/junk.
- Backup before the first change. Screenshot from the TV after every step into `~/www/tv-debloater/shots/`.
- Host is macOS. Ask before installing anything with Homebrew.

## Review Focus

- TV IP changes after reboot (DHCP): scripts re-run discovery instead of using a stored IP.
- ADB authorization prompt not accepted: scripts stop with a clear message, not a hang.
- `pm disable-user` on a package that does not exist on this model: skipped and logged, not a failure.
- Dock app not installed (e.g. F1 TV missing): tile is hidden or opens the Play Store listing, and the launcher does not crash.
- Home button during launcher crash: stock launcher still enabled so the TV is never stuck; `restore.sh` works with only the TV and the Mac.

---

### Task 1: ADB and TV discovery

**Files:**
- Create: `tools/find-tv.sh`, `tools/tv-connect.sh`

**Interfaces:**
- Produces: `tv-connect.sh` prints the connected serial (`<ip>:5555`) on stdout, exit 1 if not authorized.

- [ ] **Step 1:** Ask the owner for approval, then `brew install android-platform-tools`. Verify: `adb version` prints a version.
- [ ] **Step 2:** Ask the owner to enable Developer options (Settings > Device Preferences > About > tap Build 7 times) and Network/USB debugging.
- [ ] **Step 3:** Implement `find-tv.sh`: scan the local subnet for hosts with port 5555 open (`arp -a` plus a timed TCP probe) and print candidates. Verify: lists the TV's IP.
- [ ] **Step 4:** Implement `tv-connect.sh <ip>`: `adb connect`, fail with a message if state is `unauthorized`. Verify: `adb -s <serial> shell getprop ro.product.model` returns the Philips model.
- [ ] **Step 5:** Take a screenshot (`adb exec-out screencap -p > shots/01-connected.png`), view it, confirm the TV shows its normal screen.
- [ ] **Step 6:** If macOS blocks `adb` on the local network, switch to the `adb-shell` Python client behind the same two script interfaces.

### Task 2: Backup

**Files:**
- Create: `tools/backup.sh`; output in `backup/<timestamp>/`

**Interfaces:**
- Produces: `backup/<ts>/packages.txt` (`pm list packages -f -u` plus enabled/disabled state), `settings-{system,secure,global}.txt`, `home-activity.txt` (`cmd package resolve-activity` for HOME), `getprop.txt`, `dumpsys-meminfo.txt`.

- [ ] **Step 1:** Implement `backup.sh <serial>` to write the files above.
- [ ] **Step 2:** Run it. Verify each file is non-empty and `packages.txt` contains `com.android.vending`.
- [ ] **Step 3:** Record baseline: memory and the time a cold-start of the stock home screen takes. Screenshot `02-baseline.png`.

### Task 3: Inventory and classification (owner questions)

**Files:**
- Create: `lists/disable-clear.txt`, `lists/disable-ask.txt`, `lists/keep.txt`

**Interfaces:**
- Produces: plain text, one package id per line, `#` comments allowed.

- [ ] **Step 1:** List third-party and system packages for this TV and sort them into: clearly ads/telemetry/junk, uncertain, and keep (Cast, TV input/CEC, audio, Play Store, the 10 apps, system UI).
- [ ] **Step 2:** Ask the owner, in one message, about every package in `disable-ask.txt`, with a plain-language note on what each does.
- [ ] **Step 3:** Move the owner's answers into the right list. Verify no package from `keep.txt` appears in a disable list.

### Task 4: Disable and restore scripts

**Files:**
- Create: `tools/disable.sh`, `tools/restore.sh`, `manifest/disabled.log`

**Interfaces:**
- Consumes: `lists/disable-clear.txt` and approved entries from `lists/disable-ask.txt`.
- Produces: `disable.sh <serial> <list>`; `restore.sh <serial>` re-enables every package in the manifest and the original home.

- [ ] **Step 1:** Implement `disable.sh`: for each package, one call `pm disable-user --user 0 <pkg>`, append `pkg<TAB>previous-state` to the manifest; skip missing packages with a log line.
- [ ] **Step 2:** Implement `restore.sh`: one `pm enable <pkg>` per manifest line, then re-enable the stock home and clear our launcher as default.
- [ ] **Step 3:** Apply in small batches. After each batch: screenshot, confirm the TV responds, Cast and HDMI still listed. Verify with `pm list packages -d`.
- [ ] **Step 4:** Test the restore path now with one batch (disable, restore, confirm state matches `backup/`), then re-apply.
- [ ] **Step 5:** Measure memory and responsiveness against the baseline. Screenshot `04-debloated.png`.

### Task 5: Launcher

**Files:**
- Create: `launcher/` Gradle project; `launcher/app/src/main/res/values/config.xml` (dock package ids)
- Create: `launcher/app/src/main/java/.../HomeActivity.kt`, `DockView.kt`, `InputsRow.kt`, `WallpaperRotator.kt`

**Interfaces:**
- Produces: APK with a HOME + LEANBACK_LAUNCHER intent filter; `config.xml` holds the ordered dock list.

- [ ] **Step 1:** Install the Android SDK command-line tools and JDK 17 (ask first). Verify `./gradlew assembleDebug` builds an empty activity.
- [ ] **Step 2:** `HomeActivity`: greeting by time of day, date, clock (updates per minute), layout per the design mockup.
- [ ] **Step 3:** `DockView`: resolves each package via `getLeanbackLaunchIntentForPackage`, falling back to the launch intent, then the Play Store listing if missing. Focus is D-pad driven and remembers the last tile. Divider then Apps tile that opens `market://` home.
- [ ] **Step 4:** `InputsRow`: query `TvInputManager` for HDMI inputs and show one pill each, centered above the dock; launch via the input's passthrough URI, falling back to the stock input picker.
- [ ] **Step 5:** `WallpaperRotator`: crossfade a bundled set every 10 minutes with a text scrim. Add about 12 Wikimedia Commons photos at 1920x1080 under `assets/wallpapers/` and `WALLPAPER-CREDITS.md`.
- [ ] **Step 6:** Re-scan installed apps on `onResume` so newly installed ones are picked up.

### Task 6: Install launcher and verify on the TV

**Files:**
- Create: `launcher/build-and-install.sh`

- [ ] **Step 1:** Build, `adb install -r`, set as default home. Keep the stock launcher enabled. Force-stop the stock launcher to avoid a stale home task.
- [ ] **Step 2:** Press Home. Screenshot `06-home.png`; confirm greeting, clock, HDMI row, dock and Apps button match the mockup.
- [ ] **Step 3:** Launch each of the 10 apps from the dock (D-pad via `input keyevent`), screenshot each, return Home.
- [ ] **Step 4:** Select each HDMI pill, screenshot, return Home. Ask the owner to confirm Chromecast and soundbar audio.
- [ ] **Step 5:** Measure memory and responsiveness versus baseline and report numbers.

### Task 7: Restore drill

- [ ] **Step 1:** Run `restore.sh`. Confirm the stock home returns (screenshot `07-restored.png`) and `pm list packages -d` matches `backup/<ts>/packages.txt`.
- [ ] **Step 2:** Re-apply the debloat and launcher, since the owner wants it. Write `README.md` with the restore command.

## Self-review notes

- Spec coverage: backup (T2), reversible disable (T4), launcher with layout, HDMI, Apps button, wallpapers (T5), screenshots (every task), restore script (T4, T7), keep-list (T3).
- Disable and restore use the same manifest, so names match across T4 and T7.
- Review Focus items map to: T1 steps 3-4 (IP, auth), T4 step 1 (missing package), T5 step 3 (missing app), T6 step 1 (stock home stays enabled).
