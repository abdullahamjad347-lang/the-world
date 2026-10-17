package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.components.StatusBadge
import com.example.ui.components.UrduRtlProvider
import com.example.ui.components.UrduTopAppBar
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(
    currentUser: User,
    allAttendance: List<AttendanceRecord>,
    allBookings: List<BookingMessage>,
    allUsers: List<User>,
    todayDate: String,
    todayDayOfWeek: Int,
    onNavigateToAttendance: (String) -> Unit, // passes groupId
    onNavigateToDutyList: (String) -> Unit,
    onNavigateToBookings: () -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToStaff: () -> Unit,
    onAcceptBooking: (BookingMessage) -> Unit,
    onDeclineBooking: (BookingMessage) -> Unit,
    onSignOut: () -> Unit
) {
    val todayRecords = remember(allAttendance, todayDate) {
        allAttendance.filter { it.dutyDate == todayDate }
    }

    val todayScheduledStaff = remember(todayDayOfWeek) {
        PredefinedSchedules.getStaffForDay(todayDayOfWeek)
    }

    val recordedCount = remember(todayRecords) {
        todayRecords.count { it.arrivalTime != "-" }
    }

    val missingAttendanceCount = remember(todayScheduledStaff, recordedCount) {
        maxOf(0, todayScheduledStaff.size - recordedCount)
    }

    val pendingBookings = remember(allBookings) {
        allBookings.filter { it.status == "pending" }
    }
    val acceptedBookings = remember(allBookings) {
        allBookings.filter { it.status == "accepted" }
    }
    val declinedBookings = remember(allBookings) {
        allBookings.filter { it.status == "declined" }
    }

    val workerRelevantBookings = remember(allBookings, currentUser) {
        allBookings.filter { it.groupId == currentUser.assignedGroup || currentUser.assignedGroup == "ALL" }
    }

    UrduRtlProvider {
        Scaffold(
            topBar = {
                UrduTopAppBar(
                    title = "ڈیش بورڈ",
                    subtitle = "خوش آمدید: ${currentUser.displayName} (${when (currentUser.role) {
                        "super_admin" -> "سپر ایڈمن"
                        "group_admin" -> "گروپ ایڈمن (${currentUser.assignedGroup})"
                        else -> "کارکن (${currentUser.assignedGroup})"
                    }})",
                    actions = {
                        IconButton(onClick = onSignOut) {
                            Icon(
                                imageVector = Icons.Default.ExitToApp,
                                contentDescription = "لاگ آؤٹ",
                                tint = Color.White
                            )
                        }
                    }
                )
            },
            containerColor = MainBackground
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    // Date banner
                    Card(
                        colors = CardDefaults.cardColors(containerColor = LightMint),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "آج کی تاریخ اور دن:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SecondaryText
                                )
                                Text(
                                    text = "$todayDate (${PredefinedSchedules.getDayNameUrdu(todayDayOfWeek)})",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = PrimaryDarkGreen
                                )
                            }
                            Surface(
                                color = PrimaryDarkGreen,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "فعال سیکیورٹی ڈیوٹی",
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                if (currentUser.isSuperAdmin || currentUser.isGroupAdmin) {
                    // ADMIN SUMMARY CARDS
                    item {
                        Text(
                            text = "اعداد و شمار کا خلاصہ (حقیقی ریکارڈز)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = PrimaryDarkGreen
                        )
                    }

                    item {
                        // 2x2 grid of metrics
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            MetricCard(
                                title = "شیڈولڈ کارکنان",
                                value = "${todayScheduledStaff.size}",
                                icon = Icons.Default.People,
                                color = PrimaryDarkGreen,
                                modifier = Modifier.weight(1f)
                            )
                            MetricCard(
                                title = "حاضری درج شدہ",
                                value = "$recordedCount",
                                icon = Icons.Default.CheckCircle,
                                color = SecondaryGreen,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            MetricCard(
                                title = "غیر حاضری / باقی",
                                value = "$missingAttendanceCount",
                                icon = Icons.Default.Warning,
                                color = WarningAmber,
                                modifier = Modifier.weight(1f)
                            )
                            MetricCard(
                                title = "زیر التواء بکنگ",
                                value = "${pendingBookings.size}",
                                icon = Icons.Default.DirectionsBus,
                                color = ErrorRed,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            MetricCard(
                                title = "قبول شدہ بکنگز",
                                value = "${acceptedBookings.size}",
                                icon = Icons.Default.ThumbUp,
                                color = SecondaryGreen,
                                modifier = Modifier.weight(1f)
                            )
                            MetricCard(
                                title = "مسترد شدہ بکنگز",
                                value = "${declinedBookings.size}",
                                icon = Icons.Default.ThumbDown,
                                color = SecondaryText,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Duty Groups Quick Access
                    item {
                        Text(
                            text = "سیکیورٹی ڈیوٹی گروپس",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = PrimaryDarkGreen
                        )
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            GroupActionCard(
                                title = "گروپ A",
                                subtitle = "سیکیورٹی دستہ",
                                onClick = { onNavigateToAttendance("A") },
                                modifier = Modifier.weight(1f)
                            )
                            GroupActionCard(
                                title = "گروپ B",
                                subtitle = "حفاظتی دستہ",
                                onClick = { onNavigateToAttendance("B") },
                                modifier = Modifier.weight(1f)
                            )
                            GroupActionCard(
                                title = "گروپ C",
                                subtitle = "ریزرو دستہ",
                                onClick = { onNavigateToAttendance("C") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Administrative Quick Navigation
                    item {
                        Text(
                            text = "انتظامی کنٹرولز اور ماڈیولز",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = PrimaryDarkGreen
                        )
                    }

                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            NavRowCard(
                                title = "روزانہ حاضری (تین کالم ٹیبل)",
                                subtitle = "آمد و روانگی کے اوقات کا اندراج اور تصدیق",
                                icon = Icons.Default.Assignment,
                                onClick = { onNavigateToAttendance("A") }
                            )
                            NavRowCard(
                                title = "ہفتہ وار ڈیوٹی لسٹ (پیر تا اتوار)",
                                subtitle = "پیشگی طے شدہ شیڈول اور کوآرڈینیٹر تفصیلات",
                                icon = Icons.Default.Event,
                                onClick = { onNavigateToDutyList("A") }
                            )
                            NavRowCard(
                                title = "کوسٹر بکنگ ان باکس (${pendingBookings.size} زیر التواء)",
                                subtitle = "موصولہ میسجز پر قبول یا مسترد کرنے کا فیصلہ",
                                icon = Icons.Default.DirectionsBus,
                                onClick = onNavigateToBookings
                            )
                            NavRowCard(
                                title = "رپورٹس اور پرنٹ پیش منظر",
                                subtitle = "روزانہ، ہفتہ وار، پی ڈی ایف اور سی ایس وی ایکسپورٹ",
                                icon = Icons.Default.Print,
                                onClick = onNavigateToReports
                            )
                            if (currentUser.isSuperAdmin) {
                                NavRowCard(
                                    title = "کارکنان اور اکاؤنٹس مینجمنٹ",
                                    subtitle = "صارفین، گروپس اسائنمنٹ اور کردار کی تفویض",
                                    icon = Icons.Default.Group,
                                    onClick = onNavigateToStaff
                                )
                            }
                        }
                    }
                } else {
                    // WORKER DASHBOARD
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = CardBackground),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Text(
                                    text = "آپ کا مجاز ڈیوٹی گروپ: گروپ ${currentUser.assignedGroup}",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = PrimaryDarkGreen
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "آج کے شیڈول میں آپ کا اندراج اور بکنگ پیغامات نیچے ملاحظہ فرمائیں۔",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = SecondaryText
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { onNavigateToAttendance(currentUser.assignedGroup) },
                                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryDarkGreen),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("میری حاضری دیکھیں")
                                    }
                                    OutlinedButton(
                                        onClick = { onNavigateToDutyList(currentUser.assignedGroup) },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("ہفتہ وار شیڈول")
                                    }
                                }
                            }
                        }
                    }

                    // Worker's Pending Booking Messages with Direct Accept/Decline Actions
                    item {
                        Text(
                            text = "موصولہ کوسٹر بکنگ میسجز (کارکن فیصلہ سیکشن)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = PrimaryDarkGreen
                        )
                    }

                    if (workerRelevantBookings.isEmpty()) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = CardBackground),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "فی الوقت کوئی بکنگ پیغام موصول نہیں ہوا۔",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = SecondaryText,
                                    modifier = Modifier.padding(24.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        items(workerRelevantBookings.take(5)) { booking ->
                            WorkerBookingCard(
                                booking = booking,
                                onAccept = { onAcceptBooking(booking) },
                                onDecline = { onDeclineBooking(booking) }
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = color
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = SecondaryText
            )
        }
    }
}

