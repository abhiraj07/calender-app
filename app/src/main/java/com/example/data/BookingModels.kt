package com.example.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

enum class BookingStatus(val value: String, val label: String) {
  AVAILABLE("available", "Available"),
  PENDING("pending", "Pending"),
  BOOKED("booked", "Booked");

  companion object {
    fun fromValue(raw: String?): BookingStatus =
      entries.firstOrNull { it.value.equals(raw, ignoreCase = true) } ?: AVAILABLE
  }
}

@IgnoreExtraProperties
data class CalendarDateEntry(
  @DocumentId val id: String = "",
  val dateKey: String = "",
  val yearMonth: String = "",
  val status: String = BookingStatus.AVAILABLE.value,
  @get:PropertyName("isPublic")
  @set:PropertyName("isPublic")
  var isPublic: Boolean = true,
  val updatedByUid: String = "",
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  val bookingStatus: BookingStatus
    get() = BookingStatus.fromValue(status)
}

@IgnoreExtraProperties
data class AdminBookingEntry(
  @DocumentId val id: String = "",
  val dateKey: String = "",
  val status: String = BookingStatus.AVAILABLE.value,
  val clientName: String = "",
  val clientPhone: String = "",
  val eventType: String = "",
  val notes: String = "",
  val updatedByUid: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  val bookingStatus: BookingStatus
    get() = BookingStatus.fromValue(status)
}
