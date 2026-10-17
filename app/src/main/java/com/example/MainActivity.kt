package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.example.data.model.AttendanceRecord
import com.example.data.model.BookingMessage
import com.example.data.model.User
import com.example.data.repository.AttendanceRepository
import com.example.data.repository.BookingRepository
import com.example.data.repository.ScheduleRepository
import com.example.data.repository.UserRepository
import com.example.ui.screens.*
import com.example.ui.theme.KarkunanTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

enum class AppScreen {
    DASHBOARD,
    ATTENDANCE,
    DUTY_LIST,
    BOOKINGS,
    REPORTS,
    STAFF
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val databaseId = getString(R.string.firestore_database_id)
        val firestore = FirebaseFirestore.getInstance(databaseId)

        val attendanceRepo = AttendanceRepository(firestore)
        val bookingRepo = BookingRepository(firestore)
        val scheduleRepo = ScheduleRepository(firestore)
        val userRepo = UserRepository(firestore)

        // Seed initial schedules & sample incoming booking messages if database is fresh
        lifecycleScope.launch {
            scheduleRepo.seedInitialSchedulesIfEmpty()
            seedSampleBookingsIfEmpty(bookingRepo)
        }

        setContent {
            KarkunanTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainAppContent(
                        attendanceRepo = attendanceRepo,
                        bookingRepo = bookingRepo,
                        scheduleRepo = scheduleRepo,
                        userRepo = userRepo
                    )
                }
            }
        }
    }

    private suspend fun seedSampleBookingsIfEmpty(bookingRepo: BookingRepository) {
        try {
            // Provide initial pending booking messages to populate the inbox for workers
            bookingRepo.ingestBookingMessage(
                groupId = "A",
                originalMessage = "السلام علیکم، کل صبح 08:30 بجے مین کیمپس سے ایئرپورٹ کے لیے 15 افراد کی کوسٹر درکار ہے۔ رابطہ: 0300-1234567",
                source = "WhatsApp",
                bookingRef = "CB-10024"
            )
            bookingRepo.ingestBookingMessage(
                groupId = "B",
                originalMessage = "برائے مہربانی شام 05:00 بجے سیکیورٹی شفٹ چینج کے لیے کوسٹر مہیا کی جائے۔ پوائنٹ B سے پوائنٹ C۔",
                source = "SMS",
                bookingRef = "CB-10025"
            )
        } catch (_: Exception) {}
    }
}

