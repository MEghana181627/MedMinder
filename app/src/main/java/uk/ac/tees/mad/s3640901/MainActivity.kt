package uk.ac.tees.mad.s3640901

import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavHostController
import uk.ac.tees.mad.s3640901.ui.theme.MedMinderTheme

class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MedMinderTheme {
                MedMinderApp()
            }
        }
    }
}

private object Routes {
    const val SECURE_ACCESS = "secure_access"
    const val DASHBOARD = "dashboard"
    const val ADD_EDIT = "add_edit"
    const val HISTORY = "history"
}

@Composable
private fun MedMinderApp() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.SECURE_ACCESS
    ) {
        composable(Routes.SECURE_ACCESS) {
            SecureAccessScreen(
                onAuthenticated = {
                    navController.navigate(Routes.DASHBOARD) {
                        popUpTo(Routes.SECURE_ACCESS) {
                            inclusive = true
                        }
                    }
                }
            )
        }

        composable(Routes.DASHBOARD) {
            DashboardScreen(navController)
        }

        composable(Routes.ADD_EDIT) {
            PlaceholderScreen(
                title = "Add/Edit Medication",
                message = "Medication entry will be implemented in Sprint 2.",
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Routes.HISTORY) {
            PlaceholderScreen(
                title = "My History",
                message = "Medication history will be implemented in a later sprint.",
                onBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}

@Composable
private fun SecureAccessScreen(
    onAuthenticated: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    var message by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "MedMinder",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Secure Access",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = 12.dp)
        )

        Text(
            text = "Authenticate to view your medication information.",
            modifier = Modifier.padding(top = 12.dp)
        )

        Button(
            onClick = {
                if (activity != null) {
                    showBiometricPrompt(
                        activity = activity,
                        onSuccess = onAuthenticated,
                        onMessage = { message = it }
                    )
                } else {
                    message = "Authentication is not available."
                }
            },
            modifier = Modifier.padding(top = 24.dp)
        ) {
            Text("Unlock with biometrics")
        }

        if (message.isNotBlank()) {
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}

private fun showBiometricPrompt(
    activity: FragmentActivity,
    onSuccess: () -> Unit,
    onMessage: (String) -> Unit
) {
    val executor = ContextCompat.getMainExecutor(activity)

    val promptBuilder = BiometricPrompt.PromptInfo.Builder()
        .setTitle("Unlock MedMinder")
        .setSubtitle("Authenticate to view your medication information")

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        promptBuilder.setAllowedAuthenticators(
            androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )
    } else {
        promptBuilder
            .setAllowedAuthenticators(
                androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
            )
            .setNegativeButtonText("Cancel")
    }

    val biometricPrompt = BiometricPrompt(
        activity,
        executor,
        object : BiometricPrompt.AuthenticationCallback() {

            override fun onAuthenticationSucceeded(
                result: BiometricPrompt.AuthenticationResult
            ) {
                onSuccess()
            }

            override fun onAuthenticationError(
                errorCode: Int,
                errString: CharSequence
            ) {
                onMessage(errString.toString())
            }

            override fun onAuthenticationFailed() {
                onMessage("Authentication was not recognised. Try again.")
            }
        }
    )

    biometricPrompt.authenticate(promptBuilder.build())
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardScreen(
    navController: NavHostController
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Medication Dashboard")
                }
            )
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "No active medication",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Add a medication to begin managing your schedule.",
                modifier = Modifier.padding(top = 12.dp)
            )

            Button(
                onClick = {
                    navController.navigate(Routes.ADD_EDIT)
                },
                modifier = Modifier.padding(top = 24.dp)
            ) {
                Text("Add Medication")
            }

            OutlinedButton(
                onClick = {
                    navController.navigate(Routes.HISTORY)
                },
                modifier = Modifier.padding(top = 12.dp)
            ) {
                Text("My History")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaceholderScreen(
    title: String,
    message: String,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(title)
                }
            )
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge
            )

            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.padding(top = 24.dp)
            ) {
                Text("Back to Dashboard")
            }
        }
    }
}