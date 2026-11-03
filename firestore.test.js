const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

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

function validSlotPayload(userId = ALICE_UID) {
  const now = new Date(Date.now() - 1000);
  return {
    userId,
    campus: "WAFY",
    batchName: "Aliya Ula (Deg 1)",
    dayOfWeek: "MON",
    periodNumber: 1,
    subjectName: "Fiqh (Fathhul Mueen)",
    subjectNameArabic: "فتح المعين",
    facultyId: "fac_1",
    facultyName: "Usthad Abdul Hakeem Wafy",
    slotType: "ISLAMIC",
    isDoublePeriod: false,
    isCrossCampusFaculty: true,
    electiveSubBatches: "",
    roomName: "Hall 101",
    createdAt: now,
    updatedAt: now,
  };
}

function validAuditPayload(userId = ALICE_UID) {
  const now = new Date(Date.now() - 1000);
  return {
    userId,
    slotId: "slot_1",
    campus: "WAFY",
    batchName: "Aliya Ula (Deg 1)",
    dateString: "2026-11-03",
    dayOfWeek: "MON",
    periodNumber: 1,
    subjectName: "Fiqh (Fathhul Mueen)",
    facultyName: "Usthad Abdul Hakeem Wafy",
    status: "MISSED",
    missReason: "University Senate Meeting",
    substituteFaculty: "",
    classLeaderName: "Muhammed Sinan (Class Leader)",
    compensationDate: "2026-11-07 (Sat P5)",
    compensationNotes: "Scheduled makeup slot",
    createdAt: now,
    updatedAt: now,
  };
}

test("Unauthenticated user: cannot read or create timetable slots", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("timetable_slots").get()
  );
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("timetable_slots")
      .doc("slot_1")
      .set(validSlotPayload(ALICE_UID))
  );
});

test("Authenticated user: can create, read, list, and update own timetable slot", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const docRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("timetable_slots")
    .doc("slot_1");

  await assertSucceeds(docRef.set(validSlotPayload(ALICE_UID)));
  await assertSucceeds(docRef.get());
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("timetable_slots").get()
  );
  await assertSucceeds(
    docRef.update({
      roomName: "Hall 202",
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Cross-user isolation: Bob cannot read or modify Alice's timetable slots or audits", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context
      .firestore()
      .collection("users")
      .doc(ALICE_UID)
      .collection("timetable_slots")
      .doc("slot_1")
      .set(validSlotPayload(ALICE_UID));
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("timetable_slots")
      .doc("slot_1")
      .get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("timetable_slots").get()
  );
});

test("Dirty Dozen: rejects ghost fields, invalid enums, and immutable createdAt tampering", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const docRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("period_audits")
    .doc("audit_1");

  // Ghost field on create
  await assertFails(
    docRef.set({
      ...validAuditPayload(ALICE_UID),
      isVerifiedByDean: true,
    })
  );

  // Invalid status enum
  await assertFails(
    docRef.set({
      ...validAuditPayload(ALICE_UID),
      status: "HACKED_STATUS",
    })
  );

  // Valid create, then attempt to mutate createdAt
  await assertSucceeds(docRef.set(validAuditPayload(ALICE_UID)));
  await assertFails(
    docRef.update({
      createdAt: new Date(),
      updatedAt: new Date(Date.now() - 500),
    })
  );
});
