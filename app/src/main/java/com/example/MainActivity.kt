package com.example

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocalFlorist
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.data.CircleMember
import com.example.data.CircleStatus
import com.example.data.EmergencyContact
import com.example.data.EsmeryState
import com.example.data.FriendRequest
import com.example.data.InMemoryEsmeryRepository
import com.example.data.Moment
import com.example.data.PRESET_IMAGES
import com.example.data.SafetyRhythm
import com.example.data.SubscriptionPlan
import com.example.data.TimelineEvent
import com.example.data.TimelineEventType
import com.example.ui.theme.Apricot
import com.example.ui.theme.Cocoa
import com.example.ui.theme.Cream
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.Sage
import com.example.ui.theme.Surface
import com.example.ui.theme.Taupe
import java.util.Locale
import kotlinx.coroutines.launch

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

enum class MainTab(val labelRes: Int, val icon: ImageVector) {
  Hearth(R.string.hearth, Icons.Rounded.Home),
  Circle(R.string.circle, Icons.Rounded.Group),
  Timeline(R.string.timeline, Icons.Rounded.Schedule),
  Moments(R.string.moments, Icons.Rounded.LocalFlorist),
  Safety(R.string.safety, Icons.Rounded.Security),
  Crisis(R.string.crisis, Icons.Rounded.Warning),
  Plans(R.string.plans, Icons.Rounded.CreditCard),
}

enum class AppLanguage(val tag: String) {
  English("en"),
  Vietnamese("vi");

  companion object {
    fun fromTag(tag: String): AppLanguage = entries.firstOrNull { it.tag == tag } ?: English
  }
}

private fun AppLanguage.next(): AppLanguage = when (this) {
  AppLanguage.English -> AppLanguage.Vietnamese
  AppLanguage.Vietnamese -> AppLanguage.English
}

@Composable
private fun LanguageButton(language: AppLanguage, onClick: () -> Unit) {
  OutlinedButton(onClick = onClick, shape = RoundedCornerShape(8.dp)) {
    Icon(Icons.Rounded.Translate, contentDescription = null, tint = Cocoa, modifier = Modifier.size(18.dp))
    Spacer(Modifier.width(6.dp))
    Text(stringResource(R.string.language), color = Cocoa, fontWeight = FontWeight.Bold)
  }
}

@Composable
fun EsmeryApp(
  authGateway: AuthGateway = remember { AuthGateway() },
) {
  val navController = rememberNavController()
  val baseContext = LocalContext.current
  var languageTag by rememberSaveable { mutableStateOf(AppLanguage.English.tag) }
  val language = AppLanguage.fromTag(languageTag)
  val localizedContext = remember(baseContext, languageTag) {
    val configuration = Configuration(baseContext.resources.configuration)
    configuration.setLocale(Locale.forLanguageTag(languageTag))
    baseContext.createConfigurationContext(configuration)
  }

  CompositionLocalProvider(
    LocalContext provides localizedContext,
    LocalConfiguration provides localizedContext.resources.configuration,
  ) {
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
        title = "Create My Circle",
        body = "Add at least one trusted contact now, or continue and do it later.",
        primary = "Continue",
        onPrimary = { navController.navigate(Routes.RhythmSetup) },
      )
    }
    composable(Routes.RhythmSetup) {
      SetupScreen(
        title = stringResource(R.string.safety_rhythm),
        body = "Wakeup and bedtime checks are ready. You can edit them from Safety.",
        primary = stringResource(R.string.get_started),
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

@Composable
fun WelcomeScreen(
  modifier: Modifier = Modifier,
  authGateway: AuthGateway = remember { AuthGateway(InMemoryEsmeryRepository()) },
  language: AppLanguage = AppLanguage.English,
  onToggleLanguage: () -> Unit = {},
  onNavigateToSignUp: () -> Unit = {},
  onSignedIn: () -> Unit = {},
) {
  AuthScaffold(modifier = modifier) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
      LanguageButton(language = language, onClick = onToggleLanguage)
    }
    BrandHeader(
      title = stringResource(R.string.welcome_title),
      subtitle = stringResource(R.string.welcome_subtitle),
    )
    EsmeryTextField(value = email, onValueChange = { email = it }, label = stringResource(R.string.email), keyboardType = KeyboardType.Email)
    EsmeryTextField(value = password, onValueChange = { password = it }, label = stringResource(R.string.password), keyboardType = KeyboardType.Password, password = true)
    TextButton(onClick = { message = "Password reset email is a v1 stub." }, modifier = Modifier.align(Alignment.End)) {
      Text(stringResource(R.string.forgot_password), color = Cocoa)
    }
    message?.let { InlineMessage(it) }
    PrimaryButton(
      text = stringResource(R.string.sign_in),
      loading = isLoading,
      onClick = {
        if (email.isBlank() || password.isBlank()) {
          message = "Enter email and password."
          return@PrimaryButton
        }
        isLoading = true
        scope.launch {
          runCatching { authGateway.signIn(email.trim(), password) }
            .onSuccess { onSignedIn() }
            .onFailure { message = it.message ?: "Sign in failed." }
          isLoading = false
        }
      },
    )
    TextButton(onClick = onNavigateToSignUp, modifier = Modifier.fillMaxWidth()) {
      Text("New here? Create an account", color = Cocoa, fontWeight = FontWeight.Bold)
    }
  }
}

