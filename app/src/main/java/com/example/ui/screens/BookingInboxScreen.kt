package com.example.ui.screens

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
import androidx.compose.ui.unit.sp
import com.example.data.model.BookingMessage
import com.example.data.model.User
import com.example.ui.components.StatusBadge
import com.example.ui.components.UrduRtlProvider
import com.example.ui.components.UrduTopAppBar
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingInboxScreen(
    currentUser: User,
    bookingMessages: List<BookingMessage>,
    onAcceptBooking: (BookingMessage, String?) -> Unit,
    onDeclineBooking: (BookingMessage, String?) -> Unit,
    onIngestBooking: (groupId: String, messageText: String, source: String) -> Unit,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    var selectedFilter by remember { mutableStateOf("all") } // "all", "pending", "accepted", "declined"
    var selectedGroupFilter by remember { mutableStateOf(if (currentUser.isSuperAdmin) "ALL" else currentUser.assignedGroup) }

    // Admin message ingestion dialog state
    var showIngestionDialog by remember { mutableStateOf(false) }
    var ingestionText by remember { mutableStateOf("") }
    var ingestionGroup by remember { mutableStateOf("A") }
    var ingestionSource by remember { mutableStateOf("WhatsApp") }

    // Decision confirmation dialog state
    var pendingDecisionBooking by remember { mutableStateOf<BookingMessage?>(null) }
    var isAcceptDecision by remember { mutableStateOf(true) }
    var decisionNote by remember { mutableStateOf("") }

    val filteredList = remember(bookingMessages, selectedFilter, selectedGroupFilter) {
        bookingMessages.filter { msg ->
            val matchesStatus = when (selectedFilter) {
                "pending" -> msg.status == "pending"
                "accepted" -> msg.status == "accepted"
                "declined" -> msg.status == "declined"
                else -> true
            }
            val matchesGroup = selectedGroupFilter == "ALL" || msg.groupId == selectedGroupFilter
            matchesStatus && matchesGroup
        }
    }

    val pendingCount = remember(bookingMessages) { bookingMessages.count { it.status == "pending" } }
    val acceptedCount = remember(bookingMessages) { bookingMessages.count { it.status == "accepted" } }
    val declinedCount = remember(bookingMessages) { bookingMessages.count { it.status == "declined" } }

    UrduRtlProvider {
        Scaffold(
            topBar = {
                UrduTopAppBar(
                    title = "کوسٹر بکنگ ان باکس",
                    subtitle = "موصولہ پیغامات پر مبنی کارروائی",
                    showBackButton = true,
                    onBackClick = onBackClick,
                    actions = {
                        if (currentUser.isSuperAdmin || currentUser.isGroupAdmin) {
                            IconButton(onClick = { showIngestionDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.AddComment,
                                    contentDescription = "میسج درج کریں",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                )
            },
            floatingActionButton = {
                if (currentUser.isSuperAdmin || currentUser.isGroupAdmin) {
                    ExtendedFloatingActionButton(
                        onClick = { showIngestionDialog = true },
                        icon = { Icon(Icons.Default.Add, contentDescription = null) },
                        text = { Text("موصولہ میسج درج کریں") },
                        containerColor = PrimaryDarkGreen,
                        contentColor = Color.White,
                        modifier = Modifier.testTag("ingest_message_fab")
                    )
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

                    // Notice explaining message-based workflow
                    Card(
                        colors = CardDefaults.cardColors(containerColor = LightMint.copy(alpha = 0.7f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = PrimaryDarkGreen,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "کوسٹر بکنگ مکمل طور پر موصولہ پیغامات پر منحصر ہے۔ کارکنان کو دستی بکنگ فارم بھرنے کی ضرورت نہیں، صرف قبول یا مسترد کریں۔",
                                style = MaterialTheme.typography.bodySmall,
                                color = PrimaryDarkGreen
                            )
                        }
                    }
                }

                // Status Filter Chips
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = selectedFilter == "all",
                            onClick = { selectedFilter = "all" },
                            label = { Text("تمام (${bookingMessages.size})") }
                        )
                        FilterChip(
                            selected = selectedFilter == "pending",
                            onClick = { selectedFilter = "pending" },
                            label = { Text("زیر التواء ($pendingCount)") }
                        )
                        FilterChip(
                            selected = selectedFilter == "accepted",
                            onClick = { selectedFilter = "accepted" },
                            label = { Text("قبول ($acceptedCount)") }
                        )
                        FilterChip(
                            selected = selectedFilter == "declined",
                            onClick = { selectedFilter = "declined" },
                            label = { Text("مسترد ($declinedCount)") }
                        )
                    }
                }

                // Group Filter Chips (if admin)
                if (currentUser.isSuperAdmin) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "گروپ فلٹر:",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = SecondaryText
                            )
                            listOf("ALL", "A", "B", "C").forEach { g ->
                                FilterChip(
                                    selected = selectedGroupFilter == g,
                                    onClick = { selectedGroupFilter = g },
                                    label = { Text(if (g == "ALL") "سب" else "گروپ $g") }
                                )
                            }
                        }
                    }
                }

                // Booking Message Cards List
                if (filteredList.isEmpty()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = CardBackground),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 30.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Inbox,
                                    contentDescription = null,
                                    tint = SecondaryText,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "کوئی کوسٹر بکنگ میسج موجود نہیں ہے۔",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = PrimaryDarkGreen
                                )
                                Text(
                                    text = "جب واٹس ایپ یا ایس ایم ایس سے بکنگ کی درخواست موصول ہوگی، وہ یہاں ظاہر ہوگی۔",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SecondaryText,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    items(filteredList, key = { it.id }) { booking ->
                        BookingMessageCard(
                            booking = booking,
                            onAcceptClick = {
                                pendingDecisionBooking = booking
                                isAcceptDecision = true
                                decisionNote = ""
                            },
                            onDeclineClick = {
                                pendingDecisionBooking = booking
                                isAcceptDecision = false
                                decisionNote = ""
                            }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(60.dp))
                }
            }
        }

        // Decision Confirmation Dialog
        if (pendingDecisionBooking != null) {
            val booking = pendingDecisionBooking!!
            AlertDialog(
                onDismissRequest = { pendingDecisionBooking = null },
                title = {
                    Text(
                        text = if (isAcceptDecision) "بکنگ قبول کرنے کی تصدیق" else "بکنگ مسترد کرنے کی تصدیق",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = if (isAcceptDecision) SecondaryGreen else ErrorRed
                    )
                },
                text = {
                    Column {
                        Text(
                            text = "ریفرنس: ${booking.bookingRef}",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = PrimaryDarkGreen
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "اصل پیغام:\n${booking.originalMessage}",
                            style = MaterialTheme.typography.bodySmall,
                            color = SecondaryText
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = decisionNote,
                            onValueChange = { decisionNote = it },
                            label = { Text("اختیاری نوٹ (وجہ یا ہدایات)") },
                            singleLine = false,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (isAcceptDecision) {
                                onAcceptBooking(booking, decisionNote.ifBlank { null })
                            } else {
                                onDeclineBooking(booking, decisionNote.ifBlank { null })
                            }
                            pendingDecisionBooking = null
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isAcceptDecision) SecondaryGreen else ErrorRed
                        )
                    ) {
                        Text(if (isAcceptDecision) "قبول کریں" else "مسترد کریں")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { pendingDecisionBooking = null }) {
                        Text("واپس جائیں")
                    }
                }
            )
        }

        // Admin Message Ingestion Dialog
        if (showIngestionDialog) {
            AlertDialog(
                onDismissRequest = { showIngestionDialog = false },
                title = {
                    Text(
                        text = "موصولہ میسج سسٹم میں شامل کریں",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = PrimaryDarkGreen
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "بیرونی ذریعے (واٹس ایپ / ایس ایم ایس) سے موصول ہونے والا ٹیکسٹ یہاں درج کریں:",
                            style = MaterialTheme.typography.bodySmall,
                            color = SecondaryText
                        )

                        OutlinedTextField(
                            value = ingestionText,
                            onValueChange = { ingestionText = it },
                            label = { Text("اصل موصولہ میسج") },
                            placeholder = { Text("مثال: کل صبح 8 بجے ایئرپورٹ کے لیے 15 افراد کی کوسٹر درکار ہے۔ رابطہ نمبر 0300...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            maxLines = 5
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("ہدف گروپ:", style = MaterialTheme.typography.bodySmall)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf("A", "B", "C").forEach { g ->
                                    FilterChip(
                                        selected = ingestionGroup == g,
                                        onClick = { ingestionGroup = g },
                                        label = { Text("گروپ $g") }
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("ذریعہ:", style = MaterialTheme.typography.bodySmall)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf("WhatsApp", "SMS").forEach { s ->
                                    FilterChip(
                                        selected = ingestionSource == s,
                                        onClick = { ingestionSource = s },
                                        label = { Text(s) }
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (ingestionText.isNotBlank()) {
                                onIngestBooking(ingestionGroup, ingestionText.trim(), ingestionSource)
                                ingestionText = ""
                                showIngestionDialog = false
                                Toast.makeText(context, "میسج ان باکس میں منتقل ہو گیا!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = ingestionText.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryDarkGreen)
                    ) {
                        Text("ان باکس میں بھیجیں")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showIngestionDialog = false }) {
                        Text("منسوخ")
                    }
                }
            )
        }
    }
}

@Composable
fun BookingMessageCard(
    booking: BookingMessage,
    onAcceptClick: () -> Unit,
    onDeclineClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Reference, Group & Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(LightMint),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsBus,
                            contentDescription = null,
                            tint = PrimaryDarkGreen,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = booking.bookingRef,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = PrimaryDarkGreen
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = Color(0xFFE8F0EC),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "گروپ ${booking.groupId}",
                            style = MaterialTheme.typography.labelSmall,
                            color = PrimaryDarkGreen,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                StatusBadge(status = booking.status)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Body: Original Preserved Message
            Surface(
                color = MainBackground,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "اصل موصولہ میسج:",
                        style = MaterialTheme.typography.labelSmall,
                        color = SecondaryText
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = booking.originalMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MainText
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Metadata: Source & Decision Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "ذریعہ: ${booking.source}",
                    style = MaterialTheme.typography.bodySmall,
                    color = SecondaryText
                )
                if (booking.decidedByName != null) {
                    Text(
                        text = "فیصلہ از: ${booking.decidedByName}",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = if (booking.status == "accepted") SecondaryGreen else ErrorRed
                    )
                }
            }

            if (!booking.decisionNote.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "نوٹ: ${booking.decisionNote}",
                    style = MaterialTheme.typography.bodySmall,
                    color = SecondaryText
                )
            }

            // Exactly Two Decision Actions on Pending messages: قبول کریں & مسترد کریں
            if (booking.status == "pending") {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onAcceptClick,
                        colors = ButtonDefaults.buttonColors(containerColor = SecondaryGreen),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("accept_button_${booking.bookingRef}")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("قبول کریں")
                    }
                    Button(
                        onClick = onDeclineClick,
                        colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("decline_button_${booking.bookingRef}")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("مسترد کریں")
                    }
                }
            }
        }
    }
}
