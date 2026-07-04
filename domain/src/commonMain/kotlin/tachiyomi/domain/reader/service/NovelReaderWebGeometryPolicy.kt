package tachiyomi.domain.reader.service

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object NovelReaderWebGeometryPolicy {
    private const val MINIMUM_SCALE = 0.1

    data class Point(
        val x: Double,
        val y: Double,
    )

    data class Bounds(
        val x: Double,
        val y: Double,
        val width: Double,
        val height: Double,
    )

    fun cssPointToViewportPoint(
        x: Double,
        y: Double,
        scale: Double,
    ): Point {
        val normalizedScale = normalizedScale(scale)
        return Point(
            x = x * normalizedScale,
            y = y * normalizedScale,
        )
    }

    fun viewportPointToCssPoint(
        x: Double,
        y: Double,
        scale: Double,
    ): Point {
        val normalizedScale = normalizedScale(scale)
        return Point(
            x = x / normalizedScale,
            y = y / normalizedScale,
        )
    }

    fun cssBoundsToScreenBounds(
        x: Double,
        y: Double,
        width: Double,
        height: Double,
        viewportLeft: Double,
        viewportTop: Double,
        scale: Double,
    ): Bounds {
        val normalizedScale = normalizedScale(scale)
        return Bounds(
            x = x * normalizedScale + viewportLeft,
            y = y * normalizedScale + viewportTop,
            width = width * normalizedScale,
            height = height * normalizedScale,
        )
    }

    fun selectionRectsToScreenJson(
        json: String?,
        viewportLeft: Double,
        viewportTop: Double,
        scale: Double,
    ): String {
        val source = json ?: "[]"
        return runCatching {
            val rects = Json.parseToJsonElement(source).jsonArray
            JsonArray(
                rects.map { rect ->
                    val rectObject = rect.jsonObject
                    val bounds = cssBoundsToScreenBounds(
                        x = rectObject.requiredDouble("x"),
                        y = rectObject.requiredDouble("y"),
                        width = rectObject.requiredDouble("width"),
                        height = rectObject.requiredDouble("height"),
                        viewportLeft = viewportLeft,
                        viewportTop = viewportTop,
                        scale = scale,
                    )
                    JsonObject(
                        rectObject.toMutableMap().apply {
                            put("x", JsonPrimitive(bounds.x))
                            put("y", JsonPrimitive(bounds.y))
                            put("width", JsonPrimitive(bounds.width))
                            put("height", JsonPrimitive(bounds.height))
                        },
                    )
                },
            ).toString()
        }.getOrElse {
            source
        }
    }

    private fun normalizedScale(scale: Double): Double {
        return if (scale.isFinite()) {
            scale.coerceAtLeast(MINIMUM_SCALE)
        } else {
            MINIMUM_SCALE
        }
    }

    private fun JsonObject.requiredDouble(key: String): Double {
        return requireNotNull(this[key]?.jsonPrimitive?.doubleOrNull) {
            "Missing numeric selection rect field: $key"
        }
    }
}
