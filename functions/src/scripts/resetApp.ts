/**
 * resetApp.ts — Apaga todos os usuários e grupos do app.
 *
 * Uso:
 *   cd functions
 *   npx ts-node src/scripts/resetApp.ts           # mostra o que será apagado (dry-run)
 *   npx ts-node src/scripts/resetApp.ts --confirm  # executa o reset
 *
 * O que é apagado:
 *   - Todos os usuários do Firebase Auth
 *   - Coleção `users` (perfis no Firestore)
 *   - Coleção `groups` (incluindo subcoleções members/ e bets/)
 *   - Coleção `loginAttempts`
 *
 * O que é preservado:
 *   - Coleção `matches` (partidas do torneio)
 *
 * Requer: functions/serviceAccount.json
 */

import * as admin from "firebase-admin";
import * as fs from "fs";
import * as path from "path";

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
const auth = admin.auth();

const DRY_RUN = !process.argv.includes("--confirm");

async function deleteAuthUsers(): Promise<number> {
  let total = 0;
  let pageToken: string | undefined;

  do {
    const result = await auth.listUsers(1000, pageToken);
    if (result.users.length === 0) break;

    const uids = result.users.map((u) => u.uid);
    total += uids.length;

    if (!DRY_RUN) {
      await auth.deleteUsers(uids);
    }

    pageToken = result.pageToken;
  } while (pageToken);

  return total;
}

async function deleteCollection(collectionPath: string): Promise<number> {
  const snap = await db.collection(collectionPath).get();
  if (snap.empty) return 0;

  let total = snap.size;

  if (!DRY_RUN) {
    // recursiveDelete apaga subcoleções automaticamente (members/, bets/ etc.)
    for (const doc of snap.docs) {
      await db.recursiveDelete(doc.ref);
    }
  } else {
    // No dry-run, conta subcoleções para informar o usuário
    for (const doc of snap.docs) {
      const subColls = await doc.ref.listCollections();
      for (const sub of subColls) {
        const subSnap = await sub.get();
        total += subSnap.size;
      }
    }
  }

  return total;
}

async function main() {
  console.log("\n" + (DRY_RUN ? "🔍 DRY-RUN — nada será apagado (use --confirm para executar)" : "⚠️  EXECUTANDO RESET — isso é irreversível!"));
  console.log("─".repeat(60));

  const [authCount, usersCount, groupsCount, attemptsCount] = await Promise.all([
    deleteAuthUsers(),
    deleteCollection("users"),
    deleteCollection("groups"),
    deleteCollection("loginAttempts"),
  ]);

  console.log(`\n  Firebase Auth   : ${authCount} usuário(s)`);
  console.log(`  Firestore users : ${usersCount} documento(s)`);
  console.log(`  Firestore groups: ${groupsCount} documento(s) (incluindo subcoleções)`);
  console.log(`  loginAttempts   : ${attemptsCount} documento(s)`);
  console.log("─".repeat(60));

  if (DRY_RUN) {
    console.log("\n✅ Dry-run concluído. Para executar de verdade:");
    console.log("   npx ts-node src/scripts/resetApp.ts --confirm\n");
  } else {
    console.log("\n✅ Reset concluído. O app está limpo.\n");
  }
}

main().catch((e) => {
  console.error("❌ Erro:", e);
  process.exit(1);
});