@Composable
fun SignUpScreen(
  authGateway: AuthGateway = remember { AuthGateway(InMemoryEsmeryRepository()) },
  language: AppLanguage = AppLanguage.English,
  onToggleLanguage: () -> Unit = {},
  onNavigateBack: () -> Unit = {},
  onSignedUp: () -> Unit = {},
) {
  AuthScaffold {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
      IconButton(onClick = onNavigateBack) {
        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null, tint = Cocoa)
      }
      LanguageButton(language = language, onClick = onToggleLanguage)
    }
    BrandHeader(title = "Start Your Journey", subtitle = "Set up a private safety circle with ESMERY.")
    EsmeryTextField(value = name, onValueChange = { name = it }, label = stringResource(R.string.full_name))
    EsmeryTextField(value = email, onValueChange = { email = it }, label = stringResource(R.string.email), keyboardType = KeyboardType.Email)
    EsmeryTextField(value = password, onValueChange = { password = it }, label = stringResource(R.string.password), keyboardType = KeyboardType.Password, password = true)
    message?.let { InlineMessage(it) }
    PrimaryButton(
      text = stringResource(R.string.sign_up),
      loading = isLoading,
      onClick = {
        if (name.isBlank() || email.isBlank() || password.length < 6) {
          message = "Enter your name, email, and a password with at least 6 characters."
          return@PrimaryButton
        }
        isLoading = true
        scope.launch {
          runCatching { authGateway.signUp(name.trim(), email.trim(), password) }
            .onSuccess { onSignedUp() }
            .onFailure { message = it.message ?: "Sign up failed." }
          isLoading = false
        }
      },
    )
  }
}

@Composable
private fun AuthScaffold(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
  Column(
    modifier = modifier
      .fillMaxSize()
      .background(Cream)
      .padding(horizontal = 24.dp, vertical = 36.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
    content = content,
  )
}

@Composable
private fun BrandHeader(title: String, subtitle: String) {
  Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
    Surface(shape = CircleShape, color = Apricot.copy(alpha = 0.22f), modifier = Modifier.size(64.dp)) {
      Box(contentAlignment = Alignment.Center) {
        Icon(Icons.Rounded.Favorite, contentDescription = null, tint = Apricot, modifier = Modifier.size(34.dp))
      }
    }
    Text("ESMERY", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black, color = Cocoa)
    Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = Cocoa)
    Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = Taupe)
  }
}

