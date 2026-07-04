package com.canopus.chimareader.ui.reader

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import com.canopus.chimareader.data.BookMetadata
import com.canopus.chimareader.data.BookStorage
import java.io.File
import tachiyomi.domain.reader.service.NovelReaderInputPolicy
import tachiyomi.domain.reader.service.NovelReaderScreenPolicy

open class NovelReaderActivity : ComponentActivity() {

    companion object {
        internal const val EXTRA_BOOK_DIR = "extra_book_dir"

        /**
         * Set to [ChimaReaderActivity] from AppModule so that BookshelfScreen's
         * existing [launch] call lands in the app-side subclass (which has the
         * lookup popup), without requiring chimahon to import from app.
         */
        var activityClass: Class<out ComponentActivity> = NovelReaderActivity::class.java

        fun launch(context: Context, bookDir: File) {
            val intent = Intent(context, activityClass).apply {
                putExtra(EXTRA_BOOK_DIR, bookDir.absolutePath)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            context.startActivity(intent)
        }
    }

    /** Controls whether the reader interactions (taps, selection) are enabled. */
    protected var isPopupActive by androidx.compose.runtime.mutableStateOf(false)

    protected var readerViewModel by androidx.compose.runtime.mutableStateOf<ReaderViewModel?>(null)
    protected var bookMetadata: BookMetadata? = null
    protected var showHud by androidx.compose.runtime.mutableStateOf(false)
    private var readerBackgroundColor: Int = 0xFF000000.toInt()

    protected open fun handleVolumeKey(forward: Boolean): Boolean {
        val vm = readerViewModel ?: return false
        vm.bridge.paginate(forward)
        return true
    }

    override fun onKeyDown(keyCode: Int, event: android.view.KeyEvent): Boolean {
        val action = NovelReaderInputPolicy.hardwareKeyDownAction(
            key = keyCode.readerHardwareKey(),
            popupActive = isPopupActive,
        )
        if (performHardwareKeyAction(action)) {
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: android.view.KeyEvent): Boolean {
        val action = NovelReaderInputPolicy.hardwareKeyUpAction(
            key = keyCode.readerHardwareKey(),
            popupActive = isPopupActive,
            ctrlPressed = event.isCtrlModifierPressed(),
        )
        if (performHardwareKeyAction(action)) {
            return true
        }
        return super.onKeyUp(keyCode, event)
    }

    private fun performHardwareKeyAction(action: NovelReaderInputPolicy.HardwareKeyAction): Boolean {
        return when (action) {
            NovelReaderInputPolicy.HardwareKeyAction.Ignore -> false
            NovelReaderInputPolicy.HardwareKeyAction.Consume -> true
            is NovelReaderInputPolicy.HardwareKeyAction.HandleVolumeKey -> handleVolumeKey(action.forward)
            is NovelReaderInputPolicy.HardwareKeyAction.Paginate -> {
                readerViewModel?.bridge?.paginate(action.forward)
                true
            }
            is NovelReaderInputPolicy.HardwareKeyAction.ChangeChapter -> {
                if (action.forward) {
                    readerViewModel?.nextChapter()
                } else {
                    readerViewModel?.previousChapter()
                }
                true
            }
            NovelReaderInputPolicy.HardwareKeyAction.ToggleHud -> {
                showHud = NovelReaderScreenPolicy.hudVisibleAfterToggle(showHud)
                applySystemBarsState()
                true
            }
        }
    }

    /** Override in subclass to receive text selection events from the reader. */
    protected open fun onLookupRequested(word: String, sentence: String, x: Float, y: Float, w: Float, h: Float) = Unit

    /** Override in subclass to receive the sentence context after onLookupRequested. */
    protected open fun onSentenceReady(sentence: String) = Unit

    /** Override in subclass to dismiss the popup when background is tapped. */
    protected open fun onDismissPopupRequested() = Unit

    protected open fun onDismissPopup() {
        isPopupActive = false
    }

    @Composable
    protected open fun PopupOverlay() {}

    @Composable
    protected open fun AdditionalAppearanceSettings() {}

    /** Subclasses can override to pass a profile ID for per-profile settings. */
    protected open fun getSettingsNamespace(): String? = null

    /** Override to receive selection rects from JS for native highlight overlay. */
    protected open fun getSelectionRectsCallback(): ((String) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        super.onCreate(savedInstanceState)

        val path = intent.getStringExtra(EXTRA_BOOK_DIR)
        val root = path?.let(::File)
        val storedMetadata = root
            ?.takeIf { it.exists() && it.isDirectory }
            ?.let(BookStorage::loadMetadata)
        val metadata = when (
            val action = NovelReaderScreenPolicy.hostLaunchAction(
                bookDirPath = path,
                rootExists = root?.exists() == true,
                rootIsDirectory = root?.isDirectory == true,
                rootName = root?.name.orEmpty(),
                storedMetadata = storedMetadata,
            )
        ) {
            NovelReaderScreenPolicy.HostLaunchAction.Finish -> {
                finish()
                return
            }
            is NovelReaderScreenPolicy.HostLaunchAction.OpenBook -> action.metadata
        }
        bookMetadata = metadata

        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            Box(Modifier.fillMaxSize()) {
                ReaderScreen(
                    book = metadata,
                    showHud = showHud,
                    onBack = { finish() },
                    onShowHudChanged = { visible ->
                        showHud = visible
                        applySystemBarsState()
                    },
                    onThemeChanged = { bgColor ->
                        readerBackgroundColor = bgColor
                        applySystemBarsState()
                    },
                    onLookupRequested = { word, sentence, x, y, w, h -> onLookupRequested(word, sentence, x, y, w, h) },
                    onSentenceReady = { sentence -> onSentenceReady(sentence) },
                    onDismissPopupRequested = { onDismissPopupRequested() },
                    isPopupActive = isPopupActive,
                    onViewModelReady = { readerViewModel = it },
                    additionalSettings = { AdditionalAppearanceSettings() },
                    settingsNamespace = getSettingsNamespace(),
                    onSelectionRectsReceived = getSelectionRectsCallback(),
                )
                PopupOverlay()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        applySystemBarsState()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            applySystemBarsState()
        }
    }

    private fun applySystemBarsState() {
        val state = NovelReaderScreenPolicy.systemBarsState(
            showHud = showHud,
            backgroundColor = readerBackgroundColor,
        )
        val windowInsetsController = androidx.core.view.WindowInsetsControllerCompat(window, window.decorView)
        if (state.visible) {
            windowInsetsController.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
        } else {
            windowInsetsController.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
        }
        windowInsetsController.isAppearanceLightStatusBars = state.useDarkIcons
        windowInsetsController.isAppearanceLightNavigationBars = state.useDarkIcons
        windowInsetsController.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }
}

private fun Int.readerHardwareKey(): NovelReaderInputPolicy.HardwareKey {
    return when (this) {
        android.view.KeyEvent.KEYCODE_VOLUME_UP -> NovelReaderInputPolicy.HardwareKey.VOLUME_UP
        android.view.KeyEvent.KEYCODE_VOLUME_DOWN -> NovelReaderInputPolicy.HardwareKey.VOLUME_DOWN
        android.view.KeyEvent.KEYCODE_DPAD_UP -> NovelReaderInputPolicy.HardwareKey.DPAD_UP
        android.view.KeyEvent.KEYCODE_DPAD_DOWN -> NovelReaderInputPolicy.HardwareKey.DPAD_DOWN
        android.view.KeyEvent.KEYCODE_DPAD_LEFT -> NovelReaderInputPolicy.HardwareKey.DPAD_LEFT
        android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> NovelReaderInputPolicy.HardwareKey.DPAD_RIGHT
        android.view.KeyEvent.KEYCODE_PAGE_UP -> NovelReaderInputPolicy.HardwareKey.PAGE_UP
        android.view.KeyEvent.KEYCODE_PAGE_DOWN -> NovelReaderInputPolicy.HardwareKey.PAGE_DOWN
        android.view.KeyEvent.KEYCODE_N -> NovelReaderInputPolicy.HardwareKey.NEXT
        android.view.KeyEvent.KEYCODE_P -> NovelReaderInputPolicy.HardwareKey.PREVIOUS
        android.view.KeyEvent.KEYCODE_MENU -> NovelReaderInputPolicy.HardwareKey.MENU
        else -> NovelReaderInputPolicy.HardwareKey.OTHER
    }
}

private fun android.view.KeyEvent.isCtrlModifierPressed(): Boolean {
    return metaState and android.view.KeyEvent.META_CTRL_ON != 0
}
