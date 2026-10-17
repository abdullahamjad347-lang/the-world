package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.AttendanceRecord
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class AttendanceRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val collectionPath = "attendance_records"

    fun observeAttendanceForDate(groupId: String, dutyDate: String): Flow<List<AttendanceRecord>> {
        return db.collection(collectionPath)
            .whereEqualTo("groupId", groupId)
            .whereEqualTo("dutyDate", dutyDate)
            .snapshots()
            .map { snapshot -> snapshot.toObjects(AttendanceRecord::class.java) }
            .catch { error ->
                if (error is Exception) {
                    handleFirestoreError(error, OperationType.LIST, collectionPath)
                }
                throw error
            }
    }

    fun observeAllAttendance(groupId: String? = null): Flow<List<AttendanceRecord>> {
        val query = if (groupId != null && groupId != "ALL") {
            db.collection(collectionPath).whereEqualTo("groupId", groupId)
        } else {
            db.collection(collectionPath)
        }

        return query.snapshots()
            .map { snapshot -> snapshot.toObjects(AttendanceRecord::class.java) }
            .catch { error ->
                if (error is Exception) {
                    handleFirestoreError(error, OperationType.LIST, collectionPath)
                }
                throw error
            }
    }

    suspend fun saveOrUpdateRecord(record: AttendanceRecord): Result<String> {
        return try {
            val collectionRef = db.collection(collectionPath)
            if (record.id.isNotBlank()) {
                val docRef = collectionRef.document(record.id)
                val updatePayload = mapOf(
                    "arrivalTime" to record.arrivalTime,
                    "departureTime" to record.departureTime,
                    "status" to record.status,
                    "notes" to record.notes,
                    "recordedBy" to record.recordedBy,
                    "recordedByName" to record.recordedByName,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updatePayload).await()
                Result.success(record.id)
            } else {
                // Check if existing record exists for this worker, group and date
                val existing = collectionRef
                    .whereEqualTo("groupId", record.groupId)
                    .whereEqualTo("dutyDate", record.dutyDate)
                    .whereEqualTo("workerName", record.workerName)
                    .get()
                    .await()

                if (!existing.isEmpty) {
                    val existingDoc = existing.documents.first()
                    val docRef = collectionRef.document(existingDoc.id)
                    val updatePayload = mapOf(
                        "arrivalTime" to record.arrivalTime,
                        "departureTime" to record.departureTime,
                        "status" to record.status,
                        "notes" to record.notes,
                        "recordedBy" to record.recordedBy,
                        "recordedByName" to record.recordedByName,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    docRef.update(updatePayload).await()
                    Result.success(existingDoc.id)
                } else {
                    val payload = mapOf(
                        "groupId" to record.groupId,
                        "dutyDate" to record.dutyDate,
                        "dayOfWeek" to record.dayOfWeek,
                        "workerName" to record.workerName,
                        "isCoordinator" to record.isCoordinator,
                        "arrivalTime" to record.arrivalTime,
                        "departureTime" to record.departureTime,
                        "status" to record.status,
                        "notes" to record.notes,
                        "recordedBy" to record.recordedBy,
                        "recordedByName" to record.recordedByName,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    val added = collectionRef.add(payload).await()
                    Result.success(added.id)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, collectionPath)
            Result.failure(e)
        }
    }
}
