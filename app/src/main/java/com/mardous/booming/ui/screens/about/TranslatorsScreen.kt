/*
 * Copyright (c) 2025 Christians Martínez Alvarado
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

package com.mardous.booming.ui.screens.about

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.mardous.booming.R
import com.mardous.booming.extensions.openUrl
import com.mardous.booming.ui.components.lists.ContributionListItem
import com.mardous.booming.viewmodels.about.AboutViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslatorsScreen(
    viewModel: AboutViewModel = koinViewModel(),
    onBackButtonClick: () -> Unit
) {
    val context = LocalContext.current

    val translators by viewModel.translators.collectAsState()
    LaunchedEffect(Unit) {
        viewModel.loadTranslators()
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MediumTopAppBar(
                title = { Text(text = stringResource(R.string.translators_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackButtonClick) {
                        Icon(
                            painter = painterResource(R.drawable.ic_back_24dp),
                            contentDescription = null
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing
            .only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal)
    ) { innerPadding ->
        LazyColumn(contentPadding = innerPadding) {
            items(translators) { contribution ->
                ContributionListItem(
                    contribution,
                    onClick = {
                        contribution.url?.let {
                            context.openUrl(it)
                        }
                    }
                )
            }
        }
    }
}