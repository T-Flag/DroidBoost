package fr.tflag.droidboost.core.journal

/** Categories of journal events, stored as strings in the database. */
object JournalTypes {
    const val APP = "app"
    const val PERMISSION = "permission"
    const val SYSTEM = "system"
    const val EXIT = "exit"
    const val ACTION = "action"
    const val SHIZUKU = "shizuku"
    const val ANOMALY = "anomaly"
}
