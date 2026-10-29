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
./gradlew :app:assembleRelease       # APK release (clé stable si les variables DROIDBOOST_* sont définies, sinon clé de debug)
PKG=fr.tflag.droidboost.debug bash .github/scripts/smoke.sh app/build/outputs/apk/debug   # parcours de tous les écrans
```

La variante debug a son propre paquet : elle s'installe à côté de la release du téléphone sans toucher à son
historique, mais il faut lui accorder les permissions à part :

```bash
adb shell 'for p in PACKAGE_USAGE_STATS BATTERY_STATS DUMP WRITE_SECURE_SETTINGS READ_LOGS ACCESS_FINE_POWER_MONITORS; do pm grant fr.tflag.droidboost.debug android.permission.$p; done; appops set fr.tflag.droidboost.debug GET_USAGE_STATS allow'
```

Depuis le 29/10/2026, la CI signe la release avec une clé stable (PKCS12, alias `droidboost`) fournie par les secrets
du dépôt `DROIDBOOST_KEYSTORE_B64`, `DROIDBOOST_STORE_PASSWORD`, `DROIDBOOST_KEY_ALIAS` et `DROIDBOOST_KEY_PASSWORD` :
chaque APK de la CI s'installe par-dessus celui du téléphone et garde son historique. La clé et son mot de passe ne
sont jamais dans le dépôt, ni dans aucun fichier qu'il contient. Une release compilée sans ces variables est signée
avec la clé de debug : Android la refuse comme mise à jour de l'appli installée.

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

## Vérifié sur le Pixel 8 Pro (29/10/2026)

Android 17, build `CP3A.261005.005`, correctif du 5 octobre 2026. Variante debug installée par ADB, d'abord sans
Shizuku, puis avec Shizuku 13.6 (Play Store).

### Niveaux A et B

- **Fréquences CPU** : lisibles dans sysfs depuis l'appli. 9 cœurs : 0-3 jusqu'à 1,70 GHz, 4-7 jusqu'à 2,37 GHz,
  8 jusqu'à 2,91 GHz. `/proc/stat` reste refusé à l'appli : la charge CPU demande Shizuku.
- **Températures du HAL** : lues via DUMP sans Shizuku (HAL thermique AIDL v3, une trentaine de capteurs). La plupart
  des capteurs Pixel ont le type -1 : ils s'affichent sous leur seul nom et restent hors de l'historique par type.
- **Rails ODPM** : 24 rails lus avec `ACCESS_FINE_POWER_MONITORS`. L'API renvoie aussi les consommateurs d'énergie
  dérivés (`DISPLAY`, `CPU/0`…), écartés de l'affichage pour ne pas compter deux fois la même énergie.
- **Batterie** : dates de fabrication et de première utilisation, cycles et `STATE_OF_HEALTH` lisibles avec
  BATTERY_STATS. Mais le Pixel publie 100 % (`health_index=100`, `health_status=-1`, indice non calibré) alors que la
  jauge a appris 4 596 mAh pour 5 050 mAh nominaux (5 022 selon `charge_full_design`), soit ≈ 91 % : la santé
  système n'est pas fiable sur ce téléphone.
  `/sys/class/power_supply` est refusé à l'appli. La capacité apprise se lit dans `dumpsys batterystats --charged <paquet>`
  (≈ 0,6 Mo, contre 20 Mo sans argument) : le tableau de bord en tire la « Santé estimée (jauge) ».
- **`pm uninstall -k --user 0`** sur une appli système : Android 17 refuse avec
  `Failure [only root can delete system app for a particular user]`, que `CommandOutcome` reconnaît.
  `pm disable-user --user 0` et `pm enable --user 0` fonctionnent.
- **`appops query-op <OP> allow`** : disponible, un paquet par ligne. **`pm archive`** : refuse les applis sans
  installeur (`No installer found`), donc celles installées par ADB ; archive une appli du Play Store en gardant ses
  données, et `pm request-unarchive` la fait retélécharger en une dizaine de secondes. Une appli archivée perd
  `FLAG_INSTALLED` et ne publie plus ni version ni permissions : l'appli la distingue d'un retrait par
  `ApplicationInfo.isArchived`.
- **`dumpsys batterystats`** : sur Android 17, les lignes `UID` sont au même niveau d'indentation que l'en-tête
  « Estimated power use ».
- **Courant batterie** : `BATTERY_PROPERTY_CURRENT_NOW` est en µA. Près de zéro (charge à l'arrêt à 80 %,
  consommation égale à l'apport du chargeur), une valeur sous 10 000 est bien des µA, pas des mA.
- **Service de premier plan, notification, overlay** (rafraîchi chaque seconde), **tuiles** (sous-titres, toucher) et
  **widget** (rafraîchi chaque minute par le service) : OK. Une mise à jour de l'appli arrête le service : il est
  relancé par `MY_PACKAGE_REPLACED`.

### Niveau C (Shizuku)

- **Démarrage** : Shizuku 13.6 ne fournit plus `start.sh` ; la commande affichée par l'appli Shizuku est
  `adb shell /data/app/~~…/moe.shizuku.privileged.api-…/lib/arm64/libshizuku.so`. Elle démarre sous Android 17.
- **Charge CPU** (totale et par cœur, via `/proc/stat`) et **swap zRAM** : OK, conformes à `/proc/meminfo`.
- **Vidage global des caches** (`pm trim-caches`) depuis l'écran Stockage et depuis la tuile : OK. Le premier vidage a
  libéré environ 18 Go et pris une à deux minutes, sous la limite de 120 s.
- **Octroi des permissions en un geste** : READ_LOGS porte des groupes Linux (`log`, `update_engine_log` dans
  `platform.xml`) et Android tue l'appli pour l'appliquer. Ce type de permission passe donc en dernier
  (`DevGrant.restartsApp`), après les autres et après `GET_USAGE_STATS`.
- **Restes d'applis** (Android/data, obb, media) : listés via Shizuku, résultat identique au contrôle par le shell.
- **Debloat** : liste et boutons disponibles. Google Wallet et le NFC ne figurent pas au catalogue.
- **Actions sur une appli** (testées sur `com.vincentlee.compass`) : arrêt forcé, désactivation et réactivation,
  suspension, accord et retrait d'une permission, bucket de veille, restriction d'arrière-plan, effacement des données,
  archivage et désarchivage : OK.
  - `pm enable` laisse l'état « activé » explicite (1) au lieu de l'état par défaut (0) ; `pm default-state` le remet.
  - `am set-standby-bucket` marque le bucket comme forcé (`reason=f`), même pour revenir à la valeur d'origine.
  - `pm revoke` répond succès sans rien changer pour `ACCESS_LOCAL_NETWORK` quand Android l'accorde d'office aux
    applis qui ciblent un SDK antérieur (drapeau `REVOKE_WHEN_REQUESTED`) : l'appli relit l'état réel après chaque
    changement de permission.

Pas testé, volontairement : `pm uninstall --user 0` sur une appli utilisateur (il la supprime complètement) et la
relance de Shizuku par le débogage sans fil.
