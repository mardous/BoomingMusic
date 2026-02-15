package com.mardous.booming.ui.component.compose

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.mardous.booming.R
import com.mardous.booming.ui.component.views.TopAppBarLayout
import com.mardous.booming.util.Preferences

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollapsibleAppBarScaffold(
    title: String,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    showNavigationButton: Boolean = true,
    collapsibleAppBar: Boolean = Preferences.appBarMode == TopAppBarLayout.AppBarMode.COLLAPSING,
    miniPlayerMargin: Int = 0,
    onBackClick: () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    val scrollBehavior = if (collapsibleAppBar) {
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    } else {
        TopAppBarDefaults.enterAlwaysScrollBehavior()
    }
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            if (collapsibleAppBar) {
                MediumTopAppBar(
                    title = { Text(text = title) },
                    navigationIcon = {
                        if (showNavigationButton) {
                            IconButton(onClick = onBackClick,) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_back_24dp),
                                    contentDescription = stringResource(R.string.back_action)
                                )
                            }
                        }
                    },
                    actions = actions,
                    scrollBehavior = scrollBehavior
                )
            } else {
                TopAppBar(
                    title = { Text(text = title) },
                    navigationIcon = {
                        if (showNavigationButton) {
                            IconButton(onClick = onBackClick,) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_back_24dp),
                                    contentDescription = stringResource(R.string.back_action)
                                )
                            }
                        }
                    },
                    actions = actions,
                    scrollBehavior = scrollBehavior
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