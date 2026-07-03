package app.chimahon.shared

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import tachiyomi.core.platform.storage.AndroidPlatformStorageDirectories
import java.io.File

internal actual object ChimahonPlatformIntegration {
    private val context: Context
        get() = ChimahonAndroidHost.requireContext()

    private val storageDirectories: AndroidPlatformStorageDirectories
        get() = AndroidPlatformStorageDirectories(context)

    actual fun openExternalUrl(url: String): Boolean {
        val candidate = url.normalizedExternalUrlString() ?: return copyText(url)
        return startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(candidate)),
        ) || copyText(candidate)
    }

    actual fun openPath(path: String): Boolean {
        val uri = path.fileUriOrNull() ?: return false
        return startActivity(
            Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            },
        )
    }

    actual fun revealPath(path: String): Boolean {
        return openPath(path) || copyText(path)
    }

    actual fun copyText(text: String): Boolean {
        return runCatching {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText(APP_NAME, text))
            true
        }.getOrDefault(false)
    }

    actual fun shareText(text: String, title: String?): Boolean {
        if (text.isBlank()) return false
        return startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, text)
                },
                title?.trim()?.takeIf(String::isNotBlank),
            ),
        ) || copyText(text)
    }

    actual fun shareFile(path: String, title: String?): Boolean {
        val uri = path.fileUriOrNull() ?: return false
        return startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "*/*"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                },
                title?.trim()?.takeIf(String::isNotBlank),
            ),
        ) || copyText(path)
    }

    actual fun platformInfo(): ChimahonPlatformInfo {
        val packageInfo = runCatching {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0)
        }.getOrNull()

        return ChimahonPlatformInfo(
            platformName = "Android",
            platformVersion = Build.VERSION.RELEASE.orEmpty(),
            deviceModel = listOf(Build.MANUFACTURER, Build.MODEL)
                .filter(String::isNotBlank)
                .joinToString(" ")
                .ifBlank { Build.DEVICE.orEmpty() },
            appVersion = packageInfo?.versionName,
            buildNumber = packageInfo?.longVersionCodeCompat()?.toString(),
        )
    }

    actual fun storagePaths(): ChimahonStoragePaths {
        storageDirectories.ensureChimahonDirectories()
        return ChimahonStoragePaths(
            filesDir = storageDirectories.filesDir.toString(),
            cacheDir = storageDirectories.cacheDir.toString(),
            downloadsDir = storageDirectories.defaultDownloadsDir(APP_NAME).toString(),
            temporaryDir = storageDirectories.temporaryDir.toString(),
        )
    }

    private fun startActivity(intent: Intent): Boolean {
        return runCatching {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }
}

private fun String.normalizedExternalUrlString(): String? {
    val candidate = trim()
    if (candidate.isBlank() || candidate.any(Char::isWhitespace)) return null
    return when {
        candidate.hasUrlScheme() -> candidate
        candidate.contains("@") -> null
        candidate.looksLikeHost() -> "https://$candidate"
        else -> null
    }
}

private fun String.fileUriOrNull(): Uri? {
    val candidate = trim()
    if (candidate.isBlank()) return null
    if (candidate.startsWith("content:", ignoreCase = true) || candidate.startsWith("file:", ignoreCase = true)) {
        return Uri.parse(candidate)
    }

    val file = File(candidate)
    return if (file.exists()) Uri.fromFile(file) else null
}

private fun String.hasUrlScheme(): Boolean {
    val colon = indexOf(':')
    if (colon <= 0) return false
    val scheme = take(colon)
    return scheme.first().isLetter() &&
        scheme.all { it.isLetterOrDigit() || it == '+' || it == '-' || it == '.' }
}

private fun String.looksLikeHost(): Boolean {
    if (startsWith("/") || startsWith("#")) return false
    val host = substringBefore('/').substringBefore('?')
    return host.startsWith("www.", ignoreCase = true) ||
        (host.contains('.') && host.any(Char::isLetter))
}

private fun android.content.pm.PackageInfo.longVersionCodeCompat(): Long {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        longVersionCode
    } else {
        @Suppress("DEPRECATION")
        versionCode.toLong()
    }
}
