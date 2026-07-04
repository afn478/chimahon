package tachiyomi.domain.reader.service

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class NovelReaderWebGeometryPolicyTest {
    @Test
    fun cssPointToViewportPointAppliesScale() {
        val point = NovelReaderWebGeometryPolicy.cssPointToViewportPoint(
            x = 12.0,
            y = 8.0,
            scale = 2.5,
        )

        assertEquals(30.0, point.x, absoluteTolerance = 0.0001)
        assertEquals(20.0, point.y, absoluteTolerance = 0.0001)
    }

    @Test
    fun viewportPointToCssPointAppliesInverseScale() {
        val point = NovelReaderWebGeometryPolicy.viewportPointToCssPoint(
            x = 30.0,
            y = 20.0,
            scale = 2.5,
        )

        assertEquals(12.0, point.x, absoluteTolerance = 0.0001)
        assertEquals(8.0, point.y, absoluteTolerance = 0.0001)
    }

    @Test
    fun coordinateConversionsClampInvalidScales() {
        val point = NovelReaderWebGeometryPolicy.cssPointToViewportPoint(
            x = 12.0,
            y = 8.0,
            scale = 0.0,
        )

        assertEquals(1.2, point.x, absoluteTolerance = 0.0001)
        assertEquals(0.8, point.y, absoluteTolerance = 0.0001)
    }

    @Test
    fun cssBoundsToScreenBoundsAppliesScaleAndViewportOffset() {
        val bounds = NovelReaderWebGeometryPolicy.cssBoundsToScreenBounds(
            x = 5.0,
            y = 10.0,
            width = 20.0,
            height = 30.0,
            viewportLeft = 100.0,
            viewportTop = 200.0,
            scale = 2.0,
        )

        assertEquals(110.0, bounds.x, absoluteTolerance = 0.0001)
        assertEquals(220.0, bounds.y, absoluteTolerance = 0.0001)
        assertEquals(40.0, bounds.width, absoluteTolerance = 0.0001)
        assertEquals(60.0, bounds.height, absoluteTolerance = 0.0001)
    }

    @Test
    fun selectionRectsToScreenJsonTransformsRectCoordinatesAndPreservesMetadata() {
        val json = """
            [
                {"x":5.0,"y":10.0,"width":20.0,"height":30.0,"source":"selection"}
            ]
        """.trimIndent()

        val transformed = NovelReaderWebGeometryPolicy.selectionRectsToScreenJson(
            json = json,
            viewportLeft = 100.0,
            viewportTop = 200.0,
            scale = 2.0,
        )
        val rect = Json.parseToJsonElement(transformed).jsonArray[0].jsonObject

        assertEquals(110.0, rect.requiredDouble("x"), absoluteTolerance = 0.0001)
        assertEquals(220.0, rect.requiredDouble("y"), absoluteTolerance = 0.0001)
        assertEquals(40.0, rect.requiredDouble("width"), absoluteTolerance = 0.0001)
        assertEquals(60.0, rect.requiredDouble("height"), absoluteTolerance = 0.0001)
        assertEquals("selection", rect["source"]?.jsonPrimitive?.content)
    }

    @Test
    fun selectionRectsToScreenJsonUsesEmptyArrayForNullResults() {
        assertEquals(
            "[]",
            NovelReaderWebGeometryPolicy.selectionRectsToScreenJson(
                json = null,
                viewportLeft = 100.0,
                viewportTop = 200.0,
                scale = 2.0,
            ),
        )
    }

    @Test
    fun selectionRectsToScreenJsonFallsBackToOriginalJsonWhenMalformed() {
        val malformedJson = """[{"x":1.0}]"""

        assertEquals(
            malformedJson,
            NovelReaderWebGeometryPolicy.selectionRectsToScreenJson(
                json = malformedJson,
                viewportLeft = 100.0,
                viewportTop = 200.0,
                scale = 2.0,
            ),
        )
    }

    private fun Map<String, JsonElement>.requiredDouble(key: String): Double {
        return requireNotNull(this[key]?.jsonPrimitive?.double) {
            "Expected numeric JSON field: $key"
        }
    }
}
