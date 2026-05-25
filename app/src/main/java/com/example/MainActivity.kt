package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.rounded.Nightlight
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Check
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.draw.alpha
import kotlinx.coroutines.launch
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.theme.*
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
          val navController = rememberNavController()
          NavHost(
            navController = navController,
            startDestination = "signin",
            modifier = Modifier.padding(innerPadding)
          ) {
            composable("signin") {
              WelcomeScreen(
                onNavigateToSignUp = {
                  if (navController.currentDestination?.route == "signin") {
                    navController.navigate("signup")
                  }
                },
                onSignIn = {
                  if (navController.currentDestination?.route == "signin") {
                    navController.navigate("home") {
                      popUpTo("signin") { inclusive = true }
                    }
                  }
                }
              )
            }
            composable("signup") {
              SignUpScreen(
                onNavigateToSignIn = {
                  if (navController.currentDestination?.route == "signup") {
                    navController.popBackStack()
                  }
                },
                onSignUpComplete = {
                  if (navController.currentDestination?.route == "signup") {
                    navController.navigate("onboarding")
                  }
                }
              )
            }
            composable("onboarding") {
              OnboardingPagerScreen(
                onGetStarted = {
                  if (navController.currentDestination?.route == "onboarding") {
                    navController.navigate("home") {
                      popUpTo("onboarding") { inclusive = true }
                    }
                  }
                }
              )
            }
            composable("home") {
              HomeScreen()
            }
          }
        }
      }
    }
  }
}

