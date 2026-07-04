package tachiyomi.domain.reader.service

object NovelEpubImagePolicy {
    const val MAX_DIMENSION = 2048
    const val MAX_PIXELS = 4_000_000L

    fun shouldDownsample(
        width: Int,
        height: Int,
    ): Boolean {
        if (width <= 0 || height <= 0) return false
        return width > MAX_DIMENSION ||
            height > MAX_DIMENSION ||
            width.toLong() * height > MAX_PIXELS
    }

    fun sampleSize(
        width: Int,
        height: Int,
    ): Int {
        if (!shouldDownsample(width = width, height = height)) return 1

        var sampleSize = 1
        var sampledWidth = width
        var sampledHeight = height
        while (sampledWidth > MAX_DIMENSION ||
            sampledHeight > MAX_DIMENSION ||
            sampledWidth.toLong() * sampledHeight > MAX_PIXELS
        ) {
            sampleSize *= 2
            sampledWidth = width / sampleSize
            sampledHeight = height / sampleSize
        }
        return sampleSize
    }

    fun mimeMightHaveAlpha(mimeType: String?): Boolean {
        return mimeType.orEmpty().let {
            it.contains("png", ignoreCase = true) ||
                it.contains("gif", ignoreCase = true) ||
                it.contains("webp", ignoreCase = true)
        }
    }
}
