import * as functions from "firebase-functions/v2/https";
import * as admin from "firebase-admin";

/**
 * Called by the Android app after a successful Google Play purchase.
 * Validates the purchase token and grants VIP status.
 *
 * TODO: add real Google Play Developer API token validation before production.
 */
export const verifyPurchase = functions.onCall(
  { region: "southamerica-east1" },
  async (request) => {
    const uid = request.auth?.uid;
    if (!uid) {
      throw new functions.HttpsError("unauthenticated", "Usuário não autenticado.");
    }

    const { purchaseToken, productId } = request.data as {
      purchaseToken?: string;
      productId?: string;
    };

    if (!purchaseToken || !productId) {
      throw new functions.HttpsError("invalid-argument", "purchaseToken e productId são obrigatórios.");
    }

    if (productId !== "vip_one_time") {
      throw new functions.HttpsError("invalid-argument", "Produto desconhecido.");
    }

    // TODO: validate purchaseToken against Google Play Developer API here.
    // For now, trust the client (acceptable only for development/testing).

    await admin.firestore().collection("users").doc(uid).set(
      { isVip: true },
      { merge: true }
    );

    return { success: true };
  }
);
