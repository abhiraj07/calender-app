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
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
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
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

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
  var retryKey by remember { mutableIntStateOf(0) }
  var authError by remember { mutableStateOf<String?>(null) }
  val scope = rememberCoroutineScope()

  LaunchedEffect(auth, currentUser, retryKey) {
    if (currentUser == null) {
      authError = null
      try {
        auth.signInAnonymously().await()
      } catch (error: Exception) {
        authError = error.localizedMessage ?: "Unable to connect to the booking service."
      }
    }
  }

  val user = currentUser
  if (user == null) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
      Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
      ) {
        if (authError == null) {
          CircularProgressIndicator()
          Text("Opening Shree Banquet Hall…")
        } else {
          Text("Unable to open the booking calendar.")
          Text(authError!!)
          Button(onClick = { retryKey++ }) { Text("Retry") }
        }
      }
    }
  } else {
    AuthenticatedHallApp(
      currentUserId = user.uid,
      currentUserEmail = user.email,
    )
  }
}

@Composable
private fun AuthenticatedHallApp(
  currentUserId: String,
  currentUserEmail: String?,
) {

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
          viewModel.lockAdminMode()
          Firebase.auth.signOut()
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
