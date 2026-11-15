package com.example.ui.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Login
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.ui.theme.LeafGreenContainer
import com.example.ui.theme.LeafGreenOnContainer
import com.example.ui.theme.LeafGreenSecondary
import com.example.ui.theme.WaterBlueContainer
import com.example.ui.theme.WaterBlueOnContainer
import com.example.ui.theme.WaterBluePrimary
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

@Composable
fun AuthScreen(
  onAuthSuccess: () -> Unit
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember { CredentialManager.create(context) }
  var isLoading by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  var email by remember { mutableStateOf("") }
  var password by remember { mutableStateOf("") }
  var passwordVisible by remember { mutableStateOf(false) }
  var isRegisterMode by remember { mutableStateOf(false) }

  LaunchedEffect(Unit) {
    attemptAutoSignIn(
      context = context,
      credentialManager = credentialManager,
      onAuthSuccess = onAuthSuccess,
      onUnauthenticated = { },
      scope = scope
    )
  }

  Scaffold(
    contentWindowInsets = WindowInsets(0, 0, 0, 0),
    containerColor = MaterialTheme.colorScheme.background
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .verticalScroll(rememberScrollState())
          .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(250.dp)
        ) {
          Image(
            painter = painterResource(id = R.drawable.img_weru_water_hero),
            contentDescription = "Ilustrasi sumber air bersih Dusun Weru",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
          )
          Box(
            modifier = Modifier
              .fillMaxSize()
              .background(
                Brush.verticalGradient(
                  colors = listOf(
                    Color(0x66033982),
                    Color(0xBB0457C1),
                    MaterialTheme.colorScheme.background
                  )
                )
              )
          )
          Column(
            modifier = Modifier
              .fillMaxSize()
              .statusBarsPadding()
              .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.Bottom
          ) {
            Surface(
              color = LeafGreenSecondary,
              shape = RoundedCornerShape(50)
            ) {
              Text(
                text = "BY HENDIKA WP",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
              )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = "PAMSIMAS WERU",
              style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
              color = Color.White
            )
            Text(
              text = "Sistem Pengelolaan Air Bersih Dusun Weru",
              style = MaterialTheme.typography.bodyMedium,
              color = Color.White.copy(alpha = 0.92f)
            )
          }
        }

        Column(
          modifier = Modifier
            .widthIn(max = 560.dp)
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
          ) {
            Column(modifier = Modifier.padding(18.dp)) {
              Text(
                text = if (isRegisterMode) "Daftar Akun Baru" else "Masuk Portal PAMSIMAS",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "Masuk dengan Email & Password, atau gunakan akun Google Anda.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )

              Spacer(modifier = Modifier.height(14.dp))

              OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                leadingIcon = { Icon(Icons.Rounded.Email, null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("email_field"),
                shape = RoundedCornerShape(12.dp)
              )
              Spacer(modifier = Modifier.height(10.dp))

              OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                leadingIcon = { Icon(Icons.Rounded.Lock, null) },
                trailingIcon = {
                  IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                      if (passwordVisible) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                      null
                    )
                  }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("password_field"),
                shape = RoundedCornerShape(12.dp)
              )

              if (errorMessage != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                  color = MaterialTheme.colorScheme.errorContainer,
                  shape = RoundedCornerShape(10.dp),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Text(
                    text = errorMessage!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(10.dp)
                  )
                }
              }

              Spacer(modifier = Modifier.height(14.dp))

              Button(
                onClick = {
                  errorMessage = null
                  if (email.isBlank() || password.isBlank()) {
                    errorMessage = "Email dan password wajib diisi."
                    return@Button
                  }
                  if (password.length < 6) {
                    errorMessage = "Password minimal 6 karakter."
                    return@Button
                  }
                  isLoading = true
                  scope.launch {
                    try {
                      if (isRegisterMode) {
                        Firebase.auth.createUserWithEmailAndPassword(email.trim(), password).await()
                      } else {
                        Firebase.auth.signInWithEmailAndPassword(email.trim(), password).await()
                      }
                      isLoading = false
                      ensureUserDocAndLogin(onAuthSuccess)
                    } catch (e: Exception) {
                      isLoading = false
                      errorMessage = when {
                        e.message?.contains("already in use") == true -> "Email sudah terdaftar, silakan login."
                        e.message?.contains("invalid-credential") == true -> "Email / password salah."
                        e.message?.contains("invalid-email") == true -> "Format email tidak valid."
                        else -> e.localizedMessage ?: "Gagal masuk."
                      }
                    }
                  }
                },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = WaterBluePrimary)
              ) {
                if (isLoading) {
                  CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White, strokeWidth = 2.5.dp)
                  Spacer(modifier = Modifier.width(10.dp))
                  Text("Memproses...")
                } else {
                  Text(if (isRegisterMode) "Daftar" else "Masuk dengan Email")
                }
              }

              TextButton(
                onClick = {
                  isRegisterMode = !isRegisterMode
                  errorMessage = null
                },
                modifier = Modifier.fillMaxWidth()
              ) {
                Text(if (isRegisterMode) "Sudah punya akun? Masuk" else "Belum punya akun? Daftar di sini")
              }

              Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                Box(modifier = Modifier.weight(1f).height(1.dp).background(Color.Gray.copy(alpha = 0.3f)))
                Text("  ATAU  ", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                Box(modifier = Modifier.weight(1f).height(1.dp).background(Color.Gray.copy(alpha = 0.3f)))
              }

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
                    onAuthCancelled = { isLoading = false }
                  )
                },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth().height(52.dp).testTag("google_sign_in_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White)
              ) {
                Icon(imageVector = Icons.Rounded.Login, contentDescription = null, tint = Color.Black, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Masuk dengan Google", style = MaterialTheme.typography.labelLarge, color = Color.Black)
              }

              Spacer(modifier = Modifier.height(14.dp))
              RoleAccessInfoTile(
                icon = Icons.Rounded.AdminPanelSettings,
                title = "Akses Admin (Pengelola)",
                desc = "Kelola pelanggan, catat meteran, tagihan otomatis, pembayaran, laporan & kuitansi.",
                containerColor = WaterBlueContainer,
                iconTint = WaterBlueOnContainer
              )
              Spacer(modifier = Modifier.height(10.dp))
              RoleAccessInfoTile(
                icon = Icons.Rounded.Person,
                title = "Akses Pelanggan",
                desc = "Lihat profil sambungan, riwayat meteran, tagihan, pembayaran & kirim bukti transfer.",
                containerColor = LeafGreenContainer,
                iconTint = LeafGreenOnContainer
              )
              Spacer(modifier = Modifier.height(12.dp))
              Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                  Icon(imageVector = Icons.Rounded.WaterDrop, contentDescription = null, tint = WaterBluePrimary, modifier = Modifier.size(20.dp))
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(
                    text = "Tarif Rp2.000/m³ • Tambahan beban Rp5.000/bulan jika pemakaian < 8 m³",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(imageVector = Icons.Rounded.CloudDone, contentDescription = null, tint = LeafGreenSecondary, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Dukungan Offline & Sinkronisasi Firebase Cloud",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }
    }
  }
}

