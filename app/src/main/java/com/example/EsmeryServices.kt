package com.example

import com.example.data.EsmeryRepository
import com.example.data.ResilientEsmeryRepository
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email

object EsmeryServices {
  val repository: EsmeryRepository = ResilientEsmeryRepository()
}

class AuthGateway(
  private val repository: EsmeryRepository = EsmeryServices.repository,
) {
  suspend fun signIn(email: String, password: String) {
    val normalizedEmail = email.trim().lowercase()
    supabase.auth.signInWith(Email) {
      this.email = normalizedEmail
      this.password = password
    }
    val user = supabase.auth.currentUserOrNull()
    repository.loadForUser(
      userId = user?.id ?: normalizedEmail,
      email = user?.email ?: normalizedEmail,
      displayName = user?.email?.substringBefore('@') ?: normalizedEmail.substringBefore('@'),
    )
  }

  suspend fun signUp(name: String, email: String, password: String) {
    val normalizedEmail = email.trim().lowercase()
    supabase.auth.signUpWith(Email) {
      this.email = normalizedEmail
      this.password = password
    }
    val user = supabase.auth.currentUserOrNull()
    repository.loadForUser(
      userId = user?.id ?: normalizedEmail,
      email = user?.email ?: normalizedEmail,
      displayName = name.ifBlank { normalizedEmail.substringBefore('@') },
    )
  }

  suspend fun signOut() {
    runCatching { supabase.auth.signOut() }
    repository.clearLocalSession()
  }

  suspend fun resetPassword(email: String) {
    require(email.isNotBlank()) { "Enter your email first." }
    supabase.auth.resetPasswordForEmail(email.trim().lowercase())
  }
}
