package com.canopus.chimareader.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.canopus.chimareader.data.epub.EpubParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tachiyomi.domain.library.service.NovelBookImportPolicy
import tachiyomi.domain.reader.service.NovelEpubContentPolicy
import tachiyomi.domain.reader.service.NovelEpubImagePolicy
import java.io.File
import java.util.zip.ZipFile

data class ImportResult(
    val metadata: BookMetadata? = null,
    val error: String? = null,
)

object BookImporter {

    private const val TAG = "BookImporter"

    suspend fun importEpub(
        context: Context,
        uri: Uri,
        categoryIds: List<String>? = null,
    ): ImportResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting import from URI: $uri")

            val tempFile = File(context.cacheDir, "import_${System.currentTimeMillis()}.epub")
            context.contentResolver.openInputStream(uri)?.use { input ->
                tempFile.outputStream().use { output -> input.copyTo(output) }
            } ?: return@withContext ImportResult(error = "Could not read file")

            Log.d(TAG, "Copied to temp: ${tempFile.absolutePath}, size: ${tempFile.length()}")

            try {
                ZipFile(tempFile).use { zip ->
                    val entries = zip.entries().asSequence().toList()
                    Log.d(TAG, "ZIP contains ${entries.size} entries")
                    if (entries.isEmpty()) {
                        return@withContext ImportResult(error = "File is empty")
                    }
                }
            } catch (e: Exception) {
                tempFile.delete()
                return@withContext ImportResult(error = "Not a valid EPUB file: ${e.message}")
            }

            // We'll calculate the stableId after parsing metadata to ensure parity with migration
            val booksDir = BookStorage.getBooksDirectory(context)
            Log.d(TAG, "Books directory: ${booksDir.absolutePath}")
            booksDir.mkdirs()

            val tempExtractDir = File(context.cacheDir, "temp_extract_${System.currentTimeMillis()}")
            tempExtractDir.mkdirs()

            ZipFile(tempFile).use { zip ->
                zip.entries().asSequence().forEach { entry ->
                    val file = File(tempExtractDir, entry.name)
                    if (entry.isDirectory) {
                        file.mkdirs()
                    } else {
                        file.parentFile?.mkdirs()
                        zip.getInputStream(entry).use { input ->
                            file.outputStream().use { output -> input.copyTo(output) }
                        }
                    }
                }
            }

            val extractedBook = EpubParser.parse(tempExtractDir)
            val title = NovelBookImportPolicy.importedTitle(extractedBook.title)
            val author = NovelBookImportPolicy.importedAuthor(extractedBook.author)
            val stableId = NovelBookImportPolicy.stableIdForImport(
                title = title,
                author = author,
            )
            val bookDir = File(booksDir, stableId)

            Log.d(TAG, "Stable ID (Title+Author): $stableId ($title | $author)")

            var existingBookmark: Bookmark? = null
            var existingStats: List<Statistics>? = null
            var existingMetadata: BookMetadata? = null

            // Move from temp to final destination
            if (bookDir.exists()) {
                existingBookmark = BookStorage.loadBookmark(bookDir)
                existingStats = BookStorage.loadStatistics(bookDir)
                existingMetadata = BookStorage.loadMetadata(bookDir)
                bookDir.deleteRecursively()
            }
            tempExtractDir.renameTo(bookDir)

            // Re-run normalisation and pre-wrapping on the final directory
            bookDir.walkTopDown().forEach { file ->
                val ext = file.extension.lowercase()
                if (NovelEpubContentPolicy.isImageExtension(ext)) {
                    normaliseImageInPlace(file)
                }
                if (NovelEpubContentPolicy.isMarkupExtension(ext)) {
                    preWrapBodyContent(file)
                }
                if (NovelEpubContentPolicy.isStylesheetExtension(ext)) {
                    cleanBookCss(file)
                }
            }

            tempFile.delete()

            Log.d(TAG, "Extracted to: ${bookDir.absolutePath}")

            val extractedFiles = bookDir.walkTopDown().take(20).map { it.relativeTo(bookDir).path }.toList()
            Log.d(TAG, "Extracted files (first 20): $extractedFiles")

            Log.d(TAG, "Finalized to: ${bookDir.absolutePath}")

            val coverAbsPath = extractedBook.coverPath?.let { File(bookDir, it).absolutePath }

            Log.d(TAG, "Parsed EPUB: title=$title, contentDir=${extractedBook.contentDirectory}, chapters=${extractedBook.spine.items.size}")

            val metadata = NovelBookImportPolicy.metadataForImportedBook(
                title = extractedBook.title,
                author = extractedBook.author,
                coverPath = coverAbsPath,
                language = extractedBook.language,
                existingMetadata = existingMetadata,
                requestedCategoryIds = categoryIds,
                importTimeMillis = System.currentTimeMillis(),
                uncategorizedCategoryId = NovelCategory.UNCATEGORIZED_ID,
            )
            BookStorage.saveMetadata(metadata, bookDir)

