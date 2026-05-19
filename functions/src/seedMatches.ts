/**
 * seedMatches — HTTP trigger (one-time use)
 *
 * Fetches all FIFA World Cup 2026 fixtures from football-data.org
 * and populates the Firestore `matches` collection.
 *
 * Usage (after deploy):
 *   curl -X POST \
 *     -H "X-Auth-Token: YOUR_TOKEN" \
 *     https://us-central1-bolao-copa-2026-4dfc5.cloudfunctions.net/seedMatches
 *
 * Safe to run multiple times — uses merge:true (won't overwrite scores).
 *
 * Config: firebase functions:config:set footballdata.token=YOUR_TOKEN
 */

import { onRequest } from "firebase-functions/v2/https";
import { logger } from "firebase-functions/v2";
import * as admin from "firebase-admin";
import axios from "axios";
import { FDMatch, mapStatus } from "./pollMatchResults";

interface FDResponse {
  count: number;
  matches: FDMatch[];
}

function extractGroup(group: string | null): string {
  if (!group) return "";
  // "GROUP_A" → "A", "GROUP_B" → "B", knockout stages return ""
  return group.startsWith("GROUP_") ? group.replace("GROUP_", "") : group;
}

export const seedMatches = onRequest(
  { timeoutSeconds: 120, memory: "256MiB" },
  async (req, res) => {
    if (req.method !== "POST") {
      res.status(405).send("Use POST");
      return;
    }

    // The football-data.org token doubles as the admin credential for this endpoint
    const token =
      (req.headers["x-auth-token"] as string) ||
      process.env.FOOTBALL_DATA_TOKEN;

    if (!token) {
      res.status(403).json({ error: "Missing X-Auth-Token header" });
      return;
    }

    let matches: FDMatch[];
    try {
      const response = await axios.get<FDResponse>(
        "https://api.football-data.org/v4/competitions/WC/matches",
        {
          headers: { "X-Auth-Token": token },
          params: { season: 2026 },
          timeout: 15_000,
        }
      );
      matches = response.data.matches;
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : String(err);
      logger.error("seedMatches: API error", { msg });
      res.status(502).json({ error: "football-data.org request failed", detail: msg });
      return;
    }

    const db = admin.firestore();
    const BATCH_SIZE = 500;
    let batch = db.batch();
    let count = 0;
    let total = 0;

    for (const match of matches) {
      const docId = `wc2026_${match.id}`;
      const ref = db.collection("matches").doc(docId);

      batch.set(
        ref,
        {
          externalId: String(match.id),
          homeTeam: match.homeTeam.name,
          homeTeamTla: match.homeTeam.tla,
          awayTeam: match.awayTeam.name,
          awayTeamTla: match.awayTeam.tla,
          venue: match.venue ?? null,
          group: extractGroup(match.group),
          stage: match.stage,
          matchday: match.matchday ?? 0,
          scheduledAt: admin.firestore.Timestamp.fromDate(
            new Date(match.utcDate)
          ),
          status: mapStatus(match.status),
          scoreHome: match.score.fullTime.home,
          scoreAway: match.score.fullTime.away,
          dataStale: false,
        },
        { merge: true } // preserves scores if already set
      );

      count++;
      total++;
      if (count >= BATCH_SIZE) {
        await batch.commit();
        batch = db.batch();
        count = 0;
      }
    }

    if (count > 0) await batch.commit();

    logger.info(`seedMatches: seeded ${total} matches`);
    res.status(200).json({ success: true, seeded: total });
  });
