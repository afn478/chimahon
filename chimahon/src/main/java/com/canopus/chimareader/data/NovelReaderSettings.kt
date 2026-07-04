package com.canopus.chimareader.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import tachiyomi.domain.reader.service.NovelReaderPreferenceKeys
import tachiyomi.domain.reader.service.NovelReaderSettingsDefaults
import tachiyomi.domain.reader.service.NovelReaderSettingsPolicy

val Context.novelReaderDataStore: DataStore<Preferences> by preferencesDataStore(name = "novel_reader_settings")

typealias Theme = tachiyomi.domain.reader.model.NovelReaderTheme
typealias CustomReaderTheme = tachiyomi.domain.reader.model.CustomReaderTheme
typealias StatisticsAutostartMode = tachiyomi.domain.reader.model.StatisticsAutostartMode

class NovelReaderSettings(private val context: Context, private val namespace: String? = null) {

    companion object {
        fun default(): NovelReaderSettings {
            throw IllegalStateException("NovelReaderSettings requires a Context. Pass context from Activity.")
        }
    }

    private fun prefName(name: String): String {
        return NovelReaderSettingsPolicy.preferenceName(name = name, namespace = namespace)
    }

    private fun stringKey(name: String): androidx.datastore.preferences.core.Preferences.Key<String> {
        return stringPreferencesKey(prefName(name))
    }

    private fun booleanKey(name: String): androidx.datastore.preferences.core.Preferences.Key<Boolean> {
        return booleanPreferencesKey(prefName(name))
    }

    private fun intKey(name: String): androidx.datastore.preferences.core.Preferences.Key<Int> {
        return intPreferencesKey(prefName(name))
    }

    private fun doubleKey(name: String): androidx.datastore.preferences.core.Preferences.Key<Double> {
        return doublePreferencesKey(prefName(name))
    }

    private val dataStore = context.novelReaderDataStore

    private fun encodeCustomThemes(themes: List<CustomReaderTheme>): String {
        return NovelReaderSettingsPolicy.encodeCustomThemes(themes)
    }

    private fun decodeCustomThemes(value: String?): List<CustomReaderTheme> {
        return NovelReaderSettingsPolicy.decodeCustomThemes(value)
    }

    private fun androidx.datastore.preferences.core.Preferences.getSafeDouble(key: androidx.datastore.preferences.core.Preferences.Key<Double>, default: Double): Double {
        return try {
            this[key] ?: default
        } catch (e: ClassCastException) {
            val intKey = androidx.datastore.preferences.core.intPreferencesKey(key.name)
            this[intKey]?.toDouble() ?: default
        }
    }

    private fun androidx.datastore.preferences.core.Preferences.getSafeInt(key: androidx.datastore.preferences.core.Preferences.Key<Int>, default: Int): Int {
        return try {
            this[key] ?: default
        } catch (e: ClassCastException) {
            val doubleKey = androidx.datastore.preferences.core.doublePreferencesKey(key.name)
            this[doubleKey]?.toInt() ?: default
        }
    }

    val theme: Flow<Theme> = dataStore.data.map { prefs ->
        Theme.valueOf(prefs[keys.THEME] ?: NovelReaderSettingsDefaults.theme.name)
    }

