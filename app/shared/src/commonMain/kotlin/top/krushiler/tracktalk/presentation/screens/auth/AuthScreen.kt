package top.krushiler.tracktalk.presentation.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import tracktalk.app.shared.generated.resources.Res
import tracktalk.app.shared.generated.resources.auth_check_inbox
import tracktalk.app.shared.generated.resources.auth_create_account
import tracktalk.app.shared.generated.resources.auth_create_account_button
import tracktalk.app.shared.generated.resources.auth_email_or_phone
import tracktalk.app.shared.generated.resources.auth_forgot_password
import tracktalk.app.shared.generated.resources.auth_full_name
import tracktalk.app.shared.generated.resources.auth_password
import tracktalk.app.shared.generated.resources.auth_reset_link_sent
import tracktalk.app.shared.generated.resources.auth_reset_password
import tracktalk.app.shared.generated.resources.auth_send_reset_link
import tracktalk.app.shared.generated.resources.auth_sign_in
import tracktalk.app.shared.generated.resources.auth_subtitle_forgot
import tracktalk.app.shared.generated.resources.auth_subtitle_login
import tracktalk.app.shared.generated.resources.auth_subtitle_signup
import tracktalk.app.shared.generated.resources.auth_welcome_back
import tracktalk.app.shared.generated.resources.back
import tracktalk.app.shared.generated.resources.coming_soon
import tracktalk.app.shared.generated.resources.download_app
import top.krushiler.tracktalk.platform.isWebTarget
import top.krushiler.tracktalk.platform.openExternalUrl
import top.krushiler.tracktalk.presentation.theme.AppColors
import top.krushiler.tracktalk.presentation.theme.AppIcons
import top.krushiler.tracktalk.presentation.theme.GradientButton
import top.krushiler.tracktalk.presentation.theme.LocalDownloadUrl
import top.krushiler.tracktalk.presentation.theme.appTextFieldColors

private enum class AuthMode { Login, Signup, Forgot }

@Composable
fun AuthRoute(
    onAuthenticated: () -> Unit,
    viewModel: AuthViewModel = koinViewModel<AuthViewModel>(),
) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(state.isAuthenticated) {
        if (state.isAuthenticated) onAuthenticated()
    }
    AuthScreen(
        state = state,
        onLoginChange = viewModel::onLoginChange,
        onPasswordChange = viewModel::onPasswordChange,
        onSubmit = viewModel::login,
    )
}

