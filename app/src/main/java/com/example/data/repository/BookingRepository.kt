package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.BookingMessage
import com.google.firebase.firestore.FieldValue
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class BookingRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val collectionPath = "booking_messages"

    fun observeBookingMessages(groupId: String? = null): Flow<List<BookingMessage>> {
        val query = if (groupId != null && groupId != "ALL") {
            db.collection(collectionPath).whereEqualTo("groupId", groupId)
        } else {
            db.collection(collectionPath)
        }

        return query.snapshots()
            .map { snapshot ->
                snapshot.toObjects(BookingMessage::class.java)
                    .sortedByDescending { it.receivedAt ?: it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                if (error is Exception) {
                    handleFirestoreError(error, OperationType.LIST, collectionPath)
                }
                throw error
            }
    }

    /**
     * Accepts or declines a booking message.
     * Uses a Firestore transaction to ensure atomic concurrency control:
     * If the message is no longer "pending", the transaction aborts with an error
     * so duplicate or conflicting decisions cannot overwrite each other.
     */
    suspend fun decideBooking(
        messageId: String,
        decision: String, // "accepted" or "declined"
        decidedBy: String,
        decidedByName: String,
        note: String? = null
    ): Result<Unit> {
        return try {
            val docRef = db.collection(collectionPath).document(messageId)
            db.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                if (!snapshot.exists()) {
                    throw IllegalStateException("درخواست موجود نہیں ہے")
                }
                val currentStatus = snapshot.getString("status")
                if (currentStatus != "pending") {
                    throw IllegalStateException("یہ درخواست پہلے ہی ${if (currentStatus == "accepted") "قبول" else "مسترد"} ہو چکی ہے")
                }

                val updates = mutableMapOf<String, Any>(
                    "status" to decision,
                    "decidedBy" to decidedBy,
                    "decidedByName" to decidedByName,
                    "decidedAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (!note.isNullOrBlank()) {
                    updates["decisionNote"] = note
                }
                transaction.update(docRef, updates)
            }.await()

            // Also record in audit log
            recordAuditLog(
                actorId = decidedBy,
                actorName = decidedByName,
                action = if (decision == "accepted") "BOOKING_ACCEPTED" else "BOOKING_DECLINED",
                entityType = "booking_message",
                entityId = messageId,
                details = "فیصلہ: ${if (decision == "accepted") "قبول" else "مسترد"}"
            )

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "$collectionPath/$messageId")
            Result.failure(e)
        }
    }

    /**
     * Ingestion interface: Used by administrators or webhook integrations
     * to register received WhatsApp/SMS messages into the workers' inbox.
     */
    suspend fun ingestBookingMessage(
        groupId: String,
        originalMessage: String,
        source: String = "WhatsApp",
        bookingRef: String = "CB-${System.currentTimeMillis().toString().takeLast(6)}"
    ): Result<String> {
        return try {
            val payload = mapOf(
                "bookingRef" to bookingRef,
                "groupId" to groupId,
                "source" to source,
                "originalMessage" to originalMessage,
                "receivedAt" to FieldValue.serverTimestamp(),
                "status" to "pending",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            val added = db.collection(collectionPath).add(payload).await()
            Result.success(added.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, collectionPath)
            Result.failure(e)
        }
    }

    private suspend fun recordAuditLog(
        actorId: String,
        actorName: String,
        action: String,
        entityType: String,
        entityId: String,
        details: String
    ) {
        try {
            db.collection("audit_logs").add(
                mapOf(
                    "actorId" to actorId,
                    "actorEmail" to "",
                    "actorName" to actorName,
                    "action" to action,
                    "entityType" to entityType,
                    "entityId" to entityId,
                    "details" to details,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: Exception) {
            // Log but don't break main flow
            handleFirestoreError(e, OperationType.CREATE, "audit_logs")
        }
    }
}
