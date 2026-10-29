package fr.tflag.droidboost.core.parse

import fr.tflag.droidboost.core.model.MemInfo

/** Parses `/proc/meminfo` (values in kB). */
object MemInfoParser {

    private val LINE = Regex("^(\\w+(?:\\(\\w+\\))?):\\s+(\\d+)")

    fun parse(text: String): MemInfo {
        val values = text.lineSequence()
            .mapNotNull { LINE.find(it.trim()) }
            .associate { it.groupValues[1] to it.groupValues[2].toLong() * 1024 }
        return MemInfo(
            totalBytes = values["MemTotal"],
            availableBytes = values["MemAvailable"],
            freeBytes = values["MemFree"],
            cachedBytes = values["Cached"],
            swapTotalBytes = values["SwapTotal"],
            swapFreeBytes = values["SwapFree"],
        )
    }
}
