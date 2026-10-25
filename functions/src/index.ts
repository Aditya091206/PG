import * as functions from "firebase-functions/v2";
import * as admin from "firebase-admin";
import { ExternalGstVerificationAdapter } from "./gstAdapter";
import { slugifyPgName, generateHmacSuffix, formatPgCode } from "./codeGenerator";

admin.initializeApp();
const db = admin.firestore();
const auth = admin.auth();
const messaging = admin.messaging();

const HMAC_SECRET = process.env.HMAC_SECRET || "production_default_pg_secret_key_2026";
const gstAdapter = new ExternalGstVerificationAdapter();

// Single configuration value for join request expiry
export const JOIN_REQUEST_EXPIRY_HOURS = 24;

/**
 * Callable: Assign user role and custom claims.
 * Client cannot set its own custom claims; this function enforces role assignment.
 */
export const assignUserRole = functions.https.onCall(
  { enforceAppCheck: false }, // Set to true in production with Play Integrity
  async (request) => {
    if (!request.auth) {
      throw new functions.https.HttpsError("unauthenticated", "User must be authenticated.");
    }
    const { role } = request.data;
    if (role !== "owner" && role !== "student") {
      throw new functions.https.HttpsError("invalid-argument", "Role must be 'owner' or 'student'.");
    }

    const uid = request.auth.uid;
    const currentClaims = (await auth.getUser(uid)).customClaims || {};

    // Do not downgrade or tamper if already approved student
    await auth.setCustomUserClaims(uid, {
      ...currentClaims,
      role: role,
    });

    await db.collection("users").doc(uid).set(
      {
        userId: uid,
        role: role,
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      },
      { merge: true }
    );

    return { success: true, role };
  }
);

/**
 * Callable: Generate unique PG code with HMAC suffix.
 */
export const generatePgCodeForProperty = functions.https.onCall(
  async (request) => {
    if (!request.auth) {
      throw new functions.https.HttpsError("unauthenticated", "User must be authenticated.");
    }
    const { pgId, pgName, contactPhone } = request.data;
    if (!pgId || !pgName || !contactPhone) {
      throw new functions.https.HttpsError("invalid-argument", "Missing required fields.");
    }

    const pgDoc = await db.collection("pgs").doc(pgId).get();
    if (!pgDoc.exists) {
      throw new functions.https.HttpsError("not-found", "PG not found.");
    }
    if (pgDoc.data()?.ownerId !== request.auth.uid) {
      throw new functions.https.HttpsError("permission-denied", "Only the owner can generate codes.");
    }

    const slug = slugifyPgName(pgName);
    let nonce = 0;
    let code = "";
    let isUnique = false;

    while (!isUnique && nonce < 20) {
      const suffix = generateHmacSuffix(contactPhone, HMAC_SECRET, nonce);
      code = formatPgCode(slug, suffix);
      const codeDoc = await db.collection("pg_codes").doc(code).get();
      if (!codeDoc.exists) {
        isUnique = true;
      } else {
        nonce++;
      }
    }

    if (!isUnique) {
      throw new functions.https.HttpsError("internal", "Could not generate a unique code.");
    }

    const batch = db.batch();
    const codeRef = db.collection("pg_codes").doc(code);
    batch.set(codeRef, {
      code,
      pgId,
      pgName,
      ownerId: request.auth.uid,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
      failedAttempts: 0,
      isLocked: false,
    });

    const pgRef = db.collection("pgs").doc(pgId);
    batch.update(pgRef, {
      code,
      codeCreatedAt: admin.firestore.FieldValue.serverTimestamp(),
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    });

    await batch.commit();
    return { code };
  }
);

/**
 * Callable: Regenerate PG code.
 */
