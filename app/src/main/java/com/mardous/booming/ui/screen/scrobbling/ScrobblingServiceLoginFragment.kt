package com.mardous.booming.ui.screen.scrobbling

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.mardous.booming.R
import com.mardous.booming.data.model.network.LoginParams
import com.mardous.booming.data.model.network.LoginState
import com.mardous.booming.data.model.network.ScrobblingService
import com.mardous.booming.data.model.network.canLogIn
import com.mardous.booming.data.model.network.isFailure
import com.mardous.booming.data.model.network.isLoading
import com.mardous.booming.extensions.extraNotNull
import com.mardous.booming.extensions.openUrl
import com.mardous.booming.extensions.withArgs
import com.mardous.booming.ui.component.compose.BottomSheetDialogSurface
import com.mardous.booming.ui.component.compose.TipView
import com.mardous.booming.ui.screen.library.LibraryViewModel
import com.mardous.booming.ui.theme.BoomingMusicTheme
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class ScrobblingServiceLoginFragment : BottomSheetDialogFragment() {

    private val viewModel: LibraryViewModel by activityViewModel()
    private val service: ScrobblingService by extraNotNull("service")

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
            )
            setContent {
                BoomingMusicTheme {
                    LoginScreen(service, viewModel)
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
    @Composable
    private fun LoginScreen(
        service: ScrobblingService,
        viewModel: LibraryViewModel
    ) {
        val uiState by viewModel.getLoginState(service).collectAsState(null)

        val serviceName = when (service) {
            ScrobblingService.Lastfm -> stringResource(R.string.lastfm_title)
            ScrobblingService.ListenBrainz -> stringResource(R.string.listenbrainz_title)
        }

        DisposableEffect(Unit) {
            onDispose {
                if (uiState?.isFailure == true) {
                    viewModel.logoutFromService(service)
                }
            }
        }

        BottomSheetDialogSurface {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .nestedScroll(rememberNestedScrollInteropConnection())
            ) {
                BottomSheetDefaults.DragHandle(
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Text(
                    text = stringResource(R.string.sign_in_to_x_title, serviceName),
                    style = MaterialTheme.typography.headlineSmallEmphasized,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )

                Crossfade(uiState) { loginState ->
                    when (loginState) {
                        is LoginState.Empty,
                        is LoginState.LoggingIn,
                        is LoginState.Failure -> {
                            when (service) {
                                ScrobblingService.Lastfm -> {
                                    LastFmForm(
                                        loginState = loginState,
                                        onLoginClick = { params ->
                                            viewModel.logInToService(service, params)
                                        }
                                    )
                                }

                                ScrobblingService.ListenBrainz -> {
                                    ListenBrainzForm(
                                        loginState = loginState,
                                        onLoginClick = { params ->
                                            viewModel.logInToService(service, params)
                                        }
                                    )
                                }
                            }

                        }

                        is LoginState.LoggedIn -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier
                                    .padding(horizontal = 24.dp, vertical = 16.dp)
                            ) {
                                ConnectedUserCard(
                                    loggedInState = loginState,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Button(onClick = { viewModel.logoutFromService(service) }) {
                                    Text(stringResource(R.string.logout_action))
                                }
                            }
                        }

                        else -> {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 48.dp)
                            ) {
                                CircularWavyProgressIndicator()
                            }
                        }
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    @Composable
    private fun LastFmForm(
        loginState: LoginState,
        onLoginClick: (LoginParams) -> Unit,
        modifier: Modifier = Modifier
    ) {
        var username by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var passwordVisible by remember { mutableStateOf(false) }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 8.dp, bottom = 24.dp)
        ) {
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text(stringResource(R.string.lastfm_login_username)) },
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_person_24dp),
                        contentDescription = null
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = loginState.canLogIn,
                shape = MaterialTheme.shapes.medium
            )

            Spacer(modifier = Modifier.height(4.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(stringResource(R.string.lastfm_login_password)) },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_lock_24dp),
                        contentDescription = null
                    )
                },
                trailingIcon = {
                    val icon = if (passwordVisible) {
                        painterResource(R.drawable.ic_visibility_off_24dp)
                    } else {
                        painterResource(R.drawable.ic_visibility_24dp)
                    }
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            painter = icon,
                            contentDescription = stringResource(R.string.lastfm_login_toggle_password_visibility)
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = loginState.canLogIn,
                shape = MaterialTheme.shapes.medium
            )

            Spacer(modifier = Modifier.height(16.dp))

            AnimatedVisibility(
                visible = loginState.isLoading
            ) {
                LinearWavyProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                )
            }

            ErrorMessage(loginState, R.string.error_lastfm_generic)

            Button(
                onClick = {
                    onLoginClick(LoginParams.Lastfm(username, password))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = MaterialTheme.shapes.extraLarge,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                enabled = username.isNotBlank() && password.isNotBlank() && loginState.canLogIn,
            ) {
                Text(
                    text = stringResource(R.string.login_action),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    @Composable
    private fun ListenBrainzForm(
        loginState: LoginState,
        onLoginClick: (LoginParams) -> Unit,
        modifier: Modifier = Modifier
    ) {
        var token by remember { mutableStateOf("") }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 8.dp, bottom = 24.dp)
        ) {
            Text(
                text = stringResource(R.string.listenbrainz_login_summary),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )

            OutlinedTextField(
                value = token,
                onValueChange = { token = it },
                label = { Text(stringResource(R.string.listenbrainz_token_label)) },
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_lock_24dp),
                        contentDescription = null
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = loginState.canLogIn,
                shape = MaterialTheme.shapes.medium
            )

            Spacer(modifier = Modifier.height(16.dp))

            AnimatedVisibility(
                visible = loginState.isLoading
            ) {
                LinearWavyProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                )
            }

            ErrorMessage(loginState, R.string.error_listenbrainz_generic)

            Button(
                onClick = {
                    onLoginClick(LoginParams.ListenBrainz(token))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = MaterialTheme.shapes.extraLarge,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                enabled = token.isNotBlank() && loginState.canLogIn,
            ) {
                Text(
                    text = stringResource(R.string.login_action),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }

    @Composable
    private fun ErrorMessage(
        loginState: LoginState,
        genericMessageRes: Int,
    ) {
        AnimatedVisibility(
            visible = loginState.isFailure
        ) {
            if (loginState is LoginState.Failure) {
                TipView(
                    text = loginState.message ?: stringResource(genericMessageRes),
                    icon = painterResource(R.drawable.ic_error_24dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                )
            }
        }
    }

    @Composable
    private fun ConnectedUserCard(
        loggedInState: LoginState.LoggedIn,
        modifier: Modifier = Modifier
    ) {
        val context = LocalContext.current

        ElevatedCard(
            onClick = {
                if (!loggedInState.url.isNullOrEmpty()) {
                    context.openUrl(loggedInState.url)
                }
            },
            modifier = modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_person_24dp),
                    contentDescription = "User Icon",
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = stringResource(R.string.connected_account_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = loggedInState.username,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Icon(
                    painter = painterResource(R.drawable.ic_open_in_new_24dp),
                    contentDescription = "Open in browser",
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.outline
                )
            }
        }
    }

    companion object {
        fun create(service: ScrobblingService) =
            ScrobblingServiceLoginFragment().withArgs {
                putSerializable("service", service)
            }
    }
}
