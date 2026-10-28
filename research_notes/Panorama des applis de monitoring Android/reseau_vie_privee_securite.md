# Android network monitoring, privacy and security-audit apps: state of play in October 2026 (target: Pixel 8 Pro, unrooted)

> Research date: 2026-10-28. Release dates come from the GitHub `releases.atom` feeds, which give full ISO timestamps. The HTML release pages hide the year. Several primary hosts could not be reached from this research session: play.google.com, f-droid.org, netguard.me, adguard.com, support.google.com, emanuele-f.github.io and helpnetsecurity.com. Claims about those hosts come from search-engine snippets and are marked "(snippet)". The shared web-search budget ran out near the end of the pass, so a few topics are listed under Gaps.

## Q1. Which apps are the references in 2025-2026? Developer, pricing, licence, access required, maintenance status

### Takeaway
The open-source VPN-slot apps are the active core of the field in 2026:
- **RethinkDNS**, **NetGuard**, **TrackerControl**, **PCAPdroid** and **InviZible Pro** all shipped releases between August and October 2026.
- Two rooted or radio tools are also active: **AFWall+** (root) and **Network Survey**.
- So are **WiFiAnalyzer**, **App Manager**, **Permission Pilot** and **GrapheneOS Auditor**.

Several former references are stale or dead:
- **Exodus app**: last release in July 2024.
- **ClassyShark3xodus**: last update in January 2024.
- **YASNAC**: archived in January 2024.
- **Hypatia**: development stopped with DivestOS in December 2024.
- **Bouncer**: reportedly pulled from the Play Store.

GlassWire is alive but slow: it became fully free in April 2024 and its last known build is from June 2026.

### Cited Findings

#### A. Firewalls and connection monitors (VpnService slot, or root)

