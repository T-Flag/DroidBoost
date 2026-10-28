# Lacunes, frustrations et attentes des power users Android : apps de monitoring, nettoyage et gestion d'apps (état oct. 2026), et idées venues d'autres écosystèmes

> **À lire avant d'utiliser ces notes (méthode et limites)**
> - Date de recherche : 28 oct. 2026. Appareil cible : Pixel 8 Pro (Tensor G3), non rooté, avec Shizuku/ADB possible.
> - **Reddit n'a pas pu être interrogé** : le domaine reddit.com est refusé par l'outil de recherche. Seuls quelques fils r/tasker sont accessibles via des miroirs (redlib). Les fetchs directs ont aussi échoué sur androidpolice.com, androidcentral.com, piunikaweb.com, xdaforums.com, mirfatif.github.io et hn.algolia.com, car ces domaines sont hors de la liste réseau autorisée de la session. Pour ces sources, les affirmations reposent sur **les extraits renvoyés par le moteur de recherche** (signalés « via extrait de recherche »), pas sur une lecture complète.
> - Le **quota de recherches web a été épuisé** en fin de session. Les recherches prévues sur l'historique batterie 10 jours d'iOS, les fils Hacker News, les plaintes sur DevCheck/CPU-Z et le statut des API batterystats sous Android 16 n'ont pas pu être faites (voir les Gaps).
> - GitHub (issues, releases, README) a été lu directement : ce sont les preuves les plus solides de ces notes.

## Q1. De quoi les utilisateurs se plaignent-ils dans les apps existantes (DevCheck, CPU-Z, AccuBattery, CCleaner, SD Maid SE, Files by Google, App Manager, Canta, Hail, GSam, NetGuard, Shizuku…) ?

### Takeaway
Les plaintes se regroupent en cinq familles :
1. **Fragilité de la couche de privilèges.** Shizuku s'arrête au reboot, se coupe en Wi-Fi ou écran éteint, et casse à chaque QPR. Sa maintenance est jugée défaillante au point qu'un fork (Porter) est apparu en 2026.
2. **Casse récurrente due aux nouvelles versions d'Android.** Le nettoyage de cache par accessibilité de SD Maid SE a cassé sous Android 16 et 17, Pixel compris.
3. **Apps de référence abandonnées ou partielles.** BetterBatteryStats est archivé, Wakelock Detector n'est plus maintenu, et AccuBattery ou Battery Guru sont jugés insuffisants pour diagnostiquer un drain.
4. **Modèle économique et vie privée des « cleaners » grand public.** CCleaner affiche pubs, upsell et abonnements, partage des données avec des tiers et sur-annonce l'espace libéré.
5. **Limites imposées par la plateforme.** Il est impossible de voir le CPU par app sans privilèges depuis Android 7/8, et un seul slot VPN est disponible pour NetGuard.

### Cited Findings

