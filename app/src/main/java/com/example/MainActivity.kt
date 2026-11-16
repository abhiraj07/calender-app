package com.example

import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.data.BookingRepository
import com.example.ui.BookingCalendarViewModel
import com.example.ui.screens.AdminLoginScreen
import com.example.ui.screens.BookingCalendarScreen
import com.example.ui.theme.MyApplicationTheme

const val ROUTE_CALENDAR = "booking_calendar"
const val ROUTE_ADMIN_LOGIN = "admin_login"

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent { MyApplicationTheme { AppNavigation() } }
  }
}

@Composable
fun AppNavigation() {
  val context = LocalContext.current
  val repository = remember(context) { BookingRepository(context.applicationContext) }
  val viewModel: BookingCalendarViewModel = viewModel(
    factory = viewModelFactory {
      initializer { BookingCalendarViewModel(repository) }
    },
  )
  val navController = rememberNavController()

  NavHost(navController = navController, startDestination = ROUTE_CALENDAR) {
    composable(ROUTE_CALENDAR) {
      BookingCalendarScreen(
        viewModel = viewModel,
        onNavigateToAdminLogin = {
          viewModel.resetAdminLoginFeedback()
          navController.navigate(ROUTE_ADMIN_LOGIN) { launchSingleTop = true }
        },
      )
    }
    composable(ROUTE_ADMIN_LOGIN) {
      AdminLoginScreen(
        viewModel = viewModel,
        onLoginSuccess = {
          val navigationStartedAt = SystemClock.elapsedRealtime()
          navController.popBackStack()
          Log.d("LoginTiming", "calendar_navigation_pop_call durationMs=${SystemClock.elapsedRealtime() - navigationStartedAt}")
        },
        onBackToCalendar = { navController.popBackStack() },
        onSwitchGoogleAccount = {},
      )
    }
  }
}
