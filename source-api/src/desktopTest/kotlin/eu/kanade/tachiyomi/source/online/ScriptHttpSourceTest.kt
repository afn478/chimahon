package eu.kanade.tachiyomi.source.online

import com.sun.net.httpserver.HttpServer
import eu.kanade.tachiyomi.source.model.Page
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tachiyomi.core.extensions.ScriptExtensionInvoker
import tachiyomi.core.extensions.ScriptExtensionLoader
import tachiyomi.core.platform.javascript.DesktopJavaScriptRuntimeFactory
import java.net.InetSocketAddress

class ScriptHttpSourceTest {
    @Test
    fun fetchesAndParsesPopularMangaThroughJavaScript() {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/popular") { exchange ->
            val response = "Manga from HTTP"
            exchange.sendResponseHeaders(200, response.toByteArray().size.toLong())
            exchange.responseBody.use { body ->
                body.write(response.toByteArray())
            }
        }
        server.start()

        try {
            val baseUrl = "http://127.0.0.1:${server.address.port}"
            val source = runBlocking {
                val loader = ScriptExtensionLoader(DesktopJavaScriptRuntimeFactory)
                val extension = loader.load(script(baseUrl))
                ScriptSourceFactory(
                    extension = extension,
                    invoker = ScriptExtensionInvoker(DesktopJavaScriptRuntimeFactory),
                ).createSources().single() as ScriptHttpSource
            }

            val page = runBlocking {
                source.getPopularManga(1)
            }

            assertEquals("Manga from HTTP", page.mangas.single().title)
            assertEquals("/manga/from-http", page.mangas.single().url)
            assertEquals(false, page.hasNextPage)
        } finally {
            server.stop(0)
        }
    }

    @Test
    fun fetchesImagesThroughJavaScriptRequestDescriptor() {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/image") { exchange ->
            if (exchange.requestHeaders.getFirst("X-Script-Image") != "allowed") {
                exchange.sendResponseHeaders(403, -1)
                exchange.close()
                return@createContext
            }

            val response = byteArrayOf(1, 3, 5, 7)
            exchange.sendResponseHeaders(200, response.size.toLong())
            exchange.responseBody.use { body ->
                body.write(response)
            }
        }
        server.start()

        try {
            val baseUrl = "http://127.0.0.1:${server.address.port}"
            val source = runBlocking {
                val loader = ScriptExtensionLoader(DesktopJavaScriptRuntimeFactory)
                val extension = loader.load(imageScript(baseUrl))
                ScriptSourceFactory(
                    extension = extension,
                    invoker = ScriptExtensionInvoker(DesktopJavaScriptRuntimeFactory),
                ).createSources().single() as ScriptHttpSource
            }

            val bytes = runBlocking {
                source.getImageBytes(Page(index = 0, imageUrl = "/image"))
            }

            assertArrayEquals(byteArrayOf(1, 3, 5, 7), bytes)
        } finally {
            server.stop(0)
        }
    }

    private fun script(baseUrl: String): String {
        return """
            module.exports = {
                manifest: {
                    id: "org.chimahon.http-test",
                    name: "HTTP Test",
                    version: "1.0.0",
                    sources: [{
                        id: 101,
                        name: "HTTP Source",
                        language: "en",
                        baseUrl: "$baseUrl"
                    }]
                },
                sources: {
                    "101": {
                        popularMangaRequest: function (args) {
                            return { url: "/popular?page=" + args.page };
                        },
                        popularMangaParse: function (input) {
                            return {
                                mangas: [{
                                    url: "/manga/from-http",
                                    title: input.response.body
                                }],
                                hasNextPage: false
                            };
                        }
                    }
                }
            };
        """.trimIndent()
    }

    private fun imageScript(baseUrl: String): String {
        return """
            module.exports = {
                manifest: {
                    id: "org.chimahon.image-test",
                    name: "Image Test",
                    version: "1.0.0",
                    sources: [{
                        id: 102,
                        name: "Image Source",
                        language: "en",
                        baseUrl: "$baseUrl"
                    }]
                },
                sources: {
                    "102": {
                        imageRequest: function (input) {
                            return {
                                url: input.page.imageUrl,
                                headers: {
                                    "X-Script-Image": "allowed"
                                }
                            };
                        }
                    }
                }
            };
        """.trimIndent()
    }
}
