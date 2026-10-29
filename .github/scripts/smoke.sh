#!/usr/bin/env bash
# Installe l'APK, accorde les permissions ADB, ouvre chaque onglet et échoue si l'appli plante.
set -u
APK_DIR="$1"
PKG=fr.tflag.droidboost
OUT=smoke-out
mkdir -p "$OUT"

adb install -r "$(ls "$APK_DIR"/*.apk | head -1)" || exit 1
for p in PACKAGE_USAGE_STATS BATTERY_STATS DUMP WRITE_SECURE_SETTINGS READ_LOGS POST_NOTIFICATIONS; do
  adb shell pm grant "$PKG" "android.permission.$p" || true
done
adb shell appops set "$PKG" GET_USAGE_STATS allow || true
adb logcat -c

adb shell am start -W -n "$PKG/.MainActivity"
sleep 8
adb exec-out screencap -p > "$OUT/1-tableau-de-bord.png"

# Onglets de la barre de navigation : 4 boutons répartis sur la largeur, en bas de l'écran.
read -r W H < <(adb shell wm size | sed -E 's/.*: ([0-9]+)x([0-9]+).*/\1 \2/')
Y=$((H - H / 22))
i=2
for tab in applis stockage acces; do
  X=$((W * (2 * i - 1) / 8))
  adb shell input tap "$X" "$Y"
  sleep 8
  adb exec-out screencap -p > "$OUT/$i-$tab.png"
  i=$((i + 1))
done

# Détail de la première appli de la liste.
adb shell input tap $((W / 8 * 3)) "$Y"
sleep 6
adb shell input tap $((W / 2)) $((H * 2 / 5))
sleep 6
adb exec-out screencap -p > "$OUT/5-detail-appli.png"

adb logcat -d > "$OUT/logcat.txt"
if grep -E "FATAL EXCEPTION|ANR in $PKG" "$OUT/logcat.txt"; then
  grep -A 40 "FATAL EXCEPTION" "$OUT/logcat.txt" | head -80
  echo "L'appli a planté"
  exit 1
fi
if ! adb shell pidof "$PKG" > /dev/null; then
  echo "Le processus de l'appli n'est plus vivant"
  exit 1
fi
echo "Aucun plantage détecté"
