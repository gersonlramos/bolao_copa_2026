/**
 * pollMatchResults — Cloud Scheduler (every 5 minutes)
 *
 * Queries football-data.org for WC 2026 matches, updates Firestore
 * documents whose status or score changed.
 *
 * Config: firebase functions:config:set footballdata.token=YOUR_TOKEN
 */

import { onSchedule } from "firebase-functions/v2/scheduler";
import { logger } from "firebase-functions/v2";
import * as admin from "firebase-admin";
import axios, { AxiosError } from "axios";

// ---------------------------------------------------------------------------
// Types
// ---------------------------------------------------------------------------

export interface FDMatch {
  id: number;
  utcDate: string;
  status:
    | "SCHEDULED"
    | "TIMED"
    | "IN_PLAY"
    | "PAUSED"
    | "FINISHED"
    | "POSTPONED"
    | "CANCELLED"
    | "SUSPENDED";
  matchday: number | null;
  stage: string;
  group: string | null;
  venue?: string | null;
  homeTeam: { id: number; name: string; shortName: string; tla: string };
  awayTeam: { id: number; name: string; shortName: string; tla: string };
  score: {
    fullTime: { home: number | null; away: number | null };
  };
}

interface FDResponse {
  matches: FDMatch[];
}

type AppStatus = "SCHEDULED" | "IN_PROGRESS" | "FINISHED";

export interface MatchDocument {
  status: AppStatus;
  scoreHome: number | null;
  scoreAway: number | null;
  dataStale: boolean;
  externalId: string;
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

function getToken(): string {
  const token = process.env.FOOTBALL_DATA_TOKEN;
  if (token) return token;
  throw new Error(
    "football-data.org token not configured.\n" +
      "Add FOOTBALL_DATA_TOKEN=your_token to functions/.env"
  );
}

export function mapStatus(apiStatus: FDMatch["status"]): AppStatus {
  switch (apiStatus) {
    case "IN_PLAY":
    case "PAUSED":
      return "IN_PROGRESS";
    case "FINISHED":
      return "FINISHED";
    default:
      return "SCHEDULED";
  }
}

export async function fetchAllMatches(token: string): Promise<FDMatch[]> {
  const response = await axios.get<FDResponse>(
    "https://api.football-data.org/v4/competitions/WC/matches",
    {
      headers: { "X-Auth-Token": token },
      params: { season: 2026 },
      timeout: 15_000,
    }
  );
  return response.data.matches;
}

/**
 * Loads all Firestore match documents indexed by externalId.
 */
export async function loadAllMatchDocs(
  db: admin.firestore.Firestore
): Promise<Map<string, { ref: admin.firestore.DocumentReference; data: MatchDocument }>> {
  const snapshot = await db.collection("matches").get();
  const map = new Map<string, { ref: admin.firestore.DocumentReference; data: MatchDocument }>();
  snapshot.forEach((doc) => {
    const data = doc.data() as MatchDocument;
    if (data.externalId) map.set(data.externalId, { ref: doc.ref, data });
  });
  return map;
}

/**
 * Writes updates for matches whose status or score changed.
 * Returns counts of updated and skipped documents.
 */
export async function applyUpdates(
  db: admin.firestore.Firestore,
  apiMatches: FDMatch[],
  docMap: Map<string, { ref: admin.firestore.DocumentReference; data: MatchDocument }>
): Promise<{ updated: number; skipped: number }> {
  let updated = 0;
  let skipped = 0;
  const BATCH_SIZE = 500;
  let batch = db.batch();
  let batchCount = 0;

  const commitBatch = async () => {
    if (batchCount > 0) {
      await batch.commit();
      batch = db.batch();
      batchCount = 0;
    }
  };

  for (const match of apiMatches) {
    const externalId = String(match.id);
    const entry = docMap.get(externalId);
    if (!entry) { skipped++; continue; }

    const { ref, data } = entry;
    const newStatus = mapStatus(match.status);
    const newHome = match.score.fullTime.home;
    const newAway = match.score.fullTime.away;

    // Skip if nothing changed
    if (
      data.status === newStatus &&
      data.scoreHome === newHome &&
      data.scoreAway === newAway &&
      !data.dataStale
    ) {
      skipped++;
      continue;
    }

    batch.update(ref, {
      status: newStatus,
      scoreHome: newHome,
      scoreAway: newAway,
      dataStale: false,
    } satisfies Partial<MatchDocument>);

    batchCount++;
    updated++;
    if (batchCount >= BATCH_SIZE) await commitBatch();
  }

  await commitBatch();
  return { updated, skipped };
}

export async function markMatchesAsStale(
  db: admin.firestore.Firestore
): Promise<number> {
  const snapshot = await db
    .collection("matches")
    .where("status", "!=", "FINISHED")
    .get();

  if (snapshot.empty) return 0;

  const BATCH_SIZE = 500;
  let batch = db.batch();
  let batchCount = 0;
  let marked = 0;

  for (const doc of snapshot.docs) {
    batch.update(doc.ref, { dataStale: true });
    batchCount++;
    marked++;
    if (batchCount >= BATCH_SIZE) {
      await batch.commit();
      batch = db.batch();
      batchCount = 0;
    }
  }
  if (batchCount > 0) await batch.commit();
  return marked;
}

// ---------------------------------------------------------------------------
// Cloud Function
// ---------------------------------------------------------------------------

export const pollMatchResults = onSchedule(
  { schedule: "every 5 minutes", timeZone: "UTC", timeoutSeconds: 120, memory: "256MiB" },
  async (_event) => {
    const db = admin.firestore();
    logger.info("pollMatchResults: starting");

    let token: string;
    try {
      token = getToken();
    } catch (err) {
      logger.error("pollMatchResults: token not configured", { err });
      return;
    }

    let apiMatches: FDMatch[];
    try {
      apiMatches = await fetchAllMatches(token);
      logger.info(`pollMatchResults: fetched ${apiMatches.length} match(es) from API`);
    } catch (err) {
      const e = err as AxiosError;
      logger.error("pollMatchResults: API request failed", {
        message: e.message,
        status: e.response?.status,
      });
      const staleCount = await markMatchesAsStale(db).catch(() => 0);
      logger.warn(`pollMatchResults: marked ${staleCount} match(es) as stale`);
      return;
    }

    const docMap = await loadAllMatchDocs(db);
    const { updated, skipped } = await applyUpdates(db, apiMatches, docMap);
    logger.info(`pollMatchResults: updated=${updated}, skipped=${skipped}`);
  });