@Composable
fun GroupActionCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = PrimaryDarkGreen
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = SecondaryText
            )
        }
    }
}

@Composable
fun NavRowCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(LightMint),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = PrimaryDarkGreen)
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = PrimaryDarkGreen
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = SecondaryText
                )
            }
            Icon(
                imageVector = Icons.Default.ArrowBack, // In RTL layout arrow back points left
                contentDescription = null,
                tint = SecondaryText
            )
        }
    }
}

@Composable
fun WorkerBookingCard(
    booking: BookingMessage,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ریفرنس: ${booking.bookingRef}",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = PrimaryDarkGreen
                )
                StatusBadge(status = booking.status)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                color = MainBackground,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = booking.originalMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MainText,
                    modifier = Modifier.padding(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "ذریعہ: ${booking.source}  |  گروپ: ${booking.groupId}",
                    style = MaterialTheme.typography.bodySmall,
                    color = SecondaryText
                )
                if (booking.decidedByName != null) {
                    Text(
                        text = "فیصلہ کنندہ: ${booking.decidedByName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondaryGreen
                    )
                }
            }

            if (booking.status == "pending") {
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onAccept,
                        colors = ButtonDefaults.buttonColors(containerColor = SecondaryGreen),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("accept_booking_button")
                    ) {
                        Text("قبول کریں")
                    }
                    Button(
                        onClick = onDecline,
                        colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("decline_booking_button")
                    ) {
                        Text("مسترد کریں")
                    }
                }
            }
        }
    }
}
