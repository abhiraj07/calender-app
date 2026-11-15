package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
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
  val repository = remember { BookingRepository() }
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
        onSignOutClick = { viewModel.lockAdminMode() },
      )
    }
    composable(ROUTE_ADMIN_LOGIN) {
      AdminLoginScreen(
        viewModel = viewModel,
        onLoginSuccess = { navController.popBackStack() },
        onBackToCalendar = { navController.popBackStack() },
        onSwitchGoogleAccount = {},
      )
    }
  }
}
