# Batterie & énergie : applis de monitoring/gestion Android et fonctions natives Pixel (état octobre 2026, cible Pixel 8 Pro)

> Note méthodo : recherche faite le 2026-10-28. L'accès direct (fetch) était **bloqué** dans cette session pour support.google.com, play.google.com, 9to5google.com, androidauthority.com, androidpolice.com, apkmirror.com, perfetto.dev et accubattery.zendesk.com. Pour ces domaines, les faits viennent des extraits/résumés du moteur de recherche (WebSearch), pas d'une lecture complète de la page. Ont été lus directement : developer.android.com (diffs d'API), github.com (BetterBatteryStats, Battery Historian, ACC, DrainScope, Pixel-8-Battery-Health). Les infos de version/date tirées de miroirs d'APK sont indicatives.

---

## Q1. Quelles applis font référence en 2025-2026 ? Développeur, prix, open source, niveau d'accès, maintenance

### Takeaway
Le paysage est **vieillissant** : plusieurs références historiques sont abandonnées ou archivées (BetterBatteryStats archivé en mars 2026, Battery Historian archivé fin 2022, Kaspersky Battery Life arrêté en 2022, GSam sans release notable depuis fin 2022, AccuBattery peu mis à jour depuis mi-2024). Les applis encore actives en 2025-2026 sont surtout Battery Guru, Battery Widget Reborn, Ampere, Brevent, plus une vague de petits projets open source/GitHub qui s'appuient sur **Shizuku/ADB** (BATTERY_STATS, DUMP, PACKAGE_USAGE_STATS) pour lire `dumpsys batterystats` et les API santé d'Android 14+.

### Cited Findings

**AccuBattery (Digibites)** — monitoring santé/usure, freemium
- Dernière version repérée sur APKMirror : 2.1.6, mise en ligne le 20 juin 2024 (Android 7.0+, API 24) ; la 2.1.4 ciblait Android 13 (API 33) ; aucune trace fiable d'une 2.1.8 officielle ni de mention Android 15 dans les changelogs — [APKMirror AccuBattery](https://apkmirror.com/apk/digibites/accubattery) (via résultats de recherche). Des sites tiers de "mods" listent une 2.1.8 (non fiable) — [DroidCove](https://droidcove.com/accubattery-battery-health-pro/).
- Premium = achat in-app (prix non indiqué sur l'aide officielle) — [AccuBattery Help Center](https://accubattery.zendesk.com/hc/en-us/articles/209507129-What-do-I-get-with-AccuBattery-premium). Prix contradictoires : 3,99 $ dans une vieille revue vidéo — [YouTube](https://www.youtube.com/watch?v=YgC0mfRoTso) ; 19,99 $ "in-app purchase" selon un site APK peu fiable — [DroidCove](https://droidcove.com/accubattery-battery-health-pro/). Prix Play actuel non vérifié (play.google.com bloqué).
- Une version iOS "AccuBattery - Battery Info" existe sur l'App Store (certaines fonctions Pro y sont marquées "Coming Soon") — [App Store](https://apps.apple.com/sa/app/accubattery-battery-info/id6737747779).
- Accès : aucune permission spéciale (fonctionne avec les API publiques) — implicite dans les listings ; pas open source.

**Battery Guru (Paget96)** — monitoring santé/charge, très actif
- Version officielle la plus récente repérée : 2.5.0 (703), datée du 2 juin 2026 — [APKMirror Battery Guru](https://www.apkmirror.com/apk/paget96/battery-guru-health-saver/) (via résultats de recherche) ; [APKCombo](https://apkcombo.com/battery-guru/com.paget96.batteryguru/).
- Gratuit avec Pro optionnel ; Pro débloquable temporairement en regardant une pub récompensée (24 ou 48 h, illimité) ; Pro = sans pub, historique complet des sessions, overlays avancés ; fonctions : santé, température, charge, widget niveau+température, overlay déplaçable, niveau des appareils Bluetooth, alarmes, "always-on display" — [APKCombo](https://apkcombo.com/battery-guru/com.paget96.batteryguru/) ; [APKMirror](https://www.apkmirror.com/apk/paget96/battery-guru-health-saver/). Prix exact du Pro non trouvé.

**GSam Battery Monitor / Pro (GSam Labs)** — stats par appli, wakelocks
- Pro : 2,49 USD sur Google Play, listing mis à jour le 3 janvier 2026 selon un agrégateur ; un listing grec plus ancien donnait 2,69 € et une MAJ en juillet 2021 — [AppFollow](https://apps.appfollow.io/android/gsam-battery-monitor-pro/com.gsamlabs.bbm.pro?country=gr) ; [AppPricingLab](https://apppricinglab.com/app/google_play/com.gsamlabs.bbm.pro).
- Dernière version repérée : 3.45 (28 nov. 2022) sur APKMirror ; une 3.46 non datée apparaît ailleurs — [APKMirror GSam](https://www.apkmirror.com/apk/gsam-labs/gsam-battery-monitor) ; [Softpedia](https://mobile.softpedia.com/apk/gsam-battery-monitor/). **Statut : quasi figé depuis 2022.**
- Accès : depuis KitKat les stats par appli exigent BATTERY_STATS ; "Root Companion" (copie dans /system/priv-app, root, ~2015) ou, depuis Lollipop, `adb shell pm grant com.gsamlabs.bbm android.permission.BATTERY_STATS` (package `com.gsamlabs.bbm.pro` pour la Pro) ; un utilisateur rapporte que ça fonctionne sur Pixel 6 Pro Android 12 non rooté, un autre a eu une SecurityException — [GitHub GSam Root Companion](https://github.com/GSamLabs/GSamBatteryMonitor-RootCompanion) ; [XDA](https://xdaforums.com/t/is-gsam-battery-nag-or-pro-app-working-enable-more-stats.4365587/) ; [GSam Users Guide](https://blogger.gsamlabs.com/2011/11/badass-battery-monitor-users-guide.html).

**BetterBatteryStats (Sven Knispel / asksven)** — wakelocks, alarmes, kernel wakelocks
- Dépôt GitHub **archivé par le propriétaire le 15 mars 2026** (lecture seule) ; licence Apache 2.0 ; 669 étoiles, 101 issues ouvertes — [GitHub asksven/BetterBatteryStats](https://github.com/asksven/BetterBatteryStats).
- Accès historique : `adb -d shell pm grant com.asksven.betterbatterystats android.permission.BATTERY_STATS` + `... android.permission.DUMP` — [APKMirror BBS release notes](https://www.apkmirror.com/apk/sven-knispel/betterbatterystats) (via résultats de recherche). Disponible aussi sur F-Droid — [F-Droid](https://f-droid.org/packages/com.asksven.betterbatterystats).

**Battery Historian (Google)** — outil développeur (PC)
- Dépôt **archivé le 29 déc. 2022** ; Apache 2.0 ; supporte Android 5.0+ ; analyse un `adb bugreport` (timeline des événements système/app, stats agrégées depuis la dernière charge complète, métriques par app, comparaison A/B de deux bugreports, overlay power monitor et wakeup sources kernel) ; Docker `gcr.io/android-battery-historian/stable:3.0` — [GitHub google/battery-historian](https://github.com/google/battery-historian).

**Ampere (Braintrapp / gombosdev)** — courant de charge/décharge en direct
- Mesure le courant (négatif sur batterie ; sur secteur, affiche ce qui reste pour charger après la consommation du téléphone) ; valeur moyennée sur 50 mesures, peut être instable ou nulle ; certains appareils (ex. Samsung S5) ne donnent pas de vraie mesure ; Pro = widgets, notification, alertes sur l'appareil et Wear OS ; Android 6.0+ — [Google Play Ampere](https://play.google.com/store/apps/details?id=com.gombosdev.ampere&hl=en_CA) (via résultats de recherche) ; [APKMirror Ampere](https://www.apkmirror.com/apk/braintrapp/ampere/).
- Version : v4.35.9 (août 2025) sur APKMirror ; d'autres sites listent 4.36.1 / 4.37.0 — [APKMirror](https://www.apkmirror.com/apk/braintrapp/ampere/ampere-v4-35-9-release/) ; [AndroidAPKsFree](https://androidapks.com/ampere/com-gombosdev-ampere/).

**Battery Widget Reborn (Tomas Hubalek, Tchéquie)** — widgets/notification, actif
- Releases 2025 : 5.0.3 (15 avr.), 5.0.12 (22 mai), 5.1.0 (29 juin), 5.1.4 (29 juil.), 5.2.7 (1er déc. 2025) ; plus récente repérée 5.3.8 (refactoring/bug fixes), MAJ ~juin 2026 selon Cafe Bazaar (dates incohérentes entre sources) — [APKMirror BWR](https://www.apkmirror.com/apk/tomas-hubalek/battery-widget-reborn-free) ; [APKMirror 5.3.8](https://www.apkmirror.com/apk/tomas-hubalek/battery-widget-reborn-free/battery-widget-reborn-5-3-8-free-release/) ; [Cafe Bazaar](https://cafebazaar.ir/app/net.hubalek.android.reborn.beta?l=en). Existe en version "Pro" séparée (`net.hubalek.android.apps.reborn.pro`) — [AppGoblin](https://appgoblin.info/apps/net.hubalek.android.apps.reborn.pro).

**Kaspersky Battery Life** — **abandonné**
- Développement arrêté en octobre 2022 en tant qu'appli séparée ; fonctions fusionnées dans l'appli "Kaspersky for Android" — [Kaspersky Support](https://support.kaspersky.com/battery-life/tr13/244258) ; [Kaspersky blog](https://www.kaspersky.com/blog/kaspersky-battery-life/17849/).

**Naptime (Francisco Franco)** — Doze agressif
- Force Doze juste après extinction de l'écran ; root, ou sans root via ADB `pm grant` de DUMP et WRITE_SECURE_SETTINGS ; dernière version repérée 8.4.1 (date non visible) — [APKMirror Naptime](https://www.apkmirror.com/apk/francisco-franco/naptime-doze-mode-improved) ; [AndroidAyuda](https://androidayuda.com/mejora-la-autonomia-bateria-android-sin-root/).

**Greenify (Oasis Feng)** — hibernation d'apps
- Le changelog Play mentionne le support d'Android 15 et un correctif de l'hibernation par accessibilité sur Android 14+ — [Google Play Greenify](https://play.google.com/store/apps/details?id=com.oasisfeng.greenify&hl=en) (via résultats de recherche). Versions : 4.7.8 (11 mars 2025, Uptodown), 5.1.1 (20 mars 2025, Malavida) — [Uptodown](https://greenify.en.uptodown.com/android) ; [Malavida](https://www.malavida.com/en/soft/greenify/android/). Un article estime qu'il n'est plus en développement actif et que son utilité a baissé avec Doze/App Standby — [MakeUseOf](https://www.makeuseof.com/greenify-android-battery-management/).

**Brevent (Jianyu Studio)** — app standby / force-stop sans root (via ADB/debug)
- 4.2.30 mise en ligne le 24 juin 2026 ; 4.2.29.1 (janv. 2026) cible Android 15 (API 35) ; 4.2.26 (juin 2025) "use android 16 sdk" ; 4.2.27 (sept. 2025) "fix crash on latest Android" ; 4.2.25 : met en standby les apps lancées puis quittées ; ne fonctionne pas si le débogage est coupé — [APKMirror Brevent](https://www.apkmirror.com/apk/jianyu-studio/brevent/brevent-4-2-25-release) ; [GitHub betaxab/Brevent](https://github.com/betaxab/Brevent).

**ACC – Advanced Charging Controller (VR25)** — contrôle de charge (root)
- Root obligatoire (Magisk, KernelSU, SuperSU) ; pause/reprise de charge (défauts 75 %/70 %), "idle mode" (= bypass, natif selon appareil ou émulé), limite de courant 0–9999 mA (selon kernel), limite de tension 3700–4300 mV (très rare), seuils de température (cooldown 45 °C, pause 50 °C, reprise 40 °C, arrêt 55 °C), planification, auto-shutdown 5 %, profils, plugins ; GPL-3.0 ; 2,4k étoiles, copyright "2017-2024" ; note Pixel : forcer la charge sans fil rapide via `apply_on_plug=wireless/voltage_max::9000000` "may not work on all Pixel devices" — [GitHub VR-25/acc](https://github.com/VR-25/acc).

**Battery Charge Limit / forks** — root
- Le fork "BatteryChargeLimiter" exige root ; l'auteur indique qu'il "n'est pas possible de contrôler la charge sans root" (une solution par alarme est envisagée) — [GitHub MuntashirAkon/BatteryChargeLimiter](https://github.com/MuntashirAkon/BatteryChargeLimiter).
- "Charge Control: Battery Limit" propose un mode sans root qui pilote une **prise connectée** ; les fonctions dépendent des contrôles exposés par le kernel ; un blog 2024 rapporte que le mode root ne marchait pas sur un Pixel 6 Pro — [Google Play Charge Control](https://play.google.com/store/apps/details?id=com.rhs.ccontrol&hl=en) (via résultats de recherche) ; [billauer.se](https://billauer.se/blog/2024/05/battery-charge-limit-google-pixel/).

**Applis "API santé Android 14+" (Batt, aBattery)**
- Batt (`com.porg.batt`) : affiche cycles et statut de charge par défaut ; avec BATTERY_STATS (Shizuku ou `adb shell pm grant com.porg.batt android.permission.BATTERY_STATS`) affiche santé, date de fabrication, date de première utilisation ; requiert Android 14 ; listé comme open source GPL-3.0 (dépôt non confirmé) — [AlternativeTo Batt](https://alternativeto.net/software/batt/about) ; [Android Authority](https://www.androidauthority.com/android-14-battery-health-check-3331255/).
- aBattery (Google Play) utilise les mêmes API Android 14 (cycles, date de fabrication…) et supporte Shizuku pour éviter le PC — [Mishaal Rahman sur X](https://x.com/MishaalRahman/status/1699180507664146901).

**Nouvelle vague de projets GitHub (2025-2026), Shizuku/ADB**
- "Pixel 8 Battery Health" (`dev.rubiks.batterybuddy`) : APK seul (source non publiée), bannière AdMob ; grants ADB `BATTERY_STATS`, `DUMP`, `PACKAGE_USAGE_STATS` ; BATTERY_STATS débloque "Android's health score, manufacture date, first-use date", DUMP+PACKAGE_USAGE_STATS débloquent la "capacité mesurée par Android sur de vraies charges" ; sans permissions → estimation ; testé sur **Pixel 8 sous Android 17** ; capture : 92 %, 4 172/4 550 mAh, 291 cycles ; Pixel 8 Pro non testé — [GitHub AhmedVohra/Pixel-8-Battery-Health](https://github.com/AhmedVohra/Pixel-8-Battery-Health).
- DrainScope : conso par app via `dumpsys batterystats` lu par Shizuku (uid 2000 shell), sinon estimation via UsageStats (~8 mAh/min de premier plan) ; ventilation écran (luminosité, taux de rafraîchissement), Wi-Fi, cellulaire (signal), GPS, Bluetooth, CPU, wakelocks/wakeups ; cycle count firmware si publié ; Apache-2.0, minSdk 24, compilé SDK 36 ; 2 étoiles (projet confidentiel) — [GitHub santo1993/drainscope](https://github.com/santo1993/drainscope).
- VoltCore : mA/W/V en direct, santé, conso par app, historiques ; indique que le grant USB de DUMP + PACKAGE_USAGE_STATS débloque les estimations par app (le toggle "Accès aux données d'utilisation" ne suffit pas) — [GitHub ABDURREHMAN629/voltcore](https://github.com/ABDURREHMAN629/voltcore).
- S23 Optimizer exécute `dumpsys batterystats --charged` via Shizuku + UsageStats — [GitHub muhib-karim/s23-optimizer](https://github.com/muhib-karim/s23-optimizer).

### Inferences
- **Abandonnés / à signaler** : Kaspersky Battery Life (2022), Battery Historian (archivé 2022), BetterBatteryStats (archivé 03/2026), GSam (pas de release identifiée après 2022), AccuBattery (dernière release identifiée juin 2024, soit >2 ans).
- **Actifs** : Battery Guru (2026), Battery Widget Reborn (2025-2026), Brevent (2026), Ampere (2025), Greenify (2025, maintenance minimale).
- Il n'existe **pas** d'appli grand public active, bien maintenue, qui combine (a) API santé Android 14+ via BATTERY_STATS, (b) `dumpsys batterystats` par app via Shizuku, (c) historique long + export. Ce créneau n'est occupé que par de petits projets GitHub récents.

### Gaps
- Prix Play actuels (AccuBattery Pro, Battery Guru Pro, Ampere Pro, BWR Pro, GSam Pro) non vérifiables : play.google.com bloqué.
- Statut AccA (front-end ACC) : aucune source trouvée.
- Date exacte de Naptime 8.4.1 et compatibilité Android 16 de Greenify/Naptime : non trouvées.
- Aucune confirmation d'un dépôt GitHub public pour Batt.

---

## Q2. Couverture fonctionnelle détaillée (gratuit vs payant)

### Takeaway
Les mesures "physiques" en direct (mA, W, V, °C, niveau, statut) sont partout et gratuites ; ce qui fait la différence ce sont (1) l'**estimation de capacité/santé** et l'**usure par session** (AccuBattery, Battery Guru), (2) la **conso par app/wakelocks** qui exige BATTERY_STATS/DUMP (GSam, BBS, DrainScope), (3) le **contrôle réel de la charge** qui exige root (ACC). Les fonctions payantes sont surtout de confort : historique long, overlays, widgets/notifications riches, thèmes, sans pub.

### Cited Findings
- **AccuBattery – gratuit** : usure par session de charge, vitesse de décharge et conso par app, estimations écran allumé/éteint, capacité réelle mesurée en mAh, temps de charge/usage restant, % de deep sleep ; **Pro** : overlay temps réel CPU/puissance, historique des sessions de plus d'un jour, stats détaillées en notification, thèmes sombre/AMOLED, sans pub — [AccuBattery Help Center](https://accubattery.zendesk.com/hc/en-us/articles/209507129-What-do-I-get-with-AccuBattery-premium) (via résultats de recherche) ; [App Store listing](https://apps.apple.com/sa/app/accubattery-battery-info/id6737747779).
- **Battery Guru – gratuit** : santé, température, charge, widget, overlay, niveaux Bluetooth, alarmes ; **Pro** : sans pub, historique complet des sessions, overlays avancés (débloquable par pubs récompensées 24/48 h) — [APKCombo](https://apkcombo.com/battery-guru/com.paget96.batteryguru/).
- **Ampere – gratuit** : courant charge/décharge ; **Pro** : widgets, notification, alertes appareil et Wear OS — [Google Play Ampere](https://play.google.com/store/apps/details?id=com.gombosdev.ampere&hl=en_CA).
- **GSam** : stats par app détaillées (version Pro) nécessitant une commande ADB depuis un PC — [Samsung Community via recherche](https://r1.community.samsung.com/t5/galaxy-a/bateria/td-p/6266065) ; [GSam Users Guide](https://blogger.gsamlabs.com/2011/11/badass-battery-monitor-users-guide.html).
- **DrainScope (gratuit, OSS)** : ventilation par composant matériel (écran, Wi-Fi, radio, GPS, BT, CPU), wakelocks, wakeups, export local — [GitHub drainscope](https://github.com/santo1993/drainscope).
- **Pixel-8-Battery-Health** : cycles, cycles/jour moyens, puissance live W (graphe 2 min), température, tension, courant, âge batterie, type de chargeur, temps jusqu'à plein — [GitHub](https://github.com/AhmedVohra/Pixel-8-Battery-Health).
- **ACC (root, gratuit)** : limite de charge, bypass/idle, courant/tension max, température, planification — [GitHub VR-25/acc](https://github.com/VR-25/acc).
- **Naptime** : Doze agressif + stats Doze — [APKMirror Naptime](https://www.apkmirror.com/apk/francisco-franco/naptime-doze-mode-improved).
- **Battery Historian** : timeline wakelocks (avec `--enable full-wake-history`, valable ~3-4 h avant débordement), jobs, alarmes, radios, stats depuis dernière charge complète — [GitHub battery-historian](https://github.com/google/battery-historian).

### Inferences
Matrice de synthèse (dérivée des sources ci-dessus ; G = gratuit, P = payant, A = nécessite ADB/Shizuku, R = root, — = absent/non documenté) :

| Fonction | AccuBattery | Battery Guru | Ampere | GSam | BBS (archivé) | Pixel natif (8 Pro) | DrainScope/VoltCore (OSS) | ACC |
|---|---|---|---|---|---|---|---|---|
| Courant mA / puissance W live | G | G | G | G | — | — | G | (R) |
| Capacité/santé estimée % | G (estim.) | G (estim.) | — | — | — | **Non** sur 8 Pro | A (API) / estim. | — |
| Cycle count | estimé | estimé | — | — | — | non garanti (cf. Q3) | A/API 34 | — |
| Usure par session | G | G | — | — | — | — | — | — |
| Alarme de charge (ex. 80 %) | G | G | P (alertes) | G | — | — | ? | R |
| Limite de charge réelle | — | — | — | — | — | 80 % fixe | — | R (libre) |
| Bypass charging | — | — | — | — | — | oui avec "Limit to 80%" | — | R (idle) |
| Conso par app (fg/bg) | estimée | ? | — | A | A | oui (%) | A (dumpsys) | — |
| Wakelocks/alarms | — | — | — | A | A | — | A | — |
| Deep sleep / Doze stats | G (deep sleep %) | ? | — | G | A | — | A | — |
| Écran on/off drain | G | ? | — | G | — | partiel (fg/bg) | A | — |
| Température | G | G | G | G | — | — | G | R (seuils) |
| Historique long | P (>1 jour) | P | — | G | G | jusqu'à dernière charge | G | — |
| Widgets / notif | P (notif détaillée) | G | P | G | — | widget batterie | ? | — |
| Export données | ? | ? | — | ? | G (dump) | — | G (local) | logs |

- Le **contrôle de charge** (limite libre, bypass, courant max) est le seul domaine où rien n'est possible sans root, hormis les réglages natifs Pixel ou une prise connectée.

### Gaps
- Pas de confirmation sourcée des fonctions export (CSV) dans AccuBattery/Battery Guru/GSam, ni de l'alarme de charge précise pour chaque appli (les "?" de la matrice).
- Aucune comparaison indépendante récente (2025-2026) de précision entre applis.

---

## Q3. Fonctions natives Pixel (Settings > Battery) sur Pixel 8 Pro sous Android 16/17

### Takeaway
Le Pixel 8 Pro **n'a pas** l'estimation de capacité/état de santé de la page "Battery health" (réservée au Pixel 8a, 9 et plus récents, "product limitations"), et le **cycle count dans About phone n'est officiellement documenté que pour 8a+**. Il bénéficie en revanche de : Charging optimization (Adaptive Charging ou **Limit to 80 %** avec bypass, depuis la Pixel Drop de déc. 2024), **Battery Health Assistance** (optionnelle sur 8 Pro depuis Android 16 QPR1), usage par app depuis la dernière charge complète, Adaptive Battery, Battery Saver / Extreme Battery Saver.

### Cited Findings
**Page Battery health / cycle count**
- Android 14 QPR1 (fin 2023) a ajouté Settings > About phone > Battery information (date de fabrication + nombre de cycles), sans pourcentage de santé ; disclaimer : le compteur peut ne pas être à zéro à la première utilisation (inspections avant livraison) — [9to5Google 2023](https://9to5google.com/2023/09/20/pixel-android-battery-charge-cycle-counts/) ; [PhoneArena](https://www.phonearena.com/news/google-pixel-phones-battery-cycles-not-a-secret-in-android-14-qpr1_id150841).
- La mise à jour de mars 2024 a fait disparaître la page "Battery information" chez certains — [9to5Google 2024](https://9to5google.com/2024/03/05/pixel-battery-information-bug/). Un utilisateur Pixel 8 Pro rapporte que Google "a donné puis rapidement retiré" l'accès aux cycles sur 8 Pro — [Android Central forums](https://forums.androidcentral.com/threads/pixels-can-see-charge-cycles-now.1074741/).
- L'aide Google indique que le suivi des cycles est disponible "starting with the Pixel 8a and later", et que la section Battery health n'est disponible que sur Pixel 8a, Pixel 9 et plus récents, **hors Pixel 8 et 8 Pro** ; Pixel 6a, 7, 7 Pro, 7a, 8 et 8 Pro "don't indicate battery health status" — [Google Pixel Help 15738128](https://support.google.com/pixelphone/answer/15738128?hl=en) (via résultats de recherche) ; [Beebom](https://gadgets.beebom.com/guides/how-to-check-pixel-phone-battery-health).
- Bêta Android 16 : un employé Google : "currently supported in Beta 3 on Pixel 8a and Pixel 9 products including 9 Pro Fold" ; "Due to product limitations, this feature will not be available on older in-market Pixel devices" — [Android Central](https://www.androidcentral.com/apps-software/android-os/google-wont-bring-android-16s-battery-health-page-to-older-pixels) ; [9to5Google mai 2025](https://9to5google.com/2025/05/06/pixel-battery-health-android-16/). En Beta 3, un Pixel 8 Pro affichait la page sans santé ni capacité ; la Beta 4 a retiré la page sur plusieurs modèles dont le 8 Pro — [Android Authority](https://www.androidauthority.com/android-16-battery-health-menu-missing-3554776/) ; [9to5Google mars 2025](https://9to5google.com/2025/03/13/android-16-pixel-battery-health-live/).
- Android 16 stable (juin 2025) : tous les Pixel 6+ ont un menu "Battery health" dans Settings > Battery, mais "battery capacity percentage is not available on the Pixel 8 Pro and older devices" — [9to5Google juin 2025](https://9to5google.com/2025/06/11/android-16-pixel-battery-health-2/) (via résultats de recherche).
- Durée de vie annoncée : Pixel 3 → Pixel 8 Pro (et Fold) ≈ 80 % de capacité jusqu'à ~800 cycles ; Pixel 8a et plus récents ≈ 1000 cycles — [9to5Google janv. 2025](https://9to5google.com/2025/01/06/pixel-battery-charge-cycles/) ; [PhoneArena](https://www.phonearena.com/news/google-reveals-how-many-charging-cycles-a-pixel-battery-gets_id166537).

**Battery Health Assistance**
- Ajuste progressivement la tension maximale de la batterie à partir de 200 cycles et jusqu'à 1000 cycles (perte d'autonomie annoncée faible, ralentissement de charge non chiffré) — [MakeUseOf](https://www.makeuseof.com/pixel-battery-200-cycle-countdown/) (qui cite la page d'aide Google).
- Apparue avec le Pixel 9a (obligatoire), étendue aux Pixel 6a+ avec Android 16 QPR1 Beta 2 (~août 2025), chemin Battery > Battery health > Battery health assistance — [Android Authority](https://www.androidauthority.com/android-16-qpr1-beta-2-battery-health-assistance-3566396/) ; [Pocket-lint](https://www.pocket-lint.com/more-pixels-battery-health-assistance/) ; [Android Police](https://www.androidpolice.com/battery-health-assistance-expanding-android-16-qpr1-beta2/).
- Désactivable sur Pixel 6a, 7, 7 Pro, 7a, 8, **8 Pro**, 8a, 9, 9 Pro, 9 Pro XL, 9 Pro Fold (activée par défaut) ; obligatoire sans toggle sur Pixel 9a, 10, 10 Pro, 10 Pro XL, 10 Pro Fold, 10a ; quelques Pixel 8 la voient encore active après désactivation — [newsbeep](https://www.newsbeep.com/us/894860/) ; [brick.tech](https://articles.brick.tech/pixel-battery-health-assistance-explained/).

**Charging optimization (Adaptive Charging / Limit to 80 %) et bypass**
- "Limit to 80%" arrivé avec la Pixel Drop de décembre 2024 (Android 15 QPR1), Settings > Battery > Charging optimization — [9to5Google déc. 2024](https://9to5google.com/2024/12/18/charging-optimization-saves-pixel-battery-heres-how/) ; [Android Police](https://www.androidpolice.com/the-battery-charging-limit-feature-for-pixel-phones-appears-in-stable-android-15/).
- Bypass charging confirmé par Google quand le téléphone reste branché à 80 % — [Android Authority](https://www.androidauthority.com/pixel-bypass-charging-3507373/) ; [Android Authority confirmation](https://www.androidauthority.com/google-confirms-pixel-bypass-charging-3508077/) ; [GSMArena](https://www.gsmarena.com/google_pixels_got_80_battery_charge_limit_and_battery_bypass_with_the_latest_update-news-65708.php).
- Android 16 (Beta 3) : la limite est levée pour charger à 100 % "once every one to two weeks to maintain battery level reporting accuracy" — [9to5Google](https://9to5google.com/2025/03/14/pixel-charge-80-fully/) ; [Android Police](https://www.androidpolice.com/android-16-beta-3-override-limit-80-charging-1-2-weeks/).
- Sous Android 16, Charging optimization a été déplacé en bas de la page Battery health — [9to5Google juin 2025](https://9to5google.com/2025/06/11/android-16-pixel-battery-health-2/) (via résultats de recherche).
- Pixel Drop de mars 2026 / Android 16 QPR3 : avec Limit to 80 %, charge normale jusqu'à ~77 % puis courant réduit (ex. 47 min pour passer de 77 à 80 % ; <1 W vs 12-14 W sans limite) ; Google dit que c'est voulu (chaleur, santé) — [XDA](https://www.xda-developers.com/googles-latest-pixel-drop-includes-charging-bug-thats-actually-intentional/) ; [BGR](https://www.bgr.com/2121588/google-pixel-battery-update-80-percent-charge-working-correctly/) ; [heise](https://www.heise.de/en/news/Pixel-Google-massively-throttles-charging-at-80-percent-limit-11206891.html).
- Téléphone éteint et branché : la limite 80 % est ignorée (charge à 100 %) — [Pocket-lint](https://www.pocket-lint.com/pixel-charging-limit-fault/).
- Le Pixel limite automatiquement à ~70-80 % dans certaines conditions (ex. branché en continu plusieurs jours) — [Google Pixel Help 6090612](https://support.google.com/pixelphone/answer/6090612?hl=en) (via résultats de recherche).
- Adaptive Charging : apprend les habitudes sur ~2 semaines ; charge à 80 % puis termine ~1 h avant le débranchement prévu ; conditions citées : charge entre 21 h et 4 h avec réveil entre 3 h et 10 h — [Google Store magazine](https://store.google.com/us/magazine/pixel-battery-saver-features?hl=en-US) ; [Android Central](https://www.androidcentral.com/phones/google-pixel/how-optimize-charging-google-pixel).
- Pas de pourcentage de limite personnalisable sur Pixel (contrairement à OnePlus 13 sous Android 16 bêta, 5 paliers) — [GSMArena](https://m.gsmarena.com/newscomm-67367.php).
- Android 17 Beta 3 : code caché "Priority Charging" (met en pause l'activité de fond pour accélérer la charge, chargeur 30 W+, gestion thermique) ; non confirmé officiellement — [Android Authority](https://www.androidauthority.com/android-17-beta-3-priority-charging-3652492/).

**Usage par app, économiseurs**
- Settings > Battery > Battery usage : graphe depuis la dernière charge complète, "View by apps" / "View by systems" — [Google Pixel Help 2819525](https://support.google.com/pixelphone/answer/2819525?hl=en) (via résultats de recherche). Sous Android 16, chaque app affiche son % et ses minutes d'utilisation en arrière-plan — [MakeUseOf](https://www.makeuseof.com/disabled-android-background-apps-battery-life/). Historique : Android 12 était passé à 24 h, Android 13 QPR1 est revenu à "depuis la dernière charge complète" — [PhoneArena](https://www.phonearena.com/news/android-13-QPR-1-beta-2-improves-battery-stats-page_id143082).
- Adaptive Battery (IA qui limite les apps peu utilisées en arrière-plan) ; Extreme Battery Saver (choix des apps qui restent actives) — [Google Store magazine](https://store.google.com/us/magazine/pixel-battery-saver-features?hl=en-US) ; [Google Pixel Help 6090612](https://support.google.com/pixelphone/answer/6090612?hl=en).

**Charge Pixel 8 Pro (référence pour un "testeur de chargeur")**
- 30 W filaire max avec chargeur USB-PD 3.0 PPS (pic mesuré ~27 W) ; 23 W sans fil uniquement avec Pixel Stand 2, 12 W avec Qi EPP générique ; 100 % en ~79 min filaire — [9to5Google](https://9to5google.com/2023/10/04/google-pixel-8-charging-speed/) ; [Android Authority](https://www.androidauthority.com/google-pixel-8-battery-life-3382266/) ; [ChargerLAB](https://www.chargerlab.com/wired-wireless-charging-review-of-google-pixel-8-pro/).

### Inferences
- Sur Pixel 8 Pro, l'utilisateur **n'a aucune** valeur officielle de santé (% / mAh) ; le cycle count natif est au mieux incertain. C'est **le** trou le plus net que la future appli peut combler (via BATTERY_STATS ou estimation).
- Le natif n'offre aucune **alarme** de charge personnalisable, aucun **seuil** autre que 80 %, aucun **historique long** (au-delà de la dernière charge complète), aucun **export**, ni wakelocks/Doze.
- La limite 80 % étant matérielle/firmware (et avec bypass), une appli sideloadée non root peut simplement guider l'utilisateur vers ce réglage et mesurer son effet (courant ≈ 0 à 80 %, durée 77→80 %).

### Gaps
- Pages support.google.com non lisibles directement : les listes exactes de modèles reposent sur les extraits de recherche et la presse.
- Non confirmé : ce qu'affiche précisément la page Battery health sur un 8 Pro sous Android 16 QPR3 / Android 17 (probablement seulement le toggle Battery Health Assistance + Charging optimization) et si "About phone > Battery information" montre le cycle count sur 8 Pro en 2026.
- Aucune note officielle Google sur des fonctions batterie propres à Android 17 stable sur Pixel (Priority Charging non confirmé).

---

## Q4. Quelles API Android exposent quoi, avec quel niveau d'accès ; valeurs réelles sur Pixel 8 Pro

### Takeaway
Sans permission : `BatteryManager` (courant instantané/moyen, charge counter, energy counter, %, temps restant jusqu'à plein) + broadcast `ACTION_BATTERY_CHANGED` (tension, température, statut, plug, santé grossière, **cycle count depuis API 34**, **capacity level depuis API 36**). Avec **BATTERY_STATS** accordé par ADB/Shizuku : état de santé (SoH %), dates de fabrication/première utilisation, charging policy (Android 14), statut de pièce/serial (Android 15, flaggé). Avec **DUMP (+PACKAGE_USAGE_STATS)** via ADB ou Shizuku : `dumpsys batterystats` (conso par app/composant, wakelocks, capacité apprise). Le **contrôle** de charge (sysfs `google,charger/charge_stop_level`) reste root-only.

### Cited Findings
**API publiques (aucune permission)**
- `BATTERY_PROPERTY_CHARGE_COUNTER` (µAh), `CURRENT_NOW` (µA instantané), `CURRENT_AVERAGE` (µA moyen), `CAPACITY` (%), `ENERGY_COUNTER` (nWh) via `getIntProperty/getLongProperty` ; `computeChargeTimeRemaining()` ; `isCharging()` — [Android Developers – BatteryManager](https://developer.android.com/reference/android/os/BatteryManager) (NB : l'outil n'a pas pu extraire le corps de la page ; unités et méthodes conformes à la référence connue, à re-vérifier).
- API 34 (Android 14) : ajout public de `EXTRA_CYCLE_COUNT` et `EXTRA_CHARGING_STATUS` — [API diff 34](https://developer.android.com/sdk/api_diff/34/changes/android.os.BatteryManager).
- API 35 (Android 15) : ajout de `String getStringProperty(int)` — [API diff 35](https://developer.android.com/sdk/api_diff/35/changes/android.os.BatteryManager).
- API 36 (Android 16) : `EXTRA_CAPACITY_LEVEL` + `BATTERY_CAPACITY_LEVEL_{UNSUPPORTED,UNKNOWN,CRITICAL,LOW,NORMAL,HIGH,FULL}` — [API diff 36](https://developer.android.com/sdk/api_diff/36/changes/android.os.BatteryManager). Valeurs : UNSUPPORTED −1, UNKNOWN 0, LOW 2, NORMAL 3, HIGH 4, FULL 5 ; extra `"android.os.extra.CAPACITY_LEVEL"` dans `ACTION_BATTERY_CHANGED`, derrière le flag `android.os.battery_part_status_api` ; LOW = le framework doit limiter les jobs de fond pour ne pas ralentir la charge, CRITICAL lié à un arrêt programmé — [AOSP commit "BatteryService: expose capacity level API"](https://gerrit.omnirom.org/plugins/gitiles/android_frameworks_base/+/376d014abfe87b9ff7ee5df596f52944fa50ca94%5E%21) ; [AOSP commit description](https://gerrit.omnirom.org/plugins/gitiles/android_frameworks_base/+/ebade90af1d5fb4de2f7fcc80872816e9492e101%5E%21).

**System API gatées par BATTERY_STATS**
- Android 14 : cycle count & charging status publics ; manufacturing date, first usage date, charging policy, state of health = system API ; toute app ayant BATTERY_STATS peut les appeler ; accordable via ADB ; au départ uniquement sur Pixel (Android 14 Beta 2+) ; valeurs dépendant du charging IC et du Health HAL, "reporting for the state of health is not guaranteed" — [Android Authority](https://www.androidauthority.com/android-14-battery-health-check-3331255/) ; [Android Police](https://www.androidpolice.com/android-14-battery-health-results-may-not-be-accurate/) ; [Mishaal Rahman sur X](https://x.com/MishaalRahman/status/1699180507664146901).
- Android 15 (Health HAL v3) : `BATTERY_PROPERTY_SERIAL_NUMBER` et `BATTERY_PROPERTY_PART_STATUS` (unsupported / original / replaced), system API, BATTERY_STATS requis, `@FlaggedApi("android.os.battery_part_status_api")`, `getStringProperty` renvoie null si non supporté ; le serial est "stubbed" (pas de nœud sysfs à l'époque du commit) — [AOSP commit](https://gerrit.omnirom.org/plugins/gitiles/android_frameworks_base/+/da893ff40f836950b8148d2148c600513ad16221%5E%21) ; [/e/OS mirror native](https://gitlab.e.foundation/e/os/android_frameworks_native/-/commit/4f0fe01a9e54a45f5cc2b953d4b05396baac1ccb).
- Commandes en usage (Android 14 → 17) : `adb shell pm grant <pkg> android.permission.BATTERY_STATS` (Batt, aBattery via Shizuku, Pixel-8-Battery-Health testé sur Android 17) — [AlternativeTo Batt](https://alternativeto.net/software/batt/about) ; [GitHub Pixel-8-Battery-Health](https://github.com/AhmedVohra/Pixel-8-Battery-Health).
- Historique de la permission : passée de dangerous à signature|system (4.4, /system/priv-app requis), un commit ultérieur montre `signature|system|development` (le flag *development* est ce qui rend `pm grant` possible) ; des échecs `SecurityException` sont rapportés sur certains builds OEM (Xiaomi) — [android-platform group](https://groups.google.com/g/android-platform/c/f-7Td9aeFKY) ; [AOSP commit](https://android.googlesource.com/platform/frameworks/base/+/3714141d287c965ea3451831dd5167f63f8db87b%5E%21/) ; [XDA BBS](https://forum.xda-developers.com/apps/betterbatterystats/unable-to-grant-permissions-using-adb-t3812658/post81702273).

**Conso par app / wakelocks / Doze sans root**
- `adb shell dumpsys batterystats [options]`, forme machine `--checkin` — [Android Developers – dumpsys](https://developer.android.com/tools/dumpsys).
- Via Shizuku (uid 2000 shell) une app peut lire `dumpsys batterystats` (valeurs mesurées) ; sans Shizuku, estimation via UsageStatsManager — [GitHub drainscope](https://github.com/santo1993/drainscope) ; `dumpsys batterystats --charged` via Shizuku — [GitHub s23-optimizer](https://github.com/muhib-karim/s23-optimizer).
- Alternative sans Shizuku : grant ADB de DUMP + PACKAGE_USAGE_STATS (le toggle Settings "Usage access" ne remplace pas ce grant pour les estimations par app selon VoltCore) — [GitHub voltcore](https://github.com/ABDURREHMAN629/voltcore) ; DUMP + PACKAGE_USAGE_STATS donnent la "capacité mesurée par Android sur de vraies charges" — [GitHub Pixel-8-Battery-Health](https://github.com/AhmedVohra/Pixel-8-Battery-Health).
- Battery Historian : `adb shell dumpsys batterystats --reset`, `--enable full-wake-history`, `adb bugreport bugreport.zip` (7.0+) — [GitHub battery-historian](https://github.com/google/battery-historian).

**Mesure de puissance fine (outils dev)**
- Android Studio Power Profiler (depuis Hedgehog) : rails ODPM (CPU Big/Mid/Little, caméra, cellulaire, écran, GPS, GPU, mémoire, sensor core, UFS, WLAN), via system trace, sur **Pixel 6 et ultérieurs** (donc Pixel 8 Pro), Android 10+ ; disponibilité des rails variable selon appareil — [Android Developers – Power Profiler](https://developer.android.com/studio/profile/power-profiler) ; [Perfetto docs](https://perfetto.dev/docs/data-sources/battery-counters) (via résultats de recherche) ; [AndroidPerformance 2026](https://androidperformance.com/en/2026/05/04/Android-Perfetto-16-GPU-Power-Counters/).

**sysfs / root**
- `/sys/class/power_supply/battery/charge_full` et `charge_full_design` (µAh) → santé ; `cycle_count` présent sur certains appareils, souvent root — [XDA guide root](https://xdaforums.com/t/guide-how-to-check-battery-cycle-count-and-capacity-root.4123177/) ; [Technastic](https://technastic.com/check-battery-health-charge-level-cycle-capacity-adb/).
- Pixel (Tensor) : `/sys/devices/platform/google,charger/charge_stop_level` et `charge_start_level` (arrêt/reprise par niveau de charge) ; écriture root (Magisk sur init_boot) sur Pixel 8 Pro ; ne pas laisser `charge_start_level` à 0 ; sur Pixel 7 Pro l'arrêt passe en "passthrough" — [droidwin Pixel 8 Pro](https://droidwin.com/how-to-limit-the-battery-charging-level-on-pixel-8-pro/) ; [billauer.se](https://billauer.se/blog/2024/05/battery-charge-limit-google-pixel/) ; [AOSP kernel commit google_charger](https://android.googlesource.com/kernel/msm/+/8ae5028eba3259a34fd136900502b553d1052695%5E%21/).

**Contraintes OS pour une appli de monitoring en tâche de fond**
- Android 15 (apps ciblant 35+) : FGS `dataSync` limité à 6 h / 24 h (puis `onTimeout`), un receiver BOOT_COMPLETED ne peut plus lancer de FGS dataSync/camera/mediaPlayback/phoneCall/mediaProjection/microphone — [Android 15 behavior changes](https://developer.android.com/about/versions/15/behavior-changes-15).
- Google Play pénalise depuis le 1er mars 2026 les apps dépassant le seuil "Excessive Partial Wake Lock" d'Android vitals (avertissement sur la fiche, exclusion des recommandations) — [Android Developers Blog](https://android-developers.googleblog.com/2026/03/battery-technical-quality-enforcement.html) ; [9to5Google](https://9to5google.com/2025/11/10/google-play-battery-wake-locks/).

### Inferences
- **Pixel 8 Pro, ce qui est quasi certain** : CURRENT_NOW/AVERAGE, CHARGE_COUNTER, tension, température, statut, plug sont fournis (les applis type Ampere/AccuBattery fonctionnent sur Pixel). `EXTRA_CYCLE_COUNT` est une API publique depuis Android 14 et Pixel en était l'appareil de lancement, donc très probablement renseignée sur 8 Pro — **à vérifier** (`adb shell dumpsys battery`).
- **Probable mais non prouvé** : STATE_OF_HEALTH via BATTERY_STATS renvoie une valeur sur Pixel 8 Pro, car le Pixel 8 (même génération Tensor G3) en renvoie une sous Android 17 selon Pixel-8-Battery-Health ; Google masque cependant cette donnée dans Settings ("product limitations"), ce qui peut signifier qu'elle est peu fiable sur ce modèle.
- La "capacité mesurée par Android" lue avec DUMP correspond vraisemblablement aux lignes de capacité apprise de `dumpsys batterystats` (inférence, non sourcée).
- Une appli personnelle sideloadée peut donc atteindre, **sans root**, un niveau "BBS + AccuBattery + santé Android 14" grâce à une seule étape ADB (ou Shizuku) : `pm grant` BATTERY_STATS + DUMP + PACKAGE_USAGE_STATS. Le contrôle de charge reste hors de portée sans root. Les règles Play (wakelocks) ne s'appliquent pas, mais les limites FGS d'Android 15+ si.

### Gaps
- Pas pu lire le corps de la référence `BatteryManager` (page trop lourde) : unités, valeurs d'échec (`Integer.MIN_VALUE`/`Long.MIN_VALUE`) et convention de signe de CURRENT_NOW non citées mot pour mot.
- Aucune source sur la lisibilité de `/sys/class/power_supply/*` par une app non privilégiée sous la SELinux du Pixel 8 Pro.
- Aucune source sur les valeurs réelles retournées par un Pixel 8 Pro pour STATE_OF_HEALTH, MANUFACTURING_DATE, FIRST_USAGE_DATE, PART_STATUS, EXTRA_CAPACITY_LEVEL. Test sur appareil nécessaire.
- Pas vérifié si `BatteryStatsManager`/`BatteryUsageStats` (system API) est appelable par réflexion avec BATTERY_STATS sur Android 16/17.

---

## Q5. Problèmes connus, ce qui est mal fait ou impossible

### Takeaway
Les estimations de santé type AccuBattery sont jugées "assez bonnes pour un ordre de grandeur" mais fluctuent (±10 % rapportés) et demandent plusieurs cycles de calibration ; les API santé officielles dépendent du HAL/charging IC ; les outils historiques de diagnostic (BBS, Battery Historian) sont archivés ; et sans root il est impossible de limiter la charge à un seuil libre ou de forcer le bypass hors de la limite 80 % native.

### Cited Findings
- AccuBattery : "accurate enough for general use to get an estimate of health" ; variances naturelles de capacité usine — [Android Central forums](https://forums.androidcentral.com/threads/how-accurate-is-accubattery.1042146/). Onglet santé "pretty inaccurate", fluctuation jusqu'à ±10 % ; besoin de plusieurs cycles — [Samsung Community](https://r2.community.samsung.com/t5/Galaxy-A/Accu-battery/td-p/5646186). Un post Facebook rapporte un Pixel 8 neuf à 95 % dans l'app (anecdote) — [Facebook group](https://www.facebook.com/groups/3055381168095824/posts/3247809565519649/).
- API santé Android 14 : "les résultats peuvent ne pas être précis", dépendance au charging IC et au HAL — [Android Police](https://www.androidpolice.com/android-14-battery-health-results-may-not-be-accurate/) ; [Android Authority](https://www.androidauthority.com/android-14-battery-health-check-3331255/).
- Ampere : valeurs instables ou nulles, certains appareils ne mesurent pas vraiment — [Google Play Ampere](https://play.google.com/store/apps/details?id=com.gombosdev.ampere&hl=en_CA).
- Charge sans root : "not possible to control charging without root" — [GitHub BatteryChargeLimiter](https://github.com/MuntashirAkon/BatteryChargeLimiter) ; limite native = 80 % fixe, ralentie depuis mars 2026, ignorée téléphone éteint — [heise](https://www.heise.de/en/news/Pixel-Google-massively-throttles-charging-at-80-percent-limit-11206891.html) ; [Pocket-lint](https://www.pocket-lint.com/pixel-charging-limit-fault/).
- Grant ADB qui échoue (SecurityException) sur certains builds — [XDA](https://forum.xda-developers.com/apps/betterbatterystats/unable-to-grant-permissions-using-adb-t3812658/post81702273).
- Battery Historian : full-wake-history déborde en quelques heures (~3-4 h utiles), outil archivé — [GitHub battery-historian](https://github.com/google/battery-historian).
- Greenify : hibernation par accessibilité à corriger sur Android 14+ ; utilité réduite par Doze/App Standby natifs — [Google Play Greenify](https://play.google.com/store/apps/details?id=com.oasisfeng.greenify&hl=en) ; [MakeUseOf](https://www.makeuseof.com/greenify-android-battery-management/). Brevent : correctifs de compatibilité Android 15 et crash "latest Android" en 2025 — [APKMirror Brevent](https://www.apkmirror.com/apk/jianyu-studio/brevent/brevent-4-2-25-release).
- Battery Health Assistance parfois encore active après désactivation sur Pixel 8 — [brick.tech](https://articles.brick.tech/pixel-battery-health-assistance-explained/).

### Inferences
- Les applis d'estimation (AccuBattery, Battery Guru) ne peuvent mesurer qu'à partir de leur installation (pas d'historique antérieur), sauf à lire le cycle count/SoH système.
- **Impossible sans root sur Pixel 8 Pro** : limite de charge à seuil libre, bypass hors 80 % natif, limite de courant/tension, lecture garantie des nœuds sysfs du chargeur.
- **Possible mais mal fait aujourd'hui** : santé/cycles fiables sur 8 Pro (Google ne l'affiche pas, apps estiment), historique long des sessions de charge avec courbes mA/W/°C, détection de wakelocks/conso par app moderne (BBS archivé, GSam figé), export ouvert des données, vérification "mon chargeur délivre-t-il vraiment 27-30 W / PPS ?", suivi de l'effet de la limite 80 % (temps 77→80 %, bypass effectif), suivi des pas de tension de Battery Health Assistance (200 → 1000 cycles).
- La future appli gagnerait à **croiser** l'estimation coulométrique (CHARGE_COUNTER intégré sur sessions) avec SoH Android (BATTERY_STATS) et la capacité apprise (dumpsys), et à afficher l'écart.

### Gaps
- Aucune étude indépendante récente de la précision d'AccuBattery spécifiquement sur Pixel 8 / 8 Pro (aucun fil Reddit exploitable trouvé).
- Pas de liste sourcée des applis batterie cassées par Android 16/17 (au-delà des correctifs Brevent/Greenify).
- Pas d'info sur une éventuelle restriction d'Android 16/17 au `pm grant` des permissions *development* ou à Shizuku.
