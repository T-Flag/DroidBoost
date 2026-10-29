# DroidBoost — périmètre et plan de réalisation

Application personnelle de surveillance et de maintenance pour un **Pixel 8 Pro sous Android 17**, installée en sideload.
Décisions de cadrage (29/10/2026) :

- **Pas de root** : le téléphone sert de moyen de paiement (Play Integrity doit rester intact).
- **Shizuku accepté** (ou un serveur compatible : Porter, fork thedjchi) et **commandes ADB ponctuelles depuis un PC** acceptées.
- **Mesure et actions dans la même appli**, en implémentant le maximum de fonctions.

Le détail des applis existantes et des contraintes techniques est dans
[`reports/Panorama des applis de monitoring Android.md`](../reports/Panorama%20des%20applis%20de%20monitoring%20Android.md).

## Principes d'architecture

1. **Trois niveaux d'accès, dégradation propre.** Chaque fonction déclare le niveau minimal dont elle a besoin et
   l'interface explique ce qui manque au lieu d'échouer en silence.
   - **A — appli normale** + accès spéciaux activés dans les Réglages (données d'usage, accès à tous les fichiers,
     notifications, superposition).
   - **B — `pm grant` ponctuels** (BATTERY_STATS, DUMP, PACKAGE_USAGE_STATS, WRITE_SECURE_SETTINGS, READ_LOGS) :
     accordés une fois via ADB ou via Shizuku, ils survivent aux redémarrages.
   - **C — shell ADB via Shizuku** : actions sur les applis, `dumpsys`, températures du HAL, charge CPU.
     Perdu à chaque redémarrage : l'appli le détecte et le signale.
2. **La mesure ne dépend jamais de Shizuku** quand une API publique ou un `pm grant` suffit ; Shizuku ne fait qu'enrichir.
3. **Commandes shell validées** : tout nom de paquet ou de permission est vérifié avant d'être injecté dans une commande.
4. **Tout reste local** : aucune donnée ne quitte le téléphone (pas de permission INTERNET).
5. **Code testable** : les analyseurs (`dumpsys`, `/proc`, sorties de `pm`) et la logique vivent dans le module `core`
   (Kotlin pur, testé par JUnit) ; l'appli Android (`app`) s'occupe des API système et de l'interface.

Pile technique : Kotlin 2.4, Jetpack Compose + Material 3 (couleurs dynamiques), AGP 9.5, compileSdk 37,
targetSdk 36, minSdk 34, Shizuku API 13.1.5, Room, WorkManager, DataStore.

## Fonctions retenues, par phase

Légende du niveau : **A**, **B**, **C** comme ci-dessus.

**État au 29/10/2026 : les six phases sont implémentées** (compilées par la CI et vérifiées écran par écran
sur émulateur), à valider sur le Pixel 8 Pro réel. Écarts par rapport au plan initial :

- L'historique des AppOps (micro, caméra, localisation au-delà de 7 jours) est reporté : le format de
  `dumpsys appops` doit d'abord être relevé sur le téléphone.
- Les températures des capteurs, les plantages/ANR et la conso par appli marchent **sans Shizuku** dès que
  la permission DUMP est accordée (dump direct des services système).
- La base locale est en version 2 ; les prochaines évolutions de schéma devront fournir une migration
  (le repli destructif ne sert que de filet de sécurité).

### Phase 1 — Fondations et temps réel

