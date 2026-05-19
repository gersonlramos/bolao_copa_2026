/**
 * loginAttempt — Callable Cloud Function
 *
 * Called by the app on each failed login attempt.
 * Increments failCount in loginAttempts/{emailHash}.
 * After 5 failures, sets blockedUntil = now + 15 minutes.
 * Also validates server-side if the account is currently blocked.
 *
 * Requirements: 2.3, 2.6
 */

import { onCall, HttpsError } from "firebase-functions/v2/https";
import * as admin from "firebase-admin";
import * as crypto from "crypto";

const MAX_ATTEMPTS = 5;
const BLOCK_DURATION_MS = 15 * 60 * 1000; // 15 minutes

function hashEmail(email: string): string {
  return crypto.createHash("sha256").update(email.toLowerCase().trim()).digest("hex");
}

interface LoginAttemptRequest {
  email: string;
}

interface LoginAttemptResponse {
  blocked: boolean;
  blockedUntil?: string; // ISO timestamp
  failCount: number;
}

export const loginAttempt = onCall(async (request): Promise<LoginAttemptResponse> => {
    const { email } = request.data as LoginAttemptRequest;
    if (!email) {
      throw new HttpsError("invalid-argument", "email is required");
    }

    const db = admin.firestore();
    const emailHash = hashEmail(email);
    const ref = db.collection("loginAttempts").doc(emailHash);

    // Use a transaction to safely read-modify-write
    const result = await db.runTransaction(async (tx) => {
      const snap = await tx.get(ref);
      const now = Date.now();

      const existing = snap.exists ? snap.data()! : null;
      const blockedUntilTs = existing?.blockedUntil as admin.firestore.Timestamp | undefined;
      const blockedUntilMs = blockedUntilTs ? blockedUntilTs.toMillis() : 0;

      // Server-side block validation
      if (blockedUntilMs > now) {
        return {
          blocked: true,
          blockedUntil: new Date(blockedUntilMs).toISOString(),
          failCount: (existing?.failCount ?? 0) as number,
        };
      }

      // Increment fail count
      const newFailCount = ((existing?.failCount ?? 0) as number) + 1;
      const shouldBlock = newFailCount >= MAX_ATTEMPTS;
      const newBlockedUntil = shouldBlock
        ? admin.firestore.Timestamp.fromMillis(now + BLOCK_DURATION_MS)
        : null;

      const updateData: Record<string, unknown> = {
        failCount: newFailCount,
        lastAttemptAt: admin.firestore.Timestamp.fromMillis(now),
      };
      if (shouldBlock) {
        updateData.blockedUntil = newBlockedUntil;
      }

      tx.set(ref, updateData, { merge: true });

      return {
        blocked: shouldBlock,
        blockedUntil: shouldBlock ? new Date(now + BLOCK_DURATION_MS).toISOString() : undefined,
        failCount: newFailCount,
      } as LoginAttemptResponse;
    });

    return result;
  }
);

/** Resets login attempts on successful login. Call this internally after successful auth. */
export async function resetLoginAttempts(email: string): Promise<void> {
  const db = admin.firestore();
  const emailHash = hashEmail(email);
  await db.collection("loginAttempts").doc(emailHash).delete();
}
