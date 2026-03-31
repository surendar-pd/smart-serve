package com.smartserve.sharedauth

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Surface
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.PaddingValues
import coil.compose.AsyncImage
import com.smartserve.sharedui.SharedButton
import com.smartserve.sharedui.SharedButtonVariant
import com.smartserve.sharedui.SharedCard
import com.smartserve.sharedui.SharedDropdown
import com.smartserve.sharedui.SharedDividerWithCenterLabel
import com.smartserve.sharedui.SharedInputIcon
import com.smartserve.sharedui.SharedPaddedScrollColumn
import com.smartserve.sharedui.SharedScaffold
import com.smartserve.sharedui.SharedScreenHeader
import com.smartserve.sharedui.SharedSwitchRow
import com.smartserve.sharedui.SharedText
import com.smartserve.sharedui.SharedTextField
import com.smartserve.sharedui.SharedTextVariant
import com.smartserve.sharedui.SharedTopAppBar
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Warning

/** Extra space below status bar so the snackbar clears [SharedTopAppBar]. */
private val AuthSnackbarBelowTopBarInset = 64.dp

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onBack: () -> Unit,
    onForgotPassword: () -> Unit,
    onNavigateToSignUp: () -> Unit,
    onNavigateToCustomerHome: () -> Unit,
    onNavigateToProviderHome: () -> Unit,
    onNavigateToCustomerSetup: (String) -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(state.navigateTo) {
        when (val dest = state.navigateTo) {
            is AuthNavDestination.CustomerHome -> {
                onNavigateToCustomerHome()
                viewModel.clearNavigation()
            }
            is AuthNavDestination.ProviderHome -> {
                onNavigateToProviderHome()
                viewModel.clearNavigation()
            }
            is AuthNavDestination.CustomerProfileSetup -> {
                onNavigateToCustomerSetup(dest.uid)
                viewModel.clearNavigation()
            }
            null -> {}
        }
    }

    AuthScaffoldColumn(
        topBar = { SharedTopAppBar(title = "Sign in", onBack = onBack) },
        snackbarBelowTopBarInset = AuthSnackbarBelowTopBarInset,
    ) { snackbarHostState ->
        AuthErrorSnackbarLaunchedEffect(
            errorMessage = state.errorMessage,
            snackbarHostState = snackbarHostState,
            onConsumed = viewModel::clearError,
        )
        SharedScreenHeader(title = "Welcome back", subtitle = "Sign in to your SmartServe account")

        SharedTextField(
            value = state.loginEmail,
            onValueChange = viewModel::onLoginEmailChange,
            label = "Email",
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            leadingIcon = { SharedInputIcon(Icons.Filled.Email, contentDescription = null) },
        )

        SharedTextField(
            value = state.loginPassword,
            onValueChange = viewModel::onLoginPasswordChange,
            label = "Password",
            modifier = Modifier.fillMaxWidth(),
            passwordToggleEnabled = true,
            leadingIcon = { SharedInputIcon(Icons.Filled.Lock, contentDescription = null) },
        )

        SharedButton(
            text = "Forgot Password?",
            onClick = onForgotPassword,
            modifier = Modifier.align(Alignment.End),
            variant = SharedButtonVariant.Ghost,
        )

        Spacer(Modifier.height(8.dp))

        AuthPrimaryButton(
            label = "Log In",
            isLoading = state.isLoading,
            onClick = viewModel::login,
        )

        SharedButton(
            text = "Don't have an account? Sign up",
            onClick = onNavigateToSignUp,
            modifier = Modifier.fillMaxWidth(),
            variant = SharedButtonVariant.Ghost,
        )

        SharedDividerWithCenterLabel(label = "or")

        SharedButton(
            text = "Continue with Google (coming soon)",
            onClick = {},
            modifier = Modifier.fillMaxWidth(),
            variant = SharedButtonVariant.Outline,
            enabled = false,
        )
    }
}

