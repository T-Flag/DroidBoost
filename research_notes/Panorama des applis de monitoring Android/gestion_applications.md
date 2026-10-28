# Gestion des applications Android (uninstall, freeze, debloat, permissions, components, backup, usage stats) — state of the art, October 2026

> Scope: reference tools for managing installed apps, free vs paid features, access level they need, and what Android 14 → 17 allows or blocks on an unrooted Google Pixel 8 Pro. Research date: 2026-10-28.
> Method note: GitHub and developer.android.com were readable directly. Repos were shallow-cloned so READMEs, docs, tag dates and last-commit dates could be read; dates below come from `git log` / `git ls-remote` on the public repos. Many other domains were **not reachable** from this session: play.google.com, androidauthority.com, 9to5google.com, bayton.org, f-droid.org, apkmirror.com, support.google.com, swiftapps.org and adbappcontrol.com. Claims about closed-source/paid apps therefore rely on search-engine snippets of those pages and are flagged "(snippet)". The WebSearch budget also ran out partway through, so some paid-app questions stay open (see Gaps).

## 1. Which tools are the references in 2025-2026, and what are their pricing, licence, access level and maintenance status?

### Takeaway
The active, open-source core of 2026 is **App Manager** (all-in-one, root/ADB, no Shizuku by design), **Canta** (Shizuku debloater), **Hail** (freezer), **Inure** (freemium, root/Shizuku), **LibChecker** (inspection), **Blocker** (components), **UAD-ng** (desktop debloater with the reference package database), **Obtainium / Droid-ify / Aurora / F-Droid** (updates) and **Install with Options** (installer bypasses). The weak point is **Shizuku itself**: upstream has had no release since May 2025, and the fork everyone points to for Android 16/17 (thedjchi) paused maintenance in July 2026. Several historic tools are stale or abandoned: SAI (2021), Sui (2023), Titanium Backup, and Island on GitHub (default branch untouched since 2021).

### Cited Findings

