package com.example.ui.admin

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AdminUser
import com.example.data.model.AppConfig
import com.example.data.model.Bill
import com.example.data.model.Customer
import com.example.domain.TariffCalculator
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.LeafGreenSecondary
import com.example.ui.theme.StatusUnpaidText
import com.example.ui.theme.WaterBluePrimary
import com.example.util.ReceiptUtils
import kotlin.math.max

@Composable
fun AdminReportsTab(
  customers: List<Customer>,
  bills: List<Bill>,
  appConfig: AppConfig,
  admins: List<AdminUser>,
  onSaveConfig: (String, String, String, String, String) -> Unit,
  onAddAdmin: (String, String, String) -> Unit,
  onPreviewPelanggan: () -> Unit
) {
  val context = LocalContext.current
  val currentPeriode = remember { TariffCalculator.currentPeriode() }
  var scopeBulanIni by remember { mutableStateOf(true) }

  val scopedBills = remember(bills, scopeBulanIni, currentPeriode) {
    if (scopeBulanIni) bills.filter { it.periode == currentPeriode } else bills
  }

  val totalTagihan = scopedBills.sumOf { it.totalTagihan }
  val totalPemakaian = scopedBills.sumOf { it.pemakaian }
  val totalTunai = scopedBills.filter { it.status == "lunas" && it.metodePembayaran == "tunai" }
    .sumOf { it.totalTagihan }
  val totalTransfer = scopedBills.filter { it.status == "lunas" && it.metodePembayaran == "transfer" }
    .sumOf { it.totalTagihan }
  val totalTunggakan = scopedBills.filter { it.status != "lunas" }.sumOf { it.totalTagihan }

  var namaBank by remember(appConfig) { mutableStateOf(appConfig.namaBank) }
  var nomorRekening by remember(appConfig) { mutableStateOf(appConfig.nomorRekening) }
  var atasNama by remember(appConfig) { mutableStateOf(appConfig.atasNama) }
  var kontakAdmin by remember(appConfig) { mutableStateOf(appConfig.kontakAdmin) }
  var catatanPembayaran by remember(appConfig) { mutableStateOf(appConfig.catatanPembayaran) }

  var showAddAdminDialog by remember { mutableStateOf(false) }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        FilterChip(
          selected = scopeBulanIni,
          onClick = { scopeBulanIni = true },
          label = { Text(TariffCalculator.formatPeriode(currentPeriode)) }
        )
        FilterChip(
          selected = !scopeBulanIni,
          onClick = { scopeBulanIni = false },
          label = { Text("Semua Periode") }
        )
      }
    }

    item {
      Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Assessment, contentDescription = null, tint = WaterBluePrimary)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Laporan Keuangan & Pemakaian Air", style = MaterialTheme.typography.titleMedium)
          }
          Spacer(modifier = Modifier.height(12.dp))

          ReportLine("Total Pelanggan Terdaftar", "${customers.size} Pelanggan")
          ReportLine("Total Pemakaian Air", "$totalPemakaian m³")
          ReportLine("Total Tagihan Terbit", TariffCalculator.formatRupiah(totalTagihan))
          HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
          ReportLine("Terbayar Tunai", TariffCalculator.formatRupiah(totalTunai), LeafGreenSecondary)
          ReportLine("Terbayar Transfer", TariffCalculator.formatRupiah(totalTransfer), WaterBluePrimary)
          ReportLine("Belum Lunas / Tunggakan", TariffCalculator.formatRupiah(totalTunggakan), StatusUnpaidText)

          Spacer(modifier = Modifier.height(14.dp))

          // Visual proportional bar chart using Compose Canvas
          Text("Komposisi Pembayaran", style = MaterialTheme.typography.labelMedium)
          Spacer(modifier = Modifier.height(8.dp))
          val maxVal = max(1L, maxOf(totalTunai, totalTransfer, totalTunggakan))
          Canvas(
            modifier = Modifier
              .fillMaxWidth()
              .height(64.dp)
          ) {
            val barHeight = 14.dp.toPx()
            val gap = 10.dp.toPx()
            val items = listOf(
              totalTunai to LeafGreenSecondary,
              totalTransfer to WaterBluePrimary,
              totalTunggakan to StatusUnpaidText
            )
            items.forEachIndexed { idx, (value, color) ->
              val top = idx * (barHeight + gap)
              drawRoundRect(
                color = color.copy(alpha = 0.14f),
                topLeft = Offset(0f, top),
                size = Size(size.width, barHeight),
                cornerRadius = CornerRadius(barHeight / 2, barHeight / 2)
              )
              val w = (value.toFloat() / maxVal.toFloat()) * size.width
              if (w > 0f) {
                drawRoundRect(
                  color = color,
                  topLeft = Offset(0f, top),
                  size = Size(w.coerceAtLeast(barHeight), barHeight),
                  cornerRadius = CornerRadius(barHeight / 2, barHeight / 2)
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(12.dp))
          Button(
            onClick = {
              val periodLabel = if (scopeBulanIni) TariffCalculator.formatPeriode(currentPeriode) else "Semua Periode"
              val reportText = buildString {
                appendLine("LAPORAN PAMSIMAS WERU BY HENDIKA WP")
                appendLine("Periode: $periodLabel")
                appendLine("------------------------------------")
                appendLine("Pelanggan Terdaftar : ${customers.size}")
                appendLine("Total Pemakaian Air : $totalPemakaian m3")
                appendLine("Total Tagihan       : ${TariffCalculator.formatRupiah(totalTagihan)}")
                appendLine("Masuk Tunai         : ${TariffCalculator.formatRupiah(totalTunai)}")
                appendLine("Masuk Transfer      : ${TariffCalculator.formatRupiah(totalTransfer)}")
                appendLine("Total Terkumpul     : ${TariffCalculator.formatRupiah(totalTunai + totalTransfer)}")
                appendLine("Tunggakan           : ${TariffCalculator.formatRupiah(totalTunggakan)}")
                appendLine("------------------------------------")
                appendLine("Dusun Weru • Pengelola Hendika WP")
              }
              ReceiptUtils.shareText(context, "Laporan PAMSIMAS Weru", reportText)
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = WaterBluePrimary),
            shape = RoundedCornerShape(12.dp)
          ) {
            Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Bagikan Laporan")
          }
        }
      }
    }

    item {
      Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.AccountBalance, contentDescription = null, tint = LeafGreenSecondary)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Pengaturan Rekening Transfer", style = MaterialTheme.typography.titleMedium)
          }
          OutlinedTextField(
            value = namaBank,
            onValueChange = { namaBank = it },
            label = { Text("Nama Bank / E-Wallet") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
          OutlinedTextField(
            value = nomorRekening,
            onValueChange = { nomorRekening = it },
            label = { Text("Nomor Rekening") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
          OutlinedTextField(
            value = atasNama,
            onValueChange = { atasNama = it },
            label = { Text("Atas Nama") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
          OutlinedTextField(
            value = kontakAdmin,
            onValueChange = { kontakAdmin = it },
            label = { Text("Kontak WhatsApp Admin") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
          OutlinedTextField(
            value = catatanPembayaran,
            onValueChange = { catatanPembayaran = it },
            label = { Text("Catatan Pembayaran Pelanggan") },
            modifier = Modifier.fillMaxWidth()
          )
          Button(
            onClick = { onSaveConfig(namaBank, nomorRekening, atasNama, kontakAdmin, catatanPembayaran) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = LeafGreenSecondary),
            shape = RoundedCornerShape(12.dp)
          ) {
            Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Simpan Pengaturan Rekening")
          }
        }
      }
    }

    item {
      Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.AdminPanelSettings, contentDescription = null, tint = WaterBluePrimary)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Kelola Akses & Pratinjau", style = MaterialTheme.typography.titleMedium)
          }
          Text(
            text = "Admin tambahan terdaftar: ${admins.size} akun (+ Admin Utama Hendika WP)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
              onClick = { showAddAdminDialog = true },
              modifier = Modifier.weight(1f),
              shape = RoundedCornerShape(12.dp)
            ) {
              Text("+ Tambah Admin")
            }
            Button(
              onClick = onPreviewPelanggan,
              modifier = Modifier.weight(1f),
              colors = ButtonDefaults.buttonColors(containerColor = WaterBluePrimary),
              shape = RoundedCornerShape(12.dp)
            ) {
              Text("Pratinjau Pelanggan")
            }
          }
        }
      }
    }
  }

  if (showAddAdminDialog) {
    var uid by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var nama by remember { mutableStateOf("") }
    AlertDialog(
      onDismissRequest = { showAddAdminDialog = false },
      title = { Text("Tambah Admin Pengelola") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            "Masukkan UID Google Firebase pengguna yang akan diberi akses Admin penuh.",
            style = MaterialTheme.typography.bodySmall
          )
          OutlinedTextField(value = uid, onValueChange = { uid = it }, label = { Text("UID Google *") })
          OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email Google *") })
          OutlinedTextField(value = nama, onValueChange = { nama = it }, label = { Text("Nama Pengelola *") })
        }
      },
      confirmButton = {
        Button(
          onClick = {
            onAddAdmin(uid, email, nama)
            showAddAdminDialog = false
          },
          enabled = uid.isNotBlank() && email.isNotBlank() && nama.length >= 2
        ) {
          Text("Simpan")
        }
      },
      dismissButton = {
        TextButton(onClick = { showAddAdminDialog = false }) { Text("Batal") }
      }
    )
  }
}

@Composable
private fun ReportLine(label: String, value: String, valueColor: Color = MaterialTheme.colorScheme.onSurface) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 3.dp),
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(
      text = value,
      style = MaterialTheme.typography.bodyMedium.copy(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Bold
      ),
      color = valueColor
    )
  }
}
