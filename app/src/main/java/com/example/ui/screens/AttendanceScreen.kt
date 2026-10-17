package com.example.ui.screens

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.unit.sp
import com.example.data.model.AttendanceRecord
import com.example.data.model.PredefinedSchedules
import com.example.data.model.PredefinedStaff
import com.example.data.model.User
import com.example.ui.components.TimePickerDialog24h
import com.example.ui.components.UrduRtlProvider
import com.example.ui.components.UrduTopAppBar
import com.example.ui.theme.*
import com.example.ui.util.PrintUtils
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceScreen(
    currentUser: User,
    initialGroupId: String = "A",
    existingRecords: List<AttendanceRecord>,
    onSaveRecords: (List<AttendanceRecord>) -> Unit,
    onBackClick: () -> Unit,
    onNavigateToReports: () -> Unit
) {
    val context = LocalContext.current
    var selectedGroup by remember { mutableStateOf(initialGroupId) }

    // Date state
    val calendar = remember { Calendar.getInstance() }
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH) }
    var selectedDate by remember { mutableStateOf(dateFormat.format(calendar.time)) }
    var selectedDayOfWeek by remember {
        val dow = calendar.get(Calendar.DAY_OF_WEEK)
        // Convert Java Calendar (Sun=1, Mon=2..Sat=7) to our 1=Mon..7=Sun
        val mapped = if (dow == Calendar.SUNDAY) 7 else dow - 1
        mutableStateOf(mapped)
    }

    // Predefined staff list for selected day
    val scheduledStaff = remember(selectedDayOfWeek) {
        PredefinedSchedules.getStaffForDay(selectedDayOfWeek)
    }

    // Editable draft records map: WorkerName -> Pair(arrivalTime, departureTime)
    val draftTimes = remember { mutableStateMapOf<String, Pair<String, String>>() }
    var hasUnsavedChanges by remember { mutableStateOf(false) }

    // Sync draft with existing records whenever date or group changes
    LaunchedEffect(selectedDate, selectedGroup, existingRecords, scheduledStaff) {
        draftTimes.clear()
        scheduledStaff.forEach { staff ->
            val match = existingRecords.find {
                it.workerName == staff.name && it.groupId == selectedGroup && it.dutyDate == selectedDate
            }
            if (match != null) {
                draftTimes[staff.name] = Pair(match.arrivalTime, match.departureTime)
            } else {
                draftTimes[staff.name] = Pair("-", "-")
            }
        }
        hasUnsavedChanges = false
    }

    // Time picker dialog state
    var editingWorkerName by remember { mutableStateOf<String?>(null) }
    var editingIsArrival by remember { mutableStateOf(true) }

    // Print preview dialog state
    var showPrintPreview by remember { mutableStateOf(false) }

    // Validation helper
    fun validateTimeOrder(arr: String, dep: String): Boolean {
        if (arr == "-" || dep == "-") return true
        val arrParts = arr.split(":")
        val depParts = dep.split(":")
        if (arrParts.size != 2 || depParts.size != 2) return true
        val arrMinutes = (arrParts[0].toIntOrNull() ?: 0) * 60 + (arrParts[1].toIntOrNull() ?: 0)
        val depMinutes = (depParts[0].toIntOrNull() ?: 0) * 60 + (depParts[1].toIntOrNull() ?: 0)
        return depMinutes >= arrMinutes
    }

    // Construct current list of records for saving/printing
    val currentAttendanceList = remember(draftTimes, scheduledStaff, selectedGroup, selectedDate, selectedDayOfWeek) {
        scheduledStaff.mapIndexed { index, staff ->
            val times = draftTimes[staff.name] ?: Pair("-", "-")
            val arrival = times.first
            val departure = times.second
            val status = when {
                arrival != "-" && departure != "-" -> "recorded"
                arrival != "-" -> "incomplete"
                else -> "not_recorded"
            }
            val existingId = existingRecords.find {
                it.workerName == staff.name && it.groupId == selectedGroup && it.dutyDate == selectedDate
            }?.id ?: ""

            AttendanceRecord(
                id = existingId,
                groupId = selectedGroup,
                dutyDate = selectedDate,
                dayOfWeek = selectedDayOfWeek,
                workerName = staff.name,
                isCoordinator = staff.isCoordinator,
                arrivalTime = arrival,
                departureTime = departure,
                status = status,
                recordedBy = currentUser.userId,
                recordedByName = currentUser.displayName
            )
        }
    }

    val presentCount = currentAttendanceList.count { it.arrivalTime != "-" }
    val totalCount = currentAttendanceList.size

    UrduRtlProvider {
        Scaffold(
            topBar = {
                UrduTopAppBar(
                    title = "روزانہ حاضری (ڈیوٹی لسٹ)",
                    subtitle = "گروپ $selectedGroup  |  ${PredefinedSchedules.getDayNameUrdu(selectedDayOfWeek)}",
                    showBackButton = true,
                    onBackClick = onBackClick,
                    actions = {
                        IconButton(onClick = { showPrintPreview = true }) {
                            Icon(Icons.Default.Print, contentDescription = "پرنٹ پیش منظر", tint = Color.White)
                        }
                    }
                )
            },
            bottomBar = {
                Surface(
                    color = CardBackground,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onNavigateToReports,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("رپورٹ دیکھیں")
                        }

                        Button(
                            onClick = {
                                // Validate all records before saving
                                var hasValidationError = false
                                for (record in currentAttendanceList) {
                                    if (!validateTimeOrder(record.arrivalTime, record.departureTime)) {
                                        Toast.makeText(
                                            context,
                                            "غلطی: ${record.workerName} کا جانے کا وقت آنے کے وقت سے پہلے ہے!",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        hasValidationError = true
                                        break
                                    }
                                }

                                if (!hasValidationError) {
                                    onSaveRecords(currentAttendanceList)
                                    hasUnsavedChanges = false
                                    Toast.makeText(context, "حاضری ریکارڈ کامیابی سے محفوظ ہو گیا!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryDarkGreen),
                            modifier = Modifier
                                .weight(1.3f)
                                .testTag("save_attendance_button")
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("محفوظ کریں")
                        }
                    }
                }
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

                    // Group Selector Tabs (Group A, Group B, Group C)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("A", "B", "C").forEach { groupCode ->
                            FilterChip(
                                selected = selectedGroup == groupCode,
                                onClick = { selectedGroup = groupCode },
                                label = {
                                    Text(
                                        text = "گروپ $groupCode",
                                        fontWeight = if (selectedGroup == groupCode) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Date Picker Card
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CardBackground),
                        shape = RoundedCornerShape(12.dp),
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
                                    text = "منتخب تاریخ اور دن:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SecondaryText
                                )
                                Text(
                                    text = "$selectedDate (${PredefinedSchedules.getDayNameUrdu(selectedDayOfWeek)})",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = PrimaryDarkGreen
                                )
                            }

                            Button(
                                onClick = {
                                    val parts = selectedDate.split("-")
                                    val year = parts[0].toInt()
                                    val month = parts[1].toInt() - 1
                                    val day = parts[2].toInt()

                                    DatePickerDialog(
                                        context,
                                        { _, y, m, d ->
                                            val newCal = Calendar.getInstance().apply {
                                                set(y, m, d)
                                            }
                                            selectedDate = dateFormat.format(newCal.time)
                                            val dow = newCal.get(Calendar.DAY_OF_WEEK)
                                            selectedDayOfWeek = if (dow == Calendar.SUNDAY) 7 else dow - 1
                                        },
                                        year,
                                        month,
                                        day
                                    ).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SecondaryGreen),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("change_date_button")
                            ) {
                                Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("تاریخ تبدیل کریں")
                            }
                        }
                    }
                }

                // Summary Stats Badge
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "حاضری لسٹ (کل: $totalCount | حاضر: $presentCount | غیر حاضر: ${totalCount - presentCount})",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = PrimaryDarkGreen
                        )

                        if (hasUnsavedChanges) {
                            Surface(
                                color = Color(0xFFFFF3CD),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "غیر محفوظ تبدیلیاں!",
                                    color = WarningAmber,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                // Three-Column RTL Attendance Table Header
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = PrimaryDarkGreen),
                        shape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "کارکن کا نام",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White,
                                modifier = Modifier.weight(1.8f),
                                textAlign = TextAlign.Right
                            )
                            Text(
                                text = "آنے کا وقت",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White,
                                modifier = Modifier.weight(1.1f),
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "جانے کا وقت",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White,
                                modifier = Modifier.weight(1.1f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Three-Column Attendance Table Rows
                itemsIndexed(scheduledStaff) { index, staff ->
                    val times = draftTimes[staff.name] ?: Pair("-", "-")
                    val arrival = times.first
                    val departure = times.second
                    val isValidTimes = validateTimeOrder(arrival, departure)

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (index % 2 == 0) CardBackground else Color(0xFFF9FBF9)
                        ),
                        shape = RoundedCornerShape(0.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(width = 0.5.dp, color = BorderColor)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Column 1: Worker Name (Read-Only)
                            Column(modifier = Modifier.weight(1.8f)) {
                                Text(
                                    text = "${index + 1}. ${staff.name}",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MainText
                                )
                                if (staff.isCoordinator) {
                                    Surface(
                                        color = LightMint,
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "کوآرڈینیٹر",
                                            color = SecondaryGreen,
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }

                            // Column 2: Arrival Time Button
                            Box(
                                modifier = Modifier
                                    .weight(1.1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (arrival != "-") LightMint else Color(0xFFEDEDED))
                                    .clickable {
                                        editingWorkerName = staff.name
                                        editingIsArrival = true
                                    }
                                    .padding(vertical = 8.dp, horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = arrival,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (arrival != "-") SecondaryGreen else SecondaryText,
                                    textAlign = TextAlign.Center
                                )
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            // Column 3: Departure Time Button
                            Box(
                                modifier = Modifier
                                    .weight(1.1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        when {
                                            !isValidTimes -> Color(0xFFFFDAD6)
                                            departure != "-" -> LightMint
                                            else -> Color(0xFFEDEDED)
                                        }
                                    )
                                    .clickable {
                                        editingWorkerName = staff.name
                                        editingIsArrival = false
                                    }
                                    .padding(vertical = 8.dp, horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = departure,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = when {
                                        !isValidTimes -> ErrorRed
                                        departure != "-" -> SecondaryGreen
                                        else -> SecondaryText
                                    },
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }

        // Time Picker Dialog
        if (editingWorkerName != null) {
            val workerName = editingWorkerName!!
            val times = draftTimes[workerName] ?: Pair("-", "-")
            val initialVal = if (editingIsArrival) times.first else times.second
            val titleText = if (editingIsArrival) "$workerName: آنے کا وقت" else "$workerName: جانے کا وقت"

            TimePickerDialog24h(
                initialTime = initialVal,
                title = titleText,
                onDismiss = { editingWorkerName = null },
                onConfirm = { chosenTime ->
                    val currentTimes = draftTimes[workerName] ?: Pair("-", "-")
                    if (editingIsArrival) {
                        draftTimes[workerName] = Pair(chosenTime, currentTimes.second)
                    } else {
                        draftTimes[workerName] = Pair(currentTimes.first, chosenTime)
                    }
                    hasUnsavedChanges = true
                    editingWorkerName = null
                }
            )
        }

        // Print Preview Dialog
        if (showPrintPreview) {
            AlertDialog(
                onDismissRequest = { showPrintPreview = false },
                title = {
                    Text(
                        text = "پرنٹ پیش منظر (Print Preview)",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = PrimaryDarkGreen
                    )
                },
                text = {
                    Column {
                        Text(
                            text = "تاریخ: $selectedDate  |  گروپ: $selectedGroup  |  دن: ${PredefinedSchedules.getDayNameUrdu(selectedDayOfWeek)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = SecondaryText
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "کل ملازمین: $totalCount  |  حاضر: $presentCount  |  غیر حاضر: ${totalCount - presentCount}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = PrimaryDarkGreen
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "کیا آپ اس حاضری شیٹ کی پی ڈی ایف فائل بنا کر پرنٹ / شیئر کرنا چاہتے ہیں؟",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val pdfFile = PrintUtils.generateAttendancePdf(
                                context = context,
                                dutyDate = selectedDate,
                                groupName = selectedGroup,
                                records = currentAttendanceList
                            )
                            if (pdfFile != null) {
                                PrintUtils.sharePdf(context, pdfFile, "حاضری رپورٹ - $selectedDate")
                            } else {
                                Toast.makeText(context, "پی ڈی ایف بنانے میں ناکامی", Toast.LENGTH_SHORT).show()
                            }
                            showPrintPreview = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryDarkGreen)
                    ) {
                        Text("پی ڈی ایف ایکسپورٹ اور شیئر کریں")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showPrintPreview = false }) {
                        Text("بند کریں")
                    }
                }
            )
        }
    }
}
