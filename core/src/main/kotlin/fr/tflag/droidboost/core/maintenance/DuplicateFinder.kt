package fr.tflag.droidboost.core.maintenance

data class FileEntry(val path: String, val sizeBytes: Long, val modifiedMs: Long)

data class DuplicateGroup(val sizeBytes: Long, val files: List<FileEntry>) {
    /** Space freed by keeping a single copy. */
    val wastedBytes: Long get() = sizeBytes * (files.size - 1)
}

object DuplicateFinder {

    /**
     * Groups identical files: same size first (free), then same content [hash] (computed only for size collisions).
     * Tiny files are ignored, deleting them saves nothing.
     */
    fun find(files: List<FileEntry>, minSizeBytes: Long = 1024 * 1024, hash: (FileEntry) -> String?): List<DuplicateGroup> =
        files.asSequence()
            .filter { it.sizeBytes >= minSizeBytes }
            .groupBy { it.sizeBytes }
            .values
            .filter { it.size > 1 }
            .flatMap { sameSize ->
                sameSize.mapNotNull { file -> hash(file)?.let { it to file } }
                    .groupBy({ it.first }, { it.second })
                    .values
                    .filter { it.size > 1 }
                    .map { group -> DuplicateGroup(group.first().sizeBytes, group.sortedBy { it.modifiedMs }) }
            }
            .sortedByDescending { it.wastedBytes }
}
