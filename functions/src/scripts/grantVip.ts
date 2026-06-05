/**
 * grantVip.ts — Libera ou remove o VIP de um usuário pelo e-mail.
 *
 * Uso:
 *   cd functions
 *   npx ts-node src/scripts/grantVip.ts --email usuario@email.com          # libera VIP
 *   npx ts-node src/scripts/grantVip.ts --email usuario@email.com --remove  # remove VIP
 *
 * Requer: functions/serviceAccount.json
 */

import * as admin from "firebase-admin";
import * as fs from "fs";
import * as path from "path";

const serviceAccountPath = path.resolve(__dirname, "../../serviceAccount.json");
if (!fs.existsSync(serviceAccountPath)) {
  console.error("❌ serviceAccount.json não encontrado em functions/");
  process.exit(1);
}

admin.initializeApp({
  credential: admin.credential.cert(
    JSON.parse(fs.readFileSync(serviceAccountPath, "utf8"))
  ),
});

const args = process.argv.slice(2);
const emailIdx = args.indexOf("--email");
const remove = args.includes("--remove");

if (emailIdx === -1 || !args[emailIdx + 1]) {
  console.error("Uso: npx ts-node grantVip.ts --email usuario@email.com [--remove]");
  process.exit(1);
}

const email = args[emailIdx + 1].trim();

async function main() {
  let user: admin.auth.UserRecord;
  try {
    user = await admin.auth().getUserByEmail(email);
  } catch {
    console.error(`❌ Usuário não encontrado: ${email}`);
    process.exit(1);
  }

  await admin.firestore()
    .collection("users")
    .doc(user.uid)
    .set({ isVip: !remove }, { merge: true });

  if (remove) {
    console.log(`✅ VIP removido de ${email} (uid: ${user.uid})`);
  } else {
    console.log(`✅ VIP liberado para ${email} (uid: ${user.uid})`);
  }
}

main().catch((e) => { console.error("❌ Erro:", e); process.exit(1); });
