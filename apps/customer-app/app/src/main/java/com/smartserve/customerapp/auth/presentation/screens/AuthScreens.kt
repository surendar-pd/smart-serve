//package com.smartserve.auth.presentation.screens
package com.smartserve.customerapp.auth.presentation.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.GeoPoint
import com.google.maps.android.compose.*
import com.smartserve.customerapp.auth.presentation.viewmodel.AuthViewModel
import com.smartserve.customerapp.auth.presentation.viewmodel.CustomerProfileViewModel
import com.smartserve.customerapp.auth.presentation.viewmodel.ProviderProfileViewModel
import com.smartserve.customerapp.auth.presentation.viewmodel.AuthNavDestination
import androidx.compose.material3.ExperimentalMaterial3Api

// ═══════════════════════════════════════════════════════════════
// 1. Introduction – Customer
// ═══════════════════════════════════════════════════════════════
@Composable
fun IntroCustomerScreen(
    onGetStarted: () -> Unit,
    onLogin: () -> Unit
) {
    IntroScaffold(
        tagline        = "Book local services,\nsmarter and faster.",
        description    = "SmartServe connects Ottawa residents with trusted local providers for cleaning, tutoring, moving, and more — with smart recurring suggestions so booking takes seconds.",
        primaryLabel   = "Get Started",
        secondaryLabel = "Already have an account? Log In",
        onPrimary      = onGetStarted,
        onSecondary    = onLogin
    )
}

// ═══════════════════════════════════════════════════════════════
// 2. Introduction – Provider
// ═══════════════════════════════════════════════════════════════
@Composable
fun IntroProviderScreen(
    onJoinAsProvider: () -> Unit,
    onLogin: () -> Unit
) {
    IntroScaffold(
        tagline        = "Grow your service\nbusiness in Ottawa.",
        description    = "List your services, set your availability, and start receiving bookings from local customers. SmartServe handles the rest.",
        primaryLabel   = "Join as Provider",
        secondaryLabel = "Already a provider? Log In",
        onPrimary      = onJoinAsProvider,
        onSecondary    = onLogin
    )
}

