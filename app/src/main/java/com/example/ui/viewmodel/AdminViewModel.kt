package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.unwrapFirestoreException
import com.example.data.model.AdminUser
import com.example.data.model.AppConfig
import com.example.data.model.Bill
import com.example.data.model.Customer
import com.example.data.model.CustomerRequest
import com.example.data.model.MeterReading
import com.example.data.model.Payment
import com.example.data.model.SnapshotData
import com.example.data.model.SyncMetadataState
import com.example.data.model.WriteResultStatus
import com.example.data.network.ConnectivityObserver
import com.example.data.repository.PamsimasRepository
import com.example.domain.TariffCalculator
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
  data object Loading : UiState<Nothing>
  data class Success<T>(val data: T) : UiState<T>
  data class Error(val message: String) : UiState<Nothing>
}

class AdminViewModel(
  private val repository: PamsimasRepository,
  private val connectivityObserver: ConnectivityObserver,
  private val currentAdminName: String
) : ViewModel() {

  val isOnline: StateFlow<Boolean> = connectivityObserver.observeIsOnline()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), connectivityObserver.isCurrentlyOnline())

  private val customersSnapshot: StateFlow<UiState<SnapshotData<List<Customer>>>> =
    repository.observeAllCustomers()
      .map<SnapshotData<List<Customer>>, UiState<SnapshotData<List<Customer>>>> { UiState.Success(it) }
      .catch { e -> emit(handleUiStreamError("customers", e)) }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

  private val readingsSnapshot: StateFlow<UiState<SnapshotData<List<MeterReading>>>> =
    repository.observeAllReadings()
      .map<SnapshotData<List<MeterReading>>, UiState<SnapshotData<List<MeterReading>>>> { UiState.Success(it) }
      .catch { e -> emit(handleUiStreamError("meter_readings", e)) }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

  private val billsSnapshot: StateFlow<UiState<SnapshotData<List<Bill>>>> =
    repository.observeAllBills()
      .map<SnapshotData<List<Bill>>, UiState<SnapshotData<List<Bill>>>> { UiState.Success(it) }
      .catch { e -> emit(handleUiStreamError("bills", e)) }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

  private val paymentsSnapshot: StateFlow<UiState<SnapshotData<List<Payment>>>> =
    repository.observeAllPayments()
      .map<SnapshotData<List<Payment>>, UiState<SnapshotData<List<Payment>>>> { UiState.Success(it) }
      .catch { e -> emit(handleUiStreamError("payments", e)) }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

  val customers: StateFlow<List<Customer>> = customersSnapshot
    .map { (it as? UiState.Success)?.data?.data ?: emptyList() }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  val readings: StateFlow<List<MeterReading>> = readingsSnapshot
    .map { (it as? UiState.Success)?.data?.data ?: emptyList() }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  val bills: StateFlow<List<Bill>> = billsSnapshot
    .map { (it as? UiState.Success)?.data?.data ?: emptyList() }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  val payments: StateFlow<List<Payment>> = paymentsSnapshot
    .map { (it as? UiState.Success)?.data?.data ?: emptyList() }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  val customerRequests: StateFlow<List<CustomerRequest>> =
    repository.observeAllCustomerRequests()
      .catch { e ->
        Log.w(TAG, "Error observing requests", e)
        emit(emptyList())
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  val admins: StateFlow<List<AdminUser>> =
    repository.observeAdmins()
      .catch { e ->
        Log.w(TAG, "Error observing admins", e)
        emit(emptyList())
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  val appConfig: StateFlow<AppConfig> =
    repository.observeAppConfig()
      .map { it ?: AppConfig() }
      .catch { emit(AppConfig()) }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), AppConfig())

  val isLoading: StateFlow<Boolean> = combine(
    customersSnapshot,
    billsSnapshot
  ) { c, b -> c is UiState.Loading || b is UiState.Loading }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), true)

  val syncState: StateFlow<SyncMetadataState> = combine(
    isOnline,
    customersSnapshot,
    billsSnapshot,
    paymentsSnapshot
  ) { online, c, b, p ->
    val list = listOfNotNull(
      (c as? UiState.Success)?.data,
      (b as? UiState.Success)?.data,
      (p as? UiState.Success)?.data
    )
    SyncMetadataState(
      isOnline = online,
      isFromCache = list.any { it.isFromCache },
      hasPendingWrites = list.any { it.hasPendingWrites }
    )
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), SyncMetadataState())

  private val _feedbackMessage = MutableStateFlow<String?>(null)
  val feedbackMessage: StateFlow<String?> = _feedbackMessage.asStateFlow()

  private val _isBusy = MutableStateFlow(false)
  val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

  fun clearFeedback() {
    _feedbackMessage.value = null
  }

  private fun formatWriteFeedback(baseMessage: String, status: WriteResultStatus): String {
    return if (status == WriteResultStatus.QUEUED_OFFLINE) {
      "$baseMessage (tersimpan offline, akan sinkron otomatis saat online)"
    } else {
      baseMessage
    }
  }

  fun createCustomer(
    nama: String,
    noMeter: String,
    alamat: String,
    rtRw: String,
    noHp: String,
    emailPelanggan: String,
    meterAwal: Long
  ) {
    viewModelScope.launch {
      _isBusy.value = true
      try {
        val (_, status) = repository.createCustomer(
          nama = nama,
          noMeter = noMeter,
          alamat = alamat,
          rtRw = rtRw,
          noHp = noHp,
          emailPelanggan = emailPelanggan,
          meterAwal = meterAwal,
          allowOfflineQueue = true
        )
        _feedbackMessage.value = formatWriteFeedback("Pelanggan \"$nama\" berhasil ditambahkan", status)
      } catch (e: Exception) {
        _feedbackMessage.value = "Gagal menambah pelanggan: ${e.localizedMessage ?: "Terjadi kesalahan"}"
      } finally {
        _isBusy.value = false
      }
    }
  }

  fun updateCustomer(
    customer: Customer,
    nama: String,
    noMeter: String,
    alamat: String,
    rtRw: String,
    noHp: String,
    emailPelanggan: String,
    ownerUid: String,
    aktif: Boolean
  ) {
    viewModelScope.launch {
      _isBusy.value = true
      try {
        val status = repository.updateCustomer(
          customer = customer,
          nama = nama,
          noMeter = noMeter,
          alamat = alamat,
          rtRw = rtRw,
          noHp = noHp,
          emailPelanggan = emailPelanggan,
          ownerUid = ownerUid,
          aktif = aktif,
          allowOfflineQueue = true
        )
        _feedbackMessage.value = formatWriteFeedback("Data pelanggan \"$nama\" diperbarui", status)
      } catch (e: Exception) {
        _feedbackMessage.value = "Gagal memperbarui pelanggan: ${e.localizedMessage ?: "Terjadi kesalahan"}"
      } finally {
        _isBusy.value = false
      }
    }
  }

  fun deleteCustomer(customer: Customer) {
    viewModelScope.launch {
      _isBusy.value = true
      try {
        val status = repository.deleteCustomer(customer.id, allowOfflineQueue = true)
        _feedbackMessage.value = formatWriteFeedback("Pelanggan \"${customer.nama}\" dihapus", status)
      } catch (e: Exception) {
        _feedbackMessage.value = "Gagal menghapus pelanggan: ${e.localizedMessage ?: "Terjadi kesalahan"}"
      } finally {
        _isBusy.value = false
      }
    }
  }

  fun recordMeterAndBill(
    customer: Customer,
    periode: String,
    meterAwal: Long,
    meterAkhir: Long,
    catatan: String
  ) {
    viewModelScope.launch {
      _isBusy.value = true
      try {
        val (_, status) = repository.recordMeterAndCreateBill(
          customer = customer,
          periode = periode,
          meterAwal = meterAwal,
          meterAkhir = meterAkhir,
          catatan = catatan,
          allowOfflineQueue = true
        )
        val calc = TariffCalculator.calculate(meterAwal, meterAkhir)
        _feedbackMessage.value = formatWriteFeedback(
          "Meteran ${customer.nama} (${calc.pemakaian} m³ = ${TariffCalculator.formatRupiah(calc.totalTagihan)}) tersimpan",
          status
        )
      } catch (e: Exception) {
        _feedbackMessage.value = "Gagal mencatat meteran: ${e.localizedMessage ?: "Terjadi kesalahan"}"
      } finally {
        _isBusy.value = false
      }
    }
  }

  fun recordCashPayment(bill: Bill) {
    viewModelScope.launch {
      _isBusy.value = true
      try {
        val (receiptNo, status) = repository.recordCashPayment(
          bill = bill,
          adminName = currentAdminName,
          allowOfflineQueue = true
        )
        _feedbackMessage.value = formatWriteFeedback(
          "Pembayaran tunai lunas • Kuitansi $receiptNo",
          status
        )
      } catch (e: Exception) {
        _feedbackMessage.value = "Gagal mencatat pembayaran tunai: ${e.localizedMessage ?: "Terjadi kesalahan"}"
      } finally {
        _isBusy.value = false
      }
    }
  }

  fun verifyTransferPayment(bill: Bill, pendingPayment: Payment?) {
    viewModelScope.launch {
      _isBusy.value = true
      try {
        val (receiptNo, status) = repository.verifyTransferPayment(
          bill = bill,
          pendingPayment = pendingPayment,
          adminName = currentAdminName,
          allowOfflineQueue = true
        )
        _feedbackMessage.value = formatWriteFeedback(
          "Transfer diverifikasi Lunas • Kuitansi $receiptNo",
          status
        )
      } catch (e: Exception) {
        _feedbackMessage.value = "Gagal memverifikasi transfer: ${e.localizedMessage ?: "Terjadi kesalahan"}"
      } finally {
        _isBusy.value = false
      }
    }
  }

  fun rejectTransferPayment(bill: Bill, pendingPayment: Payment?, reason: String) {
    viewModelScope.launch {
      _isBusy.value = true
      try {
        val status = repository.rejectTransferPayment(
          bill = bill,
          pendingPayment = pendingPayment,
          reason = reason,
          adminName = currentAdminName,
          allowOfflineQueue = true
        )
        _feedbackMessage.value = formatWriteFeedback("Bukti transfer ditolak", status)
      } catch (e: Exception) {
        _feedbackMessage.value = "Gagal menolak transfer: ${e.localizedMessage ?: "Terjadi kesalahan"}"
      } finally {
        _isBusy.value = false
      }
    }
  }

  fun approveRequest(request: CustomerRequest, existingCustomer: Customer?) {
    viewModelScope.launch {
      _isBusy.value = true
      try {
        val status = repository.approveRequestAndLinkCustomer(
          request = request,
          existingCustomer = existingCustomer,
          allowOfflineQueue = true
        )
        _feedbackMessage.value = formatWriteFeedback(
          "Akun ${request.nama} berhasil ditautkan sebagai Pelanggan",
          status
        )
      } catch (e: Exception) {
        _feedbackMessage.value = "Gagal menyetujui pengajuan: ${e.localizedMessage ?: "Terjadi kesalahan"}"
      } finally {
        _isBusy.value = false
      }
    }
  }

  fun rejectRequest(request: CustomerRequest, reason: String) {
    viewModelScope.launch {
      _isBusy.value = true
      try {
        val status = repository.rejectCustomerRequest(request, reason, allowOfflineQueue = true)
        _feedbackMessage.value = formatWriteFeedback("Pengajuan ditolak", status)
      } catch (e: Exception) {
        _feedbackMessage.value = "Gagal menolak pengajuan: ${e.localizedMessage ?: "Terjadi kesalahan"}"
      } finally {
        _isBusy.value = false
      }
    }
  }

  fun saveAppConfig(
    namaBank: String,
    nomorRekening: String,
    atasNama: String,
    kontakAdmin: String,
    catatanPembayaran: String
  ) {
    viewModelScope.launch {
      _isBusy.value = true
      try {
        val status = repository.saveAppConfig(
          namaBank = namaBank,
          nomorRekening = nomorRekening,
          atasNama = atasNama,
          kontakAdmin = kontakAdmin,
          catatanPembayaran = catatanPembayaran,
          allowOfflineQueue = true
        )
        _feedbackMessage.value = formatWriteFeedback("Pengaturan rekening PAMSIMAS disimpan", status)
      } catch (e: Exception) {
        _feedbackMessage.value = "Gagal menyimpan pengaturan: ${e.localizedMessage ?: "Terjadi kesalahan"}"
      } finally {
        _isBusy.value = false
      }
    }
  }

  fun addAdmin(uid: String, email: String, nama: String) {
    viewModelScope.launch {
      _isBusy.value = true
      try {
        repository.addAdmin(uid, email, nama)
        _feedbackMessage.value = "Admin baru \"$nama\" berhasil ditambahkan"
      } catch (e: Exception) {
        _feedbackMessage.value = "Gagal menambah admin: ${e.localizedMessage ?: "Terjadi kesalahan"}"
      } finally {
        _isBusy.value = false
      }
    }
  }

  fun syncNow() {
    viewModelScope.launch {
      if (!isOnline.value) {
        _feedbackMessage.value = "Perangkat sedang offline. Sinkronisasi berjalan otomatis saat koneksi kembali."
        return@launch
      }
      _isBusy.value = true
      try {
        repository.syncPendingWrites()
        _feedbackMessage.value = "Semua data telah tersinkron dengan server Firebase."
      } catch (e: Exception) {
        _feedbackMessage.value = "Sinkronisasi tertunda: ${e.localizedMessage ?: "Periksa koneksi internet"}"
      } finally {
        _isBusy.value = false
      }
    }
  }

  private fun <T> handleUiStreamError(stream: String, error: Throwable): UiState<T> {
    val firestoreError = error.unwrapFirestoreException()
    if (firestoreError == null && error is CancellationException) throw error
    val rootError = firestoreError ?: error
    Log.w(TAG, "Error observing $stream", rootError)
    return UiState.Error(rootError.message ?: "Gagal memuat $stream")
  }

  private companion object {
    const val TAG = "AdminVM"
  }
}
