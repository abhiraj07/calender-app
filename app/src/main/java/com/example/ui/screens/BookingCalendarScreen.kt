package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.AdminBookingEntry
import com.example.data.BookingStatus
import com.example.data.CalendarDateEntry
import com.example.data.HallDate
import com.example.data.HallYearMonth
import com.example.ui.BookingCalendarViewModel
import com.example.ui.UiState
import com.example.ui.components.BookingDetailSheet
import com.example.ui.components.HallTopBar
import com.example.ui.theme.BookedRed
import com.example.ui.theme.CharcoalText
import com.example.ui.theme.EmeraldAvailable
import com.example.ui.theme.EmeraldAvailableDot
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldHairline
import com.example.ui.theme.InputBorder
import com.example.ui.theme.IvoryCream
import com.example.ui.theme.MutedSageText
import com.example.ui.theme.PastSlate
import com.example.ui.theme.PendingYellow
import com.example.ui.theme.PlayfairDisplayFontFamily
import com.example.ui.theme.PureWarmWhite
import com.example.ui.theme.SoftGoldTint

@Composable
fun BookingCalendarScreen(
  viewModel: BookingCalendarViewModel,
  onNavigateToAdminLogin: () -> Unit,
) {
  val calendarState by viewModel.calendarDatesState.collectAsStateWithLifecycle()
  val adminBookings by viewModel.adminBookingsState.collectAsStateWithLifecycle()
  val displayedMonth by viewModel.displayedMonth.collectAsStateWithLifecycle()
  val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
  val isAdminUnlocked by viewModel.isAdminModeUnlocked.collectAsStateWithLifecycle()
  val isSaving by viewModel.isSavingBooking.collectAsStateWithLifecycle()
  val actionMessage by viewModel.actionMessage.collectAsStateWithLifecycle()

  val today = remember { HallDate.today() }
  val snackbarHostState = remember { SnackbarHostState() }

  LaunchedEffect(actionMessage) {
    val msg = actionMessage
    if (msg != null) {
      snackbarHostState.showSnackbar(msg)
      viewModel.clearActionMessage()
    }
  }

  Scaffold(
    containerColor = IvoryCream,
    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    topBar = {
      HallTopBar(
        showAdminLoginButton = true,
        isAdminUnlocked = isAdminUnlocked,
        showBackToCalendarButton = false,
        onAdminLoginClick = onNavigateToAdminLogin,
        onLockAdminClick = { viewModel.lockAdminMode() },
        onBackToCalendarClick = {},
      )
    },
  ) { innerPadding ->
    Box(
      modifier =
        Modifier
          .fillMaxSize()
          .background(IvoryCream)
          .padding(innerPadding)
          .navigationBarsPadding(),
      contentAlignment = Alignment.TopCenter,
    ) {
      Column(
        modifier =
          Modifier
            .fillMaxWidth()
            .widthIn(max = 680.dp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Text(
          text = stringResource(R.string.plan_your_celebration),
          style = MaterialTheme.typography.labelSmall,
          color = GoldAccent,
          letterSpacing = 2.2.sp,
          textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
          text = stringResource(R.string.check_date_availability),
          fontFamily = PlayfairDisplayFontFamily,
          fontWeight = FontWeight.SemiBold,
          fontSize = 30.sp,
          lineHeight = 36.sp,
          color = EmeraldDeep,
          textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
          text = stringResource(R.string.availability_subtitle),
          style = MaterialTheme.typography.bodyMedium,
          color = MutedSageText,
          textAlign = TextAlign.Center,
          modifier = Modifier.padding(horizontal = 12.dp),
        )

        if (isAdminUnlocked) {
          Spacer(modifier = Modifier.height(16.dp))
          AdminModeBanner(
            bookedCount =
              (calendarState as? UiState.Success)?.data?.values?.count {
                it.bookingStatus == BookingStatus.BOOKED
              } ?: 0,
            pendingCount =
              (calendarState as? UiState.Success)?.data?.values?.count {
                it.bookingStatus == BookingStatus.PENDING
              } ?: 0,
          )
        }

        Spacer(modifier = Modifier.height(20.dp))

        when (val state = calendarState) {
          is UiState.Loading -> {
            CalendarCardContainer(
              displayedMonth = displayedMonth,
              datesMap = emptyMap(),
              today = today,
              isLoading = true,
              onPrevMonth = viewModel::previousMonth,
              onNextMonth = viewModel::nextMonth,
              onDateClick = viewModel::selectDate,
            )
          }
          is UiState.Error -> {
            Card(
              shape = RoundedCornerShape(16.dp),
              colors = CardDefaults.cardColors(containerColor = PureWarmWhite),
              border = BorderStroke(1.dp, BookedRed.copy(alpha = 0.4f)),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
              ) {
                Text(
                  text = "Unable to load live calendar",
                  fontFamily = PlayfairDisplayFontFamily,
                  fontWeight = FontWeight.SemiBold,
                  fontSize = 18.sp,
                  color = BookedRed,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                  text = state.message,
                  style = MaterialTheme.typography.bodyMedium,
                  color = MutedSageText,
                  textAlign = TextAlign.Center,
                )
              }
            }
          }
          is UiState.Success -> {
            CalendarCardContainer(
              displayedMonth = displayedMonth,
              datesMap = state.data,
              today = today,
              isLoading = false,
              onPrevMonth = viewModel::previousMonth,
              onNextMonth = viewModel::nextMonth,
              onDateClick = viewModel::selectDate,
            )

            if (isAdminUnlocked && adminBookings.isNotEmpty()) {
              Spacer(modifier = Modifier.height(20.dp))
              AdminUpcomingBookingsSection(
                bookings = adminBookings.values.sortedBy { it.dateKey },
                onBookingClick = { dateKey ->
                  HallDate.fromKey(dateKey)?.let { viewModel.selectDate(it) }
                },
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(20.dp))


      }
    }
  }

  val activeDate = selectedDate
  if (activeDate != null) {
    val datesMap = (calendarState as? UiState.Success)?.data ?: emptyMap()
    val entry = datesMap[activeDate.dateKey]
    val status = entry?.bookingStatus ?: BookingStatus.AVAILABLE
    val adminEntry = adminBookings[activeDate.dateKey]
    BookingDetailSheet(
      date = activeDate,
      isPastDate = activeDate < today,
      currentStatus = status,
      adminBooking = adminEntry,
      isAdminUnlocked = isAdminUnlocked,
      isSaving = isSaving,
      onDismiss = viewModel::dismissSelectedDate,
      onSaveBooking = { newStatus, clientName, clientPhone, eventType, notes ->
        viewModel.saveBooking(
          dateKey = activeDate.dateKey,
          status = newStatus,
          clientName = clientName,
          clientPhone = clientPhone,
          eventType = eventType,
          notes = notes,
        )
      },
      onClearBooking = { viewModel.clearBooking(activeDate.dateKey) },
    )
  }
}

@Composable
private fun AdminModeBanner(bookedCount: Int, pendingCount: Int) {
  Surface(
    color = SoftGoldTint,
    shape = RoundedCornerShape(14.dp),
    border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.65f)),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(
        imageVector = Icons.Rounded.AdminPanelSettings,
        contentDescription = null,
        tint = EmeraldDeep,
        modifier = Modifier.size(22.dp),
      )
      Spacer(modifier = Modifier.width(10.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = "Admin Management Active",
          fontWeight = FontWeight.Bold,
          fontSize = 14.sp,
          color = EmeraldDeep,
        )
        Text(
          text = "Tap any date below to update availability. ($bookedCount Booked • $pendingCount Pending)",
          fontSize = 12.sp,
          color = CharcoalText.copy(alpha = 0.8f),
        )
      }
    }
  }
}

