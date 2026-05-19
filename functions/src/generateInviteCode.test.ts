import { generateInviteCode } from "./generateInviteCode";
import * as admin from "firebase-admin";

// ─── Helpers ──────────────────────────────────────────────────────────────────

/**
 * Creates a minimal Firestore mock whose `where(...).limit(1).get()` chain
 * returns an empty snapshot (code not taken) by default.
 */
function makeFirestoreMock(
  existingCodes: Set<string> = new Set()
): admin.firestore.Firestore {
  const getMock = jest.fn().mockImplementation(async () => {
    // The mock captures the code from the where() call via closure below
    return { empty: true };
  });

  const limitMock = jest.fn().mockReturnValue({ get: getMock });

  const whereMock = jest.fn().mockImplementation(
    (_field: string, _op: string, value: string) => {
      // Override get to check against existingCodes
      const get = async () => ({ empty: !existingCodes.has(value) });
      return { limit: () => ({ get }) };
    }
  );

  const collectionMock = jest.fn().mockReturnValue({
    where: whereMock,
    limit: limitMock,
  });

  return { collection: collectionMock } as unknown as admin.firestore.Firestore;
}

// ─── Tests ────────────────────────────────────────────────────────────────────

describe("generateInviteCode", () => {
  describe("code format", () => {
    it("returns a string of exactly 8 characters", async () => {
      const db = makeFirestoreMock();
      const code = await generateInviteCode(db);
      expect(code).toHaveLength(8);
    });

    it("returns only uppercase letters and digits (A-Z, 0-9)", async () => {
      const db = makeFirestoreMock();
      const code = await generateInviteCode(db);
      expect(code).toMatch(/^[A-Z0-9]{8}$/);
    });

    it("generates different codes on successive calls (probabilistic)", async () => {
      const db = makeFirestoreMock();
      const codes = new Set<string>();
      for (let i = 0; i < 20; i++) {
        codes.add(await generateInviteCode(db));
      }
      // With 36^8 ≈ 2.8 trillion possibilities, 20 calls should always be unique
      expect(codes.size).toBe(20);
    });
  });

  describe("uniqueness check", () => {
    it("retries and returns a code not present in Firestore", async () => {
      // First two generated codes will be "taken"; third will be free.
      // We control this by pre-populating existingCodes with the first two
      // codes that Math.random will produce — instead, we mock Math.random.
      const callCount = { value: 0 };
      const originalRandom = Math.random;

      // First call → 'A' repeated 8 times → "AAAAAAAA" (taken)
      // Second call → 'B' repeated 8 times → "BBBBBBBB" (taken)
      // Third call → 'C' repeated 8 times → "CCCCCCCC" (free)
      Math.random = jest.fn().mockImplementation(() => {
        callCount.value++;
        // Each code needs 8 random calls
        const codeIndex = Math.ceil(callCount.value / 8) - 1; // 0, 1, 2, ...
        return codeIndex === 0 ? 0 : codeIndex === 1 ? 1 / 36 : 2 / 36;
      });

      const existingCodes = new Set(["AAAAAAAA", "BBBBBBBB"]);
      const db = makeFirestoreMock(existingCodes);

      const code = await generateInviteCode(db);

      Math.random = originalRandom;

      expect(code).toHaveLength(8);
      expect(code).toMatch(/^[A-Z0-9]{8}$/);
      expect(existingCodes.has(code)).toBe(false);
    });

    it("throws an error when all retry attempts are exhausted", async () => {
      // Make every possible code appear "taken" by always returning non-empty snapshot
      const alwaysTakenDb = {
        collection: jest.fn().mockReturnValue({
          where: jest.fn().mockReturnValue({
            limit: jest.fn().mockReturnValue({
              get: jest.fn().mockResolvedValue({ empty: false }),
            }),
          }),
        }),
      } as unknown as admin.firestore.Firestore;

      await expect(generateInviteCode(alwaysTakenDb)).rejects.toThrow(
        /Failed to generate a unique invite code after \d+ attempts/
      );
    });
  });

  describe("Firestore query", () => {
    it("queries the groups collection with inviteCode field", async () => {
      const getMock = jest.fn().mockResolvedValue({ empty: true });
      const limitMock = jest.fn().mockReturnValue({ get: getMock });
      const whereMock = jest.fn().mockReturnValue({ limit: limitMock });
      const collectionMock = jest.fn().mockReturnValue({ where: whereMock });

      const db = { collection: collectionMock } as unknown as admin.firestore.Firestore;

      await generateInviteCode(db);

      expect(collectionMock).toHaveBeenCalledWith("groups");
      expect(whereMock).toHaveBeenCalledWith(
        "inviteCode",
        "==",
        expect.stringMatching(/^[A-Z0-9]{8}$/)
      );
      expect(limitMock).toHaveBeenCalledWith(1);
      expect(getMock).toHaveBeenCalled();
    });
  });
});
