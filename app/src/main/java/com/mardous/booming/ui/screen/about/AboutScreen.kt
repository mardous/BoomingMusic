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

import android.content.ClipData
import android.content.Intent
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil3.compose.AsyncImage
import com.mardous.booming.R
import com.mardous.booming.core.model.about.Contribution
import com.mardous.booming.core.model.about.DeviceInfo
import com.mardous.booming.extensions.*
import com.mardous.booming.ui.component.compose.ActionButton
import com.mardous.booming.ui.component.compose.CollapsibleAppBarScaffold
import dev.jeziellago.compose.markdowntext.MarkdownText
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

private const val AUTHOR_GITHUB_URL = "https://www.github.com/mardous"
private const val GITHUB_URL = "$AUTHOR_GITHUB_URL/BoomingMusic"
private const val RELEASES_LINK = "$GITHUB_URL/releases"
const val ISSUE_TRACKER_LINK = "$GITHUB_URL/issues"
private const val AUTHOR_TELEGRAM_LINK = "https://t.me/mardeez"
private const val COMMUNITY_LINK = "https://github.com/mardous/BoomingMusic/wiki/Community"
private const val FAQ_LINK = "https://github.com/mardous/BoomingMusic/wiki/FAQ"
private const val APP_TELEGRAM_LINK = "https://t.me/mardousdev"
private const val CROWDIN_PROJECT_LINK = "https://crowdin.com/project/booming-music"
private const val DONATE_LINK = "https://ko-fi.com/christiaam"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    viewModel: AboutViewModel = koinViewModel(),
    onBackClick: () -> Unit,
    onNavigateToId: (Int) -> Unit
) {
    val clipboard = LocalClipboard.current
    val context = LocalContext.current

    var showReportDialog by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    if (showReportDialog) {
        val deviceInfo = DeviceInfo()
        val clipLabel = stringResource(R.string.device_info)

        ReportBugsDialog(
            onDismiss = { showReportDialog = false },
            onContinue = {
                showReportDialog = false
                coroutineScope.launch {
                    clipboard.setClipEntry(
                        ClipData.newPlainText(clipLabel, deviceInfo.toMarkdown()).toClipEntry()
                    )
                    context.showToast(R.string.copied_device_info_to_clipboard, Toast.LENGTH_LONG)
                    context.openUrl(ISSUE_TRACKER_LINK)
                }
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
                onTelegramClick = {
                    context.openUrl(AUTHOR_TELEGRAM_LINK)
                },
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
                    context.openUrl(CROWDIN_PROJECT_LINK)
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
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = version,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
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
                label = stringResource(R.string.fork_on_github),
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
    onTelegramClick: () -> Unit = {},
    onGitHubClick: () -> Unit = {},
    onEmailClick: () -> Unit = {},
    onDonateClick: () -> Unit = {}
) {
    AboutSection(title = stringResource(R.string.author)) {
        AboutCard {
            AboutListItem(
                iconRes = R.drawable.ic_person_24dp,
                title = stringResource(R.string.mardous),
                summary = stringResource(R.string.mardous_summary)
            )
            AboutListItem(
                iconRes = R.drawable.ic_coffee_24dp,
                title = stringResource(R.string.buy_me_a_coffee),
                summary = stringResource(R.string.buy_me_a_coffee_summary),
                onClick = onDonateClick
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .wrapContentSize()
                    .padding(vertical = 8.dp)
            ) {
                IconButton(
                    onClick = onTelegramClick,
                    modifier = Modifier.wrapContentSize()
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_telegram_24dp),
                        contentDescription = stringResource(R.string.follow_on_telegram)
                    )
                }

                IconButton(
                    onClick = onGitHubClick,
                    modifier = Modifier.wrapContentSize()
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_github_circle_24dp),
                        contentDescription = stringResource(R.string.fork_on_github)
                    )
                }

                IconButton(
                    onClick = onEmailClick,
                    modifier = Modifier.wrapContentSize()
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_email_24dp),
                        contentDescription = stringResource(R.string.write_an_email)
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
                title = stringResource(R.string.telegram_chat),
                summary = stringResource(R.string.telegram_chat_summary),
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