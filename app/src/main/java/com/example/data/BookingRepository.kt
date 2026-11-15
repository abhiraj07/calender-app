package com.example.data

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.auth.signInWithEmailAndPassword
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class BookingRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = Firebase.auth,
) {
  constructor(context: Context) : this(
    db =
      FirebaseFirestore.getInstance(
        context.applicationContext.getString(R.string.firestore_database_id)
      ),
    auth = Firebase.auth,
  )

  private fun requireUserId(): String =
    auth.currentUser?.uid
      ?: throw IllegalStateException("User must be signed in before accessing Firestore.")

  fun observePublicCalendarDates(): Flow<Map<String, CalendarDateEntry>> {
    val path = CALENDAR_DATES_COLLECTION
    return db
      .collection(path)
      .whereEqualTo("isPublic", true)
      .snapshots()
      .map { snapshot ->
        snapshot
          .toObjects(
            CalendarDateEntry::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
          )
          .associateBy { it.dateKey.ifBlank { it.id } }
      }
      .catch { error ->
        val firestoreError =
          generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser != null) {
          handleFirestoreError(exceptionToLog, OperationType.LIST, path)
        }
        throw error
      }
  }

  fun observeAdminBookings(): Flow<Map<String, AdminBookingEntry>> {
    val path = ADMIN_BOOKINGS_COLLECTION
    return db
      .collection(path)
      .snapshots()
      .map { snapshot ->
        snapshot
          .toObjects(
            AdminBookingEntry::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
          )
          .associateBy { it.dateKey.ifBlank { it.id } }
      }
      .catch { error ->
        val firestoreError =
          generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser != null) {
          handleFirestoreError(exceptionToLog, OperationType.LIST, path)
        }
        throw error
      }
  }

  suspend fun signInAdmin(usernameOrEmail: String, password: String): Boolean {
    val identifier = usernameOrEmail.trim().lowercase()
    val email = when (identifier) {
      ADMIN_USERNAME -> BOOTSTRAPPED_ADMIN_HALL
      BOOTSTRAPPED_ADMIN_HALL -> BOOTSTRAPPED_ADMIN_HALL
      else -> return false
    }
    return try {
      auth.signInWithEmailAndPassword(email, password).await()
      val user = auth.currentUser ?: return false
      if (!user.isEmailVerified) {
        auth.signOut()
        return false
      }
      checkIsAdminAccount()
    } catch (e: CancellationException) {
      throw e
    } catch (_: Exception) {
      false
    }
  }

  suspend fun checkIsAdminAccount(): Boolean {
    val user = auth.currentUser ?: return false
    val email = user.email?.lowercase()
    if (user.isEmailVerified && (email == BOOTSTRAPPED_ADMIN_PRIMARY || email == BOOTSTRAPPED_ADMIN_HALL)) {
      return true
    }
    val docPath = "$ADMINS_COLLECTION/${user.uid}"
    return try {
      db.collection(ADMINS_COLLECTION).document(user.uid).get().await().exists()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, docPath)
      false
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
    val uid = requireUserId()
    val yearMonth = dateKey.take(7)
    val publicDocRef = db.collection(CALENDAR_DATES_COLLECTION).document(dateKey)
    val adminDocRef = db.collection(ADMIN_BOOKINGS_COLLECTION).document(dateKey)

    try {
      val publicExists = publicDocRef.get().await().exists()
      val adminExists = adminDocRef.get().await().exists()
      val batch = db.batch()

      if (publicExists) {
        batch.update(
          publicDocRef,
          mapOf(
            "status" to status.value,
            "updatedByUid" to uid,
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )
      } else {
        batch.set(
          publicDocRef,
          mapOf(
            "dateKey" to dateKey,
            "yearMonth" to yearMonth,
            "status" to status.value,
            "isPublic" to true,
            "updatedByUid" to uid,
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )
      }

      if (adminExists) {
        batch.update(
          adminDocRef,
          mapOf(
            "status" to status.value,
            "clientName" to clientName.trim().take(100),
            "clientPhone" to clientPhone.trim().take(30),
            "eventType" to eventType.trim().take(60),
            "notes" to notes.trim().take(500),
            "updatedByUid" to uid,
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )
      } else {
        batch.set(
          adminDocRef,
          mapOf(
            "dateKey" to dateKey,
            "status" to status.value,
            "clientName" to clientName.trim().take(100),
            "clientPhone" to clientPhone.trim().take(30),
            "eventType" to eventType.trim().take(60),
            "notes" to notes.trim().take(500),
            "updatedByUid" to uid,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )
      }

      batch.commit().await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, publicDocRef.path)
      throw e
    }
  }

  suspend fun clearBooking(dateKey: String) {
    requireUserId()
    val publicDocRef = db.collection(CALENDAR_DATES_COLLECTION).document(dateKey)
    val adminDocRef = db.collection(ADMIN_BOOKINGS_COLLECTION).document(dateKey)
    try {
      val batch = db.batch()
      batch.delete(publicDocRef)
      batch.delete(adminDocRef)
      batch.commit().await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, publicDocRef.path)
      throw e
    }
  }

  companion object {
    const val CALENDAR_DATES_COLLECTION = "calendar_dates"
    const val ADMIN_BOOKINGS_COLLECTION = "admin_bookings"
    const val ADMINS_COLLECTION = "admins"
    const val ADMIN_USERNAME = "admin980123"
    const val BOOTSTRAPPED_ADMIN_PRIMARY = "abhiraj2482@gmail.com"
    const val BOOTSTRAPPED_ADMIN_HALL = "krshreebanquethall@gmail.com"
  }
}
