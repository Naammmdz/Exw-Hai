package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.core.i18n.AppLanguage
import com.example.core.i18n.LocalAppLanguage
import com.example.core.i18n.appString
import com.example.core.i18n.next
import com.example.core.i18n.t
import com.example.feature.auth.SignUpScreen
import com.example.feature.auth.WelcomeScreen
import com.example.feature.home.HomeScreen
import com.example.feature.onboarding.OnboardingPagerScreen
import com.example.feature.onboarding.SetupScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        EsmeryApp()
      }
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
        SetupScreen(
          title = t("Create My Circle", "Tạo vòng thân của tôi"),
          body = t(
            "Add at least one trusted contact now, or continue and do it later.",
            "Thêm ít nhất một liên hệ tin cậy ngay bây giờ, hoặc tiếp tục và thêm sau.",
          ),
          primary = t("Continue", "Tiếp tục"),
          onPrimary = { navController.navigate(Routes.RhythmSetup) },
        )
      }
      composable(Routes.RhythmSetup) {
        SetupScreen(
          title = appString(R.string.safety_rhythm),
          body = t(
            "Wakeup and bedtime checks are ready. You can edit them from Safety.",
            "Lịch xác nhận khi thức dậy và trước khi ngủ đã sẵn sàng. Bạn có thể chỉnh trong mục An toàn.",
          ),
          primary = appString(R.string.get_started),
          onPrimary = {
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
