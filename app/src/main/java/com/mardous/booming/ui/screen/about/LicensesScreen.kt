package com.mardous.booming.ui.screen.about

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.mardous.booming.R
import com.mardous.booming.ui.component.compose.CollapsibleAppBarScaffold
import com.mikepenz.aboutlibraries.ui.compose.android.produceLibraries
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OSSLicensesScreen(
    onBackClick: () -> Unit
) {
    val libraries by produceLibraries(R.raw.aboutlibraries)
    CollapsibleAppBarScaffold(
        title = stringResource(R.string.licenses),
        onBackClick = onBackClick
    ) { paddingValues ->
        LibrariesContainer(
            libraries = libraries,
            modifier = Modifier.fillMaxSize(),
            contentPadding = paddingValues,
            showDescription = true,
            licenseDialogConfirmText = stringResource(R.string.close_action)
        )
    }
}