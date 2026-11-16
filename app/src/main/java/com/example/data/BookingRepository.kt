package com.example.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Single source of truth for the Android app: the same Supabase project used by
 * the public website and private calendar. No Firebase/Firestore data path.
 */
class BookingRepository(context: Context) {
  private val sessionStore = SecureSessionStore(context)
  @Volatile private var accessToken: String? = null
  @Volatile private var signedInUserId: String? = null

  suspend fun signInAdmin(usernameOrEmail: String, password: String): Boolean {
    val identifier = usernameOrEmail.trim().lowercase()
    val email = when (identifier) {
      ADMIN_USERNAME -> ADMIN_EMAIL
      ADMIN_EMAIL -> ADMIN_EMAIL
      else -> throw IllegalArgumentException("Enter the admin username or registered email.")
    }
    if (password.isBlank()) throw IllegalArgumentException("Enter your password.")

    val auth = request(
      path = "/auth/v1/token?grant_type=password",
      method = "POST",
      body = JSONObject().put("email", email).put("password", password),
      token = null,
    )
    val token = auth.optString("access_token")
    val refreshToken = auth.optString("refresh_token")
    val userId = auth.optJSONObject("user")?.optString("id").orEmpty()
    if (token.isBlank() || userId.isBlank()) throw IllegalStateException("Supabase did not return a valid admin session.")

    accessToken = token
    signedInUserId = userId
    if (refreshToken.isNotBlank()) withContext(Dispatchers.IO) { sessionStore.saveRefreshToken(refreshToken) }
    val allowed = request(
      path = "/rest/v1/admin_users?select=user_id&user_id=eq.${enc(userId)}&limit=1",
      method = "GET",
      token = token,
    )
    if (allowed.optJSONArrayOrEmpty().length() == 0) {
      signOut()
      throw IllegalStateException("This account is not authorised for Shree Banquet Hall admin access.")
    }
    return true
  }

  fun signOut() {
    accessToken = null
    signedInUserId = null
    sessionStore.clear()
  }

  suspend fun restoreAdminSession(): Boolean {
    val refreshToken = withContext(Dispatchers.IO) { sessionStore.readRefreshToken() } ?: return false
    return try {
      val auth = request(
        path = "/auth/v1/token?grant_type=refresh_token",
        method = "POST",
        body = JSONObject().put("refresh_token", refreshToken),
        token = null,
      )
      val token = auth.optString("access_token")
      val nextRefreshToken = auth.optString("refresh_token")
      val userId = auth.optJSONObject("user")?.optString("id").orEmpty()
      if (token.isBlank() || userId.isBlank()) throw IllegalStateException("Invalid refreshed session.")
      accessToken = token
      signedInUserId = userId
      if (nextRefreshToken.isNotBlank()) withContext(Dispatchers.IO) { sessionStore.saveRefreshToken(nextRefreshToken) }
      if (!checkIsAdminAccount()) throw IllegalStateException("Admin authorization is no longer valid.")
      true
    } catch (_: Exception) {
      signOut()
      false
    }
  }

  suspend fun checkIsAdminAccount(): Boolean {
    val token = accessToken ?: return false
    val uid = signedInUserId ?: return false
    return try {
      request(
        path = "/rest/v1/admin_users?select=user_id&user_id=eq.${enc(uid)}&limit=1",
        method = "GET",
        token = token,
      ).optJSONArrayOrEmpty().length() > 0
    } catch (_: Exception) {
      false
    }
  }

  fun observePublicCalendarDates(): Flow<Map<String, CalendarDateEntry>> = flow {
    while (true) {
      val rows = request(
        path = "/rest/v1/public_booking_calendar?select=booking_date,status&order=booking_date.asc",
        method = "GET",
        token = null,
      ).optJSONArrayOrEmpty()
      val result = linkedMapOf<String, CalendarDateEntry>()
      for (i in 0 until rows.length()) {
        val row = rows.getJSONObject(i)
        val date = row.optString("booking_date")
        if (date.isNotBlank()) result[date] = CalendarDateEntry(
          id = date, dateKey = date, yearMonth = date.take(7),
          status = row.optString("status", BookingStatus.AVAILABLE.value),
        )
      }
      emit(result)
      delay(POLL_INTERVAL_MS)
    }
  }

