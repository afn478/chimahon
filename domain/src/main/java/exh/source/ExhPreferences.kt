package exh.source

import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore

class ExhPreferences(
    private val preferenceStore: PreferenceStore,
) {
    // KMK -->
    fun appShouldAutoUpdate() = preferenceStore.getStringSet(
        ExhPreferenceKeys.SHOULD_AUTO_UPDATE,
        ExhPreferenceDefaults.SHOULD_AUTO_UPDATE,
    )
    // KMK <--

    // SY -->
    fun isHentaiEnabled() = preferenceStore.getBoolean(
        ExhPreferenceKeys.HENTAI_ENABLED,
        ExhPreferenceDefaults.HENTAI_ENABLED,
    )

    // KMK -->
    fun ehIncognitoMode() = preferenceStore.getBoolean(
        ExhPreferenceKeys.INCOGNITO_MODE,
        ExhPreferenceDefaults.INCOGNITO_MODE,
    )
    // KMK <--

    fun enableExhentai() = preferenceStore.getBoolean(
        Preference.privateKey(ExhPreferenceKeys.ENABLE_EXHENTAI),
        ExhPreferenceDefaults.ENABLE_EXHENTAI,
    )

    fun imageQuality() = preferenceStore.getString(
        ExhPreferenceKeys.IMAGE_QUALITY,
        ExhPreferenceDefaults.IMAGE_QUALITY,
    )

    fun useHentaiAtHome() = preferenceStore.getInt(
        ExhPreferenceKeys.USE_HENTAI_AT_HOME,
        ExhPreferenceDefaults.USE_HENTAI_AT_HOME,
    )

    fun useJapaneseTitle() = preferenceStore.getBoolean(
        ExhPreferenceKeys.USE_JAPANESE_TITLE,
        ExhPreferenceDefaults.USE_JAPANESE_TITLE,
    )

    fun exhUseOriginalImages() = preferenceStore.getBoolean(
        ExhPreferenceKeys.USE_ORIGINAL_IMAGES,
        ExhPreferenceDefaults.USE_ORIGINAL_IMAGES,
    )

    fun ehTagFilterValue() = preferenceStore.getInt(
        ExhPreferenceKeys.TAG_FILTER_VALUE,
        ExhPreferenceDefaults.TAG_FILTER_VALUE,
    )

    fun ehTagWatchingValue() = preferenceStore.getInt(
        ExhPreferenceKeys.TAG_WATCHING_VALUE,
        ExhPreferenceDefaults.TAG_WATCHING_VALUE,
    )

    // EH Cookies
    fun memberIdVal() = preferenceStore.getString(
        Preference.privateKey(ExhPreferenceKeys.MEMBER_ID),
        ExhPreferenceDefaults.MEMBER_ID,
    )

    fun passHashVal() = preferenceStore.getString(
        Preference.privateKey(ExhPreferenceKeys.PASS_HASH),
        ExhPreferenceDefaults.PASS_HASH,
    )
    fun igneousVal() = preferenceStore.getString(
        Preference.privateKey(ExhPreferenceKeys.IGNEOUS),
        ExhPreferenceDefaults.IGNEOUS,
    )
    fun ehSettingsProfile() = preferenceStore.getInt(
        Preference.privateKey(ExhPreferenceKeys.EH_SETTINGS_PROFILE),
        ExhPreferenceDefaults.EH_SETTINGS_PROFILE,
    )
    fun exhSettingsProfile() = preferenceStore.getInt(
        Preference.privateKey(ExhPreferenceKeys.EXH_SETTINGS_PROFILE),
        ExhPreferenceDefaults.EXH_SETTINGS_PROFILE,
    )
    fun exhSettingsKey() = preferenceStore.getString(
        Preference.privateKey(ExhPreferenceKeys.SETTINGS_KEY),
        ExhPreferenceDefaults.SETTINGS_KEY,
    )
    fun exhSessionCookie() = preferenceStore.getString(
        Preference.privateKey(ExhPreferenceKeys.SESSION_COOKIE),
        ExhPreferenceDefaults.SESSION_COOKIE,
    )
    fun exhHathPerksCookies() = preferenceStore.getString(
        Preference.privateKey(ExhPreferenceKeys.HATH_PERKS_COOKIE),
        ExhPreferenceDefaults.HATH_PERKS_COOKIE,
    )

    fun exhShowSyncIntro() = preferenceStore.getBoolean(
        ExhPreferenceKeys.SHOW_SYNC_INTRO,
        ExhPreferenceDefaults.SHOW_SYNC_INTRO,
    )

    fun exhReadOnlySync() = preferenceStore.getBoolean(
        ExhPreferenceKeys.READ_ONLY_SYNC,
        ExhPreferenceDefaults.READ_ONLY_SYNC,
    )

    fun exhLenientSync() = preferenceStore.getBoolean(
        ExhPreferenceKeys.LENIENT_SYNC,
        ExhPreferenceDefaults.LENIENT_SYNC,
    )

    fun exhShowSettingsUploadWarning() = preferenceStore.getBoolean(
        ExhPreferenceKeys.SHOW_SETTINGS_UPLOAD_WARNING,
        ExhPreferenceDefaults.SHOW_SETTINGS_UPLOAD_WARNING,
    )

    fun logLevel() = preferenceStore.getInt(
        ExhPreferenceKeys.LOG_LEVEL,
        ExhPreferenceDefaults.LOG_LEVEL,
    )

    fun exhAutoUpdateFrequency() = preferenceStore.getInt(
        ExhPreferenceKeys.AUTO_UPDATE_FREQUENCY,
        ExhPreferenceDefaults.AUTO_UPDATE_FREQUENCY,
    )

    fun exhAutoUpdateRequirements() = preferenceStore.getStringSet(
        ExhPreferenceKeys.AUTO_UPDATE_REQUIREMENTS,
        ExhPreferenceDefaults.AUTO_UPDATE_REQUIREMENTS,
    )

    fun exhAutoUpdateStats() = preferenceStore.getString(
        Preference.appStateKey(ExhPreferenceKeys.AUTO_UPDATE_STATS),
        ExhPreferenceDefaults.AUTO_UPDATE_STATS,
    )

    fun exhWatchedListDefaultState() = preferenceStore.getBoolean(
        ExhPreferenceKeys.WATCHED_LIST_DEFAULT_STATE,
        ExhPreferenceDefaults.WATCHED_LIST_DEFAULT_STATE,
    )

    fun exhSettingsLanguages() = preferenceStore.getString(
        ExhPreferenceKeys.SETTINGS_LANGUAGES,
        ExhPreferenceDefaults.SETTINGS_LANGUAGES,
    )

    fun exhEnabledCategories() = preferenceStore.getString(
        ExhPreferenceKeys.ENABLED_CATEGORIES,
        ExhPreferenceDefaults.ENABLED_CATEGORIES,
    )

    fun enhancedEHentaiView() = preferenceStore.getBoolean(
        ExhPreferenceKeys.ENHANCED_EHENTAI_VIEW,
        ExhPreferenceDefaults.ENHANCED_EHENTAI_VIEW,
    )
}
