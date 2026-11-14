package com.example.ui.pelanggan

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AppConfig
import com.example.data.model.Bill
import com.example.data.model.Payment
import com.example.domain.TariffCalculator
import com.example.ui.components.BillStatusPill
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ProofImagePreviewDialog
import com.example.ui.components.ReceiptDialog
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.LeafGreenSecondary
import com.example.ui.theme.StatusPendingText
import com.example.ui.theme.StatusUnpaidBg
import com.example.ui.theme.StatusUnpaidText
import com.example.ui.theme.WaterBlueContainer
import com.example.ui.theme.WaterBluePrimary
import com.example.util.ImageUtils
import com.example.util.ReceiptUtils

@Composable
fun PelangganBillsTab(
  bills: List<Bill>,
  appConfig: AppConfig,
  isBusy: Boolean,
  onSubmitTransferProof: (Bill, String, String, String, String) -> Unit
) {
  var transferTargetBill by remember { mutableStateOf<Bill?>(null) }
  var receiptBill by remember { mutableStateOf<Bill?>(null) }

  if (bills.isEmpty()) {
    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(16.dp)
    ) {
      item {
        EmptyStateCard(
          icon = Icons.Rounded.ReceiptLong,
          title = "Belum Ada Tagihan",
          description = "Tagihan air bulanan Anda akan muncul otomatis setelah petugas mencatat meteran."
        )
      }
    }
  } else {
    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      items(bills, key = { it.id }) { bill ->
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
                text = TariffCalculator.formatPeriode(bill.periode),
                style = MaterialTheme.typography.titleMedium
              )
              BillStatusPill(bill.status)
            }
            Spacer(modifier = Modifier.height(8.dp))
            BillBreakdownRow("Meteran", "${bill.meterAwal} → ${bill.meterAkhir} m³ (${bill.pemakaian} m³)")
            BillBreakdownRow("Biaya Air (@Rp2.000/m³)", TariffCalculator.formatRupiah(bill.biayaPemakaian))
            BillBreakdownRow(
              if (bill.biayaBeban > 0L) "Beban (< 8 m³)" else "Biaya Beban",
              TariffCalculator.formatRupiah(bill.biayaBeban)
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text("Total Tagihan", style = MaterialTheme.typography.titleSmall)
              Text(
                text = TariffCalculator.formatRupiah(bill.totalTagihan),
                style = MaterialTheme.typography.titleLarge.copy(
                  fontFamily = JetBrainsMono,
                  fontWeight = FontWeight.Bold
                ),
                color = WaterBluePrimary
              )
            }

            if (bill.status == "ditolak" && bill.alasanPenolakan.isNotBlank()) {
              Spacer(modifier = Modifier.height(8.dp))
              Surface(color = StatusUnpaidBg, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                Text(
                  text = "Ditolak: ${bill.alasanPenolakan}",
                  style = MaterialTheme.typography.bodySmall,
                  color = StatusUnpaidText,
                  modifier = Modifier.padding(10.dp)
                )
              }
            }

            Spacer(modifier = Modifier.height(12.dp))

            when (bill.status) {
              "lunas" -> {
                OutlinedButton(
                  onClick = { receiptBill = bill },
                  modifier = Modifier.fillMaxWidth(),
                  shape = RoundedCornerShape(12.dp)
                ) {
                  Icon(Icons.Rounded.ReceiptLong, contentDescription = null, modifier = Modifier.size(18.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Lihat Kuitansi Resmi")
                }
              }
              "menunggu_verifikasi" -> {
                Surface(
                  color = MaterialTheme.colorScheme.surfaceVariant,
                  shape = RoundedCornerShape(12.dp),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Text(
                    text = "Bukti transfer sudah dikirim (${bill.bankPengirim} a.n ${bill.namaPengirimTransfer}). Menunggu verifikasi Admin.",
                    style = MaterialTheme.typography.bodySmall,
                    color = StatusPendingText,
                    modifier = Modifier.padding(12.dp)
                  )
                }
              }
              else -> {
                Button(
                  onClick = { transferTargetBill = bill },
                  enabled = !isBusy,
                  modifier = Modifier
                    .fillMaxWidth()
                    .testTag("upload_transfer_button_${bill.periode}"),
                  colors = ButtonDefaults.buttonColors(containerColor = LeafGreenSecondary),
                  shape = RoundedCornerShape(12.dp)
                ) {
                  Icon(Icons.Rounded.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(if (bill.status == "ditolak") "Kirim Ulang Bukti Transfer" else "Kirim Bukti Transfer")
                }
              }
            }
          }
        }
      }
    }
  }

  if (transferTargetBill != null) {
    TransferProofDialog(
      bill = transferTargetBill!!,
      appConfig = appConfig,
      onDismiss = { transferTargetBill = null },
      onSubmit = { nama, bank, catatan, base64 ->
        onSubmitTransferProof(transferTargetBill!!, nama, bank, catatan, base64)
        transferTargetBill = null
      }
    )
  }

  if (receiptBill != null) {
    ReceiptDialog(bill = receiptBill!!, onDismiss = { receiptBill = null })
  }
}