@Composable
fun SignUpCustomerScreen(
    viewModel: AuthViewModel,
    onBack: () -> Unit,
    onNavigateToProfileSetup: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.navigateTo) {
        val dest = state.navigateTo
        if (dest is AuthNavDestination.CustomerProfileSetup) {
            onNavigateToProfileSetup(dest.uid)
            viewModel.clearNavigation()
        }
    }

    AuthScaffoldColumn(
        topBar = { SharedTopAppBar(title = "Create Account", onBack = onBack) },
        snackbarBelowTopBarInset = AuthSnackbarBelowTopBarInset,
    ) { snackbarHostState ->
        AuthErrorSnackbarLaunchedEffect(
            errorMessage = state.errorMessage,
            snackbarHostState = snackbarHostState,
            onConsumed = viewModel::clearError,
        )
        SharedScreenHeader(
            title = "Join SmartServe",
            subtitle = "Create your customer account"
        )

        SharedTextField(
            value = state.signUpFullName,
            onValueChange = viewModel::onSignUpNameChange,
            label = "Full Name",
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { SharedInputIcon(Icons.Filled.Person, contentDescription = null) },
        )

        SharedTextField(
            value = state.signUpEmail,
            onValueChange = viewModel::onSignUpEmailChange,
            label = "Email",
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            leadingIcon = { SharedInputIcon(Icons.Filled.Email, contentDescription = null) },
        )

        PasswordField(
            value = state.signUpPassword,
            onValueChange = viewModel::onSignUpPasswordChange,
            label = "Password"
        )

        PasswordField(
            value = state.signUpConfirmPassword,
            onValueChange = viewModel::onSignUpConfirmPasswordChange,
            label = "Confirm Password"
        )

        Spacer(Modifier.height(8.dp))

        AuthPrimaryButton(
            label = "Create Account",
            isLoading = state.isLoading,
            onClick = viewModel::signUpCustomer
        )

        SharedDividerWithCenterLabel(label = "or")

        SharedButton(
            text = "Continue with Google (coming soon)",
            onClick = {},
            modifier = Modifier.fillMaxWidth(),
            variant = SharedButtonVariant.Outline,
            enabled = false,
        )
    }
}

