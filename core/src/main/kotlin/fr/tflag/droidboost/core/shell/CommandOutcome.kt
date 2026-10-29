package fr.tflag.droidboost.core.shell

/** Result of a shell command as seen by the user. */
sealed interface CommandOutcome {
    data class Success(val output: String) : CommandOutcome

    /** Android 17 refuses `pm uninstall --user 0` on system apps from a non-root shell. */
    data class RootRequired(val message: String) : CommandOutcome

    data class Failed(val message: String) : CommandOutcome

    companion object {
        private val ROOT_ONLY = Regex("only root can", RegexOption.IGNORE_CASE)
        private val FAILURE = Regex("^(Failure|Error)\\b|Exception|Unknown package|not found", RegexOption.IGNORE_CASE)

        /**
         * Interprets the output of `pm`, `am`, `appops` or `cmd`.
         *
         * Those tools often exit with code 0 and print `Failure [...]` or an exception on stderr,
         * so the text matters as much as the exit code.
         */
        fun from(exitCode: Int, stdout: String, stderr: String): CommandOutcome {
            val out = stdout.trim()
            val err = stderr.trim()
            val combined = listOf(out, err).filter { it.isNotEmpty() }.joinToString("\n")
            return when {
                ROOT_ONLY.containsMatchIn(combined) -> RootRequired(combined)
                exitCode != 0 -> Failed(combined.ifEmpty { "Code de sortie $exitCode" })
                combined.lineSequence().any { FAILURE.containsMatchIn(it.trim()) } -> Failed(combined)
                else -> Success(out)
            }
        }
    }
}
