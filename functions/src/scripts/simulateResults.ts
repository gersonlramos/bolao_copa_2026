/**
 * simulateResults.ts — Script para simular resultados de partidas no Firestore.
 *
 * Uso:
 *   cd functions
 *   npx ts-node src/scripts/simulateResults.ts              # lista partidas disponíveis
 *   npx ts-node src/scripts/simulateResults.ts --apply      # aplica os resultados definidos em RESULTS
 *   npx ts-node src/scripts/simulateResults.ts --reset      # volta partidas para SCHEDULED
 *
 * Requer: firebase login --reauth (Application Default Credentials)
 * O Cloud Function calculateScores dispara automaticamente após cada update.
 */

import * as admin from "firebase-admin";
import * as fs from "fs";
import * as path from "path";

// Carrega serviceAccount.json de functions/ (2 níveis acima de src/scripts/)
const serviceAccountPath = path.resolve(__dirname, "../../serviceAccount.json");
console.log(`🔑 Credenciais: ${serviceAccountPath}`);

if (!fs.existsSync(serviceAccountPath)) {
  console.error("\n❌ Arquivo não encontrado:", serviceAccountPath);
  console.error("   Baixe a chave em: Firebase Console → Configurações → Contas de serviço → Gerar nova chave");
  console.error("   Salve como: functions/serviceAccount.json\n");
  process.exit(1);
}

const serviceAccount = JSON.parse(fs.readFileSync(serviceAccountPath, "utf8"));
admin.initializeApp({ credential: admin.credential.cert(serviceAccount) });

const db = admin.firestore();

// ─── EDITE AQUI para definir os resultados que quer simular ─────────────────
// Use os TLAs das seleções (3 letras) para identificar as partidas.
const RESULTS: Array<{ home: string; away: string; scoreHome: number; scoreAway: number }> = [
  { home: "BRA", away: "MAR", scoreHome: 3, scoreAway: 1 },
  { home: "MEX", away: "RSA", scoreHome: 1, scoreAway: 1 },
  { home: "KOR", away: "CZE", scoreHome: 0, scoreAway: 2 },
  { home: "CAN", away: "BIH", scoreHome: 2, scoreAway: 0 },
  { home: "USA", away: "PAR", scoreHome: 1, scoreAway: 2 },
  { home: "CAT", away: "SUI", scoreHome: 1, scoreAway: 2 }
];
// ─────────────────────────────────────────────────────────────────────────────

async function listMatches() {
  console.log("\n📋 Partidas disponíveis no Firestore:\n");
  const snap = await db.collection("matches").orderBy("scheduledAt").limit(30).get();

  if (snap.empty) {
    console.log("Nenhuma partida encontrada. Execute seedMatches primeiro.");
    return;
  }

  for (const doc of snap.docs) {
    const d = doc.data();
    console.log(
      `  [${d.homeTeamTla ?? "???"}] ${d.homeTeam ?? "?"} x ${d.awayTeam ?? "?"} [${d.awayTeamTla ?? "???"}]` +
      `  | Status: ${d.status}` +
      (d.status === "FINISHED" ? `  | Placar: ${d.scoreHome}-${d.scoreAway}` : "") +
      `  | ID: ${doc.id}`
    );
  }
  console.log(`\n  Total mostrado: ${snap.size} (máx 30)`);
}

async function applyResults() {
  console.log("\n⚽ Aplicando resultados simulados...\n");

  for (const result of RESULTS) {
    // Busca partida pelo TLA de casa e fora
    const snap = await db.collection("matches")
      .where("homeTeamTla", "==", result.home)
      .where("awayTeamTla", "==", result.away)
      .limit(1)
      .get();

    if (snap.empty) {
      console.log(`  ❌ Partida não encontrada: ${result.home} x ${result.away}`);
      continue;
    }

    const matchDoc = snap.docs[0];
    const before = matchDoc.data();

    await matchDoc.ref.update({
      status: "FINISHED",
      scoreHome: result.scoreHome,
      scoreAway: result.scoreAway,
    });

    console.log(
      `  ✅ ${before.homeTeam} ${result.scoreHome} x ${result.scoreAway} ${before.awayTeam}` +
      `  (era: ${before.status})`
    );
    console.log(`     → calculateScores vai disparar automaticamente para: ${matchDoc.id}`);

    // Pequeno delay para não sobrecarregar o Firestore
    await new Promise(r => setTimeout(r, 300));
  }

  console.log("\n✔  Resultados aplicados! Aguarde ~10s para o calculateScores processar os pontos.\n");
  console.log("   Verifique o ranking no app ou rode:");
  console.log("   npx ts-node src/scripts/simulateResults.ts\n");
}

