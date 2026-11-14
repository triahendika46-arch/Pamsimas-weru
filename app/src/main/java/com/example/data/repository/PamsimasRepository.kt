package com.example.data.repository

import android.content.Context
import com.example.data.firebase.FirestoreClientProvider
import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.data.firebase.unwrapFirestoreException
import com.example.data.model.AdminUser
import com.example.data.model.AppConfig
import com.example.data.model.Bill
import com.example.data.model.Customer
import com.example.data.model.CustomerRequest
import com.example.data.model.MeterReading
import com.example.data.model.Payment
import com.example.data.model.SnapshotData
import com.example.data.model.WriteResultStatus
import com.example.domain.TariffCalculator
import com.google.android.gms.tasks.Task
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

class PamsimasRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = Firebase.auth
) {
  constructor(context: Context) : this(
    db = FirestoreClientProvider.getDatabase(context),
    auth = Firebase.auth
  )

  private fun requireUserId(): String {
    return auth.currentUser?.uid
      ?: throw IllegalStateException("Pengguna harus masuk dengan Google terlebih dahulu.")
  }

  private fun sanitizeDocIdSegment(raw: String): String {
    return raw.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
      .trim('_')
      .ifBlank { "id" }
      .take(40)
  }

  private suspend fun <T> executeWrite(
    operationType: OperationType,
    path: String,
    allowOfflineQueue: Boolean,
    taskProvider: () -> Task<T>
  ): WriteResultStatus {
    return try {
      val task = taskProvider()
      if (allowOfflineQueue) {
        val completed = withTimeoutOrNull(OFFLINE_WRITE_WAIT_MS) {
          task.await()
          true
        }
        if (completed == true) WriteResultStatus.SYNCED_ONLINE else WriteResultStatus.QUEUED_OFFLINE
      } else {
        task.await()
        WriteResultStatus.SYNCED_ONLINE
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, operationType, path)
      throw e
    }
  }

  suspend fun isCurrentUserAdmin(): Boolean {
    val user = auth.currentUser ?: return false
    val email = user.email?.lowercase()?.trim()
    if (user.isEmailVerified && email == BOOTSTRAP_ADMIN_EMAIL) {
      return true
    }
    return try {
      val snap = db.collection("admins").document(user.uid).get().await()
      snap.exists()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, "admins/${user.uid}")
      false
    }
  }

  fun observeAdmins(): Flow<List<AdminUser>> = flow {
    requireUserId()
    val path = "admins"
    emitAll(
      db.collection(path)
        .snapshots(MetadataChanges.INCLUDE)
        .map { snap ->
          snap.toObjects(AdminUser::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedBy { it.nama.lowercase() }
        }
        .catch { error ->
          handleStreamError(error, OperationType.LIST, path)
          throw error
        }
    )
  }

  suspend fun addAdmin(uid: String, email: String, nama: String): WriteResultStatus {
    requireUserId()
    val cleanUid = uid.trim()
    require(cleanUid.isNotBlank() && cleanUid.matches(Regex("^[a-zA-Z0-9_\\-]+$"))) {
      "UID Google tidak valid"
    }
    val docRef = db.collection("admins").document(cleanUid)
    val payload = mapOf(
      "uid" to cleanUid,
      "email" to email.trim().lowercase(),
      "nama" to nama.trim(),
      "createdAt" to FieldValue.serverTimestamp()
    )
    return executeWrite(OperationType.CREATE, docRef.path, allowOfflineQueue = false) {
      docRef.set(payload)
    }
  }

  // --- CUSTOMERS ---

  fun observeAllCustomers(): Flow<SnapshotData<List<Customer>>> = flow {
    requireUserId()
    val path = "customers"
    emitAll(
      db.collection(path)
        .snapshots(MetadataChanges.INCLUDE)
        .map { snap ->
          SnapshotData(
            data = snap.toObjects(
              Customer::class.java,
              DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ).sortedBy { it.noMeter.lowercase() },
            isFromCache = snap.metadata.isFromCache,
            hasPendingWrites = snap.metadata.hasPendingWrites()
          )
        }
        .catch { error ->
          handleStreamError(error, OperationType.LIST, path)
          throw error
        }
    )
  }

  fun observeMyCustomerProfile(uid: String, verifiedEmail: String?): Flow<SnapshotData<Customer?>> = flow {
    requireUserId()
    val byUidFlow = db.collection("customers")
      .whereEqualTo("ownerUid", uid)
      .snapshots(MetadataChanges.INCLUDE)
      .map { snap ->
        SnapshotData(
          data = snap.toObjects(
            Customer::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
          ).firstOrNull(),
          isFromCache = snap.metadata.isFromCache,
          hasPendingWrites = snap.metadata.hasPendingWrites()
        )
      }
      .catch { error ->
        handleStreamError(error, OperationType.LIST, "customers?ownerUid=$uid")
        throw error
      }

    val normalizedEmail = verifiedEmail?.trim()?.lowercase()?.takeIf { it.isNotBlank() }
    if (normalizedEmail != null && auth.currentUser?.isEmailVerified == true) {
      val byEmailFlow = db.collection("customers")
        .whereEqualTo("emailPelanggan", normalizedEmail)
        .snapshots(MetadataChanges.INCLUDE)
        .map { snap ->
          SnapshotData(
            data = snap.toObjects(
              Customer::class.java,
              DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ).firstOrNull(),
            isFromCache = snap.metadata.isFromCache,
            hasPendingWrites = snap.metadata.hasPendingWrites()
          )
        }
        .catch { error ->
          handleStreamError(error, OperationType.LIST, "customers?emailPelanggan=$normalizedEmail")
          throw error
        }

      emitAll(
        combine(byUidFlow, byEmailFlow) { uidRes, emailRes ->
          val chosen = uidRes.data ?: emailRes.data
          SnapshotData(
            data = chosen,
            isFromCache = uidRes.isFromCache || emailRes.isFromCache,
            hasPendingWrites = uidRes.hasPendingWrites || emailRes.hasPendingWrites
          )
        }
      )
    } else {
      emitAll(byUidFlow)
    }
  }

  suspend fun createCustomer(
    nama: String,
    noMeter: String,
    alamat: String,
    rtRw: String,
    noHp: String,
    emailPelanggan: String,
    ownerUid: String = "",
    meterAwal: Long = 0L,
    allowOfflineQueue: Boolean = false
  ): Pair<String, WriteResultStatus> {
    requireUserId()
    val docRef = db.collection("customers").document()
    val payload = mapOf(
      "nama" to nama.trim(),
      "noMeter" to noMeter.trim().uppercase(),
      "alamat" to alamat.trim(),
      "rtRw" to rtRw.trim(),
      "noHp" to noHp.trim(),
      "emailPelanggan" to emailPelanggan.trim().lowercase(),
      "ownerUid" to ownerUid.trim(),
      "aktif" to true,
      "meterTerakhir" to meterAwal.coerceAtLeast(0L),
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp()
    )
    val status = executeWrite(OperationType.CREATE, docRef.path, allowOfflineQueue) {
      docRef.set(payload)
    }
    return docRef.id to status
  }

  suspend fun updateCustomer(
    customer: Customer,
    nama: String,
    noMeter: String,
    alamat: String,
    rtRw: String,
    noHp: String,
    emailPelanggan: String,
    ownerUid: String,
    aktif: Boolean,
    allowOfflineQueue: Boolean = false
  ): WriteResultStatus {
    requireUserId()
    require(customer.id.isNotBlank()) { "ID pelanggan kosong" }
    val docRef = db.collection("customers").document(customer.id)
    val updates = mapOf(
      "nama" to nama.trim(),
      "noMeter" to noMeter.trim().uppercase(),
      "alamat" to alamat.trim(),
      "rtRw" to rtRw.trim(),
      "noHp" to noHp.trim(),
      "emailPelanggan" to emailPelanggan.trim().lowercase(),
      "ownerUid" to ownerUid.trim(),
      "aktif" to aktif,
      "updatedAt" to FieldValue.serverTimestamp()
    )
    return executeWrite(OperationType.UPDATE, docRef.path, allowOfflineQueue) {
      docRef.update(updates)
    }
  }

  private suspend fun fetchCustomerDocRefs(
    collectionPath: String,
    customerId: String,
    isOnline: Boolean
  ): List<DocumentReference> {
    val query = db.collection(collectionPath).whereEqualTo("customerId", customerId)
    val snap = if (!isOnline) {
      try {
        query.get(Source.CACHE).await()
      } catch (e: CancellationException) {
        throw e
      } catch (_: Exception) {
        null
      }
    } else {
      try {
        withTimeoutOrNull(3000L) { query.get().await() } ?: query.get(Source.CACHE).await()
      } catch (e: CancellationException) {
        throw e
      } catch (_: Exception) {
        try {
          query.get(Source.CACHE).await()
        } catch (e2: CancellationException) {
          throw e2
        } catch (_: Exception) {
          null
        }
      }
    }
    return snap?.documents?.map { it.reference }.orEmpty()
  }

  suspend fun deleteCustomer(
    customerId: String,
    ownerUid: String = "",
    isOnline: Boolean = true,
    allowOfflineQueue: Boolean = false
  ): WriteResultStatus {
    requireUserId()
    require(customerId.isNotBlank()) { "ID pelanggan kosong" }

    val refsToDelete = LinkedHashSet<DocumentReference>()
    refsToDelete.addAll(fetchCustomerDocRefs("meter_readings", customerId, isOnline))
    refsToDelete.addAll(fetchCustomerDocRefs("bills", customerId, isOnline))
    refsToDelete.addAll(fetchCustomerDocRefs("payments", customerId, isOnline))

    val cleanOwnerUid = ownerUid.trim()
    if (cleanOwnerUid.isNotBlank() && cleanOwnerUid.matches(Regex("^[a-zA-Z0-9_\\-]+$"))) {
      refsToDelete.add(db.collection("customer_requests").document(cleanOwnerUid))
    }

    val customerDocRef = db.collection("customers").document(customerId)
    refsToDelete.add(customerDocRef)

    val effectiveOfflineQueue = allowOfflineQueue || !isOnline
    var finalStatus = WriteResultStatus.SYNCED_ONLINE
    for (chunk in refsToDelete.toList().chunked(450)) {
      val batch = db.batch()
      chunk.forEach { ref -> batch.delete(ref) }
      val status = executeWrite(OperationType.DELETE, customerDocRef.path, effectiveOfflineQueue) {
        batch.commit()
      }
      if (status == WriteResultStatus.QUEUED_OFFLINE) {
        finalStatus = WriteResultStatus.QUEUED_OFFLINE
      }
    }
    return finalStatus
  }

  suspend fun claimOrUpdateMyProfile(
    customer: Customer,
    noHp: String,
    alamat: String,
    allowOfflineQueue: Boolean = false
  ): WriteResultStatus {
    val uid = requireUserId()
    require(customer.id.isNotBlank()) { "ID pelanggan kosong" }
    val docRef = db.collection("customers").document(customer.id)
    val updates = mutableMapOf<String, Any>(
      "noHp" to noHp.trim(),
      "alamat" to alamat.trim(),
      "updatedAt" to FieldValue.serverTimestamp()
    )
    if (customer.ownerUid.isBlank()) {
      updates["ownerUid"] = uid
    }
    return executeWrite(OperationType.UPDATE, docRef.path, allowOfflineQueue) {
      docRef.update(updates)
    }
  }

  // --- CUSTOMER ACTIVATION REQUESTS ---

  fun observeAllCustomerRequests(): Flow<List<CustomerRequest>> = flow {
    requireUserId()
    val path = "customer_requests"
    emitAll(
      db.collection(path)
        .snapshots(MetadataChanges.INCLUDE)
        .map { snap ->
          snap.toObjects(
            CustomerRequest::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
          ).sortedByDescending { it.createdAt?.seconds ?: 0L }
        }
        .catch { error ->
          handleStreamError(error, OperationType.LIST, path)
          throw error
        }
    )
  }

  fun observeMyCustomerRequest(uid: String): Flow<CustomerRequest?> = flow {
    requireUserId()
    val path = "customer_requests/$uid"
    emitAll(
      db.collection("customer_requests")
        .whereEqualTo("uid", uid)
        .snapshots(MetadataChanges.INCLUDE)
        .map { snap ->
          snap.toObjects(
            CustomerRequest::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
          ).firstOrNull()
        }
        .catch { error ->
          handleStreamError(error, OperationType.LIST, path)
          throw error
        }
    )
  }

  suspend fun submitCustomerRequest(
    nama: String,
    noMeter: String,
    alamat: String,
    rtRw: String,
    noHp: String,
    allowOfflineQueue: Boolean = false
  ): WriteResultStatus {
    val uid = requireUserId()
    val email = auth.currentUser?.email?.trim()?.lowercase() ?: ""
    val docRef = db.collection("customer_requests").document(uid)
    val existing = try {
      docRef.get().await()
    } catch (_: Exception) {
      null
    }
    if (existing != null && existing.exists()) {
      val updates = mapOf(
        "nama" to nama.trim(),
        "noMeter" to noMeter.trim().uppercase(),
        "alamat" to alamat.trim(),
        "rtRw" to rtRw.trim(),
        "noHp" to noHp.trim(),
        "status" to "menunggu",
        "updatedAt" to FieldValue.serverTimestamp()
      )
      return executeWrite(OperationType.UPDATE, docRef.path, allowOfflineQueue) {
        docRef.update(updates)
      }
    } else {
      val payload = mapOf(
        "uid" to uid,
        "email" to email,
        "nama" to nama.trim(),
        "noMeter" to noMeter.trim().uppercase(),
        "alamat" to alamat.trim(),
        "rtRw" to rtRw.trim(),
        "noHp" to noHp.trim(),
        "status" to "menunggu",
        "catatanAdmin" to "",
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
      )
      return executeWrite(OperationType.CREATE, docRef.path, allowOfflineQueue) {
        docRef.set(payload)
      }
    }
  }

  suspend fun approveRequestAndLinkCustomer(
    request: CustomerRequest,
    existingCustomer: Customer?,
    allowOfflineQueue: Boolean = false
  ): WriteResultStatus {
    requireUserId()
    val batch = db.batch()
    val reqRef = db.collection("customer_requests").document(request.uid)
    batch.update(
      reqRef,
      mapOf(
        "status" to "disetujui",
        "catatanAdmin" to "Akun berhasil ditautkan",
        "updatedAt" to FieldValue.serverTimestamp()
      )
    )
    if (existingCustomer != null) {
      val custRef = db.collection("customers").document(existingCustomer.id)
      batch.update(
        custRef,
        mapOf(
          "ownerUid" to request.uid,
          "emailPelanggan" to request.email.trim().lowercase(),
          "updatedAt" to FieldValue.serverTimestamp()
        )
      )
    } else {
      val custRef = db.collection("customers").document()
      val meterCode = request.noMeter.trim().uppercase().ifBlank {
        "WR-${(System.currentTimeMillis() % 10000).toString().padStart(4, '0')}"
      }
      batch.set(
        custRef,
        mapOf(
          "nama" to request.nama.trim(),
          "noMeter" to meterCode,
          "alamat" to request.alamat.trim(),
          "rtRw" to request.rtRw.trim(),
          "noHp" to request.noHp.trim(),
          "emailPelanggan" to request.email.trim().lowercase(),
          "ownerUid" to request.uid,
          "aktif" to true,
          "meterTerakhir" to 0L,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp()
        )
      )
    }
    return executeWrite(OperationType.WRITE, "customer_requests/${request.uid}", allowOfflineQueue) {
      batch.commit()
    }
  }

  suspend fun rejectCustomerRequest(
    request: CustomerRequest,
    reason: String,
    allowOfflineQueue: Boolean = false
  ): WriteResultStatus {
    requireUserId()
    val reqRef = db.collection("customer_requests").document(request.uid)
    return executeWrite(OperationType.UPDATE, reqRef.path, allowOfflineQueue) {
      reqRef.update(
        mapOf(
          "status" to "ditolak",
          "catatanAdmin" to reason.trim(),
          "updatedAt" to FieldValue.serverTimestamp()
        )
      )
    }
  }

  // --- METER READINGS & BILLS ---

  fun observeAllReadings(): Flow<SnapshotData<List<MeterReading>>> = flow {
    requireUserId()
    val path = "meter_readings"
    emitAll(
      db.collection(path)
        .snapshots(MetadataChanges.INCLUDE)
        .map { snap ->
          SnapshotData(
            data = snap.toObjects(
              MeterReading::class.java,
              DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ).sortedWith(compareByDescending<MeterReading> { it.periode }.thenBy { it.noMeter }),
            isFromCache = snap.metadata.isFromCache,
            hasPendingWrites = snap.metadata.hasPendingWrites()
          )
        }
        .catch { error ->
          handleStreamError(error, OperationType.LIST, path)
          throw error
        }
    )
  }

  fun observeCustomerReadings(
    uid: String,
    verifiedEmail: String?
  ): Flow<SnapshotData<List<MeterReading>>> = observeCustomerScopedList(
    collectionPath = "meter_readings",
    uid = uid,
    verifiedEmail = verifiedEmail,
    clazz = MeterReading::class.java,
    sorter = { list -> list.sortedByDescending { it.periode } }
  )

  suspend fun recordMeterAndCreateBill(
    customer: Customer,
    periode: String,
    meterAwal: Long,
    meterAkhir: Long,
    catatan: String,
    allowOfflineQueue: Boolean = false
  ): Pair<String, WriteResultStatus> {
    val adminUid = requireUserId()
    require(customer.id.isNotBlank()) { "Pelanggan harus dipilih" }
    require(periode.matches(Regex("^\\d{4}-\\d{2}$"))) { "Format periode harus YYYY-MM" }
    val calc = TariffCalculator.calculate(meterAwal, meterAkhir)

    val docId = "${sanitizeDocIdSegment(customer.id)}_${sanitizeDocIdSegment(periode)}"
    val readingRef = db.collection("meter_readings").document(docId)
    val billRef = db.collection("bills").document(docId)
    val customerRef = db.collection("customers").document(customer.id)

    val existingBillSnap = try {
      billRef.get().await()
    } catch (_: Exception) {
      null
    }
    if (existingBillSnap != null && existingBillSnap.exists()) {
      val status = existingBillSnap.getString("status")
      require(status != "lunas") {
        "Tagihan periode ${TariffCalculator.formatPeriode(periode)} untuk ${customer.nama} sudah lunas dan tidak dapat diubah."
      }
    }

    val batch = db.batch()
    val existingReadingSnap = try {
      readingRef.get().await()
    } catch (_: Exception) {
      null
    }

    if (existingReadingSnap != null && existingReadingSnap.exists()) {
      batch.update(
        readingRef,
        mapOf(
          "customerNama" to customer.nama,
          "noMeter" to customer.noMeter,
          "customerOwnerUid" to customer.ownerUid,
          "customerEmail" to customer.emailPelanggan.lowercase(),
          "periode" to periode,
          "meterAwal" to calc.meterAwal,
          "meterAkhir" to calc.meterAkhir,
          "pemakaian" to calc.pemakaian,
          "catatan" to catatan.trim(),
          "dicatatOlehUid" to adminUid,
          "updatedAt" to FieldValue.serverTimestamp()
        )
      )
    } else {
      batch.set(
        readingRef,
        mapOf(
          "customerId" to customer.id,
          "customerNama" to customer.nama,
          "noMeter" to customer.noMeter,
          "customerOwnerUid" to customer.ownerUid,
          "customerEmail" to customer.emailPelanggan.lowercase(),
          "periode" to periode,
          "meterAwal" to calc.meterAwal,
          "meterAkhir" to calc.meterAkhir,
          "pemakaian" to calc.pemakaian,
          "catatan" to catatan.trim(),
          "dicatatOlehUid" to adminUid,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp()
        )
      )
    }

    if (existingBillSnap != null && existingBillSnap.exists()) {
      batch.update(
        billRef,
        mapOf(
          "customerNama" to customer.nama,
          "noMeter" to customer.noMeter,
          "rtRw" to customer.rtRw,
          "customerOwnerUid" to customer.ownerUid,
          "customerEmail" to customer.emailPelanggan.lowercase(),
          "meterAwal" to calc.meterAwal,
          "meterAkhir" to calc.meterAkhir,
          "pemakaian" to calc.pemakaian,
          "tarifPerM3" to calc.tarifPerM3,
          "biayaPemakaian" to calc.biayaPemakaian,
          "biayaBeban" to calc.biayaBeban,
          "totalTagihan" to calc.totalTagihan,
          "updatedAt" to FieldValue.serverTimestamp()
        )
      )
    } else {
      batch.set(
        billRef,
        mapOf(
          "customerId" to customer.id,
          "customerNama" to customer.nama,
          "noMeter" to customer.noMeter,
          "rtRw" to customer.rtRw,
          "customerOwnerUid" to customer.ownerUid,
          "customerEmail" to customer.emailPelanggan.lowercase(),
          "readingId" to docId,
          "periode" to periode,
          "meterAwal" to calc.meterAwal,
          "meterAkhir" to calc.meterAkhir,
          "pemakaian" to calc.pemakaian,
          "tarifPerM3" to calc.tarifPerM3,
          "biayaPemakaian" to calc.biayaPemakaian,
          "biayaBeban" to calc.biayaBeban,
          "totalTagihan" to calc.totalTagihan,
          "status" to "belum_bayar",
          "metodePembayaran" to "",
          "namaPengirimTransfer" to "",
          "bankPengirim" to "",
          "catatanTransfer" to "",
          "proofImageBase64" to "",
          "alasanPenolakan" to "",
          "nomorKuitansi" to "",
          "diverifikasiOleh" to "",
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp()
        )
      )
    }

    batch.update(
      customerRef,
      mapOf(
        "meterTerakhir" to calc.meterAkhir,
        "updatedAt" to FieldValue.serverTimestamp()
      )
    )

    val writeStatus = executeWrite(OperationType.WRITE, billRef.path, allowOfflineQueue) {
      batch.commit()
    }
    return docId to writeStatus
  }

  // --- BILLS & PAYMENTS ---

  fun observeAllBills(): Flow<SnapshotData<List<Bill>>> = flow {
    requireUserId()
    val path = "bills"
    emitAll(
      db.collection(path)
        .snapshots(MetadataChanges.INCLUDE)
        .map { snap ->
          SnapshotData(
            data = snap.toObjects(
              Bill::class.java,
              DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ).sortedWith(compareByDescending<Bill> { it.periode }.thenBy { it.customerNama.lowercase() }),
            isFromCache = snap.metadata.isFromCache,
            hasPendingWrites = snap.metadata.hasPendingWrites()
          )
        }
        .catch { error ->
          handleStreamError(error, OperationType.LIST, path)
          throw error
        }
    )
  }

  fun observeCustomerBills(
    uid: String,
    verifiedEmail: String?
  ): Flow<SnapshotData<List<Bill>>> = observeCustomerScopedList(
    collectionPath = "bills",
    uid = uid,
    verifiedEmail = verifiedEmail,
    clazz = Bill::class.java,
    sorter = { list -> list.sortedByDescending { it.periode } }
  )

  fun observeAllPayments(): Flow<SnapshotData<List<Payment>>> = flow {
    requireUserId()
    val path = "payments"
    emitAll(
      db.collection(path)
        .snapshots(MetadataChanges.INCLUDE)
        .map { snap ->
          SnapshotData(
            data = snap.toObjects(
              Payment::class.java,
              DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ).sortedByDescending { it.updatedAt?.seconds ?: it.createdAt?.seconds ?: 0L },
            isFromCache = snap.metadata.isFromCache,
            hasPendingWrites = snap.metadata.hasPendingWrites()
          )
        }
        .catch { error ->
          handleStreamError(error, OperationType.LIST, path)
          throw error
        }
    )
  }

  fun observeCustomerPayments(
    uid: String,
    verifiedEmail: String?
  ): Flow<SnapshotData<List<Payment>>> = observeCustomerScopedList(
    collectionPath = "payments",
    uid = uid,
    verifiedEmail = verifiedEmail,
    clazz = Payment::class.java,
    sorter = { list -> list.sortedByDescending { it.updatedAt?.seconds ?: it.createdAt?.seconds ?: 0L } }
  )

  suspend fun submitTransferProof(
    bill: Bill,
    namaPengirim: String,
    bankPengirim: String,
    catatan: String,
    proofImageBase64: String,
    allowOfflineQueue: Boolean = false
  ): WriteResultStatus {
    requireUserId()
    require(bill.id.isNotBlank()) { "Tagihan tidak valid" }
    require(bill.status != "lunas") { "Tagihan ini sudah lunas" }
    require(proofImageBase64.length <= 350000) { "Ukuran gambar terlalu besar" }

    val billRef = db.collection("bills").document(bill.id)
    val paymentRef = db.collection("payments").document()
    val batch = db.batch()

    batch.update(
      billRef,
      mapOf(
        "status" to "menunggu_verifikasi",
        "metodePembayaran" to "transfer",
        "namaPengirimTransfer" to namaPengirim.trim(),
        "bankPengirim" to bankPengirim.trim(),
        "catatanTransfer" to catatan.trim(),
        "proofImageBase64" to proofImageBase64,
        "alasanPenolakan" to "",
        "updatedAt" to FieldValue.serverTimestamp()
      )
    )

    batch.set(
      paymentRef,
      mapOf(
        "billId" to bill.id,
        "customerId" to bill.customerId,
        "customerNama" to bill.customerNama,
        "noMeter" to bill.noMeter,
        "customerOwnerUid" to bill.customerOwnerUid,
        "customerEmail" to bill.customerEmail,
        "periode" to bill.periode,
        "pemakaian" to bill.pemakaian,
        "meterAwal" to bill.meterAwal,
        "meterAkhir" to bill.meterAkhir,
        "biayaPemakaian" to bill.biayaPemakaian,
        "biayaBeban" to bill.biayaBeban,
        "jumlahBayar" to bill.totalTagihan,
        "metode" to "transfer",
        "status" to "menunggu_verifikasi",
        "nomorKuitansi" to "",
        "namaPengirim" to namaPengirim.trim(),
        "bankPengirim" to bankPengirim.trim(),
        "catatan" to catatan.trim(),
        "proofImageBase64" to proofImageBase64,
        "diverifikasiOleh" to "",
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
      )
    )

    return executeWrite(OperationType.WRITE, billRef.path, allowOfflineQueue) {
      batch.commit()
    }
  }

  suspend fun recordCashPayment(
    bill: Bill,
    adminName: String,
    catatan: String = "Pembayaran tunai di loket PAMSIMAS Weru",
    allowOfflineQueue: Boolean = false
  ): Pair<String, WriteResultStatus> {
    requireUserId()
    require(bill.id.isNotBlank()) { "Tagihan tidak valid" }
    val receiptNo = generateReceiptNumber(bill)
    val billRef = db.collection("bills").document(bill.id)
    val paymentRef = db.collection("payments").document()
    val batch = db.batch()

    batch.update(
      billRef,
      mapOf(
        "status" to "lunas",
        "metodePembayaran" to "tunai",
        "nomorKuitansi" to receiptNo,
        "alasanPenolakan" to "",
        "dibayarPada" to FieldValue.serverTimestamp(),
        "diverifikasiOleh" to adminName.trim().ifBlank { "Admin PAMSIMAS Weru" },
        "updatedAt" to FieldValue.serverTimestamp()
      )
    )

    batch.set(
      paymentRef,
      mapOf(
        "billId" to bill.id,
        "customerId" to bill.customerId,
        "customerNama" to bill.customerNama,
        "noMeter" to bill.noMeter,
        "customerOwnerUid" to bill.customerOwnerUid,
        "customerEmail" to bill.customerEmail,
        "periode" to bill.periode,
        "pemakaian" to bill.pemakaian,
        "meterAwal" to bill.meterAwal,
        "meterAkhir" to bill.meterAkhir,
        "biayaPemakaian" to bill.biayaPemakaian,
        "biayaBeban" to bill.biayaBeban,
        "jumlahBayar" to bill.totalTagihan,
        "metode" to "tunai",
        "status" to "lunas",
        "nomorKuitansi" to receiptNo,
        "namaPengirim" to bill.customerNama,
        "bankPengirim" to "Tunai",
        "catatan" to catatan.trim(),
        "proofImageBase64" to "",
        "diverifikasiOleh" to adminName.trim().ifBlank { "Admin PAMSIMAS Weru" },
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
      )
    )

    val status = executeWrite(OperationType.WRITE, billRef.path, allowOfflineQueue) {
      batch.commit()
    }
    return receiptNo to status
  }

  suspend fun verifyTransferPayment(
    bill: Bill,
    pendingPayment: Payment?,
    adminName: String,
    allowOfflineQueue: Boolean = false
  ): Pair<String, WriteResultStatus> {
    requireUserId()
    val receiptNo = generateReceiptNumber(bill)
    val verifier = adminName.trim().ifBlank { "Admin PAMSIMAS Weru" }
    val billRef = db.collection("bills").document(bill.id)
    val batch = db.batch()

    batch.update(
      billRef,
      mapOf(
        "status" to "lunas",
        "metodePembayaran" to "transfer",
        "nomorKuitansi" to receiptNo,
        "alasanPenolakan" to "",
        "dibayarPada" to FieldValue.serverTimestamp(),
        "diverifikasiOleh" to verifier,
        "updatedAt" to FieldValue.serverTimestamp()
      )
    )

    if (pendingPayment != null && pendingPayment.id.isNotBlank()) {
      val payRef = db.collection("payments").document(pendingPayment.id)
      batch.update(
        payRef,
        mapOf(
          "status" to "lunas",
          "nomorKuitansi" to receiptNo,
          "diverifikasiOleh" to verifier,
          "updatedAt" to FieldValue.serverTimestamp()
        )
      )
    } else {
      val payRef = db.collection("payments").document()
      batch.set(
        payRef,
        mapOf(
          "billId" to bill.id,
          "customerId" to bill.customerId,
          "customerNama" to bill.customerNama,
          "noMeter" to bill.noMeter,
          "customerOwnerUid" to bill.customerOwnerUid,
          "customerEmail" to bill.customerEmail,
          "periode" to bill.periode,
          "pemakaian" to bill.pemakaian,
          "meterAwal" to bill.meterAwal,
          "meterAkhir" to bill.meterAkhir,
          "biayaPemakaian" to bill.biayaPemakaian,
          "biayaBeban" to bill.biayaBeban,
          "jumlahBayar" to bill.totalTagihan,
          "metode" to "transfer",
          "status" to "lunas",
          "nomorKuitansi" to receiptNo,
          "namaPengirim" to bill.namaPengirimTransfer,
          "bankPengirim" to bill.bankPengirim,
          "catatan" to bill.catatanTransfer,
          "proofImageBase64" to bill.proofImageBase64,
          "diverifikasiOleh" to verifier,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp()
        )
      )
    }

    val status = executeWrite(OperationType.WRITE, billRef.path, allowOfflineQueue) {
      batch.commit()
    }
    return receiptNo to status
  }

  suspend fun rejectTransferPayment(
    bill: Bill,
    pendingPayment: Payment?,
    reason: String,
    adminName: String,
    allowOfflineQueue: Boolean = false
  ): WriteResultStatus {
    requireUserId()
    val verifier = adminName.trim().ifBlank { "Admin PAMSIMAS Weru" }
    val billRef = db.collection("bills").document(bill.id)
    val batch = db.batch()

    batch.update(
      billRef,
      mapOf(
        "status" to "ditolak",
        "alasanPenolakan" to reason.trim(),
        "diverifikasiOleh" to verifier,
        "updatedAt" to FieldValue.serverTimestamp()
      )
    )

    if (pendingPayment != null && pendingPayment.id.isNotBlank()) {
      val payRef = db.collection("payments").document(pendingPayment.id)
      batch.update(
        payRef,
        mapOf(
          "status" to "ditolak",
          "catatan" to reason.trim(),
          "diverifikasiOleh" to verifier,
          "updatedAt" to FieldValue.serverTimestamp()
        )
      )
    }

    return executeWrite(OperationType.WRITE, billRef.path, allowOfflineQueue) {
      batch.commit()
    }
  }

  // --- APP CONFIG ---

  fun observeAppConfig(): Flow<AppConfig?> = flow {
    requireUserId()
    val docRef = db.collection("app_config").document("pamsimas")
    emitAll(
      docRef.snapshots(MetadataChanges.INCLUDE)
        .map { snap ->
          if (snap.exists()) {
            snap.toObject(AppConfig::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          } else {
            AppConfig()
          }
        }
        .catch { error ->
          handleStreamError(error, OperationType.GET, docRef.path)
          emit(AppConfig())
        }
    )
  }

  suspend fun saveAppConfig(
    namaBank: String,
    nomorRekening: String,
    atasNama: String,
    kontakAdmin: String,
    catatanPembayaran: String,
    allowOfflineQueue: Boolean = false
  ): WriteResultStatus {
    requireUserId()
    val docRef = db.collection("app_config").document("pamsimas")
    val payload = mapOf(
      "namaBank" to namaBank.trim(),
      "nomorRekening" to nomorRekening.trim(),
      "atasNama" to atasNama.trim(),
      "kontakAdmin" to kontakAdmin.trim(),
      "catatanPembayaran" to catatanPembayaran.trim(),
      "updatedAt" to FieldValue.serverTimestamp()
    )
    return executeWrite(OperationType.WRITE, docRef.path, allowOfflineQueue) {
      docRef.set(payload)
    }
  }

  suspend fun syncPendingWrites() {
    requireUserId()
    db.waitForPendingWrites().await()
  }

  // --- INTERNAL HELPERS ---

  private fun <T> observeCustomerScopedList(
    collectionPath: String,
    uid: String,
    verifiedEmail: String?,
    clazz: Class<T>,
    sorter: (List<T>) -> List<T>
  ): Flow<SnapshotData<List<T>>> = flow {
    requireUserId()
    val byUidFlow = db.collection(collectionPath)
      .whereEqualTo("customerOwnerUid", uid)
      .snapshots(MetadataChanges.INCLUDE)
      .map { snap ->
        SnapshotData(
          data = snap.toObjects(clazz, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE),
          isFromCache = snap.metadata.isFromCache,
          hasPendingWrites = snap.metadata.hasPendingWrites()
        )
      }
      .catch { error ->
        handleStreamError(error, OperationType.LIST, "$collectionPath?customerOwnerUid=$uid")
        throw error
      }

    val normalizedEmail = verifiedEmail?.trim()?.lowercase()?.takeIf { it.isNotBlank() }
    if (normalizedEmail != null && auth.currentUser?.isEmailVerified == true) {
      val byEmailFlow = db.collection(collectionPath)
        .whereEqualTo("customerEmail", normalizedEmail)
        .snapshots(MetadataChanges.INCLUDE)
        .map { snap ->
          SnapshotData(
            data = snap.toObjects(clazz, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE),
            isFromCache = snap.metadata.isFromCache,
            hasPendingWrites = snap.metadata.hasPendingWrites()
          )
        }
        .catch { error ->
          handleStreamError(error, OperationType.LIST, "$collectionPath?customerEmail=$normalizedEmail")
          throw error
        }

      emitAll(
        combine(byUidFlow, byEmailFlow) { uidRes, emailRes ->
          val merged = (uidRes.data + emailRes.data).distinct()
          SnapshotData(
            data = sorter(merged),
            isFromCache = uidRes.isFromCache || emailRes.isFromCache,
            hasPendingWrites = uidRes.hasPendingWrites || emailRes.hasPendingWrites
          )
        }
      )
    } else {
      emitAll(
        byUidFlow.map { res ->
          res.copy(data = sorter(res.data))
        }
      )
    }
  }

  private fun generateReceiptNumber(bill: Bill): String {
    val cleanPeriod = bill.periode.replace("-", "")
    val cleanMeter = bill.noMeter.replace(Regex("[^A-Za-z0-9]"), "").take(6).ifBlank { "WR" }
    val suffix = (System.currentTimeMillis() % 10000).toString().padStart(4, '0')
    return "KW-$cleanPeriod-$cleanMeter-$suffix"
  }

  private fun handleStreamError(error: Throwable, operationType: OperationType, path: String) {
    val firestoreError = error.unwrapFirestoreException()
    if (firestoreError == null && error is CancellationException) return
    val toLog = firestoreError ?: error
    if (auth.currentUser != null) {
      handleFirestoreError(toLog, operationType, path)
    }
  }

  companion object {
    const val BOOTSTRAP_ADMIN_EMAIL = "tria.hendika46@admin.smp.belajar.id"
    private const val OFFLINE_WRITE_WAIT_MS = 750L
  }
}