@Composable
fun OnboardingPagerScreen(onDone: () -> Unit = {}) {
  val pages = listOf(
    stringResource(R.string.onboarding_one) to "Stay connected without feeling watched.",
    stringResource(R.string.onboarding_two) to "Tap once to tell your trusted people you are safe.",
    stringResource(R.string.onboarding_three) to "Share gentle moments, nudges, and calm safety signals.",
  )
  var page by remember { mutableStateOf(0) }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(Cream)
      .padding(24.dp),
    verticalArrangement = Arrangement.SpaceBetween,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Spacer(Modifier.height(24.dp))
    Card(colors = CardDefaults.cardColors(containerColor = Surface), shape = RoundedCornerShape(8.dp)) {
      Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        Icon(Icons.Rounded.Security, contentDescription = null, tint = Apricot, modifier = Modifier.size(72.dp))
        Text(pages[page].first, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center, fontWeight = FontWeight.Black, color = Cocoa)
        Text(pages[page].second, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, color = Taupe)
      }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      pages.indices.forEach { index ->
        Surface(
          modifier = Modifier.size(width = if (index == page) 28.dp else 10.dp, height = 10.dp),
          shape = CircleShape,
          color = if (index == page) Apricot else Taupe.copy(alpha = 0.25f),
          content = {},
        )
      }
    }
    PrimaryButton(text = if (page == pages.lastIndex) stringResource(R.string.get_started) else "Next") {
      if (page == pages.lastIndex) onDone() else page += 1
    }
  }
}

@Composable
private fun SetupScreen(title: String, body: String, primary: String, onPrimary: () -> Unit) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(Cream)
      .padding(24.dp),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Icon(Icons.Rounded.Group, contentDescription = null, tint = Apricot, modifier = Modifier.size(72.dp))
    Spacer(Modifier.height(20.dp))
    Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = Cocoa, textAlign = TextAlign.Center)
    Spacer(Modifier.height(10.dp))
    Text(body, style = MaterialTheme.typography.bodyLarge, color = Taupe, textAlign = TextAlign.Center)
    Spacer(Modifier.height(28.dp))
    PrimaryButton(text = primary, onClick = onPrimary)
  }
}

@Composable
fun HomeScreen(
  repository: com.example.data.EsmeryRepository = EsmeryServices.repository,
  authGateway: AuthGateway = remember { AuthGateway(InMemoryEsmeryRepository()) },
  language: AppLanguage = AppLanguage.English,
  onToggleLanguage: () -> Unit = {},
  onSignedOut: () -> Unit = {},
) {
  val state by repository.state.collectAsState()
  var selectedTab by remember { mutableStateOf(MainTab.Hearth) }
  val scope = rememberCoroutineScope()
  var toast by remember { mutableStateOf<String?>(null) }

  LaunchedEffect(toast) {
    if (toast != null) {
      kotlinx.coroutines.delay(2200)
      toast = null
    }
  }

  Scaffold(
    bottomBar = {
      Surface(color = Surface, shadowElevation = 8.dp) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
          horizontalArrangement = Arrangement.SpaceAround,
        ) {
          MainTab.entries.forEach { tab ->
            TabButton(tab = tab, selected = selectedTab == tab, onClick = { selectedTab = tab })
          }
        }
      }
    },
    containerColor = Cream,
  ) { padding ->
    Box(modifier = Modifier.padding(padding).fillMaxSize()) {
      when (selectedTab) {
        MainTab.Hearth -> HearthScreen(state, onCheckIn = {
          scope.launch {
            repository.checkIn()
            toast = "Your circle has been notified."
          }
        }, language = language, onToggleLanguage = onToggleLanguage, onLogout = {
          scope.launch {
            authGateway.signOut()
            onSignedOut()
          }
        })
        MainTab.Circle -> CircleScreen(state, repository, onToast = { toast = it })
        MainTab.Timeline -> TimelineScreen(state.timelineEvents)
        MainTab.Moments -> MomentsScreen(state.moments, repository, onToast = { toast = it })
        MainTab.Safety -> SafetyScreen(state, repository, onToast = { toast = it })
        MainTab.Crisis -> CrisisScreen(state, repository, onToast = { toast = it })
        MainTab.Plans -> PlansScreen(state, repository, onToast = { toast = it })
      }
      toast?.let {
        Surface(
          modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(16.dp),
          color = Cocoa,
          shape = RoundedCornerShape(8.dp),
          shadowElevation = 8.dp,
        ) {
          Text(it, color = Cream, modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp), fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

@Composable
private fun TabButton(tab: MainTab, selected: Boolean, onClick: () -> Unit) {
  Column(
    modifier = Modifier
      .width(52.dp)
      .clickable(onClick = onClick)
      .padding(vertical = 4.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Icon(tab.icon, contentDescription = null, tint = if (selected) Apricot else Taupe, modifier = Modifier.size(24.dp))
    Text(stringResource(tab.labelRes), color = if (selected) Cocoa else Taupe, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
  }
}

@Composable
private fun HearthScreen(
  state: EsmeryState,
  onCheckIn: () -> Unit,
  language: AppLanguage,
  onToggleLanguage: () -> Unit,
  onLogout: () -> Unit,
) {
  ScreenList(title = "Good morning, ${state.profile.displayName}", subtitle = "Last check-in: ${friendlyTime(state.profile.lastSafeAt)}") {
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        LanguageButton(language = language, onClick = onToggleLanguage)
        OutlinedButton(onClick = onLogout, shape = RoundedCornerShape(8.dp)) {
          Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null, tint = Cocoa, modifier = Modifier.size(18.dp))
          Spacer(Modifier.width(6.dp))
          Text(stringResource(R.string.logout), color = Cocoa, fontWeight = FontWeight.Bold)
        }
      }
    }
    item {
      Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Button(
          onClick = onCheckIn,
          modifier = Modifier.size(190.dp),
          shape = CircleShape,
          colors = ButtonDefaults.buttonColors(containerColor = Apricot),
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(54.dp))
            Text(stringResource(R.string.im_safe), color = Color.White, fontWeight = FontWeight.Black)
          }
        }
      }
    }
    item {
      InfoCard(icon = Icons.Rounded.NotificationsActive, title = "Safety signal ready", body = stringResource(R.string.circle_notified))
    }
    item {
      InfoCard(icon = Icons.Rounded.Group, title = "Circle health", body = "${state.circleMembers.count { it.status == CircleStatus.Accepted }} trusted people connected.")
    }
  }
}

