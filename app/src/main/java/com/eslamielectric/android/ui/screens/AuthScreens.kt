package com.eslamielectric.android.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eslamielectric.android.R
import com.eslamielectric.android.core.network.ProfilePatchRequest
import com.eslamielectric.android.core.network.SignupRequest
import com.eslamielectric.android.feature.auth.AccountViewModel
import com.eslamielectric.android.feature.auth.AuthFormState
import com.eslamielectric.android.feature.auth.AuthRepository
import com.eslamielectric.android.feature.auth.ClaimAccountUiState
import com.eslamielectric.android.feature.auth.ClaimAccountViewModel
import com.eslamielectric.android.feature.auth.ForgotPasswordViewModel
import com.eslamielectric.android.feature.auth.LoginViewModel
import com.eslamielectric.android.feature.auth.ProfileUiState
import com.eslamielectric.android.feature.auth.ProfileViewModel
import com.eslamielectric.android.feature.auth.ResetPasswordViewModel
import com.eslamielectric.android.feature.auth.SignUpViewModel
import com.eslamielectric.android.feature.auth.authViewModelFactory
import com.eslamielectric.android.feature.auth.claimAccountViewModelFactory
import com.eslamielectric.android.feature.auth.resetPasswordViewModelFactory
import com.eslamielectric.android.util.WebLinks
import kotlinx.coroutines.launch

