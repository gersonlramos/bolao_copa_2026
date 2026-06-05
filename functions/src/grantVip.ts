import * as functions from "firebase-functions/v2/https";
import * as admin from "firebase-admin";

export const grantVip = functions.onCall(
  { region: "southamerica-east1" },
  async (request) => {
    const { adminKey, userEmail } = request.data as {
      adminKey?: string;
      userEmail?: string;
    };

    const validKey = process.env.ADMIN_KEY;
    if (!adminKey || !validKey || adminKey !== validKey) {
      throw new functions.HttpsError("permission-denied", "Código de acesso inválido.");
    }

    if (!userEmail) {
      throw new functions.HttpsError("invalid-argument", "userEmail é obrigatório.");
    }

    let targetUser: admin.auth.UserRecord;
    try {
      targetUser = await admin.auth().getUserByEmail(userEmail);
    } catch {
      throw new functions.HttpsError("not-found", `Usuário não encontrado: ${userEmail}`);
    }

    await admin.firestore()
      .collection("users")
      .doc(targetUser.uid)
      .set({ isVip: true }, { merge: true });

    return { success: true, uid: targetUser.uid };
  }
);
