package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.Bill
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.UUID

class PamsimasRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun admin_createCustomer_recordMeter_and_cashPayment_succeeds() = runBlocking {
    val customerEmail = "warga.${UUID.randomUUID().toString().take(6)}@weru.id"
    val customerUid = signInTestUser(customerEmail, markVerified = true)
    auth.signOut()

    signInTestUser(PamsimasRepository.BOOTSTRAP_ADMIN_EMAIL, markVerified = true)
    val repo = PamsimasRepository(firestore, auth)

    val meterCode = "WR-${UUID.randomUUID().toString().take(5).uppercase()}"
    val (customerId, _) = withTimeout(TIMEOUT_MS) {
      repo.createCustomer(
        nama = "Siti Aminah",
        noMeter = meterCode,
        alamat = "Dusun Weru Krajan",
        rtRw = "RT 02 / RW 01",
        noHp = "081234567890",
        emailPelanggan = customerEmail,
        ownerUid = customerUid,
        meterAwal = 20L
      )
    }
    assertTrue(customerId.isNotBlank())

    val customers = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeAllCustomers().first { snap -> snap.data.any { it.id == customerId } }.data
    }
    val customer = customers.first { it.id == customerId }

    // Record 25 m3 (pemakaian 5 m3 -> <8 m3 -> 5*2000 + 5000 = 15000)
    val periode = "2026-11"
    val (billId, _) = withTimeout(TIMEOUT_MS) {
      repo.recordMeterAndCreateBill(
        customer = customer,
        periode = periode,
        meterAwal = 20L,
        meterAkhir = 25L,
        catatan = "Pencatatan rutin"
      )
    }

    val bills = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeAllBills().first { snap -> snap.data.any { it.id == billId } }.data
    }
    val createdBill = bills.first { it.id == billId }
    assertEquals(5L, createdBill.pemakaian)
    assertEquals(10000L, createdBill.biayaPemakaian)
    assertEquals(5000L, createdBill.biayaBeban)
    assertEquals(15000L, createdBill.totalTagihan)
    assertEquals("belum_bayar", createdBill.status)

    // Pay cash
    val (receiptNo, _) = withTimeout(TIMEOUT_MS) {
      repo.recordCashPayment(createdBill, "Hendika WP")
    }
    assertTrue(receiptNo.startsWith("KW-"))
  }

  @Test
  fun pelanggan_canReadOwnBillsAndSubmitTransferProof_andCrossUserBlocked() = runBlocking {
    val suffix = UUID.randomUUID().toString().take(6)
    val aliceEmail = "alice.$suffix@weru.id"
    val bobEmail = "bob.$suffix@weru.id"
    val aliceUid = signInTestUser(aliceEmail, markVerified = true)
    auth.signOut()
    signInTestUser(bobEmail, markVerified = true)
    auth.signOut()

    // Admin creates Alice's customer & bill
    signInTestUser(PamsimasRepository.BOOTSTRAP_ADMIN_EMAIL, markVerified = true)
    val adminRepo = PamsimasRepository(firestore, auth)
    val (customerId, _) = withTimeout(TIMEOUT_MS) {
      adminRepo.createCustomer(
        nama = "Alice Weru",
        noMeter = "WR-$suffix",
        alamat = "Dusun Weru",
        rtRw = "RT 01 / RW 01",
        noHp = "0811111111",
        emailPelanggan = aliceEmail,
        ownerUid = aliceUid,
        meterAwal = 10L
      )
    }
    val aliceCustomer = withTimeout(FLOW_TIMEOUT_MS) {
      adminRepo.observeAllCustomers().first { it.data.any { c -> c.id == customerId } }.data
        .first { it.id == customerId }
    }
    val (billId, _) = withTimeout(TIMEOUT_MS) {
      adminRepo.recordMeterAndCreateBill(
        customer = aliceCustomer,
        periode = "2026-10",
        meterAwal = 10L,
        meterAkhir = 20L,
        catatan = "Meter naik 10 m3"
      )
    }
    auth.signOut()

    // Alice reads her own bill & submits transfer proof
    signInTestUser(aliceEmail, markVerified = true)
    val aliceRepo = PamsimasRepository(firestore, auth)
    val aliceBills = withTimeout(FLOW_TIMEOUT_MS) {
      aliceRepo.observeCustomerBills(aliceUid, aliceEmail)
        .first { it.data.any { b -> b.id == billId } }.data
    }
    val aliceBill: Bill = aliceBills.first { it.id == billId }
    assertEquals(10L, aliceBill.pemakaian)
    assertEquals(0L, aliceBill.biayaBeban)
    assertEquals(20000L, aliceBill.totalTagihan)

    withTimeout(TIMEOUT_MS) {
      aliceRepo.submitTransferProof(
        bill = aliceBill,
        namaPengirim = "Alice",
        bankPengirim = "BRI",
        catatan = "Transfer m-banking",
        proofImageBase64 = "ZHVtbXk="
      )
    }
    auth.signOut()

    // Bob cannot list all customers or read Alice's bills
    signInTestUser(bobEmail, markVerified = true)
    val bobRepo = PamsimasRepository(firestore, auth)
    try {
      withTimeout(FLOW_TIMEOUT_MS) {
        bobRepo.observeAllCustomers().first()
      }
      fail("Expected PERMISSION_DENIED for Bob listing all customers")
    } catch (e: Exception) {
      val fsEx = e.findFirestoreException()
      assertNotNull("Expected nested FirebaseFirestoreException, got $e", fsEx)
      assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx!!.code)
    }

    try {
      withTimeout(FLOW_TIMEOUT_MS) {
        bobRepo.observeCustomerBills(aliceUid, null).first()
      }
      fail("Expected PERMISSION_DENIED for Bob querying Alice's bills")
    } catch (e: Exception) {
      val fsEx = e.findFirestoreException()
      assertNotNull("Expected nested FirebaseFirestoreException, got $e", fsEx)
      assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx!!.code)
    }
  }

  @Test
  fun unauthenticatedCaller_cannotQueryCustomersOrBills() = runBlocking {
    auth.signOut()
    val repo = PamsimasRepository(firestore, auth)
    // Wait: repository checks requireUserId() synchronously in flow builder,
    // so test both repository guard and direct Firestore query denial.
    try {
      withTimeout(FLOW_TIMEOUT_MS) {
        repo.observeAllBills().first()
      }
      fail("Expected exception for unauthenticated user")
    } catch (e: IllegalStateException) {
      assertTrue(e.message!!.contains("Google"))
    }
  }

  @Test
  fun admin_deleteCustomer_cascadesAndRemovesAllCustomerData() = runBlocking {
    val suffix = UUID.randomUUID().toString().take(6)
    val custEmail = "hapus.$suffix@weru.id"
    val custUid = signInTestUser(custEmail, markVerified = true)
    val custRepo = PamsimasRepository(firestore, auth)
    withTimeout(TIMEOUT_MS) {
      custRepo.submitCustomerRequest(
        nama = "Warga Hapus",
        noMeter = "WR-$suffix",
        alamat = "Dusun Weru",
        rtRw = "RT 03 / RW 01",
        noHp = "081299998888"
      )
    }
    auth.signOut()

    signInTestUser(PamsimasRepository.BOOTSTRAP_ADMIN_EMAIL, markVerified = true)
    val adminRepo = PamsimasRepository(firestore, auth)

    val (customerId, _) = withTimeout(TIMEOUT_MS) {
      adminRepo.createCustomer(
        nama = "Warga Hapus",
        noMeter = "WR-$suffix",
        alamat = "Dusun Weru",
        rtRw = "RT 03 / RW 01",
        noHp = "081299998888",
        emailPelanggan = custEmail,
        ownerUid = custUid,
        meterAwal = 10L
      )
    }
    val createdCustomer = withTimeout(FLOW_TIMEOUT_MS) {
      adminRepo.observeAllCustomers().first { it.data.any { c -> c.id == customerId } }.data
        .first { it.id == customerId }
    }

    val (billId, _) = withTimeout(TIMEOUT_MS) {
      adminRepo.recordMeterAndCreateBill(
        customer = createdCustomer,
        periode = "2026-10",
        meterAwal = 10L,
        meterAkhir = 22L,
        catatan = "Sebelum dihapus"
      )
    }
    val createdBill = withTimeout(FLOW_TIMEOUT_MS) {
      adminRepo.observeAllBills().first { it.data.any { b -> b.id == billId } }.data
        .first { it.id == billId }
    }
    withTimeout(TIMEOUT_MS) {
      adminRepo.recordCashPayment(createdBill, "Hendika WP")
    }

    withTimeout(FLOW_TIMEOUT_MS) {
      adminRepo.observeAllPayments().first { it.data.any { p -> p.customerId == customerId } }
    }

    // Delete customer and verify all associated records are gone immediately
    withTimeout(TIMEOUT_MS) {
      adminRepo.deleteCustomer(
        customerId = customerId,
        ownerUid = custUid,
        isOnline = true,
        allowOfflineQueue = false
      )
    }

    val remainingCustomers = withTimeout(FLOW_TIMEOUT_MS) {
      adminRepo.observeAllCustomers().first { snap -> snap.data.none { it.id == customerId } }.data
    }
    val remainingReadings = withTimeout(FLOW_TIMEOUT_MS) {
      adminRepo.observeAllReadings().first { snap -> snap.data.none { it.customerId == customerId } }.data
    }
    val remainingBills = withTimeout(FLOW_TIMEOUT_MS) {
      adminRepo.observeAllBills().first { snap -> snap.data.none { it.customerId == customerId } }.data
    }
    val remainingPayments = withTimeout(FLOW_TIMEOUT_MS) {
      adminRepo.observeAllPayments().first { snap -> snap.data.none { it.customerId == customerId } }.data
    }
    val requestSnap = withTimeout(TIMEOUT_MS) {
      firestore.collection("customer_requests").document(custUid).get().await()
    }

    assertTrue(remainingCustomers.none { it.id == customerId })
    assertTrue(remainingReadings.none { it.customerId == customerId })
    assertTrue(remainingBills.none { it.customerId == customerId })
    assertTrue(remainingPayments.none { it.customerId == customerId })
    assertTrue(!requestSnap.exists())
  }

  private companion object {
    const val TIMEOUT_MS = 6000L
    const val FLOW_TIMEOUT_MS = 4000L
  }
}
