package com.example.domain

import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

data class BillCalculation(
  val meterAwal: Long,
  val meterAkhir: Long,
  val pemakaian: Long,
  val tarifPerM3: Long,
  val biayaPemakaian: Long,
  val biayaBeban: Long,
  val totalTagihan: Long
)

object TariffCalculator {
  const val TARIF_PER_M3 = 2000L
  const val BATAS_BEBAN_M3 = 8L
  const val BIAYA_BEBAN_KURANG_8 = 5000L

  fun calculate(meterAwal: Long, meterAkhir: Long): BillCalculation {
    require(meterAwal >= 0L) { "Meter awal tidak boleh negatif" }
    require(meterAkhir >= meterAwal) { "Meter akhir ($meterAkhir) harus >= meter awal ($meterAwal)" }
    val pemakaian = meterAkhir - meterAwal
    val biayaPemakaian = pemakaian * TARIF_PER_M3
    val biayaBeban = if (pemakaian < BATAS_BEBAN_M3) BIAYA_BEBAN_KURANG_8 else 0L
    val totalTagihan = biayaPemakaian + biayaBeban
    return BillCalculation(
      meterAwal = meterAwal,
      meterAkhir = meterAkhir,
      pemakaian = pemakaian,
      tarifPerM3 = TARIF_PER_M3,
      biayaPemakaian = biayaPemakaian,
      biayaBeban = biayaBeban,
      totalTagihan = totalTagihan
    )
  }

  fun formatRupiah(amount: Long): String {
    val formatter = NumberFormat.getNumberInstance(Locale.forLanguageTag("id-ID"))
    return "Rp${formatter.format(amount)}"
  }

  fun currentPeriode(): String {
    val cal = Calendar.getInstance()
    val year = cal.get(Calendar.YEAR)
    val month = cal.get(Calendar.MONTH) + 1
    return "%04d-%02d".format(year, month)
  }

  fun formatPeriode(periode: String): String {
    val parts = periode.split("-")
    if (parts.size != 2) return periode
    val year = parts[0]
    val monthName = when (parts[1]) {
      "01" -> "Januari"
      "02" -> "Februari"
      "03" -> "Maret"
      "04" -> "April"
      "05" -> "Mei"
      "06" -> "Juni"
      "07" -> "Juli"
      "08" -> "Agustus"
      "09" -> "September"
      "10" -> "Oktober"
      "11" -> "November"
      "12" -> "Desember"
      else -> parts[1]
    }
    return "$monthName $year"
  }
}
