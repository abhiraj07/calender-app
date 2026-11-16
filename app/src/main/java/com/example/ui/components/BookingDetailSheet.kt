package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.EventBusy
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.AdminBookingEntry
import com.example.data.BookingStatus
import com.example.data.HallDate
import com.example.ui.theme.BookedRed
import com.example.ui.theme.CharcoalText
import com.example.ui.theme.EmeraldAvailable
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.InputBorder
import com.example.ui.theme.MutedSageText
import com.example.ui.theme.PastSlate
import com.example.ui.theme.PendingYellow
import com.example.ui.theme.PlayfairDisplayFontFamily
import com.example.ui.theme.PureWarmWhite
import com.example.ui.theme.SoftGoldTint
import com.example.ui.theme.SoftGreenTint
import com.example.ui.theme.SoftRedTint

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingDetailSheet(
  date: HallDate,
  isPastDate: Boolean,
  currentStatus: BookingStatus,
  adminBooking: AdminBookingEntry?,
  isAdminUnlocked: Boolean,
  isSaving: Boolean,
  onDismiss: () -> Unit,
  onSaveBooking: (BookingStatus, String, String, String, String) -> Unit,
  onClearBooking: () -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  var selectedStatus by remember(date.dateKey, currentStatus) { mutableStateOf(currentStatus) }
  var clientName by remember(date.dateKey, adminBooking) {
    mutableStateOf(adminBooking?.clientName.orEmpty())
  }
  var clientPhone by remember(date.dateKey, adminBooking) {
    mutableStateOf(adminBooking?.clientPhone.orEmpty())
  }
  var eventType by remember(date.dateKey, adminBooking) {
    mutableStateOf(adminBooking?.eventType.orEmpty())
  }
  var eventTypeExpanded by remember { mutableStateOf(false) }
  val eventTypeOptions = listOf("Marriage", "Engagement", "Anniversary", "Meeting", "Birthday Party", "Others")
  var notes by remember(date.dateKey, adminBooking) {
    mutableStateOf(adminBooking?.notes.orEmpty())
  }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = PureWarmWhite,
    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
  ) {
    Column(
      modifier =
        Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .navigationBarsPadding()
          .padding(horizontal = 24.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Text(
        text = stringResource(R.string.plan_your_celebration),
        style = MaterialTheme.typography.labelSmall,
        color = GoldAccent,
      )

      Text(
        text = date.formatFullDisplay(),
        fontFamily = PlayfairDisplayFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        color = EmeraldDeep,
      )

      StatusSummaryBanner(status = currentStatus, isPastDate = isPastDate)

      if (!isAdminUnlocked) {
        val guidance =
          when {
            isPastDate -> "This date has passed and is no longer open for new reservations."
            currentStatus == BookingStatus.BOOKED ->
              "Shree Banquet Hall is already reserved on this date. Please explore green dates on the calendar for open availability."
            currentStatus == BookingStatus.PENDING ->
              "A celebration inquiry is currently pending confirmation for this date. Contact Shree Banquet Hall management for waitlist details."
            else ->
              "This date is currently available at Shree Banquet Hall. Contact our management team to place a booking or hold."
          }

        Text(
          text = guidance,
          style = MaterialTheme.typography.bodyMedium,
          color = MutedSageText,
        )

        Spacer(modifier = Modifier.height(4.dp))

        Button(
          onClick = onDismiss,
          shape = RoundedCornerShape(12.dp),
          colors =
            ButtonDefaults.buttonColors(
              containerColor = EmeraldDeep,
              contentColor = PureWarmWhite,
            ),
          modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        ) {
          Text(text = stringResource(R.string.close), fontWeight = FontWeight.SemiBold)
        }
      } else {
        Text(
          text = "Update Date Status",
          style = MaterialTheme.typography.titleMedium,
          color = EmeraldDeep,
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          StatusSelectorChip(
            label = "Available",
            selected = selectedStatus == BookingStatus.AVAILABLE,
            activeColor = EmeraldAvailable,
            onClick = { selectedStatus = BookingStatus.AVAILABLE },
            modifier = Modifier.weight(1f).testTag("status_chip_available"),
          )
          StatusSelectorChip(
            label = "Pending",
            selected = selectedStatus == BookingStatus.PENDING,
            activeColor = PendingYellow,
            onClick = { selectedStatus = BookingStatus.PENDING },
            modifier = Modifier.weight(1f).testTag("status_chip_pending"),
          )
          StatusSelectorChip(
            label = "Booked",
            selected = selectedStatus == BookingStatus.BOOKED,
            activeColor = BookedRed,
            onClick = { selectedStatus = BookingStatus.BOOKED },
            modifier = Modifier.weight(1f).testTag("status_chip_booked"),
          )
        }

        val fieldColors =
          OutlinedTextFieldDefaults.colors(
            focusedBorderColor = EmeraldDeep,
            unfocusedBorderColor = InputBorder,
            focusedLabelColor = EmeraldDeep,
            cursorColor = EmeraldDeep,
          )

        OutlinedTextField(
          value = clientName,
          onValueChange = { clientName = it },
          label = { Text(stringResource(R.string.client_name_label)) },
          singleLine = true,
          colors = fieldColors,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth().testTag("booking_client_name_input"),
        )

        OutlinedTextField(
          value = clientPhone,
          onValueChange = { clientPhone = it },
          label = { Text(stringResource(R.string.client_phone_label)) },
          singleLine = true,
          colors = fieldColors,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth().testTag("booking_client_phone_input"),
        )

        ExposedDropdownMenuBox(
          expanded = eventTypeExpanded,
          onExpandedChange = { eventTypeExpanded = !eventTypeExpanded },
          modifier = Modifier.fillMaxWidth(),
        ) {
          OutlinedTextField(
            value = eventType,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.event_type_label)) },
            placeholder = { Text("Select event type") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = eventTypeExpanded) },
            colors = fieldColors,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.menuAnchor().fillMaxWidth().testTag("booking_event_type_input"),
          )
          ExposedDropdownMenu(
            expanded = eventTypeExpanded,
            onDismissRequest = { eventTypeExpanded = false },
          ) {
            eventTypeOptions.forEach { option ->
              DropdownMenuItem(
                text = { Text(option) },
                onClick = {
                  eventType = option
                  eventTypeExpanded = false
                },
              )
            }
          }
        }

        OutlinedTextField(
          value = notes,
          onValueChange = { notes = it },
          label = { Text(stringResource(R.string.notes_label)) },
          minLines = 2,
          maxLines = 4,
          colors = fieldColors,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth().testTag("booking_notes_input"),
        )

        Button(
          onClick = { onSaveBooking(selectedStatus, clientName, clientPhone, eventType, notes) },
          enabled = !isSaving,
          shape = RoundedCornerShape(12.dp),
          colors =
            ButtonDefaults.buttonColors(
              containerColor = GoldAccent,
              contentColor = EmeraldDeep,
            ),
          modifier =
            Modifier
              .fillMaxWidth()
              .heightIn(min = 50.dp)
              .testTag("save_booking_button"),
        ) {
          if (isSaving) {
            CircularProgressIndicator(
              modifier = Modifier.size(20.dp),
              color = EmeraldDeep,
              strokeWidth = 2.dp,
            )
          } else {
            Icon(
              imageVector = Icons.Rounded.CheckCircle,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = stringResource(R.string.save_booking),
              fontWeight = FontWeight.Bold,
            )
          }
        }

        if (currentStatus != BookingStatus.AVAILABLE || adminBooking != null) {
          OutlinedButton(
            onClick = onClearBooking,
            enabled = !isSaving,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, EmeraldAvailable.copy(alpha = 0.5f)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldAvailable),
            modifier =
              Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .testTag("clear_booking_button"),
          ) {
            Icon(
              imageVector = Icons.Rounded.DeleteOutline,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = stringResource(R.string.clear_booking),
              fontWeight = FontWeight.SemiBold,
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))
    }
  }
}

