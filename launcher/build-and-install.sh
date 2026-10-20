#!/bin/bash
# Usage: build-and-install.sh [serial]   Builds launcher/app.apk with the Android SDK tools; installs it if a serial is given.
set -eu
here=$(cd "$(dirname "$0")" && pwd)
export JAVA_HOME=${JAVA_HOME:-/opt/homebrew/opt/openjdk@17}
SDK=${ANDROID_HOME:-/opt/homebrew/share/android-commandlinetools}
BT=$SDK/build-tools/34.0.0
JAR=$SDK/platforms/android-34/android.jar
PATH=$JAVA_HOME/bin:$PATH
out=$here/build; rm -rf "$out"; mkdir -p "$out/classes" "$out/dex"

$BT/aapt2 link -o "$out/base.apk" -I "$JAR" --manifest "$here/AndroidManifest.xml" \
  --min-sdk-version 26 --target-sdk-version 34 -A "$here/assets"
javac --release 8 -Xlint:-options -cp "$JAR" -d "$out/classes" $(find "$here/src" -name '*.java')
$BT/d8 --release --min-api 26 --lib "$JAR" --output "$out/dex" $(find "$out/classes" -name '*.class')
cp "$out/base.apk" "$out/unaligned.apk"
(cd "$out/dex" && zip -q -j "$out/unaligned.apk" classes.dex)
$BT/zipalign -p -f 4 "$out/unaligned.apk" "$out/aligned.apk"
ks=$here/debug.keystore
[ -f "$ks" ] || keytool -genkeypair -keystore "$ks" -storepass android -keypass android -alias tvhome \
  -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=tv-home" >/dev/null 2>&1
$BT/apksigner sign --ks "$ks" --ks-pass pass:android --key-pass pass:android --out "$here/app.apk" "$out/aligned.apk"
echo "built $here/app.apk ($(du -h "$here/app.apk" | cut -f1))"

if [ $# -ge 1 ]; then
  s=$1
  adb -s "$s" install -r "$here/app.apk"
fi
