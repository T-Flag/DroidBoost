# DroidBoost

Appli Android personnelle de surveillance et de maintenance pour un **Pixel 8 Pro sous Android 17**, installée en
sideload. Contraintes fixées par le propriétaire du téléphone :

- **Jamais de root** : le téléphone sert de moyen de paiement.
- Shizuku et les commandes ADB ponctuelles sont acceptés.
- Mesure **et** actions dans la même appli, avec le maximum de fonctions.

Pour le détail, voir `docs/perimetre.md` (fonctions par phase et niveau d'accès), `README.md` (installation, accès à
accorder) et `reports/` (étude des applis existantes).

## Structure

- `core/` : build inclus en Kotlin pur. Il contient les analyseurs (`dumpsys`, `/proc`, sorties de `pm`), les
  commandes shell validées (`shell/ShellCommands.kt`), les formats français, et la logique de suivi, d'alertes,
  d'anomalies, de journal et de debloat. Tout ce qui peut y vivre y vit, avec ses tests JUnit.
- `app/` : appli Android (Compose, Material 3, Room, DataStore, Glance, Shizuku). Le code est dans
  `app/src/main/java/fr/tflag/droidboost/` :
  - `AppContainer.kt` : injection manuelle ;
  - `shell/` : UserService Shizuku et AIDL ;
  - `system/` : lecteurs batterie, thermique, CPU, RAM, stockage, rails d'alimentation ;
  - `monitor/` : service de premier plan, enregistrement, alertes, overlay ;
  - `journal/`, `maintenance/`, `privacy/`, `network/` ;
  - `ui/` : un dossier par écran, avec son ViewModel.

## Commandes

```bash
./gradlew -p core test               # tests de la logique pure
./gradlew :app:installDebug          # variante debug : paquet fr.tflag.droidboost.debug, nom « DroidBoost dev »
./gradlew :app:assembleRelease       # APK release (clé de debug si les variables DROIDBOOST_* sont absentes)
PKG=fr.tflag.droidboost.debug bash .github/scripts/smoke.sh app/build/outputs/apk/debug   # parcours de tous les écrans
```

La variante debug a son propre paquet : elle s'installe à côté de la release du téléphone sans toucher à son
historique, mais il faut lui accorder les permissions à part :

```bash
adb shell 'for p in PACKAGE_USAGE_STATS BATTERY_STATS DUMP WRITE_SECURE_SETTINGS READ_LOGS ACCESS_FINE_POWER_MONITORS; do pm grant fr.tflag.droidboost.debug android.permission.$p; done; appops set fr.tflag.droidboost.debug GET_USAGE_STATS allow'
```

Le script de parcours sert à la CI (`.github/workflows/smoke.yml`, émulateur API 35) et marche aussi sur un émulateur
local. Il cherche des textes à l'écran : si un libellé change dans l'interface, mets le script à jour.

## Règles

- Interface et messages de commit en français. Le code et les commentaires sont en anglais.
- Pas de permission INTERNET : rien ne quitte le téléphone.
- Toute commande shell passe par `ShellCommands` et ses noms de paquet ou de permission validés. Jamais de
  concaténation libre.
- Chaque fonction déclare son niveau d'accès : A (appli normale), B (`pm grant` ponctuel) ou C (Shizuku). Quand
  l'accès manque, l'écran l'explique (`MissingAccessHint`) au lieu d'échouer en silence. La mesure ne dépend de Shizuku
  que si aucune API publique ni permission accordée ne suffit.
- Avec la permission DUMP, `ServiceDumper` lit directement les services système (thermalservice, batterystats…), sans
  Shizuku.
- Base Room en version 2 : tout changement de schéma doit fournir une `Migration`. Le repli destructif efface
  l'historique du téléphone.
- SDK : compileSdk 37, targetSdk 36, minSdk 34. Les versions des dépendances sont dans `gradle/libs.versions.toml`.

## Pas encore vérifié sur le vrai téléphone

Jusqu'ici, l'appli n'a été testée que sur émulateur (API 35). Points à confirmer sur le Pixel 8 Pro branché en ADB :

- fréquences CPU lisibles dans sysfs ;
- températures du HAL via DUMP ;
- rails ODPM (`ACCESS_FINE_POWER_MONITORS`) ;
- santé et capacité de la batterie (propriétés privilégiées de `BatteryManager`) ;
- repli de `pm uninstall --user 0` sur `pm disable-user` quand Android 17 refuse ;
- disponibilité de `appops query-op` et de `pm archive` ;
- widget, tuiles et overlay en usage réel.
