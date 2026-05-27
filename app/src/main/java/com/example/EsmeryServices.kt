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
    supabase.auth.signInWith(Email) {
      this.email = email
      this.password = password
    }
    val user = supabase.auth.currentUserOrNull()
    repository.loadForUser(
      userId = user?.id ?: email,
      email = user?.email ?: email,
      displayName = user?.email?.substringBefore('@') ?: email.substringBefore('@'),
    )
  }

  suspend fun signUp(name: String, email: String, password: String) {
    supabase.auth.signUpWith(Email) {
      this.email = email
      this.password = password
    }
    val user = supabase.auth.currentUserOrNull()
    repository.loadForUser(
      userId = user?.id ?: email,
      email = user?.email ?: email,
      displayName = name.ifBlank { email.substringBefore('@') },
    )
  }

  suspend fun signOut() {
    runCatching { supabase.auth.signOut() }
    repository.clearLocalSession()
  }

  suspend fun resetPasswordStub(email: String) {
    require(email.isNotBlank()) { "Enter your email first." }
  }
}