@Composable
private fun CircleScreen(state: EsmeryState, repository: com.example.data.EsmeryRepository, onToast: (String) -> Unit) {
  var showAdd by remember { mutableStateOf(false) }
  val scope = rememberCoroutineScope()

  ScreenList(title = stringResource(R.string.circle), subtitle = "Trusted people who can receive safety alerts.") {
    item {
      PrimaryButton(text = stringResource(R.string.add_friend), icon = Icons.Rounded.Add) { showAdd = true }
    }
    items(state.friendRequests) { request ->
      FriendRequestCard(request = request, onAccept = {
        scope.launch {
          repository.updateFriendRequest(request.id, CircleStatus.Accepted)
          onToast("Friend request accepted.")
        }
      }, onDecline = {
        scope.launch {
          repository.updateFriendRequest(request.id, CircleStatus.Declined)
          onToast("Friend request declined.")
        }
      })
    }
    items(state.circleMembers) { member ->
      CircleMemberCard(member, onNudge = {
        scope.launch {
          repository.sendNudge(member.id)
          onToast("Gentle nudge sent to ${member.name}.")
        }
      })
    }
  }

  if (showAdd) {
    AddFriendDialog(onDismiss = { showAdd = false }, onAdd = { contact, name, relationship ->
      scope.launch {
        repository.addFriendRequest(contact, name, relationship)
        showAdd = false
        onToast("Invitation sent.")
      }
    })
  }
}

@Composable
private fun MomentsScreen(moments: List<Moment>, repository: com.example.data.EsmeryRepository, onToast: (String) -> Unit) {
  var showAdd by remember { mutableStateOf(false) }
  val scope = rememberCoroutineScope()
  ScreenList(title = stringResource(R.string.moments), subtitle = "Small updates for the people who care.") {
    item { PrimaryButton(text = stringResource(R.string.share_moment), icon = Icons.Rounded.Add) { showAdd = true } }
    items(moments) { moment ->
      InfoCard(icon = Icons.Rounded.LocalFlorist, title = moment.caption, body = "Shared to circle - ${friendlyTime(moment.createdAt)}")
    }
  }
  if (showAdd) {
    MomentDialog(onDismiss = { showAdd = false }, onShare = { caption, image ->
      scope.launch {
        repository.shareMoment(caption, image)
        showAdd = false
        onToast("Moment shared.")
      }
    })
  }
}