@Composable
fun WelcomeScreen(modifier: Modifier = Modifier, onNavigateToSignUp: () -> Unit = {}, onSignIn: () -> Unit = {}) {
  var email by remember { mutableStateOf("") }
  var password by remember { mutableStateOf("") }
  var passwordVisible by remember { mutableStateOf(false) }
  var isLoading by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  val coroutineScope = rememberCoroutineScope()

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(Cream)
      .padding(horizontal = 32.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Spacer(modifier = Modifier.height(48.dp))

    // Illustration Illustration
    Box(
      modifier = Modifier
        .size(200.dp),
      contentAlignment = Alignment.Center
    ) {
      // Glow behind
      Canvas(modifier = Modifier.fillMaxSize()) {
        drawCircle(
          brush = Brush.radialGradient(
            colors = listOf(Apricot.copy(alpha = 0.2f), Color.Transparent)
          ),
          radius = size.width / 2f
        )
      }

      // Main circle
      Surface(
        modifier = Modifier.size(160.dp),
        shape = CircleShape,
        color = Surface,
        shadowElevation = 8.dp,
        tonalElevation = 0.dp
      ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
          // Inner dashed ring
          Canvas(modifier = Modifier.matchParentSize()) {
            drawCircle(
              color = Cocoa.copy(alpha = 0.3f),
              radius = size.width / 2f * 0.8f,
              style = Stroke(
                width = 2f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
              )
            )
            // Draw small dots
            drawCircle(
              color = Sage,
              radius = 5.dp.toPx(),
              center = Offset(size.width * 0.85f, size.height * 0.15f)
            )
            drawCircle(
              color = Apricot,
              radius = 4.dp.toPx(),
              center = Offset(size.width * 0.15f, size.height * 0.85f)
            )
          }

          Icon(
            imageVector = Icons.Rounded.Nightlight,
            contentDescription = "Moon",
            tint = Apricot,
            modifier = Modifier.size(80.dp)
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(40.dp))

    Text(
      text = "Welcome Back",
      color = Cocoa,
      fontSize = 32.sp,
      fontWeight = FontWeight.ExtraBold
    )
    
    Spacer(modifier = Modifier.height(8.dp))

    Text(
      text = "Step inside your digital sanctuary",
      color = Taupe,
      fontSize = 16.sp,
      fontWeight = FontWeight.Medium
    )

    Spacer(modifier = Modifier.height(40.dp))

    CustomTextField(
      value = email,
      onValueChange = { email = it },
      placeholder = "Email Address",
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
    )

    Spacer(modifier = Modifier.height(16.dp))

    CustomTextField(
      value = password,
      onValueChange = { password = it },
      placeholder = "Password",
      isPassword = true,
      passwordVisible = passwordVisible,
      onPasswordVisibilityChange = { passwordVisible = it },
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
    )

    if (errorMessage != null) {
      Spacer(modifier = Modifier.height(8.dp))
      Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
    }

    Spacer(modifier = Modifier.height(16.dp))

    Box(
      modifier = Modifier.fillMaxWidth(),
      contentAlignment = Alignment.CenterEnd
    ) {
      Text(
        text = "Forgot Password?",
        color = Taupe,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.clickable { }
      )
    }

    Spacer(modifier = Modifier.height(32.dp))

    Button(
      onClick = {
        if (email.isBlank() || password.isBlank()) {
          errorMessage = "Vui lòng nhập email và mật khẩu"
          return@Button
        }
        isLoading = true
        errorMessage = null
        coroutineScope.launch {
          try {
            supabase.auth.signInWith(Email) {
              this.email = email
              this.password = password
            }
            isLoading = false
            onSignIn()
          } catch (e: Exception) {
            isLoading = false
            errorMessage = e.message ?: "Đăng nhập thất bại"
          }
        }
      },
      enabled = !isLoading,
      colors = ButtonDefaults.buttonColors(containerColor = Apricot),
      shape = CircleShape,
      modifier = Modifier
        .fillMaxWidth()
        .height(64.dp)
        .shadow(12.dp, CircleShape, spotColor = Apricot, ambientColor = Apricot)
    ) {
      Text(
        text = "Sign In",
        color = Color.White,
        fontSize = 18.sp,
        fontWeight = FontWeight.ExtraBold
      )
    }

    Spacer(modifier = Modifier.weight(1f))

    Text(
      text = buildAnnotatedString {
        append("New here? ")
        withStyle(style = SpanStyle(color = Cocoa, fontWeight = FontWeight.ExtraBold)) {
          append("Create an account")
        }
      },
      color = Taupe,
      fontSize = 16.sp,
      fontWeight = FontWeight.Medium,
      modifier = Modifier
        .padding(bottom = 32.dp)
        .clickable { onNavigateToSignUp() }
    )
  }
}

@Composable
fun SignUpScreen(modifier: Modifier = Modifier, onNavigateToSignIn: () -> Unit = {}, onSignUpComplete: () -> Unit = {}) {
  var name by remember { mutableStateOf("") }
  var email by remember { mutableStateOf("") }
  var password by remember { mutableStateOf("") }
  var passwordVisible by remember { mutableStateOf(false) }
  var isLoading by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  val coroutineScope = rememberCoroutineScope()

  Box(modifier = modifier.fillMaxSize().background(Cream)) {
    // Blurred background spots
    Canvas(modifier = Modifier.fillMaxSize()) {
      drawCircle(
        color = Apricot.copy(alpha = 0.05f),
        radius = 200.dp.toPx(),
        center = Offset(size.width, 0f) // top-right
      )
      drawCircle(
        color = Sage.copy(alpha = 0.1f),
        radius = 200.dp.toPx(),
        center = Offset(0f, size.height / 2f) // middle-left
      )
    }

    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 32.dp)
    ) {
      Spacer(modifier = Modifier.height(48.dp))

      // Header icon
      Box(
        modifier = Modifier
          .size(48.dp)
          .background(Apricot.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Filled.Favorite,
          contentDescription = "Favorite",
          tint = Apricot,
          modifier = Modifier.size(28.dp)
        )
      }

      Spacer(modifier = Modifier.height(16.dp))

      Text(
        text = "Start Your\nJourney",
        color = Cocoa,
        fontSize = 32.sp,
        fontWeight = FontWeight.ExtraBold,
        lineHeight = 40.sp
      )

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = "Let's set up your safe space together.",
        color = Taupe,
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium
      )

      Spacer(modifier = Modifier.height(32.dp))

      // Form
      SignUpTextField(
        label = "FULL NAME",
        value = name,
        onValueChange = { name = it },
        placeholder = "Alex Rivers"
      )

      Spacer(modifier = Modifier.height(20.dp))

      SignUpTextField(
        label = "EMAIL",
        value = email,
        onValueChange = { email = it },
        placeholder = "alex@hygge.com",
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
      )

      Spacer(modifier = Modifier.height(20.dp))

      SignUpTextField(
        label = "CREATE PASSWORD",
        value = password,
        onValueChange = { password = it },
        placeholder = "••••••••",
        isPassword = true,
        passwordVisible = passwordVisible,
        onPasswordVisibilityChange = { passwordVisible = it },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
      )

      Text(
        text = "Keep it safe and memorable.",
        color = Taupe,
        fontSize = 13.sp,
        fontStyle = FontStyle.Italic,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp)
      )

      if (errorMessage != null) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
      }

      Spacer(modifier = Modifier.height(32.dp))

      Button(
        onClick = {
          if (email.isBlank() || password.isBlank()) {
               errorMessage = "Vui lòng nhập email và mật khẩu hợp lệ"
               return@Button
          }
          isLoading = true
          errorMessage = null
          coroutineScope.launch {
               try {
                  supabase.auth.signUpWith(Email) {
                     this.email = email
                     this.password = password
                  }
                  isLoading = false
                  onSignUpComplete()
               } catch (e: Exception) {
                  isLoading = false
                  errorMessage = e.message ?: "Đăng ký thất bại"
               }
          }
        },
        enabled = !isLoading,
        colors = ButtonDefaults.buttonColors(containerColor = Sage),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
          .fillMaxWidth()
          .height(64.dp)
          .shadow(8.dp, RoundedCornerShape(16.dp), spotColor = Sage, ambientColor = Sage)
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.Center
        ) {
          Text(
            text = "Create My Circle",
            color = Cocoa,
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold
          )
          Spacer(modifier = Modifier.width(8.dp))
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = "Arrow Forward",
            tint = Cocoa
          )
        }
      }

      Spacer(modifier = Modifier.weight(1f))

      Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = buildAnnotatedString {
            append("Already have an account? ")
            withStyle(style = SpanStyle(color = Cocoa, fontWeight = FontWeight.ExtraBold)) {
              append("Sign In")
            }
          },
          color = Taupe,
          fontSize = 14.sp,
          fontWeight = FontWeight.Medium,
          modifier = Modifier
            .padding(bottom = 32.dp)
            .clickable { onNavigateToSignIn() }
        )
      }
    }
  }
}

