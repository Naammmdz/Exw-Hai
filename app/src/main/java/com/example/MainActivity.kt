package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.automation.DeviceTokenBootstrap
import com.example.automation.EsmeryNotificationChannels
import com.example.automation.SafetyAutomationScheduler
import com.example.core.i18n.AppLanguage
import com.example.core.i18n.LocalAppLanguage
import com.example.core.i18n.appString
import com.example.core.i18n.next
import com.example.feature.auth.SignUpScreen
import com.example.feature.auth.WelcomeScreen
import com.example.feature.home.HomeScreen
import com.example.feature.onboarding.CircleSetupScreen
import com.example.feature.onboarding.OnboardingPagerScreen
import com.example.feature.onboarding.RhythmSetupScreen
import com.example.ui.theme.MyApplicationTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    EsmeryNotificationChannels.ensure(this)
    SafetyAutomationScheduler.schedule(this)
    lifecycleScope.launch {
      DeviceTokenBootstrap.registerFirebaseToken(EsmeryServices.repository)
    }
    setContent {
      MyApplicationTheme {
        NotificationPermissionRequest()
        EsmeryApp()
      }
    }
  }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun NotificationPermissionRequest() {
  if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
  val permissionState = rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
  LaunchedEffect(Unit) {
    if (!permissionState.status.isGranted) {
      permissionState.launchPermissionRequest()
    }
  }
}

private object Routes {
  const val SignIn = "signin"
  const val SignUp = "signup"
  const val Onboarding = "onboarding"
  const val CircleSetup = "circleSetup"
  const val RhythmSetup = "rhythmSetup"
  const val Home = "home"
}

@Composable
fun EsmeryApp(
  authGateway: AuthGateway = remember { AuthGateway() },
) {
  val navController = rememberNavController()
  var languageTag by rememberSaveable { mutableStateOf(AppLanguage.English.tag) }
  val language = AppLanguage.fromTag(languageTag)

  CompositionLocalProvider(LocalAppLanguage provides language) {
    NavHost(navController = navController, startDestination = Routes.SignIn) {
      composable(Routes.SignIn) {
        WelcomeScreen(
          authGateway = authGateway,
          language = language,
          onToggleLanguage = { languageTag = language.next().tag },
          onNavigateToSignUp = { navController.navigate(Routes.SignUp) },
          onSignedIn = {
            navController.navigate(Routes.Home) {
              popUpTo(Routes.SignIn) { inclusive = true }
            }
          },
        )
      }
      composable(Routes.SignUp) {
        SignUpScreen(
          authGateway = authGateway,
          language = language,
          onToggleLanguage = { languageTag = language.next().tag },
          onNavigateBack = { navController.popBackStack() },
          onSignedUp = { navController.navigate(Routes.Onboarding) },
        )
      }
      composable(Routes.Onboarding) {
        OnboardingPagerScreen(onDone = { navController.navigate(Routes.CircleSetup) })
      }
      composable(Routes.CircleSetup) {
        CircleSetupScreen(
          onContinue = { navController.navigate(Routes.RhythmSetup) },
          onSkip = { navController.navigate(Routes.RhythmSetup) },
        )
      }
      composable(Routes.RhythmSetup) {
        RhythmSetupScreen(
          onContinue = {
            navController.navigate(Routes.Home) {
              popUpTo(Routes.SignIn) { inclusive = true }
            }
          },
          onSkip = {
            navController.navigate(Routes.Home) {
              popUpTo(Routes.SignIn) { inclusive = true }
            }
          },
        )
      }
      composable(Routes.Home) {
        HomeScreen(
          authGateway = authGateway,
          language = language,
          onToggleLanguage = { languageTag = language.next().tag },
          onSignedOut = {
            navController.navigate(Routes.SignIn) {
              popUpTo(Routes.Home) { inclusive = true }
            }
          },
        )
      }
    }
  }
}
