package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.PredefinedSchedules
import com.example.data.model.WeeklySchedule
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class ScheduleRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val collectionPath = "weekly_schedules"

    fun observeSchedule(groupId: String, dayOfWeek: Int): Flow<List<WeeklySchedule>> {
        return db.collection(collectionPath)
            .whereEqualTo("groupId", groupId)
            .whereEqualTo("dayOfWeek", dayOfWeek)
            .snapshots()
            .map { snapshot ->
                val list = snapshot.toObjects(WeeklySchedule::class.java)
                if (list.isNotEmpty()) {
                    list.sortedBy { it.sortOrder }
                } else {
                    // Fallback to in-memory predefined schedule if database collection not yet seeded
                    PredefinedSchedules.getStaffForDay(dayOfWeek).mapIndexed { index, staff ->
                        WeeklySchedule(
                            id = "predefined_${groupId}_${dayOfWeek}_$index",
                            groupId = groupId,
                            dayOfWeek = dayOfWeek,
                            dayNameUrdu = PredefinedSchedules.getDayNameUrdu(dayOfWeek),
                            sortOrder = index + 1,
                            workerName = staff.name,
                            isCoordinator = staff.isCoordinator,
                            coordinatorLabel = staff.coordinatorLabel,
                            active = true
                        )
                    }
                }
            }
            .catch { error ->
                if (error is Exception) {
                    handleFirestoreError(error, OperationType.LIST, collectionPath)
                }
                throw error
            }
    }

    /**
     * Seeds the initial predefined schedules into Firestore if not already populated.
     */
    suspend fun seedInitialSchedulesIfEmpty() {
        try {
            val existing = db.collection(collectionPath).limit(1).get().await()
            if (existing.isEmpty) {
                val batch = db.batch()
                val groups = listOf("A", "B", "C")
                for (group in groups) {
                    for (day in 1..7) {
                        val staffList = PredefinedSchedules.getStaffForDay(day)
                        staffList.forEachIndexed { index, staff ->
                            val docRef = db.collection(collectionPath).document()
                            batch.set(
                                docRef,
                                mapOf(
                                    "groupId" to group,
                                    "dayOfWeek" to day,
                                    "dayNameUrdu" to PredefinedSchedules.getDayNameUrdu(day),
                                    "sortOrder" to (index + 1),
                                    "workerName" to staff.name,
                                    "isCoordinator" to staff.isCoordinator,
                                    "coordinatorLabel" to staff.coordinatorLabel,
                                    "active" to true,
                                    "updatedAt" to FieldValue.serverTimestamp()
                                )
                            )
                        }
                    }
                }
                batch.commit().await()
            }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, collectionPath)
        }
    }
}
