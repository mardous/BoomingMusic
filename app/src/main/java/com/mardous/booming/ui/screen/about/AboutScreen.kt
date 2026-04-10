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

package com.mardous.booming.ui.screen.about

import android.content.Intent
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil3.compose.AsyncImage
import com.mardous.booming.R
import com.mardous.booming.core.model.about.Contribution
import com.mardous.booming.extensions.MIME_TYPE_PLAIN_TEXT
import com.mardous.booming.extensions.openUrl
import com.mardous.booming.extensions.toChooser
import com.mardous.booming.extensions.tryStartActivity
import com.mardous.booming.ui.component.compose.ActionButton
import com.mardous.booming.ui.component.compose.CollapsibleAppBarScaffold
import com.mardous.booming.ui.component.compose.ShapedText
import dev.jeziellago.compose.markdowntext.MarkdownText
import org.koin.androidx.compose.koinViewModel

private const val AUTHOR_GITHUB_URL = "https://www.github.com/mardous"
private const val GITHUB_URL = "$AUTHOR_GITHUB_URL/BoomingMusic"
private const val RELEASES_LINK = "$GITHUB_URL/releases"
const val ISSUE_TRACKER_LINK = "$GITHUB_URL/issues"
private const val COMMUNITY_LINK = "$GITHUB_URL/wiki/Community"
private const val FAQ_LINK = "$GITHUB_URL/wiki/FAQ"
private const val APP_TELEGRAM_LINK = "https://t.me/mardousdev"
private const val TRANSLATIONS_LINK = "https://hosted.weblate.org/engage/booming-music/"
private const val DONATE_LINK = "https://ko-fi.com/christiaam"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    viewModel: AboutViewModel = koinViewModel(),
    onBackClick: () -> Unit,
    onNavigateToId: (Int) -> Unit
) {
    val context = LocalContext.current

    var showReportDialog by remember { mutableStateOf(false) }
    if (showReportDialog) {
        ReportBugsDialog(
            onDismiss = { showReportDialog = false },
            onContinue = {
                showReportDialog = false
                context.openUrl(ISSUE_TRACKER_LINK)
            }
        )
    }

    val contributors by viewModel.contributors.collectAsState()
    LaunchedEffect(Unit) {
        viewModel.loadContributors()
    }

    val sendInvitationTitle = stringResource(R.string.send_invitation_message)
    val invitationMessage = stringResource(R.string.invitation_message_content, RELEASES_LINK)

    CollapsibleAppBarScaffold(
        title = stringResource(R.string.about_title),
        onBackClick = onBackClick
    ) { contentPadding ->
        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(contentPadding)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AboutHeader(
                version = viewModel.appVersion,
                onChangelogClick = {
                    context.openUrl(RELEASES_LINK)
                },
                onForkClick = {
                    context.openUrl(GITHUB_URL)
                },
                onLicensesClick = {
                    onNavigateToId(R.id.nav_licenses)
                },
                onFAQClick = {
                    context.openUrl(FAQ_LINK)
                }
            )

            AboutAuthorSection(
                onGitHubClick = {
                    context.openUrl(AUTHOR_GITHUB_URL)
                },
                onEmailClick = {
                    context.tryStartActivity(
                        Intent(Intent.ACTION_SENDTO)
                            .setData("mailto:".toUri())
                            .putExtra(Intent.EXTRA_EMAIL, arrayOf("mardous.contact@gmail.com"))
                            .putExtra(Intent.EXTRA_SUBJECT, "Booming Music - Support & questions")
                    )
                },
                onDonateClick = {
                    context.openUrl(DONATE_LINK)
                }
            )

            AboutContributorSection(
                contributors = contributors,
                onClick = {
                    if (it.url != null) {
                        context.openUrl(it.url)
                    }
                }
            )

            AboutAcknowledgmentSection(
                onTranslatorsClick = {
                    onNavigateToId(R.id.nav_translators)
                },
                onContributorsClick = {
                    context.openUrl(COMMUNITY_LINK)
                }
            )

            AboutSupportSection(
                onTranslateClick = {
                    context.openUrl(TRANSLATIONS_LINK)
                },
                onReportBugsClick = {
                    showReportDialog = true
                },
                onShareAppClick = {
                    context.tryStartActivity(
                        Intent(Intent.ACTION_SEND)
                            .putExtra(Intent.EXTRA_TEXT, invitationMessage)
                            .setType(MIME_TYPE_PLAIN_TEXT)
                            .toChooser(sendInvitationTitle)
                    )
                },
                onJoinChatClick = {
                    context.openUrl(APP_TELEGRAM_LINK)
                }
            )
        }
    }
}

@Composable
private fun AboutHeader(
    version: String,
    onChangelogClick: () -> Unit,
    onForkClick: () -> Unit,
    onLicensesClick: () -> Unit,
    onFAQClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 8.dp, start = 16.dp, end = 16.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.icon_web),
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                contentScale = ContentScale.Inside
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.app_name_long),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(4.dp))
            ShapedText(
                text = version,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            ActionButton(
                icon = R.drawable.ic_history_24dp,
                label = stringResource(R.string.changelog),
                modifier = Modifier.weight(1f),
                onClick = onChangelogClick
            )

            ActionButton(
                icon = R.drawable.ic_help_24dp,
                label = stringResource(R.string.faq),
                modifier = Modifier.weight(1f),
                onClick = onFAQClick
            )

            ActionButton(
                icon = R.drawable.ic_github_circle_24dp,
                label = stringResource(R.string.github),
                modifier = Modifier.weight(1f),
                onClick = onForkClick
            )

            ActionButton(
                icon = R.drawable.ic_description_24dp,
                label = stringResource(R.string.licenses),
                modifier = Modifier.weight(1f),
                onClick = onLicensesClick
            )
        }
    }
}

