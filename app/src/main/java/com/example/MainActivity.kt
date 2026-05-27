package com.example

import android.content.Intent
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
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

private val LocalAppLanguage = compositionLocalOf { AppLanguage.English }

private fun AppLanguage.next(): AppLanguage = when (this) {
  AppLanguage.English -> AppLanguage.Vietnamese
  AppLanguage.Vietnamese -> AppLanguage.English
}

@Composable
private fun LanguageButton(language: AppLanguage, onClick: () -> Unit) {
  OutlinedButton(onClick = onClick, shape = RoundedCornerShape(8.dp)) {
    Icon(Icons.Rounded.Translate, contentDescription = null, tint = Cocoa, modifier = Modifier.size(18.dp))
    Spacer(Modifier.width(6.dp))
    Text(if (language == AppLanguage.English) "Tiếng Việt" else "English", color = Cocoa, fontWeight = FontWeight.Bold)
  }
}

@Composable
private fun esmeryString(id: Int): String {
  val language = LocalAppLanguage.current
  if (language == AppLanguage.English) return stringResource(id)
  return when (id) {
    R.string.sign_in -> "Đăng nhập"
    R.string.sign_up -> "Tạo tài khoản"
    R.string.email -> "Email"
    R.string.password -> "Mật khẩu"
    R.string.full_name -> "Họ tên"
    R.string.forgot_password -> "Quên mật khẩu?"
    R.string.welcome_title -> "Một bàn tay dịu dàng luôn ở bên"
    R.string.welcome_subtitle -> "Xác nhận an toàn cho người sống độc lập."
    R.string.onboarding_one -> "Một bàn tay dịu dàng luôn ở bên"
    R.string.onboarding_two -> "Xác nhận an toàn thật đơn giản"
    R.string.onboarding_three -> "Cảm nhận sự ấm áp"
    R.string.get_started -> "Bắt đầu"
    R.string.im_safe -> "Tôi an toàn"
    R.string.circle_notified -> "Vòng thân của bạn đã được thông báo."
    R.string.hearth -> "Mái ấm"
    R.string.circle -> "Vòng thân"
    R.string.timeline -> "Dòng thời gian"
    R.string.moments -> "Khoảnh khắc"
    R.string.safety -> "An toàn"
    R.string.crisis -> "Khẩn cấp"
    R.string.plans -> "Gói dịch vụ"
    R.string.add_friend -> "Thêm bạn"
    R.string.share_moment -> "Chia sẻ khoảnh khắc"
    R.string.emergency_contacts -> "Liên hệ khẩn cấp"
    R.string.safety_rhythm -> "Nhịp an toàn"
    R.string.logout -> "Đăng xuất"
    else -> stringResource(id)
  }
}

@Composable
private fun t(en: String, vi: String): String = if (LocalAppLanguage.current == AppLanguage.English) en else vi

private fun tr(language: AppLanguage, en: String, vi: String): String = if (language == AppLanguage.English) en else vi

@Composable
private fun localizedEventText(value: String): String = when (value) {
  "Check-in confirmed" -> t("Check-in confirmed", "Đã xác nhận an toàn")
  "Your circle has been notified." -> t("Your circle has been notified.", "Vòng thân của bạn đã được thông báo.")
  "Circle invitation sent" -> t("Circle invitation sent", "Đã gửi lời mời vào vòng thân")
  "Circle invitation accepted" -> t("Circle invitation accepted", "Đã chấp nhận lời mời")
  "Circle invitation declined" -> t("Circle invitation declined", "Đã từ chối lời mời")
  "Request status updated." -> t("Request status updated.", "Trạng thái lời mời đã được cập nhật.")
  "Gentle nudge sent" -> t("Gentle nudge sent", "Đã gửi nhắc nhở nhẹ nhàng")
  "Moment shared" -> t("Moment shared", "Đã chia sẻ khoảnh khắc")
  "Emergency contact saved" -> t("Emergency contact saved", "Đã lưu liên hệ khẩn cấp")
  "Safety rhythm updated" -> t("Safety rhythm updated", "Đã cập nhật nhịp an toàn")
  "Morning check-in" -> t("Morning check-in", "Xác nhận an toàn buổi sáng")
  "Automatic safety heartbeat sent." -> t("Automatic safety heartbeat sent.", "Tín hiệu an toàn tự động đã được gửi.")
  "Morning coffee ritual" -> t("Morning coffee ritual", "Thói quen cà phê sáng")
  else -> value
}

