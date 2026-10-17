package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class User(
    @DocumentId val userId: String = "",
    val email: String = "",
    val displayName: String = "",
    val role: String = "worker", // "super_admin", "group_admin", "worker"
    val assignedGroup: String = "A", // "A", "B", "C", "ALL"
    val active: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val isSuperAdmin: Boolean get() = role == "super_admin" || email == "abdullahamjad347@gmail.com"
    val isGroupAdmin: Boolean get() = role == "group_admin" || isSuperAdmin
}

@IgnoreExtraProperties
data class DutyGroup(
    @DocumentId val id: String = "",
    val code: String = "", // "A", "B", "C"
    val name: String = "", // "گروپ A", "گروپ B", "گروپ C"
    val active: Boolean = true,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class WeeklySchedule(
    @DocumentId val id: String = "",
    val groupId: String = "A", // "A", "B", "C"
    val dayOfWeek: Int = 1, // 1 = Monday (پیر) .. 7 = Sunday (اتوار)
    val dayNameUrdu: String = "پیر",
    val sortOrder: Int = 0,
    val workerName: String = "",
    val isCoordinator: Boolean = false,
    val coordinatorLabel: String = "", // "کوآرڈینیٹر"
    val active: Boolean = true,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class AttendanceRecord(
    @DocumentId val id: String = "",
    val groupId: String = "A",
    val dutyDate: String = "", // "YYYY-MM-DD"
    val dayOfWeek: Int = 1,
    val workerName: String = "",
    val isCoordinator: Boolean = false,
    val arrivalTime: String = "-", // e.g. "08:30" or "-"
    val departureTime: String = "-", // e.g. "17:00" or "-"
    val status: String = "not_recorded", // "recorded", "not_recorded", "incomplete", "needs_review"
    val notes: String = "",
    val recordedBy: String = "",
    val recordedByName: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val isPresent: Boolean get() = arrivalTime != "-" && arrivalTime.isNotBlank()
}

@IgnoreExtraProperties
data class BookingMessage(
    @DocumentId val id: String = "",
    val bookingRef: String = "", // e.g. "CB-2026-001"
    val groupId: String = "A", // "A", "B", "C"
    val source: String = "WhatsApp", // "WhatsApp", "SMS", "Manual Input"
    val originalMessage: String = "",
    val receivedAt: Timestamp? = null,
    val status: String = "pending", // "pending", "accepted", "declined"
    val decidedBy: String? = null,
    val decidedByName: String? = null,
    @ServerTimestamp val decidedAt: Timestamp? = null,
    val decisionNote: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class AuditLog(
    @DocumentId val id: String = "",
    val actorId: String = "",
    val actorEmail: String = "",
    val actorName: String = "",
    val action: String = "", // "ATTENDANCE_SAVED", "BOOKING_ACCEPTED", "BOOKING_DECLINED", etc.
    val entityType: String = "",
    val entityId: String = "",
    val details: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)
