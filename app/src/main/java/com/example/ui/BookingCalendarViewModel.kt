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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
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
  data class Error(val message: String, val requiresAdminGoogleAccount: Boolean = false) : AdminLoginFeedback
}

@OptIn(ExperimentalCoroutinesApi::class)
class BookingCalendarViewModel(
  private val repository: BookingRepository,
) : ViewModel() {
  private val _displayedMonth = MutableStateFlow(HallYearMonth.current())
  private val refreshTrigger = MutableStateFlow(0)
  private val _isRefreshing = MutableStateFlow(false)
  val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()
  val displayedMonth: StateFlow<HallYearMonth> = _displayedMonth.asStateFlow()
  private val _selectedDate = MutableStateFlow<HallDate?>(null)
  val selectedDate: StateFlow<HallDate?> = _selectedDate.asStateFlow()
  private val _isAuthorizedAdmin = MutableStateFlow(false)
  val isAuthorizedAdmin: StateFlow<Boolean> = _isAuthorizedAdmin.asStateFlow()
  val currentUserEmail: String? get() = if (_isAdminModeUnlocked.value) BookingRepository.ADMIN_EMAIL else null
  private val _isAdminModeUnlocked = MutableStateFlow(false)
  val isAdminModeUnlocked: StateFlow<Boolean> = _isAdminModeUnlocked.asStateFlow()
  private val _adminLoginFeedback = MutableStateFlow<AdminLoginFeedback>(AdminLoginFeedback.Idle)
  val adminLoginFeedback: StateFlow<AdminLoginFeedback> = _adminLoginFeedback.asStateFlow()
  private val _isSavingBooking = MutableStateFlow(false)
  val isSavingBooking: StateFlow<Boolean> = _isSavingBooking.asStateFlow()
  private val _actionMessage = MutableStateFlow<String?>(null)
  val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

  val calendarDatesState: StateFlow<UiState<Map<String, CalendarDateEntry>>> =
    refreshTrigger.flatMapLatest { repository.observePublicCalendarDates() }
      .map<Map<String, CalendarDateEntry>, UiState<Map<String, CalendarDateEntry>>> { UiState.Success(it) }
      .catch { error ->
        if (error is CancellationException) throw error
        Log.w(TAG, "Error observing Supabase public calendar", error)
        emit(UiState.Error(error.message ?: "Failed to load booking calendar"))
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Loading)

  val adminBookingsState: StateFlow<Map<String, AdminBookingEntry>> =
    combine(_isAdminModeUnlocked, refreshTrigger) { unlocked, _ -> unlocked }
      .distinctUntilChanged()
      .flatMapLatest { unlocked ->
      if (unlocked) repository.observeAdminBookings().catch { error ->
        if (error is CancellationException) throw error
        Log.w(TAG, "Error observing Supabase private bookings", error)
        emit(emptyMap())
      } else flowOf(emptyMap())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyMap())

  fun refreshCalendar() {
    if (_isRefreshing.value) return
    _isRefreshing.value = true
    refreshTrigger.value += 1
    viewModelScope.launch {
      kotlinx.coroutines.delay(900)
      _isRefreshing.value = false
    }
  }

  fun previousMonth() { _displayedMonth.value = _displayedMonth.value.plusMonths(-1) }
  fun nextMonth() { _displayedMonth.value = _displayedMonth.value.plusMonths(1) }
  fun selectDate(date: HallDate) { _selectedDate.value = date }
  fun dismissSelectedDate() { _selectedDate.value = null }
  fun clearActionMessage() { _actionMessage.value = null }
  fun resetAdminLoginFeedback() { _adminLoginFeedback.value = AdminLoginFeedback.Idle }

  fun attemptAdminLogin(usernameOrEmail: String, password: String) {
    viewModelScope.launch {
      _adminLoginFeedback.value = AdminLoginFeedback.Idle
      try {
        repository.signInAdmin(usernameOrEmail, password)
        val authorized = repository.checkIsAdminAccount()
        _isAuthorizedAdmin.value = authorized
        if (!authorized) throw IllegalStateException("This account is not authorised for admin access.")
        _isAdminModeUnlocked.value = true
        _adminLoginFeedback.value = AdminLoginFeedback.Success
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        repository.signOut()
        _isAuthorizedAdmin.value = false
        _isAdminModeUnlocked.value = false
        _adminLoginFeedback.value = AdminLoginFeedback.Error(e.localizedMessage ?: "Login failed. Check username and password.")
      }
    }
  }

  fun lockAdminMode() {
    repository.signOut()
    _isAdminModeUnlocked.value = false
    _isAuthorizedAdmin.value = false
    _adminLoginFeedback.value = AdminLoginFeedback.Idle
  }

  fun saveBooking(dateKey: String, status: BookingStatus, clientName: String, clientPhone: String, eventType: String, notes: String) {
    if (_isSavingBooking.value) return
    viewModelScope.launch {
      _isSavingBooking.value = true
      try {
        repository.saveBooking(dateKey, status, clientName, clientPhone, eventType, notes)
        refreshTrigger.value += 1
        _actionMessage.value = "Updated $dateKey to ${status.label}"
        _selectedDate.value = null
      } catch (e: CancellationException) { throw e }
      catch (e: Exception) { _actionMessage.value = e.localizedMessage ?: "Failed to save booking" }
      finally { _isSavingBooking.value = false }
    }
  }

  fun clearBooking(dateKey: String) {
    if (_isSavingBooking.value) return
    viewModelScope.launch {
      _isSavingBooking.value = true
      try {
        repository.clearBooking(dateKey)
        refreshTrigger.value += 1
        _actionMessage.value = "$dateKey marked Available"
        _selectedDate.value = null
      } catch (e: CancellationException) { throw e }
      catch (e: Exception) { _actionMessage.value = e.localizedMessage ?: "Failed to clear booking" }
      finally { _isSavingBooking.value = false }
    }
  }

  private companion object {
    const val TAG = "BookingCalendarVM"
    const val STOP_TIMEOUT_MILLIS = 5000L
  }
}