@Composable
private fun TimelineScreen(events: List<TimelineEvent>) {
  ScreenList(title = stringResource(R.string.timeline), subtitle = "Your safety history.") {
    items(events) { event ->
      InfoCard(icon = event.type.icon(), title = event.title, body = "${event.body} - ${friendlyTime(event.createdAt)}")
    }
  }
}

@Composable
private fun SafetyScreen(state: EsmeryState, repository: com.example.data.EsmeryRepository, onToast: (String) -> Unit) {
  var label by remember { mutableStateOf("") }
  var time by remember { mutableStateOf("") }
  val scope = rememberCoroutineScope()
  ScreenList(title = stringResource(R.string.safety_rhythm), subtitle = "Reminder and inactivity settings are stored as v1 stubs.") {
    item {
      CardBlock {
        EsmeryTextField(value = label, onValueChange = { label = it }, label = "Check label")
        EsmeryTextField(value = time, onValueChange = { time = it }, label = "Time, e.g. 18:00")
        PrimaryButton(text = "Save rhythm") {
          if (label.isNotBlank() && time.isNotBlank()) {
            scope.launch {
              repository.saveSafetyRhythm(SafetyRhythm(id = "", userId = state.profile.id, label = label, checkTime = time))
              label = ""
              time = ""
              onToast("Safety rhythm saved.")
            }
          }
        }
      }
    }
    items(state.safetyRhythms) { rhythm ->
      InfoCard(icon = Icons.Rounded.Schedule, title = rhythm.label, body = "${rhythm.checkTime} - ${if (rhythm.isEnabled) "enabled" else "paused"}")
    }
    item { InfoCard(icon = Icons.Rounded.Warning, title = "Escalation delay", body = "Stored setting stub: alert emergency contacts after missed check-ins.") }
  }
}

@Composable
private fun CrisisScreen(state: EsmeryState, repository: com.example.data.EsmeryRepository, onToast: (String) -> Unit) {
  var showAdd by remember { mutableStateOf(false) }
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  ScreenList(title = stringResource(R.string.crisis), subtitle = "Fast access to contacts and safe steps.") {
    item { PrimaryButton(text = "Add emergency contact", icon = Icons.Rounded.Add) { showAdd = true } }
    item { InfoCard(icon = Icons.Rounded.Security, title = "My Safe Steps", body = "Pause, move to a safer place, call a trusted contact, then contact local services if needed.") }
    item { InfoCard(icon = Icons.Rounded.Warning, title = "Local support", body = "Nearby police stations and hospitals are placeholders in v1.") }
    items(state.emergencyContacts) { contact ->
      CardBlock {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
          Icon(Icons.Rounded.Phone, contentDescription = null, tint = Apricot)
          Column(modifier = Modifier.weight(1f)) {
            Text(contact.name, fontWeight = FontWeight.Bold, color = Cocoa)
            Text("${contact.contact} - verified: ${contact.isVerified}", color = Taupe)
          }
          IconButton(onClick = {
            runCatching {
              context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${contact.contact}")))
            }.onFailure { onToast("Contact action is unavailable on this device.") }
          }) {
            Icon(Icons.Rounded.Call, contentDescription = null, tint = Cocoa)
          }
        }
      }
    }
  }
  if (showAdd) {
    EmergencyContactDialog(onDismiss = { showAdd = false }, onSave = { name, contact ->
      scope.launch {
        repository.saveEmergencyContact(EmergencyContact(id = "", userId = state.profile.id, name = name, contact = contact))
        showAdd = false
        onToast("Emergency contact saved.")
      }
    })
  }
}

