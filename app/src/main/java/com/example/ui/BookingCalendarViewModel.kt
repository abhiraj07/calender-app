package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AdminBookingEntry
import com.example.data.BookingRepository
import com.example.data.BookingStatus
import com.example.data.CalendarDateEntry
import com.example.data.HallDate
import com.example.data.HallYearMonth
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
  data object Loading : UiState<Nothing>
  data class Success<T>(val data: T) : UiState<T>
  data class Error(val message: String) : UiState<Nothing>
}

sealed interface AdminLoginFeedback {
  data object Idle : AdminLoginFeedback
  data object Success : AdminLoginFeedback
  data class Error(val message: String) :
    AdminLoginFeedback
}

@OptIn(ExperimentalCoroutinesApi::class)
class BookingCalendarViewModel(
  private val repository: BookingRepository,
  val currentUserId: String,
  val currentUserEmail: String?,
) : ViewModel() {

  private val _displayedMonth = MutableStateFlow(HallYearMonth.current())
  val displayedMonth: StateFlow<HallYearMonth> = _displayedMonth.asStateFlow()

  private val _selectedDate = MutableStateFlow<HallDate?>(null)
  val selectedDate: StateFlow<HallDate?> = _selectedDate.asStateFlow()

  private val _isAuthorizedAdmin = MutableStateFlow(false)
  val isAuthorizedAdmin: StateFlow<Boolean> = _isAuthorizedAdmin.asStateFlow()

  private val _isAdminModeUnlocked = MutableStateFlow(false)
  val isAdminModeUnlocked: StateFlow<Boolean> = _isAdminModeUnlocked.asStateFlow()

  private val _adminLoginFeedback = MutableStateFlow<AdminLoginFeedback>(AdminLoginFeedback.Idle)
  val adminLoginFeedback: StateFlow<AdminLoginFeedback> = _adminLoginFeedback.asStateFlow()

  private val _isSavingBooking = MutableStateFlow(false)
  val isSavingBooking: StateFlow<Boolean> = _isSavingBooking.asStateFlow()

  private val _actionMessage = MutableStateFlow<String?>(null)
  val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

  val calendarDatesState: StateFlow<UiState<Map<String, CalendarDateEntry>>> =
    repository
      .observePublicCalendarDates()
      .map<Map<String, CalendarDateEntry>, UiState<Map<String, CalendarDateEntry>>> {
        UiState.Success(it)
      }
      .catch { error ->
        val firestoreError =
          generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        Log.w(TAG, "Error observing calendar dates", rootError)
        emit(UiState.Error(rootError.message ?: "Failed to load booking calendar"))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading,
      )

  val adminBookingsState: StateFlow<Map<String, AdminBookingEntry>> =
    _isAdminModeUnlocked
      .flatMapLatest { unlocked ->
        if (unlocked) {
          repository.observeAdminBookings().catch { error ->
            val firestoreError =
              generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            Log.w(TAG, "Error observing admin bookings", firestoreError ?: error)
            emit(emptyMap())
          }
        } else {
          flowOf(emptyMap())
        }
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = emptyMap(),
      )

  init {
    viewModelScope.launch {
      _isAuthorizedAdmin.value = repository.checkIsAdminAccount()
      _isAdminModeUnlocked.value = _isAuthorizedAdmin.value
    }
  }

  fun previousMonth() {
    _displayedMonth.value = _displayedMonth.value.plusMonths(-1)
  }

  fun nextMonth() {
    _displayedMonth.value = _displayedMonth.value.plusMonths(1)
  }

  fun selectDate(date: HallDate) {
    _selectedDate.value = date
  }

  fun dismissSelectedDate() {
    _selectedDate.value = null
  }

  fun clearActionMessage() {
    _actionMessage.value = null
  }

  fun resetAdminLoginFeedback() {
    _adminLoginFeedback.value = AdminLoginFeedback.Idle
  }

  fun attemptAdminLogin(usernameOrEmail: String, password: String) {
    val identifier = usernameOrEmail.trim().lowercase()
    if (identifier.isBlank()) {
      _adminLoginFeedback.value = AdminLoginFeedback.Error("Please enter your admin username or email.")
      return
    }
    if (password.isBlank()) {
      _adminLoginFeedback.value = AdminLoginFeedback.Error("Please enter your admin password.")
      return
    }
    if (identifier != BookingRepository.ADMIN_USERNAME && identifier != BookingRepository.BOOTSTRAPPED_ADMIN_HALL) {
      _adminLoginFeedback.value = AdminLoginFeedback.Error("Use admin980123 or the registered admin email.")
      return
    }
    viewModelScope.launch {
      _adminLoginFeedback.value = AdminLoginFeedback.Idle
      try {
        val authorized = repository.signInAdmin(identifier, password)
        _isAuthorizedAdmin.value = authorized
        if (authorized) {
          _isAdminModeUnlocked.value = true
          _adminLoginFeedback.value = AdminLoginFeedback.Success
        } else {
          _adminLoginFeedback.value = AdminLoginFeedback.Error(
            "Login failed. Check the password and make sure the admin email is verified."
          )
        }
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        _adminLoginFeedback.value = AdminLoginFeedback.Error(
          e.localizedMessage ?: "Unable to sign in. Please try again."
        )
      }
    }
  }

  fun lockAdminMode() {
    _isAdminModeUnlocked.value = false
    _adminLoginFeedback.value = AdminLoginFeedback.Idle
  }

  fun saveBooking(
    dateKey: String,
    status: BookingStatus,
    clientName: String,
    clientPhone: String,
    eventType: String,
    notes: String,
  ) {
    if (_isSavingBooking.value) return
    viewModelScope.launch {
      _isSavingBooking.value = true
      try {
        repository.saveBooking(
          dateKey = dateKey,
          status = status,
          clientName = clientName,
          clientPhone = clientPhone,
          eventType = eventType,
          notes = notes,
        )
        _actionMessage.value = "Updated $dateKey to ${status.label}"
        _selectedDate.value = null
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        _actionMessage.value = e.localizedMessage ?: "Failed to save booking"
      } finally {
        _isSavingBooking.value = false
      }
    }
  }

  fun clearBooking(dateKey: String) {
    if (_isSavingBooking.value) return
    viewModelScope.launch {
      _isSavingBooking.value = true
      try {
        repository.clearBooking(dateKey)
        _actionMessage.value = "$dateKey marked Available"
        _selectedDate.value = null
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        _actionMessage.value = e.localizedMessage ?: "Failed to clear booking"
      } finally {
        _isSavingBooking.value = false
      }
    }
  }

  private companion object {
    const val TAG = "BookingCalendarVM"
    const val STOP_TIMEOUT_MILLIS = 5000L
    const val ADMIN_USERNAME = "admin980123"
  }
}