@Composable
private fun localizedRelationship(value: String): String = when (value) {
  "Best friend" -> t("Best friend", "Bạn thân")
  "Family" -> t("Family", "Gia đình")
  "Trusted contact" -> t("Trusted contact", "Liên hệ tin cậy")
  else -> value
}

@Composable
private fun localizedRhythmLabel(value: String): String = when (value) {
  "Wakeup Check" -> t("Wakeup Check", "Xác nhận khi thức dậy")
  "Bedtime Check" -> t("Bedtime Check", "Xác nhận trước khi ngủ")
  else -> value
}

@Composable
private fun localizedCircleStatus(status: CircleStatus): String = when (status) {
  CircleStatus.Pending -> t("pending", "đang chờ")
  CircleStatus.Accepted -> t("accepted", "đã chấp nhận")
  CircleStatus.Declined -> t("declined", "đã từ chối")
}

@Composable
fun EsmeryApp(
  authGateway: AuthGateway = remember { AuthGateway() },
) {
  val navController = rememberNavController()
  var languageTag by rememberSaveable { mutableStateOf(AppLanguage.English.tag) }
  val language = AppLanguage.fromTag(languageTag)

  androidx.compose.runtime.CompositionLocalProvider(LocalAppLanguage provides language) {
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
        title = esmeryString(R.string.safety_rhythm),
        body = t(
          "Wakeup and bedtime checks are ready. You can edit them from Safety.",
          "Lịch xác nhận khi thức dậy và trước khi ngủ đã sẵn sàng. Bạn có thể chỉnh trong mục An toàn.",
        ),
        primary = esmeryString(R.string.get_started),
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
    val isEnglish = LocalAppLanguage.current == AppLanguage.English
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
      LanguageButton(language = language, onClick = onToggleLanguage)
    }
    BrandHeader(
      title = esmeryString(R.string.welcome_title),
      subtitle = esmeryString(R.string.welcome_subtitle),
    )
    EsmeryTextField(value = email, onValueChange = { email = it }, label = esmeryString(R.string.email), keyboardType = KeyboardType.Email)
    EsmeryTextField(value = password, onValueChange = { password = it }, label = esmeryString(R.string.password), keyboardType = KeyboardType.Password, password = true)
    TextButton(onClick = { message = if (isEnglish) "Password reset email is a v1 stub." else "Email đặt lại mật khẩu là mô phỏng ở bản v1." }, modifier = Modifier.align(Alignment.End)) {
      Text(esmeryString(R.string.forgot_password), color = Cocoa)
    }
    message?.let { InlineMessage(it) }
    PrimaryButton(
      text = esmeryString(R.string.sign_in),
      loading = isLoading,
      onClick = {
        if (email.isBlank() || password.isBlank()) {
          message = if (isEnglish) "Enter email and password." else "Nhập email và mật khẩu."
          return@PrimaryButton
        }
        isLoading = true
        scope.launch {
          runCatching { authGateway.signIn(email.trim(), password) }
            .onSuccess { onSignedIn() }
            .onFailure { message = it.message ?: if (isEnglish) "Sign in failed." else "Đăng nhập thất bại." }
          isLoading = false
        }
      },
    )
    TextButton(onClick = onNavigateToSignUp, modifier = Modifier.fillMaxWidth()) {
      Text(t("New here? Create an account", "Bạn mới ở đây? Tạo tài khoản"), color = Cocoa, fontWeight = FontWeight.Bold)
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
    val isEnglish = LocalAppLanguage.current == AppLanguage.English
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
    BrandHeader(
      title = t("Start Your Journey", "Bắt đầu hành trình của bạn"),
      subtitle = t("Set up a private safety circle with ESMERY.", "Thiết lập vòng thân an toàn riêng tư cùng ESMERY."),
    )
    EsmeryTextField(value = name, onValueChange = { name = it }, label = esmeryString(R.string.full_name))
    EsmeryTextField(value = email, onValueChange = { email = it }, label = esmeryString(R.string.email), keyboardType = KeyboardType.Email)
    EsmeryTextField(value = password, onValueChange = { password = it }, label = esmeryString(R.string.password), keyboardType = KeyboardType.Password, password = true)
    message?.let { InlineMessage(it) }
    PrimaryButton(
      text = esmeryString(R.string.sign_up),
      loading = isLoading,
      onClick = {
        if (name.isBlank() || email.isBlank() || password.length < 6) {
          message = if (isEnglish) {
            "Enter your name, email, and a password with at least 6 characters."
          } else {
            "Nhập họ tên, email và mật khẩu có ít nhất 6 ký tự."
          }
          return@PrimaryButton
        }
        isLoading = true
        scope.launch {
          runCatching { authGateway.signUp(name.trim(), email.trim(), password) }
            .onSuccess { onSignedUp() }
            .onFailure { message = it.message ?: if (isEnglish) "Sign up failed." else "Đăng ký thất bại." }
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
    esmeryString(R.string.onboarding_one) to t("Stay connected without feeling watched.", "Giữ kết nối mà không có cảm giác bị theo dõi."),
    esmeryString(R.string.onboarding_two) to t("Tap once to tell your trusted people you are safe.", "Chạm một lần để báo cho người tin cậy rằng bạn vẫn an toàn."),
    esmeryString(R.string.onboarding_three) to t("Share gentle moments, nudges, and calm safety signals.", "Chia sẻ khoảnh khắc, nhắc nhở nhẹ nhàng và tín hiệu an toàn bình yên."),
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
    PrimaryButton(text = if (page == pages.lastIndex) esmeryString(R.string.get_started) else t("Next", "Tiếp theo")) {
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
  val circleNotifiedToast = t("Your circle has been notified.", "Vòng thân của bạn đã được thông báo.")

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
            toast = circleNotifiedToast
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
    Text(esmeryString(tab.labelRes), color = if (selected) Cocoa else Taupe, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
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
  ScreenList(
    title = t("Good morning, ${state.profile.displayName}", "Chào buổi sáng, ${state.profile.displayName}"),
    subtitle = t("Last check-in: ${friendlyTimeText(state.profile.lastSafeAt)}", "Lần xác nhận gần nhất: ${friendlyTimeText(state.profile.lastSafeAt)}"),
  ) {
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
          Text(esmeryString(R.string.logout), color = Cocoa, fontWeight = FontWeight.Bold)
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
            Text(esmeryString(R.string.im_safe), color = Color.White, fontWeight = FontWeight.Black)
          }
        }
      }
    }
    item {
      InfoCard(icon = Icons.Rounded.NotificationsActive, title = t("Safety signal ready", "Tín hiệu an toàn đã sẵn sàng"), body = esmeryString(R.string.circle_notified))
    }
    item {
      val count = state.circleMembers.count { it.status == CircleStatus.Accepted }
      InfoCard(
        icon = Icons.Rounded.Group,
        title = t("Circle health", "Tình trạng vòng thân"),
        body = t("$count trusted people connected.", "$count người tin cậy đang kết nối."),
      )
    }
  }
}

@Composable
private fun CircleScreen(state: EsmeryState, repository: com.example.data.EsmeryRepository, onToast: (String) -> Unit) {
  var showAdd by remember { mutableStateOf(false) }
  val scope = rememberCoroutineScope()
  val language = LocalAppLanguage.current
  val acceptedMessage = t("Friend request accepted.", "Đã chấp nhận lời mời.")
  val declinedMessage = t("Friend request declined.", "Đã từ chối lời mời.")
  val invitationSentMessage = t("Invitation sent.", "Đã gửi lời mời.")

  ScreenList(title = esmeryString(R.string.circle), subtitle = t("Trusted people who can receive safety alerts.", "Những người tin cậy có thể nhận cảnh báo an toàn.")) {
    item {
      PrimaryButton(text = esmeryString(R.string.add_friend), icon = Icons.Rounded.Add) { showAdd = true }
    }
    items(state.friendRequests) { request ->
      FriendRequestCard(request = request, onAccept = {
        scope.launch {
          repository.updateFriendRequest(request.id, CircleStatus.Accepted)
          onToast(acceptedMessage)
        }
      }, onDecline = {
        scope.launch {
          repository.updateFriendRequest(request.id, CircleStatus.Declined)
          onToast(declinedMessage)
        }
      })
    }
    items(state.circleMembers) { member ->
      CircleMemberCard(member, onNudge = {
        scope.launch {
          repository.sendNudge(member.id)
          onToast(tr(language, "Gentle nudge sent to ${member.name}.", "Đã gửi nhắc nhở nhẹ nhàng cho ${member.name}."))
        }
      })
    }
  }

  if (showAdd) {
    AddFriendDialog(onDismiss = { showAdd = false }, onAdd = { contact, name, relationship ->
      scope.launch {
        repository.addFriendRequest(contact, name, relationship)
        showAdd = false
        onToast(invitationSentMessage)
      }
    })
  }
}

@Composable
private fun MomentsScreen(moments: List<Moment>, repository: com.example.data.EsmeryRepository, onToast: (String) -> Unit) {
  var showAdd by remember { mutableStateOf(false) }
  val scope = rememberCoroutineScope()
  val sharedMessage = t("Moment shared.", "Đã chia sẻ khoảnh khắc.")
  ScreenList(title = esmeryString(R.string.moments), subtitle = t("Small updates for the people who care.", "Những cập nhật nhỏ dành cho người quan tâm bạn.")) {
    item { PrimaryButton(text = esmeryString(R.string.share_moment), icon = Icons.Rounded.Add) { showAdd = true } }
    items(moments) { moment ->
      InfoCard(
        icon = Icons.Rounded.LocalFlorist,
        title = localizedEventText(moment.caption),
        body = t("Shared to circle - ${friendlyTimeText(moment.createdAt)}", "Đã chia sẻ với vòng thân - ${friendlyTimeText(moment.createdAt)}"),
      )
    }
  }
  if (showAdd) {
    MomentDialog(onDismiss = { showAdd = false }, onShare = { caption, image ->
      scope.launch {
        repository.shareMoment(caption, image)
        showAdd = false
        onToast(sharedMessage)
      }
    })
  }
}

@Composable
private fun TimelineScreen(events: List<TimelineEvent>) {
  ScreenList(title = esmeryString(R.string.timeline), subtitle = t("Your safety history.", "Lịch sử an toàn của bạn.")) {
    items(events) { event ->
      InfoCard(
        icon = event.type.icon(),
        title = localizedEventText(event.title),
        body = "${localizedEventText(event.body)} - ${friendlyTimeText(event.createdAt)}",
      )
    }
  }
}

@Composable
private fun SafetyScreen(state: EsmeryState, repository: com.example.data.EsmeryRepository, onToast: (String) -> Unit) {
  var label by remember { mutableStateOf("") }
  var time by remember { mutableStateOf("") }
  val scope = rememberCoroutineScope()
  val savedMessage = t("Safety rhythm saved.", "Đã lưu nhịp an toàn.")
  ScreenList(title = esmeryString(R.string.safety_rhythm), subtitle = t("Reminder and inactivity settings are stored as v1 stubs.", "Cài đặt nhắc nhở và phát hiện không hoạt động đang được lưu mô phỏng ở bản v1.")) {
    item {
      CardBlock {
        EsmeryTextField(value = label, onValueChange = { label = it }, label = t("Check label", "Tên lịch xác nhận"))
        EsmeryTextField(value = time, onValueChange = { time = it }, label = t("Time, e.g. 18:00", "Thời gian, ví dụ 18:00"))
        PrimaryButton(text = t("Save rhythm", "Lưu nhịp an toàn")) {
          if (label.isNotBlank() && time.isNotBlank()) {
            scope.launch {
              repository.saveSafetyRhythm(SafetyRhythm(id = "", userId = state.profile.id, label = label, checkTime = time))
              label = ""
              time = ""
              onToast(savedMessage)
            }
          }
        }
      }
    }
    items(state.safetyRhythms) { rhythm ->
      InfoCard(
        icon = Icons.Rounded.Schedule,
        title = localizedRhythmLabel(rhythm.label),
        body = "${rhythm.checkTime} - ${if (rhythm.isEnabled) t("enabled", "đang bật") else t("paused", "đang tạm dừng")}",
      )
    }
    item {
      InfoCard(
        icon = Icons.Rounded.Warning,
        title = t("Escalation delay", "Thời gian chờ trước cảnh báo"),
        body = t(
          "Stored setting stub: alert emergency contacts after missed check-ins.",
          "Cài đặt mô phỏng: cảnh báo liên hệ khẩn cấp sau khi bỏ lỡ xác nhận an toàn.",
        ),
      )
    }
  }
}

@Composable
private fun CrisisScreen(state: EsmeryState, repository: com.example.data.EsmeryRepository, onToast: (String) -> Unit) {
  var showAdd by remember { mutableStateOf(false) }
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val language = LocalAppLanguage.current
  val unavailableMessage = t("Contact action is unavailable on this device.", "Thiết bị này không mở được thao tác liên hệ.")
  val savedMessage = t("Emergency contact saved.", "Đã lưu liên hệ khẩn cấp.")
  ScreenList(title = esmeryString(R.string.crisis), subtitle = t("Fast access to contacts and safe steps.", "Truy cập nhanh liên hệ và các bước an toàn.")) {
    item { PrimaryButton(text = t("Add emergency contact", "Thêm liên hệ khẩn cấp"), icon = Icons.Rounded.Add) { showAdd = true } }
    item {
      InfoCard(
        icon = Icons.Rounded.Security,
        title = t("My Safe Steps", "Các bước an toàn của tôi"),
        body = t(
          "Pause, move to a safer place, call a trusted contact, then contact local services if needed.",
          "Dừng lại, di chuyển đến nơi an toàn hơn, gọi người tin cậy, rồi liên hệ dịch vụ địa phương nếu cần.",
        ),
      )
    }
    item {
      InfoCard(
        icon = Icons.Rounded.Warning,
        title = t("Local support", "Hỗ trợ địa phương"),
        body = t("Nearby police stations and hospitals are placeholders in v1.", "Đồn công an và bệnh viện gần đây là dữ liệu mô phỏng ở bản v1."),
      )
    }
    items(state.emergencyContacts) { contact ->
      CardBlock {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
          Icon(Icons.Rounded.Phone, contentDescription = null, tint = Apricot)
          Column(modifier = Modifier.weight(1f)) {
            Text(contact.name, fontWeight = FontWeight.Bold, color = Cocoa)
            val verified = tr(language, if (contact.isVerified) "yes" else "no", if (contact.isVerified) "có" else "không")
            Text("${contact.contact} - ${tr(language, "verified", "đã xác minh")}: $verified", color = Taupe)
          }
          IconButton(onClick = {
            runCatching {
              context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${contact.contact}")))
            }.onFailure { onToast(unavailableMessage) }
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
        onToast(savedMessage)
      }
    })
  }
}

@Composable
private fun PlansScreen(state: EsmeryState, repository: com.example.data.EsmeryRepository, onToast: (String) -> Unit) {
  val scope = rememberCoroutineScope()
  val basicSelected = t("Basic Care selected.", "Đã chọn gói Chăm sóc cơ bản.")
  val monthlySelected = t("Monthly plan selected.", "Đã chọn gói tháng.")
  val yearlySelected = t("Yearly plan selected.", "Đã chọn gói năm.")
  ScreenList(title = esmeryString(R.string.plans), subtitle = t("Checkout is a functional stub in v1.", "Thanh toán đang được mô phỏng ở bản v1.")) {
    item {
      PlanCard(
        t("Basic Care", "Chăm sóc cơ bản"),
        t("Free - manual daily check-in, 1 family notification.", "Miễn phí - check-in thủ công hằng ngày, thông báo cho 1 người thân."),
        state.subscriptionStatus.plan == SubscriptionPlan.Basic,
      ) {
        scope.launch { repository.updateSubscription(SubscriptionPlan.Basic); onToast(basicSelected) }
      }
    }
    item {
      PlanCard(
        t("Advanced Monthly", "Nâng cao theo tháng"),
        t("49,000 VND/month - smart inactivity detection and unlimited contacts.", "49.000 VND/tháng - phát hiện không hoạt động thông minh và không giới hạn liên hệ."),
        state.subscriptionStatus.plan == SubscriptionPlan.Monthly,
      ) {
        scope.launch { repository.updateSubscription(SubscriptionPlan.Monthly); onToast(monthlySelected) }
      }
    }
    item {
      PlanCard(
        t("Advanced Yearly", "Nâng cao theo năm"),
        t("499,000 VND/year - monthly features plus priority support.", "499.000 VND/năm - gồm tính năng gói tháng và hỗ trợ ưu tiên."),
        state.subscriptionStatus.plan == SubscriptionPlan.Yearly,
      ) {
        scope.launch { repository.updateSubscription(SubscriptionPlan.Yearly); onToast(yearlySelected) }
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
        Text(if (selected) t("Active", "Đang dùng") else t("Choose", "Chọn"), color = if (selected) Cocoa else Color.White)
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
        Text("${localizedRelationship(member.relationship)} - ${localizedCircleStatus(member.status)} - ${friendlyTimeText(member.lastSafeAt)}", color = Taupe)
      }
      OutlinedButton(onClick = onNudge, shape = RoundedCornerShape(8.dp)) {
        Text(t("Nudge", "Nhắc nhẹ"), color = Cocoa)
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
        Text(t("Pending request", "Lời mời đang chờ"), color = Cocoa, fontWeight = FontWeight.Bold)
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
    title = { Text(esmeryString(R.string.add_friend), color = Cocoa, fontWeight = FontWeight.Black) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        EsmeryTextField(contact, { contact = it }, t("Email, phone, or ID", "Email, số điện thoại hoặc ID"))
        EsmeryTextField(name, { name = it }, t("Name", "Tên"))
        EsmeryTextField(relationship, { relationship = it }, t("Relationship", "Mối quan hệ"))
      }
    },
    confirmButton = { Button(onClick = { if (contact.isNotBlank()) onAdd(contact, name, relationship) }) { Text(t("Send", "Gửi")) } },
    dismissButton = { TextButton(onClick = onDismiss) { Text(t("Cancel", "Hủy")) } },
  )
}

@Composable
private fun MomentDialog(onDismiss: () -> Unit, onShare: (String, String) -> Unit) {
  var caption by remember { mutableStateOf("") }
  var image by remember { mutableStateOf(PRESET_IMAGES.first()) }
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(esmeryString(R.string.share_moment), color = Cocoa, fontWeight = FontWeight.Black) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        EsmeryTextField(caption, { caption = it }, t("Caption", "Chú thích"))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          PRESET_IMAGES.forEachIndexed { index, url ->
            FilterChip(selected = image == url, onClick = { image = url }, label = { Text(t("Image ${index + 1}", "Ảnh ${index + 1}")) })
          }
        }
      }
    },
    confirmButton = { Button(onClick = { if (caption.isNotBlank()) onShare(caption, image) }) { Text(t("Share", "Chia sẻ")) } },
    dismissButton = { TextButton(onClick = onDismiss) { Text(t("Cancel", "Hủy")) } },
  )
}

@Composable
private fun EmergencyContactDialog(onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
  var name by remember { mutableStateOf("") }
  var contact by remember { mutableStateOf("") }
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(t("Emergency contact", "Liên hệ khẩn cấp"), color = Cocoa, fontWeight = FontWeight.Black) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        EsmeryTextField(name, { name = it }, t("Name", "Tên"))
        EsmeryTextField(contact, { contact = it }, t("Phone or email", "Số điện thoại hoặc email"))
      }
    },
    confirmButton = { Button(onClick = { if (name.isNotBlank() && contact.isNotBlank()) onSave(name, contact) }) { Text(t("Save", "Lưu")) } },
    dismissButton = { TextButton(onClick = onDismiss) { Text(t("Cancel", "Hủy")) } },
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

@Composable
private fun friendlyTimeText(value: String?): String {
  if (value.isNullOrBlank()) return t("not yet", "chưa có")
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
