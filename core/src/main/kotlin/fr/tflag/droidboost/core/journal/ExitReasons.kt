package fr.tflag.droidboost.core.journal

/** Process deaths worth showing to the user, from `ApplicationExitInfo`. */
enum class ExitKind(val label: String) {
    LOW_MEMORY("Tuée par manque de mémoire"),
    CRASH("Plantage"),
    NATIVE_CRASH("Plantage natif"),
    ANR("Ne répondait plus (ANR)"),
    INIT_FAILURE("Échec au démarrage"),
    EXCESSIVE_RESOURCES("Ressources excessives"),
    MEMORY_LIMIT("Limite mémoire d'Android 17"),
    ;

    companion object {
        // ApplicationExitInfo.REASON_* values.
        private const val REASON_LOW_MEMORY = 3
        private const val REASON_CRASH = 4
        private const val REASON_CRASH_NATIVE = 5
        private const val REASON_ANR = 6
        private const val REASON_INITIALIZATION_FAILURE = 7
        private const val REASON_EXCESSIVE_RESOURCE_USAGE = 9
        private const val REASON_OTHER = 13

        /**
         * Classifies an exit; normal exits (user swipe, app update, self exit…) return null.
         * Android 17 reports its per-app memory limits as REASON_OTHER with a "MemoryLimiter" description.
         */
        fun classify(reason: Int, description: String?): ExitKind? = when (reason) {
            REASON_LOW_MEMORY -> LOW_MEMORY
            REASON_CRASH -> CRASH
            REASON_CRASH_NATIVE -> NATIVE_CRASH
            REASON_ANR -> ANR
            REASON_INITIALIZATION_FAILURE -> INIT_FAILURE
            REASON_EXCESSIVE_RESOURCE_USAGE -> EXCESSIVE_RESOURCES
            REASON_OTHER -> if (description?.contains("MemoryLimiter", ignoreCase = true) == true) MEMORY_LIMIT else null
            else -> null
        }
    }
}
