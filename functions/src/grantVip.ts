import * as functions from "firebase-functions/v2/https";
import * as admin from "firebase-admin";

const ADMIN_EMAIL = "gersonlopesr@gmail.com";

export const grantVip = functions.onCall(
  { region: "southamerica-east1" },
  async (request) => {
    // Only the admin can call this
    if (!request.auth) {
      throw new functions.HttpsError("unauthenticated", "Não autenticado.");
    }
    if (request.auth.token.email !== ADMIN_EMAIL) {
      throw new functions.HttpsError("permission-denied", "Acesso negado.");
    }

    const { userEmail } = request.data as { userEmail?: string };
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
