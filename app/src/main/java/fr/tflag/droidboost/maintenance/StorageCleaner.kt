package fr.tflag.droidboost.maintenance

import android.content.Context
import android.content.pm.PackageManager
import android.os.Environment
import fr.tflag.droidboost.core.maintenance.DuplicateFinder
import fr.tflag.droidboost.core.maintenance.DuplicateGroup
import fr.tflag.droidboost.core.maintenance.FileEntry
import fr.tflag.droidboost.core.shell.AppFolderArea
import fr.tflag.droidboost.core.shell.CommandOutcome
import fr.tflag.droidboost.core.shell.Identifiers
import fr.tflag.droidboost.core.shell.ShellCommands
import fr.tflag.droidboost.shell.ShizukuShell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile
import java.security.MessageDigest

data class ScanResult(
    val largest: List<FileEntry>,
    val duplicates: List<DuplicateGroup>,
    val emptyFolders: List<String>,
    val fileCount: Int,
    val totalBytes: Long,
)

/** Leftover folder of an app that is no longer installed. */
data class Leftover(val area: AppFolderArea, val packageName: String, val sizeBytes: Long)

/**
 * Shared-storage cleaner: largest files, duplicates and empty folders (all-files access), plus leftovers
 * of uninstalled apps in Android/data, obb and media (Shizuku).
 */
class StorageCleaner(private val context: Context, private val shell: ShizukuShell) {

    private val root: File get() = Environment.getExternalStorageDirectory()

    suspend fun scan(onProgress: (Int) -> Unit = {}): ScanResult = withContext(Dispatchers.IO) {
        val files = mutableListOf<FileEntry>()
        val emptyFolders = mutableListOf<String>()
        root.walkTopDown()
            .onEnter { dir -> dir.absolutePath !in skippedDirs() }
            .forEach { file ->
                ensureActive()
                if (file.isDirectory) {
                    if (file != root && file.list()?.isEmpty() == true) emptyFolders += file.absolutePath
                } else {
                    files += FileEntry(file.absolutePath, file.length(), file.lastModified())
                    if (files.size % PROGRESS_STEP == 0) onProgress(files.size)
                }
            }
        ScanResult(
            largest = files.sortedByDescending { it.sizeBytes }.take(LARGEST_COUNT),
            duplicates = DuplicateFinder.find(files) { partialHash(File(it.path)) },
            emptyFolders = emptyFolders.sorted(),
            fileCount = files.size,
            totalBytes = files.sumOf { it.sizeBytes },
        )
    }

    /** Deletes [path] if it is still inside shared storage; duplicates are re-checked with a full hash first. */
    suspend fun delete(path: String, keepIdenticalTo: String? = null): Boolean = withContext(Dispatchers.IO) {
        val file = File(path).canonicalFile
        if (!file.path.startsWith(root.canonicalPath + File.separator)) return@withContext false
        if (keepIdenticalTo != null && fullHash(file) != fullHash(File(keepIdenticalTo))) return@withContext false
        file.delete()
    }

    suspend fun leftovers(): List<Leftover>? {
        if (!shell.isReady) return null
        val pm = context.packageManager
        return AppFolderArea.entries.flatMap { area ->
            val names = shell.output(ShellCommands.listAppFolders(area)).orEmpty().lines().map { it.trim() }
            names.filter { Identifiers.isValidPackage(it) && !isInstalled(pm, it) }.mapNotNull { pkg ->
                val kb = shell.output(ShellCommands.folderSizeKb(area, pkg))?.trim()?.substringBefore('\t')?.substringBefore(' ')?.toLongOrNull()
                kb?.let { Leftover(area, pkg, it * 1024) }
            }
        }.sortedByDescending { it.sizeBytes }
    }

    suspend fun deleteLeftover(leftover: Leftover): CommandOutcome {
        // Re-check right before deleting: the app may have been reinstalled since the scan.
        if (isInstalled(context.packageManager, leftover.packageName)) return CommandOutcome.Failed("L'appli est installée")
        return shell.run(ShellCommands.deleteAppFolder(leftover.area, leftover.packageName))
    }

    private fun isInstalled(pm: PackageManager, pkg: String): Boolean = runCatching {
        pm.getPackageInfo(pkg, PackageManager.PackageInfoFlags.of(PackageManager.MATCH_UNINSTALLED_PACKAGES.toLong()))
    }.isSuccess

    private fun skippedDirs(): Set<String> = setOf(
        File(root, "Android/data").absolutePath,
        File(root, "Android/obb").absolutePath,
    )

    /** Size + first and last 64 KB: cheap and reliable enough to group candidates. */
    private fun partialHash(file: File): String? = runCatching {
        val digest = MessageDigest.getInstance("SHA-256")
        RandomAccessFile(file, "r").use { raf ->
            val length = raf.length()
            val buffer = ByteArray(PARTIAL_BYTES)
            val head = raf.read(buffer)
            if (head > 0) digest.update(buffer, 0, head)
            if (length > PARTIAL_BYTES * 2) {
                raf.seek(length - PARTIAL_BYTES)
                val tail = raf.read(buffer)
                if (tail > 0) digest.update(buffer, 0, tail)
            }
            digest.update(length.toString().toByteArray())
        }
        digest.digest().joinToString("") { "%02x".format(it) }
    }.getOrNull()

    private fun fullHash(file: File): String? = runCatching {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        digest.digest().joinToString("") { "%02x".format(it) }
    }.getOrNull()

    private companion object {
        const val LARGEST_COUNT = 100
        const val PARTIAL_BYTES = 64 * 1024
        const val DEFAULT_BUFFER = 256 * 1024
        const val PROGRESS_STEP = 500
    }
}
