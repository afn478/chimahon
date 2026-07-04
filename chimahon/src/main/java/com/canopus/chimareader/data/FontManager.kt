package com.canopus.chimareader.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tachiyomi.domain.reader.service.NovelReaderFontPolicy
import java.io.File

object FontManager {
    val defaultFonts = NovelReaderFontPolicy.defaultFonts

    fun isCustomFont(context: Context, fontName: String): Boolean {
        return NovelReaderFontPolicy.isCustomFont(
            fontName = fontName,
            importedFontNames = getImportedFonts(context),
        )
    }

    fun getFontUri(context: Context, fontName: String): String? {
        val file = getFontFile(context, fontName) ?: return null
        return NovelReaderFontPolicy.fontFileUri(file.absolutePath)
    }

    private fun getFontsDir(context: Context): File {
        val dir = File(context.filesDir, NovelReaderFontPolicy.FONTS_DIRECTORY)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    suspend fun importFont(context: Context, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val fileName = getFileName(context, uri)
                ?: NovelReaderFontPolicy.importedFontFileName(System.currentTimeMillis())
            val targetFile = File(getFontsDir(context), fileName)

            context.contentResolver.openInputStream(uri)?.use { input ->
                targetFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun getImportedFonts(context: Context): List<String> {
        val dir = getFontsDir(context)
        return dir.listFiles()
            ?.let { files -> NovelReaderFontPolicy.customFontNames(files.map { it.name }) }
            ?: emptyList()
    }

    fun getFontFile(context: Context, fontName: String): File? {
        if (NovelReaderFontPolicy.isDefaultFont(fontName)) return null

        val dir = getFontsDir(context)
        return dir.listFiles()
            ?.find { NovelReaderFontPolicy.matchesImportedFontFile(fileName = it.name, fontName = fontName) }
    }

    fun deleteFont(context: Context, fontName: String) {
        getFontFile(context, fontName)?.delete()
    }

    private fun getFileName(context: Context, uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex("_display_name")
                    if (index >= 0) {
                        result = cursor.getString(index)
                    }
                }
            }
        }
        if (result == null) {
            result = uri.path?.let { File(it).name }
        }
        return result
    }
}