@Composable
private fun StatusSummaryBanner(status: BookingStatus, isPastDate: Boolean) {
  val (bgColor, fgColor, icon, label) =
    when {
      isPastDate && status == BookingStatus.AVAILABLE ->
        Quad(Color(0xFFECEEEF), PastSlate, Icons.Rounded.Schedule, stringResource(R.string.status_past))
      status == BookingStatus.BOOKED ->
        Quad(SoftRedTint, BookedRed, Icons.Rounded.EventBusy, stringResource(R.string.status_booked))
      status == BookingStatus.PENDING ->
        Quad(SoftGoldTint, Color(0xFF8F6A14), Icons.Rounded.Schedule, stringResource(R.string.status_pending))
      else ->
        Quad(SoftGreenTint, EmeraldAvailable, Icons.Rounded.EventAvailable, stringResource(R.string.status_available))
    }

  Surface(
    color = bgColor,
    shape = RoundedCornerShape(12.dp),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(fgColor))
      Spacer(modifier = Modifier.width(10.dp))
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = fgColor,
        modifier = Modifier.size(18.dp),
      )
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        text = label,
        color = fgColor,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
      )
    }
  }
}

@Composable
private fun StatusSelectorChip(
  label: String,
  selected: Boolean,
  activeColor: Color,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  FilterChip(
    selected = selected,
    onClick = onClick,
    label = {
      Text(
        text = label,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        fontSize = 13.sp,
        maxLines = 1,
      )
    },
    colors =
      FilterChipDefaults.filterChipColors(
        selectedContainerColor = activeColor,
        selectedLabelColor = PureWarmWhite,
        containerColor = Color(0xFFF3EFE6),
        labelColor = CharcoalText,
      ),
    border =
      FilterChipDefaults.filterChipBorder(
        enabled = true,
        selected = selected,
        borderColor = InputBorder,
        selectedBorderColor = activeColor,
      ),
    modifier = modifier.heightIn(min = 48.dp),
  )
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