@Composable
fun MainAppContent(
    attendanceRepo: AttendanceRepository,
    bookingRepo: BookingRepository,
    scheduleRepo: ScheduleRepository,
    userRepo: UserRepository
) {
    val coroutineScope = rememberCoroutineScope()
    var currentFirebaseUser by remember { mutableStateOf(FirebaseAuth.getInstance().currentUser) }
    var activeAppUser by remember { mutableStateOf<User?>(null) }

    // Monitor Firebase Auth state
    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentFirebaseUser = auth.currentUser
        }
        FirebaseAuth.getInstance().addAuthStateListener(listener)
        onDispose {
            FirebaseAuth.getInstance().removeAuthStateListener(listener)
        }
    }

    // Sync activeAppUser with Firestore profile when signed in with Google
    LaunchedEffect(currentFirebaseUser) {
        val fbUser = currentFirebaseUser
        if (fbUser != null) {
            val user = userRepo.getOrCreateUser(
                userId = fbUser.uid,
                email = fbUser.email ?: "",
                displayName = fbUser.displayName ?: "کارکن"
            )
            activeAppUser = user
        } else if (activeAppUser?.userId?.startsWith("demo_") != true) {
            activeAppUser = null
        }
    }

    if (activeAppUser == null) {
        AuthScreen(
            onAuthSuccess = { loggedInUser ->
                activeAppUser = loggedInUser
            },
            onDemoLogin = { demoUser ->
                activeAppUser = demoUser
            }
        )
    } else {
        val user = activeAppUser!!
        var currentScreen by remember { mutableStateOf(AppScreen.DASHBOARD) }
        var activeGroupId by remember { mutableStateOf(if (user.assignedGroup == "ALL") "A" else user.assignedGroup) }

        // Date calculation for today
        val calendar = remember { Calendar.getInstance() }
        val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH) }
        val todayDate = remember { dateFormat.format(calendar.time) }
        val todayDayOfWeek = remember {
            val dow = calendar.get(Calendar.DAY_OF_WEEK)
            if (dow == Calendar.SUNDAY) 7 else dow - 1
        }

        // Live observations from repositories
        val allAttendance by attendanceRepo.observeAllAttendance(
            groupId = if (user.isSuperAdmin) null else user.assignedGroup
        ).collectAsStateWithLifecycle(initialValue = emptyList())

        val allBookings by bookingRepo.observeBookingMessages(
            groupId = if (user.isSuperAdmin) null else user.assignedGroup
        ).collectAsStateWithLifecycle(initialValue = emptyList())

        val allUsers by userRepo.observeAllUsers().collectAsStateWithLifecycle(initialValue = emptyList())

        // BackHandler for sub-screens
        if (currentScreen != AppScreen.DASHBOARD) {
            BackHandler {
                currentScreen = AppScreen.DASHBOARD
            }
        }

        when (currentScreen) {
            AppScreen.DASHBOARD -> {
                DashboardScreen(
                    currentUser = user,
                    allAttendance = allAttendance,
                    allBookings = allBookings,
                    allUsers = allUsers,
                    todayDate = todayDate,
                    todayDayOfWeek = todayDayOfWeek,
                    onNavigateToAttendance = { groupId ->
                        activeGroupId = groupId
                        currentScreen = AppScreen.ATTENDANCE
                    },
                    onNavigateToDutyList = { groupId ->
                        activeGroupId = groupId
                        currentScreen = AppScreen.DUTY_LIST
                    },
                    onNavigateToBookings = {
                        currentScreen = AppScreen.BOOKINGS
                    },
                    onNavigateToReports = {
                        currentScreen = AppScreen.REPORTS
                    },
                    onNavigateToStaff = {
                        currentScreen = AppScreen.STAFF
                    },
                    onAcceptBooking = { booking ->
                        coroutineScope.launch {
                            bookingRepo.decideBooking(
                                messageId = booking.id,
                                decision = "accepted",
                                decidedBy = user.userId,
                                decidedByName = user.displayName
                            )
                        }
                    },
                    onDeclineBooking = { booking ->
                        coroutineScope.launch {
                            bookingRepo.decideBooking(
                                messageId = booking.id,
                                decision = "declined",
                                decidedBy = user.userId,
                                decidedByName = user.displayName
                            )
                        }
                    },
                    onSignOut = {
                        FirebaseAuth.getInstance().signOut()
                        activeAppUser = null
                    }
                )
            }

            AppScreen.ATTENDANCE -> {
                AttendanceScreen(
                    currentUser = user,
                    initialGroupId = activeGroupId,
                    existingRecords = allAttendance,
                    onSaveRecords = { recordsToSave ->
                        coroutineScope.launch {
                            recordsToSave.forEach { rec ->
                                attendanceRepo.saveOrUpdateRecord(rec)
                            }
                        }
                    },
                    onBackClick = { currentScreen = AppScreen.DASHBOARD },
                    onNavigateToReports = { currentScreen = AppScreen.REPORTS }
                )
            }

            AppScreen.DUTY_LIST -> {
                DutyScheduleScreen(
                    currentUser = user,
                    initialGroupId = activeGroupId,
                    onBackClick = { currentScreen = AppScreen.DASHBOARD }
                )
            }

            AppScreen.BOOKINGS -> {
                BookingInboxScreen(
                    currentUser = user,
                    bookingMessages = allBookings,
                    onAcceptBooking = { booking, note ->
                        coroutineScope.launch {
                            bookingRepo.decideBooking(
                                messageId = booking.id,
                                decision = "accepted",
                                decidedBy = user.userId,
                                decidedByName = user.displayName,
                                note = note
                            )
                        }
                    },
                    onDeclineBooking = { booking, note ->
                        coroutineScope.launch {
                            bookingRepo.decideBooking(
                                messageId = booking.id,
                                decision = "declined",
                                decidedBy = user.userId,
                                decidedByName = user.displayName,
                                note = note
                            )
                        }
                    },
                    onIngestBooking = { groupId, messageText, source ->
                        coroutineScope.launch {
                            bookingRepo.ingestBookingMessage(
                                groupId = groupId,
                                originalMessage = messageText,
                                source = source
                            )
                        }
                    },
                    onBackClick = { currentScreen = AppScreen.DASHBOARD }
                )
            }

            AppScreen.REPORTS -> {
                ReportsScreen(
                    currentUser = user,
                    allAttendance = allAttendance,
                    allBookings = allBookings,
                    onBackClick = { currentScreen = AppScreen.DASHBOARD }
                )
            }

            AppScreen.STAFF -> {
                StaffManagementScreen(
                    currentUser = user,
                    allUsers = allUsers,
                    onUpdateUser = { targetUserId, newGroup, newRole ->
                        coroutineScope.launch {
                            userRepo.updateUserGroupOrRole(targetUserId, newGroup, newRole)
                        }
                    },
                    onBackClick = { currentScreen = AppScreen.DASHBOARD }
                )
            }
        }
    }
}
