const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = "demo-no-project";
const DATABASE_ID = "ai-studio-android-karkunan-2f6a73fe-8121-4d71-8b0b-2e02002454b1";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const ADMIN_UID = "admin_789";
const ADMIN_EMAIL = "abdullahamjad347@gmail.com";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
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

test("Unauthenticated user: cannot read attendance records", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("attendance_records").get());
});

test("Unauthenticated user: cannot read booking messages", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("booking_messages").get());
});

test("Authenticated user: can read duty groups", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("duty_groups").get());
});

test("Authenticated user: can create attendance record with valid payload", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();
  await assertSucceeds(
    aliceDb.collection("attendance_records").add({
      groupId: "A",
      dutyDate: "2026-10-17",
      dayOfWeek: 6,
      workerName: "محمد زین",
      isCoordinator: true,
      arrivalTime: "08:00",
      departureTime: "16:00",
      status: "recorded",
      notes: "حاضر",
      recordedBy: ALICE_UID,
      recordedByName: "Alice",
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("Authenticated user: cannot alter booking message after decision", async () => {
  const now = new Date();
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("booking_messages").doc("msg1").set({
      bookingRef: "CB-001",
      groupId: "A",
      source: "WhatsApp",
      originalMessage: "Need coaster tomorrow 8am",
      status: "accepted",
      decidedBy: ALICE_UID,
      decidedByName: "Alice",
      decidedAt: now,
      decisionNote: "Approved",
      createdAt: now,
      updatedAt: now,
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb.collection("booking_messages").doc("msg1").update({
      status: "declined",
      decidedBy: BOB_UID,
      updatedAt: new Date(),
    })
  );
});
