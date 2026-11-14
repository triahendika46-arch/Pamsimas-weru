const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");
const serverTimestamp = () => new Date(Date.now() - 1000);

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ADMIN_UID = "admin_hendika";
const ADMIN_EMAIL = "tria.hendika46@admin.smp.belajar.id";
const ALICE_UID = "alice_uid";
const ALICE_EMAIL = "alice@gmail.com";
const BOB_UID = "bob_uid";
const BOB_EMAIL = "bob@gmail.com";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function adminContext() {
  return testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
  });
}

function aliceContext() {
  return testEnv.authenticatedContext(ALICE_UID, {
    email: ALICE_EMAIL,
    email_verified: true,
  });
}

function bobContext() {
  return testEnv.authenticatedContext(BOB_UID, {
    email: BOB_EMAIL,
    email_verified: true,
  });
}

function makeCustomer(overrides = {}) {
  return {
    nama: "Budi Santoso",
    noMeter: "WR-001",
    alamat: "Dusun Weru RT 01",
    rtRw: "RT 01 / RW 01",
    noHp: "08123456789",
    emailPelanggan: ALICE_EMAIL,
    ownerUid: ALICE_UID,
    aktif: true,
    meterTerakhir: 10,
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
    ...overrides,
  };
}

function makeReading(overrides = {}) {
  return {
    customerId: "cust_1",
    customerNama: "Budi Santoso",
    noMeter: "WR-001",
    customerOwnerUid: ALICE_UID,
    customerEmail: ALICE_EMAIL,
    periode: "2026-11",
    meterAwal: 10,
    meterAkhir: 15,
    pemakaian: 5,
    catatan: "Normal",
    dicatatOlehUid: ADMIN_UID,
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
    ...overrides,
  };
}

function makeBill(overrides = {}) {
  const meterAwal = overrides.meterAwal ?? 10;
  const meterAkhir = overrides.meterAkhir ?? 15;
  const pemakaian = overrides.pemakaian ?? (meterAkhir - meterAwal);
  const biayaPemakaian = overrides.biayaPemakaian ?? pemakaian * 2000;
  const biayaBeban = overrides.biayaBeban ?? (pemakaian < 8 ? 5000 : 0);
  const totalTagihan = overrides.totalTagihan ?? (biayaPemakaian + biayaBeban);
  return {
    customerId: "cust_1",
    customerNama: "Budi Santoso",
    noMeter: "WR-001",
    rtRw: "RT 01 / RW 01",
    customerOwnerUid: ALICE_UID,
    customerEmail: ALICE_EMAIL,
    readingId: "read_1",
    periode: "2026-11",
    meterAwal,
    meterAkhir,
    pemakaian,
    tarifPerM3: 2000,
    biayaPemakaian,
    biayaBeban,
    totalTagihan,
    status: "belum_bayar",
    metodePembayaran: "",
    namaPengirimTransfer: "",
    bankPengirim: "",
    catatanTransfer: "",
    proofImageBase64: "",
    alasanPenolakan: "",
    nomorKuitansi: "",
    diverifikasiOleh: "",
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
    ...overrides,
  };
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user: cannot read any protected collection", async () => {
  const db = testEnv.unauthenticatedContext().firestore();
  await assertFails(db.collection("customers").get());
  await assertFails(db.collection("meter_readings").get());
  await assertFails(db.collection("bills").get());
  await assertFails(db.collection("payments").get());
  await assertFails(db.collection("customer_requests").get());
  await assertFails(db.collection("app_config").doc("pamsimas").get());
});

test("Email spoofing test: unverified admin email is rejected", async () => {
  const spoofDb = testEnv.authenticatedContext("spoof_uid", {
    email: ADMIN_EMAIL,
    email_verified: false,
  }).firestore();
  await assertFails(spoofDb.collection("customers").doc("cust_1").set(makeCustomer()));
});

test("Admin: can create customer, meter reading, and valid bills (<8 m3 with beban & >=8 m3 without beban)", async () => {
  const db = adminContext().firestore();
  await assertSucceeds(db.collection("customers").doc("cust_1").set(makeCustomer()));
  await assertSucceeds(db.collection("meter_readings").doc("read_1").set(makeReading()));
  // 5 m3 -> 10.000 + 5.000 = 15.000
  await assertSucceeds(db.collection("bills").doc("bill_low").set(makeBill()));
  // 8 m3 -> 16.000 + 0 = 16.000
  await assertSucceeds(
    db.collection("bills").doc("bill_high").set(
      makeBill({
        meterAwal: 10,
        meterAkhir: 18,
        pemakaian: 8,
        biayaPemakaian: 16000,
        biayaBeban: 0,
        totalTagihan: 16000,
      })
    )
  );
});

test("Tariff integrity: invalid biayaBeban or totalTagihan is rejected even for Admin", async () => {
  const db = adminContext().firestore();
  await assertSucceeds(db.collection("customers").doc("cust_1").set(makeCustomer()));
  // 5 m3 without Rp5.000 beban -> must fail
  await assertFails(
    db.collection("bills").doc("bill_bad_1").set(
      makeBill({
        pemakaian: 5,
        biayaPemakaian: 10000,
        biayaBeban: 0,
        totalTagihan: 10000,
      })
    )
  );
  // 10 m3 with Rp5.000 beban -> must fail
  await assertFails(
    db.collection("bills").doc("bill_bad_2").set(
      makeBill({
        meterAwal: 10,
        meterAkhir: 20,
        pemakaian: 10,
        biayaPemakaian: 20000,
        biayaBeban: 5000,
        totalTagihan: 25000,
      })
    )
  );
});