| Fonction | Niveau |
|---|---|
| Écran **Accès** : état de Shizuku, des accès spéciaux et des `pm grant`, octroi des grants en un geste via Shizuku, commandes ADB à copier | A / C |
| **Tableau de bord** : batterie (niveau, courant, puissance, tension, température, cycles, santé API), thermique (statut, marge, températures du HAL), CPU (fréquence par cœur et par cluster, charge), mémoire (RAM, swap/zRAM), stockage, débit réseau, uptime et sommeil profond, version et correctif de sécurité | A, B (santé batterie), C (températures HAL, charge CPU, swap) |
| **Applications** : liste complète (utilisateur, système, désactivées, désinstallées pour l'utilisateur), recherche, tri (nom, taille, cache, dernière utilisation, installation, mise à jour), détails (versions, SDK, installateur, tailles, permissions) | A, B (tailles, usage) |
| Actions sur les applis : ouvrir, infos système, désinstaller avec confirmation | A |
| Actions privilégiées : forcer l'arrêt, désactiver / réactiver, suspendre, désinstaller pour l'utilisateur 0 (repli automatique sur la désactivation si Android 17 refuse), réinstaller (`install-existing`), effacer les données, révoquer / accorder une permission, bucket de veille, restriction d'arrière-plan | C |
| **Stockage** : total / libre, répartition par catégorie, classement des applis par taille et par cache, vidage global des caches (`pm trim-caches` via Shizuku, ou dialogue système) | A, B, C |
| **Notification permanente** avec les mesures clés (service de premier plan `specialUse`), relancée au démarrage | A |

### Phase 2 — Historique, batterie et thermique

| Fonction | Niveau |
|---|---|
| Base locale (Room) d'échantillons : batterie, thermique, CPU, RAM, réseau ; rétention configurable | A |
| Graphiques d'historique (24 h, 7 j, 30 j) | A |
| **Sessions de charge** : début/fin, durée, mAh injectés, puissance moyenne et max, température max, effet de la limite à 80 % | A |
| **Santé batterie** : santé API, capacité estimée par sessions, capacité « apprise » (`dumpsys`), dates de fabrication / première utilisation | A, B |
| **Historique thermique** : marge, statut, épisodes de throttling avec l'appli au premier plan et l'état de charge | A, B |
| Alertes : seuil de charge, température, batterie faible au repos | A |

### Phase 3 — Journal et diagnostic

| Fonction | Niveau |
|---|---|
| **Journal unifié** : installations, mises à jour, désinstallations, changements de permissions, changement de build Android, état de Shizuku | A |
| **Raisons de sortie** de toutes les applis : processus tués par le LMK, ANR, plantages, limites mémoire d'Android 17 | B (DUMP) |
| **Comparaison avant / après mise à jour** du système : drain au repos, température, temps CPU | A |
| Export CSV / JSON des historiques et du journal | A |

### Phase 4 — Réseau, vie privée et sécurité

| Fonction | Niveau |
|---|---|
| Consommation de données par appli (mobile / Wi-Fi), historique jour / mois, alertes | B |
| Inventaire des permissions sensibles par appli et historique des changements | A |
| Historique des AppOps (micro, caméra, localisation…) au-delà des 7 jours natifs | C |
| Bilan de posture de sécurité : correctif, démarrage vérifié, ADB, sources d'installation, applis avec accessibilité / admin / écouteur de notifications | A, C |

### Phase 5 — Maintenance avancée

| Fonction | Niveau |
|---|---|
| Gros fichiers, doublons (hash), dossiers vides, restes d'applis désinstallées | A (accès à tous les fichiers), C (Android/data) |
| Mesure du cache par appli dans le temps et alertes de croissance | B |
| **Debloat Pixel guidé** : liste vérifiée avec niveau de risque, désactivation par défaut, registre des retraits, vérification après chaque OTA | C |
| Consommation batterie par appli et wakelocks (`dumpsys batterystats`) | B (DUMP) / C |
| Énergie par sous-système (rails ODPM : CPU, GPU, modem, écran) | A / B |
| Suggestions d'archivage des applis inutilisées | A |

### Phase 6 — Alertes intelligentes et confort

| Fonction | Niveau |
|---|---|
| Détection d'anomalies par métrique (ligne de base propre au téléphone) | A |
| Widgets d'écran d'accueil, tuiles des Réglages rapides, overlay flottant | A |
| Surveillance de Shizuku et aide à la relance (débogage sans fil via WRITE_SECURE_SETTINGS) | B |

## Hors périmètre (impossible sans root ou contre-productif)

- Vider le cache d'**une seule** appli (API réservée à la plateforme) — seule la purge globale est possible.
- Désinstaller une appli système sous Android 17 si le système le refuse : la désactivation reste disponible.
- Limite de charge autre que les 80 % natifs, blocage de composants, sauvegarde avec données, `pm hide`.
- « Booster RAM », « refroidisseur » et autres tueurs de tâches : inutiles depuis Android 14.
- Thermomètre infrarouge du Pixel 8 Pro : réservé à l'appli système de Google.
- Pare-feu par VPN : écarté pour l'instant, car il occuperait l'unique slot VPN (VPN by Google, etc.).

## Installation et compilation

- La CI GitHub Actions compile l'APK à chaque push (`.github/workflows/android.yml`) et le publie comme artefact.
- Installer par ADB (`adb install -r app-release.apk`) : les installations ADB sont exemptées de la vérification des
  développeurs de Google.
- Voir le [README](../README.md) pour les commandes ADB à lancer une fois et la configuration de la signature.
