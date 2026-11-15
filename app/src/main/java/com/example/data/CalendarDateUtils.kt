package com.example.data

import java.util.Calendar
import java.util.Locale

data class HallDate(
  val year: Int,
  val month: Int, // 1..12
  val day: Int,   // 1..31
) : Comparable<HallDate> {

  val dateKey: String
    get() = String.format(Locale.US, "%04d-%02d-%02d", year, month, day)

  val yearMonth: HallYearMonth
    get() = HallYearMonth(year, month)

  override fun compareTo(other: HallDate): Int =
    compareValuesBy(this, other, { it.year }, { it.month }, { it.day })

  fun formatFullDisplay(): String {
    val cal =
      Calendar.getInstance().apply {
        clear()
        set(year, month - 1, day)
      }
    val dayName =
      cal.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.US) ?: ""
    val monthName =
      cal.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.US) ?: ""
    return "$dayName, $day $monthName $year"
  }

  companion object {
    fun today(): HallDate {
      val cal = Calendar.getInstance()
      return HallDate(
        year = cal.get(Calendar.YEAR),
        month = cal.get(Calendar.MONTH) + 1,
        day = cal.get(Calendar.DAY_OF_MONTH),
      )
    }

    fun fromKey(dateKey: String): HallDate? {
      val parts = dateKey.split("-")
      if (parts.size != 3) return null
      val y = parts[0].toIntOrNull() ?: return null
      val m = parts[1].toIntOrNull() ?: return null
      val d = parts[2].toIntOrNull() ?: return null
      return HallDate(y, m, d)
    }
  }
}

data class HallYearMonth(
  val year: Int,
  val month: Int, // 1..12
) {
  val key: String
    get() = String.format(Locale.US, "%04d-%02d", year, month)

  fun formatMonthYear(): String {
    val cal =
      Calendar.getInstance().apply {
        clear()
        set(year, month - 1, 1)
      }
    val monthName =
      cal.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.US) ?: ""
    return "$monthName $year"
  }

  fun plusMonths(delta: Int): HallYearMonth {
    val cal =
      Calendar.getInstance().apply {
        clear()
        set(year, month - 1, 1)
        add(Calendar.MONTH, delta)
      }
    return HallYearMonth(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
  }

  fun daysInMonth(): Int {
    val cal =
      Calendar.getInstance().apply {
        clear()
        set(year, month - 1, 1)
      }
    return cal.getActualMaximum(Calendar.DAY_OF_MONTH)
  }

  /** 0 for Sunday, 1 for Monday, ..., 6 for Saturday */
  fun firstDayOfWeekOffset(): Int {
    val cal =
      Calendar.getInstance().apply {
        clear()
        set(year, month - 1, 1)
      }
    return cal.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY
  }

  companion object {
    fun current(): HallYearMonth {
      val cal = Calendar.getInstance()
      return HallYearMonth(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
    }
  }
}
