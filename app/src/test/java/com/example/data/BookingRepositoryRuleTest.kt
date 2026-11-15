package com.example.data

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class BookingRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun observePublicCalendarDates_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
    auth.signOut()
    val repository = BookingRepository(firestore, auth)

    try {
      withTimeout(FLOW_TIMEOUT_MS) { repository.observePublicCalendarDates().first() }
      fail("Expected PERMISSION_DENIED for unauthenticated user")
    } catch (e: Exception) {
      val firestoreException = e.unwrapFirestoreException()
      assertNotNull("Expected nested FirebaseFirestoreException, got $e", firestoreException)
      assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException!!.code)
    }
  }

  @Test
  fun saveBooking_nonAdminUser_failsWithPermissionDenied() = runBlocking {
    signInTestUser(VISITOR_EMAIL)
    val repository = BookingRepository(firestore, auth)

    try {
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repository.saveBooking(
          dateKey = "2026-11-23",
          status = BookingStatus.BOOKED,
          clientName = "Test Client",
          clientPhone = "9876543210",
          eventType = "Wedding",
          notes = "Unauthorized write test",
        )
      }
      fail("Expected PERMISSION_DENIED for non-admin user")
    } catch (e: Exception) {
      val firestoreException = e.unwrapFirestoreException()
      assertNotNull("Expected FirebaseFirestoreException, got $e", firestoreException)
      assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException!!.code)
    }
  }

  @Test
  fun observePublicCalendarDates_authenticatedVisitor_succeeds() = runBlocking {
    signInTestUser(VISITOR_EMAIL)
    val repository = BookingRepository(firestore, auth)

    val dates = withTimeout(FLOW_TIMEOUT_MS) { repository.observePublicCalendarDates().first() }
    assertTrue(dates.isEmpty() || dates.values.all { it.isPublic })
  }

  @Test
  fun observeAdminBookings_nonAdminUser_failsWithPermissionDenied() = runBlocking {
    signInTestUser(VISITOR_EMAIL)
    val repository = BookingRepository(firestore, auth)

    try {
      withTimeout(FLOW_TIMEOUT_MS) { repository.observeAdminBookings().first() }
      fail("Expected PERMISSION_DENIED for non-admin reading PII collection")
    } catch (e: Exception) {
      val firestoreException = e.unwrapFirestoreException()
      assertNotNull("Expected nested FirebaseFirestoreException, got $e", firestoreException)
      assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException!!.code)
    }
  }

  private companion object {
    const val VISITOR_EMAIL = "visitor@example.com"
    const val DEFAULT_TIMEOUT_MS = 5000L
    const val FLOW_TIMEOUT_MS = 3000L
  }
}