@Preview
@Composable
private fun AboutAuthorSection(
    onGitHubClick: () -> Unit = {},
    onEmailClick: () -> Unit = {},
    onDonateClick: () -> Unit = {}
) {
    AboutSection(title = stringResource(R.string.author)) {
        AboutCard {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 16.dp, bottom = 8.dp)
            ) {
                AsyncImage(
                    model = "file:///android_asset/images/mardous.png".toUri(),
                    contentDescription = "Lead Dev's image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.mardous),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = stringResource(R.string.mardous_summary),
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .wrapContentSize()
                    .padding(8.dp)
            ) {
                Button(
                    onClick = onDonateClick,
                    modifier = Modifier.wrapContentSize()
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_volunteer_activism_24dp),
                        contentDescription = null
                    )
                    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                    Text(stringResource(R.string.support_my_work))
                }

                IconButton(
                    onClick = onGitHubClick,
                    modifier = Modifier.wrapContentSize()
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_github_circle_24dp),
                        contentDescription = "GitHub profile"
                    )
                }

                IconButton(
                    onClick = onEmailClick,
                    modifier = Modifier.wrapContentSize()
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_email_24dp),
                        contentDescription = "Write an email"
                    )
                }
            }
        }
    }
}

@Composable
private fun AboutContributorSection(
    contributors: List<Contribution>,
    onClick: (Contribution) -> Unit
) {
    AboutSection(title = stringResource(R.string.contributors)) {
        AboutCard {
            contributors.forEach {
                ContributionListItem(contribution = it) {
                    onClick(it)
                }
            }
        }
    }
}

@Composable
private fun AboutSupportSection(
    onReportBugsClick: () -> Unit,
    onTranslateClick: () -> Unit,
    onJoinChatClick: () -> Unit,
    onShareAppClick: () -> Unit
) {
    AboutSection(title = stringResource(R.string.support_development)) {
        AboutCard {
            AboutListItem(
                iconRes = R.drawable.ic_bug_report_24dp,
                title = stringResource(R.string.report_bugs),
                summary = stringResource(R.string.report_bugs_summary),
                onClick = onReportBugsClick
            )
            AboutListItem(
                iconRes = R.drawable.ic_language_24dp,
                title = stringResource(R.string.help_with_translations),
                summary = stringResource(R.string.help_with_translations_summary),
                onClick = onTranslateClick
            )
            AboutListItem(
                iconRes = R.drawable.ic_telegram_24dp,
                title = stringResource(R.string.telegram_community),
                summary = stringResource(R.string.telegram_community_summary),
                onClick = onJoinChatClick
            )
            AboutListItem(
                iconRes = R.drawable.ic_share_24dp,
                title = stringResource(R.string.share_app),
                summary = stringResource(R.string.share_app_summary),
                onClick = onShareAppClick
            )
        }
    }
}

@Composable
private fun AboutAcknowledgmentSection(
    onTranslatorsClick: () -> Unit,
    onContributorsClick: () -> Unit
) {
    AboutSection(title = stringResource(R.string.acknowledgments_title)) {
        AboutCard {
            AboutListItem(
                iconRes = R.drawable.ic_translate_24dp,
                title = stringResource(R.string.translators_title),
                summary = stringResource(R.string.translators_summary),
                onClick = onTranslatorsClick
            )
            AboutListItem(
                iconRes = R.drawable.ic_groups_24dp,
                title = stringResource(R.string.contributors_title),
                summary = stringResource(R.string.contributors_summary),
                onClick = onContributorsClick
            )
        }
    }
}

@Composable
private fun ReportBugsDialog(
    onDismiss: () -> Unit = {},
    onContinue: () -> Unit = {}
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                painter = painterResource(R.drawable.ic_bug_report_24dp),
                contentDescription = null
            )
        },
        title = { Text(stringResource(R.string.report_bugs)) },
        text = {
            Text(text = stringResource(R.string.you_will_be_forwarded_to_the_issue_tracker_website))
        },
        confirmButton = {
            Button(onClick = onContinue) {
                Text(text = stringResource(R.string.continue_action))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(android.R.string.cancel))
            }
        }
    )
}

@Composable
private fun AboutSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 24.dp)
        )

        content()
    }
}

@Composable
private fun AboutCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp), content = content)
    }
}

@Composable
private fun AboutListItem(
    @DrawableRes iconRes: Int,
    title: String,
    modifier: Modifier = Modifier,
    summary: String? = null,
    summaryMaxLines: Int = 4,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null, onClick = { onClick?.invoke() })
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.wrapContentSize()
        )
        if (summary.isNullOrEmpty()) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        } else {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = summaryMaxLines,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun ContributionListItem(
    contribution: Contribution,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = !contribution.url.isNullOrEmpty()) { onClick() }
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (contribution.image != null) {
            AsyncImage(
                model = contribution.imageUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier
                .weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = contribution.name,
                style = MaterialTheme.typography.bodyLarge,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (!contribution.description.isNullOrBlank()) {
                MarkdownText(
                    markdown = contribution.description,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                )
            }
        }
    }
}