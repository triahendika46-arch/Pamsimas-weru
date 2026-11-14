package com.example.ui.admin

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.PersonSearch
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.Bill
import com.example.data.model.Customer
import com.example.data.model.MeterReading
import com.example.data.model.Payment
import com.example.domain.TariffCalculator
import com.example.ui.components.BillStatusPill
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ProofImagePreviewDialog
import com.example.ui.components.ReceiptDialog
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.LeafGreenSecondary
import com.example.ui.theme.StatusPendingBg
import com.example.ui.theme.StatusPendingText
import com.example.ui.theme.WaterBlueContainer
import com.example.ui.theme.WaterBluePrimary
import com.example.util.ImageUtils

@Composable
fun AdminMeterTab(
  customers: List<Customer>,
  readings: List<MeterReading>,
  isBusy: Boolean,
  onRecordMeter: (Customer, String, Long, Long, String) -> Unit
) {
  var periode by remember { mutableStateOf(TariffCalculator.currentPeriode()) }
  var selectedCustomerId by remember { mutableStateOf<String?>(null) }
  var showCustomerPicker by remember { mutableStateOf(false) }

  val selectedCustomer = remember(customers, selectedCustomerId) {
    customers.firstOrNull { it.id == selectedCustomerId }
  }

  var meterAwalText by remember(selectedCustomer?.id) {
    mutableStateOf((selectedCustomer?.meterTerakhir ?: 0L).toString())
  }
  var meterAkhirText by remember(selectedCustomer?.id) { mutableStateOf("") }
  var catatan by remember { mutableStateOf("") }

  val meterAwal = meterAwalText.toLongOrNull() ?: 0L
  val meterAkhir = meterAkhirText.toLongOrNull()
  val validCalculation = remember(meterAwal, meterAkhir) {
    if (meterAkhir != null && meterAkhir >= meterAwal) {
      TariffCalculator.calculate(meterAwal, meterAkhir)
    } else null
  }

  val periodReadings = remember(readings, periode) {
    readings.filter { it.periode == periode }
  }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    item {
      Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Text(
            text = "Catat Meteran & Terbitkan Tagihan",
            style = MaterialTheme.typography.titleMedium
          )
          Spacer(modifier = Modifier.height(10.dp))

          // Period Selector
          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              IconButton(onClick = { periode = shiftPeriode(periode, -1) }) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "Bulan sebelumnya")
              }
              Text(
                text = TariffCalculator.formatPeriode(periode),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
              )
              IconButton(onClick = { periode = shiftPeriode(periode, 1) }) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "Bulan berikutnya")
              }
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          OutlinedButton(
            onClick = { showCustomerPicker = true },
            modifier = Modifier
              .fillMaxWidth()
              .testTag("select_customer_meter_button"),
            shape = RoundedCornerShape(12.dp)
          ) {
            Icon(Icons.Rounded.PersonSearch, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = selectedCustomer?.let { "${it.noMeter} • ${it.nama}" }
                ?: "Pilih Pelanggan (${customers.count { it.aktif }} aktif)"
            )
          }

          if (selectedCustomer != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
              OutlinedTextField(
                value = meterAwalText,
                onValueChange = { meterAwalText = it.filter { ch -> ch.isDigit() } },
                label = { Text("Meter Awal (m³)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f).testTag("meter_awal_input")
              )
              OutlinedTextField(
                value = meterAkhirText,
                onValueChange = { meterAkhirText = it.filter { ch -> ch.isDigit() } },
                label = { Text("Meter Akhir (m³) *") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f).testTag("meter_akhir_input")
              )
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
              value = catatan,
              onValueChange = { catatan = it },
              label = { Text("Catatan Pencatatan (Opsional)") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (validCalculation != null) {
              Surface(
                color = WaterBlueContainer.copy(alpha = 0.65f),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(modifier = Modifier.padding(12.dp)) {
                  Text(
                    text = "Pratinjau Perhitungan Otomatis",
                    style = MaterialTheme.typography.labelLarge,
                    color = WaterBluePrimary
                  )
                  Spacer(modifier = Modifier.height(6.dp))
                  CalcRow("Pemakaian Air", "${validCalculation.pemakaian} m³")
                  CalcRow(
                    "Biaya Pemakaian (@Rp2.000/m³)",
                    TariffCalculator.formatRupiah(validCalculation.biayaPemakaian)
                  )
                  CalcRow(
                    if (validCalculation.pemakaian < 8L) "Beban (< 8 m³)" else "Biaya Beban",
                    TariffCalculator.formatRupiah(validCalculation.biayaBeban)
                  )
                  HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                  ) {
                    Text("Total Tagihan", style = MaterialTheme.typography.titleSmall)
                    Text(
                      text = TariffCalculator.formatRupiah(validCalculation.totalTagihan),
                      style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = JetBrainsMono,
                        fontWeight = FontWeight.Bold
                      ),
                      color = WaterBluePrimary
                    )
                  }
                }
              }
            } else if (meterAkhir != null && meterAkhir < meterAwal) {
              Text(
                text = "Meter akhir tidak boleh lebih kecil dari meter awal ($meterAwal m³).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
              )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
              onClick = {
                onRecordMeter(selectedCustomer, periode, meterAwal, meterAkhir!!, catatan)
                meterAkhirText = ""
                catatan = ""
                selectedCustomerId = null
              },
              enabled = !isBusy && validCalculation != null,
              modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("save_meter_reading_button"),
              colors = ButtonDefaults.buttonColors(containerColor = LeafGreenSecondary),
              shape = RoundedCornerShape(12.dp)
            ) {
              Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text("Simpan Meteran & Buat Tagihan")
            }
          }
        }
      }
    }

    item {
      Text(
        text = "Tercatat pada ${TariffCalculator.formatPeriode(periode)} (${periodReadings.size})",
        style = MaterialTheme.typography.titleMedium
      )
    }

    if (periodReadings.isEmpty()) {
      item {
        EmptyStateCard(
          icon = Icons.Rounded.Speed,
          title = "Belum Ada Pencatatan Bulan Ini",
          description = "Pilih pelanggan di atas untuk mencatat angka meteran dan menerbitkan tagihan otomatis."
        )
      }
    } else {
      items(periodReadings, key = { it.id }) { reading ->
        Card(
          shape = RoundedCornerShape(14.dp),
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
                text = "${reading.noMeter} • ${reading.customerNama}",
                style = MaterialTheme.typography.titleSmall
              )
              Text(
                text = "Meter: ${reading.meterAwal} → ${reading.meterAkhir} m³",
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMono),
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
            Surface(
              color = MaterialTheme.colorScheme.primaryContainer,
              shape = RoundedCornerShape(50)
            ) {
              Text(
                text = "${reading.pemakaian} m³",
                style = MaterialTheme.typography.labelMedium.copy(
                  fontFamily = JetBrainsMono,
                  fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
              )
            }
          }
        }
      }
    }
  }

  if (showCustomerPicker) {
    CustomerPickerDialog(
      customers = customers.filter { it.aktif },
      recordedIds = periodReadings.map { it.customerId }.toSet(),
      onDismiss = { showCustomerPicker = false },
      onSelect = { cust ->
        selectedCustomerId = cust.id
        showCustomerPicker = false
      }
    )
  }
}

