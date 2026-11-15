const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ADMIN_UID = "admin_user_1";
const ADMIN_EMAIL = "abhiraj2482@gmail.com";
const VISITOR_UID = "visitor_user_2";
const VISITOR_EMAIL = "visitor@example.com";

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

function adminContext(emailVerified = true) {
  return testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: emailVerified,
  }).firestore();
}

function visitorContext() {
  return testEnv.authenticatedContext(VISITOR_UID, {
    email: VISITOR_EMAIL,
    email_verified: true,
  }).firestore();
}

test("Unauthenticated user cannot read or list calendar_dates or admin_bookings", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("calendar_dates").where("isPublic", "==", true).get());
  await assertFails(unauthDb.collection("calendar_dates").doc("2026-11-23").get());
  await assertFails(unauthDb.collection("admin_bookings").get());
});

test("Verified admin can create and update calendar_dates and admin_bookings", async () => {
  const db = adminContext(true);
  const now = new Date();

  await assertSucceeds(
    db.collection("calendar_dates").doc("2026-11-23").set({
      dateKey: "2026-11-23",
      yearMonth: "2026-11",
      status: "booked",
      isPublic: true,
      updatedByUid: ADMIN_UID,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    db.collection("calendar_dates").doc("2026-11-23").update({
      status: "pending",
      updatedByUid: ADMIN_UID,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    db.collection("admin_bookings").doc("2026-11-23").set({
      dateKey: "2026-11-23",
      status: "pending",
      clientName: "Aarav Sharma",
      clientPhone: "9876543210",
      eventType: "Wedding",
      notes: "Evening gala",
      updatedByUid: ADMIN_UID,
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("Spoofed admin email with email_verified=false is rejected", async () => {
  const spoofDb = adminContext(false);
  const now = new Date();
  await assertFails(
    spoofDb.collection("calendar_dates").doc("2026-11-23").set({
      dateKey: "2026-11-23",
      yearMonth: "2026-11",
      status: "booked",
      isPublic: true,
      updatedByUid: ADMIN_UID,
      updatedAt: now,
    })
  );
});

test("Authenticated visitor can query public calendar_dates with isPublic filter, but fails unfiltered query and cannot write or read PII", async () => {
  const adminDb = adminContext(true);
  const now = new Date();
  await assertSucceeds(
    adminDb.collection("calendar_dates").doc("2026-11-23").set({
      dateKey: "2026-11-23",
      yearMonth: "2026-11",
      status: "booked",
      isPublic: true,
      updatedByUid: ADMIN_UID,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    adminDb.collection("admin_bookings").doc("2026-11-23").set({
      dateKey: "2026-11-23",
      status: "booked",
      clientName: "Private Client",
      clientPhone: "9999999999",
      eventType: "Reception",
      notes: "VIP",
      updatedByUid: ADMIN_UID,
      createdAt: now,
      updatedAt: now,
    })
  );

  const vDb = visitorContext();
  // Allowed: filtered public list query
  await assertSucceeds(vDb.collection("calendar_dates").where("isPublic", "==", true).get());
  // Denied: unfiltered list query
  await assertFails(vDb.collection("calendar_dates").get());
  // Denied: write to calendar_dates
  await assertFails(
    vDb.collection("calendar_dates").doc("2026-11-24").set({
      dateKey: "2026-11-24",
      yearMonth: "2026-11",
      status: "booked",
      isPublic: true,
      updatedByUid: VISITOR_UID,
      updatedAt: now,
    })
  );
  // Denied: read or list private PII admin_bookings
  await assertFails(vDb.collection("admin_bookings").doc("2026-11-23").get());
  await assertFails(vDb.collection("admin_bookings").where("dateKey", "==", "2026-11-23").get());
  // Denied: self-assign admin role
  await assertFails(vDb.collection("admins").doc(VISITOR_UID).set({ email: VISITOR_EMAIL, role: "admin" }));
});

test("Shadow update with ghost field and invalid status are rejected", async () => {
  const db = adminContext(true);
  const now = new Date();
  await assertFails(
    db.collection("calendar_dates").doc("2026-11-26").set({
      dateKey: "2026-11-26",
      yearMonth: "2026-11",
      status: "booked",
      isPublic: true,
      updatedByUid: ADMIN_UID,
      updatedAt: now,
      ghostField: "hack",
    })
  );
  await assertFails(
    db.collection("calendar_dates").doc("2026-11-26").set({
      dateKey: "2026-11-26",
      yearMonth: "2026-11",
      status: "invalid_status",
      isPublic: true,
      updatedByUid: ADMIN_UID,
      updatedAt: now,
    })
  );
});
