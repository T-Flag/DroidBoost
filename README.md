# DroidBoost

Application Android personnelle de surveillance et de maintenance d'un **Pixel 8 Pro (Android 17)**, sans root :
batterie, thermique, CPU, mémoire, stockage, gestion avancée des applis via Shizuku.

## Fonctions

| Écran | Contenu |
|---|---|
| Tableau de bord | Batterie (courant, puissance, cycles, santé), thermique (marge, capteurs), CPU par cœur et charge, RAM et zRAM, stockage, débit, système ; notification permanente |
| Applis | Liste filtrable et triable (taille, cache, usage, dates), fiche détaillée, actions Shizuku : arrêt forcé, désactivation, suspension, retrait pour l'utilisateur (repli sur la désactivation sous Android 17), réinstallation, effacement des données, permissions, restriction d'arrière-plan, bucket de veille |
| Stockage | Répartition, classement par taille et cache, vidage global des caches, plus fortes croissances sur 7 jours, applis inutilisées à archiver |
| Journal | Installations, mises à jour, permissions, mises à jour système, plantages / ANR / arrêts mémoire, actions, anomalies ; comparaison avant / après mise à jour ; export ZIP de CSV |
| Plus › Batterie | Santé (API, jauge, capacité mesurée), historique, sessions de charge, conso par appli, énergie par composant (ODPM) |
| Plus › Thermique | Marge et seuils, capteurs, historique, épisodes de bridage avec l'appli au premier plan |
| Plus › Réseau | Données mobile / Wi-Fi par appli, histogramme 30 jours, débit en direct |
| Plus › Nettoyage | Gros fichiers, doublons, dossiers vides, restes d'applis désinstallées |
| Plus › Debloat Pixel | Applis préinstallées classées par risque, désactivation réversible, réapplication après mise à jour |
| Plus › Vie privée / Sécurité | Permissions sensibles par appli, accès spéciaux, bilan de sécurité |
| Plus › Réglages | Enregistrement, rétention, alertes (charge, température, bridage, anomalies), moniteur flottant, débogage sans fil |

Également : widget d'écran d'accueil, tuiles « DroidBoost » et « Vider les caches » des Réglages rapides.

- Périmètre et plan par phases : [`docs/perimetre.md`](docs/perimetre.md)
- État de l'art des applis existantes : [`reports/`](reports/)

## Récupérer l'APK

Chaque push déclenche la compilation GitHub Actions (onglet **Actions** du dépôt). L'APK se trouve dans
l'artefact `droidboost-<numéro>` du dernier run réussi.

Installation recommandée par ADB (les installations ADB sont exemptées de la vérification des développeurs) :

```bash
adb install -r app-release.apk
```

## Accès à donner une fois

1. **Shizuku** (ou Porter) : à démarrer via le débogage sans fil, puis autoriser DroidBoost dans l'écran **Accès**.
   Shizuku doit être relancé après chaque redémarrage.
2. **Permissions ADB** : le bouton « Tout accorder via Shizuku » de l'écran **Accès** suffit. Sans Shizuku, depuis un PC :

```bash
adb shell pm grant fr.tflag.droidboost android.permission.PACKAGE_USAGE_STATS
adb shell pm grant fr.tflag.droidboost android.permission.BATTERY_STATS
adb shell pm grant fr.tflag.droidboost android.permission.DUMP
adb shell pm grant fr.tflag.droidboost android.permission.WRITE_SECURE_SETTINGS
adb shell pm grant fr.tflag.droidboost android.permission.READ_LOGS
adb shell pm grant fr.tflag.droidboost android.permission.ACCESS_FINE_POWER_MONITORS
adb shell appops set fr.tflag.droidboost GET_USAGE_STATS allow
```

Ces permissions survivent aux redémarrages. DUMP suffit à obtenir les températures des capteurs, les plantages et la
consommation par appli même quand Shizuku est arrêté.

3. Dans l'écran **Accès**, activez aussi l'accès aux données d'usage, l'accès à tous les fichiers (Nettoyage) et la
   superposition (moniteur flottant). Si Android parle de « paramètre restreint » : Infos de l'appli > ⋮ >
   « Autoriser les paramètres restreints ».
4. Sur le **Tableau de bord**, activez la notification permanente : c'est elle qui enregistre l'historique et
   déclenche les alertes.

## Signature stable (recommandé)

Sans configuration, la CI signe avec une clé de debug gardée en cache GitHub (elle peut expirer après 7 jours sans
compilation, ce qui obligerait à désinstaller l'appli et à redonner les accès). Pour une clé permanente, générez-la
une fois sur votre PC :

```bash
keytool -genkeypair -v -keystore droidboost.jks -alias droidboost -keyalg RSA -keysize 4096 -validity 10000
```

Puis ajoutez ces secrets dans **Settings > Secrets and variables > Actions** du dépôt :
`DROIDBOOST_KEYSTORE_B64` (sortie de `base64 -w0 droidboost.jks`), `DROIDBOOST_STORE_PASSWORD`,
`DROIDBOOST_KEY_ALIAS` (`droidboost`) et `DROIDBOOST_KEY_PASSWORD`. Le dépôt étant public, ne commitez jamais la clé.

## Développement

- `core/` : logique pure (analyseurs `dumpsys` / `/proc`, commandes shell validées, formats), testée par JUnit :
  `./gradlew -p core test`
- `app/` : appli Android (Kotlin, Jetpack Compose, Material 3), AGP 9.5, compileSdk 37, targetSdk 36, minSdk 34.
