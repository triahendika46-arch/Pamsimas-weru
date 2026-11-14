package com.example.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.Bill
import com.example.data.model.Customer
import com.example.data.model.CustomerRequest
import com.example.data.model.MeterReading
import com.example.data.model.Payment
import com.example.domain.TariffCalculator
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.PamsimasHeroHeader
import com.example.ui.components.StatMetricCard
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.LeafGreenSecondary
import com.example.ui.theme.StatusPaidBg
import com.example.ui.theme.StatusPaidText
import com.example.ui.theme.StatusPendingText
import com.example.ui.theme.StatusUnpaidBg
import com.example.ui.theme.StatusUnpaidText
import com.example.ui.theme.WaterBluePrimary

@Composable
fun AdminDashboardTab(
  customers: List<Customer>,
  readings: List<MeterReading>,
  bills: List<Bill>,
  payments: List<Payment>,
  requests: List<CustomerRequest>,
  onNavigateTab: (Int) -> Unit,
  onApproveRequest: (CustomerRequest, Customer?) -> Unit,
  onRejectRequest: (CustomerRequest, String) -> Unit,
  onQuickVerifyBill: (Bill, Payment?) -> Unit
) {
  val currentPeriod = remember { TariffCalculator.currentPeriode() }
  val activeCustomersCount = customers.count { it.aktif }
  val monthlyUsageM3 = readings.filter { it.periode == currentPeriod }.sumOf { it.pemakaian }
  val monthlyCollected = bills.filter { it.periode == currentPeriod && it.status == "lunas" }
    .sumOf { it.totalTagihan }
  val pendingVerifyBills = bills.filter { it.status == "menunggu_verifikasi" }
  val pendingRequests = requests.filter { it.status == "menunggu" }

  var linkingRequest by remember { mutableStateOf<CustomerRequest?>(null) }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    item {
      PamsimasHeroHeader(
        title = "PAMSIMAS WERU",
        subtitle = "Pengelolaan Air Bersih Dusun Weru • By Hendika WP",
        badgeText = "ADMIN"
      ) {
        Spacer(modifier = Modifier.height(12.dp))
        Text(
          text = "Periode Aktif: ${TariffCalculator.formatPeriode(currentPeriod)}",
          style = MaterialTheme.typography.labelLarge,
          color = Color.White
        )
        Text(
          text = "Tarif Rp2.000/m³ • Beban Rp5.000/bln jika pemakaian < 8 m³",
          style = MaterialTheme.typography.bodySmall,
          color = Color.White.copy(alpha = 0.88f)
        )
      }
    }

    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        StatMetricCard(
          label = "Pelanggan Aktif",
          value = "$activeCustomersCount / ${customers.size}",
          icon = Icons.Rounded.Group,
          accentColor = WaterBluePrimary,
          modifier = Modifier.weight(1f)
        )
        StatMetricCard(
          label = "Pemakaian Bulan Ini",
          value = "$monthlyUsageM3 m³",
          icon = Icons.Rounded.WaterDrop,
          accentColor = LeafGreenSecondary,
          modifier = Modifier.weight(1f)
        )
      }
    }

    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        StatMetricCard(
          label = "Pemasukan Bulan Ini",
          value = TariffCalculator.formatRupiah(monthlyCollected),
          icon = Icons.Rounded.AccountBalanceWallet,
          accentColor = LeafGreenSecondary,
          modifier = Modifier.weight(1f)
        )
        StatMetricCard(
          label = "Perlu Verifikasi",
          value = "${pendingVerifyBills.size} Transfer",
          icon = Icons.Rounded.HourglassTop,
          accentColor = StatusPendingText,
          modifier = Modifier.weight(1f)
        )
      }
    }

    item {
      Text(
        text = "Aksi Cepat Pengelola",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground
      )
      Spacer(modifier = Modifier.height(8.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        QuickActionButton(
          label = "Pelanggan",
          icon = Icons.Rounded.PersonAdd,
          color = WaterBluePrimary,
          onClick = { onNavigateTab(1) },
          modifier = Modifier.weight(1f)
        )
        QuickActionButton(
          label = "Catat Meter",
          icon = Icons.Rounded.Speed,
          color = LeafGreenSecondary,
          onClick = { onNavigateTab(2) },
          modifier = Modifier.weight(1f)
        )
        QuickActionButton(
          label = "Tagihan",
          icon = Icons.Rounded.ReceiptLong,
          color = WaterBluePrimary,
          onClick = { onNavigateTab(3) },
          modifier = Modifier.weight(1f)
        )
        QuickActionButton(
          label = "Laporan",
          icon = Icons.Rounded.Assessment,
          color = LeafGreenSecondary,
          onClick = { onNavigateTab(4) },
          modifier = Modifier.weight(1f)
        )
      }
    }

    if (pendingRequests.isNotEmpty()) {
      item {
        Text(
          text = "Permintaan Aktivasi Pelanggan (${pendingRequests.size})",
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onBackground
        )
      }
      items(pendingRequests, key = { it.uid }) { req ->
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Text(
              text = req.nama,
              style = MaterialTheme.typography.titleSmall,
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = "${req.email} • Meter: ${req.noMeter.ifBlank { "-" }} • ${req.rtRw}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (req.noHp.isNotBlank()) {
              Text(
                text = "HP: ${req.noHp} • ${req.alamat}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              Button(
                onClick = { linkingRequest = req },
                colors = ButtonDefaults.buttonColors(containerColor = LeafGreenSecondary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f)
              ) {
                Icon(Icons.Rounded.Link, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Setujui & Tautkan")
              }
              OutlinedButton(
                onClick = { onRejectRequest(req, "Data belum sesuai catatan Dusun Weru") },
                shape = RoundedCornerShape(10.dp)
              ) {
                Icon(Icons.Rounded.Close, contentDescription = "Tolak", modifier = Modifier.size(16.dp))
              }
            }
          }
        }
      }
    }

    if (pendingVerifyBills.isNotEmpty()) {
      item {
        Text(
          text = "Transfer Menunggu Verifikasi (${pendingVerifyBills.size})",
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onBackground
        )
      }
      items(pendingVerifyBills.take(5), key = { it.id }) { bill ->
        val matchingPayment = payments.firstOrNull {
          it.billId == bill.id && it.status == "menunggu_verifikasi"
        }
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "${bill.customerNama} (${bill.noMeter})",
                style = MaterialTheme.typography.titleSmall
              )
              Text(
                text = "${TariffCalculator.formatPeriode(bill.periode)} • ${bill.bankPengirim} a.n ${bill.namaPengirimTransfer}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Text(
                text = TariffCalculator.formatRupiah(bill.totalTagihan),
                style = MaterialTheme.typography.titleSmall.copy(
                  fontFamily = JetBrainsMono,
                  fontWeight = FontWeight.Bold
                ),
                color = WaterBluePrimary
              )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              Button(
                onClick = { onQuickVerifyBill(bill, matchingPayment) },
                colors = ButtonDefaults.buttonColors(containerColor = LeafGreenSecondary),
                shape = RoundedCornerShape(10.dp)
              ) {
                Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Lunas")
              }
              TextButton(onClick = { onNavigateTab(3) }) {
                Text("Detail")
              }
            }
          }
        }
      }
    }
  }

  if (linkingRequest != null) {
    val req = linkingRequest!!
    AlertDialog(
      onDismissRequest = { linkingRequest = null },
      title = { Text("Tautkan Akun Pelanggan") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("Pilih pelanggan yang sudah ada untuk ditautkan ke ${req.nama} (${req.email}), atau buat data pelanggan baru.")
          val candidateCustomers = customers.filter {
            it.ownerUid.isBlank() || it.emailPelanggan.equals(req.email, ignoreCase = true)
          }
          if (candidateCustomers.isNotEmpty()) {
            Text(
              "Pelanggan belum tertaut:",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.primary
            )
            candidateCustomers.take(6).forEach { cust ->
              OutlinedButton(
                onClick = {
                  onApproveRequest(req, cust)
                  linkingRequest = null
                },
                modifier = Modifier.fillMaxWidth()
              ) {
                Text("${cust.noMeter} • ${cust.nama}")
              }
            }
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            onApproveRequest(req, null)
            linkingRequest = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = LeafGreenSecondary)
        ) {
          Text("Buat Pelanggan Baru")
        }
      },
      dismissButton = {
        TextButton(onClick = { linkingRequest = null }) {
          Text("Batal")
        }
      }
    )
  }
}