**All-in-one app managers**
- **App Manager** (MuntashirAkon). GPLv3+, free, no paid tier ("Fully reproducible, copylefted libre software") — [GitHub](https://github.com/MuntashirAkon/AppManager). Latest tag v4.1.1, 2026-09-04. Last commit 2026-10-18, so actively maintained — [releases](https://github.com/MuntashirAkon/AppManager/releases).
  - Modes of operation: Auto, Root, ADB over TCP, Wireless debugging (Android 11+, pairs with adbd itself), No-root — [settings docs](https://github.com/MuntashirAkon/AppManager/blob/master/docs/raw/en/pages/settings-page.tex).
  - Shizuku is deliberately **not** supported: "App Manager already supports both ADB and root which in some cases is more capable than Shizuku"; integrating it would add complexity — [FAQ](https://github.com/MuntashirAkon/AppManager/blob/master/docs/raw/en/faq/misc.tex).
  - v4.1.1 release notes mention a fix for "app listing on Android 17" — [releases](https://github.com/MuntashirAkon/AppManager/releases).
- **Inure App Manager** (Hamza417). GPL-3.0, **freemium** — [GitHub](https://github.com/Hamza417/Inure).
  - The app has a 15-day trial (`MAX_TRIAL_DAYS = 0xF`) and then needs the "Full Version" — [TrialPreferences.kt](https://github.com/Hamza417/Inure/blob/master/app/src/main/java/app/simple/inure/preferences/TrialPreferences.kt).
  - The Full Version is sold as a Gumroad licence key, a Play "Full Unlocker", or via GitHub Sponsors / Ko-fi; no price is shown on the repo — [GitHub](https://github.com/Hamza417/Inure).
  - Modes: Root and Shizuku — [GitHub](https://github.com/Hamza417/Inure).
  - Latest build107.3.0 on 2026-10-21. build107.2.3 (2026-09-19) fixed an Image Viewer vulnerability that exposed app-private files (GHSA-wc49-6f34-hjrg) — [releases](https://github.com/Hamza417/Inure/releases).
  - Debloat, bloat indicator, VirusTotal and licence-key activation exist **only in GitHub builds**, not in Play builds — [FEATURES.md](https://github.com/Hamza417/Inure/blob/master/FEATURES.md).
- **Package Manager** (SmartPack / sunilpaulmathew). GPL-3.0, "free, ad-free, and open source". A paid "Pro" listing on Play also exists. v8.0 released 2026-08-25 — [GitHub](https://github.com/SmartPack/PackageManager).
  - Note: the brief attributes "Package Manager" to Hamza417. The open-source app by that name is SmartPack's; Hamza417's app is Inure.
- **SD Maid SE** (d4rken). GPL-3.0, free with a "SD Maid SE Pro" upgrade on Google Play. Its AppControl module manages apps — [GitHub](https://github.com/d4rken-org/sdmaid-se). Last commit 2026-10-22.

**Debloaters**
- **Canta** (samolego). LGPL-3.0, free — [GitHub](https://github.com/samolego/Canta).
  - Needs Shizuku and Android 9+. It uses the UAD list for recommendations and "Detects previously uninstalled apps (even across reinstalls)" — [GitHub](https://github.com/samolego/Canta).
  - v4.0.0 (2026-10-02) is a Kotlin Multiplatform rewrite. It adds a **web version over WebUSB** (Chromium + USB cable); a desktop build is planned — [releases](https://github.com/samolego/Canta/releases).
  - The README links the thedjchi Shizuku fork: "Android 16 might need forked version" — [GitHub](https://github.com/samolego/Canta).
- **Universal Android Debloater Next Generation (UAD-ng)**. GPL-3.0, Rust desktop app over ADB, free — [GitHub](https://github.com/Universal-Debloater-Alliance/universal-android-debloater-next-generation).
  - v1.2.0 (2026-01-10/12) added description search, a single log file, package-state verification with cross-user detection, and keyboard shortcuts. Package lists update automatically on launch — [releases](https://github.com/Universal-Debloater-Alliance/universal-android-debloater-next-generation/releases).
  - Last commit 2026-10-27, so the database is very active.
- **ADB AppControl** (Windows desktop, closed source). Free, plus a paid "Extended" version that adds Debloat Wizard, Process Manager, dark theme, batch install, drag-and-drop presets and "ADB key protection" — [official extended page (snippet)](https://adbappcontrol.com/en/extended/).
  - One key covers a single PC. Price is about US$9.99 per [MajorGeeks (snippet)](https://www.majorgeeks.com/files/details/adb_appcontrol.html); the official page shows "Loading price", so this is unconfirmed.
  - The donate page says Xiaomi devices on Android 17 are no longer supported — [donate page (snippet)](https://adbappcontrol.com/en/donate/).

**Freezers / hibernators**
- **Hail** (aistra0528). GPL-3.0, free — [GitHub](https://github.com/aistra0528/Hail).
  - Releases: v1.11.0 on 2026-08-26; v1.10.0 on 2026-01-01 (multiple tags, Xposed hook to auto-unfreeze suspended apps, KernelSU profiles); v1.9.0 in Sept 2025 (targets Android 15, adds Force-Stop modes, reinstall of uninstalled system apps in Root/Shizuku) — [releases](https://github.com/aistra0528/Hail/releases).
  - Last commit 2026-10-28.
- **Ice Box** (closed source). It relies on a PC-based "Device Owner" grant when unrooted; 3.25.6 notes mention a Shizuku-mode fix. Build 3.30.11 was uploaded 2026-01-07, and the 3.25.x builds target API 34 — [APKMirror (snippet)](https://www.apkmirror.com/apk/ruoxin-he/ice-box-apps-freezer-root/ice-box-apps-freezer-3-25-5-g-release/ice-box-apps-freezer-3-25-5-g-android-apk-download).
- **Brevent** (closed source, "jianyu-studio"). Works through ADB (USB debugging, or Wireless debugging since Android 11). 4.2.26 "use android 16 sdk"; 4.2.29.1 uploaded 2026-01-26; 4.2.30 uploaded 2026-06-24 — [APKMirror (snippet)](https://www.apkmirror.com/apk/jianyu-studio/brevent/brevent-4-2-26-release/brevent-4-2-26-android-apk-download/). I found no source tying Brevent to Shizuku.
- **Greenify** (Oasis Feng): no reliable 2025-2026 information found (see Gaps).

**Work-profile isolation**
- **Island** (oasisfeng). Apache-2.0. The GitHub default branch was last committed 2021-09-02; tag v6.4.2 is dated 2025-04-24 — [GitHub](https://github.com/oasisfeng/island). The README notes that only one DPC can be active at a time ("price of switching DPC is far too heavy").
- **Insular** (FLOSS fork of Island, on F-Droid). Apache-2.0. Features: isolate apps, clone/multi-account, freeze or archive apps, re-freeze shortcuts, hide apps, per-group VPN, prohibit USB — [GitLab](https://gitlab.com/secure-system/Insular). Latest tag v6.4.2-d46911c9 (2025-07-31); last commit 2026-09-11 on `dev-ci`.
- **Shelter**: its repo (gitea.angry.im) and F-Droid page could not be reached (see Gaps).
- **Dhizuku** (iamr0s). GPL-3.0. It shares Device Owner privileges with other apps (Shizuku-like API, DO-based). v2.12.0 on 2026-06-24; last commit 2026-10-28 — [GitHub](https://github.com/iamr0s/Dhizuku).

**Privilege bridges**
- **Shizuku** (RikkaApps). Apache-2.0. The latest upstream tag is **v13.6.0, 2025-05-25**, and the last commit is 2025-06-18. Upstream is therefore effectively stalled for about 16 months — [GitHub](https://github.com/RikkaApps/Shizuku).
  - Many Android 16 bug reports opened Aug–Oct 2026 are still visible. Examples: #2633 "UserService fails to start on 13.6.0 … works on 13.5.4", #2561/#2541 NPE in `LoadedApk.makeApplication`, #2596 disconnects when wireless debugging is enabled, #2443 hangs after Wi-Fi changes — [issues search](https://github.com/RikkaApps/Shizuku/issues?q=android+16).
  - Install with Options also notes "it's been almost a year since 13.6.0 was released" — [GitHub](https://github.com/zacharee/InstallWithOptions).
- **Shizuku fork by thedjchi**. Apache-2.0. Adds start-on-boot (waits for Wi-Fi), a watchdog, TCP mode (`adb tcpip`), and start/stop intents (auth token required since v13.6.0.r1311, 2026-02-04) — [GitHub](https://github.com/thedjchi/Shizuku).
  - v13.7.0 (2026-07-02) adds "Android 17 support" and is the author's last release "for the time being". The README was updated on 2026-07-14 to say maintenance is paused — [releases](https://github.com/thedjchi/Shizuku/releases).
- **Sui** (root/Magisk Shizuku). Last tag v13.5.1 (2023-09-15), last commit 2023-09-30, so **stale** — [GitHub](https://github.com/RikkaApps/Sui).

**Permissions / AppOps / components**
- **App Ops** (Rikka, closed source). Free. The paid stand-alone pro version was deprecated after Nov 2018. Non-root use needs Shizuku; "Pure root mode is removed from v4.0.0"; a Device admin mode exists for Android 9+ — [APKMirror changelog (snippet)](https://www.apkmirror.com/apk/xingchen-rikka/app-ops-permission-manager/app-ops-permission-manager-4-1-0-r1109-d29ee09c-release/).
  - A snippet claimed a "9.0.7" upload in January 2024. This looks inconsistent with the 4.x version line, so it is treated as unverified.
- **Permission Pilot** (d4rken). GPL-3.0, free with a Play upgrade. It is **read-only**: "Android does not allow third-party apps to change permissions on non-rooted devices", so it deep-links to Settings instead — [GitHub](https://github.com/d4rken-org/permission-pilot). Tag v2.2.4-rc0 on 2026-09-10; last commit 2026-10-14.
- **Blocker** (lihenggui). Apache-2.0. Modes: PackageManager (needs root), Intent Firewall (IFW), and Shizuku/Sui ("unmodified normal apps need Shizuku started with root"). Two builds: `foss` and `market` (Firebase) — [GitHub](https://github.com/lihenggui/blocker). v2.0.6447 on 2026-10-20.
- **Bouncer** (temporary permissions, Sam Ruston): no source reachable (see Gaps).

**Inspection**
- **LibChecker**. 2.5.4 (2026-06-17) "Adapted for Android 17". It detects 16 KB-alignment and stripped symbols, analyses an APK from a download link, and records the system Build ID and patch level in snapshots. Shizuku improves DEX-optimisation and installer details — [releases](https://github.com/LibChecker/LibChecker/releases). Last commit 2026-10-28.
- **Activity Launcher** (Adam Szalkowski / ActivityLauncher org). Launches hidden activities and creates shortcuts, with a Shizuku launch plugin. On F-Droid, and on Play as `de.szalkowski.activitylauncher.pro`. 2.4.1 on 2026-07-08 — [GitHub](https://github.com/butzist/ActivityLauncher).

**Backup**
- **Neo Backup** (formerly OAndBackupX). AGPL-3.0. **Requires root**. Backs up and restores apps with data, single or batch, with unlimited schedules and custom lists — [GitHub](https://github.com/NeoApplications/Neo-Backup). 8.3.18 on 2026-05-03; last commit 2026-10-17.
- **Swift Backup** (swiftapps, closed source).
  - Premium unlocks cloud backups of apps, App Labels, custom backup/restore configs and scheduled backups. Google Sign-in is required for cloud and premium features — [APKMirror 5.1.0 (snippet)](https://www.apkmirror.com/apk/swiftapps-org/swift-backup/swift-backup-5-1-0-release/swift-backup-5-1-0-android-apk-download/).
  - Shizuku support since 3.7.0; 5.1.0 adds Wi-Fi backup/restore with Shizuku on Android 11+ — [APKMirror 3.7.0 (snippet)](https://www.apkmirror.com/apk/swiftapps-org/swift-backup/swift-backup-3-7-0-release/).
  - A "Lifetime plan" exists (5.0.8 changelog) and there is a 14-day refund policy — [APKMirror 5.0.8 (snippet)](https://www.apkmirror.com/apk/swiftapps-org/swift-backup/swift-backup-5-0-8-release/swift-backup-5-0-8-android-apk-download/).
  - Without root, app data cannot be backed up or restored per a third-party guide — [DroidWin (snippet)](https://droidwin.com/?p=47596).
- **Titanium Backup**: legacy, root-only. No reachable 2024+ source (see Gaps).

**Installers and update managers**
- **SAI (Split APKs Installer)** (Aefyr). GPL-3.0. Last tag "4.5" and last commit **2021-02-25**, so **abandoned** — [GitHub](https://github.com/Aefyr/SAI).
- **Install with Options** (zacharee). MIT. Uses Shizuku to "install test-only apps, bypass Android 14's target SDK limit, downgrade certain packages", plus split and batch installs. 0.9.2 on 2026-03-14 — [GitHub](https://github.com/zacharee/InstallWithOptions). It points users to **InstallerX Revived** for an advanced installer.
- **Obtainium** (ImranR98). GPL-3.0. "Get Android app updates straight from the source": GitHub, GitLab, Forgejo/Codeberg, F-Droid and third-party repos, IzzyOnDroid, APKPure, Aptoide, Uptodown and more, with release notifications. v1.7.3 on 2026-10-24 — [GitHub](https://github.com/ImranR98/Obtainium).
- **Droid-ify**. GPL-3.0. F-Droid repo client with "Automatic app updates in the background" and installation via Session, Root or Shizuku. v0.7.8 on 2026-09-13 — [GitHub](https://github.com/Droid-ify/client).
- **Aurora Store**. GPL-3.0. Anonymous or personal Play login, device/locale spoofing, Exodus tracker integration, Plexus, update blacklisting, download manager. It "Can not download or update paid apps" or apps using Play Asset Delivery, and its token dispenser is "not super reliable". 4.8.4 on 2026-07-27 — [GitLab](https://gitlab.com/AuroraOSS/AuroraStore).
- **F-Droid client**: actively developed, last commit 2026-10-28 — [GitLab](https://gitlab.com/fdroid/fdroidclient).

### Inferences
- **Shizuku is a single point of failure.** Upstream has been stalled since mid-2025, Android 16 regressions are open, and the main fork is paused. A personal app that depends on Shizuku should also implement App Manager's approach: talk to adbd directly over Wireless debugging, pair once, reconnect after reboot. It should also accept any Shizuku-API-compatible server (upstream, thedjchi fork, Sui, or a future fork). This is an inference from the maintenance data above.
- Paid tiers in this space are thin. Most power features are free and open source (App Manager, Canta, Hail, Blocker, UAD-ng). Paid differentiation sits mainly in Swift Backup (cloud/schedules), Inure (everything after the 15-day trial), ADB AppControl Extended (desktop wizard/process manager), and "Pro/upgrade" donations for SD Maid SE, Permission Pilot and SmartPack.

### Gaps
- Exact prices could not be verified: Swift Backup premium/lifetime, Inure Full Version, SD Maid SE Pro, Permission Pilot upgrade, SmartPack Pro, Activity Launcher Pro, ADB AppControl Extended. play.google.com and the vendor sites were blocked in this session.
- No reachable 2024-2026 source for **Greenify, Titanium Backup, Bouncer, StayFree, ActionDash, App Inspector, Shelter** (current version, price, maintenance). WebSearch budget was exhausted, and APKMirror and F-Droid were blocked.
- LibChecker and Activity Launcher licences were not verified (LICENSE file not retrieved).

## 2. Feature-level coverage: what existing tools offer (free vs paid) for each management function

### Takeaway
Read-only inventory is fully covered without any privilege: list, sort and filter apps; versions and SDKs; signatures; permissions; components; manifest; trackers; native libs; size, last-used and screen-time via Usage Access. App Manager is the most complete free implementation. Every **write** action on other apps needs ADB-level (Shizuku / Wireless-debugging ADB) or root: disable, hide, suspend, force-stop, clear data, revoke permissions, AppOps. Component blocking and full data backup/restore remain **root-only** in practice. Nothing reviewed offers a good **install/uninstall/update history log** apart from Permission Pilot's permission-change watcher and LibChecker snapshots.

### Cited Findings

**Listing, sorting and filtering**
- App Manager sort keys — [main-page docs](https://github.com/MuntashirAkon/AppManager/blob/master/docs/raw/en/pages/main-page.tex):
  - No permission needed: user apps first, app label, package name, last update, shared user ID, target SDK, signature, frozen first, blocked first, backed up first, trackers, last actions, installation date.
  - Requires **Usage Access**: total size, data usage, times opened, screen time, last used.
- App Manager filters: user, system, frozen, stopped (force-stopped), installed, uninstalled, with-rules, among others — [main-page docs](https://github.com/MuntashirAkon/AppManager/blob/master/docs/raw/en/pages/main-page.tex).
- App Manager v4.1.0 added filter-expression-based profiles and Frozen/Unfrozen filters, plus a 200 ms debounced search — [releases](https://github.com/MuntashirAkon/AppManager/releases).
- Permission Pilot: dashboard by category (privacy, security, install source, system); covers all apps including work profile and multi-user; install source; search, sort and filter; export to Markdown, CSV or JSON — [GitHub](https://github.com/d4rken-org/permission-pilot).
- SmartPack Package Manager: separate System/User views; APK path (.apk/.apks/.apkm/.xapk); install and update dates; data and native-lib paths — [GitHub](https://github.com/SmartPack/PackageManager).

**App details and inspection**
- App Manager lists activities, receivers, services, providers, app ops, permissions, signatures and shared libraries. It also offers a tracker/library scanner with tracking-class listing, a manifest viewer, and data usage and storage (Usage Access) — [GitHub](https://github.com/MuntashirAkon/AppManager).
- Inure (free tier during the trial): components, a resources viewer, tracker scan, runtime DEX decompile, deep search across all apps' components, info for non-installed APKs, FOSS licence indicator, Xposed detection — [FEATURES.md](https://github.com/Hamza417/Inure/blob/master/FEATURES.md).
- LibChecker: native libraries and ABIs, 16 KB alignment, a rules database (v44), and snapshots that track per-version changes and size diffs — [releases](https://github.com/LibChecker/LibChecker/releases).
- Aurora shows Exodus trackers before install — [GitLab](https://gitlab.com/AuroraOSS/AuroraStore).

**Uninstall and debloat**
- UAD-ng commands on Android 6+ — [UAD-ng wiki FAQ](https://github.com/Universal-Debloater-Alliance/universal-android-debloater-next-generation/wiki/FAQ):
  - Uninstall: `pm uninstall --user <user> <package>`
  - Restore: `cmd package install-existing --user <user> <package>`
  - Disable: `pm disable-user --user <user>` + `am force-stop` + `pm clear`
  - Enable: `pm enable`
- Canta does the same on-device through Shizuku, and also lists already-uninstalled packages — [GitHub](https://github.com/samolego/Canta).
- App Manager has a Debloater page that shows "unsafe bloatware info" (v4.0.5). It can reinstall existing system or other-user apps through `package:` URIs — [releases](https://github.com/MuntashirAkon/AppManager/releases).
- Hail can reinstall uninstalled system apps in Root/Shizuku modes (v1.9.0) — [releases](https://github.com/aistra0528/Hail/releases).
- Batch uninstall:
  - App Manager batch ops cover freeze, unfreeze, force-stop and uninstall — [main-page docs](https://github.com/MuntashirAkon/AppManager/blob/master/docs/raw/en/pages/main-page.tex).
  - SmartPack offers "Debloat (uninstall system apps) — requires root or Shizuku" — [GitHub](https://github.com/SmartPack/PackageManager).
  - Inure offers uninstall, disable and close through Shizuku, or root — [FEATURES.md](https://github.com/Hamza417/Inure/blob/master/FEATURES.md).

**Freeze, hibernate and force-stop**
- Hail capability matrix (Force-stop / Disable / Hide / Suspend / Uninstall-reinstall system apps) — [Hail README](https://github.com/aistra0528/Hail):
  - Root: all five.
  - Shizuku (adb): Force-stop, Disable, Suspend, Uninstall/Reinstall — **no Hide**.
  - Shizuku (root) or Sui: all five.
  - Device Owner, Dhizuku and Island/Insular: Hide and Suspend only.
  - Privileged system app: Force-stop and Disable.
- Other Hail features: "Any app that has been frozen on Hail will need to be unfrozen by the same working mode"; Device Owner is set with `adb shell dpm set-device-owner com.aistra.hail/.receiver.DeviceAdminReceiver`; tags, auto-freeze, the `hail://` URL scheme and intents for automation — [Hail README](https://github.com/aistra0528/Hail).
- App Manager's freeze/unfreeze and force-stop are Root/ADB-only — [GitHub](https://github.com/MuntashirAkon/AppManager). The UI distinguishes "Disabled (hidden from the launcher)" from "Suspended (grayed out in the launcher)" — [app-details docs](https://github.com/MuntashirAkon/AppManager/blob/master/docs/raw/en/pages/app-details-page.tex).
- SD Maid SE AppControl — [docs](https://github.com/d4rken-org/sdmaid-se/blob/main/docs/appcontrol.md):
  - Force stop "Works best with Root or ADB/Shizuku, otherwise uses the accessibility service".
  - Freeze "Requires root or ADB/Shizuku"; the state persists even if SD Maid is uninstalled.
  - Export APK works without root for unencrypted APKs.

**Clear data/cache, battery and background restrictions, network**
- App Manager, Root/ADB only: clear data or cache; view or change net policy and battery optimisation; revoke runtime and development permissions; change AppOps modes — [GitHub](https://github.com/MuntashirAkon/AppManager).
- App Manager batch "Preventing the background operations" (Android 7+) — [main-page docs](https://github.com/MuntashirAkon/AppManager/blob/master/docs/raw/en/pages/main-page.tex).
- **App Manager v4.1.1: per-app internet blocking via eBPF in root *or ADB* mode.** Rules are lost on reboot and re-applied once ADB reconnects — [releases](https://github.com/MuntashirAkon/AppManager/releases).
- Inure via Shizuku: battery optimisation, clear data/cache, reinstall. Root-only: boot-component manager, IFW tracker blocking, AppOps — [FEATURES.md](https://github.com/Hamza417/Inure/blob/master/FEATURES.md).

**Runtime permissions and AppOps**
- App Ops (Rikka) needs Shizuku when unrooted, and offers templates and an app monitor — [APKMirror (snippet)](https://www.apkmirror.com/apk/xingchen-rikka/app-ops-permission-manager/app-ops-permission-manager-4-1-0-r1109-d29ee09c-release/).
- Permission Pilot is read-only without root. Its "Permission Watcher" reports permission changes on install, update or removal — [GitHub](https://github.com/d4rken-org/permission-pilot).

**Component and tracker blocking**
- App Manager blocks activities, receivers, services and providers **root-only**, and can import native, Watt and Blocker rules — [GitHub](https://github.com/MuntashirAkon/AppManager).
- Blocker supports PM (root), IFW and Shizuku/Sui; it needs root-started Shizuku for normal apps — [GitHub](https://github.com/lihenggui/blocker).
- Inure blocks trackers with IFW (root) — [FEATURES.md](https://github.com/Hamza417/Inure/blob/master/FEATURES.md).

**Backup and restore**
- App Manager — [GitHub](https://github.com/MuntashirAkon/AppManager) and [releases](https://github.com/MuntashirAkon/AppManager/releases):
  - Backup/restore "with data, rules, and extras" is root-only.
  - No-root users get APK backup and share, plus OpenPGP/RSA/ECC/AES encryption.
  - v4.1.0 adds per-package ADB `.ab` data backups that App Manager encrypts itself.
- **However, `adb backup` excludes the data of any app targeting Android 12 (API 31)+ unless it is debuggable** — [Android 12 behaviour changes](https://developer.android.com/about/versions/12/behavior-changes-12).
- Neo Backup requires root — [GitHub](https://github.com/NeoApplications/Neo-Backup). Swift Backup's premium features are listed in Q1 — [APKMirror (snippet)](https://www.apkmirror.com/apk/swiftapps-org/swift-backup/swift-backup-5-1-0-release/swift-backup-5-1-0-android-apk-download/).

**Installation (APK/APKS/APKM/XAPK)**
- App Manager installs APK, APKS, APKM and XAPK (with OBB) and can re-sign APKs — [GitHub](https://github.com/MuntashirAkon/AppManager).
- Inure's installer handles "All split apk format", shows a diff of what changed, checks signatures, blocks trackers at install, has Root/Shizuku/Session backends, and "Bypass[es] the low target sdk restrictions for both Android 14 and Android 15" — [FEATURES.md](https://github.com/Hamza417/Inure/blob/master/FEATURES.md).
- Install with Options: Shizuku-based bypass of the targetSdk block, test-only installs, downgrades, splits and batches — [GitHub](https://github.com/zacharee/InstallWithOptions).

**Update tracking**
- Obtainium covers source-of-truth updates with notifications — [GitHub](https://github.com/ImranR98/Obtainium).
- Droid-ify does background auto-updates from F-Droid repos via Session, Root or Shizuku — [GitHub](https://github.com/Droid-ify/client).
- Aurora covers Play apps, with update blacklisting but no paid apps or Play Asset Delivery apps — [GitLab](https://gitlab.com/AuroraOSS/AuroraStore).

**Usage stats**
- Inure: "View usage stats of apps up to a year and detailed usage stats for up to two weeks" — [FEATURES.md](https://github.com/Hamza417/Inure/blob/master/FEATURES.md).
- App Manager: screen time, times opened, last used and data usage sorts (Usage Access) — [main-page docs](https://github.com/MuntashirAkon/AppManager/blob/master/docs/raw/en/pages/main-page.tex).
- Digital Wellbeing on Pixel is a "hard dependency for the Settings app on Android 12+" (UAD-ng description, rated Advanced) — [uad_lists.json](https://github.com/Universal-Debloater-Alliance/universal-android-debloater-next-generation/blob/main/resources/assets/uad_lists.json).

**Export and logs**
- App Manager exports the app list as Markdown, CSV, JSON or XML, exports blocking rules, and has a logcat viewer — [main-page docs](https://github.com/MuntashirAkon/AppManager/blob/master/docs/raw/en/pages/main-page.tex).
- UAD-ng v1.2.0 keeps a single log file and verifies package state across users — [releases](https://github.com/Universal-Debloater-Alliance/universal-android-debloater-next-generation/releases).

**Hibernation (Android 11+/12+) and archiving (Android 15)**
- Hibernation basics — [App hibernation](https://developer.android.com/topic/performance/app-hibernation):
  - Applies when targetSdk ≥ 30 and the app is unused for "a few months".
  - On Android 12+ it resets runtime permissions, blocks jobs, alarms and FCM, and clears the cache.
  - The user toggle is "Pause app activity if unused" (Android 13+).
  - Exempt: apps without a launcher icon, work-profile copies, DPCs, carrier-privileged apps, and third-party store installers.
  - Developer API: `PackageManagerCompat.getUnusedAppRestrictionsStatus()`.
  - Test with ADB: `cmd app_hibernation get-state` and `device_config put permissions auto_revoke_unused_threshold_millis2`.
- Archiving: "Apps with the `REQUEST_DELETE_PACKAGES` permission can call the `PackageInstaller` `requestArchive` method". Archiving "removes the APK and any cached files, but persists user data". Unarchive requests go to "the responsible installer" — [Android 15 features](https://developer.android.com/about/versions/15/features).
- Android 15 also adds `StorageStats.getAppBytesByDataType()` (for an app's own storage) and `ApplicationStartInfo` — [Android 15 features](https://developer.android.com/about/versions/15/features).

### Inferences
- **"Covered" (well done, free):** inventory with rich sort and filter, details, manifest, trackers and libs (App Manager, LibChecker, Inure); debloat with curated risk ratings (UAD-ng database consumed by Canta and App Manager); freeze via multiple backends (Hail); update tracking from sources (Obtainium); installer bypasses (Install with Options, Inure).
- **"Poorly done / under-served", which are opportunities for a personal monitoring app:**
  1. A persistent **timeline/history of installs, uninstalls, updates and permission changes**. Only Permission Pilot's watcher and LibChecker snapshots approach it.
  2. A **unified dashboard** that cross-references usage stats with size, last-used, hibernation state, archive candidates and background-restriction state.
  3. **Debloat state tracking across OTAs.** UAD-ng says OTAs and factory resets can restore packages, and only UAD-ng v1.2.0 verifies state.
  4. Archiving or hibernation suggestions for unused apps. Archiving through `requestArchive` needs `REQUEST_DELETE_PACKAGES`; whether a non-installer-of-record app can archive arbitrary apps is not confirmed by the docs read.
- AppOps and runtime-permission **changes** are impossible without Shizuku/ADB or root. A no-privilege app can only read and deep-link to Settings, as Permission Pilot does.

### Gaps
- StayFree, ActionDash and Digital Wellbeing feature/pricing details (limits, focus modes, history length, paid tiers) were not retrievable in this session.
- Whether `PackageInstaller.requestArchive` works on apps the caller did not install, and whether `pm archive` exists as a shell command on Android 15/16/17, is not confirmed in the pages read.
- Titanium Backup, Bouncer and Greenify feature sets for 2025-2026 are not documented here.

## 3. Pixel 8 Pro debloat: what bloat exists and what is safe to remove

### Takeaway
UAD-ng's database (5,382 packages; 183 in the "Google" list plus Pixel-specific "Oem" entries) is the reference for Pixel bloat. Most "Recommended" Google items are ordinary Play apps: YouTube, YouTube Music, Google TV, Books, News, Keep, Photos, Drive, Google One, Gemini, Pixel Tips, Data Transfer Tool, Pixel Troubleshooting, Daily Hub, Android Beta Feedback, Pixel Buds companion, carrier apps. Several items rated "Recommended" still power Pixel features: Android System Intelligence, Find My Device, Accessibility Suite. Treat UAD-ng's own warning literally: "Recommended IS NOT ACTUALLY RECOMMENDED".

### Cited Findings

**How the UAD-ng list is organised**
- Size and breakdown: 5,382 entries. Lists: Oem 4,252, Misc 433, Aosp 272, Carrier 242, Google 183. Ratings: Recommended 3,003, Advanced 1,159, Expert 923, Unsafe 297. Counted from [uad_lists.json](https://github.com/Universal-Debloater-Alliance/universal-android-debloater-next-generation/blob/main/resources/assets/uad_lists.json), commit of 2026-10-27.
- Rating definitions — [Debloat-Lists wiki](https://github.com/Universal-Debloater-Alliance/universal-android-debloater-next-generation/wiki/Debloat-Lists):
  - *Recommended*: safe to uninstall and/or available from the built-in store, "you should review them anyways".
  - *Advanced*: breaks obscure or minor functionality, overlays, or replaceable defaults.
  - *Expert*: breaks important functionality but "*should not* boot-loop".
  - *Unsafe*: "extremely high risk of boot-looping".
- The same wiki carries the banner "**'Recommended' IS NOT ACTUALLY RECOMMENDED**". The `dependencies`/`neededBy` fields are not exhaustive, and UAD-ng does not auto-remove dependents.

**Pixel / Google packages rated *Recommended*** (UAD-ng descriptions) — [uad_lists.json](https://github.com/Universal-Debloater-Alliance/universal-android-debloater-next-generation/blob/main/resources/assets/uad_lists.json)
- Pixel-specific: `com.google.android.apps.tips` (Pixel Tips), `com.google.android.apps.pixel.support` (Pixel Troubleshooting), `com.google.android.apps.pixelmigrate` (Data Transfer Tool), `com.google.android.apps.betterbug` (Android Beta Feedback), `com.google.android.apps.wearables.maestro.companion` (Pixel Buds), `com.google.android.apps.work.clouddpc` (Device Policy), `com.google.android.apps.pixel.subzero` (Daily Hub).
- Pixel overlays and setup leftovers: `com.google.pixel.digitalkey.timesync`, `com.google.android.pixel.setupwizard.overlay*`, accent/colour overlays `com.android.theme.color.*`, `com.android.*.overlay.pixel`.
- Google Play apps: `com.google.android.youtube`, `apps.youtube.music`, `videos` (Google TV), `apps.books`, `apps.magazines` (News), `keep`, `apps.photos`, `apps.docs` (Drive), `apps.subscriptions.red` (Google One, "Occasionally runs in the background"), `apps.bard` (Gemini, "safe to remove"), `apps.googleassistant`, `apps.recorder`, `apps.tachyon` (Duo/Meet), `apps.maps`, `gm` (Gmail), `calendar`, `calculator`, `apps.translate`, `apps.tasks`, `apps.fitness`, `play.games`, `ar.core`, `ar.lens`, `apps.weather` ("If removed, 'At a Glance' and the lock screen will still redirect to the Google app").
- Recommended but feature-bearing:
  - `com.google.android.as` (Android System Intelligence): Live Caption, App Predictions, Smart Text Selection, Notification Smart Actions, Screen Attention.
  - `com.google.android.apps.adm` (Find My Device).
  - `com.google.marvin.talkback` (Accessibility Suite).
  - `com.google.android.apps.scone` ("Automatically switches basebands between LTE and 5G on demand").
  - `com.google.android.apps.restore` (restore wizard).
  - `com.google.android.apps.security.securityhub`.
  - `com.google.android.apps.speechservices`.
- Carrier (US variants): 27 Verizon/T-Mobile packages are rated Recommended, e.g. `com.asurion.android.verizon.vms` and `com.motricity.verizon.ssodownloadable`.

**Rated *Advanced*** (functional loss likely) — [uad_lists.json](https://github.com/Universal-Debloater-Alliance/universal-android-debloater-next-generation/blob/main/resources/assets/uad_lists.json)
- `com.google.android.apps.wellbeing` ("hard dependency for the Settings app on Android 12+ for Pixel phones")
- `apps.safetyhub` (Personal Safety: car-crash detection, Emergency SOS)
- `apps.wallpaper.pixel`, `apps.dreamliner` (Pixel Stand / AI wallpaper), `pixel.livewallpaper`, `pixel.avatarpicker`
- `com.android.DeviceAsWebcam` and `com.google.pixel.camera.services` (both "Dependency for Private space on Pixel devices")
- `com.android.pixeldisplayservice`, `com.google.android.pixelnfc` (FeliCa region lock), `com.google.android.repairmode`, `com.google.euiccpixel` (NFC/eSE/eSIM firmware updater), `com.google.pixel.iwlan`, `apps.pixel.creativeassistant`
- Google core apps: Chrome, Messages, Dialer, Contacts, Gboard, Carrier Services (`com.google.android.ims`), Health Connect, Android Auto, Google app, `apps.gcs`, `apps.work.oobconfig`, setup wizards, `mainline.telemetry`.

**Rated *Expert*** — [uad_lists.json](https://github.com/Universal-Debloater-Alliance/universal-android-debloater-next-generation/blob/main/resources/assets/uad_lists.json)
- `com.google.android.apps.turbo` (Device Health Services, "Breaks battery settings")
- `apps.nexuslauncher` (Pixel Launcher), `com.android.vending` (Play Store), `com.google.android.gms`, `com.google.android.gsf`, `com.google.android.webview`, `com.google.android.contactkeys`, `com.google.android.configupdater`
- `com.google.android.apps.camera.services` (Pixel Camera Services), `apps.customization.pixel`
- `overlay.gmsconfig.searchselector` ("Removal will crash most apps like settings, clock, firefox…")
- `overlay.gmsconfig.searchlauncherqs` ("breaks the Settings app for Android 15" on one device)
- `com.google.euiccpixel.overlay.zuma`, Pixel battery-health and VPN overlays.

**Rated *Unsafe*** — [uad_lists.json](https://github.com/Universal-Debloater-Alliance/universal-android-debloater-next-generation/blob/main/resources/assets/uad_lists.json)
- `com.google.android.overlay.pixelconfig2018/2019/2019midyear/common`
- `com.google.android.pixelsystemservice`
- `com.google.android.rkpdapp`
- `com.google.android.wifi.resources(.pixel)`
- `com.android.settings.overlay.pixel2021`
- `com.google.android.overlay.gmsconfig.photos` ("makes it impossible to unlock Android 15" on one device)

**Recent list maintenance for Pixel** — [releases](https://github.com/Universal-Debloater-Alliance/universal-android-debloater-next-generation/releases)
- v1.2.0 changed the ratings of the Pixel setup wizard and "Pixel Private Space dependencies" and added a Gemini/Bard entry.
- v1.1.1 moved Google overlay removals to "expert".

**Disable vs uninstall, and how to recover** — [UAD-ng wiki FAQ](https://github.com/Universal-Debloater-Alliance/universal-android-debloater-next-generation/wiki/FAQ)
- Disable:
  - Safest option, "minimal stability impact".
  - Can silently fail and be restored immediately.
  - Some apps' Enable button is greyed out in Settings, so UAD-ng is needed to restore them.
- Uninstall for user 0:
  - "More likely to trigger instability"; dependents may keep waking the missing app and "drain battery faster".
  - Usually also removes the app's updates.
- "If you're debloating anything other than `Recommended`, you should disable rather than uninstall."
- A **factory reset restores all debloated packages**, because `/system` is read-only.
- A package that comes back immediately is "protected by the system in some way"; one that returns later was probably restored by a system update.
- Recovery method: restore half of recent removals and bisect.
- Field reports on Canta: one user removed about 200 packages and later had Camera crashes, so the advice is to remove a few at a time and test for a day or two. Canta's "Recommended" badge is criticised as misleading — [GIGA (snippet)](https://www.giga.de/tech/versteckte-spione-und-akku-fresser-so-einfach-entfernt-ihr-nervige-schrott-apps--01KECHV2XT9E8Y1AACW06XWSHH), [Kevin Boone (snippet)](https://kevinboone.me/canta.html).
- Canta's README: "No *permanent* bricking (If you uninstall **critical apps**, you can experience bootloop and have to **factory reset**!)" — [GitHub](https://github.com/samolego/Canta).

### Inferences
- A safe Pixel 8 Pro default profile would include only Play-store-reinstallable apps the user doesn't use, Pixel Tips, Data Transfer Tool, Pixel Troubleshooting, Beta Feedback, Daily Hub and carrier apps, using **disable** rather than uninstall.
- Never-touch set: anything Unsafe or Expert plus Digital Wellbeing (Settings dependency), Device Health Services, Pixel camera services and DeviceAsWebcam (Private Space dependencies), and the gmsconfig overlays.
- `euiccpixel.overlay.zuma` is probably Tensor G3-specific, since "zuma" appears to be the G3 platform codename used by the Pixel 8 series. That codename mapping was not verified in this session.
- Our app should embed or consume `uad_lists.json`, as Canta and App Manager do, rather than maintain its own list. It should keep a local "removed packages" registry so they can be re-applied after OTAs.

### Gaps
- No Pixel 8 Pro-specific list (as opposed to generic Pixel/Google) was found. I could not determine which packages actually ship on the husky Android 16/17 image: that needs `pm list packages -s` on the device.
- No authoritative list of Pixel packages that refuse `disable-user` ("protected packages") was found.

## 4. Android 14 / 15 / 16 / 17 restrictions affecting these tools (Shizuku, restricted settings, targetSdk block, pm changes, Pixel protections, bootloop risks)

### Takeaway
Each release has tightened control. Android 14 blocks installs of apps with targetSdk < 23 and stops `killBackgroundProcesses` on other apps. Android 15 raises the targetSdk floor to 24, widens restricted settings, makes force-stop cancel pending intents and widgets, and adds Private Space and archiving. Android 16 left Shizuku upstream with regressions. **Android 17 blocks `pm uninstall --user 0` of preinstalled system apps from a non-root ADB shell** ("only root can delete system app for a particular user"); disabling still works. Android 17's Advanced Protection also blocks the accessibility service for non-accessibility tools. Developer verification has applied to sideloading in four countries since Sept 2026; ADB installs are reported exempt.

### Cited Findings

**Android 14**
- Apps with targetSdk < 23 cannot be installed (`INSTALL_FAILED_DEPRECATED_SDK_VERSION`); bypass with `adb install --bypass-low-target-sdk-block`; apps already installed stay — [Android 14 behaviour changes](https://developer.android.com/about/versions/14/behavior-changes-all).
- The bypass flag was added to AOSP under bug 237321649, and it is reportedly stripped if the caller isn't root or shell. Bayton calls it the only bypass, meant for testing — [OmniROM gerrit mirror of the AOSP commit (snippet)](https://gerrit.omnirom.org/plugins/gitiles/android_frameworks_base/+/476b5433ec76348325d3f04c38e5ecc8c73b28c2%5E2), [Bayton (snippet)](https://bayton.org/android/android-14-minimum-sdk/). On-device, Shizuku-based installers (Install with Options, Inure) apply it because they run as shell.
- `killBackgroundProcesses()` "can now kill only the background processes of your own app" — [Android 14 behaviour changes](https://developer.android.com/about/versions/14/behavior-changes-all).
- MediaStore `OWNER_PACKAGE_NAME` is redacted unless the caller holds `QUERY_ALL_PACKAGES` — [Android 14 behaviour changes](https://developer.android.com/about/versions/14/behavior-changes-all).

**Android 15**
- Install floor raised to targetSdk 24 — [Android 15 behaviour changes](https://developer.android.com/about/versions/15/behavior-changes-all).
- Android 16 did **not** raise it further — [Bayton matrix (snippet)](https://bayton.org/android/android-minimum-targetsdk-matrix/).
- Inure's installer bypasses both the Android 14 and 15 floors — [FEATURES.md](https://github.com/Hamza417/Inure/blob/master/FEATURES.md).
- Stopped state: when an app is force-stopped, "the system also cancels all pending intents" and disables its widgets until next launch; `ApplicationStartInfo.wasForceStopped()` reports it — [Android 15 behaviour changes](https://developer.android.com/about/versions/15/behavior-changes-all). Force-stopping from a manager is therefore more destructive.
- Untrusted `NotificationListenerService` apps can no longer read OTP content — [Android 15 behaviour changes](https://developer.android.com/about/versions/15/behavior-changes-all).
- Private Space:
  - It is a separate user profile, and its apps are hidden "from other apps when the private space is locked" — [Android 15 features](https://developer.android.com/about/versions/15/features).
  - On Pixel, DeviceAsWebcam and Pixel camera services are its dependencies (UAD-ng) — [uad_lists.json](https://github.com/Universal-Debloater-Alliance/universal-android-debloater-next-generation/blob/main/resources/assets/uad_lists.json).

**Restricted settings (Android 13+, widened in Android 15)**
- Background: sideloaded apps hit a "Restricted setting" dialog when the user tries to enable their accessibility service. The user lifts it via App info → ⋮ → "Allow restricted settings". SD Maid SE documents this and links Esper's Android 13 background article — [SD Maid SE setup docs](https://github.com/d4rken-org/sdmaid-se/blob/main/docs/setup.md). Coverage of notification-listener access in Android 13 was not re-verified in this session.
- Android 15 reportedly widened the list. Sideloaded apps "can no longer easily receive permissions such as drawing over other apps, reading usage statistics, or becoming device admins". The restriction "can still be manually disabled on a per-app basis", and the Android 15 CDD makes restricted settings a default requirement. An app counts as sideloaded when it is installed without the session-based installer API that stores use — [Android Authority (snippet)](https://androidauthority.com/android-15-restricted-settings-sideloading-3481098), [9to5Google (snippet)](https://9to5google.com/2024/09/12/android-15-sideloaded-apps-restrictions/).
- *Conflict / uncertainty:* "Enhanced Confirmation Mode" was described from Beta 1.1 code as a stricter allowlist-based successor that had not yet been deployed — [Android Authority ECM (snippet)](https://www.androidauthority.com/android-15-enhanced-confirmation-mode-3436697). I could not confirm its shipped status.
- Practical detail from SD Maid SE — [setup docs](https://github.com/d4rken-org/sdmaid-se/blob/main/docs/setup.md):
  - The "Allow restricted settings" menu item only appears after the user has seen the "Restricted setting" dialog at least once and navigated to the app's settings page by hand.
  - An installer can avoid the restriction by setting `INSTALL_ALL_WHITELIST_RESTRICTED_PERMISSIONS`, which Install with Options can do via Shizuku.
  - SD Maid SE can re-enable its accessibility service itself after `pm grant … WRITE_SECURE_SETTINGS`.

**Android 16**
- JobScheduler runtime quotas are now enforced even for jobs that started while visible or that run with a foreground service; broadcast priority is no longer guaranteed across processes; intent-redirection hardening — [Android 16 behaviour changes](https://developer.android.com/about/versions/16/behavior-changes-all).
- Shizuku upstream 13.6.0 has multiple open Android 16 UserService/NPE/disconnect reports — [issues](https://github.com/RikkaApps/Shizuku/issues?q=android+16).
- Canta's README says Android 16 "might need" the thedjchi fork — [GitHub](https://github.com/samolego/Canta).

**Android 17**
- **Uninstall restriction:**
  - From a normal ADB shell, `pm uninstall -k --user 0 <system pkg>` can fail with "Failure [only root can delete system app for a particular user]". It was first reported in the UAD-ng community on 2026-05-25 and announced on 2026-07-27.
  - "Disabling will continue to work as well", and "root will continue to work" — [UAD-ng discussion #1440](https://github.com/Universal-Debloater-Alliance/universal-android-debloater-next-generation/discussions/1440).
  - Issue #1436 ("Uninstall doesn't work on Android 17", opened 2026-07-22, first seen on Xiaomi HyperOS) has a Pixel 9 user confirming. Suggested workaround: `pm disable-user --user 0` — [issue #1436](https://github.com/Universal-Debloater-Alliance/universal-android-debloater-next-generation/issues/1436).
  - The UAD-ng FAQ now links both threads under "Only root can delete system app for a particular user" — [FAQ](https://github.com/Universal-Debloater-Alliance/universal-android-debloater-next-generation/wiki/FAQ).
  - *Conflicting report:* one user says Canta with Shizuku v13.7.0-thedjchi still uninstalled YouTube Music for the current user on a **Pixel 9a running Android 17**. Another says Canta works as root or via Shizuku and that UAD can restore its removals "for now at least" — [discussion #1440](https://github.com/Universal-Debloater-Alliance/universal-android-debloater-next-generation/discussions/1440). These are unverified single-user claims.
- The official Android 17 "all apps" behaviour-changes page does not mention this `pm` change. It covers memory limits (`am memory-limiter`), blocked cross-profile loopback (affects work-profile tools), the SMS OTP 3-hour delay, and keystore limits — [Android 17 behaviour changes](https://developer.android.com/about/versions/17/behavior-changes-all).
- **Advanced Protection:** "On Android 17 and newer, Google's Advanced Protection mode blocks apps that aren't dedicated accessibility tools … from using the accessibility service", and cleaners are explicitly excluded — [SD Maid SE setup docs](https://github.com/d4rken-org/sdmaid-se/blob/main/docs/setup.md).

**Shizuku lifecycle without root**
- Wireless-debugging start (Android 11+) must be redone after every reboot "due to system limitations" — [Shizuku manual (snippet)](https://shizuku.rikka.app/guide/setup/).
- The thedjchi fork's start-on-boot is "Available only when using Wireless Debugging or Root mode". It waits for Wi-Fi, needs `RECEIVE_BOOT_COMPLETED`, a foreground service and a battery-optimisation exemption — [GitHub](https://github.com/thedjchi/Shizuku).
- An Android Authority article dated 2026-09-14 covers restarting Shizuku (fork) via Wireless debugging — [Android Authority (snippet)](https://androidauthority.com/how-to-restart-shizuku-3708818).
- App Manager's eBPF internet rules are lost on reboot and re-applied once ADB reconnects — [releases](https://github.com/MuntashirAkon/AppManager/releases).

**Developer verification (sideloading)**
- New rules went live in Sept 2026 in Brazil, Indonesia, Singapore and Thailand; they expand globally in 2027 — [Android Authority (snippet)](https://www.androidauthority.com/android-developer-verification-rollout-sideloading-flow-3653395), [Thurrott (snippet)](https://www.thurrott.com/mobile/android/334356/google-begins-rolling-out-android-developer-verification).
- An "advanced flow" for non-ADB sideloading was rolling out in Aug 2026 — [9to5Google (snippet)](https://9to5google.com/2026/08/18/google-gradually-rolling-out-androids-advanced-sideloading-ahead-of-developer-verification/).
- ADB installs are described as a bypass / exempt, with no 24-hour wait — [Notebookcheck (snippet)](https://www.notebookcheck.net/Google-confirms-Android-app-sideloading-stays-but-with-a-catch.1130253.0.html), [Android Authority bypass guide (snippet)](https://androidauthority.com/how-to-bypass-android-developer-verification-sideloading-rules-3718002).
- A free hobbyist/student tier exists for small-group distribution (snippet, same sources).
- Install with Options' latest commit (2026-07-10) is "ADI registration", i.e. Android developer identity registration — [GitHub](https://github.com/zacharee/InstallWithOptions).

**Older but still relevant**
- `adb backup` excludes data for apps targeting API 31+ — [Android 12 behaviour changes](https://developer.android.com/about/versions/12/behavior-changes-12).
- `pm hide` needs root since Android 6.0; `pm disable-user` has worked without root since Android 6.0 — [UAD-ng ADB reference](https://github.com/Universal-Debloater-Alliance/universal-android-debloater-next-generation/wiki/ADB-Reference). This matches Hail's table: no Hide in Shizuku (adb) mode.

### Inferences
- For a Pixel 8 Pro on Android 17 without root, the robust debloat primitive is now **`pm disable-user --user 0` plus `pm clear`**, not uninstall. Our app should default to disable and use uninstall only opportunistically, detecting the "only root can delete system app" failure and falling back.
- Because our app is **sideloaded**, it will hit restricted settings for accessibility, notification-listener and (per the Android 15 reports) possibly usage access and overlay. Plan for the manual "Allow restricted settings" flow, or install it with an installer that sets `INSTALL_ALL_WHITELIST_RESTRICTED_PERMISSIONS`. If installed via `adb install` it is also exempt from developer verification per current reports.
- **Do not rely on an AccessibilityService for automation** (e.g. auto-clicking force-stop) if the user enables Advanced Protection on Android 17.
- Any ADB-level privilege (Shizuku or the app's own Wireless-debugging ADB client) is lost at every reboot. Tasks requiring it must be queued until the user, or a Wi-Fi-triggered auto-start, restores the session.

### Gaps
- No primary Google/AOSP source was retrieved for the Android 17 `pm uninstall --user` restriction. Unknown: the exact build (stable vs beta, QPR), whether it is a flag or a CDD requirement, and whether Pixel 8 Pro stable builds enforce it. android.googlesource.com and support.google.com were blocked.
- The exact Android 15+ restricted-settings list (is `PACKAGE_USAGE_STATS` really gated for sideloaded apps on Pixel?) is not confirmed from a primary source. It needs an on-device test.
- I could not determine why upstream Shizuku breaks on some Android 16 builds, or whether Android 17 changed Wireless-debugging behaviour further. A snippet titled "Android restringirá ADB inalámbrico" was low-quality and not used.

## 5. What is impossible or poorly done on Android 14+ without root (Pixel 8 Pro), and what changes with Shizuku/ADB or root?

### Takeaway
On a stock, unrooted Pixel 8 Pro there are three tiers:
- **No privilege:** read-only inventory, Usage Access stats, storage stats, launching or uninstalling *user* apps through system dialogs, deep-links to Settings.
- **ADB tier (Shizuku, or a built-in Wireless-debugging ADB client), lost at each reboot:** disable/enable, suspend, force-stop, clear data/cache, grant/revoke runtime permissions, AppOps, battery and background restrictions, install bypasses, `install-existing`. eBPF per-app network block via App Manager. Uninstall of system apps for user 0 only up to Android 16.
- **Root only:** component blocking via PM/IFW for normal apps, full app-data backup/restore, `pm hide`, shared-prefs editing, SSAID, and uninstalling system apps on Android 17.

### Cited Findings
- App Manager's own split of its features — [GitHub](https://github.com/MuntashirAkon/AppManager):
  - No-root: listing, components view, tracker scan, manifest, install/uninstall APKs, APK backup, logcat, Usage-Access stats.
  - Root/ADB-only: revoke permissions, AppOps, kill/force-stop, clear data/cache, net policy and battery optimisation, freeze.
  - Root-only: component blocking, shared prefs, backup with data, system configs, SSAID.
- Permission Pilot: without root, changing permissions is impossible, so it links to Settings — [GitHub](https://github.com/d4rken-org/permission-pilot).
- Blocker needs Shizuku started **with root** for normal apps — [GitHub](https://github.com/lihenggui/blocker).
- Neo Backup requires root — [GitHub](https://github.com/NeoApplications/Neo-Backup). `adb backup` drops data for apps targeting API 31+ — [Android 12 behaviour changes](https://developer.android.com/about/versions/12/behavior-changes-12).
- Swift Backup without root cannot back up or restore app data per a third-party guide — [DroidWin (snippet)](https://droidwin.com/?p=47596).
- Freezing with Shizuku (adb) supports Disable and Suspend but not Hide; Device Owner (via `dpm set-device-owner` over ADB) and Dhizuku support Hide and Suspend without needing Shizuku after setup — [Hail README](https://github.com/aistra0528/Hail).
- Island/Insular: one DPC at a time, which makes Device-Owner-based tools mutually exclusive — [Island README](https://github.com/oasisfeng/island).
- SD Maid SE without root/ADB falls back to the accessibility service for force-stop and cache cleaning. Android 17 Advanced Protection blocks that service, and with ADB access a full "clean all apps" run works without accessibility — [setup docs](https://github.com/d4rken-org/sdmaid-se/blob/main/docs/setup.md), [AppControl docs](https://github.com/d4rken-org/sdmaid-se/blob/main/docs/appcontrol.md).
- Third-party apps can no longer kill other apps' background processes via API (Android 14) — [Android 14 behaviour changes](https://developer.android.com/about/versions/14/behavior-changes-all).
- Android 17: uninstalling system apps for user 0 from an ADB shell is reported blocked; disable still works; root still works — [UAD-ng #1440](https://github.com/Universal-Debloater-Alliance/universal-android-debloater-next-generation/discussions/1440).
- Hibernation and auto-revoke are system-controlled. Apps can query their own status and send the user to the toggle, but not set other apps' exemption without privilege — [App hibernation](https://developer.android.com/topic/performance/app-hibernation).
- Archiving needs `REQUEST_DELETE_PACKAGES` and routes unarchive to the responsible installer — [Android 15 features](https://developer.android.com/about/versions/15/features).
- Private Space apps are invisible to other apps while it is locked, so inventory tools will miss them — [Android 15 features](https://developer.android.com/about/versions/15/features).
- `QUERY_ALL_PACKAGES` is described as "subject to Google Play policy", and Android filters package visibility per the linked guide — [Android 14 behaviour changes](https://developer.android.com/about/versions/14/behavior-changes-all).
- Disabled or uninstalled-for-user packages "can still be seen by Settings and 3rd-party apps that request `QUERY_ALL_PACKAGES`" — [UAD-ng wiki FAQ](https://github.com/Universal-Debloater-Alliance/universal-android-debloater-next-generation/wiki/FAQ). A sideloaded personal app can simply declare it, since Play policy doesn't bind us (per the brief).

### Inferences
- **Recommended capability architecture for the personal app:**
  1. **Base tier (no privilege):** `QUERY_ALL_PACKAGES` + `PACKAGE_USAGE_STATS`, read-only, always available.
  2. **ADB tier:** an embedded Wireless-debugging ADB client like App Manager's, and/or a Shizuku-API client, for disable, suspend, force-stop, clear, AppOps and permissions. Re-pair or reconnect flow after each reboot.
  3. Root is optional and not assumed.
- Unique value we can add over existing tools:
  - Persistent event history: package add/remove/replace broadcasts plus periodic diffing. No reviewed tool covers this well.
  - Cross-reboot re-application of ADB-tier rules (network blocks, suspensions), since App Manager's eBPF rules reset on reboot.
  - OTA-aware debloat-state checks.
  - "Unused app" recommendations combining usage stats, size and hibernation status.
- Features to avoid promising without root: component/tracker blocking for normal apps, full app-data backup/restore, `pm hide`, and system-app uninstall on Android 17.

### Gaps
- Not verified on-device: whether Android 16/17 Pixel builds still let an ADB shell (`uid 2000`) run `pm suspend`, `appops set` and `pm revoke` for every package. Some might be "protected packages". This needs testing on the Pixel 8 Pro.
- No primary source found on whether Google plans to restrict Wireless debugging/ADB for on-device clients (Shizuku/LADB-style) on Android 17+. Only a low-quality snippet hinted at it, so it was excluded.
