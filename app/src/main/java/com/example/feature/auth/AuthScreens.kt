package com.example.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.AuthGateway
import com.example.R
import com.example.core.i18n.AppLanguage
import com.example.core.i18n.LocalAppLanguage
import com.example.core.i18n.appString
import com.example.core.i18n.t
import com.example.core.ui.EsmeryTextField
import com.example.core.ui.InlineMessage
import com.example.core.ui.LanguageButton
import com.example.core.ui.PrimaryButton
import com.example.data.InMemoryEsmeryRepository
import com.example.ui.theme.Apricot
import com.example.ui.theme.Cocoa
import com.example.ui.theme.Cream
import com.example.ui.theme.Taupe
import kotlinx.coroutines.launch

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
    BrandHeader(title = appString(R.string.welcome_title), subtitle = appString(R.string.welcome_subtitle))
    EsmeryTextField(value = email, onValueChange = { email = it }, label = appString(R.string.email), keyboardType = KeyboardType.Email)
    EsmeryTextField(value = password, onValueChange = { password = it }, label = appString(R.string.password), keyboardType = KeyboardType.Password, password = true)
    TextButton(onClick = { message = if (isEnglish) "Password reset email is a v1 stub." else "Email đặt lại mật khẩu là mô phỏng ở bản v1." }, modifier = Modifier.align(Alignment.End)) {
      Text(appString(R.string.forgot_password), color = Cocoa)
    }
    message?.let { InlineMessage(it) }
    PrimaryButton(
      text = appString(R.string.sign_in),
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
    EsmeryTextField(value = name, onValueChange = { name = it }, label = appString(R.string.full_name))
    EsmeryTextField(value = email, onValueChange = { email = it }, label = appString(R.string.email), keyboardType = KeyboardType.Email)
    EsmeryTextField(value = password, onValueChange = { password = it }, label = appString(R.string.password), keyboardType = KeyboardType.Password, password = true)
    message?.let { InlineMessage(it) }
    PrimaryButton(
      text = appString(R.string.sign_up),
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