@Composable
private fun RoleAccessInfoTile(
  icon: ImageVector,
  title: String,
  desc: String,
  containerColor: Color,
  iconTint: Color
) {
  Row(
    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(containerColor.copy(alpha = 0.55f)).padding(12.dp),
    verticalAlignment = Alignment.Top
  ) {
    Box(
      modifier = Modifier.size(34.dp).clip(CircleShape).background(containerColor),
      contentAlignment = Alignment.Center
    ) {
      Icon(imageVector = icon, contentDescription = title, tint = iconTint, modifier = Modifier.size(19.dp))
    }
    Spacer(modifier = Modifier.width(10.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(text = title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
      Spacer(modifier = Modifier.height(2.dp))
      Text(text = desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
  }
}

fun attemptAutoSignIn(
  context: Context,
  credentialManager: CredentialManager,
  onAuthSuccess: () -> Unit,
  onUnauthenticated: () -> Unit,
  scope: CoroutineScope
) {
  if (Firebase.auth.currentUser != null) {
    scope.launch { ensureUserDocAndLogin(onAuthSuccess) }
    return
  }
  val clientId = try {
    context.getString(R.string.default_web_client_id)
  } catch (_: Exception) {
    onUnauthenticated()
    return
  }
  val googleIdOption = GetGoogleIdOption.Builder()
    .setFilterByAuthorizedAccounts(true)
    .setServerClientId(clientId)
    .setAutoSelectEnabled(true)
    .build()
  val request = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()
  scope.launch {
    try {
      val result = credentialManager.getCredential(context, request)
      val credential = result.credential
      if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
        val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
        Firebase.auth.signInWithCredential(authCredential).await()
        ensureUserDocAndLogin(onAuthSuccess)
      } else {
        onUnauthenticated()
      }
    } catch (_: Exception) {
      onUnauthenticated()
    }
  }
}

fun onGoogleSignInClicked(
  context: Context,
  credentialManager: CredentialManager,
  onAuthSuccess: () -> Unit,
  onAuthError: (String) -> Unit,
  scope: CoroutineScope,
  onAuthCancelled: () -> Unit = {}
) {
  val clientId = try {
    context.getString(R.string.default_web_client_id)
  } catch (_: Exception) {
    onAuthError("Konfigurasi Google Sign-In tidak ditemukan.")
    return
  }
  val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
  val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()
  scope.launch {
    try {
      val activity = context as? Activity
      val result = credentialManager.getCredential(activity ?: context, request)
      val credential = result.credential
      if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
        val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
        Firebase.auth.signInWithCredential(authCredential).await()
        ensureUserDocAndLogin(onAuthSuccess)
      } else {
        onAuthError("Tipe kredensial tidak dikenali.")
      }
    } catch (e: GetCredentialCancellationException) {
      Log.w("Auth", "Google Sign-In cancelled: ${e.message}", e)
      onAuthCancelled()
    } catch (e: Exception) {
      Log.e("Auth", "Google Sign-In failed", e)
      onAuthError(e.localizedMessage ?: "Gagal masuk dengan Google.")
    }
  }
}

fun signOutFromApp(
  credentialManager: CredentialManager,
  scope: CoroutineScope,
  onSignOutComplete: () -> Unit = {}
) {
  scope.launch {
    withContext(NonCancellable) {
      try {
        credentialManager.clearCredentialState(ClearCredentialStateRequest())
      } catch (e: Exception) {
        Log.w("Auth", "Failed to clear credential state: ${e.message}")
      } finally {
        Firebase.auth.signOut()
        onSignOutComplete()
      }
    }
  }
}

suspend fun ensureUserDocAndLogin(onAuthSuccess: () -> Unit) {
    val user = Firebase.auth.currentUser ?: return
    val db = FirebaseFirestore.getInstance()
    val uid = user.uid
    val email = user.email ?: ""
    try {
        val adminDoc = db.collection("admins").document(uid).get().await()
        if (adminDoc.exists()) { onAuthSuccess(); return }
        val userDoc = db.collection("users").document(uid).get().await()
        if (userDoc.exists()) { onAuthSuccess(); return }
        val isMyAdmin = email.equals("triahendika46@gmail.com", ignoreCase = true)
        if (isMyAdmin) {
            db.collection("admins").document(uid).set(mapOf("email" to email, "role" to "admin", "createdAt" to System.currentTimeMillis())).await()
        } else {
            db.collection("users").document(uid).set(mapOf("email" to email, "role" to "customer", "createdAt" to System.currentTimeMillis()), SetOptions.merge()).await()
        }
        onAuthSuccess()
    } catch (e: Exception) { onAuthSuccess() }
}
