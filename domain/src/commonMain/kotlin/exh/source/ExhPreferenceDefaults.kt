package exh.source

import tachiyomi.domain.release.service.AppUpdatePolicy

object ExhPreferenceDefaults {
    val SHOULD_AUTO_UPDATE = setOf(
        AppUpdatePolicy.DEVICE_ONLY_ON_WIFI,
    )
    const val HENTAI_ENABLED = false
    const val INCOGNITO_MODE = false
    const val ENABLE_EXHENTAI = false
    const val IMAGE_QUALITY = "auto"
    const val USE_HENTAI_AT_HOME = 0
    const val USE_JAPANESE_TITLE = false
    const val USE_ORIGINAL_IMAGES = false
    const val TAG_FILTER_VALUE = 0
    const val TAG_WATCHING_VALUE = 0
    const val MEMBER_ID = ""
    const val PASS_HASH = ""
    const val IGNEOUS = ""
    const val EH_SETTINGS_PROFILE = -1
    const val EXH_SETTINGS_PROFILE = -1
    const val SETTINGS_KEY = ""
    const val SESSION_COOKIE = ""
    const val HATH_PERKS_COOKIE = ""
    const val SHOW_SYNC_INTRO = true
    const val READ_ONLY_SYNC = false
    const val LENIENT_SYNC = false
    const val SHOW_SETTINGS_UPLOAD_WARNING = true
    const val LOG_LEVEL = 0
    const val AUTO_UPDATE_FREQUENCY = 1
    val AUTO_UPDATE_REQUIREMENTS: Set<String> = emptySet()
    const val AUTO_UPDATE_STATS = ""
    const val WATCHED_LIST_DEFAULT_STATE = false
    val SETTINGS_LANGUAGES =
        "false*false*false\nfalse*false*false\nfalse*false*false\nfalse*false*false\nfalse*false*false\n" +
            "false*false*false\nfalse*false*false\nfalse*false*false\nfalse*false*false\nfalse*false*false\n" +
            "false*false*false\nfalse*false*false\nfalse*false*false\nfalse*false*false\nfalse*false*false\n" +
            "false*false*false\nfalse*false*false"
    const val ENABLED_CATEGORIES = "false,false,false,false,false,false,false,false,false,false"
    const val ENHANCED_EHENTAI_VIEW = true
}
