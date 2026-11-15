@file:OptIn(io.github.jan.supabase.annotations.SupabaseExperimental::class)

package com.example.data

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.selectAsFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class BookingRepository(
  private val client: io.github.jan.supabase.SupabaseClient = SupabaseClientProvider.client,
) {
  fun observePublicCalendarDates(): Flow<Map<String, CalendarDateEntry>> =
    client.from(BOOKING_DATES_TABLE).selectAsFlow(BookingDateRow::bookingDate)
      .map { rows -> rows.associate { row ->
        row.bookingDate to CalendarDateEntry(id = row.bookingDate, dateKey = row.bookingDate, status = row.status)
      } }

  fun observeAdminBookings(): Flow<Map<String, AdminBookingEntry>> =
    client.from(ADMIN_BOOKINGS_TABLE).selectAsFlow(AdminBookingRow::bookingDate)
      .map { rows -> rows.associate { row ->
        row.bookingDate to AdminBookingEntry(
          id = row.bookingDate, dateKey = row.bookingDate, status = row.status,
          clientName = row.clientName, clientPhone = row.clientPhone,
          eventType = row.eventType, notes = row.notes
        )
      } }

  suspend fun signInAdmin(usernameOrEmail: String, password: String): Boolean {
    val identifier = usernameOrEmail.trim().lowercase()
    if ((identifier != ADMIN_USERNAME && identifier != ADMIN_AUTH_EMAIL) || password.isBlank()) return false
    return try {
      client.auth.signInWith(Email) {
        email = ADMIN_AUTH_EMAIL
        this.password = password
      }
      val authorized = checkIsAdminAccount()
      if (!authorized) client.auth.signOut()
      authorized
    } catch (error: kotlinx.coroutines.CancellationException) {
      throw error
    } catch (_: Exception) {
      false
    }
  }

  suspend fun checkIsAdminAccount(): Boolean = try {
    client.auth.awaitInitialization()
    client.from(ADMIN_USERS_TABLE).selectAsFlow(AdminUserRow::userId).first().isNotEmpty()
  } catch (error: kotlinx.coroutines.CancellationException) {
    throw error
  } catch (_: Exception) {
    false
  }

  suspend fun signOutAdmin() {
    client.auth.signOut()
  }

  suspend fun saveBooking(
    dateKey: String, status: BookingStatus, clientName: String,
    clientPhone: String, eventType: String, notes: String,
  ) {
    if (status == BookingStatus.AVAILABLE) {
      clearBooking(dateKey)
      return
    }
    client.postgrest.rpc("save_shree_booking", buildJsonObject {
      put("p_booking_date", dateKey)
      put("p_status", status.value)
      put("p_client_name", clientName.trim().take(100))
      put("p_client_phone", clientPhone.trim().take(30))
      put("p_event_type", eventType.trim().take(60))
      put("p_notes", notes.trim().take(500))
    })
  }

  suspend fun clearBooking(dateKey: String) {
    client.postgrest.rpc("clear_shree_booking", buildJsonObject {
      put("p_booking_date", dateKey)
    })
  }

  companion object {
    const val BOOKING_DATES_TABLE = "public_booking_calendar"
    const val ADMIN_BOOKINGS_TABLE = "admin_bookings"
    const val ADMIN_USERS_TABLE = "admin_users"
    const val ADMIN_USERNAME = "admin980123"
    const val ADMIN_AUTH_EMAIL = "krshreebanquethall@gmail.com"
  }
}
