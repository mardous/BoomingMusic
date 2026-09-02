package com.mardous.booming.ui.component.compose

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.mardous.booming.R
import com.mardous.booming.ui.component.views.TopAppBarLayout.AppBarMode as AppHeaderMode

val LocalHeaderMode = compositionLocalOf { AppHeaderMode.COLLAPSING }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun rememberTopAppBarScrollBehavior(
    headerMode: AppHeaderMode = LocalHeaderMode.current
): TopAppBarScrollBehavior {
    return if (headerMode == AppHeaderMode.COLLAPSING) {
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    } else {
        TopAppBarDefaults.enterAlwaysScrollBehavior()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollapsibleAppBarScaffold(
    title: String?,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    showNavigationButton: Boolean = true,
    navigationButton: @Composable () -> Unit = {
        TopAppBarFilledIconButton(
            onClick = onBackClick,
            icon = painterResource(R.drawable.ic_back_24dp),
            contentDescription = stringResource(R.string.back_action)
        )
    },
    headerMode: AppHeaderMode = LocalHeaderMode.current,
    miniPlayerMargin: Int = 0,
    multiSelectState: MultiSelectState<*>? = null,
    scrollBehavior: TopAppBarScrollBehavior? = rememberTopAppBarScrollBehavior(headerMode),
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .then(
                if (scrollBehavior != null) {
                    Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
                } else Modifier
            ),
        topBar = {
            if (multiSelectState?.hasSelectedItems == true) {
                BoomingMusicTopAppBar(
                    title = { TopAppBarTitle(multiSelectState.selectionTitle) },
                    actions = actions,
                    navigationButton = {
                        if (showNavigationButton) {
                            navigationButton()
                        }
                    },
                    scrollBehavior = scrollBehavior,
                    headerMode = headerMode
                )
            } else {
                BoomingMusicTopAppBar(
                    title = { TopAppBarTitle(title.orEmpty()) },
                    actions = actions,
                    navigationButton = {
                        if (showNavigationButton) {
                            navigationButton()
                        }
                    },
                    scrollBehavior = scrollBehavior,
                    headerMode = headerMode
                )
            }
        },
        snackbarHost = snackbarHost,
        contentWindowInsets = WindowInsets.safeDrawing
            .only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal)
            .add(WindowInsets(bottom = miniPlayerMargin))
    ) { contentPadding ->
        content(contentPadding)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BoomingMusicTopAppBar(
    title: @Composable () -> Unit,
    actions: @Composable RowScope.() -> Unit,
    modifier: Modifier = Modifier,
    navigationButton: @Composable () -> Unit = {},
    headerMode: AppHeaderMode = LocalHeaderMode.current,
    colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors(),
    scrollBehavior: TopAppBarScrollBehavior? = rememberTopAppBarScrollBehavior(headerMode),
    contentPadding: PaddingValues = TopAppBarDefaults.ContentPadding
) {
    when (headerMode) {
        AppHeaderMode.SIMPLE -> {
            TopAppBar(
                title = title,
                navigationIcon = navigationButton,
                actions = actions,
                colors = colors,
                scrollBehavior = scrollBehavior,
                contentPadding = contentPadding,
                modifier = modifier
            )
        }
        AppHeaderMode.COLLAPSING -> {
            LargeFlexibleTopAppBar(
                title = title,
                navigationIcon = navigationButton,
                actions = actions,
                colors = colors,
                scrollBehavior = scrollBehavior,
                modifier = modifier
            )
        }
    }
}

@Composable
fun TopAppBarTitle(
    text: String?,
    modifier: Modifier = Modifier
) {
    Text(
        text = text.orEmpty(),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

@Composable
fun TopAppBarFilledIconButton(
    onClick: () -> Unit,
    icon: Painter,
    modifier: Modifier = Modifier,
    contentDescription: String? = null
) {
    FilledIconButton(
        onClick = onClick,
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = LocalContentColor.current.copy(alpha = .1f)
        ),
        modifier = modifier
    ) {
        Icon(
            painter = icon,
            contentDescription = contentDescription
        )
    }
}