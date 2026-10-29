package fr.tflag.droidboost.core.maintenance

/**
 * How risky it is to disable a preinstalled package, adapted from the UAD-ng ratings for Pixel
 * (github.com/Universal-Debloater-Alliance/universal-android-debloater-next-generation, resources/assets/uad_lists.json).
 */
enum class DebloatRisk(val label: String) {
    /** Standalone app: nothing else depends on it, reinstallable from the Play Store. */
    SAFE("Sans risque"),

    /** Disabling removes a Pixel feature, described in the entry. */
    FEATURE_LOSS("Perte de fonction"),

    /** Other parts of the system depend on it: DroidBoost refuses to touch it. */
    PROTECTED("Protégé"),
}

data class DebloatEntry(val packageName: String, val label: String, val risk: DebloatRisk, val note: String)

object DebloatCatalog {

    val entries: List<DebloatEntry> = listOf(
        // Pixel extras.
        safe("com.google.android.apps.tips", "Astuces Pixel", "Conseils d'utilisation."),
        safe("com.google.android.apps.pixel.support", "Dépannage Pixel", "Outil de diagnostic et de contact du support."),
        safe("com.google.android.apps.pixelmigrate", "Outil de transfert de données", "Ne sert qu'à la configuration d'un nouveau téléphone."),
        safe("com.google.android.apps.betterbug", "Commentaires Android Bêta", "Inutile hors programme bêta."),
        safe("com.google.android.apps.wearables.maestro.companion", "Pixel Buds", "Inutile sans écouteurs Pixel Buds."),
        safe("com.google.android.apps.pixel.subzero", "Daily Hub", "Résumé quotidien Pixel."),
        safe("com.google.android.apps.work.clouddpc", "Device Policy", "Gestion d'un appareil professionnel par une entreprise."),
        // Google apps available from the Play Store.
        safe("com.google.android.youtube", "YouTube", "Réinstallable depuis le Play Store."),
        safe("com.google.android.apps.youtube.music", "YouTube Music", "Réinstallable depuis le Play Store."),
        safe("com.google.android.videos", "Google TV", "Réinstallable depuis le Play Store."),
        safe("com.google.android.apps.books", "Play Livres", "Réinstallable depuis le Play Store."),
        safe("com.google.android.apps.magazines", "Google Actualités", "Réinstallable depuis le Play Store."),
        safe("com.google.android.keep", "Keep", "Réinstallable depuis le Play Store."),
        safe("com.google.android.apps.docs", "Drive", "Réinstallable depuis le Play Store."),
        safe("com.google.android.apps.subscriptions.red", "Google One", "Tourne parfois en arrière-plan."),
        safe("com.google.android.apps.bard", "Gemini", "Réinstallable depuis le Play Store."),
        safe("com.google.android.apps.recorder", "Enregistreur", "Réinstallable depuis le Play Store."),
        safe("com.google.android.apps.tachyon", "Meet", "Réinstallable depuis le Play Store."),
        safe("com.google.android.apps.maps", "Maps", "Réinstallable depuis le Play Store."),
        safe("com.google.android.gm", "Gmail", "Réinstallable depuis le Play Store."),
        safe("com.google.android.calendar", "Agenda", "Réinstallable depuis le Play Store."),
        safe("com.google.android.calculator", "Calculatrice", "Réinstallable depuis le Play Store."),
        safe("com.google.android.apps.translate", "Traduction", "Réinstallable depuis le Play Store."),
        safe("com.google.android.apps.tasks", "Tasks", "Réinstallable depuis le Play Store."),
        safe("com.google.android.apps.fitness", "Fit", "Réinstallable depuis le Play Store."),
        safe("com.google.android.play.games", "Play Jeux", "Réinstallable depuis le Play Store."),
        safe("com.google.ar.lens", "Lens", "Réinstallable depuis le Play Store."),
        feature("com.google.android.apps.photos", "Photos", "Arrête la sauvegarde des photos et vidéos."),
        feature("com.google.ar.core", "Services Google Play pour la RA", "Les applis de réalité augmentée ne fonctionneront plus."),
        feature("com.google.android.apps.weather", "Météo", "« En un coup d'œil » renverra vers l'appli Google."),
        feature("com.google.android.apps.googleassistant", "Assistant", "L'Assistant ne répondra plus."),
        // Pixel features.
        feature("com.google.android.as", "Android System Intelligence", "Supprime Sous-titres instantanés, suggestions d'applis, sélection intelligente du texte et actions des notifications."),
        feature("com.google.android.apps.adm", "Localiser mon appareil", "Le téléphone ne pourra plus être localisé, sonné ou effacé à distance."),
        feature("com.google.android.marvin.talkback", "Suite d'accessibilité", "Désactive TalkBack, Switch Access et le menu d'accessibilité."),
        feature("com.google.android.apps.scone", "Connectivité adaptative", "Plus de bascule automatique entre 4G et 5G pour économiser la batterie."),
        feature("com.google.android.apps.safetyhub", "Sécurité personnelle", "Plus de détection d'accident de voiture ni de SOS urgence."),
        feature("com.google.android.apps.wallpaper.pixel", "Fonds d'écran Pixel", "Les fonds d'écran Pixel et générés ne seront plus disponibles."),
        feature("com.google.android.apps.dreamliner", "Pixel Stand", "Inutile sans socle Pixel Stand."),
        feature("com.google.android.apps.pixel.creativeassistant", "Assistant créatif", "Fonctions de création assistée par IA."),
        feature("com.google.android.apps.restore", "Assistant de restauration", "Restauration des données lors d'une configuration."),
        feature("com.google.android.apps.security.securityhub", "Centre de sécurité", "L'écran Sécurité et confidentialité perd une partie de ses vérifications."),
        // Never touch: other components depend on them (UAD-ng « Expert » / « Unsafe » or explicit dependency).
        protected("com.google.android.apps.wellbeing", "Bien-être numérique", "Dépendance de l'appli Réglages sur Pixel."),
        protected("com.google.android.apps.turbo", "Device Health Services", "Casse les réglages de la batterie."),
        protected("com.android.DeviceAsWebcam", "Webcam", "Dépendance de l'Espace privé."),
        protected("com.google.pixel.camera.services", "Services de caméra Pixel", "Dépendance de l'Espace privé."),
        protected("com.google.android.apps.camera.services", "Pixel Camera Services", "Fonctions photo de base."),
        protected("com.google.android.pixelsystemservice", "Pixel System Service", "Risque de démarrage en boucle."),
        protected("com.google.android.rkpdapp", "RKPD", "Clés d'attestation : risque de démarrage en boucle."),
        protected("com.google.android.gms", "Services Google Play", "Presque toutes les applis en dépendent."),
        protected("com.google.android.gsf", "Google Services Framework", "Presque toutes les applis Google en dépendent."),
        protected("com.android.vending", "Play Store", "Plus de mises à jour des applis."),
        protected("com.google.android.webview", "WebView", "De nombreuses applis affichent leurs pages avec."),
        protected("com.google.android.apps.nexuslauncher", "Pixel Launcher", "Écran d'accueil du téléphone."),
        protected("com.google.android.configupdater", "Config Updater", "Mises à jour de sécurité de configuration."),
    )

    private val byPackage = entries.associateBy { it.packageName }

    /** Package prefixes that are never safe to disable (overlays and resources loaded at boot). */
    private val PROTECTED_PREFIXES = listOf(
        "com.google.android.overlay.pixelconfig",
        "com.google.android.overlay.gmsconfig",
        "com.google.android.wifi.resources",
        "com.android.settings.overlay",
    )

    fun find(packageName: String): DebloatEntry? = byPackage[packageName]

    fun isProtected(packageName: String): Boolean =
        byPackage[packageName]?.risk == DebloatRisk.PROTECTED || PROTECTED_PREFIXES.any { packageName.startsWith(it) }

    private fun safe(pkg: String, label: String, note: String) = DebloatEntry(pkg, label, DebloatRisk.SAFE, note)
    private fun feature(pkg: String, label: String, note: String) = DebloatEntry(pkg, label, DebloatRisk.FEATURE_LOSS, note)
    private fun protected(pkg: String, label: String, note: String) = DebloatEntry(pkg, label, DebloatRisk.PROTECTED, note)
}