@Composable
fun SignUpTextField(
  label: String,
  value: String,
  onValueChange: (String) -> Unit,
  placeholder: String,
  isPassword: Boolean = false,
  passwordVisible: Boolean = false,
  onPasswordVisibilityChange: (Boolean) -> Unit = {},
  keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
  Column {
    Text(
      text = label,
      color = Cocoa.copy(alpha = 0.6f),
      fontSize = 12.sp,
      fontWeight = FontWeight.ExtraBold,
      letterSpacing = 1.sp,
      modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
    )
    CustomTextField(
      value = value,
      onValueChange = onValueChange,
      placeholder = placeholder,
      isPassword = isPassword,
      passwordVisible = passwordVisible,
      onPasswordVisibilityChange = onPasswordVisibilityChange,
      keyboardOptions = keyboardOptions
    )
  }
}

@Composable
fun CustomTextField(
  value: String,
  onValueChange: (String) -> Unit,
  placeholder: String,
  isPassword: Boolean = false,
  passwordVisible: Boolean = false,
  onPasswordVisibilityChange: (Boolean) -> Unit = {},
  keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
  BasicTextField(
    value = value,
    onValueChange = onValueChange,
    keyboardOptions = keyboardOptions,
    visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
    textStyle = TextStyle(
      color = Cocoa,
      fontSize = 16.sp,
      fontWeight = FontWeight.Medium
    ),
    cursorBrush = SolidColor(Apricot),
    decorationBox = { innerTextField ->
      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .height(56.dp),
        shape = RoundedCornerShape(16.dp),
        color = Surface,
        shadowElevation = 2.dp
      ) {
        Row(
          modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterStart
          ) {
            if (value.isEmpty()) {
              Text(
                text = placeholder,
                color = Taupe.copy(alpha = 0.5f),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
              )
            }
            innerTextField()
          }

          if (isPassword) {
            IconButton(
              onClick = { onPasswordVisibilityChange(!passwordVisible) },
              modifier = Modifier.size(24.dp)
            ) {
              Icon(
                imageVector = Icons.Filled.Visibility,
                contentDescription = if (passwordVisible) "Hide password" else "Show password",
                tint = Taupe.copy(alpha = 0.6f)
              )
            }
          }
        }
      }
    }
  )
}

@Composable
fun OnboardingPagerScreen(modifier: Modifier = Modifier, onGetStarted: () -> Unit = {}) {
  val pagerState = rememberPagerState(pageCount = { 3 })
  val coroutineScope = rememberCoroutineScope()

  HorizontalPager(
    state = pagerState,
    modifier = modifier.fillMaxSize().background(Cream)
  ) { page ->
    when (page) {
      0 -> Onboarding1ScreenContent(
        onNext = {
          coroutineScope.launch {
            pagerState.animateScrollToPage(1)
          }
        }
      )
      1 -> Onboarding2ScreenContent(
        onNext = {
          coroutineScope.launch {
            pagerState.animateScrollToPage(2)
          }
        },
        onSkip = {
          coroutineScope.launch {
            pagerState.scrollToPage(2)
          }
        }
      )
      2 -> Onboarding3ScreenContent(onGetStarted = onGetStarted)
    }
  }
}

