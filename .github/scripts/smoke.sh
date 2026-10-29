#!/usr/bin/env bash
# Installe l'APK, parcourt les écrans en cherchant leurs textes à l'écran (uiautomator),
# active la surveillance continue, puis échoue si l'appli a planté ou si un écran n'affiche pas ce qu'il doit.
set -u
APK_DIR="$1"
PKG=fr.tflag.droidboost
OUT=smoke-out
mkdir -p "$OUT"
FAILURES=0
STEP=0

adb install -r "$(ls "$APK_DIR"/*.apk | head -1)" || exit 1
for p in PACKAGE_USAGE_STATS BATTERY_STATS DUMP WRITE_SECURE_SETTINGS READ_LOGS POST_NOTIFICATIONS; do
  adb shell pm grant "$PKG" "android.permission.$p" || true
done
adb shell appops set "$PKG" GET_USAGE_STATS allow || true
adb logcat -c

# Écran courant au format XML ; l'écran se met à jour en continu, d'où les essais répétés.
dump() {
  for _ in 1 2 3 4; do
    if adb shell uiautomator dump /sdcard/ui.xml > /dev/null 2>&1; then
      adb shell cat /sdcard/ui.xml
      return 0
    fi
    sleep 2
  done
  return 1
}

shot() {
  STEP=$((STEP + 1))
  adb exec-out screencap -p > "$OUT/$STEP-$1.png"
}

expect() {
  local screen="$1"; shift
  local xml
  xml=$(dump) || { echo "?? $screen : écran illisible par uiautomator"; return; }
  for text in "$@"; do
    if echo "$xml" | grep -qF "text=\"$text"; then
      echo "OK $screen : « $text »"
    else
      echo "KO $screen : « $text » introuvable"
      FAILURES=$((FAILURES + 1))
    fi
  done
}

# Touche le centre du premier élément dont le texte commence par $1.
tap_text() {
  local xml bounds
  xml=$(dump) || return 1
  bounds=$(echo "$xml" | tr '>' '\n' | grep -F "text=\"$1" | head -1 | grep -oE 'bounds="\[[0-9]+,[0-9]+\]\[[0-9]+,[0-9]+\]"')
  [ -z "$bounds" ] && { echo "KO bouton « $1 » introuvable"; FAILURES=$((FAILURES + 1)); return 1; }
  read -r x1 y1 x2 y2 < <(echo "$bounds" | grep -oE '[0-9]+' | tr '\n' ' ')
  adb shell input tap $(((x1 + x2) / 2)) $(((y1 + y2) / 2))
}

tap_class() {
  local xml bounds
  xml=$(dump) || { echo "?? écran illisible pour trouver $1"; return 1; }
  bounds=$(echo "$xml" | tr '>' '\n' | grep -F "class=\"$1\"" | head -1 | grep -oE 'bounds="\[[0-9]+,[0-9]+\]\[[0-9]+,[0-9]+\]"')
  [ -z "$bounds" ] && { echo "KO élément $1 introuvable"; echo "$xml" | tr '>' '\n' | grep -oE 'class="[^"]+"' | sort | uniq -c; return 1; }
  read -r x1 y1 x2 y2 < <(echo "$bounds" | grep -oE '[0-9]+' | tr '\n' ' ')
  adb shell input tap $(((x1 + x2) / 2)) $(((y1 + y2) / 2))
}

back() { adb shell input keyevent 4; sleep 3; }

adb shell am start -W -n "$PKG/.MainActivity"
sleep 8
shot tableau-de-bord
expect "Tableau de bord" "Notification permanente" "Batterie" "Thermique"

# Surveillance continue : l'interrupteur de la première carte.
tap_class "android.widget.Switch" && sleep 8
if adb shell dumpsys activity services "$PKG" | grep -q "MonitorService"; then
  echo "OK service de surveillance démarré"
else
  echo "KO service de surveillance absent"
  FAILURES=$((FAILURES + 1))
fi

tap_text "Applis" && sleep 10
shot applis
expect "Applis" "Utilisateur" "Système"
tap_text "DroidBoost" && sleep 5
shot detail-appli
expect "Détail" "Actions" "Actions avancées"
back

tap_text "Stockage" && sleep 10
shot stockage
expect "Stockage" "Répartition" "Caches"

tap_text "Journal" && sleep 8
shot journal
expect "Journal" "Tout" "Stabilité" "Début du journal"

tap_text "Plus" && sleep 3
shot plus
expect "Plus" "Batterie" "Thermique" "Accès" "Réglages"
tap_text "Batterie" && sleep 5
shot batterie
expect "Batterie" "Santé de la batterie" "Historique"
back
tap_text "Thermique" && sleep 5
shot thermique
expect "Thermique" "Maintenant" "Historique"
back
tap_text "Réseau" && sleep 6
shot reseau
expect "Réseau" "Débit en direct" "Consommation"
back
tap_text "Vie privée" && sleep 8
shot vie-privee
expect "Vie privée" "Inclure les applis système" "Permissions sensibles accordées"
back
tap_text "Sécurité" && sleep 8
shot securite
expect "Sécurité" "Bilan de sécurité" "Correctif de sécurité"
back
tap_text "Réglages" && sleep 3
shot reglages
expect "Réglages" "Historique" "Alertes"
back
tap_text "Accès" && sleep 3
shot acces
expect "Accès" "Shizuku" "Accès spéciaux"

# Laisse tourner le service assez longtemps pour au moins un enregistrement d'historique.
sleep 70
adb shell dumpsys notification --noredact 2>/dev/null | grep -A 3 "pkg=$PKG" | head -20

adb logcat -d > "$OUT/logcat.txt"
grep -E "MonitorService|DroidBoost" "$OUT/logcat.txt" | grep -E " [EW] " | head -20
if grep -E "FATAL EXCEPTION|ANR in $PKG" "$OUT/logcat.txt"; then
  grep -A 40 "FATAL EXCEPTION" "$OUT/logcat.txt" | head -80
  echo "L'appli a planté"
  exit 1
fi
if ! adb shell pidof "$PKG" > /dev/null; then
  echo "Le processus de l'appli n'est plus vivant"
  exit 1
fi
if [ "$FAILURES" -gt 0 ]; then
  echo "$FAILURES vérification(s) en échec"
  exit 1
fi
echo "Aucun plantage, tous les écrans vérifiés"
