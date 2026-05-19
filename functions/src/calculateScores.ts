/**
 * calculateScores — Firestore onUpdate trigger on matches/{matchId}
 *
 * Fired when a match document is updated. If status changes to "FINISHED",
 * calculates scores for all bets on that match across all groups.
 *
 * Requirements: 8.1–8.9, 9.3
 */

import { onDocumentUpdated } from "firebase-functions/v2/firestore";
import { logger } from "firebase-functions/v2";
import * as admin from "firebase-admin";

// ---------------------------------------------------------------------------
// Scoring logic (mirrors ScoreCalculator.kt)
// ---------------------------------------------------------------------------

type ScoringCategory =
  | "EXACT_SCORE"
  | "CORRECT_WINNER_AND_WINNER_GOALS"
  | "CORRECT_WINNER_AND_LOSER_GOALS"
  | "CORRECT_DRAW"
  | "NO_SCORE";

interface ScoringSystem {
  exactScore: number;
  correctWinnerAndWinnerGoals: number;
  correctWinnerAndLoserGoals: number;
  correctDraw: number;
}

function winner(home: number, away: number): "HOME" | "AWAY" | "DRAW" {
  if (home > away) return "HOME";
  if (away > home) return "AWAY";
  return "DRAW";
}

function determineCategory(
  betHome: number,
  betAway: number,
  resultHome: number,
  resultAway: number,
  system: ScoringSystem
): ScoringCategory {
  if (betHome === resultHome && betAway === resultAway) return "EXACT_SCORE";

  const betWinner = winner(betHome, betAway);
  const resultWinner = winner(resultHome, resultAway);

  if (betWinner !== resultWinner) return "NO_SCORE";
  if (betWinner === "DRAW") return "CORRECT_DRAW";

  const candidates: Array<{ cat: ScoringCategory; pts: number }> = [];

  if (betWinner === "HOME" && betHome === resultHome)
    candidates.push({ cat: "CORRECT_WINNER_AND_WINNER_GOALS", pts: system.correctWinnerAndWinnerGoals });
  if (betWinner === "AWAY" && betAway === resultAway)
    candidates.push({ cat: "CORRECT_WINNER_AND_WINNER_GOALS", pts: system.correctWinnerAndWinnerGoals });
  if (betWinner === "HOME" && betAway === resultAway)
    candidates.push({ cat: "CORRECT_WINNER_AND_LOSER_GOALS", pts: system.correctWinnerAndLoserGoals });
  if (betWinner === "AWAY" && betHome === resultHome)
    candidates.push({ cat: "CORRECT_WINNER_AND_LOSER_GOALS", pts: system.correctWinnerAndLoserGoals });

  if (candidates.length === 0) return "NO_SCORE";
  return candidates.reduce((best, c) => (c.pts >= best.pts ? c : best)).cat;
}

function calculatePoints(category: ScoringCategory, system: ScoringSystem): number {
  switch (category) {
    case "EXACT_SCORE": return system.exactScore;
    case "CORRECT_WINNER_AND_WINNER_GOALS": return system.correctWinnerAndWinnerGoals;
    case "CORRECT_WINNER_AND_LOSER_GOALS": return system.correctWinnerAndLoserGoals;
    case "CORRECT_DRAW": return system.correctDraw;
    case "NO_SCORE": return 0;
  }
}

// ---------------------------------------------------------------------------
// Cloud Function
// ---------------------------------------------------------------------------

const SCORE_TIMEOUT_MS = 5 * 60 * 1000; // 5 minutes

export const calculateScores = onDocumentUpdated(
  { document: "matches/{matchId}", timeoutSeconds: 540, memory: "512MiB" },
  async (event) => {
    const before = event.data!.before.data();
    const after = event.data!.after.data();

    // Only run when status transitions to FINISHED
    if (before.status === "FINISHED" || after.status !== "FINISHED") return;

    const matchId = event.params.matchId;
    const resultHome: number = after.scoreHome ?? 0;
    const resultAway: number = after.scoreAway ?? 0;

    const db = admin.firestore();
    const startTime = Date.now();

    logger.info("calculateScores: processing", { matchId });

    // 1. Find all groups
    const groupsSnap = await db.collection("groups").get();

    for (const groupDoc of groupsSnap.docs) {
      if (Date.now() - startTime > SCORE_TIMEOUT_MS) {
        // Mark ranking stale and stop processing
        logger.warn("calculateScores: timeout, marking rankingStale", { matchId });
        await groupDoc.ref.update({ rankingStale: true });
        continue;
      }

      const groupData = groupDoc.data();
      const system = groupData.scoringSystem as ScoringSystem;
      if (!system) continue;

      // 2. Get bets for this group/match
      const betsSnap = await db.collection("bets")
        .where("groupId", "==", groupDoc.id)
        .where("matchId", "==", matchId)
        .get();

      if (betsSnap.empty) continue;

      const batch = db.batch();

      // 3. Calculate score for each bet
      for (const betDoc of betsSnap.docs) {
        const bet = betDoc.data();
        const category = determineCategory(
          bet.homeGoals, bet.awayGoals,
          resultHome, resultAway,
          system
        );
        const points = calculatePoints(category, system);

        batch.update(betDoc.ref, {
          score: points,
          scoringCategory: category,
          updatedAt: admin.firestore.FieldValue.serverTimestamp(),
        });
      }

      await batch.commit();

      // 4. Recalculate totalScore for each member in this group
      const membersSnap = await db.collection("groups").doc(groupDoc.id)
        .collection("members").get();

      const allBetsSnap = await db.collection("bets")
        .where("groupId", "==", groupDoc.id)
        .get();

      const memberTotals = new Map<string, number>();
      for (const b of allBetsSnap.docs) {
        const bd = b.data();
        if (bd.score !== null && bd.score !== undefined) {
          memberTotals.set(bd.userId, (memberTotals.get(bd.userId) ?? 0) + (bd.score as number));
        }
      }

      const memberBatch = db.batch();
      for (const memberDoc of membersSnap.docs) {
        const total = memberTotals.get(memberDoc.id) ?? 0;
        memberBatch.update(memberDoc.ref, { totalScore: total });
      }
      await memberBatch.commit();
    }

    logger.info("calculateScores: finished", { matchId });
  });