@Composable
fun Onboarding1ScreenContent(modifier: Modifier = Modifier, onNext: () -> Unit = {}) {
  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 40.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Spacer(modifier = Modifier.height(32.dp))

    Text(
      text = "A Gentle Hand on Your Shoulder",
      color = Cocoa,
      fontSize = 32.sp,
      fontWeight = FontWeight.ExtraBold,
      lineHeight = 40.sp,
      textAlign = TextAlign.Center,
      modifier = Modifier.padding(bottom = 16.dp)
    )

    Text(
      text = "Stay connected with those you love, quietly and simply. A safe space for solo dwellers.",
      color = Taupe,
      fontSize = 17.sp,
      fontWeight = FontWeight.Medium,
      textAlign = TextAlign.Center,
      lineHeight = 24.sp
    )

    Spacer(modifier = Modifier.weight(1f))

    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(300.dp),
      contentAlignment = Alignment.Center
    ) {
      Canvas(modifier = Modifier.size(250.dp)) {
        drawCircle(
          color = Apricot.copy(alpha = 0.2f),
          radius = size.width / 2f
        )
      }

      Text("📚", fontSize = 32.sp, modifier = Modifier.offset(x = (-80).dp, y = 100.dp).alpha(0.3f))
      Text("🌿", fontSize = 40.sp, modifier = Modifier.offset(x = 80.dp, y = 80.dp).alpha(0.4f))

      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
          modifier = Modifier
            .width(128.dp)
            .height(176.dp),
          shape = RoundedCornerShape(topStart = 64.dp, topEnd = 64.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
          color = Surface,
          border = BorderStroke(6.dp, Cocoa.copy(alpha = 0.1f)),
          shadowElevation = 8.dp
        ) {
          Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.size(80.dp).background(Apricot.copy(alpha = 0.4f), CircleShape))
            Icon(
              imageVector = Icons.Rounded.Lightbulb,
              contentDescription = null,
              tint = Apricot,
              modifier = Modifier.size(64.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Box(
          modifier = Modifier
            .width(144.dp)
            .height(16.dp)
            .background(Cocoa.copy(alpha = 0.1f), CircleShape)
        )
      }
    }

    Spacer(modifier = Modifier.weight(1f))

    Row(
      horizontalArrangement = Arrangement.spacedBy(10.dp),
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(bottom = 40.dp)
    ) {
      Box(modifier = Modifier.width(32.dp).height(10.dp).background(Apricot, CircleShape))
      Box(modifier = Modifier.size(10.dp).background(Cocoa.copy(alpha = 0.1f), CircleShape))
      Box(modifier = Modifier.size(10.dp).background(Cocoa.copy(alpha = 0.1f), CircleShape))
    }

    Button(
      onClick = onNext,
      colors = ButtonDefaults.buttonColors(containerColor = Apricot),
      shape = RoundedCornerShape(16.dp),
      modifier = Modifier
        .fillMaxWidth()
        .height(64.dp)
        .shadow(12.dp, RoundedCornerShape(16.dp), spotColor = Apricot, ambientColor = Apricot)
    ) {
      Text(
        text = "Next",
        color = Color.White,
        fontSize = 20.sp,
        fontWeight = FontWeight.ExtraBold
      )
    }

    Spacer(modifier = Modifier.height(32.dp))
  }
}

