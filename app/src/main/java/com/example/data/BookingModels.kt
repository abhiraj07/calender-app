package com.example.data

enum class BookingStatus(val value: String, val label: String) {
  AVAILABLE("available", "Available"),
  PENDING("pending", "Pending"),
  BOOKED("booked", "Booked");

  companion object {
    fun fromValue(raw: String?): BookingStatus =
      entries.firstOrNull { it.value.equals(raw, ignoreCase = true) } ?: AVAILABLE
  }
}

data class CalendarDateEntry(
  val id: String = "",
  val dateKey: String = "",
  val yearMonth: String = "",
  val status: String = BookingStatus.AVAILABLE.value,
  val isPublic: Boolean = true,
  val updatedByUid: String = "",
) {
  val bookingStatus: BookingStatus
    get() = BookingStatus.fromValue(status)
}

data class AdminBookingEntry(
  val id: String = "",
  val dateKey: String = "",
  val status: String = BookingStatus.AVAILABLE.value,
  val clientName: String = "",
  val clientPhone: String = "",
  val eventType: String = "",
  val notes: String = "",
  val updatedByUid: String = "",
) {
  val bookingStatus: BookingStatus
    get() = BookingStatus.fromValue(status)
}
