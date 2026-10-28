# Technical feasibility of monitoring and maintenance features on the Google Pixel 8 Pro (husky, Tensor G3), Android 14 to 17, by access level

Scope note (read first):
- Access levels used everywhere below: **A** = normal app (install-time and runtime permissions); **B** = special user-granted access (Usage access, All-files access, overlay, accessibility, notification listener, Device Admin, Device Owner via `adb shell dpm set-device-owner`, or permissions granted once with `adb shell pm grant` / `appops set`); **C** = ADB shell or Shizuku (uid 2000, including hidden system APIs called through `ShizukuBinderWrapper` / UserService); **D** = root (Magisk, KernelSU, APatch).
- Verdict codes: **Y** = works, **P** = partial or with caveats, **N** = does not work, **?** = not verified, test on the device.
- AOSP permission and source facts below come from the `aosp-mirror/platform_frameworks_base` GitHub mirror (main branch). That snapshot is **Android 16-era**: it contains `QUERY_ADVANCED_PROTECTION_MODE` (Android 16) but not `ACCESS_LOCAL_NETWORK` (Android 17). Android 17 behaviour comes from developer.android.com. The canonical source is cs.android.com, which this session could not reach.
- Network limits during research: source.android.com, cs.android.com, android.googlesource.com, support.google.com, store.google.com, blog.google, android-developers.googleblog.com, shizuku.rikka.app, sdmse.darken.eu, Wikipedia, GSMArena, XDA, Android Authority and 9to5Google were all unreachable. The shared web-search quota ran out partway through. Claims that depended on those sources are listed under Gaps.

## Q0. Master feasibility matrix (metric or action vs. access level, Pixel 8 Pro)

### Takeaway
A normal app covers only coarse data on its own: memory totals, thermal status and headroom, battery basics and cycle count, and confirmed uninstall. A few one-time `pm grant`s (PACKAGE_USAGE_STATS, BATTERY_STATS, DUMP, READ_LOGS, WRITE_SECURE_SETTINGS), all marked `development` in AOSP, unlock most monitoring data. Shizuku (uid 2000) unlocks most maintenance actions. Root is only needed for a small remaining set: per-app cache deletion, `pm hide`, raw sysfs/GPU/thermal nodes, and other apps' private data.

### Cited Findings

