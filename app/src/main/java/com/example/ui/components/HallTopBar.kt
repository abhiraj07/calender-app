package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.EmeraldSurface
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldLight
import com.example.ui.theme.PlayfairDisplayFontFamily
import com.example.ui.theme.SubtleIvoryText

@Composable
fun HallTopBar(
  showAdminLoginButton: Boolean,
  isAdminUnlocked: Boolean,
  showBackToCalendarButton: Boolean,
  onAdminLoginClick: () -> Unit,
  onLockAdminClick: () -> Unit,
  onBackToCalendarClick: () -> Unit,
) {
  Surface(
    color = EmeraldDeep,
    shadowElevation = 4.dp,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier =
        Modifier
          .fillMaxWidth()
          .statusBarsPadding()
          .padding(horizontal = 16.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.weight(1f),
      ) {
        Box(
          modifier =
            Modifier
              .size(40.dp)
              .clip(CircleShape)
              .border(1.dp, GoldAccent.copy(alpha = 0.65f), CircleShape),
          contentAlignment = Alignment.Center,
        ) {
          Image(
            painter = painterResource(id = R.drawable.ic_kr_monogram_1794735372457),
            contentDescription = stringResource(R.string.kr_logo_cd),
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(40.dp).clip(CircleShape),
          )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
          Text(
            text = stringResource(R.string.app_name),
            fontFamily = PlayfairDisplayFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            color = GoldLight,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = stringResource(R.string.brand_subtitle),
            style = MaterialTheme.typography.labelSmall,
            fontSize = 9.sp,
            letterSpacing = 1.4.sp,
            color = SubtleIvoryText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }

      Spacer(modifier = Modifier.width(8.dp))

      when {
        showBackToCalendarButton -> {
          OutlinedButton(
            onClick = onBackToCalendarClick,
            shape = RoundedCornerShape(50),
            border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.7f)),
            colors =
              ButtonDefaults.outlinedButtonColors(
                containerColor = EmeraldSurface,
                contentColor = GoldLight,
              ),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
            modifier =
              Modifier
                .defaultMinSize(minHeight = 48.dp)
                .testTag("back_to_calendar_button"),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
              contentDescription = null,
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = stringResource(R.string.back_to_calendar),
              fontSize = 13.sp,
              fontWeight = FontWeight.SemiBold,
            )
          }
        }
        isAdminUnlocked -> {
          OutlinedButton(
            onClick = onLockAdminClick,
            shape = RoundedCornerShape(50),
            border = BorderStroke(1.dp, GoldAccent),
            colors =
              ButtonDefaults.outlinedButtonColors(
                containerColor = EmeraldSurface,
                contentColor = GoldLight,
              ),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
            modifier =
              Modifier
                .defaultMinSize(minHeight = 48.dp)
                .testTag("lock_admin_button"),
          ) {
            Icon(
              imageVector = Icons.Rounded.AdminPanelSettings,
              contentDescription = null,
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = stringResource(R.string.admin_active_badge),
              fontSize = 13.sp,
              fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
              imageVector = Icons.Rounded.Lock,
              contentDescription = null,
              modifier = Modifier.size(14.dp),
            )
          }
        }
        showAdminLoginButton -> {
          Button(
            onClick = onAdminLoginClick,
            shape = RoundedCornerShape(50),
            colors =
              ButtonDefaults.buttonColors(
                containerColor = GoldAccent,
                contentColor = EmeraldDeep,
              ),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            modifier =
              Modifier
                .defaultMinSize(minHeight = 48.dp)
                .testTag("admin_login_button"),
          ) {
            Text(
              text = stringResource(R.string.admin_login_button),
              fontSize = 13.sp,
              fontWeight = FontWeight.Bold,
            )
          }
        }
      }
    }
  }
}
