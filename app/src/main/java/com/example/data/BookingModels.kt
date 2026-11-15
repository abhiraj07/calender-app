package com.example.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

enum class BookingStatus(val value: String, val label: String) {
  AVAILABLE("available", "Available"),
  PENDING("pending", "Pending"),
  BOOKED("booked", "Booked");

  companion object {
    fun fromValue(raw: String?): BookingStatus =
      entries.firstOrNull { it.value.equals(raw, ignoreCase = true) } ?: AVAILABLE
  }
}

@Serializable
data class BookingDateRow(
  @SerialName("booking_date") val bookingDate: String,
  val status: String,
)

@Serializable
data class AdminBookingRow(
  @SerialName("booking_date") val bookingDate: String,
  val status: String,
  @SerialName("client_name") val clientName: String = "",
  @SerialName("client_phone") val clientPhone: String = "",
  @SerialName("event_type") val eventType: String = "",
  val notes: String = "",
)

@Serializable
data class AdminUserRow(
  @SerialName("user_id") val userId: String,
  val username: String,
)

@Serializable
data class SaveBookingParams(
  @SerialName("p_booking_date") val bookingDate: String,
  @SerialName("p_status") val status: String,
  @SerialName("p_client_name") val clientName: String,
  @SerialName("p_client_phone") val clientPhone: String,
  @SerialName("p_event_type") val eventType: String,
  @SerialName("p_notes") val notes: String,
)

@Serializable
data class ClearBookingParams(
  @SerialName("p_booking_date") val bookingDate: String,
)

data class CalendarDateEntry(
  val id: String = "",
  val dateKey: String = "",
  val status: String = BookingStatus.AVAILABLE.value,
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
) {
  val bookingStatus: BookingStatus
    get() = BookingStatus.fromValue(status)
}
