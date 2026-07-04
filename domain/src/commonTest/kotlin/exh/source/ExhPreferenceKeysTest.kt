package exh.source

import tachiyomi.domain.release.service.AppUpdatePolicy
import kotlin.test.Test
import kotlin.test.assertEquals

class ExhPreferenceKeysTest {
    @Test
    fun updateAndVisibilityPreferenceKeysRemainStable() {
        assertEquals("should_auto_update", ExhPreferenceKeys.SHOULD_AUTO_UPDATE)
        assertEquals("eh_is_hentai_enabled", ExhPreferenceKeys.HENTAI_ENABLED)
        assertEquals("eh_incognito_mode", ExhPreferenceKeys.INCOGNITO_MODE)
        assertEquals("enable_exhentai", ExhPreferenceKeys.ENABLE_EXHENTAI)
        assertEquals("ehentai_quality", ExhPreferenceKeys.IMAGE_QUALITY)
        assertEquals("eh_enable_hah", ExhPreferenceKeys.USE_HENTAI_AT_HOME)
        assertEquals("use_jp_title", ExhPreferenceKeys.USE_JAPANESE_TITLE)
        assertEquals("eh_useOrigImages", ExhPreferenceKeys.USE_ORIGINAL_IMAGES)
        assertEquals("enhanced_e_hentai_view", ExhPreferenceKeys.ENHANCED_EHENTAI_VIEW)
    }

    @Test
    fun tagCookieAndProfilePreferenceKeysRemainStable() {
        assertEquals("eh_tag_filtering_value", ExhPreferenceKeys.TAG_FILTER_VALUE)
        assertEquals("eh_tag_watching_value", ExhPreferenceKeys.TAG_WATCHING_VALUE)
        assertEquals("eh_ipb_member_id", ExhPreferenceKeys.MEMBER_ID)
        assertEquals("eh_ipb_pass_hash", ExhPreferenceKeys.PASS_HASH)
        assertEquals("eh_igneous", ExhPreferenceKeys.IGNEOUS)
        assertEquals("eh_ehSettingsProfile", ExhPreferenceKeys.EH_SETTINGS_PROFILE)
        assertEquals("eh_exhSettingsProfile", ExhPreferenceKeys.EXH_SETTINGS_PROFILE)
        assertEquals("eh_settingsKey", ExhPreferenceKeys.SETTINGS_KEY)
        assertEquals("eh_sessionCookie", ExhPreferenceKeys.SESSION_COOKIE)
        assertEquals("eh_hathPerksCookie", ExhPreferenceKeys.HATH_PERKS_COOKIE)
    }

    @Test
    fun syncAndAutoUpdatePreferenceKeysRemainStable() {
        assertEquals("eh_show_sync_intro", ExhPreferenceKeys.SHOW_SYNC_INTRO)
        assertEquals("eh_sync_read_only", ExhPreferenceKeys.READ_ONLY_SYNC)
        assertEquals("eh_lenient_sync", ExhPreferenceKeys.LENIENT_SYNC)
        assertEquals("eh_showSettingsUploadWarning2", ExhPreferenceKeys.SHOW_SETTINGS_UPLOAD_WARNING)
        assertEquals("eh_log_level", ExhPreferenceKeys.LOG_LEVEL)
        assertEquals("eh_auto_update_frequency", ExhPreferenceKeys.AUTO_UPDATE_FREQUENCY)
        assertEquals("eh_auto_update_restrictions", ExhPreferenceKeys.AUTO_UPDATE_REQUIREMENTS)
        assertEquals("eh_auto_update_stats", ExhPreferenceKeys.AUTO_UPDATE_STATS)
        assertEquals("eh_watched_list_default_state", ExhPreferenceKeys.WATCHED_LIST_DEFAULT_STATE)
        assertEquals("eh_settings_languages", ExhPreferenceKeys.SETTINGS_LANGUAGES)
        assertEquals("eh_enabled_categories", ExhPreferenceKeys.ENABLED_CATEGORIES)
    }

    @Test
    fun updateAndVisibilityPreferenceDefaultsRemainStable() {
        assertEquals(
            setOf(AppUpdatePolicy.DEVICE_ONLY_ON_WIFI),
            ExhPreferenceDefaults.SHOULD_AUTO_UPDATE,
        )
        assertEquals(false, ExhPreferenceDefaults.HENTAI_ENABLED)
        assertEquals(false, ExhPreferenceDefaults.INCOGNITO_MODE)
        assertEquals(false, ExhPreferenceDefaults.ENABLE_EXHENTAI)
        assertEquals("auto", ExhPreferenceDefaults.IMAGE_QUALITY)
        assertEquals(0, ExhPreferenceDefaults.USE_HENTAI_AT_HOME)
        assertEquals(false, ExhPreferenceDefaults.USE_JAPANESE_TITLE)
        assertEquals(false, ExhPreferenceDefaults.USE_ORIGINAL_IMAGES)
        assertEquals(true, ExhPreferenceDefaults.ENHANCED_EHENTAI_VIEW)
    }

    @Test
    fun tagCookieAndProfilePreferenceDefaultsRemainStable() {
        assertEquals(0, ExhPreferenceDefaults.TAG_FILTER_VALUE)
        assertEquals(0, ExhPreferenceDefaults.TAG_WATCHING_VALUE)
        assertEquals("", ExhPreferenceDefaults.MEMBER_ID)
        assertEquals("", ExhPreferenceDefaults.PASS_HASH)
        assertEquals("", ExhPreferenceDefaults.IGNEOUS)
        assertEquals(-1, ExhPreferenceDefaults.EH_SETTINGS_PROFILE)
        assertEquals(-1, ExhPreferenceDefaults.EXH_SETTINGS_PROFILE)
        assertEquals("", ExhPreferenceDefaults.SETTINGS_KEY)
        assertEquals("", ExhPreferenceDefaults.SESSION_COOKIE)
        assertEquals("", ExhPreferenceDefaults.HATH_PERKS_COOKIE)
    }

    @Test
    fun syncAndAutoUpdatePreferenceDefaultsRemainStable() {
        assertEquals(true, ExhPreferenceDefaults.SHOW_SYNC_INTRO)
        assertEquals(false, ExhPreferenceDefaults.READ_ONLY_SYNC)
        assertEquals(false, ExhPreferenceDefaults.LENIENT_SYNC)
        assertEquals(true, ExhPreferenceDefaults.SHOW_SETTINGS_UPLOAD_WARNING)
        assertEquals(0, ExhPreferenceDefaults.LOG_LEVEL)
        assertEquals(1, ExhPreferenceDefaults.AUTO_UPDATE_FREQUENCY)
        assertEquals(emptySet(), ExhPreferenceDefaults.AUTO_UPDATE_REQUIREMENTS)
        assertEquals("", ExhPreferenceDefaults.AUTO_UPDATE_STATS)
        assertEquals(false, ExhPreferenceDefaults.WATCHED_LIST_DEFAULT_STATE)
        assertEquals(
            "false*false*false\nfalse*false*false\nfalse*false*false\nfalse*false*false\nfalse*false*false\n" +
                "false*false*false\nfalse*false*false\nfalse*false*false\nfalse*false*false\nfalse*false*false\n" +
                "false*false*false\nfalse*false*false\nfalse*false*false\nfalse*false*false\nfalse*false*false\n" +
                "false*false*false\nfalse*false*false",
            ExhPreferenceDefaults.SETTINGS_LANGUAGES,
        )
        assertEquals(
            "false,false,false,false,false,false,false,false,false,false",
            ExhPreferenceDefaults.ENABLED_CATEGORIES,
        )
    }
}
