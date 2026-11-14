package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.firebase.FirestoreClientProvider
import com.example.data.model.UserRole
import com.example.data.network.ConnectivityObserver
import com.example.data.repository.PamsimasRepository
import com.example.ui.admin.AdminPortalScreen
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.signOutFromApp
import com.example.ui.pelanggan.PelangganPortalScreen
import com.example.ui.theme.PamsimasWeruTheme
import com.example.ui.viewmodel.AdminViewModel
import com.example.ui.viewmodel.PelangganViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      PamsimasWeruTheme {
        PamsimasAppNavigation()
      }
    }
  }
}

private fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
  val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
    trySend(firebaseAuth.currentUser)
  }
  addAuthStateListener(listener)
  awaitClose { removeAuthStateListener(listener) }
}

@Composable
fun PamsimasAppNavigation(auth: FirebaseAuth = Firebase.auth) {
  val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
  val user = currentUser

  if (user == null) {
    AuthScreen(onAuthSuccess = { /* AuthStateListener drives navigation */ })
  } else {
    key(user.uid) {
      AuthenticatedRoleRouter(
        userUid = user.uid,
        userEmail = user.email,
        userName = user.displayName
      )
    }
  }
}

@Composable
private fun AuthenticatedRoleRouter(
  userUid: String,
  userEmail: String?,
  userName: String?
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember { CredentialManager.create(context) }

  var resolvedRole by remember(userUid) { mutableStateOf<UserRole?>(null) }
  var previewAsPelanggan by rememberSaveable(userUid) { mutableStateOf(false) }

  LaunchedEffect(userUid) {
    val db = FirestoreClientProvider.getDatabase(context)
    val repo = PamsimasRepository(db, Firebase.auth)
    val isAdmin = repo.isCurrentUserAdmin()
    resolvedRole = if (isAdmin) UserRole.ADMIN else UserRole.PELANGGAN
  }

  val onSignOut: () -> Unit = {
    signOutFromApp(credentialManager = credentialManager, scope = scope)
  }

  when (resolvedRole) {
    null -> {
      Scaffold { innerPadding ->
        Column(
          modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
          verticalArrangement = Arrangement.Center,
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          CircularProgressIndicator()
          Spacer(modifier = Modifier.height(12.dp))
          Text(
            text = "Memuat akses PAMSIMAS WERU...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    }
    UserRole.ADMIN -> {
      if (previewAsPelanggan) {
        BackHandler { previewAsPelanggan = false }
        val pelangganVm: PelangganViewModel = viewModel(
          key = "preview_$userUid",
          factory = viewModelFactory {
            initializer {
              val app = checkNotNull(this[APPLICATION_KEY])
              val db = FirestoreClientProvider.getDatabase(app)
              PelangganViewModel(
                repository = PamsimasRepository(db, Firebase.auth),
                connectivityObserver = ConnectivityObserver(app),
                currentUserId = userUid,
                currentUserEmail = userEmail,
                currentUserName = userName
              )
            }
          }
        )
        PelangganPortalScreen(
          viewModel = pelangganVm,
          isAdminPreview = true,
          onExitPreview = { previewAsPelanggan = false },
          onSignOut = onSignOut
        )
      } else {
        val adminVm: AdminViewModel = viewModel(
          key = "admin_$userUid",
          factory = viewModelFactory {
            initializer {
              val app = checkNotNull(this[APPLICATION_KEY])
              val db = FirestoreClientProvider.getDatabase(app)
              AdminViewModel(
                repository = PamsimasRepository(db, Firebase.auth),
                connectivityObserver = ConnectivityObserver(app),
                currentAdminName = userName ?: "Hendika WP"
              )
            }
          }
        )
        AdminPortalScreen(
          viewModel = adminVm,
          onSignOut = onSignOut,
          onPreviewPelanggan = { previewAsPelanggan = true }
        )
      }
    }
    UserRole.PELANGGAN -> {
      val pelangganVm: PelangganViewModel = viewModel(
        key = "pelanggan_$userUid",
        factory = viewModelFactory {
          initializer {
            val app = checkNotNull(this[APPLICATION_KEY])
            val db = FirestoreClientProvider.getDatabase(app)
            PelangganViewModel(
              repository = PamsimasRepository(db, Firebase.auth),
              connectivityObserver = ConnectivityObserver(app),
              currentUserId = userUid,
              currentUserEmail = userEmail,
              currentUserName = userName
            )
          }
        }
      )
      PelangganPortalScreen(
        viewModel = pelangganVm,
        isAdminPreview = false,
        onSignOut = onSignOut
      )
    }
  }
}
