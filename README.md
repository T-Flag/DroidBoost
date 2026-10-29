# DroidBoost

Application Android personnelle de surveillance et de maintenance d'un **Pixel 8 Pro (Android 17)**, sans root :
batterie, thermique, CPU, mémoire, stockage, gestion avancée des applis via Shizuku.

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
adb shell appops set fr.tflag.droidboost GET_USAGE_STATS allow
```

Ces permissions survivent aux redémarrages.

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
