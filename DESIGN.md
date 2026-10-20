# Philips Google TV (TA1): debloat and minimal home screen

Date: 2026-10-20

## Intent
The TV is slow in the home screen and when opening apps. Cause (my guess, to be verified on the device): heavy stock home screen with sponsored rows, plus many preinstalled/background apps competing for limited RAM. Goal: a fast, ad-free TV that still does everything the owner uses, and can be returned to stock at any time.

## Goals
1. Find the TV on the Wi-Fi and connect over ADB.
2. Back up before any change: installed package list with enabled/disabled state, relevant settings, default home/launcher, device info. Stored in `~/www/tv-debloater/backup/<timestamp>/`.
3. Disable (never uninstall) manufacturer/Google ads, telemetry and junk. Every disabled package is recorded so it can be re-enabled.
4. Build and install a small, fast launcher (plain Android, no network access, no background services).
5. Provide `restore.sh` that re-enables everything disabled and restores the original launcher.
6. Check each step with a screenshot taken from the TV.

## Non-goals
- No uninstalling, rooting, flashing or bootloader changes.
- No custom ROM, no changes to the TV's system partition.
- No support for other TV brands.

## Anti-goals (failure even if it technically works)
- Breaking Chromecast, HDMI inputs/CEC, or audio (optical out to soundbar, TV speakers).
- Breaking any of the 10 apps or the Play Store.
- Leaving the TV without a working Home button or launcher.
- Disabling something with no way to undo it.

## Decisions made by the owner
- Apps in the dock, in order: YouTube, Netflix, Prime Video, F1 TV, HBO Max, Apple TV, Jellyfin, SkyShowtime, NPO Start, Videoland. Then a divider and an **Apps** button that opens the Play Store.
- Layout: greeting and date top left, clock top right, a **centered row of HDMI pills directly above the dock**, dock at the bottom, nature wallpaper behind, rotating on a timer. (Mockup: `home-layout-b-v4`.)
- Built-in features to keep: **Chromecast, HDMI inputs, audio output** (optical out to a soundbar, or TV speakers).
- Owner wants to be asked before anything touching apps or features they might use.

## Constraints
- Mac (macOS) host; `adb` is not installed. Plan: use Homebrew `android-platform-tools`, ask first. macOS can block `adb` on the local network; fallback is a Python ADB client (`adb-shell`) as used by the reference repo.
- TV must have Developer options and network debugging enabled by the owner. The TV will show an authorization prompt the owner must accept.
- Reference repo `github.com/mihirgupta0900/tv-home` is inspiration only. Its scripts are not run before being read. It was tested on a TCL, not a Philips.

## Builder's calls (mine, not agreed with the owner)
- Disable list is built from this TV's actual package list, tiered: (a) clearly ads/telemetry, (b) preinstalled junk, (c) anything uncertain goes to the owner as a question. Nothing in (c) is touched without a yes.
- Google TV's setup app (`com.google.android.tungsten.setupwraith`) may take the Home button; disable it only if testing shows it does.
- HDMI port count and names are read from the TV (TV input framework), not assumed. Placeholder was three.
- HDMI pills launch via the TV input framework; if a port can't be launched directly, fall back to the stock input picker.
- Wallpapers: about 12 freely licensed nature photos (Wikimedia Commons featured pictures), 1920x1080, crossfade about every 10 minutes, with credits file.
- Launcher re-checks for newly installed apps when returning to it, and shows only the 10 favourites plus Apps and HDMI.
- Commands run one `pm` call per shell call, to avoid silent stops.
- Screenshots are saved next to each step in `~/www/tv-debloater/shots/`.
- Work lives in `~/www/tv-debloater/` (not a git repo, so this file is not committed).

## Left to the builder
Launcher code structure, exact focus behaviour with the remote, icon sourcing, performance measurements (RAM before and after).

## Acceptance
- Home screen opens quickly and shows the agreed layout on the real TV (screenshot).
- All 10 apps launch from the dock; Apps opens the Play Store; each HDMI pill switches input.
- Chromecast casting and audio still work (owner confirms).
- `restore.sh` run on the TV returns it to the stock launcher and re-enables every disabled package (screenshot).
