package com.canopus.chimareader.ui.reader

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import tachiyomi.domain.reader.service.NovelReaderChapterListSheetPolicy

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChapterListSheet(
    viewModel: ReaderViewModel,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    val chapters = viewModel.document.linearSpineItems
    val toc = viewModel.getFlattenedToc()
    val rows = if (NovelReaderChapterListSheetPolicy.hasTableOfContents(toc)) {
        NovelReaderChapterListSheetPolicy.tocRows(
            toc = toc,
            currentSpineIndex = viewModel.index,
            characterCountForSpineIndex = { viewModel.accumulatedCharCounts[it] },
            spineIndexForHref = { viewModel.getSpineIndexForHref(it) },
        )
    } else {
        NovelReaderChapterListSheetPolicy.fallbackRows(
            chapterCount = chapters.size,
            currentSpineIndex = viewModel.index,
            titleForSpineIndex = { viewModel.getChapterTitle(it) },
            hrefForSpineIndex = { viewModel.document.getChapterHref(it) },
            characterCountForSpineIndex = { viewModel.accumulatedCharCounts[it] },
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(modifier = Modifier.padding(bottom = 32.dp)) {
            Text(
                text = NovelReaderChapterListSheetPolicy.TITLE,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
            )

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
            ) {
                items(rows) { row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.jumpToChapter(row.spineIndex, row.fragment)
                                onDismiss()
                            }
                            .padding(
                                start = 24.dp + (row.depth * 12).dp,
                                top = 16.dp,
                                end = 24.dp,
                                bottom = 16.dp,
                            ),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = row.title,
                            style = if (row.isCurrent) {
                                MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                            } else {
                                MaterialTheme.typography.bodyLarge
                            },
                            color = if (row.isCurrent) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = row.characterCountLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
