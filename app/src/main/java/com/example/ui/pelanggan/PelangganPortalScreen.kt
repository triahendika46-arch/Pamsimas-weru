package com.example.ui.pelanggan

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.SyncStatusBanner
import com.example.ui.theme.WaterBlueContainer
import com.example.ui.theme.WaterBluePrimary
import com.example.ui.viewmodel.PelangganViewModel

private data class PelangganTabItem(
  val title: String,
  val icon: ImageVector,
  val tag: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PelangganPortalScreen(
  viewModel: PelangganViewModel,
  isAdminPreview: Boolean = false,
  onExitPreview: () -> Unit = {},
  onSignOut: () -> Unit
) {
  val customer by viewModel.customer.collectAsStateWithLifecycle()
  val isProfileLoading by viewModel.isProfileLoading.collectAsStateWithLifecycle()
  val myRequest by viewModel.myRequest.collectAsStateWithLifecycle()
  val readings by viewModel.readings.collectAsStateWithLifecycle()
  val bills by viewModel.bills.collectAsStateWithLifecycle()
  val payments by viewModel.payments.collectAsStateWithLifecycle()
  val appConfig by viewModel.appConfig.collectAsStateWithLifecycle()
  val syncState by viewModel.syncState.collectAsStateWithLifecycle()
  val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
  val feedback by viewModel.feedbackMessage.collectAsStateWithLifecycle()

  val snackbarHostState = remember { SnackbarHostState() }
  LaunchedEffect(feedback) {
    if (feedback != null) {
      snackbarHostState.showSnackbar(feedback!!)
      viewModel.clearFeedback()
    }
  }

  var selectedTab by rememberSaveable { mutableIntStateOf(0) }
  val unpaidCount = bills.count { it.status == "belum_bayar" || it.status == "ditolak" }
  val tabs = listOf(
    PelangganTabItem("Beranda", Icons.Rounded.Home, "pelanggan_tab_home"),
    PelangganTabItem("Meteran", Icons.Rounded.Speed, "pelanggan_tab_meter"),
    PelangganTabItem("Tagihan", Icons.Rounded.ReceiptLong, "pelanggan_tab_bills"),
    PelangganTabItem("Riwayat", Icons.Rounded.History, "pelanggan_tab_history")
  )

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val useRail = maxWidth >= 680.dp
    Scaffold(
      snackbarHost = { SnackbarHost(snackbarHostState) },
      topBar = {
        TopAppBar(
          navigationIcon = {
            if (isAdminPreview) {
              IconButton(onClick = onExitPreview) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Kembali ke Admin")
              }
            }
          },
          title = {
            Column {
              Text(
                text = "PAMSIMAS WERU",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
              )
              Text(
                text = "Portal Pelanggan • Dusun Weru",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          },
          actions = {
            IconButton(
              onClick = onSignOut,
              modifier = Modifier.testTag("pelanggan_sign_out_button")
            ) {
              Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = "Keluar")
            }
          },
          colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
          )
        )
      },
      bottomBar = {
        if (!useRail) {
          NavigationBar(modifier = Modifier.navigationBarsPadding()) {
            tabs.forEachIndexed { idx, item ->
              NavigationBarItem(
                selected = selectedTab == idx,
                onClick = { selectedTab = idx },
                modifier = Modifier.testTag(item.tag),
                icon = {
                  if (idx == 2 && unpaidCount > 0) {
                    BadgedBox(badge = { Badge { Text("$unpaidCount") } }) {
                      Icon(item.icon, contentDescription = item.title)
                    }
                  } else {
                    Icon(item.icon, contentDescription = item.title)
                  }
                },
                label = { Text(item.title, maxLines = 1) }
              )
            }
          }
        }
      }
    ) { innerPadding ->
      Row(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding)
      ) {
        if (useRail) {
          NavigationRail(modifier = Modifier.fillMaxHeight()) {
            tabs.forEachIndexed { idx, item ->
              NavigationRailItem(
                selected = selectedTab == idx,
                onClick = { selectedTab = idx },
                icon = { Icon(item.icon, contentDescription = item.title) },
                label = { Text(item.title) }
              )
            }
          }
        }

        Column(modifier = Modifier.fillMaxSize()) {
          if (isAdminPreview) {
            Surface(
              color = WaterBlueContainer,
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "Mode Pratinjau Pelanggan (Admin)",
                  style = MaterialTheme.typography.labelMedium,
                  color = WaterBluePrimary,
                  modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onExitPreview) {
                  Text("Kembali ke Admin")
                }
              }
            }
          }
          SyncStatusBanner(
            syncState = syncState,
            onSyncClick = { viewModel.syncNow() },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
          )
          Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
              0 -> PelangganHomeTab(
                customer = customer,
                isProfileLoading = isProfileLoading,
                myRequest = myRequest,
                userUid = viewModel.currentUserId,
                userEmail = viewModel.currentUserEmail,
                userName = viewModel.currentUserName,
                bills = bills,
                appConfig = appConfig,
                onClaimOrUpdateProfile = { cust, hp, alamat ->
                  viewModel.claimOrUpdateProfile(cust, hp, alamat)
                },
                onSubmitActivationRequest = { nama, meter, alamat, rtRw, hp ->
                  viewModel.submitActivationRequest(nama, meter, alamat, rtRw, hp)
                },
                onGoToBills = { selectedTab = 2 }
              )
              1 -> PelangganMeterTab(readings = readings)
              2 -> PelangganBillsTab(
                bills = bills,
                appConfig = appConfig,
                isBusy = isBusy,
                onSubmitTransferProof = { bill, nama, bank, catatan, base64 ->
                  viewModel.submitTransferProof(bill, nama, bank, catatan, base64)
                }
              )
              3 -> PelangganPaymentsTab(payments = payments)
            }
          }
        }
      }
    }
  }
}