test("Pelanggan isolation: Bob cannot read or query Alice's customer, bills, or readings", async () => {
  const adminDb = adminContext().firestore();
  await adminDb.collection("customers").doc("cust_1").set(makeCustomer());
  await adminDb.collection("meter_readings").doc("read_1").set(makeReading());
  await adminDb.collection("bills").doc("bill_1").set(makeBill());

  const bobDb = bobContext().firestore();
  await assertFails(bobDb.collection("customers").doc("cust_1").get());
  await assertFails(bobDb.collection("customers").get());
  await assertFails(bobDb.collection("bills").doc("bill_1").get());
  await assertFails(bobDb.collection("bills").where("customerOwnerUid", "==", ALICE_UID).get());
  await assertFails(bobDb.collection("meter_readings").doc("read_1").get());
});

test("Pelanggan Alice: can query own customer, readings, bills and submit transfer proof, but cannot mark lunas", async () => {
  const adminDb = adminContext().firestore();
  await adminDb.collection("customers").doc("cust_1").set(makeCustomer());
  await adminDb.collection("meter_readings").doc("read_1").set(makeReading());
  await adminDb.collection("bills").doc("bill_1").set(makeBill());

  const aliceDb = aliceContext().firestore();
  await assertSucceeds(aliceDb.collection("customers").where("ownerUid", "==", ALICE_UID).get());
  await assertSucceeds(aliceDb.collection("meter_readings").where("customerOwnerUid", "==", ALICE_UID).get());
  await assertSucceeds(aliceDb.collection("bills").where("customerOwnerUid", "==", ALICE_UID).get());

  // Submit transfer proof -> succeeds
  await assertSucceeds(
    aliceDb.collection("bills").doc("bill_1").update({
      status: "menunggu_verifikasi",
      metodePembayaran: "transfer",
      namaPengirimTransfer: "Budi Santoso",
      bankPengirim: "BRI",
      catatanTransfer: "Sudah transfer",
      proofImageBase64: "ZHVtbXk=",
      alasanPenolakan: "",
      updatedAt: serverTimestamp(),
    })
  );

  // Create pending payment record -> succeeds
  await assertSucceeds(
    aliceDb.collection("payments").doc("pay_1").set({
      billId: "bill_1",
      customerId: "cust_1",
      customerNama: "Budi Santoso",
      noMeter: "WR-001",
      customerOwnerUid: ALICE_UID,
      customerEmail: ALICE_EMAIL,
      periode: "2026-11",
      pemakaian: 5,
      meterAwal: 10,
      meterAkhir: 15,
      biayaPemakaian: 10000,
      biayaBeban: 5000,
      jumlahBayar: 15000,
      metode: "transfer",
      status: "menunggu_verifikasi",
      nomorKuitansi: "",
      namaPengirim: "Budi Santoso",
      bankPengirim: "BRI",
      catatan: "Sudah transfer",
      proofImageBase64: "ZHVtbXk=",
      diverifikasiOleh: "",
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  // Pelanggan cannot mark bill as lunas -> fails
  await assertFails(
    aliceDb.collection("bills").doc("bill_1").update({
      status: "lunas",
      nomorKuitansi: "KW-FAKE",
      updatedAt: serverTimestamp(),
    })
  );

  // Shadow update with ghost field -> fails
  await assertFails(
    aliceDb.collection("bills").doc("bill_1").update({
      status: "menunggu_verifikasi",
      ghostField: true,
      updatedAt: serverTimestamp(),
    })
  );
});

test("Terminal state locking: Pelanggan cannot modify a lunas bill", async () => {
  const adminDb = adminContext().firestore();
  await adminDb.collection("customers").doc("cust_1").set(makeCustomer());
  await adminDb.collection("bills").doc("bill_1").set(
    makeBill({
      status: "lunas",
      metodePembayaran: "tunai",
      nomorKuitansi: "KW-202611-0001",
      diverifikasiOleh: "Hendika WP",
    })
  );

  const aliceDb = aliceContext().firestore();
  await assertFails(
    aliceDb.collection("bills").doc("bill_1").update({
      status: "menunggu_verifikasi",
      metodePembayaran: "transfer",
      updatedAt: serverTimestamp(),
    })
  );
});

test("Customer activation request: user can create own request, cannot read others", async () => {
  const aliceDb = aliceContext().firestore();
  await assertSucceeds(
    aliceDb.collection("customer_requests").doc(ALICE_UID).set({
      uid: ALICE_UID,
      email: ALICE_EMAIL,
      nama: "Alice Weru",
      noMeter: "WR-010",
      alamat: "Dusun Weru",
      rtRw: "RT 02 / RW 01",
      noHp: "081234567",
      status: "menunggu",
      catatanAdmin: "",
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  const bobDb = bobContext().firestore();
  await assertFails(bobDb.collection("customer_requests").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("customer_requests").get());
});
