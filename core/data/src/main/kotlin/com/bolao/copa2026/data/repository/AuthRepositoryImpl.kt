package com.bolao.copa2026.data.repository

import com.bolao.copa2026.data.mapper.toUser
import com.bolao.copa2026.data.source.FirestoreAuthDataSource
import com.bolao.copa2026.domain.model.AppError
import com.bolao.copa2026.domain.model.LoginBlockStatus
import com.bolao.copa2026.domain.model.User
import com.bolao.copa2026.domain.repository.AuthRepository
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.time.Instant
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val authDataSource: FirestoreAuthDataSource
) : AuthRepository {

    override suspend fun register(
        displayName: String,
        email: String,
        password: String
    ): Result<User> = runCatching {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        val user = result.user!!
        
        // Update profile
        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(displayName)
            .build()
        user.updateProfile(profileUpdates).await()
        
        val uid = user.uid
        authDataSource.saveUserDocument(uid, displayName, email)
        User(uid, displayName, email)
    }.mapError()

    override suspend fun login(email: String, password: String): Result<User> = runCatching {
        // Check block status before attempting auth
        val blockStatus = checkLoginBlock(email)
        if (blockStatus is LoginBlockStatus.Blocked) {
            throw com.bolao.copa2026.domain.model.AppError.AccountBlocked(blockStatus.blockedUntil)
        }
        val result = auth.signInWithEmailAndPassword(email, password).await()
        val uid = result.user!!.uid
        val doc = authDataSource.getUserDocument(uid)
        User(
            id = uid,
            displayName = doc?.getString("displayName") ?: "",
            email = email
        )
    }.mapError()

    override suspend fun loginWithGoogle(idToken: String): Result<User> = runCatching {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val result = auth.signInWithCredential(credential).await()
        val uid = result.user!!.uid
        val doc = authDataSource.getUserDocument(uid)
        
        // If it's a new user, save their document
        if (doc == null) {
            val displayName = result.user!!.displayName ?: ""
            val email = result.user!!.email ?: ""
            authDataSource.saveUserDocument(uid, displayName, email)
            User(uid, displayName, email)
        } else {
            User(
                id = uid,
                displayName = doc.getString("displayName") ?: "",
                email = result.user!!.email ?: ""
            )
        }
    }.mapError()

    override suspend fun logout() {
        auth.signOut()
    }

    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> = runCatching {
        auth.sendPasswordResetEmail(email).await()
        Unit
    }.mapError()

    override suspend fun updatePassword(currentPassword: String, newPassword: String): Result<Unit> = runCatching {
        val user = auth.currentUser ?: throw AppError.Unknown(Exception("Usuário não autenticado"))
        val email = user.email ?: throw AppError.Unknown(Exception("E-mail não encontrado"))
        val credential = EmailAuthProvider.getCredential(email, currentPassword)
        user.reauthenticate(credential).await()
        user.updatePassword(newPassword).await()
        Unit
    }.mapError()

    override suspend fun updateDisplayName(name: String): Result<Unit> = runCatching {
        val user = auth.currentUser ?: throw AppError.Unknown(Exception("Usuário não autenticado"))
        
        // Update Firebase Auth profile
        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(name)
            .build()
        user.updateProfile(profileUpdates).await()
        
        // Update Firestore document
        authDataSource.saveUserDocument(user.uid, name, user.email ?: "")
        Unit
    }.mapError()

    override fun isEmailPasswordUser(): Boolean {
        val user = auth.currentUser ?: return false
        return user.providerData.any { it.providerId == EmailAuthProvider.PROVIDER_ID }
    }

    override fun currentUser(): Flow<User?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { fa ->
            val u = fa.currentUser
            trySend(u?.let { User(it.uid, it.displayName ?: "", it.email ?: "") })
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    override suspend fun checkLoginBlock(email: String): LoginBlockStatus {
        val emailHash = email.lowercase().trim().hashCode().toString()
        val dto = authDataSource.getLoginAttempts(emailHash) ?: return LoginBlockStatus.NotBlocked
        val blocked = dto.blockedUntil
        return if (blocked != null && blocked.isAfter(Instant.now())) {
            LoginBlockStatus.Blocked(blocked)
        } else {
            LoginBlockStatus.NotBlocked
        }
    }

    private fun <T> Result<T>.mapError(): Result<T> = this.recoverCatching { e ->
        android.util.Log.e("AuthRepository", "Error during auth operation", e)
        val appError: AppError = when (e) {
            is AppError -> e
            is FirebaseAuthUserCollisionException -> AppError.EmailAlreadyRegistered
            is FirebaseAuthInvalidCredentialsException -> AppError.InvalidCredentials
            else -> AppError.Unknown(e)
        }
        throw RuntimeException(appError.toString(), e)
    }
}
