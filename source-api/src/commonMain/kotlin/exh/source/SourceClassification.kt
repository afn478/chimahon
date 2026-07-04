@file:Suppress("PropertyName")

package exh.source

import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.online.all.EhBasedSource

// Used to speed up isLewdSource
var metadataDelegatedSourceIds: List<Long> = emptyList()

var nHentaiSourceIds: List<Long> = emptyList()

var lanraragiSourceIds: List<Long> = emptyList()

var mangaDexSourceIds: List<Long> = emptyList()

var LIBRARY_UPDATE_EXCLUDED_SOURCES = listOf(
    EH_SOURCE_ID,
    EXH_SOURCE_ID,
    PURURIN_SOURCE_ID,
)

// This method MUST be fast!
fun isMetadataSource(source: Long) = source in 6900..6999 ||
    // KMK -->
    source == EH_SOURCE_ID ||
    source == EXH_SOURCE_ID ||
    // KMK <--
    metadataDelegatedSourceIds.binarySearch(source) >= 0

fun isEhBasedSourceId(sourceId: Long) = sourceId in eHentaiSourceIds

// KMK -->
fun Source.isEhBasedSource() = this is EhBasedSource && isEhBasedSourceId(id)
// KMK <--

fun isMdBasedSourceId(sourceId: Long) = sourceId in mangaDexSourceIds

fun Source.isMdBasedSource() = isMdBasedSourceId(id)