**Shizuku : la dépendance la plus fragile de tout l'écosystème « sans root »**
- Le README de Porter (fork de Shizuku par d4rken, auteur de SD Maid SE) écrit : « The original Shizuku app was no longer actively maintained, so I started Porter ». Il vise « a minimal, stable and maintained alternative ». Porter non plus ne redémarre pas seul : le service « stops when the device restarts. Start it again afterwards. » — [d4rken-org/porter README](https://github.com/d4rken-org/porter)
- SD Maid SE v2.2.0 prend en charge Porter à côté de Shizuku. Son guide d'installation recommande désormais Porter pour les nouvelles configurations ADB — [SD Maid SE releases](https://github.com/d4rken-org/sdmaid-se/releases) ; [PR #2871](https://github.com/d4rken-org/sdmaid-se/pull/2871) ; [PR #2780](https://github.com/d4rken-org/sdmaid-se/pull/2780)
- Issue Shizuku #1125 « Android 16 QPR 1 Beta breaks Shizuku », ouverte le 20 mai 2025, 29 👍. Des propriétaires de **Pixel 8 Pro** figurent parmi les touchés.
  - Cause : `AbstractMethodError` sur `IProcessObserver.onProcessStarted`.
  - Un build non officiel a circulé dès le 21 mai, puis la v13.6.0 est sortie sur GitHub, mais la version Play Store est restée en retard (« Don't use the Google Play version »).
  - En septembre 2025, on signale encore des arrêts aléatoires après 10 minutes ou quelques heures — [Shizuku #1125](https://github.com/RikkaApps/Shizuku/issues/1125)
- Issue Shizuku #1478 « Shizuku is constantly turning off now », ouverte le 29 sept. 2025, toujours ouverte, 14 👍.
  - Déconnexions parfois toutes les quelques secondes, sur One UI 8/8.5, Pixel 9 Pro et Pixel 10.
  - « ADB over Wi-Fi drops on Android 16 after a while ».
  - Aucune réponse de mainteneur dans le fil — [Shizuku #1478](https://github.com/RikkaApps/Shizuku/issues/1478)
- Issue Shizuku #294 « Shizuku stops working daily/have to reinstate wireless bugging every morning » (2023-2024).
  - Sur Pixel 6 Pro (Android 13), il faut le réactiver une fois par jour.
  - Sur OnePlus 11 (Android 14), le débogage sans fil se coupe « ~3 to 10 minutes ».
  - Contournement : un script lancé par Tasker à chaque déverrouillage — [Shizuku #294](https://github.com/RikkaApps/Shizuku/issues/294)
- Autres signaux du même problème :
  - #864 « Workaround - Keeping shizuku working after leaving wifi » (déc. 2024, 11 👍) — [Shizuku #864](https://github.com/RikkaApps/Shizuku/issues/864)
  - #1426 « Shizuku stops when switching screen off on Xiaomi devices and on some other devices » (62 commentaires) — [Shizuku #1426](https://github.com/RikkaApps/Shizuku/issues/1426)
- La v13.6.0 ajoute « auto start without root on Android13+ when connected to a trusted WLAN ». Le démarrage auto existe donc, mais seulement sur un Wi-Fi de confiance — [Shizuku releases](https://github.com/RikkaApps/Shizuku/releases)
- Discussion #462, auto-activation sans root (Termux + scripts) :
  - « Wireless debugging must be turned on again after each reboot ».
  - Les scripts cassent quand le chemin de `libshizuku.so` change.
  - Erreurs « more than one device/emulator » ; certains n'arrivent jamais à le faire fonctionner (avr. 2025) — [Shizuku Discussion #462](https://github.com/RikkaApps/Shizuku/discussions/462)
- Côté Tasker (intégration Shizuku depuis la beta 6.6.2, juillet 2025), des utilisateurs notent que « Tasker can check ADB Wifi state but not Shizuku's state » et que l'ADB Wifi reste nécessaire pour logcat (via miroir Reddit) — [r/tasker (redlib)](https://redlib.hackliberty.org/r/tasker/comments/1l1ynue/how_to_utilize_shizuku_to_run_adb_shell_commands/mx8s8el)

**SD Maid SE : fonctionnel mais régulièrement cassé par les nouvelles versions d'Android, et en partie payant**
- Correctifs listés dans les notes de version (dates sans année sur la page, très probablement 2026) :
  - v2.2.0 : le cache d'AppCleaner échouait sur Motorola et Sony sous Android 16+.
  - v2.2.1 : des ROM Android 17 cachaient les boutons « vider le cache » ou ouvraient la recherche des Paramètres.
  - v2.2.2 : le vidage de cache échouait ou était compté comme échec **sur Pixel sous Android 17**, ainsi que sur les Pixel avec grande police ou grande taille d'affichage — [SD Maid SE releases](https://github.com/d4rken-org/sdmaid-se/releases)
- Issues récentes :
  - #2280 « Android 16 cleaning problem » (mars 2026) — [#2280](https://github.com/d4rken-org/sdmaid-se/issues/2280)
  - #1829 « SD Maid is being killed while scanning in the background » (juin 2025) — [#1829](https://github.com/d4rken-org/sdmaid-se/issues/1829)
  - #1247 « System Cleaner scan results not showing when completed in background » — [#1247](https://github.com/d4rken-org/sdmaid-se/issues/1247)
- Limites payantes : le Deduplicator exige la version Pro pour la suppression en un geste (v1.7.4). Swiper est limité à deux sessions gratuites (v2.2.2) — [SD Maid SE releases](https://github.com/d4rken-org/sdmaid-se/releases)

**Apps batterie : abandon et insuffisance**
- BetterBatteryStats (BBS) : le dépôt GitHub est archivé et des utilisateurs disent qu'il ne marche pas sous Android 14. Un fork « bbs_reloaded » circule (via extrait de recherche) — [XDA « Looking for an alternative to BetterBatteryStats or GSam »](https://xdaforums.com/t/looking-for-an-alternative-to-betterbatterystats-or-gsam-battery-monitor.4746711/) ; [AlternativeTo BBS](https://alternativeto.net/software/betterbatterystats/about) ; [XDA « Abandoned... Alternatives? »](https://xdaforums.com/t/abandoned-alternatives.4783381/)
- Dans ces fils XDA, des utilisateurs jugent AccuBattery et Battery Guru **insuffisants pour diagnostiquer un drain** (via extrait de recherche) — [XDA](https://xdaforums.com/t/looking-for-an-alternative-to-betterbatterystats-or-gsam-battery-monitor.4746711/)
- Un commentateur XDA affirme qu'Android 16 aurait « removed the API / stats which BBS and GSam were reading ». C'est **non vérifié** et contredit par d'autres rapports : GSam 3.46 a ajouté la prise en charge d'Android 14 et fonctionne sur S23 Ultra — [XDA (extrait)](https://xdaforums.com/t/abandoned-alternatives.4783381/) ; [APKMirror GSam 3.46](https://www.apkmirror.com/apk/gsam-labs/gsam-battery-monitor/gsam-battery-monitor-3-46-release/gsam-battery-monitor-3-46-android-apk-download/)
- Wakelock Detector est décrit comme « outdated and unmaintained », avec des données incomplètes ou un échec au lancement sur les téléphones récents — [androidayuda](https://en.androidayuda.com/Identify-apps-that-wake-the-processor-(wakelocks)-on-Android/)
- Précision d'AccuBattery : un fil Android Central (≈2021, **ancien**) oppose ceux qui disent que « [it] cannot be accurate » et ceux qui le jugent « accurate enough for general use ». Un avis AlternativeTo indique qu'il ne fonctionne pas avec d'anciens Samsung qui « don't give out the right data » — [Android Central](https://forums.androidcentral.com/threads/how-accurate-is-accubattery.1042146/) ; [AlternativeTo AccuBattery](https://alternativeto.net/software/accubattery/about/)

**Cleaners grand public : pubs, abonnements, vie privée, résultats gonflés**
- CCleaner Android :
  - La fiche Play indique pubs et achats intégrés. Un avis Play se plaint de publicités d'upgrade affichées même après avoir payé.
  - La section « sécurité des données » mentionne un partage possible de la localisation et de l'activité dans les apps avec des tiers — [Google Play CCleaner](https://play.google.com/store/apps/details/CCleaner_Cache_RAM_cleaner?id=com.piriform.ccleaner&hl=en_ZA)
  - Trustpilot rapporte des facturations d'abonnement inattendues, des « billing loops » et des pop-ups promotionnels constants — [Trustpilot CCleaner](https://uk.trustpilot.com/review/www.ccleaner.com?page=3)
  - Un test 2026 conclut que l'espace réellement récupéré est **inférieur** à ce que l'app annonce — [thehightechsociety](https://thehightechsociety.com/ccleaner-for-android-review/) ; voir aussi [Cybernews](https://cybernews.com/privacy-tools/ccleaner-review/)
- Une étude Surfshark (citée de seconde main, centrée App Store, dont le rapport original n'a pas été retrouvé) affirme que les 10 apps de nettoyage les plus populaires partagent toutes des données avec des tiers : 90 % des identifiants uniques et 70 % d'autres données (achats, localisation). L'article dit que c'est pareil sur Android — [digitalrights.asia](https://digitalrights.asia/events/smartphone-cleaning-apps-privacy-risk/?lang_ui=en)
- Trend Micro classe comme indésirables (PUA) les cleaners qui « trick you with fake cleaning results, gather information without asking you, and do not improve device performance or storage space » — [Trend Micro / TrendLife](https://helpcenter.trendlife.com/en-us/article/tmka-11485/)
- Les « RAM boosters » sont décrits comme largement inefficaces, puisqu'Android gère la mémoire lui-même. La source est un éditeur concurrent, donc biaisée — [Cleanor](https://cleanor.app/blog/the-truth-about-cleaner-apps-are-they-safe-to-use)

**Limites de plateforme qui frustrent les moniteurs « type Task Manager »**
- Selon le développeur d'AnotherMonitor, Android 7.0 a « significantly restricted access to the proc file system » et fait disparaître le CPU par processus des autres apps. Android 8 a ensuite bloqué `/proc/stat` par SELinux pour les apps non système — [AnotherMonitor](https://github.com/AntonioRedondo/AnotherMonitor) ; [softwaresamurai (2018, ancien)](https://www.softwaresamurai.org/2018/12/19/android-oreos-cpu-stats/)
- Un module Magisk (« No Proc Stat Restriction ») existe justement pour lever cette restriction sur un appareil rooté — [magisk.dev](https://magisk.dev/modules/noprocstatrestriction/)
- NetGuard occupe « the one and only active VPN slot on your pixel, without actually being a VPN ». RethinkDNS contourne le problème en servant lui-même de client WireGuard — [GrapheneOS forum](https://discuss.grapheneos.org/d/16993-grapheneos-appverse-a-curated-collection-of-privacy-respecting-apps/39) ; [Techlore](https://discuss.techlore.tech/t/what-are-your-opinions-on-rethinkdns/5471)
- L'app compagnon Home Assistant, souvent utilisée comme « exporteur » de capteurs, génère elle-même des plaintes de consommation. Exemple : l'issue « Extremely high battery usage with all sensors enabled » — [HA #3887](https://github.com/home-assistant/android/issues/3887) ; [HA community](https://community.home-assistant.io/t/battery-usage-too-high/458714)

**Facteur bus et maintenance des petites apps FOSS**
- Canta : sur la demande de prise en charge du profil professionnel (sept. 2026), le mainteneur répond « Currently I'm finishing my master thesis so kinda busy with that » — [Canta #350](https://github.com/samolego/Canta/issues/350)
- Hail : la question « How to see log of auto freeze » (avr. 2026) reste sans réponse du mainteneur. Un bug récent rapporte que l'auto-freeze après verrouillage cesse de marcher après la première exécution (oct. 2026) — [Hail #388](https://github.com/aistra0528/Hail/issues/388) ; [Hail #452](https://github.com/aistra0528/Hail/issues/452)

### Inferences
- La fiabilité de la couche Shizuku/Porter est le premier risque d'architecture pour une app personnelle. Toute fonction « Shizuku-only » doit dégrader proprement, signaler clairement que le service est tombé, et idéalement tenter de le relancer : démarrage auto sur Wi-Fi de confiance (Shizuku 13.6+) ou relance via Tasker.
- Le nettoyage de cache par AccessibilityService est structurellement fragile, car il dépend de l'UI des Paramètres, qui change à chaque version, ROM ou taille de police. Une app personnelle gagnerait à **ne pas réimplémenter** le nettoyage. Elle peut plutôt mesurer et historiser, puis déléguer l'action à SD Maid SE, ou passer par des commandes shell via Shizuku quand c'est possible.
- Plusieurs griefs (pubs, abonnements, partage de données, upsell) disparaissent mécaniquement avec une app sideloadée personnelle, 100 % locale et sans réseau. C'est un avantage différenciant réel, pas seulement marketing.

### Gaps
- Aucune plainte datée 2024-2026 n'a été trouvée spécifiquement sur **DevCheck, CPU-Z ou Files by Google** : le quota de recherche était épuisé et Reddit inaccessible. À compléter.
- Statut de maintenance de NetGuard : un rapport de seconde main évoque un possible arrêt ou un retrait du Play Store ([Techlore](https://discuss.techlore.tech/t/netguard-release-notes-urges-me-to-work-on-my-google-phones/5750)), non vérifié.
- Aucune citation directe n'a été trouvée sur le besoin de « 5 à 10 apps différentes » (Reddit bloqué). Seul indice indirect : la liste [awesome-shizuku](https://github.com/timschneeb/awesome-shizuku), qui recense des dizaines d'outils mono-fonction.
- Pas de confirmation officielle que l'affichage des estimations d'autonomie de Pixel ou d'AccuBattery est inexact en 2025-2026.

## Q2. Quelles fonctionnalités les utilisateurs demandent-ils explicitement (issues GitHub, XDA, « is there an app that… ») ?

### Takeaway
Les demandes explicites portent surtout sur :
- des **journaux et historiques** : journal d'activité, historique des permissions et capteurs, historique des notifications, log de l'auto-freeze, statistiques de nettoyage ;
- la **portabilité** : export de la liste d'apps, des configurations et des révocations de permissions sous forme de commandes ADB ;
- l'**automatisation** : profils et planification de gel, gel ou dégel à la sortie, révocation sélective de permissions ;
- la **couverture multi-profils** (profil pro, Shelter/Island) ;
- un **diagnostic de wakelocks sans root** que les utilisateurs n'arrivent pas à obtenir.

### Cited Findings
- **App Manager #143 « Add an activity log »**, ouverte par le mainteneur le 11 nov. 2020 et toujours ouverte, au statut « Pending ». Le projet : un onglet listant « every events such as app usage (foreground/background) », plus l'usage data, les app ops et les notifications — [AppManager #143](https://github.com/MuntashirAkon/AppManager/issues/143)
- **App Manager #382 « Usage monitor and history »** (avr. 2021) : « Show a notification when an app is using location, camera, microphone… History would contain this usage record. » L'issue a été fermée comme doublon de #143, qui n'est toujours pas implémentée — [AppManager #382](https://github.com/MuntashirAkon/AppManager/issues/382)
- Autres demandes ouvertes sur App Manager :
  - #458 « Notification history » — [#458](https://github.com/MuntashirAkon/AppManager/issues/458)
  - #755 « Log viewer enhancements » — [#755](https://github.com/MuntashirAkon/AppManager/issues/755)
  - #1937 « Batch export permission revocations as ADB commands » (mars 2026) — [#1937](https://github.com/MuntashirAkon/AppManager/issues/1937)
  - #844 « CPU usage background » (2022) — [#844](https://github.com/MuntashirAkon/AppManager/issues/844)
- **SD Maid SE #154 « Suggestion to add statistics »** (avr. 2023) : on demande des stats « comme dans SD Maid 1 », et le mainteneur propose un « deletion history? #46 ». L'issue a été convertie en discussion #359 — [SD Maid SE #154](https://github.com/d4rken-org/sdmaid-se/issues/154)
- SD Maid SE #2834 (6 oct. 2026) demande un export automatique des réglages après modification — [#2834](https://github.com/d4rken-org/sdmaid-se/issues/2834)
- Les demandes de tendance stockage ont été en partie satisfaites :
  - v2.0.4 : « Analyzer: Fix the storage trend, estimate days until full, and warn before space runs out » — [release v2.0.4-rc0](https://github.com/d4rken-org/sdmaid-se/releases/tag/v2.0.4-rc0)
  - v2.2.1 : historique de tendance stockage conservé jusqu'à un an — [SD Maid SE releases](https://github.com/d4rken-org/sdmaid-se/releases)
- **Hail** :
  - #388 « I'm interested in auto freeze service, when it started and what it did, was it successful » (avr. 2026) — [#388](https://github.com/aistra0528/Hail/issues/388)
  - #37 « Add frozen profile and schedule » — [#37](https://github.com/aistra0528/Hail/issues/37)
  - #124 « Auto freeze app on exit » — [#124](https://github.com/aistra0528/Hail/issues/124)
  - #447 « Selective runtime permission revocation/restoration when freezing apps » (oct. 2026) — [#447](https://github.com/aistra0528/Hail/issues/447)
  - #403 « freeze unfreeze app by notification » (juin 2026) — [#403](https://github.com/aistra0528/Hail/issues/403)
  - #396 « auto-unfreeze » — [#396](https://github.com/aistra0528/Hail/issues/396)
  - #237 « API Feature Request - Automate Freeze Whitelisted » — [#237](https://github.com/aistra0528/Hail/issues/237)
- **Canta** :
  - #350 « Add Work Profile Support » (mars 2026, 6 👍, la demande la plus votée du dépôt). Motif : « previously removed bloatware apps reappear inside the work profile » — [Canta #350](https://github.com/samolego/Canta/issues/350)
  - #358 « Add the possibility to export app list » (avr. 2026) — [#358](https://github.com/samolego/Canta/issues/358)
  - #211 « Create configs » (mai 2025) — [#211](https://github.com/samolego/Canta/issues/211)
- **Shizuku** :
  - #840 « Support for adb root » — [#840](https://github.com/RikkaApps/Shizuku/issues/840)
  - démarrage persistant ([#864](https://github.com/RikkaApps/Shizuku/issues/864), [Discussion #462](https://github.com/RikkaApps/Shizuku/discussions/462))
- **LogFox** : demandes autour de l'enregistrement et de la relecture de logs (#29 « add ability to record and view log files », #185 sur la fonction « recording ») — [LogFox #29](https://github.com/F0x1d/LogFox/issues/29) ; [LogFox #185](https://github.com/F0x1d/LogFox/issues/185)
- **Home Assistant (app Android)** :
  - #5031 « Send smartphone sensors on a limited periodicity » — [HA #5031](https://github.com/home-assistant/android/issues/5031)
  - #2637 « Internal Storage sensor only updating once on app start » (2022, ancien) — [HA #2637](https://github.com/home-assistant/android/issues/2637)
- **XDA** :
  - Un utilisateur sous Android 13 sans root a essayé `adb shell dumpsys batterystats` et Battery Historian. Il dit encore « struggled to pinpoint the app causing repeated partial wakelocks » et demande un workflow pour appareil non rooté (via extrait de recherche) — [XDA « Identifying High-Frequency Wake-Locks on Android 13 Without Root »](https://xdaforums.com/t/identifying-high-frequency-wake-locks-on-android-13-without-root.4754880/)
  - Fil « [App suggestions?] manage wakelocks // show processes CPU usage » (titre seulement) — [XDA](https://xdaforums.com/t/app-suggestions-manage-wakelocks-show-processes-cpu-usage.4599015/)

### Inferences
- Le thème « journal / historique / traçabilité » revient dans presque tous les dépôts (App Manager, Hail, SD Maid SE, LogFox), mais aucun projet ne l'a livré de façon transversale. C'est la lacune la mieux documentée.
- La demande « exporter en commandes ADB » (#1937) et « exporter la liste d'apps » (Canta #358) montre que les power users veulent un état **reproductible** du téléphone : debloat, permissions, gel. Une app personnelle peut stocker des « snapshots » d'état et produire un diff entre deux dates.
- Les utilisateurs ne demandent pas un « booster » : ils veulent comprendre (qui, quand, pourquoi) et automatiser proprement.

### Gaps
- Les demandes Reddit (« is there an app that… ») n'ont pas pu être collectées (domaine bloqué). Il manque des citations directes sur un dashboard unifié, un « health score » ou des explications « pourquoi mon téléphone chauffe ».
- Les nombres de 👍 sont faibles sur la plupart des dépôts (0 à 29). Il est difficile de hiérarchiser par le vote seul.
- Pas de forum de suggestions DevCheck consulté.

## Q3. Quelles capacités sont rares ou absentes sur Android, et quelles apps de niche les couvrent partiellement ?

### Takeaway
Android natif (Pixel) ne conserve qu'un horizon court :
- la batterie « depuis la dernière charge complète » ;
- le Privacy Dashboard sur 7 jours, et seulement pour localisation, caméra et micro ;
- aucune santé batterie pour le Pixel 8 Pro.

Il n'offre ni alerte d'anomalie côté utilisateur, ni journal unifié installations/permissions/plantages, ni export. Des briques de niche existent mais sont **éparpillées** :
- tendance stockage de SD Maid SE ;
- drain par app via Shizuku dans BatStats ;
- rails d'énergie ODPM et CSV dans Gymnotus ;
- plantages et ANR dans LogFox ;
- automatisation dans Tasker + Shizuku ;
- capteurs vers Home Assistant.

Plusieurs API récentes rendent possible ce qui manque : PowerMonitor (API 35), thermal headroom (API 30) et l'usage stats.

### Cited Findings

**Historique long terme (batterie, stockage, données)**
- Pixel : le graphique batterie montre le niveau « since the last full charge » (Paramètres > Batterie > Utilisation de la batterie) — [Pixel Help](https://support.google.com/pixelphone/answer/2819525?hl=en)
- Android 12 n'affichait que les dernières 24 h. Android 13 QPR1 a restauré la vue « depuis la dernière charge complète » — [PhoneArena](https://www.phonearena.com/news/android-13-QPR-1-beta-2-improves-battery-stats-page_id143082)
- L'issue Google 262066100 note que les stats ne se réinitialisent qu'à 100 % (contre 90 % auparavant), et c'est toujours vrai sur les premiers builds Android 14 — [Issue Tracker 262066100](https://issuetracker.google.com/issues/262066100)
- Couverture partielle du stockage : SD Maid SE a désormais une tendance de stockage avec estimation « jours avant saturation », alerte et historique d'un an. Il s'agit d'une tendance **globale**, pas par app d'après les notes de version — [SD Maid SE releases](https://github.com/d4rken-org/sdmaid-se/releases)
- Couverture partielle de la batterie par app : BatStats (GPL-3.0) propose un mode heuristique (Usage Stats → app au premier plan) et un mode « Enhanced » via Shizuku. Ce dernier ventile CPU, wakelocks, réseau, GPS, capteurs, caméra et Bluetooth, avec données 100 % locales selon les fiches. Aucune revue indépendante de sa précision n'a été trouvée — [AlternativeTo BatStats](https://alternativeto.net/software/batstats/about) ; [APKPure BatStats](https://apkpure.com/batstats/org.mlm.batstats/amp)

**Historique des permissions et de la vie privée**
- Le Privacy Dashboard 7 jours est arrivé via la mise à jour système Google Play de novembre 2024 (Pixel, Android 15 QPR1). La timeline détaillée ne concerne que localisation, caméra et micro : pas d'historique pour Agenda, Téléphone ou Contacts — [9to5Google](https://9to5google.com/2024/12/10/android-privacy-dashboard-7-day-history/) ; [Android Police](https://www.androidpolice.com/android-15-7-day-privacy-dashboard-view-ahead-of-schedule/)
- Aucun outil trouvé qui historise les **changements** de permissions (octroi/révocation). La demande d'App Manager à ce sujet (#143/#382) n'est pas implémentée — [AppManager #143](https://github.com/MuntashirAkon/AppManager/issues/143)

**Journal installations / désinstallations / mises à jour**
- Aucune app open source trouvée qui journalise installations, désinstallations **et** mises à jour. Les plus proches :
  - « Changes » : timeline des mises à jour + changelogs ; article de **2017**, ancien — [gHacks](https://www.ghacks.net/2017/08/01/keep-track-of-android-app-updates-with-changes/)
  - « Changelogs » — [PhoneArena](https://phonearena.com/news/Changelogs-for-Android-shows-you-whats-new-after-the-latest-app-updates_id79680)
  - whatsnew-app : popup « What's new » + journal d'activité — [GitHub](https://github.com/Nishant-C/whatsnew-app)
  - OSS Tracker : suivi des releases GitHub/GitLab, sans historique d'installation — [F-Droid](https://f-droid.org/en/packages/com.jroddev.android_oss_release_tracker/)

**Détection d'anomalies et alertes**
- Google a créé une métrique « Excessive partial wake locks » réservée aux **développeurs** : sont excessifs ≥ 2 h cumulées de wakelocks partiels en arrière-plan sur 24 h. Au-delà de 5 % des sessions sur 28 jours, la visibilité Play peut être affectée. Les wakelocks audio, localisation et jobs initiés par l'utilisateur sont exemptés — [Android Developers – Excessive partial wake locks](https://developer.android.com/google/play/vitals/excessive-wakelock)
- Depuis mars 2026, les apps qui dépassent ce seuil peuvent recevoir des avertissements sur leur fiche Play et être exclues des recommandations — [Android Developers Blog, mars 2026](https://android-developers.googleblog.com/2026/03/battery-technical-quality-enforcement.html?hl=ru) ; [9to5Google, nov. 2025](https://9to5google.com/2025/11/10/google-play-battery-wake-locks/)
- L'utilisateur final n'a donc qu'une étiquette sur le Store, pas d'alerte sur son propre appareil. Côté apps, les seules options sont anciennes ou partielles :
  - GSam montre « which apps are waking up your device most frequently » ;
  - WhatsRunning a un historique de wakelocks sous Android 11+ ;
  - Wakelock Detector n'est plus maintenu — [androidayuda](https://en.androidayuda.com/Identify-apps-that-wake-the-processor-(wakelocks)-on-Android/)

**Journal plantages / ANR toutes apps**
- LogFox (GPL-3.0, Shizuku/root/ADB) observe les plantages Java/JNI et les ANR des autres apps et envoie des notifications. Dernière version 2.1.10 (mars 2026) — [LogFox GitHub](https://github.com/F0x1d/LogFox) ; [F-Droid](https://f-droid.org/en/packages/com.f0x1d.logfox/) ; [Uptodown](https://logfox.en.uptodown.com/android)
- Limite plateforme : sous Android 13+, une nouvelle lecture logcat par uid d'app est refusée quand l'app n'est pas au premier plan, ce qui gêne la lecture en arrière-plan (constat dans un projet dérivé) — [LogSleuth PR #107](https://github.com/shiaho777/LogSleuth/pull/107)

**Énergie fine et thermique : des API récentes peu exploitées**
- Android 15 (API 35) expose `SystemHealthManager.getSupportedPowerMonitors()` et `getPowerMonitorReadings()`. La classe `PowerMonitor` représente un rail ODPM mesuré ou un consommateur modélisé. Les noms de rails dépendent de l'appareil, et la liste est vide sans ODPM — [PowerMonitor](https://developer.android.com/reference/android/os/PowerMonitor) ; [SystemHealthManager](https://developer.android.com/reference/android/os/health/SystemHealthManager)
- ODPM est disponible sur Pixel 6 et ultérieurs, ce qui inclut en principe le Pixel 8 Pro — [Android Studio Power Profiler](https://developer.android.com/studio/profile/power-profiler)
- Gymnotus (open source) lit ces rails : vue live, dashboard par rail, log CSV en arrière-plan.
  - Sans permission, le rafraîchissement est limité à toutes les 20 s.
  - Avec `ACCESS_FINE_POWER_MONITORS`, accordée par `adb shell pm grant` ou débogage sans fil, il descend à 250 ms.
  - Testé uniquement sur Pixel 10 Pro / Android 17.
  - Les mesures sont au niveau de l'appareil, pas par app, et le système ajoute un léger bruit — [Gymnotus](https://github.com/tengigabytes/Gymnotus)
- Thermique : `PowerManager.getThermalHeadroom(forecastSeconds)` existe depuis l'API 30, avec une prévision de 0 à 60 s. Une valeur ≈ 1.0 correspond à un throttling sévère, et NaN signifie que la fonction est indisponible ou appelée trop souvent. Android 15 ajoute `getThermalHeadroomThresholds()` — [Android Developers – Thermal API](https://developer.android.com/games/optimize/adpf/thermal) ; [Microsoft Learn (binding .NET)](https://learn.microsoft.com/en-us/dotnet/api/android.os.powermanager.getthermalheadroom) ; [commit AOSP getThermalHeadroomThresholds](https://gerrit.omnirom.org/plugins/gitiles/android_frameworks_base/+/7ab922b37262e4ba324198e50962d8660fe3e8b3%5E%21)

**Export de métriques et automatisation**
- L'app compagnon Home Assistant expose des capteurs Android, la plupart désactivés par défaut sauf battery_level, battery_state et charger_type. La doc consultée ne listait le capteur de stockage que pour iOS/macOS — [HA Companion – Sensors](https://companion.home-assistant.io/docs/core/sensors/)
- Tasker intègre Shizuku (annonce beta 6.6.2, juillet 2025 ; notes de 6.6.18 du 05/02/2026) : actions Wi-Fi, Bluetooth, mode avion, Kill App et Run Shell via Shizuku — [Tasker changes 6.6](https://tasker.joaoapps.com/changes/changes6.6.html) ; [r/tasker dev post (miroir)](https://reddit.zackpollard.pro/r/tasker/comments/1lulpiq/dev_tasker_662beta_shizuku_integration/n4hz11g/?context=3)

**Santé batterie**
- Pixel 8 Pro : la page « Battery health » d'Android 16 est réservée au Pixel 8a et aux Pixel 9 et ultérieurs. Elle « will not be available on older in-market Pixel devices » (via extrait de recherche) — [Android Central](https://www.androidcentral.com/apps-software/android-os/google-wont-bring-android-16s-battery-health-page-to-older-pixels)
- Google indique que les Pixel 3 à 8 Pro conservent jusqu'à 80 % de capacité pendant environ 800 cycles, et que les Pixel 8/8 Pro « don't indicate battery health status » — [Pixel Help – Understand your Pixel battery](https://support.google.com/pixelphone/answer/15738128?hl=en)
- Contradiction : un article de la beta Android 16 voyait la page santé apparaître sur Pixel 8 Pro, ce que le déploiement final ne confirme pas — [9to5Google, mars 2025](https://9to5google.com/2025/03/13/android-16-pixel-battery-health-live/)

### Inferences
- **Lacunes les plus nettes**, faisables pour une app personnelle :
  - historique long (semaines/mois) **par app** de la batterie, du cache, des données et du stockage : UsageStats et StorageStatsManager suffisent pour le stockage et le temps d'usage, Shizuku apporte batterystats ;
  - journal unifié installations, mises à jour, désinstallations et changements de permissions, construit par des snapshots périodiques puis un diff ;
  - alertes d'anomalie locales, par exemple en reprenant le seuil Google « 2 h de wakelock partiel en arrière-plan / 24 h » comme heuristique ;
  - historique thermique (headroom + température batterie) corrélé à l'app au premier plan ;
  - export CSV/JSON ;
  - estimation de santé batterie pour le Pixel 8 Pro, à partir des cycles et de la capacité estimée à la charge.
- Le Pixel 8 Pro est **l'un des seuls appareils** où une vue « énergie par sous-système » (CPU big/mid/little, GPU, modem, écran…) est faisable sans root, grâce à ODPM et PowerMonitor. Rare dans l'offre actuelle (Gymnotus seul trouvé), et très pertinent pour le modem Exynos 5300 (voir Q4). Point **à vérifier** : la disponibilité des rails sur le 8 Pro.
- Une partie des demandes (« pourquoi mon téléphone chauffe maintenant ? ») peut être satisfaite en combinant headroom thermique, app au premier plan (UsageStats), rails ODPM et état réseau. Cela donnerait une explication heuristique, pas une vérité causale.

### Gaps
- Temps de démarrage / boot time : aucune source trouvée (ni app, ni demande).
- « Health score » : aucune app Android trouvée qui en propose un de façon crédible. Seul analogue : l'indice de stabilité de Windows (Q5).
- Contenu complet de la table des capteurs Android de Home Assistant non vérifié (page tronquée).
- Disponibilité réelle de `getPowerMonitorReadings` sur Pixel 8 Pro (noms de rails, fréquence) non confirmée : Gymnotus n'a été testé que sur Pixel 10 Pro.
- Non vérifié : qu'Android 16/17 ait restreint l'accès shell à `dumpsys batterystats` ou aux stats par UID (seule affirmation trouvée : un commentaire XDA non sourcé).

## Q4. Quels problèmes spécifiques au Pixel 8 Pro une app de monitoring pourrait-elle aider à diagnostiquer ?

### Takeaway
Les quatre familles de problèmes rapportées sont toutes **difficiles à diagnostiquer avec les outils natifs** :
- **drain lié au modem Exynos 5300** : données mobiles et 5G, signal faible ;
- **chauffe et throttling rapide du Tensor G3** ;
- **régressions après mises à jour** : Android 16 QPR1 en sept. 2025 avec drain au repos de 20-25 % la nuit, mises à jour de mars-avril 2026, chauffe post-Android 17 ;
- **stockage « Système » et IA (AICore) volumineux**.

L'absence de page santé batterie pour le 8 Pro et la casse d'outils tiers à chaque QPR (Shizuku, SD Maid SE) aggravent le tout.

### Cited Findings

**Modem et réseau** (sources de 2023, donc **anciennes** ; persistance en 2025-2026 non vérifiée)
- Les Pixel 8/8 Pro se vident plus vite en données mobiles qu'en Wi-Fi, avec environ 5 h d'écran allumé sur données contre environ 8 h en Wi-Fi selon des utilisateurs — [Android Authority](https://www.androidauthority.com/pixel-8-battery-drain-connectivity-issues-3381576/)
- Les utilisateurs mettent en cause le modem Exynos 5300, le même que sur le Tensor G2 ; Google n'a pas confirmé la cause — [Android Headlines](https://www.androidheadlines.com/2023/11/pixel-8-battery-drain-connectivity-issues.html) ; [Android Central](https://www.androidcentral.com/phones/google-pixel-8-and-8-pro-battery-drain-and-network-issues)
- Le problème touche surtout la 5G ; contournement suggéré : le mode LTE seul — [MobileSyrup](https://mobilesyrup.com/2023/11/01/modem-woes-persist-on-google-pixel-8-series/)
- Les testeurs notent un signal difficile à tenir et de mauvais passages d'un réseau à l'autre. Les modems Snapdragon captent généralement mieux en zone faible — [Droid-Life](https://www.droid-life.com/2023/11/02/and-here-come-the-pixel-8-and-pixel-8-pro-complaints-battery-drain/) ; [Android Police](https://www.androidpolice.com/things-google-fix-pixel-8-series/)
- Un redémarrage règle parfois le drain, qui disparaît aussi parfois seul en quelques jours — [Android Police](https://www.androidpolice.com/5-things-google-got-wrong-with-pixel-8-pro/)

**Thermique Tensor G3** (tests de 2023, **anciens**)
- Throttling rapide en charge soutenue, avec une perte de score d'environ 15 % en 20 minutes. Pics d'environ 43 °C pour le 8 Pro, « un peu plus chaud » que la plupart des concurrents — [Android Authority – Tensor G3](https://www.androidauthority.com/google-tensor-g3-explained-3324692/)
- Digital Trends mesure jusqu'à 40 °C après 20 minutes, throttling compris — [Digital Trends](https://www.digitaltrends.com/phones/google-pixel-8-tensor-g3-chip-good-or-bad-explained/)
- XDA décrit des fréquences qui chutent en environ une minute, de 3 GHz à 1,9 GHz sur le cœur prime (via extrait de recherche, attribution précise à vérifier) — [XDA – Pixel 8 Pro performance](https://www.xda-developers.com/google-pixel-8-pro-performance-evaluation/) ; [Wccftech](https://wccftech.com/pixel-8-pro-tensor-g3-geekbench-6-and-3d-mark-wild-life-benchmarks/)
- Le guide des problèmes d'Android Authority relaie des plaintes de chauffe en usage léger et des pertes allant jusqu'à 30 % de batterie en une heure (≈2023-2024). En test, le site obtient pourtant une journée complète — [Android Authority – problems & fixes](https://www.androidauthority.com/google-pixel-8-pro-problems-fixes-3384863/)
- Selon un réparateur, le 8 Pro utilise des dissipateurs graphite et cuivre sans chambre à vapeur. Source de qualité faible (blog SEO) — [Sydney CBD Repair Centre](https://www.sydneycbdrepaircentre.com.au/blogs/pixel-repairs/google-pixel-8-pro-teardown-thermal-throttling-mystery)

**Régressions logicielles 2025-2026**
- Android 16 QPR1 (sept. 2025) : des propriétaires de Pixel 8 Pro rapportent environ 30 % d'autonomie en moins, un drain de 20-25 % la nuit au repos et des « system apps » qui font tourner le CPU longtemps alors que l'appareil est inactif. Les réinitialisations d'usine donnent des résultats mitigés (via extrait de recherche) — [PiunikaWeb](https://piunikaweb.com/2025/09/30/android-16-qpr1-pixel-battery-complaints/)
- Les mises à jour Pixel de mars et avril 2026 ont donné lieu à des rapports de drain rapide, de chauffe au repos et d'autonomie nettement dégradée. L'article ne cible pas le 8 Pro en particulier (via extrait de recherche) — [Android Police](https://www.androidpolice.com/pixel-phones-getting-better-why-battery-complaints-still-follow/)
- Après une mise à jour majeure (Android 17), chauffe et drain peuvent durer plusieurs jours à cause des traitements en arrière-plan — [How-To Geek](https://www.howtogeek.com/google-pixel-phone-gets-hot-and-has-a-worse-battery-life-after-updates/)
- Des utilisateurs de Pixel 8 Pro ont différé Android 16 par crainte de régressions, quand d'autres n'ont aucun problème. Témoignages anecdotiques — [HardForum](https://hardforum.com/threads/android-16-update.2042490/)
- Les outils tiers cassent eux aussi au rythme des QPR : Shizuku sur Pixel 8 Pro avec Android 16 QPR1 beta ([#1125](https://github.com/RikkaApps/Shizuku/issues/1125)), le vidage de cache de SD Maid SE sur Pixel avec Android 17 ([releases](https://github.com/d4rken-org/sdmaid-se/releases))

**Stockage**
- Un auteur d'Android Police (Pixel 128 Go) ne garde que 96 Go utilisables après le système, les apps d'arrière-plan et les apps préinstallées non supprimables comme AICore. AICore ne stocke rien en cache : vider le cache ne sert à rien, et l'espace remonte vite après un effacement des données, surtout avec des apps d'IA (via extrait de recherche) — [Android Police](https://www.androidpolice.com/how-google-is-cheating-you-with-your-pixels-storage/)
- Estimation avant lancement : 15-20 Go pour l'OS et les apps préinstallées — [Android Authority](https://www.androidauthority.com/google-pixel-8-storage-128gb-opinion-3353219/)
- Le 8 Pro existe en 128 Go, 256 Go, 512 Go et 1 To (UFS 3.1) — [Wikipedia – Pixel 8](https://en.wikipedia.org/wiki/Pixel_8)

**Batterie**
- Aucune page santé batterie pour le 8 Pro (Q3). Google donne seulement le repère « 80 % après environ 800 cycles » — [Pixel Help](https://support.google.com/pixelphone/answer/15738128?hl=en) ; [Android Central](https://www.androidcentral.com/apps-software/android-os/google-wont-bring-android-16s-battery-health-page-to-older-pixels)

### Inferences
Diagnostics à forte valeur pour le Pixel 8 Pro (inférences tirées des problèmes ci-dessus) :
1. **Journal « avant / après mise à jour »** : détecter automatiquement un changement de build (OTA ou QPR) et comparer le drain au repos, la température moyenne et le temps CPU système sur les 7 jours avant et après. Cela répond directement aux plaintes « depuis la mise à jour X ».
2. **Drain nocturne** : mesurer le % perdu écran éteint par nuit, avec ventilation par wakelocks et alarmes via Shizuku/batterystats, et alerter au-delà d'un seuil (ex. > 5 %/nuit).
3. **Corrélation modem** : enregistrer le type de réseau (5G, LTE, Wi-Fi), la force du signal et le drain. Le rail modem ODPM, si exposé, permettrait de quantifier ce que coûtent la 5G et un signal faible, et de proposer « passer en LTE ici ».
4. **Historique thermique** : headroom et température batterie toutes les N minutes, épisodes de throttling horodatés, avec l'app au premier plan et l'état de charge (charge + usage = chauffe).
5. **Suivi du stockage « Système / IA »** : historiser l'empreinte d'AICore et des apps système, et la tendance globale.
6. **Santé batterie maison** : estimer la capacité à partir des sessions de charge, à la manière d'AccuBattery, et suivre les cycles si l'API les expose.

### Gaps
- Pas de preuves 2025-2026 propres au 8 Pro pour le modem (toutes les sources datent de fin 2023).
- Les rapports PiunikaWeb et Android Police n'ont pu être lus qu'en extrait. Aucune reconnaissance officielle de Google pour un bug Android 16/17 sur 8 Pro n'a été trouvée.
- Recherches non faites (quota épuisé) : les cycles de charge sont-ils lisibles sur le 8 Pro (BatteryManager / « Battery information »), et existe-t-il des problèmes de gonflement de batterie sur le 8 Pro en 2026 ?
- La page vendeur Kryozon (« Smart Data Sensing » comme coupable de chauffe) a été écartée : contenu commercial peu fiable.

## Q5. Quelles idées d'autres écosystèmes (Windows, macOS, Linux, Netdata, iOS) manquent à Android, et sont-elles faisables (sans privilège / Shizuku / root) ?

### Takeaway
Cinq idées transposables :
- le **journal de fiabilité** et l'indice de stabilité de Windows (Reliability Monitor) ;
- la **détection d'anomalies locale par métrique** de Netdata, légère et sans cloud ;
- les **widgets permanents et graphes d'historique** de macOS Stats / iStat Menus ;
- l'**export standard** de Glances (CSV/InfluxDB/Prometheus) ;
- le **rapport réseau par app** d'iOS (App Privacy Report, domaines contactés sur 7 jours).

Sur Android, la plupart sont faisables en local. Certaines exigent Shizuku (batterystats, plantages, logcat) ou se heurtent à des limites de plateforme : CPU par app, slot VPN unique pour l'inspection réseau.

### Cited Findings
- **Windows Reliability Monitor** :
  - Indice de stabilité de 1 à 10 calculé par moyenne mobile pondérée, sur une timeline journalière ou hebdomadaire.
  - Il recense plantages d'apps, défaillances Windows, erreurs matérielles et mises à jour échouées, et s'exporte (« Save reliability history ») ou se lit via WMI (Win32_ReliabilityStabilityMetrics / Win32_ReliabilityRecords) — [Microsoft – System Stability Index](https://learn.microsoft.com/it-it/previous-versions/windows/it-pro/windows-server-2008-R2-and-2008/cc749032(v=ws.10)) ; [ElevenForum](https://www.elevenforum.com/t/view-reliability-history-in-windows-11.5791/) ; [TweakTown](https://www.tweaktown.com/guides/11399/i-stopped-guessing-why-my-pc-crashes-after-i-started-using-this-built-in-windows-tool/index.html)
  - Critique : l'indice est arbitraire, et son vrai intérêt est de **dater** le début d'un problème — [MalwareTips](https://malwaretips.com/threads/the-reliability-monitor-is-a-nifty-hidden-tool-to-track-your-pc-health.139072/)
- **Netdata** :
  - k-means par métrique avec 18 modèles sur des fenêtres de 6 h décalées de 3 h ; un point n'est anormal que si tous les modèles sont d'accord.
  - Le bit d'anomalie est stocké avec la métrique, le traitement se fait « at the edge… no cloud dependency », pour environ 18 Ko de RAM par métrique — [Netdata ML docs](https://learn.netdata.cloud/docs/ai-&-ml/ml-anomaly-detection)
  - Exemple d'alerte : taux d'anomalie supérieur à 1 % sur une minute — [Netdata blog](https://blog.netdata.cloud/our-first-ml-based-anomaly-alert)
  - Limites : rate les anomalies subtiles et finit par traiter comme « normaux » des motifs anormaux récurrents — [Netdata ML accuracy](https://learn.netdata.cloud/docs/ai-&-ml/ml-anomaly-detection/ml-accuracy)
- **macOS Stats (exelban, MIT)** :
  - Barre de menus avec CPU, GPU, mémoire, disque, réseau, batterie et capteurs (température, tension, puissance) — [project-awesome Stats](https://project-awesome.org/r/exelban-stats)
  - Graphes historiques selon une fiche tierce — [AlternativeTo Stats](https://alternativeto.net/software/stats/about)
- **Glances (Linux)** :
  - Exporte en CSV (`--export csv`), vers InfluxDB 2 (`--export influxdb2`) et vers Prometheus (serveur HTTP via `prometheus_client`) — [Glances docs InfluxDB](https://glances.readthedocs.io/en/latest/gw/influxdb.html) ; [aide-mémoire Glances](https://lzw.me/x/reference/docs/glances.html)
  - Des utilisateurs doivent lui adjoindre un « process-exporter » pour le suivi par processus — [Glances #794](https://github.com/nicolargo/glances/issues/794)
- **iOS App Privacy Report** :
  - Domaines contactés par chaque app (directement ou via du contenu web) sur les 7 derniers jours, vue inverse domaine → apps, et « Most Contacted Domains » — [Apple Support 102188](https://support.apple.com/102188)
  - Il existe des outils tiers d'analyse de l'export — [App Privacy Report Analyzer](https://apps.apple.com/us/app/app-privacy-report-analyzer/id6759659732)
- **Outils Android « développeurs »** équivalents mais inadaptés au grand public :
  - Battery Historian, pour la consommation dans le temps — [Android Developers](https://developer.android.com/topic/performance/power/battery-historian)
  - Perfetto, pour les compteurs GPU et énergie — [Android Performance – Perfetto 16](https://androidperformance.com/en/2026/05/04/Android-Perfetto-16-GPU-Power-Counters/)
- **Faisabilité sur Android** :
  - Le CPU par app des autres processus est bloqué pour une app normale depuis Android 7/8, et un module Magisk existe pour lever la restriction en root — [AnotherMonitor](https://github.com/AntonioRedondo/AnotherMonitor) ; [magisk.dev](https://magisk.dev/modules/noprocstatrestriction/)
  - L'inspection réseau par app passe par le slot VPN unique — [GrapheneOS forum](https://discuss.grapheneos.org/d/16993-grapheneos-appverse-a-curated-collection-of-privacy-respecting-apps/39)
  - Les rails ODPM sont lisibles sans root, avec une permission accordable par ADB — [Gymnotus](https://github.com/tengigabytes/Gymnotus)
  - Les plantages et ANR exigent Shizuku, root ou ADB — [LogFox](https://github.com/F0x1d/LogFox)
- **Risque de distribution d'une app sideloadée** :
  - Google exigera la vérification du développeur, y compris pour les apps sideloadées : dès septembre 2026 au Brésil, en Indonésie, à Singapour et en Thaïlande, puis au niveau mondial en 2027.
  - Prévus : des comptes gratuits de « distribution limitée » pour amateurs et étudiants, et un « advanced install flow » pour installer des apps non vérifiées.
  - Aucune source ne confirme une exemption formelle pour l'installation par ADB — [Android Central](https://www.androidcentral.com/apps-software/sideloading-apps-onto-android-phones-from-random-sources-wont-be-an-option-starting-in-2026) ; [Engadget](https://engadget.com/apps/android-will-hide-app-sideloading-behind-a-new-one-time-security-process-184651171.html) ; [T3](https://www.t3.com/tech/phones/experienced-android-phone-owners-might-still-be-able-to-sideload-unverified-apps)

### Inferences
Transpositions proposées, du plus faisable au moins faisable. Ce sont des inférences de conception, **non sourcées** comme fonctionnalités existantes :
- **« Reliability Monitor » Android** : timeline journalière qui superpose plantages et ANR (Shizuku : dropbox/logcat, à la LogFox), redémarrages, mises à jour système et apps, installations et désinstallations, changements de permissions, épisodes thermiques et nuits à fort drain. Un « indice » éventuel resterait **indicatif** ; la valeur principale est de dater le début d'un problème (leçon de la critique MalwareTips).
- **Anomalies à la Netdata, version légère** : sur un téléphone, une ligne de base par métrique et par créneau horaire (médiane + écart absolu médian, MAD) suffit probablement à la place du k-means. Exemples : drain par heure écran éteint, croissance du cache par app, octets de données d'arrière-plan, temps de wakelock. Exécution périodique avec WorkManager. Le seuil Google « 2 h de wakelock / 24 h » peut servir de règle dure complémentaire.
- **« Menu bar » à la Stats/iStat** : tuiles Réglages rapides, widget et notification persistante (température, headroom, puissance instantanée ODPM, débit réseau), avec mini-sparklines.
- **Export à la Glances** : CSV/JSON locaux, et en option un endpoint Prometheus local sur le LAN ou un push MQTT/REST vers Home Assistant. Tout est désactivé par défaut pour rester « local-first ».
- **App Privacy Report à l'iOS** : faisable seulement en occupant le slot VPN, en conflit avec NetGuard/RethinkDNS ou un vrai VPN. Une alternative consiste à importer les journaux de RethinkDNS ou NetGuard s'ils sont exportables (à vérifier).
- **Hors de portée sans root** : un « Task Manager » CPU temps réel par app, comme Windows ou htop, à cause de SELinux. Les approximations possibles passent par batterystats (temps CPU par UID) ou `dumpsys cpuinfo` / `top` via Shizuku (non vérifié ici).
- Pour un usage personnel en France, la vérification des développeurs ne devrait concerner que le déploiement mondial de 2027, selon le calendrier rapporté. Il faut néanmoins prévoir un plan B : compte de distribution limitée, ou installation par ADB si elle reste tolérée.

### Gaps
- Fonctions détaillées d'iStat Menus, de macOS Activity Monitor (« Energy Impact », historique 12 h) et de l'onglet « Historique des applications » du Gestionnaire des tâches Windows : non sourcées dans cette session (quota épuisé). À vérifier avant de les citer.
- iOS : l'écran batterie d'iOS, avec historique par app sur 10 jours et activité en arrière-plan, n'a pas pu être sourcé (quota de recherche épuisé).
- btop/htop : aucune source récupérée.
- Fils Hacker News sur « Android n'a pas d'Activity Monitor » : non récupérés (hn.algolia.com bloqué, quota épuisé).

## Q6 (objectif). Quelles lacunes prioriser pour une app de monitoring personnelle sur Pixel 8 Pro ?

### Takeaway
Les manques à la fois **documentés par les utilisateurs** et **techniquement atteignables** sur Pixel 8 Pro non rooté, avec Shizuku en option, sont les suivants :
1. historique long terme par app (batterie, stockage et cache, données) ;
2. journal unifié « ce qui a changé » (apps, permissions, mises à jour, plantages) ;
3. alertes d'anomalie locales (drain nocturne, wakelocks, croissance du stockage, chauffe) ;
4. diagnostic thermique, énergétique et modem propre au Tensor G3, via ODPM et le thermal headroom ;
5. comparaison automatique avant/après mise à jour ;
6. export local (CSV/JSON, et en option Home Assistant ou Prometheus) ;
7. supervision de Shizuku lui-même.

Les couvertures partielles existent, mais dans 6 ou 7 apps distinctes : SD Maid SE, BatStats, Gymnotus, LogFox, Tasker, Home Assistant et le Privacy Dashboard.

### Cited Findings
Tableau de synthèse : chaque ligne renvoie aux preuves des sections précédentes.

| Lacune | Preuve de la demande / douleur | Couverture partielle existante | Faisabilité Pixel 8 Pro |
|---|---|---|---|
| Historique batterie par app sur plusieurs semaines | Stats Pixel limitées à « depuis la dernière charge » ([Pixel Help](https://support.google.com/pixelphone/answer/2819525?hl=en), [Issue 262066100](https://issuetracker.google.com/issues/262066100)) ; BBS archivé, AccuBattery jugé insuffisant ([XDA](https://xdaforums.com/t/looking-for-an-alternative-to-betterbatterystats-or-gsam-battery-monitor.4746711/)) | BatStats via Shizuku ([AlternativeTo](https://alternativeto.net/software/batstats/about)), GSam ([APKMirror](https://www.apkmirror.com/apk/gsam-labs/gsam-battery-monitor/gsam-battery-monitor-3-46-release/gsam-battery-monitor-3-46-android-apk-download/)) | Shizuku requis pour une ventilation fiable |
| Tendance stockage et cache par app | Statistiques demandées sur SD Maid SE ([#154](https://github.com/d4rken-org/sdmaid-se/issues/154)) ; stockage « IA » qui regonfle ([Android Police](https://www.androidpolice.com/how-google-is-cheating-you-with-your-pixels-storage/)) | Tendance **globale** + « jours avant saturation » dans SD Maid SE ([releases](https://github.com/d4rken-org/sdmaid-se/releases)) | Sans privilège (permission d'accès aux données d'usage) |
| Journal d'activité, permissions, installations et mises à jour | [AppManager #143](https://github.com/MuntashirAkon/AppManager/issues/143), [#382](https://github.com/MuntashirAkon/AppManager/issues/382), [Hail #388](https://github.com/aistra0528/Hail/issues/388) | Privacy Dashboard 7 j, 3 permissions seulement ([9to5Google](https://9to5google.com/2024/12/10/android-privacy-dashboard-7-day-history/)) ; apps de changelog éparses ([gHacks](https://www.ghacks.net/2017/08/01/keep-track-of-android-app-updates-with-changes/)) | Sans privilège pour installations et mises à jour ; Shizuku pour les app ops |
| Alertes d'anomalie (wakelocks, drain nocturne) | Utilisateur XDA incapable d'identifier les wakelocks sans root ([XDA](https://xdaforums.com/t/identifying-high-frequency-wake-locks-on-android-13-without-root.4754880/)) ; drain nocturne de 20-25 % après Android 16 QPR1 ([PiunikaWeb](https://piunikaweb.com/2025/09/30/android-16-qpr1-pixel-battery-complaints/)) | Seuil Google côté développeurs seulement ([Android Developers](https://developer.android.com/google/play/vitals/excessive-wakelock)) | Shizuku (batterystats) |
| Plantages et ANR toutes apps | Demandes de log sur App Manager ([#755](https://github.com/MuntashirAkon/AppManager/issues/755)) | LogFox ([GitHub](https://github.com/F0x1d/LogFox)) | Shizuku, root ou ADB |
| Énergie par sous-système (modem, CPU, GPU, écran) | Drain modem Exynos 5300 ([Android Authority](https://www.androidauthority.com/pixel-8-battery-drain-connectivity-issues-3381576/)) | Gymnotus ([GitHub](https://github.com/tengigabytes/Gymnotus)) | API 35 sans root ; permission ADB pour 250 ms ; disponibilité sur 8 Pro à vérifier |
| Historique thermique et throttling | Chauffe du Tensor G3 ([Android Authority](https://www.androidauthority.com/google-tensor-g3-explained-3324692/)) | Aucune app trouvée qui historise le headroom | API 30 sans privilège ([Thermal API](https://developer.android.com/games/optimize/adpf/thermal)) |
| Santé batterie du 8 Pro | Page santé absente sur 8/8 Pro ([Pixel Help](https://support.google.com/pixelphone/answer/15738128?hl=en)) | AccuBattery (précision contestée) ([Android Central](https://forums.androidcentral.com/threads/how-accurate-is-accubattery.1042146/)) | Estimation locale à partir des charges |
| Supervision de Shizuku (état, relance) | [#1478](https://github.com/RikkaApps/Shizuku/issues/1478), [#294](https://github.com/RikkaApps/Shizuku/issues/294), [Porter](https://github.com/d4rken-org/porter) | Démarrage auto sur Wi-Fi de confiance ([releases](https://github.com/RikkaApps/Shizuku/releases)) ; Tasker ne lit pas l'état de Shizuku ([r/tasker](https://redlib.hackliberty.org/r/tasker/comments/1l1ynue/how_to_utilize_shizuku_to_run_adb_shell_commands/mx8s8el)) | Sans privilège (binder ping), plus une notification |
| Export (CSV/JSON, HA, Prometheus) | [Canta #358](https://github.com/samolego/Canta/issues/358), [AppManager #1937](https://github.com/MuntashirAkon/AppManager/issues/1937), [SD Maid SE #2834](https://github.com/d4rken-org/sdmaid-se/issues/2834) | HA Companion (capteurs limités) ([docs](https://companion.home-assistant.io/docs/core/sensors/)) ; CSV de Gymnotus | Sans privilège |

### Inferences
- Le positionnement le plus différenciant n'est pas un « cleaner » de plus. C'est un **observatoire local** (historique + journal + anomalies + explications) qui **délègue les actions** à des outils éprouvés : SD Maid SE pour nettoyer, Hail ou Canta pour geler et désinstaller, Tasker pour automatiser. On évite ainsi la fragilité de l'accessibilité et le travail de maintenance qui use les petits projets FOSS (Canta, Hail).
- Ordre de priorité suggéré pour un MVP, du plus utile et faisable au plus coûteux :
  1. collecte périodique sans privilège : niveau et température batterie, headroom, stockage par app, usage, réseau ;
  2. journal des installations et mises à jour, plus la détection de changement de build ;
  3. rapport « drain nocturne » et comparaison avant/après mise à jour ;
  4. couche Shizuku : batterystats, wakelocks, plantages et ANR, avec supervision de son état ;
  5. rails ODPM ;
  6. export.
- La règle de conception « tout fonctionne sans Shizuku, Shizuku enrichit » découle directement de la fragilité documentée de Shizuku sous Android 16 et 17.

### Gaps
- Aucune validation quantitative du poids de chaque lacune : les votes GitHub sont faibles et Reddit était inaccessible.
- Pas de vérification de l'exposition, sur Pixel 8 Pro sous Android 16/17, des API clés : rails PowerMonitor, cycles de charge via BatteryManager, accès shell à batterystats et dropbox.
- Les recherches complémentaires (iOS 10 jours, HN, DevCheck, Pixel 8 Pro 2026) restent à faire dans une session ultérieure : quota épuisé dans celle-ci.
