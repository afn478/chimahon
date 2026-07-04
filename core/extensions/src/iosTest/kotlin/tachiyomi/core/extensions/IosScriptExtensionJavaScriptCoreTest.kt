package tachiyomi.core.extensions

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import tachiyomi.core.platform.javascript.IosJavaScriptRuntimeFactory
import kotlin.test.Test
import kotlin.test.assertEquals

class IosScriptExtensionJavaScriptCoreTest {
    private val loader = ScriptExtensionLoader(IosJavaScriptRuntimeFactory)
    private val invoker = ScriptExtensionInvoker(IosJavaScriptRuntimeFactory)

    @Test
    fun loadsAndInvokesScriptExtensionsWithJavaScriptCore() = runBlocking {
        val extension = loader.load(TEST_EXTENSION)
        val arguments = buildJsonObject {
            put("page", 4)
        }

        assertEquals("org.chimahon.ios", extension.manifest.id)
        assertEquals(77L, extension.manifest.sources.single().id)

        val request = invoker.invoke(
            extension = extension,
            sourceId = 77,
            method = "popularMangaRequest",
            arguments = arguments,
            deserializer = ScriptHttpRequest.serializer(),
        )
        assertEquals("/popular?page=4", request.url)

        val page = invoker.invoke(
            extension = extension,
            sourceId = 77,
            method = "popularMangaParse",
            arguments = arguments,
            deserializer = ScriptMangasPage.serializer(),
        )
        assertEquals("iOS Example 4", page.mangas.single().title)
        assertEquals("/manga/4", page.mangas.single().url)
        assertEquals(true, page.hasNextPage)
    }

    private companion object {
        val TEST_EXTENSION = """
            module.exports = {
                manifest: {
                    id: "org.chimahon.ios",
                    name: "iOS JavaScriptCore",
                    version: "1.0.0",
                    sources: [{
                        id: 77,
                        name: "iOS Example",
                        language: "en",
                        baseUrl: "https://ios.example"
                    }]
                },
                sources: {
                    "77": {
                        popularMangaRequest: function (args) {
                            return { url: "/popular?page=" + args.page };
                        },
                        popularMangaParse: function (args) {
                            return {
                                mangas: [{
                                    url: "/manga/" + args.page,
                                    title: "iOS Example " + args.page
                                }],
                                hasNextPage: true
                            };
                        }
                    }
                }
            };
        """.trimIndent()
    }
}
