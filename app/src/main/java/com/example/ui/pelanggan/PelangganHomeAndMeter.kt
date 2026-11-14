package com.example.ui.pelanggan

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.AppConfig
import com.example.data.model.Bill
import com.example.data.model.Customer
import com.example.data.model.CustomerRequest
import com.example.data.model.MeterReading
import com.example.domain.TariffCalculator
import com.example.ui.components.BillStatusPill
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.PamsimasHeroHeader
import com.example.ui.components.StatMetricCard
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.LeafGreenSecondary
import com.example.ui.theme.StatusPaidBg
import com.example.ui.theme.StatusPaidText
import com.example.ui.theme.StatusPendingBg
import com.example.ui.theme.StatusPendingText
import com.example.ui.theme.StatusUnpaidBg
import com.example.ui.theme.StatusUnpaidText
import com.example.ui.theme.WaterBlueContainer
import com.example.ui.theme.WaterBluePrimary
import kotlin.math.max

@Composable
fun PelangganHomeTab(
  customer: Customer?,
  isProfileLoading: Boolean,
  myRequest: CustomerRequest?,
  userUid: String,
  userEmail: String?,
  userName: String?,
  bills: List<Bill>,
  appConfig: AppConfig,
  onClaimOrUpdateProfile: (Customer, String, String) -> Unit,
  onSubmitActivationRequest: (String, String, String, String, String) -> Unit,
  onGoToBills: () -> Unit
) {
  val context = LocalContext.current
  var showEditDialog by remember { mutableStateOf(false) }

  val unpaidBills = bills.filter { it.status == "belum_bayar" || it.status == "ditolak" }
  val totalTunggakan = unpaidBills.sumOf { it.totalTagihan }
  val latestBill = bills.firstOrNull()

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    item {
      PamsimasHeroHeader(
        title = "PAMSIMAS WERU",
        subtitle = "Portal Pelanggan Dusun Weru • By Hendika WP",
        badgeText = "PELANGGAN"
      ) {
        Spacer(modifier = Modifier.height(10.dp))
        Text(
          text = customer?.nama ?: (userName ?: "Warga Dusun Weru"),
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          color = Color.White
        )
        Text(
          text = if (customer != null) {
            "No. Meter: ${customer.noMeter} • ${customer.rtRw.ifBlank { "Dusun Weru" }}"
          } else {
            userEmail ?: "Akun Google Pelanggan"
          },
          style = MaterialTheme.typography.bodySmall,
          color = Color.White.copy(alpha = 0.9f)
        )
      }
    }

    if (customer == null && !isProfileLoading) {
      item {
        ActivationRequestCard(
          userUid = userUid,
          userEmail = userEmail,
          userName = userName,
          myRequest = myRequest,
          onSubmit = onSubmitActivationRequest
        )
      }
    } else if (customer != null) {
      if (customer.ownerUid.isBlank()) {
        item {
          Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = WaterBlueContainer)
          ) {
            Column(modifier = Modifier.padding(14.dp)) {
              Text(
                text = "Profil Meteran Ditemukan!",
                style = MaterialTheme.typography.titleSmall,
                color = WaterBluePrimary
              )
              Text(
                text = "Ketuk tombol di bawah untuk menautkan akun Google Anda secara permanen ke meteran ${customer.noMeter}.",
                style = MaterialTheme.typography.bodySmall
              )
              Spacer(modifier = Modifier.height(8.dp))
              Button(
                onClick = { onClaimOrUpdateProfile(customer, customer.noHp, customer.alamat) },
                colors = ButtonDefaults.buttonColors(containerColor = WaterBluePrimary)
              ) {
                Text("Tautkan Profil Saya Sekarang")
              }
            }
          }
        }
      }

      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          StatMetricCard(
            label = "Meter Terakhir",
            value = "${customer.meterTerakhir} m³",
            icon = Icons.Rounded.Speed,
            accentColor = WaterBluePrimary,
            modifier = Modifier.weight(1f)
          )
          StatMetricCard(
            label = if (unpaidBills.isEmpty()) "Status Tagihan" else "Tagihan Aktif",
            value = if (unpaidBills.isEmpty()) "Lunas Semua" else TariffCalculator.formatRupiah(totalTunggakan),
            icon = if (unpaidBills.isEmpty()) Icons.Rounded.CheckCircle else Icons.Rounded.Payments,
            accentColor = if (unpaidBills.isEmpty()) LeafGreenSecondary else StatusUnpaidText,
            modifier = Modifier.weight(1f)
          )
        }
      }

      if (latestBill != null) {
        item {
          Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
          ) {
            Column(modifier = Modifier.padding(16.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "Tagihan ${TariffCalculator.formatPeriode(latestBill.periode)}",
                  style = MaterialTheme.typography.titleMedium
                )
                BillStatusPill(latestBill.status)
              }
              Spacer(modifier = Modifier.height(8.dp))
              Text(
                text = "Pemakaian: ${latestBill.pemakaian} m³ (${latestBill.meterAwal} → ${latestBill.meterAkhir} m³)",
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMono),
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Text(
                text = TariffCalculator.formatRupiah(latestBill.totalTagihan),
                style = MaterialTheme.typography.headlineSmall.copy(
                  fontFamily = JetBrainsMono,
                  fontWeight = FontWeight.Bold
                ),
                color = WaterBluePrimary
              )
              if (latestBill.status != "lunas") {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                  onClick = onGoToBills,
                  modifier = Modifier.fillMaxWidth(),
                  colors = ButtonDefaults.buttonColors(containerColor = LeafGreenSecondary),
                  shape = RoundedCornerShape(12.dp)
                ) {
                  Text(
                    if (latestBill.status == "menunggu_verifikasi") "Lihat Status Verifikasi"
                    else "Bayar / Kirim Bukti Transfer"
                  )
                }
              }
            }
          }
        }
      }

      // Customer Profile Card
      item {
        Card(
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Badge, contentDescription = null, tint = WaterBluePrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Profil Pelanggan", style = MaterialTheme.typography.titleMedium)
              }
              IconButton(onClick = { showEditDialog = true }) {
                Icon(Icons.Rounded.Edit, contentDescription = "Edit kontak")
              }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            ProfileRow("Nama Lengkap", customer.nama)
            ProfileRow("No. Meteran", customer.noMeter, mono = true)
            ProfileRow("Wilayah", customer.rtRw.ifBlank { "Dusun Weru" })
            ProfileRow("Alamat", customer.alamat.ifBlank { "-" })
            ProfileRow("No. HP / WA", customer.noHp.ifBlank { "-" })
            ProfileRow("Email Tertaut", customer.emailPelanggan.ifBlank { userEmail ?: "-" })
            ProfileRow("Status Sambungan", if (customer.aktif) "Aktif" else "Nonaktif")
          }
        }
      }
    }

    // Transfer Bank Info Card
    item {
      Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.AccountBalance, contentDescription = null, tint = LeafGreenSecondary)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Info Pembayaran Transfer & Tunai", style = MaterialTheme.typography.titleMedium)
          }
          Spacer(modifier = Modifier.height(8.dp))
          ProfileRow("Bank / Tujuan", appConfig.namaBank)
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text("No. Rekening", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              Text(
                text = appConfig.nomorRekening,
                style = MaterialTheme.typography.titleMedium.copy(
                  fontFamily = JetBrainsMono,
                  fontWeight = FontWeight.Bold
                ),
                color = WaterBluePrimary
              )
            }
            OutlinedButton(
              onClick = {
                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("Rekening PAMSIMAS", appConfig.nomorRekening))
              },
              shape = RoundedCornerShape(10.dp)
            ) {
              Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Salin")
            }
          }
          ProfileRow("Atas Nama", appConfig.atasNama)
          if (appConfig.kontakAdmin.isNotBlank() && appConfig.kontakAdmin != "-") {
            ProfileRow("Kontak Admin", appConfig.kontakAdmin)
          }
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = appConfig.catatanPembayaran,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    }
  }

  if (showEditDialog && customer != null) {
    var noHp by remember { mutableStateOf(customer.noHp) }
    var alamat by remember { mutableStateOf(customer.alamat) }
    AlertDialog(
      onDismissRequest = { showEditDialog = false },
      title = { Text("Perbarui Kontak Profil") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = noHp,
            onValueChange = { noHp = it },
            label = { Text("No. HP / WhatsApp") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true
          )
          OutlinedTextField(
            value = alamat,
            onValueChange = { alamat = it },
            label = { Text("Alamat Rumah") }
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            onClaimOrUpdateProfile(customer, noHp, alamat)
            showEditDialog = false
          }
        ) {
          Text("Simpan")
        }
      },
      dismissButton = {
        TextButton(onClick = { showEditDialog = false }) { Text("Batal") }
      }
    )
  }
}

