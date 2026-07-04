package mihon.domain.extensionrepo.service

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class ExtensionRepoDtoTest {
    @Test
    fun metadataJsonMapsToExtensionRepo() {
        val dto = Json.decodeFromString<ExtensionRepoMetaDto>(
            """
            {
                "meta": {
                    "name": "Komikku Extensions",
                    "shortName": "Komikku",
                    "website": "https://repo.example",
                    "signingKeyFingerprint": "fingerprint"
                }
            }
            """.trimIndent(),
        )

        val repo = dto.toExtensionRepo(baseUrl = "https://repo.example/extensions")

        assertEquals("https://repo.example/extensions", repo.baseUrl)
        assertEquals("Komikku Extensions", repo.name)
        assertEquals("Komikku", repo.shortName)
        assertEquals("https://repo.example", repo.website)
        assertEquals("fingerprint", repo.signingKeyFingerprint)
    }
}
