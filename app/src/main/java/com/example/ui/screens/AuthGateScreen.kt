package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.auth.attemptAutoSignIn
import com.example.auth.onGoogleSignInClicked
import com.example.ui.components.HallTopBar
import com.example.ui.theme.BookedRed
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.InputBorder
import com.example.ui.theme.IvoryCream
import com.example.ui.theme.MutedSageText
import com.example.ui.theme.PlayfairDisplayFontFamily
import com.example.ui.theme.PureWarmWhite
import com.example.ui.theme.SoftRedTint

@Composable
fun AuthGateScreen(onAuthSuccess: () -> Unit) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }

  var isLoading by remember { mutableStateOf(true) }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  LaunchedEffect(Unit) {
    attemptAutoSignIn(
      context = context,
      credentialManager = credentialManager,
      onAuthSuccess = {
        isLoading = false
        onAuthSuccess()
      },
      onUnauthenticated = { isLoading = false },
      scope = scope,
    )
  }

  Scaffold(
    containerColor = IvoryCream,
    topBar = {
      HallTopBar(
        showAdminLoginButton = false,
        isAdminUnlocked = false,
        showBackToCalendarButton = false,
        onAdminLoginClick = {},
        onLockAdminClick = {},
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
          .navigationBarsPadding()
          .padding(24.dp),
      contentAlignment = Alignment.Center,
    ) {
      Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = PureWarmWhite),
        border = BorderStroke(1.dp, InputBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth().widthIn(max = 420.dp),
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(28.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
          Box(
            modifier =
              Modifier
                .size(72.dp)
                .clip(CircleShape)
                .border(1.5.dp, GoldAccent, CircleShape),
            contentAlignment = Alignment.Center,
          ) {
            Image(
              painter = painterResource(id = R.drawable.ic_kr_monogram_1794735372457),
              contentDescription = stringResource(R.string.kr_logo_cd),
              contentScale = ContentScale.Crop,
              modifier = Modifier.size(72.dp).clip(CircleShape),
            )
          }

          Text(
            text = stringResource(R.string.plan_your_celebration),
            style = MaterialTheme.typography.labelSmall,
            color = GoldAccent,
            letterSpacing = 2.sp,
          )

          Text(
            text = stringResource(R.string.google_gate_title),
            fontFamily = PlayfairDisplayFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 26.sp,
            lineHeight = 32.sp,
            color = EmeraldDeep,
            textAlign = TextAlign.Center,
          )

          Text(
            text = stringResource(R.string.google_gate_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MutedSageText,
            textAlign = TextAlign.Center,
          )

          if (errorMessage != null) {
            Box(
              modifier =
                Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(10.dp))
                  .background(SoftRedTint)
                  .padding(12.dp)
            ) {
              Text(
                text = errorMessage!!,
                style = MaterialTheme.typography.bodyMedium,
                color = BookedRed,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
              )
            }
          }

          Spacer(modifier = Modifier.height(4.dp))

          Button(
            onClick = {
              errorMessage = null
              isLoading = true
              onGoogleSignInClicked(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {
                  isLoading = false
                  onAuthSuccess()
                },
                onAuthError = { msg ->
                  isLoading = false
                  errorMessage = msg
                },
                scope = scope,
                onAuthCancelled = { isLoading = false },
              )
            },
            enabled = !isLoading,
            shape = RoundedCornerShape(12.dp),
            colors =
              ButtonDefaults.buttonColors(
                containerColor = GoldAccent,
                contentColor = EmeraldDeep,
              ),
            modifier =
              Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .testTag("google_sign_in_button"),
          ) {
            if (isLoading) {
              CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = EmeraldDeep,
                strokeWidth = 2.5.dp,
              )
            } else {
              Icon(
                imageVector = Icons.Rounded.VerifiedUser,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = stringResource(R.string.sign_in_with_google),
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
              )
            }
          }
        }
      }
    }
  }
}
