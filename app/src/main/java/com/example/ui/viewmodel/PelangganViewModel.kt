package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.unwrapFirestoreException
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

class PelangganViewModel(
  private val repository: PamsimasRepository,
  private val connectivityObserver: ConnectivityObserver,
  val currentUserId: String,
  val currentUserEmail: String?,
  val currentUserName: String?
) : ViewModel() {

  val isOnline: StateFlow<Boolean> = connectivityObserver.observeIsOnline()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), connectivityObserver.isCurrentlyOnline())

  private val customerSnapshot: StateFlow<UiState<SnapshotData<Customer?>>> =
    repository.observeMyCustomerProfile(currentUserId, currentUserEmail)
      .map<SnapshotData<Customer?>, UiState<SnapshotData<Customer?>>> { UiState.Success(it) }
      .catch { e -> emit(handleStreamError("profil pelanggan", e)) }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

  private val readingsSnapshot: StateFlow<UiState<SnapshotData<List<MeterReading>>>> =
    repository.observeCustomerReadings(currentUserId, currentUserEmail)
      .map<SnapshotData<List<MeterReading>>, UiState<SnapshotData<List<MeterReading>>>> { UiState.Success(it) }
      .catch { e -> emit(handleStreamError("meteran", e)) }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

  private val billsSnapshot: StateFlow<UiState<SnapshotData<List<Bill>>>> =
    repository.observeCustomerBills(currentUserId, currentUserEmail)
      .map<SnapshotData<List<Bill>>, UiState<SnapshotData<List<Bill>>>> { UiState.Success(it) }
      .catch { e -> emit(handleStreamError("tagihan", e)) }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

  private val paymentsSnapshot: StateFlow<UiState<SnapshotData<List<Payment>>>> =
    repository.observeCustomerPayments(currentUserId, currentUserEmail)
      .map<SnapshotData<List<Payment>>, UiState<SnapshotData<List<Payment>>>> { UiState.Success(it) }
      .catch { e -> emit(handleStreamError("riwayat pembayaran", e)) }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

  val customer: StateFlow<Customer?> = customerSnapshot
    .map { (it as? UiState.Success)?.data?.data }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

  val isProfileLoading: StateFlow<Boolean> = customerSnapshot
    .map { it is UiState.Loading }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), true)

  val readings: StateFlow<List<MeterReading>> = readingsSnapshot
    .map { (it as? UiState.Success)?.data?.data ?: emptyList() }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  val bills: StateFlow<List<Bill>> = billsSnapshot
    .map { (it as? UiState.Success)?.data?.data ?: emptyList() }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  val payments: StateFlow<List<Payment>> = paymentsSnapshot
    .map { (it as? UiState.Success)?.data?.data ?: emptyList() }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  val myRequest: StateFlow<CustomerRequest?> =
    repository.observeMyCustomerRequest(currentUserId)
      .catch { emit(null) }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

  val appConfig: StateFlow<AppConfig> =
    repository.observeAppConfig()
      .map { it ?: AppConfig() }
      .catch { emit(AppConfig()) }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), AppConfig())

  val syncState: StateFlow<SyncMetadataState> = combine(
    isOnline,
    customerSnapshot,
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
      "$baseMessage (tersimpan offline, akan terkirim otomatis saat online)"
    } else {
      baseMessage
    }
  }

  fun claimOrUpdateProfile(customer: Customer, noHp: String, alamat: String) {
    viewModelScope.launch {
      _isBusy.value = true
      try {
        val status = repository.claimOrUpdateMyProfile(
          customer = customer,
          noHp = noHp,
          alamat = alamat,
          allowOfflineQueue = true
        )
        _feedbackMessage.value = formatWriteFeedback("Profil pelanggan berhasil diperbarui", status)
      } catch (e: Exception) {
        _feedbackMessage.value = "Gagal memperbarui profil: ${e.localizedMessage ?: "Terjadi kesalahan"}"
      } finally {
        _isBusy.value = false
      }
    }
  }

  fun submitActivationRequest(
    nama: String,
    noMeter: String,
    alamat: String,
    rtRw: String,
    noHp: String
  ) {
    viewModelScope.launch {
      _isBusy.value = true
      try {
        val status = repository.submitCustomerRequest(
          nama = nama,
          noMeter = noMeter,
          alamat = alamat,
          rtRw = rtRw,
          noHp = noHp,
          allowOfflineQueue = true
        )
        _feedbackMessage.value = formatWriteFeedback("Pengajuan aktivasi dikirim ke Admin PAMSIMAS Weru", status)
      } catch (e: Exception) {
        _feedbackMessage.value = "Gagal mengirim pengajuan: ${e.localizedMessage ?: "Terjadi kesalahan"}"
      } finally {
        _isBusy.value = false
      }
    }
  }

  fun submitTransferProof(
    bill: Bill,
    namaPengirim: String,
    bankPengirim: String,
    catatan: String,
    proofImageBase64: String
  ) {
    viewModelScope.launch {
      _isBusy.value = true
      try {
        val status = repository.submitTransferProof(
          bill = bill,
          namaPengirim = namaPengirim,
          bankPengirim = bankPengirim,
          catatan = catatan,
          proofImageBase64 = proofImageBase64,
          allowOfflineQueue = true
        )
        _feedbackMessage.value = formatWriteFeedback(
          "Bukti transfer berhasil dikirim, menunggu verifikasi Admin",
          status
        )
      } catch (e: Exception) {
        _feedbackMessage.value = "Gagal mengirim bukti transfer: ${e.localizedMessage ?: "Terjadi kesalahan"}"
      } finally {
        _isBusy.value = false
      }
    }
  }

  fun syncNow() {
    viewModelScope.launch {
      if (!isOnline.value) {
        _feedbackMessage.value = "Sedang offline. Perubahan akan tersinkron otomatis saat terhubung internet."
        return@launch
      }
      _isBusy.value = true
      try {
        repository.syncPendingWrites()
        _feedbackMessage.value = "Data berhasil disinkronkan dengan server Firebase."
      } catch (e: Exception) {
        _feedbackMessage.value = "Sinkronisasi tertunda: ${e.localizedMessage ?: "Periksa internet"}"
      } finally {
        _isBusy.value = false
      }
    }
  }

  private fun <T> handleStreamError(stream: String, error: Throwable): UiState<T> {
    val firestoreError = error.unwrapFirestoreException()
    if (firestoreError == null && error is CancellationException) throw error
    val rootError = firestoreError ?: error
    Log.w(TAG, "Error observing $stream", rootError)
    return UiState.Error(rootError.message ?: "Gagal memuat $stream")
  }

  private companion object {
    const val TAG = "PelangganVM"
  }
}