@Composable
private fun TransferProofDialog(
  bill: Bill,
  appConfig: AppConfig,
  onDismiss: () -> Unit,
  onSubmit: (String, String, String, String) -> Unit
) {
  val context = LocalContext.current
  var namaPengirim by remember { mutableStateOf(bill.customerNama) }
  var bankPengirim by remember { mutableStateOf("") }
  var catatan by remember { mutableStateOf("") }
  var encodedBase64 by remember { mutableStateOf("") }
  var imageError by remember { mutableStateOf<String?>(null) }

  // Compliant zero-permission Android Photo Picker
  val photoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia(),
    onResult = { uri ->
      if (uri != null) {
        val b64 = ImageUtils.encodeUriToBase64Jpeg(context, uri)
        if (b64 != null) {
          encodedBase64 = b64
          imageError = null
        } else {
          imageError = "Gagal memproses gambar. Coba pilih foto lain."
        }
      }
    }
  )

  val previewBitmap = remember(encodedBase64) {
    ImageUtils.decodeBase64ToImageBitmap(encodedBase64)
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Kirim Bukti Transfer") },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Surface(color = WaterBlueContainer, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
          Column(modifier = Modifier.padding(10.dp)) {
            Text(
              text = "Tagihan ${TariffCalculator.formatPeriode(bill.periode)}: ${TariffCalculator.formatRupiah(bill.totalTagihan)}",
              style = MaterialTheme.typography.titleSmall,
              color = WaterBluePrimary
            )
            Text(
              text = "Transfer ke ${appConfig.namaBank} • ${appConfig.nomorRekening} a.n ${appConfig.atasNama}",
              style = MaterialTheme.typography.bodySmall
            )
          }
        }

        OutlinedTextField(
          value = namaPengirim,
          onValueChange = { namaPengirim = it },
          label = { Text("Nama Pengirim Rekening *") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
          value = bankPengirim,
          onValueChange = { bankPengirim = it },
          label = { Text("Bank / E-Wallet Pengirim * (cth: BRI, DANA)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
          value = catatan,
          onValueChange = { catatan = it },
          label = { Text("Catatan (Opsional)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )

        OutlinedButton(
          onClick = {
            photoPickerLauncher.launch(
              PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
          },
          modifier = Modifier
            .fillMaxWidth()
            .testTag("pick_transfer_photo_button"),
          shape = RoundedCornerShape(12.dp)
        ) {
          Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(if (encodedBase64.isBlank()) "Pilih Foto Bukti Transfer" else "Ganti Foto Bukti Transfer")
        }

        if (previewBitmap != null) {
          Image(
            bitmap = previewBitmap,
            contentDescription = "Pratinjau bukti transfer",
            modifier = Modifier
              .fillMaxWidth()
              .height(140.dp)
              .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop
          )
        }
        if (imageError != null) {
          Text(imageError!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
      }
    },
    confirmButton = {
      Button(
        onClick = { onSubmit(namaPengirim, bankPengirim, catatan, encodedBase64) },
        enabled = namaPengirim.isNotBlank() && bankPengirim.isNotBlank() && encodedBase64.isNotBlank(),
        colors = ButtonDefaults.buttonColors(containerColor = LeafGreenSecondary),
        modifier = Modifier.testTag("submit_transfer_proof_button")
      ) {
        Icon(Icons.Rounded.Send, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Kirim")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Batal") }
    }
  )
}

@Composable
fun PelangganPaymentsTab(
  payments: List<Payment>
) {
  var receiptPayment by remember { mutableStateOf<Payment?>(null) }
  var zoomImage by remember { mutableStateOf<String?>(null) }

  if (payments.isEmpty()) {
    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(16.dp)
    ) {
      item {
        EmptyStateCard(
          icon = Icons.Rounded.History,
          title = "Belum Ada Riwayat Pembayaran",
          description = "Riwayat pembayaran tunai maupun transfer Anda akan tercatat rapi di sini."
        )
      }
    }
  } else {
    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      items(payments, key = { it.id }) { payment ->
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
                  text = TariffCalculator.formatPeriode(payment.periode),
                  style = MaterialTheme.typography.titleMedium
                )
                Text(
                  text = (if (payment.metode == "tunai") "Tunai" else "Transfer (${payment.bankPengirim})") +
                    " • " + ReceiptUtils.formatTimestamp(payment.updatedAt ?: payment.createdAt),
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
              BillStatusPill(payment.status)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "Pemakaian ${payment.pemakaian} m³",
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMono),
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Text(
                text = TariffCalculator.formatRupiah(payment.jumlahBayar),
                style = MaterialTheme.typography.titleMedium.copy(
                  fontFamily = JetBrainsMono,
                  fontWeight = FontWeight.Bold
                ),
                color = WaterBluePrimary
              )
            }
            if (payment.status == "lunas") {
              Spacer(modifier = Modifier.height(10.dp))
              OutlinedButton(
                onClick = { receiptPayment = payment },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
              ) {
                Icon(Icons.Rounded.ReceiptLong, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Kuitansi ${payment.nomorKuitansi}")
              }
            } else if (payment.proofImageBase64.isNotBlank()) {
              Spacer(modifier = Modifier.height(8.dp))
              Text(
                text = "Lihat lampiran bukti transfer",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { zoomImage = payment.proofImageBase64 }
              )
            }
          }
        }
      }
    }
  }

  if (receiptPayment != null) {
    ReceiptDialog(payment = receiptPayment!!, onDismiss = { receiptPayment = null })
  }

  if (zoomImage != null) {
    ProofImagePreviewDialog(
      base64 = zoomImage!!,
      title = "Bukti Transfer Saya",
      onDismiss = { zoomImage = null }
    )
  }
}

@Composable
private fun BillBreakdownRow(label: String, value: String) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 2.dp),
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(
      value,
      style = MaterialTheme.typography.bodySmall.copy(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Medium
      )
    )
  }
}
