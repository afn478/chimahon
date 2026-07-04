package eu.kanade.tachiyomi.ui.browse.migration.manga

import androidx.compose.runtime.Immutable
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.tachiyomi.source.Source
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.manga.interactor.GetFavorites
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.selection.service.ListSelectionPolicy
import tachiyomi.domain.selection.service.ListSelectionState
import tachiyomi.domain.selection.service.SelectableListItem
import tachiyomi.domain.source.service.SourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class MigrateMangaScreenModel(
    private val sourceId: Long,
    private val sourceManager: SourceManager = Injekt.get(),
    private val getFavorites: GetFavorites = Injekt.get(),
) : StateScreenModel<MigrateMangaScreenModel.State>(State()) {

    private val _events: Channel<MigrationMangaEvent> = Channel()
    val events: Flow<MigrationMangaEvent> = _events.receiveAsFlow()

    // KMK -->
    private var selectionState = ListSelectionState<Long>()
    // KMK <--

    init {
        screenModelScope.launch {
            mutableState.update { state ->
                state.copy(source = sourceManager.getOrStub(sourceId))
            }

            getFavorites.subscribe(sourceId)
                .catch {
                    logcat(LogPriority.ERROR, it)
                    _events.send(MigrationMangaEvent.FailedFetchingFavorites)
                    mutableState.update { state ->
                        state.copy(titleList = persistentListOf())
                    }
                }
                // KMK -->
                .map { manga ->
                    toMigrationMangaScreenItems(manga)
                }
                // KMK <--
                .map { manga ->
                    manga
                        .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.manga.title })
                        .toImmutableList()
                }
                .collectLatest { list ->
                    mutableState.update { it.copy(titleList = list) }
                }
        }
    }

    // KMK -->
    private fun toMigrationMangaScreenItems(mangas: List<Manga>): List<MigrateMangaItem> {
        return mangas.map { manga ->
            MigrateMangaItem(
                manga = manga,
                selected = manga.id in selectionState.selectedIds,
            )
        }
    }

    fun toggleSelection(
        item: MigrateMangaItem,
        selected: Boolean,
        userSelected: Boolean = false,
        fromLongPress: Boolean = false,
    ) {
        mutableState.update { state ->
            val result = ListSelectionPolicy.toggleSelection(
                items = state.titles.toSelectableListItems(),
                state = selectionState,
                targetId = item.manga.id,
                selected = selected,
                userSelected = userSelected,
                fromLongPress = fromLongPress,
            )
            selectionState = result.state
            state.copy(titleList = state.titles.withSelection(result.items).toImmutableList())
        }
    }

    fun toggleAllSelection(selected: Boolean = true) {
        mutableState.update { state ->
            val result = ListSelectionPolicy.toggleAllSelection(
                items = state.titles.toSelectableListItems(),
                state = selectionState,
                selected = selected,
            )
            selectionState = result.state
            state.copy(titleList = state.titles.withSelection(result.items).toImmutableList())
        }
    }

    fun invertSelection() {
        mutableState.update { state ->
            val result = ListSelectionPolicy.invertSelection(
                items = state.titles.toSelectableListItems(),
                state = selectionState,
            )
            selectionState = result.state
            state.copy(titleList = state.titles.withSelection(result.items).toImmutableList())
        }
    }

    private fun List<MigrateMangaItem>.toSelectableListItems(): List<SelectableListItem<Long>> {
        return map { item ->
            SelectableListItem(
                id = item.manga.id,
                selected = item.selected,
            )
        }
    }

    private fun List<MigrateMangaItem>.withSelection(
        selectedItems: List<SelectableListItem<Long>>,
    ): List<MigrateMangaItem> {
        return mapIndexed { index, item ->
            item.copy(selected = selectedItems[index].selected)
        }
    }
    // KMK <--

    fun clearSelection() {
        // KMK -->
        toggleAllSelection(false)
        // KMK <--
    }

    @Immutable
    data class State(
        val source: Source? = null,
        private val titleList: ImmutableList<MigrateMangaItem>? = null,
    ) {
        // KMK -->
        val selection = titles.filter { it.selected }
        // KMK <--

        val titles: ImmutableList<MigrateMangaItem>
            get() = titleList ?: persistentListOf()

        val isLoading: Boolean
            get() = source == null || titleList == null

        val isEmpty: Boolean
            get() = titles.isEmpty()

        val selectionMode = selection.isNotEmpty()
    }
}

sealed interface MigrationMangaEvent {
    data object FailedFetchingFavorites : MigrationMangaEvent
}

// KMK -->
@Immutable
data class MigrateMangaItem(
    val manga: Manga,
    val selected: Boolean,
)
// KMK <--
