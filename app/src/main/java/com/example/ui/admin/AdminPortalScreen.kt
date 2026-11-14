package com.example.ui.admin

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Group
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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.SyncStatusBanner
import com.example.ui.viewmodel.AdminViewModel

private data class AdminTabItem(
  val title: String,
  val icon: ImageVector,
  val tag: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPortalScreen(
  viewModel: AdminViewModel,
  onSignOut: () -> Unit,
  onPreviewPelanggan: () -> Unit
) {
  val customers by viewModel.customers.collectAsStateWithLifecycle()
  val readings by viewModel.readings.collectAsStateWithLifecycle()
  val bills by viewModel.bills.collectAsStateWithLifecycle()
  val payments by viewModel.payments.collectAsStateWithLifecycle()
  val requests by viewModel.customerRequests.collectAsStateWithLifecycle()
  val admins by viewModel.admins.collectAsStateWithLifecycle()
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
  val pendingCount = bills.count { it.status == "menunggu_verifikasi" }
  val tabs = listOf(
    AdminTabItem("Ringkasan", Icons.Rounded.Dashboard, "admin_tab_dashboard"),
    AdminTabItem("Pelanggan", Icons.Rounded.Group, "admin_tab_customers"),
    AdminTabItem("Meteran", Icons.Rounded.Speed, "admin_tab_meter"),
    AdminTabItem("Tagihan", Icons.Rounded.ReceiptLong, "admin_tab_bills"),
    AdminTabItem("Laporan", Icons.Rounded.Assessment, "admin_tab_reports")
  )

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val useRail = maxWidth >= 680.dp
    Scaffold(
      snackbarHost = { SnackbarHost(snackbarHostState) },
      topBar = {
        TopAppBar(
          title = {
            Column {
              Text(
                text = "PAMSIMAS WERU",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
              )
              Text(
                text = "Portal Admin • Hendika WP",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          },
          actions = {
            IconButton(
              onClick = onSignOut,
              modifier = Modifier.testTag("admin_sign_out_button")
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
                  if (idx == 3 && pendingCount > 0) {
                    BadgedBox(badge = { Badge { Text("$pendingCount") } }) {
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
          SyncStatusBanner(
            syncState = syncState,
            onSyncClick = { viewModel.syncNow() },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
          )
          Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
              0 -> AdminDashboardTab(
                customers = customers,
                readings = readings,
                bills = bills,
                payments = payments,
                requests = requests,
                onNavigateTab = { selectedTab = it },
                onApproveRequest = { req, cust -> viewModel.approveRequest(req, cust) },
                onRejectRequest = { req, reason -> viewModel.rejectRequest(req, reason) },
                onQuickVerifyBill = { bill, payment -> viewModel.verifyTransferPayment(bill, payment) }
              )
              1 -> AdminCustomersTab(
                customers = customers,
                onCreateCustomer = { nama, noMeter, alamat, rtRw, noHp, email, meterAwal ->
                  viewModel.createCustomer(nama, noMeter, alamat, rtRw, noHp, email, meterAwal)
                },
                onUpdateCustomer = { cust, nama, noMeter, alamat, rtRw, noHp, email, uid, aktif ->
                  viewModel.updateCustomer(cust, nama, noMeter, alamat, rtRw, noHp, email, uid, aktif)
                },
                onDeleteCustomer = { viewModel.deleteCustomer(it) }
              )
              2 -> AdminMeterTab(
                customers = customers,
                readings = readings,
                isBusy = isBusy,
                onRecordMeter = { cust, per, awal, akhir, cat ->
                  viewModel.recordMeterAndBill(cust, per, awal, akhir, cat)
                }
              )
              3 -> AdminBillsTab(
                bills = bills,
                payments = payments,
                onRecordCash = { viewModel.recordCashPayment(it) },
                onVerifyTransfer = { bill, payment -> viewModel.verifyTransferPayment(bill, payment) },
                onRejectTransfer = { bill, payment, reason -> viewModel.rejectTransferPayment(bill, payment, reason) }
              )
              4 -> AdminReportsTab(
                customers = customers,
                bills = bills,
                appConfig = appConfig,
                admins = admins,
                onSaveConfig = { bank, rek, an, hp, cat ->
                  viewModel.saveAppConfig(bank, rek, an, hp, cat)
                },
                onAddAdmin = { uid, email, nama -> viewModel.addAdmin(uid, email, nama) },
                onPreviewPelanggan = onPreviewPelanggan
              )
            }
          }
        }
      }
    }
  }
}
