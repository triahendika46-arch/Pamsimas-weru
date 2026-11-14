package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class UserRole {
  ADMIN,
  PELANGGAN
}

enum class WriteResultStatus {
  SYNCED_ONLINE,
  QUEUED_OFFLINE
}

data class SyncMetadataState(
  val isOnline: Boolean = true,
  val isFromCache: Boolean = false,
  val hasPendingWrites: Boolean = false
)

data class SnapshotData<T>(
  val data: T,
  val isFromCache: Boolean = false,
  val hasPendingWrites: Boolean = false
)

@IgnoreExtraProperties
data class AdminUser(
  @DocumentId val id: String = "",
  val uid: String = "",
  val email: String = "",
  val nama: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Customer(
  @DocumentId val id: String = "",
  val nama: String = "",
  val noMeter: String = "",
  val alamat: String = "",
  val rtRw: String = "",
  val noHp: String = "",
  val emailPelanggan: String = "",
  val ownerUid: String = "",
  val aktif: Boolean = true,
  val meterTerakhir: Long = 0L,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CustomerRequest(
  @DocumentId val id: String = "",
  val uid: String = "",
  val email: String = "",
  val nama: String = "",
  val noMeter: String = "",
  val alamat: String = "",
  val rtRw: String = "",
  val noHp: String = "",
  val status: String = "menunggu",
  val catatanAdmin: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class MeterReading(
  @DocumentId val id: String = "",
  val customerId: String = "",
  val customerNama: String = "",
  val noMeter: String = "",
  val customerOwnerUid: String = "",
  val customerEmail: String = "",
  val periode: String = "",
  val meterAwal: Long = 0L,
  val meterAkhir: Long = 0L,
  val pemakaian: Long = 0L,
  val catatan: String = "",
  val dicatatOlehUid: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Bill(
  @DocumentId val id: String = "",
  val customerId: String = "",
  val customerNama: String = "",
  val noMeter: String = "",
  val rtRw: String = "",
  val customerOwnerUid: String = "",
  val customerEmail: String = "",
  val readingId: String = "",
  val periode: String = "",
  val meterAwal: Long = 0L,
  val meterAkhir: Long = 0L,
  val pemakaian: Long = 0L,
  val tarifPerM3: Long = 2000L,
  val biayaPemakaian: Long = 0L,
  val biayaBeban: Long = 0L,
  val totalTagihan: Long = 0L,
  val status: String = "belum_bayar",
  val metodePembayaran: String = "",
  val namaPengirimTransfer: String = "",
  val bankPengirim: String = "",
  val catatanTransfer: String = "",
  val proofImageBase64: String = "",
  val alasanPenolakan: String = "",
  val nomorKuitansi: String = "",
  @ServerTimestamp val dibayarPada: Timestamp? = null,
  val diverifikasiOleh: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Payment(
  @DocumentId val id: String = "",
  val billId: String = "",
  val customerId: String = "",
  val customerNama: String = "",
  val noMeter: String = "",
  val customerOwnerUid: String = "",
  val customerEmail: String = "",
  val periode: String = "",
  val pemakaian: Long = 0L,
  val meterAwal: Long = 0L,
  val meterAkhir: Long = 0L,
  val biayaPemakaian: Long = 0L,
  val biayaBeban: Long = 0L,
  val jumlahBayar: Long = 0L,
  val metode: String = "tunai",
  val status: String = "lunas",
  val nomorKuitansi: String = "",
  val namaPengirim: String = "",
  val bankPengirim: String = "",
  val catatan: String = "",
  val proofImageBase64: String = "",
  val diverifikasiOleh: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class AppConfig(
  @DocumentId val id: String = "pamsimas",
  val namaBank: String = "Bank BRI",
  val nomorRekening: String = "Belum diatur oleh Admin",
  val atasNama: String = "PAMSIMAS WERU (Hendika WP)",
  val kontakAdmin: String = "-",
  val catatanPembayaran: String = "Sertakan nama pelanggan dan nomor meteran pada berita transfer.",
  @ServerTimestamp val updatedAt: Timestamp? = null
)
