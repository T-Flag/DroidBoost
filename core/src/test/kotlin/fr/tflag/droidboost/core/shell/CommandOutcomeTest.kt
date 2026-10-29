package fr.tflag.droidboost.core.shell

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class CommandOutcomeTest {

    @Test
    fun `success with output`() {
        val outcome = CommandOutcome.from(0, "Package com.example.app new state: disabled-user\n", "")
        assertEquals(CommandOutcome.Success("Package com.example.app new state: disabled-user"), outcome)
    }

    @Test
    fun `silent success`() {
        assertIs<CommandOutcome.Success>(CommandOutcome.from(0, "", ""))
    }

    @Test
    fun `android 17 refuses system uninstall from shell`() {
        val outcome = CommandOutcome.from(1, "Failure [only root can delete system app for a particular user]", "")
        assertIs<CommandOutcome.RootRequired>(outcome)
    }

    @Test
    fun `failure printed with exit code zero`() {
        assertIs<CommandOutcome.Failed>(CommandOutcome.from(0, "Failure [DELETE_FAILED_INTERNAL_ERROR]", ""))
    }

    @Test
    fun `exception on stderr`() {
        val stderr = """
            Exception occurred while executing 'grant':
            java.lang.SecurityException: Package com.example.app has not requested permission android.permission.DUMP
        """.trimIndent()
        assertIs<CommandOutcome.Failed>(CommandOutcome.from(255, "", stderr))
        assertIs<CommandOutcome.Failed>(CommandOutcome.from(0, "", stderr))
    }

    @Test
    fun `non zero exit without output`() {
        assertEquals(CommandOutcome.Failed("Code de sortie 3"), CommandOutcome.from(3, "", ""))
    }
}