@Composable
private fun CustomerPickerDialog(
  customers: List<Customer>,
  recordedIds: Set<String>,
  onDismiss: () -> Unit,
  onSelect: (Customer) -> Unit
) {
  var query by remember { mutableStateOf("") }
  val filtered = remember(customers, query) {
    customers.filter {
      query.isBlank() ||
        it.nama.contains(query, ignoreCase = true) ||
        it.noMeter.contains(query, ignoreCase = true) ||
        it.rtRw.contains(query, ignoreCase = true)
    }
  }
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Pilih Pelanggan") },
    text = {
      Column {
        OutlinedTextField(
          value = query,
          onValueChange = { query = it },
          placeholder = { Text("Cari nama / no. meter...") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn(modifier = Modifier.height(300.dp)) {
          items(filtered, key = { it.id }) { cust ->
            val alreadyRecorded = cust.id in recordedIds
            Surface(
              onClick = { onSelect(cust) },
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 10.dp, horizontal = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text("${cust.noMeter} • ${cust.nama}", style = MaterialTheme.typography.titleSmall)
                  Text(
                    "Meter terakhir: ${cust.meterTerakhir} m³",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
                if (alreadyRecorded) {
                  Text(
                    "Tercatat",
                    style = MaterialTheme.typography.labelSmall,
                    color = LeafGreenSecondary
                  )
                }
              }
            }
            HorizontalDivider()
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) { Text("Tutup") }
    }
  )
}

@Composable
fun AdminBillsTab(
  bills: List<Bill>,
  payments: List<Payment>,
  onRecordCash: (Bill) -> Unit,
  onVerifyTransfer: (Bill, Payment?) -> Unit,
  onRejectTransfer: (Bill, Payment?, String) -> Unit
) {
  var statusFilter by remember { mutableStateOf("semua") }
  var searchQuery by remember { mutableStateOf("") }
  var cashConfirmBill by remember { mutableStateOf<Bill?>(null) }
  var inspectTransferBill by remember { mutableStateOf<Bill?>(null) }
  var receiptBill by remember { mutableStateOf<Bill?>(null) }
  var zoomImageBase64 by remember { mutableStateOf<String?>(null) }

  val filteredBills = remember(bills, statusFilter, searchQuery) {
    bills.filter { bill ->
      val matchesStatus = statusFilter == "semua" || bill.status == statusFilter
      val matchesSearch = searchQuery.isBlank() ||
        bill.customerNama.contains(searchQuery, ignoreCase = true) ||
        bill.noMeter.contains(searchQuery, ignoreCase = true) ||
        bill.periode.contains(searchQuery, ignoreCase = true)
      matchesStatus && matchesSearch
    }
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp)
  ) {
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedTextField(
      value = searchQuery,
      onValueChange = { searchQuery = it },
      modifier = Modifier.fillMaxWidth(),
      placeholder = { Text("Cari nama, no. meter, atau periode (2026-11)...") },
      leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
      singleLine = true,
      shape = RoundedCornerShape(14.dp)
    )
    Spacer(modifier = Modifier.height(8.dp))
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      val tabs = listOf(
        "semua" to "Semua (${bills.size})",
        "belum_bayar" to "Belum Bayar (${bills.count { it.status == "belum_bayar" }})",
        "menunggu_verifikasi" to "Verifikasi (${bills.count { it.status == "menunggu_verifikasi" }})",
        "lunas" to "Lunas (${bills.count { it.status == "lunas" }})",
        "ditolak" to "Ditolak (${bills.count { it.status == "ditolak" }})"
      )
      items(tabs) { (key, label) ->
        FilterChip(
          selected = statusFilter == key,
          onClick = { statusFilter = key },
          label = { Text(label) }
        )
      }
    }
    Spacer(modifier = Modifier.height(8.dp))

    if (filteredBills.isEmpty()) {
      EmptyStateCard(
        icon = Icons.Rounded.ReceiptLong,
        title = "Tidak Ada Tagihan",
        description = "Tagihan akan terbit otomatis setelah pencatatan meteran."
      )
    } else {
      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        items(filteredBills, key = { it.id }) { bill ->
          Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
          ) {
            Column(modifier = Modifier.padding(14.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = "${bill.noMeter} • ${bill.customerNama}",
                    style = MaterialTheme.typography.titleMedium
                  )
                  Text(
                    text = TariffCalculator.formatPeriode(bill.periode),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
                BillStatusPill(bill.status)
              }

              Spacer(modifier = Modifier.height(8.dp))
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text(
                  text = "Meter ${bill.meterAwal} → ${bill.meterAkhir} (${bill.pemakaian} m³)",
                  style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMono),
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                  text = TariffCalculator.formatRupiah(bill.totalTagihan),
                  style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Bold
                  ),
                  color = WaterBluePrimary
                )
              }
              if (bill.biayaBeban > 0L) {
                Text(
                  text = "Termasuk beban Rp5.000 (pemakaian < 8 m³)",
                  style = MaterialTheme.typography.labelSmall,
                  color = StatusPendingText
                )
              }

              Spacer(modifier = Modifier.height(10.dp))

              when (bill.status) {
                "lunas" -> {
                  OutlinedButton(
                    onClick = { receiptBill = bill },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                  ) {
                    Icon(Icons.Rounded.ReceiptLong, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Lihat & Bagikan Kuitansi (${bill.nomorKuitansi})")
                  }
                }
                "menunggu_verifikasi" -> {
                  Button(
                    onClick = { inspectTransferBill = bill },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = StatusPendingText),
                    shape = RoundedCornerShape(10.dp)
                  ) {
                    Text("Periksa Bukti Transfer")
                  }
                }
                else -> {
                  Button(
                    onClick = { cashConfirmBill = bill },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = LeafGreenSecondary),
                    shape = RoundedCornerShape(10.dp)
                  ) {
                    Icon(Icons.Rounded.Payments, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Bayar Tunai Sekarang")
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  if (cashConfirmBill != null) {
    val bill = cashConfirmBill!!
    AlertDialog(
      onDismissRequest = { cashConfirmBill = null },
      title = { Text("Konfirmasi Pembayaran Tunai") },
      text = {
        Text(
          "Tandai tagihan ${bill.customerNama} (${TariffCalculator.formatPeriode(bill.periode)}) sebesar ${TariffCalculator.formatRupiah(bill.totalTagihan)} sebagai LUNAS Tunai?"
        )
      },
      confirmButton = {
        Button(
          onClick = {
            onRecordCash(bill)
            cashConfirmBill = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = LeafGreenSecondary)
        ) {
          Text("Ya, Terima Tunai")
        }
      },
      dismissButton = {
        TextButton(onClick = { cashConfirmBill = null }) { Text("Batal") }
      }
    )
  }

  if (inspectTransferBill != null) {
    val bill = inspectTransferBill!!
    val matchingPayment = payments.firstOrNull {
      it.billId == bill.id && it.status == "menunggu_verifikasi"
    }
    var rejectReason by remember { mutableStateOf("") }
    var showRejectField by remember { mutableStateOf(false) }
    val thumbBitmap = remember(bill.proofImageBase64) {
      ImageUtils.decodeBase64ToImageBitmap(bill.proofImageBase64)
    }

    AlertDialog(
      onDismissRequest = { inspectTransferBill = null },
      title = { Text("Verifikasi Bukti Transfer") },
      text = {
        Column(
          modifier = Modifier.verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Text(
            "${bill.customerNama} • ${TariffCalculator.formatRupiah(bill.totalTagihan)}",
            style = MaterialTheme.typography.titleSmall
          )
          Surface(
            color = StatusPendingBg,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(10.dp)) {
              Text("Pengirim: ${bill.namaPengirimTransfer}", style = MaterialTheme.typography.bodySmall)
              Text("Bank/E-Wallet: ${bill.bankPengirim}", style = MaterialTheme.typography.bodySmall)
              if (bill.catatanTransfer.isNotBlank()) {
                Text("Catatan: ${bill.catatanTransfer}", style = MaterialTheme.typography.bodySmall)
              }
            }
          }
          if (thumbBitmap != null) {
            Image(
              bitmap = thumbBitmap,
              contentDescription = "Bukti transfer",
              modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable { zoomImageBase64 = bill.proofImageBase64 },
              contentScale = ContentScale.Crop
            )
            Text(
              "Ketuk gambar untuk memperbesar",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.primary
            )
          } else {
            Text(
              "Tidak ada lampiran gambar bukti transfer.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }

          if (showRejectField) {
            OutlinedTextField(
              value = rejectReason,
              onValueChange = { rejectReason = it },
              label = { Text("Alasan Penolakan *") },
              modifier = Modifier.fillMaxWidth()
            )
          }
        }
      },
      confirmButton = {
        if (showRejectField) {
          Button(
            onClick = {
              onRejectTransfer(
                bill,
                matchingPayment,
                rejectReason.ifBlank { "Bukti transfer tidak terbaca / nominal tidak sesuai" }
              )
              inspectTransferBill = null
            },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
          ) {
            Text("Konfirmasi Tolak")
          }
        } else {
          Button(
            onClick = {
              onVerifyTransfer(bill, matchingPayment)
              inspectTransferBill = null
            },
            colors = ButtonDefaults.buttonColors(containerColor = LeafGreenSecondary)
          ) {
            Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Verifikasi Lunas")
          }
        }
      },
      dismissButton = {
        Row {
          if (!showRejectField) {
            TextButton(
              onClick = { showRejectField = true },
              colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
              Icon(Icons.Rounded.Close, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Tolak")
            }
          }
          TextButton(onClick = { inspectTransferBill = null }) { Text("Tutup") }
        }
      }
    )
  }

  if (receiptBill != null) {
    ReceiptDialog(bill = receiptBill!!, onDismiss = { receiptBill = null })
  }

  if (zoomImageBase64 != null) {
    ProofImagePreviewDialog(
      base64 = zoomImageBase64!!,
      title = "Bukti Transfer Pelanggan",
      onDismiss = { zoomImageBase64 = null }
    )
  }
}

@Composable
private fun CalcRow(label: String, value: String) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 2.dp),
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Text(label, style = MaterialTheme.typography.bodySmall)
    Text(
      value,
      style = MaterialTheme.typography.bodySmall.copy(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.SemiBold
      )
    )
  }
}

fun shiftPeriode(periode: String, deltaMonths: Int): String {
  val parts = periode.split("-")
  val year = parts.getOrNull(0)?.toIntOrNull() ?: 2026
  val month = parts.getOrNull(1)?.toIntOrNull() ?: 1
  val totalMonths = year * 12 + (month - 1) + deltaMonths
  val newYear = totalMonths / 12
  val newMonth = (totalMonths % 12) + 1
  return "%04d-%02d".format(newYear, newMonth)
}
