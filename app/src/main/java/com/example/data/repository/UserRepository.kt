package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.User
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class UserRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val collectionPath = "users"

    fun observeUser(userId: String): Flow<User?> {
        return db.collection(collectionPath).document(userId)
            .snapshots()
            .map { it.toObject(User::class.java) }
            .catch { error ->
                if (error is Exception) {
                    handleFirestoreError(error, OperationType.GET, "$collectionPath/$userId")
                }
                throw error
            }
    }

    fun observeAllUsers(): Flow<List<User>> {
        return db.collection(collectionPath)
            .snapshots()
            .map { it.toObjects(User::class.java) }
            .catch { error ->
                if (error is Exception) {
                    handleFirestoreError(error, OperationType.LIST, collectionPath)
                }
                throw error
            }
    }

    suspend fun getOrCreateUser(userId: String, email: String, displayName: String): User {
        val docRef = db.collection(collectionPath).document(userId)
        val snapshot = docRef.get().await()
        if (snapshot.exists()) {
            return snapshot.toObject(User::class.java) ?: User(userId = userId, email = email, displayName = displayName)
        }

        val isSuperAdminEmail = email.equals("abdullahamjad347@gmail.com", ignoreCase = true)
        val initialRole = if (isSuperAdminEmail) "super_admin" else "worker"
        val initialGroup = if (isSuperAdminEmail) "ALL" else "A"

        val payload = mapOf(
            "userId" to userId,
            "email" to email,
            "displayName" to displayName.ifBlank { "کارکن" },
            "role" to initialRole,
            "assignedGroup" to initialGroup,
            "active" to true,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        try {
            docRef.set(payload).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "$collectionPath/$userId")
        }

        return User(
            userId = userId,
            email = email,
            displayName = displayName.ifBlank { "کارکن" },
            role = initialRole,
            assignedGroup = initialGroup,
            active = true
        )
    }

    suspend fun updateUserGroupOrRole(targetUserId: String, newGroup: String, newRole: String): Result<Unit> {
        return try {
            val docRef = db.collection(collectionPath).document(targetUserId)
            docRef.update(
                mapOf(
                    "assignedGroup" to newGroup,
                    "role" to newRole,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "$collectionPath/$targetUserId")
            Result.failure(e)
        }
    }
}
