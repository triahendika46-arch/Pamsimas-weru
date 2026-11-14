package com.example.util

import android.content.Context
import android.content.Intent
import com.example.data.model.Bill
import com.example.data.model.Payment
import com.example.domain.TariffCalculator
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale

object ReceiptUtils {
  fun formatTimestamp(ts: Timestamp?): String {
    if (ts == null) return "-"
    val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.forLanguageTag("id-ID"))
    return sdf.format(ts.toDate())
  }

  fun buildReceiptShareText(bill: Bill): String {
    val periodeText = TariffCalculator.formatPeriode(bill.periode)
    val tglBayar = formatTimestamp(bill.dibayarPada ?: bill.updatedAt)
    val metode = if (bill.metodePembayaran == "tunai") "Tunai" else "Transfer Bank"
    return buildString {
      appendLine("====================================")
      appendLine("    KUITANSI RESMI PEMBAYARAN AIR")
      appendLine("    PAMSIMAS WERU BY HENDIKA WP")
      appendLine("         Dusun Weru - Lunas")
      appendLine("====================================")
      appendLine("No. Kuitansi : ${bill.nomorKuitansi.ifBlank { "-" }}")
      appendLine("Periode      : $periodeText")
      appendLine("Pelanggan    : ${bill.customerNama}")
      appendLine("No. Meteran  : ${bill.noMeter}")
      if (bill.rtRw.isNotBlank()) appendLine("Wilayah      : ${bill.rtRw}")
      appendLine("------------------------------------")
      appendLine("Meter Awal   : ${bill.meterAwal} m3")
      appendLine("Meter Akhir  : ${bill.meterAkhir} m3")
      appendLine("Pemakaian    : ${bill.pemakaian} m3")
      appendLine("Biaya Air    : ${TariffCalculator.formatRupiah(bill.biayaPemakaian)} (@Rp2.000/m3)")
      if (bill.biayaBeban > 0L) {
        appendLine("Biaya Beban  : ${TariffCalculator.formatRupiah(bill.biayaBeban)} (Pemakaian < 8 m3)")
      } else {
        appendLine("Biaya Beban  : Rp0")
      }
      appendLine("TOTAL LUNAS  : ${TariffCalculator.formatRupiah(bill.totalTagihan)}")
      appendLine("------------------------------------")
      appendLine("Metode Bayar : $metode")
      appendLine("Waktu Lunas  : $tglBayar")
      appendLine("Diverifikasi : ${bill.diverifikasiOleh.ifBlank { "Hendika WP" }}")
      appendLine("====================================")
      appendLine("Terima kasih telah menjaga air bersih Dusun Weru.")
    }
  }

  fun buildPaymentReceiptShareText(payment: Payment): String {
    val periodeText = TariffCalculator.formatPeriode(payment.periode)
    val tglBayar = formatTimestamp(payment.updatedAt ?: payment.createdAt)
    val metode = if (payment.metode == "tunai") "Tunai" else "Transfer Bank"
    return buildString {
      appendLine("====================================")
      appendLine("    KUITANSI RESMI PEMBAYARAN AIR")
      appendLine("    PAMSIMAS WERU BY HENDIKA WP")
      appendLine("====================================")
      appendLine("No. Kuitansi : ${payment.nomorKuitansi.ifBlank { "-" }}")
      appendLine("Periode      : $periodeText")
      appendLine("Pelanggan    : ${payment.customerNama}")
      appendLine("No. Meteran  : ${payment.noMeter}")
      appendLine("------------------------------------")
      appendLine("Meter        : ${payment.meterAwal} -> ${payment.meterAkhir} m3 (${payment.pemakaian} m3)")
      appendLine("Biaya Air    : ${TariffCalculator.formatRupiah(payment.biayaPemakaian)}")
      appendLine("Biaya Beban  : ${TariffCalculator.formatRupiah(payment.biayaBeban)}")
      appendLine("TOTAL BAYAR  : ${TariffCalculator.formatRupiah(payment.jumlahBayar)}")
      appendLine("Metode       : $metode")
      appendLine("Tanggal      : $tglBayar")
      appendLine("Petugas      : ${payment.diverifikasiOleh.ifBlank { "Hendika WP" }}")
      appendLine("====================================")
    }
  }

  fun shareText(context: Context, title: String, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
      type = "text/plain"
      putExtra(Intent.EXTRA_SUBJECT, title)
      putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, title))
  }
}