@Composable
private fun PlansScreen(state: EsmeryState, repository: com.example.data.EsmeryRepository, onToast: (String) -> Unit) {
  val scope = rememberCoroutineScope()
  ScreenList(title = stringResource(R.string.plans), subtitle = "Checkout is a functional stub in v1.") {
    item {
      PlanCard("Basic Care", "Free - manual daily check-in, 1 family notification.", state.subscriptionStatus.plan == SubscriptionPlan.Basic) {
        scope.launch { repository.updateSubscription(SubscriptionPlan.Basic); onToast("Basic Care selected.") }
      }
    }
    item {
      PlanCard("Advanced Monthly", "49,000 VND/month - smart inactivity detection and unlimited contacts.", state.subscriptionStatus.plan == SubscriptionPlan.Monthly) {
        scope.launch { repository.updateSubscription(SubscriptionPlan.Monthly); onToast("Monthly plan selected.") }
      }
    }
    item {
      PlanCard("Advanced Yearly", "499,000 VND/year - monthly features plus priority support.", state.subscriptionStatus.plan == SubscriptionPlan.Yearly) {
        scope.launch { repository.updateSubscription(SubscriptionPlan.Yearly); onToast("Yearly plan selected.") }
      }
    }
  }
}

@Composable
private fun PlanCard(title: String, body: String, selected: Boolean, onSelect: () -> Unit) {
  CardBlock(border = if (selected) BorderStroke(2.dp, Apricot) else null) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
      Icon(if (selected) Icons.Rounded.CheckCircle else Icons.Rounded.CreditCard, contentDescription = null, tint = Apricot)
      Column(modifier = Modifier.weight(1f)) {
        Text(title, color = Cocoa, fontWeight = FontWeight.Black)
        Text(body, color = Taupe)
      }
      Button(onClick = onSelect, colors = ButtonDefaults.buttonColors(containerColor = if (selected) Sage else Apricot)) {
        Text(if (selected) "Active" else "Choose", color = if (selected) Cocoa else Color.White)
      }
    }
  }
}

@Composable
private fun CircleMemberCard(member: CircleMember, onNudge: () -> Unit) {
  CardBlock {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
      Surface(shape = CircleShape, color = Sage, modifier = Modifier.size(44.dp)) {
        Box(contentAlignment = Alignment.Center) { Text(member.name.take(1), color = Cocoa, fontWeight = FontWeight.Black) }
      }
      Column(modifier = Modifier.weight(1f)) {
        Text(member.name, color = Cocoa, fontWeight = FontWeight.Bold)
        Text("${member.relationship} - ${member.status.name.lowercase()} - ${friendlyTime(member.lastSafeAt)}", color = Taupe)
      }
      OutlinedButton(onClick = onNudge, shape = RoundedCornerShape(8.dp)) {
        Text("Nudge", color = Cocoa)
      }
    }
  }
}

@Composable
private fun FriendRequestCard(request: FriendRequest, onAccept: () -> Unit, onDecline: () -> Unit) {
  CardBlock {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
      Icon(Icons.Rounded.Group, contentDescription = null, tint = Apricot)
      Column(modifier = Modifier.weight(1f)) {
        Text("Pending request", color = Cocoa, fontWeight = FontWeight.Bold)
        Text(request.receiverContact, color = Taupe)
      }
      IconButton(onClick = onAccept) { Icon(Icons.Rounded.Check, contentDescription = null, tint = Cocoa) }
      IconButton(onClick = onDecline) { Icon(Icons.Rounded.Close, contentDescription = null, tint = Taupe) }
    }
  }
}

@Composable
private fun AddFriendDialog(onDismiss: () -> Unit, onAdd: (String, String, String) -> Unit) {
  var contact by remember { mutableStateOf("") }
  var name by remember { mutableStateOf("") }
  var relationship by remember { mutableStateOf("") }
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(stringResource(R.string.add_friend), color = Cocoa, fontWeight = FontWeight.Black) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        EsmeryTextField(contact, { contact = it }, "Email, phone, or ID")
        EsmeryTextField(name, { name = it }, "Name")
        EsmeryTextField(relationship, { relationship = it }, "Relationship")
      }
    },
    confirmButton = { Button(onClick = { if (contact.isNotBlank()) onAdd(contact, name, relationship) }) { Text("Send") } },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
  )
}

