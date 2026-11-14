package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Bill
import com.example.data.model.Payment
import com.example.domain.TariffCalculator
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.LeafGreenSecondary
import com.example.ui.theme.StatusPaidBg
import com.example.ui.theme.StatusPaidText
import com.example.ui.theme.WaterBluePrimary
import com.example.util.ImageUtils
import com.example.util.ReceiptUtils

@Composable
fun ReceiptDialog(
  bill: Bill? = null,
  payment: Payment? = null,
  onDismiss: () -> Unit
) {
  val context = LocalContext.current
  val title = "Kuitansi Pembayaran PAMSIMAS Weru"

  val receiptNo = bill?.nomorKuitansi ?: payment?.nomorKuitansi ?: "-"
  val customerName = bill?.customerNama ?: payment?.customerNama ?: "-"
  val noMeter = bill?.noMeter ?: payment?.noMeter ?: "-"
  val periode = TariffCalculator.formatPeriode(bill?.periode ?: payment?.periode ?: "")
  val meterAwal = bill?.meterAwal ?: payment?.meterAwal ?: 0L
  val meterAkhir = bill?.meterAkhir ?: payment?.meterAkhir ?: 0L
  val pemakaian = bill?.pemakaian ?: payment?.pemakaian ?: 0L
  val biayaAir = bill?.biayaPemakaian ?: payment?.biayaPemakaian ?: 0L
  val biayaBeban = bill?.biayaBeban ?: payment?.biayaBeban ?: 0L
  val total = bill?.totalTagihan ?: payment?.jumlahBayar ?: 0L
  val metode = when (bill?.metodePembayaran ?: payment?.metode) {
    "tunai" -> "Tunai"
    "transfer" -> "Transfer Bank"
    else -> "-"
  }
  val tanggal = ReceiptUtils.formatTimestamp(
    bill?.dibayarPada ?: bill?.updatedAt ?: payment?.updatedAt ?: payment?.createdAt
  )
  val verifier = (bill?.diverifikasiOleh ?: payment?.diverifikasiOleh).orEmpty().ifBlank { "Hendika WP" }
  val shareBody = when {
    bill != null -> ReceiptUtils.buildReceiptShareText(bill)
    payment != null -> ReceiptUtils.buildPaymentReceiptShareText(payment)
    else -> ""
  }

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(24.dp),
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 6.dp,
      modifier = Modifier
        .fillMaxWidth()
        .testTag("receipt_dialog")
    ) {
      Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .background(
              Brush.horizontalGradient(listOf(WaterBluePrimary, LeafGreenSecondary))
            )
            .padding(18.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Rounded.ReceiptLong,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(26.dp)
              )
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text(
                  text = "KUITANSI RESMI",
                  style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                  color = Color.White
                )
                Text(
                  text = "PAMSIMAS WERU BY HENDIKA WP",
                  style = MaterialTheme.typography.labelSmall,
                  color = Color.White.copy(alpha = 0.9f)
                )
              }
            }
            IconButton(onClick = onDismiss) {
              Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "Tutup",
                tint = Color.White
              )
            }
          }
        }

        Column(modifier = Modifier.padding(18.dp)) {
          Surface(
            color = StatusPaidBg,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.Center
            ) {
              Icon(
                imageVector = Icons.Rounded.CheckCircle,
                contentDescription = null,
                tint = StatusPaidText,
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "PEMBAYARAN LUNAS • $metode",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = StatusPaidText
              )
            }
          }

          Spacer(modifier = Modifier.height(14.dp))
          ReceiptRow("No. Kuitansi", receiptNo, mono = true)
          ReceiptRow("Periode Tagihan", periode)
          ReceiptRow("Nama Pelanggan", customerName)
          ReceiptRow("No. Meteran", noMeter, mono = true)

          HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

          ReceiptRow("Meter Awal", "$meterAwal m³", mono = true)
          ReceiptRow("Meter Akhir", "$meterAkhir m³", mono = true)
          ReceiptRow("Pemakaian Air", "$pemakaian m³", mono = true)
          ReceiptRow("Biaya Air (Rp2.000/m³)", TariffCalculator.formatRupiah(biayaAir), mono = true)
          ReceiptRow(
            if (biayaBeban > 0L) "Beban (< 8 m³)" else "Biaya Beban",
            TariffCalculator.formatRupiah(biayaBeban),
            mono = true
          )

          HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "TOTAL BAYAR",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = TariffCalculator.formatRupiah(total),
              style = MaterialTheme.typography.titleLarge.copy(
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Bold
              ),
              color = MaterialTheme.colorScheme.primary
            )
          }

          HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

          ReceiptRow("Waktu Lunas", tanggal)
          ReceiptRow("Petugas / Admin", verifier)

          Spacer(modifier = Modifier.height(16.dp))

          Button(
            onClick = { ReceiptUtils.shareText(context, title, shareBody) },
            modifier = Modifier
              .fillMaxWidth()
              .testTag("share_receipt_button"),
            colors = ButtonDefaults.buttonColors(containerColor = LeafGreenSecondary),
            shape = RoundedCornerShape(14.dp)
          ) {
            Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Bagikan Kuitansi")
          }
        }
      }
    }
  }
}

@Composable
private fun ReceiptRow(label: String, value: String, mono: Boolean = false) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 3.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Text(
      text = value,
      style = if (mono) {
        MaterialTheme.typography.bodyMedium.copy(
          fontFamily = JetBrainsMono,
          fontWeight = FontWeight.Medium
        )
      } else {
        MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
      },
      color = MaterialTheme.colorScheme.onSurface
    )
  }
}

@Composable
fun ProofImagePreviewDialog(
  base64: String,
  title: String,
  onDismiss: () -> Unit
) {
  val bitmap = remember(base64) { ImageUtils.decodeBase64ToImageBitmap(base64) }
  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(20.dp),
      color = MaterialTheme.colorScheme.surface,
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(title, style = MaterialTheme.typography.titleMedium)
          IconButton(onClick = onDismiss) {
            Icon(Icons.Rounded.Close, contentDescription = "Tutup")
          }
        }
        Spacer(modifier = Modifier.height(8.dp))
        if (bitmap != null) {
          Image(
            bitmap = bitmap,
            contentDescription = "Bukti transfer",
            modifier = Modifier
              .fillMaxWidth()
              .height(360.dp)
              .clip(RoundedCornerShape(14.dp)),
            contentScale = ContentScale.Fit
          )
        } else {
          Text(
            text = "Gambar bukti transfer tidak dapat dimuat.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error
          )
        }
      }
    }
  }
}
