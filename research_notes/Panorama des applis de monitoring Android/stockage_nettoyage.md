# Analyse du stockage, nettoyage et gestion du cache sur Android (état octobre 2026), cible Pixel 8 Pro

> Notes de recherche rédigées le 2026-10-28. **Limite de méthode, à lire avant tout le reste** : dans cette session, seuls `github.com` et `developer.android.com` étaient accessibles en lecture directe. `play.google.com`, `support.google.com`, `androidpolice.com`, `reddit.com`, `ftc.gov`, `ccleaner.com`, `apkmirror.com`, `appgoblin.info`, `wikipedia.org`, `sdmse.darken.eu`, `giga.de` et `itechify.com` étaient bloqués (DNS/proxy). Le quota de recherche web partagé (200 requêtes) a aussi été épuisé en cours de route. Les affirmations sourcées sur ces domaines viennent donc des **extraits/résumés du moteur de recherche** et non d'une lecture de la page. Elles sont signalées « (via résumé de recherche) ». Les affirmations sur SD Maid SE, Shizuku, App Manager, DiskUsage, Disky, Material Files, Fossify, Porter et les API Android viennent de **lectures directes** des sources primaires.

## 1. Quelles applis font référence en 2025-2026 : éditeur, prix, licence, accès requis, maintenance

### Takeaway
La seule référence « sérieuse » activement maintenue est **SD Maid 2/SE** (open source GPLv3, freemium, dernière RC le 19/10/2026). Elle combine nettoyage, analyse, doublons et historique de stockage, et contourne les restrictions d'Android par l'accessibilité, Porter/Shizuku (ADB) ou root. Les autres options se répartissent en trois groupes. (a) Les outils grand public de Google : **Files by Google**, plus les réglages Pixel. (b) Les « cleaners » commerciaux de Gen Digital (**CCleaner, Avast Cleanup, AVG Cleaner, Norton Clean**), plus marketing et entachés de controverses de confidentialité. (c) Des analyseurs et gestionnaires de fichiers (DiskUsage, Disky, Storage Analyzer & Disk Usage, Cx, Solid, MiXplorer, Material Files, Fossify) dont la maintenance va d'active à abandonnée.

### Cited Findings