            existingBookmark?.let { BookStorage.saveBookmark(it, bookDir) }
            existingStats?.let { BookStorage.saveStatistics(it, bookDir) }

            return@withContext ImportResult(metadata = metadata)
        } catch (e: Exception) {
            Log.e(TAG, "Import failed", e)
            return@withContext ImportResult(error = "Import failed: ${e.message}")
        }
    }

    private fun normaliseImageInPlace(file: File) {
        try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, bounds)

            val w = bounds.outWidth
            val h = bounds.outHeight
            if (w <= 0 || h <= 0) {
                Log.w(TAG, "normalise: could not read bounds for ${file.name} — skipping")
                return
            }

            val needsDownsample = NovelEpubImagePolicy.shouldDownsample(
                width = w,
                height = h,
            )
            if (!needsDownsample) return // small enough — leave it alone

            val sampleSize = NovelEpubImagePolicy.sampleSize(
                width = w,
                height = h,
            )
            Log.w(TAG, "normalise: ${file.name} ${w}x$h → 1:$sampleSize sample")

            val mightHaveAlpha = NovelEpubImagePolicy.mimeMightHaveAlpha(bounds.outMimeType)

            val decodeOpts = BitmapFactory.Options().apply {
                inJustDecodeBounds = false
                inSampleSize = sampleSize
                inPreferredConfig = if (mightHaveAlpha) Bitmap.Config.ARGB_8888 else Bitmap.Config.RGB_565
            }
            val bitmap: Bitmap? = BitmapFactory.decodeFile(file.absolutePath, decodeOpts)
            if (bitmap == null) {
                Log.e(TAG, "normalise: decode returned null for ${file.name} — skipping")
                return
            }

            try {
                val hasAlpha = bitmap.hasAlpha() && mightHaveAlpha

                @Suppress("DEPRECATION")
                val format: Bitmap.CompressFormat
                val quality: Int
                if (hasAlpha) {
                    // Lossless WebP to preserve transparency
                    format = if (android.os.Build.VERSION.SDK_INT >= 30) {
                        Bitmap.CompressFormat.WEBP_LOSSLESS
                    } else {
                        Bitmap.CompressFormat.WEBP
                    }
                    quality = 100
                } else {
                    // Lossy WebP — smallest file, no transparency needed
                    format = if (android.os.Build.VERSION.SDK_INT >= 30) {
                        Bitmap.CompressFormat.WEBP_LOSSY
                    } else {
                        Bitmap.CompressFormat.WEBP
                    }
                    quality = 82
                }

                val tmp = File(file.parent, "${file.name}.norm.tmp")
                val ok = try {
                    tmp.outputStream().use { out -> bitmap.compress(format, quality, out) }
                } catch (e: Exception) {
                    Log.e(TAG, "normalise: compress failed for ${file.name}", e)
                    tmp.delete()
                    false
                }

                if (ok) {
                    if (!tmp.renameTo(file)) {
                        // renameTo can fail cross-device; fall back to copy + delete
                        tmp.inputStream().use { src -> file.outputStream().use { dst -> src.copyTo(dst) } }
                        tmp.delete()
                    }
                    Log.d(TAG, "normalise: ${file.name} compressed OK (alpha=$hasAlpha)")
                } else {
                    Log.e(TAG, "normalise: compress returned false for ${file.name} — keeping original")
                    tmp.delete()
                }
            } finally {
                bitmap.recycle()
            }
        } catch (e: OutOfMemoryError) {
            Log.e(TAG, "normalise: OOM on ${file.name} — keeping original", e)
        } catch (e: Exception) {
            Log.e(TAG, "normalise: unexpected error for ${file.name} — keeping original", e)
        }
    }

    private fun preWrapBodyContent(file: File) {
        try {
            val text = file.readText(Charsets.UTF_8)
            val result = NovelEpubContentPolicy.wrapBodyContent(text)
            if (result == text) return

            file.writeText(result, Charsets.UTF_8)
            Log.d(TAG, "preWrap: wrapped ${file.name} (${text.length} → ${result.length} bytes)")
        } catch (e: Exception) {
            Log.e(TAG, "preWrap: failed for ${file.name} — skipping", e)
        }
    }

    private fun cleanBookCss(file: File) {
        try {
            val text = file.readText(Charsets.UTF_8)
            if (text.isBlank()) return

            val cleanedText = NovelEpubContentPolicy.cleanCss(text)

            file.writeText(cleanedText, Charsets.UTF_8)
            Log.d(TAG, "cleanCss: cleaned ${file.name}")
        } catch (e: Exception) {
            Log.e(TAG, "cleanCss: failed for ${file.name} — skipping", e)
        }
    }

}
