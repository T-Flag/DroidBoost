package fr.tflag.droidboost.core.privacy

/** Sensitive permission groups as the user thinks about them, with the runtime permissions they cover. */
enum class PermissionGroup(val label: String, val permissions: Set<String>) {
    LOCATION(
        "Localisation",
        setOf("android.permission.ACCESS_FINE_LOCATION", "android.permission.ACCESS_COARSE_LOCATION", BACKGROUND_LOCATION),
    ),
    CAMERA("Caméra", setOf("android.permission.CAMERA")),
    MICROPHONE("Micro", setOf("android.permission.RECORD_AUDIO")),
    CONTACTS(
        "Contacts",
        setOf("android.permission.READ_CONTACTS", "android.permission.WRITE_CONTACTS", "android.permission.GET_ACCOUNTS"),
    ),
    CALENDAR("Agenda", setOf("android.permission.READ_CALENDAR", "android.permission.WRITE_CALENDAR")),
    SMS(
        "SMS",
        setOf(
            "android.permission.READ_SMS",
            "android.permission.SEND_SMS",
            "android.permission.RECEIVE_SMS",
            "android.permission.RECEIVE_MMS",
            "android.permission.RECEIVE_WAP_PUSH",
        ),
    ),
    PHONE(
        "Téléphone et appels",
        setOf(
            "android.permission.READ_PHONE_STATE",
            "android.permission.READ_PHONE_NUMBERS",
            "android.permission.CALL_PHONE",
            "android.permission.ANSWER_PHONE_CALLS",
            "android.permission.READ_CALL_LOG",
            "android.permission.WRITE_CALL_LOG",
            "android.permission.ADD_VOICEMAIL",
            "android.permission.USE_SIP",
        ),
    ),
    PHOTOS(
        "Photos et vidéos",
        setOf(
            "android.permission.READ_MEDIA_IMAGES",
            "android.permission.READ_MEDIA_VIDEO",
            "android.permission.READ_MEDIA_VISUAL_USER_SELECTED",
            "android.permission.READ_EXTERNAL_STORAGE",
        ),
    ),
    AUDIO("Musique et audio", setOf("android.permission.READ_MEDIA_AUDIO")),
    NEARBY(
        "Appareils à proximité",
        setOf(
            "android.permission.BLUETOOTH_SCAN",
            "android.permission.BLUETOOTH_CONNECT",
            "android.permission.BLUETOOTH_ADVERTISE",
            "android.permission.NEARBY_WIFI_DEVICES",
            "android.permission.UWB_RANGING",
        ),
    ),
    BODY(
        "Capteurs corporels et activité",
        setOf(
            "android.permission.BODY_SENSORS",
            "android.permission.BODY_SENSORS_BACKGROUND",
            "android.permission.ACTIVITY_RECOGNITION",
        ),
    ),
    NOTIFICATIONS("Notifications", setOf("android.permission.POST_NOTIFICATIONS")),
    ;

    companion object {
        /** Groups touched by a set of granted permissions, with the permissions found in each. */
        fun grouped(granted: Set<String>): Map<PermissionGroup, Set<String>> =
            entries.associateWith { group -> group.permissions intersect granted }
                .filterValues { it.isNotEmpty() }

        /** Health Connect permissions (`android.permission.health.*`) count as body data too. */
        fun groupOf(permission: String): PermissionGroup? =
            entries.firstOrNull { permission in it.permissions }
                ?: BODY.takeIf { permission.startsWith("android.permission.health.") }
    }
}

const val BACKGROUND_LOCATION = "android.permission.ACCESS_BACKGROUND_LOCATION"
