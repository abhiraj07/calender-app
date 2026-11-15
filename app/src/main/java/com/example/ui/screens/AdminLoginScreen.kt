package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ui.AdminLoginFeedback
import com.example.ui.BookingCalendarViewModel
import com.example.ui.components.HallTopBar
import com.example.ui.theme.CharcoalText
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.InputBorder
import com.example.ui.theme.IvoryCream
import com.example.ui.theme.MutedSageText
import com.example.ui.theme.PlayfairDisplayFontFamily
import com.example.ui.theme.PureWarmWhite

@Composable
fun AdminLoginScreen(
  viewModel: BookingCalendarViewModel,
  onLoginSuccess: () -> Unit,
  onBackToCalendar: () -> Unit,
) {
  BackHandler { onBackToCalendar() }

  val feedback by viewModel.adminLoginFeedback.collectAsStateWithLifecycle()

  var usernameOrEmail by rememberSaveable { mutableStateOf("") }
  var password by rememberSaveable { mutableStateOf("") }
  var passwordVisible by rememberSaveable { mutableStateOf(false) }

  LaunchedEffect(feedback) {
    if (feedback is AdminLoginFeedback.Success) {
      viewModel.resetAdminLoginFeedback()
      onLoginSuccess()
    }
  }

  Scaffold(
    containerColor = IvoryCream,
    topBar = {
      HallTopBar(
        showAdminLoginButton = false,
        isAdminUnlocked = false,
        showBackToCalendarButton = true,
        onAdminLoginClick = {},
        onLockAdminClick = {},
        onBackToCalendarClick = onBackToCalendar,
      )
    },
  ) { innerPadding ->
    Box(
      modifier =
        Modifier
          .fillMaxSize()
          .background(IvoryCream)
          .padding(innerPadding)
          .navigationBarsPadding()
          .verticalScroll(rememberScrollState())
          .padding(24.dp),
      contentAlignment = Alignment.Center,
    ) {
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PureWarmWhite),
        border = BorderStroke(1.dp, InputBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier.fillMaxWidth().widthIn(max = 440.dp),
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(28.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
              text = stringResource(R.string.admin_login_title),
              fontFamily = PlayfairDisplayFontFamily,
              fontWeight = FontWeight.SemiBold,
              fontSize = 28.sp,
              color = EmeraldDeep,
            )
            Text(
              text = stringResource(R.string.admin_login_subtitle),
              style = MaterialTheme.typography.bodyMedium,
              color = MutedSageText,
            )
          }

          val textFieldColors =
            OutlinedTextFieldDefaults.colors(
              focusedBorderColor = EmeraldDeep,
              unfocusedBorderColor = InputBorder,
              focusedTextColor = CharcoalText,
              unfocusedTextColor = CharcoalText,
              cursorColor = EmeraldDeep,
            )

          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
              text = stringResource(R.string.username_or_email_label),
              style = MaterialTheme.typography.labelLarge,
              color = CharcoalText,
              fontSize = 13.sp,
            )
            OutlinedTextField(
              value = usernameOrEmail,
              onValueChange = {
                usernameOrEmail = it
                if (feedback is AdminLoginFeedback.Error) {
                  viewModel.resetAdminLoginFeedback()
                }
              },
              placeholder = {
                Text(
                  text = stringResource(R.string.username_or_email_hint),
                  color = MutedSageText.copy(alpha = 0.7f),
                  fontSize = 13.sp,
                )
              },
              singleLine = true,
              keyboardOptions =
                KeyboardOptions(
                  keyboardType = KeyboardType.Email,
                  imeAction = ImeAction.Next,
                ),
              colors = textFieldColors,
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.fillMaxWidth().testTag("admin_username_input"),
            )
          }

          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
              text = stringResource(R.string.password_label),
              style = MaterialTheme.typography.labelLarge,
              color = CharcoalText,
              fontSize = 13.sp,
            )
            OutlinedTextField(
              value = password,
              onValueChange = {
                password = it
                if (feedback is AdminLoginFeedback.Error) {
                  viewModel.resetAdminLoginFeedback()
                }
              },
              singleLine = true,
              visualTransformation =
                if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
              trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                  Icon(
                    imageVector =
                      if (passwordVisible) Icons.Rounded.VisibilityOff
                      else Icons.Rounded.Visibility,
                    contentDescription =
                      if (passwordVisible) "Hide password" else "Show password",
                    tint = MutedSageText,
                  )
                }
              },
              keyboardOptions =
                KeyboardOptions(
                  keyboardType = KeyboardType.Password,
                  imeAction = ImeAction.Done,
                ),
              keyboardActions =
                KeyboardActions(
                  onDone = { viewModel.attemptAdminLogin(usernameOrEmail, password) }
                ),
              colors = textFieldColors,
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.fillMaxWidth().testTag("admin_password_input"),
            )
          }

          val errorFeedback = feedback as? AdminLoginFeedback.Error
          if (errorFeedback != null) {
            Text(
              text = errorFeedback.message,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.error,
              modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            )
          }

          Spacer(modifier = Modifier.height(2.dp))

          Button(
            onClick = { viewModel.attemptAdminLogin(usernameOrEmail, password) },
            shape = RoundedCornerShape(10.dp),
            colors =
              ButtonDefaults.buttonColors(
                containerColor = GoldAccent,
                contentColor = EmeraldDeep,
              ),
            modifier =
              Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .testTag("login_securely_button"),
          ) {
            Text(
              text = stringResource(R.string.login_securely),
              fontWeight = FontWeight.Bold,
              fontSize = 15.sp,
            )
          }
        }
      }
    }
  }
}