@Composable
private fun QuickActionButton(
  label: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  color: Color,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Card(
    onClick = onClick,
    modifier = modifier,
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 12.dp, horizontal = 6.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(24.dp))
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
        maxLines = 1
      )
    }
  }
}

@Composable
fun AdminCustomersTab(
  customers: List<Customer>,
  onCreateCustomer: (String, String, String, String, String, String, Long) -> Unit,
  onUpdateCustomer: (Customer, String, String, String, String, String, String, String, Boolean) -> Unit,
  onDeleteCustomer: (Customer) -> Unit
) {
  var searchQuery by remember { mutableStateOf("") }
  var filterMode by remember { mutableStateOf("semua") }
  var showFormDialog by remember { mutableStateOf(false) }
  var editingCustomer by remember { mutableStateOf<Customer?>(null) }
  var deletingCustomer by remember { mutableStateOf<Customer?>(null) }

  val filteredCustomers = remember(customers, searchQuery, filterMode) {
    customers.filter { cust ->
      val matchesSearch = searchQuery.isBlank() ||
        cust.nama.contains(searchQuery, ignoreCase = true) ||
        cust.noMeter.contains(searchQuery, ignoreCase = true) ||
        cust.rtRw.contains(searchQuery, ignoreCase = true) ||
        cust.emailPelanggan.contains(searchQuery, ignoreCase = true)
      val matchesFilter = when (filterMode) {
        "aktif" -> cust.aktif
        "nonaktif" -> !cust.aktif
        "belum_taut" -> cust.ownerUid.isBlank()
        else -> true
      }
      matchesSearch && matchesFilter
    }
  }

  Scaffold(
    floatingActionButton = {
      FloatingActionButton(
        onClick = {
          editingCustomer = null
          showFormDialog = true
        },
        containerColor = WaterBluePrimary,
        contentColor = Color.White,
        modifier = Modifier.testTag("add_customer_fab")
      ) {
        Icon(Icons.Rounded.Add, contentDescription = "Tambah Pelanggan")
      }
    }
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .padding(horizontal = 16.dp)
    ) {
      Spacer(modifier = Modifier.height(8.dp))
      OutlinedTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        modifier = Modifier
          .fillMaxWidth()
          .testTag("search_customer_input"),
        placeholder = { Text("Cari nama, no. meter, RT/RW, atau email...") },
        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
        singleLine = true,
        shape = RoundedCornerShape(14.dp)
      )
      Spacer(modifier = Modifier.height(8.dp))
      LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        val options = listOf(
          "semua" to "Semua (${customers.size})",
          "aktif" to "Aktif (${customers.count { it.aktif }})",
          "nonaktif" to "Nonaktif (${customers.count { !it.aktif }})",
          "belum_taut" to "Belum Tertaut (${customers.count { it.ownerUid.isBlank() }})"
        )
        items(options) { (key, label) ->
          FilterChip(
            selected = filterMode == key,
            onClick = { filterMode = key },
            label = { Text(label) }
          )
        }
      }
      Spacer(modifier = Modifier.height(8.dp))

      if (filteredCustomers.isEmpty()) {
        EmptyStateCard(
          icon = Icons.Rounded.Group,
          title = "Belum Ada Pelanggan",
          description = "Ketuk tombol + di kanan bawah untuk menambahkan pelanggan air bersih Dusun Weru tanpa batas jumlah."
        )
      } else {
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          contentPadding = PaddingValues(bottom = 88.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          items(filteredCustomers, key = { it.id }) { cust ->
            Card(
              shape = RoundedCornerShape(16.dp),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                      text = cust.noMeter,
                      style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = JetBrainsMono,
                        fontWeight = FontWeight.Bold
                      ),
                      color = WaterBluePrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                      color = if (cust.aktif) StatusPaidBg else StatusUnpaidBg,
                      shape = RoundedCornerShape(50)
                    ) {
                      Text(
                        text = if (cust.aktif) "Aktif" else "Nonaktif",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (cust.aktif) StatusPaidText else StatusUnpaidText,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                      )
                    }
                  }
                  Spacer(modifier = Modifier.height(4.dp))
                  Text(
                    text = cust.nama,
                    style = MaterialTheme.typography.titleMedium
                  )
                  Text(
                    text = "${cust.rtRw.ifBlank { "Dusun Weru" }} • ${cust.alamat.ifBlank { "-" }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                  Text(
                    text = "Meter terakhir: ${cust.meterTerakhir} m³ • HP: ${cust.noHp.ifBlank { "-" }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                  if (cust.emailPelanggan.isNotBlank()) {
                    Text(
                      text = "Email: ${cust.emailPelanggan}" + if (cust.ownerUid.isNotBlank()) " (Tertaut)" else " (Menunggu login)",
                      style = MaterialTheme.typography.labelSmall,
                      color = LeafGreenSecondary
                    )
                  }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                  IconButton(
                    onClick = {
                      editingCustomer = cust
                      showFormDialog = true
                    },
                    modifier = Modifier.testTag("edit_customer_${cust.noMeter}")
                  ) {
                    Icon(Icons.Rounded.Edit, contentDescription = "Edit pelanggan")
                  }
                  IconButton(
                    onClick = { deletingCustomer = cust },
                    modifier = Modifier.testTag("delete_customer_${cust.noMeter}")
                  ) {
                    Icon(
                      Icons.Rounded.Delete,
                      contentDescription = "Hapus pelanggan",
                      tint = MaterialTheme.colorScheme.error
                    )
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  if (showFormDialog) {
    CustomerFormDialog(
      existing = editingCustomer,
      onDismiss = { showFormDialog = false },
      onSave = { nama, noMeter, alamat, rtRw, noHp, email, meterAwal, aktif ->
        val existing = editingCustomer
        if (existing == null) {
          onCreateCustomer(nama, noMeter, alamat, rtRw, noHp, email, meterAwal)
        } else {
          onUpdateCustomer(existing, nama, noMeter, alamat, rtRw, noHp, email, existing.ownerUid, aktif)
        }
        showFormDialog = false
      },
      onDelete = { cust ->
        onDeleteCustomer(cust)
        showFormDialog = false
      }
    )
  }

  if (deletingCustomer != null) {
    val cust = deletingCustomer!!
    AlertDialog(
      onDismissRequest = { deletingCustomer = null },
      title = { Text("Hapus Pelanggan & Semua Data?") },
      text = {
        Text(
          "Menghapus ${cust.nama} (${cust.noMeter}) akan langsung menghilangkan seluruh data pelanggan, riwayat pencatatan meteran, tagihan, bukti transfer, dan kuitansi pembayarannya secara permanen."
        )
      },
      confirmButton = {
        Button(
          onClick = {
            onDeleteCustomer(cust)
            deletingCustomer = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
          modifier = Modifier.testTag("confirm_delete_customer_button")
        ) {
          Text("Hapus Semua Data")
        }
      },
      dismissButton = {
        TextButton(onClick = { deletingCustomer = null }) {
          Text("Batal")
        }
      }
    )
  }
}

@Composable
private fun CustomerFormDialog(
  existing: Customer?,
  onDismiss: () -> Unit,
  onSave: (String, String, String, String, String, String, Long, Boolean) -> Unit,
  onDelete: (Customer) -> Unit
) {
  var nama by remember { mutableStateOf(existing?.nama ?: "") }
  var noMeter by remember { mutableStateOf(existing?.noMeter ?: "") }
  var alamat by remember { mutableStateOf(existing?.alamat ?: "Dusun Weru") }
  var rtRw by remember { mutableStateOf(existing?.rtRw ?: "RT 01 / RW 01") }
  var noHp by remember { mutableStateOf(existing?.noHp ?: "") }
  var email by remember { mutableStateOf(existing?.emailPelanggan ?: "") }
  var meterAwalText by remember { mutableStateOf((existing?.meterTerakhir ?: 0L).toString()) }
  var aktif by remember { mutableStateOf(existing?.aktif ?: true) }
  var confirmDelete by remember { mutableStateOf(false) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(if (existing == null) "Tambah Pelanggan Baru" else "Edit Pelanggan") },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        OutlinedTextField(
          value = nama,
          onValueChange = { nama = it },
          label = { Text("Nama Lengkap *") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("customer_name_input")
        )
        OutlinedTextField(
          value = noMeter,
          onValueChange = { noMeter = it },
          label = { Text("No. Meteran * (cth: WR-001)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("customer_meter_input")
        )
        OutlinedTextField(
          value = rtRw,
          onValueChange = { rtRw = it },
          label = { Text("RT / RW") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
          value = alamat,
          onValueChange = { alamat = it },
          label = { Text("Alamat Dusun Weru") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
          value = noHp,
          onValueChange = { noHp = it },
          label = { Text("No. HP / WhatsApp") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
          value = email,
          onValueChange = { email = it },
          label = { Text("Email Google Pelanggan (Opsional)") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
        if (existing == null) {
          OutlinedTextField(
            value = meterAwalText,
            onValueChange = { meterAwalText = it.filter { ch -> ch.isDigit() } },
            label = { Text("Angka Meter Awal (m³)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
        } else {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text("Status Sambungan Aktif", style = MaterialTheme.typography.bodyMedium)
            Switch(checked = aktif, onCheckedChange = { aktif = it })
          }
          if (!confirmDelete) {
            TextButton(
              onClick = { confirmDelete = true },
              colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
              Icon(Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Hapus Pelanggan Ini")
            }
          } else {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Text(
                text = "Semua data meteran, tagihan, bukti transfer & kuitansi milik ${existing.nama} akan ikut dihapus permanen.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
              )
              Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                  onClick = { onDelete(existing) },
                  colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                  modifier = Modifier.testTag("dialog_confirm_delete_customer_button")
                ) {
                  Text("Konfirmasi Hapus")
                }
                TextButton(onClick = { confirmDelete = false }) { Text("Batal") }
              }
            }
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onSave(
            nama,
            noMeter,
            alamat,
            rtRw,
            noHp,
            email,
            meterAwalText.toLongOrNull() ?: 0L,
            aktif
          )
        },
        enabled = nama.trim().length >= 2 && noMeter.trim().length >= 2,
        colors = ButtonDefaults.buttonColors(containerColor = WaterBluePrimary),
        modifier = Modifier.testTag("save_customer_button")
      ) {
        Text("Simpan")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Batal") }
    }
  )
}
