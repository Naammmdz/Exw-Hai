package com.example

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth // Import updated to auth

val supabase = createSupabaseClient(
    supabaseUrl = BuildConfig.SUPABASE_URL,
    supabaseKey = BuildConfig.SUPABASE_ANON_KEY
) {
    install(Auth)
}