@Composable
private fun MomentDialog(onDismiss: () -> Unit, onShare: (String, String) -> Unit) {
  var caption by remember { mutableStateOf("") }
  var image by remember { mutableStateOf(PRESET_IMAGES.first()) }
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(stringResource(R.string.share_moment), color = Cocoa, fontWeight = FontWeight.Black) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        EsmeryTextField(caption, { caption = it }, "Caption")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          PRESET_IMAGES.forEachIndexed { index, url ->
            FilterChip(selected = image == url, onClick = { image = url }, label = { Text("Image ${index + 1}") })
          }
        }
      }
    },
    confirmButton = { Button(onClick = { if (caption.isNotBlank()) onShare(caption, image) }) { Text("Share") } },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
  )
}

@Composable
private fun EmergencyContactDialog(onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
  var name by remember { mutableStateOf("") }
  var contact by remember { mutableStateOf("") }
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Emergency contact", color = Cocoa, fontWeight = FontWeight.Black) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        EsmeryTextField(name, { name = it }, "Name")
        EsmeryTextField(contact, { contact = it }, "Phone or email")
      }
    },
    confirmButton = { Button(onClick = { if (name.isNotBlank() && contact.isNotBlank()) onSave(name, contact) }) { Text("Save") } },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
  )
}

@Composable
private fun ScreenList(title: String, subtitle: String, content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit) {
  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .background(Cream)
      .padding(horizontal = 18.dp),
    contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 28.dp, bottom = 96.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      Text(title, style = MaterialTheme.typography.headlineSmall, color = Cocoa, fontWeight = FontWeight.Black)
      Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Taupe)
      Spacer(Modifier.height(8.dp))
    }
    content()
  }
}

@Composable
private fun InfoCard(icon: ImageVector, title: String, body: String) {
  CardBlock {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
      Icon(icon, contentDescription = null, tint = Apricot, modifier = Modifier.size(30.dp))
      Column {
        Text(title, color = Cocoa, fontWeight = FontWeight.Black)
        Text(body, color = Taupe, style = MaterialTheme.typography.bodyMedium)
      }
    }
  }
}

@Composable
private fun CardBlock(border: BorderStroke? = null, content: @Composable ColumnScope.() -> Unit) {
  Card(
    colors = CardDefaults.cardColors(containerColor = Surface),
    shape = RoundedCornerShape(8.dp),
    border = border,
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
  }
}

@Composable
private fun EsmeryTextField(
  value: String,
  onValueChange: (String) -> Unit,
  label: String,
  keyboardType: KeyboardType = KeyboardType.Text,
  password: Boolean = false,
) {
  OutlinedTextField(
    value = value,
    onValueChange = onValueChange,
    label = { Text(label) },
    modifier = Modifier.fillMaxWidth(),
    singleLine = true,
    keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
    visualTransformation = if (password) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
  )
}

@Composable
private fun PrimaryButton(
  text: String,
  icon: ImageVector? = null,
  loading: Boolean = false,
  onClick: () -> Unit,
) {
  Button(
    onClick = onClick,
    enabled = !loading,
    colors = ButtonDefaults.buttonColors(containerColor = Apricot),
    shape = RoundedCornerShape(8.dp),
    modifier = Modifier
      .fillMaxWidth()
      .height(54.dp),
  ) {
    if (loading) {
      CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
    } else {
      icon?.let {
        Icon(it, contentDescription = null, tint = Color.White)
        Spacer(Modifier.width(8.dp))
      }
      Text(text, color = Color.White, fontWeight = FontWeight.Black)
    }
  }
}

@Composable
private fun InlineMessage(text: String) {
  Surface(color = Sage.copy(alpha = 0.5f), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
    Text(text, color = Cocoa, modifier = Modifier.padding(12.dp))
  }
}

private fun friendlyTime(value: String?): String {
  if (value.isNullOrBlank()) return "not yet"
  return value.substringAfter('T', value).take(5).ifBlank { value }
}

private fun TimelineEventType.icon(): ImageVector = when (this) {
  TimelineEventType.CheckIn -> Icons.Rounded.CheckCircle
  TimelineEventType.Moment -> Icons.Rounded.LocalFlorist
  TimelineEventType.Nudge -> Icons.Rounded.NotificationsActive
  TimelineEventType.FriendRequest -> Icons.Rounded.Group
  TimelineEventType.SafetyRhythm -> Icons.Rounded.Schedule
  TimelineEventType.Emergency -> Icons.Rounded.Warning
}
