package fr.tflag.droidboost.core.model

data class MemInfo(
    val totalBytes: Long?,
    val availableBytes: Long?,
    val freeBytes: Long?,
    val cachedBytes: Long?,
    val swapTotalBytes: Long?,
    val swapFreeBytes: Long?,
) {
    val swapUsedBytes: Long?
        get() = if (swapTotalBytes != null && swapFreeBytes != null) swapTotalBytes - swapFreeBytes else null
}