@Composable
private fun CalendarCardContainer(
  displayedMonth: HallYearMonth,
  datesMap: Map<String, CalendarDateEntry>,
  today: HallDate,
  isLoading: Boolean,
  onPrevMonth: () -> Unit,
  onNextMonth: () -> Unit,
  onDateClick: (HallDate) -> Unit,
) {
  Card(
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = EmeraldDeep),
    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 22.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Text(
        text = stringResource(R.string.booking_calendar_title),
        fontFamily = PlayfairDisplayFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 26.sp,
        color = PureWarmWhite,
        textAlign = TextAlign.Center,
      )

      Spacer(modifier = Modifier.height(4.dp))

      Text(
        text = stringResource(R.string.booking_calendar_motto),
        style = MaterialTheme.typography.labelSmall,
        color = GoldAccent,
        letterSpacing = 1.8.sp,
        textAlign = TextAlign.Center,
      )

      Spacer(modifier = Modifier.height(16.dp))

      HorizontalDivider(
        color = GoldHairline.copy(alpha = 0.65f),
        thickness = 1.dp,
      )

      Spacer(modifier = Modifier.height(14.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          text = displayedMonth.formatMonthYear(),
          fontFamily = PlayfairDisplayFontFamily,
          fontWeight = FontWeight.SemiBold,
          fontSize = 22.sp,
          color = PureWarmWhite,
          modifier = Modifier.testTag("displayed_month_title"),
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(
            onClick = onPrevMonth,
            modifier = Modifier.size(48.dp).testTag("prev_month_button"),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
              contentDescription = stringResource(R.string.previous_month_cd),
              tint = GoldAccent,
            )
          }
          IconButton(
            onClick = onNextMonth,
            modifier = Modifier.size(48.dp).testTag("next_month_button"),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
              contentDescription = stringResource(R.string.next_month_cd),
              tint = GoldAccent,
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      val weekdays = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        weekdays.forEach { dayName ->
          Text(
            text = dayName,
            color = GoldAccent,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      if (isLoading) {
        Box(
          modifier = Modifier.fillMaxWidth().height(260.dp),
          contentAlignment = Alignment.Center,
        ) {
          CircularProgressIndicator(color = GoldAccent)
        }
      } else {
        val daysInMonth = displayedMonth.daysInMonth()
        val offset = displayedMonth.firstDayOfWeekOffset()
        val totalCells = offset + daysInMonth
        val rows = (totalCells + 6) / 7

        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          for (row in 0 until rows) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              for (col in 0 until 7) {
                val cellIndex = row * 7 + col
                val dayNum = cellIndex - offset + 1
                if (dayNum in 1..daysInMonth) {
                  val date = HallDate(displayedMonth.year, displayedMonth.month, dayNum)
                  val status = datesMap[date.dateKey]?.bookingStatus ?: BookingStatus.AVAILABLE
                  val isPast = date < today
                  val isToday = date == today

                  CalendarDayTile(
                    dayNumber = dayNum,
                    status = status,
                    isPast = isPast,
                    isToday = isToday,
                    onClick = { onDateClick(date) },
                    modifier =
                      Modifier
                        .weight(1f)
                        .testTag("calendar_day_${date.dateKey}"),
                  )
                } else {
                  Spacer(modifier = Modifier.weight(1f))
                }
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(18.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        LegendItem(color = BookedRed, label = stringResource(R.string.legend_booked))
        LegendItem(color = EmeraldAvailableDot, label = stringResource(R.string.legend_available))
        LegendItem(color = PendingYellow, label = stringResource(R.string.legend_pending))
      }
    }
  }
}

@Composable
private fun CalendarDayTile(
  dayNumber: Int,
  status: BookingStatus,
  isPast: Boolean,
  isToday: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val tileColor =
    when {
      isPast && status == BookingStatus.AVAILABLE -> PastSlate
      status == BookingStatus.BOOKED -> BookedRed
      status == BookingStatus.PENDING -> PendingYellow
      else -> EmeraldAvailable
    }

  val textColor =
    when {
      isPast && status == BookingStatus.AVAILABLE -> Color.White.copy(alpha = 0.78f)
      else -> PureWarmWhite
    }

  val borderMod =
    if (isToday) {
      Modifier.border(1.5.dp, GoldAccent, RoundedCornerShape(10.dp))
    } else {
      Modifier
    }

  Box(
    modifier =
      modifier
        .heightIn(min = 48.dp)
        .clip(RoundedCornerShape(10.dp))
        .background(tileColor)
        .then(borderMod)
        .clickable(onClick = onClick),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = dayNumber.toString(),
      color = textColor,
      fontSize = 15.sp,
      fontWeight = FontWeight.SemiBold,
    )
  }
}

@Composable
private fun LegendItem(color: Color, label: String) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    Box(
      modifier = Modifier.size(10.dp).clip(CircleShape).background(color)
    )
    Spacer(modifier = Modifier.width(6.dp))
    Text(
      text = label,
      color = PureWarmWhite.copy(alpha = 0.9f),
      fontSize = 12.sp,
      fontWeight = FontWeight.Medium,
    )
  }
}

@Composable
private fun AdminUpcomingBookingsSection(
  bookings: List<AdminBookingEntry>,
  onBookingClick: (String) -> Unit,
) {
  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = PureWarmWhite),
    border = BorderStroke(1.dp, InputBorder),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Text(
        text = "Private Booking Records",
        fontFamily = PlayfairDisplayFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        color = EmeraldDeep,
      )
      bookings.forEach { booking ->
        val badgeColor =
          when (booking.bookingStatus) {
            BookingStatus.BOOKED -> BookedRed
            BookingStatus.PENDING -> PendingYellow
            BookingStatus.AVAILABLE -> EmeraldAvailable
          }
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = Color(0xFFF7F3EA),
          modifier =
            Modifier
              .fillMaxWidth()
              .clickable { onBookingClick(booking.dateKey) },
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text =
                  HallDate.fromKey(booking.dateKey)?.formatFullDisplay() ?: booking.dateKey,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = EmeraldDeep,
              )
              val summary =
                listOf(booking.clientName, booking.eventType)
                  .filter { it.isNotBlank() }
                  .joinToString(" • ")
              if (summary.isNotBlank()) {
                Text(
                  text = summary,
                  fontSize = 13.sp,
                  color = MutedSageText,
                )
              }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Box(
              modifier =
                Modifier
                  .clip(RoundedCornerShape(50))
                  .background(badgeColor)
                  .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
              Text(
                text = booking.bookingStatus.label,
                color = PureWarmWhite,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
              )
            }
          }
        }
      }
    }
  }
}
