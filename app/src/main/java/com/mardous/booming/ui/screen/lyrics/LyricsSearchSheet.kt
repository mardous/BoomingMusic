/*
 * Copyright (c) 2026 Christians Martínez Alvarado
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.mardous.booming.ui.screen.lyrics

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mardous.booming.R
import com.mardous.booming.data.model.Song
import com.mardous.booming.data.model.lyrics.RawLyrics
import com.mardous.booming.data.remote.lyrics.LyricsProviderSearchResult
import com.mardous.booming.data.remote.lyrics.LyricsProviderSearchStatus
import com.mardous.booming.data.remote.lyrics.api.LyricsProvider

@Composable
fun LyricsSearchOverlay(
    song: Song,
    state: LyricsSearchUiState,
    onShowProviders: () -> Unit,
    onShowResults: () -> Unit,
    onCollapse: () -> Unit,
    onUseResult: (LyricsProvider) -> Unit,
    onManualSearch: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (state.songId != song.id) return

    var renderedStage by remember(state.songId) {
        mutableStateOf(state.sheetStage)
    }
    LaunchedEffect(state.sheetStage) {
        if (state.sheetStage != LyricsSearchSheetStage.Hidden) {
            renderedStage = state.sheetStage
        }
    }

    AnimatedVisibility(
        visible = state.sheetStage != LyricsSearchSheetStage.Hidden,
        enter = slideInVertically(tween(350)) { it / 2 } + fadeIn(tween(250)),
        exit = slideOutVertically(tween(300)) { it / 2 } + fadeOut(tween(200)),
        modifier = modifier
    ) {
        when (renderedStage) {
            LyricsSearchSheetStage.Hidden -> Unit
            LyricsSearchSheetStage.SourceChip -> SourceChip(
                result = state.activeResult,
                onClick = onShowResults
            )
            LyricsSearchSheetStage.Docked -> DockedLyricsSearchStrip(
                state = state,
                onDetailsClick = onShowProviders,
                onExpand = onShowProviders
            )
            LyricsSearchSheetStage.Providers -> ProviderListSheet(
                state = state,
                onExpand = onShowResults,
                onCollapse = onCollapse,
                onManualSearch = onManualSearch,
                onDismiss = onDismiss
            )
            LyricsSearchSheetStage.Results -> ResultsSheet(
                state = state,
                onShowProviders = onShowProviders,
                onCollapse = onCollapse,
                onUseResult = onUseResult
            )
        }
    }
}

@Composable
private fun SourceChip(
    result: LyricsProviderSearchResult?,
    onClick: () -> Unit
) {
    if (result == null) return
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        tonalElevation = 2.dp,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_lyrics_24dp),
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "${result.provider.displayName} · ${lyricsTypeLabel(result.lyrics)}",
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
private fun DockedLyricsSearchStrip(
    state: LyricsSearchUiState,
    onDetailsClick: () -> Unit,
    onExpand: () -> Unit
) {
    SheetSurface(
        stage = LyricsSearchSheetStage.Docked,
        state = state,
        onExpand = onExpand,
        onCollapse = {}
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp, bottom = 14.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_check_24dp),
                    contentDescription = null,
                    modifier = Modifier.padding(10.dp).size(22.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.lyrics_search_found_better),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(
                        R.string.lyrics_search_progress,
                        state.completedProviderCount,
                        state.providerResults.size
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = onDetailsClick) {
                Text(stringResource(R.string.lyrics_search_details))
            }
        }
        SearchProgress(state, Modifier.padding(horizontal = 16.dp))
        Spacer(Modifier.height(14.dp))
    }
}

@Composable
private fun ProviderListSheet(
    state: LyricsSearchUiState,
    onExpand: () -> Unit,
    onCollapse: () -> Unit,
    onManualSearch: () -> Unit,
    onDismiss: () -> Unit
) {
    SheetSurface(
        stage = LyricsSearchSheetStage.Providers,
        state = state,
        onExpand = onExpand,
        onCollapse = onCollapse
    ) {
        SheetHeader(
            title = if (state.isRunning) {
                stringResource(R.string.lyrics_search_finding)
            } else if (state.hasUsableResult) {
                stringResource(R.string.lyrics_search_results)
            } else {
                stringResource(R.string.lyrics_search_no_results_title)
            },
            subtitle = if (state.isRunning) {
                stringResource(
                    R.string.lyrics_search_progress,
                    state.completedProviderCount,
                    state.providerResults.size
                )
            } else {
                stringResource(R.string.lyrics_search_complete)
            },
            action = when {
                state.hasUsableResult -> stringResource(R.string.lyrics_search_collapse)
                !state.isRunning -> stringResource(R.string.close_action)
                else -> null
            },
            onAction = if (!state.isRunning && !state.hasUsableResult) onDismiss else onCollapse
        )

        SearchProgress(state, Modifier.padding(horizontal = 20.dp))
        Spacer(Modifier.height(8.dp))
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 380.dp)
        ) {
            items(state.providerResults, key = { it.provider.name }) { result ->
                ProviderStatusRow(
                    result = result,
                    deEmphasizeErrors = state.hasUsableResult
                )
                if (result != state.providerResults.lastOrNull()) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }

        if (!state.isRunning && !state.hasUsableResult) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = stringResource(R.string.lyrics_search_no_results_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = onManualSearch) {
                    Text(stringResource(R.string.lyrics_search_manual_search))
                }
            }
        } else {
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ResultsSheet(
    state: LyricsSearchUiState,
    onShowProviders: () -> Unit,
    onCollapse: () -> Unit,
    onUseResult: (LyricsProvider) -> Unit
) {
    val recommended = state.recommendedResult ?: return
    val otherResults = state.usableResults.filterNot { it.provider == recommended.provider }
    val unavailableResults = state.providerResults.filterNot { it.isUsable }
    var showUnavailable by rememberSaveable { mutableStateOf(false) }

    SheetSurface(
        stage = LyricsSearchSheetStage.Results,
        state = state,
        onExpand = {},
        onCollapse = onShowProviders,
        modifier = Modifier.fillMaxHeight(0.9f)
    ) {
        SheetHeader(
            title = stringResource(R.string.lyrics_search_results),
            subtitle = if (state.isRunning) {
                stringResource(
                    R.string.lyrics_search_progress,
                    state.completedProviderCount,
                    state.providerResults.size
                )
            } else {
                stringResource(R.string.lyrics_search_complete)
            },
            action = stringResource(R.string.lyrics_search_collapse),
            onAction = onCollapse
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 20.dp)
        ) {
            item {
                SectionTitle(stringResource(R.string.lyrics_search_recommended))
                RecommendedResult(
                    result = recommended,
                    onUse = { onUseResult(recommended.provider) }
                )
            }

            if (otherResults.isNotEmpty()) {
                item { SectionTitle(stringResource(R.string.lyrics_search_other_results)) }
                items(otherResults, key = { it.provider.name }) { result ->
                    ResultRow(result = result, onUse = { onUseResult(result.provider) })
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                }
            }

            if (unavailableResults.isNotEmpty()) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { showUnavailable = !showUnavailable }
                            .padding(vertical = 12.dp)
                    ) {
                        Icon(
                            painter = painterResource(
                                if (showUnavailable) R.drawable.ic_keyboard_arrow_up_24dp
                                else R.drawable.ic_keyboard_arrow_down_24dp
                            ),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(
                                R.string.lyrics_search_nothing_found,
                                unavailableResults.size
                            ),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (showUnavailable) {
                    items(unavailableResults, key = { it.provider.name }) { result ->
                        ProviderStatusRow(result = result, deEmphasizeErrors = true)
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun RecommendedResult(
    result: LyricsProviderSearchResult,
    onUse: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = result.provider.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = lyricsTypeLabel(result.lyrics),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = stringResource(R.string.lyrics_search_best_available),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.height(14.dp))
            Text(
                text = lyricsPreview(result.lyrics),
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = recommendationReason(result.lyrics),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(14.dp))
            Button(onClick = onUse, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.lyrics_search_use_lyrics))
            }
        }
    }
}

@Composable
private fun ResultRow(
    result: LyricsProviderSearchResult,
    onUse: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_lyrics_outline_24dp),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(result.provider.displayName, style = MaterialTheme.typography.titleSmall)
            Text(
                lyricsTypeLabel(result.lyrics),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        TextButton(onClick = onUse) {
            Text(stringResource(R.string.lyrics_search_use))
        }
    }
}

@Composable
private fun ProviderStatusRow(
    result: LyricsProviderSearchResult,
    deEmphasizeErrors: Boolean
) {
    val statusColor = when (result.status) {
        LyricsProviderSearchStatus.Found,
        LyricsProviderSearchStatus.Searching -> MaterialTheme.colorScheme.primary
        LyricsProviderSearchStatus.Failed,
        LyricsProviderSearchStatus.TimedOut -> if (deEmphasizeErrors) {
            MaterialTheme.colorScheme.onSurfaceVariant
        } else {
            MaterialTheme.colorScheme.error
        }
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 13.dp)
    ) {
        ProviderStatusIcon(result.status, statusColor)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(result.provider.displayName, style = MaterialTheme.typography.titleSmall)
            Text(
                text = providerStatusLabel(result),
                style = MaterialTheme.typography.bodySmall,
                color = statusColor
            )
        }
    }
}

@Composable
private fun ProviderStatusIcon(status: LyricsProviderSearchStatus, tint: Color) {
    if (status == LyricsProviderSearchStatus.Searching) {
        CircularProgressIndicator(
            color = tint,
            strokeWidth = 2.dp,
            modifier = Modifier.size(24.dp)
        )
        return
    }
    val icon = when (status) {
        LyricsProviderSearchStatus.Found -> R.drawable.ic_check_24dp
        LyricsProviderSearchStatus.NoMatch -> R.drawable.ic_close_24dp
        LyricsProviderSearchStatus.Failed,
        LyricsProviderSearchStatus.TimedOut -> R.drawable.ic_error_24dp
        LyricsProviderSearchStatus.Waiting -> R.drawable.ic_history_24dp
        LyricsProviderSearchStatus.Searching -> return
    }
    Icon(
        painter = painterResource(icon),
        contentDescription = null,
        tint = tint,
        modifier = Modifier.size(24.dp)
    )
}

@Composable
private fun SheetHeader(
    title: String,
    subtitle: String,
    action: String?,
    onAction: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 8.dp, bottom = 12.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineSmall)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (action != null) {
            TextButton(onClick = onAction) { Text(action) }
        }
    }
}

@Composable
private fun SearchProgress(state: LyricsSearchUiState, modifier: Modifier = Modifier) {
    val total = state.providerResults.size.coerceAtLeast(1)
    val progress = state.completedProviderCount.toFloat() / total
    val description = stringResource(
        R.string.lyrics_search_progress,
        state.completedProviderCount,
        state.providerResults.size
    )
    LinearProgressIndicator(
        progress = { progress },
        color = MaterialTheme.colorScheme.primary,
        trackColor = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier
            .fillMaxWidth()
            .height(5.dp)
            .clip(RoundedCornerShape(50))
            .semantics {
                stateDescription = description
                progressBarRangeInfo = ProgressBarRangeInfo(
                    current = progress,
                    range = 0f..1f,
                    steps = (total - 1).coerceAtLeast(0)
                )
            }
    )
}

@Composable
private fun SheetSurface(
    stage: LyricsSearchSheetStage,
    state: LyricsSearchUiState,
    onExpand: () -> Unit,
    onCollapse: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val handleExpands = stage != LyricsSearchSheetStage.Results
    val handleAction = if (handleExpands) onExpand else onCollapse
    val handleDescription = stringResource(
        if (handleExpands) R.string.lyrics_search_expand_results
        else R.string.lyrics_search_collapse_results
    )
    Surface(
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 3.dp,
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(tween(350))
            .lyricsSheetDragGestures(stage, state.hasUsableResult, onExpand, onCollapse)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .align(Alignment.CenterHorizontally)
                    .clickable(onClick = handleAction)
                    .clearAndSetSemantics {
                        contentDescription = handleDescription
                        role = Role.Button
                        onClick {
                            handleAction()
                            true
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.size(width = 42.dp, height = 4.dp)
                ) {}
            }
            content()
        }
    }
}

private fun Modifier.lyricsSheetDragGestures(
    stage: LyricsSearchSheetStage,
    hasUsableResult: Boolean,
    onExpand: () -> Unit,
    onCollapse: () -> Unit
): Modifier = pointerInput(stage, hasUsableResult) {
    var dragDistance = 0f
    val threshold = 40.dp.toPx()
    detectVerticalDragGestures(
        onDragStart = { dragDistance = 0f },
        onDragEnd = {
            when {
                dragDistance < -threshold -> onExpand()
                dragDistance > threshold &&
                    (hasUsableResult || stage == LyricsSearchSheetStage.Results) -> onCollapse()
            }
            dragDistance = 0f
        },
        onDragCancel = { dragDistance = 0f }
    ) { change, dragAmount ->
        change.consume()
        dragDistance += dragAmount
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
    )
}

@Composable
private fun providerStatusLabel(result: LyricsProviderSearchResult): String {
    return when (result.status) {
        LyricsProviderSearchStatus.Searching -> stringResource(R.string.lyrics_search_status_searching)
        LyricsProviderSearchStatus.Waiting -> stringResource(R.string.lyrics_search_status_waiting)
        LyricsProviderSearchStatus.NoMatch -> stringResource(R.string.lyrics_search_status_no_match)
        LyricsProviderSearchStatus.Failed -> stringResource(R.string.lyrics_search_status_failed)
        LyricsProviderSearchStatus.TimedOut -> stringResource(R.string.lyrics_search_status_timed_out)
        LyricsProviderSearchStatus.Found -> {
            "${stringResource(R.string.lyrics_search_status_found)} · ${lyricsTypeLabel(result.lyrics)}"
        }
    }
}

@Composable
private fun lyricsTypeLabel(lyrics: RawLyrics.Remote?): String = when {
    lyrics?.hasBoth == true -> stringResource(R.string.lyrics_search_plain_and_synced)
    lyrics?.hasSynced == true -> stringResource(R.string.lyrics_search_line_synced)
    else -> stringResource(R.string.plain_lyrics)
}

@Composable
private fun recommendationReason(lyrics: RawLyrics.Remote?): String = when {
    lyrics?.hasBoth == true -> stringResource(R.string.lyrics_search_most_complete_reason)
    lyrics?.hasSynced == true -> stringResource(R.string.lyrics_search_synced_reason)
    else -> stringResource(R.string.lyrics_search_plain_reason)
}

private fun lyricsPreview(lyrics: RawLyrics.Remote?): String {
    return lyrics?.lyrics.orEmpty()
        .replace(Regex("<[^>]+>"), " ")
        .replace(Regex("\\[[^]]+]"), " ")
        .lineSequence()
        .map { line -> line.trim() }
        .filter { line -> line.isNotEmpty() }
        .take(2)
        .joinToString("\n")
}
