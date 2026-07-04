package eu.kanade.tachiyomi.ui.browse.source

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.core.preference.asState
import eu.kanade.domain.source.interactor.GetEnabledSources
import eu.kanade.domain.source.interactor.GetShowLatest
import eu.kanade.domain.source.interactor.GetSourceCategories
import eu.kanade.domain.source.interactor.SetSourceCategories
import eu.kanade.domain.source.interactor.ToggleExcludeFromDataSaver
import eu.kanade.domain.source.interactor.ToggleSource
import eu.kanade.domain.source.interactor.ToggleSourcePin
import eu.kanade.domain.source.model.installedExtension
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.domain.source.service.SourcePreferences.DataSaver
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.presentation.browse.SourceUiModel
import eu.kanade.presentation.components.SEARCH_DEBOUNCE_MILLIS
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.source.model.Source
import tachiyomi.domain.source.service.SourceListFilterMetadata
import tachiyomi.domain.source.service.SourceListFilterPolicy
import tachiyomi.domain.source.service.SourceListGroupingPolicy
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class SourcesScreenModel(
    private val getEnabledSources: GetEnabledSources = Injekt.get(),
    private val toggleSource: ToggleSource = Injekt.get(),
    private val toggleSourcePin: ToggleSourcePin = Injekt.get(),
    // SY -->
    private val uiPreferences: UiPreferences = Injekt.get(),
    private val getSourceCategories: GetSourceCategories = Injekt.get(),
    private val getShowLatest: GetShowLatest = Injekt.get(),
    private val toggleExcludeFromDataSaver: ToggleExcludeFromDataSaver = Injekt.get(),
    private val setSourceCategories: SetSourceCategories = Injekt.get(),
    private val sourcePreferences: SourcePreferences = Injekt.get(),
    val smartSearchConfig: SourcesScreen.SmartSearchConfig?,
    // SY <--
) : StateScreenModel<SourcesScreenModel.State>(State()) {

    private val _events = Channel<Event>(Int.MAX_VALUE)
    val events = _events.receiveAsFlow()

    val useNewSourceNavigation by uiPreferences.useNewSourceNavigation().asState(screenModelScope)

    init {
        // SY -->
        combine(
            // KMK -->
            state.map { Pair(it.searchQuery, it.nsfwOnly) }
                .distinctUntilChanged().debounce(SEARCH_DEBOUNCE_MILLIS),
            // KMK <--
            getEnabledSources.subscribe(),
            getSourceCategories.subscribe(),
            getShowLatest.subscribe(smartSearchConfig != null),
            flowOf(smartSearchConfig == null),
            ::collectLatestSources,
        )
            .catch {
                logcat(LogPriority.ERROR, it)
                _events.send(Event.FailedFetchingSources)
            }
            .flowOn(Dispatchers.IO)
            .launchIn(screenModelScope)

        sourcePreferences.dataSaver().changes()
            .onEach {
                mutableState.update {
                    it.copy(
                        dataSaverEnabled = sourcePreferences.dataSaver().get() != DataSaver.NONE,
                    )
                }
            }
            .launchIn(screenModelScope)
        // SY <--
    }

    private fun collectLatestSources(
        // KMK -->
        filters: Pair<String?, Boolean>,
        unfilteredSources: List<Source>,
        // sources: List<Source>,
        // KMK <--
        categories: List<String>,
        showLatest: Boolean,
        showPin: Boolean,
    ) {
        // KMK -->
        val sources = SourceListFilterPolicy.filterSources(
            sources = unfilteredSources,
            searchQuery = filters.first,
            nsfwOnly = filters.second,
            metadata = { source ->
                val extension = source.installedExtension
                SourceListFilterMetadata(
                    extensionName = extension?.name,
                    extensionIsNsfw = extension?.isNsfw,
                )
            },
        )
        // KMK <--
        val sourceGroups = SourceListGroupingPolicy.groupSources(sources)
        mutableState.update { state ->
            state.copy(
                isLoading = false,
                items = sourceGroups
                    .flatMap { group ->
                        listOf(
                            SourceUiModel.Header(
                                group.header,
                                group.isCategory,
                            ),
                            *group.sources.map { source ->
                                SourceUiModel.Item(source)
                            }.toTypedArray(),
                        )
                    }
                    .toImmutableList(),
                // SY -->
                categories = categories
                    .toImmutableList(),
                showPin = showPin,
                showLatest = showLatest,
                // SY <--
            )
        }
    }

    fun toggleSource(source: Source) {
        toggleSource.await(source)
    }

    fun togglePin(source: Source) {
        toggleSourcePin.await(source)
    }

    // SY -->
    fun toggleExcludeFromDataSaver(source: Source) {
        toggleExcludeFromDataSaver.await(source)
    }

    fun setSourceCategories(source: Source, categories: List<String>) {
        setSourceCategories.await(source, categories)
    }

    fun showSourceCategoriesDialog(source: Source) {
        mutableState.update { it.copy(dialog = Dialog.SourceCategories(source)) }
    }
    // SY <--

    fun showSourceDialog(source: Source) {
        mutableState.update { it.copy(dialog = Dialog.SourceLongClick(source)) }
    }

    fun closeDialog() {
        mutableState.update { it.copy(dialog = null) }
    }

    // KMK -->
    fun search(query: String?) {
        mutableState.update {
            it.copy(searchQuery = query)
        }
    }

    fun toggleNsfwOnly() {
        mutableState.update {
            it.copy(nsfwOnly = !it.nsfwOnly)
        }
    }
    // KMK <--

    sealed interface Event {
        data object FailedFetchingSources : Event
    }

    sealed class Dialog {
        data class SourceLongClick(val source: Source) : Dialog()
        data class SourceCategories(val source: Source) : Dialog()
    }

    @Immutable
    data class State(
        val dialog: Dialog? = null,
        val isLoading: Boolean = true,
        val items: ImmutableList<SourceUiModel> = persistentListOf(),
        // SY -->
        val categories: ImmutableList<String> = persistentListOf(),
        val showPin: Boolean = true,
        val showLatest: Boolean = false,
        val dataSaverEnabled: Boolean = false,
        // SY <--
        // KMK -->
        val searchQuery: String? = null,
        val nsfwOnly: Boolean = false,
        // KMK <--
    ) {
        val isEmpty = items.isEmpty()
    }

}
