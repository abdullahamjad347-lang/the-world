package com.example.ui.screens

import android.app.DatePickerDialog
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.model.AttendanceRecord
import com.example.data.model.BookingMessage
import com.example.data.model.User
import com.example.ui.components.StatusBadge
import com.example.ui.components.UrduRtlProvider
import com.example.ui.components.UrduTopAppBar
import com.example.ui.theme.*
import com.example.ui.util.PrintUtils
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    currentUser: User,
    allAttendance: List<AttendanceRecord>,
    allBookings: List<BookingMessage>,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val calendar = remember { Calendar.getInstance() }
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH) }

    var reportCategory by remember { mutableStateOf("attendance") } // "attendance" or "bookings"
    var selectedGroup by remember { mutableStateOf("ALL") }

    var startDate by remember {
        val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -7) }
        mutableStateOf(dateFormat.format(cal.time))
    }
    var endDate by remember {
        mutableStateOf(dateFormat.format(calendar.time))
    }

    // Filtered attendance data
    val filteredAttendance = remember(allAttendance, startDate, endDate, selectedGroup) {
        allAttendance.filter { record ->
            val inDateRange = record.dutyDate >= startDate && record.dutyDate <= endDate
            val inGroup = selectedGroup == "ALL" || record.groupId == selectedGroup
            inDateRange && inGroup
        }
    }

    // Filtered booking data
    val filteredBookings = remember(allBookings, selectedGroup) {
        allBookings.filter { booking ->
            selectedGroup == "ALL" || booking.groupId == selectedGroup
        }
    }

    val presentCount = filteredAttendance.count { it.arrivalTime != "-" }
    val fullDayCount = filteredAttendance.count { it.arrivalTime != "-" && it.departureTime != "-" }

    UrduRtlProvider {
        Scaffold(
            topBar = {
                UrduTopAppBar(
                    title = "رپورٹس اور پرنٹنگ",
                    subtitle = "مکمل ڈیٹا کا تجزیہ اور پی ڈی ایف برآمد",
                    showBackButton = true,
                    onBackClick = onBackClick
                )
            },
            containerColor = MainBackground
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(6.dp))

                    // Report Category Tabs: Attendance vs Coaster Booking
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = reportCategory == "attendance",
                            onClick = { reportCategory = "attendance" },
                            label = { Text("حاضری رپورٹس (${filteredAttendance.size})") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = reportCategory == "bookings",
                            onClick = { reportCategory = "bookings" },
                            label = { Text("کوسٹر بکنگ رپورٹس (${filteredBookings.size})") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Filter controls card: Date Range & Group
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CardBackground),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "رپورٹ فلٹرز:",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = PrimaryDarkGreen
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Group selector chips
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("گروپ:", style = MaterialTheme.typography.bodySmall)
                                listOf("ALL", "A", "B", "C").forEach { g ->
                                    FilterChip(
                                        selected = selectedGroup == g,
                                        onClick = { selectedGroup = g },
                                        label = { Text(if (g == "ALL") "تمام گروپس" else "گروپ $g") }
                                    )
                                }
                            }

                            if (reportCategory == "attendance") {
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            val parts = startDate.split("-")
                                            DatePickerDialog(
                                                context,
                                                { _, y, m, d ->
                                                    val c = Calendar.getInstance().apply { set(y, m, d) }
                                                    startDate = dateFormat.format(c.time)
                                                },
                                                parts[0].toInt(),
                                                parts[1].toInt() - 1,
                                                parts[2].toInt()
                                            ).show()
                                        }
                                    ) {
                                        Text("شروعات: $startDate")
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            val parts = endDate.split("-")
                                            DatePickerDialog(
                                                context,
                                                { _, y, m, d ->
                                                    val c = Calendar.getInstance().apply { set(y, m, d) }
                                                    endDate = dateFormat.format(c.time)
                                                },
                                                parts[0].toInt(),
                                                parts[1].toInt() - 1,
                                                parts[2].toInt()
                                            ).show()
                                        }
                                    ) {
                                        Text("اختتام: $endDate")
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Export buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        if (reportCategory == "attendance") {
                                            val file = PrintUtils.generateAttendancePdf(
                                                context = context,
                                                dutyDate = "$startDate تا $endDate",
                                                groupName = selectedGroup,
                                                records = filteredAttendance
                                            )
                                            if (file != null) {
                                                PrintUtils.sharePdf(context, file, "حاضری رپورٹ")
                                            } else {
                                                Toast.makeText(context, "پی ڈی ایف بنانے میں ناکامی", Toast.LENGTH_SHORT).show()
                                            }
                                        } else {
                                            // Share CSV for Bookings
                                            val csv = buildString {
                                                appendLine("Reference,Group,Source,Status,DecidedBy,Message")
                                                filteredBookings.forEach { b ->
                                                    appendLine("${b.bookingRef},${b.groupId},${b.source},${b.status},${b.decidedByName ?: ""},\"${b.originalMessage.replace("\"", "\"\"")}\"")
                                                }
                                            }
                                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_SUBJECT, "کوسٹر بکنگ رپورٹ")
                                                putExtra(Intent.EXTRA_TEXT, csv)
                                            }
                                            context.startActivity(Intent.createChooser(sendIntent, "سی ایس وی شیئر کریں"))
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryDarkGreen),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (reportCategory == "attendance") "پی ڈی ایف برآمد کریں" else "سی ایس وی برآمد کریں")
                                }
                            }
                        }
                    }
                }

                // Summary Numbers
                item {
                    if (reportCategory == "attendance") {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = LightMint),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("کل ریکارڈز", style = MaterialTheme.typography.bodySmall, color = SecondaryText)
                                    Text("${filteredAttendance.size}", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = PrimaryDarkGreen)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("کل حاضری", style = MaterialTheme.typography.bodySmall, color = SecondaryText)
                                    Text("$presentCount", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = SecondaryGreen)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("مکمل ڈیوٹی", style = MaterialTheme.typography.bodySmall, color = SecondaryText)
                                    Text("$fullDayCount", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = PrimaryDarkGreen)
                                }
                            }
                        }
                    }
                }

                // Records List
                if (reportCategory == "attendance") {
                    if (filteredAttendance.isEmpty()) {
                        item {
                            Text(
                                text = "منتخب کردہ تاریخوں کے درمیان کوئی حاضری ریکارڈ نہیں ملا۔",
                                style = MaterialTheme.typography.bodyMedium,
                                color = SecondaryText,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        items(filteredAttendance) { rec ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = CardBackground),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = rec.workerName,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = MainText
                                        )
                                        Text(
                                            text = "تاریخ: ${rec.dutyDate}  |  گروپ: ${rec.groupId}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = SecondaryText
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "آمد: ${rec.arrivalTime}  |  روانگی: ${rec.departureTime}",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                            color = SecondaryGreen
                                        )
                                        StatusBadge(status = rec.status)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Bookings list
                    if (filteredBookings.isEmpty()) {
                        item {
                            Text(
                                text = "کوئی بکنگ ریکارڈ موجود نہیں ہے۔",
                                style = MaterialTheme.typography.bodyMedium,
                                color = SecondaryText,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        items(filteredBookings) { booking ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = CardBackground),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "ریفرنس: ${booking.bookingRef} (گروپ ${booking.groupId})",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = PrimaryDarkGreen
                                        )
                                        StatusBadge(status = booking.status)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = booking.originalMessage,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MainText
                                    )
                                    if (booking.decidedByName != null) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "فیصلہ از: ${booking.decidedByName} ${if (!booking.decisionNote.isNullOrBlank()) "(${booking.decisionNote})" else ""}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = SecondaryGreen
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(30.dp))
                }
            }
        }
    }
}
