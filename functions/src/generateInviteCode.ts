import * as admin from "firebase-admin";

/**
 * Characters used for invite code generation.
 * Uppercase letters (A-Z) + digits (0-9) = 36 possible characters.
 */
const INVITE_CODE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
const INVITE_CODE_LENGTH = 8;
const MAX_RETRIES = 10;

/**
 * Generates a random alphanumeric string of the specified length.
 * Uses uppercase letters (A-Z) and digits (0-9).
 */
function generateRandomCode(length: number): string {
  let code = "";
  for (let i = 0; i < length; i++) {
    const randomIndex = Math.floor(Math.random() * INVITE_CODE_CHARS.length);
    code += INVITE_CODE_CHARS[randomIndex];
  }
  return code;
}

/**
 * Checks whether a given invite code already exists in the Firestore `groups` collection.
 *
 * @param db - Firestore instance
 * @param code - The invite code to check
 * @returns true if the code is already in use, false otherwise
 */
async function isCodeTaken(
  db: admin.firestore.Firestore,
  code: string
): Promise<boolean> {
  const snapshot = await db
    .collection("groups")
    .where("inviteCode", "==", code)
    .limit(1)
    .get();
  return !snapshot.empty;
}

/**
 * Generates a unique 8-character alphanumeric invite code (uppercase letters + digits).
 *
 * The function retries up to MAX_RETRIES times if the generated code already exists
 * in the Firestore `groups` collection, ensuring global uniqueness before returning.
 *
 * This is an internal helper — it is NOT a Cloud Function trigger. It is called
 * by other functions (e.g., createGroup) during group creation.
 *
 * @param db - Firestore instance (injected for testability)
 * @returns A unique 8-character alphanumeric invite code
 * @throws Error if a unique code cannot be generated within MAX_RETRIES attempts
 *
 * Requirements: 4.3
 */
export async function generateInviteCode(
  db: admin.firestore.Firestore
): Promise<string> {
  for (let attempt = 1; attempt <= MAX_RETRIES; attempt++) {
    const code = generateRandomCode(INVITE_CODE_LENGTH);
    const taken = await isCodeTaken(db, code);
    if (!taken) {
      return code;
    }
  }
  throw new Error(
    `Failed to generate a unique invite code after ${MAX_RETRIES} attempts.`
  );
}