// Shared intro layout
@Composable
private fun IntroScaffold(
    tagline: String,
    description: String,
    primaryLabel: String,
    secondaryLabel: String,
    onPrimary: () -> Unit,
    onSecondary: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Logo placeholder — replace with your actual drawable
        Icon(
            imageVector         = Icons.Default.Home,
            contentDescription  = "SmartServe Logo",
            tint                = MaterialTheme.colorScheme.primary,
            modifier            = Modifier.size(72.dp)
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text       = "SmartServe",
            style      = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color      = MaterialTheme.colorScheme.primary
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text      = tagline,
            style     = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            color     = MaterialTheme.colorScheme.onBackground
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text      = description,
            style     = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color     = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(48.dp))

        Button(
            onClick  = onPrimary,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape    = RoundedCornerShape(12.dp)
        ) {
            Text(primaryLabel, fontSize = 16.sp)
        }

        Spacer(Modifier.height(16.dp))

        TextButton(onClick = onSecondary) {
            Text(secondaryLabel, color = MaterialTheme.colorScheme.primary)
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// 3. Login
// ═══════════════════════════════════════════════════════════════
@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onForgotPassword: () -> Unit,
    onNavigateToCustomerHome: () -> Unit,
    onNavigateToProviderHome: () -> Unit,
    onNavigateToCustomerSetup: (String) -> Unit,
    onNavigateToProviderSetup: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var passwordVisible by remember { mutableStateOf(false) }

    // Handle navigation
    LaunchedEffect(state.navigateTo) {
        when (val dest = state.navigateTo) {
            is AuthNavDestination.CustomerHome          -> { onNavigateToCustomerHome(); viewModel.clearNavigation() }
            is AuthNavDestination.ProviderHome          -> { onNavigateToProviderHome(); viewModel.clearNavigation() }
            is AuthNavDestination.CustomerProfileSetup  -> { onNavigateToCustomerSetup(dest.uid); viewModel.clearNavigation() }
            is AuthNavDestination.ProviderProfileSetup  -> { onNavigateToProviderSetup(dest.uid); viewModel.clearNavigation() }
            null -> {}
        }
    }

    AuthScaffoldColumn {
        AuthHeader(title = "Welcome back", subtitle = "Sign in to your SmartServe account")

        SmartServeTextField(
            value       = state.loginEmail,
            onValueChange = viewModel::onLoginEmailChange,
            label       = "Email",
            leadingIcon = Icons.Default.Email,
            keyboardType = KeyboardType.Email
        )

        SmartServeTextField(
            value         = state.loginPassword,
            onValueChange = viewModel::onLoginPasswordChange,
            label         = "Password",
            leadingIcon   = Icons.Default.Lock,
            keyboardType  = KeyboardType.Password,
            trailingIcon  = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
            onTrailingClick = { passwordVisible = !passwordVisible },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation()
        )

        TextButton(
            onClick  = onForgotPassword,
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Forgot Password?")
        }

        Spacer(Modifier.height(8.dp))

        /*AuthPrimaryButton(
            label     = "Log In",
            isLoading = state.isLoading,
            onClick   = viewModel::login
        )

        AuthDivider()

        GoogleSignInButton(
            onClick = {
                // Trigger your Google Sign-In launcher here and pass idToken to viewModel.signInWithGoogle()
            }
        )*/

      AuthPrimaryButton(
        label     = "Log In",
        isLoading = state.isLoading,
        onClick   = viewModel::login
      )

      AuthDivider()

      OutlinedButton(
        onClick  = {},
        enabled  = false,
        modifier = Modifier
          .fillMaxWidth()
          .height(52.dp),
        shape    = RoundedCornerShape(12.dp)
      ) {
        Text(
          text  = "Continue with Google (coming soon)",
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

        state.errorMessage?.let { msg ->
            ErrorSnackbar(msg) { viewModel.clearError() }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// 4. Sign Up – Customer
// ═══════════════════════════════════════════════════════════════
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

    AuthScaffoldColumn(topBar = { AuthTopBar("Create Account", onBack) }) {
        AuthHeader(
            title    = "Join SmartServe",
            subtitle = "Create your customer account"
        )

        SmartServeTextField(
            value         = state.signUpFullName,
            onValueChange = viewModel::onSignUpNameChange,
            label         = "Full Name",
            leadingIcon   = Icons.Default.Person
        )

        SmartServeTextField(
            value         = state.signUpEmail,
            onValueChange = viewModel::onSignUpEmailChange,
            label         = "Email",
            leadingIcon   = Icons.Default.Email,
            keyboardType  = KeyboardType.Email
        )

        PasswordField(
            value         = state.signUpPassword,
            onValueChange = viewModel::onSignUpPasswordChange,
            label         = "Password"
        )

        PasswordField(
            value         = state.signUpConfirmPassword,
            onValueChange = viewModel::onSignUpConfirmPasswordChange,
            label         = "Confirm Password"
        )

        Spacer(Modifier.height(8.dp))

        /*AuthPrimaryButton(
            label     = "Create Account",
            isLoading = state.isLoading,
            onClick   = viewModel::signUpCustomer
        )

        AuthDivider()

        GoogleSignInButton(onClick = { /* launch Google Sign-In */ })*/

      AuthPrimaryButton(
        label     = "Create Account",
        isLoading = state.isLoading,
        onClick   = viewModel::signUpCustomer
      )

      AuthDivider()

      OutlinedButton(
        onClick  = {},
        enabled  = false,
        modifier = Modifier
          .fillMaxWidth()
          .height(52.dp),
        shape    = RoundedCornerShape(12.dp)
      ) {
        Text(
          text  = "Continue with Google (coming soon)",
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

        state.errorMessage?.let { ErrorSnackbar(it) { viewModel.clearError() } }
    }
}

// ═══════════════════════════════════════════════════════════════
// 5. Sign Up – Provider
// ═══════════════════════════════════════════════════════════════
@Composable
fun SignUpProviderScreen(
    viewModel: AuthViewModel,
    onBack: () -> Unit,
    onNavigateToProfileSetup: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.navigateTo) {
        val dest = state.navigateTo
        if (dest is AuthNavDestination.ProviderProfileSetup) {
            onNavigateToProfileSetup(dest.uid)
            viewModel.clearNavigation()
        }
    }

    AuthScaffoldColumn(topBar = { AuthTopBar("Join as Provider", onBack) }) {
        AuthHeader(
            title    = "Provider Sign Up",
            subtitle = "Create your provider account"
        )

        SmartServeTextField(
            value         = state.signUpFullName,
            onValueChange = viewModel::onSignUpNameChange,
            label         = "Full Name",
            leadingIcon   = Icons.Default.Person
        )

        SmartServeTextField(
            value         = state.signUpEmail,
            onValueChange = viewModel::onSignUpEmailChange,
            label         = "Email",
            leadingIcon   = Icons.Default.Email,
            keyboardType  = KeyboardType.Email
        )

        // Phone is required for providers (SMS OTP verification)
        SmartServeTextField(
            value         = state.signUpPhone,
            onValueChange = viewModel::onSignUpPhoneChange,
            label         = "Phone Number *",
            leadingIcon   = Icons.Default.Phone,
            keyboardType  = KeyboardType.Phone
        )

        PasswordField(
            value         = state.signUpPassword,
            onValueChange = viewModel::onSignUpPasswordChange,
            label         = "Password"
        )

        PasswordField(
            value         = state.signUpConfirmPassword,
            onValueChange = viewModel::onSignUpConfirmPasswordChange,
            label         = "Confirm Password"
        )

        Spacer(Modifier.height(8.dp))

        /*AuthPrimaryButton(
            label     = "Create Account",
            isLoading = state.isLoading,
            onClick   = viewModel::signUpProvider
        )

        AuthDivider()

        GoogleSignInButton(onClick = { /* launch Google Sign-In */ })*/

      AuthPrimaryButton(
        label     = "Create Account",
        isLoading = state.isLoading,
        onClick   = viewModel::signUpProvider
      )

      AuthDivider()

      OutlinedButton(
        onClick  = {},
        enabled  = false,
        modifier = Modifier
          .fillMaxWidth()
          .height(52.dp),
        shape    = RoundedCornerShape(12.dp)
      ) {
        Text(
          text  = "Continue with Google (coming soon)",
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

        state.errorMessage?.let { ErrorSnackbar(it) { viewModel.clearError() } }
    }
}

// ═══════════════════════════════════════════════════════════════
// 6. Forgot Password
// ═══════════════════════════════════════════════════════════════
@Composable
fun ForgotPasswordScreen(
    viewModel: AuthViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    AuthScaffoldColumn(topBar = { AuthTopBar("Reset Password", onBack) }) {
        AuthHeader(
            title    = "Forgot Password?",
            subtitle = "Enter your email and we'll send you a reset link. The link expires after 15 minutes."
        )

        SmartServeTextField(
            value         = state.forgotEmail,
            onValueChange = viewModel::onForgotEmailChange,
            label         = "Email",
            leadingIcon   = Icons.Default.Email,
            keyboardType  = KeyboardType.Email
        )

        Spacer(Modifier.height(8.dp))

        AuthPrimaryButton(
            label     = "Send Reset Link",
            isLoading = state.isLoading,
            onClick   = viewModel::sendPasswordReset
        )

        state.successMessage?.let { msg ->
            Spacer(Modifier.height(16.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Text(
                    text     = msg,
                    modifier = Modifier.padding(16.dp),
                    color    = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        state.errorMessage?.let { ErrorSnackbar(it) { viewModel.clearError() } }
    }
}

// ═══════════════════════════════════════════════════════════════
// 7. Customer Profile Setup
// ═══════════════════════════════════════════════════════════════
@Composable
fun CustomerProfileSetupScreen(
    uid: String,
    viewModel: CustomerProfileViewModel,
    onStart: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.navigateToHome) {
        if (state.navigateToHome) { onStart(); viewModel.clearNavigation() }
    }

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? -> viewModel.onPhotoSelected(uri) }

    AuthScaffoldColumn {
        AuthHeader(
            title    = "Set up your profile",
            subtitle = "Just a few details to personalise your experience"
        )

        // ── Photo Upload ──
        Box(
            modifier          = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                .clickable { photoPicker.launch("image/*") }
                .align(Alignment.CenterHorizontally),
            contentAlignment  = Alignment.Center
        ) {
            if (state.photoUri != null) {
                AsyncImage(
                    model             = state.photoUri,
                    contentDescription = "Profile photo",
                    contentScale      = ContentScale.Crop,
                    modifier          = Modifier.fillMaxSize()
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.AddAPhoto, contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary)
                    Text("Add Photo", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // ── Optional phone ──
        SmartServeTextField(
            value         = state.phone,
            onValueChange = viewModel::onPhoneChange,
            label         = "Phone Number (optional)",
            leadingIcon   = Icons.Default.Phone,
            keyboardType  = KeyboardType.Phone
        )

        // ── Home address — used as default in all future bookings ──
        SmartServeTextField(
            value         = state.homeAddress,
            onValueChange = viewModel::onHomeAddressChange,
            label         = "Home Address *",
            leadingIcon   = Icons.Default.Home
        )

        Text(
            text  = "This address will be pre-filled in all your future bookings.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )

        // ── Toggles ──
        PreferenceToggleRow(
            label    = "Location-based recommendations",
            checked  = state.locationAwareness,
            onChange = viewModel::onLocationToggle
        )

        PreferenceToggleRow(
            label    = "Push notifications",
            checked  = state.pushNotifications,
            onChange = viewModel::onNotifToggle
        )

        Spacer(Modifier.height(16.dp))

        AuthPrimaryButton(
            label     = "Start",
            isLoading = state.isLoading,
            onClick   = { viewModel.saveAndStart(uid) }
        )

        state.errorMessage?.let { ErrorSnackbar(it) {} }
    }
}

// ═══════════════════════════════════════════════════════════════
// 8. Provider Profile Setup
// ═══════════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderProfileSetupScreen(
    uid: String,
    viewModel: ProviderProfileViewModel,
    onStart: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.navigateToHome) {
        if (state.navigateToHome) { onStart(); viewModel.clearNavigation() }
    }

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? -> viewModel.onPhotoSelected(uri) }

    val categories = listOf("home" to "Home Services", "education" to "Education", "studentLife" to "Student Life Services")
    val daysOfWeek = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    AuthScaffoldColumn {
        AuthHeader(
            title    = "Provider Profile",
            subtitle = "Tell customers about your services"
        )

        // ── Photo ──
        Box(
            modifier          = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                .clickable { photoPicker.launch("image/*") }
                .align(Alignment.CenterHorizontally),
            contentAlignment  = Alignment.Center
        ) {
            if (state.photoUri != null) {
                AsyncImage(
                    model              = state.photoUri,
                    contentDescription = "Profile photo",
                    contentScale       = ContentScale.Crop,
                    modifier           = Modifier.fillMaxSize()
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.AddAPhoto, null, tint = MaterialTheme.colorScheme.primary)
                    Text("Add Photo", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── Service Category Dropdown ──
        Text("Service Category *", style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(bottom = 4.dp))

        var categoryExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded  = categoryExpanded,
            onExpandedChange = { categoryExpanded = !categoryExpanded }
        ) {
            OutlinedTextField(
                value         = categories.find { it.first == state.serviceCategory }?.second ?: "",
                onValueChange = {},
                readOnly      = true,
                label         = { Text("Select category") },
                trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                modifier      = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )
            ExposedDropdownMenu(
                expanded  = categoryExpanded,
                onDismissRequest = { categoryExpanded = false }
            ) {
                categories.forEach { (value, label) ->
                    DropdownMenuItem(
                        text    = { Text(label) },
                        onClick = {
                            viewModel.onCategoryChange(value)
                            categoryExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // ── Service Description ──
        OutlinedTextField(
            value         = state.serviceDescription,
            onValueChange = viewModel::onDescriptionChange,
            label         = { Text("Service Description *") },
            minLines      = 3,
            maxLines      = 6,
            modifier      = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        // ── Hourly Rate ──
        SmartServeTextField(
            value         = state.hourlyRate,
            onValueChange = viewModel::onHourlyRateChange,
            label         = "Hourly Rate (CAD) *",
            leadingIcon   = Icons.Default.AttachMoney,
            keyboardType  = KeyboardType.Decimal
        )

        Spacer(Modifier.height(12.dp))

        // ── Service Radius Map ──
        Text("Service Area *", style = MaterialTheme.typography.labelMedium)
        Text(
            "Drag the map to set your service center. Adjust the radius slider.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(8.dp))

        ServiceRadiusMapPicker(
            center   = state.serviceCenter,
            radiusKm = state.serviceRadiusKm,
            onCenterChanged = { latLng ->
                viewModel.onServiceCenterChange(GeoPoint(latLng.latitude, latLng.longitude))
            },
            onRadiusChanged = viewModel::onRadiusChange
        )

        Spacer(Modifier.height(12.dp))

        // ── Availability Days ──
        Text("Availability *", style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            daysOfWeek.forEach { day ->
                val selected = day in state.availabilityDays
                FilterChip(
                    selected = selected,
                    onClick  = {
                        val updated = if (selected) state.availabilityDays - day
                        else state.availabilityDays + day
                        viewModel.onAvailabilityDaysChange(updated)
                    },
                    label = { Text(day) }
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // ── Availability Hours ──
        Row(
            modifier            = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TimePickerTextField(
                label    = "From",
                value    = state.availabilityStart,
                onChange = viewModel::onAvailabilityStartChange,
                modifier = Modifier.weight(1f)
            )
            TimePickerTextField(
                label    = "To",
                value    = state.availabilityEnd,
                onChange = viewModel::onAvailabilityEndChange,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(20.dp))

        AuthPrimaryButton(
            label     = "Start",
            isLoading = state.isLoading,
            onClick   = {
                // fullName and phone already stored in users/{uid} from sign-up
                viewModel.saveAndStart(uid, displayName = "", phone = "")
            }
        )

        state.errorMessage?.let { ErrorSnackbar(it) {} }
    }
}

// ═══════════════════════════════════════════════════════════════
// Reusable Components
// ═══════════════════════════════════════════════════════════════

@Composable
private fun AuthScaffoldColumn(
    topBar: @Composable () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    Scaffold(topBar = topBar) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthTopBar(title: String, onBack: () -> Unit) {
    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        }
    )
}

@Composable
private fun AuthHeader(title: String, subtitle: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(subtitle, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun SmartServeTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text,
    trailingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onTrailingClick: (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    OutlinedTextField(
        value             = value,
        onValueChange     = onValueChange,
        label             = { Text(label) },
        leadingIcon       = { Icon(leadingIcon, contentDescription = null) },
        trailingIcon      = trailingIcon?.let { icon ->
            { IconButton(onClick = { onTrailingClick?.invoke() }) { Icon(icon, null) } }
        },
        visualTransformation = visualTransformation,
        keyboardOptions   = KeyboardOptions(keyboardType = keyboardType),
        singleLine        = true,
        modifier          = Modifier.fillMaxWidth(),
        shape             = RoundedCornerShape(12.dp)
    )
}

@Composable
private fun PasswordField(value: String, onValueChange: (String) -> Unit, label: String) {
    var visible by remember { mutableStateOf(false) }
    SmartServeTextField(
        value                = value,
        onValueChange        = onValueChange,
        label                = label,
        leadingIcon          = Icons.Default.Lock,
        keyboardType         = KeyboardType.Password,
        trailingIcon         = if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
        onTrailingClick      = { visible = !visible },
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation()
    )
}

@Composable
private fun AuthPrimaryButton(label: String, isLoading: Boolean, onClick: () -> Unit) {
    Button(
        onClick  = onClick,
        enabled  = !isLoading,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape    = RoundedCornerShape(12.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color    = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp
            )
        } else {
            Text(label, fontSize = 16.sp)
        }
    }
}

@Composable
private fun AuthDivider() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier          = Modifier.fillMaxWidth()
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f))
        Text("  or  ", color = MaterialTheme.colorScheme.onSurfaceVariant)
        HorizontalDivider(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun GoogleSignInButton(onClick: () -> Unit) {
    OutlinedButton(
        onClick  = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape    = RoundedCornerShape(12.dp)
    ) {
        Icon(
            painter            = painterResource(id = android.R.drawable.ic_menu_search), // swap for actual Google icon
            contentDescription = "Google",
            modifier           = Modifier.size(20.dp),
            tint               = Color.Unspecified
        )
        Spacer(Modifier.width(8.dp))
        Text("Continue with Google")
    }
}

@Composable
private fun PreferenceToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun ErrorSnackbar(message: String, onDismiss: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        shape    = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text(
                text     = message,
                color    = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.onErrorContainer)
            }
        }
    }
}

// ── Google Maps radius picker for Provider Profile Setup ──────
@Composable
private fun ServiceRadiusMapPicker(
    center: GeoPoint?,
    radiusKm: Double,
    onCenterChanged: (LatLng) -> Unit,
    onRadiusChanged: (Double) -> Unit
) {
    val ottawaLatLng = LatLng(45.4215, -75.6972)
    val initialCenter = center?.let { LatLng(it.latitude, it.longitude) } ?: ottawaLatLng

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialCenter, 11f)
    }

    var markerPosition by remember { mutableStateOf(initialCenter) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
        shape    = RoundedCornerShape(12.dp)
    ) {
        GoogleMap(
            modifier             = Modifier.fillMaxSize(),
            cameraPositionState  = cameraPositionState,
            onMapClick           = { latLng ->
                markerPosition = latLng
                onCenterChanged(latLng)
            }
        ) {
            Marker(
                state = MarkerState(position = markerPosition),
                title = "Service Center"
            )
            Circle(
                center      = markerPosition,
                radius      = radiusKm * 1000,
                strokeColor = MaterialTheme.colorScheme.primary,
                fillColor   = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                strokeWidth = 3f
            )
        }
    }

    Spacer(Modifier.height(8.dp))

    Row(
        modifier              = Modifier.fillMaxWidth(),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("Radius", style = MaterialTheme.typography.bodySmall)
        Text("${radiusKm.toInt()} km", style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold)
    }

    Slider(
        value        = radiusKm.toFloat(),
        onValueChange = { onRadiusChanged(it.toDouble()) },
        valueRange   = 1f..50f,
        steps        = 48,
        modifier     = Modifier.fillMaxWidth()
    )
}

@Composable
private fun TimePickerTextField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value         = value,
        onValueChange = onChange,
        label         = { Text(label) },
        placeholder   = { Text("HH:mm") },
        singleLine    = true,
        modifier      = modifier,
        shape         = RoundedCornerShape(12.dp)
    )
}