@Composable
fun SignUpProviderScreen(
    viewModel: AuthViewModel,
    onBack: () -> Unit,
    onNavigateToProviderHome: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.navigateTo) {
        val dest = state.navigateTo
        if (dest is AuthNavDestination.ProviderHome) {
            onNavigateToProviderHome()
            viewModel.clearNavigation()
        }
    }

    AuthScaffoldColumn(
        topBar = { SharedTopAppBar(title = "Join as Provider", onBack = onBack) },
        snackbarBelowTopBarInset = AuthSnackbarBelowTopBarInset,
    ) { snackbarHostState ->
        AuthErrorSnackbarLaunchedEffect(
            errorMessage = state.errorMessage,
            snackbarHostState = snackbarHostState,
            onConsumed = viewModel::clearError,
        )
        SharedScreenHeader(
            title = "Provider Sign Up",
            subtitle = "Create your provider account"
        )

        SharedTextField(
            value = state.signUpFullName,
            onValueChange = viewModel::onSignUpNameChange,
            label = "Full Name",
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { SharedInputIcon(Icons.Filled.Person, contentDescription = null) },
        )

        SharedTextField(
            value = state.signUpEmail,
            onValueChange = viewModel::onSignUpEmailChange,
            label = "Email",
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            leadingIcon = { SharedInputIcon(Icons.Filled.Email, contentDescription = null) },
        )

        SharedTextField(
            value = state.signUpPhone,
            onValueChange = viewModel::onSignUpPhoneChange,
            label = "Phone Number *",
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            leadingIcon = { SharedInputIcon(Icons.Filled.Phone, contentDescription = null) },
        )

        SharedText(
            text = "You can add your services, pricing, and coverage area later from Profile → Services and Details.",
            variant = SharedTextVariant.Caption,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp),
        )

        PasswordField(
            value = state.signUpPassword,
            onValueChange = viewModel::onSignUpPasswordChange,
            label = "Password"
        )

        PasswordField(
            value = state.signUpConfirmPassword,
            onValueChange = viewModel::onSignUpConfirmPasswordChange,
            label = "Confirm Password"
        )

        Spacer(Modifier.height(8.dp))

        AuthPrimaryButton(
            label = "Create Account",
            isLoading = state.isLoading,
            onClick = viewModel::signUpProvider
        )

        SharedDividerWithCenterLabel(label = "or")

        SharedButton(
            text = "Continue with Google (coming soon)",
            onClick = {},
            modifier = Modifier.fillMaxWidth(),
            variant = SharedButtonVariant.Outline,
            enabled = false,
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// 1. Introduction – Customer
// ═══════════════════════════════════════════════════════════════
@Composable
fun IntroCustomerScreen(
    onGetStarted: () -> Unit,
) {
    IntroScaffold(
        headlineLine1 = "Book local services,",
        headlineLine2 = "smarter and faster",
        subtitle = "Find trusted Ottawa providers for home,\neducation, and everyday needs.",
        primaryLabel = "Get started",
        onPrimary = onGetStarted,
    )
}

// ═══════════════════════════════════════════════════════════════
// 2. Introduction – Provider
// ═══════════════════════════════════════════════════════════════
@Composable
fun IntroProviderScreen(
    onGetStarted: () -> Unit,
) {
    IntroScaffold(
        headlineLine1 = "Grow your",
        headlineLine2 = "service business",
        subtitle = "List your services and reach customers\nwho need you in Ottawa.",
        primaryLabel = "Join as provider",
        onPrimary = onGetStarted,
    )
}

// Shared intro layout — bottom-aligned, minimal (reference: bold headline + pill CTA)
@Composable
private fun IntroScaffold(
    headlineLine1: String,
    headlineLine2: String,
    subtitle: String,
    primaryLabel: String,
    onPrimary: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomStart)
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SharedText(
                text = "${headlineLine1}\n${headlineLine2}",
                variant = SharedTextVariant.Headline,
                fontWeight = FontWeight.Bold,
                lineHeight = MaterialTheme.typography.headlineLarge.fontSize,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth(),
            )
            SharedText(
                text = subtitle,
                variant = SharedTextVariant.Body,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            SharedButton(
                text = primaryLabel,
                onClick = onPrimary,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// 3. Forgot Password
// ═══════════════════════════════════════════════════════════════
@Composable
fun ForgotPasswordScreen(
    viewModel: AuthViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    AuthScaffoldColumn(
        topBar = { SharedTopAppBar(title = "Reset Password", onBack = onBack) },
        snackbarBelowTopBarInset = AuthSnackbarBelowTopBarInset,
    ) { snackbarHostState ->
        AuthErrorSnackbarLaunchedEffect(
            errorMessage = state.errorMessage,
            snackbarHostState = snackbarHostState,
            onConsumed = viewModel::clearError,
        )
        SharedScreenHeader(
            title = "Forgot Password?",
            subtitle = "Enter your email and we'll send you a reset link. The link expires after 15 minutes."
        )

        SharedTextField(
            value = state.forgotEmail,
            onValueChange = viewModel::onForgotEmailChange,
            label = "Email",
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            leadingIcon = { SharedInputIcon(Icons.Filled.Email, contentDescription = null) },
        )

        Spacer(Modifier.height(8.dp))

        AuthPrimaryButton(
            label = "Send Reset Link",
            isLoading = state.isLoading,
            onClick = viewModel::sendPasswordReset
        )

        state.successMessage?.let { msg ->
            Spacer(Modifier.height(16.dp))
            SharedCard(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                contentPadding = PaddingValues(16.dp),
            ) {
                SharedText(
                    text = msg,
                    variant = SharedTextVariant.Body,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// 4. Customer Profile Setup
// ═══════════════════════════════════════════════════════════════
@Composable
fun CustomerProfileSetupScreen(
    uid: String,
    viewModel: CustomerProfileViewModel,
    onStart: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.onboardingCompleted.collect {
            onStart()
        }
    }

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? -> viewModel.onPhotoSelected(uri) }

    AuthScaffoldColumn { snackbarHostState ->
        AuthErrorSnackbarLaunchedEffect(
            errorMessage = state.errorMessage,
            snackbarHostState = snackbarHostState,
            onConsumed = viewModel::clearError,
        )
        SharedScreenHeader(
            title = "Set up your profile",
            subtitle = "Just a few details to personalise your experience"
        )

        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                .clickable { photoPicker.launch("image/*") }
                .align(Alignment.CenterHorizontally),
            contentAlignment = Alignment.Center
        ) {
            if (state.photoUri != null) {
                AsyncImage(
                    model = state.photoUri,
                    contentDescription = "Profile photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.AddAPhoto, contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary)
                    SharedText(
                        text = "Add Photo",
                        variant = SharedTextVariant.Caption,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        SharedTextField(
            value = state.phone,
            onValueChange = viewModel::onPhoneChange,
            label = "Phone Number (optional)",
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            leadingIcon = { SharedInputIcon(Icons.Filled.Phone, contentDescription = null) },
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SharedTextField(
                value = state.homeAddress,
                onValueChange = viewModel::onHomeAddressChange,
                label = "Home Address *",
                modifier = Modifier.weight(1f),
                leadingIcon = { SharedInputIcon(Icons.Filled.Home, contentDescription = null) },
            )
            SharedButton(
                text = "Verify",
                onClick = viewModel::validateAddress,
                enabled = state.homeAddress.isNotBlank() &&
                          state.addressValidState != AddressValidState.Validating,
                loading = state.addressValidState == AddressValidState.Validating,
                variant = SharedButtonVariant.Outline,
            )
        }

        // Autocomplete suggestions
        if (state.addressSuggestions.isNotEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                tonalElevation = 4.dp,
                shadowElevation = 2.dp,
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    state.addressSuggestions.forEachIndexed { index, suggestion ->
                        if (index > 0) {
                            androidx.compose.material3.HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant,
                            )
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.onSuggestionSelected(suggestion) }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                SharedText(
                                    text = suggestion.shortLabel,
                                    variant = SharedTextVariant.Body,
                                )
                                if (!suggestion.isInOttawa) {
                                    SharedText(
                                        text = "Outside Ottawa area",
                                        variant = SharedTextVariant.Caption,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
        }

        AddressValidationStatusRow(state.addressValidState, state.addressGeoResult)

        SharedText(
            text = "This address will be pre-filled in all your future bookings.",
            variant = SharedTextVariant.Caption,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
        )

        PreferenceToggleRow(
            label = "Location-based recommendations",
            checked = state.locationAwareness,
            onChange = viewModel::onLocationToggle
        )

        PreferenceToggleRow(
            label = "Push notifications",
            checked = state.pushNotifications,
            onChange = viewModel::onNotifToggle
        )

        Spacer(Modifier.height(16.dp))

        AuthPrimaryButton(
            label = "Complete setup",
            isLoading = state.isLoading,
            onClick = { viewModel.completeOnboarding(uid) }
        )
    }
}

@Composable
private fun AuthErrorSnackbarLaunchedEffect(
    errorMessage: String?,
    snackbarHostState: SnackbarHostState,
    onConsumed: () -> Unit,
) {
    LaunchedEffect(errorMessage) {
        val msg = errorMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message = msg)
        onConsumed()
    }
}

@Composable
private fun AuthScaffoldColumn(
    topBar: @Composable () -> Unit = {},
    /** When a [topBar] is shown, pass ~64.dp so the snackbar sits below the app bar. */
    snackbarBelowTopBarInset: Dp = 0.dp,
    content: @Composable ColumnScope.(SnackbarHostState) -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    Box(Modifier.fillMaxSize()) {
        SharedScaffold(
            topBar = topBar,
            snackbarHost = {},
        ) { padding ->
            SharedPaddedScrollColumn(paddingValues = padding) {
                content(snackbarHostState)
            }
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .statusBarsPadding()
                .padding(top = 8.dp)
                .padding(top = snackbarBelowTopBarInset),
            snackbar = { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    actionContentColor = MaterialTheme.colorScheme.onErrorContainer,
                    dismissActionContentColor = MaterialTheme.colorScheme.onErrorContainer,
                )
            },
        )
    }
}

@Composable
private fun PasswordField(value: String, onValueChange: (String) -> Unit, label: String) {
    SharedTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        modifier = Modifier.fillMaxWidth(),
        passwordToggleEnabled = true,
        leadingIcon = { SharedInputIcon(Icons.Filled.Lock, contentDescription = null) },
    )
}

@Composable
private fun AuthPrimaryButton(label: String, isLoading: Boolean, onClick: () -> Unit) {
    SharedButton(
        text = label,
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        variant = SharedButtonVariant.Default,
        loading = isLoading,
    )
}

@Composable
private fun GoogleSignInButton(onClick: () -> Unit) {
    SharedButton(
        text = "Continue with Google",
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        variant = SharedButtonVariant.Outline,
    )
}

@Composable
private fun PreferenceToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    SharedSwitchRow(
        title = label,
        checked = checked,
        onCheckedChange = onChange,
    )
}

@Composable
fun AddressValidationStatusRow(
    state: AddressValidState,
    geoResult: GeoResult?,
) {
    when (state) {
        AddressValidState.Idle, AddressValidState.Validating -> Unit
        AddressValidState.Valid -> Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth().padding(start = 4.dp, top = 2.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp),
            )
            SharedText(
                text = "Ottawa address confirmed${geoResult?.shortLabel?.let { ": $it" } ?: ""}",
                variant = SharedTextVariant.Caption,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        AddressValidState.NotOttawa -> Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth().padding(start = 4.dp, top = 2.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(16.dp),
            )
            SharedText(
                text = "Address found but outside the Ottawa service area",
                variant = SharedTextVariant.Caption,
                color = MaterialTheme.colorScheme.tertiary,
            )
        }
        AddressValidState.NotFound -> Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth().padding(start = 4.dp, top = 2.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Error,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(16.dp),
            )
            SharedText(
                text = "Address not found — try a more specific address",
                variant = SharedTextVariant.Caption,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}