  fun observeAdminBookings(): Flow<Map<String, AdminBookingEntry>> = flow {
    while (true) {
      val token = accessToken ?: throw IllegalStateException("Please log in again to view private booking details.")
      val rows = request(
        path = "/rest/v1/admin_bookings?select=booking_date,status,client_name,client_phone,event_type,notes&order=booking_date.asc",
        method = "GET",
        token = token,
      ).optJSONArrayOrEmpty()
      val result = linkedMapOf<String, AdminBookingEntry>()
      for (i in 0 until rows.length()) {
        val row = rows.getJSONObject(i)
        val date = row.optString("booking_date")
        if (date.isNotBlank()) result[date] = AdminBookingEntry(
          id = date, dateKey = date,
          status = row.optString("status", BookingStatus.AVAILABLE.value),
          clientName = row.optString("client_name"),
          clientPhone = row.optString("client_phone"),
          eventType = row.optString("event_type"),
          notes = row.optString("notes"),
        )
      }
      emit(result)
      delay(POLL_INTERVAL_MS)
    }
  }

  suspend fun saveBooking(
    dateKey: String,
    status: BookingStatus,
    clientName: String,
    clientPhone: String,
    eventType: String,
    notes: String,
  ) {
    val token = accessToken ?: throw IllegalStateException("Please log in again before saving.")
    request(
      path = "/rest/v1/rpc/save_shree_booking",
      method = "POST",
      token = token,
      body = JSONObject()
        .put("p_booking_date", dateKey)
        .put("p_status", status.value)
        .put("p_client_name", clientName.trim().take(100))
        .put("p_client_phone", clientPhone.trim().take(30))
        .put("p_event_type", eventType.trim().take(60))
        .put("p_notes", notes.trim().take(500)),
    )
  }

  suspend fun clearBooking(dateKey: String) {
    val token = accessToken ?: throw IllegalStateException("Please log in again before clearing a booking.")
    request(
      path = "/rest/v1/rpc/clear_shree_booking",
      method = "POST",
      token = token,
      body = JSONObject().put("p_booking_date", dateKey),
    )
  }

  private suspend fun request(
    path: String,
    method: String,
    body: JSONObject? = null,
    token: String? = accessToken,
  ): JSONObject = withContext(Dispatchers.IO) {
    val connection = (URL(SUPABASE_URL + path).openConnection() as HttpURLConnection).apply {
      requestMethod = method
      connectTimeout = 15000
      readTimeout = 20000
      setRequestProperty("apikey", SUPABASE_KEY)
      setRequestProperty("Authorization", "Bearer ${token ?: SUPABASE_KEY}")
      setRequestProperty("Accept", "application/json")
      if (body != null) {
        doOutput = true
        setRequestProperty("Content-Type", "application/json")
      }
    }
    try {
      if (body != null) connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
      val code = connection.responseCode
      val stream = if (code in 200..299) connection.inputStream else connection.errorStream
      val response = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
      if (code !in 200..299) {
        val message = try { JSONObject(response).optString("message").ifBlank { JSONObject(response).optString("msg") } } catch (_: Exception) { response }
        throw IllegalStateException(message.ifBlank { "Supabase request failed (HTTP $code)." })
      }
      if (response.isBlank()) JSONObject() else if (response.trimStart().startsWith("[")) JSONObject().put("data", JSONArray(response)) else JSONObject(response)
    } finally {
      connection.disconnect()
    }
  }

  private fun JSONObject.optJSONArrayOrEmpty(): JSONArray = optJSONArray("data") ?: JSONArray()
  private fun enc(value: String): String = URLEncoder.encode(value, "UTF-8")

  companion object {
    private const val SUPABASE_URL = "https://agshupnzhpeqiwezsnyp.supabase.co"
    private const val SUPABASE_KEY = "sb_publishable_-gIJozghyFLW9TQfwY3FxA_OWZI097M"
    const val ADMIN_USERNAME = "admin980123"
    const val ADMIN_EMAIL = "krshreebanquethall@gmail.com"
    private const val POLL_INTERVAL_MS = 12000L
  }
}