    val systemLightSepia: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[keys.SYSTEM_LIGHT_SEPIA] ?: NovelReaderSettingsDefaults.systemLightSepia
    }

    val uiTheme: Flow<Theme> = dataStore.data.map { prefs ->
        Theme.valueOf(prefs[keys.UI_THEME] ?: NovelReaderSettingsDefaults.uiTheme.name)
    }

    val customBackgroundColor: Flow<Int> = dataStore.data.map { prefs ->
        prefs[keys.CUSTOM_BACKGROUND_COLOR] ?: NovelReaderSettingsDefaults.customBackgroundColor
    }

    val customTextColor: Flow<Int> = dataStore.data.map { prefs ->
        prefs[keys.CUSTOM_TEXT_COLOR] ?: NovelReaderSettingsDefaults.customTextColor
    }

    val customThemes: Flow<List<CustomReaderTheme>> = dataStore.data.map { prefs ->
        decodeCustomThemes(prefs[keys.CUSTOM_THEMES])
    }

    val customInfoColor: Flow<Int> = dataStore.data.map { prefs ->
        prefs[keys.CUSTOM_INFO_COLOR] ?: NovelReaderSettingsDefaults.customInfoColor
    }

    val verticalWriting: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[keys.VERTICAL_WRITING] ?: NovelReaderSettingsDefaults.verticalWriting
    }

    val selectedFont: Flow<String> = dataStore.data.map { prefs ->
        prefs[keys.SELECTED_FONT] ?: NovelReaderSettingsDefaults.selectedFont
    }

    val fontSize: Flow<Double> = dataStore.data.map { prefs ->
        prefs.getSafeDouble(keys.FONT_SIZE, NovelReaderSettingsDefaults.fontSize)
    }

    val readerHideFurigana: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[keys.READER_HIDE_FURIGANA] ?: NovelReaderSettingsDefaults.readerHideFurigana
    }

    val continuousMode: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[keys.CONTINUOUS_MODE] ?: NovelReaderSettingsDefaults.continuousMode
    }

    val chapterSwipeDistance: Flow<Int> = dataStore.data.map { prefs ->
        prefs.getSafeInt(keys.CHAPTER_SWIPE_DISTANCE, NovelReaderSettingsDefaults.chapterSwipeDistance)
    }

    val chapterTapZones: Flow<Int> = dataStore.data.map { prefs ->
        prefs.getSafeInt(keys.CHAPTER_TAP_ZONES, NovelReaderSettingsDefaults.chapterTapZones)
    }

    val horizontalPadding: Flow<Double> = dataStore.data.map { prefs ->
        prefs.getSafeDouble(keys.HORIZONTAL_PADDING, NovelReaderSettingsDefaults.horizontalPadding)
    }

    val verticalPadding: Flow<Double> = dataStore.data.map { prefs ->
        prefs.getSafeDouble(keys.VERTICAL_PADDING, NovelReaderSettingsDefaults.verticalPadding)
    }

    val avoidPageBreak: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[keys.AVOID_PAGE_BREAK] ?: NovelReaderSettingsDefaults.avoidPageBreak
    }

    val justifyText: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[keys.JUSTIFY_TEXT] ?: NovelReaderSettingsDefaults.justifyText
    }

    val layoutAdvanced: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[keys.LAYOUT_ADVANCED] ?: NovelReaderSettingsDefaults.layoutAdvanced
    }

    val lineHeight: Flow<Double> = dataStore.data.map { prefs ->
        prefs.getSafeDouble(keys.LINE_HEIGHT, NovelReaderSettingsDefaults.lineHeight)
    }

    val characterSpacing: Flow<Double> = dataStore.data.map { prefs ->
        prefs.getSafeDouble(keys.CHARACTER_SPACING, NovelReaderSettingsDefaults.characterSpacing)
    }

    val paragraphSpacing: Flow<Double> = dataStore.data.map { prefs ->
        prefs.getSafeDouble(keys.PARAGRAPH_SPACING, NovelReaderSettingsDefaults.paragraphSpacing)
    }

    val readerShowTitle: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[keys.READER_SHOW_TITLE] ?: NovelReaderSettingsDefaults.readerShowTitle
    }

    val readerShowCharacters: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[keys.READER_SHOW_CHARACTERS] ?: NovelReaderSettingsDefaults.readerShowCharacters
    }

    val readerShowPercentage: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[keys.READER_SHOW_PERCENTAGE] ?: NovelReaderSettingsDefaults.readerShowPercentage
    }

    val readerShowProgressTop: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[keys.READER_SHOW_PROGRESS_TOP] ?: NovelReaderSettingsDefaults.readerShowProgressTop
    }

    val enableStatistics: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[keys.ENABLE_STATISTICS] ?: NovelReaderSettingsDefaults.enableStatistics
    }

    val statisticsAutostartMode: Flow<StatisticsAutostartMode> = dataStore.data.map { prefs ->
        StatisticsAutostartMode.valueOf(
            prefs[keys.STATISTICS_AUTOSTART_MODE] ?: NovelReaderSettingsDefaults.statisticsAutostartMode.name,
        )
    }

    val readerShowReadingSpeed: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[keys.READER_SHOW_READING_SPEED] ?: NovelReaderSettingsDefaults.readerShowReadingSpeed
    }

    val readerShowReadingTime: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[keys.READER_SHOW_READING_TIME] ?: NovelReaderSettingsDefaults.readerShowReadingTime
    }

    val popupWidth: Flow<Int> = dataStore.data.map { prefs ->
        prefs.getSafeInt(keys.POPUP_WIDTH, NovelReaderSettingsDefaults.popupWidth)
    }

    val popupHeight: Flow<Int> = dataStore.data.map { prefs ->
        prefs.getSafeInt(keys.POPUP_HEIGHT, NovelReaderSettingsDefaults.popupHeight)
    }

    val popupFullWidth: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[keys.POPUP_FULL_WIDTH] ?: NovelReaderSettingsDefaults.popupFullWidth
    }

    val popupSwipeToDismiss: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[keys.POPUP_SWIPE_TO_DISMISS] ?: NovelReaderSettingsDefaults.popupSwipeToDismiss
    }

    val popupSwipeThreshold: Flow<Int> = dataStore.data.map { prefs ->
        prefs.getSafeInt(keys.POPUP_SWIPE_THRESHOLD, NovelReaderSettingsDefaults.popupSwipeThreshold)
    }

    val maxResults: Flow<Int> = dataStore.data.map { prefs ->
        prefs.getSafeInt(keys.MAX_RESULTS, NovelReaderSettingsDefaults.maxResults)
    }

    val scanLength: Flow<Int> = dataStore.data.map { prefs ->
        prefs.getSafeInt(keys.SCAN_LENGTH, NovelReaderSettingsDefaults.scanLength)
    }

    val keepScreenOn: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[keys.KEEP_SCREEN_ON] ?: NovelReaderSettingsDefaults.keepScreenOn
    }

    suspend fun setTheme(value: Theme) {
        dataStore.edit { prefs ->
            prefs[keys.THEME] = value.name
        }
    }

    suspend fun setSystemLightSepia(value: Boolean) {
        dataStore.edit { prefs ->
            prefs[keys.SYSTEM_LIGHT_SEPIA] = value
        }
    }

    suspend fun setUiTheme(value: Theme) {
        dataStore.edit { prefs ->
            prefs[keys.UI_THEME] = value.name
        }
    }

    suspend fun setCustomBackgroundColor(value: Int) {
        dataStore.edit { prefs ->
            prefs[keys.CUSTOM_BACKGROUND_COLOR] = value
        }
    }

    suspend fun setCustomTextColor(value: Int) {
        dataStore.edit { prefs ->
            prefs[keys.CUSTOM_TEXT_COLOR] = value
        }
    }

    suspend fun setCustomTheme(value: CustomReaderTheme) {
        dataStore.edit { prefs ->
            prefs[keys.CUSTOM_BACKGROUND_COLOR] = value.backgroundColor
            prefs[keys.CUSTOM_TEXT_COLOR] = value.textColor
            prefs[keys.THEME] = Theme.CUSTOM.name
        }
    }

    suspend fun addCustomTheme(value: CustomReaderTheme) {
        dataStore.edit { prefs ->
            val existing = decodeCustomThemes(prefs[keys.CUSTOM_THEMES])
            val next = NovelReaderSettingsPolicy.addCustomTheme(
                themes = existing,
                theme = value,
                maxThemes = NovelReaderSettingsDefaults.maxCustomThemes,
            )
            prefs[keys.CUSTOM_THEMES] = encodeCustomThemes(next)
            prefs[keys.CUSTOM_BACKGROUND_COLOR] = value.backgroundColor
            prefs[keys.CUSTOM_TEXT_COLOR] = value.textColor
            prefs[keys.THEME] = Theme.CUSTOM.name
        }
    }

    suspend fun deleteCustomTheme(value: CustomReaderTheme) {
        dataStore.edit { prefs ->
            val existing = decodeCustomThemes(prefs[keys.CUSTOM_THEMES])
            val next = NovelReaderSettingsPolicy.deleteCustomTheme(
                themes = existing,
                theme = value,
            )
            prefs[keys.CUSTOM_THEMES] = encodeCustomThemes(next)
        }
    }

    suspend fun renameCustomTheme(old: CustomReaderTheme, newName: String) {
        dataStore.edit { prefs ->
            val existing = decodeCustomThemes(prefs[keys.CUSTOM_THEMES])
            val next = NovelReaderSettingsPolicy.renameCustomTheme(
                themes = existing,
                theme = old,
                newName = newName,
            )
            prefs[keys.CUSTOM_THEMES] = encodeCustomThemes(next)
        }
    }

    suspend fun setCustomInfoColor(value: Int) {
        dataStore.edit { prefs ->
            prefs[keys.CUSTOM_INFO_COLOR] = value
        }
    }

    suspend fun setVerticalWriting(value: Boolean) {
        dataStore.edit { prefs ->
            prefs[keys.VERTICAL_WRITING] = value
        }
    }

    suspend fun setSelectedFont(value: String) {
        dataStore.edit { prefs ->
            prefs[keys.SELECTED_FONT] = value
        }
    }

    suspend fun setFontSize(value: Double) {
        dataStore.edit { prefs ->
            prefs[keys.FONT_SIZE] = value
        }
    }

    suspend fun setReaderHideFurigana(value: Boolean) {
        dataStore.edit { prefs ->
            prefs[keys.READER_HIDE_FURIGANA] = value
        }
    }

    suspend fun setContinuousMode(value: Boolean) {
        dataStore.edit { prefs ->
            prefs[keys.CONTINUOUS_MODE] = value
        }
    }

    suspend fun setChapterSwipeDistance(value: Int) {
        dataStore.edit { prefs ->
            prefs[keys.CHAPTER_SWIPE_DISTANCE] = value
        }
    }

    suspend fun setChapterTapZones(value: Int) {
        dataStore.edit { prefs ->
            prefs[keys.CHAPTER_TAP_ZONES] = value
        }
    }

    suspend fun setHorizontalPadding(value: Double) {
        dataStore.edit { prefs ->
            prefs[keys.HORIZONTAL_PADDING] = value
        }
    }

    suspend fun setVerticalPadding(value: Double) {
        dataStore.edit { prefs ->
            prefs[keys.VERTICAL_PADDING] = value
        }
    }

    suspend fun setAvoidPageBreak(value: Boolean) {
        dataStore.edit { prefs ->
            prefs[keys.AVOID_PAGE_BREAK] = value
        }
    }

    suspend fun setJustifyText(value: Boolean) {
        dataStore.edit { prefs ->
            prefs[keys.JUSTIFY_TEXT] = value
        }
    }

    suspend fun setLayoutAdvanced(value: Boolean) {
        dataStore.edit { prefs ->
            prefs[keys.LAYOUT_ADVANCED] = value
        }
    }

    suspend fun setLineHeight(value: Double) {
        dataStore.edit { prefs ->
            prefs[keys.LINE_HEIGHT] = value
        }
    }

    suspend fun setCharacterSpacing(value: Double) {
        dataStore.edit { prefs ->
            prefs[keys.CHARACTER_SPACING] = value
        }
    }

    suspend fun setParagraphSpacing(value: Double) {
        dataStore.edit { prefs ->
            prefs[keys.PARAGRAPH_SPACING] = value
        }
    }

    suspend fun setReaderShowTitle(value: Boolean) {
        dataStore.edit { prefs ->
            prefs[keys.READER_SHOW_TITLE] = value
        }
    }

    suspend fun setReaderShowCharacters(value: Boolean) {
        dataStore.edit { prefs ->
            prefs[keys.READER_SHOW_CHARACTERS] = value
        }
    }

    suspend fun setReaderShowPercentage(value: Boolean) {
        dataStore.edit { prefs ->
            prefs[keys.READER_SHOW_PERCENTAGE] = value
        }
    }

    suspend fun setReaderShowProgressTop(value: Boolean) {
        dataStore.edit { prefs ->
            prefs[keys.READER_SHOW_PROGRESS_TOP] = value
        }
    }

    suspend fun setEnableStatistics(value: Boolean) {
        dataStore.edit { prefs ->
            prefs[keys.ENABLE_STATISTICS] = value
        }
    }

    suspend fun setStatisticsAutostartMode(value: StatisticsAutostartMode) {
        dataStore.edit { prefs ->
            prefs[keys.STATISTICS_AUTOSTART_MODE] = value.name
        }
    }

    suspend fun setReaderShowReadingSpeed(value: Boolean) {
        dataStore.edit { prefs ->
            prefs[keys.READER_SHOW_READING_SPEED] = value
        }
    }

    suspend fun setReaderShowReadingTime(value: Boolean) {
        dataStore.edit { prefs ->
            prefs[keys.READER_SHOW_READING_TIME] = value
        }
    }

    suspend fun setPopupWidth(value: Int) {
        dataStore.edit { prefs ->
            prefs[keys.POPUP_WIDTH] = value
        }
    }

    suspend fun setPopupHeight(value: Int) {
        dataStore.edit { prefs ->
            prefs[keys.POPUP_HEIGHT] = value
        }
    }

    suspend fun setPopupFullWidth(value: Boolean) {
        dataStore.edit { prefs ->
            prefs[keys.POPUP_FULL_WIDTH] = value
        }
    }

    suspend fun setPopupSwipeToDismiss(value: Boolean) {
        dataStore.edit { prefs ->
            prefs[keys.POPUP_SWIPE_TO_DISMISS] = value
        }
    }

    suspend fun setPopupSwipeThreshold(value: Int) {
        dataStore.edit { prefs ->
            prefs[keys.POPUP_SWIPE_THRESHOLD] = value
        }
    }

    suspend fun setMaxResults(value: Int) {
        dataStore.edit { prefs ->
            prefs[keys.MAX_RESULTS] = value
        }
    }

    suspend fun setScanLength(value: Int) {
        dataStore.edit { prefs ->
            prefs[keys.SCAN_LENGTH] = value
        }
    }

    suspend fun setKeepScreenOn(value: Boolean) {
        dataStore.edit { prefs ->
            prefs[keys.KEEP_SCREEN_ON] = value
        }
    }

    @Suppress("PropertyName", "ktlint:standard:property-naming")
    private inner class PreferencesKeys {
        val THEME = stringKey(NovelReaderPreferenceKeys.THEME)
        val SYSTEM_LIGHT_SEPIA = booleanKey(NovelReaderPreferenceKeys.SYSTEM_LIGHT_SEPIA)
        val UI_THEME = stringKey(NovelReaderPreferenceKeys.UI_THEME)
        val CUSTOM_BACKGROUND_COLOR = intKey(NovelReaderPreferenceKeys.CUSTOM_BACKGROUND_COLOR)
        val CUSTOM_TEXT_COLOR = intKey(NovelReaderPreferenceKeys.CUSTOM_TEXT_COLOR)
        val CUSTOM_THEMES = stringKey(NovelReaderPreferenceKeys.CUSTOM_THEMES)
        val CUSTOM_INFO_COLOR = intKey(NovelReaderPreferenceKeys.CUSTOM_INFO_COLOR)
        val VERTICAL_WRITING = booleanKey(NovelReaderPreferenceKeys.VERTICAL_WRITING)
        val SELECTED_FONT = stringKey(NovelReaderPreferenceKeys.SELECTED_FONT)
        val FONT_SIZE = doubleKey(NovelReaderPreferenceKeys.FONT_SIZE)
        val READER_HIDE_FURIGANA = booleanKey(NovelReaderPreferenceKeys.READER_HIDE_FURIGANA)
        val CONTINUOUS_MODE = booleanKey(NovelReaderPreferenceKeys.CONTINUOUS_MODE)
        val CHAPTER_SWIPE_DISTANCE = intKey(NovelReaderPreferenceKeys.CHAPTER_SWIPE_DISTANCE)
        val CHAPTER_TAP_ZONES = intKey(NovelReaderPreferenceKeys.CHAPTER_TAP_ZONES)
        val HORIZONTAL_PADDING = doubleKey(NovelReaderPreferenceKeys.HORIZONTAL_PADDING)
        val VERTICAL_PADDING = doubleKey(NovelReaderPreferenceKeys.VERTICAL_PADDING)
        val AVOID_PAGE_BREAK = booleanKey(NovelReaderPreferenceKeys.AVOID_PAGE_BREAK)
        val JUSTIFY_TEXT = booleanKey(NovelReaderPreferenceKeys.JUSTIFY_TEXT)
        val LAYOUT_ADVANCED = booleanKey(NovelReaderPreferenceKeys.LAYOUT_ADVANCED)
        val LINE_HEIGHT = doubleKey(NovelReaderPreferenceKeys.LINE_HEIGHT)
        val CHARACTER_SPACING = doubleKey(NovelReaderPreferenceKeys.CHARACTER_SPACING)
        val PARAGRAPH_SPACING = doubleKey(NovelReaderPreferenceKeys.PARAGRAPH_SPACING)
        val READER_SHOW_TITLE = booleanKey(NovelReaderPreferenceKeys.READER_SHOW_TITLE)
        val READER_SHOW_CHARACTERS = booleanKey(NovelReaderPreferenceKeys.READER_SHOW_CHARACTERS)
        val READER_SHOW_PERCENTAGE = booleanKey(NovelReaderPreferenceKeys.READER_SHOW_PERCENTAGE)
        val READER_SHOW_PROGRESS_TOP = booleanKey(NovelReaderPreferenceKeys.READER_SHOW_PROGRESS_TOP)
        val ENABLE_STATISTICS = booleanKey(NovelReaderPreferenceKeys.ENABLE_STATISTICS)
        val STATISTICS_AUTOSTART_MODE = stringKey(NovelReaderPreferenceKeys.STATISTICS_AUTOSTART_MODE)
        val READER_SHOW_READING_SPEED = booleanKey(NovelReaderPreferenceKeys.READER_SHOW_READING_SPEED)
        val READER_SHOW_READING_TIME = booleanKey(NovelReaderPreferenceKeys.READER_SHOW_READING_TIME)
        val POPUP_WIDTH = intKey(NovelReaderPreferenceKeys.POPUP_WIDTH)
        val POPUP_HEIGHT = intKey(NovelReaderPreferenceKeys.POPUP_HEIGHT)
        val POPUP_FULL_WIDTH = booleanKey(NovelReaderPreferenceKeys.POPUP_FULL_WIDTH)
        val POPUP_SWIPE_TO_DISMISS = booleanKey(NovelReaderPreferenceKeys.POPUP_SWIPE_TO_DISMISS)
        val POPUP_SWIPE_THRESHOLD = intKey(NovelReaderPreferenceKeys.POPUP_SWIPE_THRESHOLD)
        val MAX_RESULTS = intKey(NovelReaderPreferenceKeys.MAX_RESULTS)
        val SCAN_LENGTH = intKey(NovelReaderPreferenceKeys.SCAN_LENGTH)
        val KEEP_SCREEN_ON = booleanKey(NovelReaderPreferenceKeys.KEEP_SCREEN_ON)
    }

    private val keys = PreferencesKeys()
}

val Context.novelReaderSettings: NovelReaderSettings
    get() = NovelReaderSettings(this)