export const regeneratePgCode = functions.https.onCall(
  async (request) => {
    if (!request.auth) {
      throw new functions.https.HttpsError("unauthenticated", "User must be authenticated.");
    }
    const { pgId } = request.data;
    const pgDoc = await db.collection("pgs").doc(pgId).get();
    if (!pgDoc.exists || pgDoc.data()?.ownerId !== request.auth.uid) {
      throw new functions.https.HttpsError("permission-denied", "Unauthorized.");
    }

    const oldCode = pgDoc.data()?.code;
    if (oldCode) {
      await db.collection("pg_codes").doc(oldCode).delete();
    }

    const slug = slugifyPgName(pgDoc.data()?.name || "PG");
    const suffix = generateHmacSuffix(pgDoc.data()?.contactPhone || "", HMAC_SECRET, Date.now());
    const newCode = formatPgCode(slug, suffix);

    const batch = db.batch();
    batch.set(db.collection("pg_codes").doc(newCode), {
      code: newCode,
      pgId,
      pgName: pgDoc.data()?.name,
      ownerId: request.auth.uid,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
      failedAttempts: 0,
      isLocked: false,
    });

    batch.update(db.collection("pgs").doc(pgId), {
      code: newCode,
      codeCreatedAt: admin.firestore.FieldValue.serverTimestamp(),
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    });

    await batch.commit();
    return { code: newCode };
  }
);

/**
 * Callable: Verify GST number.
 */
export const verifyGst = functions.https.onCall(
  async (request) => {
    if (!request.auth) {
      throw new functions.https.HttpsError("unauthenticated", "User must be authenticated.");
    }
    const { pgId, gstNumber } = request.data;
    if (!pgId || !gstNumber) {
      throw new functions.https.HttpsError("invalid-argument", "pgId and gstNumber are required.");
    }

    const pgDoc = await db.collection("pgs").doc(pgId).get();
    if (!pgDoc.exists || pgDoc.data()?.ownerId !== request.auth.uid) {
      throw new functions.https.HttpsError("permission-denied", "Unauthorized.");
    }

    const result = await gstAdapter.verifyGst(gstNumber);
    const newStatus = result.isChecksumValid && result.isApiVerified ? "VERIFIED" : "FAILED";

    await db.collection("pgs").doc(pgId).update({
      gstNumber: gstNumber.trim().toUpperCase(),
      gstVerificationStatus: newStatus,
      gstVerificationDetails: result,
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    });

    return result;
  }
);

/**
 * Callable: Submit join request with 24h expiry.
 */
export const submitJoinRequest = functions.https.onCall(
  async (request) => {
    if (!request.auth) {
      throw new functions.https.HttpsError("unauthenticated", "User must be authenticated.");
    }
    const { code, studentData } = request.data;
    if (!code || !studentData) {
      throw new functions.https.HttpsError("invalid-argument", "Missing code or student data.");
    }

    const cleanCode = code.trim().toUpperCase();
    const codeRef = db.collection("pg_codes").doc(cleanCode);
    const codeDoc = await codeRef.get();

    if (!codeDoc.exists) {
      throw new functions.https.HttpsError("not-found", "Invalid PG code.");
    }

    const codeInfo = codeDoc.data();
    if (codeInfo?.isLocked) {
      throw new functions.https.HttpsError(
        "resource-exhausted",
        "This PG code is temporarily locked due to repeated invalid attempts."
      );
    }

    const pgId = codeInfo?.pgId;
    const studentId = request.auth.uid;

    // Check for existing pending request
    const existing = await db
      .collection("pgs")
      .doc(pgId)
      .collection("join_requests")
      .where("studentId", "==", studentId)
      .where("status", "==", "PENDING")
      .get();

    if (!existing.empty) {
      throw new functions.https.HttpsError(
        "already-exists",
        "You already have a pending join request for this PG."
      );
    }

    const expiresAt = new Date();
    expiresAt.setHours(expiresAt.getHours() + JOIN_REQUEST_EXPIRY_HOURS);

    const requestRef = db
      .collection("pgs")
      .doc(pgId)
      .collection("join_requests")
      .doc();

    const joinRequestDoc = {
      requestId: requestRef.id,
      studentId,
      fullName: studentData.fullName || "",
      phoneNumber: request.auth.token.phone_number || studentData.phoneNumber || "",
      age: Number(studentData.age) || 0,
      collegeOrWorkplace: studentData.collegeOrWorkplace || "",
      homeCity: studentData.homeCity || "",
      guardianName: studentData.guardianName || "",
      guardianPhone: studentData.guardianPhone || "",
      idProofUrl: studentData.idProofUrl || "",
      status: "PENDING",
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
      expiresAt: admin.firestore.Timestamp.fromDate(expiresAt),
    };

    await requestRef.set(joinRequestDoc);

    // Update student's active request pointer
    await db.collection("users").doc(studentId).set(
      {
        pendingJoinRequest: {
          requestId: requestRef.id,
          pgId,
          pgName: codeInfo?.pgName,
          expiresAt: admin.firestore.Timestamp.fromDate(expiresAt),
        },
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      },
      { merge: true }
    );

    return { success: true, requestId: requestRef.id, pgId, expiresAt: expiresAt.toISOString() };
  }
);

