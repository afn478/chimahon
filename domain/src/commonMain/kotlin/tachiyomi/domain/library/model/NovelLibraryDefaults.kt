package tachiyomi.domain.library.model

import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

object NovelLibraryDefaults {
    @OptIn(ExperimentalUuidApi::class)
    fun newId(): String = Uuid.random().toString()

    fun nowMillis(): Long = Clock.System.now().toEpochMilliseconds()
}
