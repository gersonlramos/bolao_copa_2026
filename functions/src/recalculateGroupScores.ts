/**
 * recalculateGroupScores — Callable Cloud Function
 *
 * Recalculates all resolved bets for a group using a new ScoringSystem.
 * Uses a Firestore transaction — if the transaction fails, the new system
 * is NOT saved, preserving the old scores intact.
 *
 * Requirements: 7.4, 7.5
 */

import { onCall, HttpsError } from "firebase-functions/v2/https";
import { logger } from "firebase-functions/v2"; // eslint-disable-line @typescript-eslint/no-unused-vars
import * as admin from "firebase-admin";

// ---------------------------------------------------------------------------
// Scoring logic (mirrors ScoreCalculator.kt)
// ---------------------------------------------------------------------------

type ScoringCategory =
  | "EXACT_SCORE"
  | "CORRECT_WINNER_AND_WINNER_GOALS"
  | "CORRECT_WINNER_AND_LOSER_GOALS"
  | "CORRECT_DRAW"
  | "CORRECT_WINNER"
  | "NO_SCORE";

interface ScoringSystem {
  exactScore: number;
  correctWinnerAndWinnerGoals: number;
  correctWinnerAndLoserGoals: number;
  correctDraw: number;
  correctWinner: number;
}

function winner(home: number, away: number): "HOME" | "AWAY" | "DRAW" {
  if (home > away) return "HOME";
  if (away > home) return "AWAY";
  return "DRAW";
}

function scoreWithSystem(
  betHome: number, betAway: number,
  resultHome: number, resultAway: number,
  system: ScoringSystem
): { category: ScoringCategory; points: number } {
  let category: ScoringCategory;

  if (betHome === resultHome && betAway === resultAway) {
    category = "EXACT_SCORE";
  } else {
    const bw = winner(betHome, betAway);
    const rw = winner(resultHome, resultAway);
    if (bw !== rw) {
      category = "NO_SCORE";
    } else if (bw === "DRAW") {
      category = "CORRECT_DRAW";
    } else {
      const cands: Array<{ cat: ScoringCategory; pts: number }> = [];
      if (bw === "HOME" && betHome === resultHome)
        cands.push({ cat: "CORRECT_WINNER_AND_WINNER_GOALS", pts: system.correctWinnerAndWinnerGoals });
      if (bw === "AWAY" && betAway === resultAway)
        cands.push({ cat: "CORRECT_WINNER_AND_WINNER_GOALS", pts: system.correctWinnerAndWinnerGoals });
      if (bw === "HOME" && betAway === resultAway)
        cands.push({ cat: "CORRECT_WINNER_AND_LOSER_GOALS", pts: system.correctWinnerAndLoserGoals });
      if (bw === "AWAY" && betHome === resultHome)
        cands.push({ cat: "CORRECT_WINNER_AND_LOSER_GOALS", pts: system.correctWinnerAndLoserGoals });
      category = cands.length > 0
        ? cands.reduce((a, b) => b.pts >= a.pts ? b : a).cat
        : "CORRECT_WINNER";
    }
  }

  const pointsMap: Record<ScoringCategory, number> = {
    EXACT_SCORE: system.exactScore,
    CORRECT_WINNER_AND_WINNER_GOALS: system.correctWinnerAndWinnerGoals,
    CORRECT_WINNER_AND_LOSER_GOALS: system.correctWinnerAndLoserGoals,
    CORRECT_DRAW: system.correctDraw,
    CORRECT_WINNER: system.correctWinner,
    NO_SCORE: 0,
  };

  return { category, points: pointsMap[category] };
}

// ---------------------------------------------------------------------------
// Callable function
// ---------------------------------------------------------------------------

interface RecalculateRequest {
  groupId: string;
  scoringSystem: ScoringSystem;
}

export const recalculateGroupScores = onCall(
  { timeoutSeconds: 540, memory: "512MiB" },
  async (request) => {
    if (!request.auth) {
      throw new HttpsError("unauthenticated", "Must be authenticated");
    }

    const { groupId, scoringSystem } = request.data as RecalculateRequest;
    if (!groupId || !scoringSystem) {
      throw new HttpsError("invalid-argument", "groupId and scoringSystem required");
    }

    const db = admin.firestore();

    // Validate caller is group admin
    const groupRef = db.collection("groups").doc(groupId);
    const groupSnap = await groupRef.get();
    if (!groupSnap.exists) {
      throw new HttpsError("not-found", "Group not found");
    }
    if (groupSnap.data()!.adminUserId !== request.auth.uid) {
      throw new HttpsError("permission-denied", "Only the group admin can change scoring");
    }

    logger.info("recalculateGroupScores", { groupId });

    // Use a transaction: all updates succeed or none do
    await db.runTransaction(async (tx) => {
      // 1. Get all finished matches (for score lookup)
      const matchesSnap = await db.collection("matches")
        .where("status", "==", "FINISHED")
        .get();

      const matchResults = new Map<string, { home: number; away: number }>();
      for (const m of matchesSnap.docs) {
        const md = m.data();
        matchResults.set(m.id, { home: md.scoreHome ?? 0, away: md.scoreAway ?? 0 });
      }

      // 2. Get all bets for this group
      const betsSnap = await db.collection("bets")
        .where("groupId", "==", groupId)
        .get();

      // 3. Recalculate each scored bet
      const memberTotals = new Map<string, number>();

      for (const betDoc of betsSnap.docs) {
        const bet = betDoc.data();
        const result = matchResults.get(bet.matchId);

        if (!result) continue; // match not finished yet — skip

        const { category, points } = scoreWithSystem(
          bet.homeGoals, bet.awayGoals,
          result.home, result.away,
          scoringSystem
        );

        tx.update(betDoc.ref, {
          score: points,
          scoringCategory: category,
          updatedAt: admin.firestore.FieldValue.serverTimestamp(),
        });

        memberTotals.set(bet.userId, (memberTotals.get(bet.userId) ?? 0) + points);
      }

      // 4. Update member totalScores
      const membersSnap = await db.collection("groups").doc(groupId)
        .collection("members").get();

      for (const memberDoc of membersSnap.docs) {
        tx.update(memberDoc.ref, { totalScore: memberTotals.get(memberDoc.id) ?? 0 });
      }

      // 5. Save the new scoring system (only if everything above succeeded)
      tx.update(groupRef, { scoringSystem, rankingStale: false });
    });

    logger.info("recalculateGroupScores: complete", { groupId });
    return { success: true };
  });