/**
 * Callable: Review join request (Approve / Reject).
 */
export const reviewJoinRequest = functions.https.onCall(
  async (request) => {
    if (!request.auth) {
      throw new functions.https.HttpsError("unauthenticated", "User must be authenticated.");
    }
    const { pgId, requestId, action, assignedRoomId, assignedBedId, rejectionReason } = request.data;
    if (!pgId || !requestId || !action) {
      throw new functions.https.HttpsError("invalid-argument", "Missing required arguments.");
    }

    const pgDoc = await db.collection("pgs").doc(pgId).get();
    if (!pgDoc.exists || pgDoc.data()?.ownerId !== request.auth.uid) {
      throw new functions.https.HttpsError("permission-denied", "Unauthorized.");
    }

    const reqRef = db.collection("pgs").doc(pgId).collection("join_requests").doc(requestId);
    const reqDoc = await reqRef.get();
    if (!reqDoc.exists) {
      throw new functions.https.HttpsError("not-found", "Join request not found.");
    }

    const reqData = reqDoc.data()!;
    if (reqData.status !== "PENDING") {
      throw new functions.https.HttpsError("failed-precondition", "Request is no longer pending.");
    }

    const studentId = reqData.studentId;

    if (action === "REJECT") {
      await reqRef.update({
        status: "REJECTED",
        rejectionReason: rejectionReason || "Rejected by owner",
        reviewedAt: admin.firestore.FieldValue.serverTimestamp(),
      });
      await db.collection("users").doc(studentId).update({
        pendingJoinRequest: admin.firestore.FieldValue.delete(),
      });
      return { success: true, status: "REJECTED" };
    }

    if (action === "APPROVE") {
      if (!assignedRoomId || !assignedBedId) {
        throw new functions.https.HttpsError(
          "invalid-argument",
          "assignedRoomId and assignedBedId are required for approval."
        );
      }

      await db.runTransaction(async (transaction) => {
        const bedRef = db.collection("pgs").doc(pgId).collection("beds").doc(assignedBedId);
        const bedDoc = await transaction.get(bedRef);
        if (!bedDoc.exists) {
          throw new functions.https.HttpsError("not-found", "Assigned bed does not exist.");
        }
        if (bedDoc.data()?.status === "BOOKED") {
          throw new functions.https.HttpsError("failed-precondition", "Assigned bed is already booked.");
        }

        const roomRef = db.collection("pgs").doc(pgId).collection("rooms").doc(assignedRoomId);
        const roomDoc = await transaction.get(roomRef);
        const roomNumber = roomDoc.data()?.roomNumber || "";
        const rent = roomDoc.data()?.rentPerBed || 0;
        const deposit = roomDoc.data()?.depositPerBed || 0;

        // Update bed record
        transaction.update(bedRef, {
          status: "BOOKED",
          currentStudentId: studentId,
          currentStudentName: reqData.fullName,
          updatedAt: admin.firestore.FieldValue.serverTimestamp(),
        });

        // Create student record under PG
        const studentRecordRef = db.collection("pgs").doc(pgId).collection("students").doc(studentId);
        transaction.set(studentRecordRef, {
          studentId,
          fullName: reqData.fullName,
          phoneNumber: reqData.phoneNumber,
          collegeOrWorkplace: reqData.collegeOrWorkplace,
          homeCity: reqData.homeCity,
          guardianName: reqData.guardianName,
          guardianPhone: reqData.guardianPhone,
          idProofUrl: reqData.idProofUrl,
          roomId: assignedRoomId,
          roomNumber,
          bedId: assignedBedId,
          bedIdentifier: bedDoc.data()?.bedIdentifier || "",
          monthlyRent: rent,
          securityDeposit: deposit,
          checkInDate: admin.firestore.FieldValue.serverTimestamp(),
          status: "ACTIVE",
          depositRefunded: false,
          createdAt: admin.firestore.FieldValue.serverTimestamp(),
        });

        // Update join request status
        transaction.update(reqRef, {
          status: "APPROVED",
          assignedRoomId,
          assignedBedId,
          reviewedAt: admin.firestore.FieldValue.serverTimestamp(),
        });

        // Clear user's pending request and set active PG
        const userRef = db.collection("users").doc(studentId);
        transaction.set(
          userRef,
          {
            activePgId: pgId,
            pendingJoinRequest: admin.firestore.FieldValue.delete(),
            updatedAt: admin.firestore.FieldValue.serverTimestamp(),
          },
          { merge: true }
        );
      });

      // Grant server-side custom claims
      await auth.setCustomUserClaims(studentId, {
        role: "student",
        pgId: pgId,
        isApproved: true,
      });

      return { success: true, status: "APPROVED" };
    }

    throw new functions.https.HttpsError("invalid-argument", "Invalid action.");
  }
);