@Composable
fun AuthScreen(
    state: AuthUiState,
    onLoginChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    var mode by remember { mutableStateOf(AuthMode.Login) }
    var passwordVisible by remember { mutableStateOf(false) }
    var resetSent by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var signupRequest by remember { mutableStateOf(0) }
    val snackbar = remember { SnackbarHostState() }
    val comingSoon = stringResource(Res.string.coming_soon)
    val downloadUrl = LocalDownloadUrl.current
    Box(Modifier.fillMaxSize().background(AppColors.loginGradient)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (mode != AuthMode.Login) {
                TextButton(
                    onClick = {
                        mode = AuthMode.Login
                        resetSent = false
                    },
                    modifier = Modifier.align(Alignment.Start),
                ) {
                    Icon(AppIcons.chevronLeft, contentDescription = null, tint = AppColors.primary)
                    Text(stringResource(Res.string.back), color = AppColors.primary)
                }
            } else {
                Spacer(Modifier.height(48.dp))
            }
            Spacer(Modifier.height(24.dp))
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .shadow(16.dp, RoundedCornerShape(22.dp))
                    .clip(RoundedCornerShape(22.dp))
                    .background(AppColors.primaryGradient),
                contentAlignment = Alignment.Center,
            ) {
                Icon(AppIcons.locationOn, contentDescription = null, tint = Color.White, modifier = Modifier.size(34.dp))
            }
            Text(
                text = when (mode) {
                    AuthMode.Login -> stringResource(Res.string.auth_welcome_back)
                    AuthMode.Signup -> stringResource(Res.string.auth_create_account)
                    AuthMode.Forgot -> stringResource(Res.string.auth_reset_password)
                },
                modifier = Modifier.padding(top = 18.dp),
                style = MaterialTheme.typography.displaySmall,
                textAlign = TextAlign.Center,
            )
            Text(
                text = when (mode) {
                    AuthMode.Login -> stringResource(Res.string.auth_subtitle_login)
                    AuthMode.Signup -> stringResource(Res.string.auth_subtitle_signup)
                    AuthMode.Forgot -> stringResource(Res.string.auth_subtitle_forgot)
                },
                modifier = Modifier.padding(top = 8.dp, bottom = 32.dp),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
            if (mode == AuthMode.Forgot && resetSent) {
                Text("📬", style = MaterialTheme.typography.displaySmall)
                Text(
                    stringResource(Res.string.auth_check_inbox),
                    modifier = Modifier.padding(top = 16.dp),
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    stringResource(Res.string.auth_reset_link_sent, state.login),
                    modifier = Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
            } else {
                if (mode == AuthMode.Signup) {
                    AuthField(
                        value = name,
                        onValue = { name = it },
                        placeholder = stringResource(Res.string.auth_full_name),
                    )
                    Spacer(Modifier.height(14.dp))
                }
                AuthField(
                    value = state.login,
                    onValue = onLoginChange,
                    placeholder = stringResource(Res.string.auth_email_or_phone),
                    keyboardType = KeyboardType.Email,
                )
                if (mode != AuthMode.Forgot) {
                    Spacer(Modifier.height(14.dp))
                    AuthField(
                        value = state.password,
                        onValue = onPasswordChange,
                        placeholder = stringResource(Res.string.auth_password),
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardType = KeyboardType.Password,
                        trailing = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    if (passwordVisible) AppIcons.visibilityOff else AppIcons.visibility,
                                    contentDescription = null,
                                    tint = AppColors.slate400,
                                )
                            }
                        },
                    )
                }
                if (mode == AuthMode.Login) {
                    TextButton(onClick = { mode = AuthMode.Forgot }, modifier = Modifier.align(Alignment.End)) {
                        Text(stringResource(Res.string.auth_forgot_password), color = AppColors.primary)
                    }
                } else {
                    Spacer(Modifier.height(18.dp))
                }
                state.error?.let { message ->
                    Text(message, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 8.dp))
                }
                GradientButton(
                    label = when (mode) {
                        AuthMode.Login -> stringResource(Res.string.auth_sign_in)
                        AuthMode.Signup -> stringResource(Res.string.auth_create_account_button)
                        AuthMode.Forgot -> stringResource(Res.string.auth_send_reset_link)
                    },
                    onClick = {
                        when (mode) {
                            AuthMode.Login -> onSubmit()
                            AuthMode.Signup -> signupRequest += 1
                            AuthMode.Forgot -> resetSent = true
                        }
                    },
                    loading = state.isLoading && mode == AuthMode.Login,
                )
                if (mode == AuthMode.Login && isWebTarget && downloadUrl.isNotBlank()) {
                    Row(
                        modifier = Modifier
                            .padding(top = 16.dp)
                            .clickable { openExternalUrl(downloadUrl) },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(AppIcons.download, contentDescription = null, tint = AppColors.primary, modifier = Modifier.size(18.dp))
                        Text(
                            stringResource(Res.string.download_app),
                            modifier = Modifier.padding(start = 8.dp),
                            color = AppColors.primary,
                        )
                    }
                }
            }
        }
        SnackbarHost(snackbar, modifier = Modifier.align(Alignment.BottomCenter))
    }
    LaunchedEffect(signupRequest) {
        if (signupRequest > 0) snackbar.showSnackbar(comingSoon)
    }
}

@Composable
private fun AuthField(
    value: String,
    onValue: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailing: @Composable (() -> Unit)? = null,
) {
    TextField(
        value = value,
        onValueChange = onValue,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(placeholder) },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        colors = appTextFieldColors(),
        visualTransformation = visualTransformation,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        trailingIcon = trailing,
    )
}