async function resetMatches() {
  console.log("\n🔄 Resetando partidas simuladas para SCHEDULED...\n");

  const affectedGroupIds = new Set<string>();

  for (const result of RESULTS) {
    const snap = await db.collection("matches")
      .where("homeTeamTla", "==", result.home)
      .where("awayTeamTla", "==", result.away)
      .limit(1)
      .get();

    if (snap.empty) {
      console.log(`  ❌ Não encontrada: ${result.home} x ${result.away}`);
      continue;
    }

    const matchDoc = snap.docs[0];
    const d = matchDoc.data();

    await matchDoc.ref.update({
      status: "SCHEDULED",
      scoreHome: admin.firestore.FieldValue.delete(),
      scoreAway: admin.firestore.FieldValue.delete(),
    });

    console.log(`  ↩  ${d.homeTeam} x ${d.awayTeam} → SCHEDULED`);

    // Reset bet scores for this match
    const betsSnap = await db.collection("bets")
      .where("matchId", "==", matchDoc.id)
      .get();

    if (!betsSnap.empty) {
      const betBatch = db.batch();
      for (const betDoc of betsSnap.docs) {
        const betData = betDoc.data();
        if (betData.groupId) affectedGroupIds.add(betData.groupId as string);
        betBatch.update(betDoc.ref, {
          score: admin.firestore.FieldValue.delete(),
          scoringCategory: admin.firestore.FieldValue.delete(),
          updatedAt: admin.firestore.FieldValue.serverTimestamp(),
        });
      }
      await betBatch.commit();
      console.log(`     → ${betsSnap.size} palpite(s) resetado(s)`);
    }

    await new Promise(r => setTimeout(r, 200));
  }

  // Recalculate totalScore for every affected group from remaining scored bets
  if (affectedGroupIds.size > 0) {
    console.log("\n  Recalculando pontuações dos grupos afetados...");
    for (const groupId of affectedGroupIds) {
      const allBetsSnap = await db.collection("bets")
        .where("groupId", "==", groupId)
        .get();

      const memberTotals = new Map<string, number>();
      for (const b of allBetsSnap.docs) {
        const bd = b.data();
        if (bd.score !== null && bd.score !== undefined) {
          memberTotals.set(bd.userId as string, (memberTotals.get(bd.userId as string) ?? 0) + (bd.score as number));
        }
      }

      const membersSnap = await db.collection("groups").doc(groupId)
        .collection("members").get();

      const memberBatch = db.batch();
      for (const memberDoc of membersSnap.docs) {
        memberBatch.update(memberDoc.ref, { totalScore: memberTotals.get(memberDoc.id) ?? 0 });
      }
      await memberBatch.commit();
      console.log(`     → Grupo ${groupId}: ${membersSnap.size} membro(s) atualizados`);
    }
  }

  console.log("\n✔  Reset concluído.\n");
}

async function resetAllScores() {
  console.log("\n🧹 Zerando todos os palpites e pontuações...\n");

  // 1. Remove score/scoringCategory de todos os palpites
  const betsSnap = await db.collection("bets").get();
  if (!betsSnap.empty) {
    const chunks: typeof betsSnap.docs[] = [];
    for (let i = 0; i < betsSnap.docs.length; i += 500) {
      chunks.push(betsSnap.docs.slice(i, i + 500));
    }
    for (const chunk of chunks) {
      const batch = db.batch();
      for (const doc of chunk) {
        batch.update(doc.ref, {
          score: admin.firestore.FieldValue.delete(),
          scoringCategory: admin.firestore.FieldValue.delete(),
          updatedAt: admin.firestore.FieldValue.serverTimestamp(),
        });
      }
      await batch.commit();
    }
    console.log(`  ✅ ${betsSnap.size} palpite(s) zerado(s)`);
  } else {
    console.log("  — Nenhum palpite encontrado");
  }

  // 2. Zera totalScore de todos os membros em todos os grupos
  const groupsSnap = await db.collection("groups").get();
  let totalMembers = 0;
  for (const groupDoc of groupsSnap.docs) {
    const membersSnap = await groupDoc.ref.collection("members").get();
    if (membersSnap.empty) continue;
    const batch = db.batch();
    for (const memberDoc of membersSnap.docs) {
      batch.update(memberDoc.ref, { totalScore: 0 });
    }
    await batch.commit();
    totalMembers += membersSnap.size;
    console.log(`  ✅ Grupo "${groupDoc.data().name}": ${membersSnap.size} membro(s) zerado(s)`);
  }

  console.log(`\n✔  Concluído — ${totalMembers} membro(s) com pontuação zerada.\n`);
}

// ─── Main ────────────────────────────────────────────────────────────────────
const arg = process.argv[2];

(async () => {
  try {
    if (arg === "--apply") {
      await applyResults();
    } else if (arg === "--reset") {
      await resetMatches();
    } else if (arg === "--reset-all") {
      await resetAllScores();
    } else {
      await listMatches();
    }
  } catch (e) {
    console.error("Erro:", e);
  } finally {
    process.exit(0);
  }
})();
