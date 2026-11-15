package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
    setContent {
      MyApplicationTheme {
        AppNavigation()
      }
    }
  }
}

@Composable
fun AppNavigation() {
  AuthenticatedHallApp()
}

@Composable
private fun AuthenticatedHallApp() {

  val viewModel: BookingCalendarViewModel =
    viewModel(
      factory =
        viewModelFactory {
          initializer {
            BookingCalendarViewModel(
              repository = BookingRepository(),
              currentUserId = "",
              currentUserEmail = null,
            )
          }
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
        onLoginSuccess = { navController.popBackStack() },
        onBackToCalendar = { navController.popBackStack() },
      )
    }
  }
}