**SD Maid 2/SE (eu.darken.sdmse), développeur « darken » (organisation GitHub d4rken-org)**
- Se décrit comme « Android's most thorough cleaning tool ». Outils : CorpseFinder (restes d'applis désinstallées), AppCleaner (caches et fichiers jetables), SystemCleaner (filtres configurables), Deduplicator, StorageAnalyzer, AppControl, Scheduler, Media Squeeze (compression d'images), Swiper (tri par balayage). Accès root ou ADB via Porter ou Shizuku. Licence du code GPL v3 (hors icônes, docs, textes Play). 7,9k étoiles, 24 issues ouvertes. Le projet se dit « actively being worked on and not feature complete ». — [GitHub sdmaid-se](https://github.com/d4rken-org/sdmaid-se)
- Dernières versions (toutes des release candidates) : v2.2.2-rc0 (2026-10-19), v2.2.1-rc0 (2026-10-07), v2.2.0-rc0 (2026-09-29), v2.1.0-rc0 (2026-09-06), v2.0.4-rc0 (25 août). — [releases.atom](https://github.com/d4rken-org/sdmaid-se/releases.atom), [Releases](https://github.com/d4rken-org/sdmaid-se/releases)
- Contenu récent des versions :
  - v2.2.0 : correction du nettoyage de cache AppCleaner sur Motorola/Sony Android 16+, et ajout de **Porter** comme option d'accès ADB à côté de Shizuku.
  - v2.2.1 : correction pour les ROM Android 17 qui masquent les boutons. Storage Trend conserve l'historique jusqu'à 1 an.
  - v2.2.2 : correction du vidage de cache sur **Pixel** et Motorola sous Android 17. Suggestion de passer à l'accès ADB quand l'automatisation par accessibilité échoue en boucle.
  - v2.1.0 : Deduplicator détecte les doublons HEIC.
  - Source : [Releases](https://github.com/d4rken-org/sdmaid-se/releases)
- Gratuit et payant, d'après la FAQ officielle :
  - Tous les outils **scannent gratuitement**.
  - CorpseFinder, SystemCleaner, AppControl et Analyzer (usage de base) sont gratuits.
  - L'upgrade est requise pour : la suppression dans AppCleaner et Deduplicator, Media Squeeze, le graphique Storage Trend, l'alerte d'espace faible, la création de filtres, l'activation du Scheduler et l'usage illimité de Swiper.
  - L'upgrade s'achète sur Google Play (achat unique ou abonnement), ou via GitHub Sponsors pour la build FOSS.
  - Les achats SD Maid 1 Pro ne sont pas transférés.
  - Source : [docs/faq.md](https://github.com/d4rken-org/sdmaid-se/blob/main/docs/faq.md)
- Limite gratuite de Swiper : 2 sessions de 50 fichiers. — [docs/swiper.md](https://github.com/d4rken-org/sdmaid-se/blob/main/docs/swiper.md)
- Prix : environ 3 $/an ou 6 $ à vie selon un article Android Police de 2026 — [Android Police](https://www.androidpolice.com/storage-cleaner-invisible-app-junk/) (via résumé de recherche). Environ 5 € à vie ou 2 €/an selon le développeur sur XDA — [XDA thread](https://xdaforums.com/t/app-8-0-sd-maid-2-se-system-cleaner.4568645/) (via résumé de recherche). Licence permanente à 6,49 € et note Play de 4,4 sur environ 8 000 avis (avril 2026) selon un guide allemand — [giga.de](https://www.giga.de/tech/nutzlose-daten-von-android-smartphones-und-tablets-loeschen-so-funktioniert-sd-maid-se--01KP2T5JPFFD3X52Q7K5YVMR2T) (via résumé de recherche, **les montants divergent selon la source et la région**).
- Sur Play, le prix est fixe. GitHub Sponsors accepte n'importe quel montant à partir d'environ 1 $. Des builds sans Google existent sur GitHub, F-Droid et IzzyOnDroid. — [Discussion #1174](https://github.com/d4rken-org/sdmaid-se/discussions/1174)
- Accès utilisés, d'après la doc Setup (toutes les permissions sont optionnelles) :
  - `MANAGE_EXTERNAL_STORAGE` : tout le stockage public, mais plus `Android/data` à partir d'Android 13.
  - SAF pour `Android/data` et `obb` : bloqué même sur Android 11/12 si l'appli Fichiers système (documentsui) est une mise à jour ciblant l'API 34+.
  - Usage statistics : tailles des données et du cache par appli, état d'exécution, dernière utilisation.
  - App inventory (`QUERY_ALL_PACKAGES`) : sur certains appareils Android 14+, cette permission n'est pas accordée par défaut ou peut être révoquée.
  - Service d'accessibilité.
  - Root.
  - ADB via Porter (recommandé, prioritaire) ou Shizuku.
  - Source : [docs/setup.md](https://github.com/d4rken-org/sdmaid-se/blob/main/docs/setup.md)
- En 2023, SD Maid avait été suspendu du Play Store au titre de la politique « Stalkerware ». Le développeur contestait. — [Android Police](https://www.androidpolice.com/sd-maid-terminated-play-store-stalkerware-policy/) (via résumé de recherche, article d'environ 3 ans). La fiche Play affichait une mise à jour au 25/10/2026, ce qui suggère un retour sur le store (via résumé de recherche, non vérifié directement).

**SD Maid « legacy » (SD Maid 1, eu.thedarken.sdm)**
- Gelé : « will keep working as it is, but won't get new features or improvements », correctifs de sécurité seulement. Dépôt **archivé le 23/07/2026** (lecture seule). — [GitHub sdmaid](https://github.com/d4rken-org/sdmaid)
- La version 5.6.2 a désactivé le filtre corpse `/data/app` sur Android 14+ pour cause de faux positifs, et renvoie vers SD Maid 2/SE à partir d'Android 13. — [CHANGELOG](https://github.com/d4rken-org/sdmaid/blob/master/CHANGELOG.md) (via résumé de recherche)
- D'après la FAQ SE, SD Maid 1 utilisait une API système cachée de vidage de cache que Google a supprimée dans les versions récentes d'Android. — [docs/faq.md](https://github.com/d4rken-org/sdmaid-se/blob/main/docs/faq.md)

**Files by Google (Google)**
- Gratuit. Onglet « Clean » avec plusieurs cartes :
  - fichiers indésirables (fichiers temporaires d'applis, supprimés définitivement) — [Aide Files](https://support.google.com/files/answer/9713869?hl=en) (via résumé de recherche) ;
  - doublons, avec un badge « Original » sur le fichier conservé et envoi des copies à la Corbeille — [Aide Files](https://support.google.com/files/answer/9764075?hl=en) (via résumé de recherche) ;
  - selon Android Police (juillet 2026) : anciennes captures d'écran, memes, fichiers téléchargés et applis inutilisées — [Android Police](https://www.androidpolice.com/found-10gb-wasted-storage-on-my-phone-with-this-free-google-app/) (via résumé de recherche).
- La détection de doublons est **octet pour octet uniquement**, sans quasi-doublons. — [byteswifts 2026](https://www.byteswifts.com/2026/08/free-up-android-storage-files-by-google-2026.html) (via résumé de recherche, source secondaire)
- La fonction Smart Storage est décrite en section 5.

**CCleaner pour Android (Piriform, groupe Gen Digital)**
- Lancé sur Android en 2014. — [Wikipedia CCleaner](https://en.wikipedia.org/wiki/CCleaner) (via résumé de recherche)
- La version gratuite couvre le nettoyage des fichiers indésirables, l'analyseur de stockage et le gestionnaire d'applis. Pro retire les publicités et active le nettoyage programmé en arrière-plan, sans outil de nettoyage supplémentaire. — [Cybernews 2026](https://cybernews.com/privacy-tools/ccleaner-review/) (via résumé de recherche)
- Une autre source cite un « Advanced Photo Optimizer » (photos similaires, anciennes ou de mauvaise qualité) parmi les fonctions Pro. — [thehightechsociety](https://thehightechsociety.com/ccleaner-for-android-review/) (via résumé de recherche, source faible)
- Aucun prix Android officiel n'est publié. Les revendeurs vont d'environ 8,99 $ en promotion à environ 26,99 $/an pour un appareil. — [Cybernews](https://cybernews.com/privacy-tools/ccleaner-review/), [Walmart](https://www.walmart.com/ip/CCleaner-Pro-for-Android-1-Year-1-Device-Global/970041998) (via résumé de recherche)
- Version 26.08.0 listée sur TechSpot. — [TechSpot](https://www.techspot.com/downloads/6581-ccleaner-for-android.html) (via résumé de recherche)
- En 2014, Piriform avait ajouté un task killer à l'appli Android, ce qu'Android Police avait critiqué. — [Android Police 2014](https://www.androidpolice.com/2014/05/21/piriform-just-ruined-ccleaner-for-android-added-in-the-dreaded-task-killer-feature/)

**Avast Cleanup pour Android (Avast, groupe Gen Digital)**
- Fonctions mises en avant sur la page officielle : « hibernation » des applis, nettoyage automatique, « Detox photo library ». L'appli Android est gratuite sur Play, et les fonctions premium comme « Deep Clean » sont payantes. — [avast.com/cleanup-android](https://www.avast.com/cleanup-android) (via résumé de recherche)
- Prix affiché : 34,99 $ la première année pour 10 appareils, puis 79,99 $/an au renouvellement. — [avast.com/cleanup-android](https://www.avast.com/cleanup-android) (via résumé de recherche, sans doute l'offre Cleanup Premium multi-appareils)

**AVG Cleaner (AVG, groupe Avast/Gen Digital)**
- Gratuit avec publicités et achats intégrés. PRO retire les publicités et ajoute des profils batterie selon le lieu (Maison, Bureau, Voiture) et le nettoyage automatique programmé. Essai PRO de 30 jours. Prix US historiques : 0,99 $/mois ou 4,99 $/an. — [PR Newswire (communiqué non daté, ancien)](https://prnewswire.co.uk/news-releases/avg-cleaner-for-android-update-tackles-performance-draining-mobile-apps-266542181.html), [AVG Help](https://help.avg.com/en/install/avg-cleaner-pro-android?os=android) (via résumé de recherche, **données probablement antérieures à 2024**)

**Norton Clean (Gen Digital / Norton Labs)**
- Le 30/03/2020, l'appli est passée du compte Play « NortonMobile » au compte « Norton Labs ». Norton prévient qu'il pourrait « deprecate the current functionality ». — [Norton Support](https://support.norton.com/sp/en/us/home/current/solutions/v135839927) (via résumé de recherche). Statut actuel sur Play non confirmé.

**Storage Analyzer & Disk Usage (Mobile Infographics Tools / Anton Patapovich, com.mobile_infographics_tools.mydrive)**
- Gratuit avec publicités, environ 14,9 M d'installations, sorti en janvier 2013. Une version PRO existe (`mydrive_ext`). — [AppGoblin](https://appgoblin.info/apps/com.mobile_infographics_tools.mydrive) (via résumé de recherche)
- Dernière version trouvée : 4.1.7.43, datée du 10/03/2024. — [APKMirror](https://www.apkmirror.com/?p=6304769) (via résumé de recherche). **Maintenance incertaine, aucune donnée de 2025-2026 trouvée.**

**DiskUsage (Ivan Volosyuk, GPL-2.0)**
- Diagramme où les répertoires sont proportionnels à leur taille, avec zoom (de type treemap). Export de code.google.com, capture d'écran d'une « ancient version », aucune release. Le README renvoie au fork WhiredPlanck. — [GitHub IvanVolosyuk/diskusage](https://github.com/IvanVolosyuk/diskusage)
- Le fork [WhiredPlanck/diskusage](https://github.com/WhiredPlanck/diskusage) n'a pas de release visible et 101 étoiles. Aucune mention du scoped storage. **Projet de fait abandonné.**

**Disky (newhinton, GPL-3.0)**
- « Find your biggest diskspace thieves! » : analyseur de système de fichiers rapide, multi-stockage, Material You. Disponible sur F-Droid (`de.felixnuesse.disky`). — [GitHub newhinton/disky](https://github.com/newhinton/disky)

**App Manager (Muntashir Akon, GPLv3+)**
- « Clear app data or app cache » est réservé au mode root/ADB. Les infos de stockage par appli demandent l'accès « Usage Access ». — [GitHub AppManager](https://github.com/MuntashirAkon/AppManager)
- Versions récentes :
  - v4.1.1 (2026-09-04) et v4.1.0 (2026-06-29) — [releases.atom](https://github.com/MuntashirAkon/AppManager/releases.atom) ;
  - v4.0.5 (2025-07-28) a ouvert « Clear data from uninstalled apps » aux utilisateurs sans root — [Releases](https://github.com/MuntashirAkon/AppManager/releases).
- Fonction « 1-Click Ops → Trim caches in all apps » :
  - affiche « Only works in root or ADB mode » ;
  - bug ouvert depuis mars 2025 (le message s'affiche même en mode ADB), libellé « Investigation Needed », titre modifié le 29/05/2026 — [Issue #1556](https://github.com/MuntashirAkon/AppManager/issues/1556) ;
  - le nombre d'octets libérés n'est pas affiché (demande « 1-Click Clean App Cache Amount Freed » rejetée en 2022) — [Issues](https://github.com/MuntashirAkon/AppManager/issues?q=is%3Aissue%20trim%20cache).

**Gestionnaires de fichiers avec analyseur**
- **Cx File Explorer** : analyseur de gros fichiers, doublons, tailles d'applis et données en cache, corbeille, réputé sans publicité. v2.8.1 datée du 01/10/2026 (Uptodown), fiche Play mise à jour le 03/10/2026. — [Uptodown](https://cx-file-explorer.en.uptodown.com/android), [cxfilexplorer.online](https://cxfilexplorer.online/cx-file-explorer-features/) (via résumé de recherche, **sites tiers non officiels**)
- **Solid Explorer (NeatBytes)** : analyseur de stockage (doublons, gros fichiers). La version 3.4.10 corrige l'accès à Android/data via Shizuku. Payante. — [APKMirror](https://www.apkmirror.com/apk/neatbytes/solid-explorer-file-manager/solid-explorer-file-manager-3-4-10-release/), [XDA PSA](https://xdaforums.com/t/psa-what-free-android-file-managers-are-integrated-with-shizuku-given-that-unrootable-phones-still-need-read-write-access-to-protected-storage.4787917/) (via résumé de recherche). Prix actuel non trouvé.
- **MiXplorer (Hootan Parsa)** : la version 6.65.4 (environ mi-2024) a ajouté l'accès à `/Android/data` via Shizuku. Aucune fonction « storage analyzer » identifiée. — [APKMirror](https://www.apkmirror.com/apk/hootan-parsa/mixplorer-hootanparsa/mixplorer-hootanparsa-6-65-4-release/mixplorer-6-65-4-android-apk-download/), [XDA FAQ](https://xdaforums.com/t/mixplorer-q-a-and-faq-user-manual.3308582/) (via résumé de recherche)
- **Material Files (zhanghai, GPL-3.0, 9,3k étoiles)** : accès root, archives, NAS. **Ni analyseur ni taille de dossier, aucune mention de Shizuku.** — [GitHub MaterialFiles](https://github.com/zhanghai/MaterialFiles)
- **Fossify File Manager (GPL-3.0, 1,8k étoiles)** : « built-in storage analysis tool » pour « Identify and clean up space-hogging files », plus accès root. — [GitHub FossifyOrg/File-Manager](https://github.com/FossifyOrg/File-Manager)

**Détecteurs de doublons et photos similaires**
- Remo Duplicate Photos Remover : gratuit, ne peut pas tout supprimer d'un coup, pas d'IA. Duplicate Photos Fixer (Systweak) : photos visuellement similaires, avec des faux positifs à vérifier. Slidebox : tri par balayage vers des albums, pas de dédoublonnage. Files by Google : doublons exacts uniquement. — [BestForAndroid (mis à jour mai 2026)](https://bestforandroid.com/remove-duplicate-photos-on-android/), [bestforandroid duplicate-photo-finder](https://bestforandroid.com/duplicate-photo-finder/) (via résumé de recherche)

**Shizuku et forks (prérequis de la plupart des contournements sans root)**
- Shizuku (RikkaApps) : dernière release v13.6.0 (2025-05-25, « Support Android16 QPR1 »), précédente v13.5.4 (2024-03-10). 601 issues ouvertes. — [Shizuku releases.atom](https://github.com/RikkaApps/Shizuku/releases.atom), [Releases](https://github.com/RikkaApps/Shizuku/releases)
- **Porter** (d4rken-org, Apache-2.0) : « ADB access for your apps. A minimal, maintained Shizuku fork. » Il est né parce que Shizuku « was no longer actively maintained ». Il est compatible avec l'API Shizuku grâce à un compagnon « Porter Compatibility ». Le service s'arrête au redémarrage. Il démarre par débogage sans fil sur Android 11+. — [GitHub porter](https://github.com/d4rken-org/porter)
- Fork thedjchi/Shizuku : démarrage au boot plus robuste, watchdog, mais « I'm pausing maintenance for the time being ». — [GitHub thedjchi/Shizuku](https://github.com/thedjchi/Shizuku)

### Inferences
- Pour un outil personnel sur Pixel 8 Pro, **SD Maid SE est le benchmark fonctionnel à égaler ou dépasser**. Il est open source (GPLv3), on peut donc étudier ses techniques : filtres, base « clutter », automatisation par accessibilité, appel `trimCaches` via ADB. Toute réutilisation de code impose la GPLv3, ce qui est sans conséquence pour un usage personnel non distribué.
- Les cleaners de Gen Digital (CCleaner, Avast, AVG, Norton) sont des produits marketing centrés sur l'abonnement et l'« hibernation » ou le « boost », et apportent peu de capacités techniques que SD Maid SE n'a pas. Leurs fiches et prix sont mal documentés publiquement.
- L'écosystème ADB sans root est fragile. Shizuku n'a pas eu de release depuis 17 mois, le fork thedjchi est en pause et Porter est jeune et porté par un seul développeur. Une appli maison qui dépend d'un accès ADB devrait supporter l'API Shizuku, compatible avec Porter et les forks, plutôt qu'une implémentation spécifique.
- Côté analyseurs purs, DiskUsage est abandonné et Storage Analyzer & Disk Usage est probablement en roue libre. Disky et Fossify sont les seules options open source maintenues repérées, et aucune ne semble gérer Android/data via Shizuku. C'est un créneau à combler.

### Gaps
- Fiches Play (prix exacts en €, date de mise à jour, note, Data Safety) de CCleaner, Avast Cleanup, AVG Cleaner, Norton Clean, Cx, Solid Explorer, Files by Google : play.google.com inaccessible depuis la session.
- **Phone Master (Transsion) et autres « boosters » préinstallés** : aucune donnée collectée, quota de recherche épuisé.
- **Cleaners dédiés WhatsApp/Telegram tiers** (par ex. « Cleaner for WhatsApp ») : aucune donnée collectée.
- Développeur exact et modèle de monétisation de Cx File Explorer : non confirmés par une source officielle.
- Exigences d'accès (accessibilité, usage stats) de CCleaner, Avast et AVG sur Android 14+ : non vérifiées.
- Analyzer/Treemap en dehors de DiskUsage (par ex. « Analyze » de Solid, treemap de X-plore) : non couverts.

## 2. Inventaire fonctionnel : ce qui est couvert, gratuit ou payant

### Takeaway
Les fonctions de base sont largement couvertes et souvent gratuites : répartition par catégorie, taille par appli, gros fichiers, doublons exacts, dossiers vides, APK superflues, restes d'applis désinstallées, médias WhatsApp. Elles sont payantes ou absentes pour : la suppression des doublons et photos similaires, le vidage en masse des caches, la planification, la compression de médias et l'**historique du stockage dans le temps**, que seul SD Maid SE propose parmi les applis identifiées, avec une partie payante.

### Cited Findings

**Répartition du stockage et taille par appli**
- Catégories de StorageAnalyzer : Apps (code, données, caches, médias), fichiers utilisateur, données système (le reste), autres utilisateurs / profil pro. Par appli : App code, App data (données, réglages, caches), App media (par ex. photos WhatsApp reçues), fichiers hors dossiers de l'appli. — [docs/storageanalyzer.md](https://github.com/d4rken-org/sdmaid-se/blob/main/docs/storageanalyzer.md)
- Sans App inventory et Usage statistics, l'espace des applis apparaît comme « données système ». Sans root, le code et les données privées des applis ne sont visibles qu'en totaux, sans navigation. Les dossiers de plus de 100 000 éléments sont seulement totalisés. Android/data demande root ou ADB à partir d'Android 13. — [docs/storageanalyzer.md](https://github.com/d4rken-org/sdmaid-se/blob/main/docs/storageanalyzer.md)
- Android 15 ajoute `StorageStats.getAppBytesByDataType()`, qui détaille l'espace d'une appli : splits APK, code AOT et d'accélération, métadonnées dex, bibliothèques, profils guidés. — [Android 15 features](https://developer.android.com/about/versions/15/features)
- App Manager : informations de stockage par appli avec « Usage Access ». — [GitHub AppManager](https://github.com/MuntashirAkon/AppManager)

**Historique et tendance du stockage**
- SD Maid SE Storage Trend :
  - relevé de l'espace libre toutes les quelques heures et après chaque nettoyage, avec des marqueurs de nettoyage indiquant l'espace libéré ;
  - variation sur 7 jours et estimation des « jours restants » (**gratuit**) ;
  - rétention de 90 jours par défaut, 365 au maximum ;
  - le graphique et l'alerte d'espace faible sont **payants**.
  - Source : [docs/storageanalyzer.md](https://github.com/d4rken-org/sdmaid-se/blob/main/docs/storageanalyzer.md)
- La v2.2.1 porte l'historique à 1 an. — [Releases](https://github.com/d4rken-org/sdmaid-se/releases)

**Restes d'applis désinstallées (« corpses »)**
- CorpseFinder couvre :
  - le stockage public, `Android/media`, `Android/data`, `Android/obb` ;
  - `/data/data`, `/data/dalvik-cache`, les `.so` natives, les APK de `/data/app` et `/system/app`, `/data/app-private`.
- La propriété est déterminée par les noms de package. Pour les emplacements sans schéma de nommage, comme la racine du stockage public, une base maintenue manuellement est utilisée. Les options à risque sont désactivées par défaut.
- Source : [docs/corpsefinder.md](https://github.com/d4rken-org/sdmaid-se/blob/main/docs/corpsefinder.md)
- CorpseFinder est gratuit, suppression comprise. — [docs/faq.md](https://github.com/d4rken-org/sdmaid-se/blob/main/docs/faq.md)
- App Manager v4.0.5 : « Clear data from uninstalled apps » est disponible sans root. — [Releases AppManager](https://github.com/MuntashirAkon/AppManager/releases)

**Fichiers parasites système (SystemCleaner, gratuit)**
- Filtres généraux : dossiers vides, captures d'écran, fichiers à la corbeille, **APK superflues**, LOST.DIR, fichiers Linux/Mac/Windows, miniatures, fichiers temporaires, fichiers analytics, fichiers publicitaires, journaux.
- Filtres spécifiques : résidus de crash, usage stats, ANR, temporaires locaux, datalogger, cache de téléchargement, log dropbox, cache d'installation, tâches récentes.
- Les zones PRIVATE_DATA et DOWNLOAD_CACHE des filtres personnalisés demandent root.
- Source : [docs/systemcleaner.md](https://github.com/d4rken-org/sdmaid-se/blob/main/docs/systemcleaner.md)
- La création de filtres personnalisés est payante. — [docs/faq.md](https://github.com/d4rken-org/sdmaid-se/blob/main/docs/faq.md)

**Caches d'applis (AppCleaner)**
- Cible les fichiers « expendable » (surtout les caches). Filtres communs et filtres propres à certaines applis (WhatsApp, Telegram). « Include inaccessible caches » vise le cache privé par défaut (`/data/data/<pkg>/cache`). Les applis système sont exclues par défaut, car certaines réagissent mal ou n'ont pas de bouton « Vider le cache ». — [docs/appcleaner.md](https://github.com/d4rken-org/sdmaid-se/blob/main/docs/appcleaner.md)
- La suppression dans AppCleaner est payante. — [docs/faq.md](https://github.com/d4rken-org/sdmaid-se/blob/main/docs/faq.md)
- Correctifs 2026 : miniatures OnePlus Gallery, dossiers cachés `.media_cache`, fichiers temporaires WhatsApp. — [Releases](https://github.com/d4rken-org/sdmaid-se/releases)

**Doublons, photos similaires, médias**
- Deduplicator utilise trois méthodes :
  - checksum (doublons exacts, activé par défaut) ;
  - hash perceptuel pour les images similaires (désactivé par défaut) ;
  - empreinte média pour l'audio et la vidéo similaires (désactivée par défaut).
- Taille minimale de 512 Ko, analyse de `Android/data` et `Android/media` (root ou ADB requis sur Android 13+), `Android/obb` exclu.
- Règles de conservation ordonnées : type, emplacement préféré, indexation média, interne avant SD, profondeur, date, taille.
- **Scan gratuit, suppression payante.** Le Scheduler ne peut pas lancer le Deduplicator.
- Source : [docs/deduplicator.md](https://github.com/d4rken-org/sdmaid-se/blob/main/docs/deduplicator.md)
- Files by Google ne trouve que les doublons exacts. — [byteswifts 2026](https://www.byteswifts.com/2026/08/free-up-android-storage-files-by-google-2026.html) (via résumé de recherche)
- Duplicate Photos Fixer trouve les photos similaires, avec des faux positifs. — [BestForAndroid](https://bestforandroid.com/remove-duplicate-photos-on-android/) (via résumé de recherche)
- Media Squeeze (payant) recompresse JPEG et WebP (par défaut), HEIC (optionnel, Android 9+) et MP4 (optionnel, demande environ 110 % de la taille en espace libre) :
  - qualité 80 par défaut, ne traite que les fichiers de plus de 90 jours et de plus de 512 Ko ;
  - ignore par défaut les photos HDR/profondeur et les Motion Photos ;
  - conserve l'EXIF mais pas le XMP ;
  - remplacement irréversible de l'original.
  - Source : [docs/mediasqueeze.md](https://github.com/d4rken-org/sdmaid-se/blob/main/docs/mediasqueeze.md)
- Swiper : tri garder/supprimer par balayage. **La suppression est définitive, sans corbeille.** — [docs/swiper.md](https://github.com/d4rken-org/sdmaid-se/blob/main/docs/swiper.md)

**Gros fichiers et téléchargements**
- Files by Google : cartes pour fichiers téléchargés, anciennes captures et memes. — [Android Police 2026](https://www.androidpolice.com/found-10gb-wasted-storage-on-my-phone-with-this-free-google-app/) (via résumé de recherche)
- Cx File Explorer et Solid Explorer : recherche de gros fichiers et de doublons. — [cxfilexplorer.online](https://cxfilexplorer.online/cx-file-explorer-features/), [Solid Explorer Play](https://play.google.com/store/apps/details?id=pl.solidexplorer2&hl=en&gl=US) (via résumé de recherche)

**Médias des messageries**
- Outil natif WhatsApp (Paramètres > Stockage et données > Gérer le stockage), introduit en 2020 :
  - catégories « Transféré de nombreuses fois » et « Plus de 5 Mo » ;
  - vue par discussion, tri par taille, suppression en masse ;
  - option « supprimer l'élément et toutes ses copies ».
  - Source : [MacRumors 2020](https://www.macrumors.com/2020/11/03/whatsapp-storage-management-feature/), [Android Central](https://androidcentral.com/new-whatsapp-feature-will-help-you-manage-your-storage-better) (via résumé de recherche, **source de 2020, antérieure à 2024**)
- SD Maid AppCleaner a des filtres spécifiques WhatsApp et Telegram. — [docs/appcleaner.md](https://github.com/d4rken-org/sdmaid-se/blob/main/docs/appcleaner.md)

**Planification et automatisation**
- SD Maid Scheduler : AppCleaner, CorpseFinder et SystemCleaner à intervalle (par ex. tous les 3 jours) et heure approximative. Options « skip on low battery » et « skip when not charging », commandes shell après exécution. **Les opérations par accessibilité ne fonctionnent pas en tâche planifiée** (écran verrouillé). Shizuku (applis utilisateur seulement) ou root (applis utilisateur et système) les remplacent. — [docs/scheduler.md](https://github.com/d4rken-org/sdmaid-se/blob/main/docs/scheduler.md)
- L'activation du Scheduler est payante. — [docs/faq.md](https://github.com/d4rken-org/sdmaid-se/blob/main/docs/faq.md)
- CCleaner Pro : nettoyage programmé en arrière-plan. — [Cybernews](https://cybernews.com/privacy-tools/ccleaner-review/) (via résumé de recherche)
- AVG Cleaner PRO : nettoyage automatique programmé. — [PR Newswire](https://prnewswire.co.uk/news-releases/avg-cleaner-for-android-update-tackles-performance-draining-mobile-apps-266542181.html) (via résumé de recherche)
- Avast Cleanup : nettoyage automatique. — [avast.com](https://www.avast.com/cleanup-android) (via résumé de recherche)

**Gestion des applis (AppControl et assimilés)**
- AppControl :
  - liste avec taille et activité récente (optionnelles, car lentes) ;
  - Force stop (root ou ADB de préférence, sinon accessibilité) ;
  - export d'APK (root pour les applis chiffrées) ;
  - Freeze, c'est-à-dire désactiver l'appli (root ou ADB) ;
  - désinstallation, multi-sélection.
  - Source : [docs/appcontrol.md](https://github.com/d4rken-org/sdmaid-se/blob/main/docs/appcontrol.md)

**Corbeille**
- Files by Google envoie les doublons à la Corbeille. Selon un guide, les fichiers peuvent y rester jusqu'à 60 jours. — [Aide Files](https://support.google.com/files/answer/9764075?hl=en), [résumé de recherche](https://www.byteswifts.com/2026/08/free-up-android-storage-files-by-google-2026.html) (**durée non confirmée par Google**)
- Cx File Explorer propose une corbeille. — [cxfilexplorer.online](https://cxfilexplorer.online/cx-file-explorer-features/) (via résumé de recherche)
- Chez SD Maid SE, Swiper et Media Squeeze suppriment ou remplacent définitivement. — [docs/swiper.md](https://github.com/d4rken-org/sdmaid-se/blob/main/docs/swiper.md), [docs/mediasqueeze.md](https://github.com/d4rken-org/sdmaid-se/blob/main/docs/mediasqueeze.md)

**Archivage d'applis**
- Android 15 intègre l'archivage et le désarchivage au niveau de l'OS. `PackageInstaller.requestArchive` (permission `REQUEST_DELETE_PACKAGES`) « removes the APK and any cached files, but persists user data ». L'installeur responsable reçoit la demande de désarchivage. — [Android 15 features](https://developer.android.com/about/versions/15/features)
- Le Play Store propose un archivage automatique sur option, déclenché quand l'espace manque pour un téléchargement, avec des exclusions par appli. Le désarchivage se fait depuis Infos de l'appli ou depuis Play, à condition que le store d'origine soit présent et l'appli encore disponible. — [Aide Android](https://support.google.com/android/answer/15523443?hl=en), [GSMArena](https://m.gsmarena.com/androids_autoarchive_feature_for_unused_apps_is_now_rolling_out-news-58208.php) (via résumé de recherche)

**Déduplication avec le cloud (Google Photos)**
- Google Photos > menu > « Libérer de l'espace » supprime les copies locales déjà sauvegardées. Smart Storage le fait automatiquement (voir section 5). — [Android Authority](https://www.androidauthority.com/control-clean-google-photos-storage-3525116/), [Aide Files Smart Storage](https://support.google.com/files/answer/10862356?hl=en-GB) (via résumé de recherche)

### Inferences
- **Matrice de couverture** (synthèse des constats ci-dessus ; G = gratuit, P = payant, — = absent ou non documenté) :

  | Fonction | SD Maid SE | Files by Google | CCleaner / Avast / AVG | App Manager | Analyseurs (Disky, DiskUsage, Storage Analyzer) |
  |---|---|---|---|---|---|
  | Répartition par catégorie | G | G (plus réglages Pixel) | G | — | G |
  | Taille par appli (code / données / cache) | G (Usage access) | partielle | G | G (Usage access) | — |
  | Gros fichiers | G (Analyzer) | G | G | — | G (cœur de métier) |
  | Doublons exacts | scan G / suppression P | G | ? | — | — |
  | Photos similaires | scan G / suppression P (phash) | — | P (Photo Optimizer, Detox) | — | — |
  | Dossiers vides, APK superflues, journaux, miniatures | G (SystemCleaner) | partiel (téléchargements, captures) | ? | — | — |
  | Restes d'applis désinstallées | G (CorpseFinder) | — | ? | G (données d'applis désinstallées) | — |
  | Vider le cache de toutes les applis | P (accessibilité ou ADB) | fichiers indésirables seulement | P / « Deep Clean » | root/ADB (bug ouvert) | — |
  | Planification | P | Smart Storage (photos seulement) | P | — | — |
  | Historique du stockage | 7 j G, graphique P | — | — | — | — |
  | Compression de médias | P | — | — | — | — |
  | Corbeille | non (suppression définitive) | oui | ? | — | — |
  | Archivage d'applis | non documenté | via Play / OS | — | ? | — |

- L'historique du stockage dans le temps, avec projection de saturation, est un **trou quasi complet du marché** : seul SD Maid SE l'a, et son graphique est payant. Pour une appli de monitoring personnelle c'est différenciant et simple à faire : relevés périodiques de `getFreeBytes`/`StorageStatsManager`, stockés localement.
- La corbeille est rare chez les nettoyeurs « puissants ». Une appli maison gagnerait à proposer une quarantaine ou corbeille avant suppression définitive.

### Gaps
- Détail exact des fonctions Android de CCleaner, Avast Cleanup et AVG Cleaner en 2026 (doublons, corps d'applis, accès Android/data) : pages officielles inaccessibles, sources secondaires faibles.
- Durée réelle de rétention de la corbeille de Files by Google (30 ou 60 jours ?) : non confirmée.
- Fonctions de gestion du stockage de Telegram (cache, durée de conservation des médias, limite de cache) : aucune donnée collectée (quota de recherche épuisé).
- Autres applis avec historique ou tendance du stockage : recherche bloquée par le quota.

## 3. Ce que ces applis ne peuvent pas faire, ou font mal, sur Android 14+ (et ce que change Shizuku/ADB ou root)

### Takeaway
Sans privilège, une appli ne peut ni vider le cache privé des autres applis, ni lire ou écrire `Android/data` et `Android/obb` (Android 13+), ni tuer les processus des autres applis (Android 14+). Les contournements sont tous imparfaits :
- **automatisation par accessibilité** de l'écran « Vider le cache » de chaque appli : lente, fragile selon la ROM, impossible écran verrouillé, bloquée par Advanced Protection sous Android 17 ;
- **ADB via Shizuku/Porter** : `trimCaches` global qui ignore les exclusions, et accès Android/data instable sur Android 16 depuis une mise à jour Play System ;
- **root**, qui lève la plupart des limites.

### Cited Findings

**Android/data, Android/obb et le stockage partagé**
- `MANAGE_EXTERNAL_STORAGE` donne lecture et écriture sur tout le stockage partagé, la table `MediaStore.Files`, et la racine des clés OTG et cartes SD. Il **exclut** `/Android/data/`, `/sdcard/Android` et la plupart de ses sous-dossiers, ainsi que les répertoires spécifiques des autres applis.
- Sur Google Play, il est réservé à certains usages : gestionnaires de fichiers, sauvegarde, antivirus, gestion de documents, recherche, chiffrement, migration.
- Test possible en ADB : `adb shell appops set --uid PKG MANAGE_EXTERNAL_STORAGE allow`.
- Source : [Manage all files](https://developer.android.com/training/data-storage/manage-all-files)
- Pour les applis ciblant Android 11+, `ACTION_OPEN_DOCUMENT_TREE` ne peut plus accorder la racine du stockage interne, les racines de carte SD « fiables » ni `Download`. `ACTION_OPEN_DOCUMENT(_TREE)` ne peut plus sélectionner dans `Android/data/` ni `Android/obb/`. « On Android 11, apps can no longer access files in any other app's dedicated, app-specific directory within external storage. » — [Android 11 storage](https://developer.android.com/about/versions/11/privacy/storage)
- Android 13 a fermé la faille SAF qui permettait aux gestionnaires de fichiers d'accorder des sous-dossiers de `/Android`. — [Esper](https://www.esper.io/blog/android-dessert-bites-28-file-manager-loophole-closed-73891524) (via résumé de recherche)
- SD Maid confirme : l'accès SAF `Android/data|obb` est bloqué à partir d'Android 13, et déjà sur Android 11/12 si documentsui est mis à jour avec une cible API 34+. Désinstaller les mises à jour de l'appli Fichiers rétablit la possibilité d'accorder l'accès. — [docs/setup.md](https://github.com/d4rken-org/sdmaid-se/blob/main/docs/setup.md)
- Contournement par Shizuku : l'utilisateur shell (ADB) garde l'accès à `/Android`. MiXplorer (v6.65.4), Solid Explorer (v3.4.10) et FV File Explorer l'utilisent. FV annonce Shizuku v13.5.3+ requis sur Android 14+, et un fonctionnement d'Android 11 à 16. — [FV File Explorer](https://folderv.com/2023/11/24/access-Android-data-and-Android-obb-on-Android-14/), [XDA MiXplorer FAQ](https://xdaforums.com/t/mixplorer-q-a-and-faq-user-manual.3308582/) (via résumé de recherche)
- Shizuku **n'ouvre pas** `/data/data/<autre appli>/`. — [XDA PSA](https://xdaforums.com/t/psa-what-free-android-file-managers-are-integrated-with-shizuku-given-that-unrootable-phones-still-need-read-write-access-to-protected-storage.4787917/) (via résumé de recherche)
- **Régression sur Android 16** (problème non résolu) :
  - Issue Shizuku #1574 (ouverte le 31/10/2025, Galaxy Z Fold 7, Android 16) : FV, MiXplorer, MT Manager, SD Maid SE, Solid, Total Commander, X-plore et ZArchiver n'accèdent plus à `Android/data`. Aucune réponse des mainteneurs.
  - Contournement partiel signalé : ZArchiver 1.0.10 avec Shizuku 13.6.0 (juin et septembre 2026), avec erreurs de lecture (ERR 13) chez certains.
  - Source : [Shizuku #1574](https://github.com/RikkaApps/Shizuku/issues/1574)
  - Issue Shizuku #1807 (23/01/2026, **Pixel 9 Pro XL et Pixel Fold sous Android 16 QPR2**, non rootés) : après une mise à jour Google Play System, MiXplorer, MT Manager, SD Maid SE et Solid ne peuvent plus copier, déplacer, supprimer ni renommer dans `/android/data`, seulement lister. Cause non identifiée, toujours ouverte. — [Shizuku #1807](https://github.com/RikkaApps/Shizuku/issues/1807)
- SD Maid StorageAnalyzer et Deduplicator : `Android/data` demande root ou ADB à partir d'Android 13. — [docs/storageanalyzer.md](https://github.com/d4rken-org/sdmaid-se/blob/main/docs/storageanalyzer.md), [docs/deduplicator.md](https://github.com/d4rken-org/sdmaid-se/blob/main/docs/deduplicator.md)

**Vider le cache des autres applis sur Android 11+**
- API officielle : « Starting in Android 11, apps that use the scoped storage model can access only their own app-specific cache files. » Une appli peut lancer `ACTION_MANAGE_STORAGE` (l'utilisateur choisit quoi supprimer) ou `ACTION_CLEAR_APP_CACHE`, qui demande à l'utilisateur son accord pour vider les caches de **toutes** les applis. Avertissement de la doc : « can substantially affect device battery life and might remove a large number of files ». — [Android 11 storage](https://developer.android.com/about/versions/11/privacy/storage), [App-specific storage](https://developer.android.com/training/data-storage/app-specific)
- `getAllocatableBytes()` peut dépasser l'espace libre, car le système compte les fichiers qu'il pourrait supprimer des caches des autres applis. « When the device is low on internal storage space, Android may delete these cache files to recover space. » — [App-specific storage](https://developer.android.com/training/data-storage/app-specific)
- SD Maid, FAQ : « Modern Android has no API for apps to clear another app's cache directly ». SD Maid SE ouvre donc l'écran des paramètres de chaque appli par le service d'accessibilité et appuie sur « Vider le cache ». Shizuku/ADB atteint ce que l'accessibilité ne peut pas. — [docs/faq.md](https://github.com/d4rken-org/sdmaid-se/blob/main/docs/faq.md), [docs/appcleaner.md](https://github.com/d4rken-org/sdmaid-se/blob/main/docs/appcleaner.md)
- Avec Shizuku ou ADB, SD Maid appelle `PkgOps.trimCaches()` :
  - `targetPkgs=null`, demande de `9223372036854775807` octets (Long.MAX_VALUE), soit un trim **global** de l'appareil ;
  - le développeur reconnaît que « The trimCaches call can not be forced to honor any SD Maid based exclusions. It's a compromise. » ;
  - la PR #2744 (fusionnée le 01/09/2026) se contente d'avertir l'utilisateur. La seule portée possible de l'appel plateforme est un volume de stockage.
  - Sources : [Issue #2729](https://github.com/d4rken-org/sdmaid-se/issues/2729), [PR #2744](https://github.com/d4rken-org/sdmaid-se/pull/2744)
- La commande ADB équivalente, `pm trim-caches <taille>`, « tries to free up space on the target by deleting caches » jusqu'à une quantité désirée. — [Cake.AndroidAdb](https://cakebuild.net/api/Cake.AndroidAdb/PackageManagerAliases/62A9CF9E) (via résumé de recherche, wrapper tiers)
- App Manager : « Trim caches in all apps » réservé au root ou à l'ADB, bug d'activation en mode ADB ouvert. — [Issue #1556](https://github.com/MuntashirAkon/AppManager/issues/1556)
- Fragilité de l'accessibilité :
  - correctifs répétés en 2026 pour Pixel et Motorola sous Android 17, Motorola et Sony sous Android 16, MIUI, GrapheneOS, gros corps de police — [Releases](https://github.com/d4rken-org/sdmaid-se/releases) ;
  - issues de 2026 : boucle de l'accessibilité sous HyperOS Android 16 (#2892, ouverte le 25/10/2026), échec sur Samsung Android 16 en allemand (#2410), Samsung Android 14 (#2665) — [Issues](https://github.com/d4rken-org/sdmaid-se/issues?q=is%3Aissue%20android%2016) ;
  - la FAQ prévient que sur Android 16, l'accessibilité peut être bloquée et le nettoyage incomplet, et suggère Shizuku — [docs/faq.md](https://github.com/d4rken-org/sdmaid-se/blob/main/docs/faq.md) ;
  - le service s'arrête souvent après une mise à jour, un crash ou une optimisation batterie agressive. Un `WRITE_SECURE_SETTINGS` accordé par ADB permet de le relancer automatiquement — [docs/setup.md](https://github.com/d4rken-org/sdmaid-se/blob/main/docs/setup.md).
- Planification : l'accessibilité ne fonctionne pas écran verrouillé, donc pas de vidage de cache planifié sans Shizuku (applis utilisateur) ou root (toutes). — [docs/scheduler.md](https://github.com/d4rken-org/sdmaid-se/blob/main/docs/scheduler.md)

**Advanced Protection (Android 16/17) et applis installées hors store**
- Android Advanced Protection Mode, lancé avec Android 16, implique entre autres le « Blocked app sideloading ». — [Advanced Protection Mode](https://developer.android.com/privacy-and-security/advanced-protection-mode)
- Sous Android 17, Advanced Protection réserve l'AccessibilityService aux applis déclarées `isAccessibilityTool="true"`, révoque les accès déjà accordés et affiche « Restricted by Advanced Protection ». Changement apparu avec Android 17 Beta 2 (mars 2026). Les sources divergent sur la version exacte : 17 ou 17.2. — [The Hacker News (oct. 2026)](https://thehackernews.com/2026/10/android-17-advanced-protection-locks.html), [Android Authority](https://www.androidauthority.com/android-17-beta-2-advanced-protection-mode-accessibility-apps-3648860/), [Help Net Security](https://www.helpnetsecurity.com/2026/03/19/google-android-accessibility-api-restrictions/) (via résumé de recherche)
- Cas réel : Pixel 10 Pro sous Android 17 (21/06/2026), l'accessibilité de SD Maid ne peut pas être activée sous Advanced Protection. Le développeur répond que c'est voulu par Google et propose Shizuku/ADB ou root pour AppCleaner, ou de désactiver Advanced Protection. — [Issue #2475](https://github.com/d4rken-org/sdmaid-se/issues/2475)
- Sur Android 13+, une appli installée hors store qui veut l'accessibilité doit passer par « Autoriser les paramètres restreints » dans ses infos d'appli (OxygenOS 15 demande en plus un flag à l'installation). — [docs/setup.md](https://github.com/d4rken-org/sdmaid-se/blob/main/docs/setup.md)

**Processus et RAM**
- « Starting in Android 14, when your app calls killBackgroundProcesses(), the API can kill only the background processes of your own app. » — [Android 14 behavior changes](https://developer.android.com/about/versions/14/behavior-changes-all)

**Inventaire des applis**
- `QUERY_ALL_PACKAGES` n'est plus forcément accordé par défaut sur certains appareils Android 14+. Si la liste est incomplète, SD Maid désactive CorpseFinder, AppCleaner et AppControl par sécurité. — [docs/setup.md](https://github.com/d4rken-org/sdmaid-se/blob/main/docs/setup.md)
- Depuis Android 14, `OWNER_PACKAGE_NAME` de MediaStore est masqué sauf avec `QUERY_ALL_PACKAGES`. — [Android 14 behavior changes](https://developer.android.com/about/versions/14/behavior-changes-all)

**Fragilité de l'ADB sans root**
- Le service Porter s'arrête au redémarrage. — [GitHub porter](https://github.com/d4rken-org/porter)
- Le service Shizuku ne survit pas au redémarrage et, sur certains appareils, il est tué au passage Wi-Fi/données mobiles. — [XDA](https://xdaforums.com/t/access-android-data-and-android-obb-on-android-14-with-shizuku.4644152/) (via résumé de recherche)
- ADB reste moins puissant que root. — [docs/setup.md](https://github.com/d4rken-org/sdmaid-se/blob/main/docs/setup.md)

### Inferences
- **Pour notre appli sur Pixel 8 Pro (non rooté, Android 16/17)**, trois choix d'architecture :
  1. **Sans privilège** : `MANAGE_EXTERNAL_STORAGE` (le Play n'est pas un obstacle puisque l'appli est installée hors store), `PACKAGE_USAGE_STATS` pour les tailles par appli, et l'intent `ACTION_CLEAR_APP_CACHE`, qui vide tout en une fois avec consentement à chaque fois. On ne peut ni lire Android/data ni nettoyer sélectivement.
  2. **Accessibilité** : vidage sélectif par appli. Fragile, impossible en planifié, et incompatible avec Advanced Protection. Sans gravité si Advanced Protection reste désactivé, mais Advanced Protection **bloque aussi l'installation hors store**, donc l'activer est de toute façon incompatible avec notre appli.
  3. **Shizuku/Porter** : `trimCaches` global (non sélectif), lecture et écriture d'Android/data (instable sur Pixel Android 16 QPR2 d'après #1807), `pm`/`appops` pour s'auto-accorder des permissions. Il faut relancer le service à chaque redémarrage.
- Le root (Magisk, bootloader déverrouillé) lève presque tout : `/data/data`, `/data/app`, dalvik-cache, autres utilisateurs, vidage sélectif fiable. C'est l'option à garder en « mode avancé ».
- Le vidage de cache en masse n'a qu'une valeur ponctuelle (voir section 4). L'appli devrait plutôt **mesurer et alerter** (tailles de cache par appli via StorageStatsManager, tendance) que vider de façon systématique.

### Gaps
- Permission exacte requise par `ACTION_CLEAR_APP_CACHE` (je pense `MANAGE_EXTERNAL_STORAGE`, mais non vérifié : page de référence StorageManager tronquée) et comportement sur Android 16/17.
- Détail de l'appel système derrière `PkgOps.trimCaches` (`IPackageManager.freeStorage`/`freeStorageAndNotify` ?) : non confirmé par la PR ni par l'issue.
- Exigences précises de `StorageStatsManager.queryStatsForPackage/queryStatsForUid` (`PACKAGE_USAGE_STATS`) : page de référence non lisible (tronquée). Le besoin d'« Usage access » est confirmé seulement indirectement par [SD Maid setup](https://github.com/d4rken-org/sdmaid-se/blob/main/docs/setup.md) et [App Manager](https://github.com/MuntashirAkon/AppManager).
- Statut d'Android 17 sur Pixel 8 Pro et d'Advanced Protection en version stable : non vérifiés directement.
- Cause et éventuel correctif de la perte d'écriture dans Android/data via Shizuku après la mise à jour Play System (Pixel, Android 16 QPR2) : non résolus au 28/10/2026.

## 4. Fonctions inutiles ou nuisibles, et contexte réglementaire et Play Store

### Takeaway
Les « RAM boosters » et task killers sont inutiles, voire contre-productifs. Google l'écrit noir sur blanc : « It isn't possible for a 3rd-party application to improve the memory, power, or thermal behavior of an Android device ». Android 14 les a neutralisés techniquement. La catégorie des cleaners est aussi un vecteur historique d'adware et de malware. Le leader commercial (Gen Digital : Avast, AVG, CCleaner, Norton) a été sanctionné par la FTC : 16,5 M$ pour la revente de données de navigation via Jumpshot, avec des remboursements toujours versés en 2025-2026.

### Cited Findings

**Boosters et task killers**
- La doc Android 14 dit : « Android is designed to keep cached apps in the background and kill them automatically when the system needs memory. If your app kills other apps unnecessarily, it can reduce system performance and increase battery consumption by requiring full restarts… ». Elle ajoute : « It isn't possible for a 3rd-party application to improve the memory, power, or thermal behavior of an Android device. You should ensure that your app is compliant with Google Play's policy regarding misleading claims. » — [Android 14 behavior changes](https://developer.android.com/about/versions/14/behavior-changes-all)
- Android 17 introduit des limites mémoire par appli selon la RAM totale de l'appareil (le système tue l'appli fautive, avec la raison « MemoryLimiter:AnonSwap »). La gestion mémoire est donc encore plus centralisée dans l'OS. — [Android 17 behavior changes](https://developer.android.com/about/versions/17/behavior-changes-all)
- How-To Geek : « Task killers aren't just useless – they can reduce performance ». Les applis relancées chargent plus lentement et consomment plus, et certaines redémarrent automatiquement. — [How-To Geek](https://www.howtogeek.com/127388/htg-explains-why-you-shouldnt-use-a-task-killer-on-android/) (via résumé de recherche, article ancien)
- Android Police a couvert le « crackdown » d'Android 14 contre les task managers et RAM boosters. — [Android Police](https://www.androidpolice.com/android-14-task-manager-ram-booster-apps/), [Chrome Unboxed](https://chromeunboxed.com/task-killer-crackdown-android-14/) (via résumé de recherche)
- Les effets mis en avant par les cleaners sont souvent surestimés : pour CCleaner, « the battery optimizer delivers more modest results than advertised ». — [softwaretestinghelp](https://www.softwaretestinghelp.com/best-android-cleaner-apps/) / [guru99](https://www.guru99.com/best-android-cleaner-apps.html) (via résumé de recherche, sources faibles)

**Vider le cache en permanence**
- La doc Android avertit que `ACTION_CLEAR_APP_CACHE` « can substantially affect device battery life ». Elle rappelle que le système supprime lui-même les caches quand l'espace manque. — [App-specific storage](https://developer.android.com/training/data-storage/app-specific)
- SD Maid définit les fichiers « expendable » comme des fichiers qui « will be recreated when needed ». — [docs/appcleaner.md](https://github.com/d4rken-org/sdmaid-se/blob/main/docs/appcleaner.md)

**Malware et adware dans la catégorie**
- McAfee, HiddenAds (juillet 2022) : de faux nettoyeurs et optimiseurs de batterie lançaient des services malveillants dès l'installation et se cachaient. L'un d'eux dépassait 1 M d'installations. — [McAfee](https://www.mcafee.com/blogs/other-blogs/mcafee-labs/new-hiddenads-malware-that-runs-automatically-and-hides-on-google-play-1m-users-affected/)
- McAfee, faux cleaners avec « AutoRun ». — [McAfee](https://www.mcafee.com/blogs/other-blogs/mcafee-labs/fake-cleaning-apps-in-google-play-an-autorun-attack-and-more/)
- Trend Micro (2020) : des boosters et optimiseurs sur Play (AndroidOS_BadBooster.HRX) pouvaient télécharger jusqu'à 3 000 charges malveillantes, pour plus de 470 000 téléchargements. — [Trend Micro](https://www.trendmicro.com/en_us/research/20/b/malicious-apps-on-google-play-communicate-with-trojans-install-malware-perform-mobile-ad-fraud.html) (via résumé de recherche, **données antérieures à 2024**)

**Politique Google Play**
- Des développeurs de cleaners ont été rejetés au titre de la politique « Deceptive Behavior », par exemple une appli se disant antivirus sans détection. L'extrait de politique cité dit : « Any changes to device settings must be made with the user's knowledge and consent and be reversible by the user. » — [CodeCanyon (notice reçue par un dev)](https://codecanyon.net/comments/28487261) (via résumé de recherche, source secondaire ; l'attribution exacte de l'extrait à cette URL est incertaine)
- `MANAGE_EXTERNAL_STORAGE` est soumis à une revue Play depuis mai 2021 pour les applis ciblant Android 11+. — [Manage all files](https://developer.android.com/training/data-storage/manage-all-files)
- SD Maid avait été suspendu en 2023 au titre de la politique « Stalkerware ». — [Android Police](https://www.androidpolice.com/sd-maid-terminated-play-store-stalkerware-policy/) (via résumé de recherche)

**Confidentialité et régulation (Avast, CCleaner, Gen Digital)**
- FTC contre Avast :
  - de 2014 à 2020, Avast a vendu les données de navigation de ses utilisateurs via sa filiale Jumpshot à plus de 100 tiers ;
  - accord de 16,5 M$ et interdiction de vendre ou licencier des données de navigation à des fins publicitaires, ordonnance proposée en février 2024 et finalisée en juin 2024.
  - Sources : [InfoLawGroup](https://infolawgroup.com/insights/2024/7/17/ftc-finalizes-165m-settlement-with-avast-over-sales-of-browsing-data-for-ad-targeting), [The Hacker News](https://thehackernews.com/2024/02/ftc-slams-avast-with-165-million-fine.html) (via résumé de recherche)
- Calendrier des remboursements : procédure de réclamation annoncée le 24/02/2025 (administrateur Rust Consulting). Selon un rapport secondaire, un second versement a eu lieu en octobre 2026 : 91 093 paiements pour plus de 1,5 M$. — [FTC](https://ftc.gov/news-events/news/press-releases/2025/02/ftc-announces-refund-claims-process-avast-customers-impacted-deceptive-privacy-claims), [OpenClassActions](https://openclassactions.com/news/avast-ftc-refunds-second-payment-2026.php) (via résumé de recherche, montants de source secondaire)
- CCleaner : en 2018, une erreur dans la version 5.45 a activé « Active Monitoring », renommé ensuite « Smart Cleaning ». Une collecte (OS, langue) reste activée par défaut. En décembre 2018, Avast aurait été installé sans consentement avec CCleaner, ce que Piriform a nié. — [Wikipedia CCleaner](https://en.wikipedia.org/wiki/CCleaner) (via résumé de recherche, **épisodes desktop, pas Android**)
- Gen Digital (CCleaner, Avast, Norton, Avira) a confirmé en 2023 une fuite de données de clients via la faille MOVEit. — [teiss](https://www.teiss.co.uk/news/gen-digital-confirms-data-breach-personal-data-of-ccleaner-users-stolen-13054) (via résumé de recherche)

### Inferences
- Pour notre appli : **aucun bouton « Boost RAM », « Refroidir le CPU » ou « Économiser la batterie » qui tuerait des processus**. C'est techniquement impossible sur Android 14+ et contre-productif selon Google. Il vaut mieux afficher les vraies métriques (utilisation mémoire, applis gourmandes) et renvoyer vers les réglages système : restriction de batterie, Force stop, désactivation.
- Le vidage de cache doit rester ponctuel et ciblé : une appli au cache anormalement gros, ou un besoin urgent d'espace. Sa valeur est surtout « diagnostique ». Le système purge déjà les caches sous pression.
- L'argument confidentialité (local-first, zéro télémétrie, open source) est un vrai différenciateur face aux cleaners commerciaux, et c'est déjà le positionnement de SD Maid SE.

### Gaps
- Texte actuel (2026) de la politique Play « Deceptive Behavior » sur les claims de performance, et éventuelles sanctions récentes (2025-2026) contre des boosters : Developer Policy Center inaccessible, quota de recherche épuisé.
- Comportement de télémétrie des applis Android CCleaner et Avast Cleanup en 2025-2026 (Data Safety Play) : non vérifié.
- Rapports malware sur des cleaners en 2025-2026 : non trouvés. Les rapports cités datent de 2020-2022.

## 5. Outils intégrés au Pixel (Settings > Stockage, Files by Google, Smart Storage, archivage)

### Takeaway
Un Pixel offre de base l'écran Paramètres > Stockage (répartition par catégorie, « Libérer de l'espace »), Files by Google (fichiers indésirables, doublons exacts, téléchargements, captures, applis inutilisées, corbeille), Smart Storage (suppression auto des photos sauvegardées dans Google Photos après 60 jours ou sous 25 % d'espace libre) et l'archivage d'applis (Play automatique, et manuel depuis Infos de l'appli sous Android 15+). Manquent : l'historique, les restes d'applis désinstallées, les caches privés sélectifs, Android/data, les photos similaires et la planification au-delà des photos.

### Cited Findings
- Smart Storage s'active dans Paramètres > Stockage > Libérer de l'espace > Paramètres. Une option distincte « Supprimer les photos et vidéos sauvegardées » existe aussi. — [Aide Pixel](https://support.google.com/pixelphone/answer/2840863?hl=en) (via résumé de recherche)
- Une fois Smart Storage activé, les photos sauvegardées dans Google Photos sont supprimées « If photos are saved for 60 days » ou « If your device's storage is less than 25% ». La fonction est désactivée par défaut. — [Aide Files Smart Storage](https://support.google.com/files/answer/10862356?hl=en-GB) (via résumé de recherche)
- Une couverture plus ancienne décrivait un choix entre 30, 60 et 90 jours. — [9to5Google 2021](https://9to5google.com/2021/09/10/google-smart-storage-files-pixel/), [XDA](https://www.xda-developers.com/files-by-google-app-smart-storage-automatically-delete-backed-up-media/), [Gadget Hacks](https://android.gadgethacks.com/how-to/automatically-delete-pictures-videos-youve-backed-up-google-photos-0251755/) (via résumé de recherche, **sources anciennes**, options actuelles non confirmées)
- La partie « Smart Storage » d'Android remonte au « Storage manager » d'Android 7.1 Nougat. — [Android Authority](https://androidauthority.com/android-7-1-nougat-smart-storage-manager-723501) (via résumé de recherche)
- Files by Google, onglet Clean : fichiers indésirables, doublons (vers la Corbeille), anciennes captures, memes, téléchargements, applis inutilisées. — [Android Police 2026](https://www.androidpolice.com/found-10gb-wasted-storage-on-my-phone-with-this-free-google-app/), [Aide Files](https://support.google.com/files/answer/9713869?hl=en) (via résumé de recherche)
- Un avis Play du 27/10/2026 signale un problème de nettoyage du dossier des fichiers indésirables. — [Fiche Play Files](https://play.google.com/store/apps/details?id=com.google.android.apps.nbu.files&hl=en) (via résumé de recherche, anecdotique)
- Archivage :
  - auto-archivage Play sur option, avec exclusions par appli — [Aide Android](https://support.google.com/android/answer/15523443?hl=en) (via résumé de recherche) ;
  - support au niveau OS dans Android 15 (APK et cache supprimés, données conservées) — [Android 15 features](https://developer.android.com/about/versions/15/features) ;
  - archivage manuel depuis la page d'informations de l'appli sous Android 15 — [Android Central](https://www.androidcentral.com/apps-software/how-to-archive-android-apps) (via résumé de recherche).
- Google Photos > « Libérer de l'espace » retire les copies locales déjà sauvegardées. — [Android Authority](https://www.androidauthority.com/control-clean-google-photos-storage-3525116/) (via résumé de recherche)

### Inferences
- Les outils Pixel couvrent bien l'usage grand public « ponctuel ». Une appli de monitoring personnelle doit apporter ce qu'ils n'ont pas :
  - historique et prévision de saturation ;
  - vue unifiée par appli avec code, données, cache et Android/data (via Shizuku) ;
  - détection des restes d'applis désinstallées ;
  - doublons similaires avec corbeille ;
  - alertes de croissance anormale d'une appli ;
  - suggestion d'archiver les applis inutilisées, en combinant les dates de dernière utilisation d'UsageStats et `requestArchive`.
- `requestArchive` (Android 15+, `REQUEST_DELETE_PACKAGES`) rend possible l'archivage depuis une appli tierce. Le désarchivage dépend de l'installeur d'origine (Play), donc les applis installées hors store ne peuvent sans doute pas être désarchivées proprement.

### Gaps
- Libellés exacts de l'écran Paramètres > Stockage sous Android 16/17 sur Pixel 8 Pro (catégories, bouton « Libérer de l'espace », éventuels caches) : support.google.com inaccessible, non vérifié.
- Présence d'un réglage système de « vidage de tous les caches » sur Pixel (Android 16/17) : non vérifié.
- Durée de la Corbeille de Files by Google et éventuelles nouveautés 2025-2026 (similaires, IA) : non confirmées.
- Comportement exact de l'archivage pour les applis installées hors store sur Android 15/16 : non vérifié.
