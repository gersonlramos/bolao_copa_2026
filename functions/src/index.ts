/**
 * Firebase Cloud Functions — Bolão Copa 2026
 *
 * All functions are exported from this entry point.
 */

import * as admin from "firebase-admin";

// Initialize Firebase Admin SDK once for all functions.
// When running inside Firebase, this picks up the default credentials automatically.
if (!admin.apps.length) {
  admin.initializeApp();
}

export { pollMatchResults } from "./pollMatchResults";
export { seedMatches } from "./seedMatches";
export { calculateScores } from "./calculateScores";
export { recalculateGroupScores } from "./recalculateGroupScores";
export { loginAttempt } from "./loginAttempt";
export { generateInviteCode } from "./generateInviteCode";
