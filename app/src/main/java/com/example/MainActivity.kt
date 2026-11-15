package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.auth.signOut
import com.example.data.BookingRepository
import com.example.ui.BookingCalendarViewModel
import com.example.ui.screens.AdminLoginScreen
import com.example.ui.screens.AuthGateScreen
import com.example.ui.screens.BookingCalendarScreen
import com.example.ui.theme.MyApplicationTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

const val ROUTE_CALENDAR = "booking_calendar"
const val ROUTE_ADMIN_LOGIN = "admin_login"

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
  val listener = FirebaseAuth.AuthStateListener { auth -> trySend(auth.currentUser) }
  addAuthStateListener(listener)
  awaitClose { removeAuthStateListener(listener) }
}

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
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
  val currentUser by
    auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)

  val user = currentUser
  if (user == null) {
    AuthGateScreen(onAuthSuccess = {})
  } else {
    key(user.uid) {
      AuthenticatedHallApp(
        currentUserId = user.uid,
        currentUserEmail = user.email,
      )
    }
  }
}

@Composable
private fun AuthenticatedHallApp(
  currentUserId: String,
  currentUserEmail: String?,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }

  val viewModel: BookingCalendarViewModel =
    viewModel(
      key = currentUserId,
      factory =
        viewModelFactory {
          initializer {
            val app =
              checkNotNull(this[APPLICATION_KEY]) {
                "APPLICATION_KEY missing from CreationExtras"
              }
            val databaseId = app.getString(R.string.firestore_database_id)
            val db = FirebaseFirestore.getInstance(databaseId)
            BookingCalendarViewModel(
              repository = BookingRepository(db),
              currentUserId = currentUserId,
              currentUserEmail = currentUserEmail,
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
        onSignOutClick = {
          signOut(credentialManager = credentialManager, scope = scope)
        },
      )
    }
    composable(ROUTE_ADMIN_LOGIN) {
      AdminLoginScreen(
        viewModel = viewModel,
        onLoginSuccess = { navController.popBackStack() },
        onBackToCalendar = { navController.popBackStack() },
        onSwitchGoogleAccount = {
          signOut(credentialManager = credentialManager, scope = scope)
        },
      )
    }
  }
}