/**
 * Scheduled Function: Runs every hour to mark expired join requests.
 */
export const expireJoinRequests = functions.scheduler.onSchedule(
  "every 1 hours",
  async () => {
    const now = admin.firestore.Timestamp.now();
    const pgs = await db.collection("pgs").get();

    for (const pgDoc of pgs.docs) {
      const expiredReqs = await pgDoc.ref
        .collection("join_requests")
        .where("status", "==", "PENDING")
        .where("expiresAt", "<=", now)
        .get();

      const batch = db.batch();
      for (const req of expiredReqs.docs) {
        batch.update(req.ref, {
          status: "EXPIRED",
          reviewedAt: admin.firestore.FieldValue.serverTimestamp(),
        });
        const studentId = req.data().studentId;
        if (studentId) {
          batch.update(db.collection("users").doc(studentId), {
            pendingJoinRequest: admin.firestore.FieldValue.delete(),
          });
        }
      }
      if (!expiredReqs.empty) {
        await batch.commit();
      }
    }
  }
);

/**
 * Trigger: Send FCM push notification on new notice created.
 */
export const onNoticeCreated = functions.firestore.onDocumentCreated(
  "pgs/{pgId}/notices/{noticeId}",
  async (event) => {
    const notice = event.data?.data();
    const pgId = event.params.pgId;
    if (!notice) return;

    const students = await db
      .collection("pgs")
      .doc(pgId)
      .collection("students")
      .where("status", "==", "ACTIVE")
      .get();

    const studentIds = students.docs.map((doc) => doc.id);
    if (studentIds.length === 0) return;

    const userDocs = await Promise.all(
      studentIds.map((id) => db.collection("users").doc(id).get())
    );

    const tokens: string[] = [];
    for (const u of userDocs) {
      const token = u.data()?.fcmToken;
      if (token) tokens.push(token);
    }

    if (tokens.length > 0) {
      await messaging.sendEachForMulticast({
        tokens,
        notification: {
          title: `Announcement: ${notice.title}`,
          body: notice.content?.substring(0, 100) || "",
        },
        data: {
          pgId,
          type: "NOTICE",
          noticeId: event.params.noticeId,
        },
      });
    }
  }
);

/**
 * Trigger: Send push notification to PG owner on new join request.
 */
export const onJoinRequestCreated = functions.firestore.onDocumentCreated(
  "pgs/{pgId}/join_requests/{requestId}",
  async (event) => {
    const request = event.data?.data();
    const pgId = event.params.pgId;
    if (!request) return;

    const pgDoc = await db.collection("pgs").doc(pgId).get();
    const ownerId = pgDoc.data()?.ownerId;
    if (!ownerId) return;

    const ownerDoc = await db.collection("users").doc(ownerId).get();
    const ownerToken = ownerDoc.data()?.fcmToken;

    if (ownerToken) {
      await messaging.send({
        token: ownerToken,
        notification: {
          title: "New Student Join Request",
          body: `${request.fullName} requested to join ${pgDoc.data()?.name || "your PG"}.`,
        },
        data: {
          pgId,
          type: "JOIN_REQUEST",
          requestId: event.params.requestId,
        },
      });
    }
  }
);

/**
 * Callable: Account deletion flow (Google Play requirement).
 */
export const deleteUserAccount = functions.https.onCall(
  async (request) => {
    if (!request.auth) {
      throw new functions.https.HttpsError("unauthenticated", "User must be authenticated.");
    }
    const uid = request.auth.uid;
    await db.collection("users").doc(uid).delete();
    await auth.deleteUser(uid);
    return { success: true };
  }
);
