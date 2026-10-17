package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.AttendanceRecord
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AttendanceRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun saveOrUpdateRecord_authenticatedUser_createsRecordSuccessfully() {
    runBlocking {
      val uid = signInTestUser("user@karkunan.pk")
      val repository = AttendanceRepository(firestore)

      val record = AttendanceRecord(
        groupId = "A",
        dutyDate = "2026-10-17",
        dayOfWeek = 6,
        workerName = "محمد زین",
        isCoordinator = true,
        arrivalTime = "08:15",
        departureTime = "16:45",
        status = "recorded",
        notes = "بروقت حاضری",
        recordedBy = uid,
        recordedByName = "ٹیسٹ صارف"
      )

      val saveResult = withTimeout(5000L) {
        repository.saveOrUpdateRecord(record)
      }

      assertTrue(saveResult.isSuccess)
      val recordId = saveResult.getOrThrow()
      assertTrue(recordId.isNotEmpty())
    }
  }

  @Test
  fun observeAttendance_unauthenticatedUser_failsWithPermissionDenied() {
    runBlocking {
      auth.signOut()
      val repository = AttendanceRepository(firestore)

      try {
        withTimeout(3000L) {
          repository.observeAttendanceForDate("A", "2026-10-17").first()
        }
        throw AssertionError("Expected FirebaseFirestoreException but succeeded")
      } catch (e: Throwable) {
        val firestoreEx = (e as? FirebaseFirestoreException)
          ?: (e.cause as? FirebaseFirestoreException)
          ?: (e.cause?.cause as? FirebaseFirestoreException)
        assertTrue(firestoreEx != null)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
      }
    }
  }
}