**NetGuard** (Marcel Bokhorst / M66B)
- Describes itself as "the first free and open source no-root firewall for Android". It is GPL-3.0, needs Android 5.1+ and works through the local VpnService. — [README](https://github.com/M66B/NetGuard)
- **Free features:**
  - Per-app blocking on Wi-Fi and/or mobile data.
  - Allow when screen on; block when roaming; block system apps.
  - IPv4/IPv6 TCP/UDP; tethering.
  - Notifications on access; per-address usage recording.
  - Hosts-file ad blocking, which is not available in the Play build.
  - — [README](https://github.com/M66B/NetGuard)
- **PRO features:**
  - Log all outgoing traffic, with search/filter and PCAP export.
  - Allow/block individual addresses per app.
  - New-app notifications and configuration from the notification.
  - **Network speed graph in the status bar**.
  - Extra themes.
  - — [README](https://github.com/M66B/NetGuard)
- **Pricing:**
  - Play build: PRO costs €8.99 for the full bundle according to PC Welt. That figure may be dated. — [PC Welt](https://www.pcwelt.de/?p=1531806)
  - GitHub/F-Droid builds: PRO unlocks with a donation of ≥ €0.10, or ≥ €7 for all your devices. A Play purchase cannot be used outside Play. — [netguard.me](https://netguard.me) (snippet)
- **Maintenance:** versions 2.334–2.337 were published on 2026-08-01, and 2.336 "Moved to SDK 36". Version 2.335 was released on 22 May (2026). — [releases.atom](https://github.com/M66B/NetGuard/releases.atom), [releases](https://github.com/M66B/NetGuard/releases)
- **Stated limitations:**
  - "Android doesn't allow chaining of VPN services".
  - No support for wired (ethernet/USB) connections, incoming connections or work-profile apps.
  - Wi-Fi calling may break if the carrier uses IPsec.
  - — [README](https://github.com/M66B/NetGuard)

**RethinkDNS + Firewall + VPN** (Celzero)
- Apache-2.0. Core networking (`firestack`) is a hard fork of outline-go-tun2socks. Three modes: VPN, DNS and Firewall. — [README](https://github.com/celzero/rethink-app)
- **Firewall:**
  - Per-app blocking.
  - Rules triggered by screen on/off, app foreground/background, or metered/unmetered network.
  - Blocking by Play Store category.
  - Domain/IP denylists.
  - Per-app mapping uses Android 10+ APIs; Android 9 and below fall back to procfs.
  - — [README](https://github.com/celzero/rethink-app)
- **DNS:** DoH, Oblivious DoH, DoT, DNSCrypt or WireGuard DNS. — [README](https://github.com/celzero/rethink-app)
- **Proxy:** SOCKS5, HTTP CONNECT and multi-tunnel WireGuard with split tunnelling. — [README](https://github.com/celzero/rethink-app)
- **Network monitor:** per-app reports of when, where and how many connections were made. DNS tracking works on Android 12+; on Android 11 and below it is heuristic. — [README](https://github.com/celzero/rethink-app)
- **Pricing:**
  - The app is free.
  - The paid **Rethink Proxy Network (RPN)** starts at **$1.75/month**, with up to 5 of 80+ locations in parallel.
  - The Play build "can't use on-device blocklists" and relies on cloud per-user lists.
  - — [README](https://github.com/celzero/rethink-app)
- The README says the app is not an anonymity tool, not antivirus, and "closer to Little Snitch than iptables". — [README](https://github.com/celzero/rethink-app)
- **Maintenance:**
  - v0.5.6, 2026-08-09: Android 17 support.
  - v0.5.7, 2026-09-21: redesigned Stats screen with on-device blocklist stats, a country map, pie charts, and allowed/blocked domain and IP stats grouped by app and time. Crashlytics was removed from the GitHub and website builds.
  - v0.5.8, 2026-10-13: automatic turn-off and truncation of `.pcap` captures, Tasker/MacroDroid intents, and "Use System DNS on Captive Portals".
  - — [releases](https://github.com/celzero/rethink-app/releases), [releases.atom](https://github.com/celzero/rethink-app/releases.atom)

**TrackerControl** (Konrad Kollnig et al.)
- GPL-3.0. VPN-based local analysis with no remote server by default. — [README](https://github.com/TrackerControl/tracker-control-android)
- **Features:**
  - Real-time tracker monitoring, with destination **companies and countries**.
  - Granular blocking and hosts-file ad blocking.
  - One-click EU GDPR data requests.
  - Static tracker-library analysis of app code, using Exodus/ClassyShark3xodus signatures fetched at runtime with an offline fallback.
  - Optional DoH.
  - Optional remote VPN via Mullvad, IVPN or a custom WireGuard profile.
  - — [README](https://github.com/TrackerControl/tracker-control-android)
- **Detection:**
  - Mainly DNS-based. TLS SNI extraction only runs in "Research mode", which disables blocking.
  - Lists used: Disconnect, DuckDuckGo Tracker Radar, X-Ray, an in-house list built from ~2,000,000 apps, Steven Black (fallback) and Peter Lowe.
  - — [README](https://github.com/TrackerControl/tracker-control-android)
- **Builds:**
  - Play "Slim" build: only *Minimal* blocking, and it auto-excludes browsers.
  - F-Droid/GitHub "Full" build: Minimal, Standard and Strict modes.
  - — [README](https://github.com/TrackerControl/tracker-control-android)
- **Stated limitations:** it cannot run alongside another VPN, and Android Private DNS is not supported alongside it. — [README](https://github.com/TrackerControl/tracker-control-android)
- **Maintenance:**
  - 2026072701: WireGuard via an embedded bridge, migrated to a Rust engine (gotatun), and a tracker activity timeline.
  - 2026072702: VPN preview, Timeline view, Material 3.
  - 2026080501: Android 17 local-network protections, plus more accurate attribution for shared IPs.
  - — [releases](https://github.com/TrackerControl/tracker-control-android/releases)

**PCAPdroid** (Emanuele Faranda)
- **Free, no root (simulated VPN):**
  - Per-app connection logging, for user and system apps.
  - Host extraction from DNS, TLS and HTTP.
  - PCAP dump with app metadata, and PCAP-over-IP to Wireshark.
  - HTTP inspection and HAR export.
  - Offline country/ASN lookup.
  - Claims low battery use for all-day capture.
  - — [README](https://github.com/emanuele-f/PCAPdroid)
- **With root:** it can capture while another VPN is running. — [README](https://github.com/emanuele-f/PCAPdroid)
- **TLS decryption:** an on-device mitmproxy-based add-on, with SSLKEYLOGFILE export. — [README](https://github.com/emanuele-f/PCAPdroid)
- **Paid features:**
  - Firewall (blocks apps, domains, IPs).
  - Malware detection via third-party blacklists. It only runs during capture, and the developer gives no accuracy guarantee.
  - PCAPng export.
  - — [README](https://github.com/emanuele-f/PCAPdroid), [paid_features](https://emanuele-f.github.io/PCAPdroid/paid_features) (snippet)
- **Unlocking:** an in-app purchase on Play; other builds use an unlock token that generates up to 3 licences. No price found. — [paid_features](https://emanuele-f.github.io/PCAPdroid/paid_features) (snippet)
- **Maintenance:**
  - v2.0.0, 2026-08-20: mitm add-on 2.3 (IPv6 decryption fix), add-on needs Android 7+, x86 and armeabi-v7a dropped. **The signing key changed**, so paid licences must be regenerated.
  - v2.0.1, 2026-09-09: add-on 2.4 restores decryption on rooted devices; fixes the firewall with always-on VPN + root capture.
  - v2.0.2, 2026-09-20.
  - — [releases](https://github.com/emanuele-f/PCAPdroid/releases), [releases.atom](https://github.com/emanuele-f/PCAPdroid/releases.atom)
- **Mechanism caveat:** an L4 proxy. The PCAP shows app↔VpnService traffic, not the real wire traffic, and "isn't reliable for per-packet analysis". — [how_it_works.md](https://github.com/emanuele-f/PCAPdroid/blob/master/docs/how_it_works.md)

**InviZible Pro** (Garmatin Oleksandr / Gedsh)
- GPL-3.0+. Combines Tor, DNSCrypt, Purple I2P and a firewall, on root and non-root devices. Funded by donations. — [README](https://github.com/Gedsh/InviZible)
- The Google Play version has **no ad/malware/adult blocking**. — [README](https://github.com/Gedsh/InviZible)
- **Maintenance:** Stable v7.6.0 on 2026-10-27 (Tor "4.9.14" as written in the notes, DNSCrypt 2.1.20, Purple I2P 3.0.0). Betas v2.7.5 and v2.7.6 on 21 and 24 October 2026. — [releases](https://github.com/Gedsh/InviZible/releases), [releases.atom](https://github.com/Gedsh/InviZible/releases.atom)

**AFWall+** (ukanth): root, iptables
- v5.0.0, 2026-10-12: global and per-app allow/block rules, a rewritten root engine with "faster, atomic rule apply", Android 16 and Private Space. — [releases](https://github.com/ukanth/afwall/releases), [releases.atom](https://github.com/ukanth/afwall/releases.atom)
- v4.1.0, 2026-08-06: target SDK 36, plus a log-watcher watchdog. — [releases](https://github.com/ukanth/afwall/releases), [releases.atom](https://github.com/ukanth/afwall/releases.atom)
- v4.0.3, 2026-03-01: NDK binaries, to avoid false antivirus positives. — [releases](https://github.com/ukanth/afwall/releases), [releases.atom](https://github.com/ukanth/afwall/releases.atom)

**GlassWire Data Usage Monitor** (GlassWire, owned by Domotz; APKs published under "SecureMix LLC" / "Domotz Inc")
- **Ownership:** Domotz announced the acquisition of GlassWire on 2023-09-21. — [Newswire](https://www.newswire.com/news/domotz-announces-acquisition-of-glasswire-and-launches-glasswire-for-22132867)
- **Now free:** the Premium tier was removed and all features became free. 窓の杜 dates the announcement to 2024-04-17 and lists firewall, bandwidth monitoring and data-plan alerts among the newly free features. — [窓の杜](https://forest.watch.impress.co.jp/docs/news/1586141.html), [GlassWire forum](https://forum.glasswire.com/t/glasswire-for-android-is-now-free-for-all-users/16358), [forum, subscriptions](https://forum.glasswire.com/t/glasswire-android-free/16386)
- **Builds:**
  - 3.0.396r around 2025-11-27, and 3.0.397r uploaded 2026-06-29 (APKMirror, snippet).
  - 3.0.386r notes: updated target API, updated Billing library, data usage shown to 2 decimals.
  - — [APKMirror 3.0.386r](https://www.apkmirror.com/apk/domotz-inc/glasswire-data-usage-privacy/glasswire-data-usage-privacy-3-0-386r-release/), [APKMirror SecureMix 3.0.380r](https://www.apkmirror.com/apk/securemix-llc/glasswire-data-usage-privacy/glasswire-data-usage-privacy-3-0-380r-release/)
- A forum user reports that the "GlassWire for Business" pilot through Domotz has ended. — [forum](https://forum.glasswire.com/t/is-domotz-partnership-still-going-on-any-info-available/16343)
- Alerts on an app's first-time data connection. — [bestforandroid 2026 roundup](https://bestforandroid.com/best-mobile-data-usage-apps.md)

#### B. DNS / ad and tracker blockers

**AdGuard for Android**
- The free version blocks ads at browser level. The paid ("full") version adds ad blocking **inside apps**, userscripts and AdGuard Extra. — [AdGuard KB](https://adguard.com/kb/adguard-for-android/features/free-vs-full) (snippet)
- **Price conflict:**
  - About $79.99 lifetime for 3 devices. — [adblock-tester](https://adblock-tester.com/ad-blockers/adguard/free-vs-paid/)
  - "From ~$2.49/month", from a competitor's review. — [AdLock](https://adlock.com/blog/adguard-review/)
  - Trial length also conflicts: 3 vs 14 days.

**Blokada** (Blocka AB)
- **Blokada 6:** "Blokada Cloud", a DoT/DoH multi-device service, plus "Blokada Plus", a WireGuard VPN. — [blokada.org](https://blokada.org/) (snippet)
- **Free tier:** Blokada 5's local DNS blocking. Blokada 6 needs a subscription after a trial. — [iTechGuides](https://www.itechguides.com/best/ad-blockers/blokada/)
- **Price:** the iOS App Store shows Blokada Cloud at €20.49 in Austria. No Android price was found. — [App Store AT](https://apps.apple.com/at/app/blokada/id1508341781?l=en-GB)
- **Maintenance:** very active; builds 26.10.1196–1198 were published on 2026-10-28. — [releases.atom](https://github.com/blokadaorg/blokada/releases.atom)

DNS blocking is also built into RethinkDNS, TrackerControl, NetGuard (hosts file, non-Play builds) and InviZible (DNSCrypt). See section A.

#### C. Data usage and speed meters

**My Data Manager** (Mobidia, now attributed to Sensor Tower / "ST Advanced")
- Tracks mobile, Wi-Fi and roaming usage, with custom usage alarms and per-app tracking. — [AppGoblin](https://appgoblin.info/apps/com.mobidia.android.mdm), [AppFollow](https://apps.appfollow.io/android/my-data-manager-data-usage/com.mobidia.android.mdm?country=hu)
- Version 9.10.4 is dated 2025-07-22 (APKMirror, snippet). — [AppGoblin](https://appgoblin.info/apps/com.mobidia.android.mdm), [AppFollow](https://apps.appfollow.io/android/my-data-manager-data-usage/com.mobidia.android.mdm?country=hu)
- Called "best for dual-SIM tracking". — [bestforandroid](https://bestforandroid.com/best-mobile-data-usage-apps.md)

**Data Usage Monitor: Data Alert** (`com.app.usage.datamanager`)
- Ad-supported with in-app purchases. Real-time usage in the status bar, plus data warnings. — [AppGoblin](https://appgoblin.info/apps/com.app.usage.datamanager)

**Internet Speed Meter Lite** (DynamicApps)
- Live speed in the status bar and daily usage in the notification, with separate mobile/Wi-Fi stats. Ad-free. — [APKMirror 1.7.0-lite](https://www.apkmirror.com/apk/dynamicapps/internet-speed-meter-lite/internet-speed-meter-lite-1-7-0-lite-release/)
- Latest found: 1.7.0-lite (2025-03-21), which added Android 15 support and needs Android 9+. — [APKMirror 1.7.0-lite](https://www.apkmirror.com/apk/dynamicapps/internet-speed-meter-lite/internet-speed-meter-lite-1-7-0-lite-release/)

**Netspeed Lite** (`com.krishna.netspeedlite`)
- A different developer; a newer status-bar speed app from early 2026. — [AppGoblin](https://appgoblin.info/apps/com.krishna.netspeedlite)

#### D. Wi-Fi and cellular radio tools

**WiFiAnalyzer (open-source)** (VREM Software Development)
- GPL-3.0; "does not require internet access", uses minimal permissions. — [repo](https://github.com/VREMSoftwareDevelopment/WifiAnalyzer)
- **Features:**
  - Nearby APs and channel graphs.
  - Channel rating.
  - Signal over time.
  - HT/VHT detection at 40/80/160/320 MHz.
  - 2.4/5/6 GHz bands.
  - Estimated distance.
  - Vendor/OUI lookup, filters and export.
  - — [repo](https://github.com/VREMSoftwareDevelopment/WifiAnalyzer)
- **Maintenance:**
  - V3.2.1, 2025-06-14: Wi-Fi 7 320 MHz.
  - V3.2.2, 2026-01-10: Android 16 / API 36.
  - V3.3.1, 2026-07-01: graphs moved from GraphView to Vico, OUI database update.
  - — [releases.atom](https://github.com/VREMSoftwareDevelopment/WifiAnalyzer/releases.atom)

**WiFiman** (Ubiquiti)
- Speed tests with history and sharing. — [APKMirror 1.6.9](https://www.apkmirror.com/apk/ubiquiti-networks-inc/wifiman/wifiman-1-6-9-release/wifiman-1-6-9-android-apk-download), [LinITX blog](https://blog.linitx.com/blog-unifi-wifiman-app-and-wifiman-wizard/), [APKMirror 2.8.0](https://apkmirror.com/apk/ubiquiti-networks-inc/wifiman/wifiman-2-8-0-release/wifiman-2-8-0-android-apk-download)
- LAN speed test between two WiFiman devices (since 1.6.9). — [APKMirror 1.6.9](https://www.apkmirror.com/apk/ubiquiti-networks-inc/wifiman/wifiman-1-6-9-release/wifiman-1-6-9-android-apk-download)
- Subnet scan using Bonjour, SNMP, NetBIOS and Ubiquiti discovery; port scanner (2.8.0). — [APKMirror 2.8.0](https://apkmirror.com/apk/ubiquiti-networks-inc/wifiman/wifiman-2-8-0-release/wifiman-2-8-0-android-apk-download), [LinITX blog](https://blog.linitx.com/blog-unifi-wifiman-app-and-wifiman-wizard/)
- Connection details: IP, gateway, DNS, BSSID, channel, **ping latency and packet loss**. — [APKMirror 2.8.0](https://apkmirror.com/apk/ubiquiti-networks-inc/wifiman/wifiman-2-8-0-release/wifiman-2-8-0-android-apk-download)
- The current version number was not confirmed.

**Network Cell Info Lite** (Wilysis, now listed under M2Catalyst LLC)
- Ad-supported with in-app purchases. A separate paid version unlocks all features **except** a yearly **Pro subscription** "for the Telecom Professionals". The Pro tier adds a logging database with xARFCN, frequency, band and RSRQ, plus a neighbour-cell database. — [AppFollow](https://apps.appfollow.io/android/network-cell-info-mobile-wifi-signal/com.wilysis.cellinfo?country=se), [APKMirror 7.3.6](https://www.apkmirror.com/?p=13624388)

**NetMonster** (Michal Mroček)
- No primary source was reachable for the app itself.
- Its open-source core library, **netmonster-core** (Apache-2.0), does the following:
  - Validates and corrects RIL data for GSM, WCDMA, LTE, TD-SCDMA and NR.
  - Detects LTE-A through ServiceState/PhysicalChannel (Android P+) and DisplayInfo (Android R+).
  - Detects NR NSA.
  - Relies partly on **hidden APIs**.
- The library also warns that OEMs "don't follow public documentation", so results aren't guaranteed. — [netmonster-core](https://github.com/mroczis/netmonster-core)

**Network Survey** (Christian Rowlands / Craxiom)
- Apache-2.0. Shows the serving cell and logs GSM/CDMA/UMTS/LTE/NR records, Wi-Fi, Bluetooth and GNSS. Can stream to gRPC. — [repo](https://github.com/christianrowlands/android-network-survey)
- **Maintenance:**
  - v1.59, 2026-09-22: LTE/5G NR band reference, a channel-to-band calculator, and a fix for blank NR bands.
  - v2.0-beta01, 2026-10-27: rebuilt around a single survey; GeoPackage logging dropped in favour of CSV.
  - — [releases.atom](https://github.com/christianrowlands/android-network-survey/releases.atom)

**Speedtest by Ookla**
- A video streaming test was added in 4.6.4 (2021). — [APKMirror 4.6.4](https://www.apkmirror.com:443/apk/ookla/speedtest/speedtest-4-6-4-release/speedtest-by-ookla-4-6-4-2-android-apk-download/)
- **Ad removal:** older listings tie it to the VPN Premium tier at $4.99/month. Third-party 2026 figures conflict: "$4.99/year ad-free" vs "$0.99 ad removal". — [APKMirror 4.6.4](https://www.apkmirror.com:443/apk/ookla/speedtest/speedtest-4-6-4-release/speedtest-by-ookla-4-6-4-2-android-apk-download/), [Subger](https://subger.com/en/service/speedtest), [Marlvel](https://marlvel.ai/intel-report/utilities/com-ookla-speedtest)

#### E. App, tracker and permission auditors (static)

**Exodus** (Exodus Privacy, French non-profit)
- GPL-3.0. Shows the trackers and permissions of installed apps, using data from the εxodus platform. — [repo](https://github.com/Exodus-Privacy/exodus-android-app)
- **Stale:** the latest release is v3.3.2. GitHub shows "24 Jul"; F-Droid lists it as added **2024-07-30**. No 2025–2026 release was found. — [releases](https://github.com/Exodus-Privacy/exodus-android-app/releases), [F-Droid](https://f-droid.org/en/packages/org.eu.exodus_privacy.exodusprivacy) (snippet)

**ClassyShark3xodus** (oF2pks)
- Offline APK scanner using the Exodus tracker list. — [IzzyOnDroid](https://apt.izzysoft.de/fdroid/index/apk/com.oF2pks.classyshark3xodus?repo=main)
- **Stale:** last version 2.0-34, last repo update **2024-01-05**. — [IzzyOnDroid](https://apt.izzysoft.de/fdroid/index/apk/com.oF2pks.classyshark3xodus?repo=main)

**App Manager** (Muntashir Akon)
- Package manager with component, app-ops and permission views, tracker components and a scanner page. — [docs](https://muntashirakon.github.io/AppManager/en/)
- 1-click operations block ad/tracker components and multiple app ops. — [docs](https://muntashirakon.github.io/AppManager/en/)
- It merged ClassyShark3xodus features. — [docs](https://muntashirakon.github.io/AppManager/en/)
- **Maintenance:** v4.1.0 on 2026-06-29, v4.1.1 on 2026-09-04. — [releases.atom](https://github.com/MuntashirAkon/AppManager/releases.atom)

**LibChecker**
- Active: 2.5.4 on 2026-07-02. Its feature set was not verified in this pass. — [releases.atom](https://github.com/LibChecker/LibChecker/releases.atom)

**Permission Pilot** (Matthias Urhahn / darken)
- GPL-3.0. Read-only overview of all permissions. The developer notes that "Android does not allow third-party apps to change permissions on non-rooted devices", so the app opens the right Settings page instead. — [myperm.darken.eu](https://myperm.darken.eu/) (snippet)
- **Maintenance:** v2.2.2-rc0 to v2.2.4-rc0 between 2026-08-16 and 2026-09-11. The release notes mention Google Play "Pro" purchases. — [releases.atom](https://github.com/d4rken-org/permission-pilot/releases.atom)

**App Ops – Permission manager** (Xingchen / Rikka)
- Without root it works through ADB or **Shizuku**. — [APKMirror](https://apkmirror.com/apk/xingchen-rikka/app-ops-permission-manager/app-ops-permission-manager-2-1-10-release) (snippet)
- A 4.1.0 build exists. — [APKMirror 4.1.0](https://www.apkmirror.com/apk/xingchen-rikka/app-ops-permission-manager/app-ops-permission-manager-4-1-0-r1109-d29ee09c-release/)

**OpTrace**
- Uses Shizuku or root to hook the hidden AppOpsManager for a "complete, persistent timeline" of app access. Android 11+.
- **MVP stage, no releases yet.** — [GitHub](https://github.com/jksalcedo/optrace)

**Bouncer** (Sam Ruston)
- Accessibility-based temporary permissions.
- Reported as **no longer available on Google Play**. — [AndroidWorld](https://androidworld.nl/tips/zo-trek-je-automatisch-toestemmingen-van-apps-terug-na-het-afsluiten/), [AlternativeTo](https://alternativeto.net/software/bouncer/about)

**AppVerifier** (soupslurpr)
- ISC licence. Verifies an app's signing-certificate hash against user-supplied hashes or an internal database. Recommends installing from Accrescent. — [repo](https://github.com/soupslurpr/AppVerifier)
- Last GitHub release: "13" on **2025-04-23**. — [releases.atom](https://github.com/soupslurpr/AppVerifier/releases.atom)

#### F. Malware scanning, device integrity and attestation

**Malwarebytes Mobile Security**
- The free version is an on-demand scanner with removal. Premium adds real-time and web protection. — [MalwareTips](https://malwaretips.com/download-malwarebytes-for-android)
- **Price:** about $44.99/year for 1 device (snippet). Trial length conflicts between sources: 14 vs 30 days. — [Toolradar](https://toolradar.com/tools/malwarebytes/pricing)

**Hypatia** (Divested Computing)
- **Dead:** DivestOS and its apps stopped receiving updates in December 2024. — [AlternativeTo news](https://www.alternativeto.net/news/2024/12/secure-mobile-operating-system-divestos-ends-after-a-decade-of-development/)
- A fork (MaintainTeam/Hypatia) was looking for a maintainer in 2025. — [Privacy Guides forum](https://discuss.privacyguides.net/t/hypatia-fork-is-looking-for-a-maintainer/24720.md)

**Play Integrity API Checker** (Nikolas Spiridakis)
- MIT licence. Shows the verdicts Google Play Services returns. — [XDA](https://www.xda-developers.com/play-integrity-api-checker-android-app/)
- v2.2 on 2025-08-18 ("migrated to new server"). — [APKMirror 2.2](https://www.apkmirror.com/apk/nikolas-spiridakis/play-integrity-api-checker/play-integrity-api-checker-2-2-release)
- Subject to a Google quota of 10k requests/day. — [XDA](https://www.xda-developers.com/play-integrity-api-checker-android-app/)
- No GitHub releases are published. — [releases.atom](https://github.com/1nikolas/play-integrity-checker-app/releases.atom)

**YASNAC** (Rikka)
- **Archived on 2024-01-31.** It was built on the retired SafetyNet API. — [IzzyOnDroid](https://apt.izzysoft.de/packages/rikka.safetynetchecker/)

**Key Attestation** (vvb2060)
- v1.8.4 on 2025-01-16: Keymint 4.0 support and an updated revocation list.
- **Shizuku is required** for device-unique-ID attestation, remote key provisioning testing and Samsung Knox attestation.
- — [releases.atom](https://github.com/vvb2060/KeyAttestation/releases.atom)

**Auditor** (GrapheneOS)
- MIT licence. "Hardware-based attestation and intrusion detection". Works locally with a second device via QR code, or remotely through scheduled server verification with email alerts. — [repo](https://github.com/GrapheneOS/Auditor)
- Pixel 8 and 8 Pro support was added in v76. — [release 76](https://github.com/GrapheneOS/Auditor/releases/tag/76)
- Stock-OS Pixels are supported (attestation.app device list). — [GrapheneOS forum](https://discuss.grapheneos.org/d/29366-using-auditor-to-verify-a-non-grapheneos-phone)
- **Maintenance:**
  - v89/v90 on 2026-05-30: devices without Android 13 dropped; Pixel 10 family added.
  - v92 on 2026-07-08: end-of-life Pixels removed.
  - — [releases.atom](https://github.com/GrapheneOS/Auditor/releases.atom)

#### G. Notification loggers

**NotificationLog**
- On F-Droid. Offline; browse logs by time or app, search, export, delete. — [AlternativeTo](https://www.alternativeto.net/software/notificationlog/about/)

**notification-listener-android** (Alfio010)
- GPL-3.0; last push in March 2025. — [ecosyste.ms](https://awesome.ecosyste.ms/projects/github.com%2FAlfio010%2Fnotification-listener-android)

**Notification History Log** (Zipoapps)
- Closed source; the most widely listed app in this category.
- The developer says it is not meant to recover WhatsApp deleted messages.
- — [APKMirror 16.3.0](https://apkmirror.com/apk/zipoapps/notification-history-log/notification-history-log-16-3-0-release)

#### H. Enabler: Shizuku (Rikka)
- **Latest official release:** v13.6.0 on 2025-05-25, with three changes:
  - Android 16 QPR1 support.
  - A start command that can live anywhere executable, e.g. `/data/local/tmp/shizuku`.
  - **Non-root auto-start on Android 13+ when connected to a trusted Wi-Fi network.**
  - — [releases.atom](https://github.com/RikkaApps/Shizuku/releases.atom)
- No 2026 official release appears in the feed.
- A third-party guide dates v13.6.0 to 12 Aug 2026, which conflicts with the official feed. — [techynoob18 blog](https://techynoob18.github.io/blogs/top-3-shizuku-apps-2026.html)
- **Start on boot:** officially only available in Wireless Debugging or root mode. — [Shizuku docs](https://shizuku.rikka.app/guide/setup/) (snippet)
- **Forks:** thedjchi/Shizuku and ShizukuPlus exist. — [upd.dev](https://upd.dev/thedjchi/Shizuku), [newreleases](https://newreleases.io/project/github/thejaustin/ShizukuPlus/release/v14.0.0.r2664)

#### Community perception
- Community threads describe NetGuard as the simple, dependable choice, RethinkDNS as feature-heavy but occasionally breaking apps, and TrackerControl as reported to slow browsers or games for some users.
- These are anecdotal; the factually.co pages look AI-generated.
- — [factually.co](https://factually.co/fact-checks/electronics-tech/best-android-app-level-firewalls-2026-netguard-vs-rethinkdns-274f70), [PieFed thread](https://piefed.nullspace.lol/c/privacy/p/88091/trackercontrol-seems-to-slow-browsers-and-make-games-stop-working), [Lemmy](https://lemmy.imagisphe.re/post/775675)

### Inferences
- **Maintenance tiers in October 2026:**
  - *Very active* (release ≤ 2 months old): RethinkDNS, InviZible, AFWall+, Blokada, Network Survey, PCAPdroid, Permission Pilot, App Manager.
  - *Active*: NetGuard (Aug 2026), TrackerControl (Aug 2026), WiFiAnalyzer (Jul 2026), Auditor (Jul 2026), LibChecker (Jul 2026), GlassWire (Jun 2026).
  - *Slow or uncertain*: Internet Speed Meter Lite (Mar 2025), Play Integrity API Checker (Aug 2025), AppVerifier (Apr 2025), Key Attestation (Jan 2025), Shizuku official (May 2025).
  - *Abandoned or stale*: Exodus app (Jul 2024), ClassyShark3xodus (Jan 2024), YASNAC (archived Jan 2024), Hypatia (Dec 2024), Bouncer (delisted).
- **Static tracker scanning:** the reference apps (Exodus, ClassyShark3xodus) are stale. The live alternatives are TrackerControl's built-in library analysis and App Manager's scanner. A personal app can reuse the Exodus tracker-signature list, which TrackerControl fetches at runtime.
- **Open source vs closed:** most open-source reference apps are GPL-3.0 (NetGuard, TrackerControl, InviZible, WiFiAnalyzer, Exodus, Permission Pilot). Code reuse would impose GPL on a personal app if it is ever distributed. RethinkDNS, Network Survey and netmonster-core are Apache-2.0; Auditor and Play Integrity Checker are MIT; AppVerifier is ISC. The commercial references (GlassWire, AdGuard, Malwarebytes, Ookla, Network Cell Info, NetMonster app, WiFiman, My Data Manager) are closed source.

### Gaps
- **Unconfirmed current data:** Play Store prices and "last updated" dates for GlassWire, NetGuard PRO, AdGuard, Malwarebytes, Ookla, Network Cell Info, NetMonster, WiFiman and My Data Manager. play.google.com and f-droid.org were unreachable from this session, so the price figures above are third-party and partly conflicting.
- **NetMonster app:** pricing (free vs paid), last update and Pixel/Exynos-specific behaviour. No primary source found.
- **Missing licences:** AFWall+ and PCAPdroid licences were not confirmed in fetched text. Both are commonly listed as GPL-3.0; not verified here.
- **Paid-tier details not found:** PCAPdroid paid-feature price, Permission Pilot Pro feature list and price, App Ops (Rikka) premium features.
- **Not verified:** whether the Help Net Security (2026-01-06) Blokada showcase describes new features; LibChecker's feature list.

## Q2. Feature-level coverage: what exists, and what is free vs paid-only?

### Takeaway
Almost every monitoring building block already exists for free in at least one maintained open-source app. The main exceptions:
- NetGuard's connection log, per-address rules and status-bar speed graph are PRO.
- PCAPdroid's firewall, malware detection and PCAPng export are paid.
- AdGuard's in-app blocking needs the full version.
- Blokada 6's cloud and VPN tiers are subscriptions.
- Cell-logging databases (Network Cell Info Pro) and Malwarebytes real-time protection are paid.

What does not exist is one app that combines:
- NetworkStatsManager history (no VPN needed),
- live VPN-based connection logs,
- radio diagnostics,
- permission/app-ops audit,
- and a security-posture checklist.

### Cited Findings

**1. Per-app mobile/Wi-Fi data usage with history**
- Pixel/Android settings show per-app usage for the billing cycle, with warning and limit thresholds. — [bestforandroid](https://bestforandroid.com/best-mobile-data-usage-apps.md)
- My Data Manager tracks mobile, Wi-Fi and roaming per app, with alarms. — [AppGoblin](https://appgoblin.info/apps/com.mobidia.android.mdm)
- GlassWire offers bandwidth monitoring, free since 2024. — [窓の杜](https://forest.watch.impress.co.jp/docs/news/1586141.html)
- NetGuard (free) records usage per address. — [README](https://github.com/M66B/NetGuard)
- RethinkDNS v0.5.7 shows stats per app and over time. — [releases](https://github.com/celzero/rethink-app/releases)
- The system data source, NetworkStatsManager, collects usage in buckets that are "hours long". — [AOSP NetworkStatsManager.java](https://github.com/aosp-mirror/platform_frameworks_base/blob/android12-release/core/java/android/app/usage/NetworkStatsManager.java)

**2. Real-time speed indicator (status bar / notification)**
- Internet Speed Meter Lite: free and ad-free. — [APKMirror](https://www.apkmirror.com/apk/dynamicapps/internet-speed-meter-lite/internet-speed-meter-lite-1-7-0-lite-release/)
- Data Usage Monitor: ads + IAP. — [AppGoblin](https://appgoblin.info/apps/com.app.usage.datamanager)
- Netspeed Lite. — [AppGoblin](https://appgoblin.info/apps/com.krishna.netspeedlite)
- NetGuard's speed graph is **PRO**. It uses extra battery, so it only shows while the screen is on (FAQ Q4). — [README](https://github.com/M66B/NetGuard), [FAQ](https://github.com/M66B/NetGuard/blob/master/FAQ.md)

**3. Data caps and alerts**
- Built-in warning and limit thresholds. — [bestforandroid](https://bestforandroid.com/best-mobile-data-usage-apps.md)
- My Data Manager "custom usage alarms". — [AppGoblin](https://appgoblin.info/apps/com.mobidia.android.mdm)
- GlassWire data-plan alerts, now free. — [窓の杜](https://forest.watch.impress.co.jp/docs/news/1586141.html)
- Data Usage Monitor warnings. — [AppGoblin](https://appgoblin.info/apps/com.app.usage.datamanager)
- GlassWire first-connection alerts per app. — [bestforandroid](https://bestforandroid.com/best-mobile-data-usage-apps.md)

**4. Per-app firewall**
- **VPN-based, free:**
  - NetGuard: Wi-Fi/mobile, screen-on, roaming and system-app options. — [README](https://github.com/M66B/NetGuard)
  - RethinkDNS: screen, foreground/background, metered, Play category, domain/IP lists. — [README](https://github.com/celzero/rethink-app)
  - TrackerControl: tracker-level blocking. — [README](https://github.com/TrackerControl/tracker-control-android)
  - InviZible. — [README](https://github.com/Gedsh/InviZible)
  - GlassWire. — [窓の杜](https://forest.watch.impress.co.jp/docs/news/1586141.html)
- **VPN-based, paid:** PCAPdroid. — [README](https://github.com/emanuele-f/PCAPdroid)
- **iptables, root:** AFWall+ (v5.0.0 adds global + per-app rules, atomic apply). — [releases](https://github.com/ukanth/afwall/releases)

**5. Connection logs (domains/IPs per app)**
- **Free:**
  - PCAPdroid: hosts from DNS/SNI/HTTP, plus country and ASN. — [README](https://github.com/emanuele-f/PCAPdroid)
  - RethinkDNS: when, where and how many; country map. — [README](https://github.com/celzero/rethink-app), [releases](https://github.com/celzero/rethink-app/releases)
  - TrackerControl: companies, countries and a timeline. — [README](https://github.com/TrackerControl/tracker-control-android), [releases](https://github.com/TrackerControl/tracker-control-android/releases)
- **PRO:** NetGuard, with search/filter and PCAP export. — [README](https://github.com/M66B/NetGuard)

**6. DNS-level ad/tracker blocking**
- **Free:**
  - RethinkDNS. The Play build uses cloud lists only. — [README](https://github.com/celzero/rethink-app)
  - TrackerControl. The Play build only has Minimal mode. — [README](https://github.com/TrackerControl/tracker-control-android)
  - NetGuard hosts file (non-Play builds). — [README](https://github.com/M66B/NetGuard)
  - InviZible DNSCrypt. Blocking is absent from the Play build. — [README](https://github.com/Gedsh/InviZible)
  - Blokada 5 (local). — [iTechGuides](https://www.itechguides.com/best/ad-blockers/blokada/)
- **AdGuard:** browser-level blocking is free; in-app blocking needs the full version. — [AdGuard KB](https://adguard.com/kb/adguard-for-android/features/free-vs-full) (snippet)
- **Blokada 6 Cloud / Plus:** subscription. — [blokada.org](https://blokada.org/) (snippet)

**7. Packet capture and TLS decryption**
- PCAPdroid (free): PCAP and PCAP-over-IP, TLS decryption through the mitm add-on, SSLKEYLOGFILE. PCAPng export is **paid**. — [README](https://github.com/emanuele-f/PCAPdroid), [paid_features](https://emanuele-f.github.io/PCAPdroid/paid_features) (snippet)
- RethinkDNS: pcap capture management (v0.5.8). — [releases](https://github.com/celzero/rethink-app/releases)
- NetGuard: PCAP export, **PRO**. — [README](https://github.com/M66B/NetGuard)
- **Decryption limit:** apps targeting API 24+ trust only system CAs by default ("apps targeting Android 6.0 (API level 23) and lower also trust the user-added CA store by default"). — [Network security config](https://developer.android.com/privacy-and-security/security-config)

**8. Tracker detection**
- **Static** (APK contents):
  - Exodus, stale. — [repo](https://github.com/Exodus-Privacy/exodus-android-app)
  - ClassyShark3xodus, stale. — [IzzyOnDroid](https://apt.izzysoft.de/fdroid/index/apk/com.oF2pks.classyshark3xodus?repo=main)
  - TrackerControl library analysis. — [README](https://github.com/TrackerControl/tracker-control-android)
  - App Manager scanner. — [docs](https://muntashirakon.github.io/AppManager/en/)
- **Dynamic** (live traffic):
  - TrackerControl: DNS, plus SNI in Research mode. — [README](https://github.com/TrackerControl/tracker-control-android)
  - RethinkDNS blocklists. — [README](https://github.com/celzero/rethink-app)
  - PCAPdroid malware blacklists, paid. — [paid_features](https://emanuele-f.github.io/PCAPdroid/paid_features) (snippet)
- TrackerControl's README notes that static analysis "can reveal tracking that does not appear during lab testing". — [README](https://github.com/TrackerControl/tracker-control-android)

**9. Wi-Fi channel analysis**
- WiFiAnalyzer: free, GPL, no ads, 2.4/5/6 GHz, up to 320 MHz. — [repo](https://github.com/VREMSoftwareDevelopment/WifiAnalyzer), [releases.atom](https://github.com/VREMSoftwareDevelopment/WifiAnalyzer/releases.atom)
- WiFiman: Wi-Fi scanning, LAN discovery, port scanning. — [APKMirror 2.8.0](https://apkmirror.com/apk/ubiquiti-networks-inc/wifiman/wifiman-2-8-0-release/wifiman-2-8-0-android-apk-download)

**10. Cell signal, band and 5G info**
- Network Survey: free, logs all RATs, NR band reference. — [releases.atom](https://github.com/christianrowlands/android-network-survey/releases.atom)
- Network Cell Info: ads; paid version; **Pro subscription** for band/xARFCN/RSRQ logging. — [AppFollow](https://apps.appfollow.io/android/network-cell-info-mobile-wifi-signal/com.wilysis.cellinfo?country=se)
- NetMonster: through netmonster-core heuristics. — [netmonster-core](https://github.com/mroczis/netmonster-core)

**11. Speed tests**
- Ookla: free, with ads; ad-free pricing unclear. — [APKMirror](https://www.apkmirror.com:443/apk/ookla/speedtest/speedtest-4-6-4-release/speedtest-by-ookla-4-6-4-2-android-apk-download/)
- WiFiman: internet and LAN tests. — [APKMirror 1.6.9](https://www.apkmirror.com/apk/ubiquiti-networks-inc/wifiman/wifiman-1-6-9-release/wifiman-1-6-9-android-apk-download)

**12. Latency / ping monitoring**
- WiFiman shows ping latency and packet loss in its connection details. — [APKMirror 2.8.0](https://apkmirror.com/apk/ubiquiti-networks-inc/wifiman/wifiman-2-8-0-release/wifiman-2-8-0-android-apk-download)
- No maintained app focused on continuous latency monitoring was identified in this pass.

**13. Permission usage history (mic, camera, location)**
- The native Privacy dashboard keeps 24 h or 7 days, covering Location, Camera and Microphone only. — [Android Authority](https://www.androidauthority.com/privacy-dashboard-7-day-3507535/), [9to5Google](https://9to5google.com/?p=650676)
- OpTrace (Shizuku/root) aims at a persistent app-ops timeline but has no release yet. — [GitHub](https://github.com/jksalcedo/optrace)

**14. App-ops audit**
- App Ops (Rikka) needs ADB or Shizuku. — [APKMirror](https://apkmirror.com/apk/xingchen-rikka/app-ops-permission-manager/app-ops-permission-manager-2-1-10-release)
- App Manager flags dangerous app ops and permissions. — [docs](https://muntashirakon.github.io/AppManager/en/)

**15. Sensitive-permission inventory**
- Permission Pilot: free overview, Pro via IAP. — [myperm.darken.eu](https://myperm.darken.eu/), [releases.atom](https://github.com/d4rken-org/permission-pilot/releases.atom)
- Exodus: permissions per app. — [repo](https://github.com/Exodus-Privacy/exodus-android-app)

**16. Security posture checks**
- Pixel Security & privacy hub: status, alerts, recommendations. — [Google Help](https://support.google.com/android/answer/13985942?hl=en) (snippet)
- Play Integrity API Checker: verdicts. — [XDA](https://www.xda-developers.com/play-integrity-api-checker-android-app/)
- Auditor: hardware attestation including verified-boot state. — [Auditor release notes](https://newreleases.io/project/github/GrapheneOS/Auditor/release/76)
- Key Attestation: certificate chain, Keymint, revocation list. — [releases.atom](https://github.com/vvb2060/KeyAttestation/releases.atom)

**17. APK signature verification**
- AppVerifier: free, ISC. — [repo](https://github.com/soupslurpr/AppVerifier)

**18. Malware scanning**
- Play Protect (built in), including on-device live threat detection. — [Android Authority](https://androidauthority.com/google-play-protect-live-threat-detection-3443252)
- Malwarebytes: on-demand scanning free, real-time protection paid. — [MalwareTips](https://malwaretips.com/download-malwarebytes-for-android)
- Hypatia: discontinued. — [AlternativeTo](https://www.alternativeto.net/news/2024/12/secure-mobile-operating-system-divestos-ends-after-a-decade-of-development/)

**19. Clipboard-access alerts**
- Native since Android 12: a toast appears each time an app calls `getPrimaryClip()` on another app's data. — [Android Developers Blog](https://android-developers.googleblog.com/2021/05/android-security-and-privacy-recap.html?hl=es_419&m=1), [9to5Google](https://9to5google.com/2021/05/18/android-12-beta-1-clipboard-notifications-are-coming-and-toasts-now-show-their-source/)
- CVE-2024-23708 allowed the toast to be suppressed on unpatched Android 12–14. — [SentinelOne](https://www.sentinelone.com/vulnerability-database/cve-2024-23708/)

**20. Notification logging**
- Open-source NotificationLog and Alfio010's listener; closed-source Zipoapps. — [AlternativeTo](https://www.alternativeto.net/software/notificationlog/about/), [ecosyste.ms](https://awesome.ecosyste.ms/projects/github.com%2FAlfio010%2Fnotification-listener-android), [APKMirror](https://apkmirror.com/apk/zipoapps/notification-history-log/notification-history-log-16-3-0-release)
- Android 15 redacts OTP notifications for untrusted listeners. — [Android 15 behaviour changes](https://developer.android.com/about/versions/15/behavior-changes-all)

### Inferences
- **Free/paid summary:** paywalls in this market cluster around four things:
  - Logs and forensics: NetGuard PRO logs, PCAPdroid PCAPng.
  - Active protection: PCAPdroid firewall and malware detection, Malwarebytes real-time, AdGuard in-app blocking.
  - Cloud or VPN services: Blokada Cloud/Plus, Rethink RPN, Ookla VPN.
  - Professional radio logging: Network Cell Info Pro.
- Basic monitoring (usage history, alerts, speed indicator, Wi-Fi analysis, integrity checks) is free almost everywhere. A personal app gains nothing by reproducing paid tiers. Its value would come from **integration and long-term history**.
- **Two data paths:** any feature that needs domains or IPs per app requires the single VPN slot. Per-app byte counts, history and caps can come from NetworkStatsManager without a VPN, but only with hour-scale buckets (see Q4). A dual design looks natural: a passive NetworkStatsManager layer that is always on, plus an optional VPN capture layer.
- **Poorly covered areas:**
  - Permission-usage history beyond 7 days.
  - App-ops timelines (only an MVP exists).
  - Continuous latency/jitter monitoring.
  - A consolidated "security posture" checklist (patch age, bootloader/verified boot via attestation, Play Integrity verdict, developer options/ADB, sideloading sources).
  - Notification history that survives beyond the system's own history.

### Gaps
- Exact native retention of Android's built-in notification history, and per-app data-usage UI details on Android 16/17 Pixel. support.google.com was unreachable and the search budget ran out.
- Whether GlassWire still offers a status-bar speed meter and per-host logs in 2026. No primary listing was fetched.
- Whether WiFiman remains completely free with no subscription in 2026.

## Q3. What the Pixel 8 Pro and Android already provide natively, and how it interacts with third-party tools

### Takeaway
On a Pixel 8 Pro running Android 16/17, Google already ships the following:
- A Privacy dashboard with 24 h / 7-day history for location, camera and mic.
- Mic/camera indicators and clipboard-read toasts.
- A unified Security & privacy hub.
- Play Protect with on-device live threat detection.
- Advanced Protection, which covers USB, 2G, WEP, MTE and sideloading.
- A built-in VPN by Google.

That VPN competes for the single VPN slot with any firewall or monitor.

### Cited Findings
- **Privacy dashboard:**
  - History was limited to 24 h from Android 12.
  - A "Show 7 days" option arrived on Pixels running Android 15 QPR1 with the November 2024 Google Play system update, and appears in Android 16.
  - Coverage is **Location, Camera and Microphone** only. There is no detailed timeline for Calendar, Phone or Contacts.
  - — [Android Authority](https://www.androidauthority.com/privacy-dashboard-7-day-3507535/), [Android Authority A16](https://androidauthority.com/android-16-privacy-dashboard-history-3501353), [9to5Google](https://9to5google.com/?p=650676), [BGR](https://www.bgr.com/2194125/android-privacy-features-permission-manager-uses-guide/)
- **Android 12 privacy controls:** real-time mic/camera indicators, quick toggles that cut off mic and camera, and clipboard-read toasts. — [Android Developers Blog](https://android-developers.googleblog.com/2021/05/android-security-and-privacy-recap.html?hl=es_419&m=1)
- **Security & privacy hub:**
  - Shows an overall status, risk alerts and recommendations.
  - Sections: Accounts security, System updates, Device finders, Privacy, More security & privacy.
  - Reached Pixel with Android 13 QPR1 in December 2022.
  - — [Google Help](https://support.google.com/android/answer/13985942?hl=en) (snippet), [9to5Google](https://9to5google.com/2022/12/20/pixel-security-privacy-settings/)
- **Play Protect live threat detection:**
  - On-device AI through Private Compute Core analyses behavioural signals tied to sensitive permissions.
  - Pixel 6 and newer first; initially focused on stalkerware.
  - An APK teardown shows a dedicated page and an alert for apps monitoring location or activity.
  - — [Android Authority](https://androidauthority.com/google-play-protect-live-threat-detection-3443252), [Android Authority rollout](https://www.androidauthority.com/play-protect-live-threat-detection-rollout-3499069/), [Android Authority page](https://www.androidauthority.com/google-live-threat-detection-page-3609356/)
- **Advanced Protection (Android 16):**
  - USB protection while locked; blocks 2G "for supported devices" and WEP.
  - Enables MTE for compatible apps.
  - Sideloading permission cannot be granted.
  - Safe Browsing and spam warnings in Messages.
  - Some features vary by manufacturer.
  - — [Android Authority teardown](https://www.androidauthority.com/android-16-advanced-protection-mode-security-features-apk-teardown-3545203/), [9to5Google](https://9to5google.com/2025/06/04/android-16-advanced-protection/), [Google Help](https://support.google.com/android/answer/16339980?hl=en) (snippet)
- **Mobile network security notifications (Android 16):**
  - Warnings for unencrypted (null-cipher) connections and network requests for IMSI/IMEI.
  - Needs a modem with **IRadio HAL 3.0**.
  - 2025 reports say only Pixel 10, or no shipping device at all, supports it.
  - Android cannot tell whether a tower is fake.
  - — [heise](https://heise.de/-10463947), [TechRadar](https://www.techradar.com/phones/android/android-16-will-soon-flag-fake-cell-towers-and-warn-you-if-someone-is-spying-on-your-phone), [SlashGear](https://www.slashgear.com/1977255/how-android-16-phone-detect-fake-cell-towers-keep-user-safe)
- **VPN by Google on Pixel 8 Pro:**
  - Built in under Settings › Network & internet › VPN, launched first on Pixel 8, renamed from "VPN by Google One".
  - Can pause on selected networks.
  - Shows only a key icon in the status bar.
  - — [9to5Google](https://9to5google.com/2023/10/12/pixel-8-google-one-vpn/), [PhoneArena](https://www.phonearena.com/news/pixel-vpn-drops-google-one-branding-and-is-now-just-pixel-vpn-by-google_id158916), [9to5Google 2025](https://9to5google.com/2025/05/06/google-fi-vpn-pixel/)
- **Exynos 5300 modem (Pixel 8 / 8 Pro):** 4 nm, 3GPP Rel-16 5G NR, FR1 + FR2 (mmWave) and EN-DC. — [SamMobile](https://www.sammobile.com/news/samsung-exynos-modem-5300-5g-launched-faster-efficient/), [FoneArena](https://www.fonearena.com/blog/389467/samsung-exynos-modem-5300-features.html/amp)
- A third-party spec site lists Pixel 8 Pro NR bands n1–n78 plus n257–n261 mmWave (secondary source). — [DeviceBeast](https://devicebeast.com/devices/google-pixel-8-pro-5g-uw-td-lte-us-1tb-g1mnw)
- **Band reporting:** forum users note that band and carrier-aggregation display "depends on what the modem exposes via the APIs". Exynos-based phones show some bands as invisible on certain networks. — [ISPreview forum](https://www.ispreview.co.uk/talk/goto/post?id=361488), [EE community](https://community.ee.co.uk/t5/Mobile-Network-discussions/EE-RAN-Misconfiguration/td-p/1548139/page/2)

### Inferences
- **Starting point for a personal app:** the native Privacy dashboard and Security hub already cover the "what accessed my mic/camera/location in the last 7 days" and "am I up to date" basics. The gap is **longer retention, cross-signal correlation and export**. Examples: correlating permission access with network bursts, or keeping a history beyond 7 days.
- **VPN conflict:** a Pixel 8 Pro user who relies on VPN by Google cannot run NetGuard, Rethink, TrackerControl or PCAPdroid at the same time without root. Rethink's or TrackerControl's own WireGuard/Mullvad chaining is the only no-root way to combine a tunnel with filtering.
- **Null-cipher/IMSI warnings:** these depend on IRadio HAL 3.0 and are reported as Pixel 10-first, so they are probably unavailable on Pixel 8 Pro. A third-party app could not replicate them without privileged telephony access (see Q4).

### Gaps
- Confirmation of which Advanced Protection sub-features (2G block, USB protection, MTE) are active specifically on Pixel 8 Pro under Android 16/17.
- Confirmation of whether Pixel 8 Pro exposes the "Mobile network security" page in 2026.
- Pixel 8 Pro's current Android version and update-support window as of October 2026 (no source fetched).
- Native notification-history retention, and Pixel's "Data warning & limit" UI specifics. The Google Help pages were unreachable.

## Q4. What is impossible or poorly done on Android 14-17 without root, and what Shizuku/ADB or root changes (including constraints for a sideloaded personal app)

### Takeaway
Without root, a monitoring app on a Pixel 8 Pro faces these hard limits:
- One VpnService for the whole device, so no firewall alongside a real VPN, and no visibility into DoT/DoH Private DNS.
- No `/proc/net` access since Android 10, and TrafficStats limited to the calling UID since Android 7.
- Only bucketed, hours-granular per-app history via NetworkStatsManager, behind user-granted Usage Access.
- Wi-Fi scans throttled to 4 per 2 minutes in the foreground (1 per 30 minutes in the background), and scan results need location permission plus location services on.
- Local-network access gated by `ACCESS_LOCAL_NETWORK` for apps targeting Android 17.
- Carrier-aggregation channel info gated by `READ_PRECISE_PHONE_STATE`.
- TLS interception limited to apps that trust user CAs.
- OTP notification content hidden from listeners since Android 15.

Shizuku/ADB unlocks the app-ops and attestation extras. Root unlocks iptables firewalls, capture alongside a VPN and system-CA TLS interception.

For a personal app installed with ADB, the 2026–2027 developer-verification rules do not apply.

### Cited Findings

**VPN slot**
- "There's only one active service per user or profile. Starting a new service, automatically stops an existing service." — [Android VPN guide](https://developer.android.com/develop/connectivity/vpn)
- The user can revoke the VPN at any time, and `onRevoke()` may not run on the main thread. — [Android VPN guide](https://developer.android.com/develop/connectivity/vpn)
- Allowed/disallowed lists cannot be combined, and changing them means establishing a new connection. — [Android VPN guide](https://developer.android.com/develop/connectivity/vpn)
- Apps that bind to a specific network (`bindProcessToNetwork()`, `Network.bindSocket()`) can route outside the tunnel. — [Android VPN guide](https://developer.android.com/develop/connectivity/vpn)
- NetGuard FAQ:
  - Only one app can use the VPN service; chaining is possible through SOCKS5 (Q2).
  - DoH/DoT/Private DNS cannot be intercepted (Q64).
  - Only TCP, UDP and ICMP can be routed; other protocols are dropped (Q27).
  - Traffic is blocked while the VPN restarts to apply rules (Q48).
  - "Block connections without VPN" can break DNS (Q51).
  - Google Play services and Download Manager handle push and downloads on behalf of apps, which affects attribution (Q19/Q66).
  - — [NetGuard FAQ](https://github.com/M66B/NetGuard/blob/master/FAQ.md)
- TrackerControl: Private DNS is not supported alongside it, and attributing system-app activity is difficult. — [README](https://github.com/TrackerControl/tracker-control-android)
- PCAPdroid can capture alongside other VPNs only with root. — [README](https://github.com/emanuele-f/PCAPdroid)

**Traffic accounting APIs**
- "On devices that run Android 10 or higher, apps cannot access `/proc/net`… Apps that need access to this information, such as VPNs, should use the NetworkStatsManager or ConnectivityManager class." — [Android 10 privacy changes](https://developer.android.com/about/versions/10/privacy/changes)
- TrafficStats per-UID counters: "starting in API level N this will only report traffic statistics for the calling UID". Other UIDs return UNSUPPORTED, and filtering is done in the kernel. Use NetworkStatsManager for other UIDs. — [AOSP source mirror](https://gitlab.e.foundation/e/os/android_frameworks_base/-/commit/0d69d3dfe152024fcfea4a61644454702d69e556), [TrafficStats reference](https://developer.android.com/reference/android/net/TrafficStats)
- NetworkStatsManager Javadoc:
  - "By default, apps can only obtain data about themselves."
  - Stats for other apps need `PACKAGE_USAGE_STATS`, which "will not be granted to third-party apps", but the user can grant it through Settings (Usage access).
  - Data is collected in "Buckets", and because they are hours long the method "cannot be used to measure data usage on a fine grained time scale".
  - From API 29 the subscriberId "is guarded by additional restrictions"; pass null to get all mobile networks.
  - — [AOSP NetworkStatsManager.java (android12-release)](https://github.com/aosp-mirror/platform_frameworks_base/blob/android12-release/core/java/android/app/usage/NetworkStatsManager.java)
- The bucket duration is read from `Settings.Global` `netstats_uid_bucket_duration`, with a framework default. — [framework commit](https://gitlab.e.foundation/e/os/android_frameworks_base/-/commit/e4704338bc158ca24f4a99a22a2a1e4bd72db010)

**Device identifiers and clipboard**
- Since Android 10, IMEI, serial number and `getSubscriberId()` need the privileged `READ_PRIVILEGED_PHONE_STATE`; third-party apps get a SecurityException. — [Android 10 privacy changes](https://developer.android.com/about/versions/10/privacy/changes)
- Since Android 10, only the default IME or the focused app can read the clipboard. — [Android 10 privacy changes](https://developer.android.com/about/versions/10/privacy/changes)

**Wi-Fi**
- `startScan()` throttling (Android 9+): **4 scans per 2 minutes per foreground app**; **1 scan per 30 minutes across all background apps**. "Wi-Fi scan throttling" can be turned off in Developer Options (Android 10+). — [Wi-Fi scanning doc](https://developer.android.com/develop/connectivity/wifi/wifi-scan)
- Scan results on Android 10+ need `ACCESS_FINE_LOCATION`, `ACCESS_WIFI_STATE`/`CHANGE_WIFI_STATE` **and location services enabled**. — [Wi-Fi scanning doc](https://developer.android.com/develop/connectivity/wifi/wifi-scan)
- Android 13+ `NEARBY_WIFI_DEVICES` (with `neverForLocation`) does **not** replace fine location for `getScanResults()`/`startScan()`. — [Wi-Fi permissions doc](https://developer.android.com/develop/connectivity/wifi/wifi-permissions)
- Apps targeting Android 10+ cannot toggle Wi-Fi (`setWifiEnabled()` returns false), and `getConfiguredNetworks()` returns an empty list. — [Android 10 privacy changes](https://developer.android.com/about/versions/10/privacy/changes)

**Local network (Android 17)**
- Local-network protections are enforced for apps targeting **Android 17 (SDK 37)**. Apps targeting SDK ≤ 36 with INTERNET get a temporary implicit grant. — [Local network permission](https://developer.android.com/privacy-and-security/local-network-permission)
- Covered traffic: TCP/UDP unicast, multicast and broadcast to LAN, mDNS/SSDP, NsdManager and `.local` resolution. — [Local network permission](https://developer.android.com/privacy-and-security/local-network-permission)
- Exception: the local DNS server on port 53. — [Local network permission](https://developer.android.com/privacy-and-security/local-network-permission)
- LAN scanners must request `ACCESS_LOCAL_NETWORK` at runtime; it belongs to the `NEARBY_DEVICES` group. — [Local network permission](https://developer.android.com/privacy-and-security/local-network-permission)
- TrackerControl already ships Android 17 local-network handling. — [releases](https://github.com/TrackerControl/tracker-control-android/releases)

**Cellular**
- `TelephonyCallback.PhysicalChannelConfigListener` (API 31+) requires `READ_PRECISE_PHONE_STATE` or carrier privileges. — [Android reference](https://developer.android.com/reference/android/telephony/TelephonyCallback.PhysicalChannelConfigListener)
- NetMonster's core relies on hidden APIs and heuristics, and warns that OEMs deviate from documentation. — [netmonster-core](https://github.com/mroczis/netmonster-core)
- Network Survey notes it "won't log all 5 cellular protocols at once". — [IzzyOnDroid](https://apt.izzysoft.de/fdroid/index/apk/com.craxiom.networksurvey?repo=main)

**TLS interception**
- Apps targeting API 24+ trust only system CAs by default, unless their network-security-config adds `src="user"`. — [Network security config](https://developer.android.com/privacy-and-security/security-config)
- PCAPdroid v2.0.1 notes decryption restored "on rooted devices", which is consistent with root-only system-CA injection. — [PCAPdroid releases](https://github.com/emanuele-f/PCAPdroid/releases)

**Notifications, foreground services, developer flags**
- Android 15: untrusted `NotificationListenerService` apps cannot read unredacted content of notifications where an OTP is detected. — [Android 15 behaviour changes](https://developer.android.com/about/versions/15/behavior-changes-all)
- Android 15 also stops network requests started outside a valid process lifecycle. — [Android 15 behaviour changes](https://developer.android.com/about/versions/15/behavior-changes-all)
- Android 15: `dataSync`/`mediaProcessing` foreground services get **6 h per 24 h** for apps targeting 15+. — [FGS timeout doc](https://developer.android.com/develop/background-work/services/fg-service-timeout)
- `Settings.Global.ADB_ENABLED` / `DEVELOPMENT_SETTINGS_ENABLED` are documented (snippet) as "always return 0 for all third-party apps". This conflicts with common Stack Overflow practice of reading them and was not verified on the live reference page. — [Settings.Global reference](https://developer.android.com/reference/android/provider/Settings.Global), [Stack Overflow mirror](https://kiwix.lema.org/stackoverflow.com_en_all_2023-11/questions/31581830/android-how-to-check-if-developer-option-is-enabled)

**Device integrity (Play Integrity, since May 2025 on Android 13+)**
- `MEETS_DEVICE_INTEGRITY` requires a locked bootloader and a certified OS image.
- `MEETS_STRONG_INTEGRITY` additionally requires security updates "in the last year for all partitions", both the OS patch and the vendor patch.
- `MEETS_BASIC_INTEGRITY` can be returned by rooted devices that have key attestation.
- Optional signals (recent device activity, Play Protect status, app access risk) need the app to be installed or updated by Google Play on Android 13+.
- — [Play Integrity improvements](https://developer.android.com/google/play/integrity/improvements)

**What Shizuku/ADB changes**
- App Ops management without root. — [APKMirror App Ops](https://apkmirror.com/apk/xingchen-rikka/app-ops-permission-manager/app-ops-permission-manager-2-1-10-release)
- An app-ops access timeline (OpTrace MVP). — [OpTrace](https://github.com/jksalcedo/optrace)
- Device-unique attestation, RKP testing and Knox attestation in Key Attestation. — [Key Attestation releases](https://github.com/vvb2060/KeyAttestation/releases.atom)
- Shizuku can auto-start without root on a trusted Wi-Fi network (Android 13+), and boot start needs wireless debugging or root. — [Shizuku releases](https://github.com/RikkaApps/Shizuku/releases.atom), [Shizuku docs](https://shizuku.rikka.app/guide/setup/) (snippet)
- Permission Pilot confirms that third-party apps cannot *change* permissions without root. — [myperm.darken.eu](https://myperm.darken.eu/) (snippet)

**What root changes**
- iptables firewalls such as AFWall+ that do not use the VPN slot. — [AFWall+ releases](https://github.com/ukanth/afwall/releases)
- PCAPdroid root capture alongside another VPN. — [PCAPdroid README](https://github.com/emanuele-f/PCAPdroid)
- InviZible root mode. — [InviZible README](https://github.com/Gedsh/InviZible)
- Root also breaks `MEETS_DEVICE_INTEGRITY` for apps that need it. — [Play Integrity improvements](https://developer.android.com/google/play/integrity/improvements)

**Sideloaded personal app**
- **Developer verification:**
  - Protections start on **30 Sep 2026** in Brazil, Indonesia, Singapore and Thailand, for participating stores only; global rollout from **2027**.
  - A power-user "advanced flow" launched in August 2026: one-time setup, 24 h waiting period, then enable for 7 days or indefinitely.
  - "Apps installed using ADB won't require verification" and "there are no changes to how ADB works".
  - Limited-distribution accounts allow sharing to up to 20 devices without ID or fee.
  - — [Developer verification](https://developer.android.com/developer-verification), [FAQ](https://developer.android.com/developer-verification/guides/faq)

### Inferences
- **What the platform permits** for a Pixel 8 Pro personal app, unrooted:
  1. Always-on passive layer: NetworkStatsManager via Usage Access, for per-app/per-network history, caps and alerts at hour-scale granularity. Live device-wide throughput can come from TrafficStats totals (device-level counters). Per-UID live counters are only available for the app itself.
  2. Optional VPN layer, while the user isn't using VPN by Google or another VPN: per-app connection logs, DNS logging/blocking and firewall rules, as in NetGuard/Rethink.
  3. Radio layer: Wi-Fi scans within throttling limits (foreground bursts) and cell info via public TelephonyManager APIs. Detailed CA/physical-channel data is likely unavailable.
  4. Audit layer: package/permission inventory, static tracker scan (Exodus signatures), signing-certificate hashes, Play Integrity and key attestation, Privacy-dashboard-like history if Shizuku is used for app ops.
- **Sideloading:** because the app is sideloaded with ADB, developer verification does not block it. Notification-listener and Usage-access grants still go through user Settings. Android's "restricted settings" for sideloaded apps may add an extra unlock step for notification listener and accessibility (unverified this pass).
- **Foreground service type:** the Android 15 `dataSync` 6 h cap means a 24/7 speed-meter notification should not use that type. A VPN service runs under the VPN's own lifecycle. The exact FGS type for a non-VPN live speed meter (e.g. `specialUse`) still needs checking.
- **Long-term history:** because NetworkStatsManager buckets are hours long, sub-hour per-app graphs without the VPN are impossible. A personal app should sample and persist its own history (e.g. every few minutes for its own and device totals) for fine timelines, and accept hourly per-app resolution.

### Gaps
- Not verified here: whether Android's "restricted settings" block notification-listener, accessibility or Usage-access grants for ADB-installed apps on Android 15–17. support.google.com was unreachable and the search budget ran out.
- The default `netstats_uid_bucket_duration` value (commonly cited as 2 h) and system retention length were not confirmed from a primary source.
- Behaviour of Private DNS while a VpnService is active, from official documentation. Only app READMEs and FAQs state the incompatibility.
- Whether the `specialUse` foreground-service type has any timeout on Android 15–17.
- Whether `ADB_ENABLED` / `DEVELOPMENT_SETTINGS_ENABLED` really return 0 to third-party apps on Android 16/17. The snippet claim conflicts with common practice and needs on-device testing.
- Whether `READ_PRECISE_PHONE_STATE` can be granted to a sideloaded app via ADB `pm grant` or appops on Pixel 8 Pro. Likely not, since it is a privileged permission; this is an untested inference.