@Composable
fun Onboarding2ScreenContent(modifier: Modifier = Modifier, onNext: () -> Unit = {}, onSkip: () -> Unit = {}) {
  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 40.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Spacer(modifier = Modifier.height(16.dp))

    // Skip
    Box(
      modifier = Modifier.fillMaxWidth(),
      contentAlignment = Alignment.CenterEnd
    ) {
      Text(
        text = "SKIP",
        color = Taupe,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        modifier = Modifier.clickable { onSkip() }
      )
    }

    Spacer(modifier = Modifier.weight(1f))

    Box(
      modifier = Modifier.size(240.dp),
      contentAlignment = Alignment.Center
    ) {
      // Blur glow
      Canvas(modifier = Modifier.fillMaxSize()) {
        drawCircle(
          color = Apricot.copy(alpha = 0.15f),
          radius = size.width / 1.5f
        )
      }

      // Outer circle
      Surface(
        modifier = Modifier.size(192.dp),
        shape = CircleShape,
        color = Surface.copy(alpha = 0.4f),
        border = BorderStroke(1.dp, Surface)
      ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
          // Inner safe button graphic
          Box(
            modifier = Modifier
              .size(128.dp)
              .shadow(elevation = 16.dp, shape = CircleShape, spotColor = Apricot, ambientColor = Apricot)
              .background(
                brush = Brush.linearGradient(
                  colors = listOf(Color(0xFFFFB5A7), Color(0xFFFFC4B8)),
                  start = Offset.Zero,
                  end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                ),
                shape = CircleShape
              ),
            contentAlignment = Alignment.Center
          ) {
             Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("I'M SAFE", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Box(modifier = Modifier.width(16.dp).height(2.dp).background(Color.White.copy(alpha = 0.4f), CircleShape))
             }
          }
        }
      }
      
      // Checkmark bubble
       Surface(
          modifier = Modifier
            .size(48.dp)
            .align(Alignment.TopEnd)
            .offset(x = (-16).dp, y = 16.dp),
          shape = CircleShape,
          color = Sage,
          shadowElevation = 8.dp
       ) {
         Box(contentAlignment = Alignment.Center) {
           Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
         }
       }
    }

    Spacer(modifier = Modifier.height(64.dp))

    Text(
      text = "Simple Check-ins",
      color = Cocoa,
      fontSize = 32.sp,
      fontWeight = FontWeight.ExtraBold,
      lineHeight = 40.sp,
      textAlign = TextAlign.Center,
      modifier = Modifier.padding(bottom = 16.dp)
    )

    Text(
      text = "One tap tells your circle you're doing well. No long messages needed, just a signal of peace.",
      color = Taupe,
      fontSize = 17.sp,
      fontWeight = FontWeight.Medium,
      textAlign = TextAlign.Center,
      lineHeight = 24.sp
    )

    Spacer(modifier = Modifier.weight(1f))

    // Indicators
    Row(
      horizontalArrangement = Arrangement.spacedBy(10.dp),
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(bottom = 40.dp)
    ) {
      Box(modifier = Modifier.size(10.dp).background(Cocoa.copy(alpha = 0.1f), CircleShape))
      Box(modifier = Modifier.width(32.dp).height(10.dp).background(Apricot, CircleShape))
      Box(modifier = Modifier.size(10.dp).background(Cocoa.copy(alpha = 0.1f), CircleShape))
    }

    Button(
      onClick = onNext,
      colors = ButtonDefaults.buttonColors(containerColor = Apricot),
      shape = RoundedCornerShape(16.dp),
      modifier = Modifier
        .fillMaxWidth()
        .height(64.dp)
        .shadow(12.dp, RoundedCornerShape(16.dp), spotColor = Apricot, ambientColor = Apricot)
    ) {
      Text(
        text = "Next",
        color = Color.White,
        fontSize = 20.sp,
        fontWeight = FontWeight.ExtraBold
      )
    }

    Spacer(modifier = Modifier.height(32.dp))
  }
}

