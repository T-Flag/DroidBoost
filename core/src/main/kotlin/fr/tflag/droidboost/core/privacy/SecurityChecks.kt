package fr.tflag.droidboost.core.privacy

import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class CheckLevel { OK, INFO, WARNING }

data class SecurityCheck(val title: String, val value: String, val level: CheckLevel, val advice: String? = null)

object SecurityChecks {

    /** Pixels get monthly patches: two missed months deserve a warning. */
    fun patchAge(patch: String, today: LocalDate): SecurityCheck {
        val date = runCatching { LocalDate.parse(patch) }.getOrNull()
            ?: return SecurityCheck("Correctif de sécurité", patch, CheckLevel.INFO)
        val days = ChronoUnit.DAYS.between(date, today)
        return SecurityCheck(
            title = "Correctif de sécurité",
            value = "$patch (il y a $days j)",
            level = if (days > 62) CheckLevel.WARNING else CheckLevel.OK,
            advice = if (days > 62) "Vérifiez les mises à jour dans Réglages > Système." else null,
        )
    }

    /** `ro.boot.verifiedbootstate`: green = locked bootloader with Google's keys. */
    fun verifiedBoot(state: String?): SecurityCheck = when (state?.trim()) {
        "green" -> SecurityCheck("Démarrage vérifié", "Vert : bootloader verrouillé", CheckLevel.OK)
        "yellow" -> SecurityCheck("Démarrage vérifié", "Jaune : clé personnalisée", CheckLevel.WARNING)
        "orange" -> SecurityCheck(
            "Démarrage vérifié",
            "Orange : bootloader déverrouillé",
            CheckLevel.WARNING,
            "Google Wallet et certaines applis bancaires peuvent refuser de fonctionner.",
        )
        null, "" -> SecurityCheck("Démarrage vérifié", "Inconnu (Shizuku requis)", CheckLevel.INFO)
        else -> SecurityCheck("Démarrage vérifié", state, CheckLevel.INFO)
    }

    /** Parses the output of `appops query-op <OP> allow`: one package name per line. */
    fun parsePackageList(output: String): List<String> = output.lineSequence()
        .map { it.trim() }
        .filter { it.matches(Regex("^[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z0-9_]+)+$")) }
        .distinct()
        .toList()

    /** Component lists stored in secure settings, e.g. `pkg/.Service:pkg2/pkg2.Other`. */
    fun parseComponentPackages(setting: String?): List<String> = setting.orEmpty()
        .split(':')
        .mapNotNull { it.substringBefore('/').trim().takeIf { pkg -> pkg.isNotEmpty() } }
        .distinct()
}