Permission protection levels, from AOSP `core/res/AndroidManifest.xml`. Anything containing `development` can be granted with `pm grant`, and anything containing `appop` can be toggled with `appops set` or by the user in Settings. Source for all rows in this list: [core manifest](https://github.com/aosp-mirror/platform_frameworks_base/blob/main/core/res/AndroidManifest.xml).
- PACKAGE_USAGE_STATS = `signature|privileged|development|appop|retailDemo`
- BATTERY_STATS = `signature|privileged|development`
- DUMP = `signature|privileged|development`
- READ_LOGS = `signature|privileged|development`
- WRITE_SECURE_SETTINGS = `signature|privileged|development|role|installer`
- GET_APP_OPS_STATS = `signature|privileged|development`
- GET_PROCESS_STATE_AND_OOM_SCORE = `signature|privileged|development`
- SET_PROCESS_LIMIT = `signature|privileged|development`
- INTERACT_ACROSS_USERS = `signature|privileged|development|role`
- READ_DROPBOX_DATA = `signature|privileged|development`
- SYSTEM_ALERT_WINDOW = `signature|setup|appop|installer|pre23|development`
- MANAGE_EXTERNAL_STORAGE = `signature|appop|preinstalled`
- SCHEDULE_EXACT_ALARM = `signature|privileged|appop`
- REQUEST_INSTALL_PACKAGES = `signature|appop`
- WRITE_SETTINGS = `signature|preinstalled|appop|pre23|role`
- Normal (granted at install): QUERY_ALL_PACKAGES, REQUEST_DELETE_PACKAGES, KILL_BACKGROUND_PROCESSES, GET_PACKAGE_SIZE, REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, USE_EXACT_ALARM, GET_TASKS (legacy), RUN_USER_INITIATED_JOBS, FOREGROUND_SERVICE_DATA_SYNC (`normal|instant`), FOREGROUND_SERVICE_SYSTEM_EXEMPTED (`normal|instant`), FOREGROUND_SERVICE_SPECIAL_USE (`normal|appop|instant`)
- Not grantable to a sideloaded app: CLEAR_APP_CACHE (`signature|privileged`), DEVICE_POWER (`signature|role`), REAL_GET_TASKS (`signature|privileged`), READ_NETWORK_USAGE_HISTORY (`signature|privileged`), FORCE_STOP_PACKAGES (`signature|privileged`), SUSPEND_APPS (`signature|role|verifier`), CHANGE_APP_IDLE_STATE (`signature|privileged`), MANAGE_APP_OPS_MODES (`signature|installer|verifier|role`), GRANT_RUNTIME_PERMISSIONS (`signature|installer|verifier`), DELETE_PACKAGES (`signature|privileged|role`), CHANGE_COMPONENT_ENABLED_STATE (`signature|privileged|role`), CLEAR_APP_USER_DATA (`signature|installer`), MANAGE_USERS (`signature|privileged`), INTERNAL_DELETE_CACHE_FILES (`signature`), DELETE_CACHE_FILES (`signature|privileged`, legacy), MANAGE_ACTIVITY_TASKS (`signature|recents`), BIND_ACCESSIBILITY_SERVICE (`signature`)

Permissions held by the shell package (uid 2000, which is what Shizuku runs as in ADB mode), from AOSP `packages/Shell/AndroidManifest.xml`. Source for both rows: [Shell manifest](https://github.com/aosp-mirror/platform_frameworks_base/blob/main/packages/Shell/AndroidManifest.xml).
- **Held:** PACKAGE_USAGE_STATS, BATTERY_STATS, DUMP, READ_LOGS, WRITE_SECURE_SETTINGS, DEVICE_POWER, CLEAR_APP_CACHE, CLEAR_APP_USER_DATA, DELETE_PACKAGES, INSTALL_PACKAGES, FORCE_STOP_PACKAGES, KILL_BACKGROUND_PROCESSES, SUSPEND_APPS, MANAGE_APP_OPS_MODES, GRANT_RUNTIME_PERMISSIONS, REVOKE_RUNTIME_PERMISSIONS, CHANGE_COMPONENT_ENABLED_STATE, INTERACT_ACROSS_USERS_FULL, CHANGE_APP_IDLE_STATE, MANAGE_EXTERNAL_STORAGE, QUERY_ALL_PACKAGES, MANAGE_ACCESSIBILITY, REAL_GET_TASKS, GET_APP_OPS_STATS, UPDATE_APP_OPS_STATS, MANAGE_DEBUGGING, ACCESS_SURFACE_FLINGER, MANAGE_GAME_MODE, READ_DEVICE_CONFIG, WRITE_DEVICE_CONFIG, MANAGE_ACTIVITY_TASKS, DELETE_CACHE_FILES, MANAGE_DEVICE_ADMINS, MANAGE_PROFILE_AND_DEVICE_OWNERS, MANAGE_NOTIFICATIONS, MANAGE_ROLE_HOLDERS, OBSERVE_APP_USAGE, MANAGE_SENSORS
- **Not held** (greps of the mirror found no entries): **MANAGE_USERS**, **INTERNAL_DELETE_CACHE_FILES**, READ_NETWORK_USAGE_HISTORY
- Shizuku's own docs say ADB-mode privilege is defined by this Shell manifest, that it changes between Android versions, and that "Shell (ADB) cannot access other apps' data files `/data/user/0/<package>`". `Shizuku#getUid()` returns 0 for root and 2000 for ADB — [Shizuku-API README](https://github.com/RikkaApps/Shizuku-API)

**Master matrix.** Codes: Y = works, P = partial or with caveats, N = does not work, ? = not verified. "inf." means the verdict is inferred from source code and still needs a test on the device. Rows are grouped in the same order as the detailed sections Q1 to Q8 that follow.

CPU and GPU

| Feature | API / command | A | B | C | D | Sources |
|---|---|---|---|---|---|---|
| Total and per-core CPU load (`/proc/stat`) | read `/proc/stat` | N (since 8.0) | P: `HardwarePropertiesManager.getCpuUsages()` if Device Owner | Y (inf.): shell `cat /proc/stat` / `top`; IHardwarePropertiesManager through DEVICE_POWER | Y | [AndroidCPU #2](https://github.com/souch/AndroidCPU/issues/2), [psutil #1334](https://github.com/giampaolo/psutil/issues/1334), [HPM service](https://github.com/aosp-mirror/platform_frameworks_base/blob/main/services/core/java/com/android/server/HardwarePropertiesManagerService.java) |
| Per-core frequency | `/sys/devices/system/cpu/cpuN/cpufreq/scaling_cur_freq` | Y? (cpufreq fallback used by CPU apps) | – | Y (inf.) | Y | [AndroidCPU #2](https://github.com/souch/AndroidCPU/issues/2) |
| Per-process CPU | `dumpsys cpuinfo`, `top` | N (own process only) | ? (DUMP via pm grant, binder dump) | Y (inf.) | Y | [Shell manifest](https://github.com/aosp-mirror/platform_frameworks_base/blob/main/packages/Shell/AndroidManifest.xml) |
| GPU (Mali-G715) frequency / load | Mali sysfs | ? | ? | ? | Y (inf.) | Gap |

Memory and processes

| Feature | API / command | A | B | C | D | Sources |
|---|---|---|---|---|---|---|
| RAM total / available | `ActivityManager.getMemoryInfo()` | Y | – | Y | Y | [ActivityManager.java](https://github.com/aosp-mirror/platform_frameworks_base/blob/main/core/java/android/app/ActivityManager.java) |
| Other apps' memory | `getProcessMemoryInfo` | N (since Q: own uid only, others zero, rate-limited) | ? (DUMP, then `dumpsys meminfo`) | Y | Y | [ActivityManager.java](https://github.com/aosp-mirror/platform_frameworks_base/blob/main/core/java/android/app/ActivityManager.java) |
| Running processes list | `getRunningAppProcesses` | N (own uid only) | N (REAL_GET_TASKS cannot be granted) | Y (shell holds REAL_GET_TASKS) | Y | [AMS](https://github.com/aosp-mirror/platform_frameworks_base/blob/main/services/core/java/com/android/server/am/ActivityManagerService.java), [ATMS](https://github.com/aosp-mirror/platform_frameworks_base/blob/main/services/core/java/com/android/server/wm/ActivityTaskManagerService.java) |
| Other apps' exit reasons (LMK and so on) | `getHistoricalProcessExitReasons(pkg)` | N | Y (pm grant DUMP) | Y | Y | [ActivityManager.java](https://github.com/aosp-mirror/platform_frameworks_base/blob/main/core/java/android/app/ActivityManager.java) |

Thermal

| Feature | API / command | A | B | C | D | Sources |
|---|---|---|---|---|---|---|
| Thermal status and headroom | `PowerManager.getCurrentThermalStatus`, `getThermalHeadroom`, `getThermalHeadroomThresholds`, `addThermalHeadroomListener` | Y (headroom: NaN if called within 500 ms) | – | Y | Y | [PowerManager.java](https://github.com/aosp-mirror/platform_frameworks_base/blob/main/core/java/android/os/PowerManager.java) |
| Real temperatures (CPU / GPU / battery / skin) | `HardwarePropertiesManager.getDeviceTemperatures` | N | Y if Device Owner | Y (inf.: DEVICE_POWER) | Y | [HPM ref](https://developer.android.com/reference/android/os/HardwarePropertiesManager), [HPM service](https://github.com/aosp-mirror/platform_frameworks_base/blob/main/services/core/java/com/android/server/HardwarePropertiesManagerService.java) |
| All HAL temperatures and cooling devices | hidden `IThermalService.getCurrentTemperatures[WithType]`, `getCurrentCoolingDevices` | N | N | Y (inf.: DEVICE_POWER) | Y | [ThermalManagerService](https://github.com/aosp-mirror/platform_frameworks_base/blob/main/services/core/java/com/android/server/power/ThermalManagerService.java) |
| `dumpsys thermalservice` | binder dump | N | ? (DUMP) | Y | Y | [ThermalManagerService](https://github.com/aosp-mirror/platform_frameworks_base/blob/main/services/core/java/com/android/server/power/ThermalManagerService.java) |
| `/sys/class/thermal/*` | sysfs | ? | ? | ? | Y | Gap |
| Pixel 8 Pro IR thermometer | none known | ? | ? | ? | ? | Gap |

Battery

| Feature | API / command | A | B | C | D | Sources |
|---|---|---|---|---|---|---|
| Battery basics, cycle count | `BatteryManager` properties, `EXTRA_CYCLE_COUNT` (API 34) | Y | – | Y | Y | [BatteryManager ref](https://developer.android.com/reference/android/os/BatteryManager) |
| State of health (property id 10) | `getIntProperty(10)` | P (no permission only if the platform flag is on) | Y (pm grant BATTERY_STATS) | Y | Y | [BatteryService.java](https://github.com/aosp-mirror/platform_frameworks_base/blob/main/services/core/java/com/android/server/BatteryService.java) |
| Manufacturing date (7), first-use date (8), charging policy (9), serial (11), part status (12) | `getLongProperty` / `getIntProperty` with literal ids (`@SystemApi`) | N | Y (pm grant BATTERY_STATS) | Y | Y | [BatteryManager.java](https://github.com/aosp-mirror/platform_frameworks_base/blob/main/core/java/android/os/BatteryManager.java) |

Storage and cleaning

| Feature | API / command | A | B | C | D | Sources |
|---|---|---|---|---|---|---|
| Other apps' sizes (app / data / cache) | `StorageStatsManager.queryStatsForPackage` | N (own only) | Y (Usage access) | Y | Y | [StorageStatsManager](https://developer.android.com/reference/android/app/usage/StorageStatsManager) |
| Whole shared storage | MANAGE_EXTERNAL_STORAGE | N | P (not `Android/data`, not most of `/sdcard/Android`) | ? (shell, test) | Y | [manage-all-files](https://developer.android.com/training/data-storage/manage-all-files) |
| Clear all apps' caches | `freeStorageAndNotify`, `pm trim-caches` | N | P: `ACTION_CLEAR_APP_CACHE` (external caches only, user dialog) or accessibility automation | Y (CLEAR_APP_CACHE) | Y | [PMS](https://github.com/aosp-mirror/platform_frameworks_base/blob/main/services/core/java/com/android/server/pm/PackageManagerService.java), [StorageManager](https://developer.android.com/reference/android/os/storage/StorageManager) |
| Clear one app's cache | `deleteApplicationCacheFiles`, `pm clear --cache-only` | N | P (accessibility automation only) | N (inf.: silently ignored for shell) | Y | [PMS](https://github.com/aosp-mirror/platform_frameworks_base/blob/main/services/core/java/com/android/server/pm/PackageManagerService.java), [PMSC](https://github.com/aosp-mirror/platform_frameworks_base/blob/main/services/core/java/com/android/server/pm/PackageManagerShellCommand.java) |
| Clear another app's data | `pm clear`, `DPM.clearApplicationUserData` | N | Y if Device Owner (API 28) | Y | Y | [DPM](https://developer.android.com/reference/android/app/admin/DevicePolicyManager), [Shell manifest](https://github.com/aosp-mirror/platform_frameworks_base/blob/main/packages/Shell/AndroidManifest.xml) |

App management

| Feature | API / command | A | B | C | D | Sources |
|---|---|---|---|---|---|---|
| List all apps | QUERY_ALL_PACKAGES (normal) | Y | – | Y | Y | [core manifest](https://github.com/aosp-mirror/platform_frameworks_base/blob/main/core/res/AndroidManifest.xml) |
| Uninstall with user confirmation | `ACTION_DELETE` / `PackageInstaller.uninstall` + REQUEST_DELETE_PACKAGES | Y | – | Y | Y | [core manifest](https://github.com/aosp-mirror/platform_frameworks_base/blob/main/core/res/AndroidManifest.xml) |
| Silent uninstall, `pm uninstall [-k] --user 0`, `cmd package install-existing` | DELETE_PACKAGES / INSTALL_PACKAGES | N | ? (Device Owner: `installExistingPackage` exists) | Y | Y | [PMSC](https://github.com/aosp-mirror/platform_frameworks_base/blob/main/services/core/java/com/android/server/pm/PackageManagerShellCommand.java), [DPM](https://developer.android.com/reference/android/app/admin/DevicePolicyManager) |
| `pm disable-user` | CHANGE_COMPONENT_ENABLED_STATE | N | – | Y | Y | [Shell manifest](https://github.com/aosp-mirror/platform_frameworks_base/blob/main/packages/Shell/AndroidManifest.xml) |
| `pm suspend` | SUSPEND_APPS | N | Y if Device Owner (`setPackagesSuspended`) | Y | Y | [DPM](https://developer.android.com/reference/android/app/admin/DevicePolicyManager) |
| `pm hide` | MANAGE_USERS | N | Y if Device Owner (`setApplicationHidden`) | **N** | Y | [PMS](https://github.com/aosp-mirror/platform_frameworks_base/blob/main/services/core/java/com/android/server/pm/PackageManagerService.java) |
| Force-stop | `am force-stop` (FORCE_STOP_PACKAGES) | N (only `killBackgroundProcesses`) | – | Y | Y | [core manifest](https://github.com/aosp-mirror/platform_frameworks_base/blob/main/core/res/AndroidManifest.xml) |
| Grant / revoke runtime permissions, `appops set` | GRANT/REVOKE_RUNTIME_PERMISSIONS, MANAGE_APP_OPS_MODES | N | Y if Device Owner (`setPermissionGrantState`) | Y | Y | [Shell manifest](https://github.com/aosp-mirror/platform_frameworks_base/blob/main/packages/Shell/AndroidManifest.xml) |
| Standby bucket | `am set-standby-bucket` (CHANGE_APP_IDLE_STATE) | N | – | Y | Y | [A16 all](https://developer.android.com/about/versions/16/behavior-changes-all) |
| Archive (Android 15+) | `PackageInstaller.requestArchive` (REQUEST_DELETE_PACKAGES), `pm archive` | Y? | – | Y | Y | [PackageInstaller](https://developer.android.com/reference/android/content/pm/PackageInstaller) |

Usage and network

| Feature | API / command | A | B | C | D | Sources |
|---|---|---|---|---|---|---|
| Usage events | `UsageStatsManager.queryEvents` | N | Y (Usage access) | Y | Y | [UsageStatsManager](https://developer.android.com/reference/android/app/usage/UsageStatsManager) |
| Data usage per uid (history) | `NetworkStatsManager` | own only | Y (Usage access) | Y | Y | [NetworkStatsManager](https://developer.android.com/reference/android/app/usage/NetworkStatsManager) |
| Real-time traffic per uid | `TrafficStats.getUidRxBytes(uid)` | own uid only (since N) | – | ? | Y (inf.) | [TrafficStats](https://developer.android.com/reference/android/net/TrafficStats) |

System

| Feature | API / command | A | B | C | D | Sources |
|---|---|---|---|---|---|---|
| Logcat (all apps) | READ_LOGS | N | P (pm grant; Android 13+ consent dialog only while the app is in the foreground; background requests are declined) | Y | Y | [LogcatManagerService](https://github.com/aosp-mirror/platform_frameworks_base/blob/main/services/core/java/com/android/server/logcat/LogcatManagerService.java) |
| Write secure / global settings | WRITE_SECURE_SETTINGS | N | Y (pm grant) | Y | Y | [core manifest](https://github.com/aosp-mirror/platform_frameworks_base/blob/main/core/res/AndroidManifest.xml) |

### Inferences
- With a one-time ADB session (`pm grant <pkg> android.permission.PACKAGE_USAGE_STATS|BATTERY_STATS|DUMP|READ_LOGS|WRITE_SECURE_SETTINGS`), the app keeps these grants across reboots without Shizuku. That is the cheapest high-value tier for a personal app. This follows from the `development` protection flag, since such grants are persistent runtime-style grants.
- The DUMP grant is especially valuable. It opens `getHistoricalProcessExitReasons()` for all packages, which gives LMK kills and ANRs system-wide, and binder `dump()` on services the app can reach.
- WRITE_SECURE_SETTINGS lets the app toggle `adb_wifi_enabled` (wireless debugging) to bootstrap Shizuku or libadb after reboot. The thedjchi Shizuku fork uses WRITE_SECURE_SETTINGS exactly "to toggle USB and wireless debugging in the background" — [thedjchi/Shizuku](https://github.com/thedjchi/Shizuku).
- The few actions that genuinely need root on this device are `pm hide`, per-app cache deletion, other apps' private data, and probably raw GPU/thermal sysfs nodes. Everything else in the matrix is reachable at level C.

### Gaps
- Whether `untrusted_app` (and `shell`) can read `/sys/class/thermal/thermal_zone*/temp`, the Mali GPU nodes, `/sys/block/zram0/*` and `/proc/pressure/*` under Pixel `zuma` SELinux policy was not verifiable: AOSP/Pixel sepolicy sources were unreachable. Test on the device with `adb shell cat ...` and from an app with `File.readText()`.
- Whether an untrusted app holding DUMP can obtain the `cpuinfo`, `meminfo`, `batterystats` or `thermalservice` binders (an SELinux `service_manager find` rule) was not verified. If it cannot, those dumps need level C.

## Q1. CPU and GPU: per-core frequency, load, per-process CPU, governor, Mali-G715

### Takeaway
Since Android 8.0, `/proc/stat` is unreadable by normal apps, so true CPU load at level A has to be estimated from cpufreq. Device Owner gives per-core usage through HardwarePropertiesManager. Shizuku (shell) gives `/proc/stat`, `top` and `dumpsys cpuinfo`. GPU data on Tensor G3 is unverified and probably needs root.

### Cited Findings
- `/proc/stat` returns `EACCES` for apps on Android 8.0+. The maintainer of the AndroidCPU library wrote: "It seems /proc/stat is no longer accessible since android 8.0", "nothing practical has been propose instead by Android (Android must be rooted or app must be a system app)". The library's workaround is a fallback that "guess[es] cpu usage by looking at the cpu frequency" — [souch/AndroidCPU #2](https://github.com/souch/AndroidCPU/issues/2)
- psutil also tracks "Android Oreo prohibited access to some info from proc" — [psutil #1334](https://github.com/giampaolo/psutil/issues/1334)
- `HardwarePropertiesManager.getCpuUsages()` returns `CpuUsageInfo[]` (per-core active and total time) and `getFanSpeeds()`. The service enforces that the caller is the Device Owner, the current VR listener, or a holder of `DEVICE_POWER`; otherwise it throws `SecurityException("The caller is neither a device owner, nor holding the DEVICE_POWER permission, nor the current VrListener.")` — [HardwarePropertiesManagerService.java](https://github.com/aosp-mirror/platform_frameworks_base/blob/main/services/core/java/com/android/server/HardwarePropertiesManagerService.java); the public doc says only "device owner or the current VR service" — [HardwarePropertiesManager ref](https://developer.android.com/reference/android/os/HardwarePropertiesManager)
- Shell holds DEVICE_POWER — [Shell manifest](https://github.com/aosp-mirror/platform_frameworks_base/blob/main/packages/Shell/AndroidManifest.xml)
- Pixel 8 Pro CPU and GPU per LineageOS device data: "Cortex-X3 & Cortex-A715 & Cortex-A510", 9 cores, `cpu_freq: 1 x 3.0 GHz + 4 x 2.45 GHz + 4 x 2.14 GHz`, GPU "ARM Mali-G715s MC10" — [LineageOS husky.yml](https://github.com/LineageOS/lineage_wiki/blob/main/_data/devices/husky.yml). The frequencies conflict with widely cited Tensor G3 clocks; see Gaps.

### Inferences
- Level A: read `/sys/devices/system/cpu/cpu*/cpufreq/{scaling_cur_freq,scaling_governor,cpuinfo_max_freq,related_cpus}`. CPU-monitor libraries rely on cpufreq after the `/proc/stat` loss, which implies these files are app-readable on the devices tested ([AndroidCPU #2](https://github.com/souch/AndroidCPU/issues/2)). Verify on husky.
- Level A: present load as "frequency utilisation", per cluster, not a true load percentage.
- Level B (Device Owner) or C: `getCpuUsages()` deltas give real per-core load.
- At level C, `IHardwarePropertiesManager.getCpuUsages(callingPackage)` through `ShizukuBinderWrapper` should pass the DEVICE_POWER check because the binder caller becomes uid 2000.
- Per-process CPU at level C: run `dumpsys cpuinfo`, `top -b -n 1 -o PID,CPU,NAME`, or read `/proc/<pid>/stat` in a Shizuku UserService.
- Writing the governor or frequency limits needs root (sysfs write).
- Read cluster topology dynamically from `cpufreq/policyN` and `related_cpus`; do not hardcode it.

### Gaps
- Official Tensor G3 clocks: commonly cited as 1x X3 @ 2.91 GHz, 4x A715 @ 2.37 GHz, 4x A510 @ 1.70 GHz, which contradicts LineageOS's 3.0/2.45/2.14 GHz. Not verified in this session; Google spec pages were blocked.
- Mali GPU sysfs paths and readability on Tensor G3 (for example `/sys/devices/platform/*.mali/` or `/sys/class/misc/mali0/device/`): not found.
- Whether shell can read other apps' `/proc/<pid>/stat` (hidepid and readproc group): not verified.

## Q2. RAM: totals, per-app memory, processes, zRAM, LMK, ApplicationExitInfo (and Android 17 memory limits)

### Takeaway
Totals are free at level A. Per-app memory and the process list are blocked for normal apps since Android 10 and 5.1 respectively. A `pm grant` of DUMP gives system-wide exit reasons, which is a cheap LMK-event proxy. Shizuku gives `dumpsys meminfo` / `procstats` and the full process list.

### Cited Findings
- `getProcessMemoryInfo`: "As of Android Q, for regular apps this method will only return information about the memory info for the processes running as the caller's uid; no other process memory info is available and will be zero. Also of Android Q the sample rate allowed by this API is significantly limited, if called faster the limit you will receive the same data as the previous call." — [ActivityManager.java](https://github.com/aosp-mirror/platform_frameworks_base/blob/main/core/java/android/app/ActivityManager.java)
- `getRunningAppProcesses()` returns all uids only if `mAtmInternal.isGetTasksAllowed(...)`. That check requires REAL_GET_TASKS, or legacy GET_TASKS plus privileged status; otherwise only the caller's own processes come back — [ActivityManagerService.java](https://github.com/aosp-mirror/platform_frameworks_base/blob/main/services/core/java/com/android/server/am/ActivityManagerService.java), [ActivityTaskManagerService.java](https://github.com/aosp-mirror/platform_frameworks_base/blob/main/services/core/java/com/android/server/wm/ActivityTaskManagerService.java)
- REAL_GET_TASKS is `signature|privileged` (not grantable) — [core manifest](https://github.com/aosp-mirror/platform_frameworks_base/blob/main/core/res/AndroidManifest.xml). Shell holds it — [Shell manifest](https://github.com/aosp-mirror/platform_frameworks_base/blob/main/packages/Shell/AndroidManifest.xml)
- `getHistoricalProcessExitReasons(packageName, pid, maxNum)`: "If this package belongs to another UID, you must hold android.Manifest.permission#DUMP in order to retrieve it." — [ActivityManager.java](https://github.com/aosp-mirror/platform_frameworks_base/blob/main/core/java/android/app/ActivityManager.java). DUMP is `development`, so `pm grant` works — [core manifest](https://github.com/aosp-mirror/platform_frameworks_base/blob/main/core/res/AndroidManifest.xml)
- **Android 17 (all apps):** new memory limits based on total device RAM. Apps hitting a limit may stutter, drain battery, or be killed. This is detectable via ApplicationExitInfo with `REASON_OTHER` and a description containing `"MemoryLimiter:AnonSwap"`. Test commands: `am memory-limiter ignore|manual|status` — [Android 17 behavior changes (all apps)](https://developer.android.com/about/versions/17/behavior-changes-all). The summary page adds "App memory runtime limits … Exceeding these limits can cause apps to crash" — [Android 17 summary](https://developer.android.com/about/versions/17/summary)
- Pixel 8 Pro RAM: 12 GB — [LineageOS husky.yml](https://github.com/LineageOS/lineage_wiki/blob/main/_data/devices/husky.yml)

### Inferences
- Level A: `ActivityManager.getMemoryInfo()` (availMem, totalMem, threshold, lowMemory) plus `/proc/meminfo` (MemAvailable, SwapTotal/SwapFree for zRAM), if readable. Not verified for husky.
- LMK events, in three tiers:
  - B (DUMP): poll `getHistoricalProcessExitReasons(null-or-each-pkg, 0, N)` and filter `REASON_LOW_MEMORY`.
  - B (READ_LOGS): follow `lmkd` lines in logcat, but only while the app is in the foreground (see Q8).
  - C: `dumpsys activity exit-info`, `dumpsys meminfo`, `dumpsys procstats --hours 3`.
- A monitoring app with a long-running foreground service, Compose UI and history storage should watch for the Android 17 `MemoryLimiter` exit reason in its own process.

### Gaps
- zRAM details (`/sys/block/zram0/mm_stat`, comp_algorithm) and PSI (`/proc/pressure/memory`) readability for apps on husky: not verified.
- The exact Android 17 per-app memory limit values for a 12 GB device are not published on the pages fetched.

## Q3. Thermal: PowerManager APIs, HardwarePropertiesManager, thermal HAL, sysfs, skin thermometer

### Takeaway
Status and headroom are public at level A, with a 500 ms client-side rate limit. Real degrees Celsius need Device Owner (HardwarePropertiesManager) or Shizuku: the hidden `IThermalService.getCurrentTemperatures*` methods require DEVICE_POWER, which shell holds, and `dumpsys thermalservice` requires DUMP. Whether the Pixel 8 Pro's IR thermometer is reachable by third parties could not be confirmed.

### Cited Findings
- `getThermalHeadroom(@IntRange(from=0,to=60) int forecastSeconds)` is rate-limited on the client side:
  - Implementation: `MINIMUM_HEADROOM_TIME_MILLIS = 500`; it returns `Float.NaN` if called within 500 ms of the last update.
  - Javadoc: "there is no benefit to calling this function more frequently than about once per second, and attempts to call significantly more frequently may result in the function returning NaN"; 1.0 means `THERMAL_STATUS_SEVERE`.
  - Forecasting needs a few seconds of samples.
  - Source: [PowerManager.java](https://github.com/aosp-mirror/platform_frameworks_base/blob/main/core/java/android/os/PowerManager.java)
- The server returns NaN if the HAL isn't ready or `forecastSeconds` is outside [MIN, MAX] — [ThermalManagerService.java](https://github.com/aosp-mirror/platform_frameworks_base/blob/main/services/core/java/com/android/server/power/ThermalManagerService.java)
- `getThermalHeadroomThresholds()` maps each thermal status to a headroom value; "Starting at BAKLAVA [Android 16] the returned map of thresholds can…" (the behaviour changed in 16). There is a new `addThermalHeadroomListener` / `OnThermalHeadroomChangedListener` (`@FlaggedApi(FLAG_ALLOW_THERMAL_THRESHOLDS_CALLBACK)`, "By API version 36, it provides a forecast in the same call"). It fires only on significant changes and "may not be used to fully replace" polling — [PowerManager.java](https://github.com/aosp-mirror/platform_frameworks_base/blob/main/core/java/android/os/PowerManager.java)
- Hidden `IThermalService` methods all enforce `DEVICE_POWER`: `registerThermalEventListener[WithType]`, `getCurrentTemperatures()`, `getCurrentTemperaturesWithType(type)`, `getCurrentCoolingDevices[WithType]` — [ThermalManagerService.java](https://github.com/aosp-mirror/platform_frameworks_base/blob/main/services/core/java/com/android/server/power/ThermalManagerService.java)
- The `thermalservice` dump requires DUMP (`DumpUtils.checkDumpPermission`). It prints "Thermal Status", "Cached temperatures", "HAL Ready", "Current temperatures from HAL", "Current cooling devices from HAL", "Temperature static thresholds from HAL" and "Temperature headroom thresholds" — [ThermalManagerService.java](https://github.com/aosp-mirror/platform_frameworks_base/blob/main/services/core/java/com/android/server/power/ThermalManagerService.java)
- `HardwarePropertiesManager.getDeviceTemperatures(type, source)` (API 24): type is `DEVICE_TEMPERATURE_CPU/GPU/BATTERY/SKIN`, source is `TEMPERATURE_CURRENT/THROTTLING/THROTTLING_BELOW_VR_MIN/SHUTDOWN` — [HardwarePropertiesManager ref](https://developer.android.com/reference/android/os/HardwarePropertiesManager). Callers must be Device Owner, the VR listener, or hold DEVICE_POWER — [HPM service](https://github.com/aosp-mirror/platform_frameworks_base/blob/main/services/core/java/com/android/server/HardwarePropertiesManagerService.java)
- LineageOS lists "Thermometer" among the Pixel 8 Pro peripherals — [LineageOS husky.yml](https://github.com/LineageOS/lineage_wiki/blob/main/_data/devices/husky.yml)
- Android 16 added granular health permissions including `READ_SKIN_TEMPERATURE` (for the health FGS type and body-sensor style data) — [FGS types](https://developer.android.com/develop/background-work/services/fgs/service-types), [Android 16 behavior changes (target 36)](https://developer.android.com/about/versions/16/behavior-changes-16)

### Inferences
- Level A design: poll `getThermalHeadroom(0)` and `getThermalHeadroom(10)` at most once per 1 s (never under 500 ms), compare against `getThermalHeadroomThresholds()`, and listen with `addThermalStatusListener`. Present the result as "thermal budget used %", not degrees.
- Level B, Device Owner: `getDeviceTemperatures(DEVICE_TEMPERATURE_SKIN | CPU | GPU | BATTERY, TEMPERATURE_CURRENT)` gives degrees.
- Level C: `IThermalService.getCurrentTemperatures()` through `ShizukuBinderWrapper(ServiceManager.getService("thermalservice"))` returns every HAL `Temperature` (name, type, value, status). Alternatively run `dumpsys thermalservice` and parse "Current temperatures from HAL".
- Battery temperature is always available at level A through `ACTION_BATTERY_CHANGED` `EXTRA_TEMPERATURE`. This is standard API, not re-verified in this session.
- The `DEVICE_TEMPERATURE_SKIN` value is the thermal HAL's (usually virtual) skin sensor used for throttling. It is very likely distinct from the user-facing IR "Thermometer" sensor.

### Gaps
- No source was found on whether the Pixel 8 Pro IR thermometer is exposed via `SensorManager` (as a public or vendor sensor type) or only to Google's Thermometer app. Check on the device with `adb shell dumpsys sensorservice` and `SensorManager.getSensorList(Sensor.TYPE_ALL)`.
- `/sys/class/thermal/thermal_zone*/{type,temp}` readability under Tensor SELinux for `untrusted_app` vs `shell`: unverified.
- `getThermalHeadroomThresholds()` API level (believed to be 35) not confirmed from docs in this session.

## Q4. Battery: BatteryManager (Android 14+ fields), batterystats

### Takeaway
Cycle count (`EXTRA_CYCLE_COUNT`, API 34) and charging status are public with no permission. State of health (property 10) needs BATTERY_STATS unless the platform flag `state_of_health_public` is on. Manufacturing date, first-use date, charging policy, serial number and part status are `@SystemApi` and need BATTERY_STATS. Because BATTERY_STATS is `development`, a one-time `pm grant` makes all of them readable by a sideloaded app.

### Cited Findings
- `EXTRA_CYCLE_COUNT` ("android.os.extra.CYCLE_COUNT", API 34) and `EXTRA_CHARGING_STATUS` (API 34) are public extras of `ACTION_BATTERY_CHANGED`. `EXTRA_CAPACITY_LEVEL` and `BATTERY_CAPACITY_LEVEL_*` are API 36. Standard properties: `BATTERY_PROPERTY_ENERGY_COUNTER` ("remaining energy in nanowatt-hours"), `BATTERY_PROPERTY_STATUS`. Source: [BatteryManager reference](https://developer.android.com/reference/android/os/BatteryManager)
- Hidden `@SystemApi @RequiresPermission(BATTERY_STATS)` battery properties: `BATTERY_PROPERTY_MANUFACTURING_DATE = 7` (epoch seconds), `BATTERY_PROPERTY_FIRST_USAGE_DATE = 8`, `BATTERY_PROPERTY_CHARGING_POLICY = 9`, `BATTERY_PROPERTY_SERIAL_NUMBER = 11` and `BATTERY_PROPERTY_PART_STATUS = 12` (both `@FlaggedApi(FLAG_BATTERY_PART_STATUS_API)`). `BATTERY_PROPERTY_STATE_OF_HEALTH = 10` is `@FlaggedApi(FLAG_STATE_OF_HEALTH_PUBLIC)`, no `@SystemApi`: "remaining estimated full charge capacity relative to the rated capacity in %". Source: [BatteryManager.java](https://github.com/aosp-mirror/platform_frameworks_base/blob/main/core/java/android/os/BatteryManager.java)
- Enforcement in `BatteryPropertiesRegistrar.getProperty`: for STATE_OF_HEALTH, `if (stateOfHealthPublic()) break;`, otherwise it falls through. For MANUFACTURING_DATE, FIRST_USAGE_DATE, CHARGING_POLICY, SERIAL_NUMBER and PART_STATUS it calls `mContext.enforceCallingPermission(BATTERY_STATS)` — [BatteryService.java](https://github.com/aosp-mirror/platform_frameworks_base/blob/main/services/core/java/com/android/server/BatteryService.java)
- The current public BatteryManager reference page does not list `BATTERY_PROPERTY_STATE_OF_HEALTH` (a parse of the page found no occurrence) — [BatteryManager reference](https://developer.android.com/reference/android/os/BatteryManager)
- BATTERY_STATS = `signature|privileged|development` — [core manifest](https://github.com/aosp-mirror/platform_frameworks_base/blob/main/core/res/AndroidManifest.xml). Shell holds BATTERY_STATS and DUMP — [Shell manifest](https://github.com/aosp-mirror/platform_frameworks_base/blob/main/packages/Shell/AndroidManifest.xml)
- Battery capacity of the Pixel 8 Pro: 5050 mAh Li-Ion (LineageOS data) — [LineageOS husky.yml](https://github.com/LineageOS/lineage_wiki/blob/main/_data/devices/husky.yml)

### Inferences
- In Kotlin, call `bm.getLongProperty(7)`, `bm.getLongProperty(8)` and `bm.getIntProperty(10)` with the literal ids, because the constants are hidden from the SDK. The calls themselves are public methods, so no hidden-API bypass is needed.
- Wrap each read in `try/catch(SecurityException)`. Grant once with `adb shell pm grant <pkg> android.permission.BATTERY_STATS`.
- Reading the serial number and part status needs `getStringProperty` / `getIntProperty`. If `getStringProperty` is hidden, it needs reflection (level A with BATTERY_STATS) or a Shizuku call to `IBatteryPropertiesRegistrar`.
- `dumpsys batterystats` (DUMP plus BATTERY_STATS) at level B or C gives per-uid estimated power use. Parse `--checkin` output for robustness. `BatteryStatsManager` is a system API and therefore only practical via Shizuku.
- A `-1`, `0` or `Long.MIN_VALUE` result means the husky HAL does not populate the field.

### Gaps
- Whether the Pixel 8 Pro health HAL actually reports manufacturing date, first-use date, state of health and serial (as opposed to only Pixel 8a and later): not verified. Test with `dumpsys battery` and the app.
- Whether `FLAG_STATE_OF_HEALTH_PUBLIC` is enabled on Pixel builds of Android 16/17: unknown.

## Q5. Storage: per-app sizes, shared storage, Android/data, clearing caches and data

### Takeaway
Per-app sizes are fine with Usage access (level B). Clearing caches is where levels diverge:
- A or B can only open the system "clear app cache" dialog (external caches), or automate Settings with an accessibility service, as SD Maid SE does without root.
- Shizuku can trim all apps' caches (`pm trim-caches`, `freeStorageAndNotify`). It cannot delete a single app's cache through the API, because shell lacks `INTERNAL_DELETE_CACHE_FILES`.
- Root can do everything.

### Cited Findings
- `StorageStatsManager`: "no permissions are required when calling these APIs for your own package or UID. However, requesting details for any other package requires the android.Manifest.permission#PACKAGE_USAGE_STATS permission … an end user can then choose to grant this permission through the Settings application." — [StorageStatsManager ref](https://developer.android.com/reference/android/app/usage/StorageStatsManager)
- MANAGE_EXTERNAL_STORAGE gives "Write access to all internal storage directories except /Android/data/, /sdcard/Android, and most subdirectories of /sdcard/Android … Apps that are granted this permission still can't access the app-specific directories that belong to other apps". With SAF "you can only access a file or directory if you can do so without having the MANAGE_EXTERNAL_STORAGE permission". On Android 12+, holders of both MANAGE_EXTERNAL_STORAGE and QUERY_ALL_PACKAGES can use `getManageSpaceActivityIntent()` — [Manage all files](https://developer.android.com/training/data-storage/manage-all-files)
- `StorageManager.ACTION_CLEAR_APP_CACHE` (API 30): "Allows the user to free up space by clearing app external cache directories. The intent doesn't automatically clear cache, but shows a dialog and lets the user decide … Requires Manifest.permission.MANAGE_EXTERNAL_STORAGE" — [StorageManager ref](https://developer.android.com/reference/android/os/storage/StorageManager)
- `IPackageManager.freeStorageAndNotify(...)` is annotated `@EnforcePermission(CLEAR_APP_CACHE)` — [PackageManagerService.java](https://github.com/aosp-mirror/platform_frameworks_base/blob/main/services/core/java/com/android/server/pm/PackageManagerService.java). CLEAR_APP_CACHE is `signature|privileged` — [core manifest](https://github.com/aosp-mirror/platform_frameworks_base/blob/main/core/res/AndroidManifest.xml). Shell holds it — [Shell manifest](https://github.com/aosp-mirror/platform_frameworks_base/blob/main/packages/Shell/AndroidManifest.xml). The shell command `pm trim-caches DESIRED_FREE_SPACE [internal|UUID]` exists — [PackageManagerShellCommand.java](https://github.com/aosp-mirror/platform_frameworks_base/blob/main/services/core/java/com/android/server/pm/PackageManagerShellCommand.java)
- `deleteApplicationCacheFilesAsUser`:
  - If the caller lacks `INTERNAL_DELETE_CACHE_FILES` but holds the old `DELETE_CACHE_FILES`: "silently ignoring" (it returns without calling the observer). Otherwise it enforces `INTERNAL_DELETE_CACHE_FILES`.
  - Source: [PackageManagerService.java](https://github.com/aosp-mirror/platform_frameworks_base/blob/main/services/core/java/com/android/server/pm/PackageManagerService.java)
  - INTERNAL_DELETE_CACHE_FILES is `signature`-only. DELETE_CACHE_FILES is documented as an "Old permission … no longer used, but signals for us to quietly ignore calls" — [core manifest](https://github.com/aosp-mirror/platform_frameworks_base/blob/main/core/res/AndroidManifest.xml)
  - Shell has DELETE_CACHE_FILES but not INTERNAL_DELETE_CACHE_FILES — [Shell manifest](https://github.com/aosp-mirror/platform_frameworks_base/blob/main/packages/Shell/AndroidManifest.xml)
- `pm clear [--user USER_ID] [--cache-only] PACKAGE`: with `--cache-only`, `runClear()` calls `mInterface.deleteApplicationCacheFilesAsUser(pkg, userId, obs)` and waits on the observer. Without it, it calls `ActivityManager.clearApplicationUserData` — [PackageManagerShellCommand.java](https://github.com/aosp-mirror/platform_frameworks_base/blob/main/services/core/java/com/android/server/pm/PackageManagerShellCommand.java)
- Device Owner `DevicePolicyManager.clearApplicationUserData(admin, pkg, executor, listener)` (API 28): "equivalent to the target application calling ActivityManager.clearApplicationUserData()" — [DevicePolicyManager ref](https://developer.android.com/reference/android/app/admin/DevicePolicyManager)
- SD Maid SE lists "Deleting expendable files, e.g. caches (AppCleaner)" and "Root and ADB access (Porter or Shizuku) support" — [SD Maid SE README](https://github.com/d4rken-org/sdmaid-se). Its AppCleaner wiki has moved to sdmse.darken.eu and has an "Include inaccessible caches" setting — [SD Maid SE wiki AppCleaner](https://github.com/d4rken-org/sdmaid-se/wiki/AppCleaner)

### Inferences
- **Per-app cache at level C does not work via the API.** Through Shizuku (uid 2000), `IPackageManager.deleteApplicationCacheFiles[AsUser]` is silently ignored, and the observer is never called. The same applies to `pm clear --cache-only` run from adb or Shizuku, which may hang waiting on the observer. Test before relying on it.
- Root passes the check (uid 0 is granted all permissions) and can also `rm -rf /data/data/<pkg>/cache /data/data/<pkg>/code_cache` directly.
- **Global cache trim at level C works:** `pm trim-caches 999G`, or `freeStorageAndNotify(null, Long.MAX_VALUE, 0, observer)` via `ShizukuBinderWrapper`, clears caches of all apps until the target is met.
- **Without root or Shizuku**, SD Maid SE-style per-app clearing means an AccessibilityService that opens `Settings.ACTION_APPLICATION_DETAILS_SETTINGS`, then Storage, then taps "Clear cache". On Android 13+ sideloaded installs this requires the "Allow restricted settings" step (see Q8). Use `ACTION_CLEAR_APP_CACHE` (with All-files access) for external caches in one dialog.
- Data clearing: `pm clear <pkg>` at level C (shell has CLEAR_APP_USER_DATA), or the Device Owner API at level B.
- Android/data and obb: at level B, MANAGE_EXTERNAL_STORAGE explicitly excludes them. At level C, shell file access to `/sdcard/Android/data` is commonly used by Shizuku-aware file managers, but this is not verified here.

### Gaps
- SD Maid SE's exact mechanism and its Android 14/15/16 limitations: its docs site (sdmse.darken.eu) was unreachable.
- The history of the SAF `Android/data` workaround (blocked on Android 13+) and shell access to `Android/data` on 14+: not verified with a primary source.
- Behaviour of `pm clear --cache-only` from adb on Android 16/17 (works, hangs, or is silently ignored): needs a device test.

## Q6. App management: list, uninstall, disable, suspend, hide, force-stop, permissions, appops, buckets, hibernation, archiving

### Takeaway
At level A, listing and user-confirmed uninstall (or archive) are fine. Every silent or administrative action (`pm uninstall --user 0`, `disable-user`, `suspend`, `force-stop`, `grant/revoke`, `appops`, `set-standby-bucket`, `archive`) works at level C, because shell holds the needed permissions. `pm hide` is the exception: it needs MANAGE_USERS, which shell lacks, so it requires root or a Device Owner, which uses `setApplicationHidden`.

### Cited Findings
- QUERY_ALL_PACKAGES and REQUEST_DELETE_PACKAGES are `normal`. KILL_BACKGROUND_PROCESSES is `normal`. FORCE_STOP_PACKAGES is `signature|privileged` — [core manifest](https://github.com/aosp-mirror/platform_frameworks_base/blob/main/core/res/AndroidManifest.xml)
- `pm` shell command syntax — [PackageManagerShellCommand.java](https://github.com/aosp-mirror/platform_frameworks_base/blob/main/services/core/java/com/android/server/pm/PackageManagerShellCommand.java):
  - `install-existing [--user USER_ID|all|current]`
  - `uninstall [-k] [--user USER_ID] [--versionCode VERSION_CODE]`
  - `clear [--user] [--cache-only]`
  - `disable-user [--user USER_ID] PACKAGE_OR_COMPONENT`
  - `hide [--user USER_ID] PACKAGE_OR_COMPONENT`
  - `suspend [--user USER_ID] PACKAGE [PACKAGE...]`
  - `set-distracting-restriction`
  - `grant [--user USER_ID] [--all-permissions] PACKAGE PERMISSION`
  - `set-permission-flags`
  - `trim-caches`
  - `uninstall-system-updates`
  - `archive [--user USER_ID] PACKAGE`
  - `request-unarchive [--user USER_ID] PACKAGE`
- `setApplicationHiddenSettingAsUser` is `@EnforcePermission(MANAGE_USERS)` — [PackageManagerService.java](https://github.com/aosp-mirror/platform_frameworks_base/blob/main/services/core/java/com/android/server/pm/PackageManagerService.java). MANAGE_USERS is `signature|privileged` and is absent from the Shell manifest — [core manifest](https://github.com/aosp-mirror/platform_frameworks_base/blob/main/core/res/AndroidManifest.xml), [Shell manifest](https://github.com/aosp-mirror/platform_frameworks_base/blob/main/packages/Shell/AndroidManifest.xml)
- Shell holds DELETE_PACKAGES, INSTALL_PACKAGES, CHANGE_COMPONENT_ENABLED_STATE, SUSPEND_APPS, FORCE_STOP_PACKAGES, GRANT/REVOKE_RUNTIME_PERMISSIONS, MANAGE_APP_OPS_MODES and CHANGE_APP_IDLE_STATE — [Shell manifest](https://github.com/aosp-mirror/platform_frameworks_base/blob/main/packages/Shell/AndroidManifest.xml)
- `adb shell am set-standby-bucket APP_PACKAGE_NAME active|working_set|frequent|rare|restricted` and `am get-standby-bucket` are documented as testing tools. Android 16 makes job runtime quota depend on the standby bucket, top state and concurrent FGS — [Android 16 behavior changes (all apps)](https://developer.android.com/about/versions/16/behavior-changes-all)
- `PackageInstaller.requestArchive(packageName, statusReceiver)` (API 35): "the apps APKs and cache are removed from the device while the user data is kept … Requires Manifest.permission.DELETE_PACKAGES or Manifest.permission.REQUEST_DELETE_PACKAGES" — [PackageInstaller ref](https://developer.android.com/reference/android/content/pm/PackageInstaller)
- Device Owner equivalents — [DevicePolicyManager ref](https://developer.android.com/reference/android/app/admin/DevicePolicyManager):
  - `setApplicationHidden` (API 21): "When a package is hidden it is unavailable for use, but the data and actual package file remain"
  - `setPackagesSuspended` (API 24)
  - `setPermissionGrantState` (API 23)
  - `installExistingPackage` (API 28)
  - `clearApplicationUserData` (API 28)
  - `setSecureSetting` ("mostly deprecated")
- `dpm set-device-owner` fails if accounts exist: "Not allowed to set the device owner because there are already some accounts" — [DevicePolicyManagerService.java](https://github.com/aosp-mirror/platform_frameworks_base/blob/main/services/devicepolicy/java/com/android/server/devicepolicy/DevicePolicyManagerService.java)

### Inferences
- Implementation plan:
  - Level A: `PackageManager.getInstalledPackages(...)`, `Intent.ACTION_DELETE` / `PackageInstaller.uninstall()`, and `requestArchive()` (Android 15+; expect a user confirmation for non-privileged callers).
  - Level C: run the commands in a Shizuku UserService, or call `IPackageManager` / `IPackageInstaller` / `IActivityManager.forceStopPackage` / `IAppOpsService.setMode` / `IUsageStatsManager.setAppStandbyBucket` through `ShizukuBinderWrapper`.
- Background restriction: at level C, `appops set <pkg> RUN_ANY_IN_BACKGROUND ignore` (the appop behind the "Restricted" battery setting). Not verified in this session.
- Device Owner on a daily-driver Pixel is impractical. It requires removing every account first (or setting it up right after a factory reset), and it constrains later use. Shizuku gives nearly the same powers with less friction.

### Gaps
- App hibernation shell commands (`cmd app_hibernation ...`) and their permissions: not verified.
- Whether a Device Owner gets silent `PackageInstaller.uninstall()`: not verified in this session.

## Q7. Usage and network statistics

### Takeaway
UsageStatsManager and NetworkStatsManager for other apps both work at level B once Usage access is granted, either in Settings or with `appops`/`pm grant`. Real-time per-uid traffic is impossible at level A, because `TrafficStats` for other UIDs returns UNSUPPORTED since Android 7. It needs polling NetworkStatsManager (coarse), Shizuku, or root.

### Cited Findings
- UsageStatsManager: "Most methods on this API require the permission android.permission.PACKAGE_USAGE_STATS … the user of the device still needs to grant permission through the Settings application. See Settings.ACTION_USAGE_ACCESS_SETTINGS." `queryEvents`: "Events are only kept by the system for a few days … Starting from Android R, if the user's device is not in an unlocked state … then null will be returned." `getAppStandbyBucket()` and `queryEventsForSelf()` need no permission — [UsageStatsManager ref](https://developer.android.com/reference/android/app/usage/UsageStatsManager)
- NetworkStatsManager: "Calling querySummaryForDevice(...) or accessing stats for apps other than the calling app requires the permission Manifest.permission.PACKAGE_USAGE_STATS … the user of the device can grant permission through the Settings application" — [NetworkStatsManager ref](https://developer.android.com/reference/android/app/usage/NetworkStatsManager)
- TrafficStats `getUidRxBytes(uid)`: "Starting in Build.VERSION_CODES.N this will only report traffic statistics for the calling UID. It will return UNSUPPORTED for all other UIDs for privacy reasons. To access historical network statistics belonging to other UIDs, use NetworkStatsManager." TCP-level per-uid stats return UNSUPPORTED since JELLY_BEAN_MR2 — [TrafficStats ref](https://developer.android.com/reference/android/net/TrafficStats)
- READ_NETWORK_USAGE_HISTORY is `signature|privileged` — [core manifest](https://github.com/aosp-mirror/platform_frameworks_base/blob/main/core/res/AndroidManifest.xml)

### Inferences
- Device-wide real-time throughput works at level A (`TrafficStats.getTotalRxBytes/TxBytes` deltas every 1 s).
- For a per-app breakdown, poll `NetworkStatsManager.queryDetailsForUid` (level B). Its bucket granularity is coarse and data lags, so it is not real-time.
- Real-time per-app traffic at level C: `dumpsys netstats detail` or `INetworkStatsService`. At level D: read the kernel eBPF traffic maps.
- Granting Usage access by adb: `appops set <pkg> GET_USAGE_STATS allow` or `pm grant <pkg> android.permission.PACKAGE_USAGE_STATS`. Both follow from the `development|appop` flags.

### Gaps
- NetworkStatsManager bucket granularity and update latency on Android 16/17: not verified.

## Q8. System: logcat, dumpsys, settings, overlays, foreground services, background limits, alarms, WorkManager, widgets, QS tiles, accessibility

### Takeaway
- For a 24/7 monitor on Android 14 to 17, use a `specialUse` foreground service. It has no runtime prerequisites and no time limit. Avoid `dataSync`, which has a 6 h / 24 h cap from target 35 and cannot start from BOOT_COMPLETED.
- READ_LOGS via `pm grant` only yields logs when the user approves a dialog while the app is in the foreground. Background log access needs Shizuku.
- Widgets (30 min minimum for `updatePeriodMillis`) and WorkManager (15 min minimum) are too slow for live data. Push widget updates from the foreground service instead.

### Cited Findings
- Logcat on Android 13+, from `LogcatManagerService` — [LogcatManagerService.java](https://github.com/aosp-mirror/platform_frameworks_base/blob/main/services/core/java/com/android/server/logcat/LogcatManagerService.java):
  - `shouldShowConfirmationDialog()` returns true only when `procState == PROCESS_STATE_TOP` ("If the process is foreground, show a dialog for user consent").
  - Otherwise `onAccessDeclinedForClient(client)` runs.
  - The pending confirmation times out (400 s on user builds). Approval status expires after `STATUS_EXPIRATION_TIMEOUT_MILLIS = 60 * 1000`.
- DUMP, READ_LOGS and WRITE_SECURE_SETTINGS are `development` (pm-grantable) — [core manifest](https://github.com/aosp-mirror/platform_frameworks_base/blob/main/core/res/AndroidManifest.xml)
- FGS types — [FGS service types](https://developer.android.com/develop/background-work/services/fgs/service-types):
  - `specialUse`: permission `FOREGROUND_SERVICE_SPECIAL_USE`, "Runtime prerequisites: None"; declare the `<property android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE" android:value="..."/>`, which is reviewed only on Google Play.
  - `systemExempted`: only for a device in demo mode, Device Owner, Profile Owner, `ROLE_EMERGENCY`, Device Admin, holders of `SCHEDULE_EXACT_ALARM` or `USE_EXACT_ALARM`, and VPN apps. Otherwise it throws `ForegroundServiceTypeNotAllowedException`.
  - `shortService`: about 3 minutes.
  - `mediaProcessing`: 6 h per 24 h.
- `specialUse`, `systemExempted`, `health`, `remoteMessaging` and `shortService` are new in Android 14, where a type is mandatory — [Android 14 FGS types required](https://developer.android.com/about/versions/14/changes/fgs-types-required)
- Android 15 (target 35) — [Android 15 behavior changes (target 35)](https://developer.android.com/about/versions/15/behavior-changes-15):
  - "The system permits an app's dataSync services to run for a total of 6 hours in a 24-hour period", then `Service.onTimeout(int,int)`; "the 6-hour time limit is shared by all of an app's dataSync foreground services".
  - BOOT_COMPLETED receivers cannot launch `dataSync` (and some other types of) FGS.
  - SYSTEM_ALERT_WINDOW holders can start an FGS from the background only if they "have a visible overlay window" (`TYPE_APPLICATION_OVERLAY`).
- SCHEDULE_EXACT_ALARM "is no longer being pre-granted to most newly installed apps targeting Android 13 and higher (will be set to denied by default)" on Android 14 — [Schedule exact alarms denied by default](https://developer.android.com/about/versions/14/changes/schedule-exact-alarms). SCHEDULE_EXACT_ALARM = `signature|privileged|appop`; USE_EXACT_ALARM = `normal` — [core manifest](https://github.com/aosp-mirror/platform_frameworks_base/blob/main/core/res/AndroidManifest.xml)
- WorkManager: "The minimum repeat interval that can be defined is 15 minutes (same as the JobScheduler API)" — [Define work](https://developer.android.com/develop/background-work/background-tasks/persistent/getting-started/define-work)
- Android 16 job quotas: jobs "running concurrently with a foreground service, now adhere to the job runtime quota"; `setImportantWhileForeground` is ignored — [Android 16 behavior changes (all apps)](https://developer.android.com/about/versions/16/behavior-changes-all)
- Widgets:
  - "Updates requested with updatePeriodMillis will not be delivered more than once every 30 minutes" — [AppWidgetProviderInfo ref](https://developer.android.com/reference/android/appwidget/AppWidgetProviderInfo)
  - Android 17 (target 37): RemoteViews memory budget `6 * width * height` bytes; exceeding it throws `IllegalArgumentException` — [Android 17 behavior changes (target 37)](https://developer.android.com/about/versions/17/behavior-changes-17)
- Quick Settings: Android 13 tile placement API lets an app prompt to add its tile in one step — [Android 13 features](https://developer.android.com/about/versions/13/features)
- Accessibility: BIND_ACCESSIBILITY_SERVICE is `signature` (only the system binds) — [core manifest](https://github.com/aosp-mirror/platform_frameworks_base/blob/main/core/res/AndroidManifest.xml). The appop `OPSTR_ACCESS_RESTRICTED_SETTINGS` ("Limit user accessing restricted settings", `@SystemApi`, flag ENHANCED_CONFIRMATION_MODE_APIS) exists — [AppOpsManager.java](https://github.com/aosp-mirror/platform_frameworks_base/blob/main/core/java/android/app/AppOpsManager.java)
- Android 17 (target 37), also from [Android 17 behavior changes (target 37)](https://developer.android.com/about/versions/17/behavior-changes-17):
  - Apps "can't modify static final fields" by reflection or JNI.
  - The MessageQueue implementation is lock-free, so reflection on its private fields may break.
  - `ACCESS_LOCAL_NETWORK` is a new runtime permission (NEARBY_DEVICES group), mandatory for target 37.
  - BAL hardening extends to `IntentSender`.
- Android 17 (all apps): background audio hardening and cross-profile loopback traffic blocked — [Android 17 behavior changes (all apps)](https://developer.android.com/about/versions/17/behavior-changes-all)

### Inferences
- Recommended architecture for a sideloaded monitor:
  - A `specialUse` FGS started from the UI, which stays allowed after boot because `specialUse` is not in the BOOT_COMPLETED ban list cited for dataSync. Confirm the full list on the Android 15 page.
  - `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` (normal).
  - Widget updates pushed via `AppWidgetManager.updateAppWidget()` from the FGS (for example every 5 to 15 s while the screen is on).
  - A QS tile (`TileService`) for toggles.
  - Exact alarms only if needed, granted with `appops set <pkg> SCHEDULE_EXACT_ALARM allow`.
- Because `USE_EXACT_ALARM` is `normal` and only Play policy restricts it, a sideloaded app that declares it also qualifies for the `systemExempted` FGS type. That type is meant for system integrations, so `specialUse` is the cleaner choice.
- Logs in the background: use Shizuku (`logcat` run as shell is not gated by LogcatManagerService's per-app consent, since shell is a log-privileged client). Not verified in this session.
- If the app uses libadb to connect to the phone's own adbd over its Wi-Fi IP, `ACCESS_LOCAL_NETWORK` may be required when targeting API 37. Whether loopback or self-IP is exempt is not verified.

### Gaps
- Exact Android 13/14/15 rules for "restricted settings" on sideloaded apps (which installers trigger them, whether Android 15+ extended them to session-based installers, and whether ADB installs are exempt). The Google help pages were unreachable.
- The full list of FGS types banned from BOOT_COMPLETED on Android 15 (only `dataSync` confirmed here).

## Q9. Shizuku practicalities (start without a PC, reboot behaviour, Android 16/17, API usage, alternatives)

### Takeaway
On Android 11+ Shizuku starts on-device via Wireless debugging. It must be restarted after every reboot unless the device is rooted or the newer auto-start on a trusted Wi-Fi network (Android 13+) is used. Official Shizuku 13.6.0 has Android 17 regressions caused by hidden-API signature changes; maintained forks fix them. Build the app on Shizuku-API with UserService and keep hidden-API calls version-guarded.

### Cited Findings
- "On non-rooted devices, Shizuku needs to be manually restarted with adb every time on boot. Before Android 11, a computer is required to run adb. Android 11 and above have built-in wireless debugging support, and users can start Shizuku directly on the device." — [Shizuku-API README](https://github.com/RikkaApps/Shizuku-API)
- Shizuku v13.6.0 (latest official, dated "25 May"; the year is not shown on the page) — [Shizuku v13.6.0 release](https://github.com/RikkaApps/Shizuku/releases/tag/v13.6.0):
  - "Support Android16 QPR1"
  - "Update the start command" (the file can be copied to e.g. `/data/local/tmp/shizuku`)
  - "Support auto start without root on Android13+ when connected to a trusted WLAN"
- Release history: 13.5.4 (10 Mar), "Works on Android 14 QPR 3 beta 2" — [Shizuku releases](https://github.com/RikkaApps/Shizuku/releases)
- Shizuku-API usage — [Shizuku-API README](https://github.com/RikkaApps/Shizuku-API):
  - Dependencies `dev.rikka.shizuku:api` and `dev.rikka.shizuku:provider`; `ShizukuProvider` declared with `android:permission="android.permission.INTERACT_ACROSS_USERS_FULL"`.
  - Permission flow via `Shizuku.checkSelfPermission()` / `requestPermission(code)`; binder listeners `addBinderReceivedListener` / `addBinderDeadListener`.
  - `ShizukuBinderWrapper` forwards binder calls, and the app process then needs AndroidHiddenApiBypass or HiddenApiRefinePlugin.
  - UserService "runs in a different process and as the identity … of root (UID 0) or shell (UID 2000)", with "no restrictions on non-SDK APIs" there. However, "the User Service process is not a valid Android application process", so `registerReceiver` and `getContentResolver` won't work.
  - `newProcess` is deprecated in favour of UserService.
  - Sui is a Magisk module offering the same API.
- **Android 17 regressions in official Shizuku 13.6.0:**
  - Issue #2180 (opened 17 Jun 2026; reports from Pixel 6 to 10 including the 8 Pro): the authorized-apps list is empty and re-authorization fails on Android 17; already-authorized apps may keep working. Forks (Shevery, Nightzuku, Shizuku+, thedjchi) list and manage apps correctly; "making the official Shizuku the only one affected" — [Shizuku #2180](https://github.com/RikkaApps/Shizuku/issues/2180)
  - Issue #1988, community analysis: the hidden `IPackageManager.getInstalledPackages` return type changed from `ParceledListSlice` to `PackageInfoList` on Android 17, which causes `NoSuchMethodError` that is swallowed into an empty list. Clients may stay disconnected after a restart until their activity is opened. The fix is in an open PR (#2432) and not merged officially as of late Oct 2026. The thedjchi fork v13.7.0 was reported working on 2 Jul 2026 — [Shizuku #1988](https://github.com/RikkaApps/Shizuku/issues/1988)
  - Other open reports: "Wireless debugging pairing succeeds, but startup notification gets stuck" (#2405, 25 Aug 2026), "Is Android 17 not supported?" (#2513, 25 Sep 2026) — [Shizuku issues search](https://github.com/RikkaApps/Shizuku/issues?q=is%3Aissue+android+17)
- thedjchi fork — [thedjchi/Shizuku](https://github.com/thedjchi/Shizuku):
  - Adds "More robust 'start on boot': waits for a Wi-Fi connection", "TCP mode (i.e., the `adb tcpip` command) once Shizuku successfully starts with Wi-Fi after a reboot, you can stop/restart Shizuku without a Wi-Fi connection", a watchdog, and start/stop intents for Tasker.
  - Uses WRITE_SECURE_SETTINGS "to toggle USB and wireless debugging in the background".
  - The maintainer currently says: "I'm pausing maintenance for the time being".
- libadb-android: "enables an app to connect to the ADB daemon (`adbd`) belonging to the same or a different device and execute arbitrary services or commands (via `shell:` service)", JitPack `com.github.MuntashirAkon:libadb-android:3.1.1`, uses Conscrypt. It carries a security disclaimer: "has never gone through a security audit" — [libadb-android](https://github.com/MuntashirAkon/libadb-android)
- Android 17 blocks modification of `static final` fields via reflection/JNI for target-37 apps — [Android 17 behavior changes (target 37)](https://developer.android.com/about/versions/17/behavior-changes-17)

### Inferences
- For a personal app, the most robust stack is:
  1. Depend on Shizuku-API (works with Shizuku, its forks, and Sui under root).
  2. Put privileged logic in a UserService, a small AIDL interface running as uid 2000.
  3. Prefer `cmd`/`pm`/`dumpsys` text or stable AIDL methods, and guard every hidden-API call by `Build.VERSION.SDK_INT_FULL` with a fallback, because Android 17 changed a hidden `IPackageManager` signature.
  4. Use public APIs in the app process wherever they exist (for example QUERY_ALL_PACKAGES for listing).
- On the Pixel 8 Pro with Android 17, use a fork of Shizuku such as thedjchi v13.7.x until upstream merges the fix. Alternatively, embed libadb-android to self-connect to wireless debugging and skip a Shizuku dependency entirely. That is more work: pairing UI, key storage, and 16 KB-aligned native Conscrypt.
- After a reboot without root: open the app, let it toggle wireless debugging (with WRITE_SECURE_SETTINGS granted once via adb), and connect. Wireless debugging needs a Wi-Fi network, which is the stated condition of the official auto-start. TCP mode in the fork removes the Wi-Fi requirement until the next reboot.

### Gaps
- Official Shizuku setup page (pairing via notification, Android 16 notes) was unreachable (shizuku.rikka.app).
- Dhizuku (sharing Device Owner privileges) README was not retrievable; its status on Android 16/17 is unknown.
- LADB current status: not researched.
- Whether Android 16/17 changed wireless-debugging behaviour on Pixel (auto-disable when leaving Wi-Fi, per-network trust): not verified.

## Q10. Root on the Pixel 8 Pro (husky): unlock, tools, Android 16/17 status, consequences

### Takeaway
Rooting husky is technically mature:
- the bootloader unlocks with `fastboot flashing unlock`;
- Magisk v31.0 supports Android 17 and v30.7 supports the Android 16 QPR2 sepolicy format;
- KernelSU in LKM mode and APatch cover the device's 6.1 (and 6.12-era) kernels.

The costs are a data wipe on unlock, loss of STRONG integrity (Wallet and banking risk), and manual re-patching on every monthly update. For this app, root adds little over Shizuku: per-app cache deletion, `pm hide`, raw sysfs, and other apps' private data.

### Cited Findings
- husky unlock command `fastboot flashing unlock`. LineageOS lists the kernel as `android_kernel_google_gs-6.1_manifest, version '6.1'`; the install requires specific Android firmware (version 17); partitions boot, dtbo, vendor_kernel_boot — [LineageOS husky.yml](https://github.com/LineageOS/lineage_wiki/blob/main/_data/devices/husky.yml)
- HuskySU, a KernelSU fork for "Pixel 8 Pro / husky": releases with KMIs `android14-6.1` and `android16-6.12`, plus a canary for kernel `6.1.172-android14-11-…` on build `ZP11.260918.007`. Releases are dated 13 to 18 Oct; the builds are labelled engineering/unverified — [heDuke/KernelSU releases](https://github.com/heDuke/KernelSU/releases)
- KernelSU — [KernelSU installation guide](https://github.com/tiann/KernelSU/blob/main/website/docs/guide/installation.md):
  - Supports LKM mode and GKI mode, and recommends LKM for phones.
  - In LKM mode "on devices with Android 13, it needs to patch the `init_boot` partition".
  - A mismatched KMI can bootloop, and flashing an older security patch level can bootloop due to anti-rollback.
- Magisk — [Magisk releases](https://github.com/topjohnwu/Magisk/releases), [Magisk install doc](https://github.com/topjohnwu/Magisk/blob/master/docs/install.md):
  - v31.0 (04 Sep): "[Zygisk] Support Android 17 (API 37)".
  - v30.7 (23 Feb): "Support Android 16 QPR2 sepolicy format".
  - v31.1 (25 Oct): "Automatically suggest official firmware downloads for Google Pixel".
  - The install doc covers patching `boot.img`, `init_boot.img` or `vendor_boot.img`, and offers "Download latest Pixel factory image (Pixel devices only)".
  - It warns: "Never, ever try to restore either boot, init_boot, recovery, or vbmeta partitions back to stock!"
- APatch: "Only supports the ARM64 architecture. Only supports Android kernel versions 3.18 - 6.12". It requires `CONFIG_KALLSYMS=y`, and its SuperKey "has higher privileges than root access" — [APatch README](https://github.com/bmax121/APatch)
- Play Integrity: "On Android 13 and higher, the MEETS_STRONG_INTEGRITY verdict requires MEETS_DEVICE_INTEGRITY and security updates in the last year for all partitions"; optional labels are returned only to Play-licensed apps — [Play Integrity verdicts](https://developer.android.com/google/play/integrity/verdicts)

### Inferences
- The Pixel 8 Pro shipped with Android 14 (launched 2023-10-04 per [LineageOS](https://github.com/LineageOS/lineage_wiki/blob/main/_data/devices/husky.yml)), so Magisk or KernelSU-LKM patches `init_boot`.
- After each monthly OTA, re-patch the new `init_boot` (Magisk can fetch the matching Pixel factory image) and flash it. There is no seamless OTA retention on Pixel with a patched `init_boot`, beyond the manual "install to inactive slot" flow.
- An unlocked bootloader fails hardware-backed device integrity on Android 13+, so STRONG and usually DEVICE integrity fail. Google Wallet tap-to-pay and many banking apps then refuse to run. Spoofing modules exist but are an arms race.
- Verdict for a personal monitoring app: not worth it unless the user already roots. Shizuku plus one-time `pm grant`s cover about 90% of the matrix in Q0.
- If the user roots anyway, Sui gives the same Shizuku-API code path with uid 0, so design for Shizuku and get root support for free.

### Gaps
- Not re-verified with primary sources in this session (source.android.com, XDA and Google help unreachable):
  - that `fastboot flashing unlock` wipes userdata;
  - that carrier-sold units (notably Verizon) have OEM unlocking disabled; units sold in France are usually unlockable;
  - Google Wallet and banking behaviour.
- Whether the stock Pixel 8 Pro kernel on Android 17 is still 6.1 or moved to 6.12: HuskySU ships both KMIs, and LineageOS says 6.1. Check with `adb shell uname -r`.

## Q11. Distribution (developer verification) and Android 16/17 platform changes relevant to a sideloaded personal app

### Takeaway
Developer verification has been enforced since 30 Sep 2026 only in Brazil, Indonesia, Singapore and Thailand, with a global rollout announced for 2027. ADB installs are explicitly exempt. So for a personal app used in France there is **no impact today**. From 2027 the options are: keep installing and updating via ADB; register the package under a free "limited distribution" account (up to 20 devices, no ID); or enable the one-time "advanced flow" (24 h wait). Separately, Advanced Protection Mode (Android 16+) blocks sideloading if the user turns it on.

### Cited Findings
- Timeline — [Android developer verification](https://developer.android.com/developer-verification):
  - "August 2026: Developer APIs, limited distribution accounts, and power user advanced flow launch."
  - "September 30, 2026: Protections begin for all users who install apps from participating stores in Brazil, Indonesia, Singapore, and Thailand on certified devices running Android 7+."
  - "Verification capability will be expanded globally for all apps on certified Android devices in 2027."
- FAQ — [Developer verification FAQ](https://developer.android.com/developer-verification/guides/faq):
  - "Apps installed using ADB won't require verification"; "The waiting period does not apply to ADB installs"; no ADB command bypasses the 24 h wait "at this point".
  - Limited distribution: "share apps with up to 20 specific devices for testing and personal use at no cost and without ID verification".
  - Full distribution fee: $25.
  - Advanced flow: enable developer mode, an anti-coaching check, restart and re-authenticate, "a one-time, one-day wait", then confirm with biometrics or PIN; enable "for 7 days or indefinitely".
  - "Unregistered apps can only be installed or updated when the advanced flow is enabled or by using ADB".
  - "Enforcement starting September 30, 2026, is limited only to the specific stores mentioned".
  - Mobile and tablet form factors only; updates are delivered "through Google Play services".
- **Conflicting reports on the first-wave scope:** the FAQ says enforcement is limited to participating stores. Press coverage of the 1 Oct rollout describes unregistered apps as blocked unless the user takes the sideloading step — [Android Authority, first wave](https://www.androidauthority.com/android-sideloading-developer-verification-first-wave-rollout-3717921/) (search snippet only; page not fetched). The advanced flow was "gradually" rolling out from mid-Aug 2026 — [9to5Google, 18 Aug 2026](https://9to5google.com/2026/08/18/google-gradually-rolling-out-androids-advanced-sideloading-ahead-of-developer-verification/) (search snippet only). Google's position: "Sideloading is here to stay … via a new advanced flow" — [@AndroidDev on X](https://x.com/AndroidDev/status/2034682546970075183)
- Advanced Protection Mode:
  - Launched in Android 16. Required app adaptations include "Blocked app sideloading", "Disabled 2G and WEP", "Active forensic logging"; apps detect it via `AdvancedProtectionManager` with `QUERY_ADVANCED_PROTECTION_MODE` — [AAPM developer guide](https://developer.android.com/privacy-and-security/advanced-protection-mode)
  - The Android 17 features page lists "blocking app installation from unknown sources (sideloading), restricting USB data signaling, and mandating Google Play Protect scanning" — [Android 17 features](https://developer.android.com/about/versions/17/features)
- 16 KB pages — [Page sizes guide](https://developer.android.com/guide/practices/page-sizes), [Android 16 behavior changes (all apps)](https://developer.android.com/about/versions/16/behavior-changes-all):
  - The developer option to boot with 16 KB pages is available on "Pixel 8 and 8 Pro (with Android 15 QPR1 or higher)".
  - Android 16 runs 4 KB-aligned apps on 16 KB devices in a compatibility mode with a user dialog, which can be suppressed with `android:pageSizeCompat`.
- Edge-to-edge: "For apps targeting Android 16 (API level 36), R.attr#windowOptOutEdgeToEdgeEnforcement is deprecated and disabled" — [Android 16 behavior changes (target 36)](https://developer.android.com/about/versions/16/behavior-changes-16)
- Android 17 release: stable reached Pixel phones in mid-June 2026 (reports say 16 or 17 June), with Pixel 6 and newer eligible — [Gizmochina](https://www.gizmochina.com/2026/06/09/heres-when-google-pixels-will-receive-android-17-update/), [LAVX News](https://news.lavx.hu/article/google-starts-android-17-rollout-for-pixel-phones) (search snippets; no Google primary source fetched)

### Inferences
- For a single-user app in France:
  - (1) Today: sideload freely (no enforcement in the EU).
  - (2) In 2027: install and update via `adb install -r` (exempt, works over Wireless debugging from another device or even from the phone itself via Shizuku/libadb). Or register the package name and signing key under a free limited-distribution Android Developer Console account so normal APK updates keep working. The advanced flow is a fallback.
  - Do not enable Advanced Protection Mode on that phone. Its sideloading block applies, and its USB-data restriction may interfere with ADB.
- A pure-Kotlin app without `.so` files is unaffected by 16 KB pages. Any native dependency (for example Conscrypt for libadb, or SQLCipher) must be 16 KB-aligned to avoid the compat dialog.
- Target API 36/37 consequences:
  - Mandatory edge-to-edge.
  - Predictive back migration.
  - A `static final` reflection ban (37) that affects hidden-API hacks.
  - `ACCESS_LOCAL_NETWORK` (37).
  - The widget memory budget (37).
- Targeting an older SDK delays some of these but not the "all apps" changes (for example Android 17 memory limits).

### Gaps
- The exact list of "participating stores" and whether direct APK installs from a browser or file manager are blocked in the first-wave countries (sources conflict).
- Whether developer verification applies in the EU/France on the same 2027 timeline, given DMA considerations: not addressed by Google's pages.
- The behaviour of "restricted settings" for sideloaded apps on Android 15 to 17 (relevant to accessibility and notification-listener features): unverified.

## Q12. Pixel 8 Pro hardware specifics useful for monitoring

### Takeaway
Device data confirms the following: Tensor G3 with a 1+4+4 Cortex-X3 / A715 / A510 layout, Mali-G715 (MC10) GPU, 12 GB RAM, a 5050 mAh battery, a "Thermometer" peripheral, and a 6.1 kernel. Memory type, storage type, modem and the end of the update window could not be verified from a primary source in this session.

### Cited Findings
- husky: "Google Tensor G3"; CPU "Cortex-X3 & Cortex-A715 & Cortex-A510", 9 cores, `1 x 3.0 GHz + 4 x 2.45 GHz + 4 x 2.14 GHz`; GPU "ARM Mali-G715s MC10"; RAM 12 GB; storage 128/256/512/1024 GB; battery 5050 mAh Li-Ion; screen 6.7" LTPO OLED 1344x2992 at 120 Hz; Wi-Fi 802.11be; peripherals including Barometer, UWB and **Thermometer**; release 2023-10-04; kernel 6.1 — [LineageOS husky.yml](https://github.com/LineageOS/lineage_wiki/blob/main/_data/devices/husky.yml)
- The 16 KB-page developer option is supported on the Pixel 8 Pro from Android 15 QPR1 — [Page sizes guide](https://developer.android.com/guide/practices/page-sizes)
- The Pixel 8 Pro is among the devices receiving Android 17 (Pixel 6 and newer) — [Gizmochina](https://www.gizmochina.com/2026/06/09/heres-when-google-pixels-will-receive-android-17-update/) (search snippet)

### Inferences
- Expose per-cluster views (little A510 x4, mid A715 x4, prime X3 x1) by reading `/sys/devices/system/cpu/cpufreq/policy*/related_cpus` at runtime instead of hardcoding.
- Show battery wear by comparing `BATTERY_PROPERTY_CHARGE_COUNTER` at 100% with the rated 5050 mAh, as a fallback if `STATE_OF_HEALTH` is unavailable.

### Gaps
- Not verified, because Google Store, Google support, Wikipedia and GSMArena were unreachable:
  - the official clocks (commonly cited as 2.91 / 2.37 / 1.70 GHz, which contradicts LineageOS);
  - LPDDR5X memory, UFS 3.1 storage and the Exynos 5300 modem;
  - the 7-year OS and security update commitment (to October 2030).
- The exact Mali-G715 frequency table and the sysfs paths for GPU utilisation on Tensor G3.