@Composable
private fun ActivationRequestCard(
  userUid: String,
  userEmail: String?,
  userName: String?,
  myRequest: CustomerRequest?,
  onSubmit: (String, String, String, String, String) -> Unit
) {
  var nama by remember { mutableStateOf(myRequest?.nama ?: userName ?: "") }
  var noMeter by remember { mutableStateOf(myRequest?.noMeter ?: "") }
  var alamat by remember { mutableStateOf(myRequest?.alamat ?: "Dusun Weru") }
  var rtRw by remember { mutableStateOf(myRequest?.rtRw ?: "RT 01 / RW 01") }
  var noHp by remember { mutableStateOf(myRequest?.noHp ?: "") }

  Card(
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
  ) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Text("Akun Belum Tertaut Meteran", style = MaterialTheme.typography.titleMedium)
      Text(
        text = "Kirim pengajuan agar Admin PAMSIMAS Weru menautkan akun Google Anda (${userEmail ?: userUid}) ke data meteran rumah Anda.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )

      if (myRequest != null) {
        val (bg, fg, label) = when (myRequest.status) {
          "disetujui" -> Triple(StatusPaidBg, StatusPaidText, "Disetujui")
          "ditolak" -> Triple(StatusUnpaidBg, StatusUnpaidText, "Ditolak: ${myRequest.catatanAdmin}")
          else -> Triple(StatusPendingBg, StatusPendingText, "Menunggu Persetujuan Admin")
        }
        Surface(color = bg, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
          Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(Icons.Rounded.HourglassTop, contentDescription = null, tint = fg, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = fg)
          }
        }
      }

      OutlinedTextField(
        value = nama,
        onValueChange = { nama = it },
        label = { Text("Nama Lengkap Kepala Keluarga *") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
      )
      OutlinedTextField(
        value = noMeter,
        onValueChange = { noMeter = it },
        label = { Text("Nomor Meteran (jika tahu)") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
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

      Button(
        onClick = { onSubmit(nama, noMeter, alamat, rtRw, noHp) },
        enabled = nama.trim().length >= 2,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("submit_activation_request_button"),
        colors = ButtonDefaults.buttonColors(containerColor = WaterBluePrimary),
        shape = RoundedCornerShape(12.dp)
      ) {
        Text(if (myRequest == null) "Kirim Pengajuan Aktivasi" else "Perbarui Pengajuan")
      }
    }
  }
}

@Composable
fun PelangganMeterTab(
  readings: List<MeterReading>
) {
  val chartReadings = remember(readings) {
    readings.take(6).reversed()
  }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    if (chartReadings.isNotEmpty()) {
      item {
        Card(
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Rounded.WaterDrop, contentDescription = null, tint = WaterBluePrimary)
              Spacer(modifier = Modifier.width(8.dp))
              Text("Grafik Pemakaian Air (m³)", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(modifier = Modifier.height(12.dp))
            val maxUsage = max(10L, chartReadings.maxOf { it.pemakaian })
            Canvas(
              modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
            ) {
              val count = chartReadings.size
              val slotW = size.width / count.toFloat()
              val barW = (slotW * 0.55f).coerceAtMost(42.dp.toPx())
              chartReadings.forEachIndexed { idx, item ->
                val ratio = item.pemakaian.toFloat() / maxUsage.toFloat()
                val h = (ratio * (size.height - 16.dp.toPx())).coerceAtLeast(8.dp.toPx())
                val left = idx * slotW + (slotW - barW) / 2f
                val top = size.height - h
                val color = if (item.pemakaian < 8L) StatusPendingText else WaterBluePrimary
                drawRoundRect(
                  color = color,
                  topLeft = Offset(left, top),
                  size = Size(barW, h),
                  cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
                )
              }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceAround
            ) {
              chartReadings.forEach { r ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                  Text(
                    text = "${r.pemakaian}m³",
                    style = MaterialTheme.typography.labelSmall.copy(
                      fontFamily = JetBrainsMono,
                      fontWeight = FontWeight.Bold
                    )
                  )
                  Text(
                    text = r.periode.takeLast(2) + "/" + r.periode.substring(2, 4),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = "Biru: ≥ 8 m³ (tanpa beban) • Kuning: < 8 m³ (+ beban Rp5.000)",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }
    }

    if (readings.isEmpty()) {
      item {
        EmptyStateCard(
          icon = Icons.Rounded.Speed,
          title = "Belum Ada Riwayat Meteran",
          description = "Catatan angka meteran bulanan oleh petugas PAMSIMAS Weru akan tampil di sini."
        )
      }
    } else {
      items(readings, key = { it.id }) { r ->
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = TariffCalculator.formatPeriode(r.periode),
                style = MaterialTheme.typography.titleMedium
              )
              Text(
                text = "Meter Awal: ${r.meterAwal} m³ • Akhir: ${r.meterAkhir} m³",
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMono),
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              if (r.catatan.isNotBlank()) {
                Text(
                  text = r.catatan,
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
            Surface(
              color = MaterialTheme.colorScheme.primaryContainer,
              shape = RoundedCornerShape(50)
            ) {
              Text(
                text = "${r.pemakaian} m³",
                style = MaterialTheme.typography.titleSmall.copy(
                  fontFamily = JetBrainsMono,
                  fontWeight = FontWeight.Bold
                ),
                color = WaterBluePrimary,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun ProfileRow(label: String, value: String, mono: Boolean = false) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 3.dp),
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(
      text = value,
      style = if (mono) {
        MaterialTheme.typography.bodyMedium.copy(
          fontFamily = JetBrainsMono,
          fontWeight = FontWeight.SemiBold
        )
      } else {
        MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
      },
      color = MaterialTheme.colorScheme.onSurface
    )
  }
}
