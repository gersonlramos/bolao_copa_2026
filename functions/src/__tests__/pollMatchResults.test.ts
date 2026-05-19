/**
 * Unit tests for pollMatchResults helper functions (football-data.org adapter).
 */

import {
  fetchAllMatches,
  loadAllMatchDocs,
  applyUpdates,
  markMatchesAsStale,
  mapStatus,
  FDMatch,
  MatchDocument,
} from "../pollMatchResults";
import axios from "axios";
import type { firestore } from "firebase-admin";

jest.mock("axios");
const mockedAxios = axios as jest.Mocked<typeof axios>;

// ---------------------------------------------------------------------------
// Firestore stub
// ---------------------------------------------------------------------------

type DocData = Record<string, unknown>;

interface StubDocRef {
  id: string;
  _updates: DocData[];
  update(data: DocData): void;
}

interface StubDoc {
  id: string;
  data(): DocData;
  ref: StubDocRef;
}

function makeDocRef(id: string): StubDocRef {
  return {
    id,
    _updates: [],
    update(data) { this._updates.push(data); },
  };
}

function makeDoc(id: string, data: DocData): StubDoc {
  const ref = makeDocRef(id);
  return { id, data: () => data, ref };
}

function makeFirestoreStub(docs: StubDoc[]) {
  return {
    collection(_name: string) {
      return {
        // For loadAllMatchDocs — full collection scan
        async get() {
          return {
            empty: docs.length === 0,
            forEach(cb: (doc: StubDoc) => void) { docs.forEach(cb); },
          };
        },
        // For markMatchesAsStale — where().get()
        where(_f: string, _op: string, _v: unknown) {
          return {
            async get() {
              return {
                empty: docs.length === 0,
                docs,
                forEach(cb: (doc: StubDoc) => void) { docs.forEach(cb); },
              };
            },
          };
        },
      };
    },
    batch() {
      const ops: Array<{ ref: StubDocRef; data: DocData }> = [];
      return {
        _ops: ops,
        update(ref: StubDocRef, data: DocData) { ops.push({ ref, data }); },
        async commit() {
          for (const op of ops) op.ref._updates.push(op.data);
          ops.length = 0;
        },
      };
    },
  } as unknown as firestore.Firestore;
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

function fdMatch(overrides: Partial<FDMatch> = {}): FDMatch {
  return {
    id: 1,
    utcDate: "2026-06-12T18:00:00Z",
    status: "SCHEDULED",
    matchday: 1,
    stage: "GROUP_STAGE",
    group: "GROUP_A",
    homeTeam: { id: 10, name: "Brazil", shortName: "Brazil", tla: "BRA" },
    awayTeam: { id: 20, name: "Mexico", shortName: "Mexico", tla: "MEX" },
    score: { fullTime: { home: null, away: null } },
    ...overrides,
  };
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
function makeDocMap(docs: StubDoc[]): any {
  const map = new Map();
  for (const doc of docs) {
    const data = doc.data() as unknown as MatchDocument;
    if (data.externalId) map.set(data.externalId, { ref: doc.ref, data });
  }
  return map;
}

// ---------------------------------------------------------------------------
// mapStatus
// ---------------------------------------------------------------------------

describe("mapStatus", () => {
  it.each([
    ["SCHEDULED", "SCHEDULED"],
    ["TIMED",     "SCHEDULED"],
    ["POSTPONED", "SCHEDULED"],
    ["CANCELLED", "SCHEDULED"],
    ["IN_PLAY",   "IN_PROGRESS"],
    ["PAUSED",    "IN_PROGRESS"],
    ["FINISHED",  "FINISHED"],
  ] as const)("maps %s → %s", (input, expected) => {
    expect(mapStatus(input as FDMatch["status"])).toBe(expected);
  });
});

// ---------------------------------------------------------------------------
// fetchAllMatches
// ---------------------------------------------------------------------------

describe("fetchAllMatches", () => {
  afterEach(() => jest.clearAllMocks());

  it("returns all matches from the API", async () => {
    const matches = [fdMatch({ id: 1 }), fdMatch({ id: 2, status: "FINISHED" })];
    mockedAxios.get.mockResolvedValueOnce({ data: { matches } });

    const result = await fetchAllMatches("test-token");

    expect(result).toHaveLength(2);
    expect(result[0].id).toBe(1);
    expect(result[1].id).toBe(2);
  });

  it("calls the correct URL with X-Auth-Token header and season param", async () => {
    mockedAxios.get.mockResolvedValueOnce({ data: { matches: [] } });

    await fetchAllMatches("my-token");

    expect(mockedAxios.get).toHaveBeenCalledWith(
      "https://api.football-data.org/v4/competitions/WC/matches",
      expect.objectContaining({
        headers: { "X-Auth-Token": "my-token" },
        params: { season: 2026 },
      })
    );
  });

  it("propagates network errors", async () => {
    mockedAxios.get.mockRejectedValueOnce(new Error("Network Error"));
    await expect(fetchAllMatches("key")).rejects.toThrow("Network Error");
  });
});

// ---------------------------------------------------------------------------
// loadAllMatchDocs
// ---------------------------------------------------------------------------

describe("loadAllMatchDocs", () => {
  it("returns empty map when collection is empty", async () => {
    const db = makeFirestoreStub([]);
    const result = await loadAllMatchDocs(db);
    expect(result.size).toBe(0);
  });

  it("indexes documents by externalId", async () => {
    const docs = [
      makeDoc("m1", { externalId: "101", status: "SCHEDULED", scoreHome: null, scoreAway: null, dataStale: false }),
      makeDoc("m2", { externalId: "202", status: "FINISHED",  scoreHome: 2,    scoreAway: 1,    dataStale: false }),
    ];
    const db = makeFirestoreStub(docs);

    const result = await loadAllMatchDocs(db);

    expect(result.size).toBe(2);
    expect(result.get("101")?.data.status).toBe("SCHEDULED");
    expect(result.get("202")?.data.scoreHome).toBe(2);
  });

  it("skips documents without externalId", async () => {
    const docs = [
      makeDoc("m1", { status: "SCHEDULED" }), // no externalId
    ];
    const db = makeFirestoreStub(docs);

    const result = await loadAllMatchDocs(db);
    expect(result.size).toBe(0);
  });
});

// ---------------------------------------------------------------------------
// applyUpdates
// ---------------------------------------------------------------------------

describe("applyUpdates", () => {
  it("updates a SCHEDULED match to FINISHED with scores", async () => {
    const doc = makeDoc("m1", { externalId: "1", status: "SCHEDULED", scoreHome: null, scoreAway: null, dataStale: false });
    const db = makeFirestoreStub([doc]);

    const { updated, skipped } = await applyUpdates(
      db,
      [fdMatch({ id: 1, status: "FINISHED", score: { fullTime: { home: 2, away: 1 } } })],
      makeDocMap([doc])
    );

    expect(updated).toBe(1);
    expect(skipped).toBe(0);
    expect(doc.ref._updates[0]).toMatchObject({ status: "FINISHED", scoreHome: 2, scoreAway: 1, dataStale: false });
  });

  it("updates a SCHEDULED match to IN_PROGRESS", async () => {
    const doc = makeDoc("m1", { externalId: "1", status: "SCHEDULED", scoreHome: null, scoreAway: null, dataStale: false });
    const db = makeFirestoreStub([doc]);

    await applyUpdates(
      db,
      [fdMatch({ id: 1, status: "IN_PLAY", score: { fullTime: { home: 1, away: 0 } } })],
      makeDocMap([doc])
    );

    expect(doc.ref._updates[0]).toMatchObject({ status: "IN_PROGRESS" });
  });

  it("skips match already FINISHED with same scores and not stale", async () => {
    const doc = makeDoc("m1", { externalId: "1", status: "FINISHED", scoreHome: 2, scoreAway: 0, dataStale: false });
    const db = makeFirestoreStub([doc]);

    const { updated, skipped } = await applyUpdates(
      db,
      [fdMatch({ id: 1, status: "FINISHED", score: { fullTime: { home: 2, away: 0 } } })],
      makeDocMap([doc])
    );

    expect(updated).toBe(0);
    expect(skipped).toBe(1);
    expect(doc.ref._updates).toHaveLength(0);
  });

  it("re-updates a stale FINISHED match to clear the stale flag", async () => {
    const doc = makeDoc("m1", { externalId: "1", status: "FINISHED", scoreHome: 1, scoreAway: 1, dataStale: true });
    const db = makeFirestoreStub([doc]);

    const { updated } = await applyUpdates(
      db,
      [fdMatch({ id: 1, status: "FINISHED", score: { fullTime: { home: 1, away: 1 } } })],
      makeDocMap([doc])
    );

    expect(updated).toBe(1);
    expect(doc.ref._updates[0]).toMatchObject({ dataStale: false });
  });

  it("skips fixtures with no matching Firestore document", async () => {
    const db = makeFirestoreStub([]);
    const { updated, skipped } = await applyUpdates(db, [fdMatch({ id: 999 })], new Map());
    expect(updated).toBe(0);
    expect(skipped).toBe(1);
  });

  it("handles null scores gracefully", async () => {
    const doc = makeDoc("m1", { externalId: "1", status: "IN_PLAY", scoreHome: null, scoreAway: null, dataStale: false });
    const db = makeFirestoreStub([doc]);

    await applyUpdates(
      db,
      [fdMatch({ id: 1, status: "FINISHED", score: { fullTime: { home: null, away: null } } })],
      makeDocMap([doc])
    );

    expect(doc.ref._updates[0]).toMatchObject({ scoreHome: null, scoreAway: null });
  });

  it("updates multiple matches in a single call", async () => {
    const doc1 = makeDoc("m1", { externalId: "1", status: "SCHEDULED", scoreHome: null, scoreAway: null, dataStale: false });
    const doc2 = makeDoc("m2", { externalId: "2", status: "IN_PLAY",   scoreHome: null, scoreAway: null, dataStale: false });
    const db = makeFirestoreStub([doc1, doc2]);

    const { updated } = await applyUpdates(
      db,
      [
        fdMatch({ id: 1, status: "FINISHED", score: { fullTime: { home: 2, away: 0 } } }),
        fdMatch({ id: 2, status: "FINISHED", score: { fullTime: { home: 1, away: 1 } } }),
      ],
      makeDocMap([doc1, doc2])
    );

    expect(updated).toBe(2);
    expect(doc1.ref._updates[0]).toMatchObject({ status: "FINISHED", scoreHome: 2, scoreAway: 0 });
    expect(doc2.ref._updates[0]).toMatchObject({ status: "FINISHED", scoreHome: 1, scoreAway: 1 });
  });
});

// ---------------------------------------------------------------------------
// markMatchesAsStale
// ---------------------------------------------------------------------------

describe("markMatchesAsStale", () => {
  it("marks all non-FINISHED matches as dataStale=true", async () => {
    const doc1 = makeDoc("m1", { status: "SCHEDULED",   dataStale: false });
    const doc2 = makeDoc("m2", { status: "IN_PROGRESS", dataStale: false });
    const db = makeFirestoreStub([doc1, doc2]);

    const count = await markMatchesAsStale(db);

    expect(count).toBe(2);
    expect(doc1.ref._updates[0]).toMatchObject({ dataStale: true });
    expect(doc2.ref._updates[0]).toMatchObject({ dataStale: true });
  });

  it("returns 0 when collection is empty", async () => {
    const db = makeFirestoreStub([]);
    expect(await markMatchesAsStale(db)).toBe(0);
  });

  it("only sets dataStale — does not touch scores", async () => {
    const doc = makeDoc("m1", { status: "IN_PROGRESS", scoreHome: 2, scoreAway: 1, dataStale: false });
    const db = makeFirestoreStub([doc]);

    await markMatchesAsStale(db);

    expect(doc.ref._updates[0]).toEqual({ dataStale: true });
    expect(doc.ref._updates[0]).not.toHaveProperty("scoreHome");
    expect(doc.ref._updates[0]).not.toHaveProperty("scoreAway");
  });
});
