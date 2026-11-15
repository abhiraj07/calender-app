package com.example.data

import com.example.BuildConfig
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime

/**
 * Shared Supabase client for the public calendar and authenticated admin workspace.
 *
 * The publishable key is intended for client applications. Access control must be enforced
 * by Supabase Auth and Row Level Security, never by hiding fields in the UI.
 */
object SupabaseClientProvider {
  val client by lazy {
    createSupabaseClient(
      supabaseUrl = BuildConfig.SUPABASE_URL,
      supabaseKey = BuildConfig.SUPABASE_PUBLISHABLE_KEY,
    ) {
      install(Auth)
      install(Postgrest)
      install(Realtime)
    }
  }
}
