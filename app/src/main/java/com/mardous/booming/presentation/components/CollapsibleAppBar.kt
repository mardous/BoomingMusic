/*
 * Copyright (c) 2026 Christians Martínez Alvarado
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.mardous.booming.presentation.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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