@Composable
fun Onboarding3ScreenContent(modifier: Modifier = Modifier, onGetStarted: () -> Unit = {}) {
  Column(
    modifier = modifier
      .fillMaxSize()
      .background(Cream)
      .padding(horizontal = 32.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Spacer(modifier = Modifier.height(16.dp))

    // Top indicator
    Box(
      modifier = Modifier.fillMaxWidth(),
      contentAlignment = Alignment.CenterEnd
    ) {
      Text(
        text = "3 / 3",
        color = Cocoa.copy(alpha = 0.3f),
        fontSize = 14.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 2.sp
      )
    }

    Spacer(modifier = Modifier.height(48.dp))

    // Phones illustration
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .aspectRatio(1f),
      contentAlignment = Alignment.Center
    ) {
      // Glow
      Canvas(modifier = Modifier.size(250.dp)) {
        drawCircle(
          color = Apricot.copy(alpha = 0.1f),
          radius = size.width / 2f
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Left Phone
        PhoneMockup(
          iconTint = Color(0xFF60A5FA),
          iconBg = Sky.copy(alpha = 0.2f),
          isLeft = true
        )

        // Center connection
        Box(
          modifier = Modifier
            .width(60.dp)
            .height(100.dp),
          contentAlignment = Alignment.Center
        ) {
          // Connection line
          Canvas(modifier = Modifier.fillMaxWidth().height(2.dp)) {
            val brush = Brush.horizontalGradient(
              colors = listOf(Color.Transparent, Apricot, Color.Transparent)
            )
            drawRect(brush = brush)
          }

          // Hearts
          Text("❤️", fontSize = 20.sp, modifier = Modifier.offset(x = (-10).dp, y = (-30).dp))
          Text("🫂", fontSize = 20.sp, modifier = Modifier.offset(x = 0.dp, y = 10.dp))
          Text("🧡", fontSize = 20.sp, modifier = Modifier.offset(x = 15.dp, y = (-10).dp))
        }

        // Right Phone
        PhoneMockup(
           iconTint = Apricot,
           iconBg = Apricot.copy(alpha = 0.2f),
           isLeft = false
        )
      }
    }

    Spacer(modifier = Modifier.height(32.dp))

    Text(
      text = "Feel the Warmth",
      color = Cocoa,
      fontSize = 28.sp,
      fontWeight = FontWeight.ExtraBold,
      textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(16.dp))

    Text(
      text = "When you check in, your circle receives a warm notification. They can send a virtual hug back to let you know they're there.",
      color = Taupe,
      fontSize = 17.sp,
      fontWeight = FontWeight.Medium,
      textAlign = TextAlign.Center,
      lineHeight = 24.sp,
      modifier = Modifier.padding(horizontal = 16.dp)
    )

    Spacer(modifier = Modifier.weight(1f))

    // Pagination Docs
    Row(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(bottom = 32.dp)
    ) {
      Box(modifier = Modifier.size(8.dp).background(Cocoa.copy(alpha = 0.1f), CircleShape))
      Box(modifier = Modifier.size(8.dp).background(Cocoa.copy(alpha = 0.1f), CircleShape))
      Box(modifier = Modifier.width(24.dp).height(8.dp).background(Sage, CircleShape))
    }

    Button(
      onClick = onGetStarted,
      colors = ButtonDefaults.buttonColors(containerColor = Sage),
      shape = RoundedCornerShape(16.dp),
      modifier = Modifier
        .fillMaxWidth()
        .height(64.dp)
        .shadow(12.dp, RoundedCornerShape(16.dp), spotColor = Sage, ambientColor = Sage)
    ) {
      Text(
        text = "Get Started",
        color = Cocoa,
        fontSize = 18.sp,
        fontWeight = FontWeight.ExtraBold
      )
    }

    Spacer(modifier = Modifier.height(32.dp))
  }
}

@Composable
fun PhoneMockup(iconTint: Color, iconBg: Color, isLeft: Boolean) {
  Surface(
    modifier = Modifier
      .width(110.dp)
      .height(200.dp),
    shape = RoundedCornerShape(24.dp),
    color = Surface,
    border = BorderStroke(4.dp, Cocoa.copy(alpha = 0.1f)),
    shadowElevation = 8.dp
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(12.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      // Speaker pill
      Box(modifier = Modifier.width(24.dp).height(4.dp).background(Cocoa.copy(alpha = 0.05f), CircleShape))
      
      Spacer(modifier = Modifier.height(16.dp))

      // Icon circle
      Box(
        modifier = Modifier.size(32.dp).background(iconBg, CircleShape),
        contentAlignment = Alignment.Center
      ) {
         Icon(
           imageVector = if (isLeft) Icons.Rounded.Person else Icons.Filled.Favorite,
           contentDescription = null,
           tint = iconTint,
           modifier = Modifier.size(16.dp)
         )
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Lines
      Box(modifier = Modifier.fillMaxWidth(0.8f).height(6.dp).background(Cocoa.copy(alpha = 0.05f), CircleShape))
      Spacer(modifier = Modifier.height(6.dp))
      if (isLeft) {
        Box(modifier = Modifier.fillMaxWidth(0.5f).height(6.dp).background(Cocoa.copy(alpha = 0.05f), CircleShape))
      } else {
        Box(modifier = Modifier.fillMaxWidth().height(6.dp).background(Sage.copy(alpha = 0.3f), CircleShape))
      }
      
      Spacer(modifier = Modifier.weight(1f))

      // Bottom element
      if (isLeft) {
        // Pulsing circle
        Box(modifier = Modifier.size(36.dp).background(Apricot.copy(alpha = 0.4f), CircleShape))
      } else {
        // Bottom bar
        Box(modifier = Modifier.fillMaxWidth().height(24.dp).background(Sage.copy(alpha = 0.2f), RoundedCornerShape(8.dp)))
      }
    }
  }
}