@Composable
fun AccountHomeScreen(
    authRepository: AuthRepository,
    locale: String,
    onLocaleChange: (String) -> Unit,
    onLogin: () -> Unit,
    onSignUp: () -> Unit,
    onProfile: () -> Unit,
    onMyOrders: () -> Unit,
    onGuestTrack: () -> Unit,
    onNotifications: () -> Unit = {},
    onClaimAccount: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel: AccountViewModel = viewModel(factory = authViewModelFactory(authRepository))
    val isLoggedIn by viewModel.isLoggedIn.collectAsState(initial = authRepository.isLoggedIn())
    val profileState by viewModel.profileState.collectAsState()

    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) viewModel.refresh()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
            .testTag("screen_account"),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LanguageToggle(
            locale = locale,
            onLocaleChange = onLocaleChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        )
        if (!isLoggedIn) {
            Text(
                text = stringResource(R.string.account_signed_out_hint),
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(24.dp))
            OutlinedButton(
                onClick = onGuestTrack,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("btn_guest_track")
            ) {
                Text(stringResource(R.string.action_guest_track))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onLogin,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("btn_account_login")
            ) {
                Text(stringResource(R.string.action_login))
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onSignUp,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("btn_sign_up")
            ) {
                Text(stringResource(R.string.action_sign_up))
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onClaimAccount,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("btn_claim_account")
            ) {
                Text(stringResource(R.string.action_claim_account))
            }
        } else {
            when (val state = profileState) {
                ProfileUiState.Loading -> CircularProgressIndicator()
                is ProfileUiState.Error -> {
                    Text(state.message, color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(12.dp))
                    TextButton(onClick = viewModel::refresh) {
                        Text(stringResource(R.string.retry))
                    }
                }
                is ProfileUiState.Ready -> {
                    val name = listOfNotNull(state.profile.firstName, state.profile.surname)
                        .joinToString(" ")
                        .ifBlank { null }
                    Text(
                        text = name ?: stringResource(R.string.account_welcome),
                        style = MaterialTheme.typography.headlineSmall
                    )
                    state.profile.email?.let { email ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(email, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onMyOrders,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("btn_my_orders")
            ) {
                Text(stringResource(R.string.action_my_orders))
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(onClick = onGuestTrack, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.action_guest_track))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onProfile,
                modifier = Modifier.fillMaxWidth(),
                enabled = profileState is ProfileUiState.Ready
            ) {
                Text(stringResource(R.string.action_profile))
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onNotifications,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("btn_notifications")
            ) {
                Text(stringResource(R.string.action_notifications))
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = { viewModel.logout() },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("btn_logout")
            ) {
                Text(stringResource(R.string.action_logout))
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        OutlinedButton(
            onClick = { WebLinks.openPrivacyPolicy(context, locale) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("btn_privacy_policy")
        ) {
            Text(stringResource(R.string.action_privacy_policy))
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(
            onClick = { WebLinks.openWhatsApp(context, locale) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("btn_contact_whatsapp")
        ) {
            Text(stringResource(R.string.action_contact_whatsapp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    authRepository: AuthRepository,
    onBack: () -> Unit,
    onSignUp: () -> Unit,
    onForgotPassword: () -> Unit,
    onLoggedIn: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: LoginViewModel = viewModel(factory = authViewModelFactory(authRepository))
    val state by viewModel.state.collectAsState()
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(state) {
        if (state is AuthFormState.Success && authRepository.isLoggedIn()) {
            onLoggedIn()
        }
    }

    AuthScaffold(
        title = stringResource(R.string.login_title),
        onBack = onBack,
        modifier = modifier
    ) {
        if (viewModel.isGoogleSignInAvailable) {
            OutlinedButton(
                onClick = { viewModel.signInWithGoogle() },
                enabled = state !is AuthFormState.Loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("btn_google_sign_in")
            ) {
                Text(stringResource(R.string.continue_with_google))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f))
                Text(
                    text = stringResource(R.string.auth_or_divider),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                HorizontalDivider(modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
        OutlinedTextField(
            value = email,
            onValueChange = { email = it; viewModel.clearError() },
            label = { Text(stringResource(R.string.label_email)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("field_login_email")
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it; viewModel.clearError() },
            label = { Text(stringResource(R.string.label_password)) },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("field_login_password")
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onForgotPassword) {
                Text(stringResource(R.string.forgot_password_link))
            }
        }
        AuthErrorOrLoading(state)
        Button(
            onClick = { viewModel.login(email, password, onLoggedIn) },
            enabled = state !is AuthFormState.Loading,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("btn_login_submit")
        ) {
            Text(stringResource(R.string.action_login))
        }
        Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.no_account_prompt))
            TextButton(onClick = onSignUp) {
                Text(stringResource(R.string.action_sign_up))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(
    authRepository: AuthRepository,
    onBack: () -> Unit,
    onSignedUp: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: SignUpViewModel = viewModel(factory = authViewModelFactory(authRepository))
    val state by viewModel.state.collectAsState()
    var accountType by rememberSaveable { mutableStateOf("person") }
    var firstName by rememberSaveable { mutableStateOf("") }
    var surname by rememberSaveable { mutableStateOf("") }
    var dob by rememberSaveable { mutableStateOf("") }
    var mobile by rememberSaveable { mutableStateOf("") }
    var landline by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var address by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var companyName by rememberSaveable { mutableStateOf("") }
    var companyNumber by rememberSaveable { mutableStateOf("") }

    AuthScaffold(
        title = stringResource(R.string.signup_title),
        onBack = onBack,
        modifier = modifier.testTag("screen_signup")
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = accountType == "person",
                onClick = { accountType = "person" },
                label = { Text(stringResource(R.string.account_type_person)) }
            )
            FilterChip(
                selected = accountType == "company",
                onClick = { accountType = "company" },
                label = { Text(stringResource(R.string.account_type_company)) }
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        AuthTextField(firstName, { firstName = it; viewModel.clearError() }, R.string.label_first_name)
        AuthTextField(surname, { surname = it; viewModel.clearError() }, R.string.label_surname)
        AuthTextField(dob, { dob = it }, R.string.label_dob_optional)
        AuthTextField(mobile, { mobile = it; viewModel.clearError() }, R.string.label_mobile)
        AuthTextField(landline, { landline = it }, R.string.label_landline_optional)
        AuthTextField(email, { email = it; viewModel.clearError() }, R.string.label_email)
        AuthTextField(address, { address = it; viewModel.clearError() }, R.string.label_address)
        if (accountType == "company") {
            AuthTextField(companyName, { companyName = it }, R.string.label_company_name)
            AuthTextField(companyNumber, { companyNumber = it }, R.string.label_company_number)
        }
        AuthTextField(
            password,
            { password = it; viewModel.clearError() },
            R.string.label_password,
            password = true
        )
        AuthErrorOrLoading(state)
        Button(
            onClick = {
                val request = SignupRequest(
                    type = accountType,
                    firstName = firstName.trim(),
                    surname = surname.trim(),
                    dob = dob.trim().ifBlank { null },
                    mobile = mobile.trim(),
                    landline = landline.trim().ifBlank { null },
                    email = email.trim().lowercase(),
                    address = address.trim(),
                    companyName = if (accountType == "company") companyName.trim().ifBlank { null } else null,
                    companyNumber = if (accountType == "company") companyNumber.trim().ifBlank { null } else null,
                    password = password
                )
                viewModel.signup(request, onSignedUp)
            },
            enabled = state !is AuthFormState.Loading,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("btn_signup_submit")
        ) {
            Text(stringResource(R.string.action_sign_up))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(
    authRepository: AuthRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: ForgotPasswordViewModel = viewModel(factory = authViewModelFactory(authRepository))
    val state by viewModel.state.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()
    var email by rememberSaveable { mutableStateOf("") }

    AuthScaffold(
        title = stringResource(R.string.forgot_password_title),
        onBack = onBack,
        modifier = modifier.testTag("screen_forgot_password")
    ) {
        Text(
            text = stringResource(R.string.forgot_password_hint),
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = email,
            onValueChange = { email = it; viewModel.clearError() },
            label = { Text(stringResource(R.string.label_email)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("field_forgot_email")
        )
        successMessage?.let { msg ->
            Spacer(modifier = Modifier.height(12.dp))
            Text(msg, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.forgot_password_check_email),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        AuthErrorOrLoading(state)
        Button(
            onClick = { viewModel.submit(email) },
            enabled = state !is AuthFormState.Loading,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("btn_send_reset_link")
        ) {
            Text(stringResource(R.string.action_send_reset_link))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResetPasswordScreen(
    authRepository: AuthRepository,
    initialToken: String,
    onBack: () -> Unit,
    onResetSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: ResetPasswordViewModel = viewModel(
        factory = resetPasswordViewModelFactory(authRepository)
    )
    val state by viewModel.state.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()
    var token by rememberSaveable(initialToken) { mutableStateOf(initialToken) }
    var newPassword by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(state) {
        if (state is AuthFormState.Success) onResetSuccess()
    }

    AuthScaffold(
        title = stringResource(R.string.reset_password_title),
        onBack = onBack,
        modifier = modifier.testTag("screen_reset_password")
    ) {
        Text(
            text = stringResource(R.string.reset_password_hint),
            style = MaterialTheme.typography.bodyMedium
        )
        if (token.isBlank()) {
            AuthTextField(token, { token = it; viewModel.clearError() }, R.string.reset_password_token_label)
        }
        AuthTextField(newPassword, { newPassword = it; viewModel.clearError() }, R.string.label_password, password = true)
        AuthTextField(
            confirmPassword,
            { confirmPassword = it; viewModel.clearError() },
            R.string.label_confirm_password,
            password = true
        )
        successMessage?.let { msg ->
            Text(msg, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
        }
        AuthErrorOrLoading(state)
        Button(
            onClick = { viewModel.submit(token, newPassword, confirmPassword) },
            enabled = state !is AuthFormState.Loading,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("btn_reset_password_submit")
        ) {
            Text(stringResource(R.string.action_reset_password))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClaimAccountScreen(
    authRepository: AuthRepository,
    initialToken: String,
    onBack: () -> Unit,
    onClaimed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: ClaimAccountViewModel = viewModel(
        factory = claimAccountViewModelFactory(authRepository, initialToken)
    )
    val uiState by viewModel.uiState.collectAsState()
    val submitState by viewModel.submitState.collectAsState()
    var token by rememberSaveable(initialToken) { mutableStateOf(initialToken) }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }

    AuthScaffold(
        title = stringResource(R.string.claim_account_title),
        onBack = onBack,
        modifier = modifier.testTag("screen_claim_account")
    ) {
        Text(
            text = stringResource(R.string.claim_account_intro),
            style = MaterialTheme.typography.bodyMedium
        )
        OutlinedTextField(
            value = token,
            onValueChange = {
                token = it
                viewModel.updateToken(it)
            },
            label = { Text(stringResource(R.string.claim_account_token_label)) },
            placeholder = { Text(stringResource(R.string.claim_account_token_hint)) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("field_claim_token")
        )
        OutlinedButton(
            onClick = {
                viewModel.updateToken(token)
                viewModel.validateToken()
            },
            enabled = uiState !is ClaimAccountUiState.Validating,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("btn_claim_validate")
        ) {
            Text(stringResource(R.string.claim_account_validate))
        }
        when (val s = uiState) {
            ClaimAccountUiState.Validating -> CircularProgressIndicator(
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            is ClaimAccountUiState.Ready -> {
                Text(
                    text = stringResource(R.string.claim_account_email_label, s.maskedEmail),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            is ClaimAccountUiState.Error -> Text(s.message, color = MaterialTheme.colorScheme.error)
            ClaimAccountUiState.Idle -> Unit
        }
        AuthTextField(password, { password = it; viewModel.clearSubmitError() }, R.string.label_password, password = true)
        AuthTextField(
            confirmPassword,
            { confirmPassword = it; viewModel.clearSubmitError() },
            R.string.label_confirm_password,
            password = true
        )
        if (submitState is AuthFormState.Error) {
            Text(
                (submitState as AuthFormState.Error).message,
                color = MaterialTheme.colorScheme.error
            )
        }
        if (submitState is AuthFormState.Loading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        }
        Button(
            onClick = { viewModel.claim(password, confirmPassword, onClaimed) },
            enabled = submitState !is AuthFormState.Loading && uiState is ClaimAccountUiState.Ready,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("btn_claim_account_submit")
        ) {
            Text(stringResource(R.string.claim_account_submit))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    authRepository: AuthRepository,
    onBack: () -> Unit,
    onSessionExpired: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: ProfileViewModel = viewModel(factory = authViewModelFactory(authRepository))
    val uiState by viewModel.uiState.collectAsState()
    val saveState by viewModel.saveState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.onSessionExpired = onSessionExpired
        viewModel.load()
    }

    var firstName by rememberSaveable { mutableStateOf("") }
    var surname by rememberSaveable { mutableStateOf("") }
    var dob by rememberSaveable { mutableStateOf("") }
    var mobile by rememberSaveable { mutableStateOf("") }
    var landline by rememberSaveable { mutableStateOf("") }
    var contactEmail by rememberSaveable { mutableStateOf("") }
    var address by rememberSaveable { mutableStateOf("") }
    var companyName by rememberSaveable { mutableStateOf("") }
    var companyNumber by rememberSaveable { mutableStateOf("") }
    var companyContact by rememberSaveable { mutableStateOf("") }
    var companyPrincipal by rememberSaveable { mutableStateOf("") }
    var isCompany by rememberSaveable { mutableStateOf(false) }
    var fieldsLoaded by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(uiState) {
        if (uiState is ProfileUiState.Ready && !fieldsLoaded) {
            val p = (uiState as ProfileUiState.Ready).profile
            firstName = p.firstName.orEmpty()
            surname = p.surname.orEmpty()
            dob = p.dob.orEmpty()
            mobile = p.mobile.orEmpty()
            landline = p.landline.orEmpty()
            contactEmail = p.contactEmail.orEmpty()
            address = p.address.orEmpty()
            companyName = p.companyName.orEmpty()
            companyNumber = p.companyNumber.orEmpty()
            companyContact = p.companyContactNumber.orEmpty()
            companyPrincipal = p.companyPrincipalContact.orEmpty()
            isCompany = p.type == "company"
            fieldsLoaded = true
        }
    }

    val savedMessage = stringResource(R.string.profile_saved)
    LaunchedEffect(saveState) {
        if (saveState is AuthFormState.Success) {
            scope.launch { snackbarHostState.showSnackbar(savedMessage) }
            viewModel.clearSaveState()
        }
    }

    Scaffold(
        modifier = modifier.testTag("screen_profile"),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.profile_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        when (val state = uiState) {
            ProfileUiState.Loading -> Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) { CircularProgressIndicator() }

            is ProfileUiState.Error -> Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(state.message, color = MaterialTheme.colorScheme.error)
                TextButton(onClick = viewModel::load) { Text(stringResource(R.string.retry)) }
            }

            is ProfileUiState.Ready -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    state.profile.email?.let { email ->
                        Text(stringResource(R.string.label_email) + ": $email")
                    }
                    AuthTextField(firstName, { firstName = it; viewModel.clearSaveState() }, R.string.label_first_name)
                    AuthTextField(surname, { surname = it; viewModel.clearSaveState() }, R.string.label_surname)
                    AuthTextField(dob, { dob = it; viewModel.clearSaveState() }, R.string.label_dob_optional)
                    AuthTextField(mobile, { mobile = it; viewModel.clearSaveState() }, R.string.label_mobile)
                    AuthTextField(landline, { landline = it; viewModel.clearSaveState() }, R.string.label_landline_optional)
                    AuthTextField(contactEmail, { contactEmail = it; viewModel.clearSaveState() }, R.string.label_contact_email_optional)
                    AuthTextField(address, { address = it; viewModel.clearSaveState() }, R.string.label_address)
                    if (isCompany) {
                        HorizontalDivider()
                        AuthTextField(companyName, { companyName = it; viewModel.clearSaveState() }, R.string.label_company_name)
                        AuthTextField(companyNumber, { companyNumber = it; viewModel.clearSaveState() }, R.string.label_company_number)
                        AuthTextField(companyContact, { companyContact = it; viewModel.clearSaveState() }, R.string.label_company_contact_optional)
                        AuthTextField(companyPrincipal, { companyPrincipal = it; viewModel.clearSaveState() }, R.string.label_company_principal_optional)
                    }
                    if (saveState is AuthFormState.Error) {
                        Text(
                            (saveState as AuthFormState.Error).message,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    Button(
                        onClick = {
                            val patch = ProfilePatchRequest(
                                firstName = firstName.trim().ifBlank { null },
                                surname = surname.trim().ifBlank { null },
                                dob = dob.trim().ifBlank { null },
                                mobile = mobile.trim().ifBlank { null },
                                landline = landline.trim().ifBlank { null },
                                contactEmail = contactEmail.trim().ifBlank { null },
                                address = address.trim().ifBlank { null },
                                companyName = if (isCompany) companyName.trim().ifBlank { null } else null,
                                companyNumber = if (isCompany) companyNumber.trim().ifBlank { null } else null,
                                companyContactNumber = if (isCompany) companyContact.trim().ifBlank { null } else null,
                                companyPrincipalContact = if (isCompany) companyPrincipal.trim().ifBlank { null } else null
                            )
                            viewModel.save(patch) {
                                scope.launch { snackbarHostState.showSnackbar(savedMessage) }
                            }
                        },
                        enabled = saveState !is AuthFormState.Loading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_save_profile")
                    ) {
                        if (saveState is AuthFormState.Loading) {
                            CircularProgressIndicator(modifier = Modifier.height(24.dp))
                        } else {
                            Text(stringResource(R.string.action_save_profile))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthScaffold(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}

@Composable
private fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    labelRes: Int,
    password: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(labelRes)) },
        visualTransformation = if (password) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        keyboardOptions = if (password) KeyboardOptions(keyboardType = KeyboardType.Password)
        else KeyboardOptions.Default,
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun LanguageToggle(
    locale: String,
    onLocaleChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = stringResource(R.string.language_label),
            style = MaterialTheme.typography.titleSmall
        )
        Row(
            modifier = Modifier.padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = locale == "en",
                onClick = { onLocaleChange("en") },
                label = { Text(stringResource(R.string.language_en)) },
                modifier = Modifier.testTag("locale_en")
            )
            FilterChip(
                selected = locale == "fa",
                onClick = { onLocaleChange("fa") },
                label = { Text(stringResource(R.string.language_fa)) },
                modifier = Modifier.testTag("locale_fa")
            )
        }
    }
}

@Composable
private fun ColumnScope.AuthErrorOrLoading(state: AuthFormState) {
    when (state) {
        is AuthFormState.Loading -> {
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                CircularProgressIndicator()
            }
        }
        is AuthFormState.Error -> {
            Spacer(modifier = Modifier.height(8.dp))
            Text(state.message, color = MaterialTheme.colorScheme.error)
        }
        else -> Unit
    }
}
